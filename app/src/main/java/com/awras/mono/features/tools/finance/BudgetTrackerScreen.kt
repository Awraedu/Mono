package com.awras.mono.features.tools.finance

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.GeistMono
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Wallet
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

enum class TransactionType {
    EXPENSE, INCOME
}

data class TransactionItem(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis()
)

private const val PREFS_NAME = "mono_budget_prefs"
private const val KEY_TRANSACTIONS = "transactions_json"

private fun loadTransactions(context: Context): List<TransactionItem> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
    val list = mutableListOf<TransactionItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TransactionItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    category = obj.getString("category"),
                    type = TransactionType.valueOf(obj.getString("type")),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
    } catch (e: Exception) {
        // Fallback
    }
    return list
}

private fun saveTransactions(context: Context, list: List<TransactionItem>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val array = JSONArray()
    list.forEach { tx ->
        val obj = JSONObject().apply {
            put("id", tx.id)
            put("title", tx.title)
            put("amount", tx.amount)
            put("category", tx.category)
            put("type", tx.type.name)
            put("timestamp", tx.timestamp)
        }
        array.put(obj)
    }
    prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Persistent Transactions State (Loaded from SharedPreferences)
    var transactions by remember { mutableStateOf(loadTransactions(context)) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Add Transaction Bottom Sheet
    var showAddSheet by remember { mutableStateOf(false) }
    var addTitleInput by remember { mutableStateOf("") }
    var addAmountInput by remember { mutableStateOf("") }
    var addCategoryInput by remember { mutableStateOf("Food") }
    var addTypeInput by remember { mutableStateOf(TransactionType.EXPENSE) }

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }

    // Totals
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val filteredList = remember(transactions, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") transactions
        else transactions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    val categories = listOf("All", "Food", "Income", "Utilities", "Shopping", "Transport")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Budget",
                description = "Expense & Income Flow Tracker",
                icon = TablerIcons.Wallet,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Add,
                onRightActionClick = {
                    haptics.click()
                    showAddSheet = true
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
            // ==================== 1. BALANCE SUMMARY HERO CARD ====================
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
                                text = "NET BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                haptics.click()
                                showAddSheet = true
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
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.size(14.dp),
                                    tint = vibrantOrange
                                )
                                Text(
                                    text = "ADD ENTRY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Main Net Balance Readout
                    Text(
                        text = currencyFormat.format(netBalance),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        color = if (netBalance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        letterSpacing = (-1).sp
                    )

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
                                text = "INCOME: ${currencyFormat.format(totalIncome)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "SPENT: ${currencyFormat.format(totalExpense)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ==================== 2. CATEGORY FILTER CHIPS ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategoryFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.click()
                            selectedCategoryFilter = cat
                        },
                        label = { Text(cat.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ==================== 3. MATERIAL 3 EXPRESSIVE SEGMENTED LIST ITEMS ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTIONS LOG (${filteredList.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = vibrantOrange,
                    letterSpacing = 1.sp
                )

                if (transactions.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            haptics.click()
                            transactions = emptyList()
                            saveTransactions(context, emptyList())
                        }
                    ) {
                        Text("Clear All", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = ContinuousRoundedRectangle(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = TablerIcons.Wallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No entries logged yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap + ADD ENTRY to log your first income or expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        // Material 3 Expressive Segmented Surface List Item
                        Surface(
                            shape = ContinuousRoundedRectangle(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            ListItem(
                                leadingContent = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (item.type == TransactionType.INCOME) vibrantOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                    ) {
                                        Icon(
                                            imageVector = if (item.type == TransactionType.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (item.type == TransactionType.INCOME) vibrantOrange else MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .size(20.dp)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = item.category.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${if (item.type == TransactionType.INCOME) "+" else "-"}${currencyFormat.format(item.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = GeistMono,
                                            color = if (item.type == TransactionType.INCOME) vibrantOrange else MaterialTheme.colorScheme.error
                                        )

                                        IconButton(
                                            onClick = {
                                                haptics.tick()
                                                val updated = transactions.filter { it.id != item.id }
                                                transactions = updated
                                                saveTransactions(context, updated)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== ADD TRANSACTION MATERIAL BOTTOM SHEET ====================
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Type Toggle: EXPENSE vs INCOME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TransactionType.EXPENSE to "EXPENSE (-)", TransactionType.INCOME to "INCOME (+)").forEach { (type, label) ->
                        val isSelected = addTypeInput == type
                        Surface(
                            onClick = {
                                haptics.click()
                                addTypeInput = type
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule,
                            color = if (isSelected) (if (type == TransactionType.INCOME) vibrantOrange else MaterialTheme.colorScheme.error) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = addTitleInput,
                    onValueChange = { addTitleInput = it },
                    label = { Text("Title / Description") },
                    placeholder = { Text("e.g. Grocery Shopping") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                OutlinedTextField(
                    value = addAmountInput,
                    onValueChange = { addAmountInput = it },
                    label = { Text("Amount ($)") },
                    placeholder = { Text("e.g. 50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Food", "Income", "Utilities", "Shopping", "Transport")) { cat ->
                            val isSelected = addCategoryInput == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.click()
                                    addCategoryInput = cat
                                },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                shape = ContinuousCapsule,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = vibrantOrange, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amountParsed = addAmountInput.toDoubleOrNull() ?: 0.0
                        if (addTitleInput.isBlank() || amountParsed <= 0) {
                            Toast.makeText(context, "Please enter valid title and amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        haptics.success()
                        val newTx = TransactionItem(
                            id = "${System.currentTimeMillis()}",
                            title = addTitleInput,
                            amount = amountParsed,
                            category = addCategoryInput,
                            type = addTypeInput
                        )
                        val updated = listOf(newTx) + transactions
                        transactions = updated
                        saveTransactions(context, updated)

                        addTitleInput = ""
                        addAmountInput = ""
                        showAddSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    shape = ContinuousCapsule,
                    colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                ) {
                    Text("Add Entry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
