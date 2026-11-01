package com.example.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmScheduler
import com.example.data.entity.AppSettingsEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    viewModel: MainViewModel,
    onRequestNotificationPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val canExactAlarm = AlarmScheduler.canScheduleExactAlarms(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Customize alerts, sounds, preferences and data",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Alarms & Sound Section
        SettingsSectionHeader("REMINDER & ALARM SETTINGS", Icons.Default.NotificationsActive)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Sound Toggle
                SettingsToggleRow(
                    title = "Audible Alarm Sound",
                    subtitle = "Plays high-priority alarm ringtone at scheduled time",
                    checked = settings.alarmSoundEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(alarmSoundEnabled = it))
                    },
                    icon = Icons.Default.VolumeUp,
                    tag = "setting_sound_toggle"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Vibration Toggle
                SettingsToggleRow(
                    title = "Phone Vibration",
                    subtitle = "Vibrates phone in rhythm during reminder alert",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = {
                        viewModel.updateSettings(settings.copy(vibrationEnabled = it))
                    },
                    icon = Icons.Default.Vibration,
                    tag = "setting_vibration_toggle"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Default Snooze Duration
                Text(
                    text = "Default Snooze Duration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select default reminder snooze interval",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { mins ->
                        FilterChip(
                            selected = settings.defaultSnoozeMinutes == mins,
                            onClick = {
                                viewModel.updateSettings(settings.copy(defaultSnoozeMinutes = mins))
                            },
                            label = { Text("$mins minutes") },
                            modifier = Modifier.testTag("snooze_chip_$mins")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Alarm Button
                Button(
                    onClick = {
                        // Schedules a test alarm in 5 seconds
                        AlarmScheduler.scheduleSnooze(
                            context = context,
                            scheduleId = 99999L,
                            medicineId = 0L,
                            medicineName = "Test Reminder (Vitamin D)",
                            dosage = "1000 IU",
                            instructions = "Take with water",
                            snoozeMinutes = 0, // immediate or 5 sec
                            historyId = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_alarm"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "🔔 Test Alarm Now (Verify Sound & Vibration)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Android Permissions & Reliability
        SettingsSectionHeader("ANDROID RELIABILITY & PERMISSIONS", Icons.Default.Security)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Notification Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Permission",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasNotificationPermission) "Granted ✓" else "Not Granted ⚠️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasNotificationPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                    if (!hasNotificationPermission) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Exact Alarm Permission
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exact Alarm Permission",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (canExactAlarm) "Active for precise schedules ✓" else "Requires permission in Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canExactAlarm) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                    if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Battery Optimization Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Battery Optimization Guidance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aggressive battery savers (e.g., Xiaomi MIUI, Samsung OneUI) can freeze alarms when the app is closed. Exclude MediCare in your phone's battery settings for 100% reliability.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Theme Appearance
        SettingsSectionHeader("APPEARANCE", Icons.Default.Brightness4)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.isDarkMode == null,
                        onClick = { viewModel.updateSettings(settings.copy(isDarkMode = null)) },
                        label = { Text("System Default") }
                    )
                    FilterChip(
                        selected = settings.isDarkMode == false,
                        onClick = { viewModel.updateSettings(settings.copy(isDarkMode = false)) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = settings.isDarkMode == true,
                        onClick = { viewModel.updateSettings(settings.copy(isDarkMode = true)) },
                        label = { Text("Dark") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Data Export & Backup
        SettingsSectionHeader("DATA EXPORT & BACKUP", Icons.Default.FileDownload)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Export History CSV
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.getExportHistoryCsv()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "MediCare Medicine History Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Medicine History"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Medicine History (CSV)")
                }

                // Export Medicines CSV
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.getExportMedicinesCsv()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "MediCare Medicines Schedule Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Medicines List"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Medicines Schedule (CSV)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. About & Privacy
        SettingsSectionHeader("ABOUT & PRIVACY", Icons.Default.Info)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MediCare v1.0",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Never Miss Your Medicine.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔒 100% Offline & Private: All prescriptions, schedules, and adherence logs are stored exclusively in your device's local SQLite Room Database. No account registration, zero cloud transmission, and no tracking.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
