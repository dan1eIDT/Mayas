package com.dan1eidtj.mayas.ads

import com.dan1eidtj.mayas.ads.AdmobAds.AdMobProvider
import com.dan1eidtj.mayas.ads.YAads.YandexProvider
import java.util.Locale
object AdsFactory {

    fun create(): AdsProvider {

        return when (Locale.getDefault().country.uppercase()) {

            "RU",
            "BY",
            "KZ" -> YandexProvider()

            else -> AdMobProvider()
        }
    }
}