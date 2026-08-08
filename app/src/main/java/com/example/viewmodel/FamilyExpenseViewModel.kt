package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.HouseholdEntity
import com.example.data.sync.FirestoreSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

data class UiState(
    val householdId: String = "FAM-1001",
    val householdName: String = "My Household",
    val inviteCode: String = "FAM-1001",
    val currencySymbol: String = "$",
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val members: List<FamilyMemberEntity> = emptyList(),
    val activeMember: FamilyMemberEntity? = null,
    val categoryBudgets: List<CategoryBudgetEntity> = emptyList(),
    val household: HouseholdEntity? = null,
    val selectedCategoryFilter: String? = null,
    val selectedMemberFilter: String? = null,
    val selectedDateRangeFilter: String? = null,
    val searchQuery: String = "",
    val isSyncing: Boolean = false,
    val isLiveSyncEnabled: Boolean = true,
    val syncStatusMessage: String = "Firebase Firestore Live Sync • Connected",
    val syncLog: List<String> = listOf("Initial real-time family Firestore session connected."),
    val totalSpentToday: Double = 0.0,
    val totalSpentThisMonth: Double = 0.0,
    val monthlyBudgetLimit: Double = 3000.0,
    val dailySpendingLimit: Double = 0.0,
    val budgetNotification: BudgetNotification? = null,
    val isBudgetAlertDismissed: Boolean = false,
    val lastNotifiedThreshold: Int = 0,
    val isAddExpenseDialogOpen: Boolean = false,
    val isAddMemberDialogOpen: Boolean = false,
    val isEditBudgetDialogOpen: Boolean = false,
    val isJoinHouseholdDialogOpen: Boolean = false,
    val isMonthlySummaryDialogOpen: Boolean = false,
    val isCurrencySettingsDialogOpen: Boolean = false,
    val isExportCsvDialogOpen: Boolean = false,
    val isEditMemberDialogOpen: Boolean = false,
    val editingExpense: ExpenseEntity? = null,
    val editingMember: FamilyMemberEntity? = null,
    val isDarkMode: Boolean = false
)

class FamilyExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val firestoreSyncManager: FirestoreSyncManager
    private val prefs = application.getSharedPreferences("fam_spend_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val initialDarkMode = prefs.getBoolean("is_dark_mode", false)
        val initialDailyLimit = prefs.getFloat("daily_spending_limit", 0f).toDouble()
        _uiState.value = _uiState.value.copy(
            isDarkMode = initialDarkMode,
            dailySpendingLimit = initialDailyLimit
        )

        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(
            database.expenseDao(),
            database.familyMemberDao(),
            database.categoryBudgetDao(),
            database.householdDao()
        )
        firestoreSyncManager = FirestoreSyncManager(application, repository)

        observeData()

        // Ensure weekly scheduled review reminder is initialized
        if (com.example.util.WeeklyReminderScheduler.isReminderEnabled(application)) {
            com.example.util.WeeklyReminderScheduler.scheduleWeeklyReminder(application)
        }
    }

    private var observationJob: Job? = null

    private fun observeData() {
        startObservingHousehold(_uiState.value.householdId)
    }

    private fun startObservingHousehold(hId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch(Dispatchers.IO) {
            firestoreSyncManager.startRealtimeSync(hId) { logMsg ->
                addSyncLog(logMsg)
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
                    val active = membersList.find { it.isCurrentActiveUser } ?: membersList.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        members = membersList,
                        activeMember = active
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
                    val now = System.currentTimeMillis()
                    val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val sdfMonth = SimpleDateFormat("yyyyMM", Locale.getDefault())
                    val todayStr = sdfDay.format(Date(now))
                    val monthStr = sdfMonth.format(Date(now))

                    val spentToday = expensesList
                        .filter { sdfDay.format(Date(it.timestamp)) == todayStr }
                        .sumOf { it.amount }

                    val spentMonth = expensesList
                        .filter { sdfMonth.format(Date(it.timestamp)) == monthStr }
                        .sumOf { it.amount }

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
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.expenses

        if (!state.selectedCategoryFilter.isNullOrBlank()) {
            list = list.filter { it.category.equals(state.selectedCategoryFilter, ignoreCase = true) }
        }

        if (!state.selectedMemberFilter.isNullOrBlank()) {
            list = list.filter { it.paidByMemberId == state.selectedMemberFilter }
        }

        if (!state.selectedDateRangeFilter.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            list = when (state.selectedDateRangeFilter) {
                "TODAY" -> {
                    val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val todayStr = sdfDay.format(Date(now))
                    list.filter { sdfDay.format(Date(it.timestamp)) == todayStr }
                }
                "WEEK" -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val weekAgo = cal.timeInMillis
                    list.filter { it.timestamp >= weekAgo }
                }
                "MONTH" -> {
                    val sdfMonth = SimpleDateFormat("yyyyMM", Locale.getDefault())
                    val monthStr = sdfMonth.format(Date(now))
                    list.filter { sdfMonth.format(Date(it.timestamp)) == monthStr }
                }
                "LAST_30_DAYS" -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -30)
                    val thirtyDaysAgo = cal.timeInMillis
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
        recurringDayOfMonth: Int = 1
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val editing = state.editingExpense

            if (editing != null) {
                val updated = editing.copy(
                    amount = amount,
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
                    timestamp = System.currentTimeMillis()
                )
                repository.updateExpense(updated)
                firestoreSyncManager.syncExpenseToCloud(updated)
                val recurringText = if (isRecurring) " [Recurring $recurringFrequency on Day $recurringDayOfMonth]" else ""
                addSyncLog("Updated expense: ${description} ($${amount})${recurringText}")
            } else {
                val newExp = ExpenseEntity(
                    amount = amount,
                    category = category,
                    description = description,
                    paidByMemberId = paidByMemberId,
                    paidByMemberName = paidByMemberName,
                    splitType = splitType,
                    timestamp = System.currentTimeMillis(),
                    householdId = state.householdId,
                    paymentMethod = paymentMethod,
                    note = note,
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency,
                    recurringDayOfMonth = recurringDayOfMonth
                )
                repository.addExpense(newExp)
                firestoreSyncManager.syncExpenseToCloud(newExp)
                val recurringText = if (isRecurring) " [Recurring $recurringFrequency on Day $recurringDayOfMonth]" else ""
                addSyncLog("Added expense: ${description} ($${amount}) by ${paidByMemberName}${recurringText}")
            }

            _uiState.value = _uiState.value.copy(
                isAddExpenseDialogOpen = false,
                editingExpense = null
            )
        }
    }

    fun triggerAutoCreateRecurringExpenses() {
        checkAndAutoCreateRecurringExpenses(_uiState.value.expenses)
    }

    private fun checkAndAutoCreateRecurringExpenses(expensesList: List<ExpenseEntity>) {
        if (expensesList.isEmpty()) return

        // Find recurring template expenses (master recurring definitions)
        val recurringTemplates = expensesList.filter { it.isRecurring && !it.isAutoCreated }
        if (recurringTemplates.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val sdfMonth = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val currentMonthStr = sdfMonth.format(cal.time)

        for (template in recurringTemplates) {
            val targetDay = template.recurringDayOfMonth.coerceIn(1, 31)

            // Check if an auto-created instance of this expense already exists in the current month
            val alreadyCreatedThisMonth = expensesList.any { exp ->
                exp.isAutoCreated &&
                exp.description.equals(template.description, ignoreCase = true) &&
                exp.category.equals(template.category, ignoreCase = true) &&
                sdfMonth.format(Date(exp.timestamp)) == currentMonthStr
            }

            // Auto-create if not created yet and today is >= set day of month
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
            addSyncLog("Deleted expense: ${expense.description} ($${expense.amount})")
        }
    }

    fun restoreExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addExpense(expense)
            firestoreSyncManager.syncExpenseToCloud(expense)
            addSyncLog("Restored expense: ${expense.description} ($${expense.amount})")
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch(Dispatchers.IO) {
            val hId = _uiState.value.householdId
            repository.clearAllAppData(hId)
            addSyncLog("Cleared all app data completely. Fresh clean slate created.")
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

    fun openAddMemberDialog() {
        _uiState.value = _uiState.value.copy(isAddMemberDialogOpen = true)
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
            val newMember = FamilyMemberEntity(
                id = "mem_${System.currentTimeMillis()}",
                name = name,
                role = role,
                avatarColorHex = avatarColorHex,
                avatarIcon = listOf("person", "face", "star", "favorite").random(),
                isCurrentActiveUser = false,
                householdId = state.householdId,
                monthlyContributionGoal = monthlyGoal
            )
            repository.addFamilyMember(newMember)
            firestoreSyncManager.syncFamilyMemberToCloud(newMember)
            addSyncLog("Added family member: ${name} (${role})")
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
            addSyncLog("Updated family member: ${name}")
            dismissEditMemberDialog()
        }
    }

    fun deleteMember(member: FamilyMemberEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFamilyMember(member)
            firestoreSyncManager.deleteFamilyMemberFromCloud(member)
            addSyncLog("Removed family member: ${member.name}")
            if (_uiState.value.activeMember?.id == member.id) {
                // If active member deleted, select the first available member
                val members = repository.getMembers(_uiState.value.householdId).firstOrNull()
                members?.firstOrNull()?.let { switchActiveMember(it.id) }
            }
        }
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
            addSyncLog("Updated budget for ${categoryName}: $${monthlyLimit}")
        }
    }

    fun updateTotalMonthlyBudget(newLimit: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentH = state.household ?: HouseholdEntity(householdId = state.householdId)
            val updatedH = currentH.copy(totalMonthlyBudget = newLimit)
            repository.updateHousehold(updatedH)
            firestoreSyncManager.syncHouseholdToCloud(updatedH)
            addSyncLog("Set total family monthly budget limit to $${newLimit}")
        }
    }

    fun toggleLiveSync(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentH = state.household ?: HouseholdEntity(householdId = state.householdId)
            val updatedH = currentH.copy(isLiveSyncEnabled = enabled)
            repository.updateHousehold(updatedH)
            addSyncLog(if (enabled) "Real-time live sync enabled." else "Real-time live sync paused.")
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = "Syncing with cloud family node...")
            delay(1200)
            repository.updateLastSynced(_uiState.value.householdId)
            addSyncLog("Manual sync complete. All family devices up to date.")
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Real-time P2P sync active • Up to date"
            )
        }
    }

    fun simulateLiveFamilyTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = "Incoming live transaction...")
            val newExpense = repository.simulateLiveFamilyExpense(state.householdId, state.members)
            delay(800)
            if (newExpense != null) {
                firestoreSyncManager.syncExpenseToCloud(newExpense)
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                addSyncLog("⚡ [LIVE SYNC] ${newExpense.paidByMemberName} logged $${String.format(Locale.US, "%.2f", newExpense.amount)} for ${newExpense.category} ($timeStr)")
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = "Firebase Firestore Live Sync • Connected"
            )
        }
    }

    fun joinHousehold(newHouseholdId: String, newHouseholdName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanId = newHouseholdId.trim().uppercase()
            if (cleanId.isNotBlank()) {
                val newHousehold = HouseholdEntity(
                    householdId = cleanId,
                    householdName = newHouseholdName.ifBlank { "Household $cleanId" },
                    inviteCode = cleanId,
                    defaultCurrency = _uiState.value.currencySymbol,
                    totalMonthlyBudget = 3000.0,
                    lastSyncedTimestamp = System.currentTimeMillis()
                )
                repository.updateHousehold(newHousehold)
                _uiState.value = _uiState.value.copy(
                    householdId = cleanId,
                    isJoinHouseholdDialogOpen = false
                )
                addSyncLog("Joined household ${cleanId} (${newHousehold.householdName})")
                startObservingHousehold(cleanId)
            }
        }
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
        val cleanSymbol = newSymbol.trim().ifBlank { "$" }
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

            // Also update expenses currencySymbol in repository to keep synced
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
            val title = "🚨 100% Monthly Budget Exceeded!"
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
                addSyncLog("🚨 ALERT TRIGGERED: 100% Budget Exceeded ($percent% - $formattedSpent / $formattedLimit)")
            }
        } else if (ratio >= 0.8) {
            val title = "⚠️ 80% Monthly Budget Warning"
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
                addSyncLog("⚠️ WARNING TRIGGERED: 80% Budget Threshold Reached ($percent% - $formattedSpent / $formattedLimit)")
            }
        } else {
            // Budget under 80%
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
                // Trigger notification
                val formattedSpent = "$currencySymbol${String.format(Locale.US, "%.2f", spentToday)}"
                val formattedLimit = "$currencySymbol${String.format(Locale.US, "%.2f", limit)}"
                val title = "Daily Spending Limit Exceeded!"
                val msg = "You have spent $formattedSpent today, exceeding your daily limit of $formattedLimit."
                sendSystemBudgetNotification(getApplication(), title, msg, 2001)
                
                prefs.edit().putString("last_daily_notified_date", todayStr).apply()
                addSyncLog("🚨 DAILY LIMIT TRIGGERED: $formattedSpent / $formattedLimit")
            }
        }
    }

    fun updateDailySpendingLimit(newLimit: Double) {
        prefs.edit().putFloat("daily_spending_limit", newLimit.toFloat()).apply()
        _uiState.value = _uiState.value.copy(dailySpendingLimit = newLimit)
        checkDailyBudgetAlert(_uiState.value.totalSpentToday, newLimit, _uiState.value.currencySymbol)
        addSyncLog("Set personal daily spending limit to $${newLimit}")
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun sendSystemBudgetNotification(context: android.content.Context, title: String, message: String, notificationId: Int) {
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
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
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
        } catch (e: Exception) {
            // Fallback gracefully if notifications restricted
        }
    }

    private fun addSyncLog(message: String) {
        val sdf = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timestamped = "[${sdf.format(Date())}] $message"
        val updated = (_uiState.value.syncLog + timestamped).takeLast(25)
        _uiState.value = _uiState.value.copy(syncLog = updated)
    }

    private fun startPeriodicSyncSimulation() {
        // Disabled background simulation so no random expenses are added automatically.
    }
}
