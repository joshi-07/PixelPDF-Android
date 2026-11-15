package com.pixelpdf.app.data.repository

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pixelpdf.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

class PdfEngine(
    private val context: Context,
    private val imageProcessor: ImageProcessor
) {

    private val outputDir: File by lazy {
        val dir = File(context.filesDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun convertImagesToPdf(
        images: List<ImageItem>,
        settings: PdfSettings,
        onProgress: (ConversionProgress) -> Unit
    ): List<GeneratedPdfFile> = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext emptyList()

        when (settings.outputMode) {
            OutputMode.SINGLE_PDF -> {
                val file = buildPdfBatch(
                    images = images,
                    settings = settings,
                    batchIndex = 0,
                    totalBatches = 1,
                    onProgress = onProgress
                )
                listOfNotNull(file)
            }
            OutputMode.SEPARATE_PDFS -> {
                val results = mutableListOf<GeneratedPdfFile>()
                images.forEachIndexed { index, imageItem ->
                    coroutineContext.ensureActive()
                    val singleSettings = settings.copy(
                        fileName = "${settings.fileName}_${index + 1}"
                    )
                    val file = buildPdfBatch(
                        images = listOf(imageItem),
                        settings = singleSettings,
                        batchIndex = index,
                        totalBatches = images.size,
                        onProgress = { p ->
                            val overallCurrent = index + 1
                            val overallPercentage = overallCurrent.toFloat() / images.size
                            onProgress(
                                p.copy(
                                    current = overallCurrent,
                                    total = images.size,
                                    percentage = overallPercentage
                                )
                            )
                        }
                    )
                    file?.let { results.add(it) }
                }
                results
            }
            OutputMode.SPLIT_BY_COUNT -> {
                val chunkSize = maxOf(1, settings.imagesPerPdf)
                val chunks = images.chunked(chunkSize)
                val results = mutableListOf<GeneratedPdfFile>()

                var processedSoFar = 0
                chunks.forEachIndexed { batchIndex, chunkImages ->
                    coroutineContext.ensureActive()
                    val splitSettings = settings.copy(
                        fileName = "${settings.fileName}_part${batchIndex + 1}"
                    )
                    val file = buildPdfBatch(
                        images = chunkImages,
                        settings = splitSettings,
                        batchIndex = batchIndex,
                        totalBatches = chunks.size,
                        onProgress = { p ->
                            val currentTotalProcessed = processedSoFar + p.current
                            val overallPercentage = currentTotalProcessed.toFloat() / images.size
                            onProgress(
                                p.copy(
                                    current = currentTotalProcessed,
                                    total = images.size,
                                    percentage = overallPercentage
                                )
                            )
                        }
                    )
                    processedSoFar += chunkImages.size
                    file?.let { results.add(it) }
                }
                results
            }
        }
    }

    private suspend fun buildPdfBatch(
        images: List<ImageItem>,
        settings: PdfSettings,
        batchIndex: Int,
        totalBatches: Int,
        onProgress: (ConversionProgress) -> Unit
    ): GeneratedPdfFile? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val bgPaint = Paint().apply {
            color = settings.backgroundColorHex.toInt()
            style = Paint.Style.FILL
        }

        try {
            var currentPageNumber = 1
            images.forEachIndexed { index, imageItem ->
                coroutineContext.ensureActive()

                onProgress(
                    ConversionProgress(
                        current = index + 1,
                        total = images.size,
                        currentImageName = imageItem.name,
                        percentage = (index.toFloat() / images.size)
                    )
                )

                // 1. Calculate Page Dimensions
                val isImageLandscape = imageItem.isLandscape
                val isPageLandscape = when (settings.orientation) {
                    PageOrientation.PORTRAIT -> false
                    PageOrientation.LANDSCAPE -> true
                    PageOrientation.AUTO -> isImageLandscape
                }

                val (pageWidth, pageHeight) = if (settings.pageSize == PageSize.ORIGINAL_IMAGE) {
                    val w = imageItem.displayWidth
                    val h = imageItem.displayHeight
                    Pair(if (w > 0) w else 595, if (h > 0) h else 842)
                } else {
                    settings.pageSize.getDimensions(isPageLandscape)
                }

                // 2. Decode the sampled bitmap for this page with dynamic quality scaling
                val qualityMultiplier = when (settings.quality) {
                    ImageQuality.MAXIMUM -> 2.5f
                    ImageQuality.HIGH -> 2.0f
                    ImageQuality.MEDIUM -> 1.5f
                    ImageQuality.LOW -> 1.0f
                }
                val dpiMultiplier = settings.customDpi / 150f
                val combinedScale = qualityMultiplier * dpiMultiplier
                val reqW = maxOf(100, (pageWidth * combinedScale).toInt())
                val reqH = maxOf(100, (pageHeight * combinedScale).toInt())

                val bitmap = imageProcessor.decodeSampledBitmap(
                    imageItem = imageItem,
                    reqWidth = reqW,
                    reqHeight = reqH
                ) ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 3. Draw Background
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // 4. Calculate Margins and Content Box
                val margin = settings.margin.marginPt.toFloat()
                val topMarginAdjust = if (settings.headerText.isNotEmpty()) margin + 22f else margin
                val bottomMarginAdjust = if (settings.includePageNumbers || settings.footerText.isNotEmpty()) margin + 22f else margin

                val contentLeft = margin
                val contentTop = topMarginAdjust
                val contentRight = pageWidth - margin
                val contentBottom = pageHeight - bottomMarginAdjust

                val availableWidth = maxOf(1f, contentRight - contentLeft)
                val availableHeight = maxOf(1f, contentBottom - contentTop)

                // 5. Calculate Image Placement Rect
                val imgWidth = bitmap.width.toFloat()
                val imgHeight = bitmap.height.toFloat()

                val destRect: RectF = when (settings.scaleType) {
                    ImageScaleType.FIT_PAGE -> {
                        val scale = minOf(availableWidth / imgWidth, availableHeight / imgHeight)
                        val drawW = imgWidth * scale
                        val drawH = imgHeight * scale
                        val left = contentLeft + (availableWidth - drawW) / 2f
                        val top = contentTop + (availableHeight - drawH) / 2f
                        RectF(left, top, left + drawW, top + drawH)
                    }
                    ImageScaleType.FILL_PAGE -> {
                        RectF(contentLeft, contentTop, contentRight, contentBottom)
                    }
                    ImageScaleType.MAINTAIN_ORIGINAL -> {
                        val drawW = minOf(imgWidth, availableWidth)
                        val drawH = minOf(imgHeight, availableHeight)
                        val left = contentLeft + (availableWidth - drawW) / 2f
                        val top = contentTop + (availableHeight - drawH) / 2f
                        RectF(left, top, left + drawW, top + drawH)
                    }
                }

                // 6. Draw Bitmap (with optional Rounded Corners) (Feature 23)
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                if (settings.imageRoundedCorners > 0f) {
                    val radius = settings.imageRoundedCorners
                    val path = Path().apply {
                        addRoundRect(destRect, radius, radius, Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(path)
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                }

                // 7. Optional Border around image
                if (settings.includeBorders) {
                    if (settings.imageRoundedCorners > 0f) {
                        canvas.drawRoundRect(destRect, settings.imageRoundedCorners, settings.imageRoundedCorners, borderPaint)
                    } else {
                        canvas.drawRect(destRect, borderPaint)
                    }
                }

                // 8. Custom Page Header (Features 18, 19)
                if (settings.headerText.isNotEmpty()) {
                    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = settings.headerFooterFontSize
                        textAlign = when (settings.headerAlignment) {
                            TextAlignment.LEFT -> Paint.Align.LEFT
                            TextAlignment.CENTER -> Paint.Align.CENTER
                            TextAlignment.RIGHT -> Paint.Align.RIGHT
                        }
                    }
                    val headerX = when (settings.headerAlignment) {
                        TextAlignment.LEFT -> margin.coerceAtLeast(24f)
                        TextAlignment.CENTER -> pageWidth / 2f
                        TextAlignment.RIGHT -> (pageWidth - margin).coerceAtMost(pageWidth - 24f)
                    }
                    canvas.drawText(settings.headerText, headerX, 18f, headerPaint)
                }

                // 9. Custom Page Footer (Feature 20)
                if (settings.footerText.isNotEmpty()) {
                    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.GRAY
                        textSize = settings.headerFooterFontSize
                        textAlign = Paint.Align.LEFT
                    }
                    canvas.drawText(settings.footerText, margin.coerceAtLeast(24f), pageHeight - 8f, footerPaint)
                }

                // 10. Configurable Page Numbering (Features 21, 22)
                if (settings.includePageNumbers) {
                    val pageText = when (settings.pageNumberStyle) {
                        PageNumberStyle.CURRENT_OF_TOTAL -> "${index + 1} / ${images.size}"
                        PageNumberStyle.PAGE_X_OF_Y -> "Page ${index + 1} of ${images.size}"
                        PageNumberStyle.JUST_NUMBER -> "${index + 1}"
                        PageNumberStyle.DASHES -> "- ${index + 1} -"
                    }
                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = settings.headerFooterFontSize
                        textAlign = when (settings.pageNumberPosition) {
                            PageNumberPosition.BOTTOM_CENTER -> Paint.Align.CENTER
                            PageNumberPosition.BOTTOM_RIGHT, PageNumberPosition.TOP_RIGHT -> Paint.Align.RIGHT
                            PageNumberPosition.BOTTOM_LEFT -> Paint.Align.LEFT
                        }
                    }
                    val (numX, numY) = when (settings.pageNumberPosition) {
                        PageNumberPosition.BOTTOM_CENTER -> Pair(pageWidth / 2f, pageHeight - 8f)
                        PageNumberPosition.BOTTOM_RIGHT -> Pair(pageWidth - 24f, pageHeight - 8f)
                        PageNumberPosition.BOTTOM_LEFT -> Pair(24f, pageHeight - 8f)
                        PageNumberPosition.TOP_RIGHT -> Pair(pageWidth - 24f, 18f)
                    }
                    canvas.drawText(pageText, numX, numY, numPaint)
                }

                // 11. Watermark Support (Features 11-14)
                if (settings.watermarkText.isNotEmpty()) {
                    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = settings.watermarkColorHex.toInt()
                        alpha = (settings.watermarkOpacity * 255).toInt().coerceIn(0, 255)
                        textSize = settings.watermarkFontSize
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.save()
                    canvas.rotate(settings.watermarkAngle, pageWidth / 2f, pageHeight / 2f)
                    canvas.drawText(settings.watermarkText, pageWidth / 2f, pageHeight / 2f, wmPaint)
                    canvas.restore()
                }

                pdfDocument.finishPage(page)
                currentPageNumber++

                // 12. Duplex Blank Page Spacer (Feature 25)
                if (settings.insertBlankPages) {
                    val blankPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    val blankPage = pdfDocument.startPage(blankPageInfo)
                    val blankCanvas = blankPage.canvas
                    blankCanvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
                    pdfDocument.finishPage(blankPage)
                    currentPageNumber++
                }

                // Immediately recycle to preserve RAM
                bitmap.recycle()
            }

            coroutineContext.ensureActive()

            // Construct safe filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val rawName = settings.fileName.trim().ifEmpty { "PixelPDF" }
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val finalFileName = "${rawName}_$timestamp.pdf"
            val outputFile = File(outputDir, finalFileName)

            FileOutputStream(outputFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            GeneratedPdfFile(
                name = finalFileName,
                fileUri = fileUri,
                filePath = outputFile.absolutePath,
                sizeBytes = outputFile.length(),
                pageCount = if (settings.insertBlankPages) images.size * 2 else images.size
            )
        } catch (e: Throwable) {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            e.printStackTrace()
            null
        }
    }
}
