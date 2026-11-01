package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.NavTab
import com.example.ui.screens.AddEditMedicineDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MediCareTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.appSettings.collectAsStateWithLifecycle()

            val isDarkTheme = when (settings.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            MediCareTheme(darkTheme = isDarkTheme) {
                MediCareApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediCareApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val todayDoses by viewModel.todayDoses.collectAsStateWithLifecycle()
    val allMedicines by viewModel.allMedicines.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val historyDateFilter by viewModel.historyDateFilter.collectAsStateWithLifecycle()
    val historyStatusFilter by viewModel.historyStatusFilter.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingMedicine by viewModel.editingMedicine.collectAsStateWithLifecycle()
    val medicineToDelete by viewModel.medicineToDelete.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val weeklyBreakdown by viewModel.weeklyBreakdown.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Notification Permission Handling
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show Snackbars
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Back handler for secondary tabs: pops back to Home
    BackHandler(enabled = currentTab != NavTab.HOME) {
        viewModel.selectTab(NavTab.HOME)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MediCareBottomNav(
                currentTab = currentTab,
                onSelectTab = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        todayDoses = todayDoses,
                        activeMedicinesCount = allMedicines.count { it.medicine.activeStatus },
                        onAddMedicine = { viewModel.openAddMedicine() }
                    )
                }

                NavTab.MEDICINES -> {
                    MedicinesScreen(
                        medicines = allMedicines,
                        medicineToDelete = medicineToDelete,
                        onAddMedicine = { viewModel.openAddMedicine() },
                        onEditMedicine = { viewModel.openEditMedicine(it) },
                        onDeleteRequest = { viewModel.requestDeleteMedicine(it) },
                        onConfirmDelete = { viewModel.confirmDeleteMedicine() },
                        onDismissDelete = { viewModel.dismissDeleteDialog() },
                        onToggleActive = { item, active -> viewModel.toggleMedicineActive(item, active) }
                    )
                }

                NavTab.HISTORY -> {
                    HistoryScreen(
                        historyList = allHistory,
                        dateFilter = historyDateFilter,
                        statusFilter = historyStatusFilter,
                        onDateFilterChange = { viewModel.setHistoryDateFilter(it) },
                        onStatusFilterChange = { viewModel.setHistoryStatusFilter(it) }
                    )
                }

                NavTab.PROGRESS -> {
                    ProgressScreen(
                        dailyStats = dailyStats,
                        weeklyStats = weeklyStats,
                        monthlyStats = monthlyStats,
                        weeklyBreakdown = weeklyBreakdown
                    )
                }

                NavTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        viewModel = viewModel,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        hasNotificationPermission = hasNotificationPermission
                    )
                }
            }

            // Add/Edit Medicine Dialog Modal
            if (isAddEditOpen) {
                AddEditMedicineDialog(
                    editingItem = editingMedicine,
                    onDismiss = { viewModel.closeAddEdit() },
                    onSave = { med, schedules ->
                        viewModel.saveMedicine(med, schedules)
                    }
                )
            }
        }
    }
}

@Composable
fun MediCareBottomNav(
    currentTab: NavTab,
    onSelectTab: (NavTab) -> Unit
) {
    val navItems = listOf(
        NavItem(NavTab.HOME, "Home", Icons.Default.Home),
        NavItem(NavTab.MEDICINES, "Medicines", Icons.Default.Medication),
        NavItem(NavTab.HISTORY, "History", Icons.Default.History),
        NavItem(NavTab.PROGRESS, "Progress", Icons.Default.Assessment),
        NavItem(NavTab.SETTINGS, "Settings", Icons.Default.Settings)
    )

    NavigationBar(
        tonalElevation = 6.dp
    ) {
        navItems.forEach { item ->
            val selected = currentTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}")
            )
        }
    }
}

data class NavItem(
    val tab: NavTab,
    val label: String,
    val icon: ImageVector
)
