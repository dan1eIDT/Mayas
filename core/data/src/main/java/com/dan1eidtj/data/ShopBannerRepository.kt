/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShopBannerRepository {
    private const val COLLECTION = "shop_banners"
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val _banners = MutableStateFlow<List<ShopBanner>>(emptyList())
    val banners: StateFlow<List<ShopBanner>> = _banners.asStateFlow()

    private var listenerRegistered = false

    @Synchronized
    fun startListening() {
        if (listenerRegistered) return
        listenerRegistered = true

        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        registration = db.collection(COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ShopBannerRepository", "load failed", error)
                    registration?.remove()
                    synchronized(this) { listenerRegistered = false }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val loaded = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.toShopBanner() }
                        .onFailure { Log.e("ShopBannerRepository", "bad banner ${doc.id}", it) }
                        .getOrNull()
                }

                _banners.value = loaded.sortedBy { it.order }
            }

        val teardown: () -> Unit = {
            registration?.remove()
            synchronized(this) { listenerRegistered = false }
        }
        FirestoreListenerCoordinator.register(teardown)
    }

    val activeBanners: List<ShopBanner>
        get() = _banners.value.filter { it.isActive }
}

private fun DocumentSnapshot.toShopBanner(): ShopBanner {
    return ShopBanner(
        id = id,
        title = getString("title") ?: id,
        subtitle = getString("subtitle") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        targetType = getString("targetType")?.let { runCatching { ItemType.valueOf(it) }.getOrNull() },
        order = (getLong("order") ?: 0L).toInt(),
        isActive = getBoolean("isActive") ?: true
    )
}
