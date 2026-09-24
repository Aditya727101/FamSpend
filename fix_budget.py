with open('app/src/main/java/com/example/ui/screens/BudgetsScreen.kt', 'r') as f:
    lines = f.readlines()

while lines and lines[-1].strip() == "}":
    lines.pop()

with open('app/src/main/java/com/example/ui/screens/BudgetsScreen.kt', 'w') as f:
    f.writelines(lines)
    f.write("    }\n}\n}\n}\n}\n}\n}\n}\n}\n}\n")
print("Done")
