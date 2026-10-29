package com.awras.mono.features.tools.finance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import compose.icons.tablericons.Adjustments
import compose.icons.tablericons.Receipt2
import java.text.DecimalFormat
import kotlin.math.ceil

private enum class ActiveTipInput {
    BILL_AMOUNT, TIP_PERCENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipCalculatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var activeInput by remember { mutableStateOf(ActiveTipInput.BILL_AMOUNT) }

    var billAmountStr by remember { mutableStateOf("100") }
    var tipPercentStr by remember { mutableStateOf("18") }
    var serviceTaxPercentStr by remember { mutableStateOf("0") }
    var peopleCount by remember { mutableIntStateOf(1) }
    var isRoundUpEnabled by remember { mutableStateOf(false) }

    var showOptionsSheet by remember { mutableStateOf(false) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val percentFormat = remember { DecimalFormat("#,##0.0#") }

    // Calculations
    val billAmount = billAmountStr.toDoubleOrNull() ?: 0.0
    val tipPercent = (tipPercentStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val serviceTaxPercent = (serviceTaxPercentStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

    val serviceTaxAmount = billAmount * (serviceTaxPercent / 100.0)
    val rawTipAmount = billAmount * (tipPercent / 100.0)
    val rawTotalBill = billAmount + rawTipAmount + serviceTaxAmount

    val rawPricePerPerson = if (peopleCount > 0) rawTotalBill / peopleCount else rawTotalBill

    // Rounding logic
    val finalPricePerPerson = if (isRoundUpEnabled) ceil(rawPricePerPerson) else rawPricePerPerson
    val finalTotalBill = if (isRoundUpEnabled) finalPricePerPerson * peopleCount else rawTotalBill
    val finalTipAmount = (finalTotalBill - billAmount - serviceTaxAmount).coerceAtLeast(0.0)

    fun handleKeypadInput(key: String) {
        haptics.click()
        when (activeInput) {
            ActiveTipInput.BILL_AMOUNT -> {
                billAmountStr = updateInputString(billAmountStr, key, isDecimal = true)
            }
            ActiveTipInput.TIP_PERCENT -> {
                tipPercentStr = updateInputString(tipPercentStr, key, isDecimal = false)
            }
        }
    }

    fun copyTipSummary() {
        val summary = "Bill: ${currencyFormat.format(billAmount)}\nTip (${tipPercent.toInt()}%): ${currencyFormat.format(finalTipAmount)}\nTotal: ${currencyFormat.format(finalTotalBill)}" + if (peopleCount > 1) "\nPer Person ($peopleCount people): ${currencyFormat.format(finalPricePerPerson)}" else ""
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tip Summary", summary)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied tip summary to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Tip",
                description = "Gratitude & Split Bill Engine",
                icon = TablerIcons.Receipt2,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Adjustments,
                onRightActionClick = {
                    haptics.click()
                    showOptionsSheet = true
                }
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
            // ==================== 1. HERO GRATITUDE SUMMARY CARD ====================
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
                                text = "TOTAL WITH TIP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = {
                                    haptics.click()
                                    showOptionsSheet = true
                                },
                                shape = ContinuousCapsule,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Options",
                                        modifier = Modifier.size(14.dp),
                                        tint = vibrantOrange
                                    )
                                    Text(
                                        text = "OPTIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(
                                onClick = { copyTipSummary() },
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
                    }

                    // Main Total Bill Readout
                    Text(
                        text = currencyFormat.format(finalTotalBill),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        color = MaterialTheme.colorScheme.onSurface,
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
                                text = "TIP: ${currencyFormat.format(finalTipAmount)} (${tipPercent.toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (peopleCount > 1 || isRoundUpEnabled || serviceTaxPercent > 0) {
                            Text(
                                text = listOfNotNull(
                                    if (peopleCount > 1) "${currencyFormat.format(finalPricePerPerson)}/p" else null,
                                    if (isRoundUpEnabled) "Rounded" else null
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 2. INPUT READOUT CARDS ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics.click()
                                activeInput = ActiveTipInput.BILL_AMOUNT
                            }
                    ) {
                        Text(
                            text = "BILL AMOUNT ($)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$$billAmountStr",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeistMono,
                            color = if (activeInput == ActiveTipInput.BILL_AMOUNT) vibrantOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics.click()
                                activeInput = ActiveTipInput.TIP_PERCENT
                            }
                    ) {
                        Text(
                            text = "TIP RATE (%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$tipPercentStr%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeistMono,
                            color = if (activeInput == ActiveTipInput.TIP_PERCENT) vibrantOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 3. TIP RATE PRESET CHIPS ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(10, 12, 15, 18, 20, 22, 25, 30)) { pct ->
                    val isSelected = tipPercent.toInt() == pct
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.click()
                            activeInput = ActiveTipInput.TIP_PERCENT
                            tipPercentStr = pct.toString()
                        },
                        label = { Text("$pct% TIP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // PUSH NUMPAD ALL THE WAY DOWN TO THE BOTTOM OF THE SCREEN
            Spacer(modifier = Modifier.weight(1f))

            // ==================== 4. EVEN SPACIOUS NUMPAD GRID (ALIGNED AT BOTTOM) ====================
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
                                    ActiveTipInput.BILL_AMOUNT -> ActiveTipInput.TIP_PERCENT
                                    ActiveTipInput.TIP_PERCENT -> ActiveTipInput.BILL_AMOUNT
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

    // ==================== OPTIONS MATERIAL BOTTOM SHEET ====================
    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tip & Split Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Round Up Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Round Up Bill", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Round total to nearest dollar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    Switch(
                        checked = isRoundUpEnabled,
                        onCheckedChange = {
                            haptics.click()
                            isRoundUpEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = vibrantOrange)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Split People Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split Bill (People)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                if (peopleCount > 1) peopleCount--
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$peopleCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeistMono
                        )

                        IconButton(
                            onClick = {
                                haptics.click()
                                peopleCount++
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Service Tax Input
                OutlinedTextField(
                    value = serviceTaxPercentStr,
                    onValueChange = { serviceTaxPercentStr = it },
                    label = { Text("Service Tax / VAT Rate (%)") },
                    placeholder = { Text("e.g. 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                Button(
                    onClick = {
                        haptics.click()
                        showOptionsSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    shape = ContinuousCapsule,
                    colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                ) {
                    Text("Apply & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun updateInputString(currentStr: String, key: String, isDecimal: Boolean): String {
    return when (key) {
        "AC" -> "0"
        "DEL" -> if (currentStr.length > 1) currentStr.substring(0, currentStr.length - 1) else "0"
        "." -> if (isDecimal && !currentStr.contains(".")) "$currentStr." else currentStr
        "00" -> if (currentStr != "0" && currentStr.length < 8) "${currentStr}00" else currentStr
        else -> if (currentStr == "0") key else if (currentStr.length < 9) currentStr + key else currentStr
    }
}
