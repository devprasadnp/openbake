package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.saibabui.openbake.ui.viewmodel.AdminInventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInventoryScreen(
    onBack: () -> Unit,
    viewModel: AdminInventoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                items(state.items) { item ->
                    var stockText by remember(item.id) { mutableStateOf(item.stockCount.toString()) }
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                Text(if (item.isAvailable) "Available" else "Unavailable", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = if (item.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                            OutlinedTextField(
                                value = stockText, onValueChange = { stockText = it },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { stockText.toIntOrNull()?.let { viewModel.updateStock(item.id, it) } },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text("Update") }
                        }
                    }
                }
            }
        }
    }
}
