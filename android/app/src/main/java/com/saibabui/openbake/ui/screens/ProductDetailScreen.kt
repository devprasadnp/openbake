package com.saibabui.openbake.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.ProductVariant
import com.saibabui.openbake.ui.screens.common.GradientButton
import com.saibabui.openbake.ui.screens.common.LoadingScreen
import com.saibabui.openbake.ui.theme.*
import com.saibabui.openbake.ui.viewmodel.CartViewModel
import com.saibabui.openbake.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    productViewModel: ProductViewModel = viewModel()
) {
    val detailState by productViewModel.detailState.collectAsState()
    var selectedVariant by remember { mutableStateOf<ProductVariant?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var isEggless by remember { mutableStateOf(false) }
    val cartItems by cartViewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    // Waitlist state
    var onWaitlist by remember { mutableStateOf(false) }
    var waitlistLoading by remember { mutableStateOf(false) }

    // Wishlist state
    var isInWishlist by remember { mutableStateOf(false) }
    var wishlistLoading by remember { mutableStateOf(false) }

    // Stock error snackbar
    var stockError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(productId) {
        productViewModel.loadProductDetail(productId)
        // Check if product is in wishlist
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.getWishlist()
                if (resp.isSuccessful) {
                    isInWishlist = resp.body()?.any { it.product.id == productId } == true
                }
            } catch (_: Exception) {}
        }
    }

    if (detailState.isLoading) {
        LoadingScreen()
        return
    }

    val product = detailState.product ?: return

    val isOutOfStock = product.stockCount <= 0 || !product.isAvailable

    // Show stock error in snackbar
    LaunchedEffect(stockError) {
        stockError?.let {
            snackbarHostState.showSnackbar(it)
            stockError = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                if (product.images.isNotEmpty()) {
                    com.saibabui.openbake.ui.screens.common.OpenBakeImage(
                        model = product.images.first(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
                        contentScale = ContentScale.Crop,
                        placeholderEmoji = "🎂",
                        emojiFontSize = 72
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎂", fontSize = 72.sp)
                    }
                }

                // Out of stock overlay
                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "OUT OF STOCK",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }

                // Back button overlay
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                // Cart button + Wishlist button
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wishlist button
                    IconButton(
                        onClick = {
                            if (!wishlistLoading) {
                                wishlistLoading = true
                                scope.launch {
                                    try {
                                        if (isInWishlist) {
                                            val resp = RetrofitClient.apiService.removeFromWishlist(productId)
                                            if (resp.isSuccessful) isInWishlist = false
                                        } else {
                                            val resp = RetrofitClient.apiService.addToWishlist(productId)
                                            if (resp.isSuccessful) isInWishlist = true
                                        }
                                    } catch (_: Exception) {}
                                    wishlistLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                CircleShape
                            )
                    ) {
                        if (wishlistLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isInWishlist) "Remove from Wishlist" else "Add to Wishlist",
                                tint = if (isInWishlist) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Cart button
                    if (cartItems.isNotEmpty()) {
                        IconButton(
                            onClick = onCartClick,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    CircleShape
                                )
                        ) {
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
                }
            }

            // Content sheet
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.bottomBarTop,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Name & Price
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "₹${(product.price + (selectedVariant?.extraPrice ?: 0.0)).toInt()}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product.rating > 0) {
                            Surface(
                                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Caramel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", product.rating),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = Nunito,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                        // Stock indicator
                        if (!isOutOfStock && product.stockCount <= 5) {
                            Text(
                                text = "Only ${product.stockCount} left!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    product.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Nunito),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Waitlist section for out-of-stock products
                    if (isOutOfStock) {
                        Surface(
                            shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (onWaitlist) "You'll be notified when back in stock!"
                                    else "Get notified when this item is back in stock",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Nunito,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        waitlistLoading = true
                                        scope.launch {
                                            try {
                                                if (onWaitlist) {
                                                    val resp = RetrofitClient.apiService.leaveWaitlist(productId)
                                                    if (resp.isSuccessful) onWaitlist = false
                                                } else {
                                                    val resp = RetrofitClient.apiService.joinWaitlist(productId)
                                                    if (resp.isSuccessful) onWaitlist = true
                                                }
                                            } catch (_: Exception) {}
                                            waitlistLoading = false
                                        }
                                    },
                                    enabled = !waitlistLoading,
                                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.small,
                                    colors = if (onWaitlist) ButtonDefaults.outlinedButtonColors()
                                    else ButtonDefaults.buttonColors()
                                ) {
                                    if (waitlistLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            if (onWaitlist) "Leave Waitlist" else "Notify Me",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = Nunito,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Variants (only if in stock)
                    if (!isOutOfStock && product.variants.isNotEmpty()) {
                        Text(
                            text = "Select Size",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            product.variants.forEach { variant ->
                                val isSelected = selectedVariant == variant
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedVariant = if (isSelected) null else variant },
                                    label = {
                                        Text(
                                            text = "${variant.value} (+₹${variant.extraPrice.toInt()})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = Nunito)
                                        )
                                    },
                                    shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.pill,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Eggless option (only if in stock)
                    if (!isOutOfStock && product.isEgglessAvailable) {
                        Surface(
                            shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🌱", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Eggless Version",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        "Available at no extra cost",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEggless,
                                    onCheckedChange = { isEggless = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Quantity (only if in stock)
                    if (!isOutOfStock) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { if (quantity > 1) quantity-- }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "−",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = Nunito,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        if (quantity < product.stockCount) quantity++
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "+",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Reviews section
                    if (detailState.reviews.isNotEmpty()) {
                        Text(
                            text = "Reviews (${detailState.reviews.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        detailState.reviews.take(3).forEach { review ->
                            Surface(
                                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.input,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(review.rating) {
                                            Icon(
                                                Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Caramel,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    review.comment?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Bottom Add to Cart bar (hidden when out of stock)
        if (!isOutOfStock) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = com.saibabui.openbake.ui.theme.OpenBakeShapes.sheetTop
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Nunito),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${((product.price + (selectedVariant?.extraPrice ?: 0.0)) * quantity).toInt()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = Nunito,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    GradientButton(
                        text = "Add to Cart",
                        onClick = {
                            if (product.stockCount <= 0 || !product.isAvailable) {
                                stockError = "Sorry, this item is currently out of stock."
                            } else if (quantity > product.stockCount) {
                                stockError = "Only ${product.stockCount} items available in stock."
                            } else {
                                val added = cartViewModel.addItem(product, quantity, selectedVariant, isEggless)
                                if (added) {
                                    onBack()
                                } else {
                                    stockError = "Cannot add to cart. Stock limit reached."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Snackbar for stock errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (!isOutOfStock) 90.dp else 16.dp)
        )
    }
}
