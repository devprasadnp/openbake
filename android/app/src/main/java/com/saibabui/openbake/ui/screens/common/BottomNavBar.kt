package com.saibabui.openbake.ui.screens.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.OpenBakeShapes
import com.saibabui.openbake.ui.theme.openBakeSpacing

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val customerNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "home"),
    BottomNavItem("Orders", Icons.Filled.Receipt, Icons.Outlined.Receipt, "order_history"),
    BottomNavItem("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "cart"),
    BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
)

val adminNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "admin_dashboard"),
    BottomNavItem("Orders", Icons.Filled.Receipt, Icons.Outlined.Receipt, "admin_orders"),
    BottomNavItem("Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "admin_products"),
    BottomNavItem("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "admin_more"),
)

@Composable
fun OpenBakeBottomBar(
    currentRoute: String?,
    cartItemCount: Int = 0,
    isAdmin: Boolean = false,
    onItemSelected: (String) -> Unit
) {
    val navItems = if (isAdmin) adminNavItems else customerNavItems
    val spacing = MaterialTheme.openBakeSpacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = OpenBakeShapes.bottomBarTop,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.xs)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = tween(200),
                    label = "scale"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    animationSpec = tween(200),
                    label = "tint"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(scale)
                        .clip(OpenBakeShapes.medium)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(item.route) }
                        .padding(horizontal = spacing.md, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (isSelected) Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        OpenBakeShapes.pill
                                    )
                                    .padding(horizontal = spacing.md, vertical = spacing.xxs)
                                else Modifier.padding(horizontal = spacing.md, vertical = spacing.xxs)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        // Cart badge (only for customer nav)
                        if (!isAdmin && item.route == "cart" && cartItemCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cartItemCount > 9) "9+" else cartItemCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Nunito,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = iconTint
                    )
                }
            }
        }
    }
}
