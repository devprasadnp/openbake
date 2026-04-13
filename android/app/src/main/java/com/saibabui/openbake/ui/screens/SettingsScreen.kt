package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saibabui.openbake.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {

    var notificationsEnabled by remember { mutableStateOf(true) }
    var orderUpdates by remember { mutableStateOf(true) }
    var promoNotifications by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Notifications ────────────────────────────────────────────────
            SettingsSectionTitle("Notifications")

            SettingsToggleRow(
                title = "Push Notifications",
                subtitle = "Receive notifications on your device",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )

            SettingsToggleRow(
                title = "Order Updates",
                subtitle = "Get notified when your order status changes",
                checked = orderUpdates,
                enabled = notificationsEnabled,
                onCheckedChange = { orderUpdates = it }
            )

            SettingsToggleRow(
                title = "Promotions & Offers",
                subtitle = "Deals, discounts, and seasonal specials",
                checked = promoNotifications,
                enabled = notificationsEnabled,
                onCheckedChange = { promoNotifications = it }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── About ────────────────────────────────────────────────────────
            SettingsSectionTitle("About")

            SettingsInfoRow("App Version", "1.0.0")
            SettingsInfoRow("Build", "2025.1")
            SettingsInfoRow("Contact Support", "hello@srivinayakabakery.in")

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Legal ────────────────────────────────────────────────────────
            SettingsSectionTitle("Legal")

            SettingsInfoRow("Privacy Policy", "View →")
            SettingsInfoRow("Terms of Service", "View →")
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = Nunito,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Nunito,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
