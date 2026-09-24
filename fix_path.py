import re

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    code = f.read()

# Insert the path declarations
path_decls = """            val path = remember { Path() }
            val areaPath = remember { Path() }
            
            Box(
"""
code = code.replace("            Box(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .height(180.dp)\n            ) {", path_decls + "                modifier = Modifier\n                    .fillMaxWidth()\n                    .height(180.dp)\n            ) {")

old_path = """                    // Build line path
                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                                val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                            }
                        }
                    }

                    // Build area path for gradient
                    val areaPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, chartHeight)
                        lineTo(points.first().x, chartHeight)
                        close()
                    }"""

new_path = """                    // Build line path
                    path.reset()
                    if (points.isNotEmpty()) {
                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        }
                    }

                    // Build area path for gradient
                    areaPath.reset()
                    areaPath.addPath(path)
                    areaPath.lineTo(points.last().x, chartHeight)
                    areaPath.lineTo(points.first().x, chartHeight)
                    areaPath.close()"""

code = code.replace(old_path, new_path)

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(code)

print("Done")
