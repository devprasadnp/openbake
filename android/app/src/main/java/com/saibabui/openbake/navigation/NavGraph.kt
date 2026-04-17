package com.saibabui.openbake.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saibabui.openbake.ui.screens.*
import com.saibabui.openbake.ui.screens.admin.*
import com.saibabui.openbake.ui.screens.common.OpenBakeBottomBar
import com.saibabui.openbake.ui.viewmodel.AuthViewModel
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.OrderViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    val authState by authViewModel.uiState.collectAsState()
    val isAdmin = authState.user?.role == "admin"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val cartItems by cartViewModel.items.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }

    val customerBottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.OrderHistory.route,
        Screen.Cart.route,
        Screen.Profile.route
    )
    val adminBottomBarRoutes = listOf(
        Screen.AdminDashboard.route,
        Screen.AdminOrders.route,
        Screen.AdminProducts.route,
        Screen.AdminMore.route
    )

    val showBottomBar = currentRoute != null && (
        (isAdmin && currentRoute in adminBottomBarRoutes) ||
        (!isAdmin && currentRoute in customerBottomBarRoutes)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OpenBakeBottomBar(
                    currentRoute = currentRoute,
                    cartItemCount = cartCount,
                    isAdmin = isAdmin,
                    onItemSelected = { route ->
                        val startRoute = if (isAdmin) Screen.AdminDashboard.route else Screen.Home.route
                        navController.navigate(route) {
                            popUpTo(startRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToAdminDashboard = {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onAdminLoginSuccess = {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    cartViewModel = cartViewModel,
                    onCategoryClick = { categoryId ->
                        navController.navigate(Screen.ProductList.createRoute(categoryId))
                    },
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onCartClick = { navController.navigate(Screen.Cart.route) },
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onMenuClick = { navController.navigate(Screen.ProductList.createRoute(null)) },
                    onOrdersClick = { navController.navigate(Screen.OrderHistory.route) },
                    onWishlistClick = { navController.navigate(Screen.Wishlist.route) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProductList.route,
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                ProductListScreen(
                    categoryId = categoryId,
                    cartViewModel = cartViewModel,
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onCartClick = { navController.navigate(Screen.Cart.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                ProductDetailScreen(
                    productId = productId,
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() },
                    onCartClick = { navController.navigate(Screen.Cart.route) }
                )
            }

            composable(Screen.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    onCheckout = { navController.navigate(Screen.Checkout.route) },
                    onContinueShopping = { navController.popBackStack() }
                )
            }

            composable(Screen.Checkout.route) {
                CheckoutScreen(
                    cartViewModel = cartViewModel,
                    orderViewModel = orderViewModel,
                    onOrderPlaced = { orderId ->
                        navController.navigate(Screen.OrderConfirmation.createRoute(orderId)) {
                            popUpTo(Screen.Cart.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.OrderHistory.route) {
                OrderHistoryScreen(
                    orderViewModel = orderViewModel,
                    onOrderClick = { orderId ->
                        navController.navigate(Screen.OrderTracking.createRoute(orderId))
                    }
                )
            }

            composable(
                route = Screen.OrderTracking.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                OrderTrackingScreen(
                    orderId = orderId,
                    orderViewModel = orderViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.OrderConfirmation.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                OrderConfirmationScreen(
                    orderId = orderId,
                    onTrackOrder = {
                        navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onContinueShopping = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToOrders = {
                        navController.navigate(Screen.OrderHistory.route)
                    },
                    onNavigateToWishlist = {
                        navController.navigate(Screen.Wishlist.route)
                    },
                    onNavigateToAddresses = {
                        navController.navigate(Screen.AddressManagement.route)
                    },
                    onNavigateToEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddressManagement.route) {
                AddressManagementScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    authViewModel = authViewModel
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Admin Routes ──

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    onNavigateToOrders = { navController.navigate(Screen.AdminOrders.route) },
                    onNavigateToProducts = { navController.navigate(Screen.AdminProducts.route) }
                )
            }

            composable(Screen.AdminOrders.route) {
                AdminOrdersScreen(
                    onOrderClick = { orderId ->
                        navController.navigate(Screen.AdminOrderDetail.createRoute(orderId))
                    }
                )
            }

            composable(
                route = Screen.AdminOrderDetail.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                AdminOrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminProducts.route) {
                AdminProductsScreen(
                    onAddProduct = { navController.navigate(Screen.AdminProductEdit.createRoute(null)) },
                    onProductClick = { productId ->
                        navController.navigate(Screen.AdminProductEdit.createRoute(productId))
                    }
                )
            }

            composable(
                route = Screen.AdminProductEdit.route,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                AdminProductEditScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminMore.route) {
                AdminMoreScreen(
                    onInventory = { navController.navigate(Screen.AdminInventory.route) },
                    onCoupons = { navController.navigate(Screen.AdminCoupons.route) },
                    onAnalytics = { navController.navigate(Screen.AdminAnalytics.route) },
                    onCategories = { navController.navigate(Screen.AdminCategories.route) },
                    onSettings = { navController.navigate(Screen.AdminSettings.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.AdminInventory.route) {
                AdminInventoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminCoupons.route) {
                AdminCouponsScreen(
                    onBack = { navController.popBackStack() },
                    onCouponClick = { couponId ->
                        navController.navigate(Screen.AdminCouponEdit.createRoute(couponId))
                    }
                )
            }

            composable(
                route = Screen.AdminCouponEdit.route,
                arguments = listOf(
                    navArgument("couponId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val couponId = backStackEntry.arguments?.getString("couponId")
                AdminCouponEditScreen(
                    couponId = couponId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminAnalytics.route) {
                AdminAnalyticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminSettings.route) {
                AdminSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AdminCategories.route) {
                AdminCategoriesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
