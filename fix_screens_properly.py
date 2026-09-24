import re

def update_home():
    with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
        code = f.read()

    sig_pattern = r'fun HomeScreen\([\s\S]*?onEditBudgetClick: \(\) -> Unit = \{\}\s*\)'
    if "onToggleSelection: (String) -> Unit" not in code:
        replacement = r"""fun HomeScreen(
    uiState: UiState,
    onAddExpenseClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onSimulateLiveClick: () -> Unit,
    onMonthlySummaryClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onToggleSelection: (String) -> Unit = {},
    onDismissBudgetAlert: () -> Unit = {},
    onEditBudgetClick: () -> Unit = {}
)"""
        code = re.sub(sig_pattern, replacement, code)
        
    pattern = r'com\.example\.ui\.components\.SwipeableExpenseItem\(\s*expense = expense,\s*currencySymbol = uiState\.currencySymbol,\s*onEditExpense = onEditExpense,\s*onDeleteExpense = onDeleteExpense\s*\)'
    
    replacement2 = """val isSelected = uiState.selectedExpenseIds.contains(expense.id)
                    val isSelectionMode = uiState.selectedExpenseIds.isNotEmpty()
                    com.example.ui.components.SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        isSelected = isSelected,
                        selectionModeActive = isSelectionMode,
                        onLongClick = { onToggleSelection(expense.id) },
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(expense.id)
                            } else {
                                onEditExpense(expense)
                            }
                        },
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )"""
    code = re.sub(pattern, replacement2, code)
    with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
        f.write(code)

def update_tx():
    with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
        code = f.read()

    sig_pattern = r'fun TransactionsScreen\([\s\S]*?onDeleteExpense: \(ExpenseEntity\) -> Unit\s*\)'
    if "onToggleSelection: (String) -> Unit" not in code:
        replacement = r"""fun TransactionsScreen(
    uiState: UiState,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onDateRangeFilterChange: (String?) -> Unit = {},
    onMemberFilterChange: (String?) -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onToggleSelection: (String) -> Unit = {}
)"""
        code = re.sub(sig_pattern, replacement, code)
        
    pattern = r'com\.example\.ui\.components\.SwipeableExpenseItem\(\s*expense = expense,\s*currencySymbol = uiState\.currencySymbol,\s*onEditExpense = onEditExpense,\s*onDeleteExpense = onDeleteExpense\s*\)'
    
    replacement2 = """val isSelected = uiState.selectedExpenseIds.contains(expense.id)
                    val isSelectionMode = uiState.selectedExpenseIds.isNotEmpty()
                    com.example.ui.components.SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        isSelected = isSelected,
                        selectionModeActive = isSelectionMode,
                        onLongClick = { onToggleSelection(expense.id) },
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(expense.id)
                            } else {
                                onEditExpense(expense)
                            }
                        },
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )"""
    code = re.sub(pattern, replacement2, code)
    with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
        f.write(code)

update_home()
update_tx()
