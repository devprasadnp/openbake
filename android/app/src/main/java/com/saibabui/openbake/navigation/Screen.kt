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
    data object AddressManagement : Screen("address_management")
    data object EditProfile : Screen("edit_profile")
    data object Settings : Screen("settings")

    // ── Admin screens ──
    data object AdminDashboard : Screen("admin_dashboard")
    data object AdminOrders : Screen("admin_orders")
    data object AdminOrderDetail : Screen("admin_order_detail/{orderId}") {
        fun createRoute(orderId: String) = "admin_order_detail/$orderId"
    }
    data object AdminProducts : Screen("admin_products")
    data object AdminProductEdit : Screen("admin_product_edit?productId={productId}") {
        fun createRoute(productId: String? = null) =
            if (productId != null) "admin_product_edit?productId=$productId" else "admin_product_edit"
    }
    data object AdminMore : Screen("admin_more")
    data object AdminInventory : Screen("admin_inventory")
    data object AdminCoupons : Screen("admin_coupons")
    data object AdminCouponEdit : Screen("admin_coupon_edit?couponId={couponId}") {
        fun createRoute(couponId: String? = null) =
            if (couponId != null) "admin_coupon_edit?couponId=$couponId" else "admin_coupon_edit"
    }
    data object AdminAnalytics : Screen("admin_analytics")
    data object AdminSettings : Screen("admin_settings")
    data object AdminCategories : Screen("admin_categories")
}
