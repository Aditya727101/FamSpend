import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Replace all occurrences of "import" that don't have a newline before them (or space in between)
code = re.sub(r'([A-Za-z0-9_])import ', r'\1\nimport ', code)
code = re.sub(r'(@OptIn\(ExperimentalFoundationApi::class\)\s*){2,}', '@OptIn(ExperimentalFoundationApi::class)\n', code)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)

