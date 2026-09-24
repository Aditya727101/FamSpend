with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    lines = f.readlines()

while lines and lines[-1].strip() == "}":
    lines.pop()

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.writelines(lines)
    # The error says "Expecting a top level declaration." at 613 and 614.
    # The file currently has 2 extra braces. Let's just put the right amount (0 since we popped them).
    # Wait, the ExpenseItemCard function needs to be closed. Let's see how many braces we need for ExpenseItemCard.
    f.write("    }\n}\n")
    
print("Done")
