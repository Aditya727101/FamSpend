import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# To simplify, we will just provide a completely new HomeScreen layout inside the function body.
# First, let's find the method signature and grab everything else.

start_sig = r"fun HomeScreen\([\s\S]*?onEditBudgetClick: \(\) -> Unit = \{\}\s*\)\s*\{"
new_body = """
    val scrollState = rememberScrollState()

    if (uiState.budgetNotification != null && !uiState.isBudgetAlertDismissed) {
        AlertDialog(
            onDismissRequest = onDismissBudgetAlert,
            title = { Text(text = uiState.budgetNotification.title, fontWeight = FontWeight.Bold) },
            text = { Text(text = uiState.budgetNotification.message) },
            confirmButton = { Button(onClick = onDismissBudgetAlert) { Text("Got it") } }
        )
    }

    val spent = uiState.totalSpentThisMonth
    val limit = uiState.monthlyBudgetLimit
    val remaining = if (limit > 0) limit - spent else 0.0
    val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val percentUsed = (progress * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp) // Space for FAB
    ) {
        // Household Budget Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("My Household", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("September 2026", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha=0.5f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Synced just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("${uiState.currencySymbol}${String.format(java.util.Locale.US, "%.2f", remaining)} left", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("of ${uiState.currencySymbol}${String.format(java.util.Locale.US, "%.0f", limit)} monthly budget", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${uiState.currencySymbol}${String.format(java.util.Locale.US, "%.2f", spent)} spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                        Text("$percentUsed%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAddExpenseClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                            Text("Add expense", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onEditBudgetClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                            Text("View budget")
                        }
                    }
                }
            }
        }

        // This month at a glance
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("This month at a glance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category Insights (dummy for visual styling as requested)
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.PieChart, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Groceries", style = MaterialTheme.typography.labelMedium)
                            Text("${uiState.currencySymbol}450", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("40% of spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.5f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Upcoming Bills", style = MaterialTheme.typography.labelMedium)
                            Text("2 pending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Due in 5 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Household Members
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Household members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (uiState.members.size <= 1) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Invite family members to track shared expenses together.", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { /* Invite logic */ }) { Text("Invite member") }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.members.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ui.components.MemberAvatar(member = member, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(if(member.isAdmin) "Admin" else "Member", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${uiState.currencySymbol}0.00", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text("0% share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Expenses
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onViewAllTransactions) {
                        Text("View all")
                    }
                }
            }
        }
        
        val displayList = uiState.expenses.take(6)
        if (displayList.isEmpty()) {
            item {
                com.example.ui.components.EmptyExpensesState(onAddExpenseClick = onAddExpenseClick, onSimulateClick = onSimulateLiveClick)
            }
        } else {
            items(count = displayList.size, key = { it -> displayList[it].id }) { index ->
                val expense = displayList[index]
                Box(modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)) {
                    val isSelected = uiState.selectedExpenseIds.contains(expense.id)
                    val isSelectionMode = uiState.selectedExpenseIds.isNotEmpty()
                    com.example.ui.components.RecentExpenseCard(
                        expense = expense,
                        currencySymbol = uiState.currencySymbol,
                        isSelected = isSelected,
                        selectionModeActive = isSelectionMode,
                        onLongClick = { onToggleSelection(expense.id) },
                        onClick = {
                            if (isSelectionMode) onToggleSelection(expense.id)
                            else onViewExpense(expense)
                        },
                        onEdit = { onEditExpense(expense) },
                        onDelete = { onDeleteExpense(expense) }
                    )
                }
            }
        }
    }
}
"""

match = re.search(start_sig, code)
if match:
    # Replace the body of HomeScreen
    # We find the matching closing brace. Since we regex match up to '{', we just take everything before the match + the match + the new body.
    # We drop the old body.
    new_code = code[:match.end()] + new_body
    
    # We also need to fix some missing imports if any
    new_code = new_code.replace("import androidx.compose.foundation.layout.padding", "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.PaddingValues\nimport androidx.compose.ui.text.style.TextAlign\nimport androidx.compose.material.icons.filled.FileCopy\nimport androidx.compose.material.icons.filled.Download\nimport androidx.compose.material.icons.filled.MoreHoriz")
    
    with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
        f.write(new_code)
