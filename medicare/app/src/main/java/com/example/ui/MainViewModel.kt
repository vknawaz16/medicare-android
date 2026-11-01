package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MediCareApplication
import com.example.alarm.AlarmScheduler
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.entity.MedicineWithSchedules
import com.example.data.entity.ScheduleEntity
import com.example.data.model.DoseStatus
import com.example.data.repository.AdherenceStats
import com.example.data.repository.MediCareRepository
import com.example.data.repository.ScheduledDoseItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavTab(val title: String) {
    HOME("Home"),
    MEDICINES("Medicines"),
    HISTORY("History"),
    PROGRESS("Progress"),
    SETTINGS("Settings")
}

enum class HistoryDateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL("All Time")
}

data class DayAdherence(
    val dayLabel: String,
    val dateEpoch: Long,
    val adherencePercent: Float,
    val takenCount: Int,
    val totalCount: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediCareRepository = (application as MediCareApplication).repository

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Today Doses
    private val _todayDoses = MutableStateFlow<List<ScheduledDoseItem>>(emptyList())
    val todayDoses: StateFlow<List<ScheduledDoseItem>> = _todayDoses.asStateFlow()

    // Medicines
    val allMedicines: StateFlow<List<MedicineWithSchedules>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All History
    val allHistory: StateFlow<List<MedicineHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Settings
    val appSettings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettingsEntity()
        )

    // History Filters
    private val _historyDateFilter = MutableStateFlow(HistoryDateFilter.TODAY)
    val historyDateFilter: StateFlow<HistoryDateFilter> = _historyDateFilter.asStateFlow()

    private val _historyStatusFilter = MutableStateFlow<DoseStatus?>(null) // null = all
    val historyStatusFilter: StateFlow<DoseStatus?> = _historyStatusFilter.asStateFlow()

    // Add/Edit Medicine Dialog State
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingMedicine = MutableStateFlow<MedicineWithSchedules?>(null)
    val editingMedicine: StateFlow<MedicineWithSchedules?> = _editingMedicine.asStateFlow()

    // Medicine Delete Confirmation
    private val _medicineToDelete = MutableStateFlow<MedicineEntity?>(null)
    val medicineToDelete: StateFlow<MedicineEntity?> = _medicineToDelete.asStateFlow()

    // Progress Stats
    private val _dailyStats = MutableStateFlow(AdherenceStats(0, 0, 0, 0, 0, 0f))
    val dailyStats: StateFlow<AdherenceStats> = _dailyStats.asStateFlow()

    private val _weeklyStats = MutableStateFlow(AdherenceStats(0, 0, 0, 0, 0, 0f))
    val weeklyStats: StateFlow<AdherenceStats> = _weeklyStats.asStateFlow()

    private val _monthlyStats = MutableStateFlow(AdherenceStats(0, 0, 0, 0, 0, 0f))
    val monthlyStats: StateFlow<AdherenceStats> = _monthlyStats.asStateFlow()

    private val _weeklyBreakdown = MutableStateFlow<List<DayAdherence>>(emptyList())
    val weeklyBreakdown: StateFlow<List<DayAdherence>> = _weeklyBreakdown.asStateFlow()

    // Message toast / snackbar
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshTodayDoses()
        refreshProgressStats()
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
        if (tab == NavTab.HOME) {
            refreshTodayDoses()
        } else if (tab == NavTab.PROGRESS) {
            refreshProgressStats()
        }
    }

    fun refreshTodayDoses() {
        viewModelScope.launch {
            try {
                val list = repository.getTodayDoseItems()
                _todayDoses.value = list
                calculateDailyStatsFromToday(list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateDailyStatsFromToday(doses: List<ScheduledDoseItem>) {
        val total = doses.size
        val taken = doses.count { it.status == DoseStatus.TAKEN }
        val skipped = doses.count { it.status == DoseStatus.SKIPPED }
        val missed = doses.count { it.status == DoseStatus.MISSED }
        val pending = doses.count { it.status == DoseStatus.PENDING || it.status == DoseStatus.SNOOZED }
        val pct = if (total > 0) (taken.toFloat() / total.toFloat()) * 100f else 0f
        _dailyStats.value = AdherenceStats(total, taken, skipped, missed, pending, pct)
    }

    fun markDoseTaken(doseItem: ScheduledDoseItem) {
        viewModelScope.launch {
            com.example.alarm.AlarmSoundPlayer.stop()
            com.example.alarm.NotificationHelper.dismissNotification(getApplication(), doseItem.schedule.scheduleId)
            repository.markDoseTaken(
                doseItem.historyId,
                doseItem.medicine.medicineId,
                doseItem.schedule.scheduleId,
                doseItem.scheduledTimeMillis
            )
            AlarmScheduler.cancelAlarm(getApplication(), doseItem.schedule.scheduleId)
            // Schedule the subsequent occurrence
            AlarmScheduler.scheduleAlarm(getApplication(), doseItem.schedule, doseItem.medicine)
            _toastMessage.value = "${doseItem.medicine.medicineName} marked as Taken!"
            refreshTodayDoses()
            refreshProgressStats()
        }
    }

    fun markDoseSkipped(doseItem: ScheduledDoseItem) {
        viewModelScope.launch {
            com.example.alarm.AlarmSoundPlayer.stop()
            com.example.alarm.NotificationHelper.dismissNotification(getApplication(), doseItem.schedule.scheduleId)
            repository.markDoseSkipped(
                doseItem.historyId,
                doseItem.medicine.medicineId,
                doseItem.schedule.scheduleId,
                doseItem.scheduledTimeMillis
            )
            AlarmScheduler.cancelAlarm(getApplication(), doseItem.schedule.scheduleId)
            AlarmScheduler.scheduleAlarm(getApplication(), doseItem.schedule, doseItem.medicine)
            _toastMessage.value = "${doseItem.medicine.medicineName} dose skipped."
            refreshTodayDoses()
            refreshProgressStats()
        }
    }

    fun snoozeDose(doseItem: ScheduledDoseItem, minutes: Int) {
        viewModelScope.launch {
            com.example.alarm.AlarmSoundPlayer.stop()
            com.example.alarm.NotificationHelper.dismissNotification(getApplication(), doseItem.schedule.scheduleId)
            repository.markDoseSnoozed(doseItem.historyId, minutes)
            AlarmScheduler.scheduleSnooze(
                context = getApplication(),
                scheduleId = doseItem.schedule.scheduleId,
                medicineId = doseItem.medicine.medicineId,
                medicineName = doseItem.medicine.medicineName,
                dosage = doseItem.medicine.dosage,
                instructions = doseItem.medicine.instructions.displayName,
                snoozeMinutes = minutes,
                historyId = doseItem.historyId
            )
            _toastMessage.value = "Snoozed for $minutes minutes"
            refreshTodayDoses()
        }
    }

    fun openAddMedicine() {
        _editingMedicine.value = null
        _isAddEditOpen.value = true
    }

    fun openEditMedicine(medicineWithSchedules: MedicineWithSchedules) {
        _editingMedicine.value = medicineWithSchedules
        _isAddEditOpen.value = true
    }

    fun closeAddEdit() {
        _isAddEditOpen.value = false
        _editingMedicine.value = null
    }

    fun saveMedicine(medicine: MedicineEntity, schedules: List<ScheduleEntity>) {
        viewModelScope.launch {
            val medId = repository.saveMedicineWithSchedules(medicine, schedules)
            val fullMed = repository.getMedicineWithSchedulesById(medId)
            if (fullMed != null) {
                // Schedule alarms for all enabled schedules
                for (s in fullMed.schedules) {
                    AlarmScheduler.scheduleAlarm(getApplication(), s, fullMed.medicine)
                }
            }
            closeAddEdit()
            _toastMessage.value = "${medicine.medicineName} saved successfully!"
            refreshTodayDoses()
        }
    }

    fun requestDeleteMedicine(medicine: MedicineEntity) {
        _medicineToDelete.value = medicine
    }

    fun dismissDeleteDialog() {
        _medicineToDelete.value = null
    }

    fun confirmDeleteMedicine() {
        val med = _medicineToDelete.value ?: return
        viewModelScope.launch {
            // Cancel all existing alarms for this medicine
            val medWithSched = repository.getMedicineWithSchedulesById(med.medicineId)
            medWithSched?.schedules?.forEach {
                AlarmScheduler.cancelAlarm(getApplication(), it.scheduleId)
            }
            repository.deleteMedicine(med)
            _medicineToDelete.value = null
            _toastMessage.value = "${med.medicineName} deleted."
            refreshTodayDoses()
            refreshProgressStats()
        }
    }

    fun toggleMedicineActive(medicineWithSchedules: MedicineWithSchedules, active: Boolean) {
        viewModelScope.launch {
            repository.toggleMedicineActive(medicineWithSchedules.medicine.medicineId, active)
            if (active) {
                for (s in medicineWithSchedules.schedules) {
                    AlarmScheduler.scheduleAlarm(getApplication(), s, medicineWithSchedules.medicine.copy(activeStatus = true))
                }
                _toastMessage.value = "${medicineWithSchedules.medicine.medicineName} resumed."
            } else {
                for (s in medicineWithSchedules.schedules) {
                    AlarmScheduler.cancelAlarm(getApplication(), s.scheduleId)
                }
                _toastMessage.value = "${medicineWithSchedules.medicine.medicineName} paused."
            }
            refreshTodayDoses()
        }
    }

    fun setHistoryDateFilter(filter: HistoryDateFilter) {
        _historyDateFilter.value = filter
    }

    fun setHistoryStatusFilter(status: DoseStatus?) {
        _historyStatusFilter.value = status
    }

    fun refreshProgressStats() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Today range
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            // 7 Days ago
            val cal7 = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startOf7Days = cal7.timeInMillis

            // 30 Days ago
            val cal30 = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -29)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startOf30Days = cal30.timeInMillis

            val weekStats = repository.calculateStats(startOf7Days, now)
            val monthStats = repository.calculateStats(startOf30Days, now)
            _weeklyStats.value = weekStats
            _monthlyStats.value = monthStats

            // Calculate daily breakdown for the past 7 days
            val dayBreakdowns = mutableListOf<DayAdherence>()
            val dayCal = Calendar.getInstance()
            val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

            for (i in 6 downTo 0) {
                val dayStartCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayEndCal = Calendar.getInstance().apply {
                    timeInMillis = dayStartCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val st = repository.calculateStats(dayStartCal.timeInMillis, dayEndCal.timeInMillis)
                dayBreakdowns.add(
                    DayAdherence(
                        dayLabel = dayFormat.format(dayStartCal.time),
                        dateEpoch = dayStartCal.timeInMillis,
                        adherencePercent = st.adherencePercentage,
                        takenCount = st.takenCount,
                        totalCount = st.totalScheduled
                    )
                )
            }
            _weeklyBreakdown.value = dayBreakdowns
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            _toastMessage.value = "Settings updated."
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    suspend fun getExportHistoryCsv(): String = repository.generateHistoryCsv()
    suspend fun getExportMedicinesCsv(): String = repository.generateMedicinesCsv()
}
