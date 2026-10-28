package com.awras.mono.features.tools.productivity

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

data class TallyProfile(
    val id: String,
    val name: String,
    val count: Long,
    val stepSize: Long = 1L,
    val targetGoal: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepperScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer

    // Counter Profiles List State
    var profiles by remember {
        mutableStateOf(
            listOf(
                TallyProfile("1", "Main Counter", 0L, stepSize = 1L, targetGoal = 100L),
                TallyProfile("2", "People Count", 0L, stepSize = 1L),
                TallyProfile("3", "Inventory Tally", 0L, stepSize = 5L)
            )
        )
    }

    var activeProfileId by remember { mutableStateOf("1") }
    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.first()

    // Config Dialog / Sheet State
    var showConfigSheet by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var editTargetInput by remember { mutableStateOf("") }

    // Step Size Presets
    val stepPresets = listOf(1L, 5L, 10L, 25L, 100L)

    fun updateActiveCount(delta: Long) {
        profiles = profiles.map { p ->
            if (p.id == activeProfileId) {
                val newCount = (p.count + delta).coerceAtLeast(0L)
                if (p.targetGoal != null && newCount >= p.targetGoal && p.count < p.targetGoal) {
                    haptics.success()
                    Toast.makeText(context, "Target Goal Reached! (${p.targetGoal})", Toast.LENGTH_SHORT).show()
                } else {
                    haptics.click()
                }
                p.copy(count = newCount)
            } else p
        }
    }

    fun resetActiveCount() {
        haptics.click()
        profiles = profiles.map { p ->
            if (p.id == activeProfileId) p.copy(count = 0L) else p
        }
    }

    fun setStepSize(size: Long) {
        haptics.click()
        profiles = profiles.map { p ->
            if (p.id == activeProfileId) p.copy(stepSize = size) else p
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Tally Counter",
                description = "Tactile Multi-Counter & High-Precision Stepper Engine",
                icon = TablerIcons.Plus,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.RotateClockwise,
                onRightActionClick = { resetActiveCount() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 1. TALLY COUNTER SELECTOR BAR ====================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { p ->
                    val isSelected = p.id == activeProfileId
                    Surface(
                        onClick = {
                            haptics.click()
                            activeProfileId = p.id
                        },
                        shape = ContinuousCapsule,
                        color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = p.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${p.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = GeistMono,
                                    color = if (isSelected) Color.White else vibrantOrange,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    // Add New Counter Button
                    Surface(
                        onClick = {
                            haptics.click()
                            val newId = "${System.currentTimeMillis()}"
                            val newProfile = TallyProfile(newId, "Tally ${profiles.size + 1}", 0L)
                            profiles = profiles + newProfile
                            activeProfileId = newId
                        },
                        shape = ContinuousCapsule,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Counter",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ==================== 2. PRIMARY HERO TALLY DISPLAY ====================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ContinuousRoundedRectangle(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar: Profile Name & Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = ContinuousCapsule,
                            color = vibrantOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = activeProfile.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                editNameInput = activeProfile.name
                                editTargetInput = activeProfile.targetGoal?.toString() ?: ""
                                showConfigSheet = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Settings",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Giant Monospace Digital Tally Number
                    Text(
                        text = "${activeProfile.count}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        fontFamily = GeistMono,
                        fontSize = if ("${activeProfile.count}".length > 5) 56.sp else 80.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Target Goal Gauge (if set)
                    if (activeProfile.targetGoal != null && activeProfile.targetGoal > 0) {
                        val progress = (activeProfile.count.toFloat() / activeProfile.targetGoal.toFloat()).coerceIn(0f, 1f)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET GOAL: ${activeProfile.targetGoal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = vibrantOrange
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(ContinuousCapsule),
                                color = vibrantOrange,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // Step Multiplier Selector Pills
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STEP MULTIPLIER:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stepPresets.forEach { size ->
                                val isSelected = activeProfile.stepSize == size
                                Surface(
                                    onClick = { setStepSize(size) },
                                    shape = ContinuousCapsule,
                                    color = if (isSelected) vibrantOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+$size",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 3. GIANT TACTILE STEPPER BUTTONS ====================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Giant Primary Increment (+) Tally Button
                Surface(
                    onClick = { updateActiveCount(activeProfile.stepSize) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = ContinuousRoundedRectangle(28.dp),
                    color = vibrantOrange,
                    shadowElevation = 10.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increment",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "TAP TO COUNT (+${activeProfile.stepSize})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                // Secondary Control Row: Decrement (-) & Reset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decrement (-) Button
                    Surface(
                        onClick = { updateActiveCount(-activeProfile.stepSize) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = ContinuousRoundedRectangle(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = TablerIcons.Minus,
                                contentDescription = "Decrement",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "-${activeProfile.stepSize}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Reset Button
                    Surface(
                        onClick = { resetActiveCount() },
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight(),
                        shape = ContinuousRoundedRectangle(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = TablerIcons.RotateClockwise,
                                contentDescription = "Reset Counter",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== CONFIGURATION BOTTOM SHEET ====================
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Tally Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Counter Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                OutlinedTextField(
                    value = editTargetInput,
                    onValueChange = { editTargetInput = it },
                    label = { Text("Target Goal (Optional)") },
                    placeholder = { Text("e.g. 100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousCapsule
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (profiles.size > 1) {
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                profiles = profiles.filter { it.id != activeProfileId }
                                activeProfileId = profiles.first().id
                                showConfigSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = ContinuousCapsule,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "Delete Profile",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptics.click()
                            val targetGoalParsed = editTargetInput.toLongOrNull()
                            profiles = profiles.map { p ->
                                if (p.id == activeProfileId) {
                                    p.copy(
                                        name = editNameInput.ifBlank { p.name },
                                        targetGoal = targetGoalParsed
                                    )
                                } else p
                            }
                            showConfigSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = ContinuousCapsule,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                    ) {
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
