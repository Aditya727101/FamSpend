import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# Replace the specific Column and its contents
code = re.sub(
    r'Column\(\s*modifier = Modifier\.fillMaxSize\(\)\.background\(MaterialTheme\.colorScheme\.background\)\.verticalScroll\(scrollState\)\.padding\(horizontal = 14\.dp, vertical = 12\.dp\)\s*\)\s*\{',
    """androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        item {""",
    code
)

# And now we need to replace the bottom part.
# Let's find RecentExpensesList
code = re.sub(
    r'RecentExpensesList\([\s\S]*?onSimulateClick = onSimulateLiveClick\s*\)\s*Spacer\(modifier = Modifier\.height\(28\.dp\)\)\s*\}\s*\}',
    """}
        
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            items(
                count = displayList.size,
                key = { index -> displayList[index].id }
            ) { index ->
                val expense = displayList[index]
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
}""",
    code
)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

print("Done")
