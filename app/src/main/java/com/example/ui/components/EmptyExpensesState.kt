package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryLight
import com.example.ui.theme.FamTextPrimary
import com.example.ui.theme.FamTextSecondary

/**
 * Universal encouraging empty state card for FamSpend.
 */
@Composable
fun FamEmptyState(
    headline: String,
    subText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = FamPrimary,
    iconBgColor: Color = FamPrimaryLight,
    buttonText: String? = null,
    buttonIcon: ImageVector? = Icons.Default.Add,
    onButtonClick: (() -> Unit)? = null,
    testTag: String = "fam_empty_state"
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconBgColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = headline,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FamPrimary
                    ),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("${testTag}_cta_button")
                ) {
                    if (buttonIcon != null) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// Backward compatibility wrapper
@Composable
fun EmptyExpensesState(
    modifier: Modifier = Modifier,
    title: String = "No Expenses Logged Yet",
    onAddExpenseClick: (() -> Unit)? = null,
    onSimulateClick: (() -> Unit)? = null
) {
    FamEmptyState(
        headline = if (title.contains("matching", ignoreCase = true)) "No matching expenses" else "No expenses yet!",
        subText = if (title.contains("matching", ignoreCase = true)) "Try adjusting your search query or clear filters." else "Start tracking your family's spending by adding your first expense below.",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        buttonText = if (onAddExpenseClick != null) "Add Expense" else null,
        onButtonClick = onAddExpenseClick,
        modifier = modifier,
        testTag = "expenses_empty_state"
    )
}

@Composable
fun ExpensesListEmptyState(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "No expenses yet!",
        subText = "Start tracking your family's spending by adding your first expense below.",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        buttonText = "Add Expense",
        onButtonClick = onAddExpenseClick,
        modifier = modifier,
        testTag = "empty_expenses_list"
    )
}

@Composable
fun AnalyticsChartsEmptyState(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "Nothing to analyze yet",
        subText = "Add some expenses and come back to see visual trends, daily spikes, and charts!",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        buttonText = "Add Expense",
        onButtonClick = onAddExpenseClick,
        modifier = modifier,
        testTag = "empty_analytics_charts"
    )
}

@Composable
fun CategoryDistributionEmptyState(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "No category data",
        subText = "Your spending categories will appear here once you log expenses.",
        icon = Icons.Default.PieChart,
        buttonText = "Add Expense",
        onButtonClick = onAddExpenseClick,
        modifier = modifier,
        testTag = "empty_category_distribution"
    )
}

@Composable
fun MemberShareEmptyState(
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "No member spending yet",
        subText = "Add expenses to see how much each family member is spending.",
        icon = Icons.Default.Group,
        buttonText = "Add Expense",
        onButtonClick = onAddExpenseClick,
        modifier = modifier,
        testTag = "empty_member_share"
    )
}

@Composable
fun RecurringBillsEmptyState(
    onAddRecurringClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "No recurring bills",
        subText = "No recurring bills added. Mark expenses like rent or subscriptions as recurring when adding them.",
        icon = Icons.Default.Autorenew,
        buttonText = "Add Recurring Bill",
        onButtonClick = onAddRecurringClick,
        modifier = modifier,
        testTag = "empty_recurring_bills"
    )
}

@Composable
fun FamilyMembersEmptyState(
    onInviteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FamEmptyState(
        headline = "Build your family circle",
        subText = "Invite family members to share expenses, sync budgets in real time, and split costs fairly.",
        icon = Icons.Default.GroupAdd,
        buttonText = "Invite Your First Member",
        buttonIcon = Icons.Default.GroupAdd,
        onButtonClick = onInviteClick,
        modifier = modifier,
        testTag = "empty_family_members"
    )
}
