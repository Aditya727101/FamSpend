package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.RecurringBillsEmptyState
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryLight
import com.example.ui.theme.FamSuccess
import com.example.ui.theme.FamWarning
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val rawOverallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    val animatedOverallProgress by animateFloatAsState(
        targetValue = rawOverallProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "overall_budget_progress"
    )
    val overallPercentInt = if (totalBudget > 0) ((totalSpent / totalBudget) * 100).toInt() else 0

    val progressColor = when {
        totalSpent > totalBudget -> FamDanger
        rawOverallProgress >= 0.90f -> FamDanger
        rawOverallProgress >= 0.70f -> FamWarning
        else -> FamSuccess
    }

    // Month-to-Month comparison calculated in single pass
    val (thisMonthSpent, lastMonthSpent) = remember(uiState.expenses) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = lastMonthCal.get(Calendar.MONTH)
        val prevYear = lastMonthCal.get(Calendar.YEAR)

        val temp = Calendar.getInstance()
        var cur = 0.0
        var prev = 0.0

        for (e in uiState.expenses) {
            temp.timeInMillis = e.timestamp
            val m = temp.get(Calendar.MONTH)
            val y = temp.get(Calendar.YEAR)
            if (m == currentMonth && y == currentYear) {
                cur += e.amount
            } else if (m == prevMonth && y == prevYear) {
                prev += e.amount
            }
        }
        Pair(cur, prev)
    }

    val spendDiffPercent = remember(thisMonthSpent, lastMonthSpent) {
        if (lastMonthSpent > 0) {
            ((thisMonthSpent - lastMonthSpent) / lastMonthSpent * 100).toInt()
        } else null
    }

    // Fast O(1) Category spending & budget limit maps for 60+ FPS scrolling
    val categorySpendMap = remember(uiState.expenses) {
        val map = HashMap<String, Double>(16)
        for (e in uiState.expenses) {
            val key = e.category.lowercase(Locale.getDefault())
            map[key] = (map[key] ?: 0.0) + e.amount
        }
        map
    }

    val categoryBudgetMap = remember(uiState.categoryBudgets) {
        uiState.categoryBudgets.associateBy { it.categoryName.lowercase(Locale.getDefault()) }
    }

    // Local state for Paid/Unpaid toggle of recurring bills
    val billPaidState = remember { mutableStateMapOf<String, Boolean>() }

    val recurringBills = remember(uiState.expenses) {
        uiState.expenses.filter { it.isRecurring }
    }

    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("budgets_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item(key = "budget_alert_banner") {
            BudgetAlertBanner(
                notification = uiState.budgetNotification,
                isDismissed = uiState.isBudgetAlertDismissed,
                onDismiss = onDismissBudgetAlert,
                onEditBudgetClick = onEditBudgetsClick,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        // Overall Budget Card
        item(key = "overall_budget_card") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                text = "Monthly Household Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Limit: ${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", totalBudget)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onEditBudgetsClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FamPrimary.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("set_budget_goal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp), tint = FamPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Budget", fontWeight = FontWeight.Bold, color = FamPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", totalSpent)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (totalRemaining >= 0) "Remaining" else "Over Budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (totalRemaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else FamDanger
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", Math.abs(totalRemaining))}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (totalRemaining >= 0) FamSuccess else FamDanger
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = progressColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$overallPercentInt% used",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = progressColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", totalSpent)} of ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", totalBudget)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedOverallProgress },
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
        }

        item(key = "month_to_month_card") {
            Spacer(modifier = Modifier.height(14.dp))

            // Month-to-Month Comparison Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_to_month_comparison_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSpendingLess = spendDiffPercent != null && spendDiffPercent < 0
                    val isSpendingMore = spendDiffPercent != null && spendDiffPercent > 0

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSpendingLess) FamSuccess.copy(alpha = 0.12f)
                                else if (isSpendingMore) FamDanger.copy(alpha = 0.12f)
                                else FamPrimary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpendingLess) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Comparison",
                            tint = if (isSpendingLess) FamSuccess else if (isSpendingMore) FamDanger else FamPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (spendDiffPercent != null) {
                                if (spendDiffPercent < 0) "You've spent ${Math.abs(spendDiffPercent)}% less compared to this time last month."
                                else if (spendDiffPercent > 0) "You've spent ${spendDiffPercent}% more compared to this time last month."
                                else "You've spent the same amount compared to this time last month."
                            } else {
                                "First full month of family expense tracking."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This Month: ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", thisMonthSpent)}  •  Last Month: ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", lastMonthSpent)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Recurring Bills Section
        item(key = "recurring_bills_header") {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FamPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Recurring Bills",
                            tint = FamPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recurring Bills & Commitments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onTriggerRecurringSync != null) {
                    IconButton(
                        onClick = onTriggerRecurringSync,
                        modifier = Modifier.testTag("sync_recurring_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Recurring",
                            tint = FamPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recurringBills.isEmpty()) {
                RecurringBillsEmptyState(
                    onAddRecurringClick = onAddExpenseClick ?: {},
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recurringBills.forEach { bill ->
                        val isPaid = billPaidState[bill.id] ?: false
                        val dayOfMonth = bill.recurringDayOfMonth.coerceIn(1, 31)
                        val daysUntil = if (dayOfMonth >= todayDay) dayOfMonth - todayDay else (30 - todayDay + dayOfMonth)
                        val dueLabel = if (daysUntil == 0) "Due today" else "Due in $daysUntil days (on ${dayOfMonth}th)"

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.description.ifBlank { "Recurring Bill" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$dueLabel • ${bill.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FamPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Responsible: ${bill.paidByMemberName.split(" ").firstOrNull() ?: "Family"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FamPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", bill.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPaid) FamSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { billPaidState[bill.id] = !isPaid }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            if (isPaid) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Paid",
                                                    tint = FamSuccess,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = if (isPaid) "Paid" else "Mark Paid",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPaid) FamSuccess else MaterialTheme.colorScheme.onSurfaceVariant
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

        // Category Budget Health Header
        item(key = "category_health_header") {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budget Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap card to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Optimized Category Health Cards with unique keys and O(1) hash map lookups
        val categories = CategoryHelper.allCategories
        items(
            items = categories,
            key = { it.name },
            contentType = { "category_health" }
        ) { catInfo ->
            val catKey = catInfo.name.lowercase(Locale.getDefault())
            val categorySpent = categorySpendMap[catKey] ?: 0.0
            val budgetEntity = categoryBudgetMap[catKey]
            val limit = budgetEntity?.monthlyLimit ?: 500.0
            val rawRatio = if (limit > 0) (categorySpent / limit).toFloat() else 0f
            val percentUsed = (rawRatio * 100).toInt()
            val remaining = limit - categorySpent
            val isExceeded = categorySpent > limit

            val statusColor = when {
                isExceeded || rawRatio >= 0.90f -> FamDanger
                rawRatio >= 0.70f -> FamWarning
                else -> FamSuccess
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onEditBudgetsClick() }
                    .testTag("category_health_card_${catInfo.name}")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$percentUsed% used",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", categorySpent)} of ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", limit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isExceeded) {
                                "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", -remaining)} over"
                            } else {
                                "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", remaining)} left"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isExceeded) FamDanger else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { rawRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // Household Savings Tracker Card
        item(key = "savings_tracker_card") {
            Spacer(modifier = Modifier.height(14.dp))

            val targetSavings = totalBudget - totalSpent
            val isSaving = targetSavings >= 0

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSaving) FamSuccess.copy(alpha = 0.12f) else FamDanger.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, (if (isSaving) FamSuccess else FamDanger).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("household_savings_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSaving) FamSuccess else FamDanger),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Savings",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Household Savings Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSaving) {
                                "On track to save ${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", targetSavings)} this month!"
                            } else {
                                "Over budget by ${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", -targetSavings)} this month"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSaving) FamSuccess else FamDanger
                        )
                        Text(
                            text = "Target savings = Monthly Budget minus Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
