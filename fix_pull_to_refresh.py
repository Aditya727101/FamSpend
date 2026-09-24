import re

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code = f.read()

# Add a title row with a refresh button
imports = """import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
"""
if "import androidx.compose.material.icons.filled.Refresh" not in code:
    code = code.replace("import androidx.compose.material.icons.filled.Search", imports + "import androidx.compose.material.icons.filled.Search")

header = """
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expenses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onSearchChange("") /* Simulating a refresh/clear action */ }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
"""
code = code.replace("    ) {\n        item {\n        // Search Bar", "    ) {\n" + header + "        item {\n        // Search Bar")

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code)
