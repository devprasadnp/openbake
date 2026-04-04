package com.saibabui.openbake

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.saibabui.openbake.navigation.AppNavGraph
import com.saibabui.openbake.ui.theme.OpenBakeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed class representing Razorpay payment callback results.
 */
sealed class PaymentResult {
    data class Success(
        val razorpayPaymentId: String,
        val razorpayOrderId: String,
        val razorpaySignature: String
    ) : PaymentResult()

    data class Failure(val code: Int, val description: String) : PaymentResult()
}

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    companion object {
        private val _paymentResultFlow = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
        val paymentResultFlow = _paymentResultFlow.asSharedFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenBakeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Log.d("Payment", "Success: paymentId=$razorpayPaymentId orderId=${paymentData?.orderId}")
        val result = PaymentResult.Success(
            razorpayPaymentId = razorpayPaymentId ?: "",
            razorpayOrderId = paymentData?.orderId ?: "",
            razorpaySignature = paymentData?.signature ?: ""
        )
        _paymentResultFlow.tryEmit(result)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        Log.e("Payment", "Error: code=$code desc=$description")
        val result = PaymentResult.Failure(
            code = code,
            description = description ?: "Payment failed"
        )
        _paymentResultFlow.tryEmit(result)
    }
}
