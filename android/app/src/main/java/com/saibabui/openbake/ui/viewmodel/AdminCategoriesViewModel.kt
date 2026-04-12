package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Category
import com.saibabui.openbake.data.model.CategoryCreateRequest
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminCategoriesUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AdminCategoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminCategoriesUiState())
    val uiState: StateFlow<AdminCategoriesUiState> = _uiState

    init { loadCategories() }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getCategories().fold(
                onSuccess = { _uiState.value = AdminCategoriesUiState(categories = it) },
                onFailure = { _uiState.value = AdminCategoriesUiState(error = it.message) }
            )
        }
    }

    fun saveCategory(categoryId: String?, request: CategoryCreateRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = if (categoryId != null) repo.updateCategory(categoryId, request) else repo.createCategory(request)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true); loadCategories() },
                onFailure = { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repo.deleteCategory(categoryId).fold(
                onSuccess = { loadCategories() },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, error = null)
    }
}
