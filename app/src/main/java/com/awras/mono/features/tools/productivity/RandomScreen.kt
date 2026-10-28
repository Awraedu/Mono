package com.awras.mono.features.tools.productivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
import java.security.SecureRandom

enum class RandomMode(val displayName: String) {
    COIN_FLIP("Coin Flip"),
    DICE_ROLL("Dice Roll")
}

enum class CoinSide(val label: String) {
    HEADS("HEADS"),
    TAILS("TAILS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomScreen(
    onBackClick: () -> Unit
) {
    val haptics = LocalTactileFeedback.current
    val vibrantOrange = MaterialTheme.colorScheme.primaryContainer
    val random = remember { SecureRandom() }

    var mode by remember { mutableStateOf(RandomMode.COIN_FLIP) }

    // ==================== COIN FLIP STATE ====================
    var isFlipping by remember { mutableStateOf(false) }
    var coinResult by remember { mutableStateOf(CoinSide.HEADS) }
    var coinRotationY by remember { mutableFloatStateOf(0f) }
    var headsCount by remember { mutableIntStateOf(0) }
    var tailsCount by remember { mutableIntStateOf(0) }

    // Fast 3D Coin Rotation Physics Loop
    LaunchedEffect(isFlipping) {
        if (isFlipping) {
            val targetResult = if (random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
            var currentSpeed = 35f // Fast spin 2100 deg/sec
            var currentRotation = coinRotationY

            while (isFlipping) {
                delay(16) // 60fps frame rate
                currentRotation += currentSpeed
                coinRotationY = currentRotation

                if (currentSpeed > 5f) {
                    currentSpeed *= 0.965f // friction slowdown
                    if (currentSpeed in 8f..12f) {
                        haptics.tick()
                    }
                } else {
                    // Final landing face calculation
                    coinRotationY = if (targetResult == CoinSide.HEADS) 0f else 180f
                    coinResult = targetResult
                    if (targetResult == CoinSide.HEADS) headsCount++ else tailsCount++
                    isFlipping = false
                    haptics.success()
                    break
                }
            }
        }
    }

    fun startCoinFlip() {
        if (!isFlipping) {
            haptics.click()
            isFlipping = true
        }
    }

    // Determine current facing side during 3D rotation
    val normalizedRotation = remember(coinRotationY) {
        (coinRotationY % 360f + 360f) % 360f
    }
    val isFrontFacing = remember(normalizedRotation) {
        normalizedRotation <= 90f || normalizedRotation >= 270f
    }

    val visibleCoinLabel = if (isFlipping) {
        if (isFrontFacing) "HEADS" else "TAILS"
    } else {
        coinResult.label
    }

    // ==================== MINIMAL DICE ROLL STATE ====================
    var diceCount by remember { mutableIntStateOf(2) } // 1, 2, or 3 dice
    var isRollingDice by remember { mutableStateOf(false) }
    var diceValues by remember { mutableStateOf(listOf(4, 6)) }

    // Rapidly switch random dot values during roll
    LaunchedEffect(isRollingDice) {
        if (isRollingDice) {
            val rollSteps = 10
            for (step in 1..rollSteps) {
                diceValues = List(diceCount) { random.nextInt(6) + 1 }
                haptics.tick()
                delay(50L)
            }
            isRollingDice = false
            haptics.success()
        }
    }

    fun startDiceRoll() {
        if (!isRollingDice) {
            haptics.click()
            isRollingDice = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DatumToolTopBar(
                title = "Random",
                description = "Minimalist Coin Flip & Dice Roller",
                icon = Icons.Outlined.Casino,
                onBackClick = onBackClick,
                rightActionIcon = TablerIcons.RotateClockwise,
                onRightActionClick = {
                    haptics.click()
                    if (mode == RandomMode.COIN_FLIP) {
                        headsCount = 0
                        tailsCount = 0
                        coinResult = CoinSide.HEADS
                        coinRotationY = 0f
                    } else {
                        diceValues = List(diceCount) { 1 }
                    }
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== 1. MODE SELECTOR PILL ====================
            Surface(
                shape = ContinuousCapsule,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RandomMode.entries.forEach { itemMode ->
                        val isSelected = mode == itemMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ContinuousCapsule)
                                .background(if (isSelected) vibrantOrange else Color.Transparent)
                                .clickable {
                                    if (mode != itemMode) {
                                        haptics.click()
                                        mode = itemMode
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemMode.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ==================== 2. MAIN MINIMAL VIEW ====================
            if (mode == RandomMode.COIN_FLIP) {
                // ------------ MINIMAL COIN FLIP ------------
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Rotating Minimal Circle Coin with 3D Y-Axis Flip Perspective
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .graphicsLayer {
                                    rotationY = coinRotationY
                                    cameraDistance = 12f * density
                                }
                                .clip(CircleShape)
                                .background(if (isFrontFacing) vibrantOrange else MaterialTheme.colorScheme.tertiary)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { startCoinFlip() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.graphicsLayer {
                                    // Mirror text if on back face so text isn't reversed
                                    if (!isFrontFacing) rotationY = 180f
                                }
                            ) {
                                Text(
                                    text = visibleCoinLabel,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = GeistMono,
                                    color = Color.White
                                )

                                Text(
                                    text = if (isFlipping) "SPINNING..." else "TAP TO FLIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Result Badge (Hidden during spin)
                        Surface(
                            shape = ContinuousCapsule,
                            color = vibrantOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isFlipping) "FLIPPING COIN..." else "RESULT: ${coinResult.label}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        // Primary Flip Button
                        Button(
                            onClick = { startCoinFlip() },
                            enabled = !isFlipping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = ContinuousCapsule,
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                        ) {
                            Icon(
                                imageVector = TablerIcons.RotateClockwise,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FLIP COIN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HEADS: $headsCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            val total = (headsCount + tailsCount).coerceAtLeast(1)
                            Text(
                                text = "HEADS ${(headsCount * 100 / total)}% / TAILS ${(tailsCount * 100 / total)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = GeistMono,
                                color = vibrantOrange
                            )

                            Text(
                                text = "TAILS: $tailsCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

            } else {
                // ------------ MINIMAL DICE ROLL ------------
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ContinuousRoundedRectangle(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Dice Count Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DICE COUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3).forEach { count ->
                                    val isSelected = diceCount == count
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (diceCount != count) {
                                                haptics.click()
                                                diceCount = count
                                                diceValues = List(count) { random.nextInt(6) + 1 }
                                            }
                                        },
                                        label = { Text("$count Die${if (count > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                        shape = ContinuousCapsule,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = vibrantOrange,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Minimal Dice Container Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (0 until diceCount).forEach { idx ->
                                val diceVal = diceValues.getOrElse(idx) { 1 }
                                MinimalDiceSquare(
                                    value = diceVal,
                                    vibrantOrange = vibrantOrange,
                                    modifier = Modifier
                                        .size(if (diceCount == 1) 130.dp else if (diceCount == 2) 100.dp else 80.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { startDiceRoll() }
                                )
                            }
                        }

                        // Sum & Result Readout Badge
                        val sumTotal = diceValues.take(diceCount).sum()
                        val isDouble = diceCount == 2 && diceValues[0] == diceValues[1]
                        val isTriple = diceCount == 3 && diceValues[0] == diceValues[1] && diceValues[1] == diceValues[2]

                        Surface(
                            shape = ContinuousCapsule,
                            color = vibrantOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, vibrantOrange.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isRollingDice) "ROLLING DICE..." else "TOTAL SUM: $sumTotal ${if (isDouble) "(DOUBLE!)" else if (isTriple) "(TRIPLE!)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = vibrantOrange,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        // Primary Action Button: Roll Dice
                        Button(
                            onClick = { startDiceRoll() },
                            enabled = !isRollingDice,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = ContinuousCapsule,
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Casino,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ROLL DICE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== MINIMAL DICE SQUARE COMPOSABLE ====================
@Composable
private fun MinimalDiceSquare(
    value: Int,
    vibrantOrange: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ContinuousRoundedRectangle(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, vibrantOrange)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val offset = size.minDimension * 0.28f
                val radius = size.minDimension * 0.09f
                val pipColor = vibrantOrange

                when (value) {
                    1 -> drawCircle(pipColor, radius, Offset(cx, cy))
                    2 -> {
                        drawCircle(pipColor, radius, Offset(cx - offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy + offset))
                    }
                    3 -> {
                        drawCircle(pipColor, radius, Offset(cx - offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx, cy))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy + offset))
                    }
                    4 -> {
                        drawCircle(pipColor, radius, Offset(cx - offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx - offset, cy + offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy + offset))
                    }
                    5 -> {
                        drawCircle(pipColor, radius, Offset(cx - offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx, cy))
                        drawCircle(pipColor, radius, Offset(cx - offset, cy + offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy + offset))
                    }
                    6 -> {
                        drawCircle(pipColor, radius, Offset(cx - offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy - offset))
                        drawCircle(pipColor, radius, Offset(cx - offset, cy))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy))
                        drawCircle(pipColor, radius, Offset(cx - offset, cy + offset))
                        drawCircle(pipColor, radius, Offset(cx + offset, cy + offset))
                    }
                }
            }
        }
    }
}
