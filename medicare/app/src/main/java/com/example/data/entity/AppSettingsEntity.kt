package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val settingId: Int = 1,
    val alarmSoundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val isDarkMode: Boolean? = null, // null = system default, true = dark, false = light
    val gracePeriodMinutes: Int = 60 // minutes after scheduled time before marked missed
)
