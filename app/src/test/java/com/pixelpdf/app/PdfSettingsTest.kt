package com.pixelpdf.app

import com.pixelpdf.app.data.model.ImageItem
import com.pixelpdf.app.data.model.PageMargin
import com.pixelpdf.app.data.model.PageSize
import com.pixelpdf.app.data.model.PdfSettings
import org.junit.Assert.assertEquals
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
    }
}
