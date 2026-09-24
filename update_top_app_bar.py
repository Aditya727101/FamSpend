import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

top_bar_pattern = r'(topBar = \{\s*)(TopAppBar\()'

contextual_top_bar = """
            if (uiState.selectedExpenseIds.isNotEmpty()) {
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
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                TopAppBar(
"""

if "uiState.selectedExpenseIds.isNotEmpty()" not in code:
    code = re.sub(top_bar_pattern, r'\1' + contextual_top_bar, code, count=1)
    # also need to close the else block
    # We find where TopAppBar ends. 
    # Let's just do a simpler replacement

    # Let's do it with python string manipulation
    pass
