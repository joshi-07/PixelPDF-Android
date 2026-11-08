package com.pixelpdf.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelpdf.app.data.model.ConversionProgress
import com.pixelpdf.app.data.model.ConversionState
import com.pixelpdf.app.data.model.DraftSession
import com.pixelpdf.app.data.model.GeneratedPdfFile
import com.pixelpdf.app.data.model.ImageItem
import com.pixelpdf.app.data.model.PdfSettings
import com.pixelpdf.app.data.repository.DraftManager
import com.pixelpdf.app.data.repository.HistoryManager
import com.pixelpdf.app.data.repository.ImageProcessor
import com.pixelpdf.app.data.repository.PdfEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val _settings = MutableStateFlow(PdfSettings())
    val settings: StateFlow<PdfSettings> = _settings.asStateFlow()

    private val _draftSession = MutableStateFlow<DraftSession?>(null)
    val draftSession: StateFlow<DraftSession?> = _draftSession.asStateFlow()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()

    private val _history = MutableStateFlow<List<GeneratedPdfFile>>(emptyList())
    val history: StateFlow<List<GeneratedPdfFile>> = _history.asStateFlow()

    private val _activePreviewPdf = MutableStateFlow<GeneratedPdfFile?>(null)
    val activePreviewPdf: StateFlow<GeneratedPdfFile?> = _activePreviewPdf.asStateFlow()

    private var conversionJob: Job? = null

    init {
        loadHistory()
        loadDraft()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.value = historyManager.getHistory()
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
                        pageSize = com.pixelpdf.app.data.model.PageSize.A4,
                        orientation = com.pixelpdf.app.data.model.PageOrientation.PORTRAIT,
                        margin = com.pixelpdf.app.data.model.PageMargin.SMALL,
                        scaleType = com.pixelpdf.app.data.model.ImageScaleType.FIT_PAGE,
                        quality = com.pixelpdf.app.data.model.ImageQuality.HIGH
                    )
                }
            }
            "PHOTO" -> {
                _settings.update {
                    it.copy(
                        pageSize = com.pixelpdf.app.data.model.PageSize.ORIGINAL_IMAGE,
                        orientation = com.pixelpdf.app.data.model.PageOrientation.AUTO,
                        margin = com.pixelpdf.app.data.model.PageMargin.NONE,
                        scaleType = com.pixelpdf.app.data.model.ImageScaleType.FIT_PAGE,
                        quality = com.pixelpdf.app.data.model.ImageQuality.MAXIMUM
                    )
                }
            }
            "COMPACT" -> {
                _settings.update {
                    it.copy(
                        pageSize = com.pixelpdf.app.data.model.PageSize.A4,
                        orientation = com.pixelpdf.app.data.model.PageOrientation.AUTO,
                        margin = com.pixelpdf.app.data.model.PageMargin.MEDIUM,
                        scaleType = com.pixelpdf.app.data.model.ImageScaleType.FIT_PAGE,
                        quality = com.pixelpdf.app.data.model.ImageQuality.MEDIUM
                    )
                }
            }
        }
    }

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

    fun removeImage(id: String) {
        _selectedImages.update { list ->
            val updated = list.filterNot { it.id == id }
            if (updated.isEmpty() && _currentScreen.value == Screen.Editor) {
                _currentScreen.value = Screen.Home
            }
            updated
        }
        triggerDraftAutoSave()
    }

    fun clearAllImages() {
        _selectedImages.value = emptyList()
        discardDraft()
        _currentScreen.value = Screen.Home
    }

    fun updateSettings(newSettings: PdfSettings) {
        _settings.value = newSettings
        triggerDraftAutoSave()
    }

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

    fun getShareIntent(file: GeneratedPdfFile): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, file.fileUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
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
