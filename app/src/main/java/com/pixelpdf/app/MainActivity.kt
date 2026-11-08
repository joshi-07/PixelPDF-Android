package com.pixelpdf.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pixelpdf.app.data.model.ConversionState
import com.pixelpdf.app.ui.screens.*
import com.pixelpdf.app.ui.theme.PixelPDFTheme
import com.pixelpdf.app.ui.viewmodel.MainViewModel
import com.pixelpdf.app.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PixelPDFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val selectedImages by viewModel.selectedImages.collectAsState()
                    val settings by viewModel.settings.collectAsState()
                    val draftSession by viewModel.draftSession.collectAsState()
                    val conversionState by viewModel.conversionState.collectAsState()
                    val history by viewModel.history.collectAsState()
                    val activePreviewPdf by viewModel.activePreviewPdf.collectAsState()

                    // Back navigation handling
                    BackHandler(enabled = currentScreen != Screen.Home) {
                        when (currentScreen) {
                            Screen.Editor -> viewModel.navigateTo(Screen.Home)
                            Screen.Settings -> {
                                if (selectedImages.isNotEmpty()) {
                                    viewModel.navigateTo(Screen.Editor)
                                } else {
                                    viewModel.navigateTo(Screen.Home)
                                }
                            }
                            Screen.Preview -> viewModel.navigateTo(Screen.Home)
                            Screen.History -> viewModel.navigateTo(Screen.Home)
                            Screen.Home -> Unit
                        }
                    }

                    // Screen Switcher
                    when (currentScreen) {
                        Screen.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                recentPdfs = history,
                                draftSession = draftSession
                            )
                        }
                        Screen.Editor -> {
                            EditorScreen(
                                viewModel = viewModel,
                                images = selectedImages
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                currentSettings = settings
                            )
                        }
                        Screen.Preview -> {
                            PreviewScreen(
                                viewModel = viewModel,
                                pdfFile = activePreviewPdf
                            )
                        }
                        Screen.History -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                history = history
                            )
                        }
                    }

                    // Active Conversion Dialog
                    when (val state = conversionState) {
                        is ConversionState.Processing -> {
                            ConvertingDialog(
                                progress = state.progress,
                                onCancel = { viewModel.cancelConversion() }
                            )
                        }
                        is ConversionState.Error -> {
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetConversion()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
