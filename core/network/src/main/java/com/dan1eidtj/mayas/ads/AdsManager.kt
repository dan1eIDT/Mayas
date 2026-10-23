/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.ads

import android.app.Activity
import android.view.ViewGroup
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.dan1eidtj.mayas.ads.model.AdType

object AdsManager {

    private val provider by lazy {
        AdsFactory.create()
    }

    fun initialize(activity: Activity) =
        provider.initialize(activity)

    fun loadRewarded() =
        provider.loadRewarded()

    fun isRewardedAvailable(): Boolean =
        provider.isRewardedAvailable()

    fun remainingAdsToday(activity: Activity): Int =
        AdLimitPrefs.remainingToday(activity)

    private fun recordProfileImpression() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("adsViewed", FieldValue.increment(1L))
            .addOnFailureListener { error ->
                Log.w("AdsManager", "Не удалось сохранить счётчик рекламных показов", error)
            }
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onError: (String) -> Unit = {},
        onCaptchaRequired: () -> Unit = {},
        onClosed: () -> Unit = {}
    ) {
        if (!AdLimitPrefs.canShowAd(activity)) {
            onError("Дневной лимит рекламы исчерпан, попробуй завтра")
            return
        }
        var impressionRecorded = false
        try {
            provider.showRewarded(
                activity = activity,
                onReward = onReward,
                onError = onError,
                onImpression = {
                    if (impressionRecorded) return@showRewarded
                    impressionRecorded = true
                    val result = AdLimitPrefs.recordAdShown(activity, AdType.REWARDED)
                    recordProfileImpression()
                    if (result.captchaRequired) onCaptchaRequired()
                },
                onClosed = onClosed
            )
        } catch (e: Exception) {
            onError(e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Не удалось открыть рекламу")
            onClosed()
        }
    }

    fun loadInterstitial() =
        provider.loadInterstitial()

    fun showInterstitial(
        activity: Activity,
        onCaptchaRequired: () -> Unit = {}
    ) {
        if (!AdLimitPrefs.canShowAd(activity)) return
        var impressionRecorded = false
        provider.showInterstitial(activity) {
            if (impressionRecorded) return@showInterstitial
            impressionRecorded = true
            val result = AdLimitPrefs.recordAdShown(activity, AdType.INTERSTITIAL)
            recordProfileImpression()
            if (result.captchaRequired) onCaptchaRequired()
        }
    }

    fun loadBanner(container: ViewGroup) =
        provider.loadBanner(container)

    fun destroy() =
        provider.destroy()
}
