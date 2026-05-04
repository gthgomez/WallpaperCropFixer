package com.wallpapercropfixer.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileNameFactory {
    fun wallpaperFileName(tag: String = "wcf"): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${tag}_$timestamp"
    }
}
