import re

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code = f.read()

# Update signature
code = code.replace("onDateRangeFilterChange: (String?) -> Unit = {},", "onDateRangeFilterChange: (String?) -> Unit = {},\n    onSortChange: (String) -> Unit = {},")

# Add sort UI right after Date Range (or instead of just date range, a dropdown for sorting). Let's just add it as chips.
sort_options = """        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val sorts = listOf("Newest first", "Oldest first", "Highest amount", "Lowest amount")
            items(sorts) { opt ->
                val isSelected = uiState.selectedSortOrder == opt
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { onSortChange(opt) }
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }"""

code = code.replace("            items(dateRangeOptions) { opt ->", "            items(dateRangeOptions) { opt ->")

# Let's insert the sort_options right after the LazyRow for dateRangeOptions
code = re.sub(r'(\s*\)\s*\}\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\))', r'\n' + sort_options + r'\1', code)

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code)
