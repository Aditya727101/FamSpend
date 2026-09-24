package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
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
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val fabInteractionSource = remember { MutableInteractionSource() }
    val isFabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (isFabPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "fab_scale"
    )
    val fabRotation by animateFloatAsState(
        targetValue = if (uiState.isFabMenuOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "fab_rotation"
    )

    val fabSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val closeFabSheetWithAction: ((() -> Unit)?) -> Unit = { action ->
        scope.launch {
            fabSheetState.hide()
        }.invokeOnCompletion {
            if (!fabSheetState.isVisible) {
                viewModel.closeFabMenu()
                action?.invoke()
            }
        }
    }

    // Request notification permission for budget alerts on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val handleDeleteExpenseWithUndo: (ExpenseEntity) -> Unit = { expense ->
        viewModel.deleteExpense(expense)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${expense.description} (${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", expense.amount)})",
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
            if (uiState.selectedExpenseIds.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedExpenseIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearExpenseSelection() }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelectedExpenses() }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.signInWithGoogle(context) }
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                            contentDescription = "Sign in to Sync",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
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
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                val navItems = listOf(
                    Triple(0, Icons.Default.Home, "Home"),
                    Triple(1, Icons.AutoMirrored.Filled.ReceiptLong, "Expenses"),
                    Triple(2, Icons.Default.PieChart, "Budgets"),
                    Triple(3, Icons.Default.Analytics, "Analytics"),
                    Triple(4, Icons.Default.Group, "Family")
                )
                navItems.forEach { (index, icon, label) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(when (index) {
                            0 -> "nav_home_tab"
                            1 -> "nav_expenses_tab"
                            2 -> "nav_budgets_tab"
                            3 -> "nav_analytics_tab"
                            else -> "nav_family_tab"
                        })
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.toggleFabMenu() },
                    interactionSource = fabInteractionSource,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier
                        .testTag("main_fab_add_expense")
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Expense",
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer { rotationZ = fabRotation }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
            ) {
                // Floating Green Success Toast for logged income
                AnimatedVisibility(
                    visible = uiState.incomeSuccessToast != null,
                    enter = fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(250)) { -it },
                    exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(250)) { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .zIndex(99f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF15803D),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = uiState.incomeSuccessToast ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearIncomeSuccessToast() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                    },
                    label = "tab_navigation_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeScreen(
                            uiState = uiState,
                            onAddExpenseClick = { viewModel.openAddExpenseDialog() },
                            onSyncNowClick = { viewModel.triggerManualSync() },
                            onSimulateLiveClick = { viewModel.simulateLiveFamilyTransaction() },
                            onMonthlySummaryClick = { viewModel.openMonthlySummaryDialog() },
                            onViewAllTransactions = { selectedTab = 1 },
                            onEditExpense = { viewModel.openAddExpenseDialog(it) },
                            onViewExpense = { viewModel.openViewExpenseDialog(it) },
                            onDeleteExpense = handleDeleteExpenseWithUndo,
                            onToggleSelection = { viewModel.toggleExpenseSelection(it) },
                            onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },
                            onEditBudgetClick = { viewModel.openEditBudgetDialog() },
                            onAddMemberClick = { viewModel.openAddMemberDialog() }
                        )
                        1 -> TransactionsScreen(
                            uiState = uiState,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                            onDateRangeFilterChange = { viewModel.setDateRangeFilter(it) },
                            onMemberFilterChange = { viewModel.setMemberFilter(it) },
                            onPaymentMethodFilterChange = { viewModel.setPaymentMethodFilter(it) },
                            onAddExpenseClick = { viewModel.openAddExpenseDialog() },
                            onEditExpense = { viewModel.openAddExpenseDialog(it) },
                            onViewExpense = { viewModel.openViewExpenseDialog(it) },
                            onDeleteExpense = handleDeleteExpenseWithUndo,
                            onExportCsvClick = { viewModel.openExportCsvDialog() },
                            onDeleteIncome = { viewModel.deleteIncome(it) }
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
                }
            }

            // Dialog Modals
            if (uiState.isCurrencySettingsDialogOpen) {
                CurrencySettingsDialog(
                    currentCurrency = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissCurrencySettingsDialog() },
                    onSave = { newSymbol -> viewModel.updateCurrencySymbol(newSymbol) }
                )
            }
            
            if (uiState.isFabMenuOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeFabMenu() },
                    sheetState = fabSheetState,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.32f),
                    dragHandle = {
                        BottomSheetDefaults.DragHandle(
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "New Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                        NewEntryRow(
                            icon = Icons.Default.Add,
                            tint = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            title = "Add expense",
                            subtitle = "Log daily spend, groceries, bills, or shopping",
                            onClick = {
                                closeFabSheetWithAction { viewModel.openAddExpenseDialog() }
                            }
                        )
                        NewEntryRow(
                            icon = Icons.Default.Payments,
                            tint = Color(0xFF22C55E),
                            backgroundColor = Color(0xFFDCFCE7),
                            title = "Add income",
                            subtitle = "Salary, freelance, or household contribution",
                            onClick = {
                                closeFabSheetWithAction { viewModel.openAddIncomeDialog() }
                            }
                        )
                        NewEntryRow(
                            icon = Icons.Default.SwapHoriz,
                            tint = MaterialTheme.colorScheme.secondary,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            title = "Transfer between accounts",
                            subtitle = "Move money between banks or cards",
                            onClick = {
                                closeFabSheetWithAction { viewModel.openAddExpenseDialog() }
                            }
                        )
                        NewEntryRow(
                            icon = Icons.Default.CameraAlt,
                            tint = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            title = "Scan receipt",
                            subtitle = "Capture receipt with camera or gallery",
                            onClick = {
                                closeFabSheetWithAction { viewModel.openAddExpenseDialog() }
                            }
                        )
                        NewEntryRow(
                            icon = Icons.AutoMirrored.Filled.CallSplit,
                            tint = MaterialTheme.colorScheme.secondary,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            title = "Split expense",
                            subtitle = "Divide equally or custom with family members",
                            onClick = {
                                closeFabSheetWithAction { viewModel.openAddExpenseDialog() }
                            }
                        )
                    }
                }
            }

            if (uiState.isAddExpenseDialogOpen) {
                AddExpenseDialog(
                    expenseToEdit = uiState.editingExpense,
                    members = uiState.members,
                    activeMember = uiState.activeMember,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissAddExpenseDialog() },
                    onSave = { amount, category, description, paidByMemberId, paidByMemberName, splitType, paymentMethod, note, isRecurring, recurringFrequency, recurringDayOfMonth, timestamp ->
                        viewModel.saveExpense(
                            amount, category, description, paidByMemberId, paidByMemberName, splitType, paymentMethod, note, isRecurring, recurringFrequency, recurringDayOfMonth, timestamp
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

            
            uiState.viewingExpense?.let { expense ->
                com.example.ui.components.ExpenseDetailDialog(
                    expense = expense,
                    currencySymbol = uiState.currencySymbol,
                    members = uiState.members,
                    onDismiss = { viewModel.closeViewExpenseDialog() },
                    onEdit = {
                        viewModel.closeViewExpenseDialog()
                        viewModel.openAddExpenseDialog(it)
                    },
                    onDelete = {
                        viewModel.closeViewExpenseDialog()
                        handleDeleteExpenseWithUndo(it)
                    },
                    onDuplicate = {
                        viewModel.duplicateExpense(it)
                    },
                    onToggleSettled = {
                        viewModel.toggleExpenseSettled(it)
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

            if (uiState.isAddIncomeDialogOpen) {
                com.example.ui.components.AddIncomeDialog(
                    members = uiState.members,
                    activeMember = uiState.activeMember,
                    currencySymbol = uiState.currencySymbol,
                    onDismiss = { viewModel.dismissAddIncomeDialog() },
                    onSave = { amount, source, category, date, receivedBy, receivedByName, paymentMethod, isRecurring, recurringFrequency, recurringStartDate, note ->
                        viewModel.saveIncome(
                            amount,
                            source,
                            category,
                            date,
                            receivedBy,
                            receivedByName,
                            paymentMethod,
                            isRecurring,
                            recurringFrequency,
                            recurringStartDate,
                            note
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun NewEntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    backgroundColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
