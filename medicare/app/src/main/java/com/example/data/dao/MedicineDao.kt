package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineWithSchedules
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun getAllMedicines(): Flow<List<MedicineWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE activeStatus = 1 ORDER BY createdAt DESC")
    fun getActiveMedicines(): Flow<List<MedicineWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE medicineId = :id LIMIT 1")
    suspend fun getMedicineWithSchedulesById(id: Long): MedicineWithSchedules?

    @Transaction
    @Query("SELECT * FROM medicines WHERE activeStatus = 1")
    suspend fun getActiveMedicinesSync(): List<MedicineWithSchedules>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET activeStatus = :active WHERE medicineId = :id")
    suspend fun setMedicineActive(id: Long, active: Boolean)
}
