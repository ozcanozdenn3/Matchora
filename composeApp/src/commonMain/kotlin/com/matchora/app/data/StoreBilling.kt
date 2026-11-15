package com.matchora.app.data

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

expect val storePlatform: String
data class StoreListing(val sku: String, val price: String)

/** The SDK opens the native store. Only the authenticated API can change wallet/Plus state. */
object StoreBilling {
    private val mutex = Mutex()
    private val products = mutableMapOf<String, StoreProduct>()
    private var owner: String? = null
    private suspend fun bind(backend: BackendRepository, allowSalesDisabled: Boolean = false) {
        val user = backend.userId ?: throw BackendFailure("UNAUTHENTICATED", "Satın almak için giriş yapmalısın.")
        val store = backend.configuration?.o("store") ?: throw BackendFailure("STORE_UNAVAILABLE", "Mağaza henüz yapılandırılmadı.")
        val key = store.o("keys").s(storePlatform)
        if ((!store.b("enabled") && !allowSalesDisabled) || key.isBlank()) throw BackendFailure("STORE_UNAVAILABLE", "Mağaza henüz yapılandırılmadı.")
        if (!Purchases.isConfigured) Purchases.configure(PurchasesConfiguration.Builder(key).apply { appUserId = user }.build())
        else if (Purchases.sharedInstance.appUserID != user) suspendCoroutine<Unit> { continuation ->
            Purchases.sharedInstance.logIn(user, onError = { continuation.resumeWithException(BackendFailure("STORE_IDENTITY", "Mağaza hesabı eşleştirilemedi. Lütfen tekrar dene.")) }, onSuccess = { _, _ -> continuation.resume(Unit) })
        }
        if (owner != user) { products.clear(); owner = user }
    }
    suspend fun catalog(backend: BackendRepository): List<StoreListing> = mutex.withLock {
        bind(backend)
        val mapping = backend.configuration?.o("store")?.o("products")?.o(storePlatform) ?: return@withLock emptyList()
        val ids = mapping.values.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.distinct()
        if (ids.isEmpty()) throw BackendFailure("STORE_PRODUCTS_PENDING", "Mağaza ürünleri henüz tanımlanmadı.")
        val listings = suspendCoroutine<List<StoreProduct>> { continuation ->
            Purchases.sharedInstance.getProducts(ids, onError = { continuation.resumeWithException(BackendFailure("STORE_PRODUCTS", "Mağaza ürün bilgileri alınamadı. Bağlantını kontrol et.")) }, onSuccess = { continuation.resume(it) })
        }
        products.clear()
        for ((sku, value) in mapping) listings.find { it.id == (value as? kotlinx.serialization.json.JsonPrimitive)?.content }?.let { products[sku] = it }
        products.map { StoreListing(it.key, it.value.price.formatted) }
    }
    suspend fun purchase(backend: BackendRepository, sku: String) = mutex.withLock {
        backend.refreshConfiguration()
        bind(backend)
        val product = products[sku] ?: throw BackendFailure("STORE_PRODUCT_UNAVAILABLE", "Bu ürün mağazadan yüklenemedi. Tekrar dene.")
        suspendCoroutine<Unit> { continuation ->
            Purchases.sharedInstance.purchase(product, onError = { _, cancelled ->
                continuation.resumeWithException(BackendFailure(if (cancelled) "PURCHASE_CANCELLED" else "PURCHASE_FAILED", if (cancelled) "Satın alma iptal edildi." else "Satın alma tamamlanamadı. Lütfen tekrar dene."))
            }, onSuccess = { _, _ -> continuation.resume(Unit) })
        }
        synchronize(backend)
    }
    suspend fun restore(backend: BackendRepository) = mutex.withLock {
        backend.refreshConfiguration()
        bind(backend, allowSalesDisabled = true)
        suspendCoroutine<Unit> { continuation -> Purchases.sharedInstance.restorePurchases(onError = {
            continuation.resumeWithException(BackendFailure("RESTORE_FAILED", "Satın alımlar geri yüklenemedi. Lütfen tekrar dene."))
        }, onSuccess = { continuation.resume(Unit) }) }
        synchronize(backend)
    }
    private suspend fun synchronize(backend: BackendRepository) {
        if (backend.configuration?.o("store")?.b("sync") == true) backend.command("billing.sync", apiPayload(), backend.requestKey("store-sync"))
        else { repeat(4) { backend.reload(); delay(750) } }
    }
    fun clearProducts() { products.clear(); owner = null }
}
