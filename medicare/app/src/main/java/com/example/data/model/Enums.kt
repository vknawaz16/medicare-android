package com.example.data.model

enum class MedicineType(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    SYRUP("Syrup"),
    INJECTION("Injection"),
    DROPS("Drops"),
    OTHER("Other")
}

enum class InstructionType(val displayName: String) {
    AFTER_FOOD("After food"),
    BEFORE_FOOD("Before food"),
    WITH_FOOD("With food"),
    CUSTOM("Custom instructions")
}

enum class ScheduleFrequency(val displayName: String) {
    EVERY_DAY("Every day"),
    SPECIFIC_DAYS("Selected days of week")
}

enum class DoseStatus(val displayName: String) {
    PENDING("Pending"),
    TAKEN("Taken"),
    SNOOZED("Snoozed"),
    SKIPPED("Skipped"),
    MISSED("Missed")
}
