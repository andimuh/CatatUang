package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.UserDao
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository(
    private val userDao: UserDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("catat_uang_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        seedDefaultUserIfEmpty()
        restoreSession()
    }

    private suspend fun seedDefaultUserIfEmpty() {
        if (userDao.getUserCount() == 0) {
            val defaultUser = UserEntity(
                name = "Muh Mappanganro Andi",
                email = "muhmappanganroandi@gmail.com",
                password = "password123",
                isGuest = false
            )
            userDao.insertUser(defaultUser)
        }
    }

    private suspend fun restoreSession() {
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        if (isGuest) {
            val guestName = prefs.getString(KEY_GUEST_NAME, "Tamu") ?: "Tamu"
            _currentUser.value = UserEntity(
                id = -1,
                name = guestName,
                email = "tamu@catatuang.local",
                password = "",
                isGuest = true
            )
            return
        }

        val userId = prefs.getLong(KEY_USER_ID, -1L)
        if (userId != -1L) {
            val user = userDao.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
            }
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email dan kata sandi wajib diisi."))
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("Email belum terdaftar. Silakan daftar akun baru."))

        if (user.password != trimmedPass) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi salah. Silakan coba lagi."))
        }

        // Save session
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()

        _currentUser.value = user
        Result.success(user)
    }

    suspend fun register(name: String, email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Nama lengkap tidak boleh kosong."))
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Format email tidak valid."))
        }
        if (trimmedPass.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Kata sandi minimal 6 karakter."))
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Email sudah terdaftar. Silakan login."))
        }

        val newUser = UserEntity(
            name = trimmedName,
            email = trimmedEmail,
            password = trimmedPass,
            isGuest = false
        )

        val newId = userDao.insertUser(newUser)
        val createdUser = newUser.copy(id = newId)

        // Save session
        prefs.edit()
            .putLong(KEY_USER_ID, newId)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()

        _currentUser.value = createdUser
        Result.success(createdUser)
    }

    fun loginAsGuest(guestName: String = "Pengguna Tamu"): UserEntity {
        val guestUser = UserEntity(
            id = -1,
            name = guestName.ifBlank { "Pengguna Tamu" },
            email = "tamu@catatuang.local",
            password = "",
            isGuest = true
        )

        prefs.edit()
            .putLong(KEY_USER_ID, -1L)
            .putBoolean(KEY_IS_GUEST, true)
            .putString(KEY_GUEST_NAME, guestUser.name)
            .apply()

        _currentUser.value = guestUser
        return guestUser
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .putBoolean(KEY_IS_GUEST, false)
            .remove(KEY_GUEST_NAME)
            .apply()

        _currentUser.value = null
    }

    companion object {
        private const val KEY_USER_ID = "logged_in_user_id"
        private const val KEY_IS_GUEST = "is_guest_mode"
        private const val KEY_GUEST_NAME = "guest_display_name"
    }
}
