import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# I need to add onInviteClick to HomeScreen args
code = code.replace("onDeleteExpense: (ExpenseEntity) -> Unit,", "onDeleteExpense: (ExpenseEntity) -> Unit,\n    onInviteClick: () -> Unit = {},")

# And call it
code = code.replace("Button(onClick = { /* Invite logic */ })", "Button(onClick = onInviteClick)")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code_shell = f.read()

code_shell = code_shell.replace("onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },", "onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },\n                    onInviteClick = { viewModel.openAddMemberDialog() },")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code_shell)

