package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminDashboardViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToOrders: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error, fontFamily = Nunito)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadDashboard() }) { Text("Retry") }
                }
            }
        } else {
            val stats = state.stats
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Today", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Orders", "${stats?.todayOrders ?: 0}", Modifier.weight(1f))
                    StatCard("Revenue", fmt.format(stats?.todayRevenue ?: 0.0), Modifier.weight(1f))
                }

                Text("This Week", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Orders", "${stats?.weekOrders ?: 0}", Modifier.weight(1f))
                    StatCard("Revenue", fmt.format(stats?.weekRevenue ?: 0.0), Modifier.weight(1f))
                }

                Text("This Month", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Orders", "${stats?.monthOrders ?: 0}", Modifier.weight(1f))
                    StatCard("Revenue", fmt.format(stats?.monthRevenue ?: 0.0), Modifier.weight(1f))
                }

                Card(
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pending Orders", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                        Text("${stats?.pendingOrders ?: 0}", fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onNavigateToOrders, modifier = Modifier.weight(1f)) { Text("View Orders") }
                    OutlinedButton(onClick = onNavigateToProducts, modifier = Modifier.weight(1f)) { Text("Products") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}
