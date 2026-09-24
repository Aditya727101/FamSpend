import re

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

# I will find the Button(onClick = { val amt = amountText.toDoubleOrNull() ... onSave(...) })
# We can just replace the whole onClick logic for the Save button.

# Need to find the Save button.
save_button_pattern = r'Button\(\s*onClick = \{\s*val amt = amountText\.toDoubleOrNull\(\)\s*if \(amt != null\) \{[\s\S]*?\}\s*\},'

save_button_replacement = r'''Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                            } else if (descriptionText.isBlank()) {
                                Toast.makeText(context, "Please enter a merchant or title", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(
                                    amt,
                                    selectedCategory,
                                    descriptionText,
                                    selectedMemberId,
                                    selectedMemberName,
                                    selectedSplit,
                                    selectedPaymentMethod,
                                    noteText,
                                    isRecurring,
                                    recurringFrequency,
                                    recurringDay
                                )
                                Toast.makeText(context, "Expense saved", Toast.LENGTH_SHORT).show()
                            }
                        },'''

code = re.sub(save_button_pattern, save_button_replacement, code)

# Let's also add Cancel Confirmation
cancel_button_pattern = r'OutlinedButton\(\s*onClick = onDismiss,\s*modifier = Modifier\.weight\(1f\)\s*\) \{\s*Text\("Cancel"\)\s*\}'
cancel_button_replacement = r'''var showCancelConfirm by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { 
                            if (amountText.isNotBlank() || descriptionText.isNotBlank()) {
                                showCancelConfirm = true
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    if (showCancelConfirm) {
                        AlertDialog(
                            onDismissRequest = { showCancelConfirm = false },
                            title = { Text("Discard changes?") },
                            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
                            confirmButton = {
                                Button(onClick = { showCancelConfirm = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Discard")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCancelConfirm = false }) {
                                    Text("Keep editing")
                                }
                            }
                        )
                    }'''

code = re.sub(cancel_button_pattern, cancel_button_replacement, code)

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)
