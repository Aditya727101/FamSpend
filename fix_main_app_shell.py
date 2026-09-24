import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

# For HomeScreen
code = code.replace("onEditExpense = { viewModel.openAddExpenseDialog(it) },\n                    onDeleteExpense = handleDeleteExpenseWithUndo", 
                    "onEditExpense = { viewModel.openAddExpenseDialog(it) },\n                    onViewExpense = { viewModel.openViewExpenseDialog(it) },\n                    onDeleteExpense = handleDeleteExpenseWithUndo")

# For TransactionsScreen
code = code.replace("onEditExpense = { viewModel.openAddExpenseDialog(it) },\n                    onToggleSelection = { viewModel.toggleExpenseSelection(it) }", 
                    "onEditExpense = { viewModel.openAddExpenseDialog(it) },\n                    onViewExpense = { viewModel.openViewExpenseDialog(it) },\n                    onToggleSelection = { viewModel.toggleExpenseSelection(it) }")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
