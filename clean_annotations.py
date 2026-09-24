import re
with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    code = f.read()

# Replace the messy annotations for SwipeableExpenseItem
bad_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableExpenseItem"""

good_block = """@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableExpenseItem"""

code = code.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.write(code)
