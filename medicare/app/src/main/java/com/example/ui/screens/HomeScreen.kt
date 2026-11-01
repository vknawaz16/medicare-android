package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.ScheduledDoseItem
import com.example.ui.MainViewModel
import com.example.ui.components.AdherenceCircularGauge
import com.example.ui.components.DoseCard
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusTaken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    todayDoses: List<ScheduledDoseItem>,
    activeMedicinesCount: Int,
    onAddMedicine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> "Good Morning"
        currentHour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val todayDateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    val totalDoses = todayDoses.size
    val takenDoses = todayDoses.count { it.status == com.example.data.model.DoseStatus.TAKEN }
    val pendingDoses = todayDoses.count { it.status == com.example.data.model.DoseStatus.PENDING || it.status == com.example.data.model.DoseStatus.SNOOZED }
    val missedDoses = todayDoses.count { it.status == com.example.data.model.DoseStatus.MISSED }

    val completionPercent = if (totalDoses > 0) {
        (takenDoses.toFloat() / totalDoses.toFloat()) * 100f
    } else {
        0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Greeting & Date
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = todayDateStr,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Meds Pill
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$activeMedicinesCount Active Meds",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Hero Banner Illustration
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.medicare_hero_banner),
                        contentDescription = "MediCare Routine Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 3. Today's Progress Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Adherence",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (totalDoses == 0) "No doses scheduled for today"
                                    else if (takenDoses == totalDoses) "Awesome! All doses completed! 🎉"
                                    else "$takenDoses of $totalDoses doses taken so far",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Linear Progress bar
                                LinearProgressIndicator(
                                    progress = { (completionPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            AdherenceCircularGauge(
                                percentage = completionPercent,
                                takenDoses = takenDoses,
                                totalDoses = totalDoses,
                                modifier = Modifier.size(110.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Taken chip
                            StatMiniBadge(
                                label = "Taken",
                                count = takenDoses,
                                color = StatusTaken,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                            // Pending chip
                            StatMiniBadge(
                                label = "Pending",
                                count = pendingDoses,
                                color = StatusPending,
                                icon = Icons.Default.HourglassEmpty,
                                modifier = Modifier.weight(1f)
                            )
                            // Missed chip
                            StatMiniBadge(
                                label = "Missed",
                                count = missedDoses,
                                color = StatusMissed,
                                icon = Icons.Default.Warning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Today's Schedule Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Schedule",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$totalDoses doses",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Dose Cards
            if (todayDoses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Medicines Scheduled For Today",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button below to add your medications and reminder schedule.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(todayDoses, key = { "${it.schedule.scheduleId}_${it.scheduledTimeMillis}" }) { dose ->
                    DoseCard(
                        item = dose,
                        onTake = { viewModel.markDoseTaken(dose) },
                        onSnooze = { mins -> viewModel.snoozeDose(dose, mins) },
                        onSkip = { viewModel.markDoseSkipped(dose) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav & FAB
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddMedicine,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_medicine"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Medicine",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun StatMiniBadge(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
