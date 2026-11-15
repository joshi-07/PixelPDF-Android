package com.pixelpdf.app.data.model

enum class PageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    A3("A3 (297 x 420 mm)", 842, 1191),
    A5("A5 (148 x 210 mm)", 420, 595),
    LETTER("Letter (8.5 x 11 in)", 612, 792),
    LEGAL("Legal (8.5 x 14 in)", 612, 1008),
    ORIGINAL_IMAGE("Original Image Size", 0, 0);

    fun getDimensions(isLandscape: Boolean): Pair<Int, Int> {
        return if (isLandscape && this != ORIGINAL_IMAGE) {
            Pair(maxOf(widthPt, heightPt), minOf(widthPt, heightPt))
        } else if (this != ORIGINAL_IMAGE) {
            Pair(minOf(widthPt, heightPt), maxOf(widthPt, heightPt))
        } else {
            Pair(0, 0)
        }
    }
}

enum class PageOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Automatic (Match Image)")
}

enum class PageMargin(val displayName: String, val marginPt: Int) {
    NONE("No Margins", 0),
    SMALL("Small (18 pt)", 18),
    MEDIUM("Medium (36 pt)", 36),
    LARGE("Large (54 pt)", 54)
}

enum class ImageScaleType(val displayName: String) {
    FIT_PAGE("Fit to Page (Keep Aspect Ratio)"),
    FILL_PAGE("Fill Page (Crop if Needed)"),
    MAINTAIN_ORIGINAL("Maintain Aspect Ratio (No Stretching)")
}

enum class ImageQuality(val displayName: String, val compressionQuality: Int) {
    LOW("Small File Size (50%)", 50),
    MEDIUM("Balanced (75%)", 75),
    HIGH("High Quality (90%)", 90),
    MAXIMUM("Maximum Clarity (100%)", 100)
}

enum class OutputMode(val displayName: String) {
    SINGLE_PDF("Combine all images into one PDF"),
    SEPARATE_PDFS("Create a separate PDF for each image"),
    SPLIT_BY_COUNT("Split into multiple PDFs by image count")
}

enum class PageNumberStyle(val displayName: String) {
    CURRENT_OF_TOTAL("1 / N"),
    PAGE_X_OF_Y("Page 1 of N"),
    JUST_NUMBER("1"),
    DASHES("- 1 -")
}

enum class PageNumberPosition(val displayName: String) {
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    TOP_RIGHT("Top Right")
}

enum class TextAlignment(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

data class PdfSettings(
    val fileName: String = "PixelPDF_Export",
    val pageSize: PageSize = PageSize.A4,
    val orientation: PageOrientation = PageOrientation.AUTO,
    val margin: PageMargin = PageMargin.NONE,
    val scaleType: ImageScaleType = ImageScaleType.FIT_PAGE,
    val quality: ImageQuality = ImageQuality.HIGH,
    val includePageNumbers: Boolean = false,
    val pageNumberStyle: PageNumberStyle = PageNumberStyle.CURRENT_OF_TOTAL,
    val pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val includeBorders: Boolean = false,
    val backgroundColorHex: Long = 0xFFFFFFFF,
    val outputMode: OutputMode = OutputMode.SINGLE_PDF,
    val imagesPerPdf: Int = 5,

    // Watermark Settings (Features 11-14)
    val watermarkText: String = "",
    val watermarkOpacity: Float = 0.25f,
    val watermarkColorHex: Long = 0xFF888888,
    val watermarkAngle: Float = 45f,
    val watermarkFontSize: Float = 36f,

    // Header & Footer Settings (Features 18-22)
    val headerText: String = "",
    val headerAlignment: TextAlignment = TextAlignment.CENTER,
    val footerText: String = "",
    val headerFooterFontSize: Float = 9f,

    // Styling & Duplex Formatting (Features 23-25)
    val imageRoundedCorners: Float = 0f,
    val insertBlankPages: Boolean = false,
    val customDpi: Int = 150,
    val targetMaxFileSizeMb: Float = 0f,

    // PDF Metadata (Features 15-17)
    val documentTitle: String = "",
    val documentAuthor: String = "",
    val documentSubject: String = "",
    val documentKeywords: String = ""
)
