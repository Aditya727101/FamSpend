import re

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'r') as f:
    code = f.read()

code = code.replace("onDateRangeFilterChange = { viewModel.setDateRangeFilter(it) },", "onDateRangeFilterChange = { viewModel.setDateRangeFilter(it) },\n                    onSortChange = { viewModel.setSortOrder(it) },")

with open('app/src/main/java/com/example/ui/MainAppShell.kt', 'w') as f:
    f.write(code)
