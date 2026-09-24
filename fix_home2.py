import re
with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# Fix that exact block
code = code.replace("""        } else {
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
}

@Composable
fun ExpenseItemCard""", """        } else {
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
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun ExpenseItemCard""")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

print("Done")
