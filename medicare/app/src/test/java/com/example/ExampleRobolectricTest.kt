package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.db.MediCareDatabase
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.entity.ScheduleEntity
import com.example.data.model.DoseStatus
import com.example.data.model.InstructionType
import com.example.data.model.MedicineType
import com.example.data.model.ScheduleFrequency
import com.example.data.repository.MediCareRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies MediCare app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MediCare", appName)
    }

    @Test
    fun `test schedule calculation returns future timestamp`() {
        val sched = ScheduleEntity(
            scheduleId = 1,
            medicineId = 1,
            reminderHour = 8,
            reminderMinute = 0,
            frequency = ScheduleFrequency.EVERY_DAY
        )
        val now = System.currentTimeMillis()
        val nextTime = AlarmScheduler.calculateNextTriggerTime(sched, now)
        assertTrue(nextTime > now)
    }

    @Test
    fun `test in-memory database operations and repository calculations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(context, MediCareDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repo = MediCareRepository(
            medicineDao = inMemoryDb.medicineDao(),
            scheduleDao = inMemoryDb.scheduleDao(),
            historyDao = inMemoryDb.historyDao(),
            settingsDao = inMemoryDb.settingsDao()
        )

        // 1. Insert medicine with schedules
        val med = MedicineEntity(
            medicineName = "Amoxicillin",
            dosage = "500 mg",
            medicineType = MedicineType.CAPSULE,
            startDate = System.currentTimeMillis(),
            instructions = InstructionType.AFTER_FOOD
        )
        val scheds = listOf(
            ScheduleEntity(medicineId = 0, reminderHour = 9, reminderMinute = 0),
            ScheduleEntity(medicineId = 0, reminderHour = 21, reminderMinute = 0)
        )
        val medId = repo.saveMedicineWithSchedules(med, scheds)
        assertTrue(medId > 0)

        val retrieved = repo.getMedicineWithSchedulesById(medId)
        assertNotNull(retrieved)
        assertEquals("Amoxicillin", retrieved?.medicine?.medicineName)
        assertEquals(2, retrieved?.schedules?.size)

        // 2. Mark dose taken and calculate adherence stats
        val schedId = retrieved!!.schedules.first().scheduleId
        val schedTime = System.currentTimeMillis()
        repo.markDoseTaken(null, medId, schedId, schedTime)

        val stats = repo.calculateStats(schedTime - 1000, schedTime + 1000)
        assertEquals(1, stats.totalScheduled)
        assertEquals(1, stats.takenCount)
        assertEquals(100f, stats.adherencePercentage, 0.01f)

        // 3. Test empty date range avoids divide-by-zero
        val emptyStats = repo.calculateStats(0, 100)
        assertEquals(0, emptyStats.totalScheduled)
        assertEquals(0f, emptyStats.adherencePercentage, 0.01f)

        inMemoryDb.close()
    }
}
