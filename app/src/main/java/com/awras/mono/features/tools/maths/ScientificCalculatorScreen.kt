package com.awras.mono.features.tools.maths

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import compose.icons.tablericons.*
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.*

data class HistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var isDegreeMode by remember { mutableStateOf(true) }
    var isScientificView by remember { mutableStateOf(false) }

    // Memory Storage
    var memoryValue by remember { mutableDoubleStateOf(0.0) }

    // History Log
    var historyList by remember { mutableStateOf(listOf<HistoryItem>()) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val decimalFormat = remember { DecimalFormat("#,##0.########") }

    // Real-time Live Expression Evaluator
    LaunchedEffect(expression, isDegreeMode) {
        if (expression.isBlank()) {
            resultText = "0"
            return@LaunchedEffect
        }
        try {
            val evalResult = evaluateExpression(expression, isDegreeMode)
            if (evalResult.isNaN() || evalResult.isInfinite()) {
                resultText = "Error"
            } else {
                resultText = formatResultNumber(evalResult)
            }
        } catch (e: Exception) {
            resultText = "..."
        }
    }

    fun appendInput(str: String) {
        haptics.click()
        expression += str
    }

    fun deleteLastChar() {
        haptics.click()
        if (expression.isNotEmpty()) {
            expression = expression.substring(0, expression.length - 1)
        }
    }

    fun clearAll() {
        haptics.click()
        expression = ""
        resultText = "0"
    }

    fun executeEquals() {
        if (expression.isBlank() || resultText == "Error" || resultText == "...") return
        haptics.success()
        val finalRes = resultText
        historyList = listOf(HistoryItem(expression, finalRes)) + historyList
        expression = finalRes.replace(",", "")
    }

    fun copyResult() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Calculator Result", resultText)
        clipboard.setPrimaryClip(clip)
        haptics.success()
        Toast.makeText(context, "Copied result: $resultText", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Calculator",
                description = "Advanced Scientific & Mathematical Engine",
                icon = TablerIcons.Calculator,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.History,
                onRightActionClick = {
                    haptics.click()
                    showHistorySheet = true
                }
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
            // ==================== 1. DISPLAY SCREEN CARD ====================
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
                    // Header Bar: DEG/RAD toggle & Scientific View toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // DEG / RAD Toggle
                            Surface(
                                onClick = {
                                    haptics.click()
                                    isDegreeMode = !isDegreeMode
                                },
                                shape = ContinuousCapsule,
                                color = if (isDegreeMode) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isDegreeMode) "DEG" else "RAD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDegreeMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (memoryValue != 0.0) {
                                Surface(
                                    shape = ContinuousCapsule,
                                    color = vibrantOrange.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "M",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = vibrantOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Standard vs Scientific Toggle Pill
                        Surface(
                            onClick = {
                                haptics.click()
                                isScientificView = !isScientificView
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
                                    imageVector = TablerIcons.MathFunction,
                                    contentDescription = "Toggle Keypad",
                                    modifier = Modifier.size(14.dp),
                                    tint = vibrantOrange
                                )
                                Text(
                                    text = if (isScientificView) "SCIENTIFIC" else "BASIC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Expression & Result Readout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Formula Expression Line
                        Text(
                            text = expression.ifBlank { "0" },
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            fontSize = if (expression.length > 20) 16.sp else 20.sp
                        )

                        // Main Output Result Line
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { copyResult() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Result",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }

                            Text(
                                text = resultText,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = if (resultText.length > 12) 28.sp else 38.sp
                            )
                        }
                    }
                }
            }

            // ==================== 2. SCIENTIFIC QUICK ACCESS BAR ====================
            AnimatedVisibility(visible = isScientificView) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("sin", "cos", "tan", "ln", "log", "√", "π", "e").forEach { fn ->
                        Surface(
                            onClick = {
                                when (fn) {
                                    "π" -> appendInput("π")
                                    "e" -> appendInput("e")
                                    "√" -> appendInput("√(")
                                    else -> appendInput("$fn(")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = fn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ==================== 3. KEYPAD GRID ====================
            val keypadButtons = remember(isScientificView) {
                if (isScientificView) {
                    listOf(
                        "MC", "MR", "M+", "(", ")",
                        "sin", "cos", "tan", "^", "%",
                        "ln", "log", "√", "π", "÷",
                        "7", "8", "9", "×", "DEL",
                        "4", "5", "6", "-", "AC",
                        "1", "2", "3", "+", "=",
                        "0", ".", "e", "", ""
                    )
                } else {
                    listOf(
                        "AC", "(", ")", "÷",
                        "7", "8", "9", "×",
                        "4", "5", "6", "-",
                        "1", "2", "3", "+",
                        "0", ".", "DEL", "="
                    )
                }
            }

            val columns = if (isScientificView) 5 else 4

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(keypadButtons) { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.height(54.dp))
                    } else {
                        val isOperator = key in listOf("÷", "×", "-", "+", "=", "^", "%")
                        val isAction = key in listOf("AC", "DEL", "MC", "MR", "M+")
                        val isEquals = key == "="

                        Surface(
                            onClick = {
                                when (key) {
                                    "AC" -> clearAll()
                                    "DEL" -> deleteLastChar()
                                    "=" -> executeEquals()
                                    "MC" -> { memoryValue = 0.0; haptics.click() }
                                    "MR" -> { appendInput(formatResultNumber(memoryValue)); haptics.click() }
                                    "M+" -> {
                                        val v = evaluateExpression(expression, isDegreeMode)
                                        if (!v.isNaN()) memoryValue += v
                                        haptics.success()
                                    }
                                    "sin", "cos", "tan", "ln", "log" -> appendInput("$key(")
                                    "√" -> appendInput("√(")
                                    else -> appendInput(key)
                                }
                            },
                            shape = ContinuousRoundedRectangle(16.dp),
                            color = when {
                                isEquals -> vibrantOrange
                                isOperator -> MaterialTheme.colorScheme.surfaceVariant
                                isAction -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, if (isEquals) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.height(54.dp)
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
                                        isEquals -> Color.White
                                        isAction -> MaterialTheme.colorScheme.error
                                        isOperator -> vibrantOrange
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

    // ==================== CALCULATION HISTORY SHEET ====================
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculation History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = { historyList = emptyList() }) {
                            Text("Clear History", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history calculations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { item ->
                            Surface(
                                onClick = {
                                    haptics.click()
                                    expression = item.expression
                                    showHistorySheet = false
                                },
                                shape = ContinuousRoundedRectangle(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.expression,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "= ${item.result}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = vibrantOrange
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

private fun formatResultNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    if (value == 0.0) return "0"

    val format = DecimalFormat("#,##0.########")
    return format.format(value)
}

/**
 * Safe Recursive Descent Parser & Math Evaluator
 */
private fun evaluateExpression(expr: String, isDegreeMode: Boolean): Double {
    var cleaned = expr.replace("×", "*")
        .replace("÷", "/")
        .replace("π", Math.PI.toString())
        .replace("e", Math.E.toString())
        .replace("√", "sqrt")

    if (cleaned.isBlank()) return 0.0

    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < cleaned.length) cleaned[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < cleaned.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos

            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = cleaned.substring(startPos, this.pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = cleaned.substring(startPos, this.pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(if (isDegreeMode) Math.toRadians(x) else x)
                    "cos" -> cos(if (isDegreeMode) Math.toRadians(x) else x)
                    "tan" -> tan(if (isDegreeMode) Math.toRadians(x) else x)
                    "ln" -> ln(x)
                    "log" -> log10(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }.parse()
}
