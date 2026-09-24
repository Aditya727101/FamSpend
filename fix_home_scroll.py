import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# Replace top-level Column with verticalScroll to LazyColumn
code = code.replace("Column(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(MaterialTheme.colorScheme.background)\n            .verticalScroll(scrollState)\n            .padding(horizontal = 14.dp, vertical = 12.dp)\n    ) {",
"""LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        item {
""")

# Replace the RecentExpensesList section
old_list = """        RecentExpensesList(
            expenses = uiState.expenses,
            currencySymbol = uiState.currencySymbol,
            maxItems = 6,
            onEditExpense = onEditExpense,
            onDeleteExpense = onDeleteExpense,
            onAddExpenseClick = onAddExpenseClick,
            onSimulateClick = onSimulateLiveClick
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}"""

new_list = """        }

        val displayList = uiState.expenses.take(6)
        if (displayList.isEmpty()) {
            item {
                com.example.ui.components.EmptyExpensesState(
                    onAddExpenseClick = onAddExpenseClick,
                    onSimulateClick = onSimulateLiveClick
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        } else {
            items(
                items = displayList,
                key = { it.id }
            ) { expense ->
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    com.example.ui.components.SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}"""

code = code.replace(old_list, new_list)

# Also need to make sure LazyColumn imports are correct, but it looks like we might need to add it if missing
if "import androidx.compose.foundation.lazy.LazyColumn" not in code:
    code = code.replace("import androidx.compose.foundation.lazy.LazyRow", "import androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.LazyRow")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

print("Done")
