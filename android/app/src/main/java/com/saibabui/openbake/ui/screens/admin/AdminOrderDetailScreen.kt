package com.saibabui.openbake.ui.screens.admin

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
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(orderId) { viewModel.loadOrder(orderId) }

    val validTransitions = mapOf(
        "placed" to listOf("accepted", "cancelled"),
        "accepted" to listOf("preparing", "cancelled"),
        "preparing" to listOf("dispatched"),
        "dispatched" to listOf("delivered")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Detail", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Status", fontFamily = Nunito, style = MaterialTheme.typography.labelMedium)
                        Text(order.status.replaceFirstChar { it.uppercase() }, fontFamily = Nunito, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    }
                }

                order.customer?.let { customer ->
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Customer", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(customer.name, fontFamily = Nunito)
                            customer.email?.let { Text(it, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall) }
                            customer.phone?.let { Text(it, fontFamily = Nunito, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }

                Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Items", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        order.items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}x ${item.productName}", fontFamily = Nunito, modifier = Modifier.weight(1f))
                                Text("₹${String.format("%.2f", item.unitPrice * item.quantity)}", fontFamily = Nunito)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontFamily = Nunito, fontWeight = FontWeight.Bold)
                            Text("₹${String.format("%.2f", order.total)}", fontFamily = Nunito, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val actions = validTransitions[order.status] ?: emptyList()
                if (actions.isNotEmpty()) {
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Update Status", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                            actions.forEach { action ->
                                if (action == "cancelled") {
                                    OutlinedButton(
                                        onClick = { viewModel.updateStatus(orderId, action) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Cancel Order") }
                                } else {
                                    Button(
                                        onClick = { viewModel.updateStatus(orderId, action) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Mark as ${action.replaceFirstChar { it.uppercase() }}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
