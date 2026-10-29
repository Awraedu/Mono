package com.awras.mono.features.tools.maths

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import compose.icons.tablericons.Exchange
import java.text.DecimalFormat

data class UnitDefinition(
    val name: String,
    val symbol: String,
    val factorToBase: Double
)

enum class UnitCategoryType(val displayName: String) {
    LENGTH("Length"),
    MASS("Mass"),
    TEMPERATURE("Temp"),
    AREA("Area"),
    VOLUME("Volume"),
    SPEED("Speed"),
    TIME("Time"),
    DATA("Data"),
    PRESSURE("Pressure"),
    ENERGY("Energy")
}

private val UNITS_MAP = mapOf(
    UnitCategoryType.LENGTH to listOf(
        UnitDefinition("Meter", "m", 1.0),
        UnitDefinition("Kilometer", "km", 1000.0),
        UnitDefinition("Centimeter", "cm", 0.01),
        UnitDefinition("Millimeter", "mm", 0.001),
        UnitDefinition("Mile", "mi", 1609.344),
        UnitDefinition("Yard", "yd", 0.9144),
        UnitDefinition("Foot", "ft", 0.3048),
        UnitDefinition("Inch", "in", 0.0254),
        UnitDefinition("Nautical Mile", "nmi", 1852.0)
    ),
    UnitCategoryType.MASS to listOf(
        UnitDefinition("Kilogram", "kg", 1.0),
        UnitDefinition("Gram", "g", 0.001),
        UnitDefinition("Milligram", "mg", 0.000001),
        UnitDefinition("Metric Ton", "t", 1000.0),
        UnitDefinition("Pound", "lb", 0.45359237),
        UnitDefinition("Ounce", "oz", 0.028349523125)
    ),
    UnitCategoryType.TEMPERATURE to listOf(
        UnitDefinition("Celsius", "°C", 1.0),
        UnitDefinition("Fahrenheit", "°F", 1.0),
        UnitDefinition("Kelvin", "K", 1.0)
    ),
    UnitCategoryType.AREA to listOf(
        UnitDefinition("Square Meter", "m²", 1.0),
        UnitDefinition("Square Kilometer", "km²", 1000000.0),
        UnitDefinition("Square Centimeter", "cm²", 0.0001),
        UnitDefinition("Square Foot", "ft²", 0.09290304),
        UnitDefinition("Square Inch", "in²", 0.00064516),
        UnitDefinition("Acre", "ac", 4046.8564224),
        UnitDefinition("Hectare", "ha", 10000.0)
    ),
    UnitCategoryType.VOLUME to listOf(
        UnitDefinition("Liter", "L", 1.0),
        UnitDefinition("Milliliter", "mL", 0.001),
        UnitDefinition("Cubic Meter", "m³", 1000.0),
        UnitDefinition("Gallon (US)", "gal", 3.785411784),
        UnitDefinition("Quart (US)", "qt", 0.946352946),
        UnitDefinition("Pint (US)", "pt", 0.473176473),
        UnitDefinition("Cup (US)", "cup", 0.240),
        UnitDefinition("Fluid Ounce", "fl oz", 0.0295735295625)
    ),
    UnitCategoryType.SPEED to listOf(
        UnitDefinition("Meters/sec", "m/s", 1.0),
        UnitDefinition("Kilometers/h", "km/h", 0.277777778),
        UnitDefinition("Miles/hour", "mph", 0.44704),
        UnitDefinition("Knot", "kn", 0.514444444)
    ),
    UnitCategoryType.TIME to listOf(
        UnitDefinition("Second", "s", 1.0),
        UnitDefinition("Millisecond", "ms", 0.001),
        UnitDefinition("Minute", "min", 60.0),
        UnitDefinition("Hour", "h", 3600.0),
        UnitDefinition("Day", "d", 86400.0),
        UnitDefinition("Week", "wk", 604800.0),
        UnitDefinition("Month", "mo", 2629746.0),
        UnitDefinition("Year", "yr", 31556952.0)
    ),
    UnitCategoryType.DATA to listOf(
        UnitDefinition("Byte", "B", 1.0),
        UnitDefinition("Kilobyte", "KB", 1024.0),
        UnitDefinition("Megabyte", "MB", 1048576.0),
        UnitDefinition("Gigabyte", "GB", 1073741824.0),
        UnitDefinition("Terabyte", "TB", 1099511627776.0)
    ),
    UnitCategoryType.PRESSURE to listOf(
        UnitDefinition("Pascal", "Pa", 1.0),
        UnitDefinition("Kilopascal", "kPa", 1000.0),
        UnitDefinition("Bar", "bar", 100000.0),
        UnitDefinition("PSI", "psi", 6894.757293168),
        UnitDefinition("Atmosphere", "atm", 101325.0),
        UnitDefinition("mmHg", "mmHg", 133.3223684)
    ),
    UnitCategoryType.ENERGY to listOf(
        UnitDefinition("Joule", "J", 1.0),
        UnitDefinition("Kilojoule", "kJ", 1000.0),
        UnitDefinition("Calorie", "cal", 4.184),
        UnitDefinition("Kilocalorie", "kcal", 4184.0),
        UnitDefinition("Watt-hour", "Wh", 3600.0),
        UnitDefinition("Kilowatt-hour", "kWh", 3600000.0)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var selectedCategory by remember { mutableStateOf(UnitCategoryType.LENGTH) }
    val currentUnitsList = UNITS_MAP[selectedCategory] ?: emptyList()

    var sourceUnit by remember(selectedCategory) { mutableStateOf(currentUnitsList.first()) }
    var targetUnit by remember(selectedCategory) { mutableStateOf(currentUnitsList.getOrElse(1) { currentUnitsList.first() }) }

    var inputString by remember { mutableStateOf("1") }

    // Dropdown States
    var showSourceSheet by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }

    // Calculation Logic
    val convertedResultString by remember(inputString, sourceUnit, targetUnit, selectedCategory) {
        derivedStateOf {
            val inputValue = inputString.toDoubleOrNull() ?: return@derivedStateOf "0"
            if (selectedCategory == UnitCategoryType.TEMPERATURE) {
                val valInCelsius = when (sourceUnit.symbol) {
                    "°C" -> inputValue
                    "°F" -> (inputValue - 32.0) * 5.0 / 9.0
                    "K" -> inputValue - 273.15
                    else -> inputValue
                }
                val finalVal = when (targetUnit.symbol) {
                    "°C" -> valInCelsius
                    "°F" -> (valInCelsius * 9.0 / 5.0) + 32.0
                    "K" -> valInCelsius + 273.15
                    else -> valInCelsius
                }
                val format = DecimalFormat("#,##0.######")
                format.format(finalVal)
            } else {
                val valueInBase = inputValue * sourceUnit.factorToBase
                val finalVal = valueInBase / targetUnit.factorToBase
                val format = DecimalFormat("#,##0.######")
                format.format(finalVal)
            }
        }
    }

    fun handleNumpadKey(key: String) {
        haptics.click()
        when (key) {
            "AC" -> inputString = "0"
            "DEL" -> {
                if (inputString.length > 1) {
                    inputString = inputString.substring(0, inputString.length - 1)
                } else {
                    inputString = "0"
                }
            }
            "±" -> {
                if (inputString.startsWith("-")) {
                    inputString = inputString.substring(1)
                } else if (inputString != "0") {
                    inputString = "-$inputString"
                }
            }
            "." -> {
                if (!inputString.contains(".")) {
                    inputString += "."
                }
            }
            "SWAP" -> {
                val temp = sourceUnit
                sourceUnit = targetUnit
                targetUnit = temp
            }
            else -> {
                if (inputString == "0") {
                    inputString = key
                } else if (inputString.length < 12) {
                    inputString += key
                }
            }
        }
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Converter Result", "$convertedResultString ${targetUnit.symbol}")
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied: $convertedResultString ${targetUnit.symbol}", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Converter",
                description = "Universal Unit Scale & Dimensional Engine",
                icon = TablerIcons.Exchange,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== 1. CATEGORY PICKER ROW ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(UnitCategoryType.values()) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        onClick = {
                            haptics.click()
                            selectedCategory = cat
                        },
                        shape = ContinuousCapsule,
                        color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = cat.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ==================== 2. DISPLAY & READOUT CARD ====================
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // SOURCE UNIT INPUT SECTION
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FROM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                letterSpacing = 1.sp
                            )

                            // Source Unit Selector Pill
                            Surface(
                                onClick = {
                                    haptics.click()
                                    showSourceSheet = true
                                },
                                shape = ContinuousCapsule,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${sourceUnit.name} (${sourceUnit.symbol})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Source Value Display
                        Text(
                            text = inputString,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.awras.mono.ui.theme.GeistMono,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            fontSize = if (inputString.length > 8) 28.sp else 38.sp
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

                    // TARGET UNIT OUTPUT SECTION
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "TO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = vibrantOrange,
                                    letterSpacing = 1.sp
                                )

                                IconButton(
                                    onClick = { copyToClipboard() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Result",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Target Unit Selector Pill
                            Surface(
                                onClick = {
                                    haptics.click()
                                    showTargetSheet = true
                                },
                                shape = ContinuousCapsule,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${targetUnit.name} (${targetUnit.symbol})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Converted Result Display
                        Text(
                            text = convertedResultString,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = com.awras.mono.ui.theme.GeistMono,
                            color = vibrantOrange,
                            maxLines = 1,
                            fontSize = if (convertedResultString.length > 8) 28.sp else 38.sp
                        )
                    }
                }
            }

            // ==================== 3. BUILT-IN NUMERIC KEYPAD ====================
            val numpadGrid = listOf(
                "7", "8", "9", "AC",
                "4", "5", "6", "DEL",
                "1", "2", "3", "SWAP",
                "±", "0", ".", "="
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(numpadGrid) { key ->
                    val isAction = key in listOf("AC", "DEL")
                    val isSwap = key == "SWAP"
                    val isEquals = key == "="

                    Surface(
                        onClick = { handleNumpadKey(key) },
                        shape = ContinuousRoundedRectangle(16.dp),
                        color = when {
                            isEquals -> vibrantOrange
                            isSwap -> MaterialTheme.colorScheme.surfaceVariant
                            isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(1.dp, if (isEquals) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isSwap) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap",
                                    tint = vibrantOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isEquals -> Color.White
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

    // ==================== SOURCE UNIT SELECTION SHEET ====================
    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select From Unit (${selectedCategory.displayName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentUnitsList) { unit ->
                        val isSelected = unit.name == sourceUnit.name
                        Surface(
                            onClick = {
                                haptics.click()
                                sourceUnit = unit
                                showSourceSheet = false
                            },
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = if (isSelected) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${unit.name} (${unit.symbol})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = vibrantOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== TARGET UNIT SELECTION SHEET ====================
    if (showTargetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTargetSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select To Unit (${selectedCategory.displayName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentUnitsList) { unit ->
                        val isSelected = unit.name == targetUnit.name
                        Surface(
                            onClick = {
                                haptics.click()
                                targetUnit = unit
                                showTargetSheet = false
                            },
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = if (isSelected) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${unit.name} (${unit.symbol})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = vibrantOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
