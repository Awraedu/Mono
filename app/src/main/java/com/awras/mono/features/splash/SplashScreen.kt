package com.awras.mono.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.awras.mono.R

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.dot_movement))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = MaterialTheme.colorScheme.onBackground.toArgb(),
            keyPath = arrayOf("**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = MaterialTheme.colorScheme.onBackground.toArgb(),
            keyPath = arrayOf("**")
        )
    )

    LaunchedEffect(progress) {
        if (progress == 1f) {
            onAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(320.dp),
                dynamicProperties = dynamicProperties
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "MONO.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "READY WHEN YOU ARE.",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
