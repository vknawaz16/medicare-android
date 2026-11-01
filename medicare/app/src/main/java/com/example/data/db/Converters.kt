package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.DoseStatus
import com.example.data.model.InstructionType
import com.example.data.model.MedicineType
import com.example.data.model.ScheduleFrequency

class Converters {

    @TypeConverter
    fun fromMedicineType(value: MedicineType): String = value.name

    @TypeConverter
    fun toMedicineType(value: String): MedicineType =
        runCatching { MedicineType.valueOf(value) }.getOrDefault(MedicineType.TABLET)

    @TypeConverter
    fun fromInstructionType(value: InstructionType): String = value.name

    @TypeConverter
    fun toInstructionType(value: String): InstructionType =
        runCatching { InstructionType.valueOf(value) }.getOrDefault(InstructionType.AFTER_FOOD)

    @TypeConverter
    fun fromScheduleFrequency(value: ScheduleFrequency): String = value.name

    @TypeConverter
    fun toScheduleFrequency(value: String): ScheduleFrequency =
        runCatching { ScheduleFrequency.valueOf(value) }.getOrDefault(ScheduleFrequency.EVERY_DAY)

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus =
        runCatching { DoseStatus.valueOf(value) }.getOrDefault(DoseStatus.PENDING)
}
