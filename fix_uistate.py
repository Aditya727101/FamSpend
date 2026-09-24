import re

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    code = f.read()

# Update currencySymbol to ₹
code = re.sub(r'val currencySymbol: String = "\$"', 'val currencySymbol: String = "₹"', code)
code = re.sub(r'val isAddExpenseDialogOpen: Boolean = false,', 'val isAddExpenseDialogOpen: Boolean = false,\n    val isFabMenuOpen: Boolean = false,', code)

# Add function to toggle FAB
pattern = r'fun openAddExpenseDialog'
replacement = r'''fun toggleFabMenu() {
        _uiState.value = _uiState.value.copy(isFabMenuOpen = !_uiState.value.isFabMenuOpen)
    }

    fun closeFabMenu() {
        _uiState.value = _uiState.value.copy(isFabMenuOpen = false)
    }

    fun openAddExpenseDialog'''
if "fun toggleFabMenu" not in code:
    code = code.replace(pattern, replacement)

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.write(code)
