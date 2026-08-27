package com.wallpapercropfixer.data.face

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.repository.FaceDetectionRepository
import com.wallpapercropfixer.domain.repository.ImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitFaceDetectionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageRepository: ImageRepository
) : FaceDetectionRepository {

    override suspend fun analyzeFaces(uri: String): SubjectAnalysis {
        val meta = imageRepository.readImageMeta(uri)
        val bitmap = imageRepository.decodeBitmapSampled(uri, maxWidth = 1080, maxHeight = 1080)

        val scaleX = meta.width.toFloat() / bitmap.width.toFloat()
        val scaleY = meta.height.toFloat() / bitmap.height.toFloat()

        // A detector is created per analysis and explicitly closed once the task
        // settles, so native model resources are bounded to the detection window.
        // The bitmap is recycled only after ML Kit has finished reading it.
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.10f)
                .build()
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val faces = suspendCancellableCoroutine<List<Face>> { cont ->
            detector.process(image)
                .addOnSuccessListener { result ->
                    // Release resources only after ML Kit has consumed the bitmap.
                    bitmap.recycle()
                    detector.close()
                    cont.resume(result)
                }
                .addOnFailureListener { e ->
                    bitmap.recycle()
                    detector.close()
                    cont.resumeWithException(e)
                }
        }

        val faceBounds = faces.map { face ->
            val rect = face.boundingBox
            FaceBounds(
                left = (rect.left.toFloat() * scaleX).coerceIn(0f, meta.width.toFloat()),
                top = (rect.top.toFloat() * scaleY).coerceIn(0f, meta.height.toFloat()),
                right = (rect.right.toFloat() * scaleX).coerceIn(0f, meta.width.toFloat()),
                bottom = (rect.bottom.toFloat() * scaleY).coerceIn(0f, meta.height.toFloat())
            )
        }

        val suggestedFocus = if (faceBounds.isNotEmpty()) {
            val unionLeft = faceBounds.minOf { it.left }
            val unionTop = faceBounds.minOf { it.top }
            val unionRight = faceBounds.maxOf { it.right }
            val unionBottom = faceBounds.maxOf { it.bottom }
            FocusPoint(
                xNormalized = (((unionLeft + unionRight) / 2f) / meta.width.toFloat()).coerceIn(0f, 1f),
                yNormalized = (((unionTop + unionBottom) / 2f) / meta.height.toFloat()).coerceIn(0f, 1f)
            )
        } else null

        return SubjectAnalysis(
            faces = faceBounds,
            suggestedFocusPoint = suggestedFocus
        )
    }
}