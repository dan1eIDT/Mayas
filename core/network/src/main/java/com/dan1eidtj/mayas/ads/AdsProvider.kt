/* Copyright (C) 2026 ProjectIDT */
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
        onError: (String) -> Unit = {},
        onImpression: () -> Unit = {},
        onClosed: () -> Unit = {}
    )

    fun loadInterstitial()

    fun isInterstitialAvailable(): Boolean = false

    fun showInterstitial(activity: Activity, onImpression: () -> Unit = {})

    fun loadBanner(container: ViewGroup)

    fun destroy()
}