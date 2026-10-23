package com.awras.mono.features.tools.travel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.DatumToolTopBar
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import compose.icons.TablerIcons
import compose.icons.tablericons.Cloud
import compose.icons.tablericons.Sun
import compose.icons.tablericons.Wind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.math.*

data class CityLocation(val name: String, val lat: Double, val lon: Double)

data class HourlyItem(val time: String, val temp: Int, val isDay: Boolean)

data class DailyItem(val dayName: String, val minTemp: Int, val maxTemp: Int, val pop: Int, val condition: String)

private val CITIES = listOf(
    CityLocation("San Francisco", 37.7749, -122.4194),
    CityLocation("New York", 40.7128, -74.0060),
    CityLocation("London", 51.5074, -0.1278),
    CityLocation("Tokyo", 35.6762, 139.6503),
    CityLocation("Paris", 48.8566, 2.3522),
    CityLocation("Sydney", -33.8688, 151.2093),
    CityLocation("Cairo", 30.0444, 31.2357),
    CityLocation("Dubai", 25.2048, 55.2708)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    var selectedCity by remember { mutableStateOf(CITIES[0]) }
    var temperature by remember { mutableStateOf("--") }
    var apparentTemp by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }
    var windSpeed by remember { mutableStateOf("--") }
    var windDirection by remember { mutableStateOf("E") }
    var uvIndex by remember { mutableStateOf("0") }
    var sunriseTime by remember { mutableStateOf("--") }
    var sunsetTime by remember { mutableStateOf("--") }
    var weatherDesc by remember { mutableStateOf("Loading Forecast...") }
    var isDay by remember { mutableStateOf(true) }

    val hourlyList = remember { mutableStateListOf<HourlyItem>() }
    val dailyList = remember { mutableStateListOf<DailyItem>() }

    val scrollState = rememberScrollState()
    val hourlyScrollState = rememberScrollState()

    // Fetch Open-Meteo Weather Data
    LaunchedEffect(selectedCity) {
        withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${selectedCity.lat}&longitude=${selectedCity.lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max&timezone=auto"
                val response = URL(urlString).readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val hourly = json.getJSONObject("hourly")
                val daily = json.getJSONObject("daily")

                val temp = current.getDouble("temperature_2m")
                val appTemp = current.getDouble("apparent_temperature")
                val hum = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m")
                val windDirDeg = current.getDouble("wind_direction_10m")
                val day = current.getInt("is_day") == 1
                val code = current.getInt("weather_code")

                // Wind Direction compass
                val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                windDirection = dirs[((windDirDeg / 45.0).roundToInt() % 8)]

                temperature = String.format(Locale.US, "%.0f°", temp)
                apparentTemp = String.format(Locale.US, "%.0f°", appTemp)
                humidity = "$hum%"
                windSpeed = String.format(Locale.US, "%.0f mph", wind * 0.621371) // km/h to mph
                isDay = day

                val sunrises = daily.getJSONArray("sunrise")
                val sunsets = daily.getJSONArray("sunset")
                sunriseTime = sunrises.getString(0).substringAfter("T")
                sunsetTime = sunsets.getString(0).substringAfter("T")

                val uvs = daily.getJSONArray("uv_index_max")
                uvIndex = String.format(Locale.US, "%.1f", uvs.getDouble(0))

                weatherDesc = when (code) {
                    0 -> "Mainly clear"
                    1, 2, 3 -> "Partly cloudy"
                    45, 48 -> "Foggy"
                    51, 53, 55 -> "Drizzle"
                    61, 63, 65 -> "Rain showery"
                    71, 73, 75 -> "Snowfall"
                    95, 96, 99 -> "Thunderstorm"
                    else -> "Fair"
                }

                // Populate Hourly
                hourlyList.clear()
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val isDays = hourly.getJSONArray("is_day")
                for (i in 0 until minOf(24, times.length())) {
                    val timeStr = times.getString(i).substringAfter("T")
                    val hourNum = timeStr.substringBefore(":").toInt()
                    val ampm = if (hourNum >= 12) "PM" else "AM"
                    val displayHour = when {
                        hourNum == 0 -> "12 AM"
                        hourNum > 12 -> "${hourNum - 12} $ampm"
                        else -> "$hourNum $ampm"
                    }
                    hourlyList.add(HourlyItem(displayHour, temps.getDouble(i).roundToInt(), isDays.getInt(i) == 1))
                }

                // Populate Daily
                dailyList.clear()
                val dTimes = daily.getJSONArray("time")
                val dMax = daily.getJSONArray("temperature_2m_max")
                val dMin = daily.getJSONArray("temperature_2m_min")
                val dCodes = daily.getJSONArray("weather_code")
                for (i in 0 until minOf(7, dTimes.length())) {
                    val dName = if (i == 0) "Today" else if (i == 1) "Tomorrow" else "Day ${i + 1}"
                    val cond = when (dCodes.getInt(i)) {
                        0 -> "Sunny"
                        1, 2, 3 -> "Cloudy"
                        61, 63, 65 -> "Rain"
                        else -> "Clear"
                    }
                    dailyList.add(DailyItem(dName, dMin.getDouble(i).roundToInt(), dMax.getDouble(i).roundToInt(), 40 * i % 90, cond))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                weatherDesc = "Forecast unavailable"
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Climate Forecast",
                description = selectedCity.name,
                icon = TablerIcons.Cloud,
                onBackClick = onBackClick
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
            // 1. CITY SELECTOR CHIPS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CITIES.forEach { city ->
                    val isSelected = selectedCity == city
                    Surface(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .clickable {
                                haptics.click()
                                selectedCity = city
                            },
                        shape = ContinuousCapsule,
                        color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) vibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 2. TOP PROMINENT TEMPERATURE READOUT (REFERENCE AESTHETIC)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isDay) TablerIcons.Sun else TablerIcons.Cloud,
                    contentDescription = null,
                    tint = vibrantOrange,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = temperature,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    fontWeight = FontWeight.Black,
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    letterSpacing = (-2).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = weatherDesc,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Feels like $apparentTemp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // 3. HOURLY FORECAST HORIZONTAL SCROLL CARD
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
                    Text(
                        text = "Hourly forecast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(hourlyScrollState),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hourlyList.forEachIndexed { idx, item ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (idx == 0) "Now" else item.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Icon(
                                    imageVector = if (item.isDay) TablerIcons.Sun else TablerIcons.Cloud,
                                    contentDescription = null,
                                    tint = vibrantOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "${item.temp}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. 7-DAY FORECAST CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "7-Day Forecast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    dailyList.forEach { daily ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = daily.dayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(90.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (daily.condition.contains("Rain")) TablerIcons.Cloud else TablerIcons.Sun,
                                    contentDescription = null,
                                    tint = vibrantOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (daily.pop > 20) {
                                    Text(
                                        text = "${daily.pop}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${daily.minTemp}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                // Temp Range Bar
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(6.dp)
                                        .clip(ContinuousCapsule)
                                        .background(vibrantOrange)
                                )
                                Text(
                                    text = "${daily.maxTemp}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 5. DETAILED WEATHER METRIC CARDS GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherDetailCard(
                    title = "Wind",
                    mainValue = windSpeed,
                    subtitle = "Light breeze • From $windDirection",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(TablerIcons.Wind, contentDescription = null, tint = vibrantOrange, modifier = Modifier.size(28.dp))
                }

                WeatherDetailCard(
                    title = "UV Index",
                    mainValue = "$uvIndex • High",
                    subtitle = "Expected peak today",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(TablerIcons.Sun, contentDescription = null, tint = vibrantOrange, modifier = Modifier.size(28.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherDetailCard(
                    title = "Humidity",
                    mainValue = humidity,
                    subtitle = "Crisp & comfortable",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = vibrantOrange, modifier = Modifier.size(28.dp))
                }

                WeatherDetailCard(
                    title = "Sunrise & Sunset",
                    mainValue = sunriseTime,
                    subtitle = "Sunset at $sunsetTime",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.WbTwilight, contentDescription = null, tint = vibrantOrange, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeatherDetailCard(
    title: String,
    mainValue: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = ContinuousRoundedRectangle(20.dp),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
                content()
            }

            Text(
                text = mainValue,
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
