package com.awras.mono.features.tools.maths

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
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Adjustments
import compose.icons.tablericons.Tag
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountCalculatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var originalPriceStr by remember { mutableStateOf("100") }
    var primaryDiscountPercentStr by remember { mutableStateOf("20") }
    var extraDiscountPercentStr by remember { mutableStateOf("0") }
    var flatCouponStr by remember { mutableStateOf("0") }
    var taxRatePercentStr by remember { mutableStateOf("8") }
    var splitPeopleCount by remember { mutableIntStateOf(1) }

    var showAdvancedSheet by remember { mutableStateOf(false) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val percentFormat = remember { DecimalFormat("#,##0.0#") }

    // Calculations
    val origPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val primaryDisc = (primaryDiscountPercentStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val extraDisc = (extraDiscountPercentStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val couponVal = flatCouponStr.toDoubleOrNull() ?: 0.0
    val taxRate = taxRatePercentStr.toDoubleOrNull() ?: 0.0

    // Step 1: Primary discount
    val primarySavings = origPrice * (primaryDisc / 100.0)
    val priceAfterPrimary = (origPrice - primarySavings).coerceAtLeast(0.0)

    // Step 2: Extra stacking discount
    val extraSavings = priceAfterPrimary * (extraDisc / 100.0)
    val priceAfterExtra = (priceAfterPrimary - extraSavings).coerceAtLeast(0.0)

    // Step 3: Flat coupon
    val finalDiscountedPrice = (priceAfterExtra - couponVal).coerceAtLeast(0.0)
    val totalSavings = (origPrice - finalDiscountedPrice).coerceAtLeast(0.0)

    // Step 4: Tax
    val taxAmount = finalDiscountedPrice * (taxRate / 100.0)
    val finalTotalPriceWithTax = finalDiscountedPrice + taxAmount

    // Step 5: Split per person
    val pricePerPerson = if (splitPeopleCount > 0) finalTotalPriceWithTax / splitPeopleCount else finalTotalPriceWithTax
    val effectiveDiscountPercent = if (origPrice > 0) (totalSavings / origPrice) * 100.0 else 0.0

    fun handleKeypadInput(key: String) {
        haptics.click()
        when (key) {
            "AC" -> {
                originalPriceStr = "0"
            }
            "DEL" -> {
                originalPriceStr = if (originalPriceStr.length > 1) originalPriceStr.substring(0, originalPriceStr.length - 1) else "0"
            }
            "." -> {
                if (!originalPriceStr.contains(".")) {
                    originalPriceStr += "."
                }
            }
            "00" -> {
                if (originalPriceStr != "0" && originalPriceStr.length < 8) {
                    originalPriceStr += "00"
                }
            }
            else -> {
                originalPriceStr = if (originalPriceStr == "0") key else if (originalPriceStr.length < 9) originalPriceStr + key else originalPriceStr
            }
        }
    }

    fun copySavingsSummary() {
        val summary = "Original: ${currencyFormat.format(origPrice)}\nSavings: ${currencyFormat.format(totalSavings)} (${percentFormat.format(effectiveDiscountPercent)}% OFF)\nFinal Total: ${currencyFormat.format(finalTotalPriceWithTax)}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Discount Summary", summary)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied summary to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Discount",
                description = "Price Savings & Tax Engine",
                icon = TablerIcons.Tag,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Adjustments,
                onRightActionClick = {
                    haptics.click()
                    showAdvancedSheet = true
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
            // ==================== 1. SUMMARY HERO CARD ====================
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
                                text = "YOU PAY",
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
                                    showAdvancedSheet = true
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
                                onClick = { copySavingsSummary() },
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

                    // Main Final Price Readout
                    Text(
                        text = currencyFormat.format(finalTotalPriceWithTax),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    // Savings Pill Row
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
                                text = "SAVE ${currencyFormat.format(totalSavings)} (${percentFormat.format(effectiveDiscountPercent)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (taxRate > 0 || splitPeopleCount > 1 || extraDisc > 0 || couponVal > 0) {
                            Text(
                                text = listOfNotNull(
                                    if (taxRate > 0) "+${taxRate.toInt()}% Tax" else null,
                                    if (splitPeopleCount > 1) "${currencyFormat.format(pricePerPerson)}/p" else null
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ==================== 2. PRICE & DISCOUNT INPUT READOUT CARD ====================
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
                    Column {
                        Text(
                            text = "ORIGINAL PRICE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$$originalPriceStr",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DISCOUNT APPLIED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${primaryDisc.toInt()}% OFF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = vibrantOrange
                        )
                    }
                }
            }

            // ==================== 3. PERCENTAGE PRESET PILLS ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(5, 10, 15, 20, 25, 30, 40, 50, 75)) { pct ->
                    val isSelected = primaryDisc.toInt() == pct
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.click()
                            primaryDiscountPercentStr = pct.toString()
                        },
                        label = { Text("$pct% OFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ==================== 4. SPACIOUS TACTILE NUMPAD GRID ====================
            val keypadGrid = listOf(
                "7", "8", "9", "AC",
                "4", "5", "6", "DEL",
                "1", "2", "3", ".",
                "00", "0"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(keypadGrid) { key ->
                    val isAction = key in listOf("AC", "DEL")

                    Surface(
                        onClick = { handleKeypadInput(key) },
                        shape = ContinuousRoundedRectangle(16.dp),
                        color = when {
                            isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.height(58.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
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

    // ==================== ADVANCED OPTIONS MATERIAL BOTTOM SHEET ====================
    if (showAdvancedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvancedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Advanced Options & Tax",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = extraDiscountPercentStr,
                    onValueChange = { extraDiscountPercentStr = it },
                    label = { Text("Extra Clearance Discount (%)") },
                    placeholder = { Text("e.g. 10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                OutlinedTextField(
                    value = flatCouponStr,
                    onValueChange = { flatCouponStr = it },
                    label = { Text("Flat Coupon Value ($)") },
                    placeholder = { Text("e.g. 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                OutlinedTextField(
                    value = taxRatePercentStr,
                    onValueChange = { taxRatePercentStr = it },
                    label = { Text("Sales Tax Rate (%)") },
                    placeholder = { Text("e.g. 8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split Bill (People)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                if (splitPeopleCount > 1) splitPeopleCount--
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease People")
                        }

                        Text(
                            text = "$splitPeopleCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                haptics.click()
                                splitPeopleCount++
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase People")
                        }
                    }
                }

                Button(
                    onClick = {
                        haptics.click()
                        showAdvancedSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    shape = ContinuousCapsule,
                    colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                ) {
                    Text("Apply & Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
