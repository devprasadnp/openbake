package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.ui.screens.common.EmptyState
import com.saibabui.openbake.ui.screens.common.LoadingScreen
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    categoryId: String?,
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onBack: () -> Unit,
    productViewModel: ProductViewModel = viewModel()
) {
    val listState by productViewModel.listState.collectAsState()
    val cartItems by cartViewModel.items.collectAsState()

    LaunchedEffect(categoryId) {
        productViewModel.loadProducts(categoryId = categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Our Menu",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = onCartClick) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${cartViewModel.totalItems}")
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            listState.isLoading -> LoadingScreen()
            listState.error != null -> {
                EmptyState(
                    emoji = "😕",
                    title = "Something went wrong",
                    subtitle = listState.error ?: "Please try again",
                    actionLabel = "Retry",
                    onAction = { productViewModel.loadProducts(categoryId = categoryId) }
                )
            }
            listState.products.isEmpty() -> {
                EmptyState(
                    emoji = "🧁",
                    title = "No products found",
                    subtitle = "Check back soon for fresh bakes!"
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listState.products) { product ->
                        GridProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onAddToCart = { cartViewModel.addItem(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
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
                        Text("🎂", fontSize = 36.sp)
                    }
                }
                if (product.rating > 0) {
                    Surface(
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.compact,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⭐ ${String.format("%.1f", product.rating)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                if (product.isEgglessAvailable) {
                    Surface(
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.xSmall,
                        color = Success.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "🌱 Eggless",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                product.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
                            .size(30.dp)
                            .clickable { onAddToCart() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
