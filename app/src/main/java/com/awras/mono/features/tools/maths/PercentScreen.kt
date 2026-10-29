package com.awras.mono.features.tools.maths

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import compose.icons.tablericons.Percentage
import java.text.DecimalFormat
import kotlin.math.abs

enum class PercentMode(val displayName: String) {
    PERCENT_OF("X% OF Y"),
    IS_WHAT_PERCENT("X IS % OF Y"),
    PERCENT_CHANGE("% CHANGE"),
    RATIO_PROPORTION("RATIO A:B")
}

private enum class ActivePercentField {
    VAL1, VAL2, VAL3
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PercentScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var selectedMode by remember { mutableStateOf(PercentMode.PERCENT_OF) }
    var activeField by remember { mutableStateOf(ActivePercentField.VAL1) }

    var input1Str by remember { mutableStateOf("15") }
    var input2Str by remember { mutableStateOf("250") }
    var input3Str by remember { mutableStateOf("1920") } // Used for Ratio (A:B = C:X)

    val format = remember { DecimalFormat("#,##0.########") }

    val val1 = input1Str.toDoubleOrNull() ?: 0.0
    val val2 = input2Str.toDoubleOrNull() ?: 0.0
    val val3 = input3Str.toDoubleOrNull() ?: 0.0

    // Mode-specific calculations
    val (resultText, labelText) = remember(selectedMode, val1, val2, val3) {
        when (selectedMode) {
            PercentMode.PERCENT_OF -> {
                val res = (val1 / 100.0) * val2
                Pair(format.format(res), "${format.format(val1)}% of ${format.format(val2)}")
            }
            PercentMode.IS_WHAT_PERCENT -> {
                val res = if (val2 != 0.0) (val1 / val2) * 100.0 else 0.0
                Pair("${format.format(res)}%", "${format.format(val1)} is what % of ${format.format(val2)}")
            }
            PercentMode.PERCENT_CHANGE -> {
                val diff = val2 - val1
                val pctChange = if (val1 != 0.0) (diff / abs(val1)) * 100.0 else 0.0
                val sign = if (pctChange > 0) "+" else ""
                val type = if (pctChange >= 0) "Increase" else "Decrease"
                Pair("$sign${format.format(pctChange)}%", "Change from ${format.format(val1)} to ${format.format(val2)} ($type)")
            }
            PercentMode.RATIO_PROPORTION -> {
                // A : B = C : X  => X = (B * C) / A
                val res = if (val1 != 0.0) (val2 * val3) / val1 else 0.0
                Pair(format.format(res), "${format.format(val1)} : ${format.format(val2)} = ${format.format(val3)} : X")
            }
        }
    }

    fun handleKeypadInput(key: String) {
        haptics.click()
        val currentStr = when (activeField) {
            ActivePercentField.VAL1 -> input1Str
            ActivePercentField.VAL2 -> input2Str
            ActivePercentField.VAL3 -> input3Str
        }

        val updated = when (key) {
            "AC" -> "0"
            "DEL" -> if (currentStr.length > 1) currentStr.substring(0, currentStr.length - 1) else "0"
            "±" -> if (currentStr.startsWith("-")) currentStr.substring(1) else if (currentStr != "0") "-$currentStr" else currentStr
            "." -> if (!currentStr.contains(".")) "$currentStr." else currentStr
            else -> if (currentStr == "0") key else if (currentStr.length < 9) currentStr + key else currentStr
        }

        when (activeField) {
            ActivePercentField.VAL1 -> input1Str = updated
            ActivePercentField.VAL2 -> input2Str = updated
            ActivePercentField.VAL3 -> input3Str = updated
        }
    }

    fun copyResult() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Percent Result", resultText)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied result: $resultText", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Percent",
                description = "Ratio, Proportions & Percentage Change Engine",
                icon = TablerIcons.Percentage,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // ==================== 1. MODE SELECTOR PILLS ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(PercentMode.entries.toTypedArray()) { mode ->
                    val isSelected = mode == selectedMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.click()
                            selectedMode = mode
                        },
                        label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 2. RESULT READOUT HERO CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                text = selectedMode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = { copyResult() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Result",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = com.awras.mono.ui.theme.GeistMono,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 3. INPUT FIELDS GRID ====================
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
                    when (selectedMode) {
                        PercentMode.PERCENT_OF -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputBox(
                                    title = "PERCENT (X)",
                                    value = "$input1Str%",
                                    isSelected = activeField == ActivePercentField.VAL1,
                                    onClick = { activeField = ActivePercentField.VAL1 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("OF", fontWeight = FontWeight.Black, color = vibrantOrange)
                                InputBox(
                                    title = "TOTAL (Y)",
                                    value = input2Str,
                                    isSelected = activeField == ActivePercentField.VAL2,
                                    onClick = { activeField = ActivePercentField.VAL2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        PercentMode.IS_WHAT_PERCENT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputBox(
                                    title = "PART (X)",
                                    value = input1Str,
                                    isSelected = activeField == ActivePercentField.VAL1,
                                    onClick = { activeField = ActivePercentField.VAL1 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("IS % OF", fontWeight = FontWeight.Black, color = vibrantOrange, fontSize = 12.sp)
                                InputBox(
                                    title = "WHOLE (Y)",
                                    value = input2Str,
                                    isSelected = activeField == ActivePercentField.VAL2,
                                    onClick = { activeField = ActivePercentField.VAL2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        PercentMode.PERCENT_CHANGE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputBox(
                                    title = "FROM (X)",
                                    value = input1Str,
                                    isSelected = activeField == ActivePercentField.VAL1,
                                    onClick = { activeField = ActivePercentField.VAL1 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("➔", fontWeight = FontWeight.Black, color = vibrantOrange, fontSize = 16.sp)
                                InputBox(
                                    title = "TO (Y)",
                                    value = input2Str,
                                    isSelected = activeField == ActivePercentField.VAL2,
                                    onClick = { activeField = ActivePercentField.VAL2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        PercentMode.RATIO_PROPORTION -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputBox(
                                    title = "A",
                                    value = input1Str,
                                    isSelected = activeField == ActivePercentField.VAL1,
                                    onClick = { activeField = ActivePercentField.VAL1 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(":", fontWeight = FontWeight.Black, color = vibrantOrange)
                                InputBox(
                                    title = "B",
                                    value = input2Str,
                                    isSelected = activeField == ActivePercentField.VAL2,
                                    onClick = { activeField = ActivePercentField.VAL2 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text("=", fontWeight = FontWeight.Black, color = vibrantOrange)
                                InputBox(
                                    title = "C",
                                    value = input3Str,
                                    isSelected = activeField == ActivePercentField.VAL3,
                                    onClick = { activeField = ActivePercentField.VAL3 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // PUSH NUMPAD ALL THE WAY DOWN TO THE BOTTOM OF THE SCREEN
            Spacer(modifier = Modifier.weight(1f))

            // ==================== 4. EVEN SPACIOUS NUMPAD (ALIGNED AT BOTTOM) ====================
            val keypadGrid = listOf(
                "7", "8", "9", "AC",
                "4", "5", "6", "DEL",
                "1", "2", "3", "±",
                ".", "0", "NEXT", "="
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(keypadGrid) { key ->
                    val isAction = key in listOf("AC", "DEL")
                    val isNext = key in listOf("NEXT", "=")

                    Surface(
                        onClick = {
                            if (key == "NEXT" || key == "=") {
                                haptics.click()
                                activeField = when (activeField) {
                                    ActivePercentField.VAL1 -> ActivePercentField.VAL2
                                    ActivePercentField.VAL2 -> if (selectedMode == PercentMode.RATIO_PROPORTION) ActivePercentField.VAL3 else ActivePercentField.VAL1
                                    ActivePercentField.VAL3 -> ActivePercentField.VAL1
                                }
                            } else {
                                handleKeypadInput(key)
                            }
                        },
                        shape = ContinuousRoundedRectangle(16.dp),
                        color = when {
                            isNext -> vibrantOrange
                            isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(1.dp, if (isNext) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.height(56.dp)
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
                                    isNext -> Color.White
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

@Composable
private fun InputBox(
    title: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ContinuousRoundedRectangle(12.dp),
        color = if (isSelected) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isSelected) vibrantOrange else Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = com.awras.mono.ui.theme.GeistMono,
                color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
