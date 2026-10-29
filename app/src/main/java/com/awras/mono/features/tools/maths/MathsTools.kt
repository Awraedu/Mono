package com.awras.mono.features.tools.maths

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SquareFoot
import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.Binary
import compose.icons.tablericons.BuildingBank
import compose.icons.tablericons.Calculator
import compose.icons.tablericons.Percentage
import compose.icons.tablericons.Tag

val mathsTools = listOf(
    Tool(
        id = "scientific-calculator",
        name = "Calculator",
        description = "Advanced Scientific & Mathematical Expression Engine",
        tagline = "SCIENTIFIC CALC",
        icon = TablerIcons.Calculator,
        category = ToolCategory.MATHS,
        route = "scientific_calculator",
        content = { onBackClick -> ScientificCalculatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "unit-converter",
        name = "Converter",
        description = "Universal Unit Scale & Dimensional Converter with Built-in Numpad",
        tagline = "UNIT SCALE",
        icon = Icons.Default.SquareFoot,
        category = ToolCategory.MATHS,
        route = "unit_converter",
        content = { onBackClick -> ConverterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "base-converter",
        name = "Base Converter",
        description = "HEX, BIN, DEC, OCT Radix Converter & Bitwise Matrix Engine",
        tagline = "RADIX CONVERTER",
        icon = TablerIcons.Binary,
        category = ToolCategory.MATHS,
        route = "base_converter",
        content = { onBackClick -> BaseConverterScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "discount-calculator",
        name = "Discount",
        description = "Advanced Price Savings, Stacking Coupons & Split Tax Engine",
        tagline = "PRICE SAVINGS",
        icon = TablerIcons.Tag,
        category = ToolCategory.MATHS,
        route = "discount_calculator",
        content = { onBackClick -> DiscountCalculatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "percent-ratio",
        name = "Percent",
        description = "Ratio, Proportions & Percentage Change Engine",
        tagline = "RATIO ENGINE",
        icon = TablerIcons.Percentage,
        category = ToolCategory.MATHS,
        route = "percent_ratio",
        content = { onBackClick -> PercentScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "loan-emi",
        name = "Loan",
        description = "Equated Monthly Installment (EMI) & Amortization Analytics",
        tagline = "EMI ANALYTICS",
        icon = TablerIcons.BuildingBank,
        category = ToolCategory.MATHS,
        route = "loan_emi",
        content = { onBackClick -> LoanScreen(onBackClick = onBackClick) }
    )
)
