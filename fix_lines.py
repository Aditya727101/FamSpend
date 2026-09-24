with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = lines[:408] + lines[414:]

with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'w') as f:
    f.writelines(new_lines)
