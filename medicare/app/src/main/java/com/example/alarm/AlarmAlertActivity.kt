package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MediCareDatabase
import com.example.data.repository.MediCareRepository
import com.example.ui.theme.MediCareTheme
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusTaken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmAlertActivity : ComponentActivity() {

    private lateinit var repository: MediCareRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val db = MediCareDatabase.getDatabase(this)
        repository = MediCareRepository(
            medicineDao = db.medicineDao(),
            scheduleDao = db.scheduleDao(),
            historyDao = db.historyDao(),
            settingsDao = db.settingsDao()
        )

        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: "Prescribed dose"
        val instructions = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTIONS) ?: "As instructed"
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val historyIdExtra = intent.getLongExtra(AlarmScheduler.EXTRA_HISTORY_ID, -1L)
        val historyId = if (historyIdExtra > 0) historyIdExtra else null

        setContent {
            MediCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmAlertContent(
                        medicineName = medicineName,
                        dosage = dosage,
                        instructions = instructions,
                        scheduledTime = scheduledTime,
                        onTaken = {
                            AlarmSoundPlayer.stop()
                            NotificationHelper.dismissNotification(this@AlarmAlertActivity, scheduleId)
                            AlarmScheduler.cancelSnoozeAlarms(this@AlarmAlertActivity, scheduleId)
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.markDoseTaken(historyId, medicineId, scheduleId, scheduledTime)
                            }
                            finish()
                        },
                        onSnooze = { minutes ->
                            AlarmSoundPlayer.stop()
                            NotificationHelper.dismissNotification(this@AlarmAlertActivity, scheduleId)
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.markDoseSnoozed(historyId, minutes)
                                AlarmScheduler.scheduleSnooze(
                                    context = this@AlarmAlertActivity,
                                    scheduleId = scheduleId,
                                    medicineId = medicineId,
                                    medicineName = medicineName,
                                    dosage = dosage,
                                    instructions = instructions,
                                    snoozeMinutes = minutes,
                                    historyId = historyId
                                )
                            }
                            finish()
                        },
                        onSkip = {
                            AlarmSoundPlayer.stop()
                            NotificationHelper.dismissNotification(this@AlarmAlertActivity, scheduleId)
                            AlarmScheduler.cancelSnoozeAlarms(this@AlarmAlertActivity, scheduleId)
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.markDoseSkipped(historyId, medicineId, scheduleId, scheduledTime)
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundPlayer.stop()
    }
}

@Composable
fun AlarmAlertContent(
    medicineName: String,
    dosage: String,
    instructions: String,
    scheduledTime: Long,
    onTaken: () -> Unit,
    onSnooze: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scheduledTime))

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Alarm badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alarm Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEDICINE REMINDER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsing Pill Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = "Medicine",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // Center card: Details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = medicineName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dosage,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Scheduled Time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scheduled Time: $timeFormatted",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (instructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instructions: $instructions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Have you taken your medicine?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TAKEN Button
            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alarm_taken_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusTaken,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAKEN",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // SNOOZE Buttons (5m & 10m)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSnooze(5) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("alarm_snooze_5_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Snooze 5m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onSnooze(10) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("alarm_snooze_10_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Snooze 10m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // SKIP Button
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("alarm_skip_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StatusSkipped
                )
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SKIP THIS DOSE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
