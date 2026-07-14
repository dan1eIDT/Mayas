package com.dan1eidtj.mayas.ads.AdmobAds

import android.Manifest
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import com.dan1eidtj.mayas.ads.AdsProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "AdMobProvider"

enum class AdMobRewardedState {
    IDLE,
    LOADING,
    READY,
    SHOWING,
    FAILED
}

class AdMobProvider : AdsProvider {

    private var initialized = false

    private var rewardedAd: RewardedAd? = null
    var rewardedState: AdMobRewardedState = AdMobRewardedState.IDLE
        private set
    private var pendingReward: (() -> Unit)? = null

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    private var bannerView: AdView? = null

    override fun initialize(activity: Activity) {
        AdsActivityHolder.current = activity
        if (initialized) return
        MobileAds.initialize(activity) {
            Log.d(TAG, "AdMob initialized")
        }
        initialized = true
    }

    override fun loadRewarded() {
        if (rewardedAd != null) {
            Log.d(TAG, "loadRewarded skipped: ad already loaded")
            return
        }
        if (rewardedState == AdMobRewardedState.LOADING) {
            Log.d(TAG, "loadRewarded skipped: already loading")
            return
        }
        val activity = AdsActivityHolder.current ?: run {
            Log.w(TAG, "loadRewarded called without an Activity context")
            return
        }

        Log.d(TAG, "loadRewarded: requesting new ad…")
        rewardedState = AdMobRewardedState.LOADING
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            AdMobIds.REWARDED,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded ✅")
                    rewardedAd = ad
                    rewardedState = AdMobRewardedState.READY
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Rewarded load failed ❌ code=${adError.code} msg=${adError.message}")
                    rewardedAd = null
                    rewardedState = AdMobRewardedState.FAILED
                    scheduleRewardedRetry()
                }
            }
        )
    }

    override fun isRewardedAvailable(): Boolean = rewardedState == AdMobRewardedState.READY

    override fun showRewarded(activity: Activity, onReward: () -> Unit, onError: (String) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewarded called but no ad is ready")
            onError("Реклама сейчас недоступна, попробуй позже")
            loadRewarded()
            return
        }

        pendingReward = onReward

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded shown")
                rewardedState = AdMobRewardedState.SHOWING
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${adError.message}")
                rewardedState = AdMobRewardedState.FAILED
                pendingReward = null
                rewardedAd = null
                onError("Не удалось показать рекламу: ${adError.message}")
                loadRewarded()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded dismissed")
                rewardedState = AdMobRewardedState.IDLE
                rewardedAd = null
                loadRewarded()
            }

            override fun onAdClicked() {
                Log.d(TAG, "Rewarded clicked")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Rewarded impression")
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User rewarded: ${rewardItem.amount} ${rewardItem.type}")
            pendingReward?.invoke()
            pendingReward = null
        }
    }

    fun isRewardedReady(): Boolean = rewardedState == AdMobRewardedState.READY

    private fun scheduleRewardedRetry() {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Retrying rewarded load…")
            loadRewarded()
        }, 30_000L)
    }

    override fun loadInterstitial() {
        if (interstitialAd != null || interstitialLoading) return
        val activity = AdsActivityHolder.current ?: run {
            Log.w(TAG, "loadInterstitial called without an Activity context")
            return
        }

        interstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AdMobIds.INTERSTITIAL,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded ✅")
                    interstitialAd = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial load failed ❌ ${adError.message}")
                    interstitialAd = null
                    interstitialLoading = false
                }
            }
        )
    }

    override fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "showInterstitial called but no ad is ready")
            loadInterstitial()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                loadInterstitial()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown")
            }
        }

        ad.show(activity)
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override fun loadBanner(container: ViewGroup) {
        val activity = container.context as? Activity ?: return

        val adView = AdView(activity).apply {
            adUnitId = AdMobIds.BANNER
            setAdSize(AdSize.BANNER)
        }

        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
        bannerView = adView
    }

    override fun destroy() {
        rewardedAd = null
        rewardedState = AdMobRewardedState.IDLE
        pendingReward = null

        interstitialAd = null
        interstitialLoading = false

        bannerView?.destroy()
        bannerView = null

        initialized = false
        Log.d(TAG, "AdMobProvider destroyed")
    }
}

object AdsActivityHolder {
    var current: Activity? = null
}
