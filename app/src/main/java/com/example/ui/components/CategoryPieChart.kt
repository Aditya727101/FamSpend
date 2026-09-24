package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    val selectedCategoryInfo = remember(selectedCategoryName, categorySums) {
        categorySums.find { it.first.name.equals(selectedCategoryName, ignoreCase = true) }
    }

    val selectedTransactionCount = remember(selectedCategoryName, expenses) {
        if (selectedCategoryName == null) 0
        else expenses.count { it.category.equals(selectedCategoryName, ignoreCase = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut Chart
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(categorySums) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            var angle = Math.toDegrees(Math.atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            // Adjust for -90 start angle
                            val adjustedAngle = (angle + 90f) % 360f

                            var currentAngle = 0f
                            for ((cat, amt) in categorySums) {
                                val sweep = ((amt / totalSpent) * 360f).toFloat()
                                if (adjustedAngle in currentAngle..(currentAngle + sweep)) {
                                    selectedCategoryName = if (selectedCategoryName == cat.name) null else cat.name
                                    break
                                }
                                currentAngle += sweep
                            }
                        }
                    }
            ) {
                var startAngle = -90f
                val baseStrokeWidth = 32.dp.toPx()
                val activeStrokeWidth = 40.dp.toPx()
                val radius = size.width / 2 - activeStrokeWidth / 2

                for ((category, amount) in categorySums) {
                    val sweepAngle = (amount / totalSpent).toFloat() * 360f * animatedProgress.value
                    val isSelected = category.name.equals(selectedCategoryName, ignoreCase = true)

                    if (sweepAngle > 0) {
                        drawArc(
                            color = category.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(activeStrokeWidth / 2, activeStrokeWidth / 2),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(
                                width = if (isSelected) activeStrokeWidth else baseStrokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${currencySymbol}${String.format(Locale.US, "%,.0f", totalSpent)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Category Summary Card (Fix 2d)
        selectedCategoryInfo?.let { (cat, amt) ->
            val count = if (selectedTransactionCount > 0) selectedTransactionCount else 1
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cat.color.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("selected_category_summary_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cat.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.name,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You spent ${currencySymbol}${String.format(Locale.US, "%,.2f", amt)} on ${cat.name} this $periodLabel across $count transaction${if (count != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend List: [Color dot] Category Name — ₹Amount (X%)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categorySums.forEach { (cat, amt) ->
                val isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true)
                val percent = if (totalSpent > 0) ((amt / totalSpent) * 100).toInt() else 0

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedCategoryName = if (isSelected) null else cat.name
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(cat.color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${currencySymbol}${String.format(Locale.US, "%,.2f", amt)} ($percent%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
