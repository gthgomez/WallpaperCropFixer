package com.wallpapercropfixer.data.face

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.repository.FaceDetectionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitFaceDetectionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FaceDetectionRepository {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.10f)
            .build()
    )

    override suspend fun analyzeFaces(uri: String): SubjectAnalysis {
        // InputImage.fromFilePath uses ContentResolver internally, which throws
        // FileUriExposedException for file:// URIs on API 24+. Decode the bitmap
        // ourselves via FileInputStream and hand it to fromBitmap instead.
        val bitmap = openBitmap(uri)
            ?: error("Cannot open image for face detection: $uri")

        val image = InputImage.fromBitmap(bitmap, 0)
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        val faces = suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        val faceBounds = faces.map { face ->
            val rect = face.boundingBox
            FaceBounds(
                left = rect.left.toFloat(),
                top = rect.top.toFloat(),
                right = rect.right.toFloat(),
                bottom = rect.bottom.toFloat()
            )
        }

        val suggestedFocus = if (faceBounds.isNotEmpty()) {
            val unionLeft = faceBounds.minOf { it.left }
            val unionTop = faceBounds.minOf { it.top }
            val unionRight = faceBounds.maxOf { it.right }
            val unionBottom = faceBounds.maxOf { it.bottom }
            FocusPoint(
                xNormalized = ((unionLeft + unionRight) / 2f) / imageWidth,
                yNormalized = ((unionTop + unionBottom) / 2f) / imageHeight
            )
        } else null

        return SubjectAnalysis(
            faces = faceBounds,
            suggestedFocusPoint = suggestedFocus
        )
    }

    private fun openBitmap(uri: String) = runCatching {
        val parsed = Uri.parse(uri)
        val stream = if (parsed.scheme == "file" || parsed.scheme == null) {
            FileInputStream(File(parsed.path ?: uri))
        } else {
            context.contentResolver.openInputStream(parsed)!!
        }
        stream.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
