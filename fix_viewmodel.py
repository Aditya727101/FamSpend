import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

replacement = """
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
        timestamp: Long = System.currentTimeMillis()
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
                    timestamp = timestamp
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
                    timestamp = timestamp,
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
"""

code = re.sub(r'fun saveExpense\s*\(.*?_uiState\.value\.copy\(\s*isAddExpenseDialogOpen = false,\s*editingExpense = null\s*\)\s*\}\s*\}', replacement.strip(), code, flags=re.DOTALL)

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
