package com.saibabui.openbake.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class OpenBakeSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val LocalOpenBakeSpacing = staticCompositionLocalOf { OpenBakeSpacing() }

val MaterialTheme.openBakeSpacing: OpenBakeSpacing
    @Composable
    get() = LocalOpenBakeSpacing.current
