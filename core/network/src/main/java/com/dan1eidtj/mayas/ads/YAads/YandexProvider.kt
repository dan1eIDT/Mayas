package com.dan1eidtj.mayas.ads.YAads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import com.dan1eidtj.mayas.ads.AdsProvider
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

enum class RewardedState {
    IDLE,
    LOADING,
    READY,
    SHOWING,
    FAILED
}

private const val TAG = "YandexProvider"

class YandexProvider : AdsProvider {


    private var rewardedAd: RewardedAd? = null
    private var rewardedLoader: RewardedAdLoader? = null
    private var initialized = false

    var rewardedState: RewardedState = RewardedState.IDLE
        private set


    private var pendingReward: (() -> Unit)? = null


    override fun initialize(activity: Activity) {
        if (initialized) return

        MobileAds.initialize(activity) {
            Log.d(TAG, "Yandex Mobile Ads initialized")
        }

        setupRewardedLoader(activity)
        initialized = true
    }


    private fun setupRewardedLoader(activity: Activity) {
        rewardedLoader = RewardedAdLoader(activity).apply {
            setAdLoadListener(object : RewardedAdLoadListener {

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded ✅")
                    this@YandexProvider.rewardedAd = rewardedAd
                    rewardedState = RewardedState.READY
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    Log.e(TAG, "Rewarded load failed ❌ code=${adRequestError.code} msg=${adRequestError.description}")
                    rewardedAd = null
                    rewardedState = RewardedState.FAILED
                    scheduleRetry(activity)
                }
            })
        }
    }


    override fun loadRewarded() {
        if (rewardedAd != null) {
            Log.d(TAG, "loadRewarded skipped: ad already loaded")
            return
        }
        if (rewardedState == RewardedState.LOADING) {
            Log.d(TAG, "loadRewarded skipped: already loading")
            return
        }

        Log.d(TAG, "loadRewarded: requesting new ad…")
        rewardedState = RewardedState.LOADING
        val adRequestConfiguration = AdRequestConfiguration.Builder(YandexIds.REWARDED).build()
        rewardedLoader?.loadAd(adRequestConfiguration)
    }


    override fun showRewarded(activity: Activity, onReward: () -> Unit, onError: (String) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewarded called but no ad is ready")
            onError("Реклама сейчас недоступна, попробуй позже")
            loadRewarded()
            return
        }

        pendingReward = onReward

        ad.setAdEventListener(object : RewardedAdEventListener {

            override fun onAdShown() {
                Log.d(TAG, "Rewarded shown")
                rewardedState = RewardedState.SHOWING
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${adError.description}")
                rewardedState = RewardedState.FAILED
                pendingReward = null
                cleanupRewardedAd()
                onError("Не удалось показать рекламу: ${adError.description}")
                loadRewarded()
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Rewarded dismissed")
                rewardedState = RewardedState.IDLE
                cleanupRewardedAd()

                loadRewarded()
            }

            override fun onAdClicked() {
                Log.d(TAG, "Rewarded clicked")
            }

            override fun onAdImpression(impressionData: ImpressionData?) {
                Log.d(TAG, "Rewarded impression")
            }

            override fun onRewarded(reward: Reward) {
                Log.d(TAG, "User rewarded: ${reward.amount} ${reward.type}")
                pendingReward?.invoke()
                pendingReward = null
            }
        })

        ad.show(activity)
    }


    fun isRewardedReady(): Boolean = rewardedState == RewardedState.READY

    override fun isRewardedAvailable(): Boolean = rewardedState == RewardedState.READY


    private fun cleanupRewardedAd() {
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
    }


    private fun scheduleRetry(activity: Activity) {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Retrying rewarded load…")
            loadRewarded()
        }, 30_000L)
    }


    override fun loadInterstitial() {  }
    override fun showInterstitial(activity: Activity) {  }
    override fun loadBanner(container: ViewGroup) { }


    override fun destroy() {
        rewardedLoader?.setAdLoadListener(null)
        rewardedLoader = null
        cleanupRewardedAd()
        rewardedState = RewardedState.IDLE
        pendingReward = null
        initialized = false
        Log.d(TAG, "YandexProvider destroyed")
    }
}