package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuthManager
import com.example.data.AuthUser
import com.example.data.ExpenseRepository
import com.example.data.GoogleSignInResult
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdRole
import com.example.data.model.IncomeEntity
import com.example.data.model.InvitationEntity
import com.example.data.model.InvitationStatus
import com.example.data.model.MembershipStatus
import com.example.data.repository.FamilyInvitationRepository
import com.example.data.repository.FirestoreInvitationCloudBackend
import com.example.data.repository.InvitationAcceptResult
import com.example.data.repository.InvitationActionResult
import com.example.data.repository.InvitationCreateResult
import com.example.data.repository.InvitationValidationResult
import com.example.data.repository.MemberManagementResult
import com.example.data.sync.FirestoreSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BudgetAlertLevel {
    NONE, WARNING_80, EXCEEDED_100
}

data class BudgetNotification(
    val level: BudgetAlertLevel = BudgetAlertLevel.NONE,
    val percent: Int = 0,
    val spentAmount: Double = 0.0,
    val budgetLimit: Double = 0.0,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

sealed class InvitationLandingState {
    data object Hidden : InvitationLandingState()
    data object Loading : InvitationLandingState()
    data class RequiresSignIn(val rawToken: String) : InvitationLandingState()
    data class Ready(
        val invitation: InvitationEntity,
        val rawToken: String,
        val isAccepting: Boolean = false,
        val errorMessage: String? = null
    ) : InvitationLandingState()

    data class AcceptedSuccess(
        val householdName: String,
        val role: HouseholdRole
    ) : InvitationLandingState()

    data class Error(
        val title: String,
        val message: String
    ) : InvitationLandingState()
}

data class UiState(
    val householdId: String = "FAM-1001",
    val householdName: String = "My Household",
    val inviteCode: String = "FAM-1001",
    val currencySymbol: String = "₹",
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val incomes: List<IncomeEntity> = emptyList(),
    val totalIncomeThisMonth: Double = 0.0,
    val transactionTypeFilter: String = "All",
    val members: List<FamilyMemberEntity> = emptyList(),
    val activeMember: FamilyMemberEntity? = null,
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList(),
    val household: HouseholdEntity? = null,
    val selectedCategoryFilter: String? = null,
    val selectedMemberFilter: String? = null,
    val selectedDateRangeFilter: String? = null,
    val selectedPaymentMethodFilter: String? = null,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val isLiveSyncEnabled: Boolean = true,
    val syncStatusMessage: String = "Sign in with Google for multi-device sync",
    val syncLog: List<String> = listOf("FamSpend household ready."),
    val totalSpentToday: Double = 0.0,
    val totalSpentThisMonth: Double = 0.0,
    val monthlyBudgetLimit: Double = 3000.0,
    val dailySpendingLimit: Double = 0.0,
    val budgetNotification: BudgetNotification? = null,
    val isBudgetAlertDismissed: Boolean = false,
    val lastNotifiedThreshold: Int = 0,
    val isAddExpenseDialogOpen: Boolean = false,
    val isAddIncomeDialogOpen: Boolean = false,
    val isTransferDialogOpen: Boolean = false,
    val isScanReceiptDialogOpen: Boolean = false,
    val isSplitExpenseDialogOpen: Boolean = false,
    val transferSuccessToast: String? = null,
    val incomeSuccessToast: String? = null,
    val isFabMenuOpen: Boolean = false,
    val isAddMemberDialogOpen: Boolean = false,
    val isEditBudgetDialogOpen: Boolean = false,
    val isJoinHouseholdDialogOpen: Boolean = false,
    val isMonthlySummaryDialogOpen: Boolean = false,
    val isCurrencySettingsDialogOpen: Boolean = false,
    val isExportCsvDialogOpen: Boolean = false,
    val isEditMemberDialogOpen: Boolean = false,
    val editingExpense: ExpenseEntity? = null,
    val viewingExpense: ExpenseEntity? = null,
    val editingMember: FamilyMemberEntity? = null,
    val isDarkMode: Boolean = false,
    val selectedExpenseIds: Set<String> = emptySet(),

    // Unified Family Invitation & Member Management State
    val authUser: AuthUser? = null,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val currentUserHouseholdRole: HouseholdRole = HouseholdRole.OWNER,
    val invitations: List<InvitationEntity> = emptyList(),
    val isInvitationsLoading: Boolean = false,
    val isInviteSheetOpen: Boolean = false,
    val isMemberManagementSheetOpen: Boolean = false,
    val inviteSheetSubmitting: Boolean = false,
    val inviteSheetError: String? = null,
    val lastCreatedInvitation: InvitationEntity? = null,
    val lastCreatedInviteLink: String? = null,
    val lastInviteEmailDeliveryConfigured: Boolean = false,
    val invitationFeedbackMessage: String? = null,
    val pendingInvitationToken: String? = null,
    val invitationLandingState: InvitationLandingState = InvitationLandingState.Hidden
)

class FamilyExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getDatabase(application)
    private val repository: ExpenseRepository
    private val firestoreSyncManager: FirestoreSyncManager
    private val invitationRepository: FamilyInvitationRepository
    private val authManager: AuthManager = AuthManager(application)
    private var isFirebaseAuthenticated = false
    private val prefs = application.getSharedPreferences("fam_spend_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val initialDarkMode = prefs.getBoolean("is_dark_mode", false)
        val initialDailyLimit = prefs.getFloat("daily_spending_limit", 0f).toDouble()
        val savedHouseholdId = prefs.getString("active_household_id", "FAM-1001")
            ?.takeIf { it.isNotBlank() } ?: "FAM-1001"
        val persistedPendingToken = prefs.getString("pending_invitation_token", null)
            ?.takeIf { it.isNotBlank() }

        _uiState.value = _uiState.value.copy(
            householdId = savedHouseholdId,
            inviteCode = savedHouseholdId,
            isDarkMode = initialDarkMode,
            dailySpendingLimit = initialDailyLimit,
            pendingInvitationToken = persistedPendingToken
        )

        repository = ExpenseRepository(
            database.expenseDao(),
            database.familyMemberDao(),
            database.categoryBudgetDao(),
            database.householdDao(),
            database.incomeDao(),
            database.invitationDao()
        )
        firestoreSyncManager = FirestoreSyncManager(application, repository)
        invitationRepository = FamilyInvitationRepository(
            invitationDao = database.invitationDao(),
            familyMemberDao = database.familyMemberDao(),
            householdDao = database.householdDao(),
            cloudBackend = FirestoreInvitationCloudBackend { firestoreSyncManager.getFirestoreInstance() }
        )

        viewModelScope.launch {
            authManager.authUser.collect { user ->
                _uiState.value = _uiState.value.copy(authUser = user)
                if (user != null) {
                    isFirebaseAuthenticated = true
                    onUserAuthenticated(user)
                } else {
                    isFirebaseAuthenticated = false
                    firestoreSyncManager.stopRealtimeSync()
                    _uiState.value = _uiState.value.copy(
                        syncStatusMessage = "Sign in with Google for multi-device sync"
                    )
                    addSyncLog("Offline Mode: Sign in with Google to sync with family.")
                }
            }
        }

        observeData()

        if (com.example.util.WeeklyReminderScheduler.isReminderEnabled(application)) {
            com.example.util.WeeklyReminderScheduler.scheduleWeeklyReminder(application)
        }
    }

    private suspend fun onUserAuthenticated(user: AuthUser) {
        val pendingToken = _uiState.value.pendingInvitationToken
        if (!pendingToken.isNullOrBlank()) {
            // Preserve and immediately validate pending invitation link after sign-in
            validateIncomingInvitation(pendingToken)
            return
        }

        val currentState = _uiState.value
        val localHousehold = currentState.household ?: HouseholdEntity(
            householdId = currentState.householdId,
            householdName = currentState.householdName,
            inviteCode = currentState.inviteCode,
            defaultCurrency = currentState.currencySymbol,
            totalMonthlyBudget = currentState.monthlyBudgetLimit,
            isLiveSyncEnabled = currentState.isLiveSyncEnabled
        )

        _uiState.value = _uiState.value.copy(
            isSyncing = true,
            syncStatusMessage = "Connecting to cloud household..."
        )

        val bootstrapResult = invitationRepository.bootstrapAuthenticatedHousehold(localHousehold, user)
        bootstrapResult.onSuccess { resolvedHousehold ->
            val oldId = currentState.householdId
            val newId = resolvedHousehold.householdId

            // If user just migrated from local default FAM-1001 into their own cloud household,
            // carry over local expenses/incomes/budgets so zero user data is lost.
            if (oldId == "FAM-1001" && newId != oldId && resolvedHousehold.ownerUserId == user.uid) {
                migrateLocalDataToCloudHousehold(oldId, newId, user)
            }

            prefs.edit().putString("active_household_id", newId).apply()
            _uiState.value = _uiState.value.copy(
                householdId = newId,
                householdName = resolvedHousehold.householdName,
                inviteCode = resolvedHousehold.inviteCode,
                household = resolvedHousehold,
                isSyncing = false,
                syncStatusMessage = "Firebase Firestore Live Sync • Connected"
            )
            startObservingHousehold(newId)
        }.onFailure { err ->
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Local mode active • Cloud sync pending"
            )
            addSyncLog("Cloud household setup pending: ${err.localizedMessage ?: "offline"}")
            firestoreSyncManager.startRealtimeSync(currentState.householdId) { addSyncLog(it) }
        }
    }

    private suspend fun migrateLocalDataToCloudHousehold(
        oldHouseholdId: String,
        newHouseholdId: String,
        owner: AuthUser
    ) {
        val localExpenses = repository.getExpenses(oldHouseholdId).firstOrNull().orEmpty()
        localExpenses.forEach { expense ->
            val migrated = expense.copy(householdId = newHouseholdId)
            repository.addExpense(migrated)
            firestoreSyncManager.syncExpenseToCloud(migrated)
        }
        val localIncomes = repository.getIncomes(oldHouseholdId).firstOrNull().orEmpty()
        localIncomes.forEach { income ->
            val migrated = income.copy(householdId = newHouseholdId)
            repository.addIncome(migrated)
            firestoreSyncManager.syncIncomeToCloud(migrated)
        }
        val localBudgets = repository.getBudgets(oldHouseholdId).firstOrNull().orEmpty()
        localBudgets.forEach { budget ->
            val migrated = budget.copy(householdId = newHouseholdId)
            repository.setCategoryBudget(migrated)
            firestoreSyncManager.syncCategoryBudgetToCloud(migrated)
        }
        val ownerMember = FamilyMemberEntity(
            id = owner.uid,
            name = owner.displayName,
            role = "Primary",
            avatarColorHex = "#4F46E5",
            avatarIcon = "person",
            isCurrentActiveUser = true,
            householdId = newHouseholdId,
            email = owner.email.trim().lowercase(Locale.US),
            userId = owner.uid,
            householdRole = HouseholdRole.OWNER.value,
            membershipStatus = MembershipStatus.ACTIVE.value,
            joinedAt = System.currentTimeMillis()
        )
        repository.addFamilyMember(ownerMember)
        repository.setActiveMember(newHouseholdId, ownerMember.id)
    }

    private var observationJob: Job? = null

    private fun observeData() {
        startObservingHousehold(_uiState.value.householdId)
    }

    private fun startObservingHousehold(hId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch(Dispatchers.IO) {
            firestoreSyncManager.stopRealtimeSync()
            if (isFirebaseAuthenticated) {
                firestoreSyncManager.startRealtimeSync(hId) { logMsg ->
                    addSyncLog(logMsg)
                }
            }

            launch {
                repository.getHousehold(hId).collect { household ->
                    if (household != null) {
                        _uiState.value = _uiState.value.copy(
                            household = household,
                            householdName = household.householdName,
                            inviteCode = household.inviteCode,
                            currencySymbol = household.defaultCurrency,
                            monthlyBudgetLimit = household.totalMonthlyBudget,
                            isLiveSyncEnabled = household.isLiveSyncEnabled
                        )
                    }
                }
            }

            launch {
                repository.getMembers(hId).collect { membersList ->
                    val currentUid = _uiState.value.authUser?.uid
                    val matchedAuthMember = if (!currentUid.isNullOrBlank()) {
                        membersList.find { it.userId == currentUid || it.id == currentUid }
                    } else {
                        null
                    }
                    val active = membersList.find { it.isCurrentActiveUser }
                        ?: matchedAuthMember
                        ?: membersList.firstOrNull()

                    val resolvedRole = when {
                        matchedAuthMember != null -> matchedAuthMember.parsedHouseholdRole()
                        _uiState.value.household?.ownerUserId == currentUid && !currentUid.isNullOrBlank() ->
                            HouseholdRole.OWNER
                        active != null -> active.parsedHouseholdRole()
                        else -> HouseholdRole.OWNER
                    }

                    _uiState.value = _uiState.value.copy(
                        members = membersList,
                        activeMember = active,
                        currentUserHouseholdRole = resolvedRole
                    )
                }
            }

            launch {
                invitationRepository.observeInvitations(hId).collect { invitationsList ->
                    _uiState.value = _uiState.value.copy(
                        invitations = invitationsList,
                        isInvitationsLoading = false
                    )
                }
            }

            launch {
                repository.getBudgets(hId).collect { budgets ->
                    _uiState.value = _uiState.value.copy(categoryBudgets = budgets)
                }
            }

            launch {
                repository.getExpenses(hId).collect { expensesList ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = System.currentTimeMillis()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfDay = cal.timeInMillis

                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val startOfMonth = cal.timeInMillis

                    var spentToday = 0.0
                    var spentMonth = 0.0

                    for (expense in expensesList) {
                        val t = expense.timestamp
                        if (t >= startOfMonth) {
                            spentMonth += expense.amount
                            if (t >= startOfDay) {
                                spentToday += expense.amount
                            }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        expenses = expensesList,
                        totalSpentToday = spentToday,
                        totalSpentThisMonth = spentMonth
                    )
                    applyFilters()
                    checkAndAutoCreateRecurringExpenses(expensesList)
                    checkBudgetAlerts(spentMonth, _uiState.value.monthlyBudgetLimit, _uiState.value.currencySymbol)
                    checkDailyBudgetAlert(spentToday, _uiState.value.dailySpendingLimit, _uiState.value.currencySymbol)
                }
            }

            launch {
                repository.getIncomes(hId).collect { incomesList ->
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = cal.timeInMillis

                    val monthIncome = incomesList.filter { it.date >= startOfMonth }.sumOf { it.amount }

                    _uiState.value = _uiState.value.copy(
                        incomes = incomesList,
                        totalIncomeThisMonth = monthIncome
                    )
                }
            }
        }
    }

    // =========================================================================
    // Unified Family Invitation & Member Management Workflows
    // =========================================================================

    fun openInviteFamilySheet() {
        _uiState.value = _uiState.value.copy(
            isInviteSheetOpen = true,
            inviteSheetError = null,
            inviteSheetSubmitting = false,
            lastCreatedInvitation = null,
            lastCreatedInviteLink = null
        )
    }

    fun dismissInviteFamilySheet() {
        _uiState.value = _uiState.value.copy(
            isInviteSheetOpen = false,
            inviteSheetError = null,
            inviteSheetSubmitting = false,
            lastCreatedInvitation = null,
            lastCreatedInviteLink = null
        )
    }

    fun openMemberManagementSheet() {
        _uiState.value = _uiState.value.copy(
            isMemberManagementSheetOpen = true,
            invitationFeedbackMessage = null
        )
    }

    fun dismissMemberManagementSheet() {
        _uiState.value = _uiState.value.copy(
            isMemberManagementSheetOpen = false,
            invitationFeedbackMessage = null
        )
    }

    fun clearInvitationFeedback() {
        _uiState.value = _uiState.value.copy(invitationFeedbackMessage = null)
    }

    fun clearInviteSheetError() {
        _uiState.value = _uiState.value.copy(inviteSheetError = null)
    }

    fun sendEmailInvitation(
        email: String,
        memberName: String,
        personalMessage: String,
        role: HouseholdRole = HouseholdRole.MEMBER
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            _uiState.value = state.copy(
                inviteSheetSubmitting = true,
                inviteSheetError = null,
                lastCreatedInvitation = null,
                lastCreatedInviteLink = null
            )

            val result = invitationRepository.createInvitation(
                householdId = state.householdId,
                householdName = state.householdName,
                inviter = state.authUser,
                inviterRole = state.currentUserHouseholdRole,
                inviteeEmail = email,
                inviteeName = memberName,
                personalMessage = personalMessage,
                invitedRole = role
            )

            when (result) {
                is InvitationCreateResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        inviteSheetSubmitting = false,
                        inviteSheetError = null,
                        lastCreatedInvitation = result.invitation,
                        lastCreatedInviteLink = result.shareLink,
                        lastInviteEmailDeliveryConfigured = result.emailDeliveryConfigured
                    )
                    addSyncLog("Created invitation for ${result.invitation.inviteeEmail}")
                }
                is InvitationCreateResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        inviteSheetSubmitting = false,
                        inviteSheetError = result.message,
                        lastCreatedInvitation = null,
                        lastCreatedInviteLink = null
                    )
                }
            }
        }
    }

    fun generateShareableInvitationLink(
        role: HouseholdRole = HouseholdRole.MEMBER,
        personalMessage: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            _uiState.value = state.copy(
                inviteSheetSubmitting = true,
                inviteSheetError = null,
                lastCreatedInvitation = null,
                lastCreatedInviteLink = null
            )

            val result = invitationRepository.createInvitation(
                householdId = state.householdId,
                householdName = state.householdName,
                inviter = state.authUser,
                inviterRole = state.currentUserHouseholdRole,
                inviteeEmail = null,
                inviteeName = "",
                personalMessage = personalMessage,
                invitedRole = role
            )

            when (result) {
                is InvitationCreateResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        inviteSheetSubmitting = false,
                        inviteSheetError = null,
                        lastCreatedInvitation = result.invitation,
                        lastCreatedInviteLink = result.shareLink,
                        lastInviteEmailDeliveryConfigured = result.emailDeliveryConfigured
                    )
                    addSyncLog("Generated secure single-use invitation link for ${state.householdName}")
                }
                is InvitationCreateResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        inviteSheetSubmitting = false,
                        inviteSheetError = result.message,
                        lastCreatedInvitation = null,
                        lastCreatedInviteLink = null
                    )
                }
            }
        }
    }

    fun getCachedInvitationLink(invitationId: String): String? {
        return invitationRepository.getCachedShareLink(invitationId)
    }

    fun resendInvitation(
        invitationId: String,
        onLinkReady: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            when (
                val result = invitationRepository.resendInvitation(
                    invitationId = invitationId,
                    caller = state.authUser,
                    callerRole = state.currentUserHouseholdRole
                )
            ) {
                is InvitationActionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        invitationFeedbackMessage = result.message
                    )
                    addSyncLog(result.message)
                    result.updatedShareLink?.let { link ->
                        launch(Dispatchers.Main) { onLinkReady(link) }
                    }
                }
                is InvitationActionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        invitationFeedbackMessage = result.message
                    )
                }
            }
        }
    }

    fun revokeInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            when (
                val result = invitationRepository.revokeInvitation(
                    invitationId = invitationId,
                    caller = state.authUser,
                    callerRole = state.currentUserHouseholdRole
                )
            ) {
                is InvitationActionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        invitationFeedbackMessage = result.message
                    )
                    addSyncLog("Revoked pending family invitation.")
                }
                is InvitationActionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        invitationFeedbackMessage = result.message
                    )
                }
            }
        }
    }

    fun updateMemberHouseholdRole(member: FamilyMemberEntity, newRole: HouseholdRole) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            when (
                val result = invitationRepository.updateMemberRole(
                    householdId = state.householdId,
                    targetMember = member,
                    newRole = newRole,
                    callerUserId = state.authUser?.uid,
                    callerRole = state.currentUserHouseholdRole
                )
            ) {
                is MemberManagementResult.Success -> {
                    _uiState.value = _uiState.value.copy(invitationFeedbackMessage = result.message)
                    addSyncLog(result.message)
                }
                is MemberManagementResult.Error -> {
                    _uiState.value = _uiState.value.copy(invitationFeedbackMessage = result.message)
                }
            }
        }
    }

    fun removeHouseholdMember(member: FamilyMemberEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            when (
                val result = invitationRepository.removeMember(
                    householdId = state.householdId,
                    targetMember = member,
                    callerUserId = state.authUser?.uid,
                    callerRole = state.currentUserHouseholdRole
                )
            ) {
                is MemberManagementResult.Success -> {
                    _uiState.value = _uiState.value.copy(invitationFeedbackMessage = result.message)
                    addSyncLog(result.message)
                    if (_uiState.value.activeMember?.id == member.id) {
                        val remaining = repository.getMembers(state.householdId).firstOrNull()
                        remaining?.firstOrNull()?.let { switchActiveMember(it.id) }
                    }
                }
                is MemberManagementResult.Error -> {
                    _uiState.value = _uiState.value.copy(invitationFeedbackMessage = result.message)
                }
            }
        }
    }

    // =========================================================================
    // Deep-Link / Recipient Invitation Acceptance Workflow
    // =========================================================================

    fun handleIncomingInvitationLink(rawLinkOrToken: String) {
        val clean = rawLinkOrToken.trim()
        if (clean.isEmpty()) return

        // Persist token securely in app-private prefs so it survives auth redirect/recreation
        prefs.edit().putString("pending_invitation_token", clean).apply()
        _uiState.value = _uiState.value.copy(
            pendingInvitationToken = clean,
            isJoinHouseholdDialogOpen = false,
            invitationLandingState = InvitationLandingState.Loading
        )

        viewModelScope.launch(Dispatchers.IO) {
            validateIncomingInvitation(clean)
        }
    }

    private suspend fun validateIncomingInvitation(rawLinkOrToken: String) {
        val user = _uiState.value.authUser
        when (val validation = invitationRepository.validateInvitationToken(rawLinkOrToken, user)) {
            is InvitationValidationResult.RequiresSignIn -> {
                _uiState.value = _uiState.value.copy(
                    pendingInvitationToken = validation.rawToken,
                    invitationLandingState = InvitationLandingState.RequiresSignIn(validation.rawToken)
                )
            }
            is InvitationValidationResult.Valid -> {
                _uiState.value = _uiState.value.copy(
                    pendingInvitationToken = validation.rawToken,
                    invitationLandingState = InvitationLandingState.Ready(
                        invitation = validation.invitation,
                        rawToken = validation.rawToken
                    )
                )
            }
            is InvitationValidationResult.Expired -> {
                clearPendingInvitationToken()
                _uiState.value = _uiState.value.copy(
                    invitationLandingState = InvitationLandingState.Error(
                        title = "Invitation Expired",
                        message = validation.message
                    )
                )
            }
            is InvitationValidationResult.Revoked -> {
                clearPendingInvitationToken()
                _uiState.value = _uiState.value.copy(
                    invitationLandingState = InvitationLandingState.Error(
                        title = "Invitation Unavailable",
                        message = validation.message
                    )
                )
            }
            is InvitationValidationResult.AlreadyUsed -> {
                clearPendingInvitationToken()
                _uiState.value = _uiState.value.copy(
                    invitationLandingState = InvitationLandingState.Error(
                        title = "Invitation Already Used",
                        message = validation.message
                    )
                )
            }
            is InvitationValidationResult.Invalid -> {
                clearPendingInvitationToken()
                _uiState.value = _uiState.value.copy(
                    invitationLandingState = InvitationLandingState.Error(
                        title = "Invalid Invitation",
                        message = validation.message
                    )
                )
            }
        }
    }

    fun acceptPendingInvitation() {
        val currentLanding = _uiState.value.invitationLandingState as? InvitationLandingState.Ready ?: return
        if (currentLanding.isAccepting) return

        _uiState.value = _uiState.value.copy(
            invitationLandingState = currentLanding.copy(isAccepting = true, errorMessage = null)
        )

        viewModelScope.launch(Dispatchers.IO) {
            when (
                val result = invitationRepository.acceptInvitation(
                    rawToken = currentLanding.rawToken,
                    currentUser = _uiState.value.authUser
                )
            ) {
                is InvitationAcceptResult.Accepted -> {
                    clearPendingInvitationToken()
                    prefs.edit().putString("active_household_id", result.householdId).apply()
                    _uiState.value = _uiState.value.copy(
                        householdId = result.householdId,
                        householdName = result.householdName,
                        inviteCode = result.householdId,
                        currentUserHouseholdRole = result.role,
                        syncStatusMessage = "Firebase Firestore Live Sync • Connected",
                        invitationLandingState = InvitationLandingState.AcceptedSuccess(
                            householdName = result.householdName,
                            role = result.role
                        )
                    )
                    addSyncLog("Welcome to ${result.householdName}! Real-time sync active.")
                    startObservingHousehold(result.householdId)
                }
                InvitationAcceptResult.RequiresSignIn -> {
                    _uiState.value = _uiState.value.copy(
                        invitationLandingState = InvitationLandingState.RequiresSignIn(currentLanding.rawToken)
                    )
                }
                is InvitationAcceptResult.EmailMismatch -> {
                    _uiState.value = _uiState.value.copy(
                        invitationLandingState = currentLanding.copy(
                            isAccepting = false,
                            errorMessage = result.expectedEmail
                        )
                    )
                }
                is InvitationAcceptResult.AlreadyMember -> {
                    clearPendingInvitationToken()
                    _uiState.value = _uiState.value.copy(
                        invitationLandingState = InvitationLandingState.Error(
                            title = "Already a Member",
                            message = result.message
                        )
                    )
                }
                is InvitationAcceptResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        invitationLandingState = currentLanding.copy(
                            isAccepting = false,
                            errorMessage = result.message
                        )
                    )
                }
            }
        }
    }

    fun declinePendingInvitation() {
        clearPendingInvitationToken()
        _uiState.value = _uiState.value.copy(
            invitationLandingState = InvitationLandingState.Hidden
        )
        addSyncLog("Declined household invitation.")
    }

    fun dismissInvitationLanding() {
        clearPendingInvitationToken()
        _uiState.value = _uiState.value.copy(
            invitationLandingState = InvitationLandingState.Hidden
        )
    }

    private fun clearPendingInvitationToken() {
        prefs.edit().remove("pending_invitation_token").apply()
        _uiState.value = _uiState.value.copy(pendingInvitationToken = null)
    }

    // =========================================================================
    // Existing App Features (Preserved Intact)
    // =========================================================================

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.expenses

        if (!state.selectedCategoryFilter.isNullOrBlank()) {
            list = list.filter { it.category.equals(state.selectedCategoryFilter, ignoreCase = true) }
        }

        if (!state.selectedMemberFilter.isNullOrBlank()) {
            list = list.filter { it.paidByMemberId == state.selectedMemberFilter }
        }

        if (!state.selectedPaymentMethodFilter.isNullOrBlank()) {
            list = list.filter { it.paymentMethod.equals(state.selectedPaymentMethodFilter, ignoreCase = true) }
        }

        if (!state.selectedDateRangeFilter.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            list = when (state.selectedDateRangeFilter) {
                "TODAY" -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfDay = cal.timeInMillis
                    list.filter { it.timestamp >= startOfDay }
                }
                "WEEK" -> {
                    val weekAgo = now - (7L * 24 * 60 * 60 * 1000)
                    list.filter { it.timestamp >= weekAgo }
                }
                "MONTH" -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = cal.timeInMillis
                    list.filter { it.timestamp >= startOfMonth }
                }
                "LAST_30_DAYS" -> {
                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                    list.filter { it.timestamp >= thirtyDaysAgo }
                }
                else -> list
            }
        }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter {
                it.description.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.paidByMemberName.lowercase().contains(q) ||
                    it.note.lowercase().contains(q) ||
                    it.amount.toString().contains(q)
            }
        }

        _uiState.value = _uiState.value.copy(filteredExpenses = list)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setCategoryFilter(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
        applyFilters()
    }

    fun setDateRangeFilter(dateRange: String?) {
        _uiState.value = _uiState.value.copy(selectedDateRangeFilter = dateRange)
        applyFilters()
    }

    fun setMemberFilter(memberId: String?) {
        _uiState.value = _uiState.value.copy(selectedMemberFilter = memberId)
        applyFilters()
    }

    fun setPaymentMethodFilter(method: String?) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethodFilter = method)
        applyFilters()
    }

    fun switchActiveMember(memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveMember(_uiState.value.householdId, memberId)
            val member = _uiState.value.members.find { it.id == memberId }
            val logMessage = "Switched current device user to ${member?.name ?: "Member"}"
            addSyncLog(logMessage)
        }
    }

    fun saveExpense(
        amount: Double,
        category: String,
        description: String,
        paidByMemberId: String,
        paidByMemberName: String,
        splitType: String,
        paymentMethod: String,
        note: String,
        isRecurring: Boolean = false,
        recurringFrequency: String = "Monthly",
        recurringDayOfMonth: Int = 1,
        timestamp: Long = System.currentTimeMillis(),
        receiptUri: String? = null,
        tags: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val editing = state.editingExpense

            if (editing != null) {
                val updated = editing.copy(
                    amount = amount,
                    currencySymbol = state.currencySymbol,
                    category = category,
                    description = description,
                    paidByMemberId = paidByMemberId,
                    paidByMemberName = paidByMemberName,
                    splitType = splitType,
                    paymentMethod = paymentMethod,
                    note = note,
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency,
                    recurringDayOfMonth = recurringDayOfMonth,
                    timestamp = timestamp,
                    receiptUri = receiptUri ?: editing.receiptUri,
                    tags = tags.ifBlank { editing.tags }
                )
                repository.updateExpense(updated)
                firestoreSyncManager.syncExpenseToCloud(updated)
                val recurringText = if (isRecurring) " [Recurring $recurringFrequency on Day $recurringDayOfMonth]" else ""
                addSyncLog("Updated expense: $description (${state.currencySymbol}$amount)$recurringText")
            } else {
                val newExp = ExpenseEntity(
                    amount = amount,
                    currencySymbol = state.currencySymbol,
                    category = category,
                    description = description,
                    paidByMemberId = paidByMemberId,
                    paidByMemberName = paidByMemberName,
                    splitType = splitType,
                    timestamp = timestamp,
                    householdId = state.householdId,
                    paymentMethod = paymentMethod,
                    note = note,
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency,
                    recurringDayOfMonth = recurringDayOfMonth,
                    receiptUri = receiptUri,
                    tags = tags
                )
                repository.addExpense(newExp)
                firestoreSyncManager.syncExpenseToCloud(newExp)
                val recurringText = if (isRecurring) " [Recurring $recurringFrequency on Day $recurringDayOfMonth]" else ""
                addSyncLog("Added expense: $description (${state.currencySymbol}$amount) by $paidByMemberName$recurringText")
            }

            _uiState.value = _uiState.value.copy(
                isAddExpenseDialogOpen = false,
                editingExpense = null
            )
        }
    }

    fun duplicateExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = expense.copy(
                id = java.util.UUID.randomUUID().toString(),
                description = "${expense.description} (Copy)",
                timestamp = System.currentTimeMillis()
            )
            repository.addExpense(duplicate)
            firestoreSyncManager.syncExpenseToCloud(duplicate)
            addSyncLog("Duplicated expense: ${duplicate.description}")
        }
    }

    fun toggleExpenseSettled(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = expense.copy(isSettled = !expense.isSettled)
            repository.updateExpense(updated)
            firestoreSyncManager.syncExpenseToCloud(updated)
            _uiState.value = _uiState.value.copy(
                viewingExpense = if (_uiState.value.viewingExpense?.id == expense.id) updated else _uiState.value.viewingExpense
            )
            addSyncLog("Marked expense as ${if (updated.isSettled) "Settled" else "Pending"}: ${expense.description}")
        }
    }

    fun triggerAutoCreateRecurringExpenses() {
        checkAndAutoCreateRecurringExpenses(_uiState.value.expenses)
    }

    private fun checkAndAutoCreateRecurringExpenses(expensesList: List<ExpenseEntity>) {
        if (expensesList.isEmpty()) return

        val recurringTemplates = expensesList.filter { it.isRecurring && !it.isAutoCreated }
        if (recurringTemplates.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val sdfMonth = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val currentMonthStr = sdfMonth.format(cal.time)

        for (template in recurringTemplates) {
            val targetDay = template.recurringDayOfMonth.coerceIn(1, 31)
            val templateMonthStr = sdfMonth.format(Date(template.timestamp))

            if (templateMonthStr == currentMonthStr) continue
            if (template.timestamp > cal.timeInMillis) continue

            val alreadyCreatedThisMonth = expensesList.any { exp ->
                exp.isAutoCreated &&
                    exp.description.equals(template.description, ignoreCase = true) &&
                    exp.category.equals(template.category, ignoreCase = true) &&
                    sdfMonth.format(Date(exp.timestamp)) == currentMonthStr
            }

            if (!alreadyCreatedThisMonth && currentDayOfMonth >= targetDay) {
                viewModelScope.launch(Dispatchers.IO) {
                    val autoCal = Calendar.getInstance()
                    val maxDaysInMonth = autoCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    autoCal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDaysInMonth))

                    val autoExpense = ExpenseEntity(
                        amount = template.amount,
                        currencySymbol = template.currencySymbol,
                        category = template.category,
                        description = template.description,
                        paidByMemberId = template.paidByMemberId,
                        paidByMemberName = template.paidByMemberName,
                        splitType = template.splitType,
                        timestamp = autoCal.timeInMillis,
                        householdId = template.householdId,
                        paymentMethod = template.paymentMethod,
                        note = "Auto-generated recurring bill (${template.recurringFrequency} on Day $targetDay)",
                        isRecurring = true,
                        recurringFrequency = template.recurringFrequency,
                        recurringDayOfMonth = targetDay,
                        isAutoCreated = true
                    )
                    repository.addExpense(autoExpense)
                    firestoreSyncManager.syncExpenseToCloud(autoExpense)
                    addSyncLog("Auto-logged recurring bill: ${template.description} (${template.currencySymbol}${String.format(Locale.US, "%.2f", template.amount)}) set for Day $targetDay")
                }
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
            firestoreSyncManager.deleteExpenseFromCloud(expense)
            addSyncLog("Deleted expense: ${expense.description} (${expense.currencySymbol}${expense.amount})")
        }
    }

    fun toggleExpenseSelection(expenseId: String) {
        val currentSelected = _uiState.value.selectedExpenseIds
        val newSelected = if (currentSelected.contains(expenseId)) {
            currentSelected - expenseId
        } else {
            currentSelected + expenseId
        }
        _uiState.value = _uiState.value.copy(selectedExpenseIds = newSelected)
    }

    fun clearExpenseSelection() {
        _uiState.value = _uiState.value.copy(selectedExpenseIds = emptySet())
    }

    fun deleteSelectedExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedIds = _uiState.value.selectedExpenseIds
            if (selectedIds.isEmpty()) return@launch

            val expensesToDelete = _uiState.value.expenses.filter { it.id in selectedIds }
            expensesToDelete.forEach { expense ->
                repository.deleteExpense(expense)
                firestoreSyncManager.deleteExpenseFromCloud(expense)
            }

            addSyncLog("Deleted ${expensesToDelete.size} selected expenses")
            _uiState.value = _uiState.value.copy(selectedExpenseIds = emptySet())
        }
    }

    fun restoreExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addExpense(expense)
            firestoreSyncManager.syncExpenseToCloud(expense)
            addSyncLog("Restored expense: ${expense.description}")
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch(Dispatchers.IO) {
            val hId = _uiState.value.householdId
            repository.clearAllAppData(hId)
            addSyncLog("Cleared all local app data completely. Fresh clean slate created.")
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                selectedCategoryFilter = null,
                selectedMemberFilter = null,
                selectedDateRangeFilter = null,
                isBudgetAlertDismissed = false,
                lastNotifiedThreshold = 0
            )
        }
    }

    fun toggleDarkMode(enableDark: Boolean? = null) {
        val newMode = enableDark ?: !_uiState.value.isDarkMode
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()
        _uiState.value = _uiState.value.copy(isDarkMode = newMode)
        addSyncLog("Switched app theme to ${if (newMode) "Dark" else "Light"} mode.")
    }

    fun openViewExpenseDialog(expense: ExpenseEntity) {
        _uiState.value = _uiState.value.copy(viewingExpense = expense)
    }

    fun closeViewExpenseDialog() {
        _uiState.value = _uiState.value.copy(viewingExpense = null)
    }

    fun toggleFabMenu() {
        _uiState.value = _uiState.value.copy(isFabMenuOpen = !_uiState.value.isFabMenuOpen)
    }

    fun closeFabMenu() {
        _uiState.value = _uiState.value.copy(isFabMenuOpen = false)
    }

    fun openAddExpenseDialog(expenseToEdit: ExpenseEntity? = null) {
        _uiState.value = _uiState.value.copy(
            isAddExpenseDialogOpen = true,
            editingExpense = expenseToEdit
        )
    }

    fun dismissAddExpenseDialog() {
        _uiState.value = _uiState.value.copy(
            isAddExpenseDialogOpen = false,
            editingExpense = null
        )
    }

    fun openAddIncomeDialog() {
        _uiState.value = _uiState.value.copy(isAddIncomeDialogOpen = true)
    }

    fun dismissAddIncomeDialog() {
        _uiState.value = _uiState.value.copy(isAddIncomeDialogOpen = false)
    }

    fun clearIncomeSuccessToast() {
        _uiState.value = _uiState.value.copy(incomeSuccessToast = null)
    }

    fun setTransactionTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(transactionTypeFilter = type)
    }

    fun saveIncome(
        amount: Double,
        source: String,
        category: String,
        date: Long,
        receivedBy: String,
        receivedByName: String,
        paymentMethod: String,
        isRecurring: Boolean,
        recurringFrequency: String?,
        recurringStartDate: Long?,
        note: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val income = IncomeEntity(
                amount = amount,
                source = source,
                category = category,
                date = date,
                receivedBy = receivedBy,
                receivedByName = receivedByName,
                paymentMethod = paymentMethod,
                isRecurring = isRecurring,
                recurringFrequency = recurringFrequency,
                recurringStartDate = recurringStartDate,
                note = note,
                householdId = state.householdId,
                currencySymbol = state.currencySymbol
            )
            repository.addIncome(income)
            firestoreSyncManager.syncIncomeToCloud(income)
            addSyncLog("Added income: $source (${state.currencySymbol}${String.format(Locale.US, "%,.2f", amount)}) received by $receivedByName")

            _uiState.value = _uiState.value.copy(
                isAddIncomeDialogOpen = false,
                incomeSuccessToast = "✓ Income of ${state.currencySymbol}${String.format(Locale.US, "%,.2f", amount)} logged successfully!"
            )

            delay(2500)
            _uiState.value = _uiState.value.copy(incomeSuccessToast = null)
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIncome(income)
            firestoreSyncManager.deleteIncomeFromCloud(income)
            addSyncLog("Deleted income: ${income.source}")
        }
    }

    fun openTransferDialog() {
        _uiState.value = _uiState.value.copy(isTransferDialogOpen = true)
    }

    fun dismissTransferDialog() {
        _uiState.value = _uiState.value.copy(isTransferDialogOpen = false)
    }

    fun clearTransferSuccessToast() {
        _uiState.value = _uiState.value.copy(transferSuccessToast = null)
    }

    fun openScanReceiptDialog() {
        _uiState.value = _uiState.value.copy(isScanReceiptDialogOpen = true)
    }

    fun dismissScanReceiptDialog() {
        _uiState.value = _uiState.value.copy(isScanReceiptDialogOpen = false)
    }

    fun openSplitExpenseDialog() {
        _uiState.value = _uiState.value.copy(isSplitExpenseDialogOpen = true)
    }

    fun dismissSplitExpenseDialog() {
        _uiState.value = _uiState.value.copy(isSplitExpenseDialogOpen = false)
    }

    fun saveTransfer(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        date: Long,
        memberId: String,
        memberName: String,
        note: String,
        fee: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val desc = "Transfer: $fromAccount ➔ $toAccount"
            val feeInfo = if (fee > 0.0) " (Fee: ${state.currencySymbol}${String.format(Locale.US, "%.2f", fee)})" else ""
            val fullNote = if (note.isNotBlank()) "$note$feeInfo" else "Internal account transfer$feeInfo"

            val transferExpense = ExpenseEntity(
                amount = amount + fee,
                currencySymbol = state.currencySymbol,
                category = "Transfer",
                description = desc,
                paidByMemberId = memberId,
                paidByMemberName = memberName,
                splitType = "Transfer",
                paymentMethod = fromAccount,
                note = fullNote,
                timestamp = date,
                householdId = state.householdId,
                tags = "transfer,$fromAccount,$toAccount"
            )

            repository.addExpense(transferExpense)
            firestoreSyncManager.syncExpenseToCloud(transferExpense)
            addSyncLog("Recorded transfer: ${state.currencySymbol}${String.format(Locale.US, "%,.2f", amount)} from $fromAccount to $toAccount by $memberName")

            _uiState.value = _uiState.value.copy(
                isTransferDialogOpen = false,
                transferSuccessToast = "✓ Transfer of ${state.currencySymbol}${String.format(Locale.US, "%,.2f", amount)} recorded successfully!"
            )

            delay(2800)
            _uiState.value = _uiState.value.copy(transferSuccessToast = null)
        }
    }

    fun openAddMemberDialog() {
        openInviteFamilySheet()
    }

    fun dismissAddMemberDialog() {
        _uiState.value = _uiState.value.copy(isAddMemberDialogOpen = false)
    }

    fun openEditMemberDialog(member: FamilyMemberEntity) {
        _uiState.value = _uiState.value.copy(
            isEditMemberDialogOpen = true,
            editingMember = member
        )
    }

    fun dismissEditMemberDialog() {
        _uiState.value = _uiState.value.copy(
            isEditMemberDialogOpen = false,
            editingMember = null
        )
    }

    fun addFamilyMember(name: String, role: String, avatarColorHex: String, monthlyGoal: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            if (!state.currentUserHouseholdRole.canInviteMembers()) {
                _uiState.value = state.copy(
                    invitationFeedbackMessage = "Only Owners and Admins can add members."
                )
                return@launch
            }
            val newMember = FamilyMemberEntity(
                id = "mem_${System.currentTimeMillis()}",
                name = name,
                role = role,
                avatarColorHex = avatarColorHex,
                avatarIcon = listOf("person", "face", "star", "favorite").random(),
                isCurrentActiveUser = false,
                householdId = state.householdId,
                monthlyContributionGoal = monthlyGoal,
                householdRole = HouseholdRole.MEMBER.value,
                membershipStatus = MembershipStatus.ACTIVE.value,
                joinedAt = System.currentTimeMillis()
            )
            repository.addFamilyMember(newMember)
            firestoreSyncManager.syncFamilyMemberToCloud(newMember)
            addSyncLog("Added family profile: $name ($role)")
            _uiState.value = _uiState.value.copy(isAddMemberDialogOpen = false)
        }
    }

    fun saveEditedMember(name: String, role: String, avatarColorHex: String, monthlyGoal: Double) {
        val member = _uiState.value.editingMember ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMember = member.copy(
                name = name,
                role = role,
                avatarColorHex = avatarColorHex,
                monthlyContributionGoal = monthlyGoal
            )
            repository.updateFamilyMember(updatedMember)
            firestoreSyncManager.syncFamilyMemberToCloud(updatedMember)
            addSyncLog("Updated family member: $name")
            dismissEditMemberDialog()
        }
    }

    fun deleteMember(member: FamilyMemberEntity) {
        removeHouseholdMember(member)
    }

    fun openEditBudgetDialog() {
        _uiState.value = _uiState.value.copy(isEditBudgetDialogOpen = true)
    }

    fun dismissEditBudgetDialog() {
        _uiState.value = _uiState.value.copy(isEditBudgetDialogOpen = false)
    }

    fun openMonthlySummaryDialog() {
        _uiState.value = _uiState.value.copy(isMonthlySummaryDialogOpen = true)
    }

    fun dismissMonthlySummaryDialog() {
        _uiState.value = _uiState.value.copy(isMonthlySummaryDialogOpen = false)
    }

    fun openExportCsvDialog() {
        _uiState.value = _uiState.value.copy(isExportCsvDialogOpen = true)
    }

    fun dismissExportCsvDialog() {
        _uiState.value = _uiState.value.copy(isExportCsvDialogOpen = false)
    }

    fun saveCategoryBudget(categoryName: String, monthlyLimit: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val budget = CategoryBudgetEntity(
                categoryName = categoryName,
                monthlyLimit = monthlyLimit,
                householdId = _uiState.value.householdId
            )
            repository.setCategoryBudget(budget)
            firestoreSyncManager.syncCategoryBudgetToCloud(budget)
            addSyncLog("Updated budget for $categoryName: ${_uiState.value.currencySymbol}$monthlyLimit")
        }
    }

    fun updateTotalMonthlyBudget(newLimit: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentH = state.household ?: HouseholdEntity(householdId = state.householdId)
            val updatedH = currentH.copy(totalMonthlyBudget = newLimit)
            repository.updateHousehold(updatedH)
            firestoreSyncManager.syncHouseholdToCloud(updatedH)
            addSyncLog("Set total family monthly budget limit to ${state.currencySymbol}$newLimit")
        }
    }

    fun toggleLiveSync(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentH = state.household ?: HouseholdEntity(householdId = state.householdId)
            val updatedH = currentH.copy(isLiveSyncEnabled = enabled)
            repository.updateHousehold(updatedH)
            if (enabled && isFirebaseAuthenticated) {
                firestoreSyncManager.startRealtimeSync(state.householdId) { addSyncLog(it) }
            } else if (!enabled) {
                firestoreSyncManager.stopRealtimeSync()
            }
            addSyncLog(if (enabled) "Real-time live sync enabled." else "Real-time live sync paused.")
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = "Syncing with cloud household...")
            firestoreSyncManager.triggerFullReSync()
            delay(800)
            repository.updateLastSynced(_uiState.value.householdId)
            addSyncLog("Manual sync complete. All family devices up to date.")
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (isFirebaseAuthenticated) {
                    "Firebase Firestore Live Sync • Connected"
                } else {
                    "Sign in with Google for multi-device sync"
                }
            )
        }
    }

    fun simulateLiveFamilyTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = "Syncing household activity...")
            val newExpense = repository.simulateLiveFamilyExpense(state.householdId, state.members)
            delay(600)
            if (newExpense != null) {
                firestoreSyncManager.syncExpenseToCloud(newExpense)
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                addSyncLog("Logged ${newExpense.paidByMemberName}: ${state.currencySymbol}${String.format(Locale.US, "%.2f", newExpense.amount)} for ${newExpense.category} ($timeStr)")
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (isFirebaseAuthenticated) {
                    "Firebase Firestore Live Sync • Connected"
                } else {
                    "Sign in with Google for multi-device sync"
                }
            )
        }
    }

    /**
     * Routes incoming invitation links/tokens securely through the validated invitation acceptance workflow.
     * Prevents unauthorized household switching by raw household ID.
     */
    fun joinHouseholdWithInvitation(rawInviteLinkOrToken: String) {
        handleIncomingInvitationLink(rawInviteLinkOrToken)
    }

    fun openJoinHouseholdDialog() {
        _uiState.value = _uiState.value.copy(isJoinHouseholdDialogOpen = true)
    }

    fun dismissJoinHouseholdDialog() {
        _uiState.value = _uiState.value.copy(isJoinHouseholdDialogOpen = false)
    }

    fun openCurrencySettingsDialog() {
        _uiState.value = _uiState.value.copy(isCurrencySettingsDialogOpen = true)
    }

    fun dismissCurrencySettingsDialog() {
        _uiState.value = _uiState.value.copy(isCurrencySettingsDialogOpen = false)
    }

    fun updateCurrencySymbol(newSymbol: String) {
        val cleanSymbol = newSymbol.trim().ifBlank { "₹" }
        _uiState.value = _uiState.value.copy(
            currencySymbol = cleanSymbol,
            isCurrencySettingsDialogOpen = false
        )
        viewModelScope.launch(Dispatchers.IO) {
            val currentHousehold = _uiState.value.household
            val updated = if (currentHousehold != null) {
                currentHousehold.copy(defaultCurrency = cleanSymbol)
            } else {
                HouseholdEntity(
                    householdId = _uiState.value.householdId,
                    householdName = _uiState.value.householdName,
                    defaultCurrency = cleanSymbol
                )
            }
            repository.updateHousehold(updated)
            firestoreSyncManager.syncHouseholdToCloud(updated)

            val expensesList = _uiState.value.expenses
            expensesList.forEach { exp ->
                if (exp.currencySymbol != cleanSymbol) {
                    repository.updateExpense(exp.copy(currencySymbol = cleanSymbol))
                }
            }

            addSyncLog("Updated local and family currency symbol to '$cleanSymbol'")
        }
    }

    fun dismissBudgetAlert() {
        _uiState.value = _uiState.value.copy(isBudgetAlertDismissed = true)
    }

    fun checkBudgetAlerts(spentMonth: Double, limit: Double, currencySymbol: String) {
        if (limit <= 0) return

        val ratio = spentMonth / limit
        val percent = (ratio * 100).toInt()
        val formattedSpent = "$currencySymbol${String.format(Locale.US, "%.2f", spentMonth)}"
        val formattedLimit = "$currencySymbol${String.format(Locale.US, "%.2f", limit)}"

        val currentNotified = _uiState.value.lastNotifiedThreshold

        if (ratio >= 1.0) {
            val title = "100% Monthly Budget Exceeded!"
            val msg = "Alert: You have consumed $percent% of your monthly budget ($formattedSpent of $formattedLimit)!"
            val notif = BudgetNotification(
                level = BudgetAlertLevel.EXCEEDED_100,
                percent = percent,
                spentAmount = spentMonth,
                budgetLimit = limit,
                title = title,
                message = msg
            )
            val shouldNotify = currentNotified < 100
            _uiState.value = _uiState.value.copy(
                budgetNotification = notif,
                isBudgetAlertDismissed = if (shouldNotify) false else _uiState.value.isBudgetAlertDismissed,
                lastNotifiedThreshold = 100
            )
            if (shouldNotify) {
                sendSystemBudgetNotification(getApplication(), title, msg, 1001)
                addSyncLog("ALERT TRIGGERED: 100% Budget Exceeded ($percent% - $formattedSpent / $formattedLimit)")
            }
        } else if (ratio >= 0.8) {
            val title = "80% Monthly Budget Warning"
            val msg = "Warning: You have reached $percent% of your monthly budget ($formattedSpent of $formattedLimit)."
            val notif = BudgetNotification(
                level = BudgetAlertLevel.WARNING_80,
                percent = percent,
                spentAmount = spentMonth,
                budgetLimit = limit,
                title = title,
                message = msg
            )
            val shouldNotify = currentNotified < 80
            _uiState.value = _uiState.value.copy(
                budgetNotification = notif,
                isBudgetAlertDismissed = if (shouldNotify) false else _uiState.value.isBudgetAlertDismissed,
                lastNotifiedThreshold = 80
            )
            if (shouldNotify) {
                sendSystemBudgetNotification(getApplication(), title, msg, 1000)
                addSyncLog("WARNING TRIGGERED: 80% Budget Threshold Reached ($percent% - $formattedSpent / $formattedLimit)")
            }
        } else {
            _uiState.value = _uiState.value.copy(
                budgetNotification = null,
                isBudgetAlertDismissed = false,
                lastNotifiedThreshold = 0
            )
        }
    }

    fun checkDailyBudgetAlert(spentToday: Double, limit: Double, currencySymbol: String) {
        if (limit <= 0) return
        if (spentToday > limit) {
            val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayStr = sdfDay.format(Date())
            val lastNotified = prefs.getString("last_daily_notified_date", "")
            if (lastNotified != todayStr) {
                val formattedSpent = "$currencySymbol${String.format(Locale.US, "%.2f", spentToday)}"
                val formattedLimit = "$currencySymbol${String.format(Locale.US, "%.2f", limit)}"
                val title = "Daily Spending Limit Exceeded!"
                val msg = "You have spent $formattedSpent today, exceeding your daily limit of $formattedLimit."
                sendSystemBudgetNotification(getApplication(), title, msg, 2001)

                prefs.edit().putString("last_daily_notified_date", todayStr).apply()
                addSyncLog("DAILY LIMIT TRIGGERED: $formattedSpent / $formattedLimit")
            }
        }
    }

    fun updateDailySpendingLimit(newLimit: Double) {
        prefs.edit().putFloat("daily_spending_limit", newLimit.toFloat()).apply()
        _uiState.value = _uiState.value.copy(dailySpendingLimit = newLimit)
        checkDailyBudgetAlert(_uiState.value.totalSpentToday, newLimit, _uiState.value.currencySymbol)
        addSyncLog("Set personal daily spending limit to ${_uiState.value.currencySymbol}$newLimit")
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendSystemBudgetNotification(context: Context, title: String, message: String, notificationId: Int) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val channelId = "famspend_budget_alerts"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Budget Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when spending reaches 80% or 100% of monthly budget"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, builder.build())
        } catch (_: Exception) {
            // Fallback gracefully if notifications are restricted
        }
    }

    private fun addSyncLog(message: String) {
        val sdf = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timestamped = "[${sdf.format(Date())}] $message"
        val updated = (_uiState.value.syncLog + timestamped).takeLast(25)
        _uiState.value = _uiState.value.copy(syncLog = updated)
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAuthLoading = true, authError = null)
            when (val result = authManager.signInWithGoogle(context)) {
                is GoogleSignInResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        authError = null,
                        authUser = result.user
                    )
                    addSyncLog("Signed in as ${result.user.displayName} (${result.user.email})")
                }
                GoogleSignInResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(isAuthLoading = false)
                }
                is GoogleSignInResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAuthLoading = false,
                        authError = result.message
                    )
                    addSyncLog("Google Sign-In error: ${result.message}")
                }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            firestoreSyncManager.stopRealtimeSync()
            authManager.signOut(context)
            _uiState.value = _uiState.value.copy(
                authUser = null,
                syncStatusMessage = "Sign in with Google for multi-device sync"
            )
            addSyncLog("Signed out of Google account.")
        }
    }

    fun clearAuthError() {
        _uiState.value = _uiState.value.copy(authError = null)
    }

    override fun onCleared() {
        super.onCleared()
        authManager.cleanup()
        firestoreSyncManager.cleanup()
    }
}