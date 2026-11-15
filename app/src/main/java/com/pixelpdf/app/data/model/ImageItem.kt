package com.pixelpdf.app.data.model

import android.net.Uri

data class ImageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val name: String,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val rotationDegrees: Int = 0,
    val filter: ImageFilter = ImageFilter.ORIGINAL,
    val brightness: Float = 0f, // -0.5f to 0.5f
    val contrast: Float = 1.0f,  // 0.5f to 2.0f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val isSelected: Boolean = false,
    val exifInfo: ImageExifInfo? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayWidth: Int
        get() = if (rotationDegrees % 180 == 0) width else height

    val displayHeight: Int
        get() = if (rotationDegrees % 180 == 0) height else width

    val isLandscape: Boolean
        get() = displayWidth > displayHeight
}
