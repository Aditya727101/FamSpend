import re

with open('app/src/main/java/com/example/ui/screens/BudgetsScreen.kt', 'r') as f:
    code = f.read()

# Replace top-level Column with verticalScroll to LazyColumn
code = code.replace("Column(\n        modifier = Modifier\n            .fillMaxSize()\n            .background(MaterialTheme.colorScheme.background)\n            .verticalScroll(scrollState)\n            .padding(16.dp)\n    ) {",
"""androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
""")

# Fix the end by replacing the items rendering section
old_end = """                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recurringTemplates.forEach { template ->
                            val createdThisMonth = uiState.expenses.any { exp ->
                                exp.isAutoCreated &&
                                exp.description.equals(template.description, ignoreCase = true) &&
                                sdfMonth.format(Date(exp.timestamp)) == currentMonthStr
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${template.category} • Day ${template.recurringDayOfMonth} of month (${template.recurringFrequency})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", template.amount)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (createdThisMonth) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ) {
                                            Text(
                                                text = if (createdThisMonth) "Paid" else "Upcoming",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (createdThisMonth) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}"""

new_end = """                    }
                }
            }
        }

        if (recurringTemplates.isNotEmpty()) {
            items(
                items = recurringTemplates,
                key = { it.id }
            ) { template ->
                val createdThisMonth = uiState.expenses.any { exp ->
                    exp.isAutoCreated &&
                    exp.description.equals(template.description, ignoreCase = true) &&
                    sdfMonth.format(Date(exp.timestamp)) == currentMonthStr
                }

                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${template.category} • Day ${template.recurringDayOfMonth} of month (${template.recurringFrequency})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", template.amount)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (createdThisMonth) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = if (createdThisMonth) "Paid" else "Upcoming",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (createdThisMonth) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}"""

code = code.replace(old_end, new_end)

with open('app/src/main/java/com/example/ui/screens/BudgetsScreen.kt', 'w') as f:
    f.write(code)

print("Done")
