with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

code = code.replace("recurringDayOfMonth\n                        )", "recurringDayOfMonth,\n                            datePickerState.selectedDateMillis ?: System.currentTimeMillis()\n                        )")

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)
