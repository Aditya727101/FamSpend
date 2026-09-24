with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# Put everything in item { } blocks in HomeScreen LazyColumn where we missed it

replacement = """        item {
            Spacer(modifier = Modifier.height(20.dp))
    
            // Spending Trend & Spikes Chart
            SpendingTrendChart(
                expenses = uiState.expenses,
                currencySymbol = uiState.currencySymbol
            )
    
            Spacer(modifier = Modifier.height(20.dp))
    
            // Family Member Spending Breakdown
            Text(
                text = "Family Members Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val memberSpentMap = remember(uiState.expenses) {
            val map = mutableMapOf<String, Double>()
            for (expense in uiState.expenses) {
                val mId = expense.paidByMemberId
                map[mId] = (map[mId] ?: 0.0) + expense.amount
            }
            map
        }

        item {
            LazyRow("""

import re
code = re.sub(r"\s*Spacer\(modifier = Modifier.height\(20.dp\)\)\s*// Spending Trend & Spikes Chart\s*SpendingTrendChart\(\s*expenses = uiState.expenses,\s*currencySymbol = uiState.currencySymbol\s*\)\s*Spacer\(modifier = Modifier.height\(20.dp\)\)\s*// Family Member Spending Breakdown\s*Text\(\s*text = \"Family Members Breakdown\",\s*style = MaterialTheme.typography.titleMedium,\s*fontWeight = FontWeight.Bold\s*\)\s*Spacer\(modifier = Modifier.height\(8.dp\)\)\s*val memberSpentMap = remember\(uiState.expenses\) \{\s*val map = mutableMapOf<String, Double>\(\)\s*for \(expense in uiState.expenses\) \{\s*val mId = expense.paidByMemberId\s*map\[mId\] = \(map\[mId\] \?: 0.0\) \+ expense.amount\s*\}\s*map\s*\}\s*LazyRow\(", replacement, code, flags=re.MULTILINE)

# Now we need to close the item { LazyRow() } 
# Let's find the end of the LazyRow
code = code.replace("""                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }""", """                    }
                }
            }
        }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }""")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

print("Done")
