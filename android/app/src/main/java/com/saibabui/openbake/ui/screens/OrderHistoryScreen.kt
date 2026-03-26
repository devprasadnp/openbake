package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saibabui.openbake.data.model.Order
import com.saibabui.openbake.ui.screens.common.EmptyState
import com.saibabui.openbake.ui.screens.common.LoadingScreen
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    orderViewModel: OrderViewModel,
    onOrderClick: (String) -> Unit
) {
    val listState by orderViewModel.listState.collectAsState()

    LaunchedEffect(Unit) { orderViewModel.loadOrders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Orders",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            listState.isLoading -> LoadingScreen()
            listState.error != null -> {
                EmptyState(
                    emoji = "😕",
                    title = "Something went wrong",
                    subtitle = listState.error ?: "Please try again",
                    actionLabel = "Retry",
                    onAction = { orderViewModel.loadOrders() }
                )
            }
            listState.orders.isEmpty() -> {
                EmptyState(
                    emoji = "📦",
                    title = "No orders yet",
                    subtitle = "Your order history will appear here"
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listState.orders) { order ->
                        OrderCard(order = order, onClick = { onOrderClick(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = order.createdAt.take(10),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items summary
            Text(
                text = "${order.items.size} item${if (order.items.size > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${order.total.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "View Details →",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(Caramel.copy(alpha = 0.12f), Caramel, "Pending")
        "confirmed" -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary, "Confirmed")
        "preparing" -> Triple(Caramel.copy(alpha = 0.12f), Caramel, "Preparing")
        "out_for_delivery" -> Triple(Success.copy(alpha = 0.12f), Success, "On the way")
        "delivered" -> Triple(Success.copy(alpha = 0.12f), Success, "Delivered")
        "cancelled" -> Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.colorScheme.error, "Cancelled")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}
