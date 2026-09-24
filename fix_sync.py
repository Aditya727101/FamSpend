import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    code = f.read()

replacement = """Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha=0.5f), modifier = Modifier.clickable { onSyncNowClick() }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                if (uiState.isSyncing) {
                                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Syncing...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.syncStatusMessage.contains("Error")) "Sync Failed" else "Synced just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }"""

code = re.sub(r'Surface\(shape = RoundedCornerShape\(16\.dp\), color = MaterialTheme\.colorScheme\.surface\.copy\(alpha=0\.5f\)\) \{[\s\S]*?\}', replacement, code)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(code)
