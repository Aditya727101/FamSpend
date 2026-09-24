package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.example.ui.theme.FamDanger
import com.example.ui.theme.FamPrimary
import com.example.ui.theme.FamPrimaryGradientStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TrendDataPoint(
    val index: Int,
    val label: String,
    val fullDate: String,
    val amount: Double,
    val topCategory: String = "",
    val isSpike: Boolean = false
)

private data class TrendCalculation(
    val points: List<TrendDataPoint>,
    val maxAmount: Double,
    val avgAmount: Double,
    val peakPoint: TrendDataPoint?
)

@Composable
fun SpendingTrendChart(
    expenses: List<ExpenseEntity>,
    currencySymbol: String = "₹",
    timePeriod: String = "Month", // "Week", "Month", "Year"
    budgetLimit: Double = 3000.0,
    modifier: Modifier = Modifier,
    onAddExpenseClick: (() -> Unit)? = null
) {
    if (expenses.isEmpty()) {
        AnalyticsChartsEmptyState(
            onAddExpenseClick = onAddExpenseClick ?: {},
            modifier = modifier
        )
        return
    }

    val periodSubHeader = remember(timePeriod) {
        val now = Date()
        when (timePeriod) {
            "Week" -> "Last 7 Days"
            "Year" -> SimpleDateFormat("yyyy", Locale.getDefault()).format(now)
            else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now)
        }
    }

    // Process data points efficiently according to timePeriod
    val trendData = remember(expenses, timePeriod, budgetLimit) {
        val calendar = Calendar.getInstance()

        when (timePeriod) {
            "Week" -> {
                val points = ArrayList<TrendDataPoint>(7)
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val fullFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val start = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis

                    val dayExpenses = expenses.filter { it.timestamp in start..end }
                    val total = dayExpenses.sumOf { it.amount }
                    val topCat = dayExpenses.groupBy { it.category }
                        .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key ?: "None"

                    points.add(
                        TrendDataPoint(
                            index = 6 - i,
                            label = dayFormat.format(Date(start)),
                            fullDate = fullFormat.format(Date(start)),
                            amount = total,
                            topCategory = topCat
                        )
                    )
                }

                val maxVal = points.maxOfOrNull { it.amount } ?: 0.0
                val nonZero = points.filter { it.amount > 0 }
                val avg = if (nonZero.isNotEmpty()) nonZero.sumOf { it.amount } / nonZero.size else 0.0
                val markedPoints = points.map {
                    val isSpike = it.amount > 0 && (it.amount >= maxVal * 0.85 || (avg > 0 && it.amount >= avg * 1.7))
                    it.copy(isSpike = isSpike)
                }
                val peak = markedPoints.maxByOrNull { it.amount }
                val chartMax = maxOf(maxVal * 1.2, budgetLimit * 0.5, 100.0)
                TrendCalculation(markedPoints, chartMax, avg, peak)
            }
            "Year" -> {
                val points = ArrayList<TrendDataPoint>(12)
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val currentYear = calendar.get(Calendar.YEAR)

                for (m in 0..11) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, currentYear)
                        set(Calendar.MONTH, m)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val start = cal.timeInMillis
                    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, lastDay)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis

                    val monthExpenses = expenses.filter { it.timestamp in start..end }
                    val total = monthExpenses.sumOf { it.amount }
                    val topCat = monthExpenses.groupBy { it.category }
                        .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key ?: "None"

                    points.add(
                        TrendDataPoint(
                            index = m,
                            label = monthFormat.format(Date(start)),
                            fullDate = "${monthFormat.format(Date(start))} $currentYear",
                            amount = total,
                            topCategory = topCat
                        )
                    )
                }

                val maxVal = points.maxOfOrNull { it.amount } ?: 0.0
                val nonZero = points.filter { it.amount > 0 }
                val avg = if (nonZero.isNotEmpty()) nonZero.sumOf { it.amount } / nonZero.size else 0.0
                val markedPoints = points.map {
                    val isSpike = it.amount > 0 && (it.amount >= maxVal * 0.85 || (avg > 0 && it.amount >= avg * 1.7))
                    it.copy(isSpike = isSpike)
                }
                val peak = markedPoints.maxByOrNull { it.amount }
                val chartMax = maxOf(maxVal * 1.2, budgetLimit * 1.2, 500.0)
                TrendCalculation(markedPoints, chartMax, avg, peak)
            }
            else -> {
                // Month view
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val curMonth = calendar.get(Calendar.MONTH)
                val curYear = calendar.get(Calendar.YEAR)

                val dayMap = DoubleArray(daysInMonth + 1)
                val topCatMap = arrayOfNulls<String>(daysInMonth + 1)
                val tempCal = Calendar.getInstance()

                for (exp in expenses) {
                    tempCal.timeInMillis = exp.timestamp
                    if (tempCal.get(Calendar.MONTH) == curMonth && tempCal.get(Calendar.YEAR) == curYear) {
                        val d = tempCal.get(Calendar.DAY_OF_MONTH)
                        if (d in 1..daysInMonth) {
                            dayMap[d] += exp.amount
                            if (topCatMap[d] == null) {
                                topCatMap[d] = exp.category
                            }
                        }
                    }
                }

                val monthYearStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
                val points = (1..daysInMonth).map { day ->
                    TrendDataPoint(
                        index = day - 1,
                        label = if (day == 1 || day % 5 == 0 || day == daysInMonth) "$day" else "",
                        fullDate = "$day $monthYearStr",
                        amount = dayMap[day],
                        topCategory = topCatMap[day] ?: "None"
                    )
                }

                val maxVal = points.maxOfOrNull { it.amount } ?: 0.0
                val nonZero = points.filter { it.amount > 0 }
                val avg = if (nonZero.isNotEmpty()) nonZero.sumOf { it.amount } / nonZero.size else 0.0
                val markedPoints = points.map {
                    val isSpike = it.amount > 0 && (it.amount >= maxVal * 0.8 || (avg > 0 && it.amount >= avg * 1.8))
                    it.copy(isSpike = isSpike)
                }
                val peak = markedPoints.maxByOrNull { it.amount }
                val chartMax = maxOf(maxVal * 1.25, (budgetLimit / daysInMonth) * 2.5, 200.0)
                TrendCalculation(markedPoints, chartMax, avg, peak)
            }
        }
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedPointIndex?.let { trendData.points.getOrNull(it) }

    val lineColor = FamPrimary
    val gradientColorStart = FamPrimaryGradientStart
    val spikeColor = FamDanger
    val dailyDotColor = Color(0xFF38BDF8) // Sky Blue
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val budgetLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColorArgb = remember(textColor) { textColor.toArgb() }

    val labelPaint = remember(textColorArgb) {
        android.graphics.Paint().apply {
            color = textColorArgb
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 28f
            isAntiAlias = true
        }
    }
    val yLabelPaint = remember(textColorArgb) {
        android.graphics.Paint().apply {
            color = textColorArgb
            textAlign = android.graphics.Paint.Align.RIGHT
            textSize = 24f
            isAntiAlias = true
        }
    }

    val pointsList = trendData.points
    val maxAmt = trendData.maxAmount

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with Icon, Period subheader, and Peak Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FamPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Trend",
                            tint = FamPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = periodSubHeader,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                trendData.peakPoint?.takeIf { it.amount > 0 }?.let { peak ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Peak",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Peak: $currencySymbol${String.format(Locale.US, "%,.0f", peak.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tooltip preview bar if a point is selected
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                selectedPoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pt.fullDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (pt.topCategory != "None") "Top: ${pt.topCategory}" else "No spend logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", pt.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (pt.isSpike) FamDanger else FamPrimary
                            )
                        }
                    }
                }
            }

            // High Performance Canvas with drawWithCache (solid 60-120 FPS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pointsList) {
                            detectTapGestures { offset ->
                                val leftPadding = 80f
                                val chartWidth = size.width - leftPadding
                                val stepX = chartWidth / (pointsList.size - 1).coerceAtLeast(1)
                                val tappedIndex = ((offset.x - leftPadding + (stepX / 2f)) / stepX)
                                    .toInt()
                                    .coerceIn(0, pointsList.size - 1)
                                selectedPointIndex = if (selectedPointIndex == tappedIndex) null else tappedIndex
                            }
                        }
                        .drawWithCache {
                            val width = size.width
                            val height = size.height
                            val bottomPadding = 26.dp.toPx()
                            val leftPadding = 80f
                            val chartHeight = height - bottomPadding
                            val chartWidth = width - leftPadding
                            val stepX = chartWidth / (pointsList.size - 1).coerceAtLeast(1)

                            // Precalculate coordinates
                            val coords = pointsList.mapIndexed { index, data ->
                                val x = leftPadding + index * stepX
                                val yRatio = (data.amount / maxAmt).toFloat().coerceIn(0f, 1f)
                                val y = chartHeight * (1f - yRatio)
                                Offset(x, y)
                            }

                            // Build smooth curve path
                            val curvePath = Path()
                            if (coords.isNotEmpty()) {
                                curvePath.moveTo(coords.first().x, coords.first().y)
                                for (i in 0 until coords.size - 1) {
                                    val p1 = coords[i]
                                    val p2 = coords[i + 1]
                                    val cp1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                                    val cp2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                                    curvePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                                }
                            }

                            // Build fill area path
                            val fillPath = Path()
                            if (coords.isNotEmpty()) {
                                fillPath.addPath(curvePath)
                                fillPath.lineTo(coords.last().x, chartHeight)
                                fillPath.lineTo(coords.first().x, chartHeight)
                                fillPath.close()
                            }

                            val fillBrush = Brush.verticalGradient(
                                colors = listOf(
                                    gradientColorStart.copy(alpha = 0.32f),
                                    lineColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = chartHeight
                            )

                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                            onDrawBehind {
                                // Draw horizontal grid lines
                                val gridLevels = 4
                                for (i in 0..gridLevels) {
                                    val yRatio = i.toFloat() / gridLevels
                                    val y = chartHeight * (1f - yRatio)

                                    drawLine(
                                        color = gridColor,
                                        start = Offset(leftPadding, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )

                                    val labelAmt = maxAmt * yRatio
                                    val labelText = if (labelAmt >= 1000) "${(labelAmt / 1000).toInt()}k" else labelAmt.toInt().toString()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        labelText,
                                        leftPadding - 12f,
                                        y + 8f,
                                        yLabelPaint
                                    )
                                }

                                // Budget reference line
                                val effectiveBudgetLevel = when (timePeriod) {
                                    "Week" -> budgetLimit / 4.0
                                    "Year" -> budgetLimit * 12.0
                                    else -> budgetLimit
                                }
                                if (effectiveBudgetLevel in 0.0..maxAmt) {
                                    val budgetY = chartHeight * (1f - (effectiveBudgetLevel / maxAmt).toFloat())
                                    drawLine(
                                        color = budgetLineColor,
                                        start = Offset(leftPadding, budgetY),
                                        end = Offset(width, budgetY),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = dashEffect
                                    )
                                }

                                if (coords.isEmpty()) return@onDrawBehind

                                // Draw gradient area fill
                                drawPath(path = fillPath, brush = fillBrush)

                                // Draw smooth line
                                drawPath(
                                    path = curvePath,
                                    color = lineColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Draw data points and selected guide
                                coords.forEachIndexed { index, pt ->
                                    val item = pointsList[index]
                                    val isSelected = selectedPointIndex == index

                                    if (isSelected) {
                                        // Vertical indicator line
                                        drawLine(
                                            color = lineColor.copy(alpha = 0.7f),
                                            start = Offset(pt.x, 0f),
                                            end = Offset(pt.x, chartHeight),
                                            strokeWidth = 1.5.dp.toPx(),
                                            pathEffect = dashEffect
                                        )
                                    }

                                    if (item.isSpike) {
                                        drawCircle(
                                            color = spikeColor.copy(alpha = 0.25f),
                                            radius = 8.dp.toPx(),
                                            center = pt
                                        )
                                        drawCircle(
                                            color = spikeColor,
                                            radius = 5.dp.toPx(),
                                            center = pt
                                        )
                                    } else if (item.amount > 0 || isSelected) {
                                        drawCircle(
                                            color = if (isSelected) dailyDotColor else dailyDotColor.copy(alpha = 0.85f),
                                            radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                                            center = pt
                                        )
                                    }
                                }

                                // Draw X-Axis labels
                                val skipFactor = when {
                                    pointsList.size > 20 -> 5
                                    pointsList.size > 10 -> 2
                                    else -> 1
                                }
                                pointsList.forEachIndexed { index, item ->
                                    if (item.label.isNotBlank() && (index == 0 || index % skipFactor == 0 || index == pointsList.size - 1)) {
                                        val x = leftPadding + index * stepX
                                        drawContext.canvas.nativeCanvas.drawText(
                                            item.label,
                                            x.coerceIn(leftPadding + 10f, width - 10f),
                                            height - 4.dp.toPx(),
                                            labelPaint
                                        )
                                    }
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend: Spike (red), Daily Spend (sky blue), Tap hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(spikeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Spike",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dailyDotColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daily Spend",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Tap point for info",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
