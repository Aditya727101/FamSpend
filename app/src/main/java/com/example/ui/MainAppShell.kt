package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.CurrencySettingsDialog
import com.example.ui.components.EditBudgetDialog
import com.example.ui.components.EditMemberDialog
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.JoinHouseholdDialog
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MonthlySummaryDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.FamilySyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TransactionsScreen
import com.example.viewmodel.FamilyExpenseViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    viewModel: FamilyExpenseViewModel,
    uiState: UiState
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val handleDeleteExpenseWithUndo: (ExpenseEntity) -> Unit = { expense ->
        viewModel.deleteExpense(expense)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${expense.description} (${uiState.currencySymbol}${String.format(Locale.US, "%.2f", expense.amount)})",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreExpense(expense)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.budgetNotification, uiState.isBudgetAlertDismissed) {
        val notif = uiState.budgetNotification
        if (notif != null && !uiState.isBudgetAlertDismissed && notif.level != com.example.viewmodel.BudgetAlertLevel.NONE) {
            val result = snackbarHostState.showSnackbar(
                message = "${notif.title}: ${notif.message}",
                actionLabel = "View Budgets",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                selectedTab = 2
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "FamSpend Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "FamSpend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = uiState.householdName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Active User Profile Switcher Chip
                    uiState.activeMember?.let { active ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { selectedTab = 4 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                MemberAvatar(
                                    name = active.name,
                                    colorHex = active.avatarColorHex,
                                    iconName = active.avatarIcon,
                                    size = 22.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = active.name.split(" ").firstOrNull() ?: active.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_home_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "Expenses") },
                    label = { Text("Expenses", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_expenses_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.PieChart, contentDescription = "Budgets") },
                    label = { Text("Budgets", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_budgets_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_analytics_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Family") },
                    label = { Text("Family", fontSize = 11.sp, maxLines = 1) },
                    modifier = Modifier.testTag("nav_family_tab")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddExpenseDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("main_fab_add_expense")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Log Expense")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    uiState = uiState,
                    onAddExpenseClick = { viewModel.openAddExpenseDialog() },
                    onSyncNowClick = { viewModel.triggerManualSync() },
                    onSimulateLiveClick = { viewModel.simulateLiveFamilyTransaction() },
                    onMonthlySummaryClick = { viewModel.openMonthlySummaryDialog() },
                    onViewAllTransactions = { selectedTab = 1 },
                    onEditExpense = { viewModel.openAddExpenseDialog(it) },
                    onDeleteExpense = handleDeleteExpenseWithUndo,
                    onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },
                    onEditBudgetClick = { viewModel.openEditBudgetDialog() }
                )
                1 -> TransactionsScreen(
                    uiState = uiState,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                    onDateRangeFilterChange = { viewModel.setDateRangeFilter(it) },
                    onMemberFilterChange = { viewModel.setMemberFilter(it) },
                    onAddExpenseClick = { viewModel.openAddExpenseDialog() },
                    onEditExpense = { viewModel.openAddExpenseDialog(it) },
                    onDeleteExpense = handleDeleteExpenseWithUndo,
                    onExportCsvClick = { viewModel.openExportCsvDialog() }
                )
                2 -> BudgetsScreen(
                    uiState = uiState,
                    onEditBudgetsClick = { viewModel.openEditBudgetDialog() },
                    onAddExpenseClick = { viewModel.openAddExpenseDialog() },
                    onTriggerRecurringSync = { viewModel.triggerAutoCreateRecurringExpenses() },
                    onDismissBudgetAlert = { viewModel.dismissBudgetAlert() }
                )
                3 -> AnalyticsScreen(
                    uiState = uiState,
                    onExportCsvClick = { viewModel.openExportCsvDialog() }
                )
                4 -> FamilySyncScreen(
                    uiState = uiState,
                    onAddMemberClick = { viewModel.openAddMemberDialog() },
                    onSwitchActiveMember = { viewModel.switchActiveMember(it) },
                    onToggleLiveSync = { viewModel.toggleLiveSync(it) },
                    onSyncNowClick = { viewModel.triggerManualSync() },
                    onSimulateLiveClick = { viewModel.simulateLiveFamilyTransaction() },
                    onJoinHouseholdClick = { viewModel.openJoinHouseholdDialog() },
                    onOpenCurrencySettings = { viewModel.openCurrencySettingsDialog() },
                    onClearAllData = { viewModel.clearAllAppData() },
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    onEditMemberClick = { viewModel.openEditMemberDialog(it) },
                    onDeleteMemberClick = { viewModel.deleteMember(it) }
                )
            }

            // Dialog Modals
            if (uiState.isCurrencySettingsDialogOpen) {
                CurrencySettingsDialog(
                    currentCurrency = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissCurrencySettingsDialog() },
                    onSave = { newSymbol -> viewModel.updateCurrencySymbol(newSymbol) }
                )
            }
            if (uiState.isAddExpenseDialogOpen) {
                AddExpenseDialog(
                    expenseToEdit = uiState.editingExpense,
                    members = uiState.members,
                    activeMember = uiState.activeMember,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissAddExpenseDialog() },
                    onSave = { amount, category, description, paidByMemberId, paidByMemberName, splitType, paymentMethod, note, isRecurring, recurringFrequency, recurringDayOfMonth ->
                        viewModel.saveExpense(
                            amount, category, description, paidByMemberId, paidByMemberName, splitType, paymentMethod, note, isRecurring, recurringFrequency, recurringDayOfMonth
                        )
                    }
                )
            }

            if (uiState.isAddMemberDialogOpen) {
                AddMemberDialog(
                    onDismiss = { viewModel.dismissAddMemberDialog() },
                    onSave = { name, role, avatarColorHex, monthlyGoal ->
                        viewModel.addFamilyMember(name, role, avatarColorHex, monthlyGoal)
                    }
                )
            }
            
            if (uiState.isEditMemberDialogOpen && uiState.editingMember != null) {
                EditMemberDialog(
                    member = uiState.editingMember,
                    onDismiss = { viewModel.dismissEditMemberDialog() },
                    onSave = { name, role, avatarColorHex, monthlyGoal ->
                        viewModel.saveEditedMember(name, role, avatarColorHex, monthlyGoal)
                    }
                )
            }

            if (uiState.isEditBudgetDialogOpen) {
                EditBudgetDialog(
                    currentTotalBudget = uiState.monthlyBudgetLimit,
                    currentDailyLimit = uiState.dailySpendingLimit,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissEditBudgetDialog() },
                    onSaveCategoryBudget = { categoryName, monthlyLimit ->
                        viewModel.saveCategoryBudget(categoryName, monthlyLimit)
                    },
                    onSaveTotalBudget = { totalMonthlyBudget ->
                        viewModel.updateTotalMonthlyBudget(totalMonthlyBudget)
                    },
                    onSaveDailyLimit = { dailyLimit ->
                        viewModel.updateDailySpendingLimit(dailyLimit)
                    }
                )
            }

            if (uiState.isJoinHouseholdDialogOpen) {
                JoinHouseholdDialog(
                    currentHouseholdId = uiState.householdId,
                    onDismiss = { viewModel.dismissJoinHouseholdDialog() },
                    onJoin = { code, name ->
                        viewModel.joinHousehold(code, name)
                    }
                )
            }

            if (uiState.isMonthlySummaryDialogOpen) {
                MonthlySummaryDialog(
                    expenses = uiState.expenses,
                    members = uiState.members,
                    monthlyBudgetLimit = uiState.monthlyBudgetLimit,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissMonthlySummaryDialog() },
                    onExportCsvClick = {
                        viewModel.dismissMonthlySummaryDialog()
                        viewModel.openExportCsvDialog()
                    }
                )
            }

            if (uiState.isExportCsvDialogOpen) {
                ExportCsvDialog(
                    expenses = uiState.expenses,
                    householdName = uiState.householdName,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissExportCsvDialog() }
                )
            }
        }
    }
}
