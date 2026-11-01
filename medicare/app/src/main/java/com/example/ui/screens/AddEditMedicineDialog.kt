package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.MedicineEntity
import com.example.data.entity.MedicineWithSchedules
import com.example.data.entity.ScheduleEntity
import com.example.data.model.InstructionType
import com.example.data.model.MedicineType
import com.example.data.model.ScheduleFrequency
import java.util.Calendar
import java.util.Locale

data class TimeItem(val hour: Int, val minute: Int) {
    fun formatDisplay(): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, amPm)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicineDialog(
    editingItem: MedicineWithSchedules?,
    onDismiss: () -> Unit,
    onSave: (MedicineEntity, List<ScheduleEntity>) -> Unit
) {
    val context = LocalContext.current
    val isEditing = editingItem != null

    var medicineName by remember { mutableStateOf(editingItem?.medicine?.medicineName ?: "") }
    var dosage by remember { mutableStateOf(editingItem?.medicine?.dosage ?: "") }
    var selectedType by remember { mutableStateOf(editingItem?.medicine?.medicineType ?: MedicineType.TABLET) }
    var frequency by remember {
        mutableStateOf(
            editingItem?.schedules?.firstOrNull()?.frequency ?: ScheduleFrequency.EVERY_DAY
        )
    }
    var selectedDays by remember {
        mutableStateOf(
            editingItem?.schedules?.firstOrNull()?.selectedDays ?: "1,2,3,4,5,6,7"
        )
    }
    var instructionType by remember {
        mutableStateOf(editingItem?.medicine?.instructions ?: InstructionType.AFTER_FOOD)
    }
    var customInstruction by remember {
        mutableStateOf(editingItem?.medicine?.customInstructions ?: "")
    }
    var selectedColor by remember {
        mutableStateOf(editingItem?.medicine?.colorHex ?: "#0284C7")
    }

    val reminderTimes = remember {
        mutableStateListOf<TimeItem>().apply {
            if (isEditing && editingItem!!.schedules.isNotEmpty()) {
                addAll(editingItem.schedules.map { TimeItem(it.reminderHour, it.reminderMinute) })
            } else {
                add(TimeItem(8, 0)) // default 8:00 AM
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val daysOfWeek = listOf(
        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun"
    )
    val parsedDays = remember(selectedDays) {
        selectedDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    val availableColors = listOf("#0284C7", "#0D9488", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Medicine" else "Add New Medicine",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Medicine Name
                    OutlinedTextField(
                        value = medicineName,
                        onValueChange = {
                            medicineName = it
                            errorMessage = null
                        },
                        label = { Text("Medicine Name *") },
                        placeholder = { Text("e.g., Vitamin D3, Amoxicillin") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_medicine_name")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dosage
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = {
                            dosage = it
                            errorMessage = null
                        },
                        label = { Text("Dosage *") },
                        placeholder = { Text("e.g., 1 tablet, 500 mg, 10 ml") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_dosage")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Medicine Type Chips
                    Text(
                        text = "Medicine Form",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MedicineType.values().forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName) },
                                modifier = Modifier.testTag("type_${type.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reminder Times Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reminder Times (${reminderTimes.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        if (!reminderTimes.any { it.hour == hourOfDay && it.minute == minute }) {
                                            reminderTimes.add(TimeItem(hourOfDay, minute))
                                            reminderTimes.sortBy { it.hour * 60 + it.minute }
                                        }
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("btn_add_reminder_time")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Time", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reminderTimes.forEach { timeItem ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeItem.formatDisplay(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (reminderTimes.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { reminderTimes.remove(timeItem) },
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Frequency
                    Text(
                        text = "Frequency",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = frequency == ScheduleFrequency.EVERY_DAY,
                            onClick = { frequency = ScheduleFrequency.EVERY_DAY },
                            label = { Text("Every day") }
                        )
                        FilterChip(
                            selected = frequency == ScheduleFrequency.SPECIFIC_DAYS,
                            onClick = { frequency = ScheduleFrequency.SPECIFIC_DAYS },
                            label = { Text("Selected days") }
                        )
                    }

                    // Days of week selector if specific days
                    if (frequency == ScheduleFrequency.SPECIFIC_DAYS) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysOfWeek.forEach { (num, name) ->
                                val isSelected = parsedDays.contains(num)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            val current = parsedDays.toMutableSet()
                                            if (isSelected) {
                                                if (current.size > 1) current.remove(num)
                                            } else {
                                                current.add(num)
                                            }
                                            selectedDays = current.sorted().joinToString(",")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.first().toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instructions
                    Text(
                        text = "Food & Intake Instructions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InstructionType.values().forEach { inst ->
                            FilterChip(
                                selected = instructionType == inst,
                                onClick = { instructionType = inst },
                                label = { Text(inst.displayName) }
                            )
                        }
                    }

                    if (instructionType == InstructionType.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customInstruction,
                            onValueChange = { customInstruction = it },
                            label = { Text("Custom Instructions") },
                            placeholder = { Text("e.g. Take with 1 full glass of warm water") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Tag
                    Text(
                        text = "Card Color Tag",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        availableColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isChosen = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex }
                                    .border(
                                        width = if (isChosen) 3.dp else 0.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Error display
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (medicineName.isBlank()) {
                                errorMessage = "Please enter medicine name."
                                return@Button
                            }
                            if (dosage.isBlank()) {
                                errorMessage = "Please specify dosage."
                                return@Button
                            }
                            if (reminderTimes.isEmpty()) {
                                errorMessage = "Please add at least one reminder time."
                                return@Button
                            }

                            val startOfDay = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }.timeInMillis

                            val med = MedicineEntity(
                                medicineId = editingItem?.medicine?.medicineId ?: 0L,
                                medicineName = medicineName.trim(),
                                dosage = dosage.trim(),
                                medicineType = selectedType,
                                startDate = editingItem?.medicine?.startDate ?: startOfDay,
                                instructions = instructionType,
                                customInstructions = customInstruction.trim(),
                                colorHex = selectedColor,
                                activeStatus = editingItem?.medicine?.activeStatus ?: true
                            )

                            val schedules = reminderTimes.map { t ->
                                ScheduleEntity(
                                    medicineId = med.medicineId,
                                    reminderHour = t.hour,
                                    reminderMinute = t.minute,
                                    frequency = frequency,
                                    selectedDays = if (frequency == ScheduleFrequency.EVERY_DAY) "1,2,3,4,5,6,7" else selectedDays,
                                    enabledStatus = true
                                )
                            }

                            onSave(med, schedules)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_medicine"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Medicine")
                    }
                }
            }
        }
    }
}
