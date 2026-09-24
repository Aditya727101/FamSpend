import re

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'r') as f:
    code = f.read()

# Make sure we add `onDuplicate` to signature
code = code.replace("onDelete: (ExpenseEntity) -> Unit\n) {", "onDelete: (ExpenseEntity) -> Unit,\n    onDuplicate: (ExpenseEntity) -> Unit = {}\n) {")
code = code.replace("// call viewModel.duplicateExpense(expense)", "onDuplicate(expense)")

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code_shell = f.read()

code_shell = code_shell.replace("onDelete = {\n                        viewModel.closeViewExpenseDialog()\n                        handleDeleteExpenseWithUndo(it)\n                    }", "onDelete = {\n                        viewModel.closeViewExpenseDialog()\n                        handleDeleteExpenseWithUndo(it)\n                    },\n                    onDuplicate = {\n                        viewModel.closeViewExpenseDialog()\n                        viewModel.duplicateExpense(it)\n                    }")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code_shell)
