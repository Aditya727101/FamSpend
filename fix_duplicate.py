import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

func = """
    fun duplicateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val newExpense = expense.copy(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis()
            )
            repository.insertExpense(newExpense)
            val updated = _uiState.value.expenses + newExpense
            _uiState.value = _uiState.value.copy(expenses = updated)
            applyFilters()
            calculateTotals()
        }
    }
"""

if "fun duplicateExpense" not in code:
    code = code.replace("fun deleteExpense(expense: ExpenseEntity)", func.strip() + "\n\n    fun deleteExpense(expense: ExpenseEntity)")

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
