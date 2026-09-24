import re

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    code = f.read()

if "import androidx.compose.ui.graphics.toArgb" not in code:
    code = code.replace("import androidx.compose.ui.graphics.nativeCanvas", "import androidx.compose.ui.graphics.nativeCanvas\nimport androidx.compose.ui.graphics.toArgb")

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(code)

print("Done")
