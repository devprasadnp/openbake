package com.saibabui.openbake.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// ─── Design tokens ────────────────────────────────────────────────────────────
private val BakeryBrown = Color(0xFF2C1A0E)
private val GoldLight   = Color(0xFFF5C97A)
private val GoldDark    = Color(0xFFC48A2E)
private val CreamWhite  = Color(0xFFFDF6EC)
private val CreamMuted  = Color(0xFFD4B896)

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    // Simple one-shot fade-in, no infinite animations
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(600, easing = EaseOutCubic))
    }

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
        delay(2400)
    }

    LaunchedEffect(authState.isLoggedIn, authState.isLoading, authState.user) {
        if (!authState.isLoading) {
            delay(300)
            if (authState.isLoggedIn) {
                if (authState.user?.role == "admin") onNavigateToAdminDashboard()
                else onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BakeryBrown),
        contentAlignment = Alignment.Center
    ) {
        // Subtle static ambient glow + static rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldDark.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.width * 0.52f
                ),
                radius = size.width * 0.52f,
                center = Offset(cx, cy)
            )
            // Static outer ring
            drawCircle(
                color = GoldLight.copy(alpha = 0.12f),
                radius = 200.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
            // Inner ring
            drawCircle(
                color = GoldLight.copy(alpha = 0.18f),
                radius = 145.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )
            // Corner wheat-stalk dots (decorative)
            val dotColor = GoldDark.copy(alpha = 0.16f)
            val dotR = 3.dp.toPx()
            listOf(
                Offset(48.dp.toPx(), 80.dp.toPx()),
                Offset(60.dp.toPx(), 96.dp.toPx()),
                Offset(72.dp.toPx(), 80.dp.toPx()),
                Offset(size.width - 48.dp.toPx(), 80.dp.toPx()),
                Offset(size.width - 60.dp.toPx(), 96.dp.toPx()),
                Offset(size.width - 72.dp.toPx(), 80.dp.toPx()),
            ).forEach { drawCircle(dotColor, dotR, it) }
        }

        // Brand content — fades in once
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(contentAlpha.value)
        ) {
            // Gold medallion
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(GoldLight, GoldDark),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                Canvas(modifier = Modifier.size(104.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldDark.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
                }
                Text(text = "🍰", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sri Vinayaka\nBakery",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                color = CreamWhite,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ornamental divider  ——◆——
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(140.dp)
            ) {
                Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, GoldDark)),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                Text("  ◆  ", color = GoldLight, fontSize = 8.sp)
                Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(GoldDark, Color.Transparent)),
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Artisanal bakes, delivered fresh",
                fontFamily = Nunito,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = CreamMuted,
                letterSpacing = 0.4.sp,
                textAlign = TextAlign.Center
            )
        }

        // Simple indeterminate progress bar at bottom
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .width(120.dp)
                .alpha(contentAlpha.value),
            color = GoldLight,
            trackColor = GoldDark.copy(alpha = 0.25f),
            strokeCap = StrokeCap.Round
        )
    }
}