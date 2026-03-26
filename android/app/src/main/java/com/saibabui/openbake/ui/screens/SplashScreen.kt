package com.saibabui.openbake.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val fadeIn = remember { Animatable(0f) }
    val titleSlide = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        fadeIn.animateTo(1f, animationSpec = tween(800))
        titleSlide.animateTo(0f, animationSpec = tween(600, easing = EaseOutCubic))
    }

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
        delay(2000)
    }

    LaunchedEffect(authState.isLoggedIn, authState.isLoading) {
        if (!authState.isLoading) {
            delay(500)
            if (authState.isLoggedIn) onNavigateToHome() else onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(pulse)
                .alpha(0.06f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(pulse * 0.95f)
                .alpha(0.1f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(fadeIn.value)
        ) {
            // Brand circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍰",
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "OpenBake",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Artisanal bakes, delivered fresh",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Nunito
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
