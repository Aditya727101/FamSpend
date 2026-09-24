import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

# We need to remove the extra brace at line 150.
# Look for:
"""
                        }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
"""
code = code.replace("                        }\n                        }\n                    }\n                    Spacer(modifier = Modifier.height(24.dp))", "                        }\n                    }\n                    Spacer(modifier = Modifier.height(24.dp))")

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)
