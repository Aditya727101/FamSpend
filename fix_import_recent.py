import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Fix package statement issues
code = re.sub(r'^(import [^\n]+)\n(package com\.example\.ui\.components)', r'\2\n\1', code)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
