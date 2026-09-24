package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudSyncService
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryItem
import com.example.data.model.CategoryRegistry
import com.example.data.model.CloudSyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.ui.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryExpenseSummary(
    val category: CategoryItem,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class MonthlyAnalytics(
    val year: Int,
    val monthIndex: Int,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val dailyExpenses: Map<Int, Double>,
    val maxDailyExpense: Double,
    val maxExpenseDay: Int?,
    val categoryBreakdown: List<CategoryExpenseSummary>,
    val averageDailyExpense: Double
)

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository,
    private val authRepository: AuthRepository
) : AndroidViewModel(application) {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val calendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    private val _selectedFilterDateMillis = MutableStateFlow<Long?>(null)
    val selectedFilterDateMillis: StateFlow<Long?> = _selectedFilterDateMillis.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow(
        CloudSyncStatus(
            isSyncing = false,
            lastSyncTimestamp = repository.getLastSyncTime(),
            cloudItemsCount = repository.getCloudBackupCount(),
            isAutoSyncEnabled = repository.isAutoSync(),
            cloudAccountEmail = "muhmappanganroandi@gmail.com"
        )
    )
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unsyncedCount: StateFlow<Int> = repository.unsyncedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Filtered transactions for Home Screen list
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _filterType,
        _selectedFilterDateMillis
    ) { list, query, type, filterDate ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.note.contains(query, ignoreCase = true)

            val matchesType = type == null || item.type == type.name

            val matchesDate = filterDate == null || DateUtils.isSameDay(item.dateMillis, filterDate)

            matchesQuery && matchesType && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly Analytics for selected month
    val monthlyAnalytics: StateFlow<MonthlyAnalytics> = combine(
        allTransactions,
        _selectedYear,
        _selectedMonth
    ) { list, year, month ->
        val startMillis = DateUtils.getStartOfMonthMillis(year, month)
        val endMillis = DateUtils.getEndOfMonthMillis(year, month)

        val monthTransactions = list.filter {
            it.dateMillis in startMillis..endMillis
        }

        var totalExpense = 0.0
        var totalIncome = 0.0
        val dailyExpenses = mutableMapOf<Int, Double>()
        val categoryExpenses = mutableMapOf<String, Double>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (tx in monthTransactions) {
            if (tx.type == TransactionType.EXPENSE.name) {
                totalExpense += tx.amount
                val dayOfMonth = DateUtils.getDayOfMonth(tx.dateMillis)
                dailyExpenses[dayOfMonth] = (dailyExpenses[dayOfMonth] ?: 0.0) + tx.amount

                categoryExpenses[tx.category] = (categoryExpenses[tx.category] ?: 0.0) + tx.amount
                categoryCounts[tx.category] = (categoryCounts[tx.category] ?: 0) + 1
            } else if (tx.type == TransactionType.INCOME.name) {
                totalIncome += tx.amount
            }
        }

        val netBalance = totalIncome - totalExpense

        // Find peak day
        var maxDailyExpense = 0.0
        var maxExpenseDay: Int? = null
        dailyExpenses.forEach { (day, amount) ->
            if (amount > maxDailyExpense) {
                maxDailyExpense = amount
                maxExpenseDay = day
            }
        }

        // Category breakdown
        val breakdown = categoryExpenses.map { (catName, amount) ->
            val percentage = if (totalExpense > 0) ((amount / totalExpense) * 100).toFloat() else 0f
            val catItem = CategoryRegistry.getCategory(catName, TransactionType.EXPENSE)
            CategoryExpenseSummary(
                category = catItem,
                totalAmount = amount,
                percentage = percentage,
                transactionCount = categoryCounts[catName] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        val daysInMonth = DateUtils.getDaysInMonth(year, month)
        val averageDailyExpense = if (daysInMonth > 0) totalExpense / daysInMonth else 0.0

        MonthlyAnalytics(
            year = year,
            monthIndex = month,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netBalance = netBalance,
            dailyExpenses = dailyExpenses,
            maxDailyExpense = maxDailyExpense,
            maxExpenseDay = maxExpenseDay,
            categoryBreakdown = breakdown,
            averageDailyExpense = averageDailyExpense
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyAnalytics(
            year = calendar.get(Calendar.YEAR),
            monthIndex = calendar.get(Calendar.MONTH),
            totalExpense = 0.0,
            totalIncome = 0.0,
            netBalance = 0.0,
            dailyExpenses = emptyMap(),
            maxDailyExpense = 0.0,
            maxExpenseDay = null,
            categoryBreakdown = emptyList(),
            averageDailyExpense = 0.0
        )
    )

    // Total Overall Net Balance across all time
    val overallBalance: StateFlow<Double> = allTransactions.map { list ->
        var income = 0.0
        var expense = 0.0
        for (tx in list) {
            if (tx.type == TransactionType.INCOME.name) income += tx.amount
            else if (tx.type == TransactionType.EXPENSE.name) expense += tx.amount
        }
        income - expense
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        viewModelScope.launch {
            authRepository.initialize()
            repository.seedSampleDataIfEmpty()
            updateSyncStatusState()
        }

        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _cloudSyncStatus.update {
                    it.copy(
                        cloudAccountEmail = if (user != null) {
                            if (user.isGuest) "${user.name} (Offline)" else user.email
                        } else "muhmappanganroandi@gmail.com"
                    )
                }
            }
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.login(email, pass)
            _isAuthLoading.value = false
            result.onSuccess { user ->
                _snackbarMessage.emit("Selamat datang kembali, ${user.name}!")
            }.onFailure { err ->
                _authError.value = err.message ?: "Login gagal. Periksa email dan password."
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.register(name, email, pass)
            _isAuthLoading.value = false
            result.onSuccess { user ->
                _snackbarMessage.emit("Akun berhasil didaftarkan! Halo, ${user.name}.")
            }.onFailure { err ->
                _authError.value = err.message ?: "Pendaftaran gagal. Silakan coba lagi."
            }
        }
    }

    fun loginAsGuest(guestName: String = "Pengguna Tamu") {
        viewModelScope.launch {
            _authError.value = null
            val user = authRepository.loginAsGuest(guestName)
            _snackbarMessage.emit("Masuk sebagai ${user.name}. Mode offline aktif.")
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _snackbarMessage.emit("Anda telah keluar dari akun.")
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Transaction CRUD Operations ---
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        wallet: String,
        dateMillis: Long,
        note: String
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title,
                amount = amount,
                type = type.name,
                category = category,
                wallet = wallet,
                dateMillis = dateMillis,
                note = note,
                isSynced = false
            )
            repository.insertTransaction(entity)
            _snackbarMessage.emit("Transaksi '${title}' berhasil dicatat.")
            updateSyncStatusState()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _snackbarMessage.emit("Transaksi diperbarui.")
            updateSyncStatusState()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _snackbarMessage.emit("Transaksi '${transaction.title}' dihapus.")
            updateSyncStatusState()
        }
    }

    // --- Filtering & Navigation ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun setFilterDate(dateMillis: Long?) {
        _selectedFilterDateMillis.value = dateMillis
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun previousMonth() {
        var y = _selectedYear.value
        var m = _selectedMonth.value - 1
        if (m < 0) {
            m = 11
            y -= 1
        }
        _selectedYear.value = y
        _selectedMonth.value = m
    }

    fun nextMonth() {
        var y = _selectedYear.value
        var m = _selectedMonth.value + 1
        if (m > 11) {
            m = 0
            y += 1
        }
        _selectedYear.value = y
        _selectedMonth.value = m
    }

    // --- Cloud Sync Operations ---
    fun syncToCloud() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null && user.isGuest) {
                _snackbarMessage.emit("Mode Tamu: Buat akun untuk mengaktifkan sinkronisasi cloud.")
                return@launch
            }

            _cloudSyncStatus.update { it.copy(isSyncing = true) }
            val result = repository.syncWithCloud()
            _cloudSyncStatus.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTimestamp = repository.getLastSyncTime(),
                    cloudItemsCount = repository.getCloudBackupCount(),
                    lastSyncMessage = result.getOrNull()?.message ?: (result.exceptionOrNull()?.message ?: "Gagal menyinkronkan data")
                )
            }
            if (result.isSuccess) {
                _snackbarMessage.emit("☁️ Data berhasil disinkronkan ke Cloud!")
            } else {
                _snackbarMessage.emit("Gagal menyinkronkan: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _cloudSyncStatus.update { it.copy(isSyncing = true) }
            val result = repository.restoreFromCloud()
            _cloudSyncStatus.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTimestamp = repository.getLastSyncTime(),
                    cloudItemsCount = repository.getCloudBackupCount(),
                    lastSyncMessage = if (result.isSuccess) "Berhasil memulihkan ${result.getOrNull() ?: 0} transaksi dari Cloud" else (result.exceptionOrNull()?.message ?: "Gagal memulihkan")
                )
            }
            if (result.isSuccess) {
                _snackbarMessage.emit("Data berhasil dipulihkan dari Cloud!")
            } else {
                _snackbarMessage.emit("Pemulihan gagal: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        repository.setAutoSync(enabled)
        _cloudSyncStatus.update { it.copy(isAutoSyncEnabled = enabled) }
    }

    fun getCloudBackupJson(): String = repository.getCloudJson()
    fun getCloudBackupSize(): String = repository.getCloudBackupSize()

    private fun updateSyncStatusState() {
        _cloudSyncStatus.update {
            it.copy(
                lastSyncTimestamp = repository.getLastSyncTime(),
                cloudItemsCount = repository.getCloudBackupCount(),
                isAutoSyncEnabled = repository.isAutoSync()
            )
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val cloudService = CloudSyncService(application)
                    val repo = FinanceRepository(db.transactionDao(), cloudService)
                    val authRepo = AuthRepository(db.userDao(), application)
                    return FinanceViewModel(application, repo, authRepo) as T
                }
            }
    }
}
