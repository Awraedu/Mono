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
import compose.icons.tablericons.ReceiptTax
import java.text.DecimalFormat

enum class FilingStatus(val displayName: String) {
    SINGLE("Single"),
    MARRIED_JOINT("Married Joint"),
    HEAD_OF_HOUSEHOLD("Head of House")
}

private enum class ActiveTaxInput {
    GROSS_INCOME, DEDUCTION, TAX_CREDITS
}

data class TaxBracket(
    val limit: Double, // Upper limit for this bracket
    val rate: Double   // Tax rate % for this bracket
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCalculatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var activeInput by remember { mutableStateOf(ActiveTaxInput.GROSS_INCOME) }
    var filingStatus by remember { mutableStateOf(FilingStatus.SINGLE) }

    var grossIncomeStr by remember { mutableStateOf("100000") }
    var deductionStr by remember { mutableStateOf("14600") }
    var taxCreditsStr by remember { mutableStateOf("1000") }

    var showBracketsSheet by remember { mutableStateOf(false) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val percentFormat = remember { DecimalFormat("#,##0.0#") }

    val grossIncome = grossIncomeStr.toDoubleOrNull() ?: 0.0
    val deduction = deductionStr.toDoubleOrNull() ?: 0.0
    val credits = taxCreditsStr.toDoubleOrNull() ?: 0.0

    // Progressive Brackets according to filing status
    val brackets = remember(filingStatus) {
        when (filingStatus) {
            FilingStatus.SINGLE -> listOf(
                TaxBracket(11600.0, 0.10),
                TaxBracket(47150.0, 0.12),
                TaxBracket(100525.0, 0.22),
                TaxBracket(191950.0, 0.24),
                TaxBracket(243725.0, 0.32),
                TaxBracket(609350.0, 0.35),
                TaxBracket(Double.MAX_VALUE, 0.37)
            )
            FilingStatus.MARRIED_JOINT -> listOf(
                TaxBracket(23200.0, 0.10),
                TaxBracket(94300.0, 0.12),
                TaxBracket(201050.0, 0.22),
                TaxBracket(383900.0, 0.24),
                TaxBracket(487450.0, 0.32),
                TaxBracket(731200.0, 0.35),
                TaxBracket(Double.MAX_VALUE, 0.37)
            )
            FilingStatus.HEAD_OF_HOUSEHOLD -> listOf(
                TaxBracket(16550.0, 0.10),
                TaxBracket(63100.0, 0.12),
                TaxBracket(100500.0, 0.22),
                TaxBracket(191950.0, 0.24),
                TaxBracket(243700.0, 0.32),
                TaxBracket(609350.0, 0.35),
                TaxBracket(Double.MAX_VALUE, 0.37)
            )
        }
    }

    // Calculations
    val taxableIncome = (grossIncome - deduction).coerceAtLeast(0.0)

    val (totalTaxBeforeCredits, marginalRate) = remember(taxableIncome, brackets) {
        var taxAcc = 0.0
        var prevLimit = 0.0
        var lastRate = 0.10

        for (b in brackets) {
            if (taxableIncome > prevLimit) {
                val taxableInBracket = (taxableIncome.coerceAtMost(b.limit) - prevLimit).coerceAtLeast(0.0)
                taxAcc += taxableInBracket * b.rate
                lastRate = b.rate
                prevLimit = b.limit
            } else {
                break
            }
        }
        Pair(taxAcc, lastRate * 100.0)
    }

    val finalTaxPayable = (totalTaxBeforeCredits - credits).coerceAtLeast(0.0)
    val takeHomePayAnnual = (grossIncome - finalTaxPayable).coerceAtLeast(0.0)
    val takeHomePayMonthly = takeHomePayAnnual / 12.0

    val effectiveRate = if (grossIncome > 0) (finalTaxPayable / grossIncome) * 100.0 else 0.0

    fun handleKeypadInput(key: String) {
        haptics.click()
        when (activeInput) {
            ActiveTaxInput.GROSS_INCOME -> grossIncomeStr = updateInputString(grossIncomeStr, key)
            ActiveTaxInput.DEDUCTION -> deductionStr = updateInputString(deductionStr, key)
            ActiveTaxInput.TAX_CREDITS -> taxCreditsStr = updateInputString(taxCreditsStr, key)
        }
    }

    fun copyTaxSummary() {
        val summary = "Gross Income: ${currencyFormat.format(grossIncome)}\nTax Payable: ${currencyFormat.format(finalTaxPayable)} (${percentFormat.format(effectiveRate)}% Effective)\nAnnual Take-Home: ${currencyFormat.format(takeHomePayAnnual)}\nMonthly Take-Home: ${currencyFormat.format(takeHomePayMonthly)}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tax Summary", summary)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied tax summary to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Tax Calc",
                description = "Advanced Income Tax & Take-Home Engine",
                icon = TablerIcons.ReceiptTax,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Adjustments,
                onRightActionClick = {
                    haptics.click()
                    showBracketsSheet = true
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
            // ==================== 1. TAKE-HOME PAY SUMMARY CARD ====================
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
                                text = "NET TAKE-HOME PAY",
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
                                    showBracketsSheet = true
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
                                        contentDescription = "Brackets",
                                        modifier = Modifier.size(14.dp),
                                        tint = vibrantOrange
                                    )
                                    Text(
                                        text = "BRACKETS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(
                                onClick = { copyTaxSummary() },
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

                    // Main Take Home Annual Readout
                    Text(
                        text = "${currencyFormat.format(takeHomePayAnnual)} / yr",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    // Breakdown Pill Row
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
                                text = "TAX: ${currencyFormat.format(finalTaxPayable)} (${percentFormat.format(effectiveRate)}% Eff)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "${currencyFormat.format(takeHomePayMonthly)} / mo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 2. FILING STATUS SELECTOR ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FilingStatus.values()) { status ->
                    val isSelected = status == filingStatus
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.click()
                            filingStatus = status
                        },
                        label = { Text(status.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 3. PARAMETERS CARDS ====================
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
                    // Row 1: Gross Income
                    InputFieldCard(
                        title = "GROSS ANNUAL INCOME ($)",
                        valueDisplay = "$$grossIncomeStr",
                        isSelected = activeInput == ActiveTaxInput.GROSS_INCOME,
                        onClick = {
                            haptics.click()
                            activeInput = ActiveTaxInput.GROSS_INCOME
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                    // Row 2: Standard Deduction & Tax Credits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputFieldCard(
                            title = "DEDUCTION ($)",
                            valueDisplay = "$$deductionStr",
                            isSelected = activeInput == ActiveTaxInput.DEDUCTION,
                            onClick = {
                                haptics.click()
                                activeInput = ActiveTaxInput.DEDUCTION
                            },
                            modifier = Modifier.weight(1f)
                        )

                        InputFieldCard(
                            title = "TAX CREDITS ($)",
                            valueDisplay = "$$taxCreditsStr",
                            isSelected = activeInput == ActiveTaxInput.TAX_CREDITS,
                            onClick = {
                                haptics.click()
                                activeInput = ActiveTaxInput.TAX_CREDITS
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                                    ActiveTaxInput.GROSS_INCOME -> ActiveTaxInput.DEDUCTION
                                    ActiveTaxInput.DEDUCTION -> ActiveTaxInput.TAX_CREDITS
                                    ActiveTaxInput.TAX_CREDITS -> ActiveTaxInput.GROSS_INCOME
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

    // ==================== TAX BRACKETS MATERIAL BOTTOM SHEET ====================
    if (showBracketsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBracketsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tax Brackets (${filingStatus.displayName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Taxable Income: ${currencyFormat.format(taxableIncome)} • Marginal Rate: ${percentFormat.format(marginalRate)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = vibrantOrange,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var prevLimit = 0.0
                    brackets.forEach { b ->
                        val bracketLimitStr = if (b.limit == Double.MAX_VALUE) "And above" else "${currencyFormat.format(prevLimit)} - ${currencyFormat.format(b.limit)}"
                        Surface(
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bracketLimitStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                Surface(
                                    shape = ContinuousCapsule,
                                    color = vibrantOrange
                                ) {
                                    Text(
                                        text = "${(b.rate * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        prevLimit = b.limit
                    }
                }

                Button(
                    onClick = {
                        haptics.click()
                        showBracketsSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    shape = ContinuousCapsule,
                    colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                ) {
                    Text("Close Brackets", fontWeight = FontWeight.Bold)
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
