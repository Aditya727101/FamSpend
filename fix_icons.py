import re

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'r') as f:
    code = f.read()

imports = """import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
"""
code = code.replace("import androidx.compose.material.icons.filled.Delete", imports + "import androidx.compose.material.icons.filled.Delete")

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'w') as f:
    f.write(code)
