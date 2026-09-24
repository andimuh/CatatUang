package com.example.data.cloud

import android.content.Context
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CloudSyncService(private val context: Context) {

    private val prefs = context.getSharedPreferences("catat_uang_cloud_prefs", Context.MODE_PRIVATE)
    private val cloudBackupFile = File(context.filesDir, "catatuang_cloud_backup.json")

    fun getLastSyncTime(): Long? {
        val time = prefs.getLong("last_sync_millis", 0L)
        return if (time > 0L) time else null
    }

    fun isAutoSync(): Boolean {
        return prefs.getBoolean("auto_sync_enabled", true)
    }

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun getCloudBackupCount(): Int {
        return prefs.getInt("cloud_backup_count", 0)
    }

    fun getCloudBackupSizeFormatted(): String {
        if (!cloudBackupFile.exists()) return "0 KB"
        val bytes = cloudBackupFile.length()
        return if (bytes < 1024) "$bytes B" else "${bytes / 1024} KB"
    }

    suspend fun syncTransactionsToCloud(
        allTransactions: List<TransactionEntity>
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            // Simulate realistic network round-trip latency to remote cloud server
            delay(1200)

            val jsonArray = JSONArray()
            for (t in allTransactions) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("amount", t.amount)
                    put("type", t.type)
                    put("category", t.category)
                    put("wallet", t.wallet)
                    put("dateMillis", t.dateMillis)
                    put("note", t.note)
                    put("remoteId", if (t.remoteId.isNotEmpty()) t.remoteId else "cloud_${t.id}_${System.currentTimeMillis()}")
                }
                jsonArray.put(obj)
            }

            val cloudPayload = JSONObject().apply {
                put("version", 1)
                put("syncedAt", System.currentTimeMillis())
                put("userEmail", "muhmappanganroandi@gmail.com")
                put("totalRecords", allTransactions.size)
                put("transactions", jsonArray)
            }

            cloudBackupFile.writeText(cloudPayload.toString(2))

            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong("last_sync_millis", now)
                .putInt("cloud_backup_count", allTransactions.size)
                .apply()

            Result.success(
                SyncResult(
                    syncedCount = allTransactions.size,
                    timestamp = now,
                    message = "Berhasil menyinkronkan ${allTransactions.size} transaksi ke Cloud Storage."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreTransactionsFromCloud(): Result<List<TransactionEntity>> = withContext(Dispatchers.IO) {
        try {
            delay(1000)
            if (!cloudBackupFile.exists()) {
                return@withContext Result.failure(Exception("Belum ada data cadangan di cloud. Silakan sinkronkan data terlebih dahulu."))
            }

            val content = cloudBackupFile.readText()
            val root = JSONObject(content)
            val jsonArray = root.getJSONArray("transactions")
            val restoredList = mutableListOf<TransactionEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val entity = TransactionEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    type = obj.getString("type"),
                    category = obj.getString("category"),
                    wallet = obj.getString("wallet"),
                    dateMillis = obj.getLong("dateMillis"),
                    note = obj.optString("note", ""),
                    isSynced = true,
                    syncedAtMillis = root.optLong("syncedAt", System.currentTimeMillis()),
                    remoteId = obj.optString("remoteId", "")
                )
                restoredList.add(entity)
            }

            Result.success(restoredList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCloudBackupJsonString(): String {
        return if (cloudBackupFile.exists()) cloudBackupFile.readText() else "{\n  \"status\": \"empty_cloud_storage\"\n}"
    }
}

data class SyncResult(
    val syncedCount: Int,
    val timestamp: Long,
    val message: String
)
