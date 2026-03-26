package com.saibabui.openbake.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object ProductList : Screen("product_list?categoryId={categoryId}") {
        fun createRoute(categoryId: String? = null) =
            if (categoryId != null) "product_list?categoryId=$categoryId" else "product_list"
    }
    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    data object Cart : Screen("cart")
    data object Checkout : Screen("checkout")
    data object OrderHistory : Screen("order_history")
    data object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }
    data object OrderConfirmation : Screen("order_confirmation/{orderId}") {
        fun createRoute(orderId: String) = "order_confirmation/$orderId"
    }
    data object Profile : Screen("profile")
    data object Wishlist : Screen("wishlist")
}
