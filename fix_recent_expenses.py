import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Strip out the bad prepended imports
code = re.sub(r'^.*?package com\.example\.ui\.components', 'package com.example.ui.components', code, flags=re.DOTALL)

# Add the missing imports correctly after the package statement
imports_to_add = """
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.CheckCircle
"""
code = code.replace("package com.example.ui.components", "package com.example.ui.components\n" + imports_to_add)

# Fix duplicate ExperimentalFoundationApi annotations
code = re.sub(r'(@OptIn\(ExperimentalFoundationApi::class\)\s*){2,}', '@OptIn(ExperimentalFoundationApi::class)\n', code)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
