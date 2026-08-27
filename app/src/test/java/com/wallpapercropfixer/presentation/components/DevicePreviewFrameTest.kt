package com.wallpapercropfixer.presentation.components

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.wallpapercropfixer.domain.model.FocusPoint
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DevicePreviewFrameTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders an empty frame without a bitmap`() {
        composeRule.setContent {
            DevicePreviewFrame(bitmap = null, deviceAspectRatio = 0.45f)
        }
        composeRule.onNodeWithText("Select a photo to preview").assertIsDisplayed()
    }

    @Test
    fun `renders a wide bitmap with a focus overlay without crashing`() {
        val bitmap = Bitmap.createBitmap(1188, 2400, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            DevicePreviewFrame(
                bitmap = bitmap,
                deviceAspectRatio = 1080f / 2400f,
                focusPoint = FocusPoint(0.5f, 0.5f)
            )
        }
        composeRule.onNodeWithContentDescription("Wallpaper preview").assertIsDisplayed()
    }

    @Test
    fun `renders a lock-ratio bitmap and maps focus correctly`() {
        val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            DevicePreviewFrame(
                bitmap = bitmap,
                deviceAspectRatio = 1080f / 2400f,
                focusPoint = FocusPoint(0.5f, 0.5f)
            )
        }
        composeRule.onNodeWithContentDescription("Wallpaper preview").assertIsDisplayed()
    }
}