package com.example.data.model

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val cloudAccountEmail: String = "muhmappanganroandi@gmail.com",
    val cloudServerEndpoint: String = "cloud.catatuang.applet/v1/sync",
    val cloudItemsCount: Int = 0,
    val unsyncedCount: Int = 0,
    val lastSyncMessage: String = "Siap untuk disinkronkan",
    val isAutoSyncEnabled: Boolean = true
)
