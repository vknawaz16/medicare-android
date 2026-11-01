package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.InstructionType
import com.example.data.model.MedicineType

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val medicineId: Long = 0,
    val medicineName: String,
    val dosage: String,
    val medicineType: MedicineType,
    val startDate: Long,
    val endDate: Long? = null,
    val instructions: InstructionType = InstructionType.AFTER_FOOD,
    val customInstructions: String = "",
    val activeStatus: Boolean = true,
    val colorHex: String = "#0284C7",
    val createdAt: Long = System.currentTimeMillis()
)
