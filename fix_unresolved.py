import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    
    original = content
    for old, new in replacements:
        content = content.replace(old, new)
        
    if original != content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")

# For Icons, let's just use Icons.AutoMirrored.Filled.XXX and ensure Icons is imported
# The previous replacement replaced "Icons.Default.ReceiptLong" with the fully qualified package which might not work if it's an object property.
# Actually, `androidx.compose.material.icons.automirrored.filled.ReceiptLong` IS the property if it's imported.
# Let's revert the icons to `Icons.AutoMirrored.Filled.XXX`

replacements_icons = [
    ("androidx.compose.material.icons.automirrored.filled.ReceiptLong", "androidx.compose.material.icons.automirrored.filled.ReceiptLong"), # Wait, let's just use Icons.AutoMirrored.Filled.ReceiptLong
    ("androidx.compose.material.icons.automirrored.filled.ShowChart", "Icons.AutoMirrored.Filled.ShowChart"),
    ("androidx.compose.material.icons.automirrored.filled.TrendingUp", "Icons.AutoMirrored.Filled.TrendingUp"),
    ("androidx.compose.material.icons.automirrored.filled.Send", "Icons.AutoMirrored.Filled.Send"),
    ("androidx.compose.material.icons.automirrored.filled.ArrowForward", "Icons.AutoMirrored.Filled.ArrowForward"),
    ("androidx.compose.material.icons.automirrored.filled.ReceiptLong", "Icons.AutoMirrored.Filled.ReceiptLong")
]

for root, _, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file), replacements_icons)

# For HorizontalDivider in MonthlySummaryDialog.kt
with open('app/src/main/java/com/example/ui/components/MonthlySummaryDialog.kt', 'r') as f:
    msd = f.read()
msd = msd.replace("import androidx.compose.material3.Divider", "import androidx.compose.material3.HorizontalDivider")
if "import androidx.compose.material3.HorizontalDivider" not in msd:
    msd = msd.replace("import androidx.compose.material3.Text", "import androidx.compose.material3.Text\nimport androidx.compose.material3.HorizontalDivider")
with open('app/src/main/java/com/example/ui/components/MonthlySummaryDialog.kt', 'w') as f:
    f.write(msd)

print("Done")
