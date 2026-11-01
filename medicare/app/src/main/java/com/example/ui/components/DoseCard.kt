package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DoseStatus
import com.example.data.repository.ScheduledDoseItem
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusSnoozed
import com.example.ui.theme.StatusTaken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoseCard(
    item: ScheduledDoseItem,
    onTake: () -> Unit,
    onSnooze: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.scheduledTimeMillis))
    var snoozeMenuExpanded by remember { mutableStateOf(false) }

    val statusColor = when (item.status) {
        DoseStatus.TAKEN -> StatusTaken
        DoseStatus.SKIPPED -> StatusSkipped
        DoseStatus.MISSED -> StatusMissed
        DoseStatus.SNOOZED -> StatusSnoozed
        DoseStatus.PENDING -> StatusPending
    }

    val cardColor = if (item.status == DoseStatus.TAKEN) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dose_card_${item.schedule.scheduleId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Time + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.status.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Medicine Name + Dosage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.medicine.medicineName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.medicine.dosage} • ${item.medicine.medicineType.displayName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Instructions Chip
            if (item.medicine.instructions.displayName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.medicine.instructions.displayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Actions for PENDING and SNOOZED doses
            if (item.status == DoseStatus.PENDING || item.status == DoseStatus.SNOOZED) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAKE BUTTON
                    Button(
                        onClick = onTake,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                            .testTag("dose_take_${item.schedule.scheduleId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusTaken,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Taken", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // SNOOZE BUTTON
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { snoozeMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("dose_snooze_${item.schedule.scheduleId}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Snooze", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        DropdownMenu(
                            expanded = snoozeMenuExpanded,
                            onDismissRequest = { snoozeMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Snooze for 5 minutes") },
                                onClick = {
                                    snoozeMenuExpanded = false
                                    onSnooze(5)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze for 10 minutes") },
                                onClick = {
                                    snoozeMenuExpanded = false
                                    onSnooze(10)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze for 15 minutes") },
                                onClick = {
                                    snoozeMenuExpanded = false
                                    onSnooze(15)
                                }
                            )
                        }
                    }

                    // SKIP BUTTON
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(42.dp)
                            .testTag("dose_skip_${item.schedule.scheduleId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSkipped)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "Skip", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (item.status == DoseStatus.TAKEN && item.actualActionTimeMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val takenAtStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.actualActionTimeMillis))
                Text(
                    text = "✓ Taken at $takenAtStr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = StatusTaken
                )
            }
        }
    }
}
