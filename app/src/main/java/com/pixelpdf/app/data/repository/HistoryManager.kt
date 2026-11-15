package com.pixelpdf.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.pixelpdf.app.data.model.GeneratedPdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class HistoryManager(private val context: Context) {

    private val historyFile: File by lazy {
        File(context.filesDir, "conversion_history.json")
    }

    suspend fun getHistory(): List<GeneratedPdfFile> = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) return@withContext emptyList()

        val list = mutableListOf<GeneratedPdfFile>()
        try {
            val jsonStr = historyFile.readText()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val filePath = obj.getString("filePath")
                val diskFile = File(filePath)
                if (diskFile.exists()) {
                    val uri = try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            diskFile
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(diskFile)
                    }

                    list.add(
                        GeneratedPdfFile(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            fileUri = uri,
                            filePath = filePath,
                            sizeBytes = obj.optLong("sizeBytes", diskFile.length()),
                            pageCount = obj.optInt("pageCount", 1),
                            timestamp = obj.optLong("timestamp", diskFile.lastModified()),
                            isFavorite = obj.optBoolean("isFavorite", false)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortedWith(compareByDescending<GeneratedPdfFile> { it.isFavorite }.thenByDescending { it.timestamp })
    }

    suspend fun addRecords(records: List<GeneratedPdfFile>) = withContext(Dispatchers.IO) {
        val existing = getHistory().toMutableList()
        existing.addAll(0, records)
        saveList(existing)
    }

    suspend fun toggleFavorite(recordId: String) = withContext(Dispatchers.IO) {
        val existing = getHistory().toMutableList()
        val index = existing.indexOfFirst { it.id == recordId }
        if (index != -1) {
            val current = existing[index]
            existing[index] = current.copy(isFavorite = !current.isFavorite)
            saveList(existing)
        }
    }

    suspend fun deleteRecord(recordId: String) = withContext(Dispatchers.IO) {
        val existing = getHistory().toMutableList()
        val item = existing.find { it.id == recordId }
        item?.let {
            try {
                File(it.filePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        existing.removeAll { it.id == recordId }
        saveList(existing)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val existing = getHistory()
        existing.forEach {
            try {
                File(it.filePath).delete()
            } catch (_: Exception) {}
        }
        if (historyFile.exists()) {
            historyFile.delete()
        }
    }

    suspend fun exportAllAsZip(): File? = withContext(Dispatchers.IO) {
        val files = getHistory()
        if (files.isEmpty()) return@withContext null

        val zipFile = File(context.cacheDir, "PixelPDF_Export_All_${System.currentTimeMillis()}.zip")
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val buffer = ByteArray(8192)
                files.forEach { pdfFile ->
                    val src = File(pdfFile.filePath)
                    if (src.exists()) {
                        zos.putNextEntry(ZipEntry(src.name))
                        FileInputStream(src).use { fis ->
                            var len: Int
                            while (fis.read(buffer).also { len = it } > 0) {
                                zos.write(buffer, 0, len)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveList(list: List<GeneratedPdfFile>) {
        val jsonArray = JSONArray()
        list.take(100).forEach { file ->
            val obj = JSONObject().apply {
                put("id", file.id)
                put("name", file.name)
                put("filePath", file.filePath)
                put("sizeBytes", file.sizeBytes)
                put("pageCount", file.pageCount)
                put("timestamp", file.timestamp)
                put("isFavorite", file.isFavorite)
            }
            jsonArray.put(obj)
        }
        historyFile.writeText(jsonArray.toString())
    }
}
