package com.awras.mono.features.tools.productivity

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.Geist
import com.awras.mono.ui.theme.GeistMono
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

enum class HardwareDomain(val displayName: String) {
    OVERVIEW("Overview"),
    SYSTEM("System & CPU"),
    MEMORY("RAM & Storage"),
    BATTERY("Battery & Power"),
    DISPLAY("Display"),
    SENSORS("Sensors"),
    NETWORK("Network")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var selectedDomain by remember { mutableStateOf(HardwareDomain.OVERVIEW) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Memory Info
    val memoryInfo = remember(refreshTrigger) { getMemoryInfo(context) }
    val storageInfo = remember(refreshTrigger) { getStorageInfo() }

    // Battery Info
    val batteryInfo = remember(refreshTrigger) { getBatteryInfo(context) }

    // Display Info
    val displayInfo = remember(refreshTrigger) { getDisplayInfo(context) }

    // Network Info
    val networkInfo = remember(refreshTrigger) { getNetworkInfo(context) }

    // Sensor Count
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensorsList = remember(sensorManager) { sensorManager.getSensorList(Sensor.TYPE_ALL) }

    // Copy Spec Summary
    fun copySpecSummary() {
        haptics.click()
        val summary = buildString {
            appendLine("# MONO SYSTEM & HARDWARE SPECIFICATIONS SHEET")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Security Patch: ${Build.VERSION.SECURITY_PATCH}")
            appendLine("Board / SoC: ${Build.BOARD} / ${Build.HARDWARE}")
            appendLine("CPU Architecture: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"} (${Runtime.getRuntime().availableProcessors()} Cores)")
            appendLine("RAM: ${formatBytes(memoryInfo.usedBytes)} used / ${formatBytes(memoryInfo.totalBytes)} total (${memoryInfo.usedPercentage}%)")
            appendLine("Storage: ${formatBytes(storageInfo.usedBytes)} used / ${formatBytes(storageInfo.totalBytes)} total (${storageInfo.usedPercentage}%)")
            appendLine("Battery: ${batteryInfo.level}% (${batteryInfo.tempC}°C, ${batteryInfo.voltage}mV) - ${batteryInfo.status}")
            appendLine("Display: ${displayInfo.widthPx}x${displayInfo.heightPx} px (${displayInfo.dpi} DPI, ${displayInfo.refreshRate}Hz)")
            appendLine("Network: ${networkInfo.connectionType} - IP: ${networkInfo.localIp}")
            appendLine("Sensors Count: ${sensorsList.size} Hardware Sensors")
            appendLine("System Uptime: ${getFormattedUptime()}")
        }

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mono Hardware Summary", summary)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Hardware summary copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Hardware",
                description = "System & Hardware Inspector with Live Diagnostics",
                icon = TablerIcons.DeviceMobile,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.Refresh,
                onRightActionClick = {
                    haptics.click()
                    refreshTrigger++
                    Toast.makeText(context, "Diagnostics refreshed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. HERO DEVICE OVERVIEW CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(vibrantOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TablerIcons.DeviceMobile,
                                    contentDescription = null,
                                    tint = vibrantOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Surface(
                            shape = ContinuousCapsule,
                            color = vibrantOrange
                        ) {
                            Text(
                                text = Build.HARDWARE.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Quick Gauge Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricBadge("RAM", "${memoryInfo.usedPercentage}%", vibrantOrange, Modifier.weight(1f))
                        QuickMetricBadge("STORAGE", "${storageInfo.usedPercentage}%", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                        QuickMetricBadge("BATTERY", "${batteryInfo.level}%", if (batteryInfo.tempC > 40) MaterialTheme.colorScheme.error else vibrantOrange, Modifier.weight(1f))
                        QuickMetricBadge("CORES", "${Runtime.getRuntime().availableProcessors()}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }

                    // Action Button: Copy Spec Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copySpecSummary() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ContinuousCapsule,
                            border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = TablerIcons.Copy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = vibrantOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy Spec Sheet Summary",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = vibrantOrange
                            )
                        }
                    }
                }
            }

            // ==================== 2. DOMAIN FILTER CHIPS ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HardwareDomain.entries.toTypedArray()) { domain ->
                    val isSelected = selectedDomain == domain
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.tick()
                            selectedDomain = domain
                        },
                        label = {
                            Text(
                                text = domain.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = ContinuousCapsule,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = vibrantOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ==================== 3. DOMAIN SECTIONS ====================

            // RAM & STORAGE DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.MEMORY) {
                SectionTitle("MEMORY & STORAGE GAUGE", vibrantOrange)

                // RAM Gauge Card
                GaugeCard(
                    title = "System RAM Memory",
                    subtitle = "${formatBytes(memoryInfo.usedBytes)} / ${formatBytes(memoryInfo.totalBytes)} (${memoryInfo.usedPercentage}% Used)",
                    progress = memoryInfo.usedPercentage / 100f,
                    vibrantOrange = vibrantOrange,
                    details = listOf(
                        "Total Physical RAM" to formatBytes(memoryInfo.totalBytes),
                        "Available Free RAM" to formatBytes(memoryInfo.availableBytes),
                        "Used RAM" to formatBytes(memoryInfo.usedBytes),
                        "Low Memory Threshold" to formatBytes(memoryInfo.thresholdBytes)
                    )
                )

                // Storage Gauge Card
                GaugeCard(
                    title = "Internal Storage Space",
                    subtitle = "${formatBytes(storageInfo.usedBytes)} / ${formatBytes(storageInfo.totalBytes)} (${storageInfo.usedPercentage}% Used)",
                    progress = storageInfo.usedPercentage / 100f,
                    vibrantOrange = MaterialTheme.colorScheme.tertiary,
                    details = listOf(
                        "Total Storage Capacity" to formatBytes(storageInfo.totalBytes),
                        "Available Free Space" to formatBytes(storageInfo.availableBytes),
                        "Used Storage" to formatBytes(storageInfo.usedBytes)
                    )
                )
            }

            // BATTERY DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.BATTERY) {
                SectionTitle("BATTERY & THERMAL HEALTH", vibrantOrange)

                GaugeCard(
                    title = "Battery Level & Health",
                    subtitle = "${batteryInfo.level}% • ${batteryInfo.status} (${batteryInfo.tempC}°C)",
                    progress = batteryInfo.level / 100f,
                    vibrantOrange = if (batteryInfo.tempC > 40) MaterialTheme.colorScheme.error else vibrantOrange,
                    details = listOf(
                        "Battery Charge Level" to "${batteryInfo.level}%",
                        "Charging Status" to batteryInfo.status,
                        "Power Source" to batteryInfo.pluggedSource,
                        "Battery Temperature" to "${batteryInfo.tempC} °C",
                        "Battery Voltage" to "${batteryInfo.voltage} mV",
                        "Battery Health" to batteryInfo.health,
                        "Battery Technology" to batteryInfo.technology
                    )
                )
            }

            // SYSTEM & CPU DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.SYSTEM) {
                SectionTitle("SYSTEM & PROCESSOR SPECS", vibrantOrange)

                SpecDetailCard(
                    title = "System & Hardware Specs",
                    items = listOf(
                        "Manufacturer" to Build.MANUFACTURER,
                        "Model" to Build.MODEL,
                        "Device Code" to Build.DEVICE,
                        "Product Name" to Build.PRODUCT,
                        "Board" to Build.BOARD,
                        "SoC Hardware" to Build.HARDWARE,
                        "CPU Architecture" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
                        "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", "),
                        "CPU Core Count" to "${Runtime.getRuntime().availableProcessors()} Cores",
                        "Android OS" to "Android ${Build.VERSION.RELEASE}",
                        "API Level" to "API ${Build.VERSION.SDK_INT}",
                        "Security Patch" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
                        "Build Display ID" to Build.DISPLAY,
                        "Kernel OS Version" to (System.getProperty("os.version") ?: "Unknown"),
                        "System Uptime" to getFormattedUptime()
                    ),
                    vibrantOrange = vibrantOrange
                )
            }

            // DISPLAY DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.DISPLAY) {
                SectionTitle("DISPLAY & RESOLUTION", vibrantOrange)

                SpecDetailCard(
                    title = "Display Hardware Specs",
                    items = listOf(
                        "Screen Resolution" to "${displayInfo.widthPx} x ${displayInfo.heightPx} px",
                        "Display Density" to "${displayInfo.dpi} DPI (${displayInfo.density}x)",
                        "Screen Refresh Rate" to "${displayInfo.refreshRate} Hz",
                        "HDR Supported" to if (displayInfo.isHdr) "Yes (High Dynamic Range)" else "Standard Dynamic Range"
                    ),
                    vibrantOrange = vibrantOrange
                )
            }

            // NETWORK DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.NETWORK) {
                SectionTitle("NETWORK & CONNECTIVITY", vibrantOrange)

                SpecDetailCard(
                    title = "Network Diagnostics",
                    items = listOf(
                        "Active Connection" to networkInfo.connectionType,
                        "Local IPv4 Address" to networkInfo.localIp,
                        "Wi-Fi Connection State" to networkInfo.wifiState,
                        "SSID / Network" to networkInfo.ssid
                    ),
                    vibrantOrange = vibrantOrange
                )
            }

            // SENSORS DOMAIN
            if (selectedDomain == HardwareDomain.OVERVIEW || selectedDomain == HardwareDomain.SENSORS) {
                SectionTitle("HARDWARE SENSORS INVENTORY (${sensorsList.size})", vibrantOrange)

                SpecDetailCard(
                    title = "Hardware Sensors List",
                    items = sensorsList.take(20).map { sensor ->
                        sensor.name to getSensorTypeName(sensor.type)
                    },
                    vibrantOrange = vibrantOrange
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, vibrantOrange: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = vibrantOrange,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun QuickMetricBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ContinuousRoundedRectangle(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                fontFamily = GeistMono,
                color = color,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun GaugeCard(
    title: String,
    subtitle: String,
    progress: Float,
    vibrantOrange: Color,
    details: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ContinuousRoundedRectangle(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = GeistMono,
                    color = vibrantOrange
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(ContinuousCapsule),
                color = vibrantOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                details.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeistMono,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecDetailCard(
    title: String,
    items: List<Pair<String, String>>,
    vibrantOrange: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ContinuousRoundedRectangle(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeistMono,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}

// Data structures & Android System Inspectors
private data class RamInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val thresholdBytes: Long,
    val usedPercentage: Int
)

private data class DiskInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercentage: Int
)

private data class PowerInfo(
    val level: Int,
    val status: String,
    val pluggedSource: String,
    val tempC: Float,
    val voltage: Int,
    val health: String,
    val technology: String
)

private data class DisplaySpecs(
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int,
    val density: Float,
    val refreshRate: Int,
    val isHdr: Boolean
)

private data class NetworkSpecs(
    val connectionType: String,
    val localIp: String,
    val wifiState: String,
    val ssid: String
)

private fun getMemoryInfo(context: Context): RamInfo {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryErrorState() // benign call
        am.getMemoryInfo(mi)

        val total = mi.totalMem
        val avail = mi.availMem
        val used = total - avail
        val pct = if (total > 0) ((used.toDouble() / total.toDouble()) * 100).toInt() else 0

        RamInfo(
            totalBytes = total,
            availableBytes = avail,
            usedBytes = used,
            thresholdBytes = mi.threshold,
            usedPercentage = pct
        )
    } catch (e: Exception) {
        RamInfo(0, 0, 0, 0, 0)
    }
}

private fun ActivityManager.getMemoryErrorState() {}

private fun getStorageInfo(): DiskInfo {
    return try {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong

        val total = totalBlocks * blockSize
        val avail = availBlocks * blockSize
        val used = total - avail
        val pct = if (total > 0) ((used.toDouble() / total.toDouble()) * 100).toInt() else 0

        DiskInfo(
            totalBytes = total,
            availableBytes = avail,
            usedBytes = used,
            usedPercentage = pct
        )
    } catch (e: Exception) {
        DiskInfo(0, 0, 0, 0)
    }
}

private fun getBatteryInfo(context: Context): PowerInfo {
    return try {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0

        val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedStr = when (pluggedInt) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> "On Battery"
        }

        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val volt = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        PowerInfo(
            level = pct,
            status = statusStr,
            pluggedSource = pluggedStr,
            tempC = temp,
            voltage = volt,
            health = healthStr,
            technology = tech
        )
    } catch (e: Exception) {
        PowerInfo(0, "Unknown", "Unknown", 0f, 0, "Unknown", "Li-ion")
    }
}

private fun getDisplayInfo(context: Context): DisplaySpecs {
    val dm = context.resources.displayMetrics
    val refresh = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.mode?.refreshRate?.toInt() ?: 60
        } else 60
    } catch (e: Exception) { 60 }

    val isHdr = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.isHdr ?: false
        } else false
    } catch (e: Exception) { false }

    return DisplaySpecs(
        widthPx = dm.widthPixels,
        heightPx = dm.heightPixels,
        dpi = dm.densityDpi,
        density = dm.density,
        refreshRate = refresh,
        isHdr = isHdr
    )
}

private fun getNetworkInfo(context: Context): NetworkSpecs {
    var type = "Disconnected"
    var ip = getLocalIpAddress()
    var wifiState = "Disabled"
    var ssid = "Not Connected"

    try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)

        if (caps != null) {
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    type = "Wi-Fi Network"
                    wifiState = "Connected"
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ssid = wm.connectionInfo?.ssid?.replace("\"", "") ?: "Wi-Fi"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    type = "Cellular Mobile Data"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    type = "Ethernet Cable"
                }
            }
        }
    } catch (e: Exception) {
        type = "Active"
    }

    return NetworkSpecs(
        connectionType = type,
        localIp = ip,
        wifiState = wifiState,
        ssid = ssid
    )
}

private fun getLocalIpAddress(): String {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress
                    if (sAddr != null && !sAddr.contains(':')) {
                        return sAddr
                    }
                }
            }
        }
    } catch (e: Exception) {
        return "127.0.0.1"
    }
    return "127.0.0.1"
}

private fun getFormattedUptime(): String {
    val uptimeMs = SystemClock.elapsedRealtime()
    val hours = uptimeMs / (1000 * 3600)
    val mins = (uptimeMs % (1000 * 3600)) / (1000 * 60)
    return "$hours hrs, $mins mins"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.ENGLISH, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun getSensorTypeName(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
        Sensor.TYPE_PRESSURE -> "Barometer (Pressure)"
        Sensor.TYPE_LIGHT -> "Ambient Light Sensor"
        Sensor.TYPE_PROXIMITY -> "Proximity Sensor"
        Sensor.TYPE_GRAVITY -> "Gravity Sensor"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
        Sensor.TYPE_STEP_COUNTER -> "Pedometer Step Counter"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
        Sensor.TYPE_HEART_RATE -> "Heart Rate Monitor"
        else -> "Hardware Sensor ($type)"
    }
}
