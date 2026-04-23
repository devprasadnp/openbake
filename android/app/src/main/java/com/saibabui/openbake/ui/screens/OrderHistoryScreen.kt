package com.saibabui.openbake.ui.screens

import android.content.Intent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalContext

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
                actions = {
                    IconButton(
                        onClick = { orderViewModel.loadOrders() },
                        enabled = !listState.isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
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
                            top = 8.dp,
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
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    val context = LocalContext.current

    // Build items summary text with cake names (Issue #6)
    val itemsSummary = if (order.items.isNotEmpty()) {
        val firstName = order.items.first().productName ?: "Item"
        val remaining = order.items.size - 1
        if (remaining > 0) "$firstName + $remaining more" else firstName
    } else {
        "${order.items.size} items"
    }

    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
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
                Column(modifier = Modifier.weight(1f)) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp share button
                    IconButton(
                        onClick = {
                            val itemsText = order.items.joinToString("\n") { item ->
                                "  \u2022 ${item.productName ?: "Item"} \u00d7 ${item.quantity}"
                            }
                            val shareText = buildString {
                                append("\ud83c\udf70 *Sri Vinayaka Bakery*\n\n")
                                append("\ud83d\udce6 Order #${order.id.takeLast(8)}\n")
                                append("\ud83d\udccb Status: ${order.status.replaceFirstChar { it.uppercase() }}\n\n")
                                append("Items:\n$itemsText\n\n")
                                append("\ud83d\udcb0 Total: \u20b9${order.total.toInt()}")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(sendIntent)
                            } catch (_: Exception) {
                                val fallback = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(fallback, "Share order"))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("\ud83d\udce4", style = MaterialTheme.typography.bodyLarge)
                    }
                    StatusBadge(status = order.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items summary with product names
            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u20b9${order.total.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "View Details \u2192",
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
        "placed" -> Triple(Caramel.copy(alpha = 0.12f), Caramel, "Placed")
        "accepted" -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.colorScheme.primary, "Accepted")
        "preparing" -> Triple(Caramel.copy(alpha = 0.12f), Caramel, "Preparing")
        "dispatched" -> Triple(Success.copy(alpha = 0.12f), Success, "On the way")
        "delivered" -> Triple(Success.copy(alpha = 0.12f), Success, "Delivered")
        "cancelled" -> Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.colorScheme.error, "Cancelled")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
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
