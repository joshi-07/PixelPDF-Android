package com.pixelpdf.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.pixelpdf.app.data.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.InputStream
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

        // Check EXIF orientation
        var exifRotation = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exifRotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (exifRotation == 90 || exifRotation == 270) {
            val temp = width
            width = height
            height = temp
        }

        ImageItem(
            uri = uri,
            name = name,
            width = if (width > 0) width else 1080,
            height = if (height > 0) height else 1920,
            sizeBytes = size,
            rotationDegrees = 0
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

            // Apply custom rotation if user rotated the image
            val totalRotation = imageItem.rotationDegrees % 360
            if (totalRotation != 0) {
                val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(
                    decodedBitmap, 0, 0,
                    decodedBitmap.width, decodedBitmap.height,
                    matrix, true
                )
                if (rotatedBitmap != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                rotatedBitmap
            } else {
                decodedBitmap
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
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
}
