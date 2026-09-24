import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("com.example.ui.components.MemberAvatar(member = member, size = 40.dp)", 
                    'com.example.ui.components.MemberAvatar(name = member.name, colorHex = member.avatarColorHex, iconName = member.avatarIcon, size = 40.dp)')

code = code.replace("if(member.isAdmin) \"Admin\" else \"Member\"", "member.role")
code = code.replace("modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)", "modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 8.dp)")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    code_t = f.read()

code_t = code_t.replace("com.example.ui.components.SwipeableExpenseItem(", "com.example.ui.components.RecentExpenseCard(")

with open('app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(code_t)
