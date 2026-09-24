import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

new_card = """@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val catInfo = CategoryHelper.getCategoryInfo(expense.category)
    val timeFormatted = remember(expense.timestamp) {
        val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(expense.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
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
                .padding(16.dp),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
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

            Spacer(modifier = Modifier.width(16.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { "Untitled Expense" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catInfo.color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount and Member
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencySymbol}${String.format(java.util.Locale.US, "%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = expense.paidByMemberName.split(" ").firstOrNull() ?: "Me",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
"""

start_idx = code.find('@OptIn(ExperimentalFoundationApi::class)\n@Composable\nfun RecentExpenseCard(')
if start_idx != -1:
    code = code[:start_idx] + new_card

# Fix an issue where it might not find due to exact string match
start_idx_2 = code.find('@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n@Composable\nfun RecentExpenseCard(')
if start_idx_2 != -1:
    code = code[:start_idx_2] + new_card

if start_idx == -1 and start_idx_2 == -1:
    # Try another generic match
    start_idx_3 = code.find('@Composable\nfun RecentExpenseCard(')
    if start_idx_3 != -1:
        # We need to find the preceding @OptIn if it exists
        code = code[:start_idx_3] + new_card

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
