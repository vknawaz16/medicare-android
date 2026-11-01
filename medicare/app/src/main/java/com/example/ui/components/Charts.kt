package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DayAdherence
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusTaken
import java.util.Locale

@Composable
fun AdherenceCircularGauge(
    percentage: Float,
    takenDoses: Int,
    totalDoses: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(percentage) {
        animatedProgress.animateTo(
            targetValue = (percentage / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc
            val sweep = 270f * animatedProgress.value
            if (sweep > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${percentage.toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$takenDoses / $totalDoses Taken",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyAdherenceBarChart(
    breakdowns: List<DayAdherence>,
    modifier: Modifier = Modifier
) {
    if (breakdowns.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Last 7 Days Adherence",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                for (item in breakdowns) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${item.adherencePercent.toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Vertical Bar Canvas
                        Canvas(
                            modifier = Modifier
                                .width(20.dp)
                                .height(85.dp)
                        ) {
                            val barWidth = size.width
                            val maxHeight = size.height
                            val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

                            // Track
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(0f, 0f),
                                size = Size(barWidth, maxHeight),
                                cornerRadius = cornerRadius
                            )

                            // Filled portion
                            val fillHeight = (item.adherencePercent / 100f).coerceIn(0f, 1f) * maxHeight
                            if (fillHeight > 0f) {
                                val barColor = when {
                                    item.adherencePercent >= 80f -> StatusTaken
                                    item.adherencePercent >= 50f -> primaryColor
                                    else -> StatusSkipped
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(0f, maxHeight - fillHeight),
                                    size = Size(barWidth, fillHeight),
                                    cornerRadius = cornerRadius
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
