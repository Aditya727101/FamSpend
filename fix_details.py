import re

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'r') as f:
    code = f.read()

imports = """import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Repeat
"""

code = code.replace("import androidx.compose.material.icons.filled.Person", imports + "import androidx.compose.material.icons.filled.Person")

detail_rows = """
                        DetailRow(
                            icon = Icons.Default.CreditCard,
                            label = "Payment Method",
                            value = expense.paymentMethod
                        )
                        DetailRow(
                            icon = Icons.Default.CallSplit,
                            label = "Split Type",
                            value = expense.splitType
                        )
                        if (expense.isRecurring) {
                            DetailRow(
                                icon = Icons.Default.Repeat,
                                label = "Recurring",
                                value = "${expense.recurringFrequency} (Day ${expense.recurringDayOfMonth})"
                            )
                        }
"""

code = code.replace("value = expense.paidByMemberName\n                        )", "value = expense.paidByMemberName\n                        )" + detail_rows)

with open('app/src/main/java/com/example/ui/components/ExpenseDetailDialog.kt', 'w') as f:
    f.write(code)
