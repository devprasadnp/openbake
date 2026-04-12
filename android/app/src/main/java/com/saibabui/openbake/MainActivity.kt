package com.saibabui.openbake

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.saibabui.openbake.navigation.AppNavGraph
import com.saibabui.openbake.ui.theme.OpenBakeTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Payment callback from PayU browser/app deep-link return.
 */
data class PaymentResult(
    val orderId: String,
    val paymentStatus: String,
)

class MainActivity : ComponentActivity() {

    companion object {
        private val _paymentResultFlow = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
        val paymentResultFlow = _paymentResultFlow.asSharedFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePaymentDeepLink(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentDeepLink(intent)
    }

    private fun handlePaymentDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "openbake" || data.host != "payment-result") return

        val orderId = data.getQueryParameter("order_id") ?: return
        val paymentStatus = data.getQueryParameter("payment_status") ?: "pending"
        Log.d("Payment", "Deep link payment result: order=$orderId status=$paymentStatus")
        _paymentResultFlow.tryEmit(PaymentResult(orderId = orderId, paymentStatus = paymentStatus))
    }
}
