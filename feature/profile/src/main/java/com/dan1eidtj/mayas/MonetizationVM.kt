package com.dan1eidtj.mayas

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class MonetizationVM : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    private val AD_LIMIT = 5
    private val AD_REWARD = 10
    private val RESET_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

    private var isWatchingAd = false

    fun watchAd(currentWatched: Int, lastTimestamp: Long, onLimit: () -> Unit) {
        if (uid.isEmpty()) return
        if (isWatchingAd) return
        isWatchingAd = true

        val currentTime = System.currentTimeMillis()
        val actualWatched = if (currentTime - lastTimestamp > RESET_INTERVAL_MS) 0 else currentWatched

        if (actualWatched < AD_LIMIT) {
            val updates = mapOf(
                "balance" to FieldValue.increment(AD_REWARD.toLong()),
                "dailyAdsWatched" to (actualWatched + 1),
                "lastAdWatchTimestamp" to currentTime
            )
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener { isWatchingAd = false }
                .addOnFailureListener { e ->
                    Log.e("MonetizationVM", "Ошибка начисления за рекламу", e)
                    isWatchingAd = false
                }
        } else {
            isWatchingAd = false
            onLimit()
        }
    }

    fun buyItem(
        itemId: String,
        price: Int,
        onLowBalance: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (uid.isEmpty()) return

        val userRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val balance = snapshot.getLong("balance") ?: 0L

            if (balance < price) throw Exception("LOW_BALANCE")

            transaction.update(userRef, "balance", FieldValue.increment(-price.toLong()))
            transaction.update(userRef, "ownedItems", FieldValue.arrayUnion(itemId))
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            if (e.message == "LOW_BALANCE") {
                onLowBalance()
            } else {
                Log.e("MonetizationVM", "Ошибка покупки", e)
                onError(e.localizedMessage ?: "Ошибка транзакции")
            }
        }
    }

    fun redeemPromoCode(code: String, onResult: (String) -> Unit) {
        if (uid.isEmpty()) return

        db.collection("promo_codes").document(code).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val isActive = doc.getBoolean("active") ?: false
                val usesLeft = doc.getLong("usesLeft")?.toInt() ?: 0
                val type = doc.getString("type") ?: ""
                val value = doc.getLong("value")?.toInt() ?: 0

                if (isActive && (usesLeft > 0 || usesLeft == -1)) {
                    val batch = db.batch()
                    val userRef = db.collection("users").document(uid)

                    if (type == "premium") {
                        batch.update(userRef, "isPremium", true)
                    } else if (type == "balance") {
                        batch.update(userRef, "balance", FieldValue.increment(value.toLong()))
                    }

                    if (usesLeft != -1) {
                        batch.update(doc.reference, "usesLeft", FieldValue.increment(-1))
                        if (usesLeft == 1) batch.update(doc.reference, "active", false)
                    }

                    batch.commit()
                        .addOnSuccessListener { onResult("Промокод успешно активирован!") }
                        .addOnFailureListener { onResult("Ошибка при активации кода") }
                } else {
                    onResult("Промокод недействителен или истек")
                }
            } else {
                onResult("Промокод не найден")
            }
        }.addOnFailureListener {
            onResult("Ошибка сети")
        }
    }
}
