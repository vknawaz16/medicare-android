package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithSchedules(
    @Embedded
    val medicine: MedicineEntity,
    @Relation(
        parentColumn = "medicineId",
        entityColumn = "medicineId"
    )
    val schedules: List<ScheduleEntity>
)
