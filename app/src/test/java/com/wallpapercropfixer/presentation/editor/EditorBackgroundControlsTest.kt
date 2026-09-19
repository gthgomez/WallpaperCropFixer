package com.wallpapercropfixer.presentation.editor

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import com.wallpapercropfixer.domain.engine.CropStrategySelector
import com.wallpapercropfixer.domain.engine.FocusPointResolver
import com.wallpapercropfixer.domain.engine.TargetCanvasSpecFactory
import com.wallpapercropfixer.domain.engine.WallpaperCropEngineImpl
import com.wallpapercropfixer.domain.model.*
import com.wallpapercropfixer.presentation.theme.WallpaperCropFixerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorBackgroundControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `background controls follow current plan and stale render cannot enable them`() {
        val device = DeviceProfile("samsung", "SM-S938U", 35, 100, 200, 1f, 0.5f)
        val request = WallpaperRenderRequest(
            source = SourceImageMeta("photo", 200, 200, "image/png"),
            deviceProfile = device,
            behaviorProfile = WallpaperBehaviorProfile("g", "generic", 1f, 1f),
            target = WallpaperTarget.HOME,
            cropMode = CropMode.SAFE_FIT,
            backgroundFillMode = BackgroundFillMode.BLUR,
            manualFocusPoint = null,
            enableFaceAwareFocus = false
        )
        val plan = WallpaperCropEngineImpl(TargetCanvasSpecFactory(), FocusPointResolver(), CropStrategySelector())
            .buildPlan(request, null)
        val render = RenderedPreview(request, plan, Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888))
        val publication = PublishedPreview(1, WallpaperTarget.HOME, render)
        val state = mutableStateOf(EditorUiState(
            sourceImageMeta = request.source, deviceProfile = device,
            cropMode = CropMode.SAFE_FIT, publishedPreview = publication
        ))
        var selected: BackgroundFillMode? = null
        compose.setContent {
            WallpaperCropFixerTheme {
                EditorContent(state.value, EditorCallbacks(
                    onBack = {}, onReset = {}, onCropMode = {}, onTarget = {},
                    onFillMode = { selected = it }, onFaceAware = {}, onFocusTap = {},
                    onViewingLock = {}, onSave = {}, onApply = {}, onRetry = {}
                ))
            }
        }
        compose.onNodeWithText("Color").performScrollTo().assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(BackgroundFillMode.SOLID, selected) }
        compose.runOnIdle {
            state.value = state.value.copy(publishedPreview = publication.copy(home = render.copy(plan = plan.copy(usePadding = false))))
        }
        compose.onNodeWithText("Color").assertIsNotEnabled()
        compose.runOnIdle {
            state.value = state.value.copy(publishedPreview = null, retainedPreview = publication, isRendering = true)
        }
        compose.onNodeWithText("Color").assertIsNotEnabled()
    }
}
