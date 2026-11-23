package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.example.data.ExpenseRepository
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.IncomeEntity
import com.example.data.model.InvitationEntity
import com.example.data.model.InvitationStatus
import com.example.data.model.MembershipStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FirestoreSyncManager(
    private val context: Context,
    private val repository: ExpenseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var householdListenerRegistration: ListenerRegistration? = null
    private var expenseListenerRegistration: ListenerRegistration? = null
    private var incomeListenerRegistration: ListenerRegistration? = null
    private var budgetListenerRegistration: ListenerRegistration? = null
    private var memberListenerRegistration: ListenerRegistration? = null
    private var invitationListenerRegistration: ListenerRegistration? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var currentHouseholdId: String? = null
    private var isNetworkAvailable: Boolean = true
    private var syncLogCallback: ((String) -> Unit)? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val databaseId = context.getString(R.string.firestore_database_id)
                firestore = FirebaseFirestore.getInstance(databaseId).apply {
                    try {
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                            .build()
                        firestoreSettings = settings
                        Log.d("FirestoreSyncManager", "Firestore offline persistent disk cache enabled for [$databaseId].")
                    } catch (e: Exception) {
                        Log.w("FirestoreSyncManager", "Firestore settings note: ${e.message}")
                    }
                }
            } else {
                Log.w("FirestoreSyncManager", "FirebaseApp is not initialized.")
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to initialize Firebase Firestore", e)
        }

        registerNetworkConnectivityListener()
    }

    fun getFirestoreInstance(): FirebaseFirestore? = firestore

    private fun registerNetworkConnectivityListener() {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isNetworkAvailable = true
                    if (currentHouseholdId != null) {
                        triggerFullReSync()
                    }
                }

                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                    syncLogCallback?.invoke("Offline Mode Active: Changes cached locally until reconnected.")
                }
            }
            networkCallback = callback
            connectivityManager.registerNetworkCallback(networkRequest, callback)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Could not register connectivity listener: ${e.message}")
        }
    }

    fun isFirestoreAvailable(): Boolean = firestore != null

    fun isOnline(): Boolean = isNetworkAvailable

    fun startRealtimeSync(householdId: String, onSyncLog: (String) -> Unit) {
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        } ?: return

        syncLogCallback = onSyncLog
        if (currentHouseholdId == householdId && expenseListenerRegistration != null) return

        stopRealtimeSync()
        currentHouseholdId = householdId

        val db = firestore ?: run {
            onSyncLog("Local Room cache active")
            return
        }

        try {
            onSyncLog("Real-time household sync active [$householdId]")

            // 1. Household document listener
            val householdPath = "households/$householdId"
            householdListenerRegistration = db.collection("households")
                .document(householdId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.GET, householdPath)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        scope.launch {
                            val entity = HouseholdEntity(
                                householdId = snapshot.id,
                                householdName = snapshot.getString("householdName") ?: "My Household",
                                inviteCode = snapshot.getString("inviteCode") ?: snapshot.id,
                                defaultCurrency = snapshot.getString("defaultCurrency") ?: "₹",
                                totalMonthlyBudget = snapshot.getDouble("totalMonthlyBudget") ?: 3000.0,
                                lastSyncedTimestamp = snapshot.getLong("lastSyncedTimestamp") ?: System.currentTimeMillis(),
                                isLiveSyncEnabled = snapshot.getBoolean("isLiveSyncEnabled") ?: true,
                                ownerUserId = snapshot.getString("ownerUserId") ?: ""
                            )
                            repository.updateHousehold(entity)
                        }
                    }
                }

            // 2. Expenses listener
            val expensesPath = "households/$householdId/expenses"
            expenseListenerRegistration = db.collection("households")
                .document(householdId)
                .collection("expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, expensesPath)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        scope.launch {
                            val toAdd = mutableListOf<ExpenseEntity>()
                            val toRemove = mutableListOf<String>()

                            for (dc in snapshot.documentChanges) {
                                val doc = dc.document
                                val id = doc.id
                                when (dc.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        toAdd.add(
                                            ExpenseEntity(
                                                id = id,
                                                amount = doc.getDouble("amount") ?: 0.0,
                                                currencySymbol = doc.getString("currencySymbol") ?: "₹",
                                                category = doc.getString("category") ?: "General",
                                                description = doc.getString("description") ?: "Expense",
                                                paidByMemberId = doc.getString("paidByMemberId") ?: "",
                                                paidByMemberName = doc.getString("paidByMemberName") ?: "Family Member",
                                                splitType = doc.getString("splitType") ?: "Split Equally",
                                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                                householdId = householdId,
                                                paymentMethod = doc.getString("paymentMethod") ?: "Credit Card",
                                                note = doc.getString("note") ?: "",
                                                isRecurring = doc.getBoolean("isRecurring") ?: false,
                                                recurringFrequency = doc.getString("recurringFrequency") ?: "Monthly",
                                                recurringDayOfMonth = doc.getLong("recurringDayOfMonth")?.toInt() ?: 1,
                                                isAutoCreated = doc.getBoolean("isAutoCreated") ?: false,
                                                isSettled = doc.getBoolean("isSettled") ?: false,
                                                receiptUri = doc.getString("receiptUri"),
                                                tags = doc.getString("tags") ?: ""
                                            )
                                        )
                                    }
                                    DocumentChange.Type.REMOVED -> toRemove.add(id)
                                }
                            }

                            if (toAdd.isNotEmpty()) repository.addExpenses(toAdd)
                            toRemove.forEach { repository.deleteExpenseById(it, householdId) }
                        }
                    }
                }

            // 3. Incomes listener
            val incomesPath = "households/$householdId/incomes"
            incomeListenerRegistration = db.collection("households")
                .document(householdId)
                .collection("incomes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, incomesPath)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            val toAdd = mutableListOf<IncomeEntity>()
                            val toRemove = mutableListOf<String>()
                            for (dc in snapshot.documentChanges) {
                                val doc = dc.document
                                when (dc.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        toAdd.add(
                                            IncomeEntity(
                                                id = doc.id,
                                                amount = doc.getDouble("amount") ?: 0.0,
                                                source = doc.getString("source") ?: "Income",
                                                category = doc.getString("category") ?: "Salary",
                                                date = doc.getLong("date") ?: System.currentTimeMillis(),
                                                receivedBy = doc.getString("receivedBy") ?: "",
                                                receivedByName = doc.getString("receivedByName") ?: "Family Member",
                                                paymentMethod = doc.getString("paymentMethod") ?: "Bank Transfer",
                                                isRecurring = doc.getBoolean("isRecurring") ?: false,
                                                recurringFrequency = doc.getString("recurringFrequency"),
                                                recurringStartDate = doc.getLong("recurringStartDate"),
                                                note = doc.getString("note"),
                                                householdId = householdId,
                                                currencySymbol = doc.getString("currencySymbol") ?: "₹"
                                            )
                                        )
                                    }
                                    DocumentChange.Type.REMOVED -> toRemove.add(doc.id)
                                }
                            }
                            if (toAdd.isNotEmpty()) repository.addIncomes(toAdd)
                            toRemove.forEach { repository.deleteIncomeById(it, householdId) }
                        }
                    }
                }

            // 4. Budgets listener
            val budgetsPath = "households/$householdId/budgets"
            budgetListenerRegistration = db.collection("households")
                .document(householdId)
                .collection("budgets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, budgetsPath)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        scope.launch {
                            for (dc in snapshot.documentChanges) {
                                if (dc.type == DocumentChange.Type.ADDED || dc.type == DocumentChange.Type.MODIFIED) {
                                    val doc = dc.document
                                    val budget = CategoryBudgetEntity(
                                        categoryName = doc.getString("categoryName") ?: doc.id,
                                        monthlyLimit = doc.getDouble("monthlyLimit") ?: 0.0,
                                        householdId = householdId,
                                        iconName = doc.getString("iconName") ?: "category",
                                        colorHex = doc.getString("colorHex") ?: "#4F46E5"
                                    )
                                    repository.setCategoryBudget(budget)
                                }
                            }
                        }
                    }
                }

            // 5. Family Members listener (preserves local active selection while syncing roles/emails/status)
            val membersPath = "households/$householdId/members"
            memberListenerRegistration = db.collection("households")
                .document(householdId)
                .collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, membersPath)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        scope.launch {
                            val existingMembers = repository.getMembersOnce(householdId).associateBy { it.id }
                            val toAdd = mutableListOf<FamilyMemberEntity>()
                            val toRemove = mutableListOf<FamilyMemberEntity>()

                            for (dc in snapshot.documentChanges) {
                                val doc = dc.document
                                val id = doc.id
                                val userId = doc.getString("userId") ?: ""
                                val joinedTimestamp = doc.getTimestamp(
                                    "joinedAt",
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )?.toDate()?.time ?: System.currentTimeMillis()

                                val previouslyActive = existingMembers[id]?.isCurrentActiveUser
                                    ?: (userId.isNotEmpty() && userId == currentUser.uid)

                                val entity = FamilyMemberEntity(
                                    id = id,
                                    name = doc.getString("name") ?: "Member",
                                    role = doc.getString("role") ?: "Member",
                                    avatarColorHex = doc.getString("avatarColorHex") ?: "#1E88E5",
                                    avatarIcon = doc.getString("avatarIcon") ?: "person",
                                    isCurrentActiveUser = previouslyActive,
                                    householdId = householdId,
                                    monthlyContributionGoal = doc.getDouble("monthlyContributionGoal") ?: 0.0,
                                    email = doc.getString("email") ?: "",
                                    userId = userId,
                                    householdRole = doc.getString("householdRole") ?: HouseholdRole.MEMBER.value,
                                    membershipStatus = doc.getString("membershipStatus") ?: MembershipStatus.ACTIVE.value,
                                    joinedAt = joinedTimestamp
                                )

                                when (dc.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> toAdd.add(entity)
                                    DocumentChange.Type.REMOVED -> toRemove.add(entity)
                                }
                            }

                            if (toAdd.isNotEmpty()) repository.addFamilyMembers(toAdd)
                            toRemove.forEach { repository.deleteFamilyMember(it) }
                        }
                    }
                }

            // 6. Household Invitations listener (scoped strictly to adminUids so rules allow list)
            val invitationsPath = "invitations"
            invitationListenerRegistration = db.collection("invitations")
                .whereEqualTo("householdId", householdId)
                .whereArrayContains("adminUids", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        FirestoreErrorHandler.handleFirestoreError(error, OperationType.LIST, invitationsPath)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        scope.launch {
                            val synced = snapshot.documents.mapNotNull { doc ->
                                val hash = doc.getString("invitationTokenHash") ?: doc.id
                                val createdMs = doc.getTimestamp(
                                    "createdAt",
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )?.toDate()?.time ?: System.currentTimeMillis()
                                val expiresMs = doc.getTimestamp(
                                    "expiresAt",
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )?.toDate()?.time ?: (createdMs + 7L * 24 * 60 * 60 * 1000)
                                val acceptedMs = doc.getTimestamp(
                                    "acceptedAt",
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )?.toDate()?.time
                                val revokedMs = doc.getTimestamp(
                                    "revokedAt",
                                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                                )?.toDate()?.time

                                InvitationEntity(
                                    id = doc.getString("id") ?: hash,
                                    householdId = doc.getString("householdId") ?: householdId,
                                    householdName = doc.getString("householdName") ?: "My Household",
                                    inviterUserId = doc.getString("inviterUserId") ?: "",
                                    inviterName = doc.getString("inviterName") ?: "Family Member",
                                    inviteeEmail = doc.getString("inviteeEmail") ?: "",
                                    inviteeName = doc.getString("inviteeName") ?: "",
                                    personalMessage = doc.getString("personalMessage") ?: "",
                                    invitationTokenHash = hash,
                                    role = doc.getString("role") ?: HouseholdRole.MEMBER.value,
                                    status = doc.getString("status") ?: InvitationStatus.PENDING.value,
                                    createdAt = createdMs,
                                    expiresAt = expiresMs,
                                    acceptedAt = acceptedMs,
                                    acceptedByUserId = doc.getString("acceptedByUserId") ?: "",
                                    revokedAt = revokedMs
                                )
                            }
                            if (synced.isNotEmpty()) {
                                repository.upsertInvitations(synced)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, "households/$householdId")
        }
    }

    fun stopRealtimeSync() {
        householdListenerRegistration?.remove()
        householdListenerRegistration = null
        expenseListenerRegistration?.remove()
        expenseListenerRegistration = null
        incomeListenerRegistration?.remove()
        incomeListenerRegistration = null
        budgetListenerRegistration?.remove()
        budgetListenerRegistration = null
        memberListenerRegistration?.remove()
        memberListenerRegistration = null
        invitationListenerRegistration?.remove()
        invitationListenerRegistration = null
        currentHouseholdId = null
    }

    fun cleanup() {
        stopRealtimeSync()
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (_: Throwable) {
            // Safe cleanup
        }
        scope.cancel()
    }

    fun triggerFullReSync() {
        if (currentHouseholdId == null) return
        scope.launch {
            syncLogCallback?.invoke("Sync active. Pending changes will synchronize automatically.")
        }
    }

    fun syncExpenseToCloud(expense: ExpenseEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${expense.householdId}/expenses/${expense.id}"
        val data = hashMapOf(
            "id" to expense.id,
            "amount" to expense.amount,
            "currencySymbol" to expense.currencySymbol,
            "category" to expense.category,
            "description" to expense.description,
            "paidByMemberId" to expense.paidByMemberId,
            "paidByMemberName" to expense.paidByMemberName,
            "splitType" to expense.splitType,
            "timestamp" to expense.timestamp,
            "householdId" to expense.householdId,
            "paymentMethod" to expense.paymentMethod,
            "note" to expense.note,
            "isRecurring" to expense.isRecurring,
            "recurringFrequency" to expense.recurringFrequency,
            "recurringDayOfMonth" to expense.recurringDayOfMonth,
            "isAutoCreated" to expense.isAutoCreated,
            "isSettled" to expense.isSettled,
            "receiptUri" to expense.receiptUri,
            "tags" to expense.tags
        )

        db.collection("households")
            .document(expense.householdId)
            .collection("expenses")
            .document(expense.id)
            .set(data)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            }
    }

    fun deleteExpenseFromCloud(expense: ExpenseEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${expense.householdId}/expenses/${expense.id}"
        db.collection("households")
            .document(expense.householdId)
            .collection("expenses")
            .document(expense.id)
            .delete()
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            }
    }

    fun syncIncomeToCloud(income: IncomeEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${income.householdId}/incomes/${income.id}"
        val data = hashMapOf(
            "id" to income.id,
            "amount" to income.amount,
            "source" to income.source,
            "category" to income.category,
            "date" to income.date,
            "receivedBy" to income.receivedBy,
            "receivedByName" to income.receivedByName,
            "paymentMethod" to income.paymentMethod,
            "isRecurring" to income.isRecurring,
            "recurringFrequency" to income.recurringFrequency,
            "recurringStartDate" to income.recurringStartDate,
            "note" to income.note,
            "householdId" to income.householdId,
            "currencySymbol" to income.currencySymbol
        )
        db.collection("households")
            .document(income.householdId)
            .collection("incomes")
            .document(income.id)
            .set(data)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            }
    }

    fun deleteIncomeFromCloud(income: IncomeEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${income.householdId}/incomes/${income.id}"
        db.collection("households")
            .document(income.householdId)
            .collection("incomes")
            .document(income.id)
            .delete()
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            }
    }

    fun syncCategoryBudgetToCloud(budget: CategoryBudgetEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val docId = budget.categoryName.replace("/", "_")
        val path = "households/${budget.householdId}/budgets/$docId"
        val data = hashMapOf(
            "categoryName" to budget.categoryName,
            "monthlyLimit" to budget.monthlyLimit,
            "householdId" to budget.householdId,
            "iconName" to budget.iconName,
            "colorHex" to budget.colorHex
        )
        db.collection("households")
            .document(budget.householdId)
            .collection("budgets")
            .document(docId)
            .set(data)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            }
    }

    fun syncFamilyMemberToCloud(member: FamilyMemberEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${member.householdId}/members/${member.id}"
        val data = hashMapOf<String, Any>(
            "id" to member.id,
            "userId" to member.userId,
            "name" to member.name,
            "email" to member.email,
            "role" to member.role,
            "householdRole" to member.householdRole,
            "membershipStatus" to member.membershipStatus,
            "avatarColorHex" to member.avatarColorHex,
            "avatarIcon" to member.avatarIcon,
            "householdId" to member.householdId,
            "monthlyContributionGoal" to member.monthlyContributionGoal,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("households")
            .document(member.householdId)
            .collection("members")
            .document(member.id)
            .set(data)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            }
    }

    fun deleteFamilyMemberFromCloud(member: FamilyMemberEntity) {
        if (currentFirebaseUser() == null) return
        val db = firestore ?: return
        val path = "households/${member.householdId}/members/${member.id}"
        db.collection("households")
            .document(member.householdId)
            .collection("members")
            .document(member.id)
            .delete()
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            }
    }

    fun syncHouseholdToCloud(household: HouseholdEntity) {
        val user = currentFirebaseUser() ?: return
        val db = firestore ?: return
        val path = "households/${household.householdId}"
        val ownerUid = household.ownerUserId.ifBlank { user.uid }
        val data = hashMapOf<String, Any>(
            "householdId" to household.householdId,
            "householdName" to household.householdName,
            "inviteCode" to household.inviteCode,
            "defaultCurrency" to household.defaultCurrency,
            "totalMonthlyBudget" to household.totalMonthlyBudget,
            "lastSyncedTimestamp" to household.lastSyncedTimestamp,
            "isLiveSyncEnabled" to household.isLiveSyncEnabled,
            "ownerUserId" to ownerUid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("households")
            .document(household.householdId)
            .update(data)
            .addOnFailureListener { e ->
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            }
    }

    private fun currentFirebaseUser() = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Throwable) {
        null
    }
}
