package com.awras.mono.features.tools.travel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.google.mlkit.nl.translate.TranslateLanguage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.delay
import java.util.Locale

private enum class LanguageSheetMode {
    SOURCE, TARGET
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Downloaded models state
    var downloadedCodes by remember { mutableStateOf<Set<String>>(setOf(TranslateLanguage.ENGLISH)) }
    var downloadingCodes by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Fetch initial downloaded language models
    LaunchedEffect(Unit) {
        TranslationManager.getDownloadedLanguageCodes(
            onSuccess = { codes ->
                downloadedCodes = codes + TranslateLanguage.ENGLISH
            }
        )
    }

    // Default Languages
    val allSupportedLangs = remember(downloadedCodes) {
        TranslationManager.getAllSupportedLanguages(downloadedCodes)
    }

    var sourceLang by remember {
        mutableStateOf(allSupportedLangs.find { it.code == TranslateLanguage.ENGLISH } ?: LangItem(TranslateLanguage.ENGLISH, "English"))
    }
    var targetLang by remember {
        mutableStateOf(allSupportedLangs.find { it.code == TranslateLanguage.SPANISH } ?: LangItem(TranslateLanguage.SPANISH, "Spanish"))
    }

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var translatedText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }

    // UI Flip State for Top Half (Face-to-Face reading)
    var isTopFlipped by remember { mutableStateOf(true) }

    // Bottom Sheet State
    var showLanguageSheet by remember { mutableStateOf(false) }
    var sheetMode by remember { mutableStateOf(LanguageSheetMode.TARGET) }
    var sheetSearchQuery by remember { mutableStateOf("") }

    // Text To Speech (Device Engine)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun speakText(text: String, langCode: String) {
        if (tts != null && isTtsReady && text.isNotBlank()) {
            val locale = Locale.forLanguageTag(langCode)
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TranslatorTTS")
        }
    }

    // In-App Speech Recognizer (No popup dialog, integrated seamlessly)
    var isListening by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            haptics.click()
            // Granted - proceed
        } else {
            Toast.makeText(context, "Microphone permission is required for speech input", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(context) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun toggleSpeechRecognition() {
        haptics.click()
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                        Toast.makeText(context, "Speech input error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        inputText = TextFieldValue(matches[0])
                        haptics.success()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        inputText = TextFieldValue(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLang.code)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            isListening = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Voice input not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Translated Text", text)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Translation"))
    }

    // Translation Debounce Effect
    LaunchedEffect(inputText.text, sourceLang.code, targetLang.code) {
        val query = inputText.text
        if (query.isBlank()) {
            translatedText = ""
            isTranslating = false
            isDownloading = false
            return@LaunchedEffect
        }

        isTranslating = true
        delay(250)

        TranslationManager.translate(
            sourceLang = sourceLang.code,
            targetLang = targetLang.code,
            text = query,
            onDownloadStarted = { isDownloading = true },
            onSuccess = { result ->
                isDownloading = false
                isTranslating = false
                haptics.tick()
                translatedText = result
            },
            onError = { e ->
                isDownloading = false
                isTranslating = false
                Toast.makeText(context, "Translation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Translator",
                description = "TO: ${targetLang.name.uppercase()}",
                icon = TablerIcons.Language,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Language,
                onRightActionClick = {
                    haptics.click()
                    sheetMode = LanguageSheetMode.TARGET
                    sheetSearchQuery = ""
                    showLanguageSheet = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== TOP HALF: TRANSLATED OUTPUT (FACING OPPOSITE USER) ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .graphicsLayer {
                            rotationZ = if (isTopFlipped) 180f else 0f
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Header Row (In flipped orientation, this appears at bottom to the other user)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = ContinuousCapsule,
                                color = vibrantOrange
                            ) {
                                Text(
                                    text = targetLang.name.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Rotate/Flip toggle button
                                FilledTonalIconButton(
                                    onClick = {
                                        haptics.click()
                                        isTopFlipped = !isTopFlipped
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Rotate Text",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (translatedText.isNotBlank()) {
                                    // Device Text To Speech button
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptics.click()
                                            speakText(translatedText, targetLang.code)
                                        },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak Text",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Copy button
                                    FilledTonalIconButton(
                                        onClick = { copyToClipboard(translatedText) },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share button
                                    FilledTonalIconButton(
                                        onClick = { shareText(translatedText) },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Text",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Main Translated Text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTranslating || isDownloading) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = vibrantOrange,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = if (isDownloading) "Downloading Model..." else "Translating...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Text(
                                    text = translatedText.ifBlank { "Translation will appear here upside-down for opposite person to read..." },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (translatedText.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                                    color = if (translatedText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                    fontSize = if (translatedText.length > 100) 18.sp else 22.sp
                                )
                            }
                        }

                        // Bottom Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isTopFlipped) "• FACE-TO-FACE VIEW (180° ROTATED) •" else "• NORMAL VIEW •",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // ==================== BOTTOM HALF: CURRENT USER INPUT & CONTROLS ====================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Bar: Source Language Selector & Language Swap Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source Language Button with clipped ripple
                        Surface(
                            onClick = {
                                haptics.click()
                                sheetMode = LanguageSheetMode.SOURCE
                                sheetSearchQuery = ""
                                showLanguageSheet = true
                            },
                            shape = ContinuousCapsule,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "FROM: ${sourceLang.name.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Language Swap Button
                        FilledTonalIconButton(
                            onClick = {
                                haptics.tick()
                                val temp = sourceLang
                                sourceLang = targetLang
                                targetLang = temp
                                if (translatedText.isNotBlank()) {
                                    inputText = TextFieldValue(translatedText)
                                    translatedText = ""
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = vibrantOrange)
                        }

                        if (inputText.text.isNotEmpty()) {
                            IconButton(onClick = {
                                inputText = TextFieldValue("")
                                translatedText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Input")
                            }
                        }
                    }

                    // Input Text Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (isListening) "Listening now... speak in ${sourceLang.name}" else "Type or tap voice mic to speak...",
                                    fontSize = 16.sp
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Bottom Control Row: Seamless In-App Voice Input Mic Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { toggleSpeechRecognition() },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = if (isListening) MaterialTheme.colorScheme.error else vibrantOrange
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isListening) TablerIcons.MicrophoneOff else TablerIcons.Microphone,
                                    contentDescription = "Voice Input",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== 50+ LANGUAGES MATERIAL BOTTOM SHEET ====================
    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = if (sheetMode == LanguageSheetMode.TARGET) "Select Target Language" else "Select Source Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Search Bar
                OutlinedTextField(
                    value = sheetSearchQuery,
                    onValueChange = { sheetSearchQuery = it },
                    placeholder = { Text("Search 50+ languages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = if (sheetSearchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { sheetSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = ContinuousCapsule
                )

                val currentSelected = if (sheetMode == LanguageSheetMode.TARGET) targetLang else sourceLang
                val allLangs = remember(downloadedCodes) {
                    TranslationManager.getAllSupportedLanguages(downloadedCodes)
                }

                val filteredLangs = remember(allLangs, sheetSearchQuery) {
                    if (sheetSearchQuery.isBlank()) allLangs
                    else allLangs.filter {
                        it.name.contains(sheetSearchQuery, ignoreCase = true) ||
                        it.nativeName.contains(sheetSearchQuery, ignoreCase = true) ||
                        it.code.contains(sheetSearchQuery, ignoreCase = true)
                    }
                }

                val downloadedGroup = filteredLangs.filter { it.isDownloaded }
                val availableGroup = filteredLangs.filter { !it.isDownloaded }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Group 1: Downloaded Languages
                    if (downloadedGroup.isNotEmpty()) {
                        item {
                            Text(
                                text = "DOWNLOADED LANGUAGES (OFFLINE READY)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(downloadedGroup, key = { "dl_${it.code}" }) { lang ->
                            LanguageRowItem(
                                lang = lang,
                                isSelected = lang.code == currentSelected.code,
                                isDownloading = downloadingCodes.contains(lang.code),
                                onSelect = {
                                    haptics.click()
                                    if (sheetMode == LanguageSheetMode.TARGET) {
                                        targetLang = lang
                                    } else {
                                        sourceLang = lang
                                    }
                                    showLanguageSheet = false
                                },
                                onDownloadClick = { /* Already downloaded */ }
                            )
                        }
                    }

                    // Group 2: All Available Languages
                    if (availableGroup.isNotEmpty()) {
                        item {
                            Text(
                                text = "ALL AVAILABLE LANGUAGES (${availableGroup.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(availableGroup, key = { "avail_${it.code}" }) { lang ->
                            LanguageRowItem(
                                lang = lang,
                                isSelected = lang.code == currentSelected.code,
                                isDownloading = downloadingCodes.contains(lang.code),
                                onSelect = {
                                    haptics.click()
                                    if (sheetMode == LanguageSheetMode.TARGET) {
                                        targetLang = lang
                                    } else {
                                        sourceLang = lang
                                    }
                                    showLanguageSheet = false
                                },
                                onDownloadClick = {
                                    downloadingCodes = downloadingCodes + lang.code
                                    TranslationManager.downloadModel(
                                        languageCode = lang.code,
                                        onSuccess = {
                                            downloadingCodes = downloadingCodes - lang.code
                                            haptics.success()
                                            TranslationManager.getDownloadedLanguageCodes({ codes ->
                                                downloadedCodes = codes + TranslateLanguage.ENGLISH
                                            })
                                        },
                                        onError = { e ->
                                            downloadingCodes = downloadingCodes - lang.code
                                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRowItem(
    lang: LangItem,
    isSelected: Boolean,
    isDownloading: Boolean,
    onSelect: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    Surface(
        onClick = { onSelect() },
        shape = ContinuousRoundedRectangle(12.dp),
        color = if (isSelected) vibrantOrange.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ISO Badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = lang.code.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Column {
                    Text(
                        text = lang.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface
                    )
                    if (lang.nativeName.isNotBlank()) {
                        Text(
                            text = lang.nativeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Right Action: Downloaded indicator or Download button or Spinner
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = vibrantOrange,
                    strokeWidth = 2.dp
                )
            } else if (lang.isDownloaded) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Downloaded",
                    tint = vibrantOrange,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = TablerIcons.Download,
                        contentDescription = "Download Model",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
