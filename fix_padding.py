import re
with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("padding(horizontal = 16.dp, bottom = 8.dp)", "padding(start = 16.dp, end = 16.dp, bottom = 8.dp)")
with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)
