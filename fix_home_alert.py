import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

imports = """
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
"""
if "import androidx.compose.material3.AlertDialog" not in code:
    code = code.replace("import androidx.compose.material3.Text", imports + "import androidx.compose.material3.Text")

alert_code = """
    if (uiState.budgetNotification != null && !uiState.isBudgetAlertDismissed) {
        AlertDialog(
            onDismissRequest = onDismissBudgetAlert,
            title = {
                Text(text = uiState.budgetNotification.title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = uiState.budgetNotification.message)
            },
            confirmButton = {
                TextButton(onClick = onDismissBudgetAlert) {
                    Text("Got it")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDismissBudgetAlert()
                    onEditBudgetClick()
                }) {
                    Text("Adjust Budget")
                }
            },
            containerColor = if (uiState.budgetNotification.level == com.example.viewmodel.BudgetAlertLevel.EXCEEDED_100) 
                MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = if (uiState.budgetNotification.level == com.example.viewmodel.BudgetAlertLevel.EXCEEDED_100) 
                MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textContentColor = if (uiState.budgetNotification.level == com.example.viewmodel.BudgetAlertLevel.EXCEEDED_100) 
                MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
"""

code = code.replace("val scrollState = rememberScrollState()", "val scrollState = rememberScrollState()\n" + alert_code)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)
