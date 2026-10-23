package com.awras.mono.features.tools.travel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.SignalOrange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class HourlyPoint(val time: String, val temp: Int, val isDay: Boolean, val code: Int)
data class DailyForecast(val dayName: String, val minTemp: Int, val maxTemp: Int, val pop: Int, val code: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // 1. LIVE HARDWARE SENSOR COMPASS HEADING
    val rawDeviceHeading by rememberDeviceHeading()
    val animatedDeviceHeading by animateFloatAsState(
        targetValue = rawDeviceHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassRotation"
    )

    // Adaptive Theme Tokens
    val colors = MaterialTheme.colorScheme
    val cardTrackColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFEDEFEF)
    val textPrimary = colors.onSurface
    val textSecondary = colors.secondary
    val textTertiary = colors.tertiary
    val accentOrange = SignalOrange
    val rainBlue = Color(0xFF388AF6)
    val graphLine = Color(0xFF2A8B82)
    val pressureLine = Color(0xFF9065D0)

    var cityName by remember { mutableStateOf("Locating...") }
    var activeLat by remember { mutableDoubleStateOf(9.0320) } // Addis Ababa default
    var activeLon by remember { mutableDoubleStateOf(38.7469) }

    // Core Metrics
    var currentTemp by remember { mutableIntStateOf(16) }
    var apparentTemp by remember { mutableIntStateOf(15) }
    var weatherDesc by remember { mutableStateOf("Mainly clear") }
    var isDay by remember { mutableStateOf(false) }

    var precipitationVolume by remember { mutableDoubleStateOf(0.0) }
    var windSpeedMph by remember { mutableIntStateOf(4) }
    var windDirectionStr by remember { mutableStateOf("E") }
    var windDirectionDeg by remember { mutableDoubleStateOf(90.0) }
    var currentCloudPct by remember { mutableIntStateOf(29) }
    var uvIndexVal by remember { mutableDoubleStateOf(8.0) }
    var aqiVal by remember { mutableIntStateOf(76) }
    var humidityPct by remember { mutableIntStateOf(47) }
    var dewPointVal by remember { mutableIntStateOf(2) }
    var currentPressureInHg by remember { mutableDoubleStateOf(30.07) }

    var sunriseIso by remember { mutableStateOf("2026-10-23T06:13") }
    var sunsetIso by remember { mutableStateOf("2026-10-23T18:05") }
    var sunriseStr by remember { mutableStateOf("6:13 am") }
    var sunsetStr by remember { mutableStateOf("6:05 pm") }
    var daylightDurationStr by remember { mutableStateOf("11h 52m of daylight") }

    val hourlyList = remember { mutableStateListOf<HourlyPoint>() }
    val dailyList = remember { mutableStateListOf<DailyForecast>() }
    val cloudCoverHistory = remember { mutableStateListOf<Int>() }
    val pressureHistory = remember { mutableStateListOf<Double>() }

    fun requestLocation() {
        getUserLocation(context) { lat, lon ->
            if (lat != null && lon != null) {
                activeLat = lat
                activeLon = lon
                getCityName(context, lat, lon) { name ->
                    cityName = name ?: "Current Location"
                }
            } else {
                cityName = "Current Location"
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) requestLocation()
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) requestLocation() else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // Open-Meteo Query
    LaunchedEffect(activeLat, activeLon) {
        withContext(Dispatchers.IO) {
            try {
                val forecastDeferred = async {
                    val url = "https://api.open-meteo.com/v1/forecast?latitude=$activeLat&longitude=$activeLon" +
                            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,dew_point_2m" +
                            "&hourly=temperature_2m,weather_code,is_day,cloud_cover,pressure_msl" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,daylight_duration,uv_index_max,precipitation_probability_max" +
                            "&timezone=auto"
                    URL(url).readText()
                }

                val aqiDeferred = async {
                    try {
                        val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$activeLat&longitude=$activeLon&current=us_aqi&timezone=auto"
                        URL(aqiUrl).readText()
                    } catch (_: Exception) { null }
                }

                val forecastJson = JSONObject(forecastDeferred.await())
                val current = forecastJson.getJSONObject("current")
                val hourly = forecastJson.getJSONObject("hourly")
                val daily = forecastJson.getJSONObject("daily")

                currentTemp = current.getDouble("temperature_2m").roundToInt()
                apparentTemp = current.getDouble("apparent_temperature").roundToInt()
                humidityPct = current.getInt("relative_humidity_2m")
                dewPointVal = current.optDouble("dew_point_2m", 2.0).roundToInt()
                windSpeedMph = (current.getDouble("wind_speed_10m") * 0.621371).roundToInt()
                windDirectionDeg = current.getDouble("wind_direction_10m")
                val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                windDirectionStr = dirs[((windDirectionDeg / 45.0).roundToInt() % 8)]

                val mslHpa = current.optDouble("pressure_msl", 1018.0)
                currentPressureInHg = mslHpa * 0.02953
                currentCloudPct = current.optInt("cloud_cover", 29)
                precipitationVolume = current.optDouble("precipitation", 0.0)
                isDay = current.getInt("is_day") == 1
                weatherDesc = getWeatherDesc(current.getInt("weather_code"))

                val uvs = daily.getJSONArray("uv_index_max")
                uvIndexVal = if (uvs.length() > 0) uvs.getDouble(0) else 8.0

                val sunrises = daily.getJSONArray("sunrise")
                val sunsets = daily.getJSONArray("sunset")
                if (sunrises.length() > 0) {
                    sunriseIso = sunrises.getString(0)
                    sunriseStr = formatTime12h(sunriseIso)
                }
                if (sunsets.length() > 0) {
                    sunsetIso = sunsets.getString(0)
                    sunsetStr = formatTime12h(sunsetIso)
                }

                val daylightSec = daily.optJSONArray("daylight_duration")?.optDouble(0, 42720.0) ?: 42720.0
                daylightDurationStr = "${(daylightSec / 3600).toInt()}h ${((daylightSec % 3600) / 60).toInt()}m of daylight"

                val aqiRes = aqiDeferred.await()
                if (aqiRes != null) {
                    aqiVal = JSONObject(aqiRes).optJSONObject("current")?.optInt("us_aqi", 76) ?: 76
                }

                // Hourly Parse
                hourlyList.clear()
                cloudCoverHistory.clear()
                pressureHistory.clear()

                val hTimes = hourly.getJSONArray("time")
                val hTemps = hourly.getJSONArray("temperature_2m")
                val hCodes = hourly.getJSONArray("weather_code")
                val hIsDay = hourly.getJSONArray("is_day")
                val hClouds = hourly.optJSONArray("cloud_cover")
                val hPressures = hourly.optJSONArray("pressure_msl")

                val curTimeIso = current.getString("time").substringBefore(":")
                var sIdx = 0
                for (i in 0 until hTimes.length()) {
                    if (hTimes.getString(i).startsWith(curTimeIso)) { sIdx = i; break }
                }

                for (i in sIdx until minOf(sIdx + 24, hTimes.length())) {
                    if (hClouds != null) cloudCoverHistory.add(hClouds.getInt(i))
                    if (hPressures != null) pressureHistory.add(hPressures.getDouble(i) * 0.02953)

                    if (i < sIdx + 5) {
                        val rawHour = hTimes.getString(i).substringAfter("T").substringBefore(":").toInt()
                        val label = when {
                            i == sIdx -> "Now"
                            rawHour == 0 -> "12 AM"
                            rawHour == 12 -> "12 PM"
                            rawHour > 12 -> "${rawHour - 12} PM"
                            else -> "$rawHour AM"
                        }
                        hourlyList.add(HourlyPoint(label, hTemps.getDouble(i).roundToInt(), hIsDay.getInt(i) == 1, hCodes.getInt(i)))
                    }
                }

                // 7-Day Parse
                dailyList.clear()
                val dTimes = daily.getJSONArray("time")
                val dMax = daily.getJSONArray("temperature_2m_max")
                val dMin = daily.getJSONArray("temperature_2m_min")
                val dCodes = daily.getJSONArray("weather_code")
                val dPops = daily.optJSONArray("precipitation_probability_max")
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfOut = SimpleDateFormat("EEEE", Locale.US)

                for (i in 0 until minOf(7, dTimes.length())) {
                    val dayStr = if (i == 0) "Today" else if (i == 1) "Tomorrow" else {
                        val parsed = sdfIn.parse(dTimes.getString(i))
                        if (parsed != null) {
                            val full = sdfOut.format(parsed)
                            if (full.equals("Wednesday", ignoreCase = true)) "Wed" else full
                        } else "Day $i"
                    }
                    val pop = if (dPops != null && !dPops.isNull(i)) dPops.getInt(i) else 0
                    dailyList.add(DailyForecast(dayStr, dMin.getDouble(i).roundToInt(), dMax.getDouble(i).roundToInt(), pop, dCodes.getInt(i)))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            DatumToolTopBar(
                title = cityName,
                description = "Weather",
                icon = TablerIcons.Cloud,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HERO WEATHER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isDay) TablerIcons.Sun else TablerIcons.Moon,
                    contentDescription = null,
                    tint = if (isDay) accentOrange else Color(0xFF6B7E93),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "$currentTemp°",
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = (-4).sp,
                    lineHeight = 96.sp
                )
                Text(
                    text = weatherDesc,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Feels like $apparentTemp°",
                    fontSize = 15.sp,
                    color = textSecondary
                )
            }

            // 2. HOURLY FORECAST
            SectionHeader("Hourly forecast", textPrimary)
            DatumCard {
                Column(modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp)) {
                    HourlyInlineGraph(
                        hourly = hourlyList,
                        lineColor = graphLine,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentOrange = accentOrange
                    )
                }
            }

            // 3. 7-DAY FORECAST
            SectionHeader("7-Day Forecast", textPrimary)
            DatumCard {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val minAll = dailyList.minOfOrNull { it.minTemp } ?: 10
                    val maxAll = dailyList.maxOfOrNull { it.maxTemp } ?: 25

                    dailyList.forEachIndexed { index, day ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.dayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(82.dp)
                            )

                            Row(
                                modifier = Modifier.width(66.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (day.pop > 30) TablerIcons.CloudRain else TablerIcons.Sun,
                                    contentDescription = null,
                                    tint = if (day.pop > 30) rainBlue else accentOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (day.pop > 0) {
                                    Text(
                                        text = "${day.pop}%",
                                        fontSize = 12.sp,
                                        color = rainBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "${day.minTemp}°",
                                fontSize = 14.sp,
                                color = textSecondary,
                                modifier = Modifier.width(28.dp)
                            )

                            ForecastRangeBar(
                                min = day.minTemp,
                                max = day.maxTemp,
                                curTemp = if (index == 0) currentTemp else null,
                                minAll = minAll,
                                maxAll = maxAll,
                                barColor = accentOrange,
                                trackColor = cardTrackColor,
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(6.dp)
                                    .padding(horizontal = 4.dp)
                            )

                            Text(
                                text = "${day.maxTemp}°",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.width(28.dp)
                            )
                        }
                    }
                }
            }

            // 4. TODAY BENTO STACK
            SectionHeader("Today", textPrimary)

            RainStatusCard(precipitationVolume = precipitationVolume, trackColor = cardTrackColor)

            // LIVE SENSOR-DRIVEN ROTATING WIND COMPASS
            WindCompassCard(
                speed = windSpeedMph,
                direction = windDirectionStr,
                windDegrees = windDirectionDeg,
                deviceHeading = animatedDeviceHeading,
                dialBg = cardTrackColor,
                textSecondary = textSecondary
            )

            CloudCoverCard(
                cloudCover = currentCloudPct,
                history = cloudCoverHistory,
                graphColor = graphLine,
                textTertiary = textTertiary,
                textSecondary = textSecondary
            )

            UvIndexCard(
                uvIndex = uvIndexVal,
                accentOrange = accentOrange,
                textSecondary = textSecondary
            )

            AirQualityCard(
                aqi = aqiVal,
                accentOrange = accentOrange,
                trackColor = cardTrackColor
            )

            HumidityCard(
                humidity = humidityPct,
                dewPoint = dewPointVal,
                chipBg = cardTrackColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            SunArcCard(
                sunriseIso = sunriseIso,
                sunsetIso = sunsetIso,
                sunriseStr = sunriseStr,
                sunsetStr = sunsetStr,
                daylightStr = daylightDurationStr,
                isDay = isDay,
                cardBorder = colors.outline,
                accentOrange = accentOrange,
                textSecondary = textSecondary
            )

            PressureCard(
                pressureInHg = currentPressureInHg,
                history = pressureHistory,
                graphColor = pressureLine,
                textTertiary = textTertiary,
                textSecondary = textSecondary
            )

            MoonPhaseCard(trackColor = cardTrackColor)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// MARK: - Device Sensors & Compass Implementation
// -------------------------------------------------------------

/**
 * Listens to physical phone movement via SensorManager (TYPE_ROTATION_VECTOR)
 * and returns the continuous unwrapped azimuth in degrees.
 */
@Composable
private fun rememberDeviceHeading(): State<Float> {
    val context = LocalContext.current
    val headingState = remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        var cumulativeAngle = 0f

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val rawDeg = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0

                    // Shortest-path angle unwrapping
                    var diff = (rawDeg.toFloat() - (cumulativeAngle % 360f))
                    diff = (diff + 540f) % 360f - 180f
                    cumulativeAngle += diff
                    headingState.floatValue = cumulativeAngle
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    val rawDeg = (event.values[0] + 360f) % 360f
                    var diff = (rawDeg - (cumulativeAngle % 360f))
                    diff = (diff + 540f) % 360f - 180f
                    cumulativeAngle += diff
                    headingState.floatValue = cumulativeAngle
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return headingState
}

/**
 * Real-time Hardware Animated Compass
 * The dial turns opposite to the phone rotation (-deviceHeading) so N stays on North.
 */
@Composable
private fun WindCompassCard(
    speed: Int,
    direction: String,
    windDegrees: Double,
    deviceHeading: Float,
    dialBg: Color,
    textSecondary: Color
) {
    CleanCardBase(
        title = "Wind",
        value = "$speed mph",
        subtitle = "Light breeze · From $direction"
    ) {
        Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            // Dial rotates live against physical phone movement
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-deviceHeading),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = dialBg, radius = size.minDimension / 2)
                }

                Text("N", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp))
                Text("S", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp))
                Text("E", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
                Text("W", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))

                // The wind stream rotates inside the dial to reflect true wind direction
                Icon(
                    imageVector = TablerIcons.Wind,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate((windDegrees - 90.0).toFloat())
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MARK: - Bespoke Cards & Utilities
// -------------------------------------------------------------

@Composable
private fun DatumCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp, bottom = 2.dp)
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun HourlyInlineGraph(
    hourly: List<HourlyPoint>,
    lineColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentOrange: Color
) {
    if (hourly.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        hourly.forEach { h ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = h.time,
                    fontSize = 13.sp,
                    color = textSecondary,
                    fontWeight = if (h.time == "Now") FontWeight.Bold else FontWeight.Normal
                )
                Icon(
                    imageVector = if (h.isDay) TablerIcons.Sun else TablerIcons.Moon,
                    contentDescription = null,
                    tint = if (h.isDay) accentOrange else Color(0xFF6B7E93),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(42.dp)) {
        val count = hourly.size
        val minT = hourly.minOf { it.temp }
        val maxT = hourly.maxOf { it.temp }
        val range = (maxT - minT).coerceAtLeast(1).toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / (count - 1)
            val paddingY = 8.dp.toPx()
            val usableH = size.height - (paddingY * 2)

            val points = hourly.mapIndexed { idx, item ->
                val normY = 1f - ((item.temp - minT) / range)
                Offset(idx * stepX, paddingY + normY * usableH)
            }

            for (i in 0 until points.size - 1) {
                val start = Offset(points[i].x + 16.dp.toPx(), points[i].y)
                val end = Offset(points[i + 1].x - 16.dp.toPx(), points[i + 1].y)
                if (end.x > start.x) {
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            if (points.isNotEmpty()) {
                val last = points.last()
                drawLine(
                    color = lineColor,
                    start = Offset(last.x + 16.dp.toPx(), last.y),
                    end = Offset(size.width, last.y),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            hourly.forEach { h ->
                val normY = 1f - ((h.temp - minT) / range)
                val offsetDp = (normY * 18).dp
                Text(
                    text = "${h.temp}°",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.offset(y = offsetDp - 9.dp)
                )
            }
        }
    }
}

@Composable
private fun ForecastRangeBar(
    min: Int,
    max: Int,
    curTemp: Int?,
    minAll: Int,
    maxAll: Int,
    barColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalRange = (maxAll - minAll).coerceAtLeast(1).toFloat()
        val startF = ((min - minAll) / totalRange).coerceIn(0f, 1f)
        val endF = ((max - minAll) / totalRange).coerceIn(0f, 1f)

        drawRoundRect(color = trackColor, size = size, cornerRadius = CornerRadius(size.height / 2, size.height / 2))
        val left = size.width * startF
        val width = (size.width * (endF - startF)).coerceAtLeast(size.height)
        drawRoundRect(color = barColor, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2, size.height / 2))

        if (curTemp != null) {
            val curF = ((curTemp - minAll) / totalRange).coerceIn(startF, endF)
            val dotX = size.width * curF
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(dotX, size.height / 2))
            drawCircle(color = barColor, radius = 2.5.dp.toPx(), center = Offset(dotX, size.height / 2))
        }
    }
}

@Composable
private fun RainStatusCard(precipitationVolume: Double, trackColor: Color) {
    CleanCardBase(
        title = "Rain",
        value = if (precipitationVolume <= 0.05) "None" else "${precipitationVolume} mm",
        subtitle = if (precipitationVolume <= 0.05) "All clear" else "Rain showers active"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Box(modifier = Modifier.width(22.dp).height(48.dp).clip(RoundedCornerShape(11.dp)).background(trackColor))
            }
        }
    }
}

@Composable
private fun CloudCoverCard(
    cloudCover: Int,
    history: List<Int>,
    graphColor: Color,
    textTertiary: Color,
    textSecondary: Color
) {
    CleanCardBase(
        title = "Cloud Cover",
        value = "$cloudCover%",
        subtitle = if (cloudCover < 30) "Mostly clear" else if (cloudCover < 70) "Partly cloudy" else "Overcast"
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(110.dp)) {
            Text("100", fontSize = 8.sp, color = textTertiary)
            Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                val pts = history.take(24).ifEmpty { listOf(cloudCover, cloudCover) }
                val maxV = 100f
                val stepX = size.width / (pts.size - 1).coerceAtLeast(1)

                val mappedPts = pts.mapIndexed { i, v ->
                    val normY = 1f - (v.toFloat() / maxV)
                    Offset(i * stepX, normY * size.height)
                }

                val path = Path().apply { smoothCurve(mappedPts) }
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(graphColor.copy(alpha = 0.25f), Color.Transparent)))
                drawPath(path = path, color = graphColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", fontSize = 8.sp, color = textTertiary)
                Text("Now", fontSize = 8.sp, color = textSecondary)
                Text("7 AM", fontSize = 8.sp, color = textSecondary)
            }
        }
    }
}

@Composable
private fun UvIndexCard(uvIndex: Double, accentOrange: Color, textSecondary: Color) {
    CleanCardBase(
        title = "UV Index",
        value = "${uvIndex.roundToInt()} · ${getUvCategory(uvIndex)}",
        subtitle = "Tomorrow's expected peak"
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.1f, 0.2f, 0.35f, 0.55f, 0.8f, 1.0f, 0.9f, 0.75f, 0.5f, 0.3f, 0.15f, 0.05f)
                heights.forEach { frac ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(frac)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentOrange.copy(alpha = 0.35f + (frac * 0.65f)))
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("6 AM", fontSize = 7.sp, color = textSecondary)
                Text("12 PM", fontSize = 7.sp, color = textSecondary)
                Text("6 PM", fontSize = 7.sp, color = textSecondary)
            }
        }
    }
}

@Composable
private fun AirQualityCard(aqi: Int, accentOrange: Color, trackColor: Color) {
    CleanCardBase(
        title = "Air quality",
        value = "AQI $aqi",
        subtitle = if (aqi <= 50) "Good" else if (aqi <= 100) "Moderate" else "Unhealthy"
    ) {
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 6.dp.toPx()
                val totalSweep = 260f
                val startAng = 140f
                val segmentCount = 6
                val gap = 12f
                val chunk = (totalSweep - (segmentCount - 1) * gap) / segmentCount

                for (i in 0 until segmentCount) {
                    drawArc(
                        color = trackColor,
                        startAngle = startAng + i * (chunk + gap),
                        sweepAngle = chunk,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                val activeRatio = (aqi / 300f).coerceIn(0f, 1f)
                val activeSegments = (activeRatio * segmentCount).roundToInt().coerceAtLeast(1)

                for (i in 0 until activeSegments) {
                    drawArc(
                        color = accentOrange,
                        startAngle = startAng + i * (chunk + gap),
                        sweepAngle = chunk,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun HumidityCard(
    humidity: Int,
    dewPoint: Int,
    chipBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    DatumCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Humidity >", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
            Text("$humidity%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (humidity < 40) "Crisp & Dry" else if (humidity < 70) "Comfortable" else "Humid",
                    fontSize = 13.sp,
                    color = textSecondary
                )
                Surface(shape = RoundedCornerShape(12.dp), color = chipBg) {
                    Text(
                        text = "Dew point $dewPoint°",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SunArcCard(
    sunriseIso: String,
    sunsetIso: String,
    sunriseStr: String,
    sunsetStr: String,
    daylightStr: String,
    isDay: Boolean,
    cardBorder: Color,
    accentOrange: Color,
    textSecondary: Color
) {
    DatumCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sunrise & Sunset >", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)

            Box(modifier = Modifier.fillMaxWidth().height(115.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val horizonY = h * 0.72f
                    val arcHeight = horizonY - 14.dp.toPx()
                    val nightDepth = (h - horizonY - 10.dp.toPx()).coerceAtLeast(15.dp.toPx())

                    val xRise = w * 0.16f
                    val xSet = w * 0.84f

                    fun curveY(x: Float): Float {
                        return when {
                            x in xRise..xSet -> {
                                val t = (x - xRise) / (xSet - xRise)
                                horizonY - sin(t * Math.PI.toFloat()) * arcHeight
                            }
                            x > xSet -> {
                                val t = ((x - xSet) / (w - xSet)).coerceIn(0f, 1f)
                                horizonY + sin(t * (Math.PI.toFloat() / 2f)) * nightDepth
                            }
                            else -> {
                                val t = ((xRise - x) / xRise).coerceIn(0f, 1f)
                                horizonY + sin(t * (Math.PI.toFloat() / 2f)) * nightDepth
                            }
                        }
                    }

                    drawLine(
                        color = cardBorder,
                        start = Offset(0f, horizonY),
                        end = Offset(w, horizonY),
                        strokeWidth = 1.dp.toPx()
                    )

                    val dayPath = Path()
                    val steps = 80
                    for (i in 0..steps) {
                        val x = xRise + (i.toFloat() / steps) * (xSet - xRise)
                        val y = curveY(x)
                        if (i == 0) dayPath.moveTo(x, y) else dayPath.lineTo(x, y)
                    }

                    val fillPath = Path().apply {
                        addPath(dayPath)
                        lineTo(xSet, horizonY)
                        lineTo(xRise, horizonY)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(listOf(accentOrange.copy(alpha = 0.2f), Color.Transparent))
                    )

                    drawPath(
                        path = dayPath,
                        color = accentOrange,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val nightLeft = Path().apply {
                        for (i in 0..20) {
                            val x = (i.toFloat() / 20) * xRise
                            val y = curveY(x)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(path = nightLeft, color = Color(0xFF4A6572), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    val nightRight = Path().apply {
                        for (i in 0..20) {
                            val x = xSet + (i.toFloat() / 20) * (w - xSet)
                            val y = curveY(x)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(path = nightRight, color = Color(0xFF4A6572), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    val nowCal = Calendar.getInstance()
                    val nowMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                    val riseMins = parseIsoMins(sunriseIso) ?: 373
                    val setMins = parseIsoMins(sunsetIso) ?: 1085

                    val nodeX: Float
                    if (nowMins in riseMins..setMins) {
                        val progress = (nowMins - riseMins).toFloat() / (setMins - riseMins).coerceAtLeast(1)
                        nodeX = xRise + progress * (xSet - xRise)
                    } else if (nowMins > setMins) {
                        val nightTotal = (1440 - setMins) + riseMins
                        val progress = (nowMins - setMins).toFloat() / nightTotal.coerceAtLeast(1)
                        nodeX = (xSet + progress * (w - xSet)).coerceAtMost(w - 12.dp.toPx())
                    } else {
                        val nightTotal = (1440 - setMins) + riseMins
                        val progress = (riseMins - nowMins).toFloat() / nightTotal.coerceAtLeast(1)
                        nodeX = (xRise - progress * xRise).coerceAtLeast(12.dp.toPx())
                    }

                    val nodeY = curveY(nodeX)

                    val orbColor = if (isDay) accentOrange else Color(0xFF2A394A)
                    drawCircle(color = Color.White, radius = 9.dp.toPx(), center = Offset(nodeX, nodeY))
                    drawCircle(color = orbColor, radius = 7.dp.toPx(), center = Offset(nodeX, nodeY))
                    if (!isDay) {
                        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(nodeX - 1.dp.toPx(), nodeY - 1.dp.toPx()))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(daylightStr, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sunriseStr, fontSize = 11.sp, color = textSecondary)
                    Text(sunsetStr, fontSize = 11.sp, color = textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PressureCard(
    pressureInHg: Double,
    history: List<Double>,
    graphColor: Color,
    textTertiary: Color,
    textSecondary: Color
) {
    CleanCardBase(
        title = "Pressure",
        value = String.format(Locale.US, "%.2f inHg", pressureInHg),
        subtitle = "→ Steady"
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(110.dp)) {
            val maxP = history.maxOrNull() ?: (pressureInHg + 0.1)
            val minP = history.minOrNull() ?: (pressureInHg - 0.1)
            Text(String.format(Locale.US, "%.1f", maxP), fontSize = 8.sp, color = textTertiary)

            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                val pts = history.take(24).ifEmpty { listOf(pressureInHg, pressureInHg) }
                val stepX = size.width / (pts.size - 1).coerceAtLeast(1)
                val range = (maxP - minP).coerceAtLeast(0.01).toFloat()

                val mappedPts = pts.mapIndexed { i, v ->
                    val normY = 1f - ((v.toFloat() - minP.toFloat()) / range)
                    Offset(i * stepX, normY * size.height)
                }

                val path = Path().apply { smoothCurve(mappedPts) }
                drawPath(path = path, color = graphColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(color = graphColor, radius = 2.5.dp.toPx(), center = mappedPts.first())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format(Locale.US, "%.1f", minP), fontSize = 8.sp, color = textTertiary)
                Text("Now", fontSize = 8.sp, color = textSecondary)
                Text("7 AM", fontSize = 8.sp, color = textSecondary)
            }
        }
    }
}

@Composable
private fun MoonPhaseCard(trackColor: Color) {
    CleanCardBase(
        title = "Moon",
        value = "Waxing Gibbous",
        subtitle = "Full Moon in 3 days\n87% illuminated"
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            drawCircle(color = trackColor)
            drawArc(color = Color(0xFFB0B4BA), startAngle = 90f, sweepAngle = 180f, useCenter = true)
        }
    }
}

@Composable
private fun CleanCardBase(
    title: String,
    value: String,
    subtitle: String,
    rightContent: @Composable () -> Unit
) {
    DatumCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "$title >", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, lineHeight = 16.sp)
            }
            rightContent()
        }
    }
}

fun Path.smoothCurve(points: List<Offset>) {
    if (points.isEmpty()) return
    moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val cx = (p0.x + p1.x) / 2f
        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
}

private fun parseIsoMins(isoStr: String): Int? {
    return try {
        val timePart = isoStr.substringAfter("T")
        val parts = timePart.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (_: Exception) { null }
}

private fun getUvCategory(uv: Double): String = when {
    uv < 3 -> "Low"
    uv < 6 -> "Moderate"
    uv < 8 -> "High"
    uv < 11 -> "Very High"
    else -> "Extreme"
}

private fun getWeatherDesc(code: Int): String = when (code) {
    0 -> "Clear sky"; 1 -> "Mainly clear"; 2 -> "Partly cloudy"; 3 -> "Overcast"
    45, 48 -> "Fog"; 51, 53, 55 -> "Drizzle"; 61, 63, 65 -> "Rain"; 71, 73, 75 -> "Snowfall"
    80, 81, 82 -> "Rain showers"; 95, 96, 99 -> "Thunderstorm"; else -> "Mainly clear"
}

private fun formatTime12h(isoStr: String): String {
    return try {
        val parts = isoStr.substringAfter("T").split(":")
        val h = parts[0].toInt()
        val m = parts[1]
        val dH = if (h == 0) 12 else if (h > 12) h - 12 else h
        "$dH:$m ${if (h >= 12) "pm" else "am"}"
    } catch (_: Exception) { isoStr }
}

@SuppressLint("MissingPermission")
private fun getUserLocation(context: Context, onLocationResult: (Double?, Double?) -> Unit) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) onLocationResult(loc.latitude, loc.longitude)
                else fusedClient.lastLocation.addOnSuccessListener { last -> onLocationResult(last?.latitude, last?.longitude) }
            }.addOnFailureListener { onLocationResult(null, null) }
    } catch (_: Exception) { onLocationResult(null, null) }
}

private fun getCityName(context: Context, lat: Double, lon: Double, onResult: (String?) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1) { addrs ->
                val a = addrs.firstOrNull()
                onResult(a?.locality ?: a?.subAdminArea ?: a?.adminArea)
            }
        } else {
            @Suppress("DEPRECATION")
            val a = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            onResult(a?.locality ?: a?.subAdminArea ?: a?.adminArea)
        }
    } catch (_: Exception) { onResult(null) }
}