package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val sharedTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val sharedDayMonthFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

fun formatExpenseDateTime(timestamp: Long): String {
    val calToday = Calendar.getInstance()
    calToday.set(Calendar.HOUR_OF_DAY, 0)
    calToday.set(Calendar.MINUTE, 0)
    calToday.set(Calendar.SECOND, 0)
    calToday.set(Calendar.MILLISECOND, 0)
    val todayStart = calToday.timeInMillis
    val yesterdayStart = todayStart - 86400000L

    val d = Date(timestamp)
    return when {
        timestamp >= todayStart -> "Today, ${sharedTimeFormat.format(d)}"
        timestamp >= yesterdayStart -> "Yesterday, ${sharedTimeFormat.format(d)}"
        else -> sharedDayMonthFormat.format(d)
    }
}

@Composable
fun RecentExpensesList(
    expenses: List<ExpenseEntity>,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier,
    maxItems: Int? = null,
    onEditExpense: ((ExpenseEntity) -> Unit)? = null,
    onDeleteExpense: ((ExpenseEntity) -> Unit)? = null,
    onAddExpenseClick: (() -> Unit)? = null,
    onSimulateClick: (() -> Unit)? = null
) {
    val displayList = remember(expenses, maxItems) {
        if (maxItems != null && maxItems > 0) expenses.take(maxItems) else expenses
    }

    if (displayList.isEmpty()) {
        EmptyExpensesState(
            modifier = modifier,
            onAddExpenseClick = onAddExpenseClick,
            onSimulateClick = onSimulateClick
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("recent_expenses_list")
        ) {
            displayList.forEach { expense ->
                SwipeableExpenseItem(
                    expense = expense,
                    currencySymbol = currencySymbol,
                    onEditExpense = onEditExpense,
                    onDeleteExpense = onDeleteExpense
                )
            }
        }
    }
}

@Composable
fun SwipeableExpenseItem(
    expense: ExpenseEntity,
    currencySymbol: String,
    isSelected: Boolean = false,
    selectionModeActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onEditExpense: ((ExpenseEntity) -> Unit)? = null,
    onDeleteExpense: ((ExpenseEntity) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    RecentExpenseCard(
        expense = expense,
        currencySymbol = currencySymbol,
        isSelected = isSelected,
        selectionModeActive = selectionModeActive,
        onLongClick = {
            if (onLongClick != null) {
                onLongClick()
            } else if (onDeleteExpense != null) {
                showDeleteConfirm = true
            }
        },
        onClick = {
            if (onClick != null) {
                onClick()
            } else if (onEditExpense != null) {
                onEditExpense(expense)
            }
        },
        onEdit = onEditExpense?.let { { it(expense) } },
        onDelete = onDeleteExpense?.let { { showDeleteConfirm = true } }
    )

    if (showDeleteConfirm && onDeleteExpense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Expense?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete this expense? This will update family totals.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteExpense(expense)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FamDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentExpenseCard(
    expense: ExpenseEntity,
    currencySymbol: String,
    isSelected: Boolean = false,
    selectionModeActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val catInfo = remember(expense.category) {
        CategoryHelper.getCategoryInfo(expense.category)
    }
    val timeFormatted = remember(expense.timestamp) {
        formatExpenseDateTime(expense.timestamp)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) FamPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_expense_card_${expense.id}")
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(catInfo.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = FamPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = catInfo.icon,
                        contentDescription = expense.category,
                        tint = catInfo.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.description.ifBlank { "Untitled Expense" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (expense.isRecurring) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Recurring",
                                    tint = FamPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Recurring",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FamPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catInfo.color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Member pill & feature badges (Split, Receipt, Transfer)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(FamPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = FamPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = expense.paidByMemberName.split(" ").firstOrNull() ?: "Me",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (expense.category.equals("Transfer", ignoreCase = true) || expense.splitType.equals("Transfer", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Transfer",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF7C3AED)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Transfer",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    } else if (expense.splitType.contains("Split", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                    contentDescription = "Split",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Split",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (expense.receiptUri != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "Receipt attached",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencySymbol}${String.format(Locale.US, "%,.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
