package com.pixelpdf.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
            val appThemeColor by viewModel.appThemeColor.collectAsState()

            PixelPDFTheme(themeColor = appThemeColor) {
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

                    Scaffold(
                        bottomBar = {
                            // Bottom Navigation Bar (UI Change 1)
                            if (currentScreen != Screen.Preview) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Home,
                                        onClick = { viewModel.navigateTo(Screen.Home) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = "Home"
                                            )
                                        },
                                        label = { Text("Convert") }
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Editor,
                                        onClick = { viewModel.navigateTo(Screen.Editor) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (selectedImages.isNotEmpty()) {
                                                        Badge { Text("${selectedImages.size}") }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (currentScreen == Screen.Editor) Icons.Filled.GridView else Icons.Outlined.GridView,
                                                    contentDescription = "Arrange"
                                                )
                                            }
                                        },
                                        label = { Text("Arrange") }
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.History,
                                        onClick = { viewModel.navigateTo(Screen.History) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.History) Icons.Filled.Folder else Icons.Outlined.Folder,
                                                contentDescription = "Library"
                                            )
                                        },
                                        label = { Text("Library") }
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.Settings,
                                        onClick = { viewModel.navigateTo(Screen.Settings) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        }
                    ) { scaffoldPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(scaffoldPadding)
                        ) {
                            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                                when (screen) {
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
                            }
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
