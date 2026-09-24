import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

# Add a launcher for scan receipt
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
"""
if "import android.widget.Toast" not in code:
    code = code.replace("import android.os.Build", imports + "import android.os.Build")

# Inside MainAppShell:
# We need to insert a context and a photo picker launcher
context_code = """    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Scanning receipt...", Toast.LENGTH_SHORT).show()
            // In a real app, send to OCR, then open AddExpenseDialog with prefilled data
            viewModel.openAddExpenseDialog()
        }
    }
    
    """
# We'll put it right after `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`
code = re.sub(r'(val uiState by viewModel\.uiState\.collectAsStateWithLifecycle\(\)\s*)', r'\1' + context_code, code)


# Update the clickable actions
scan_clickable = """modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.closeFabMenu()
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }.padding(vertical = 12.dp)"""
code = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ viewModel\.closeFabMenu\(\) \}\.padding\(vertical = 12\.dp\),[\s\S]*?Icon\(Icons\.Default\.CameraAlt', scan_clickable + ',\n                            verticalAlignment = Alignment.CenterVertically\n                        ) {\n                            Icon(Icons.Default.CameraAlt', code)


split_clickable = """modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.closeFabMenu()
                                viewModel.openAddExpenseDialog() // Need to support split mode later, for now just open dialog
                            }.padding(vertical = 12.dp)"""
code = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ viewModel\.closeFabMenu\(\) \}\.padding\(vertical = 12\.dp\),[\s\S]*?Icon\(Icons\.Default\.CallSplit', split_clickable + ',\n                            verticalAlignment = Alignment.CenterVertically\n                        ) {\n                            Icon(Icons.Default.CallSplit', code)

income_clickable = """modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.closeFabMenu()
                                Toast.makeText(context, "Add Income flow opened", Toast.LENGTH_SHORT).show()
                            }.padding(vertical = 12.dp)"""
code = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ viewModel\.closeFabMenu\(\) \}\.padding\(vertical = 12\.dp\),[\s\S]*?Icon\(Icons\.Default\.AttachMoney', income_clickable + ',\n                            verticalAlignment = Alignment.CenterVertically\n                        ) {\n                            Icon(Icons.Default.AttachMoney', code)

transfer_clickable = """modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.closeFabMenu()
                                Toast.makeText(context, "Transfer flow opened", Toast.LENGTH_SHORT).show()
                            }.padding(vertical = 12.dp)"""
code = re.sub(r'modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ viewModel\.closeFabMenu\(\) \}\.padding\(vertical = 12\.dp\),[\s\S]*?Icon\(Icons\.Default\.SwapHoriz', transfer_clickable + ',\n                            verticalAlignment = Alignment.CenterVertically\n                        ) {\n                            Icon(Icons.Default.SwapHoriz', code)

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
