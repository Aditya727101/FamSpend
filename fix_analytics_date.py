import re

with open('app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'r') as f:
    code = f.read()

pattern = r'val \(categorySums, memberSums, topSpender, topCategory\) = remember\(uiState\.expenses, uiState\.members\) \{\s*// O\(N\) calculation for categories\s*val catMap = mutableMapOf<String, Double>\(\)\s*// O\(N\) calculation for members\s*val memMap = mutableMapOf<String, Double>\(\)\s*for \(expense in uiState\.expenses\) \{'

new_code = """val (categorySums, memberSums, topSpender, topCategory) = remember(uiState.expenses, uiState.members) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        // O(N) calculation for categories
        val catMap = mutableMapOf<String, Double>()
        // O(N) calculation for members
        val memMap = mutableMapOf<String, Double>()

        for (expense in uiState.expenses) {
            if (expense.timestamp < startOfMonth) continue
"""

code = re.sub(pattern, new_code, code, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'w') as f:
    f.write(code)
