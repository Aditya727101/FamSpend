import re

with open('app/src/main/java/com/example/util/CsvExportHelper.kt', 'r') as f:
    code = f.read()

replacement = """
    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\\\"") || escaped.contains("\\n") || escaped.contains("#")) {
            "\\\"$escaped\\\""
        } else {
            escaped
        }
    }
"""

match = re.search(r'private fun escapeCsv\(value\: String\)\: String \{.*?\}', code, flags=re.DOTALL)
if match:
    old_func = match.group(0)
    # The actual kotlin code uses raw strings, maybe we should just replace it using bash
