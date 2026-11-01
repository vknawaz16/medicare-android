package com.example.data.repository

import com.example.data.dao.HistoryDao
import com.example.data.dao.MedicineDao
import com.example.data.dao.ScheduleDao
import com.example.data.dao.SettingsDao
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.entity.MedicineWithSchedules
import com.example.data.entity.ScheduleEntity
import com.example.data.model.DoseStatus
import com.example.data.model.ScheduleFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ScheduledDoseItem(
    val medicine: MedicineEntity,
    val schedule: ScheduleEntity,
    val scheduledTimeMillis: Long,
    val historyId: Long? = null,
    val status: DoseStatus = DoseStatus.PENDING,
    val actualActionTimeMillis: Long? = null
)

data class AdherenceStats(
    val totalScheduled: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
    val pendingCount: Int,
    val adherencePercentage: Float
)

class MediCareRepository(
    private val medicineDao: MedicineDao,
    private val scheduleDao: ScheduleDao,
    private val historyDao: HistoryDao,
    private val settingsDao: SettingsDao
) {

    val allMedicines: Flow<List<MedicineWithSchedules>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<MedicineWithSchedules>> = medicineDao.getActiveMedicines()
    val appSettings: Flow<AppSettingsEntity?> = settingsDao.getSettings()

    suspend fun getSettingsSync(): AppSettingsEntity {
        return settingsDao.getSettingsSync() ?: AppSettingsEntity()
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun saveMedicineWithSchedules(
        medicine: MedicineEntity,
        schedules: List<ScheduleEntity>
    ): Long {
        val medId = if (medicine.medicineId == 0L) {
            medicineDao.insertMedicine(medicine)
        } else {
            medicineDao.updateMedicine(medicine)
            scheduleDao.deleteSchedulesByMedicineId(medicine.medicineId)
            medicine.medicineId
        }

        val updatedSchedules = schedules.map { it.copy(medicineId = medId) }
        scheduleDao.insertSchedules(updatedSchedules)
        return medId
    }

    suspend fun getMedicineWithSchedulesById(id: Long): MedicineWithSchedules? {
        return medicineDao.getMedicineWithSchedulesById(id)
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        scheduleDao.deleteSchedulesByMedicineId(medicine.medicineId)
        historyDao.deleteHistoryByMedicineId(medicine.medicineId)
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun toggleMedicineActive(id: Long, active: Boolean) {
        medicineDao.setMedicineActive(id, active)
    }

    suspend fun toggleScheduleEnabled(scheduleId: Long, enabled: Boolean) {
        scheduleDao.setScheduleEnabled(scheduleId, enabled)
    }

    suspend fun getActiveMedicinesSync(): List<MedicineWithSchedules> {
        return medicineDao.getActiveMedicinesSync()
    }

    /**
     * Compute and sync today's scheduled doses.
     * Ensures an entry exists in medicine_history for each scheduled time today.
     */
    suspend fun getTodayDoseItems(): List<ScheduledDoseItem> {
        // Run missed doses check first
        checkAndMarkMissedDoses()

        val cal = Calendar.getInstance()
        val currentDayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        val activeList = medicineDao.getActiveMedicinesSync()
        val existingHistories = historyDao.getTodayHistorySync(startOfDay, endOfDay)
        val historyByScheduleMap = existingHistories.associateBy { it.scheduleId }

        val doseItems = mutableListOf<ScheduledDoseItem>()
        val currentTime = System.currentTimeMillis()

        for (medWithSched in activeList) {
            val med = medWithSched.medicine
            // Check start/end date constraints
            if (med.startDate > endOfDay) continue
            if (med.endDate != null && med.endDate < startOfDay) continue

            for (sched in medWithSched.schedules) {
                if (!sched.enabledStatus) continue

                // Check frequency/days
                val isScheduledToday = when (sched.frequency) {
                    ScheduleFrequency.EVERY_DAY -> true
                    ScheduleFrequency.SPECIFIC_DAYS -> sched.parseDaysList().contains(currentDayOfWeek)
                }

                if (!isScheduledToday) continue

                val doseCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, sched.reminderHour)
                    set(Calendar.MINUTE, sched.reminderMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val doseTime = doseCal.timeInMillis

                var history = historyByScheduleMap[sched.scheduleId]
                if (history == null) {
                    // Create pending history item
                    val newHistory = MedicineHistoryEntity(
                        medicineId = med.medicineId,
                        scheduleId = sched.scheduleId,
                        medicineName = med.medicineName,
                        dosage = med.dosage,
                        scheduledDateTime = doseTime,
                        status = DoseStatus.PENDING
                    )
                    val insertedId = historyDao.insertHistory(newHistory)
                    history = newHistory.copy(historyId = insertedId)
                }

                doseItems.add(
                    ScheduledDoseItem(
                        medicine = med,
                        schedule = sched,
                        scheduledTimeMillis = doseTime,
                        historyId = history.historyId,
                        status = history.status,
                        actualActionTimeMillis = history.actualActionDateTime
                    )
                )
            }
        }

        return doseItems.sortedBy { it.scheduledTimeMillis }
    }

    suspend fun markDoseTaken(historyId: Long?, medicineId: Long, scheduleId: Long, scheduledTime: Long) {
        val now = System.currentTimeMillis()
        if (historyId != null && historyId > 0) {
            historyDao.updateStatus(historyId, DoseStatus.TAKEN, now)
        } else {
            val med = medicineDao.getMedicineWithSchedulesById(medicineId)?.medicine
            historyDao.insertHistory(
                MedicineHistoryEntity(
                    medicineId = medicineId,
                    scheduleId = scheduleId,
                    medicineName = med?.medicineName ?: "Medicine",
                    dosage = med?.dosage ?: "",
                    scheduledDateTime = scheduledTime,
                    actualActionDateTime = now,
                    status = DoseStatus.TAKEN
                )
            )
        }
    }

    suspend fun markDoseSkipped(historyId: Long?, medicineId: Long, scheduleId: Long, scheduledTime: Long) {
        val now = System.currentTimeMillis()
        if (historyId != null && historyId > 0) {
            historyDao.updateStatus(historyId, DoseStatus.SKIPPED, now)
        } else {
            val med = medicineDao.getMedicineWithSchedulesById(medicineId)?.medicine
            historyDao.insertHistory(
                MedicineHistoryEntity(
                    medicineId = medicineId,
                    scheduleId = scheduleId,
                    medicineName = med?.medicineName ?: "Medicine",
                    dosage = med?.dosage ?: "",
                    scheduledDateTime = scheduledTime,
                    actualActionDateTime = now,
                    status = DoseStatus.SKIPPED
                )
            )
        }
    }

    suspend fun markDoseSnoozed(historyId: Long?, minutes: Int) {
        if (historyId != null && historyId > 0) {
            historyDao.updateStatus(historyId, DoseStatus.SNOOZED, null, "Snoozed for $minutes min")
        }
    }

    suspend fun checkAndMarkMissedDoses() {
        val settings = getSettingsSync()
        val gracePeriodMs = settings.gracePeriodMinutes * 60 * 1000L
        val cutoffTime = System.currentTimeMillis() - gracePeriodMs
        historyDao.markPendingAsMissed(cutoffTime)
    }

    fun getAllHistory(): Flow<List<MedicineHistoryEntity>> = historyDao.getAllHistory()

    fun getHistoryBetween(start: Long, end: Long): Flow<List<MedicineHistoryEntity>> =
        historyDao.getHistoryBetween(start, end)

    fun getTodayHistoryFlow(): Flow<List<MedicineHistoryEntity>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis
        return historyDao.getTodayHistory(startOfDay, endOfDay)
    }

    suspend fun calculateStats(startTime: Long, endTime: Long): AdherenceStats {
        val list = historyDao.getAllHistorySync().filter {
            it.scheduledDateTime in startTime..endTime
        }
        val total = list.size
        val taken = list.count { it.status == DoseStatus.TAKEN }
        val skipped = list.count { it.status == DoseStatus.SKIPPED }
        val missed = list.count { it.status == DoseStatus.MISSED }
        val pending = list.count { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }

        val rate = if (total > 0) {
            (taken.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }

        return AdherenceStats(
            totalScheduled = total,
            takenCount = taken,
            skippedCount = skipped,
            missedCount = missed,
            pendingCount = pending,
            adherencePercentage = rate
        )
    }

    suspend fun generateHistoryCsv(): String {
        val list = historyDao.getAllHistorySync()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("HistoryID,Medicine,Dosage,Scheduled Time,Action Time,Status,Notes\n")
        for (item in list) {
            val schedStr = sdf.format(Date(item.scheduledDateTime))
            val actionStr = item.actualActionDateTime?.let { sdf.format(Date(it)) } ?: "N/A"
            sb.append("${item.historyId},\"${item.medicineName}\",\"${item.dosage}\",\"$schedStr\",\"$actionStr\",${item.status.name},\"${item.notes ?: ""}\"\n")
        }
        return sb.toString()
    }

    suspend fun generateMedicinesCsv(): String {
        val list = medicineDao.getAllMedicines().firstOrNull() ?: emptyList()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("MedicineID,Name,Dosage,Type,Instructions,Active,Schedules\n")
        for (m in list) {
            val times = m.schedules.joinToString("; ") { String.format(Locale.getDefault(), "%02d:%02d (%s)", it.reminderHour, it.reminderMinute, it.frequency.name) }
            sb.append("${m.medicine.medicineId},\"${m.medicine.medicineName}\",\"${m.medicine.dosage}\",${m.medicine.medicineType.name},\"${m.medicine.instructions.name}\",${m.medicine.activeStatus},\"$times\"\n")
        }
        return sb.toString()
    }
}
