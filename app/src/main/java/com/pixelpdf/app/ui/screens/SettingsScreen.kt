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
import androidx.compose.material.icons.outlined.Palette
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
import java.util.Locale

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
    var pageNumberStyle by remember { mutableStateOf(currentSettings.pageNumberStyle) }
    var pageNumberPosition by remember { mutableStateOf(currentSettings.pageNumberPosition) }
    var includeBorders by remember { mutableStateOf(currentSettings.includeBorders) }
    var backgroundColorHex by remember { mutableStateOf(currentSettings.backgroundColorHex) }

    // Watermark
    var watermarkText by remember { mutableStateOf(currentSettings.watermarkText) }
    var watermarkOpacity by remember { mutableStateOf(currentSettings.watermarkOpacity) }
    var watermarkColorHex by remember { mutableStateOf(currentSettings.watermarkColorHex) }
    var watermarkAngle by remember { mutableStateOf(currentSettings.watermarkAngle) }
    var watermarkFontSize by remember { mutableStateOf(currentSettings.watermarkFontSize) }

    // Header & Footer
    var headerText by remember { mutableStateOf(currentSettings.headerText) }
    var headerAlignment by remember { mutableStateOf(currentSettings.headerAlignment) }
    var footerText by remember { mutableStateOf(currentSettings.footerText) }
    var headerFooterFontSize by remember { mutableStateOf(currentSettings.headerFooterFontSize) }

    // Styling & Duplex
    var imageRoundedCorners by remember { mutableStateOf(currentSettings.imageRoundedCorners) }
    var insertBlankPages by remember { mutableStateOf(currentSettings.insertBlankPages) }
    var customDpi by remember { mutableStateOf(currentSettings.customDpi) }

    // Metadata
    var documentTitle by remember { mutableStateOf(currentSettings.documentTitle) }
    var documentAuthor by remember { mutableStateOf(currentSettings.documentAuthor) }
    var documentSubject by remember { mutableStateOf(currentSettings.documentSubject) }
    var documentKeywords by remember { mutableStateOf(currentSettings.documentKeywords) }

    val activeTheme by viewModel.appThemeColor.collectAsState()

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
                pageNumberStyle = pageNumberStyle,
                pageNumberPosition = pageNumberPosition,
                includeBorders = includeBorders,
                backgroundColorHex = backgroundColorHex,

                watermarkText = watermarkText.trim(),
                watermarkOpacity = watermarkOpacity,
                watermarkColorHex = watermarkColorHex,
                watermarkAngle = watermarkAngle,
                watermarkFontSize = watermarkFontSize,

                headerText = headerText.trim(),
                headerAlignment = headerAlignment,
                footerText = footerText.trim(),
                headerFooterFontSize = headerFooterFontSize,

                imageRoundedCorners = imageRoundedCorners,
                insertBlankPages = insertBlankPages,
                customDpi = customDpi,

                documentTitle = documentTitle.trim(),
                documentAuthor = documentAuthor.trim(),
                documentSubject = documentSubject.trim(),
                documentKeywords = documentKeywords.trim()
            )
        )
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
                        text = "PDF & Document Settings",
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
                        Text("Save & Apply", fontWeight = FontWeight.Bold)
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
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Theme Accent Color (Feature 50)
            item {
                SectionCard(title = "App Color Accent Theme") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeColor.values().forEach { theme ->
                                FilterChip(
                                    selected = activeTheme == theme,
                                    onClick = { viewModel.setAppThemeColor(theme) },
                                    label = { Text(theme.displayName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // PDF File Name
            item {
                SectionCard(title = "PDF File Name") {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Scanned_Invoice_2026") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Page Size & DPI
            item {
                SectionCard(title = "Page Geometry & Resolution") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Page Dimensions:", style = MaterialTheme.typography.labelMedium)
                        PageSize.values().forEach { size ->
                            OptionRadioItem(
                                title = size.displayName,
                                isSelected = selectedPageSize == size,
                                onClick = { selectedPageSize = size }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Print Resolution (DPI):", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                72 to "72 DPI (Web)",
                                150 to "150 DPI (Std)",
                                300 to "300 DPI (Print)"
                            ).forEach { (dpi, label) ->
                                FilterChip(
                                    selected = customDpi == dpi,
                                    onClick = { customDpi = dpi },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Orientation & Margins
            item {
                SectionCard(title = "Page Orientation & Margins") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Orientation:", style = MaterialTheme.typography.labelMedium)
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

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Margins:", style = MaterialTheme.typography.labelMedium)
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

            // Image Scaling & Quality
            item {
                SectionCard(title = "Image Scaling & Compression") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Scaling:", style = MaterialTheme.typography.labelMedium)
                        ImageScaleType.values().forEach { scale ->
                            OptionRadioItem(
                                title = scale.displayName,
                                isSelected = selectedScaleType == scale,
                                onClick = { selectedScaleType = scale }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Quality & Compression:", style = MaterialTheme.typography.labelMedium)
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

            // Custom Text Watermark (Features 14-17)
            item {
                SectionCard(title = "Custom Text Watermark") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. CONFIDENTIAL, DRAFT, SAMPLE") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (watermarkText.isNotEmpty()) {
                            Text(
                                text = "Opacity: ${(watermarkOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = watermarkOpacity,
                                onValueChange = { watermarkOpacity = it },
                                valueRange = 0.10f..0.80f
                            )

                            Text("Angle:", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0f to "0°", 30f to "30°", 45f to "45°", 60f to "60°", 90f to "90°").forEach { (angle, lbl) ->
                                    FilterChip(
                                        selected = watermarkAngle == angle,
                                        onClick = { watermarkAngle = angle },
                                        label = { Text(lbl) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text("Color:", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    0xFF888888 to "Gray",
                                    0xFFDC2626 to "Red Alert",
                                    0xFF2563EB to "Blue Brand",
                                    0xFF111827 to "Dark"
                                ).forEach { (hex, colorName) ->
                                    FilterChip(
                                        selected = watermarkColorHex == hex,
                                        onClick = { watermarkColorHex = hex },
                                        label = { Text(colorName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Running Headers & Footers (Features 18-20)
            item {
                SectionCard(title = "Running Headers & Footers") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = headerText,
                            onValueChange = { headerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Header text (e.g. Company Report 2026)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextAlignment.values().forEach { align ->
                                FilterChip(
                                    selected = headerAlignment == align,
                                    onClick = { headerAlignment = align },
                                    label = { Text(align.displayName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = footerText,
                            onValueChange = { footerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Footer text (e.g. Strictly Confidential)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Page Numbering (Features 21-22)
            item {
                SectionCard(title = "Page Numbering") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Print Page Numbers", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = includePageNumbers,
                                onCheckedChange = { includePageNumbers = it }
                            )
                        }

                        if (includePageNumbers) {
                            Text("Style:", style = MaterialTheme.typography.labelMedium)
                            PageNumberStyle.values().forEach { st ->
                                OptionRadioItem(
                                    title = st.displayName,
                                    isSelected = pageNumberStyle == st,
                                    onClick = { pageNumberStyle = st }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Position:", style = MaterialTheme.typography.labelMedium)
                            PageNumberPosition.values().forEach { pos ->
                                OptionRadioItem(
                                    title = pos.displayName,
                                    isSelected = pageNumberPosition == pos,
                                    onClick = { pageNumberPosition = pos }
                                )
                            }
                        }
                    }
                }
            }

            // Stylized Layout & Duplex Blank Pages (Features 23-25)
            item {
                SectionCard(title = "Page Borders & Duplex Layout") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Draw Page Border", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = includeBorders,
                                onCheckedChange = { includeBorders = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Insert Blank Pages (Duplex)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Adds blank spacers for two-sided booklet printing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = insertBlankPages,
                                onCheckedChange = { insertBlankPages = it }
                            )
                        }

                        Text(
                            text = "Image Corner Rounding: ${imageRoundedCorners.toInt()} pt",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = imageRoundedCorners,
                            onValueChange = { imageRoundedCorners = it },
                            valueRange = 0f..28f
                        )
                    }
                }
            }

            // PDF Document Metadata (Features 27-30)
            item {
                SectionCard(title = "PDF Document Metadata") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = documentTitle,
                            onValueChange = { documentTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Document Title") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = documentAuthor,
                            onValueChange = { documentAuthor = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Author / Organization") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = documentSubject,
                            onValueChange = { documentSubject = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Subject / Purpose") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = documentKeywords,
                            onValueChange = { documentKeywords = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Keywords (comma separated)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
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
                            Text(
                                text = "Images per PDF: $imagesPerPdf",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = imagesPerPdf.toFloat(),
                                onValueChange = { imagesPerPdf = it.toInt() },
                                valueRange = 1f..50f,
                                steps = 49
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { saveAndExit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply & Save Settings")
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
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
