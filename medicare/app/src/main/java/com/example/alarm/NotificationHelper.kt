package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "medicare_reminders_channel"
    private const val CHANNEL_NAME = "Medicine Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and reminders to take your scheduled medications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 1000)
                setSound(alarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicineAlertNotification(
        context: Context,
        scheduleId: Long,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        instructions: String,
        scheduledTime: Long,
        historyId: Long?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = scheduleId.toInt()

        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(scheduledTime))

        // Full screen & content intent to AlarmAlertActivity
        val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTIONS, instructions)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_HISTORY_ID, historyId ?: -1L)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (scheduleId * 10 + 1).toInt(),
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: TAKEN
        val takeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_TAKE
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_HISTORY_ID, historyId ?: -1L)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 2).toInt(),
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: SNOOZE 5m
        val snooze5Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTIONS, instructions)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)
            putExtra(AlarmScheduler.EXTRA_HISTORY_ID, historyId ?: -1L)
        }
        val snooze5PendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 3).toInt(),
            snooze5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: SNOOZE 10m
        val snooze10Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(AlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AlarmScheduler.EXTRA_INSTRUCTIONS, instructions)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
            putExtra(AlarmScheduler.EXTRA_HISTORY_ID, historyId ?: -1L)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 4).toInt(),
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: SKIP
        val skipIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SKIP
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AlarmScheduler.EXTRA_HISTORY_ID, historyId ?: -1L)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + 5).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MEDICINE REMINDER: $medicineName")
            .setContentText("Dosage: $dosage • $instructions (Time: $timeStr)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Medicine: $medicineName\nDosage: $dosage\nScheduled Time: $timeStr\nInstructions: $instructions\n\nHave you taken your medicine?")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "✓ TAKEN", takePendingIntent)
            .addAction(0, "⏰ Snooze 5m", snooze5PendingIntent)
            .addAction(0, "⏰ Snooze 10m", snooze10PendingIntent)
            .addAction(0, "✕ SKIP", skipPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun dismissNotification(context: Context, scheduleId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(scheduleId.toInt())
    }
}
