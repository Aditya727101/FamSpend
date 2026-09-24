import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

# 1. Add sort filter to UI state
code = code.replace("val selectedDateRangeFilter: String? = null,", "val selectedDateRangeFilter: String? = null,\n    val selectedSortOrder: String = \"Newest first\",")

# 2. Add sort function
sort_func = """fun setSortOrder(order: String) {
        _uiState.value = _uiState.value.copy(selectedSortOrder = order)
        applyFilters()
    }
"""
code = code.replace("fun applyFilters()", sort_func + "\n    fun applyFilters()")

# 3. Apply sort in applyFilters
sort_logic = """
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter {
                it.description.lowercase().contains(q) ||
                it.note.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.paidByMemberName.lowercase().contains(q)
            }
        }
        
        list = when (state.selectedSortOrder) {
            "Oldest first" -> list.sortedBy { it.timestamp }
            "Highest amount" -> list.sortedByDescending { it.amount }
            "Lowest amount" -> list.sortedBy { it.amount }
            else -> list.sortedByDescending { it.timestamp } // Newest first
        }

        _uiState.value = _uiState.value.copy(filteredExpenses = list)
    }
"""
code = re.sub(r'if \(state\.searchQuery\.isNotBlank\(\)\) \{[\s\S]*?copy\(filteredExpenses = list\)\s*\}', sort_logic.strip(), code)

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
