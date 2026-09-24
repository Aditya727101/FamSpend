import re

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

# Add a launcher and receipt attachment UI
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
"""
if "import android.widget.Toast" not in code:
    code = code.replace("import androidx.compose.foundation.background", imports + "import androidx.compose.foundation.background")

receipt_ui = """
            val context = LocalContext.current
            val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    Toast.makeText(context, "Receipt attached successfully", Toast.LENGTH_SHORT).show()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Add Receipt", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Receipt Photo")
            }
"""

code = code.replace("            Spacer(modifier = Modifier.height(24.dp))\n\n            HorizontalDivider()", receipt_ui + "            Spacer(modifier = Modifier.height(24.dp))\n\n            HorizontalDivider()")

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)
