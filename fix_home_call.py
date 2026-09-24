import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

code = code.replace("onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },", 
                    "onDismissBudgetAlert = { viewModel.dismissBudgetAlert() },\n                    onEditBudgetClick = { selectedTab = 2 },")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
