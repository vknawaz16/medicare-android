package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MedicineHistoryEntity
import com.example.data.model.DoseStatus
import com.example.ui.HistoryDateFilter
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusTaken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<MedicineHistoryEntity>,
    dateFilter: HistoryDateFilter,
    statusFilter: DoseStatus?,
    onDateFilterChange: (HistoryDateFilter) -> Unit,
    onStatusFilterChange: (DoseStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredList = remember(historyList, dateFilter, statusFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Filter by Date
        val dateFiltered = when (dateFilter) {
            HistoryDateFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                historyList.filter { it.scheduledDateTime in start..end }
            }
            HistoryDateFilter.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                historyList.filter { it.scheduledDateTime in start..end }
            }
            HistoryDateFilter.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                historyList.filter { it.scheduledDateTime in start..now }
            }
            HistoryDateFilter.LAST_30_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                historyList.filter { it.scheduledDateTime in start..now }
            }
            HistoryDateFilter.ALL -> historyList
        }

        // Filter by Status
        if (statusFilter != null) {
            dateFiltered.filter { it.status == statusFilter }
        } else {
            dateFiltered
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Medicine History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredList.size} log entries recorded",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Filter Chips (Horizontal Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryDateFilter.values().forEach { filter ->
                FilterChip(
                    selected = dateFilter == filter,
                    onClick = { onDateFilterChange(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("filter_date_${filter.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All Statuses") },
                modifier = Modifier.testTag("filter_status_ALL")
            )
            listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED, DoseStatus.PENDING).forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(if (statusFilter == status) null else status) },
                    label = { Text(status.displayName) },
                    modifier = Modifier.testTag("filter_status_${status.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History List
        if (filteredList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No History Records Found",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No doses match the selected filters. Records are automatically logged as you take, snooze, or skip medicines.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.historyId }) { historyItem ->
                    HistoryItemCard(history = historyItem)
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(history: MedicineHistoryEntity) {
    val dateSdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    val actionTimeSdf = SimpleDateFormat("h:mm a", Locale.getDefault())

    val scheduledStr = dateSdf.format(Date(history.scheduledDateTime))
    val actionStr = history.actualActionDateTime?.let { actionTimeSdf.format(Date(it)) }

    val statusColor = when (history.status) {
        DoseStatus.TAKEN -> StatusTaken
        DoseStatus.SKIPPED -> StatusSkipped
        DoseStatus.MISSED -> StatusMissed
        DoseStatus.SNOOZED -> MaterialTheme.colorScheme.primary
        DoseStatus.PENDING -> StatusPending
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (history.status) {
                            DoseStatus.TAKEN -> Icons.Default.CheckCircle
                            DoseStatus.MISSED -> Icons.Default.Warning
                            else -> Icons.Default.Medication
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = history.medicineName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = history.dosage,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scheduled: $scheduledStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (actionStr != null) {
                        Text(
                            text = "Action: ${history.status.displayName} at $actionStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = history.status.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
