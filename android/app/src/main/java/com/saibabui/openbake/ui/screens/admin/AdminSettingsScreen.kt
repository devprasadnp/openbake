package com.saibabui.openbake.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.saibabui.openbake.data.model.DeliveryConfigUpdateRequest
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    onBack: () -> Unit,
    viewModel: AdminSettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) { viewModel.resetSaveState() } }

    var bakeryLat by remember { mutableStateOf("") }
    var bakeryLng by remember { mutableStateOf("") }
    var freeRadius by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("") }
    var speedPerKm by remember { mutableStateOf("") }
    var speedEnabled by remember { mutableStateOf(true) }
    var codEnabled by remember { mutableStateOf(true) }
    var locationLoading by remember { mutableStateOf(false) }

    LaunchedEffect(state.config) {
        state.config?.let { c ->
            bakeryLat = c.bakeryLat.toString(); bakeryLng = c.bakeryLng.toString()
            freeRadius = c.freeDeliveryRadiusKm.toString(); deliveryFee = c.deliveryFeeDefault.toString()
            speedPerKm = c.speedMinPerKm.toString()
            speedEnabled = c.speedMinPerKm > 0
            codEnabled = c.codEnabled
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationLoading = true
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        bakeryLat = String.format("%.6f", location.latitude)
                        bakeryLng = String.format("%.6f", location.longitude)
                    }
                    locationLoading = false
                }.addOnFailureListener {
                    locationLoading = false
                }
            } catch (_: SecurityException) {
                locationLoading = false
            }
        }
    }

    fun fetchCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            locationLoading = true
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        bakeryLat = String.format("%.6f", location.latitude)
                        bakeryLng = String.format("%.6f", location.longitude)
                    }
                    locationLoading = false
                }.addOnFailureListener {
                    locationLoading = false
                }
            } catch (_: SecurityException) {
                locationLoading = false
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery Settings", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Bakery Location ──
                Text("Bakery Location", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = bakeryLat, onValueChange = { bakeryLat = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = bakeryLng, onValueChange = { bakeryLng = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }

                // Issue #4: Use Current Location button
                FilledTonalButton(
                    onClick = { fetchCurrentLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !locationLoading
                ) {
                    if (locationLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Use Current Location", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Delivery Configuration ──
                Text("Delivery Configuration", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = freeRadius, onValueChange = { freeRadius = it }, label = { Text("Free Delivery Radius (km)") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Default Delivery Fee") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

                // Issue #5: Speed Enable/Disable toggle
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Speed Estimation", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                        Text("ETA based on distance", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = speedEnabled, onCheckedChange = {
                        speedEnabled = it
                        if (!it) speedPerKm = "0.0"
                    })
                }
                if (speedEnabled) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = speedPerKm, onValueChange = { speedPerKm = it }, label = { Text("Speed (min/km)") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // ── Payment Settings ──
                Text("Payment Settings", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)

                // Issue #7: COD Enable/Disable toggle
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Cash on Delivery", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
                        Text("Allow customers to pay with cash", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Nunito) }
                if (state.saveSuccess) { Text("Settings saved!", color = MaterialTheme.colorScheme.primary, fontFamily = Nunito) }

                Button(
                    onClick = {
                        viewModel.saveConfig(DeliveryConfigUpdateRequest(
                            bakeryLat = bakeryLat.toDoubleOrNull(), bakeryLng = bakeryLng.toDoubleOrNull(),
                            freeDeliveryRadiusKm = freeRadius.toDoubleOrNull(),
                            deliveryFeeDefault = deliveryFee.toDoubleOrNull(),
                            speedMinPerKm = if (speedEnabled) speedPerKm.toDoubleOrNull() else 0.0,
                            codEnabled = codEnabled
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving
                ) { Text(if (state.isSaving) "Saving..." else "Save Settings") }
            }
        }
    }
}
