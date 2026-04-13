package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    onProductClick: (String) -> Unit,
    onAddProduct: () -> Unit,
    viewModel: AdminProductsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Products", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) }) },
        floatingActionButton = { FloatingActionButton(onClick = onAddProduct) { Icon(Icons.Filled.Add, "Add Product") } }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                items(state.products) { product ->
                    Card(shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small, modifier = Modifier.fillMaxWidth().clickable { onProductClick(product.id) }) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%.2f", product.price)} · Stock: ${product.stockCount}", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = product.isAvailable, onCheckedChange = { viewModel.toggleAvailability(product) })
                            IconButton(onClick = { deleteTarget = product.id }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete this product?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProduct(id); deleteTarget = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}
