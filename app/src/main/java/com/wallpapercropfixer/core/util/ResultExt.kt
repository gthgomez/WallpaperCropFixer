package com.wallpapercropfixer.core.util

fun <T> Result<T>.onFailureMessage(action: (String) -> Unit): Result<T> =
    onFailure { action(it.message ?: "Unknown error") }
