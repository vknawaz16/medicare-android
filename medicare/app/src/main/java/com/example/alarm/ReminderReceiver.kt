package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.MediCareDatabase
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.model.DoseStatus
import com.example.data.repository.MediCareRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val medicineId = intent.getLongExtra(AlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val scheduledTime = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val historyIdExtra = intent.getLongExtra(AlarmScheduler.EXTRA_HISTORY_ID, -1L)
        val historyId = if (historyIdExtra > 0) historyIdExtra else null

        val db = MediCareDatabase.getDatabase(context)
        val repository = MediCareRepository(
            medicineDao = db.medicineDao(),
            scheduleDao = db.scheduleDao(),
            historyDao = db.historyDao(),
            settingsDao = db.settingsDao()
        )

        when (action) {
            AlarmScheduler.ACTION_REMINDER_ALARM -> {
                val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
                val instructions = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTIONS) ?: ""

                // Asynchronously record pending history & play sound
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.getSettingsSync()

                    // Record or fetch existing history item
                    var actualHistId = historyId
                    if (actualHistId == null && scheduleId > 0) {
                        val newHist = MedicineHistoryEntity(
                            medicineId = medicineId,
                            scheduleId = scheduleId,
                            medicineName = medicineName,
                            dosage = dosage,
                            scheduledDateTime = scheduledTime,
                            status = DoseStatus.PENDING
                        )
                        actualHistId = db.historyDao().insertHistory(newHist)
                    }

                    // Play audible alarm & vibration according to user settings
                    AlarmSoundPlayer.start(
                        context,
                        playSound = settings.alarmSoundEnabled,
                        vibrate = settings.vibrationEnabled
                    )

                    // Show notification with action buttons
                    NotificationHelper.showMedicineAlertNotification(
                        context = context,
                        scheduleId = scheduleId,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = dosage,
                        instructions = instructions,
                        scheduledTime = scheduledTime,
                        historyId = actualHistId
                    )

                    // Reschedule next recurrence for normal schedule
                    if (scheduleId > 0) {
                        val schedule = db.scheduleDao().getScheduleById(scheduleId)
                        val medicine = db.medicineDao().getMedicineWithSchedulesById(medicineId)?.medicine
                        if (schedule != null && medicine != null) {
                            AlarmScheduler.scheduleAlarm(context, schedule, medicine)
                        }
                    }
                }
            }

            AlarmScheduler.ACTION_TAKE -> {
                AlarmSoundPlayer.stop()
                NotificationHelper.dismissNotification(context, scheduleId)
                AlarmScheduler.cancelSnoozeAlarms(context, scheduleId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.markDoseTaken(historyId, medicineId, scheduleId, scheduledTime)
                    Log.d(TAG, "Dose marked TAKEN for medicine $medicineId")
                }
            }

            AlarmScheduler.ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 10)
                val medicineName = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME) ?: "Medicine"
                val dosage = intent.getStringExtra(AlarmScheduler.EXTRA_DOSAGE) ?: ""
                val instructions = intent.getStringExtra(AlarmScheduler.EXTRA_INSTRUCTIONS) ?: ""

                AlarmSoundPlayer.stop()
                NotificationHelper.dismissNotification(context, scheduleId)

                CoroutineScope(Dispatchers.IO).launch {
                    repository.markDoseSnoozed(historyId, snoozeMinutes)
                    AlarmScheduler.scheduleSnooze(
                        context = context,
                        scheduleId = scheduleId,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dosage = dosage,
                        instructions = instructions,
                        snoozeMinutes = snoozeMinutes,
                        historyId = historyId
                    )
                    Log.d(TAG, "Dose snoozed for $snoozeMinutes minutes")
                }
            }

            AlarmScheduler.ACTION_SKIP -> {
                AlarmSoundPlayer.stop()
                NotificationHelper.dismissNotification(context, scheduleId)
                AlarmScheduler.cancelSnoozeAlarms(context, scheduleId)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.markDoseSkipped(historyId, medicineId, scheduleId, scheduledTime)
                    Log.d(TAG, "Dose marked SKIPPED for medicine $medicineId")
                }
            }
        }
    }
}
