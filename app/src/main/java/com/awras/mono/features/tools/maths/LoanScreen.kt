package com.awras.mono.features.tools.maths

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import compose.icons.tablericons.BuildingBank
import compose.icons.tablericons.Report
import java.text.DecimalFormat
import kotlin.math.pow

private enum class ActiveLoanField {
    PRINCIPAL, INTEREST_RATE, TENURE
}

data class AmortizationRow(
    val monthNumber: Int,
    val emi: Double,
    val principalPayment: Double,
    val interestPayment: Double,
    val remainingBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var activeField by remember { mutableStateOf(ActiveLoanField.PRINCIPAL) }

    var principalStr by remember { mutableStateOf("100000") }
    var interestRateStr by remember { mutableStateOf("8.5") }
    var tenureStr by remember { mutableStateOf("10") }
    var isTenureInYears by remember { mutableStateOf(true) }

    var showScheduleSheet by remember { mutableStateOf(false) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val percentFormat = remember { DecimalFormat("#,##0.0#") }

    // Formula Calculations
    val principal = principalStr.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateStr.toDoubleOrNull() ?: 0.0
    val tenureValue = tenureStr.toIntOrNull() ?: 1

    val totalMonths = if (isTenureInYears) tenureValue * 12 else tenureValue
    val monthlyRate = (annualRate / 12.0) / 100.0

    // EMI Formula: P * r * (1+r)^n / ((1+r)^n - 1)
    val emi = remember(principal, monthlyRate, totalMonths) {
        if (principal <= 0 || totalMonths <= 0) 0.0
        else if (monthlyRate <= 0) principal / totalMonths
        else {
            val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
            (principal * monthlyRate * factor) / (factor - 1.0)
        }
    }

    val totalPayable = emi * totalMonths
    val totalInterest = (totalPayable - principal).coerceAtLeast(0.0)

    val principalRatio = if (totalPayable > 0) (principal / totalPayable).toFloat().coerceIn(0f, 1f) else 0.5f

    // Generate Amortization Schedule
    val schedule = remember(principal, monthlyRate, totalMonths, emi) {
        val list = mutableListOf<AmortizationRow>()
        var balance = principal
        if (principal > 0 && totalMonths > 0 && emi > 0) {
            for (m in 1..totalMonths) {
                val interestPayment = balance * monthlyRate
                val principalPayment = (emi - interestPayment).coerceAtMost(balance)
                balance = (balance - principalPayment).coerceAtLeast(0.0)
                list.add(
                    AmortizationRow(
                        monthNumber = m,
                        emi = emi,
                        principalPayment = principalPayment,
                        interestPayment = interestPayment,
                        remainingBalance = balance
                    )
                )
            }
        }
        list
    }

    fun handleKeypadInput(key: String) {
        haptics.click()
        val currentStr = when (activeField) {
            ActiveLoanField.PRINCIPAL -> principalStr
            ActiveLoanField.INTEREST_RATE -> interestRateStr
            ActiveLoanField.TENURE -> tenureStr
        }

        val updated = when (key) {
            "AC" -> "0"
            "DEL" -> if (currentStr.length > 1) currentStr.substring(0, currentStr.length - 1) else "0"
            "." -> if (activeField != ActiveLoanField.TENURE && !currentStr.contains(".")) "$currentStr." else currentStr
            "00" -> if (activeField == ActiveLoanField.PRINCIPAL && currentStr != "0" && currentStr.length < 8) "${currentStr}00" else currentStr
            else -> if (currentStr == "0") key else if (currentStr.length < 9) currentStr + key else currentStr
        }

        when (activeField) {
            ActiveLoanField.PRINCIPAL -> principalStr = updated
            ActiveLoanField.INTEREST_RATE -> interestRateStr = updated
            ActiveLoanField.TENURE -> tenureStr = updated
        }
    }

    fun copyEmiSummary() {
        val summary = "Loan Principal: ${currencyFormat.format(principal)}\nMonthly EMI: ${currencyFormat.format(emi)}\nTotal Interest: ${currencyFormat.format(totalInterest)}\nTotal Payable: ${currencyFormat.format(totalPayable)}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("EMI Summary", summary)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied EMI summary to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Loan",
                description = "EMI, Amortization & Interest Analytics Engine",
                icon = TablerIcons.BuildingBank,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Report,
                onRightActionClick = {
                    haptics.click()
                    showScheduleSheet = true
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
            // ==================== 1. EMI SUMMARY HERO CARD ====================
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
                                text = "MONTHLY EMI",
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
                                    showScheduleSheet = true
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
                                        imageVector = TablerIcons.Report,
                                        contentDescription = "Schedule",
                                        modifier = Modifier.size(14.dp),
                                        tint = vibrantOrange
                                    )
                                    Text(
                                        text = "SCHEDULE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(
                                onClick = { copyEmiSummary() },
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

                    // Main Monthly EMI Readout
                    Text(
                        text = "${currencyFormat.format(emi)} / mo",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    // Principal vs Interest Proportion Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { principalRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = vibrantOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Principal: ${currencyFormat.format(principal)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = vibrantOrange
                            )

                            Text(
                                text = "Interest: ${currencyFormat.format(totalInterest)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== 2. PARAMETER INPUT CARDS ====================
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
                    // Row 1: Loan Amount
                    InputRow(
                        title = "LOAN AMOUNT ($)",
                        valueStr = "$$principalStr",
                        isSelected = activeField == ActiveLoanField.PRINCIPAL,
                        onClick = { activeField = ActiveLoanField.PRINCIPAL }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                    // Row 2: Interest Rate (%) & Tenure Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputRow(
                            title = "INTEREST RATE (%)",
                            valueStr = "$interestRateStr%",
                            isSelected = activeField == ActiveLoanField.INTEREST_RATE,
                            onClick = { activeField = ActiveLoanField.INTEREST_RATE },
                            modifier = Modifier.weight(1f)
                        )

                        // Tenure Input + Year/Month Toggle
                        Surface(
                            onClick = { activeField = ActiveLoanField.TENURE },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = if (activeField == ActiveLoanField.TENURE) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (activeField == ActiveLoanField.TENURE) vibrantOrange else Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TENURE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )

                                    Surface(
                                        onClick = {
                                            haptics.click()
                                            isTenureInYears = !isTenureInYears
                                        },
                                        shape = ContinuousCapsule,
                                        color = vibrantOrange
                                    ) {
                                        Text(
                                            text = if (isTenureInYears) "YR" else "MO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$tenureStr ${if (isTenureInYears) "Years" else "Months"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (activeField == ActiveLoanField.TENURE) vibrantOrange else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // PUSH NUMPAD DOWN TO THE BOTTOM OF THE SCREEN FOR EFFORTLESS REACH
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
                                activeField = when (activeField) {
                                    ActiveLoanField.PRINCIPAL -> ActiveLoanField.INTEREST_RATE
                                    ActiveLoanField.INTEREST_RATE -> ActiveLoanField.TENURE
                                    ActiveLoanField.TENURE -> ActiveLoanField.PRINCIPAL
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

    // ==================== AMORTIZATION SCHEDULE MATERIAL BOTTOM SHEET ====================
    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Amortization Schedule (${totalMonths} Months)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedule) { row ->
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
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Month ${row.monthNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = vibrantOrange
                                    )
                                    Text(
                                        text = "Balance: ${currencyFormat.format(row.remainingBalance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "EMI: ${currencyFormat.format(row.emi)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "P: ${currencyFormat.format(row.principalPayment)} • I: ${currencyFormat.format(row.interestPayment)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

@Composable
private fun InputRow(
    title: String,
    valueStr: String,
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
                text = valueStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
