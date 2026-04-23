package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.saibabui.openbake.data.model.Category
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.ui.screens.common.LoadingScreen
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cartViewModel: CartViewModel,
    onCategoryClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val cartItems by cartViewModel.items.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (homeState.isLoading) {
        LoadingScreen()
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 20.dp)
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.saibabui.openbake.R.drawable.bakery_logo),
                                contentDescription = "Sri Vinayaka Bakery",
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "Sri Vinayaka Bakery",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Freshly baked, delivered with love",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .clickable { onWishlistClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Favorite,
                                contentDescription = "Wishlist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { onSearchClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Hero banner ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(180.dp)
                    .clip(com.saibabui.openbake.ui.theme.OpenBakeShapes.xLarge)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .clickable { onMenuClick() }
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Fresh from\nthe Oven! 🔥",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        lineHeight = 34.sp
                    )
                    Surface(
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = "  Order Now →  ",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Emoji decoration
                Text(
                    text = "🎂",
                    fontSize = 64.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick actions ──────────────────────────────────────────────────
            SectionHeader(title = "Quick Actions")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    emoji = "🍞",
                    label = "Browse Menu",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onMenuClick
                )
                QuickActionCard(
                    emoji = "📦",
                    label = "My Orders",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onOrdersClick
                )
                QuickActionCard(
                    emoji = "🎁",
                    label = "Offers",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Navigate to Offers/Coupons if applicable */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Why choose us ─────────────────────────────────────────────────
            SectionHeader(title = "Why Sri Vinayaka Bakery?")
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val features = listOf(
                    Triple("🚚", "Free Delivery", "Orders above ₹500"),
                    Triple("🌟", "Fresh Daily", "Baked every morning"),
                    Triple("🎂", "Custom Cakes", "Any occasion"),
                    Triple("⚡", "Fast Dispatch", "2-hour slots"),
                    Triple("🥗", "Eggless Options", "Always available")
                )
                items(features) { (emoji, title, subtitle) ->
                    FeatureBadge(emoji = emoji, title = title, subtitle = subtitle)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Categories ────────────────────────────────────────────────────
            if (homeState.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeaderInline(title = "Categories")
                    Text(
                        text = "See all →",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onMenuClick() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(homeState.categories) { category ->
                        CategoryChip(category = category, onClick = { onCategoryClick(category.id) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Bestsellers ───────────────────────────────────────────────────
            if (homeState.bestsellers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeaderInline(title = "Bestsellers 🔥")
                    Text(
                        text = "View all →",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onMenuClick() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(homeState.bestsellers) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onAddToCart = {
                                val added = cartViewModel.addItem(product)
                                if (added) {
                                    scope.launch { snackbarHostState.showSnackbar("Added to cart! 🛒") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Could not add — check stock") }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Promo banner ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(com.saibabui.openbake.ui.theme.OpenBakeShapes.large)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .clickable { onMenuClick() }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎉 First Order Discount!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use code WELCOME10 for 10% off",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "  Shop Now →  ",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            // Error
            homeState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Floating cart FAB
        if (cartItems.isNotEmpty()) {
            FloatingActionButton(
                onClick = onCartClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text("${cartViewModel.totalItems}")
                        }
                    }
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                }
            }
        }

        // Snackbar for add-to-cart feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = PlayfairDisplay,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun SectionHeaderInline(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = PlayfairDisplay,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun QuickActionCard(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
        color = color,
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Nunito,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun FeatureBadge(emoji: String, title: String, subtitle: String) {
    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CategoryChip(category: Category, onClick: () -> Unit) {
    Surface(
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            if (!category.imageUrl.isNullOrBlank()) {
                com.saibabui.openbake.ui.screens.common.OpenBakeImage(
                    model = category.imageUrl,
                    contentDescription = category.name,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentScale = ContentScale.Crop,
                    placeholderEmoji = "🍞",
                    emojiFontSize = 24
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍞", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Nunito,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(170.dp)
            .clickable { onClick() },
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (product.images.isNotEmpty()) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeImage(
                        model = product.images.first(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.mediaTop,
                        contentScale = ContentScale.Crop,
                        placeholderEmoji = "🎂",
                        emojiFontSize = 40
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎂", fontSize = 40.sp)
                    }
                }
                // Rating badge
                if (product.rating > 0) {
                    Surface(
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⭐ ${String.format("%.1f", product.rating)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = Nunito,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Nunito,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onAddToCart() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "Add to cart",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
