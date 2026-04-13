package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
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
import com.google.gson.Gson
import com.saibabui.openbake.BuildConfig
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.OrderStatusEvent
import com.saibabui.openbake.ui.screens.common.GradientButton
import com.saibabui.openbake.ui.screens.common.LoadingScreen
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    orderViewModel: OrderViewModel,
    onBack: () -> Unit
) {
    val detailState by orderViewModel.detailState.collectAsState()

    LaunchedEffect(orderId) { orderViewModel.loadOrderDetail(orderId) }

    // SSE real-time status updates
    var liveStatus by remember { mutableStateOf<String?>(null) }
    var liveEta by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(orderId) {
        withContext(Dispatchers.IO) {
            try {
                val sseClient = OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS) // Keep connection open
                    .build()

                val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
                val request = Request.Builder()
                    .url("$baseUrl/orders/$orderId/stream")
                    .header("Accept", "text/event-stream")
                    .build()

                val response = sseClient.newCall(request).execute()
                val reader = response.body?.source()?.inputStream()?.bufferedReader()

                if (reader != null) {
                    var dataBuffer = ""
                    reader.forEachLine { line ->
                        if (!isActive) return@forEachLine
                        when {
                            line.startsWith("data:") -> {
                                dataBuffer = line.removePrefix("data:").trim()
                            }
                            line.isBlank() && dataBuffer.isNotEmpty() -> {
                                // Parse the JSON event
                                try {
                                    val event = Gson().fromJson(dataBuffer, OrderStatusEvent::class.java)
                                    liveStatus = event.status
                                    liveEta = event.estimatedDeliveryMinutes
                                } catch (_: Exception) {}
                                dataBuffer = ""
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // SSE connection failed — fall back to polling via loadOrderDetail
            }
        }
    }

    if (detailState.isLoading) {
        LoadingScreen()
        return
    }

    val order = detailState.order ?: return

    // Use live status from SSE if available, otherwise use the fetched status
    val currentStatus = liveStatus ?: order.status
    val estimatedEta = liveEta ?: order.estimatedDeliveryMinutes

    val steps = listOf("Placed", "Accepted", "Preparing", "Dispatched", "Delivered")
    val stepKeys = listOf("placed", "accepted", "preparing", "dispatched", "delivered")
    val currentStepIndex = when (currentStatus.lowercase()) {
        "placed" -> 0
        "accepted" -> 1
        "preparing" -> 2
        "dispatched" -> 3
        "delivered" -> 4
        else -> -1
    }
    val isCancelled = currentStatus.lowercase() == "cancelled"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Track Order",
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

            // Order info card
            Surface(
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order #${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        StatusBadge(status = currentStatus)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Placed on ${order.createdAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${order.total.toInt()} • ${order.items.size} items • ${order.paymentMethod ?: "COD"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    // ETA display
                    estimatedEta?.let { eta ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.xSmall,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                "⏱ Estimated delivery: ~$eta min",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Live indicator
                    if (liveStatus != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "● Live tracking active",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito),
                            color = Success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Vertical stepper
            if (isCancelled) {
                Surface(
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❌", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Order Cancelled",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "This order has been cancelled",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Order Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                steps.forEachIndexed { index, step ->
                    val isComplete = index <= currentStepIndex
                    val isCurrent = index == currentStepIndex
                    val timestamp = order.statusTimestamps?.get(stepKeys[index])

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Dot + line
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 32.dp else 24.dp)
                                    .background(
                                        color = when {
                                            isComplete -> Success
                                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isComplete) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (index < steps.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(40.dp)
                                        .background(
                                            if (index < currentStepIndex) Success
                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.padding(top = if (isCurrent) 4.dp else 0.dp)) {
                            Text(
                                text = step,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = Nunito,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isComplete) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            if (timestamp != null) {
                                Text(
                                    text = formatTimestamp(timestamp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Items
            Text(
                "Items",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${item.productName ?: "Item"} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "₹${(item.unitPrice * item.quantity).toInt()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Cancel button (only if placed or accepted, within cancellation window)
            if (currentStatus.lowercase() in listOf("placed", "accepted")) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { orderViewModel.cancelOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        "Cancel Order",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Format ISO timestamp like "2024-01-15T14:30:00" to "Jan 15, 2:30 PM" */
private fun formatTimestamp(iso: String): String {
    return try {
        val parts = iso.replace("T", " ").take(16).split(" ")
        if (parts.size < 2) return iso
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = months.getOrElse(dateParts[1].toInt() - 1) { "?" }
        val day = dateParts[2].toInt()
        val hour = timeParts[0].toInt()
        val minute = timeParts[1]
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        "$month $day, $hour12:$minute $amPm"
    } catch (_: Exception) { iso }
}