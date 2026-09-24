import re

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    code = f.read()

# Make sure imports are there
if "import androidx.compose.ui.text.ExperimentalTextApi" not in code:
    code = code.replace("import androidx.compose.runtime.Composable", "import androidx.compose.ui.text.ExperimentalTextApi\nimport androidx.compose.ui.geometry.CornerRadius\nimport androidx.compose.ui.geometry.Size\nimport androidx.compose.runtime.Composable")

# Replace from Interactive Line Chart Canvas to the end of the Canvas

chart_old_regex = r'// Interactive Line Chart Canvas[\s\S]*?if \(item\.dayOfMonth == 1 \|\| item\.dayOfMonth % 5 == 0 \|\| item\.dayOfMonth == dailyList\.size\) \{[\s\S]*?\}'

new_chart_code = """// Interactive Line Chart Canvas
            val textColorArgb = textColor.toArgb()
            val labelPaint = remember(textColorArgb) {
                android.graphics.Paint().apply {
                    color = textColorArgb
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 30f
                }
            }
            val yLabelPaint = remember(textColorArgb) {
                android.graphics.Paint().apply {
                    color = textColorArgb
                    textAlign = android.graphics.Paint.Align.RIGHT
                    textSize = 28f
                }
            }
            
            val tooltipPaint = remember {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 32f
                    isFakeBoldText = true
                }
            }
            
            val tooltipBgPaint = remember {
                android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                }
            }

            val path = remember { Path() }
            val areaPath = remember { Path() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                var chartWidth by remember { mutableStateOf(0f) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dailyList) {
                            detectTapGestures { offset ->
                                val leftPadding = 100f
                                val width = size.width - leftPadding
                                val stepX = width / (dailyList.size - 1).coerceAtLeast(1)
                                val tappedIndex = ((offset.x - leftPadding) / stepX).toInt().coerceIn(0, dailyList.size - 1)
                                selectedDayIndex = if (selectedDayIndex == tappedIndex) null else tappedIndex
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30.dp.toPx()
                    val leftPadding = 45.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val chartWidth = width - leftPadding
                    val stepX = chartWidth / (dailyList.size - 1).coerceAtLeast(1)

                    // Draw Y axis and grid lines
                    val gridLevels = 4
                    for (i in 0..gridLevels) {
                        val yRatio = i.toFloat() / gridLevels
                        val y = chartHeight * (1f - yRatio)
                        
                        // Gridline
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Y-label
                        val labelAmt = maxAmount * yRatio
                        val labelText = if (labelAmt >= 1000) "${(labelAmt/1000).toInt()}k" else labelAmt.toInt().toString()
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            leftPadding - 15f,
                            y + 10f, // vertical center offset
                            yLabelPaint
                        )
                    }
                    
                    // Draw Y axis vertical line
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, 0f),
                        end = Offset(leftPadding, chartHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    // Draw X axis horizontal line
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, chartHeight),
                        end = Offset(width, chartHeight),
                        strokeWidth = 2.dp.toPx()
                    )

                    if (dailyList.isEmpty()) return@Canvas

                    // Points calculation
                    val points = dailyList.mapIndexed { index, data ->
                        val x = leftPadding + index * stepX
                        val yRatio = (data.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val y = chartHeight * (1f - yRatio)
                        Offset(x, y)
                    }

                    // Build line path
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
                    areaPath.close()

                    // Draw area gradient
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw main line path
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots and spike highlights
                    points.forEachIndexed { index, pt ->
                        val item = dailyList[index]
                        val isSelected = selectedDayIndex == index

                        if (item.isSpike) {
                            drawCircle(
                                color = spikeColor.copy(alpha = 0.3f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = spikeColor,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                        } else if (item.amount > 0 || isSelected) {
                            drawCircle(
                                color = if (isSelected) lineColor else lineColor.copy(alpha = 0.7f),
                                radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                                center = pt
                            )
                        }

                        if (isSelected) {
                            // Vertical guide line
                            drawLine(
                                color = lineColor.copy(alpha = 0.5f),
                                start = Offset(pt.x, 0f),
                                end = Offset(pt.x, chartHeight),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            
                            // Tooltip Box
                            val tooltipText = "$${String.format(java.util.Locale.US, "%.0f", item.amount)}"
                            val tw = tooltipPaint.measureText(tooltipText)
                            val th = 50f
                            val px = pt.x.coerceIn(leftPadding + tw/2 + 20f, width - tw/2 - 20f)
                            val py = (pt.y - 40f).coerceAtLeast(30f)
                            
                            val rect = android.graphics.RectF(px - tw/2 - 20f, py - th/2 - 10f, px + tw/2 + 20f, py + th/2 + 10f)
                            drawContext.canvas.nativeCanvas.drawRoundRect(rect, 15f, 15f, tooltipBgPaint)
                            drawContext.canvas.nativeCanvas.drawText(tooltipText, px, py + 12f, tooltipPaint)
                        }
                    }

                    // Draw X-Axis Day Labels
                    dailyList.forEachIndexed { index, item ->
                        if (item.dayOfMonth == 1 || item.dayOfMonth % 5 == 0 || item.dayOfMonth == dailyList.size) {
                            val x = leftPadding + index * stepX
                            drawContext.canvas.nativeCanvas.drawText(
                                "D${item.dayOfMonth}",
                                x.coerceIn(leftPadding + 10f, width - 10f),
                                height - 5.dp.toPx(),
                                labelPaint
                            )
                        }
                    }"""

code = re.sub(chart_old_regex, new_chart_code, code)

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(code)

print("Done")
