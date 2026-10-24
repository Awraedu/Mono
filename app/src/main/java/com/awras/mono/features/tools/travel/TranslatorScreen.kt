package com.awras.mono.features.tools.travel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.google.mlkit.nl.translate.TranslateLanguage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Language
import kotlinx.coroutines.delay

data class LangItem(val code: String, val name: String)

private val SUPPORTED_LANGUAGES = listOf(
    LangItem(TranslateLanguage.ENGLISH, "English"),
    LangItem(TranslateLanguage.SPANISH, "Spanish"),
    LangItem(TranslateLanguage.FRENCH, "French"),
    LangItem(TranslateLanguage.GERMAN, "German"),
    LangItem(TranslateLanguage.ITALIAN, "Italian"),
    LangItem(TranslateLanguage.PORTUGUESE, "Portuguese"),
    LangItem(TranslateLanguage.CHINESE, "Chinese"),
    LangItem(TranslateLanguage.JAPANESE, "Japanese"),
    LangItem(TranslateLanguage.ARABIC, "Arabic"),
    LangItem(TranslateLanguage.RUSSIAN, "Russian"),
    LangItem(TranslateLanguage.HINDI, "Hindi"),
    LangItem(TranslateLanguage.KOREAN, "Korean")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var sourceLang by remember { mutableStateOf(SUPPORTED_LANGUAGES[0]) } // English
    var targetLang by remember { mutableStateOf(SUPPORTED_LANGUAGES[1]) } // Spanish

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var translatedText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Translated Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Translation"))
    }

    // Debounced automatic translation trigger when input, source, or target language changes
    LaunchedEffect(inputText.text, sourceLang, targetLang) {
        val query = inputText.text
        if (query.isBlank()) {
            translatedText = ""
            isTranslating = false
            isDownloading = false
            return@LaunchedEffect
        }

        isTranslating = true
        delay(300)

        TranslationManager.translate(
            sourceLang = sourceLang.code,
            targetLang = targetLang.code,
            text = query,
            onDownloadStarted = { isDownloading = true },
            onSuccess = { result ->
                isDownloading = false
                isTranslating = false
                haptics.success()
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
                description = "Offline Neural Translation (ML Kit)",
                icon = TablerIcons.Language,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. LANGUAGE SELECTOR BAR & SWAP BUTTON
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LangDropdown(
                        selected = sourceLang,
                        onSelected = { lang -> sourceLang = lang }
                    )

                    IconButton(
                        onClick = {
                            haptics.tick()
                            val tempLang = sourceLang
                            sourceLang = targetLang
                            targetLang = tempLang
                            if (translatedText.isNotBlank()) {
                                inputText = TextFieldValue(translatedText)
                                translatedText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = vibrantOrange)
                    }

                    LangDropdown(
                        selected = targetLang,
                        onSelected = { lang -> targetLang = lang }
                    )
                }
            }

            // 2. INPUT TEXT CARD (SOURCE)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "FROM (${sourceLang.name.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )

                    TextField(
                        value = inputText,
                        onValueChange = { tfv: TextFieldValue ->
                            inputText = tfv
                        },
                        placeholder = { Text("Type text to translate...") },
                        trailingIcon = if (inputText.text.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    inputText = TextFieldValue("")
                                    translatedText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedIndicatorColor = vibrantOrange,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            // 3. OUTPUT TEXT CARD (TRANSLATED)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TO (${targetLang.name.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = vibrantOrange,
                            letterSpacing = 1.sp
                        )

                        if (translatedText.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { copyToClipboard(translatedText) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { shareText(translatedText) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = ContinuousRoundedRectangle(12.dp))
                            .padding(14.dp),
                        contentAlignment = if (isTranslating || isDownloading) Alignment.Center else Alignment.TopStart
                    ) {
                        if (isTranslating || isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = vibrantOrange,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                text = translatedText.ifBlank { "Translation will appear here..." },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (translatedText.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LangDropdown(
    selected: LangItem,
    onSelected: (LangItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = ContinuousCapsule,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SUPPORTED_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.name, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onSelected(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}