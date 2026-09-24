import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Make sure package is the very first line
# Find where package is
code = re.sub(r'^(.*?)(package com\.example\.ui\.components\n)', r'\2\1', code, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
