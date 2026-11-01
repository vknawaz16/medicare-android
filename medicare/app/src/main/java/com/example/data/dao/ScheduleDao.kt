package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>): List<Long>

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE medicineId = :medicineId")
    suspend fun deleteSchedulesByMedicineId(medicineId: Long)

    @Query("SELECT * FROM schedules WHERE scheduleId = :scheduleId LIMIT 1")
    suspend fun getScheduleById(scheduleId: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE medicineId = :medicineId")
    fun getSchedulesForMedicine(medicineId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE enabledStatus = 1")
    suspend fun getAllEnabledSchedules(): List<ScheduleEntity>

    @Query("UPDATE schedules SET enabledStatus = :enabled WHERE scheduleId = :scheduleId")
    suspend fun setScheduleEnabled(scheduleId: Long, enabled: Boolean)
}
