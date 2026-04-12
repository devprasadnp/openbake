package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminCouponsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCouponsScreen(
    onBack: () -> Unit,
    onCouponClick: (String?) -> Unit,
    viewModel: AdminCouponsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coupons", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { onCouponClick(null) }) { Icon(Icons.Filled.Add, "Add Coupon") } }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                items(state.coupons) { coupon ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onCouponClick(coupon.id) }) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(coupon.code, fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                Text(
                                    if (coupon.discountType == "percentage") "${coupon.discountValue.toInt()}% off" else "₹${coupon.discountValue} off",
                                    fontFamily = Nunito, style = MaterialTheme.typography.bodySmall
                                )
                                Text("Used: ${coupon.usedCount}/${coupon.maxUses ?: "∞"}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall)
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(if (coupon.isActive) "Active" else "Inactive") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (coupon.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
