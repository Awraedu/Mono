package com.awras.mono.features.tools.finance

import com.awras.mono.core.model.Tool
import com.awras.mono.core.model.ToolCategory
import compose.icons.TablerIcons
import compose.icons.tablericons.ChartPie
import compose.icons.tablericons.Receipt2
import compose.icons.tablericons.ReceiptTax
import compose.icons.tablericons.Wallet

val financeTools = listOf(
    Tool(
        id = "tip-calculator",
        name = "Tip",
        description = "Advanced Tip, Service Gratitude & Split Bill Engine",
        tagline = "GRATITUDE ENGINE",
        icon = TablerIcons.Receipt2,
        category = ToolCategory.FINANCE,
        route = "tip_calculator",
        content = { onBackClick -> TipCalculatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "tax-calculator",
        name = "Tax Calc",
        description = "Advanced Income Tax Brackets, Deductions & Take-Home Pay Engine",
        tagline = "TAX ENGINE",
        icon = TablerIcons.ReceiptTax,
        category = ToolCategory.FINANCE,
        route = "tax_calculator",
        content = { onBackClick -> TaxCalculatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "roi-calculator",
        name = "ROI Calc",
        description = "Return on Investment & Annualized Yield Engine",
        tagline = "INVESTMENT ROI",
        icon = TablerIcons.ChartPie,
        category = ToolCategory.FINANCE,
        route = "roi_calculator",
        content = { onBackClick -> RoiCalculatorScreen(onBackClick = onBackClick) }
    ),
    Tool(
        id = "budget-tracker",
        name = "Budget",
        description = "Personal Expense & Income Flow Tracker",
        tagline = "EXPENSE TRACKER",
        icon = TablerIcons.Wallet,
        category = ToolCategory.FINANCE,
        route = "budget_tracker",
        content = { onBackClick -> BudgetTrackerScreen(onBackClick = onBackClick) }
    )
)
