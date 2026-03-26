package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saibabui.openbake.ui.screens.common.GradientButton
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel,
    onOrderPlaced: (String) -> Unit,
    onBack: () -> Unit
) {
    val cartItems by cartViewModel.items.collectAsState()
    val placingOrder by orderViewModel.placingOrder.collectAsState()
    val placedOrder by orderViewModel.placedOrder.collectAsState()
    val orderError by orderViewModel.orderError.collectAsState()

    var paymentMethod by remember { mutableStateOf("cod") }
    var specialNote by remember { mutableStateOf("") }

    LaunchedEffect(placedOrder) {
        placedOrder?.let { order ->
            cartViewModel.clearCart()
            orderViewModel.clearPlacedOrder()
            onOrderPlaced(order.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Checkout",
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StepDot(number = 1, label = "Details", isActive = true, isComplete = true)
                StepLine(isActive = true)
                StepDot(number = 2, label = "Payment", isActive = true, isComplete = false)
                StepLine(isActive = false)
                StepDot(number = 3, label = "Confirm", isActive = false, isComplete = false)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Order Summary
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.product.name} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${item.totalPrice.toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${cartViewModel.subtotal.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${cartViewModel.deliveryFee.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold))
                        Text(
                            "₹${cartViewModel.total.toInt()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Method
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                Triple("cod", "💵", "Cash on Delivery"),
                Triple("upi", "📱", "UPI Payment"),
                Triple("card", "💳", "Card Payment")
            ).forEach { (value, emoji, label) ->
                val isSelected = paymentMethod == value
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { paymentMethod = value },
                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = Nunito,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Special Note
            Text(
                text = "Special Note",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = specialNote,
                onValueChange = { specialNote = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Any special instructions...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )

            // Error message
            orderError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            GradientButton(
                text = if (placingOrder) "Placing Order…" else "Place Order • ₹${cartViewModel.total.toInt()}",
                onClick = {
                    orderViewModel.placeOrder(
                        cartItems = cartItems,
                        paymentMethod = paymentMethod,
                        specialNote = specialNote.ifBlank { null }
                    )
                },
                enabled = !placingOrder && cartItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepDot(number: Int, label: String, isActive: Boolean, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = when {
                        isComplete -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito),
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepLine(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(2.dp)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
    )
}
