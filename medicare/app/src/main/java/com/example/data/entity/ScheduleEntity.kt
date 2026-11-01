package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ScheduleFrequency

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["medicineId"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicineId"])]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val scheduleId: Long = 0,
    val medicineId: Long,
    val reminderHour: Int, // 0-23
    val reminderMinute: Int, // 0-59
    val frequency: ScheduleFrequency = ScheduleFrequency.EVERY_DAY,
    val selectedDays: String = "1,2,3,4,5,6,7", // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val enabledStatus: Boolean = true
) {
    fun parseDaysList(): List<Int> {
        return try {
            selectedDays.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
        } catch (_: Exception) {
            listOf(1, 2, 3, 4, 5, 6, 7)
        }
    }
}
