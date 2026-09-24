with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith("RecentExpenseCard("):
        new_lines.append("    " + line)
    elif line.startswith("    @Composable"):
        new_lines.append(line.replace("    @Composable", "@Composable"))
    elif line.startswith("    fun RecentExpenseCard("):
        new_lines.append(line.replace("    fun RecentExpenseCard(", "fun RecentExpenseCard("))
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/components/RecentExpensesList.kt', 'w') as f:
    f.writelines(new_lines)
