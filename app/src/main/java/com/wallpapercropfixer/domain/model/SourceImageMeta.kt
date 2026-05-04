package com.wallpapercropfixer.domain.model

data class SourceImageMeta(
    val uri: String,
    val width: Int,
    val height: Int,
    val mimeType: String?
)
