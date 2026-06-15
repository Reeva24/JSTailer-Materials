package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TailorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TailorRepository
    private val TAG = "TailorViewModel"

    // --- State Management ---
    
    // Core Navigation Modes: "BUYER" or "SELLER"
    private val _userMode = MutableStateFlow("BUYER")
    val userMode: StateFlow<String> = _userMode.asStateFlow()

    // Active Buyer tab: "EXPLORE", "CART", "ESTIMATOR"
    private val _buyerTab = MutableStateFlow("EXPLORE")
    val buyerTab: StateFlow<String> = _buyerTab.asStateFlow()

    // Active Category Filter for Buyer explore (empty or category)
    private val _categoryFilter = MutableStateFlow("All")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    // Active Selected Product for detailed view / Zooming inspection
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // Dynamic Database Content Flows
    val allProducts: StateFlow<List<Product>>
    val cartItems: StateFlow<List<CartItem>>

    // Calculated fields
    val cartTotal: Flow<Double>
    val cartCount: Flow<Int>

    // --- AI feature states ---
    
    // AI Ingestion State
    private val _ingestionLoading = MutableStateFlow(false)
    val ingestionLoading: StateFlow<Boolean> = _ingestionLoading.asStateFlow()

    private val _ingestedResult = MutableStateFlow<VisualIngestionResult?>(null)
    val ingestedResult: StateFlow<VisualIngestionResult?> = _ingestedResult.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // AI Estimator State
    private val _estimatorLoading = MutableStateFlow(false)
    val estimatorLoading: StateFlow<Boolean> = _estimatorLoading.asStateFlow()

    private val _estimatedResult = MutableStateFlow<QuantityEstimatorResult?>(null)
    val estimatedResult: StateFlow<QuantityEstimatorResult?> = _estimatedResult.asStateFlow()

    private val _estimatorInput = MutableStateFlow("")
    val estimatorInput: StateFlow<String> = _estimatorInput.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TailorRepository(database)

        // Seed initial data and configure Flow emission
        viewModelScope.launch {
            try {
                repository.seedInitialDataIfNecessary()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding database", e)
            }
        }

        allProducts = repository.allProducts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        cartItems = repository.cartItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        cartTotal = cartItems.map { items ->
            items.sumOf { it.price * it.quantity }
        }

        cartCount = cartItems.map { items ->
            items.sumOf { it.quantity.toInt() }
        }
    }

    fun setUserMode(mode: String) {
        _userMode.value = mode
    }

    fun setBuyerTab(tab: String) {
        _buyerTab.value = tab
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun setEstimatorInput(input: String) {
        _estimatorInput.value = input
    }

    fun clearAIError() {
        _aiError.value = null
    }

    // --- Cart Actions ---

    fun addToCart(product: Product, buyQuantity: Double) {
        viewModelScope.launch {
            val existing = cartItems.value.find { it.productId == product.id }
            if (existing != null) {
                // If already in cart, increment quantity
                val newQty = existing.quantity + buyQuantity
                repository.updateCartQuantity(existing.id, newQty)
            } else {
                val mediaList = product.mediaUrls.split("|")
                val primaryImage = mediaList.firstOrNull() ?: ""
                val newItem = CartItem(
                    productId = product.id,
                    productName = product.name,
                    category = product.category,
                    price = product.price,
                    unitType = product.unitType,
                    quantity = buyQuantity,
                    imageUrl = primaryImage
                )
                repository.insertCartItem(newItem)
            }
        }
    }

    fun updateCartItemQuantity(cartItemId: Long, quantity: Double) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, quantity)
        }
    }

    fun deleteCartItem(cartItemId: Long) {
        viewModelScope.launch {
            repository.deleteCartItem(cartItemId)
        }
    }

    fun executeCheckout() {
        viewModelScope.launch {
            repository.checkout()
        }
    }

    // --- CRUD Admin Actions ---

    fun createProduct(
        name: String,
        category: String,
        price: Double,
        unitType: String,
        description: String,
        stockQuantity: Double,
        mediaUrls: List<String>
    ) {
        viewModelScope.launch {
            val pipeUrls = mediaUrls.joinToString("|")
            val newProduct = Product(
                name = name,
                category = category,
                price = price,
                unitType = unitType,
                description = description,
                stockQuantity = stockQuantity,
                mediaUrls = pipeUrls
            )
            repository.insertProduct(newProduct)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun updateStock(productId: Long, newStock: Double) {
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            if (product != null) {
                repository.updateProduct(product.copy(stockQuantity = newStock))
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }

    fun markProductAsExpired(productId: Long) {
        viewModelScope.launch {
            repository.markAsExpired(productId)
        }
    }

    // --- AI Smart Visual Ingestion ---

    fun runVisualIngestion(bitmap: Bitmap) {
        viewModelScope.launch {
            _ingestionLoading.value = true
            _ingestedResult.value = null
            _aiError.value = null
            try {
                val result = GeminiService.analyzeMaterialImage(bitmap)
                _ingestedResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Visual ingestion run error", e)
                _aiError.value = "Ingestion failed: ${e.localizedMessage}"
            } finally {
                _ingestionLoading.value = false
            }
        }
    }

    fun clearIngestedResult() {
        _ingestedResult.value = null
    }

    // --- AI Smart Quantity Estimator & Shopping Cart Integrator ---

    fun estimateProjectMaterials() {
        val query = _estimatorInput.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _estimatorLoading.value = true
            _estimatedResult.value = null
            _aiError.value = null
            try {
                val result = GeminiService.estimateProjectMaterials(query)
                _estimatedResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Materials estimation error", e)
                _aiError.value = "Estimation failed: ${e.localizedMessage}"
            } finally {
                _estimatorLoading.value = false
            }
        }
    }

    /**
     * Connects UI estimations directly to search results or automatic cart entry.
     * Looks up products matching Categories (Thread, Lace, Gota, Ribbon, Buttons) and adds them!
     */
    fun addEstimatedItemsToCart() {
        val result = _estimatedResult.value ?: return
        viewModelScope.launch {
            val availableProducts = allProducts.value
            
            // 1. Gota
            if (result.gota_yards > 0) {
                val gotaProduct = availableProducts.find { it.category == "Gota" }
                if (gotaProduct != null) addToCart(gotaProduct, result.gota_yards)
            }
            
            // 2. Lace
            if (result.lace_yards > 0) {
                val laceProduct = availableProducts.find { it.category == "Lace" }
                if (laceProduct != null) addToCart(laceProduct, result.lace_yards)
            }

            // 3. Ribbon
            if (result.ribbon_meters > 0) {
                val ribbonProduct = availableProducts.find { it.category == "Ribbon" }
                if (ribbonProduct != null) addToCart(ribbonProduct, result.ribbon_meters)
            }

            // 4. Thread Spools
            if (result.thread_spools > 0) {
                // Determine if seller sells spools as spools or boxes. Let's add threads based on their unit.
                val threadProduct = availableProducts.find { it.category == "Thread" }
                if (threadProduct != null) {
                    // Seeded Thread is Sold by "Box" of 12. If spools requested, let's round or scale.
                    val qtyToAdd = if (threadProduct.unitType == "Box") {
                        (result.thread_spools / 12.0).coerceAtLeast(1.0)
                    } else {
                        result.thread_spools.toDouble()
                    }
                    addToCart(threadProduct, qtyToAdd)
                }
            }

            // 5. Buttons
            if (result.buttons_dozen > 0) {
                val buttonsProduct = availableProducts.find { it.category == "Buttons" }
                if (buttonsProduct != null) addToCart(buttonsProduct, result.buttons_dozen)
            }
        }
    }
}
