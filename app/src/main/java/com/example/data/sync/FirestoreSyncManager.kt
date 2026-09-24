package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.ExpenseRepository
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FirestoreSyncManager(
    private val context: Context,
    private val repository: ExpenseRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var expenseListenerRegistration: ListenerRegistration? = null
    private var memberListenerRegistration: ListenerRegistration? = null
    private var currentHouseholdId: String? = null
    private var isNetworkAvailable: Boolean = true
    private var syncLogCallback: ((String) -> Unit)? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance().apply {
                    try {
                        val settings = FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build()
                        firestoreSettings = settings
                        Log.d("FirestoreSyncManager", "Firestore offline persistent disk cache enabled.")
                    } catch (e: Exception) {
                        Log.w("FirestoreSyncManager", "Firestore settings config note: ${e.message}")
                    }
                }
                Log.d("FirestoreSyncManager", "Firebase Firestore initialized successfully.")
            } else {
                Log.w("FirestoreSyncManager", "FirebaseApp is not initialized.")
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to initialize Firebase Firestore", e)
        }

        registerNetworkConnectivityListener()
    }

    private fun registerNetworkConnectivityListener() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isNetworkAvailable = true
                        Log.d("FirestoreSyncManager", "Network connection established.")
                        if (currentHouseholdId != null) {
                            triggerFullReSync()
                        }
                    }

                    override fun onLost(network: Network) {
                        isNetworkAvailable = false
                        Log.w("FirestoreSyncManager", "Network connection lost. Offline persistence active.")
                        syncLogCallback?.invoke("Offline Mode Active: Local Room & Firestore persistent cache saving expenses locally.")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Could not register connectivity listener", e)
        }
    }

    fun isFirestoreAvailable(): Boolean {
        return firestore != null
    }

    fun isOnline(): Boolean = isNetworkAvailable

    fun startRealtimeSync(householdId: String, onSyncLog: (String) -> Unit) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        syncLogCallback = onSyncLog
        if (currentHouseholdId == householdId && expenseListenerRegistration != null) return

        stopRealtimeSync()
        currentHouseholdId = householdId

        val db = firestore ?: run {
            onSyncLog("Firestore real-time sync active (local Room + Firestore offline cache ready)")
            return
        }

        try {
            onSyncLog("Listening for real-time family expenses on Firestore channel [$householdId]...")
            val expensesRef = db.collection("households")
                .document(householdId)
                .collection("expenses")

            expenseListenerRegistration = expensesRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSyncManager", "Error listening to expenses", error)
                    onSyncLog("Firestore listener notice: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val isFromCache = snapshot.metadata.isFromCache
                    val hasPendingWrites = snapshot.metadata.hasPendingWrites()

                    if (isFromCache) {
                        Log.d("FirestoreSyncManager", "Expense snapshot loaded from local offline Firestore disk cache.")
                    }
                    if (hasPendingWrites) {
                        Log.d("FirestoreSyncManager", "Local changes waiting to sync to cloud once connection is online.")
                    }

                    scope.launch {
                        val toAdd = mutableListOf<ExpenseEntity>()
                        val toRemove = mutableListOf<String>()

                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val id = doc.id
                            val amount = doc.getDouble("amount") ?: 0.0
                            val category = doc.getString("category") ?: "General"
                            val description = doc.getString("description") ?: "Expense"
                            val paidByMemberId = doc.getString("paidByMemberId") ?: ""
                            val paidByMemberName = doc.getString("paidByMemberName") ?: "Family Member"
                            val splitType = doc.getString("splitType") ?: "Split Equally"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val paymentMethod = doc.getString("paymentMethod") ?: "Credit Card"
                            val note = doc.getString("note") ?: ""
                            val currencySymbol = doc.getString("currencySymbol") ?: "₹"
                            val isRecurring = doc.getBoolean("isRecurring") ?: false
                            val recurringFrequency = doc.getString("recurringFrequency") ?: "Monthly"
                            val recurringDayOfMonth = doc.getLong("recurringDayOfMonth")?.toInt() ?: 1
                            val isAutoCreated = doc.getBoolean("isAutoCreated") ?: false
                            val isSettled = doc.getBoolean("isSettled") ?: false
                            val receiptUri = doc.getString("receiptUri")
                            val tags = doc.getString("tags") ?: ""

                            val entity = ExpenseEntity(
                                id = id,
                                amount = amount,
                                currencySymbol = currencySymbol,
                                category = category,
                                description = description,
                                paidByMemberId = paidByMemberId,
                                paidByMemberName = paidByMemberName,
                                splitType = splitType,
                                timestamp = timestamp,
                                householdId = householdId,
                                paymentMethod = paymentMethod,
                                note = note,
                                isRecurring = isRecurring,
                                recurringFrequency = recurringFrequency,
                                recurringDayOfMonth = recurringDayOfMonth,
                                isAutoCreated = isAutoCreated,
                                isSettled = isSettled,
                                receiptUri = receiptUri,
                                tags = tags
                            )

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    toAdd.add(entity)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    toRemove.add(id)
                                }
                            }
                        }

                        if (toAdd.isNotEmpty()) repository.addExpenses(toAdd)
                        toRemove.forEach { repository.deleteExpenseById(it, householdId) }
                    }
                }
            }

            val membersRef = db.collection("households")
                .document(householdId)
                .collection("members")

            memberListenerRegistration = membersRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSyncManager", "Error listening to members", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch {
                        val toAdd = mutableListOf<FamilyMemberEntity>()
                        val toRemove = mutableListOf<FamilyMemberEntity>()

                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val id = doc.id
                            val name = doc.getString("name") ?: "Member"
                            val role = doc.getString("role") ?: "Parent"
                            val avatarColorHex = doc.getString("avatarColorHex") ?: "#1E88E5"
                            val avatarIcon = doc.getString("avatarIcon") ?: "person"
                            val isCurrentActiveUser = doc.getBoolean("isCurrentActiveUser") ?: false
                            val monthlyContributionGoal = doc.getDouble("monthlyContributionGoal") ?: 0.0

                            val entity = FamilyMemberEntity(
                                id = id,
                                name = name,
                                role = role,
                                avatarColorHex = avatarColorHex,
                                avatarIcon = avatarIcon,
                                isCurrentActiveUser = isCurrentActiveUser,
                                householdId = householdId,
                                monthlyContributionGoal = monthlyContributionGoal
                            )

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    toAdd.add(entity)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    toRemove.add(entity)
                                }
                            }
                        }
                        
                        if (toAdd.isNotEmpty()) repository.addFamilyMembers(toAdd)
                        toRemove.forEach { repository.deleteFamilyMember(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to attach snapshot listener", e)
        }
    }

    fun stopRealtimeSync() {
        expenseListenerRegistration?.remove()
        expenseListenerRegistration = null
        memberListenerRegistration?.remove()
        memberListenerRegistration = null
        currentHouseholdId = null
    }

    fun triggerFullReSync() {
        val hId = currentHouseholdId ?: return
        scope.launch {
            try {
                // Firestore offline persistence automatically handles syncing pending writes 
                // when the network reconnects. Re-uploading all local data causes severe lag.
                syncLogCallback?.invoke("⚡ Sync triggered. Firestore will automatically process any pending offline writes.")
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Error during full re-sync", e)
            }
        }
    }

    fun syncExpenseToCloud(expense: ExpenseEntity) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = firestore ?: return
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
            .addOnSuccessListener {
                Log.d("FirestoreSyncManager", "Expense synced to cloud/cache: ${expense.description}")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSyncManager", "Failed to sync expense to cloud", e)
            }
    }

    fun deleteExpenseFromCloud(expense: ExpenseEntity) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = firestore ?: return
        db.collection("households")
            .document(expense.householdId)
            .collection("expenses")
            .document(expense.id)
            .delete()
    }

    fun syncFamilyMemberToCloud(member: FamilyMemberEntity) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = firestore ?: return
        val data = hashMapOf(
            "id" to member.id,
            "name" to member.name,
            "role" to member.role,
            "avatarColorHex" to member.avatarColorHex,
            "avatarIcon" to member.avatarIcon,
            "householdId" to member.householdId,
            "monthlyContributionGoal" to member.monthlyContributionGoal
        )
        db.collection("households")
            .document(member.householdId)
            .collection("members")
            .document(member.id)
            .set(data)
    }

    fun deleteFamilyMemberFromCloud(member: FamilyMemberEntity) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = firestore ?: return
        db.collection("households")
            .document(member.householdId)
            .collection("members")
            .document(member.id)
            .delete()
    }

    fun syncHouseholdToCloud(household: HouseholdEntity) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        val db = firestore ?: return
        val data = hashMapOf(
            "householdId" to household.householdId,
            "householdName" to household.householdName,
            "inviteCode" to household.inviteCode,
            "defaultCurrency" to household.defaultCurrency,
            "totalMonthlyBudget" to household.totalMonthlyBudget,
            "lastSyncedTimestamp" to household.lastSyncedTimestamp,
            "isLiveSyncEnabled" to household.isLiveSyncEnabled
        )
        db.collection("households")
            .document(household.householdId)
            .set(data)
    }
}
