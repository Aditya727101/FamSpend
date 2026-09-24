import re

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    code = f.read()

# First we inject the remembered paint outside Canvas.
# We find: Box( modifier = Modifier.fillMaxWidth().height(180.dp) ) {
# And insert our paint there.

injection = """
            val textColorArgb = androidx.compose.ui.graphics.toArgb(textColor)
            val labelPaint = remember(textColorArgb) {
                android.graphics.Paint().apply {
                    color = textColorArgb
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            }
            
            Box(
"""
code = code.replace("            Box(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .height(180.dp)\n            ) {", injection + "                modifier = Modifier\n                    .fillMaxWidth()\n                    .height(180.dp)\n            ) {")

# Then we find the old paint initialization inside Canvas and update it to just set the textSize
old_paint_code = """                    // Draw X-Axis Day Labels (every 5 days)
                    val labelPaint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }"""

new_paint_code = """                    // Draw X-Axis Day Labels (every 5 days)
                    labelPaint.textSize = 10.sp.toPx()"""

code = code.replace(old_paint_code, new_paint_code)

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(code)

print("Done")
