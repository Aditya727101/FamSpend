with open('app/src/main/java/com/example/viewmodel/FamilyExpenseViewModel.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "exp.isAutoCreated &&" in line:
        print(f"{i}: {line.strip()}")
