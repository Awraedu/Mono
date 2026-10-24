package com.awras.mono.features.tools.productivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.Geist
import com.awras.mono.ui.theme.GeistMono
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import java.util.UUID
import java.util.regex.Pattern

enum class ClipType(val displayName: String) {
    ALL("All"),
    PINNED("Pinned"),
    OTP("OTPs & Codes"),
    LINK("Links"),
    CODE("Code Snippets"),
    EMAIL("Emails"),
    PHONE("Phone Numbers"),
    TEXT("Text"),
    IMAGE("Images")
}

data class ClipboardItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: ClipType,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isMasked: Boolean = type == ClipType.OTP,
    val otpCode: String? = null,
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val extraMetadata: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardManagerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // Tracking state
    var isTrackingEnabled by remember { mutableStateOf(true) }

    // Search & Filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ClipType.ALL) }

    // Dialog states
    var showClearDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newClipText by remember { mutableStateOf("") }

    // History items list
    var history by remember {
        mutableStateOf(getInitialSampleClips())
    }

    // Capture clip from system clipboard
    val captureCurrentClip: () -> Unit = remember(context, clipboardManager) {
        {
            if (isTrackingEnabled && clipboardManager.hasPrimaryClip()) {
                val clipData = clipboardManager.primaryClip
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        val uri = item.uri ?: item.intent?.data
                        val text = item.text?.toString() ?: item.coerceToText(context)?.toString()

                        val isImage = clipData.description.hasMimeType("image/*") ||
                                (uri != null && (uri.toString().contains("image") || uri.toString().contains("media")))

                        if (uri != null && isImage) {
                            val loadedBitmap = loadBitmapFromUri(context, uri)
                            val newItem = ClipboardItem(
                                content = "Clipboard Screenshot (${uri.lastPathSegment ?: "Image"})",
                                type = ClipType.IMAGE,
                                imageUri = uri,
                                bitmap = loadedBitmap,
                                extraMetadata = "Clipboard Screenshot"
                            )
                            if (history.none { it.imageUri == uri || (it.bitmap != null && loadedBitmap != null && it.bitmap == loadedBitmap) }) {
                                history = listOf(newItem) + history
                            }
                        } else if (!text.isNullOrBlank()) {
                            val trimmed = text.trim()
                            if (history.none { it.content == trimmed }) {
                                val newItem = parseClipboardText(trimmed)
                                history = listOf(newItem) + history
                            }
                        }
                    }
                }
            }
        }
    }

    // Immediately capture clipboard & load latest device screenshot on screen launch
    LaunchedEffect(Unit) {
        captureCurrentClip()

        val recentImage = getMostRecentMediaImage(context)
        if (recentImage != null) {
            val (uri, loadedBitmap) = recentImage
            if (history.none { it.imageUri == uri }) {
                val screenshotItem = ClipboardItem(
                    content = "Recent Device Screenshot (${uri.lastPathSegment ?: "Screenshot"})",
                    type = ClipType.IMAGE,
                    imageUri = uri,
                    bitmap = loadedBitmap,
                    extraMetadata = "Latest Screenshot"
                )
                history = listOf(screenshotItem) + history
            }
        }
    }

    // Realtime Clipboard Listener
    DisposableEffect(clipboardManager, isTrackingEnabled) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            if (isTrackingEnabled) {
                captureCurrentClip()
            }
        }
        clipboardManager.addPrimaryClipChangedListener(listener)
        onDispose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    // Copy to system clipboard helper
    fun copyItemToClipboard(text: String, label: String = "Mono Clipboard") {
        haptics.click()
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Filtered items logic
    val filteredHistory = remember(history, searchQuery, selectedFilter) {
        history.filter { item ->
            val matchesFilter = when (selectedFilter) {
                ClipType.ALL -> true
                ClipType.PINNED -> item.isPinned
                else -> item.type == selectedFilter
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.content.contains(searchQuery, ignoreCase = true) ||
                (item.extraMetadata != null && item.extraMetadata.contains(searchQuery, ignoreCase = true)) ||
                (item.otpCode != null && item.otpCode.contains(searchQuery))
            }

            matchesFilter && matchesSearch
        }.sortedWith(compareByDescending<ClipboardItem> { it.isPinned }.thenByDescending { it.timestamp })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Clipboard",
                description = "Smart Clipboard Tracking & Adaptive History",
                icon = TablerIcons.ClipboardList,
                onBackClick = onBackClick,
                rightActionIcon = if (isTrackingEnabled) TablerIcons.Eye else TablerIcons.EyeOff,
                onRightActionClick = {
                    haptics.click()
                    isTrackingEnabled = !isTrackingEnabled
                    Toast.makeText(
                        context,
                        if (isTrackingEnabled) "Clipboard Tracking Active" else "Clipboard Tracking Paused",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. HERO TRACKER STATUS CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isTrackingEnabled) vibrantOrange else MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = if (isTrackingEnabled) "TRACKER ACTIVE" else "TRACKER PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (isTrackingEnabled) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Surface(
                            shape = ContinuousCapsule,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${history.size} CLIPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Stats & Action Bar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                captureCurrentClip()
                                val recent = getMostRecentMediaImage(context)
                                if (recent != null) {
                                    val (uri, bitmap) = recent
                                    if (history.none { it.imageUri == uri }) {
                                        val screenshotItem = ClipboardItem(
                                            content = "Latest Screenshot (${uri.lastPathSegment ?: "Image"})",
                                            type = ClipType.IMAGE,
                                            imageUri = uri,
                                            bitmap = bitmap,
                                            extraMetadata = "Device Screenshot"
                                        )
                                        history = listOf(screenshotItem) + history
                                    }
                                }
                                Toast.makeText(context, "Clipboard & Screenshots synced", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule,
                            border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = TablerIcons.ClipboardCheck,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = vibrantOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = vibrantOrange
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                haptics.click()
                                newClipText = ""
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                showClearDialog = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = TablerIcons.Trash,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ==================== 2. SEARCH & FILTER SECTION ====================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search clipboard history...") },
                leadingIcon = {
                    Icon(
                        imageVector = TablerIcons.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(TablerIcons.X, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousCapsule,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = vibrantOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ClipType.entries.toTypedArray()) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = remember(history, filter) {
                        when (filter) {
                            ClipType.ALL -> history.size
                            ClipType.PINNED -> history.count { it.isPinned }
                            else -> history.count { it.type == filter }
                        }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.tick()
                            selectedFilter = filter
                        },
                        label = {
                            Text(
                                text = "${filter.displayName} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ==================== 3. CLIPBOARD TIMELINE LIST ====================
            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = TablerIcons.Clipboard,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No clips matching search" else "Clipboard history empty",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        ClipboardItemCard(
                            item = item,
                            onCopy = { copyItemToClipboard(it) },
                            onTogglePin = {
                                haptics.tick()
                                history = history.map {
                                    if (it.id == item.id) it.copy(isPinned = !it.isPinned) else it
                                }
                            },
                            onToggleMask = {
                                haptics.tick()
                                history = history.map {
                                    if (it.id == item.id) it.copy(isMasked = !it.isMasked) else it
                                }
                            },
                            onDelete = {
                                haptics.tick()
                                history = history.filter { it.id != item.id }
                            }
                        )
                    }
                }
            }
        }
    }

    // ==================== CLEAR HISTORY DIALOG ====================
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Clipboard History?") },
            text = { Text("This will remove all unpinned items from your clipboard timeline.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.click()
                        history = history.filter { it.isPinned }
                        showClearDialog = false
                    }
                ) {
                    Text("Clear Unpinned", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==================== ADD CUSTOM CLIP DIALOG ====================
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Custom Clip Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newClipText,
                        onValueChange = { newClipText = it },
                        placeholder = { Text("Enter text, link, code snippet, or note...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        shape = ContinuousRoundedRectangle(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newClipText.isNotBlank()) {
                            haptics.click()
                            val parsed = parseClipboardText(newClipText.trim())
                            history = listOf(parsed) + history
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Clip", fontWeight = FontWeight.Bold, color = vibrantOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ClipboardItemCard(
    item: ClipboardItem,
    onCopy: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleMask: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ContinuousRoundedRectangle(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (item.isPinned) 1.5.dp else 1.dp,
            color = if (item.isPinned) vibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Bar: Badge, Relative Time, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge Tag
                    Surface(
                        shape = ContinuousCapsule,
                        color = when (item.type) {
                            ClipType.OTP -> MaterialTheme.colorScheme.errorContainer
                            ClipType.LINK -> vibrantOrange.copy(alpha = 0.2f)
                            ClipType.CODE -> MaterialTheme.colorScheme.tertiaryContainer
                            ClipType.IMAGE -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (item.type) {
                                    ClipType.OTP -> TablerIcons.Lock
                                    ClipType.LINK -> TablerIcons.Link
                                    ClipType.CODE -> TablerIcons.Code
                                    ClipType.EMAIL -> TablerIcons.Mail
                                    ClipType.PHONE -> TablerIcons.Phone
                                    ClipType.IMAGE -> TablerIcons.Photo
                                    else -> TablerIcons.Clipboard
                                },
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = when (item.type) {
                                    ClipType.OTP -> MaterialTheme.colorScheme.error
                                    ClipType.LINK -> vibrantOrange
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            Text(
                                text = item.type.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (item.type) {
                                    ClipType.OTP -> MaterialTheme.colorScheme.error
                                    ClipType.LINK -> vibrantOrange
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    Text(
                        text = getRelativeTimeString(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Pin & Delete Header Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Pin",
                            tint = if (item.isPinned) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ==================== ADAPTIVE CONTENT BODY ====================
            when (item.type) {
                // 1. OTP / VERIFICATION CODE (MASKED / HIDDEN PRIVACY)
                ClipType.OTP -> {
                    Surface(
                        shape = ContinuousRoundedRectangle(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.ShieldCheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "PROTECTED SENSITIVE CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                IconButton(
                                    onClick = onToggleMask,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isMasked) TablerIcons.EyeOff else TablerIcons.Eye,
                                        contentDescription = "Toggle Visibility",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // OTP Code Display
                            Text(
                                text = if (item.isMasked) "••••••••" else (item.otpCode ?: item.content),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                fontFamily = GeistMono,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 2.sp
                            )

                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            // Quick Copy Code Button
                            Button(
                                onClick = { onCopy(item.otpCode ?: item.content) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ContinuousCapsule,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Copy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COPY CODE ONLY (${item.otpCode ?: ""})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 2. LINK / URL PREVIEW
                ClipType.LINK -> {
                    Surface(
                        shape = ContinuousRoundedRectangle(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(vibrantOrange.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.Link,
                                        contentDescription = null,
                                        tint = vibrantOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.extraMetadata ?: "Web Link",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = vibrantOrange,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.content))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = ContinuousCapsule
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.ExternalLink,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Link", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { onCopy(item.content) },
                                    modifier = Modifier.weight(1f),
                                    shape = ContinuousCapsule,
                                    colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.Copy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy URL", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 3. CODE SNIPPET
                ClipType.CODE -> {
                    Surface(
                        shape = ContinuousRoundedRectangle(12.dp),
                        color = Color(0xFF1E1E1E)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.extraMetadata ?: "CODE SNIPPET",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )

                                Text(
                                    text = "${item.content.length} chars",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = GeistMono,
                                color = Color(0xFFD4D4D4),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )

                            Button(
                                onClick = { onCopy(item.content) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ContinuousCapsule,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Copy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }

                // 4. IMAGE / SCREENSHOT
                ClipType.IMAGE -> {
                    val loadedBitmap = remember(item.bitmap, item.imageUri) {
                        item.bitmap ?: item.imageUri?.let { loadBitmapFromUri(context, it) }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (loadedBitmap != null) {
                            Image(
                                bitmap = loadedBitmap.asImageBitmap(),
                                contentDescription = "Clipboard Screenshot",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(ContinuousRoundedRectangle(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = ContinuousRoundedRectangle(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(TablerIcons.Photo, contentDescription = null)
                                        Text("Screenshot / Image Preview", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCopy(item.content) },
                                modifier = Modifier.weight(1f),
                                shape = ContinuousCapsule,
                                colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Copy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Text", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }

                            if (item.imageUri != null) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/*"
                                                putExtra(Intent.EXTRA_STREAM, item.imageUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot share image", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = ContinuousCapsule
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share Image", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // 5. PHONE / EMAIL / PLAIN TEXT
                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = Geist,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick Action Chips for Email or Phone
                            if (item.type == ClipType.PHONE) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.content}"))
                                        context.startActivity(intent)
                                    },
                                    shape = ContinuousCapsule
                                ) {
                                    Icon(TablerIcons.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call Number", style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (item.type == ClipType.EMAIL) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${item.content}"))
                                        context.startActivity(intent)
                                    },
                                    shape = ContinuousCapsule
                                ) {
                                    Icon(TablerIcons.Mail, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send Email", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Text(
                                    text = "${item.content.split("\\s+".toRegex()).size} words • ${item.content.length} chars",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Button(
                                onClick = { onCopy(item.content) },
                                shape = ContinuousCapsule,
                                colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Copy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Text", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Parse text content and automatically determine ClipType
private fun parseClipboardText(text: String): ClipboardItem {
    // 1. Check for OTP / Sensitive Codes (e.g. "code is 849201", "OTP: 123456", 4-8 digit standalone numbers)
    val otpPattern = Pattern.compile("(?i)(?:code|otp|verification|pin|passcode|login)?\\s*:?\\s*(\\b\\d{4,8}\\b)")
    val otpMatcher = otpPattern.matcher(text)
    if (otpMatcher.find()) {
        val extractedCode = otpMatcher.group(1)
        return ClipboardItem(
            content = text,
            type = ClipType.OTP,
            isMasked = true,
            otpCode = extractedCode,
            extraMetadata = "SENSITIVE VERIFICATION CODE"
        )
    }

    // 2. Check for URLs / Links
    if (Patterns.WEB_URL.matcher(text).matches() || text.startsWith("http://") || text.startsWith("https://") || text.startsWith("www.")) {
        val domain = try {
            val uri = Uri.parse(if (!text.startsWith("http")) "https://$text" else text)
            uri.host ?: "Web Link"
        } catch (e: Exception) {
            "Web Link"
        }
        return ClipboardItem(
            content = text,
            type = ClipType.LINK,
            extraMetadata = domain
        )
    }

    // 3. Check for Email Address
    if (Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
        return ClipboardItem(
            content = text,
            type = ClipType.EMAIL,
            extraMetadata = "EMAIL ADDRESS"
        )
    }

    // 4. Check for Phone Number
    if (Patterns.PHONE.matcher(text).matches() && text.count { it.isDigit() } in 7..15) {
        return ClipboardItem(
            content = text,
            type = ClipType.PHONE,
            extraMetadata = "PHONE NUMBER"
        )
    }

    // 5. Check for Code Snippet
    val codeKeywords = listOf("fun ", "val ", "var ", "class ", "import ", "function", "const ", "let ", "def ", "{", "}", "SELECT ", "WHERE ", "<html>")
    if (codeKeywords.any { text.contains(it) } || text.lines().size > 3) {
        return ClipboardItem(
            content = text,
            type = ClipType.CODE,
            extraMetadata = if (text.contains("fun ") || text.contains("val ")) "Kotlin Code" else "Code Snippet"
        )
    }

    // Default: Plain Text
    return ClipboardItem(
        content = text,
        type = ClipType.TEXT
    )
}

// Initial Sample Clips showcasing rich features
private fun getInitialSampleClips(): List<ClipboardItem> {
    return listOf(
        ClipboardItem(
            content = "Your Mono verification passcode is 849201. Valid for 5 minutes.",
            type = ClipType.OTP,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 2, // 2 mins ago
            isPinned = true,
            isMasked = true,
            otpCode = "849201",
            extraMetadata = "SENSITIVE VERIFICATION CODE"
        ),
        ClipboardItem(
            content = "https://github.com/android/architecture-components-samples",
            type = ClipType.LINK,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 15, // 15 mins ago
            isPinned = true,
            extraMetadata = "github.com"
        ),
        ClipboardItem(
            content = "val localDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())\nprintln(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))",
            type = ClipType.CODE,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 45, // 45 mins ago
            extraMetadata = "Kotlin Code"
        ),
        ClipboardItem(
            content = "support@awras.com",
            type = ClipType.EMAIL,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 120, // 2 hours ago
            extraMetadata = "EMAIL ADDRESS"
        ),
        ClipboardItem(
            content = "+1 (555) 234-5678",
            type = ClipType.PHONE,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 240, // 4 hours ago
            extraMetadata = "PHONE NUMBER"
        ),
        ClipboardItem(
            content = "Project sprint review meeting is scheduled for Friday at 3:00 PM EST.",
            type = ClipType.TEXT,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 360 // 6 hours ago
        )
    )
}

// Query the most recent media image / screenshot from MediaStore
private fun getMostRecentMediaImage(context: Context): Pair<Uri, Bitmap>? {
    return try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val loadedBitmap = loadBitmapFromUri(context, contentUri)
                if (loadedBitmap != null) {
                    return Pair(contentUri, loadedBitmap)
                }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

// Load image bitmap from Uri
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e2: Exception) {
            null
        }
    }
}

// Relative timestamp string generator
private fun getRelativeTimeString(timestamp: Long): String {
    val diffSecs = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        diffSecs < 60 -> "Just now"
        diffSecs < 3600 -> "${diffSecs / 60}m ago"
        diffSecs < 86400 -> "${diffSecs / 3600}h ago"
        else -> "${diffSecs / 86400}d ago"
    }
}
