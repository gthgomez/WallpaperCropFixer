package com.wallpapercropfixer.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.wallpapercropfixer.domain.model.SourceImageMeta
import com.wallpapercropfixer.domain.repository.ImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

class AndroidImageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ImageRepository {

    override suspend fun readImageMeta(uri: String): SourceImageMeta {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        val streamOpened = openStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
            true
        } ?: false

        if (!streamOpened) error("Cannot open image: $uri")
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            error("Cannot decode image bounds: $uri")
        }

        val orientation = getExifOrientation(uri)
        val isSwapped = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE

        val width = if (isSwapped) options.outHeight else options.outWidth
        val height = if (isSwapped) options.outWidth else options.outHeight

        val mimeType = options.outMimeType
            ?: context.contentResolver.getType(Uri.parse(uri))

        return SourceImageMeta(
            uri = uri,
            width = width,
            height = height,
            mimeType = mimeType
        )
    }

    override suspend fun decodeBitmapSampled(uri: String, maxWidth: Int, maxHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val orientation = getExifOrientation(uri)
        val isSwapped = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE

        val rawMaxW = if (isSwapped) maxHeight else maxWidth
        val rawMaxH = if (isSwapped) maxWidth else maxHeight

        options.inSampleSize = computeSampleSize(options.outWidth, options.outHeight, rawMaxW, rawMaxH)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val bitmap = openStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Cannot decode bitmap: $uri")

        return applyExifRotation(bitmap, orientation)
    }

    // Opens an InputStream for either a file path / file:// URI or a content:// URI.
    // Checking direct File existence first avoids Uri.parse() parsing Windows drive
    // letters (e.g. C:) as custom URI schemes, and avoids FileUriExposedException.
    private fun openStream(uri: String): InputStream? {
        val directFile = File(uri)
        if (directFile.exists() && directFile.isFile) {
            return runCatching { FileInputStream(directFile) }.getOrNull()
        }

        val parsed = Uri.parse(uri)
        return if (parsed.scheme == "file" || parsed.scheme == null || parsed.scheme?.length == 1) {
            val path = parsed.path ?: uri
            runCatching { FileInputStream(File(path)) }.getOrNull()
        } else {
            runCatching { context.contentResolver.openInputStream(parsed) }.getOrNull()
        }
    }

    private fun computeSampleSize(srcW: Int, srcH: Int, maxW: Int, maxH: Int): Int {
        var sampleSize = 1
        if (srcH > maxH || srcW > maxW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / sampleSize) >= maxH || (halfW / sampleSize) >= maxW) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    private fun getExifOrientation(uri: String): Int {
        return try {
            openStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }
}
