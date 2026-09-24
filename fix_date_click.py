import re

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

# Replace the OutlinedTextField with a Box wrapping it
replacement = """
            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis())),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    enabled = false,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                )
            }
"""

code = re.sub(r'OutlinedTextField\(\s*value = SimpleDateFormat\("MMM dd, yyyy", Locale\.getDefault\(\)\)\.format\(Date\(datePickerState\.selectedDateMillis \?: System\.currentTimeMillis\(\)\)\),\s*onValueChange = \{\},\s*label = \{ Text\("Date"\) \},\s*readOnly = true,\s*modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ showDatePicker = true \},\s*trailingIcon = \{\s*IconButton\(onClick = \{ showDatePicker = true \}\) \{\s*Icon\(imageVector = Icons\.Default\.DateRange, contentDescription = "Select Date"\)\s*\}\s*\}\s*\)', replacement.strip(), code)

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)
