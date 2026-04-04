package com.saibabui.openbake.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.razorpay.Checkout
import com.saibabui.openbake.BuildConfig
import com.saibabui.openbake.MainActivity
import com.saibabui.openbake.PaymentResult
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.Address
import com.saibabui.openbake.data.model.AddressRequest
import com.saibabui.openbake.data.model.CouponApplyRequest
import com.saibabui.openbake.data.model.VerifyPaymentRequest
import com.saibabui.openbake.data.repository.OrderRepository
import com.saibabui.openbake.ui.screens.common.GradientButton
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    orderViewModel: OrderViewModel,
    onOrderPlaced: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cartItems by cartViewModel.items.collectAsState()
    val placingOrder by orderViewModel.placingOrder.collectAsState()
    val placedOrder by orderViewModel.placedOrder.collectAsState()
    val orderError by orderViewModel.orderError.collectAsState()
    val deliveryEstimate by cartViewModel.deliveryEstimate.collectAsState()
    val deliveryLoading by cartViewModel.deliveryLoading.collectAsState()

    var paymentMethod by remember { mutableStateOf("cod") }
    var specialNote by remember { mutableStateOf("") }
    var orderType by remember { mutableStateOf("delivery") }
    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var paymentInProgress by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }

    // Prevent duplicate order placement via double-tap
    var orderPlacementInFlight by remember { mutableStateOf(false) }

    // Coupon
    var couponCode by remember { mutableStateOf("") }
    var couponDiscount by remember { mutableStateOf(0.0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }
    var couponValid by remember { mutableStateOf(false) }
    var applyingCoupon by remember { mutableStateOf(false) }

    // Time slot
    val timeSlots = listOf(
        "9:00 AM – 11:00 AM",
        "11:00 AM – 1:00 PM",
        "1:00 PM – 3:00 PM",
        "3:00 PM – 5:00 PM",
        "5:00 PM – 7:00 PM"
    )
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }

    // Inline address form state
    var showAddressForm by remember { mutableStateOf(false) }
    var newAddrLabel by remember { mutableStateOf("Home") }
    var newAddrFull by remember { mutableStateOf("") }
    var newAddrCity by remember { mutableStateOf("") }
    var newAddrPincode by remember { mutableStateOf("") }
    var savingAddr by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var newAddrLat by remember { mutableStateOf<Double?>(null) }
    var newAddrLng by remember { mutableStateOf<Double?>(null) }

    // Reset stale order state on entering checkout (fixes back-navigation bug)
    LaunchedEffect(Unit) {
        orderViewModel.clearPlacedOrder()
        orderViewModel.clearOrderError()
        paymentError = null
        orderPlacementInFlight = false
    }

    // Auto-fetch delivery estimate when selected address changes
    LaunchedEffect(selectedAddressId) {
        val addr = addresses.find { it.id == selectedAddressId }
        if (addr?.lat != null && addr.lng != null) {
            cartViewModel.fetchDeliveryEstimate(addr.lat, addr.lng)
        }
    }

    fun saveNewAddress() {
        if (newAddrFull.isBlank() || newAddrCity.isBlank() || newAddrPincode.isBlank()) return
        scope.launch {
            savingAddr = true
            runCatching {
                RetrofitClient.apiService.addAddress(
                    AddressRequest(
                        label = newAddrLabel,
                        fullAddress = newAddrFull,
                        city = newAddrCity,
                        pincode = newAddrPincode,
                        lat = newAddrLat,
                        lng = newAddrLng
                    )
                )
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val addr = resp.body()
                    if (addr != null) {
                        addresses = addresses + addr
                        selectedAddressId = addr.id
                    }
                    showAddressForm = false
                    newAddrFull = ""; newAddrCity = ""; newAddrPincode = ""
                    newAddrLat = null; newAddrLng = null
                }
            }
            savingAddr = false
        }
    }

    fun fetchLocation() {
        locating = true
        scope.launch {
            val loc = getDeviceLocation(context)
            if (loc != null) {
                newAddrLat = loc.latitude
                newAddrLng = loc.longitude
                val geo = reverseGeocode(context, loc.latitude, loc.longitude)
                if (geo != null) {
                    newAddrFull = geo.fullAddress
                    newAddrCity = geo.city
                    newAddrPincode = geo.pincode
                }
            }
            locating = false
        }
    }

    fun applyCoupon() {
        if (couponCode.isBlank()) return
        scope.launch {
            applyingCoupon = true
            couponMessage = null
            try {
                val resp = RetrofitClient.apiService.applyCoupon(
                    CouponApplyRequest(code = couponCode.trim(), subtotal = cartViewModel.subtotal)
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        couponValid = body.valid
                        couponDiscount = if (body.valid) body.discount else 0.0
                        couponMessage = body.message.ifBlank { if (body.valid) "Coupon applied! ₹${body.discount.toInt()} off" else "Invalid coupon" }
                    }
                } else {
                    couponValid = false
                    couponDiscount = 0.0
                    couponMessage = "Failed to validate coupon"
                }
            } catch (_: Exception) {
                couponValid = false
                couponDiscount = 0.0
                couponMessage = "Network error. Try again."
            }
            applyingCoupon = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchLocation() }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) fetchLocation()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Launch Razorpay Checkout for UPI/card payments.
     * Called AFTER order is created on backend with payment_status=pending.
     */
    fun launchRazorpay(orderId: String) {
        paymentInProgress = true
        paymentError = null
        scope.launch {
            val orderRepo = OrderRepository()
            val result = orderRepo.createPaymentOrder(orderId)
            result.fold(
                onSuccess = { rpOrder ->
                    try {
                        // Dev mock mode — skip Razorpay SDK entirely
                        if (rpOrder.razorpayOrderId.startsWith("order_dev_")) {
                            Log.d("Checkout", "Dev mock order — auto-verifying payment")
                            val verifyResult = orderRepo.verifyPayment(
                                VerifyPaymentRequest(
                                    orderId = orderId,
                                    razorpayOrderId = rpOrder.razorpayOrderId,
                                    razorpayPaymentId = "pay_dev_${System.currentTimeMillis()}",
                                    razorpaySignature = "dev_mock_signature"
                                )
                            )
                            verifyResult.fold(
                                onSuccess = {
                                    cartViewModel.clearCart()
                                    orderViewModel.clearPlacedOrder()
                                    paymentInProgress = false
                                    onOrderPlaced(orderId)
                                },
                                onFailure = { err ->
                                    Log.e("Checkout", "Dev mock verify failed", err)
                                    paymentError = "Payment verification failed: ${err.message}"
                                    paymentInProgress = false
                                    orderPlacementInFlight = false
                                }
                            )
                            return@launch
                        }

                        val activity = context as Activity
                        val checkout = Checkout()
                        checkout.setKeyID(rpOrder.razorpayKeyId)
                        val options = JSONObject().apply {
                            put("name", "Sri Vinayaka Bakery")
                            put("description", "Order #${orderId.takeLast(6).uppercase()}")
                            put("image", "https://ui-avatars.com/api/?name=SVB&background=D4A574&color=fff&size=128")
                            put("order_id", rpOrder.razorpayOrderId)
                            put("amount", rpOrder.amount)
                            put("currency", rpOrder.currency)
                            put("prefill", JSONObject())
                            put("theme", JSONObject().apply {
                                put("color", "#D4A574")
                            })
                        }
                        checkout.open(activity, options)
                        // Result comes via MainActivity.paymentResultFlow — do NOT reset paymentInProgress here
                    } catch (e: Exception) {
                        Log.e("Checkout", "Razorpay launch failed", e)
                        paymentError = "Failed to launch payment: ${e.message}"
                        paymentInProgress = false
                        orderPlacementInFlight = false
                    }
                },
                onFailure = { e ->
                    Log.e("Checkout", "Payment order creation failed", e)
                    paymentError = "Failed to create payment: ${e.message}"
                    paymentInProgress = false
                    orderPlacementInFlight = false
                }
            )
        }
    }

    // Listen to Razorpay payment callbacks from MainActivity
    LaunchedEffect(Unit) {
        MainActivity.paymentResultFlow.collect { result ->
            val currentOrder = placedOrder ?: return@collect
            when (result) {
                is PaymentResult.Success -> {
                    Log.d("Checkout", "Payment success — verifying with backend")
                    val orderRepo = OrderRepository()
                    val verifyResult = orderRepo.verifyPayment(
                        VerifyPaymentRequest(
                            orderId = currentOrder.id,
                            razorpayOrderId = result.razorpayOrderId,
                            razorpayPaymentId = result.razorpayPaymentId,
                            razorpaySignature = result.razorpaySignature
                        )
                    )
                    verifyResult.fold(
                        onSuccess = {
                            cartViewModel.clearCart()
                            orderViewModel.clearPlacedOrder()
                            paymentInProgress = false
                            orderPlacementInFlight = false
                            onOrderPlaced(currentOrder.id)
                        },
                        onFailure = { err ->
                            paymentError = "Payment verification failed: ${err.message}"
                            paymentInProgress = false
                            orderPlacementInFlight = false
                        }
                    )
                }
                is PaymentResult.Failure -> {
                    paymentError = "Payment failed: ${result.description}. You can retry."
                    paymentInProgress = false
                    orderPlacementInFlight = false
                }
            }
        }
    }

    // Fetch saved addresses
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getAddresses()
            if (response.isSuccessful) {
                val addrList = response.body() ?: emptyList()
                addresses = addrList
                selectedAddressId = addrList.find { it.isDefault }?.id ?: addrList.firstOrNull()?.id
            }
        } catch (_: Exception) {}
    }

    // Handle placed order: COD → confirmation, online → Razorpay
    LaunchedEffect(placedOrder) {
        placedOrder?.let { order ->
            if (paymentMethod == "cod") {
                cartViewModel.clearCart()
                orderViewModel.clearPlacedOrder()
                onOrderPlaced(order.id)
            } else {
                launchRazorpay(order.id)
            }
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

            // ── Delivery / Pickup Toggle ──
            Text(
                text = "Order Type",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("delivery" to "🚚 Delivery", "pickup" to "🏪 Pickup").forEach { (value, label) ->
                    val sel = orderType == value
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.weight(1f).clickable { orderType = value },
                        border = if (sel) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RadioButton(selected = sel, onClick = { orderType = value }, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Nunito,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Delivery Address (only for delivery) ──
            if (orderType == "delivery" && addresses.isNotEmpty()) {
                Text(
                    text = "Delivery Address",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                addresses.forEach { addr ->
                    val isSelected = selectedAddressId == addr.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { selectedAddressId = addr.id },
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedAddressId = addr.id })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    addr.label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "${addr.fullAddress}, ${addr.city} - ${addr.pincode}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!showAddressForm) {
                    TextButton(onClick = { showAddressForm = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            "+ Add New Address",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Delivery estimate
                deliveryEstimate?.let { est ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (est.isDeliverable) Success.copy(alpha = 0.08f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (est.isDeliverable) {
                                Text(
                                    "📍 ${String.format("%.1f", est.distanceKm)} km away • ETA ~${est.estimatedTimeMinutes} min",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
                                    color = Success
                                )
                                if (est.deliveryFee == 0.0) {
                                    Text("🎉 Free delivery!", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = Success)
                                }
                            } else {
                                Text(
                                    "⚠️ Outside delivery area (${String.format("%.1f", est.distanceKm)} km)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                if (deliveryLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculating delivery…", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (orderType == "delivery" && !showAddressForm) {
                // No addresses
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No delivery address saved", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add an address to proceed with delivery", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showAddressForm = true }, shape = RoundedCornerShape(12.dp)) {
                            Text("Add Address", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Inline Add Address Form ──
            if (orderType == "delivery" && showAddressForm) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add Delivery Address", style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold))
                        OutlinedButton(onClick = { requestLocation() }, modifier = Modifier.fillMaxWidth(), enabled = !locating, shape = RoundedCornerShape(12.dp)) {
                            if (locating) {
                                CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Detecting…", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito))
                            } else {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Use Current Location", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Home", "Work", "Other").forEach { l ->
                                FilterChip(selected = newAddrLabel == l, onClick = { newAddrLabel = l }, label = { Text(l, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) })
                            }
                        }
                        OutlinedTextField(value = newAddrFull, onValueChange = { newAddrFull = it }, label = { Text("Full Address", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = newAddrCity, onValueChange = { newAddrCity = it }, label = { Text("City", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = newAddrPincode, onValueChange = { newAddrPincode = it }, label = { Text("Pincode", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { showAddressForm = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Text("Cancel", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito))
                            }
                            Button(onClick = { saveNewAddress() }, modifier = Modifier.weight(1f), enabled = !savingAddr && newAddrFull.isNotBlank() && newAddrCity.isNotBlank() && newAddrPincode.isNotBlank(), shape = RoundedCornerShape(12.dp)) {
                                if (savingAddr) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text("Save", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Pickup Info ──
            if (orderType == "pickup") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🏪 Store Pickup",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pick up your order from our bakery.\nYou'll receive a notification when it's ready!",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Time Slot ──
            Text(
                text = "Preferred Time Slot",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Optional — leave empty for earliest available",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                timeSlots.forEach { slot ->
                    val sel = selectedTimeSlot == slot
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTimeSlot = if (sel) null else slot
                            },
                        border = if (sel) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel, onClick = { selectedTimeSlot = if (sel) null else slot }, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                slot,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Nunito,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Coupon Code ──
            Text(
                text = "Coupon Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = couponCode,
                    onValueChange = {
                        couponCode = it.uppercase()
                        if (couponValid) {
                            couponValid = false
                            couponDiscount = 0.0
                            couponMessage = null
                        }
                    },
                    placeholder = { Text("Enter code", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = { applyCoupon() },
                    enabled = !applyingCoupon && couponCode.isNotBlank() && !couponValid,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (applyingCoupon) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Apply", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold))
                    }
                }
            }
            couponMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                    color = if (couponValid) Success else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Order Summary ──
            Text(text = "Order Summary", style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.product.name} × ${item.quantity}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("₹${item.totalPrice.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${cartViewModel.subtotal.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (orderType == "delivery") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (cartViewModel.deliveryFee == 0.0) "Delivery (Free!)" else "Delivery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = if (cartViewModel.deliveryFee == 0.0) Success else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${cartViewModel.deliveryFee.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("N/A (Pickup)", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    cartViewModel.estimatedMinutes?.let { eta ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Delivery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~$eta min", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (couponValid && couponDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coupon Discount", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito), color = Success)
                            Text("-₹${couponDiscount.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold), color = Success)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    val finalTotal = (cartViewModel.total - couponDiscount).coerceAtLeast(0.0)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold))
                        Text("₹${finalTotal.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Payment Method ──
            Text(text = "Payment Method", style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                Triple("cod", "💵", "Cash on Delivery"),
                Triple("upi", "📱", "UPI Payment"),
                Triple("card", "💳", "Card Payment")
            ).forEach { (value, emoji, label) ->
                val isSelected = paymentMethod == value
                val subtitle = when (value) {
                    "cod" -> "Pay when your order is delivered"
                    "upi" -> "Google Pay, PhonePe, Paytm & more"
                    "card" -> "Visa, Mastercard, RuPay — secured by Razorpay"
                    else -> ""
                }
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth().clickable { paymentMethod = value },
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Nunito, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium))
                                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (value) { "upi" -> Color(0xFFF0F7FF); "card" -> Color(0xFFF5F0FF); else -> Color(0xFFF0FFF4) },
                            modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                when (value) {
                                    "cod" -> Text("Pay with cash or UPI QR when your order arrives.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    "upi" -> {
                                        Text("How UPI payment works:", style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        PaymentStep("1", "Click \"Place Order\" below")
                                        PaymentStep("2", "Razorpay opens — choose your UPI app or enter UPI ID")
                                        PaymentStep("3", "Approve in your UPI app — order confirmed!")
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🔒 Secure UPI payment via Razorpay", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    "card" -> {
                                        Text("How card payment works:", style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        PaymentStep("1", "Click \"Place Order\" below")
                                        PaymentStep("2", "Enter card details securely in Razorpay")
                                        PaymentStep("3", "Complete 3D Secure / OTP — order confirmed!")
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🔒 PCI-DSS compliant • Visa / MC / RuPay", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Special Note ──
            Text(text = "Special Note", style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = specialNote, onValueChange = { specialNote = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Any special instructions...", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)) },
                shape = RoundedCornerShape(14.dp), minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )

            // ── Error Display ──
            val displayError = paymentError ?: orderError
            displayError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Place Order Button ──
            val addressSelected = selectedAddressId != null
            val isDelivery = orderType == "delivery"
            val canPlaceOrder = !placingOrder && !paymentInProgress && !orderPlacementInFlight && cartItems.isNotEmpty()
                    && (!isDelivery || (cartViewModel.isDeliverable && addressSelected))
            val finalTotalBtn = (cartViewModel.total - couponDiscount).coerceAtLeast(0.0)

            GradientButton(
                text = when {
                    paymentInProgress -> "Processing Payment…"
                    placingOrder || orderPlacementInFlight -> "Placing Order…"
                    isDelivery && !addressSelected -> "Select Delivery Address"
                    isDelivery && !cartViewModel.isDeliverable -> "Outside Delivery Area"
                    else -> "Place Order • ₹${finalTotalBtn.toInt()}"
                },
                onClick = {
                    if (!orderPlacementInFlight) {
                        orderPlacementInFlight = true
                        paymentError = null
                        orderViewModel.placeOrder(
                            cartItems = cartItems,
                            paymentMethod = paymentMethod,
                            orderType = orderType,
                            addressId = if (isDelivery) selectedAddressId else null,
                            couponCode = if (couponValid) couponCode.trim() else null,
                            timeSlot = selectedTimeSlot,
                            specialNote = specialNote.ifBlank { null }
                        )
                    }
                },
                enabled = canPlaceOrder,
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
            modifier = Modifier.size(32.dp).background(
                color = when {
                    isComplete -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }, shape = CircleShape
            ), contentAlignment = Alignment.Center
        ) {
            if (isComplete) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            else Text("$number", style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold), color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito), color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepLine(isActive: Boolean) {
    Box(modifier = Modifier.width(48.dp).height(2.dp).background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow))
}

@Composable
private fun PaymentStep(number: String, description: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text(number, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(description, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
