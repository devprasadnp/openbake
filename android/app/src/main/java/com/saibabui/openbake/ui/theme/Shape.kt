package com.saibabui.openbake.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object OpenBakeShapes {
    val micro = RoundedCornerShape(4.dp)
    val xSmall = RoundedCornerShape(8.dp)
    val compact = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(12.dp)
    val input = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(20.dp)
    val xLarge = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)

    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val bottomBarTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val mediaTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

val OpenBakeMaterialShapes = Shapes(
    extraSmall = OpenBakeShapes.xSmall,
    small = OpenBakeShapes.small,
    medium = OpenBakeShapes.medium,
    large = OpenBakeShapes.large,
    extraLarge = OpenBakeShapes.xLarge,
)
