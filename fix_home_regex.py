import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

new_section = """} // close item

        item {
            // Recent Real-Time Family Activity Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Bolt,
                        contentDescription = "Live Feed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recent Family Spending Feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onViewAllTransactions) {
                    Text("View All")
                    Icon(
                        imageVector = androidx.compose.material.icons.automirrored.filled.ArrowForward,
                        contentDescription = "View All",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
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
}

@Composable
fun ExpenseItemCard("""

code = re.sub(
    r'// Recent Real-Time Family Activity Section[\s\S]*?@Composable\s*fun ExpenseItemCard\(',
    new_section,
    code
)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

print("Done")
