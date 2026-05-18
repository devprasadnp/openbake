package com.saibabui.openbake.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminOrderDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: AdminOrderDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    val validTransitions = mapOf(
        "placed" to listOf("accepted", "cancelled"),
        "accepted" to listOf("preparing", "cancelled"),
        "preparing" to listOf("dispatched"),
        "dispatched" to listOf("delivered")
    )

    val statusColors = mapOf(
        "placed" to MaterialTheme.colorScheme.tertiary,
        "accepted" to MaterialTheme.colorScheme.primary,
        "preparing" to MaterialTheme.colorScheme.secondary,
        "dispatched" to MaterialTheme.colorScheme.primary,
        "delivered" to MaterialTheme.colorScheme.primary,
        "cancelled" to MaterialTheme.colorScheme.error
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Detail", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.loadOrder(orderId) }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error, fontFamily = Nunito)
            }
        } else {
            val order = state.order ?: return@Scaffold
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Order ID & Status ──
                Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Order #${order.id.takeLast(8)}", fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(order.createdAt.take(16).replace("T", " at "), fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = (statusColors[order.status] ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    order.status.replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColors[order.status] ?: MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        if (order.paymentMethod != null || order.paymentStatus.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${order.paymentMethod?.uppercase() ?: "N/A"} · ${order.paymentStatus.replaceFirstChar { it.uppercase() }}",
                                    fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (order.orderType.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Type", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.orderType.replaceFirstChar { it.uppercase() }, fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ── Customer Info with Call Button ──
                order.customer?.let { customer ->
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, "Customer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Customer", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(customer.name, fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            customer.email?.let { Text(it, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            customer.phone?.let { phone ->
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(phone, fontFamily = Nunito, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Call, "Call", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Call", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Delivery Address with Maps ──
                order.address?.let { address ->
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, "Address", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Delivery Address", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            address.recipientName?.let {
                                Text(it, fontFamily = Nunito, fontWeight = FontWeight.Bold)
                            }
                            Text(address.fullAddress, fontFamily = Nunito, style = MaterialTheme.typography.bodyMedium)
                            if (address.houseNumber != null || address.street != null) {
                                Text(
                                    listOfNotNull(address.houseNumber, address.street).joinToString(", "),
                                    fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${address.city} — ${address.pincode}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            address.landmark?.let {
                                Text("Near: $it", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Maps navigation button
                            if (address.lat != null && address.lng != null) {
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        val geoUri = Uri.parse("geo:${address.lat},${address.lng}?q=${address.lat},${address.lng}(${Uri.encode(address.fullAddress)})")
                                        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(intent)
                                        } else {
                                            // Fallback to browser
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${address.lat},${address.lng}")))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.LocationOn, "Navigate", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open in Maps", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // ── Order Items ──
                Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Items (${order.items.size})", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        order.items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName ?: "Product", fontFamily = Nunito, fontWeight = FontWeight.Medium)
                                    Text("Qty: ${item.quantity} × ₹${String.format("%.2f", item.unitPrice)}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${String.format("%.2f", item.unitPrice * item.quantity)}", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                            }
                            if (item != order.items.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        // Price breakdown
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", order.subtotal)}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall)
                        }
                        if (order.deliveryFee > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Fee", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.2f", order.deliveryFee)}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (order.discount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("-₹${String.format("%.2f", order.discount)}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("₹${String.format("%.2f", order.total)}", fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // ── Special Note ──
                order.specialNote?.let { note ->
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Special Note", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(note, fontFamily = Nunito, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Scheduled Delivery ──
                if (order.scheduledDate != null || order.timeSlot != null) {
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Scheduled Delivery", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            order.scheduledDate?.let { Text("Date: $it", fontFamily = Nunito, style = MaterialTheme.typography.bodyMedium) }
                            order.timeSlot?.let { Text("Time Slot: $it", fontFamily = Nunito, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }

                // ── Update Status Actions ──
                val actions = validTransitions[order.status] ?: emptyList()
                if (actions.isNotEmpty()) {
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Update Status", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            actions.forEach { action ->
                                if (action == "cancelled") {
                                    OutlinedButton(
                                        onClick = { viewModel.updateStatus(orderId, action) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Cancel Order", fontFamily = Nunito, fontWeight = FontWeight.Bold) }
                                } else {
                                    Button(
                                        onClick = { viewModel.updateStatus(orderId, action) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Mark as ${action.replaceFirstChar { it.uppercase() }}", fontFamily = Nunito, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }

                // ── Share to Delivery Agent (WhatsApp) ──
                Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Share with Delivery Agent", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val customerName = order.customer?.name ?: "N/A"
                                val customerPhone = order.customer?.phone ?: "N/A"
                                val address = order.address
                                
                                val mapsLink = if (address != null && address.lat != null && address.lng != null) {
                                    "https://www.google.com/maps/search/?api=1&query=${address.lat},${address.lng}"
                                } else if (address != null) {
                                    "https://www.google.com/maps/search/?api=1&query=${Uri.encode(address.fullAddress)}"
                                } else null

                                val addressText = if (address != null) {
                                    listOfNotNull(
                                        address.recipientName?.let { "Recipient: $it" },
                                        address.houseNumber?.let { "H.No: $it" },
                                        address.street,
                                        address.fullAddress,
                                        address.landmark?.let { "Landmark: $it" },
                                        "${address.city} — ${address.pincode}",
                                        mapsLink?.let { "*LOCATION:* $it" }
                                    ).joinToString("\n")
                                } else "Pickup order"

                                val itemsText = order.items.joinToString("\n") { item ->
                                    "  • ${item.productName ?: "Item"} × ${item.quantity}"
                                }

                                val scheduleText = listOfNotNull(
                                    order.scheduledDate?.let { "Date: $it" },
                                    order.timeSlot?.let { "Time: $it" }
                                ).joinToString(" | ")

                                val shareText = buildString {
                                    append("*Sri Vinayaka Bakery — NEW ORDER*\n\n")
                                    append("Order: #${order.id.takeLast(8)}\n")
                                    append("Status: ${order.status.replaceFirstChar { it.uppercase() }}\n")
                                    if (scheduleText.isNotBlank()) append("Schedule: $scheduleText\n")
                                    
                                    append("\n*--- Customer Details ---*\n")
                                    append("Name: $customerName\n")
                                    append("Phone: $customerPhone\n")
                                    
                                    append("\n*--- Delivery Address ---*\n")
                                    append("$addressText\n")
                                    
                                    append("\n*--- Items ---*\n")
                                    append("$itemsText\n")
                                    
                                    append("\n*Total Amount: ₹${order.total.toInt()}*\n")
                                    append("Payment: ${order.paymentMethod?.uppercase() ?: "N/A"} (${order.paymentStatus})\n")
                                    order.specialNote?.let { append("\n*Note:* $it\n") }
                                }

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Order Details"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Share via WhatsApp", fontFamily = Nunito, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
