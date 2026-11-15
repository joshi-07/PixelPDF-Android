package com.pixelpdf.app.data.repository

import android.content.Context
import android.net.Uri
import com.pixelpdf.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DraftManager(private val context: Context) {

    private val draftFile: File by lazy {
        File(context.filesDir, "draft_session.json")
    }

    suspend fun getDraft(): DraftSession? = withContext(Dispatchers.IO) {
        if (!draftFile.exists()) return@withContext null
        try {
            val jsonStr = draftFile.readText()
            val root = JSONObject(jsonStr)

            val imagesArray = root.getJSONArray("images")
            val images = mutableListOf<ImageItem>()
            for (i in 0 until imagesArray.length()) {
                val obj = imagesArray.getJSONObject(i)
                val uriStr = obj.optString("uri", "")
                val uri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null
                val filterName = obj.optString("filter", "ORIGINAL")
                val filter = try { ImageFilter.valueOf(filterName) } catch (_: Exception) { ImageFilter.ORIGINAL }

                images.add(
                    ImageItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        uri = uri,
                        name = obj.optString("name", "Image"),
                        width = obj.optInt("width", 1080),
                        height = obj.optInt("height", 1920),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        rotationDegrees = obj.optInt("rotationDegrees", 0),
                        filter = filter,
                        brightness = obj.optDouble("brightness", 0.0).toFloat(),
                        contrast = obj.optDouble("contrast", 1.0).toFloat(),
                        flipHorizontal = obj.optBoolean("flipHorizontal", false),
                        flipVertical = obj.optBoolean("flipVertical", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            if (images.isEmpty()) return@withContext null

            val settingsObj = root.optJSONObject("settings")
            val settings = if (settingsObj != null) {
                PdfSettings(
                    fileName = settingsObj.optString("fileName", "PixelPDF_Export"),
                    pageSize = try { PageSize.valueOf(settingsObj.optString("pageSize", "A4")) } catch (_: Exception) { PageSize.A4 },
                    orientation = try { PageOrientation.valueOf(settingsObj.optString("orientation", "AUTO")) } catch (_: Exception) { PageOrientation.AUTO },
                    margin = try { PageMargin.valueOf(settingsObj.optString("margin", "NONE")) } catch (_: Exception) { PageMargin.NONE },
                    scaleType = try { ImageScaleType.valueOf(settingsObj.optString("scaleType", "FIT_PAGE")) } catch (_: Exception) { ImageScaleType.FIT_PAGE },
                    quality = try { ImageQuality.valueOf(settingsObj.optString("quality", "HIGH")) } catch (_: Exception) { ImageQuality.HIGH },
                    includePageNumbers = settingsObj.optBoolean("includePageNumbers", false),
                    pageNumberStyle = try { PageNumberStyle.valueOf(settingsObj.optString("pageNumberStyle", "CURRENT_OF_TOTAL")) } catch (_: Exception) { PageNumberStyle.CURRENT_OF_TOTAL },
                    pageNumberPosition = try { PageNumberPosition.valueOf(settingsObj.optString("pageNumberPosition", "BOTTOM_CENTER")) } catch (_: Exception) { PageNumberPosition.BOTTOM_CENTER },
                    includeBorders = settingsObj.optBoolean("includeBorders", false),
                    backgroundColorHex = settingsObj.optLong("backgroundColorHex", 0xFFFFFFFF),
                    outputMode = try { OutputMode.valueOf(settingsObj.optString("outputMode", "SINGLE_PDF")) } catch (_: Exception) { OutputMode.SINGLE_PDF },
                    imagesPerPdf = settingsObj.optInt("imagesPerPdf", 5),

                    watermarkText = settingsObj.optString("watermarkText", ""),
                    watermarkOpacity = settingsObj.optDouble("watermarkOpacity", 0.25).toFloat(),
                    watermarkColorHex = settingsObj.optLong("watermarkColorHex", 0xFF888888),
                    watermarkAngle = settingsObj.optDouble("watermarkAngle", 45.0).toFloat(),
                    watermarkFontSize = settingsObj.optDouble("watermarkFontSize", 36.0).toFloat(),

                    headerText = settingsObj.optString("headerText", ""),
                    headerAlignment = try { TextAlignment.valueOf(settingsObj.optString("headerAlignment", "CENTER")) } catch (_: Exception) { TextAlignment.CENTER },
                    footerText = settingsObj.optString("footerText", ""),
                    headerFooterFontSize = settingsObj.optDouble("headerFooterFontSize", 9.0).toFloat(),

                    imageRoundedCorners = settingsObj.optDouble("imageRoundedCorners", 0.0).toFloat(),
                    insertBlankPages = settingsObj.optBoolean("insertBlankPages", false),
                    customDpi = settingsObj.optInt("customDpi", 150),
                    targetMaxFileSizeMb = settingsObj.optDouble("targetMaxFileSizeMb", 0.0).toFloat(),

                    documentTitle = settingsObj.optString("documentTitle", ""),
                    documentAuthor = settingsObj.optString("documentAuthor", ""),
                    documentSubject = settingsObj.optString("documentSubject", ""),
                    documentKeywords = settingsObj.optString("documentKeywords", "")
                )
            } else {
                PdfSettings()
            }

            DraftSession(
                images = images,
                settings = settings,
                timestamp = root.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveDraft(images: List<ImageItem>, settings: PdfSettings) = withContext(Dispatchers.IO) {
        if (images.isEmpty()) {
            clearDraft()
            return@withContext
        }
        try {
            val root = JSONObject()
            val imagesArray = JSONArray()
            images.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("uri", item.uri?.toString() ?: "")
                    put("name", item.name)
                    put("width", item.width)
                    put("height", item.height)
                    put("sizeBytes", item.sizeBytes)
                    put("rotationDegrees", item.rotationDegrees)
                    put("filter", item.filter.name)
                    put("brightness", item.brightness.toDouble())
                    put("contrast", item.contrast.toDouble())
                    put("flipHorizontal", item.flipHorizontal)
                    put("flipVertical", item.flipVertical)
                    put("timestamp", item.timestamp)
                }
                imagesArray.put(obj)
            }
            root.put("images", imagesArray)

            val settingsObj = JSONObject().apply {
                put("fileName", settings.fileName)
                put("pageSize", settings.pageSize.name)
                put("orientation", settings.orientation.name)
                put("margin", settings.margin.name)
                put("scaleType", settings.scaleType.name)
                put("quality", settings.quality.name)
                put("includePageNumbers", settings.includePageNumbers)
                put("pageNumberStyle", settings.pageNumberStyle.name)
                put("pageNumberPosition", settings.pageNumberPosition.name)
                put("includeBorders", settings.includeBorders)
                put("backgroundColorHex", settings.backgroundColorHex)
                put("outputMode", settings.outputMode.name)
                put("imagesPerPdf", settings.imagesPerPdf)

                put("watermarkText", settings.watermarkText)
                put("watermarkOpacity", settings.watermarkOpacity.toDouble())
                put("watermarkColorHex", settings.watermarkColorHex)
                put("watermarkAngle", settings.watermarkAngle.toDouble())
                put("watermarkFontSize", settings.watermarkFontSize.toDouble())

                put("headerText", settings.headerText)
                put("headerAlignment", settings.headerAlignment.name)
                put("footerText", settings.footerText)
                put("headerFooterFontSize", settings.headerFooterFontSize.toDouble())

                put("imageRoundedCorners", settings.imageRoundedCorners.toDouble())
                put("insertBlankPages", settings.insertBlankPages)
                put("customDpi", settings.customDpi)
                put("targetMaxFileSizeMb", settings.targetMaxFileSizeMb.toDouble())

                put("documentTitle", settings.documentTitle)
                put("documentAuthor", settings.documentAuthor)
                put("documentSubject", settings.documentSubject)
                put("documentKeywords", settings.documentKeywords)
            }
            root.put("settings", settingsObj)
            root.put("timestamp", System.currentTimeMillis())

            draftFile.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearDraft() = withContext(Dispatchers.IO) {
        if (draftFile.exists()) {
            draftFile.delete()
        }
    }
}
