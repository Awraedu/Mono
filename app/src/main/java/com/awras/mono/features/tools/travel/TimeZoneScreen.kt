package com.awras.mono.features.tools.travel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class CityTimeZone(
    val id: String,
    val cityName: String,
    val countryName: String,
    val zoneId: String
)

private val ALL_GLOBAL_CITIES = listOf(
    CityTimeZone("london", "London", "United Kingdom", "Europe/London"),
    CityTimeZone("new_york", "New York", "United States", "America/New_York"),
    CityTimeZone("tokyo", "Tokyo", "Japan", "Asia/Tokyo"),
    CityTimeZone("paris", "Paris", "France", "Europe/Paris"),
    CityTimeZone("dubai", "Dubai", "United Arab Emirates", "Asia/Dubai"),
    CityTimeZone("sydney", "Sydney", "Australia", "Australia/Sydney"),
    CityTimeZone("nairobi", "Nairobi", "Kenya", "Africa/Nairobi"),
    CityTimeZone("los_angeles", "Los Angeles", "United States", "America/Los_Angeles"),
    CityTimeZone("chicago", "Chicago", "United States", "America/Chicago"),
    CityTimeZone("toronto", "Toronto", "Canada", "America/Toronto"),
    CityTimeZone("sao_paulo", "São Paulo", "Brazil", "America/Sao_Paulo"),
    CityTimeZone("berlin", "Berlin", "Germany", "Europe/Berlin"),
    CityTimeZone("rome", "Rome", "Italy", "Europe/Rome"),
    CityTimeZone("cairo", "Cairo", "Egypt", "Africa/Cairo"),
    CityTimeZone("johannesburg", "Johannesburg", "South Africa", "Africa/Johannesburg"),
    CityTimeZone("moscow", "Moscow", "Russia", "Europe/Moscow"),
    CityTimeZone("beijing", "Beijing", "China", "Asia/Shanghai"),
    CityTimeZone("singapore", "Singapore", "Singapore", "Asia/Singapore"),
    CityTimeZone("hong_kong", "Hong Kong", "Hong Kong", "Asia/Hong_Kong"),
    CityTimeZone("bangkok", "Bangkok", "Thailand", "Asia/Bangkok"),
    CityTimeZone("seoul", "Seoul", "South Korea", "Asia/Seoul"),
    CityTimeZone("mumbai", "Mumbai", "India", "Asia/Kolkata"),
    CityTimeZone("auckland", "Auckland", "New Zealand", "Pacific/Auckland"),
    CityTimeZone("buenos_aires", "Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
    CityTimeZone("vancouver", "Vancouver", "Canada", "America/Vancouver"),
    CityTimeZone("mexico_city", "Mexico City", "Mexico", "America/Mexico_City"),
    CityTimeZone("madrid", "Madrid", "Spain", "Europe/Madrid"),
    CityTimeZone("amsterdam", "Amsterdam", "Netherlands", "Europe/Amsterdam"),
    CityTimeZone("zurich", "Zurich", "Switzerland", "Europe/Zurich"),
    CityTimeZone("istanbul", "Istanbul", "Turkey", "Europe/Istanbul"),
    CityTimeZone("riyadh", "Riyadh", "Saudi Arabia", "Asia/Riyadh"),
    CityTimeZone("jakarta", "Jakarta", "Indonesia", "Asia/Jakarta"),
    CityTimeZone("manila", "Manila", "Philippines", "Asia/Manila"),
    CityTimeZone("honolulu", "Honolulu", "United States", "Pacific/Honolulu")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZoneScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Live Instant Timer
    var currentInstant by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentInstant = Instant.now()
            delay(1000)
        }
    }

    // Interactive Time Scrub Offset (in hours)
    var scrubOffsetHours by remember { mutableFloatStateOf(0f) }

    // User's Local Zone
    val localZoneId = remember { ZoneId.systemDefault() }
    val simulatedInstant = remember(currentInstant, scrubOffsetHours) {
        currentInstant.plusSeconds((scrubOffsetHours * 3600).toLong())
    }

    val localDateTime = remember(simulatedInstant, localZoneId) {
        ZonedDateTime.ofInstant(simulatedInstant, localZoneId)
    }

    // Selected Cities
    var selectedCityIds by remember {
        mutableStateOf(listOf("london", "new_york", "tokyo", "paris", "dubai", "sydney", "nairobi"))
    }

    // Add City Sheet
    var showAddCitySheet by remember { mutableStateOf(false) }
    var searchCityQuery by remember { mutableStateOf("") }

    // 12/24 Hour Format Toggle (Default: 24-hour)
    var is24HourFormat by remember { mutableStateOf(true) }

    val timeFormat = remember(is24HourFormat) {
        DateTimeFormatter.ofPattern(
            if (is24HourFormat) "HH:mm:ss" else "hh:mm:ss a",
            Locale.ENGLISH
        )
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Time Zone",
                description = "Global Timezone Inspector & Converter",
                icon = TablerIcons.Clock,
                onBackClick = onBackClick,
                rightActionIcon = if (is24HourFormat) Icons.Default.Schedule else Icons.Default.AccessTime,
                onRightActionClick = {
                    haptics.click()
                    is24HourFormat = !is24HourFormat
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 1. LOCAL TIME HERO CARD ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "YOUR LOCAL TIME (${if (is24HourFormat) "24H" else "12H"})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = getZoneOffsetString(localZoneId, simulatedInstant),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Large Digital Clock
                    Text(
                        text = localDateTime.format(timeFormat),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localDateTime.format(dateFormat),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = localZoneId.id,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = vibrantOrange
                        )
                    }

                    // Interactive Scrub Control Bar
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

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
                                text = if (scrubOffsetHours == 0f) "TIME SCRUBBER (LIVE)" else "SCRUBBED: ${if (scrubOffsetHours > 0) "+${scrubOffsetHours.toInt()}h" else "${scrubOffsetHours.toInt()}h"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (scrubOffsetHours != 0f) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            if (scrubOffsetHours != 0f) {
                                Surface(
                                    onClick = {
                                        haptics.click()
                                        scrubOffsetHours = 0f
                                    },
                                    shape = ContinuousCapsule,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "RESET TO LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = vibrantOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Slider(
                            value = scrubOffsetHours,
                            onValueChange = {
                                scrubOffsetHours = it
                            },
                            valueRange = -12f..12f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = vibrantOrange,
                                activeTrackColor = vibrantOrange,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // ==================== 2. WORLD CLOCKS HEADER ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORLD CLOCKS (${selectedCityIds.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = vibrantOrange,
                    letterSpacing = 1.sp
                )

                Surface(
                    onClick = {
                        haptics.click()
                        searchCityQuery = ""
                        showAddCitySheet = true
                    },
                    shape = ContinuousCapsule,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add City",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ADD CITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ==================== 3. WORLD CLOCKS LIST ====================
            val selectedCities = remember(selectedCityIds) {
                ALL_GLOBAL_CITIES.filter { selectedCityIds.contains(it.id) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedCities, key = { it.id }) { city ->
                    CityClockCard(
                        city = city,
                        simulatedInstant = simulatedInstant,
                        localZoneId = localZoneId,
                        is24HourFormat = is24HourFormat,
                        onRemove = {
                            haptics.tick()
                            selectedCityIds = selectedCityIds.filter { id -> id != city.id }
                        }
                    )
                }
            }
        }
    }

    // ==================== ADD CITY BOTTOM SHEET ====================
    if (showAddCitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddCitySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Select World City",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchCityQuery,
                    onValueChange = { searchCityQuery = it },
                    placeholder = { Text("Search global cities...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = if (searchCityQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchCityQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = ContinuousCapsule
                )

                val filteredCities = remember(searchCityQuery) {
                    if (searchCityQuery.isBlank()) ALL_GLOBAL_CITIES
                    else ALL_GLOBAL_CITIES.filter {
                        it.cityName.contains(searchCityQuery, ignoreCase = true) ||
                        it.countryName.contains(searchCityQuery, ignoreCase = true) ||
                        it.zoneId.contains(searchCityQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCities, key = { it.id }) { city ->
                        val isSelected = selectedCityIds.contains(city.id)
                        Surface(
                            onClick = {
                                haptics.click()
                                selectedCityIds = if (isSelected) {
                                    selectedCityIds.filter { it != city.id }
                                } else {
                                    selectedCityIds + city.id
                                }
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
                                Column {
                                    Text(
                                        text = city.cityName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${city.countryName} • ${getZoneOffsetString(ZoneId.of(city.zoneId), currentInstant)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = vibrantOrange)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityClockCard(
    city: CityTimeZone,
    simulatedInstant: Instant,
    localZoneId: ZoneId,
    is24HourFormat: Boolean,
    onRemove: () -> Unit
) {
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    val cityZoneId = remember(city.zoneId) { ZoneId.of(city.zoneId) }
    val cityDateTime = remember(simulatedInstant, cityZoneId) {
        ZonedDateTime.ofInstant(simulatedInstant, cityZoneId)
    }

    val timeFormat = remember(is24HourFormat) {
        DateTimeFormatter.ofPattern(
            if (is24HourFormat) "HH:mm" else "hh:mm a",
            Locale.ENGLISH
        )
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH) }

    // Difference in hours relative to local zone
    val diffHours = remember(simulatedInstant, localZoneId, cityZoneId) {
        val localOffset = localZoneId.rules.getOffset(simulatedInstant).totalSeconds
        val cityOffset = cityZoneId.rules.getOffset(simulatedInstant).totalSeconds
        (cityOffset - localOffset) / 3600
    }

    val isDaytime = cityDateTime.hour in 6..18

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ContinuousRoundedRectangle(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isDaytime) TablerIcons.Sun else TablerIcons.Moon,
                        contentDescription = if (isDaytime) "Daytime" else "Nighttime",
                        tint = if (isDaytime) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = city.cityName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${city.countryName} • ${getZoneOffsetString(cityZoneId, simulatedInstant)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Difference pill
                Surface(
                    shape = ContinuousCapsule,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = when {
                            diffHours > 0 -> "+${diffHours} HRS AHEAD"
                            diffHours < 0 -> "${diffHours} HRS BEHIND"
                            else -> "SAME TIME"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diffHours != 0) vibrantOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = cityDateTime.format(timeFormat),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = GeistMono,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = cityDateTime.format(dateFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove City",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getZoneOffsetString(zoneId: ZoneId, instant: Instant): String {
    val offset = zoneId.rules.getOffset(instant)
    val totalSeconds = offset.totalSeconds
    val hours = totalSeconds / 3600
    val minutes = abs((totalSeconds % 3600) / 60)

    val sign = if (hours >= 0) "+" else ""
    return if (minutes == 0) "GMT$sign$hours" else "GMT$sign$hours:${String.format(Locale.ENGLISH, "%02d", minutes)}"
}
