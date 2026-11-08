package com.pixelpdf.app.data.repository

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.pixelpdf.app.data.model.ConversionProgress
import com.pixelpdf.app.data.model.GeneratedPdfFile
import com.pixelpdf.app.data.model.ImageItem
import com.pixelpdf.app.data.model.ImageQuality
import com.pixelpdf.app.data.model.ImageScaleType
import com.pixelpdf.app.data.model.OutputMode
import com.pixelpdf.app.data.model.PageOrientation
import com.pixelpdf.app.data.model.PageSize
import com.pixelpdf.app.data.model.PdfSettings
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
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
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
                val reqW = maxOf(100, (pageWidth * qualityMultiplier).toInt())
                val reqH = maxOf(100, (pageHeight * qualityMultiplier).toInt())

                val bitmap = imageProcessor.decodeSampledBitmap(
                    imageItem = imageItem,
                    reqWidth = reqW,
                    reqHeight = reqH
                ) ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 3. Draw Background
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // 4. Calculate Margins and Content Box
                val margin = settings.margin.marginPt.toFloat()
                val contentLeft = margin
                val contentTop = margin
                val contentRight = pageWidth - margin
                val bottomMarginAdjust = if (settings.includePageNumbers) margin + 18f else margin
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
                        // Fill available content area
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

                // 6. Draw Bitmap
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                canvas.drawBitmap(bitmap, srcRect, destRect, paint)

                // 7. Optional Border around image
                if (settings.includeBorders) {
                    canvas.drawRect(destRect, borderPaint)
                }

                // 8. Optional Page Numbering
                if (settings.includePageNumbers) {
                    val pageText = "${index + 1} / ${images.size}"
                    canvas.drawText(
                        pageText,
                        pageWidth / 2f,
                        pageHeight - 8f,
                        textPaint
                    )
                }

                pdfDocument.finishPage(page)

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
                pageCount = images.size
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
