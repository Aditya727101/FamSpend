import re

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

replacement = """
                        Spacer(modifier = Modifier.height(12.dp))

                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        recurringDayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)

                        // Day of Month Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-renews on this day every month",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Day $recurringDayOfMonth",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
"""

code = re.sub(r'Spacer\(modifier = Modifier\.height\(12\.dp\)\)\s*// Day of Month Selection.*?modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.testTag\("recurring_day_slider"\)\s*\)', replacement.strip(), code, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)
