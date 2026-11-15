package com.pixelpdf.app.data.model

enum class ImageFilter(val displayName: String) {
    ORIGINAL("Original"),
    DOCUMENT_CLEAN("Doc Clean"),
    GRAYSCALE("Black & White"),
    HIGH_CONTRAST("High Contrast"),
    SEPIA("Sepia"),
    INVERT("Negative / Invert"),
    VINTAGE("Vintage"),
    COOL("Cool Tone"),
    WARM("Warm Tone")
}

data class ImageExifInfo(
    val dimensions: String = "",
    val fileSizeFormatted: String = "",
    val cameraModel: String = "",
    val dateTime: String = "",
    val orientationDegrees: Int = 0
)
