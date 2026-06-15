package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Entities ---

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Thread", "Lace", "Gota", "Ribbon", "Buttons"
    val price: Double,
    val unitType: String, // "Spool", "Box", "Yard", "Meter", "Dozen"
    val description: String,
    val stockQuantity: Double,
    val mediaUrls: String, // pipe separated list of URLs (e.g. "url1|url2")
    val expired: Boolean = false
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val category: String,
    val price: Double,
    val unitType: String,
    val quantity: Double,
    val imageUrl: String
)

// --- 2. DAOs ---

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE expired = 0 ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("UPDATE products SET expired = 1 WHERE id = :id")
    suspend fun markAsExpired(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

// --- 3. Database ---

@Database(entities = [Product::class, CartItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tailor_materials_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- 4. Repository ---

class TailorRepository(private val db: AppDatabase) {
    val allProducts: Flow<List<Product>> = db.productDao().getAllProducts()
    val cartItems: Flow<List<CartItem>> = db.cartDao().getCartItems()

    suspend fun getProductById(id: Long): Product? {
        return db.productDao().getProductById(id)
    }

    suspend fun insertProduct(product: Product): Long {
        return db.productDao().insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        db.productDao().updateProduct(product)
    }

    suspend fun deleteProduct(id: Long) {
        db.productDao().deleteProductById(id)
    }

    suspend fun markAsExpired(id: Long) {
        db.productDao().markAsExpired(id)
    }

    suspend fun insertCartItem(cartItem: CartItem) {
        db.cartDao().insertCartItem(cartItem)
    }

    suspend fun updateCartQuantity(id: Long, quantity: Double) {
        if (quantity <= 0) {
            db.cartDao().deleteCartItem(id)
        } else {
            db.cartDao().updateQuantity(id, quantity)
        }
    }

    suspend fun deleteCartItem(id: Long) {
        db.cartDao().deleteCartItem(id)
    }

    suspend fun checkout() {
        // Simple transaction logic to update inventory and clear cart
        // Fetch cart items
        db.cartDao().getCartItems().collect { cartItemsList ->
            for (item in cartItemsList) {
                val p = db.productDao().getProductById(item.productId)
                if (p != null) {
                    val newStock = (p.stockQuantity - item.quantity).coerceAtLeast(0.0)
                    db.productDao().updateProduct(p.copy(stockQuantity = newStock))
                }
            }
            db.cartDao().clearCart()
        }
    }

    suspend fun clearCart() {
        db.cartDao().clearCart()
    }

    suspend fun seedInitialDataIfNecessary() {
        if (db.productDao().getCount() == 0) {
            val initial = listOf(
                Product(
                    name = "Metallic Gold Zari Gota",
                    category = "Gota",
                    price = 4.50,
                    unitType = "Yard",
                    description = "Traditional Punjabi gota with high-density gold threads and a scalloped texture grid. Perfect for heavy dupatta linings and Anarkali dress borders.",
                    stockQuantity = 150.0,
                    mediaUrls = "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&w=400&q=80|https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=400&q=80"
                ),
                Product(
                    name = "Vibrant Silk Spool Multi-Set",
                    category = "Thread",
                    price = 14.99,
                    unitType = "Box",
                    description = "Box of 12 silk spools of extra-strong mercerized cotton sewing threads. Ideal for micro-stitching and high-speed automatic embroidery machines.",
                    stockQuantity = 40.0,
                    mediaUrls = "https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&w=400&q=80|https://images.unsplash.com/photo-1584282479213-90d1bf482e2c?auto=format&fit=crop&w=400&q=80"
                ),
                Product(
                    name = "Scalloped Ivory Crochet Lace",
                    category = "Lace",
                    price = 3.25,
                    unitType = "Meter",
                    description = "Exquisite floral crocheted cotton lace ribbon. Offers soft touch texture with highly defined lace grids and intricate hand-woven look patterns.",
                    stockQuantity = 220.0,
                    mediaUrls = "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=400&q=80|https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=400&q=80"
                ),
                Product(
                    name = "Double-Face Royal Satin Ribbon",
                    category = "Ribbon",
                    price = 1.80,
                    unitType = "Meter",
                    description = "Silky satin ribbon with dense weaving and rich pigment finish in Royal Velvet violet. Resists fraying with sealed edge design.",
                    stockQuantity = 300.0,
                    mediaUrls = "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=400&q=80|https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=400&q=80"
                ),
                Product(
                    name = "Ornate Antique Brass Buttons",
                    category = "Buttons",
                    price = 9.50,
                    unitType = "Dozen",
                    description = "Vintage coat buttons with dynamic floral carvings and heavy brass feel. Great for high-end Sherwanis, blouses, and designer tailoring outfits.",
                    stockQuantity = 25.0,
                    mediaUrls = "https://images.unsplash.com/photo-1548224859-ac9cb3692c9a?auto=format&fit=crop&w=400&q=80|https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&w=400&q=80"
                )
            )
            for (p in initial) {
                db.productDao().insertProduct(p)
            }
        }
    }
}
