package com.awras.mono.features.tools.finance

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
import com.awras.mono.ui.theme.GeistMono
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartPie
import java.text.DecimalFormat
import kotlin.math.pow

private enum class ActiveRoiInput {
    INITIAL_INVESTMENT, FINAL_AMOUNT, DURATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoiCalculatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var activeInput by remember { mutableStateOf(ActiveRoiInput.INITIAL_INVESTMENT) }

    var initialInvestmentStr by remember { mutableStateOf("10000") }
    var finalAmountStr by remember { mutableStateOf("25000") }
    var durationStr by remember { mutableStateOf("5") }
    var isDurationInYears by remember { mutableStateOf(true) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val percentFormat = remember { DecimalFormat("#,##0.0#") }

    val initialVal = initialInvestmentStr.toDoubleOrNull() ?: 0.0
    val finalVal = finalAmountStr.toDoubleOrNull() ?: 0.0
    val durationVal = (durationStr.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1)

    val durationInYears = if (isDurationInYears) durationVal else durationVal / 12.0

    // Calculations
    val netProfit = finalVal - initialVal
    val roiPercent = if (initialVal > 0) (netProfit / initialVal) * 100.0 else 0.0

    // Annualized Compound ROI: ((Final / Initial) ^ (1 / Years)) - 1
    val annualizedRoiPercent = if (initialVal > 0 && finalVal > 0 && durationInYears > 0) {
        (((finalVal / initialVal).pow(1.0 / durationInYears)) - 1.0) * 100.0
    } else 0.0

    fun handleKeypadInput(key: String) {
        haptics.click()
        when (activeInput) {
            ActiveRoiInput.INITIAL_INVESTMENT -> initialInvestmentStr = updateInputString(initialInvestmentStr, key)
            ActiveRoiInput.FINAL_AMOUNT -> finalAmountStr = updateInputString(finalAmountStr, key)
            ActiveRoiInput.DURATION -> durationStr = updateInputString(durationStr, key)
        }
    }

    fun copyRoiSummary() {
        val summary = "Initial: ${currencyFormat.format(initialVal)}\nFinal: ${currencyFormat.format(finalVal)}\nNet Profit: ${currencyFormat.format(netProfit)}\nROI: ${percentFormat.format(roiPercent)}%\nAnnualized ROI: ${percentFormat.format(annualizedRoiPercent)}%/yr"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ROI Summary", summary)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied ROI summary to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "ROI Calc",
                description = "Return on Investment & Yield Engine",
                icon = TablerIcons.ChartPie,
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
            // ==================== 1. ROI HERO SUMMARY CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "TOTAL RETURN (ROI)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { copyRoiSummary() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Summary",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Main ROI Percentage
                    Text(
                        text = "${if (roiPercent >= 0) "+" else ""}${percentFormat.format(roiPercent)}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        color = if (roiPercent >= 0) vibrantOrange else MaterialTheme.colorScheme.error,
                        letterSpacing = (-1).sp
                    )

                    // Secondary Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ContinuousCapsule,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "PROFIT: ${currencyFormat.format(netProfit)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (netProfit >= 0) vibrantOrange else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "${percentFormat.format(annualizedRoiPercent)}% / yr",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 2. INPUT CARDS ====================
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
                    // Row 1: Initial Investment & Final Return
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputFieldCard(
                            title = "INITIAL INVESTMENT ($)",
                            valueDisplay = "$$initialInvestmentStr",
                            isSelected = activeInput == ActiveRoiInput.INITIAL_INVESTMENT,
                            onClick = {
                                haptics.click()
                                activeInput = ActiveRoiInput.INITIAL_INVESTMENT
                            },
                            modifier = Modifier.weight(1f)
                        )

                        InputFieldCard(
                            title = "FINAL VALUE ($)",
                            valueDisplay = "$$finalAmountStr",
                            isSelected = activeInput == ActiveRoiInput.FINAL_AMOUNT,
                            onClick = {
                                haptics.click()
                                activeInput = ActiveRoiInput.FINAL_AMOUNT
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                    // Row 2: Duration & Unit Toggle
                    Surface(
                        onClick = { activeInput = ActiveRoiInput.DURATION },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ContinuousRoundedRectangle(12.dp),
                        color = if (activeInput == ActiveRoiInput.DURATION) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (activeInput == ActiveRoiInput.DURATION) vibrantOrange else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INVESTMENT DURATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )

                                Text(
                                    text = "$durationStr ${if (isDurationInYears) "Years" else "Months"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GeistMono,
                                    color = if (activeInput == ActiveRoiInput.DURATION) vibrantOrange else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                onClick = {
                                    haptics.click()
                                    isDurationInYears = !isDurationInYears
                                },
                                shape = ContinuousCapsule,
                                color = vibrantOrange
                            ) {
                                Text(
                                    text = if (isDurationInYears) "YEARS" else "MONTHS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // PUSH NUMPAD ALL THE WAY DOWN TO THE BOTTOM OF THE SCREEN
            Spacer(modifier = Modifier.weight(1f))

            // ==================== 3. EVEN SPACIOUS NUMPAD GRID (ALIGNED AT BOTTOM) ====================
            val keypadGrid = listOf(
                "7", "8", "9", "AC",
                "4", "5", "6", "DEL",
                "1", "2", "3", ".",
                "00", "0", "NEXT", "="
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
                                activeInput = when (activeInput) {
                                    ActiveRoiInput.INITIAL_INVESTMENT -> ActiveRoiInput.FINAL_AMOUNT
                                    ActiveRoiInput.FINAL_AMOUNT -> ActiveRoiInput.DURATION
                                    ActiveRoiInput.DURATION -> ActiveRoiInput.INITIAL_INVESTMENT
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
private fun InputFieldCard(
    title: String,
    valueDisplay: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                text = valueDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = GeistMono,
                color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun updateInputString(currentStr: String, key: String): String {
    return when (key) {
        "AC" -> "0"
        "DEL" -> if (currentStr.length > 1) currentStr.substring(0, currentStr.length - 1) else "0"
        "." -> if (!currentStr.contains(".")) "$currentStr." else currentStr
        "00" -> if (currentStr != "0" && currentStr.length < 8) "${currentStr}00" else currentStr
        else -> if (currentStr == "0") key else if (currentStr.length < 9) currentStr + key else currentStr
    }
}
