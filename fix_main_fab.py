import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

imports = """import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.foundation.layout.Arrangement
"""
code = code.replace("import androidx.compose.material3.ExperimentalMaterial3Api", imports + "import androidx.compose.material3.ExperimentalMaterial3Api")


quick_add_sheet = """
            if (uiState.isFabMenuOpen) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closeFabMenu() },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "New Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.closeFabMenu()
                                viewModel.openAddExpenseDialog()
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Add expense", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.closeFabMenu() }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Add income", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.closeFabMenu() }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Transfer between accounts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.closeFabMenu() }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Scan receipt", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.closeFabMenu() }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Split expense", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            if (uiState.isAddExpenseDialogOpen) {"""

code = code.replace("if (uiState.isAddExpenseDialogOpen) {", quick_add_sheet)

fab_code = """FloatingActionButton(
                    onClick = { viewModel.toggleFabMenu() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,"""
code = code.replace("""FloatingActionButton(
                    onClick = { viewModel.openAddExpenseDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,""", fab_code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
