package com.dan1eidtj.mayas.ads

import com.dan1eidtj.mayas.ads.AdmobAds.AdMobProvider
import com.dan1eidtj.mayas.ads.YAads.YandexProvider

object AdsFactory {


    fun create(): AdsProvider = CompositeAdsProvider(
        primary = YandexProvider(),
        secondary = AdMobProvider()
    )
}
