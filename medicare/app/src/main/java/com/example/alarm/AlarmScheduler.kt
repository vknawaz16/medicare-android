package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.MediCareDatabase
import com.example.data.entity.MedicineEntity
import com.example.data.entity.ScheduleEntity
import com.example.data.model.ScheduleFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    const val ACTION_REMINDER_ALARM = "com.example.medicare.ACTION_REMINDER_ALARM"
    const val ACTION_TAKE = "com.example.medicare.ACTION_TAKE"
    const val ACTION_SNOOZE = "com.example.medicare.ACTION_SNOOZE"
    const val ACTION_SKIP = "com.example.medicare.ACTION_SKIP"

    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_MEDICINE_ID = "extra_medicine_id"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_INSTRUCTIONS = "extra_instructions"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val EXTRA_HISTORY_ID = "extra_history_id"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Calculates the next upcoming trigger time for the given schedule.
     */
    fun calculateNextTriggerTime(schedule: ScheduleEntity, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, schedule.reminderHour)
            set(Calendar.MINUTE, schedule.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's time has already passed, advance to the next day
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (schedule.frequency == ScheduleFrequency.EVERY_DAY) {
            return cal.timeInMillis
        }

        // Specific days
        val selectedDays = schedule.parseDaysList() // 1=Mon .. 7=Sun
        for (i in 0..7) {
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            if (selectedDays.contains(dayOfWeek) && cal.timeInMillis > nowMillis) {
                return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    fun scheduleAlarm(
        context: Context,
        schedule: ScheduleEntity,
        medicine: MedicineEntity
    ) {
        if (!schedule.enabledStatus || !medicine.activeStatus) {
            cancelAlarm(context, schedule.scheduleId)
            return
        }

        val triggerTime = calculateNextTriggerTime(schedule)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_SCHEDULE_ID, schedule.scheduleId)
            putExtra(EXTRA_MEDICINE_ID, medicine.medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicine.medicineName)
            putExtra(EXTRA_DOSAGE, medicine.dosage)
            putExtra(EXTRA_INSTRUCTIONS, medicine.instructions.displayName)
            putExtra(EXTRA_SCHEDULED_TIME, triggerTime)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.scheduleId.toInt(),
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms(context)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${medicine.medicineName} at $triggerTime (schedId: ${schedule.scheduleId})")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback alarm scheduling also failed", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun scheduleSnooze(
        context: Context,
        scheduleId: Long,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        instructions: String,
        snoozeMinutes: Int,
        historyId: Long?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_INSTRUCTIONS, instructions)
            putExtra(EXTRA_SCHEDULED_TIME, triggerTime)
            putExtra(EXTRA_HISTORY_ID, historyId ?: -1L)
        }

        // Cancel any existing snoozes for this schedule before scheduling the new one
        cancelSnoozeAlarms(context, scheduleId)

        val requestCode = (scheduleId * 1000 + snoozeMinutes).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled snooze for $medicineName in $snoozeMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze", e)
        }
    }

    fun cancelSnoozeAlarms(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }
        for (mins in listOf(5, 10, 15)) {
            val reqCode = (scheduleId * 1000 + mins).toInt()
            val pending = PendingIntent.getBroadcast(context, reqCode, intent, flags)
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    fun cancelAlarm(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        cancelSnoozeAlarms(context, scheduleId)
    }

    fun rescheduleAllActiveAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MediCareDatabase.getDatabase(context)
                val activeMedicines = db.medicineDao().getActiveMedicinesSync()
                for (medWithSched in activeMedicines) {
                    for (sched in medWithSched.schedules) {
                        if (sched.enabledStatus) {
                            scheduleAlarm(context, sched, medWithSched.medicine)
                        }
                    }
                }
                Log.d(TAG, "Rescheduled all active alarms after reboot or update")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling all alarms", e)
            }
        }
    }
}
