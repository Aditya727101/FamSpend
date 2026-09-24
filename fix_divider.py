import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Replace Divider with HorizontalDivider
    content = content.replace("Divider(", "HorizontalDivider(")
    
    with open(filepath, 'w') as f:
        f.write(content)
    print(f"Updated {filepath}")

replace_in_file('app/src/main/java/com/example/ui/components/MonthlySummaryDialog.kt')
