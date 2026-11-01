package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.HistoryDao
import com.example.data.dao.MedicineDao
import com.example.data.dao.ScheduleDao
import com.example.data.dao.SettingsDao
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.entity.ScheduleEntity
import com.example.data.model.DoseStatus
import com.example.data.model.InstructionType
import com.example.data.model.MedicineType
import com.example.data.model.ScheduleFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        MedicineEntity::class,
        ScheduleEntity::class,
        MedicineHistoryEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MediCareDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MediCareDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): MediCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediCareDatabase::class.java,
                    "medicare_database.db"
                )
                    .addCallback(MediCareDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class MediCareDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: MediCareDatabase) {
            val settingsDao = database.settingsDao()
            settingsDao.insertOrUpdate(
                AppSettingsEntity(
                    settingId = 1,
                    alarmSoundEnabled = true,
                    vibrationEnabled = true,
                    defaultSnoozeMinutes = 10,
                    isDarkMode = null,
                    gracePeriodMinutes = 60
                )
            )

            // Seed sample medicines for first-time user
            val medicineDao = database.medicineDao()
            val scheduleDao = database.scheduleDao()
            val historyDao = database.historyDao()

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis

            // 1. Vitamin D (Tablet - 8:00 AM, After food)
            val vitDId = medicineDao.insertMedicine(
                MedicineEntity(
                    medicineName = "Vitamin D3",
                    dosage = "1000 IU (1 Tablet)",
                    medicineType = MedicineType.TABLET,
                    startDate = startOfDay,
                    instructions = InstructionType.AFTER_FOOD,
                    colorHex = "#0284C7"
                )
            )
            val vitDScheduleId = scheduleDao.insertSchedule(
                ScheduleEntity(
                    medicineId = vitDId,
                    reminderHour = 8,
                    reminderMinute = 0,
                    frequency = ScheduleFrequency.EVERY_DAY
                )
            )

            // 2. Omega-3 Fish Oil (Capsule - 14:00 PM, With food)
            val omegaId = medicineDao.insertMedicine(
                MedicineEntity(
                    medicineName = "Omega-3 Fish Oil",
                    dosage = "1000 mg (1 Capsule)",
                    medicineType = MedicineType.CAPSULE,
                    startDate = startOfDay,
                    instructions = InstructionType.WITH_FOOD,
                    colorHex = "#0D9488"
                )
            )
            val omegaScheduleId = scheduleDao.insertSchedule(
                ScheduleEntity(
                    medicineId = omegaId,
                    reminderHour = 14,
                    reminderMinute = 0,
                    frequency = ScheduleFrequency.EVERY_DAY
                )
            )

            // Seed today's doses in history
            val vitDTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val omegaTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 14)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            // Vit D marked as taken if past 8am, otherwise pending
            val isVitDPast = now > vitDTime
            historyDao.insertHistory(
                MedicineHistoryEntity(
                    medicineId = vitDId,
                    scheduleId = vitDScheduleId,
                    medicineName = "Vitamin D3",
                    dosage = "1000 IU (1 Tablet)",
                    scheduledDateTime = vitDTime,
                    actualActionDateTime = if (isVitDPast) vitDTime + 5 * 60 * 1000 else null,
                    status = if (isVitDPast) DoseStatus.TAKEN else DoseStatus.PENDING
                )
            )

            historyDao.insertHistory(
                MedicineHistoryEntity(
                    medicineId = omegaId,
                    scheduleId = omegaScheduleId,
                    medicineName = "Omega-3 Fish Oil",
                    dosage = "1000 mg (1 Capsule)",
                    scheduledDateTime = omegaTime,
                    actualActionDateTime = null,
                    status = DoseStatus.PENDING
                )
            )
        }
    }
}
