import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

# Add to UiState
pattern_uistate = r'(val editingExpense: ExpenseEntity\? = null,\n)'
replacement_uistate = r'\1    val viewingExpense: ExpenseEntity? = null,\n'
if "val viewingExpense" not in code:
    code = re.sub(pattern_uistate, replacement_uistate, code)

# Add openViewExpenseDialog and closeViewExpenseDialog
pattern_methods = r'(fun openAddExpenseDialog\(expenseToEdit: ExpenseEntity\? = null\) \{)'
replacement_methods = r'''fun openViewExpenseDialog(expense: ExpenseEntity) {
        _uiState.value = _uiState.value.copy(viewingExpense = expense)
    }

    fun closeViewExpenseDialog() {
        _uiState.value = _uiState.value.copy(viewingExpense = null)
    }

    \1'''
if "fun openViewExpenseDialog" not in code:
    code = re.sub(pattern_methods, replacement_methods, code)

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
