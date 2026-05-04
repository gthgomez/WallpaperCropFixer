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

        val mimeType = options.outMimeType
            ?: context.contentResolver.getType(Uri.parse(uri))

        return SourceImageMeta(
            uri = uri,
            width = options.outWidth,
            height = options.outHeight,
            mimeType = mimeType
        )
    }

    override suspend fun decodeBitmapSampled(uri: String, maxWidth: Int, maxHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = computeSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val bitmap = openStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Cannot decode bitmap: $uri")

        return applyExifRotation(bitmap, uri)
    }

    // Opens an InputStream for either a file:// path (plain local file) or a
    // content:// URI. Using FileInputStream directly for file paths avoids
    // FileUriExposedException, which ContentResolver.openInputStream throws on
    // file:// schemes on Android 7+ (API 24+).
    private fun openStream(uri: String): InputStream? {
        val parsed = Uri.parse(uri)
        return if (parsed.scheme == "file" || parsed.scheme == null) {
            val path = parsed.path ?: uri
            runCatching { FileInputStream(File(path)) }.getOrNull()
        } else {
            context.contentResolver.openInputStream(parsed)
        }
    }

    private fun computeSampleSize(srcW: Int, srcH: Int, maxW: Int, maxH: Int): Int {
        var sampleSize = 1
        if (srcH > maxH || srcW > maxW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / sampleSize) > maxH && (halfW / sampleSize) > maxW) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    private fun applyExifRotation(bitmap: Bitmap, uri: String): Bitmap {
        val rotation = try {
            openStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val degrees = when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }
}
