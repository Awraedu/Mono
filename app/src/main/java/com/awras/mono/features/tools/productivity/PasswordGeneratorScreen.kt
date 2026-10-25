package com.awras.mono.features.tools.productivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.security.SecureRandom
import kotlin.math.log2

enum class PassgenMode(val displayName: String) {
    RANDOM("Random"),
    PASSPHRASE("Passphrase"),
    NUMERIC("PIN / Numeric"),
    PRONOUNCEABLE("Phonetic")
}

data class GeneratedPasswordItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val password: String,
    val strengthLabel: String,
    val entropyBits: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isMasked: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // Generator Options State
    var mode by remember { mutableStateOf(PassgenMode.RANDOM) }
    var passwordLength by remember { mutableFloatStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }

    // Current Generated Password & Visibility State
    var generatedPassword by remember { mutableStateOf("") }
    var isMasked by remember { mutableStateOf(false) }

    // History Log
    var passwordHistory by remember { mutableStateOf(listOf<GeneratedPasswordItem>()) }

    // Core Password Generator logic
    fun generateNewPassword() {
        haptics.click()
        val length = passwordLength.toInt()
        val newPassword = generatePassword(
            mode = mode,
            length = length,
            useUpper = includeUppercase,
            useLower = includeLowercase,
            useDigits = includeNumbers,
            useSymbols = includeSymbols,
            noAmbiguous = excludeAmbiguous
        )
        generatedPassword = newPassword

        val (entropy, strengthLabel, _) = evaluatePasswordStrength(newPassword)
        val newItem = GeneratedPasswordItem(
            password = newPassword,
            strengthLabel = strengthLabel,
            entropyBits = entropy
        )
        passwordHistory = (listOf(newItem) + passwordHistory).take(20)
    }

    // Generate password on launch or when parameters change
    LaunchedEffect(mode, passwordLength, includeUppercase, includeLowercase, includeNumbers, includeSymbols, excludeAmbiguous) {
        generateNewPassword()
    }

    // Copy to clipboard helper
    fun copyToClipboard(text: String) {
        haptics.click()
        val clip = ClipData.newPlainText("Generated Password", text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val (entropyBits, strengthLabel, crackTimeEstimate) = remember(generatedPassword) {
        evaluatePasswordStrength(generatedPassword)
    }

    val strengthColor = when {
        entropyBits < 40 -> MaterialTheme.colorScheme.error
        entropyBits < 70 -> Color(0xFFFFB74D) // Amber / Warning
        else -> vibrantOrange
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "PassGen",
                description = "Cryptographically Secure Password & Key Generator",
                icon = TablerIcons.Lock,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Refresh,
                onRightActionClick = {
                    generateNewPassword()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. HERO DISPLAY CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: Mode Badge & Visibility Toggle
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
                                text = mode.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                isMasked = !isMasked
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isMasked) TablerIcons.EyeOff else TablerIcons.Eye,
                                contentDescription = "Toggle Visibility",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Main Password Display
                    Surface(
                        shape = ContinuousRoundedRectangle(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, strengthColor.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isMasked) "••••••••••••••••" else generatedPassword,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GeistMono,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Strength Progress Bar & Security Metrics
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(strengthColor)
                                )
                                Text(
                                    text = strengthLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor
                                )
                            }

                            Text(
                                text = "$entropyBits BITS ENTROPY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (entropyBits / 100f).coerceIn(0.1f, 1.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(ContinuousCapsule),
                            color = strengthColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "Estimated crack time: $crackTimeEstimate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { generateNewPassword() },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule
                        ) {
                            Icon(
                                imageVector = TablerIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-roll", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { copyToClipboard(generatedPassword) },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule,
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                        ) {
                            Icon(
                                imageVector = TablerIcons.Copy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Password", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // ==================== 2. GENERATOR MODES & LENGTH ====================
            // Mode Segment Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PassgenMode.entries.toTypedArray()) { itemMode ->
                    val isSelected = mode == itemMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.tick()
                            mode = itemMode
                        },
                        label = {
                            Text(
                                text = itemMode.displayName,
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

            // Password Length Control Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(18.dp),
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
                            text = "PASSWORD LENGTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "${passwordLength.toInt()} CHARS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = GeistMono,
                            color = vibrantOrange
                        )
                    }

                    Slider(
                        value = passwordLength,
                        onValueChange = {
                            passwordLength = it
                        },
                        valueRange = 4f..64f,
                        colors = SliderDefaults.colors(
                            thumbColor = vibrantOrange,
                            activeTrackColor = vibrantOrange,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Quick Length Preset Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(8, 12, 16, 24, 32).forEach { preset ->
                            val isPresetSelected = passwordLength.toInt() == preset
                            Surface(
                                onClick = {
                                    haptics.click()
                                    passwordLength = preset.toFloat()
                                },
                                shape = ContinuousCapsule,
                                color = if (isPresetSelected) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$preset",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPresetSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 3. CHARACTER SET TOGGLES ====================
            if (mode == PassgenMode.RANDOM || mode == PassgenMode.PRONOUNCEABLE) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CHARACTER SETS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        ToggleRow("Uppercase Letters (A-Z)", includeUppercase) {
                            if (!it && !includeLowercase && !includeNumbers && !includeSymbols) return@ToggleRow
                            includeUppercase = it
                        }
                        ToggleRow("Lowercase Letters (a-z)", includeLowercase) {
                            if (!it && !includeUppercase && !includeNumbers && !includeSymbols) return@ToggleRow
                            includeLowercase = it
                        }
                        ToggleRow("Numbers (0-9)", includeNumbers) {
                            if (!it && !includeUppercase && !includeLowercase && !includeSymbols) return@ToggleRow
                            includeNumbers = it
                        }
                        ToggleRow("Special Symbols (!@#$)", includeSymbols) {
                            if (!it && !includeUppercase && !includeLowercase && !includeNumbers) return@ToggleRow
                            includeSymbols = it
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        ToggleRow("Avoid Ambiguous (1, l, I, 0, O)", excludeAmbiguous) {
                            excludeAmbiguous = it
                        }
                    }
                }
            }

            // ==================== 4. SESSION HISTORY LOG ====================
            if (passwordHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SESSION VAULT LOG (${passwordHistory.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )

                    TextButton(onClick = { passwordHistory = emptyList() }) {
                        Text("Clear Log", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    passwordHistory.forEach { item ->
                        var isItemMasked by remember(item.id) { mutableStateOf(true) }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ContinuousRoundedRectangle(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isItemMasked) "••••••••••••" else item.password,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = GeistMono,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.strengthLabel} • ${item.entropyBits} bits",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { isItemMasked = !isItemMasked },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isItemMasked) TablerIcons.EyeOff else TablerIcons.Eye,
                                            contentDescription = "Toggle",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { copyToClipboard(item.password) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = TablerIcons.Copy,
                                            contentDescription = "Copy",
                                            tint = vibrantOrange,
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
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalTactileFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.tick()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

// Secure Cryptographic Password Generator
private fun generatePassword(
    mode: PassgenMode,
    length: Int,
    useUpper: Boolean,
    useLower: Boolean,
    useDigits: Boolean,
    useSymbols: Boolean,
    noAmbiguous: Boolean
): String {
    val random = SecureRandom()

    when (mode) {
        PassgenMode.PASSPHRASE -> {
            val words = listOf(
                "galaxy", "falcon", "orbit", "shield", "beacon", "cipher", "starlight", "matrix",
                "quantum", "zenith", "vortex", "nexus", "aurora", "nebula", "pulsar", "vector",
                "titan", "eclipse", "horizon", "solaris", "hyper", "cyber", "prism", "spectre"
            )
            val count = (length / 5).coerceIn(3, 8)
            val selectedWords = (1..count).map { words[random.nextInt(words.size)] }
            val num = random.nextInt(90) + 10
            return selectedWords.joinToString("-") + "-$num"
        }

        PassgenMode.NUMERIC -> {
            val digits = "0123456789"
            return (1..length).map { digits[random.nextInt(digits.length)] }.joinToString("")
        }

        PassgenMode.PRONOUNCEABLE -> {
            val vowels = "aeiou"
            val consonants = "bcdfghjklmnpqrstvwxyz"
            val sb = StringBuilder()
            var isVowel = random.nextBoolean()
            for (i in 1..length) {
                val pool = if (isVowel) vowels else consonants
                var char = pool[random.nextInt(pool.length)]
                if (i == 1 && random.nextBoolean()) char = char.uppercaseChar()
                sb.append(char)
                isVowel = !isVowel
            }
            if (useDigits) sb.append(random.nextInt(90) + 10)
            return sb.toString()
        }

        PassgenMode.RANDOM -> {
            val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val lower = "abcdefghijklmnopqrstuvwxyz"
            val digits = "0123456789"
            val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
            val ambiguous = "1lI0Oo"

            var pool = ""
            if (useUpper) pool += upper
            if (useLower) pool += lower
            if (useDigits) pool += digits
            if (useSymbols) pool += symbols

            if (noAmbiguous) {
                pool = pool.filterNot { it in ambiguous }
            }

            if (pool.isEmpty()) pool = lower

            val passChars = mutableListOf<Char>()

            // Guarantee at least 1 character from each enabled set
            fun filterSet(str: String): String = if (noAmbiguous) str.filterNot { it in ambiguous } else str

            if (useUpper) passChars.add(filterSet(upper).random(random))
            if (useLower) passChars.add(filterSet(lower).random(random))
            if (useDigits) passChars.add(filterSet(digits).random(random))
            if (useSymbols) passChars.add(filterSet(symbols).random(random))

            while (passChars.size < length) {
                passChars.add(pool[random.nextInt(pool.length)])
            }

            passChars.shuffle(random)
            return passChars.joinToString("")
        }
    }
}

// Password Strength & Entropy Evaluator
private fun evaluatePasswordStrength(password: String): Triple<Int, String, String> {
    if (password.isEmpty()) return Triple(0, "Empty", "Instant")

    var poolSize = 0
    if (password.any { it.isUpperCase() }) poolSize += 26
    if (password.any { it.isLowerCase() }) poolSize += 26
    if (password.any { it.isDigit() }) poolSize += 10
    if (password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) poolSize += 32
    if (poolSize == 0) poolSize = 26

    val entropy = (password.length * log2(poolSize.toDouble())).toInt()

    val label = when {
        entropy < 35 -> "Very Weak"
        entropy < 50 -> "Weak"
        entropy < 70 -> "Good"
        entropy < 90 -> "Strong"
        else -> "Unbreakable"
    }

    val crackTime = when {
        entropy < 30 -> "Instant"
        entropy < 45 -> "Few minutes"
        entropy < 60 -> "Few days"
        entropy < 80 -> "Several years"
        else -> "Centuries"
    }

    return Triple(entropy, label, crackTime)
}

// Helper to select random char from String using SecureRandom
private fun String.random(random: SecureRandom): Char {
    return this[random.nextInt(this.length)]
}
