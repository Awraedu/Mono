package com.awras.mono.features.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awras.mono.ui.components.AppBarButton
import com.awras.mono.ui.components.ToolCard
import com.awras.mono.ui.theme.LocalTactileFeedback
import com.kyant.capsule.ContinuousCapsule
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onExploreClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    val haptics = LocalTactileFeedback.current
    
    // Header state: 0 = Hi Dagi, 1 = Welcome, 2 = 142 Tools.
    var headerState by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            headerState = 0
            delay(4000)
            headerState = 1
            delay(4000)
            headerState = 2
            delay(4000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MONO.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    )
                },
                actions = {
                    AppBarButton(icon = TablerIcons.Search, onClick = {})
                    AppBarButton(icon = TablerIcons.Settings, onClick = {})
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            // Animated Cascading Header Section
            Box(modifier = Modifier.height(64.dp), contentAlignment = Alignment.BottomStart) {
                AnimatedContent(
                    targetState = headerState,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        ) { it } + fadeIn() togetherWith 
                        slideOutVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) { -it } + fadeOut()
                    },
                    label = "CascadingHeader"
                ) { targetState ->
                    when (targetState) {
                        0 -> CascadingLetterText(
                            prefix = "Hi ",
                            accent = "Dagi",
                            suffix = ",",
                            accentBrush = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF9900)))
                        )
                        1 -> CascadingLetterText(
                            accent = "Welcome",
                            suffix = ",",
                            accentBrush = Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
                        )
                        else -> CascadingLetterText(
                            accent = "142",
                            suffix = " Tools.",
                            accentBrush = Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF00FF66)))
                        )
                    }
                }
            }
            
            Text(
                text = "READY WHEN YOU ARE.",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp,
                letterSpacing = 0.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Priority Tools Section
            Text(
                text = "PRIORITY TOOLS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ToolCard(
                    category = "Doc Scanner",
                    title = "Digitize",
                    icon = TablerIcons.Scan,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                ToolCard(
                    category = "Spirit Level",
                    title = "Level",
                    icon = TablerIcons.ViewportWide,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ToolCard(
                    category = "Detector",
                    title = "Metal",
                    icon = TablerIcons.ToggleLeft,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                ToolCard(
                    category = "Unit Scale",
                    title = "Convert",
                    icon = TablerIcons.Exchange,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    haptics.heavySnap()
                    onExploreClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = ContinuousCapsule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.White else Color.Black,
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLORE ALL COLLECTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = TablerIcons.ArrowRight,
                        contentDescription = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun CascadingLetterText(
    prefix: String = "",
    accent: String = "",
    suffix: String = "",
    accentBrush: Brush? = null
) {
    Row {
        prefix.forEachIndexed { index, char ->
            CascadingLetter(char.toString(), index * 25)
        }
        accent.forEachIndexed { index, char ->
            CascadingLetter(char.toString(), (prefix.length + index) * 25, accentBrush)
        }
        suffix.forEachIndexed { index, char ->
            CascadingLetter(char.toString(), (prefix.length + accent.length + index) * 25)
        }
    }
}

@Composable
fun CascadingLetter(
    char: String,
    delayMs: Int,
    brush: Brush? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically { -it / 2 } + fadeOut()
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.displayMedium.copy(
                brush = brush
            ),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}
