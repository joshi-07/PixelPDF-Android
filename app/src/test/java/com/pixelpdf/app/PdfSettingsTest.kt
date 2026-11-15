package com.pixelpdf.app

import com.pixelpdf.app.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSettingsTest {

    @Test
    fun testA4PortraitDimensions() {
        val (width, height) = PageSize.A4.getDimensions(isLandscape = false)
        assertEquals(595, width)
        assertEquals(842, height)
    }

    @Test
    fun testA4LandscapeDimensions() {
        val (width, height) = PageSize.A4.getDimensions(isLandscape = true)
        assertEquals(842, width)
        assertEquals(595, height)
    }

    @Test
    fun testLetterDimensions() {
        val (width, height) = PageSize.LETTER.getDimensions(isLandscape = false)
        assertEquals(612, width)
        assertEquals(792, height)
    }

    @Test
    fun testPageMargins() {
        assertEquals(0, PageMargin.NONE.marginPt)
        assertEquals(18, PageMargin.SMALL.marginPt)
        assertEquals(36, PageMargin.MEDIUM.marginPt)
        assertEquals(54, PageMargin.LARGE.marginPt)
    }

    @Test
    fun testImageItemRotationLogic() {
        val item = ImageItem(
            uri = null,
            name = "test.jpg",
            width = 1080,
            height = 1920,
            rotationDegrees = 90
        )
        // With 90 deg rotation, display width and height swap
        assertEquals(1920, item.displayWidth)
        assertEquals(1080, item.displayHeight)
        assertTrue(item.isLandscape)
    }

    @Test
    fun testDefaultSettings() {
        val settings = PdfSettings()
        assertEquals("PixelPDF_Export", settings.fileName)
        assertEquals(PageSize.A4, settings.pageSize)
        assertEquals(PageMargin.NONE, settings.margin)
        assertEquals(PageNumberStyle.CURRENT_OF_TOTAL, settings.pageNumberStyle)
        assertEquals(PageNumberPosition.BOTTOM_CENTER, settings.pageNumberPosition)
        assertEquals(150, settings.customDpi)
        assertFalse(settings.insertBlankPages)
    }

    @Test
    fun testWatermarkAndMetadataSettings() {
        val settings = PdfSettings(
            watermarkText = "CONFIDENTIAL",
            watermarkOpacity = 0.35f,
            documentTitle = "Q4 Report",
            documentAuthor = "PixelPDF",
            insertBlankPages = true,
            imageRoundedCorners = 12f
        )
        assertEquals("CONFIDENTIAL", settings.watermarkText)
        assertEquals(0.35f, settings.watermarkOpacity, 0.001f)
        assertEquals("Q4 Report", settings.documentTitle)
        assertEquals("PixelPDF", settings.documentAuthor)
        assertTrue(settings.insertBlankPages)
        assertEquals(12f, settings.imageRoundedCorners, 0.001f)
    }

    @Test
    fun testImageFilterEnums() {
        assertEquals("Original", ImageFilter.ORIGINAL.displayName)
        assertEquals("Doc Clean", ImageFilter.DOCUMENT_CLEAN.displayName)
        assertEquals("Black & White", ImageFilter.GRAYSCALE.displayName)
        assertEquals("High Contrast", ImageFilter.HIGH_CONTRAST.displayName)
        assertEquals("Sepia", ImageFilter.SEPIA.displayName)
        assertEquals(9, ImageFilter.values().size)
    }

    @Test
    fun testAppThemeColors() {
        val themes = AppThemeColor.values()
        assertEquals(4, themes.size)
        assertTrue(themes.contains(AppThemeColor.SAPPHIRE_BLUE))
        assertTrue(themes.contains(AppThemeColor.EMERALD_GREEN))
        assertTrue(themes.contains(AppThemeColor.SUNSET_ORANGE))
        assertTrue(themes.contains(AppThemeColor.ROYAL_VIOLET))
    }
}
