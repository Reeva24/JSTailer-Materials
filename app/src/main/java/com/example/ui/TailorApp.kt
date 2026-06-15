package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*

// Categories in JS Tailer Materials
val categoriesList = listOf("All", "Thread", "Lace", "Gota", "Ribbon", "Buttons")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailorApp(viewModel: TailorViewModel) {
    val context = LocalContext.current
    val userMode by viewModel.userMode.collectAsStateWithLifecycle()
    val buyerTabByVM by viewModel.buyerTab.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val cart by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle(0.0)
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle(0)
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BorderColor,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "JS Tailer Materials",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Luxury Trims & Elements",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Modern user mode pill toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.setUserMode("BUYER") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userMode == "BUYER") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (userMode == "BUYER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Buyer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.setUserMode("SELLER") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userMode == "SELLER") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (userMode == "SELLER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Seller/Admin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (userMode == "BUYER") {
                NavigationBar {
                    NavigationBarItem(
                        selected = buyerTabByVM == "EXPLORE",
                        onClick = { viewModel.setBuyerTab("EXPLORE") },
                        label = { Text("Browse") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Browse materials"
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = buyerTabByVM == "CART",
                        onClick = { viewModel.setBuyerTab("CART") },
                        label = { Text("My Cart") },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge { Text(cartCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "My Cart items"
                                )
                            }
                        }
                    )
                    NavigationBarItem(
                        selected = buyerTabByVM == "ESTIMATOR",
                        onClick = { viewModel.setBuyerTab("ESTIMATOR") },
                        label = { Text("AI Assistant") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Quantity Estimator"
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (userMode == "BUYER") {
                when (buyerTabByVM) {
                    "EXPLORE" -> BuyerExploreScreen(viewModel)
                    "CART" -> BuyerCartScreen(viewModel, cart, cartTotal)
                    "ESTIMATOR" -> AIEstimatorScreen(viewModel)
                }
            } else {
                SellerConsoleScreen(viewModel, products)
            }

            // Global Full Stitch & Zoom Detail Dialog for inspectors
            selectedProduct?.let { product ->
                DetailInspectionDialog(
                    product = product,
                    onDismiss = { viewModel.selectProduct(null) },
                    onAddToCart = { qty ->
                        viewModel.addToCart(product, qty)
                        Toast.makeText(context, "${product.name} added to cart!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// --- BUYER SCREENS ---

@Composable
fun BuyerExploreScreen(viewModel: TailorViewModel) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val activeFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    val filteredProducts = remember(products, activeFilter) {
        if (activeFilter == "All") products else products.filter { it.category == activeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcoming header
        Text(
            text = "Explore Premium Materials",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select any product to inspect stitch count, mesh pattern, and width under our microscopic zoom lens.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom horizontal styling scrollable categories row
        ScrollableTabRow(
            selectedTabIndex = categoriesList.indexOf(activeFilter).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {},
            indicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            categoriesList.forEach { category ->
                val selected = (category == activeFilter)
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { viewModel.setCategoryFilter(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty list",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No material listings in this category",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->
                    ProductGridCard(
                        product = product,
                        onClick = { viewModel.selectProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val primaryUrl = product.mediaUrls.split("|").firstOrNull() ?: ""
            ImageLoader(
                url = primaryUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            // Category tag overlay
            Surface(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)} / ${product.unitType}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Color coded stock level warning
                val textStock = when {
                    product.stockQuantity <= 0 -> "Out of Stock"
                    product.stockQuantity < 10 -> "Only ${product.stockQuantity.toInt()} left"
                    else -> "${product.stockQuantity.toInt()} available"
                }
                val stockColor = when {
                    product.stockQuantity <= 0 -> Color.Red
                    product.stockQuantity < 10 -> Color(0xFFE65100)
                    else -> Color(0xFF2E7D32)
                }
                Text(
                    text = textStock,
                    fontSize = 9.sp,
                    color = stockColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BuyerCartScreen(viewModel: TailorViewModel, cart: List<CartItem>, total: Double) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Shopping Cart",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Buy by customizable metrics matching tailor requirements.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Empty cart",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your materials cart is empty",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ImageLoader(
                                url = item.imageUrl,
                                contentDescription = item.productName,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.category} • $${String.format("%.2f", item.price)} per ${item.unitType}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Quantity selector
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.id, item.quantity - 1.0) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircleOutline,
                                            contentDescription = "Decrease",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unitType}s",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.id, item.quantity + 1.0) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Increase",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.height(70.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.deleteCartItem(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "$${String.format("%.2f", item.price * item.quantity)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimated Total:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.executeCheckout()
                            Toast.makeText(context, "Purchase Completed! Stock quantities updated.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = "Checkout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Materials Order")
                    }
                }
            }
        }
    }
}

@Composable
fun AIEstimatorScreen(viewModel: TailorViewModel) {
    val inputDesc by viewModel.estimatorInput.collectAsStateWithLifecycle()
    val loading by viewModel.estimatorLoading.collectAsStateWithLifecycle()
    val result by viewModel.estimatedResult.collectAsStateWithLifecycle()
    val error by viewModel.aiError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val suggestionChips = listOf(
        "Heavy Gota Anarkali Lehenga Suit",
        "Simple Kurta with Scallop Lace sleeves",
        "Designer Wedding Sherwani with brass work"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Smart Quantity Estimator",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Tell the AI what sewing project you are planning, and it will estimate the exact trim lengths and threads, with quick automatic binding to your shopping cart!",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Suggestion Chips
        Text(
            text = "Quick Suggestion Prompts:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionChips.forEach { chip ->
                SuggestionChip(
                    onClick = { viewModel.setEstimatorInput(chip) },
                    label = { Text(chip, fontSize = 10.sp, maxLines = 1) }
                )
            }
        }

        OutlinedTextField(
            value = inputDesc,
            onValueChange = { viewModel.setEstimatorInput(it) },
            placeholder = { Text("Describe what you are sewing: style, length, flares, heavy decoration...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            label = { Text("What are you sewing?") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.estimateProjectMaterials() },
            enabled = !loading && inputDesc.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing pattern with Gemini...")
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Run AI")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Estimate Shopping List")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp
                )
            }
        }

        // Display Result and bind directly to shopping cart
        result?.let { est ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Text(
                        text = "Estimated Material Breakdown:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "A.I. Smart Estimation Metrics",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricResultPill("Gota", "${est.gota_yards} Yds", Icons.Default.Straighten)
                                MetricResultPill("Lace", "${est.lace_yards} Yds", Icons.Default.Stream)
                                MetricResultPill("Ribbon", "${est.ribbon_meters} Mtr", Icons.Default.Label)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricResultPill("Thread", "${est.thread_spools} Spls", Icons.Default.Texture)
                                MetricResultPill("Buttons", "${est.buttons_dozen} Doz", Icons.Default.Radio)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Why this estimate?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = est.explanation,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.addEstimatedItemsToCart()
                            Toast.makeText(context, "All matching estimated materials added to Cart!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add all to cart")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add AI Shopping List to Cart")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricResultPill(title: String, quantity: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(quantity, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// --- SELLER / ADMIN CONSOLE ---

@Composable
fun SellerConsoleScreen(viewModel: TailorViewModel, products: List<Product>) {
    var showAddPane by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Seller Control Deck",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage inventory catalogs and smart visual ingestion.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showAddPane = !showAddPane },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (showAddPane) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = if (showAddPane) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Toggle Ingest Pane"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showAddPane) {
            // Expandable Add screen / Ingest Panel
            AdminAddMaterialPane(viewModel, onSubmitted = { showAddPane = false })
        } else {
            // Normal Stock Inventory List
            Text(
                text = "Live Store Inventory",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products in directory")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val primImg = p.mediaUrls.split("|").firstOrNull() ?: ""
                                ImageLoader(
                                    url = primImg,
                                    contentDescription = p.name,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Category: ${p.category} | Price: $${String.format("%.2f", p.price)}/${p.unitType}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Quantity: ${p.stockQuantity} ${p.unitType}s in Stock",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.stockQuantity <= 5.0) Color.Red else Color(0xFF2E7D32)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = { viewModel.markProductAsExpired(p.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = "Expire Listing",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteProduct(p.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Admin Ingest Pane offering dynamic photo processing
@Composable
fun AdminAddMaterialPane(viewModel: TailorViewModel, onSubmitted: () -> Unit) {
    val ingestedResult by viewModel.ingestedResult.collectAsStateWithLifecycle()
    val loading by viewModel.ingestionLoading.collectAsStateWithLifecycle()
    val error by viewModel.aiError.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Gota") }
    var priceStr by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("Yard") }
    var description by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var imageUlrText by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Detect if AI filled up properties so fields stay synchronized
    LaunchedEffect(ingestedResult) {
        ingestedResult?.let { res ->
            name = res.material_name
            category = res.category
            priceStr = res.suggested_price.toString()
            unitType = res.unit_type
            description = "${res.pattern_description} Width: ${res.estimated_width}. Smart Classification: Detected ${res.color} color profiles."
            stockStr = "100" // reasonable default quantity
            imageUlrText = when (res.category) {
                "Gota" -> "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=400&q=80"
                "Thread" -> "https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&w=400&q=80"
                "Lace" -> "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=400&q=80"
                "Buttons" -> "https://images.unsplash.com/photo-1548224859-ac9cb3692c9a?auto=format&fit=crop&w=400&q=80"
                else -> "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=400&q=80"
            }
            Toast.makeText(context, "Fields Autofilled by Gemini AI Engine!", Toast.LENGTH_SHORT).show()
            viewModel.clearIngestedResult()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Smart Visual Ingestion Mode",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Simulate snapping a photograph of a lace or gota strip. Gemini will identify color, width, recommended price, category, and autofill the form with pristine precision.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.85f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val b = createSimulatedMaterialPattern("Gota", android.graphics.Color.YELLOW)
                                viewModel.runVisualIngestion(b)
                            },
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Ingest Gota", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val b = createSimulatedMaterialPattern("Lace", android.graphics.Color.WHITE)
                                viewModel.runVisualIngestion(b)
                            },
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Ingest Lace", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val b = createSimulatedMaterialPattern("Buttons", android.graphics.Color.GRAY)
                                viewModel.runVisualIngestion(b)
                            },
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Ingest Buttons", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text("Gemini is examining fabric texture grids...", fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        error?.let {
            item {
                Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.clearAIError() }) { Text("Clear Error") }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Material Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ScrollableTabRow(
                        selectedTabIndex = categoriesList.drop(1).indexOf(category).coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        categoriesList.drop(1).forEach { cat ->
                            Tab(
                                selected = (category == cat),
                                onClick = { category = cat },
                                text = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = unitType,
                    onValueChange = { unitType = it },
                    label = { Text("Metric Unit") }, // Yard, Meter, Dozen, Box
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Stock Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = imageUlrText,
                    onValueChange = { imageUlrText = it },
                    label = { Text("High-Res Image URL") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Stitch & Quality Notes") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    val finalPrice = priceStr.toDoubleOrNull() ?: 3.99
                    val finalStock = stockStr.toDoubleOrNull() ?: 50.0
                    val finalImg = if (imageUlrText.isNotBlank()) imageUlrText else "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=400&q=80"

                    viewModel.createProduct(
                        name = name,
                        category = category,
                        price = finalPrice,
                        unitType = unitType,
                        description = description,
                        stockQuantity = finalStock,
                        mediaUrls = listOf(finalImg)
                    )
                    onSubmitted()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && priceStr.isNotBlank()
            ) {
                Text("Confirm Save Inventory Material")
            }
        }
    }
}

// --- MICRO INSPECTOR MATERIAL PREVIEW ZOOM DIALOG ---

@Composable
fun DetailInspectionDialog(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (Double) -> Unit
) {
    var count by remember { mutableStateOf(1.0) }
    val imageUrls = remember(product) { product.mediaUrls.split("|") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title and clear button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${product.category} • $${String.format("%.2f", product.price)} per ${product.unitType}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close description")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High performance zooming viewport
                Text(
                    text = "🔎 Ultra-High Zoom inspection lens toggle (Stitch Grid / Ruler Gauge below):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // High magnification Canvas Zoomable
                HighPerformanceZoomCanvas(
                    imageUrl = imageUrls.firstOrNull(),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description and properties
                Text(
                    text = "Styling Characteristics:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = product.description,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add quantity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Measure metric: ${product.unitType}s", fontSize = 11.sp, color = Color.Gray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { count = (count - 1.0).coerceAtLeast(1.0) }) {
                            Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Minus")
                        }
                        // Custom Metric unit inputs
                        OutlinedTextField(
                            value = if (count % 1.0 == 0.0) count.toInt().toString() else count.toString(),
                            onValueChange = {
                                val maybeDouble = it.toDoubleOrNull()
                                if (maybeDouble != null) count = maybeDouble
                            },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.width(70.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        IconButton(onClick = { count = (count + 1.0).coerceAtLeast(1.0) }) {
                            Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Plus")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onAddToCart(count)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add $count ${product.unitType}s to Cart")
                }
            }
        }
    }
}

// --- SUPPORTING IMAGE LOADER WITH MOCK FALLBACKS ---

@Composable
fun ImageLoader(url: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

// --- HELPER TO GENERATE HIGH-CONTRAST PATTERN BITMAP FOR AI ---

fun createSimulatedMaterialPattern(category: String, colorValue: Int): Bitmap {
    val b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint().apply {
        color = colorValue
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    // Draw canvas texture representation
    c.drawColor(android.graphics.Color.DKGRAY)
    
    when (category) {
        "Gota" -> {
            p.color = android.graphics.Color.YELLOW
            // Draw gold parallel lines simulating embroidery
            c.drawLine(10f, 20f, 118f, 20f, p)
            c.drawLine(10f, 40f, 118f, 40f, p)
            c.drawLine(10f, 60f, 118f, 60f, p)
            c.drawLine(10f, 80f, 118f, 80f, p)
            c.drawLine(10f, 100f, 118f, 100f, p)
        }
        "Lace" -> {
            p.color = android.graphics.Color.WHITE
            // Draw a lattice network/crochet grid
            for (i in 0..10) {
                c.drawLine(12f * i, 10f, 12f * i, 118f, p)
                c.drawLine(10f, 12f * i, 118f, 12f * i, p)
            }
        }
        "Buttons" -> {
            p.color = android.graphics.Color.LTGRAY
            // Draw circular nested buttons
            p.style = Paint.Style.FILL
            c.drawCircle(64f, 64f, 36f, p)
            p.color = android.graphics.Color.BLACK
            c.drawCircle(50f, 64f, 6f, p)
            c.drawCircle(78f, 64f, 6f, p)
        }
    }
    return b
}
