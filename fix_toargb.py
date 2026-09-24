import re

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    code = f.read()

code = code.replace("androidx.compose.ui.graphics.toArgb(textColor)", "textColor.toArgb()")

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(code)

print("Done")
