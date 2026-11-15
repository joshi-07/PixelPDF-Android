package com.pixelpdf.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pixelpdf.app.data.model.ImageFilter
import com.pixelpdf.app.data.model.ImageItem
import com.pixelpdf.app.ui.viewmodel.MainViewModel
import com.pixelpdf.app.ui.viewmodel.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    images: List<ImageItem>
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val selectedIds by viewModel.selectedImageIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()

    var showClearConfirm by remember { mutableStateOf(false) }
    var showBatchFilterDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingImage by remember { mutableStateOf<ImageItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val contentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    val openImagePicker = {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                contentPickerLauncher.launch("image/*")
            }
        } catch (_: Exception) {
            try {
                contentPickerLauncher.launch("image/*")
            } catch (_: Exception) {}
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Images?") },
            text = { Text("This will remove all ${images.size} selected images from your conversion queue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showClearConfirm = false
                        viewModel.clearAllImages()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch Filter Dialog
    if (showBatchFilterDialog) {
        AlertDialog(
            onDismissRequest = { showBatchFilterDialog = false },
            title = { Text("Apply Filter to ${selectedIds.size} Images") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a filter preset to apply in bulk:")
                    ImageFilter.values().forEach { filter ->
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.batchApplyFilter(filter)
                                showBatchFilterDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(filter.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Single Image Lightbox / Visual Tuner Modal
    editingImage?.let { activeItem ->
        ImageLightboxEditorDialog(
            item = activeItem,
            onDismiss = { editingImage = null },
            onUpdateFilter = { filter ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateImageFilter(activeItem.id, filter)
                editingImage = activeItem.copy(filter = filter)
            },
            onUpdateTuning = { brightness, contrast ->
                viewModel.updateImageTuning(activeItem.id, brightness, contrast)
                editingImage = activeItem.copy(brightness = brightness, contrast = contrast)
            },
            onRotate = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.rotateImage(activeItem.id)
                editingImage = activeItem.copy(rotationDegrees = (activeItem.rotationDegrees + 90) % 360)
            },
            onFlipHorizontal = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleFlipHorizontal(activeItem.id)
                editingImage = activeItem.copy(flipHorizontal = !activeItem.flipHorizontal)
            },
            onFlipVertical = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleFlipVertical(activeItem.id)
                editingImage = activeItem.copy(flipVertical = !activeItem.flipVertical)
            },
            onDuplicate = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.duplicateImage(activeItem.id)
                editingImage = null
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Contextual Selection TopBar (UI Change 3)
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.deselectAllImages() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Deselect")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedIds.size == images.size) viewModel.deselectAllImages()
                            else viewModel.selectAllImages()
                        }) {
                            Icon(
                                imageVector = if (selectedIds.size == images.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = "Select All"
                            )
                        }
                        IconButton(onClick = { showBatchFilterDialog = true }) {
                            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Batch Filter")
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteSelectedImages()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Arrange & Edit",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${images.size} ${if (images.size == 1) "page" else "pages"} • Tap to edit / tune",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Sort Menu Dropdown
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(imageVector = Icons.Default.FilterList, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)") },
                                    onClick = {
                                        viewModel.sortImagesByName(true)
                                        showSortMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z to A)") },
                                    onClick = {
                                        viewModel.sortImagesByName(false)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest First)") },
                                    onClick = {
                                        viewModel.sortImagesByDate(false)
                                        showSortMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Size (Largest)") },
                                    onClick = {
                                        viewModel.sortImagesBySize(false)
                                        showSortMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Reverse Order") },
                                    onClick = {
                                        viewModel.reverseImagesOrder()
                                        showSortMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) }
                                )
                            }
                        }

                        IconButton(onClick = { openImagePicker() }) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add More")
                        }

                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = "Clear All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Floating Summary Pill (UI Change 2)
                    val estBytes = viewModel.getEstimatedPdfSizeBytes()
                    val estMb = estBytes / (1024f * 1024f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${images.size} Pages • Est. ~${String.format(Locale.US, "%.1f MB", estMb)} • High Quality",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.Settings) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Settings")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startConversion()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Convert PDF",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(images, key = { _, item -> item.id }) { index, item ->
                val isSelected = selectedIds.contains(item.id)
                EnhancedImageGridCard(
                    item = item,
                    index = index,
                    totalCount = images.size,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.toggleImageSelection(item.id)
                        } else {
                            editingImage = item
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleImageSelection(item.id)
                    },
                    onSelectToggle = { viewModel.toggleImageSelection(item.id) },
                    onRotate = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.rotateImage(item.id)
                    },
                    onMoveLeft = {
                        if (index > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.moveImage(index, index - 1)
                        }
                    },
                    onMoveRight = {
                        if (index < images.size - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.moveImage(index, index + 1)
                        }
                    },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.removeImage(item.id)
                    }
                )
            }
        }
    }
}

@Composable
fun EnhancedImageGridCard(
    item: ImageItem,
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onRotate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Image with Filter & Rotation preview
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 44.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .rotate(item.rotationDegrees.toFloat()),
                contentScale = ContentScale.Crop
            )

            // Number Badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Selection Checkbox / Toggle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = onSelectToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Select",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Filter Badge if applied
            if (item.filter != ImageFilter.ORIGINAL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.filter.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            // Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(
                        onClick = onMoveLeft,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Move Left",
                            modifier = Modifier.size(13.dp),
                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }

                    IconButton(
                        onClick = onMoveRight,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Move Right",
                            modifier = Modifier.size(13.dp),
                            tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onRotate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                            contentDescription = "Rotate",
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageLightboxEditorDialog(
    item: ImageItem,
    onDismiss: () -> Unit,
    onUpdateFilter: (ImageFilter) -> Unit,
    onUpdateTuning: (brightness: Float, contrast: Float) -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onDuplicate: () -> Unit
) {
    var brightness by remember { mutableStateOf(item.brightness) }
    var contrast by remember { mutableStateOf(item.contrast) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Image Preview
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .rotate(item.rotationDegrees.toFloat()),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Rotate, Flip H, Flip V, Duplicate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(onClick = onRotate) {
                        Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("90°")
                    }
                    FilledTonalButton(onClick = onFlipHorizontal) {
                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flip H")
                    }
                    FilledTonalButton(onClick = onFlipVertical) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flip V")
                    }
                    FilledTonalButton(onClick = onDuplicate) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clone")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Selector Chips
                Text(
                    text = "Color & Document Filters",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ImageFilter.values()) { f ->
                        FilterChip(
                            selected = item.filter == f,
                            onClick = { onUpdateFilter(f) },
                            label = { Text(f.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fine-tuning Brightness & Contrast
                Text(
                    text = "Exposure & Contrast Tuning",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    text = "Brightness: ${String.format(Locale.US, "%+.2f", brightness)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        onUpdateTuning(brightness, contrast)
                    },
                    valueRange = -0.5f..0.5f
                )

                Text(
                    text = "Contrast: ${String.format(Locale.US, "%.2fx", contrast)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = contrast,
                    onValueChange = {
                        contrast = it
                        onUpdateTuning(brightness, contrast)
                    },
                    valueRange = 0.5f..2.0f
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Technical Info Details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Image Specifications",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val kb = item.sizeBytes / 1024f
                        Text(
                            text = "Resolution: ${item.displayWidth} × ${item.displayHeight} px • Size: ${String.format(Locale.US, "%.1f KB", kb)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
