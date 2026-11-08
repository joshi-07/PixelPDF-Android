package com.pixelpdf.app.data.model

import android.net.Uri

data class GeneratedPdfFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val fileUri: Uri,
    val filePath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }
}

data class ConversionProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentImageName: String = "",
    val percentage: Float = 0f,
    val isCancelled: Boolean = false
)

sealed class ConversionState {
    object Idle : ConversionState()
    data class Processing(val progress: ConversionProgress) : ConversionState()
    data class Success(val files: List<GeneratedPdfFile>) : ConversionState()
    data class Error(val message: String) : ConversionState()
}
