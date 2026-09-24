import re
with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

code = code.replace("package com.example.ui.componentsimport", "package com.example.ui.components\nimport")
with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
