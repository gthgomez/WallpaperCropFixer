package com.wallpapercropfixer.domain.repository

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.model.SourceImageMeta

interface ImageRepository {
    suspend fun readImageMeta(uri: String): SourceImageMeta
    suspend fun decodeBitmapSampled(uri: String, maxWidth: Int, maxHeight: Int): Bitmap
}
