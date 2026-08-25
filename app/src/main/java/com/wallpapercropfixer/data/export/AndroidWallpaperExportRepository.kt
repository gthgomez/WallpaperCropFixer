package com.wallpapercropfixer.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wallpapercropfixer.domain.repository.WallpaperExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class AndroidWallpaperExportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WallpaperExportRepository {

    override suspend fun exportBitmap(
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): String {
        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val fullName = "$fileName.$ext"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: write via MediaStore to shared Pictures/WallpaperCropFixer — no storage permission needed
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fullName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/WallpaperCropFixer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert returned null")

            try {
                val ok = context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(format, quality, out)
                } ?: false

                if (!ok) {
                    context.contentResolver.delete(uri, null, null)
                    error("Bitmap compression failed for $fullName")
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                val updatedRows = context.contentResolver.update(uri, values, null, null)
                if (updatedRows <= 0) {
                    context.contentResolver.delete(uri, null, null)
                    error("Failed to finalize MediaStore image for $fullName")
                }

                uri.toString()
            } catch (e: Exception) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw e
            }
        } else {
            // API 28 and below: write to public Pictures if possible, otherwise app-specific external pictures directory
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val baseDir = if (publicDir != null && (publicDir.canWrite() || publicDir.mkdirs())) {
                File(publicDir, "WallpaperCropFixer")
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: File(context.filesDir, "WallpaperCropFixer")
            }
            baseDir.mkdirs()

            val file = File(baseDir, fullName)
            file.outputStream().use { out ->
                val ok = bitmap.compress(format, quality, out)
                if (!ok) {
                    file.delete()
                    error("Bitmap compression failed for $file")
                }
            }
            file.absolutePath
        }
    }
}
