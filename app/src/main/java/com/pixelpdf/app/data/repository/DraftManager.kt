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
                images.add(
                    ImageItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        uri = uri,
                        name = obj.optString("name", "Image"),
                        width = obj.optInt("width", 1080),
                        height = obj.optInt("height", 1920),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        rotationDegrees = obj.optInt("rotationDegrees", 0)
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
                    includeBorders = settingsObj.optBoolean("includeBorders", false),
                    backgroundColorHex = settingsObj.optLong("backgroundColorHex", 0xFFFFFFFF),
                    outputMode = try { OutputMode.valueOf(settingsObj.optString("outputMode", "SINGLE_PDF")) } catch (_: Exception) { OutputMode.SINGLE_PDF },
                    imagesPerPdf = settingsObj.optInt("imagesPerPdf", 5)
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
                put("includeBorders", settings.includeBorders)
                put("backgroundColorHex", settings.backgroundColorHex)
                put("outputMode", settings.outputMode.name)
                put("imagesPerPdf", settings.imagesPerPdf)
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
