package com.example

import android.app.Application
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.db.MediCareDatabase
import com.example.data.repository.MediCareRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediCareApplication : Application() {

    lateinit var database: MediCareDatabase
        private set

    lateinit var repository: MediCareRepository
        private set

    companion object {
        private const val TAG = "MediCareApp"
    }

    override fun onCreate() {
        super.onCreate()

        database = MediCareDatabase.getDatabase(this)
        repository = MediCareRepository(
            medicineDao = database.medicineDao(),
            scheduleDao = database.scheduleDao(),
            historyDao = database.historyDao(),
            settingsDao = database.settingsDao()
        )

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)

        // Run missed dose detection and ensure active alarms are scheduled
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.checkAndMarkMissedDoses()
                AlarmScheduler.rescheduleAllActiveAlarms(this@MediCareApplication)
                Log.d(TAG, "MediCareApplication initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error during MediCareApplication startup", e)
            }
        }
    }
}
