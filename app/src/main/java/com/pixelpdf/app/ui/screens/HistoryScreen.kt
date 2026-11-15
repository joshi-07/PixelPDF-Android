package com.pixelpdf.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pixelpdf.app.data.model.GeneratedPdfFile
import com.pixelpdf.app.ui.viewmodel.MainViewModel
import com.pixelpdf.app.ui.viewmodel.Screen
import com.pixelpdf.app.util.PrintHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    history: List<GeneratedPdfFile>
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var isExportingZip by remember { mutableStateOf(false) }

    val filteredList = remember(history, searchQuery, showFavoritesOnly) {
        history.filter { item ->
            val matchesQuery = if (searchQuery.isBlank()) true else item.name.contains(searchQuery, ignoreCase = true)
            val matchesFav = if (showFavoritesOnly) item.isFavorite else true
            matchesQuery && matchesFav
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently remove all generated PDFs from the library and reclaim disk space.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllHistory()
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PDF Library & History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                },
                actions = {
                    // Export all as ZIP (Feature 46)
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                isExportingZip = true
                                viewModel.exportAllHistoryAsZip { zipFile ->
                                    isExportingZip = false
                                    if (zipFile != null && zipFile.exists()) {
                                        try {
                                            val zipUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                zipFile
                                            )
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(android.content.Intent.EXTRA_STREAM, zipUri)
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "PixelPDF_All_Documents.zip")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share All PDFs as ZIP"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Zip export ready: ${zipFile.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Could not generate ZIP", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isExportingZip
                        ) {
                            Icon(imageVector = Icons.Outlined.Archive, contentDescription = "Export All as ZIP")
                        }

                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar & Filter Chips (Features 43-45)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search PDFs by file name...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showFavoritesOnly,
                    onClick = { showFavoritesOnly = false },
                    label = { Text("All (${history.size})") }
                )
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = true },
                    leadingIcon = {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                    },
                    label = { Text("Starred (${history.count { it.isFavorite }})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching documents" else "No saved PDFs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { pdf ->
                        HistoryPdfCard(
                            pdf = pdf,
                            onOpen = { viewModel.selectPreviewPdf(pdf) },
                            onToggleFavorite = { viewModel.toggleFavoriteHistory(pdf.id) },
                            onPrint = {
                                PrintHelper.printPdf(context, File(pdf.filePath), pdf.name)
                            },
                            onEmail = {
                                try {
                                    context.startActivity(viewModel.getEmailIntent(pdf))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                context.startActivity(
                                    android.content.Intent.createChooser(
                                        viewModel.getShareIntent(pdf),
                                        "Share PDF via"
                                    )
                                )
                            },
                            onDelete = { viewModel.deleteHistoryItem(pdf.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryPdfCard(
    pdf: GeneratedPdfFile,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPrint: () -> Unit,
    onEmail: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(pdf.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pdf.pageCount} pages • ${pdf.formattedSize} • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (pdf.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (pdf.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Print, Email, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrint, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Print, contentDescription = "Print", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEmail, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Email, contentDescription = "Email", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
