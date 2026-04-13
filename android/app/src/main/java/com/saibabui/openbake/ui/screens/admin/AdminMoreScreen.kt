package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMoreScreen(
    onInventory: () -> Unit = {},
    onCoupons: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    onCategories: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("More", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoreMenuItem(Icons.Filled.Inventory, "Inventory", "Manage product stock", onClick = onInventory)
            MoreMenuItem(Icons.Filled.LocalOffer, "Coupons", "Create & manage coupons", onClick = onCoupons)
            MoreMenuItem(Icons.Filled.Analytics, "Analytics", "Sales & order analytics", onClick = onAnalytics)
            MoreMenuItem(Icons.Filled.Category, "Categories", "Manage product categories", onClick = onCategories)
            MoreMenuItem(Icons.Filled.Settings, "Delivery Settings", "Configure delivery options", onClick = onSettings)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            MoreMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Logout", "Sign out of admin", onClick = onLogout, isDestructive = true)
        }
    }
}

@Composable
private fun MoreMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontFamily = Nunito, fontWeight = FontWeight.SemiBold, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
