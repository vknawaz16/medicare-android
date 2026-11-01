package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AdherenceStats
import com.example.ui.DayAdherence
import com.example.ui.components.AdherenceCircularGauge
import com.example.ui.components.StatSummaryCard
import com.example.ui.components.WeeklyAdherenceBarChart
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusTaken

@Composable
fun ProgressScreen(
    dailyStats: AdherenceStats,
    weeklyStats: AdherenceStats,
    monthlyStats: AdherenceStats,
    weeklyBreakdown: List<DayAdherence>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = "Adherence & Progress",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your routine consistency over time",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Big Overview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("progress_overview_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Weekly Adherence",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (weeklyStats.adherencePercentage >= 80f) {
                                "Excellent routine adherence! You are staying right on track with your doctor's prescriptions."
                            } else if (weeklyStats.adherencePercentage >= 50f) {
                                "Good effort! Try to minimize missed doses to keep your treatment plan effective."
                            } else {
                                "MediCare will help keep you on schedule. Make sure reminders are turned on!"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    AdherenceCircularGauge(
                        percentage = weeklyStats.adherencePercentage,
                        takenDoses = weeklyStats.takenCount,
                        totalDoses = weeklyStats.totalScheduled,
                        modifier = Modifier.size(110.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Daily / Weekly / Monthly Comparisons
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressMetricRow(
                        label = "Today",
                        percentage = dailyStats.adherencePercentage,
                        detail = "${dailyStats.takenCount} of ${dailyStats.totalScheduled} doses"
                    )
                    ProgressMetricRow(
                        label = "Last 7 Days",
                        percentage = weeklyStats.adherencePercentage,
                        detail = "${weeklyStats.takenCount} of ${weeklyStats.totalScheduled} doses"
                    )
                    ProgressMetricRow(
                        label = "Last 30 Days",
                        percentage = monthlyStats.adherencePercentage,
                        detail = "${monthlyStats.takenCount} of ${monthlyStats.totalScheduled} doses"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7 Days Bar Chart
        WeeklyAdherenceBarChart(breakdowns = weeklyBreakdown)

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown Summary Grid
        Text(
            text = "Total Dose Summary (30 Days)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatSummaryCard(
                title = "Taken",
                count = monthlyStats.takenCount,
                icon = Icons.Default.CheckCircle,
                color = StatusTaken,
                modifier = Modifier.weight(1f)
            )
            StatSummaryCard(
                title = "Skipped",
                count = monthlyStats.skippedCount,
                icon = Icons.Default.Cancel,
                color = StatusSkipped,
                modifier = Modifier.weight(1f)
            )
            StatSummaryCard(
                title = "Missed",
                count = monthlyStats.missedCount,
                icon = Icons.Default.Warning,
                color = StatusMissed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wellness Tip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Consistency Matters",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Taking medicine at consistent daily times maintains stable blood levels and maximizes efficacy.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun ProgressMetricRow(
    label: String,
    percentage: Float,
    detail: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${percentage.toInt()}% ($detail)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (percentage >= 80f) StatusTaken else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
