import os

def replace_in_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    original = content
    content = content.replace("import androidx.compose.material.icons.filled.ReceiptLong", "import androidx.compose.material.icons.automirrored.filled.ReceiptLong")
    content = content.replace("import androidx.compose.material.icons.filled.ShowChart", "import androidx.compose.material.icons.automirrored.filled.ShowChart")
    content = content.replace("import androidx.compose.material.icons.filled.TrendingUp", "import androidx.compose.material.icons.automirrored.filled.TrendingUp")
    content = content.replace("import androidx.compose.material.icons.filled.Send", "import androidx.compose.material.icons.automirrored.filled.Send")
    content = content.replace("import androidx.compose.material.icons.filled.ArrowForward", "import androidx.compose.material.icons.automirrored.filled.ArrowForward")
    
    if original != content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")


for root, _, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            replace_in_file(os.path.join(root, file))

print("Done")
