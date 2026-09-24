import re

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'r') as f:
    code = f.read()

imports = """import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
"""
if "import android.widget.Toast" not in code:
    code = code.replace("import androidx.compose.foundation.background", imports + "import androidx.compose.foundation.background")

# Find the DropdownMenu
dropdown = """val context = LocalContext.current
                    DropdownMenu(
                        expanded = moreMenu,
                        onDismissRequest = { moreMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("Duplicate expense") }, onClick = { 
                            moreMenu = false
                            Toast.makeText(context, "Duplicated expense", Toast.LENGTH_SHORT).show()
                            // call viewModel.duplicateExpense(expense)
                        }, leadingIcon = { Icon(Icons.Default.FileCopy, null) })
                        
                        DropdownMenuItem(text = { Text("Update split") }, onClick = { 
                            moreMenu = false
                            onEdit(expense) // Opens edit dialog to update split
                        }, leadingIcon = { Icon(Icons.Default.CallSplit, null) })
                        
                        if (expense.splitType != "Paid Individual") {
                            DropdownMenuItem(text = { Text("Record settlement") }, onClick = { 
                                moreMenu = false
                                Toast.makeText(context, "Settlement recorded", Toast.LENGTH_SHORT).show()
                            }, leadingIcon = { Icon(Icons.Default.PriceCheck, null) })
                            
                            DropdownMenuItem(text = { Text("Send payment reminder") }, onClick = { 
                                moreMenu = false
                                Toast.makeText(context, "Reminder sent to members", Toast.LENGTH_SHORT).show()
                            }, leadingIcon = { Icon(Icons.Default.NotificationsActive, null) })
                        }
                        
                        DropdownMenuItem(text = { Text("Add/view receipt") }, onClick = { 
                            moreMenu = false
                            Toast.makeText(context, "Receipt viewer opened", Toast.LENGTH_SHORT).show()
                        }, leadingIcon = { Icon(Icons.Default.Receipt, null) })
                        
                        DropdownMenuItem(text = { Text("Share expense") }, onClick = { 
                            moreMenu = false
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Expense: ${expense.description} for ${currencySymbol}${expense.amount}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }, leadingIcon = { Icon(Icons.Default.Share, null) })
                        
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { moreMenu = false; showDeleteConfirm = true },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }"""

code = re.sub(r'DropdownMenu\([\s\S]*?\}\s*\}\s*\}\s*\}\s*if \(showDeleteConfirm\)', dropdown + '\n                }\n            }\n        }\n    }\n    \n    if (showDeleteConfirm)', code)

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'w') as f:
    f.write(code)
