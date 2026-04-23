package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.saibabui.openbake.ui.viewmodel.AdminOrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onOrderClick: (String) -> Unit,
    viewModel: AdminOrdersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filters = listOf("all", "placed", "accepted", "preparing", "dispatched", "delivered", "cancelled")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Orders", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.loadOrders(filter) },
                        label = { Text(filter.replaceFirstChar { it.uppercase() }, fontFamily = Nunito) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadOrders(state.selectedFilter) },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.orders.isEmpty() && !state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No orders found", fontFamily = Nunito) }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.orders) { order ->
                            Card(
                                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                                modifier = Modifier.fillMaxWidth().clickable { onOrderClick(order.id) }
                            ) {
                                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("#${order.id.takeLast(6)}", fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                        // Show first cake name + remaining count
                                        val itemsSummary = if (order.items.isNotEmpty()) {
                                            val firstName = order.items.first().productName ?: "Item"
                                            val remaining = order.items.size - 1
                                            if (remaining > 0) "$firstName + $remaining more" else firstName
                                        } else {
                                            "${order.items.size} items"
                                        }
                                        Text(itemsSummary, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(order.status.replaceFirstChar { it.uppercase() }, fontFamily = Nunito) }
                                        )
                                        Text("₹${String.format("%.2f", order.total)}", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
