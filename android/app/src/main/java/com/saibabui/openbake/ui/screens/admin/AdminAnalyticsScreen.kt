package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.saibabui.openbake.ui.viewmodel.AdminAnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AdminAnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(state.error ?: "", color = MaterialTheme.colorScheme.error) }
        } else {
            val data = state.data ?: return@Scaffold
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("7-Day Trend", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        val maxRevenue = data.dailyTrend.maxOfOrNull { it.revenue } ?: 1.0
                        data.dailyTrend.forEach { day ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(day.date, fontFamily = Nunito, modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall)
                                Box(Modifier.weight(1f).height(20.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction = (day.revenue / maxRevenue).toFloat().coerceIn(0f, 1f)).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("₹${String.format("%.0f", day.revenue)}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }

                if (data.topProducts.isNotEmpty()) {
                    Text("Top Products", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            data.topProducts.forEachIndexed { i, p ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${i + 1}. ${p.name}", fontFamily = Nunito, modifier = Modifier.weight(1f))
                                    Text("${p.units} sold", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (data.statusBreakdown.isNotEmpty()) {
                    Text("Order Status", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            data.statusBreakdown.forEach { s ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.status.replaceFirstChar { it.uppercase() }, fontFamily = Nunito)
                                    Text("${s.count}", fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
