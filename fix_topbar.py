import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

pattern = r'(topBar = \{\s*)(TopAppBar\([\s\S]*?colors = TopAppBarDefaults\.topAppBarColors\([\s\S]*?containerColor = MaterialTheme\.colorScheme\.surface\s*\)\s*\))'

new_code = r"""\1if (uiState.selectedExpenseIds.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedExpenseIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearExpenseSelection() }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelectedExpenses() }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                \2
            }"""

if "uiState.selectedExpenseIds.isNotEmpty()" not in code:
    code = re.sub(pattern, new_code, code)
    if "import androidx.compose.material.icons.filled.Delete" not in code:
        code = code.replace("import androidx.compose.material.icons.filled.DarkMode", "import androidx.compose.material.icons.filled.DarkMode\nimport androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.Close")
    
    with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
        f.write(code)
