import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

code = code.replace("onEditBudgetClick = { selectedTab = 2 },\n                    onEditBudgetClick = { viewModel.openEditBudgetDialog() }", "onEditBudgetClick = { selectedTab = 2 }")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code_t = f.read()

# I will just append a } at the end.
code_t += "\n}\n"

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code_t)

