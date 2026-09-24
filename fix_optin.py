import re
with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Remove duplicate OptIn annotations preceding @Composable
code = re.sub(r'(@OptIn\(ExperimentalFoundationApi::class\)\s*){2,}', '@OptIn(ExperimentalFoundationApi::class)\n', code)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
