package com.dmitrylab.traffictracker.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.getDefault(), "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.getDefault(), "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.getDefault(), "%.1f KB", safe / kb)
        else -> "$safe B"
    }
}

internal fun formatTime(timestampMs: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}
