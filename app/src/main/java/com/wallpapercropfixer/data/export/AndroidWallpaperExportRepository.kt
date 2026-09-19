package com.wallpapercropfixer.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wallpapercropfixer.domain.repository.ExportDestination
import com.wallpapercropfixer.domain.repository.ExportResult
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
    ): ExportResult {
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
                ?: throw IllegalStateException("MediaStore insert returned null")

            try {
                val ok = context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(format, quality.coerceIn(0, 100), out)
                } ?: false

                if (!ok) {
                    context.contentResolver.delete(uri, null, null)
                    throw IllegalStateException("Bitmap compression failed for $fullName")
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                val updatedRows = context.contentResolver.update(uri, values, null, null)
                if (updatedRows <= 0) {
                    context.contentResolver.delete(uri, null, null)
                    throw IllegalStateException("Failed to finalize MediaStore image for $fullName")
                }

                ExportResult(
                    destination = ExportDestination.MEDIA_STORE,
                    pathOrUri = uri.toString(),
                    displayName = fullName
                )
            } catch (e: Exception) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                throw e
            }
        } else {
            // API 28 and below: no storage permission is declared, so write to app-specific
            // external pictures storage (visible via a Files app, not the gallery).
            // Intentionally does not request WRITE_EXTERNAL_STORAGE.
            val externalPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val baseDir = if (externalPictures != null) {
                File(externalPictures, "WallpaperCropFixer")
            } else {
                File(context.filesDir, "WallpaperCropFixer")
            }
            baseDir.mkdirs()

            // Filenames only carry second-resolution timestamps, so a re-save within
            // the same second would silently overwrite the previous export. Shift to
            // -1, -2, ... before the extension whenever the target already exists.
            var file = File(baseDir, fullName)
            if (file.exists()) {
                val dot = fullName.lastIndexOf('.')
                val base = if (dot > 0) fullName.substring(0, dot) else fullName
                val extension = if (dot > 0) fullName.substring(dot) else ""
                var index = 1
                while (file.exists()) {
                    file = File(baseDir, "$base-$index$extension")
                    index++
                }
            }

            try {
                file.outputStream().use { out ->
                    check(bitmap.compress(format, quality.coerceIn(0, 100), out)) {
                        "Bitmap compression failed for $file"
                    }
                }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            ExportResult(
                destination = if (externalPictures != null) {
                    ExportDestination.APP_EXTERNAL_FILES
                } else {
                    ExportDestination.APP_INTERNAL_FILES
                },
                pathOrUri = file.absolutePath,
                displayName = file.name
            )
        }
    }
}