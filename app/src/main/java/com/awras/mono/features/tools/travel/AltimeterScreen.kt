package com.awras.mono.features.tools.travel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.WorldLatitude
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.*

enum class AltitudeUnit(val symbol: String) {
    METERS("m"),
    FEET("ft")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltimeterScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Altimeter State
    var rawBaroPressureHpa by remember { mutableFloatStateOf(1013.25f) }
    var seaLevelReferenceHpa by remember { mutableFloatStateOf(1013.25f) }
    var gpsAltitudeMeters by remember { mutableDoubleStateOf(0.0) }
    var demSeaLevelMeters by remember { mutableDoubleStateOf(0.0) }
    var hasBarometerSensor by remember { mutableStateOf(false) }
    var selectedUnit by remember { mutableStateOf(AltitudeUnit.METERS) }

    // Elevation History Buffer (60 points)
    val altitudeHistory = remember { mutableStateListOf<Float>().apply { repeat(60) { add(0f) } } }

    val scrollState = rememberScrollState()

    // Location Permission Launcher
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsAndDemElevation(context) { lat, lon, alt ->
                if (alt != null) gpsAltitudeMeters = alt
                if (lat != null && lon != null) {
                    fetchOpenMeteoDemElevation(lat, lon) { dem ->
                        if (dem != null) demSeaLevelMeters = dem
                    }
                }
            }
        }
    }

    // Register Barometer Pressure Sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val baroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (baroSensor != null) {
            hasBarometerSensor = true
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                        val press = event.values[0]
                        rawBaroPressureHpa = press
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, baroSensor, SensorManager.SENSOR_DELAY_UI)

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            hasBarometerSensor = false
            onDispose {}
        }
    }

    // Initial Location & DEM Elevation Fetch
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) {
            fetchGpsAndDemElevation(context) { lat, lon, alt ->
                if (alt != null) gpsAltitudeMeters = alt
                if (lat != null && lon != null) {
                    fetchOpenMeteoDemElevation(lat, lon) { dem ->
                        if (dem != null) demSeaLevelMeters = dem
                    }
                }
            }
        } else {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Calculate Barometric Height in Meters: h = 44330 * (1 - (P / P0)^0.1903)
    val calculatedBaroAltitudeMeters = if (hasBarometerSensor) {
        (44330.0 * (1.0 - (rawBaroPressureHpa / seaLevelReferenceHpa).toDouble().pow(0.1903))).toFloat()
    } else {
        demSeaLevelMeters.toFloat()
    }

    // Active Altitude (Fused Barometer + DEM fallback)
    val activeAltitudeMeters = if (hasBarometerSensor) calculatedBaroAltitudeMeters else demSeaLevelMeters.toFloat()

    // Smooth Altitude Motion
    val smoothAltitudeMeters by animateFloatAsState(
        targetValue = activeAltitudeMeters,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "SmoothAltitude"
    )

    // Update History Buffer
    LaunchedEffect(smoothAltitudeMeters) {
        if (altitudeHistory.size >= 60) {
            altitudeHistory.removeAt(0)
        }
        altitudeHistory.add(smoothAltitudeMeters)
    }

    // Unit Conversion
    val displayAltitude = if (selectedUnit == AltitudeUnit.METERS) smoothAltitudeMeters else smoothAltitudeMeters * 3.28084f
    val displayGps = if (selectedUnit == AltitudeUnit.METERS) gpsAltitudeMeters else gpsAltitudeMeters * 3.28084
    val displayDem = if (selectedUnit == AltitudeUnit.METERS) demSeaLevelMeters else demSeaLevelMeters * 3.28084

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Altimeter",
                description = "Sea-Level Elevation",
                icon = Icons.Outlined.Terrain,
                onBackClick = onBackClick,
                rightActionIcon = Icons.Default.Refresh,
                onRightActionClick = {
                    haptics.click()
                    seaLevelReferenceHpa = 1013.25f
                    altitudeHistory.clear()
                    repeat(60) { altitudeHistory.add(smoothAltitudeMeters) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. PROMINENT ALTITUDE READOUT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f", displayAltitude),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 68.sp),
                        fontWeight = FontWeight.Black,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = selectedUnit.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        color = vibrantOrange,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (hasBarometerSensor) "BAROMETRIC + DEM FUSED ELEVATION" else "OPEN-METEO DEM GROUND ELEVATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = vibrantOrange,
                    letterSpacing = 1.sp
                )
            }

            // 2. ELEVATION DATA SOURCES CARD GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevationSourceCard(
                    title = "GPS Altitude",
                    value = String.format(Locale.US, "%.0f %s", displayGps, selectedUnit.symbol),
                    subtitle = "WGS84 Ellipsoid",
                    icon = Icons.Default.MyLocation,
                    modifier = Modifier.weight(1f)
                )
                ElevationSourceCard(
                    title = "DEM Sea-Level",
                    value = String.format(Locale.US, "%.0f %s", displayDem, selectedUnit.symbol),
                    subtitle = "Open-Meteo Ground",
                    icon = TablerIcons.WorldLatitude,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. BAROMETRIC PRESSURE & SEA-LEVEL CALIBRATION CARD
            if (hasBarometerSensor) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(20.dp),
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
                                Icon(
                                    imageVector = Icons.Default.Compress,
                                    contentDescription = null,
                                    tint = vibrantOrange
                                )
                                Text(
                                    text = "BAROMETRIC PRESSURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = vibrantOrange,
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = String.format(Locale.US, "%.1f hPa", rawBaroPressureHpa),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "P0 Reference: %.2f hPa", seaLevelReferenceHpa),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        haptics.click()
                                        seaLevelReferenceHpa -= 0.5f
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "-0.5 hPa")
                                }
                                IconButton(
                                    onClick = {
                                        haptics.click()
                                        seaLevelReferenceHpa += 0.5f
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "+0.5 hPa")
                                }
                            }
                        }
                    }
                }
            }

            // 4. VERTICAL ELEVATION PROFILE GRAPH
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Elevation Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        if (altitudeHistory.size > 1) {
                            val minH = altitudeHistory.minOrNull() ?: 0f
                            val maxH = altitudeHistory.maxOrNull() ?: 100f
                            val rangeH = (maxH - minH).coerceAtLeast(10f)

                            val stepX = w / (altitudeHistory.size - 1).toFloat()
                            val path = Path()

                            for (i in altitudeHistory.indices) {
                                val altVal = altitudeHistory[i]
                                val x = i * stepX
                                val y = h - ((altVal - minH) / rangeH) * h

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = vibrantOrange,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            // 5. UNIT SWITCHER PILL (METERS vs FEET)
            Surface(
                modifier = Modifier.clip(ContinuousCapsule),
                shape = ContinuousCapsule,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AltitudeUnit.entries.forEach { unit ->
                        val isSelected = selectedUnit == unit
                        val bgColor = if (isSelected) vibrantOrange else Color.Transparent
                        val txtColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(bgColor)
                                .clickable {
                                    haptics.tick()
                                    selectedUnit = unit
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${unit.name} (${unit.symbol})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = txtColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ElevationSourceCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = modifier,
        shape = ContinuousRoundedRectangle(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = vibrantOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = vibrantOrange,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchGpsAndDemElevation(
    context: Context,
    onResult: (Double?, Double?, Double?) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val alt = if (loc.hasAltitude()) loc.altitude else null
                    onResult(loc.latitude, loc.longitude, alt)
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { last ->
                        val lastAlt = if (last != null && last.hasAltitude()) last.altitude else null
                        onResult(last?.latitude, last?.longitude, lastAlt)
                    }
                }
            }
            .addOnFailureListener {
                onResult(null, null, null)
            }
    } catch (_: Exception) {
        onResult(null, null, null)
    }
}

private fun fetchOpenMeteoDemElevation(
    lat: Double,
    lon: Double,
    onResult: (Double?) -> Unit
) {
    thread {
        try {
            val url = "https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lon"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val elevationArr = json.getJSONArray("elevation")
            if (elevationArr.length() > 0) {
                onResult(elevationArr.getDouble(0))
            } else {
                onResult(null)
            }
        } catch (_: Exception) {
            onResult(null)
        }
    }
}
