package com.pixelpdf.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpdf.app.data.model.*
import com.pixelpdf.app.data.repository.DraftManager
import com.pixelpdf.app.data.repository.HistoryManager
import com.pixelpdf.app.data.repository.ImageProcessor
import com.pixelpdf.app.data.repository.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

sealed class Screen {
    object Home : Screen()
    object Editor : Screen()
    object Settings : Screen()
    object Preview : Screen()
    object History : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val imageProcessor = ImageProcessor(application)
    private val pdfEngine = PdfEngine(application, imageProcessor)
    private val historyManager = HistoryManager(application)
    private val draftManager = DraftManager(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val selectedImages: StateFlow<List<ImageItem>> = _selectedImages.asStateFlow()

    private val _selectedImageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedImageIds: StateFlow<Set<String>> = _selectedImageIds.asStateFlow()

    private val _settings = MutableStateFlow(PdfSettings())
    val settings: StateFlow<PdfSettings> = _settings.asStateFlow()

    private val _draftSession = MutableStateFlow<DraftSession?>(null)
    val draftSession: StateFlow<DraftSession?> = _draftSession.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    private val _history = MutableStateFlow<List<GeneratedPdfFile>>(emptyList())
    val history: StateFlow<List<GeneratedPdfFile>> = _history.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _activePreviewPdf = MutableStateFlow<GeneratedPdfFile?>(null)
    val activePreviewPdf: StateFlow<GeneratedPdfFile?> = _activePreviewPdf.asStateFlow()

    private val _appThemeColor = MutableStateFlow(AppThemeColor.SAPPHIRE_BLUE)
    val appThemeColor: StateFlow<AppThemeColor> = _appThemeColor.asStateFlow()

    var pendingCameraUri: Uri? = null

    private var conversionJob: Job? = null

    init {
        loadHistory()
        loadDraft()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setAppThemeColor(theme: AppThemeColor) {
        _appThemeColor.value = theme
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setShowFavoritesOnly(showOnly: Boolean) {
        _showFavoritesOnly.value = showOnly
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.value = historyManager.getHistory()
        }
    }

    fun toggleFavoriteHistory(recordId: String) {
        viewModelScope.launch {
            historyManager.toggleFavorite(recordId)
            loadHistory()
        }
    }

    fun loadDraft() {
        viewModelScope.launch {
            _draftSession.value = draftManager.getDraft()
        }
    }

    fun resumeDraft() {
        val draft = _draftSession.value ?: return
        _selectedImages.value = draft.images
        _settings.value = draft.settings
        _currentScreen.value = Screen.Editor
    }

    fun discardDraft() {
        viewModelScope.launch {
            draftManager.clearDraft()
            _draftSession.value = null
        }
    }

    private fun triggerDraftAutoSave() {
        viewModelScope.launch {
            val imgs = _selectedImages.value
            if (imgs.isNotEmpty()) {
                draftManager.saveDraft(imgs, _settings.value)
                _draftSession.value = draftManager.getDraft()
            } else {
                draftManager.clearDraft()
                _draftSession.value = null
            }
        }
    }

    // Camera Capture Uri Helper
    fun createCameraTempUri(): Uri {
        val context = getApplication<Application>()
        val cameraDir = File(context.cacheDir, "camera")
        if (!cameraDir.exists()) cameraDir.mkdirs()
        val photoFile = File(cameraDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        pendingCameraUri = uri
        return uri
    }

    fun handleCameraCaptureSuccess() {
        val uri = pendingCameraUri ?: return
        addImages(listOf(uri))
        pendingCameraUri = null
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        uris.forEach { uri ->
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val newItems = imageProcessor.getImageItemsFromUris(uris)
                _selectedImages.update { it + newItems }
                triggerDraftAutoSave()
                if (_currentScreen.value == Screen.Home) {
                    _currentScreen.value = Screen.Editor
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyPreset(presetType: String) {
        when (presetType) {
            "DOC" -> {
                _settings.update {
                    it.copy(
                        pageSize = PageSize.A4,
                        orientation = PageOrientation.PORTRAIT,
                        margin = PageMargin.SMALL,
                        scaleType = ImageScaleType.FIT_PAGE,
                        quality = ImageQuality.HIGH
                    )
                }
            }
            "PHOTO" -> {
                _settings.update {
                    it.copy(
                        pageSize = PageSize.ORIGINAL_IMAGE,
                        orientation = PageOrientation.AUTO,
                        margin = PageMargin.NONE,
                        scaleType = ImageScaleType.FIT_PAGE,
                        quality = ImageQuality.MAXIMUM
                    )
                }
            }
            "COMPACT" -> {
                _settings.update {
                    it.copy(
                        pageSize = PageSize.A4,
                        orientation = PageOrientation.AUTO,
                        margin = PageMargin.MEDIUM,
                        scaleType = ImageScaleType.FIT_PAGE,
                        quality = ImageQuality.MEDIUM
                    )
                }
            }
        }
    }

    // Multi-Selection Controls (Feature 31-33)
    fun toggleImageSelection(id: String) {
        _selectedImageIds.update { set ->
            if (set.contains(id)) set - id else set + id
        }
    }

    fun selectAllImages() {
        _selectedImageIds.value = _selectedImages.value.map { it.id }.toSet()
    }

    fun deselectAllImages() {
        _selectedImageIds.value = emptySet()
    }

    fun deleteSelectedImages() {
        val toDelete = _selectedImageIds.value
        if (toDelete.isEmpty()) return
        _selectedImages.update { list ->
            list.filterNot { toDelete.contains(it.id) }
        }
        _selectedImageIds.value = emptySet()
        if (_selectedImages.value.isEmpty() && _currentScreen.value == Screen.Editor) {
            _currentScreen.value = Screen.Home
        }
        triggerDraftAutoSave()
    }

    // Batch Adjustments (Feature 34-35)
    fun batchApplyFilter(filter: ImageFilter) {
        val targets = _selectedImageIds.value
        _selectedImages.update { list ->
            list.map { item ->
                if (targets.isEmpty() || targets.contains(item.id)) {
                    item.copy(filter = filter)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun batchApplyTuning(brightness: Float, contrast: Float) {
        val targets = _selectedImageIds.value
        _selectedImages.update { list ->
            list.map { item ->
                if (targets.isEmpty() || targets.contains(item.id)) {
                    item.copy(brightness = brightness, contrast = contrast)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    // Image Duplication (Feature 36)
    fun duplicateImage(id: String) {
        _selectedImages.update { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                val original = list[index]
                val copy = original.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${original.name} (Copy)"
                )
                val mutable = list.toMutableList()
                mutable.add(index + 1, copy)
                mutable
            } else {
                list
            }
        }
        triggerDraftAutoSave()
    }

    // Sorting (Feature 37-40)
    fun sortImagesByName(ascending: Boolean = true) {
        _selectedImages.update { list ->
            if (ascending) list.sortedBy { it.name.lowercase() }
            else list.sortedByDescending { it.name.lowercase() }
        }
        triggerDraftAutoSave()
    }

    fun sortImagesByDate(ascending: Boolean = false) {
        _selectedImages.update { list ->
            if (ascending) list.sortedBy { it.timestamp }
            else list.sortedByDescending { it.timestamp }
        }
        triggerDraftAutoSave()
    }

    fun sortImagesBySize(ascending: Boolean = false) {
        _selectedImages.update { list ->
            if (ascending) list.sortedBy { it.sizeBytes }
            else list.sortedByDescending { it.sizeBytes }
        }
        triggerDraftAutoSave()
    }

    fun reverseImagesOrder() {
        _selectedImages.update { it.reversed() }
        triggerDraftAutoSave()
    }

    // Single Image Modifications
    fun moveImage(fromIndex: Int, toIndex: Int) {
        _selectedImages.update { list ->
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            } else {
                list
            }
        }
        triggerDraftAutoSave()
    }

    fun rotateImage(id: String) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun toggleFlipHorizontal(id: String) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(flipHorizontal = !item.flipHorizontal)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun toggleFlipVertical(id: String) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(flipVertical = !item.flipVertical)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun updateImageFilter(id: String, filter: ImageFilter) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(filter = filter)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun updateImageTuning(id: String, brightness: Float, contrast: Float) {
        _selectedImages.update { list ->
            list.map { item ->
                if (item.id == id) {
                    item.copy(brightness = brightness, contrast = contrast)
                } else {
                    item
                }
            }
        }
        triggerDraftAutoSave()
    }

    fun removeImage(id: String) {
        _selectedImages.update { list ->
            val updated = list.filterNot { it.id == id }
            if (updated.isEmpty() && _currentScreen.value == Screen.Editor) {
                _currentScreen.value = Screen.Home
            }
            updated
        }
        _selectedImageIds.update { it - id }
        triggerDraftAutoSave()
    }

    fun clearAllImages() {
        _selectedImages.value = emptyList()
        _selectedImageIds.value = emptySet()
        discardDraft()
        _currentScreen.value = Screen.Home
    }

    fun updateSettings(newSettings: PdfSettings) {
        _settings.value = newSettings
        triggerDraftAutoSave()
    }

    // Conversion
    fun startConversion() {
        val images = _selectedImages.value
        if (images.isEmpty()) return

        conversionJob?.cancel()
        _conversionState.value = ConversionState.Processing(
            ConversionProgress(total = images.size, percentage = 0f)
        )

        conversionJob = viewModelScope.launch {
            try {
                val generated = pdfEngine.convertImagesToPdf(
                    images = images,
                    settings = _settings.value,
                    onProgress = { progress ->
                        _conversionState.value = ConversionState.Processing(progress)
                    }
                )

                if (generated.isNotEmpty()) {
                    historyManager.addRecords(generated)
                    loadHistory()
                    discardDraft()
                    _activePreviewPdf.value = generated.first()
                    _conversionState.value = ConversionState.Success(generated)
                    _currentScreen.value = Screen.Preview
                } else {
                    _conversionState.value = ConversionState.Error("No PDF files were created.")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _conversionState.value = ConversionState.Idle
                } else {
                    _conversionState.value = ConversionState.Error(e.localizedMessage ?: "Conversion failed.")
                }
            }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        _conversionState.value = ConversionState.Idle
    }

    fun resetConversion() {
        _conversionState.value = ConversionState.Idle
    }

    fun selectPreviewPdf(file: GeneratedPdfFile) {
        _activePreviewPdf.value = file
        _currentScreen.value = Screen.Preview
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            historyManager.deleteRecord(id)
            loadHistory()
            if (_activePreviewPdf.value?.id == id) {
                _activePreviewPdf.value = null
                _currentScreen.value = Screen.Home
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyManager.clearAll()
            loadHistory()
            _activePreviewPdf.value = null
            _currentScreen.value = Screen.Home
        }
    }

    // Export All as ZIP (Feature 46)
    fun exportAllHistoryAsZip(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val zipFile = historyManager.exportAllAsZip()
            onComplete(zipFile)
        }
    }

    // Storage Management (Feature 49)
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        var cacheSize = 0L
        val cacheDir = context.cacheDir
        cacheDir.walkTopDown().forEach { file ->
            if (file.isFile) cacheSize += file.length()
        }

        var pdfSize = 0L
        val pdfsDir = File(context.filesDir, "generated_pdfs")
        if (pdfsDir.exists()) {
            pdfsDir.walkTopDown().forEach { file ->
                if (file.isFile) pdfSize += file.length()
            }
        }

        StorageStats(
            cacheSizeBytes = cacheSize,
            pdfSizeBytes = pdfSize,
            pdfCount = _history.value.size
        )
    }

    suspend fun cleanTempCache(): Long = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        var freedBytes = 0L
        val cameraDir = File(context.cacheDir, "camera")
        if (cameraDir.exists()) {
            cameraDir.listFiles()?.forEach {
                freedBytes += it.length()
                it.delete()
            }
        }
        val coilCache = File(context.cacheDir, "image_cache")
        if (coilCache.exists()) {
            coilCache.listFiles()?.forEach {
                freedBytes += it.length()
                it.delete()
            }
        }
        freedBytes
    }

    // Size Estimator (Feature 26)
    fun getEstimatedPdfSizeBytes(): Long {
        val count = _selectedImages.value.size
        if (count == 0) return 0L
        val avgKb = when (_settings.value.quality) {
            ImageQuality.LOW -> 90L
            ImageQuality.MEDIUM -> 220L
            ImageQuality.HIGH -> 480L
            ImageQuality.MAXIMUM -> 1200L
        }
        val dpiFactor = when {
            _settings.value.customDpi <= 72 -> 0.7f
            _settings.value.customDpi >= 300 -> 1.5f
            else -> 1.0f
        }
        return (count * avgKb * 1024L * dpiFactor).toLong()
    }

    // Intents
    fun getShareIntent(file: GeneratedPdfFile): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, file.fileUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getEmailIntent(file: GeneratedPdfFile): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "PDF: ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Attached is the document generated using PixelPDF.")
            putExtra(Intent.EXTRA_STREAM, file.fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun getOpenIntent(file: GeneratedPdfFile): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun exportToDestination(sourceFile: GeneratedPdfFile, targetUri: Uri): Boolean {
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            contentResolver.openOutputStream(targetUri)?.use { outStream ->
                FileInputStream(File(sourceFile.filePath)).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class StorageStats(
    val cacheSizeBytes: Long,
    val pdfSizeBytes: Long,
    val pdfCount: Int
)
