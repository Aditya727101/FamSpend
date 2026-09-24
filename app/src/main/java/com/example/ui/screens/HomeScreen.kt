package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CategoryHelper
import com.example.ui.components.FamilyMembersEmptyState
import com.example.ui.components.MemberAvatar
import com.example.ui.components.RecentExpensesList
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
fun HomeScreen(
    uiState: UiState,
    onAddExpenseClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onSimulateLiveClick: () -> Unit,
    onMonthlySummaryClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onViewExpense: (ExpenseEntity) -> Unit = {},
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onToggleSelection: (String) -> Unit = {},
    onDismissBudgetAlert: () -> Unit = {},
    onEditBudgetClick: () -> Unit = {},
    onAddMemberClick: () -> Unit = {}
) {
    val cal = Calendar.getInstance()
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    // Fix 5e: "October 2024 • Day 12 of 31"
    val monthlyContext = "$currentMonthYear • Day $currentDay of $daysInMonth"

    val spent = uiState.totalSpentThisMonth
    val limit = uiState.monthlyBudgetLimit
    val remaining = if (limit > 0) limit - spent else 0.0

    // Top categories for segmented progress bar (Fix 5a)
    val categoryBreakdown = remember(uiState.expenses, spent) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = startCal.timeInMillis
        val monthExps = uiState.expenses.filter { it.timestamp >= start }
        val grouped = monthExps.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        CategoryHelper.allCategories.mapNotNull { cat ->
            val sum = grouped[cat.name] ?: 0.0
            if (sum > 0) Pair(cat, sum) else null
        }.sortedByDescending { it.second }
    }

    val top3Categories = remember(categoryBreakdown) {
        categoryBreakdown.take(3)
    }

    // Fix 5c: "At a Glance" Stats (Today's Spend, This Week, Top Spender, Active Budget)
    val todaySpend = remember(uiState.expenses) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = todayCal.timeInMillis
        uiState.expenses.filter { it.timestamp >= startOfToday }.sumOf { it.amount }
    }

    val thisWeekSpend = remember(uiState.expenses) {
        val weekCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val startOfWeek = weekCal.timeInMillis
        uiState.expenses.filter { it.timestamp >= startOfWeek }.sumOf { it.amount }
    }

    val dailyAverage = if (currentDay > 0) spent / currentDay else 0.0
    val todayVsAvgPercent = if (dailyAverage > 0) ((todaySpend / dailyAverage) * 100).toInt() else 0

    val topSpender = remember(uiState.expenses, uiState.members) {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val start = startCal.timeInMillis
        val memMap = mutableMapOf<String, Double>()
        uiState.expenses.filter { it.timestamp >= start }.forEach { exp ->
            memMap[exp.paidByMemberId] = (memMap[exp.paidByMemberId] ?: 0.0) + exp.amount
        }
        val topEntry = memMap.maxByOrNull { it.value }
        if (topEntry != null && topEntry.value > 0) {
            val mem = uiState.members.find { it.id == topEntry.key }
            if (mem != null) Triple(mem, topEntry.value, true) else null
        } else {
            null
        }
    }

    val percentUsed = if (limit > 0) ((spent / limit) * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Fix 5a, 5d, 5e: Main Budget Hero Card with Segmented Progress Bar & Sync Pill
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("home_household_budget_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with Household name, Monthly context, and Sync indicator pill (Fix 5d, 5e)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = uiState.householdName.ifBlank { "My Household" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Fix 5e: Monthly context
                            Text(
                                text = monthlyContext,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        // Fix 5d: Household Sync Status Indicator (pill)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSyncNowClick() }
                                .testTag("home_sync_status_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(FamSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isSyncing) "Syncing..." else "Synced just now",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Remaining and Budget limit numbers with Income awareness (Step 5)
                    Text(
                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", remaining)} left to spend",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", spent)} spent of ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", limit)} budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.totalIncomeThisMonth > 0) {
                            "💰 ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.totalIncomeThisMonth)} income this month  ·  ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", limit)} budget"
                        } else {
                            "💰 ${uiState.currencySymbol}0 income logged this month"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Fix 5a: Segmented Budget Progress Bar
                    val segment1 = top3Categories.getOrNull(0)
                    val segment2 = top3Categories.getOrNull(1)
                    val segment3 = top3Categories.getOrNull(2)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val total = if (limit > 0) limit else 100.0

                            var startX = 0f

                            top3Categories.forEach { (cat, amt) ->
                                val segWidth = ((amt / total) * w).toFloat().coerceAtLeast(0f)
                                if (segWidth > 0) {
                                    drawRect(
                                        color = cat.color,
                                        topLeft = Offset(startX, 0f),
                                        size = Size(segWidth, h)
                                    )
                                    startX += segWidth
                                }
                            }

                            // Other spending beyond top 3
                            val otherSpent = spent - top3Categories.sumOf { it.second }
                            if (otherSpent > 0) {
                                val otherWidth = ((otherSpent / total) * w).toFloat().coerceAtLeast(0f)
                                drawRect(
                                    color = Color(0xFF607D8B),
                                    topLeft = Offset(startX, 0f),
                                    size = Size(otherWidth, h)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fix 5a: Legend below the segmented bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            top3Categories.forEach { (cat, amt) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${cat.name.split(" ").first()}: ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", amt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        // Remaining / Left indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9E9E9E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Left: ${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", remaining.coerceAtLeast(0.0))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAddExpenseClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FamPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Expense", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onEditBudgetClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))
                        ) {
                            Text("View Budgets", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Fix 5b: Category Spending Snapshots (Horizontal Scrolling Mini Cards)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "Category Snapshots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val snapshotCategories = CategoryHelper.allCategories.take(5)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(snapshotCategories) { cat ->
                        val catSpent = uiState.expenses
                            .filter { it.category.equals(cat.name, ignoreCase = true) }
                            .sumOf { it.amount }
                        val catBudget = uiState.categoryBudgets.find {
                            it.categoryName.equals(cat.name, ignoreCase = true)
                        }?.monthlyLimit ?: 500.0
                        val catRatio = if (catBudget > 0) (catSpent / catBudget).toFloat().coerceIn(0f, 1f) else 0f

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onEditBudgetClick() }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(cat.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = cat.name,
                                            tint = cat.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Mini circular progress (% of that category's budget)
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { catRatio },
                                            modifier = Modifier.size(24.dp),
                                            color = cat.color,
                                            strokeWidth = 3.dp,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "${(catRatio * 100).toInt()}%",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", catSpent)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fix 5c: Quick Stats / "At a Glance" Tiles (2x2 Grid)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = "At a Glance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Row 1: Today's Spend & This Week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 1: Today's Spend
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Today, contentDescription = null, tint = FamPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Today's Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", todaySpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (dailyAverage > 0) "$todayVsAvgPercent% of daily avg" else "First spend today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tile 2: This Week
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = FamWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("This Week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", thisWeekSpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Past 7 days spending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: Top Spender & Active Budget Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 3: Top Spender
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = FamPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Top Spender", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (topSpender != null) {
                                val (mem, memAmt) = topSpender
                                Text(
                                    text = mem.name.split(" ").firstOrNull() ?: mem.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.0f", memAmt)} spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FamPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "No spend yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Family shared",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Tile 4: Active Budget
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wallet, contentDescription = null, tint = FamSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Active Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$percentUsed% used",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (percentUsed > 90) FamDanger.copy(alpha = 0.15f) else FamSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (percentUsed > 100) "Over Budget" else if (percentUsed > 80) "Caution" else "Healthy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (percentUsed > 90) FamDanger else FamSuccess,
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

        // Household Members Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Household Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = onAddMemberClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite Member", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.members.size <= 1) {
                    FamilyMembersEmptyState(
                        onInviteClick = onAddMemberClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.members.forEach { member ->
                            val memberSpent = uiState.expenses
                                .filter { it.paidByMemberId == member.id }
                                .sumOf { it.amount }
                            val memberShare = if (spent > 0) ((memberSpent / spent) * 100).toInt() else 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatar(
                                    name = member.name,
                                    colorHex = member.avatarColorHex,
                                    iconName = member.avatarIcon,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(member.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${uiState.currencySymbol}${String.format(Locale.US, "%,.2f", memberSpent)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("$memberShare% share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Expenses Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onViewAllTransactions) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View All", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                RecentExpensesList(
                    expenses = uiState.expenses,
                    currencySymbol = uiState.currencySymbol,
                    maxItems = 5,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense,
                    onAddExpenseClick = onAddExpenseClick,
                    onSimulateClick = onSimulateLiveClick
                )
            }
        }
    }
}
