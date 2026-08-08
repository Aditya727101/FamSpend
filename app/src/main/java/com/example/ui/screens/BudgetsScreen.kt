package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudgetEntity
import com.example.ui.components.BudgetAlertBanner
import com.example.ui.components.CategoryHelper
import com.example.ui.components.WeeklyReminderCard
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BudgetsScreen(
    uiState: UiState,
    onEditBudgetsClick: () -> Unit,
    onAddExpenseClick: (() -> Unit)? = null,
    onTriggerRecurringSync: (() -> Unit)? = null,
    onDismissBudgetAlert: () -> Unit = {}
) {
    val totalBudget = uiState.monthlyBudgetLimit
    val totalSpent = uiState.totalSpentThisMonth
    val totalRemaining = totalBudget - totalSpent
    val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    val overallPercentInt = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

    val progressColor = when {
        totalSpent > totalBudget -> MaterialTheme.colorScheme.error
        overallProgress > 0.85f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Budget Alert Notification Banner (80% / 100% threshold alert)
        BudgetAlertBanner(
            notification = uiState.budgetNotification,
            isDismissed = uiState.isBudgetAlertDismissed,
            onDismiss = onDismissBudgetAlert,
            onEditBudgetClick = onEditBudgetsClick,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        // Overall Budget Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("overall_budget_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Overall Monthly Family Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target Goal: ${uiState.currencySymbol}${String.format(Locale.US, "%.2f", totalBudget)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onEditBudgetsClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("set_budget_goal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Budget")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", totalSpent)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (totalRemaining >= 0) "Remaining" else "Over Budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (totalRemaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFFF5252)
                        )
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%.2f", Math.abs(totalRemaining))}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (totalRemaining >= 0) Color(0xFF00A86B) else Color(0xFFFF5252)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monthly Budget Goal Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Consumed: $overallPercentInt%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%.0f", totalSpent)} / ${uiState.currencySymbol}${String.format(Locale.US, "%.0f", totalBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { overallProgress },
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("monthly_budget_progress_bar")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recurring Bills & Goals Manager Card
        val recurringTemplates = uiState.expenses.filter { it.isRecurring && !it.isAutoCreated }
        val totalRecurringMonthly = recurringTemplates.sumOf { it.amount }
        val sdfMonth = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val currentMonthStr = sdfMonth.format(Date())

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recurring_bills_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Recurring Bills & Subscriptions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Committed: ${uiState.currencySymbol}${String.format(Locale.US, "%.2f", totalRecurringMonthly)} / month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (onTriggerRecurringSync != null) {
                        IconButton(
                            onClick = onTriggerRecurringSync,
                            modifier = Modifier.testTag("sync_recurring_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Auto-Check Recurring",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (recurringTemplates.isEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No recurring expenses set yet. Mark rent or subscriptions when adding expenses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (onAddExpenseClick != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = onAddExpenseClick,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                text = if (createdThisMonth) "Auto-Created" else "Due Day ${template.recurringDayOfMonth}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (createdThisMonth) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly End-of-Week Scheduled Review Reminder Service Card
        WeeklyReminderCard()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Category Budget Health",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val categories = CategoryHelper.allCategories

            categories.forEach { catInfo ->
                val categorySpent = uiState.expenses
                    .filter { it.category.equals(catInfo.name, ignoreCase = true) }
                    .sumOf { it.amount }

                val budgetEntity = uiState.categoryBudgets.find {
                    it.categoryName.equals(catInfo.name, ignoreCase = true)
                }

                val limit = budgetEntity?.monthlyLimit ?: 500.0
                val progress = (categorySpent / limit).toFloat().coerceIn(0f, 1f)
                val remaining = limit - categorySpent
                val isOver = categorySpent > limit
                val isWarning = progress > 0.85f && !isOver

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(catInfo.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catInfo.icon,
                                        contentDescription = catInfo.name,
                                        tint = catInfo.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = catInfo.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isOver || isWarning) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOver) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Alert",
                                            tint = if (isOver) Color(0xFFD32F2F) else Color(0xFFF57F17),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isOver) "Exceeded" else "Near Limit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOver) Color(0xFFD32F2F) else Color(0xFFF57F17),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: ${uiState.currencySymbol}${String.format(Locale.US, "%.2f", categorySpent)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Limit: ${uiState.currencySymbol}${String.format(Locale.US, "%.0f", limit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when {
                                isOver -> Color(0xFFFF5252)
                                isWarning -> Color(0xFFFFB300)
                                else -> catInfo.color
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
