package com.wallpapercropfixer.core.util

import android.util.Log

object Logger {
    private const val TAG = "WCF"

    fun d(msg: String) = Log.d(TAG, msg)
    fun e(msg: String, t: Throwable? = null) = if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg)
}
