package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Coupon
import com.saibabui.openbake.data.model.CouponCreateRequest
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminCouponsUiState(
    val isLoading: Boolean = false,
    val coupons: List<Coupon> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AdminCouponsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminCouponsUiState())
    val uiState: StateFlow<AdminCouponsUiState> = _uiState

    init { loadCoupons() }

    fun loadCoupons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getCoupons().fold(
                onSuccess = { _uiState.value = AdminCouponsUiState(coupons = it) },
                onFailure = { _uiState.value = AdminCouponsUiState(error = it.message) }
            )
        }
    }

    fun saveCoupon(couponId: String?, request: CouponCreateRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = if (couponId != null) repo.updateCoupon(couponId, request) else repo.createCoupon(request)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true); loadCoupons() },
                onFailure = { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
            )
        }
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, error = null)
    }
}
