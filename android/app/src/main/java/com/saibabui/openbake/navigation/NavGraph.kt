package com.saibabui.openbake.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.saibabui.openbake.ui.screens.common.OpenBakeBottomBar
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.OrderViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute in listOf(
        Screen.Home.route,
        Screen.OrderHistory.route,
        Screen.Wishlist.route,
        Screen.Profile.route,
        Screen.Cart.route,
        Screen.Search.route
    ) || (currentRoute?.startsWith("product_list") == true)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OpenBakeBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
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
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
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
                    onSearchClick = { navController.navigate(Screen.Search.route) }
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
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToOrders = {
                        navController.navigate(Screen.OrderHistory.route)
                    },
                    onNavigateToWishlist = {
                        navController.navigate(Screen.Wishlist.route)
                    }
                )
            }

            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }
        }
    }
}
