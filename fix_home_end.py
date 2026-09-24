import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# Let's replace the whole Recent Real-Time Family Activity Section down to the end of the HomeScreen function
old_section = """        // Recent Real-Time Family Activity Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
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
        
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.expenses.isEmpty()) {
            com.example.ui.components.EmptyExpensesState(
                onAddExpenseClick = onAddExpenseClick,
                onSimulateClick = onSimulateLiveClick
            )
            Spacer(modifier = Modifier.height(28.dp))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.expenses.take(6).forEach { expense ->
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    com.example.ui.components.SwipeableExpenseItem(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )
                }
                
                Spacer(modifier = Modifier.height(18.dp))

        }
        }
        }
    }
}"""

new_section = """        } // close item

        item {
            // Recent Real-Time Family Activity Section
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.automirrored.filled.ArrowForward, // just a placeholder since Bolt is removed
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
}"""

# Bolt is Icons.Default.Bolt. Let's fix that.
new_section = new_section.replace("androidx.compose.material.icons.automirrored.filled.ArrowForward, // just a placeholder since Bolt is removed", "androidx.compose.material.icons.Icons.Default.Bolt,")

if old_section in code:
    code = code.replace(old_section, new_section)
    print("Replaced section.")
else:
    print("Section not found. Checking diffs.")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

