import re

with open('app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'r') as f:
    code = f.read()

import_statement = "import com.example.ui.components.CategoryPieChart\n"
if "CategoryPieChart" not in code:
    code = code.replace("import com.example.ui.components.CategoryHelper", import_statement + "import com.example.ui.components.CategoryHelper")

pattern = r'Card\(\s*shape = RoundedCornerShape\(16\.dp\),\s*colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\),\s*elevation = CardDefaults\.cardElevation\(defaultElevation = 1\.dp\),\s*modifier = Modifier\.fillMaxWidth\(\)\s*\)\s*\{\s*Column\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.padding\(16\.dp\),\s*verticalArrangement = Arrangement\.spacedBy\(14\.dp\)\s*\)\s*\{\s*if\s*\(typedCategorySums\.isEmpty\(\)\).*?\}\s*\}\s*\}\s*\}\s*\}'

new_card = """Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                CategoryPieChart(
                    categorySums = typedCategorySums,
                    totalSpent = totalSpent,
                    currencySymbol = uiState.currencySymbol
                )
            }
        }"""

code = re.sub(pattern, new_card, code, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'w') as f:
    f.write(code)
