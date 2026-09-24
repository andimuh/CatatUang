package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.TransactionEntity
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleTransaction = TransactionEntity(
      id = 1,
      title = "Makan Siang Nasi Padang",
      amount = 45000.0,
      type = "EXPENSE",
      category = "Makanan & Minuman",
      wallet = "CASH",
      dateMillis = System.currentTimeMillis(),
      note = "Catatan keuangan harian",
      isSynced = true
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TransactionItemCard(
          transaction = sampleTransaction,
          onEdit = {},
          onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
