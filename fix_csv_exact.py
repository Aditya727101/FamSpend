import re

with open('app/src/main/java/com/example/util/CsvExportHelper.kt', 'r') as f:
    code = f.read()

replacement = """
    fun generateCsvString(expenses: List<ExpenseEntity>, householdName: String = "Household"): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthName = monthFormat.format(Date())
        val sb = StringBuilder()

        // Header info metadata (Simplified)
        sb.append(householdName).append(" Expense Report - ").append(currentMonthName).append("\\n")
        val totalSum = expenses.sumOf { it.amount }
        sb.append("Total Spent: ").append(String.format(Locale.US, "%.2f", totalSum)).append("\\n\\n")

        // Column CSV Headers (Simplified)
        sb.append("Date,Category,Description,Paid By,Amount,Notes\\n")

        for (expense in expenses) {
            val dateStr = dateFormat.format(Date(expense.timestamp))
            val amountFormatted = "${expense.currencySymbol}${String.format(Locale.US, "%.2f", expense.amount)}"
            
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(expense.category)).append(",")
            sb.append(escapeCsv(expense.description)).append(",")
            sb.append(escapeCsv(expense.paidByMemberName)).append(",")
            sb.append(escapeCsv(amountFormatted)).append(",")
            sb.append(escapeCsv(expense.note))
            sb.append("\\n")
        }

        return sb.toString()
    }
"""

# Extract the old function
match = re.search(r'fun generateCsvString\(.*?\)\: String \{.*?(?=private fun escapeCsv)', code, flags=re.DOTALL)
if match:
    old_func = match.group(0)
    code = code.replace(old_func, replacement.strip() + "\n\n    ")

with open('app/src/main/java/com/example/util/CsvExportHelper.kt', 'w') as f:
    f.write(code)
