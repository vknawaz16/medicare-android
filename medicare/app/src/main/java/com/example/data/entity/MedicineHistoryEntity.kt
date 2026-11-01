package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.DoseStatus

@Entity(
    tableName = "medicine_history",
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["scheduleId"]),
        Index(value = ["scheduledDateTime"])
    ]
)
data class MedicineHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0,
    val medicineId: Long,
    val scheduleId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledDateTime: Long, // Epoch ms
    val actualActionDateTime: Long? = null, // Epoch ms when user marked taken/skipped
    val status: DoseStatus = DoseStatus.PENDING,
    val notes: String? = null
)
