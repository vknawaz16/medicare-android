package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.model.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: MedicineHistoryEntity): Long

    @Update
    suspend fun updateHistory(history: MedicineHistoryEntity)

    @Query("SELECT * FROM medicine_history WHERE historyId = :historyId LIMIT 1")
    suspend fun getHistoryById(historyId: Long): MedicineHistoryEntity?

    @Query("""
        SELECT * FROM medicine_history 
        WHERE scheduleId = :scheduleId 
          AND scheduledDateTime >= :startOfDay 
          AND scheduledDateTime <= :endOfDay 
        LIMIT 1
    """)
    suspend fun getHistoryForScheduleAndDate(scheduleId: Long, startOfDay: Long, endOfDay: Long): MedicineHistoryEntity?

    @Query("""
        SELECT * FROM medicine_history 
        WHERE status = 'PENDING' AND scheduledDateTime < :cutoffTime
    """)
    suspend fun getPendingHistoriesBefore(cutoffTime: Long): List<MedicineHistoryEntity>

    @Query("""
        UPDATE medicine_history 
        SET status = 'MISSED' 
        WHERE status = 'PENDING' AND scheduledDateTime < :cutoffTime
    """)
    suspend fun markPendingAsMissed(cutoffTime: Long)

    @Query("""
        SELECT * FROM medicine_history 
        WHERE scheduledDateTime >= :startTime AND scheduledDateTime <= :endTime 
        ORDER BY scheduledDateTime DESC
    """)
    fun getHistoryBetween(startTime: Long, endTime: Long): Flow<List<MedicineHistoryEntity>>

    @Query("SELECT * FROM medicine_history ORDER BY scheduledDateTime DESC")
    fun getAllHistory(): Flow<List<MedicineHistoryEntity>>

    @Query("SELECT * FROM medicine_history ORDER BY scheduledDateTime DESC")
    suspend fun getAllHistorySync(): List<MedicineHistoryEntity>

    @Query("""
        SELECT * FROM medicine_history 
        WHERE scheduledDateTime >= :startOfDay AND scheduledDateTime <= :endOfDay 
        ORDER BY scheduledDateTime ASC
    """)
    fun getTodayHistory(startOfDay: Long, endOfDay: Long): Flow<List<MedicineHistoryEntity>>

    @Query("""
        SELECT * FROM medicine_history 
        WHERE scheduledDateTime >= :startOfDay AND scheduledDateTime <= :endOfDay 
        ORDER BY scheduledDateTime ASC
    """)
    suspend fun getTodayHistorySync(startOfDay: Long, endOfDay: Long): List<MedicineHistoryEntity>

    @Query("DELETE FROM medicine_history WHERE medicineId = :medicineId")
    suspend fun deleteHistoryByMedicineId(medicineId: Long)

    @Query("UPDATE medicine_history SET status = :status, actualActionDateTime = :actionTime, notes = :notes WHERE historyId = :historyId")
    suspend fun updateStatus(historyId: Long, status: DoseStatus, actionTime: Long?, notes: String? = null)
}
