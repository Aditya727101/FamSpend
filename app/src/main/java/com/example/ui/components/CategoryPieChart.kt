package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import java.util.Locale

data class CategorySlice(
    val category: CategoryInfo,
    val amount: Double,
    val startAngle: Float,
    val sweepAngle: Float,
    val percentage: Int
)

@Composable
fun CategoryPieChart(
    categorySums: List<Pair<CategoryInfo, Double>>,
    totalSpent: Double,
    currencySymbol: String,
    periodLabel: String = "month",
    expenses: List<ExpenseEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onAddExpenseClick: (() -> Unit)? = null
) {
    if (categorySums.isEmpty() || totalSpent <= 0.0) {
        CategoryDistributionEmptyState(
            onAddExpenseClick = onAddExpenseClick ?: {},
            modifier = modifier
        )
        return
    }

    var selectedCategoryName by remember { mutableStateOf<String?>(categorySums.firstOrNull()?.first?.name) }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySums) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    // Precalculate slices for 60+ FPS rendering without per-frame math
    val slices = remember(categorySums, totalSpent) {
        var currentAngle = 0f
        categorySums.map { (cat, amt) ->
            val sweep = ((amt / totalSpent) * 360.0).toFloat()
            val pct = ((amt / totalSpent) * 100.0).toInt()
            val slice = CategorySlice(
                category = cat,
                amount = amt,
                startAngle = currentAngle,
                sweepAngle = sweep,
                percentage = pct
            )
            currentAngle += sweep
            slice
        }
    }

    val selectedSlice = remember(selectedCategoryName, slices) {
        slices.find { it.category.name.equals(selectedCategoryName, ignoreCase = true) }
    }

    val selectedTransactionCount = remember(selectedCategoryName, expenses) {
        if (selectedCategoryName == null) 0
        else expenses.count { it.category.equals(selectedCategoryName, ignoreCase = true) }
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut Chart Container
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(210.dp)
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            var angle = Math.toDegrees(Math.atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            // Adjust for -90 start angle
                            val adjustedAngle = (angle + 90f) % 360f

                            for (slice in slices) {
                                if (adjustedAngle in slice.startAngle..(slice.startAngle + slice.sweepAngle)) {
                                    selectedCategoryName = if (selectedCategoryName == slice.category.name) null else slice.category.name
                                    break
                                }
                            }
                        }
                    }
            ) {
                val baseStrokeWidth = 28.dp.toPx()
                val activeStrokeWidth = 36.dp.toPx()
                val radius = (size.width - activeStrokeWidth) / 2f
                val topLeft = Offset(activeStrokeWidth / 2f, activeStrokeWidth / 2f)
                val arcSize = Size(radius * 2f, radius * 2f)
                val progress = animatedProgress.value

                // Draw background track ring
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = baseStrokeWidth, cap = StrokeCap.Round)
                )

                // Draw category arcs with gap and progress
                for (slice in slices) {
                    val isSelected = slice.category.name.equals(selectedCategoryName, ignoreCase = true)
                    val sweep = slice.sweepAngle * progress

                    if (sweep > 0f) {
                        val strokeW = if (isSelected) activeStrokeWidth else baseStrokeWidth
                        drawArc(
                            color = slice.category.color,
                            startAngle = -90f + slice.startAngle * progress,
                            sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Center Stat Text with Glassmorphism subtle pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(116.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Total Spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currencySymbol}${String.format(Locale.US, "%,.0f", totalSpent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                    if (selectedSlice != null) {
                        Text(
                            text = "${selectedSlice.percentage}% in ${selectedSlice.category.name.split(" ").first()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = selectedSlice.category.color
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Category Summary Card
        AnimatedVisibility(
            visible = selectedSlice != null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            selectedSlice?.let { slice ->
                val count = if (selectedTransactionCount > 0) selectedTransactionCount else 1
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = slice.category.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, slice.category.color.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("selected_category_summary_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(slice.category.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = slice.category.icon,
                                contentDescription = slice.category.name,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${slice.category.name} (${slice.percentage}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Spent ${currencySymbol}${String.format(Locale.US, "%,.2f", slice.amount)} this $periodLabel across $count transaction${if (count != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend List with Smooth Click Selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                val isSelected = slice.category.name.equals(selectedCategoryName, ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) slice.category.color.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedCategoryName = if (isSelected) null else slice.category.name
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.category.color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = slice.category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${currencySymbol}${String.format(Locale.US, "%,.2f", slice.amount)} (${slice.percentage}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) slice.category.color else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
