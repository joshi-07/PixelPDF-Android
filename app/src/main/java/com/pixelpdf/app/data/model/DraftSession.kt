package com.pixelpdf.app.data.model

data class DraftSession(
    val images: List<ImageItem>,
    val settings: PdfSettings,
    val timestamp: Long = System.currentTimeMillis()
)
