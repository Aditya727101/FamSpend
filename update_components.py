import re

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Add ExperimentalFoundationApi import if not present
if "import androidx.compose.foundation.ExperimentalFoundationApi" not in code:
    code = "import androidx.compose.foundation.ExperimentalFoundationApi\nimport androidx.compose.foundation.combinedClickable\nimport androidx.compose.material.icons.filled.CheckCircle\n" + code

pattern1 = r'fun SwipeableExpenseItem\(\s*expense: ExpenseEntity,\s*currencySymbol: String,\s*onEditExpense: \(\(ExpenseEntity\) -> Unit\)\? = null,\s*onDeleteExpense: \(\(ExpenseEntity\) -> Unit\)\? = null\s*\) \{'

replacement1 = """@OptIn(ExperimentalFoundationApi::class)
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
) {"""

code = re.sub(pattern1, replacement1, code)

pattern2 = r'RecentExpenseCard\(\s*expense = expense,\s*currencySymbol = currencySymbol,\s*onEdit = onEditExpense\?\.let \{ \{ it\(expense\) \} \},\s*onDelete = onDeleteExpense\?\.let \{ \{ it\(expense\) \} \}\s*\)'

replacement2 = """RecentExpenseCard(
            expense = expense,
            currencySymbol = currencySymbol,
            isSelected = isSelected,
            selectionModeActive = selectionModeActive,
            onLongClick = onLongClick,
            onClick = onClick,
            onEdit = onEditExpense?.let { { it(expense) } },
            onDelete = onDeleteExpense?.let { { it(expense) } }
        )"""

code = re.sub(pattern2, replacement2, code)

pattern3 = r'@Composable\s*fun RecentExpenseCard\(\s*expense: ExpenseEntity,\s*currencySymbol: String,\s*onEdit: \(\(\) -> Unit\)\? = null,\s*onDelete: \(\(\) -> Unit\)\? = null\s*\)'

replacement3 = """@OptIn(ExperimentalFoundationApi::class)
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
)"""

code = re.sub(pattern3, replacement3, code)

pattern4 = r'modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.testTag\("recent_expense_card_\$\{expense\.id\}"\)'

replacement4 = """modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_expense_card_${expense.id}")
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )"""

code = re.sub(pattern4, replacement4, code)

# We also need to change the Card background color if isSelected
pattern5 = r'colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\),'

replacement5 = """colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),"""
code = re.sub(pattern5, replacement5, code)

# Let's add a checkmark if isSelected over the avatar.
# Box containing Icon.
pattern6 = r'(Box\(\s*modifier = Modifier\s*\.size\(44\.dp\)\s*\.clip\(CircleShape\)\s*\.background\(catInfo\.color\.copy\(alpha = 0\.15f\)\),\s*contentAlignment = Alignment\.Center\s*\)\s*\{[\s\S]*?)(\s*Icon\([\s\S]*?modifier = Modifier\.size\(22\.dp\)\s*\)\s*\})'

replacement6 = r"""\1
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
\2
                }"""
code = re.sub(pattern6, replacement6, code)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
