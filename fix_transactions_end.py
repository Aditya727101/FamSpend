import re

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code = f.read()

# We need to remove the first `} else {` before the `if (uiState.filteredExpenses.isEmpty()) {`
bad_block = """        } else {
        if (uiState.filteredExpenses.isEmpty()) {"""
good_block = """        if (uiState.filteredExpenses.isEmpty()) {"""
code = code.replace(bad_block, good_block)

# And check if there's a missing brace for the whole function.
# LazyColumn { ... } closes at 358, fun TransactionsScreen { ... } closes at 359?
# Wait, if I removed `} else {`, I need to remove one closing `}` at the end.
with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code)
