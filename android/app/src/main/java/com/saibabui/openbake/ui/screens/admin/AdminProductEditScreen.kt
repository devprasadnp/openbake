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
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminProductEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductEditScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: AdminProductEditViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(productId) { viewModel.load(productId) }
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) onBack() }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockCount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isAvailable by remember { mutableStateOf(true) }
    var isEggless by remember { mutableStateOf(false) }
    var customizable by remember { mutableStateOf(false) }

    LaunchedEffect(state.product) {
        state.product?.let { p ->
            name = p.name; description = p.description ?: ""
            price = p.price.toString(); stockCount = p.stockCount.toString()
            selectedCategoryId = p.categoryId; imageUrl = p.images.firstOrNull() ?: ""
            isAvailable = p.isAvailable; isEggless = p.isEgglessAvailable; customizable = p.customizable
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId != null) "Edit Product" else "New Product", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = stockCount, onValueChange = { stockCount = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryId = cat.id; expanded = false })
                        }
                    }
                }

                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Available", fontFamily = Nunito); Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Eggless Option", fontFamily = Nunito); Switch(checked = isEggless, onCheckedChange = { isEggless = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Customizable", fontFamily = Nunito); Switch(checked = customizable, onCheckedChange = { customizable = it })
                }

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontFamily = Nunito) }

                Button(
                    onClick = {
                        viewModel.saveProduct(
                            productId, name, description, price.toDoubleOrNull() ?: 0.0, selectedCategoryId,
                            if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList(),
                            isAvailable, isEggless, customizable, stockCount.toIntOrNull() ?: 0, emptyList()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && name.isNotBlank() && selectedCategoryId.isNotBlank()
                ) { Text(if (state.isSaving) "Saving..." else "Save Product") }
            }
        }
    }
}
