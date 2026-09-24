with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

replacement = """actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.material3.IconButton(
                        onClick = { viewModel.signInWithGoogle(context) }
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AccountCircle,
                            contentDescription = "Sign in to Sync",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Active User Profile Switcher Chip"""

# Ensure import exists
if "import androidx.compose.material.icons.filled.AccountCircle" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.AccountBalanceWallet",
                        "import androidx.compose.material.icons.filled.AccountBalanceWallet\nimport androidx.compose.material.icons.filled.AccountCircle")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)

print("Done")
