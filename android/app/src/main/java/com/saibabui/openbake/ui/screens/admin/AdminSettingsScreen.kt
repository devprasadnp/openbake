package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) { viewModel.resetSaveState() } }

    var bakeryLat by remember { mutableStateOf("") }
    var bakeryLng by remember { mutableStateOf("") }
    var freeRadius by remember { mutableStateOf("") }
    var deliveryFee by remember { mutableStateOf("") }
    var speedPerKm by remember { mutableStateOf("") }

    LaunchedEffect(state.config) {
        state.config?.let { c ->
            bakeryLat = c.bakeryLat.toString(); bakeryLng = c.bakeryLng.toString()
            freeRadius = c.freeDeliveryRadiusKm.toString(); deliveryFee = c.deliveryFeeDefault.toString()
            speedPerKm = c.speedMinPerKm.toString()
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
                Text("Bakery Location", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = bakeryLat, onValueChange = { bakeryLat = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = bakeryLng, onValueChange = { bakeryLng = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }

                Text("Delivery Configuration", fontFamily = Nunito, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = freeRadius, onValueChange = { freeRadius = it }, label = { Text("Free Delivery Radius (km)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it }, label = { Text("Default Delivery Fee") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = speedPerKm, onValueChange = { speedPerKm = it }, label = { Text("Speed (min/km)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Nunito) }
                if (state.saveSuccess) { Text("Settings saved!", color = MaterialTheme.colorScheme.primary, fontFamily = Nunito) }

                Button(
                    onClick = {
                        viewModel.saveConfig(DeliveryConfigUpdateRequest(
                            bakeryLat = bakeryLat.toDoubleOrNull(), bakeryLng = bakeryLng.toDoubleOrNull(),
                            freeDeliveryRadiusKm = freeRadius.toDoubleOrNull(),
                            deliveryFeeDefault = deliveryFee.toDoubleOrNull(),
                            speedMinPerKm = speedPerKm.toDoubleOrNull()
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving
                ) { Text(if (state.isSaving) "Saving..." else "Save Settings") }
            }
        }
    }
}
