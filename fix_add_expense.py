import re

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'r') as f:
    code = f.read()

# Add necessary imports
imports_to_add = [
    "import androidx.compose.animation.AnimatedVisibility",
    "import androidx.compose.material3.ExposedDropdownMenuBox",
    "import androidx.compose.material3.DropdownMenuItem",
    "import androidx.compose.material3.ExperimentalMaterial3Api",
    "import androidx.compose.foundation.lazy.grid.LazyVerticalGrid",
    "import androidx.compose.foundation.lazy.grid.GridCells",
    "import androidx.compose.material.icons.filled.KeyboardArrowDown",
    "import androidx.compose.material.icons.filled.KeyboardArrowUp"
]

for imp in imports_to_add:
    if imp not in code:
        code = code.replace("import androidx.compose.runtime.Composable", imp + "\nimport androidx.compose.runtime.Composable")


# Replace category grid logic
cat_section_old_regex = r'Text\(\s*text = "Category",\s*style = MaterialTheme\.typography\.titleMedium,\s*fontWeight = FontWeight\.SemiBold\s*\)\s*Spacer\(modifier = Modifier\.height\(8\.dp\)\)\s*FlowRow\([\s\S]*?\}\s*\}\s*\}\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\)'

cat_section_new = """Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val commonCategories = CategoryHelper.allCategories.take(6)
            val otherCategories = CategoryHelper.allCategories.drop(6)
            var showMoreCategories by remember { mutableStateOf(false) }

            // 3x2 Grid for common categories
            Column(modifier = Modifier.fillMaxWidth()) {
                val rows = commonCategories.chunked(3)
                rows.forEach { rowCats ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val isSelected = cat.name.equals(selectedCategory, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCategory = cat.name }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else cat.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        // Fill empty spaces if not multiple of 3
                        repeat(3 - rowCats.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showMoreCategories = !showMoreCategories }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showMoreCategories) "Hide More Categories" else "More Categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (showMoreCategories) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            AnimatedVisibility(visible = showMoreCategories) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    otherCategories.forEach { cat ->
                        val isSelected = cat.name.equals(selectedCategory, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .clickable { selectedCategory = cat.name }
                                .testTag("category_chip_${cat.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else cat.color,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))"""

code = re.sub(cat_section_old_regex, cat_section_new, code)


# Now accordion for Split and Payment
split_payment_old_regex = r'// Split Type[\s\S]*?Spacer\(modifier = Modifier\.height\(16\.dp\)\)\s*// Payment Method[\s\S]*?Spacer\(modifier = Modifier\.height\(16\.dp\)\)'

split_payment_new = """// Advanced Options Accordion
            var showAdvancedOptions by remember { mutableStateOf(false) }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().clickable { showAdvancedOptions = !showAdvancedOptions }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Advanced Options",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$selectedSplit • $selectedPaymentMethod",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            AnimatedVisibility(visible = showAdvancedOptions) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    // Split Type
                    Text(
                        text = "Expense Split",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        splitOptions.forEach { split ->
                            val isSelected = split == selectedSplit
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedSplit = split }
                                    .testTag("split_chip_$split")
                            ) {
                                Text(
                                    text = split,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        paymentMethods.forEach { method ->
                            val isSelected = method == selectedPaymentMethod
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedPaymentMethod = method }
                                    .testTag("payment_chip_$method")
                            ) {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))"""

code = re.sub(split_payment_old_regex, split_payment_new, code)

with open('app/src/main/java/com/example/ui/components/AddExpenseDialog.kt', 'w') as f:
    f.write(code)

print("Done")
