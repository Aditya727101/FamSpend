new_chart = """package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySpending(
    val dayOfMonth: Int,
    val dayLabel: String,
    val amount: Double,
    val isSpike: Boolean = false
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SpendingTrendChart(
    expenses: List<ExpenseEntity>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val monthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Process daily spending for current month
    val (dailyList, maxAmount, avgAmount, peakDay) = remember(expenses) {
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        val dayMap = mutableMapOf<Int, Double>()
        for (day in 1..daysInMonth) {
            dayMap[day] = 0.0
        }

        val tempCal = Calendar.getInstance()
        for (exp in expenses) {
            if (exp.timestamp in startOfMonth..endOfMonth) {
                tempCal.timeInMillis = exp.timestamp
                val day = tempCal.get(Calendar.DAY_OF_MONTH)
                dayMap[day] = (dayMap[day] ?: 0.0) + exp.amount
            }
        }

        val totalSpent = dayMap.values.sum()
        val nonZeroDays = dayMap.values.count { it > 0 }
        val avg = if (nonZeroDays > 0) totalSpent / nonZeroDays else 0.0
        val maxVal = dayMap.values.maxOrNull() ?: 0.0

        val list = (1..daysInMonth).map { day ->
            val amt = dayMap[day] ?: 0.0
            val isSpike = amt > 0 && (amt >= maxVal * 0.8 || (avg > 0 && amt >= avg * 1.8))
            DailySpending(
                dayOfMonth = day,
                dayLabel = "Day $day",
                amount = amt,
                isSpike = isSpike
            )
        }

        val peak = list.maxByOrNull { it.amount }
        Quadruple(list, if (maxVal == 0.0) 100.0 else maxVal * 1.15, avg, peak)
    }

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val selectedItem = selectedDayIndex?.let { dailyList.getOrNull(it) }

    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val spikeColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Trend",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                peakDay?.takeIf { it.amount > 0 }?.let { peak ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Peak",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Peak: $currencySymbol${String.format(Locale.US, "%.0f", peak.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Line Chart Canvas
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
                    val leftPadding = 90f
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
                            y + 10f, 
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend / Helper text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(spikeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Spending Spike",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(lineColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daily Spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Tap point for details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
"""

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.write(new_chart)

print("Done rewrite")
