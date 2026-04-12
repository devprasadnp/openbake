package com.saibabui.openbake.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saibabui.openbake.data.model.CategoryCreateRequest
import com.saibabui.openbake.ui.theme.Nunito
import com.saibabui.openbake.ui.theme.PlayfairDisplay
import com.saibabui.openbake.ui.viewmodel.AdminCategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoriesScreen(
    onBack: () -> Unit,
    viewModel: AdminCategoriesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editImageUrl by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) { viewModel.resetSaveState(); showDialog = false } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editId = null; editName = ""; editImageUrl = ""; showDialog = true }) {
                Icon(Icons.Filled.Add, "Add Category")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(padding)) {
                items(state.categories) { category ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(category.name, fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                if (category.isActive) Text("Active", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                else Text("Inactive", fontFamily = Nunito, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { editId = category.id; editName = category.name; editImageUrl = category.imageUrl ?: ""; showDialog = true }) {
                                Icon(Icons.Filled.Edit, "Edit")
                            }
                            IconButton(onClick = { deleteTarget = category.id }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editId != null) "Edit Category" else "New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = { Text("Image URL (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveCategory(editId, CategoryCreateRequest(editName, editImageUrl.ifBlank { null })) },
                    enabled = editName.isNotBlank() && !state.isSaving
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure? Products in this category may be affected.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(id); deleteTarget = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}
