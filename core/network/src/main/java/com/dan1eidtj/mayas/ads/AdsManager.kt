package com.dan1eidtj.mayas.ads

import android.app.Activity
import android.view.ViewGroup

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

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onError: (String) -> Unit = {}
    ) =
        provider.showRewarded(activity, onReward, onError)

    fun loadInterstitial() =
        provider.loadInterstitial()

    fun showInterstitial(activity: Activity) =
        provider.showInterstitial(activity)

    fun loadBanner(container: ViewGroup) =
        provider.loadBanner(container)

    fun destroy() =
        provider.destroy()
}