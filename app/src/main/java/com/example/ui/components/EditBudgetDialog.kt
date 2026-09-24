package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetDialog(
    currentTotalBudget: Double,
    currentDailyLimit: Double,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSaveCategoryBudget: (categoryName: String, monthlyLimit: Double) -> Unit,
    onSaveTotalBudget: (totalMonthlyBudget: Double) -> Unit,
    onSaveDailyLimit: (dailyLimit: Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(CategoryHelper.allCategories.first().name) }
    var categoryLimitText by remember { mutableStateOf("") }
    var totalBudgetLimitText by remember { mutableStateOf(currentTotalBudget.toString()) }
    var dailyLimitText by remember { mutableStateOf(if (currentDailyLimit > 0) currentDailyLimit.toString() else "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Monthly Family Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Overall Family Monthly Target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = totalBudgetLimitText,
                    onValueChange = { totalBudgetLimitText = it },
                    label = { Text("Total Monthly Family Limit ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("total_budget_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Personal Daily Limit Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = dailyLimitText,
                    onValueChange = { dailyLimitText = it },
                    label = { Text("Daily Limit ($currencySymbol)") },
                    placeholder = { Text("e.g., 500 (0 to disable)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("daily_limit_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Category Budget Limit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CategoryHelper.allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = categoryLimitText,
                    onValueChange = {
                        categoryLimitText = it
                        errorMessage = null
                    },
                    label = { Text("Category Monthly Limit ($)") },
                    placeholder = { Text("e.g. 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_budget_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalBudgetLimitText.toDoubleOrNull()
                    if (total != null && total > 0) {
                        onSaveTotalBudget(total)
                    }

                    val catLimit = categoryLimitText.toDoubleOrNull()
                    if (catLimit != null && catLimit > 0) {
                        onSaveCategoryBudget(selectedCategory, catLimit)
                    }

                    val dailyLimit = dailyLimitText.toDoubleOrNull() ?: 0.0
                    onSaveDailyLimit(dailyLimit)

                    onDismiss()
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save Budgets")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
