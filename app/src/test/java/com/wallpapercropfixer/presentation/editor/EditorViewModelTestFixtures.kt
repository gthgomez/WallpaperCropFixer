package com.wallpapercropfixer.presentation.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.data.behavior.KnownWallpaperProfiles
import com.wallpapercropfixer.domain.engine.CropStrategySelector
import com.wallpapercropfixer.domain.engine.FocusPointResolver
import com.wallpapercropfixer.domain.engine.TargetCanvasSpecFactory
import com.wallpapercropfixer.domain.engine.WallpaperCropEngineImpl
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.DeviceProfile
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.model.UserSettings
import com.wallpapercropfixer.domain.model.WallpaperBehaviorProfile
import com.wallpapercropfixer.domain.model.WallpaperRenderPlan
import com.wallpapercropfixer.domain.model.WallpaperRenderRequest
import com.wallpapercropfixer.domain.model.WallpaperTarget
import com.wallpapercropfixer.domain.repository.DeviceProfileRepository
import com.wallpapercropfixer.domain.repository.ExportDestination
import com.wallpapercropfixer.domain.repository.ExportResult
import com.wallpapercropfixer.domain.repository.FaceDetectionRepository
import com.wallpapercropfixer.domain.repository.ImageRepository
import com.wallpapercropfixer.domain.repository.SettingsRepository
import com.wallpapercropfixer.domain.repository.WallpaperApplyRepository
import com.wallpapercropfixer.domain.repository.WallpaperBehaviorRepository
import com.wallpapercropfixer.domain.repository.WallpaperExportRepository
import com.wallpapercropfixer.domain.usecase.AnalyzeSubjectUseCase
import com.wallpapercropfixer.domain.usecase.ApplyWallpaperUseCase
import com.wallpapercropfixer.domain.usecase.BuildWallpaperRenderPlanUseCase
import com.wallpapercropfixer.domain.usecase.ExportWallpaperUseCase
import com.wallpapercropfixer.domain.usecase.GetCurrentDeviceProfileUseCase
import com.wallpapercropfixer.domain.usecase.RenderWallpaperBitmapUseCase
import com.wallpapercropfixer.domain.usecase.ResolveWallpaperBehaviorUseCase
import com.wallpapercropfixer.rendering.WallpaperBitmapRenderer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FakeImageRepository : ImageRepository {
    var meta: SourceImageMeta = SourceImageMeta("file:///none", 4000, 3000, "image/jpeg")
    override suspend fun readImageMeta(uri: String): SourceImageMeta = meta.copy(uri = uri)
    override suspend fun decodeBitmapSampled(uri: String, maxWidth: Int, maxHeight: Int): Bitmap =
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
}

class FakeSettingsRepository : SettingsRepository {
    val settings = MutableStateFlow(UserSettings())
    override fun observeSettings(): Flow<UserSettings> = settings
    override suspend fun updateSettings(settings: UserSettings) { this.settings.value = settings }
}

class FakeDeviceProfileRepository : DeviceProfileRepository {
    override suspend fun getCurrentDeviceProfile(): DeviceProfile =
        DeviceProfile("test", "phone", 35, 1080, 2400, 2.75f, 1080f / 2400f)
}

class FakeBehaviorRepository : WallpaperBehaviorRepository {
    override suspend fun resolveBehaviorProfile(deviceProfile: DeviceProfile): WallpaperBehaviorProfile =
        KnownWallpaperProfiles.generic
}

class FakeFaceDetectionRepository : FaceDetectionRepository {
    val analyses = ConcurrentHashMap<String, SubjectAnalysis>()
    val gates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    val started = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    var completedCount = 0

    override suspend fun analyzeFaces(uri: String): SubjectAnalysis {
        started.getOrPut(uri) { CompletableDeferred() }.complete(Unit)
        val gate = gates[uri]
        if (gate != null) withContext(NonCancellable) { gate.await() }
        completedCount++
        return analyses[uri] ?: SubjectAnalysis(emptyList(), null)
    }
}

/** Controllable renderer whose gates make cancellation and completion deterministic. */
class FakeWallpaperBitmapRenderer : WallpaperBitmapRenderer {
    var renderCalls = 0
    val started = ConcurrentHashMap<CropMode, CompletableDeferred<Unit>>()
    val invocationCount = ConcurrentHashMap<CropMode, Int>()
    val gates = ConcurrentHashMap<CropMode, CompletableDeferred<Unit>>()
    val nonCancellableModes: MutableSet<CropMode> = ConcurrentHashMap.newKeySet()

    override suspend fun render(request: WallpaperRenderRequest, plan: WallpaperRenderPlan): Bitmap {
        renderCalls++
        started.getOrPut(request.cropMode) { CompletableDeferred() }.complete(Unit)
        invocationCount[request.cropMode] = (invocationCount[request.cropMode] ?: 0) + 1
        gates[request.cropMode]?.let { gate ->
            if (request.cropMode in nonCancellableModes) {
                withContext(NonCancellable) { gate.await() }
            } else {
                gate.await()
            }
        }
        return Bitmap.createBitmap(request.cropMode.ordinal + 1, 10, Bitmap.Config.ARGB_8888)
    }
}

class FakeExportRepository(
    var result: ExportResult = ExportResult(ExportDestination.MEDIA_STORE, "content://media/1", "wcf.jpg")
) : WallpaperExportRepository {
    val exported = mutableListOf<Pair<Bitmap, String>>()
    var started: CompletableDeferred<Unit>? = null
    var gate: CompletableDeferred<Unit>? = null
    var failWith: Throwable? = null

    override suspend fun exportBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): ExportResult {
        started?.complete(Unit)
        gate?.await()
        failWith?.let { throw it }
        exported.add(bitmap to fileName)
        return result
    }
}

class FakeApplyRepository : WallpaperApplyRepository {
    val applied = mutableListOf<Pair<Bitmap, WallpaperTarget>>()
    var started: CompletableDeferred<Unit>? = null
    var gate: CompletableDeferred<Unit>? = null
    var failWith: Throwable? = null

    override suspend fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget): Result<Unit> {
        started?.complete(Unit)
        gate?.await()
        applied.add(bitmap to target)
        failWith?.let { return Result.failure(it) }
        return Result.success(Unit)
    }
}

fun buildEditorViewModel(
    imageRepository: ImageRepository = FakeImageRepository(),
    settingsRepository: SettingsRepository = FakeSettingsRepository(),
    deviceProfileRepository: DeviceProfileRepository = FakeDeviceProfileRepository(),
    behaviorRepository: WallpaperBehaviorRepository = FakeBehaviorRepository(),
    faceDetectionRepository: FaceDetectionRepository = FakeFaceDetectionRepository(),
    renderer: WallpaperBitmapRenderer = FakeWallpaperBitmapRenderer(),
    exportRepository: WallpaperExportRepository = FakeExportRepository(),
    applyRepository: WallpaperApplyRepository = FakeApplyRepository(),
    savedStateHandle: SavedStateHandle = SavedStateHandle()
): EditorViewModel = EditorViewModel(
    imageRepository = imageRepository,
    settingsRepository = settingsRepository,
    getDeviceProfile = GetCurrentDeviceProfileUseCase(deviceProfileRepository),
    resolveBehavior = ResolveWallpaperBehaviorUseCase(behaviorRepository),
    analyzeSubject = AnalyzeSubjectUseCase(faceDetectionRepository),
    buildRenderPlan = BuildWallpaperRenderPlanUseCase(
        WallpaperCropEngineImpl(
            TargetCanvasSpecFactory(),
            FocusPointResolver(),
            CropStrategySelector()
        )
    ),
    renderBitmap = RenderWallpaperBitmapUseCase(renderer),
    exportWallpaper = ExportWallpaperUseCase(exportRepository),
    applyWallpaper = ApplyWallpaperUseCase(applyRepository),
    savedStateHandle = savedStateHandle
)
