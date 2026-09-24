import re

with open('app/src/main/java/com/example/ui/components/AddMemberDialog.kt', 'r') as f:
    code = f.read()

imports = """import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
"""
if "import android.widget.Toast" not in code:
    code = code.replace("import androidx.compose.foundation.layout.*", imports + "import androidx.compose.foundation.layout.*")

context_logic = "val context = LocalContext.current"
code = code.replace("var role by remember { mutableStateOf(\"Member\") }", "var role by remember { mutableStateOf(\"Member\") }\n    " + context_logic)

save_pattern = r'Button\(\s*onClick = \{\s*if \(name\.isNotBlank\(\)\) \{\s*onAdd\(name, role\)\s*onDismiss\(\)\s*\}\s*\}'
save_replacement = r'''Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name, role)
                            Toast.makeText(context, "Invitation sent to $name", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter a name or email", Toast.LENGTH_SHORT).show()
                        }
                    }'''

code = re.sub(save_pattern, save_replacement, code)

with open('app/src/main/java/com/example/ui/components/AddMemberDialog.kt', 'w') as f:
    f.write(code)
