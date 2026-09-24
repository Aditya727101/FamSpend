import re

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code = f.read()

# 1. Replace Column with LazyColumn
code = code.replace("Column(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(MaterialTheme.colorScheme.background)\n            .padding(16.dp)\n    ) {", 
"""LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
""")

# 2. Add an item { } wrap before Category Filter Row
code = code.replace("        // Category Filter Row", 
"""        }
        item {
        // Category Filter Row""")

# 3. Add an item { } wrap before Family Member Filter Row
code = code.replace("        // Family Member Filter Row", 
"""        }
        item {
        // Family Member Filter Row""")

# 4. Add an item { } wrap before List Header Row
code = code.replace("        // List Header Row with Export Action", 
"""        }
        item {
        // List Header Row with Export Action""")

# 5. Add an item { } wrap for Empty State, and flatten LazyColumn items for populated state
# Note: we need to find "// Expenses List" and replace the rest of the function correctly.
empty_list_str = """        // Expenses List
        if (uiState.filteredExpenses.isEmpty()) {
            val isFiltering = uiState.searchQuery.isNotEmpty() || uiState.selectedCategoryFilter != null || uiState.selectedDateRangeFilter != null || uiState.selectedMemberFilter != null
            EmptyExpensesState(
                title = if (isFiltering) "No Matching Expenses Found" else "No Expenses Logged Yet",
                subtitle = if (isFiltering) "Try adjusting or clearing your search keywords, category tags, or date filters to view expenses." else "Start tracking family expenses together. Log groceries, house maintenance, bills, or leisure.",
                onAddExpenseClick = if (!isFiltering) onAddExpenseClick else null
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.filteredExpenses,
                    key = { it.id }
                ) { expense ->
                    SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )
                }
            }
        }
    }
}"""

replacement_str = """        }
        
        // Expenses List
        if (uiState.filteredExpenses.isEmpty()) {
            item {
                val isFiltering = uiState.searchQuery.isNotEmpty() || uiState.selectedCategoryFilter != null || uiState.selectedDateRangeFilter != null || uiState.selectedMemberFilter != null
                EmptyExpensesState(
                    title = if (isFiltering) "No Matching Expenses Found" else "No Expenses Logged Yet",
                    subtitle = if (isFiltering) "Try adjusting or clearing your search keywords, category tags, or date filters to view expenses." else "Start tracking family expenses together. Log groceries, house maintenance, bills, or leisure.",
                    onAddExpenseClick = if (!isFiltering) onAddExpenseClick else null
                )
            }
        } else {
            items(
                items = uiState.filteredExpenses,
                key = { it.id }
            ) { expense ->
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )
                }
            }
        }
    }
}"""

code = code.replace(empty_list_str, replacement_str)

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code)

print("Done")
