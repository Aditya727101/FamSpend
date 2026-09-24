import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

code = code.replace("private fun setSortOrder", "fun setSortOrder")
code = code.replace("repository.insertExpense(newExpense)", "repository.addExpense(newExpense)")
code = code.replace("val updated = _uiState.value.expenses + newExpense\n            _uiState.value = _uiState.value.copy(expenses = updated)\n            applyFilters()\n            calculateTotals()", "")

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
