import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

# Fix conflicting imports
code = code.replace("import androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport android.widget.Toast\nimport androidx.compose.ui.platform.LocalContext\nimport android.os.Build", "import android.widget.Toast\nimport androidx.compose.ui.platform.LocalContext\nimport android.os.Build")

# Insert photoPickerLauncher right after var selectedTab by remember { mutableIntStateOf(0) }
picker_code = """
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Scanning receipt...", Toast.LENGTH_SHORT).show()
            viewModel.openAddExpenseDialog()
        }
    }
"""

code = code.replace("var selectedTab by remember { mutableIntStateOf(0) }", "var selectedTab by remember { mutableIntStateOf(0) }\n" + picker_code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)

