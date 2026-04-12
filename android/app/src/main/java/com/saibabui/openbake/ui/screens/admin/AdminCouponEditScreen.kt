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
import com.saibabui.openbake.data.model.CouponCreateRequest
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminCouponsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCouponEditScreen(
    couponId: String?,
    onBack: () -> Unit,
    viewModel: AdminCouponsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) { viewModel.resetSaveState(); onBack() } }

    val existingCoupon = state.coupons.find { it.id == couponId }

    var code by remember { mutableStateOf(existingCoupon?.code ?: "") }
    var discountType by remember { mutableStateOf(existingCoupon?.discountType ?: "percentage") }
    var discountValue by remember { mutableStateOf(existingCoupon?.discountValue?.toString() ?: "") }
    var minOrderValue by remember { mutableStateOf(existingCoupon?.minOrderValue?.toString() ?: "") }
    var maxUses by remember { mutableStateOf(existingCoupon?.maxUses?.toString() ?: "100") }
    var validFrom by remember { mutableStateOf(existingCoupon?.validFrom ?: LocalDate.now().toString()) }
    var validUntil by remember { mutableStateOf(existingCoupon?.validUntil ?: LocalDate.now().plusMonths(1).toString()) }
    var isActive by remember { mutableStateOf(existingCoupon?.isActive ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (couponId != null) "Edit Coupon" else "New Coupon", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Coupon Code") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Text("Discount Type", fontFamily = Nunito, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = discountType == "percentage", onClick = { discountType = "percentage" }, label = { Text("Percentage") })
                FilterChip(selected = discountType == "flat", onClick = { discountType = "flat" }, label = { Text("Flat Amount") })
            }

            OutlinedTextField(value = discountValue, onValueChange = { discountValue = it }, label = { Text(if (discountType == "percentage") "Discount %" else "Discount Amount") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = minOrderValue, onValueChange = { minOrderValue = it }, label = { Text("Min Order Value") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = maxUses, onValueChange = { maxUses = it }, label = { Text("Max Uses") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Valid From (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = validUntil, onValueChange = { validUntil = it }, label = { Text("Valid Until (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active", fontFamily = Nunito); Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Nunito) }

            Button(
                onClick = {
                    viewModel.saveCoupon(couponId, CouponCreateRequest(
                        code = code, discountType = discountType,
                        discountValue = discountValue.toDoubleOrNull() ?: 0.0,
                        minOrderValue = minOrderValue.toDoubleOrNull() ?: 0.0,
                        maxUses = maxUses.toIntOrNull() ?: 100,
                        validFrom = validFrom,
                        validUntil = validUntil,
                        isActive = isActive
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && code.isNotBlank() && discountValue.isNotBlank()
            ) { Text(if (state.isSaving) "Saving..." else "Save Coupon") }
        }
    }
}
