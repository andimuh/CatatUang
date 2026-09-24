package com.example.data.repository

import com.example.data.cloud.CloudSyncService
import com.example.data.cloud.SyncResult
import com.example.data.local.TransactionDao
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val cloudSyncService: CloudSyncService
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val unsyncedCount: Flow<Int> = transactionDao.getUnsyncedCount()
    val totalCount: Flow<Int> = transactionDao.getTransactionCount()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // If auto sync is on, we can trigger sync or mark pending
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction.copy(isSynced = false))
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun syncWithCloud(): Result<SyncResult> {
        val all = transactionDao.getAllTransactions().first()
        val result = cloudSyncService.syncTransactionsToCloud(all)
        if (result.isSuccess) {
            val syncResult = result.getOrThrow()
            transactionDao.markAllAsSynced(syncResult.timestamp)
        }
        return result
    }

    suspend fun restoreFromCloud(): Result<Int> {
        val result = cloudSyncService.restoreTransactionsFromCloud()
        return if (result.isSuccess) {
            val restored = result.getOrThrow()
            if (restored.isNotEmpty()) {
                transactionDao.clearAll()
                transactionDao.insertTransactions(restored)
            }
            Result.success(restored.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Gagal memulihkan data dari cloud"))
        }
    }

    fun getLastSyncTime(): Long? = cloudSyncService.getLastSyncTime()
    fun isAutoSync(): Boolean = cloudSyncService.isAutoSync()
    fun setAutoSync(enabled: Boolean) = cloudSyncService.setAutoSync(enabled)
    fun getCloudBackupCount(): Int = cloudSyncService.getCloudBackupCount()
    fun getCloudBackupSize(): String = cloudSyncService.getCloudBackupSizeFormatted()
    fun getCloudJson(): String = cloudSyncService.getCloudBackupJsonString()

    suspend fun seedSampleDataIfEmpty() {
        val current = transactionDao.getAllTransactions().first()
        if (current.isNotEmpty()) return

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val samples = mutableListOf<TransactionEntity>()

        fun makeDate(day: Int, hour: Int, minute: Int): Long {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 28))
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis
        }

        // Gaji awal bulan
        samples.add(
            TransactionEntity(
                title = "Gaji Bulanan PT Maju Jaya",
                amount = 8500000.0,
                type = "INCOME",
                category = "Gaji Pokok",
                wallet = "BANK",
                dateMillis = makeDate(1, 9, 0),
                note = "Transfer payroll rekening utama",
                isSynced = true,
                syncedAtMillis = System.currentTimeMillis()
            )
        )

        // Bonus proyek freelance
        samples.add(
            TransactionEntity(
                title = "Pembayaran Proyek Desain Web",
                amount = 2250000.0,
                type = "INCOME",
                category = "Freelance / Proyek",
                wallet = "BANK",
                dateMillis = makeDate(5.coerceAtMost(currentDay), 14, 30),
                note = "Klien startup e-commerce",
                isSynced = true,
                syncedAtMillis = System.currentTimeMillis()
            )
        )

        // Expense samples throughout the month
        val expenseData = listOf(
            Triple("Belanja Mingguan Supermarket", 485000.0, "Belanja & Kebutuhan" to "BANK"),
            Triple("Listrik PLN & Tagihan Wifi", 650000.0, "Tagihan & Listrik" to "E_WALLET"),
            Triple("Bensin & Tol Mobil", 250000.0, "Transportasi" to "E_WALLET"),
            Triple("Makan Siang Soto Betawi & Es Teh", 42000.0, "Makanan & Minuman" to "CASH"),
            Triple("Kopi Susu Senja & Croissant", 38000.0, "Makanan & Minuman" to "E_WALLET"),
            Triple("Nonton Bioskop & Popcorn", 110000.0, "Hiburan & Rekreasi" to "E_WALLET"),
            Triple("Beli Vitamin & Obat Apotek", 135000.0, "Kesehatan & Medis" to "CASH"),
            Triple("Langganan Spotify & Netflix", 185000.0, "Tagihan & Listrik" to "BANK"),
            Triple("Makan Malam Bersama Keluarga", 210000.0, "Makanan & Minuman" to "BANK"),
            Triple("Ongkos Transportasi MRT & Ojek", 35000.0, "Transportasi" to "E_WALLET"),
            Triple("Buku Referensi & Kursus Online", 275000.0, "Pendidikan" to "BANK"),
            Triple("Belanja Keperluan Dapur & Rumah", 320000.0, "Keluarga & Rumah" to "CASH"),
            Triple("Makan Siang Nasi Padang Komplit", 45000.0, "Makanan & Minuman" to "CASH"),
            Triple("Servis Rutin Kendaraan", 380000.0, "Transportasi" to "BANK"),
            Triple("Makan Malam Ayam Geprek Spesial", 35000.0, "Makanan & Minuman" to "E_WALLET")
        )

        var dayStep = 2
        for (item in expenseData) {
            val assignedDay = dayStep.coerceAtMost(currentDay)
            samples.add(
                TransactionEntity(
                    title = item.first,
                    amount = item.second,
                    type = "EXPENSE",
                    category = item.third.first,
                    wallet = item.third.second,
                    dateMillis = makeDate(assignedDay, (11 + (assignedDay % 8)), (assignedDay * 7) % 60),
                    note = "Catatan keuangan harian",
                    isSynced = true,
                    syncedAtMillis = System.currentTimeMillis()
                )
            )
            dayStep = if (dayStep + 2 > currentDay) 1 else dayStep + 2
        }

        // Add 2 unsynced transactions for today so user can see cloud sync feature in action!
        samples.add(
            TransactionEntity(
                title = "Sarapan Bubur Ayam & Teh Manis",
                amount = 25000.0,
                type = "EXPENSE",
                category = "Makanan & Minuman",
                wallet = "CASH",
                dateMillis = makeDate(currentDay, 7, 45),
                note = "Sarapan pagi sebelum kerja",
                isSynced = false,
                syncedAtMillis = null
            )
        )
        samples.add(
            TransactionEntity(
                title = "Topup E-Money Transport",
                amount = 50000.0,
                type = "EXPENSE",
                category = "Transportasi",
                wallet = "E_WALLET",
                dateMillis = makeDate(currentDay, 8, 15),
                note = "Kartu komuter",
                isSynced = false,
                syncedAtMillis = null
            )
        )

        transactionDao.insertTransactions(samples)
        cloudSyncService.syncTransactionsToCloud(samples)
    }
}
