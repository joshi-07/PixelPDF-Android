package com.pixelpdf.app.data.model

import android.net.Uri

data class ImageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val name: String,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val rotationDegrees: Int = 0
) {
    val displayWidth: Int
        get() = if (rotationDegrees % 180 == 0) width else height

    val displayHeight: Int
        get() = if (rotationDegrees % 180 == 0) height else width

    val isLandscape: Boolean
        get() = displayWidth > displayHeight
}
