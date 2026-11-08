package com.pixelpdf.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelpdf.app.data.model.*
import com.pixelpdf.app.ui.viewmodel.MainViewModel
import com.pixelpdf.app.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    currentSettings: PdfSettings
) {
    var fileName by remember { mutableStateOf(currentSettings.fileName) }
    var selectedPageSize by remember { mutableStateOf(currentSettings.pageSize) }
    var selectedOrientation by remember { mutableStateOf(currentSettings.orientation) }
    var selectedMargin by remember { mutableStateOf(currentSettings.margin) }
    var selectedScaleType by remember { mutableStateOf(currentSettings.scaleType) }
    var selectedQuality by remember { mutableStateOf(currentSettings.quality) }
    var selectedOutputMode by remember { mutableStateOf(currentSettings.outputMode) }
    var imagesPerPdf by remember { mutableStateOf(currentSettings.imagesPerPdf) }
    var includePageNumbers by remember { mutableStateOf(currentSettings.includePageNumbers) }
    var includeBorders by remember { mutableStateOf(currentSettings.includeBorders) }
    var backgroundColorHex by remember { mutableStateOf(currentSettings.backgroundColorHex) }

    fun saveAndExit() {
        viewModel.updateSettings(
            currentSettings.copy(
                fileName = fileName.trim().ifEmpty { "PixelPDF_Export" },
                pageSize = selectedPageSize,
                orientation = selectedOrientation,
                margin = selectedMargin,
                scaleType = selectedScaleType,
                quality = selectedQuality,
                outputMode = selectedOutputMode,
                imagesPerPdf = imagesPerPdf,
                includePageNumbers = includePageNumbers,
                includeBorders = includeBorders,
                backgroundColorHex = backgroundColorHex
            )
        )
        // If there are images selected, go back to editor; otherwise home
        if (viewModel.selectedImages.value.isNotEmpty()) {
            viewModel.navigateTo(Screen.Editor)
        } else {
            viewModel.navigateTo(Screen.Home)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PDF Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and Back")
                    }
                },
                actions = {
                    TextButton(onClick = { saveAndExit() }) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // File Name Section
            item {
                SectionCard(title = "PDF File Name") {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Invoices_Nov2026") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Page Size Section
            item {
                SectionCard(title = "Page Size") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageSize.values().forEach { size ->
                            OptionRadioItem(
                                title = size.displayName,
                                isSelected = selectedPageSize == size,
                                onClick = { selectedPageSize = size }
                            )
                        }
                    }
                }
            }

            // Orientation Section
            item {
                SectionCard(title = "Orientation") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PageOrientation.values().forEach { orient ->
                            FilterChip(
                                selected = selectedOrientation == orient,
                                onClick = { selectedOrientation = orient },
                                label = { Text(orient.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Margins Section
            item {
                SectionCard(title = "Page Margins") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageMargin.values().forEach { margin ->
                            OptionRadioItem(
                                title = margin.displayName,
                                isSelected = selectedMargin == margin,
                                onClick = { selectedMargin = margin }
                            )
                        }
                    }
                }
            }

            // Image Scaling
            item {
                SectionCard(title = "Image Scaling") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageScaleType.values().forEach { scale ->
                            OptionRadioItem(
                                title = scale.displayName,
                                isSelected = selectedScaleType == scale,
                                onClick = { selectedScaleType = scale }
                            )
                        }
                    }
                }
            }

            // Image Compression / Quality
            item {
                SectionCard(title = "Image Quality & Compression") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageQuality.values().forEach { q ->
                            OptionRadioItem(
                                title = q.displayName,
                                isSelected = selectedQuality == q,
                                onClick = { selectedQuality = q }
                            )
                        }
                    }
                }
            }

            // Output Mode
            item {
                SectionCard(title = "Output Options") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutputMode.values().forEach { mode ->
                            OptionRadioItem(
                                title = mode.displayName,
                                isSelected = selectedOutputMode == mode,
                                onClick = { selectedOutputMode = mode }
                            )
                        }

                        if (selectedOutputMode == OutputMode.SPLIT_BY_COUNT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Images per PDF:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (imagesPerPdf > 1) imagesPerPdf-- },
                                        enabled = imagesPerPdf > 1
                                    ) {
                                        Text("-", style = MaterialTheme.typography.titleLarge)
                                    }
                                    Text(
                                        text = "$imagesPerPdf",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    IconButton(onClick = { imagesPerPdf++ }) {
                                        Text("+", style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Additional Customizations
            item {
                SectionCard(title = "Extra Formatting") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Page Numbering", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Print '1 / N' footer at the bottom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = includePageNumbers,
                                onCheckedChange = { includePageNumbers = it }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Page Borders", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Draw a subtle border outline around each image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = includeBorders,
                                onCheckedChange = { includeBorders = it }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Page Background", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val colors = listOf(
                                    0xFFFFFFFF to "White",
                                    0xFFF8FAFC to "Slate",
                                    0xFFFFFBEB to "Cream",
                                    0xFF0F172A to "Dark"
                                )
                                colors.forEach { (hex, name) ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (backgroundColorHex == hex) 3.dp else 1.dp,
                                                color = if (backgroundColorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { backgroundColorHex = hex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (backgroundColorHex == hex) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = name,
                                                tint = if (hex == 0xFF0F172A) Color.White else Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun OptionRadioItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
