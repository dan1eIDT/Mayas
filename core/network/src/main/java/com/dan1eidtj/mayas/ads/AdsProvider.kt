package com.dan1eidtj.mayas.ads

import android.app.Activity
import android.view.ViewGroup

interface AdsProvider {

    fun initialize(activity: Activity)

    fun loadRewarded()

    fun isRewardedAvailable(): Boolean

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onError: (String) -> Unit = {}
    )

    fun loadInterstitial()

    fun showInterstitial(activity: Activity)

    fun loadBanner(container: ViewGroup)

    fun destroy()
}