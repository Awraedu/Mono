package com.awras.mono.features.tools.maths

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Binary
import java.math.BigInteger

enum class NumeralBase(val displayName: String, val radix: Int, val prefix: String) {
    DEC("Decimal", 10, ""),
    HEX("Hexadecimal", 16, "0x"),
    BIN("Binary", 2, "0b"),
    OCT("Octal", 8, "0o")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseConverterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var activeBase by remember { mutableStateOf(NumeralBase.DEC) }

    // BigInteger value storing the true numeric value
    var currentValue by remember { mutableStateOf(BigInteger.valueOf(255L)) }

    // Active Input String
    var rawInputString by remember(activeBase, currentValue) {
        mutableStateOf(currentValue.toString(activeBase.radix).uppercase())
    }

    val scrollState = rememberScrollState()

    // Formatted Base Representations
    val decString = remember(currentValue) { currentValue.toString(10) }
    val hexString = remember(currentValue) { currentValue.toString(16).uppercase() }
    val octString = remember(currentValue) { currentValue.toString(8) }
    val binString = remember(currentValue) {
        val raw = currentValue.toString(2)
        // Group in 4s (nibbles)
        raw.padStart(((raw.length + 3) / 4) * 4, '0')
            .chunked(4)
            .joinToString(" ")
    }

    fun handleKeyInput(key: String) {
        haptics.click()
        when (key) {
            "AC" -> {
                currentValue = BigInteger.ZERO
                rawInputString = "0"
            }
            "DEL" -> {
                if (rawInputString.length > 1) {
                    rawInputString = rawInputString.substring(0, rawInputString.length - 1)
                } else {
                    rawInputString = "0"
                }
                try {
                    currentValue = BigInteger(rawInputString, activeBase.radix)
                } catch (e: Exception) {
                    currentValue = BigInteger.ZERO
                }
            }
            "NOT" -> {
                currentValue = currentValue.not()
            }
            "<<" -> {
                currentValue = currentValue.shiftLeft(1)
            }
            ">>" -> {
                currentValue = currentValue.shiftRight(1)
            }
            else -> {
                val newStr = if (rawInputString == "0") key else rawInputString + key
                try {
                    val parsed = BigInteger(newStr, activeBase.radix)
                    currentValue = parsed
                    rawInputString = newStr
                } catch (e: Exception) {
                    // Invalid digit or overflow
                }
            }
        }
    }

    fun copyToClipboard(label: String, valStr: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, valStr)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied $label: $valStr", Toast.LENGTH_SHORT).show()
    }

    fun isKeyEnabled(key: String): Boolean {
        if (key in listOf("AC", "DEL", "NOT", "<<", ">>")) return true
        val digitValue = key.toIntOrNull(16) ?: return false
        return digitValue < activeBase.radix
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Base Converter",
                description = "HEX, BIN, DEC, OCT Radix & Bitwise Engine",
                icon = TablerIcons.Binary,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== 1. FOUR SYNCHRONIZED RADIX CARDS ====================
            listOf(
                NumeralBase.DEC to decString,
                NumeralBase.HEX to hexString,
                NumeralBase.BIN to binString,
                NumeralBase.OCT to octString
            ).forEach { (base, valStr) ->
                val isActive = activeBase == base
                Surface(
                    onClick = {
                        haptics.click()
                        activeBase = base
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(18.dp),
                    color = if (isActive) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isActive) vibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = ContinuousCapsule,
                                    color = if (isActive) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = base.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = base.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Text(
                                text = valStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = com.awras.mono.ui.theme.GeistMono,
                                color = if (isActive) vibrantOrange else MaterialTheme.colorScheme.onSurface,
                                fontSize = if (valStr.length > 20) 14.sp else 18.sp
                            )
                        }

                        IconButton(
                            onClick = { copyToClipboard(base.name, valStr) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ==================== 2. INTERACTIVE 8-BIT MATRIX ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "8-BIT MATRIX (LOW BYTE)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = vibrantOrange,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (7 downTo 0).forEach { bitIdx ->
                            val isBitSet = currentValue.testBit(bitIdx)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "b$bitIdx",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )

                                Surface(
                                    onClick = {
                                        haptics.tick()
                                        currentValue = currentValue.flipBit(bitIdx)
                                    },
                                    shape = ContinuousRoundedRectangle(10.dp),
                                    color = if (isBitSet) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = if (isBitSet) "1" else "0",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isBitSet) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 3. ADAPTIVE KEYPAD GRID ====================
            val keypadGrid = listOf(
                "A", "B", "C", "D", "E", "F",
                "7", "8", "9", "NOT", "<<", ">>",
                "4", "5", "6", "1", "2", "3",
                "0", "AC", "DEL"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(keypadGrid) { key ->
                    val enabled = isKeyEnabled(key)
                    val isAction = key in listOf("AC", "DEL")
                    val isBitOp = key in listOf("NOT", "<<", ">>")

                    Surface(
                        onClick = { if (enabled) handleKeyInput(key) },
                        shape = ContinuousRoundedRectangle(12.dp),
                        color = when {
                            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            isBitOp -> vibrantOrange.copy(alpha = 0.2f)
                            isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Transparent),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    isBitOp -> vibrantOrange
                                    isAction -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
