import os

files_to_check = [
    'app/src/main/java/com/example/ui/components/ExportCsvDialog.kt',
    'app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt',
    'app/src/main/java/com/example/ui/screens/TransactionsScreen.kt',
    'app/src/main/java/com/example/ui/components/MonthlySummaryDialog.kt'
]

replacements = {
    'Export Month CSV': 'Export Monthly Report',
    'Export Current Month Expenses (CSV)': 'Export Monthly Report (Text)',
    'Export CSV': 'Export Report',
    'Export Current Month CSV': 'Export Monthly Report',
    'CSV Preview': 'Report Preview',
    'Copy CSV to Clipboard': 'Copy Text to Clipboard',
    'Export & Share CSV File': 'Export & Share Text Report',
    'CSV copied to clipboard!': 'Report copied to clipboard!'
}

for filepath in files_to_check:
    with open(filepath, 'r') as f:
        content = f.read()
    
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

