import re

with open('app/src/main/java/com/example/ui/components/EditBudgetDialog.kt', 'r') as f:
    code = f.read()

# Replace menuAnchor() with menuAnchor(type = MenuAnchorType.PrimaryNotEditable) or similar
code = code.replace(".menuAnchor()", ".menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)")

with open('app/src/main/java/com/example/ui/components/EditBudgetDialog.kt', 'w') as f:
    f.write(code)

print("Done")
