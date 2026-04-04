package com.saibabui.openbake.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.Address
import com.saibabui.openbake.data.model.AddressRequest
import com.saibabui.openbake.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

// ── Helper data ──────────────────────────────────────────────────────────────
data class GeocodedAddress(val fullAddress: String, val city: String, val pincode: String)

@Suppress("MissingPermission")
suspend fun getDeviceLocation(context: Context): Location? = withContext(Dispatchers.IO) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // Try last known location first (fast path)
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        if (lm.isProviderEnabled(provider)) {
            lm.getLastKnownLocation(provider)?.let { return@withContext it }
        }
    }
    // Request a fresh fix, but time-out after 10 seconds
    withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine { cont ->
            var done = false
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    if (!done) { done = true; cont.resume(loc) }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) = Unit
            }
            cont.invokeOnCancellation {
                try { lm.removeUpdates(listener) } catch (_: Exception) { }
            }
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider != null) {
                try {
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (_: Exception) {
                    if (!done) { done = true; cont.resume(null) }
                }
            } else {
                if (!done) { done = true; cont.resume(null) }
            }
        }
    }
}

@Suppress("DEPRECATION")
suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): GeocodedAddress? =
    withContext(Dispatchers.IO) {
        runCatching {
            val addrs = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
            val a = addrs?.firstOrNull() ?: return@withContext null
            val line = buildString {
                a.subThoroughfare?.let { append("$it, ") }
                a.thoroughfare?.let { append(it) }
                if (isEmpty()) append(a.featureName ?: "")
            }.trim().trimEnd(',')
            GeocodedAddress(
                fullAddress = line.ifBlank { a.getAddressLine(0)?.substringBefore(",") ?: "" },
                city = a.locality ?: a.subAdminArea ?: a.adminArea ?: "",
                pincode = a.postalCode ?: ""
            )
        }.getOrNull()
    }

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddForm by remember { mutableStateOf(false) }

    // Form fields
    var label by remember { mutableStateOf("Home") }
    var fullAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            runCatching { RetrofitClient.apiService.getAddresses() }
                .onSuccess { if (it.isSuccessful) addresses = it.body() ?: emptyList() }
                .also { loading = false }
        }
    }

    LaunchedEffect(Unit) { reload() }

    fun deleteAddress(id: String) {
        scope.launch {
            runCatching { RetrofitClient.apiService.deleteAddress(id) }
                .onSuccess { addresses = addresses.filter { a -> a.id != id } }
        }
    }

    fun resetForm() {
        label = "Home"; fullAddress = ""; city = ""; pincode = ""
        isDefault = false; locationLat = null; locationLng = null; errorMsg = null
    }

    fun saveAddress() {
        if (fullAddress.isBlank() || city.isBlank() || pincode.isBlank()) return
        scope.launch {
            saving = true
            errorMsg = null
            runCatching {
                RetrofitClient.apiService.addAddress(
                    AddressRequest(
                        label = label,
                        fullAddress = fullAddress,
                        city = city,
                        pincode = pincode,
                        isDefault = isDefault,
                        lat = locationLat,
                        lng = locationLng
                    )
                )
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    resp.body()?.let { addresses = addresses + it }
                    showAddForm = false
                    resetForm()
                } else {
                    val body = resp.errorBody()?.string()
                    errorMsg = try {
                        org.json.JSONObject(body ?: "").optString("detail", "Failed to save address")
                    } catch (_: Exception) {
                        "Failed to save address (${resp.code()})"
                    }
                }
            }.onFailure { e ->
                errorMsg = e.message ?: "Network error"
            }
            saving = false
        }
    }

    fun fetchLocation() {
        locating = true
        errorMsg = null
        scope.launch {
            val loc = getDeviceLocation(context)
            if (loc != null) {
                locationLat = loc.latitude
                locationLng = loc.longitude
                val geo = reverseGeocode(context, loc.latitude, loc.longitude)
                if (geo != null) {
                    fullAddress = geo.fullAddress
                    city = geo.city
                    pincode = geo.pincode
                } else errorMsg = "Could not determine address from location"
            } else errorMsg = "Unable to get location. Ensure GPS is enabled."
            locating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation()
        else errorMsg = "Location permission denied"
    }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) fetchLocation()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Addresses",
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
                actions = {
                    IconButton(onClick = { showAddForm = !showAddForm; if (showAddForm) resetForm() }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Address",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Add Address Form ──────────────────────────────────────────────
            if (showAddForm) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "New Address",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            // Current location button
                            OutlinedButton(
                                onClick = { requestLocation() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !locating,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (locating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Detecting…",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.MyLocation,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Use Current Location",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                                    )
                                }
                            }

                            errorMsg?.let { msg ->
                                Text(
                                    msg,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                                )
                            }

                            // Label chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Home", "Work", "Other").forEach { l ->
                                    FilterChip(
                                        selected = label == l,
                                        onClick = { label = l },
                                        label = {
                                            Text(
                                                l,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                                            )
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = fullAddress,
                                onValueChange = { fullAddress = it },
                                label = {
                                    Text(
                                        "Full Address",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = {
                                        Text(
                                            "City",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { pincode = it },
                                    label = {
                                        Text(
                                            "Pincode",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                                Text(
                                    "Set as default address",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showAddForm = false; resetForm() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Cancel",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                                    )
                                }
                                Button(
                                    onClick = { saveAddress() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !saving && fullAddress.isNotBlank() && city.isNotBlank() && pincode.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (saving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(
                                            "Save",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = Nunito,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Address list ──────────────────────────────────────────────────
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (addresses.isEmpty() && !showAddForm) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No saved addresses",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add your first delivery address",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { showAddForm = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add Address",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            } else {
                items(addresses, key = { it.id }) { addr ->
                    SavedAddressCard(address = addr, onDelete = { deleteAddress(addr.id) })
                }
            }
        }
    }
}

@Composable
private fun SavedAddressCard(address: Address, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (address.isDefault)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        address.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (address.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "Default",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    address.fullAddress,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${address.city} — ${address.pincode}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = {
                Text(
                    "Delete Address?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    "This address will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito)
                )
            },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito))
                }
            }
        )
    }
}
