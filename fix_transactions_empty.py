import re

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code = f.read()

replacement = """        if (uiState.filteredExpenses.isEmpty()) {
            item {
                EmptyExpensesState(
                    onAddExpenseClick = onAddExpenseClick,
                    onSimulateClick = {} 
                )
            }
        } else {
            items(
                items = uiState.filteredExpenses,
                key = { it.id }
            ) { expense ->"""

code = code.replace("""            items(
                items = uiState.filteredExpenses,
                key = { it.id }
            ) { expense ->""", replacement)

# Add closing brace for else block. 
# We need to find the end of items() {} block.
items_block = r'(\s*items\(\s*items = uiState\.filteredExpenses,\s*key = \{ it\.id \}\s*\) \{ expense ->[\s\S]*?onDelete = \{ onDeleteExpense\(expense\) \}\s*\)\s*\}\s*)'
replacement_full = r'        if (uiState.filteredExpenses.isEmpty()) {\n            item {\n                EmptyExpensesState(\n                    onAddExpenseClick = onAddExpenseClick,\n                    onSimulateClick = {} \n                )\n            }\n        } else {\n\1\n        }'
code = re.sub(items_block, replacement_full, code)

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code)
