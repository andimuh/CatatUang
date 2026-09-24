package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CatatUang", appName)
  }

  @Test
  fun `test auth repository registration and guest login`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val authRepo = AuthRepository(db.userDao(), context)
    authRepo.initialize()

    // Test guest login
    val guestUser = authRepo.loginAsGuest("Tamu Penguji")
    assertTrue(guestUser.isGuest)
    assertEquals("Tamu Penguji", guestUser.name)
    assertEquals(guestUser.name, authRepo.currentUser.value?.name)

    // Test register new user
    val registerResult = authRepo.register("User Baru", "baru@test.com", "password123")
    assertTrue(registerResult.isSuccess)
    val registeredUser = registerResult.getOrThrow()
    assertFalse(registeredUser.isGuest)
    assertEquals("baru@test.com", registeredUser.email)

    // Test login
    val loginResult = authRepo.login("baru@test.com", "password123")
    assertTrue(loginResult.isSuccess)
    assertEquals(registeredUser.id, loginResult.getOrThrow().id)

    // Test logout
    authRepo.logout()
    assertNull(authRepo.currentUser.value)
  }
}
