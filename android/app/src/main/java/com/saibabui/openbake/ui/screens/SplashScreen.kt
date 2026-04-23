package com.saibabui.openbake.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.R
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.AuthViewModel

// ─── Design tokens ────────────────────────────────────────────────────────────
private val BakeryBrown = Color(0xFF2C1A0E)
private val GoldLight   = Color(0xFFF5C97A)

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    LaunchedEffect(authState.isLoggedIn, authState.isLoading, authState.user) {
        if (!authState.isLoading) {
            if (authState.isLoggedIn) {
                if (authState.user?.role == "admin") onNavigateToAdminDashboard()
                else onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fadeIn.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BakeryBrown),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(fadeIn.value)
        ) {
            // Bakery logo
            Image(
                painter = painterResource(id = R.drawable.bakery_logo),
                contentDescription = "Sri Vinayaka Bakery",
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sri Vinayaka",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = GoldLight
            )
            Text(
                text = "B A K E R Y",
                fontFamily = Nunito,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 6.sp,
                color = GoldLight.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Show loading spinner while checking authentication status
            if (authState.isLoading) {
                CircularProgressIndicator(
                    color = GoldLight,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
