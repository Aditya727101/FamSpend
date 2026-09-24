import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

# Add imports for permissions
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
"""
if "rememberLauncherForActivityResult" not in code:
    code = code.replace("import androidx.compose.runtime.Composable", imports + "import androidx.compose.runtime.Composable")

effect_code = """
    // Request notification permission for budget alerts on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
"""

if "notificationPermissionLauncher" not in code:
    code = code.replace("var selectedTab by remember { mutableIntStateOf(0) }", "var selectedTab by remember { mutableIntStateOf(0) }\n" + effect_code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
