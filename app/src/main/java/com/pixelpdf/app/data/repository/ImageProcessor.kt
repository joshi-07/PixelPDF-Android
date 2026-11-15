package com.pixelpdf.app.data.repository

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.pixelpdf.app.data.model.ImageExifInfo
import com.pixelpdf.app.data.model.ImageFilter
import com.pixelpdf.app.data.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import kotlin.math.max

class ImageProcessor(private val context: Context) {

    suspend fun getImageItemFromUri(uri: Uri): ImageItem = withContext(Dispatchers.IO) {
        var name = "Image_${System.currentTimeMillis()}"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: name
                }
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        // Decode bounds to get dimensions without loading bitmap into memory
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var width = options.outWidth
        var height = options.outHeight

        // Check EXIF orientation and metadata
        var exifRotation = 0
        var cameraModel = "Standard Image"
        var dateTime = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exifRotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "Camera Capture"
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (exifRotation == 90 || exifRotation == 270) {
            val temp = width
            width = height
            height = temp
        }

        val formattedSize = formatBytes(size)
        val exifInfo = ImageExifInfo(
            dimensions = "${if (width > 0) width else 1080} × ${if (height > 0) height else 1920}",
            fileSizeFormatted = formattedSize,
            cameraModel = cameraModel,
            dateTime = dateTime,
            orientationDegrees = exifRotation
        )

        ImageItem(
            uri = uri,
            name = name,
            width = if (width > 0) width else 1080,
            height = if (height > 0) height else 1920,
            sizeBytes = size,
            rotationDegrees = 0,
            exifInfo = exifInfo
        )
    }

    suspend fun getImageItemsFromUris(uris: List<Uri>): List<ImageItem> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            async(Dispatchers.IO) {
                getImageItemFromUri(uri)
            }
        }.awaitAll()
    }

    suspend fun decodeSampledBitmap(
        imageItem: ImageItem,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = imageItem.uri ?: return@withContext null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Conserves 50% RAM compared to ARGB_8888

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return@withContext null

            // 1. Apply custom rotation & flips (Features 7, 8)
            var currentBitmap = decodedBitmap
            val totalRotation = imageItem.rotationDegrees % 360
            val needsMatrix = totalRotation != 0 || imageItem.flipHorizontal || imageItem.flipVertical

            if (needsMatrix) {
                val matrix = Matrix()
                if (totalRotation != 0) {
                    matrix.postRotate(totalRotation.toFloat())
                }
                val scaleX = if (imageItem.flipHorizontal) -1f else 1f
                val scaleY = if (imageItem.flipVertical) -1f else 1f
                if (scaleX != 1f || scaleY != 1f) {
                    matrix.postScale(scaleX, scaleY)
                }

                val transformedBitmap = Bitmap.createBitmap(
                    currentBitmap, 0, 0,
                    currentBitmap.width, currentBitmap.height,
                    matrix, true
                )
                if (transformedBitmap != currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = transformedBitmap
                }
            }

            // 2. Apply Filters, Brightness & Contrast (Features 1-6)
            val hasFilter = imageItem.filter != ImageFilter.ORIGINAL
            val hasAdjustment = imageItem.brightness != 0f || imageItem.contrast != 1.0f

            if (hasFilter || hasAdjustment) {
                val filteredBitmap = applyColorFilters(currentBitmap, imageItem)
                if (filteredBitmap != currentBitmap) {
                    currentBitmap.recycle()
                    currentBitmap = filteredBitmap
                }
            }

            currentBitmap
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun applyColorFilters(source: Bitmap, imageItem: ImageItem): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val finalMatrix = ColorMatrix()

        // Filter Matrix
        when (imageItem.filter) {
            ImageFilter.GRAYSCALE -> {
                finalMatrix.setSaturation(0f)
            }
            ImageFilter.DOCUMENT_CLEAN -> {
                // High contrast with brightened highlights for crisp document pages
                val clean = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, 15f,
                    0f, 1.4f, 0f, 0f, 15f,
                    0f, 0f, 1.4f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
                val gray = ColorMatrix().apply { setSaturation(0.05f) }
                clean.postConcat(gray)
                finalMatrix.postConcat(clean)
            }
            ImageFilter.HIGH_CONTRAST -> {
                finalMatrix.set(floatArrayOf(
                    1.8f, 0f, 0f, 0f, -60f,
                    0f, 1.8f, 0f, 0f, -60f,
                    0f, 0f, 1.8f, 0f, -60f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            ImageFilter.SEPIA -> {
                val sepia = ColorMatrix()
                sepia.setScale(1.0f, 0.92f, 0.78f, 1.0f)
                val gray = ColorMatrix().apply { setSaturation(0f) }
                gray.postConcat(sepia)
                finalMatrix.postConcat(gray)
            }
            ImageFilter.INVERT -> {
                finalMatrix.set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            ImageFilter.VINTAGE -> {
                val vintage = ColorMatrix()
                vintage.setScale(0.9f, 0.85f, 0.75f, 1.0f)
                finalMatrix.postConcat(vintage)
            }
            ImageFilter.COOL -> {
                val cool = ColorMatrix()
                cool.setScale(0.85f, 0.95f, 1.15f, 1.0f)
                finalMatrix.postConcat(cool)
            }
            ImageFilter.WARM -> {
                val warm = ColorMatrix()
                warm.setScale(1.15f, 1.02f, 0.85f, 1.0f)
                finalMatrix.postConcat(warm)
            }
            ImageFilter.ORIGINAL -> Unit
        }

        // Brightness & Contrast Adjustment
        if (imageItem.brightness != 0f || imageItem.contrast != 1.0f) {
            val c = imageItem.contrast
            val b = imageItem.brightness * 255f
            val adjMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c, 0f, b,
                0f, 0f, 0f, 1f, 0f
            ))
            finalMatrix.postConcat(adjMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(finalMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
