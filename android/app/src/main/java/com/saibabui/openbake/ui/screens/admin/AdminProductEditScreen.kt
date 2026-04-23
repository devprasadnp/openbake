package com.saibabui.openbake.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val context = LocalContext.current
    LaunchedEffect(productId) { viewModel.load(productId) }
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) onBack() }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockCount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isAvailable by remember { mutableStateOf(true) }
    var isEggless by remember { mutableStateOf(false) }
    var customizable by remember { mutableStateOf(false) }
    var unlimitedStock by remember { mutableStateOf(false) }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageUrl = it.toString()
        }
    }

    // Permission launcher for gallery access
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            imagePickerLauncher.launch("image/*")
        } else {
            permissionDeniedMsg = "Storage permission is required to pick images. Please enable it in Settings."
        }
    }

    fun pickImage() {
        permissionDeniedMsg = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses photo picker, no permission needed
            imagePickerLauncher.launch("image/*")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12: scoped storage, no READ_EXTERNAL_STORAGE needed for picker
            imagePickerLauncher.launch("image/*")
        } else {
            // Android 9 and below: need READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                imagePickerLauncher.launch("image/*")
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    LaunchedEffect(state.product) {
        state.product?.let { p ->
            name = p.name; description = p.description ?: ""
            price = p.price.toString(); stockCount = p.stockCount.toString()
            selectedCategoryId = p.categoryId; imageUrl = p.images.firstOrNull() ?: ""
            isAvailable = p.isAvailable; isEggless = p.isEgglessAvailable; customizable = p.customizable
            unlimitedStock = p.unlimitedStock
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
                com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small)
                com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    if (!unlimitedStock) {
                        com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = stockCount, onValueChange = { stockCount = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeTextField(
                        value = state.categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategoryId = cat.id; expanded = false })
                        }
                    }
                }

                com.saibabui.openbake.ui.screens.common.OpenBakeTextField(value = imageUrl, onValueChange = { imageUrl = it; selectedImageUri = null }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth(), shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                    placeholder = { Text("Enter URL or pick from gallery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) }
                )

                // Image preview
                if (selectedImageUri != null || imageUrl.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(com.saibabui.openbake.ui.theme.OpenBakeShapes.medium)) {
                        com.saibabui.openbake.ui.screens.common.OpenBakeImage(
                            model = selectedImageUri ?: imageUrl,
                            contentDescription = "Product image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUrl = ""; selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), com.saibabui.openbake.ui.theme.OpenBakeShapes.pill)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove image", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Gallery picker button
                OutlinedButton(
                    onClick = { pickImage() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick Image from Gallery", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold))
                }

                permissionDeniedMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Unlimited Stock", fontFamily = Nunito)
                        Text("No stock tracking needed", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = unlimitedStock, onCheckedChange = { unlimitedStock = it })
                }
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
                            isAvailable, isEggless, customizable, stockCount.toIntOrNull() ?: 0, unlimitedStock, emptyList()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && name.isNotBlank() && selectedCategoryId.isNotBlank()
                ) { Text(if (state.isSaving) "Saving..." else "Save Product") }
            }
        }
    }
}
