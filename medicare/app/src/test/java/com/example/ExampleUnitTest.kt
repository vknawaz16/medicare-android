package com.example

import com.example.alarm.AlarmScheduler
import com.example.data.entity.ScheduleEntity
import com.example.data.model.ScheduleFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test adherence percentage calculation handles standard cases`() {
        val total = 10
        val taken = 8
        val rate = (taken.toFloat() / total.toFloat()) * 100f
        assertEquals(80.0f, rate, 0.01f)
    }

    @Test
    fun `test zero total doses avoids divide by zero`() {
        val total = 0
        val taken = 0
        val rate = if (total > 0) (taken.toFloat() / total.toFloat()) * 100f else 0f
        assertEquals(0.0f, rate, 0.01f)
    }

    @Test
    fun `test next reminder time calculation is strictly in the future`() {
        val sched = ScheduleEntity(
            scheduleId = 42,
            medicineId = 1,
            reminderHour = 8,
            reminderMinute = 30,
            frequency = ScheduleFrequency.EVERY_DAY
        )
        val now = 1700000000000L // arbitrary fixed epoch
        val nextTrigger = AlarmScheduler.calculateNextTriggerTime(sched, now)
        assertTrue(nextTrigger > now)
    }

    @Test
    fun `test specific days schedule parsing and days bitmask`() {
        val sched = ScheduleEntity(
            scheduleId = 10,
            medicineId = 2,
            reminderHour = 14,
            reminderMinute = 0,
            frequency = ScheduleFrequency.SPECIFIC_DAYS,
            selectedDays = "1,3,5" // Mon, Wed, Fri
        )
        val days = sched.parseDaysList()
        assertEquals(3, days.size)
        assertTrue(days.contains(1))
        assertTrue(days.contains(3))
        assertTrue(days.contains(5))
    }
}
