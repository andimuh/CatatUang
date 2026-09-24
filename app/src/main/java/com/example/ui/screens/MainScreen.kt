package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddTransactionSheet
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

enum class MainTab(val title: String) {
    HOME("Harian"),
    ANALYTICS("Grafik Bulanan"),
    CLOUD_SYNC("Cloud Sync")
}

@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // If user is not authenticated (neither logged in nor in guest session), show Auth Screen
    if (currentUser == null) {
        Box(modifier = modifier.fillMaxSize()) {
            AuthScreen(
                isLoading = isAuthLoading,
                errorMessage = authError,
                onLogin = { email, pass -> viewModel.login(email, pass) },
                onRegister = { name, email, pass -> viewModel.register(name, email, pass) },
                onLoginAsGuest = { guestName -> viewModel.loginAsGuest(guestName) },
                onClearError = { viewModel.clearAuthError() }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val overallBalance by viewModel.overallBalance.collectAsStateWithLifecycle()
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                // Tab 1: Harian (Home)
                NavigationBarItem(
                    selected = currentTab == MainTab.HOME,
                    onClick = { currentTab = MainTab.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.HOME) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Catatan Harian"
                        )
                    },
                    label = { Text("Harian") },
                    modifier = Modifier.testTag("nav_item_home")
                )

                // Tab 2: Grafik Bulanan (Monthly Analytics & Charts)
                NavigationBarItem(
                    selected = currentTab == MainTab.ANALYTICS,
                    onClick = { currentTab = MainTab.ANALYTICS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.ANALYTICS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Grafik Bulanan"
                        )
                    },
                    label = { Text("Grafik") },
                    modifier = Modifier.testTag("nav_item_analytics")
                )

                // Tab 3: Cloud Sync
                NavigationBarItem(
                    selected = currentTab == MainTab.CLOUD_SYNC,
                    onClick = { currentTab = MainTab.CLOUD_SYNC },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unsyncedCount > 0) {
                                    Badge {
                                        Text("$unsyncedCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == MainTab.CLOUD_SYNC) Icons.Filled.CloudSync else Icons.Outlined.CloudSync,
                                contentDescription = "Cloud Sync"
                            )
                        }
                    },
                    label = { Text("Cloud Sync") },
                    modifier = Modifier.testTag("nav_item_cloud")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        overallBalance = overallBalance,
                        monthlyAnalytics = monthlyAnalytics,
                        transactions = filteredTransactions,
                        cloudSyncStatus = cloudSyncStatus,
                        unsyncedCount = unsyncedCount,
                        searchQuery = searchQuery,
                        filterType = filterType,
                        currentUser = currentUser,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterTypeChange = { viewModel.setFilterType(it) },
                        onSyncClick = { viewModel.syncToCloud() },
                        onAddClick = {
                            editingTransaction = null
                            showAddSheet = true
                        },
                        onEditTransaction = { item ->
                            editingTransaction = item
                            showAddSheet = true
                        },
                        onDeleteTransaction = { item ->
                            viewModel.deleteTransaction(item)
                        },
                        onNavigateToAnalytics = { currentTab = MainTab.ANALYTICS },
                        onNavigateToCloud = { currentTab = MainTab.CLOUD_SYNC },
                        onLogout = { viewModel.logout() }
                    )
                }

                MainTab.ANALYTICS -> {
                    MonthlyAnalyticsScreen(
                        monthlyAnalytics = monthlyAnalytics,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onResetToCurrentMonth = {
                            val c = java.util.Calendar.getInstance()
                            viewModel.setMonth(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH))
                        }
                    )
                }

                MainTab.CLOUD_SYNC -> {
                    CloudSyncScreen(
                        syncStatus = cloudSyncStatus,
                        unsyncedCount = unsyncedCount,
                        cloudBackupSize = viewModel.getCloudBackupSize(),
                        currentUser = currentUser,
                        onSyncNow = { viewModel.syncToCloud() },
                        onRestoreCloud = { viewModel.restoreFromCloud() },
                        onToggleAutoSync = { viewModel.toggleAutoSync(it) },
                        onLogout = { viewModel.logout() },
                        getCloudJson = { viewModel.getCloudBackupJson() }
                    )
                }
            }
        }
    }

    // Add or Edit Transaction Sheet
    if (showAddSheet) {
        AddTransactionSheet(
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
            },
            onSave = { title, amount, type, category, wallet, dateMillis, note ->
                if (editingTransaction != null) {
                    val updated = editingTransaction!!.copy(
                        title = title,
                        amount = amount,
                        type = type.name,
                        category = category,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                    viewModel.updateTransaction(updated)
                } else {
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        wallet = wallet,
                        dateMillis = dateMillis,
                        note = note
                    )
                }
                showAddSheet = false
                editingTransaction = null
            }
        )
    }
}
