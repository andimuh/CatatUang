package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String,          // "EXPENSE" or "INCOME"
    val category: String,
    val wallet: String,        // "CASH", "BANK", "E_WALLET"
    val dateMillis: Long,      // timestamp of the transaction
    val note: String = "",
    val isSynced: Boolean = false,
    val syncedAtMillis: Long? = null,
    val remoteId: String = ""
)
