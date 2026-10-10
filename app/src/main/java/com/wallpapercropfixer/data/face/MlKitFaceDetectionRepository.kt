package com.wallpapercropfixer.data.face

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
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

/**
 * On-device subject analysis: ML Kit Face Detection plus, optionally, ML Kit
 * Subject Segmentation (people/pets/objects).
 *
 * Segmentation is an unbundled model delivered by Google Play services. When it is
 * not downloaded, unsupported, or fails, analysis degrades to faces, then to the
 * image center — a missing model must never fail the edit. All processing is local.
 */
class MlKitFaceDetectionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageRepository: ImageRepository
) : FaceDetectionRepository {

    override suspend fun analyzeFaces(uri: String): SubjectAnalysis {
        val meta = imageRepository.readImageMeta(uri)
        val bitmap = imageRepository.decodeBitmapSampled(uri, maxWidth = 1080, maxHeight = 1080)

        try {
            val scaleX = meta.width.toFloat() / bitmap.width.toFloat()
            val scaleY = meta.height.toFloat() / bitmap.height.toFloat()

            val faces = runCatching { detectFaces(bitmap) }.getOrDefault(emptyList())
            // A missing segmentation model returns no results; that must not break
            // the flow, so failures collapse to an empty subject list.
            val bitmapSubjectBounds = runCatching { detectSubjects(bitmap) }.getOrDefault(emptyList())

            val faceBounds = faces.map { face ->
                val rect = face.boundingBox
                FaceBounds(
                    left = (rect.left.toFloat() * scaleX).coerceIn(0f, meta.width.toFloat()),
                    top = (rect.top.toFloat() * scaleY).coerceIn(0f, meta.height.toFloat()),
                    right = (rect.right.toFloat() * scaleX).coerceIn(0f, meta.width.toFloat()),
                    bottom = (rect.bottom.toFloat() * scaleY).coerceIn(0f, meta.height.toFloat())
                )
            }
            val subjectBounds = bitmapSubjectBounds.map { b ->
                FaceBounds(
                    left = (b.left * scaleX).coerceIn(0f, meta.width.toFloat()),
                    top = (b.top * scaleY).coerceIn(0f, meta.height.toFloat()),
                    right = (b.right * scaleX).coerceIn(0f, meta.width.toFloat()),
                    bottom = (b.bottom * scaleY).coerceIn(0f, meta.height.toFloat())
                )
            }

            val anchors = subjectBounds.ifEmpty { faceBounds }
            val suggestedFocus = if (anchors.isNotEmpty()) {
                val centerX = (anchors.minOf { it.left } + anchors.maxOf { it.right }) / 2f
                val centerY = (anchors.minOf { it.top } + anchors.maxOf { it.bottom }) / 2f
                FocusPoint(
                    xNormalized = (centerX / meta.width.toFloat()).coerceIn(0f, 1f),
                    yNormalized = (centerY / meta.height.toFloat()).coerceIn(0f, 1f)
                )
            } else {
                null
            }

            return SubjectAnalysis(
                faces = faceBounds,
                suggestedFocusPoint = suggestedFocus,
                subjects = subjectBounds
            )
        } finally {
            // Both detectors have finished reading the bitmap by now.
            bitmap.recycle()
        }
    }

    private suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.10f)
                .build()
        )
        val task = detector.process(InputImage.fromBitmap(bitmap, 0))
        return suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                runCatching { detector.close() }
                if (cont.isActive) cont.resume(result)
            }
            task.addOnFailureListener { e ->
                runCatching { detector.close() }
                if (cont.isActive) cont.resumeWithException(e)
            }
            // No GMS Task.cancel(); closing the detector fails any pending work, which
            // settles the task and routes cleanup through the listeners above.
            cont.invokeOnCancellation { runCatching { detector.close() } }
        }
    }

    /**
     * Returns the union bounding box of the segmented foreground, in the decoded
     * bitmap's pixel space, or an empty list when no subject is found.
     */
    private suspend fun detectSubjects(bitmap: Bitmap): List<FaceBounds> {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        val task = segmenter.process(InputImage.fromBitmap(bitmap, 0))
        return suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                runCatching { segmenter.close() }
                // The foreground bitmap is owned by the SDK; do not recycle it.
                val bounds = result.foregroundBitmap?.let { boundingBoxOfOpaque(it) }
                if (cont.isActive) cont.resume(if (bounds != null) listOf(bounds) else emptyList())
            }
            task.addOnFailureListener {
                runCatching { segmenter.close() }
                if (cont.isActive) cont.resume(emptyList())
            }
            cont.invokeOnCancellation { runCatching { segmenter.close() } }
        }
    }

    /** Union bounding box of pixels with alpha above [ALPHA_THRESHOLD], or null. */
    private fun boundingBoxOfOpaque(bitmap: Bitmap): FaceBounds? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((pixels[row + x] ushr 24) > ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return null
        return FaceBounds(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat())
    }

    private companion object {
        const val ALPHA_THRESHOLD = 8
    }
}
