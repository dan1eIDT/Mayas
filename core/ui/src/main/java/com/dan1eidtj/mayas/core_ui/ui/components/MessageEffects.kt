/* Copyright (C) 2026 dan1eIDT */
package com.dan1eidtj.mayas.core_ui.ui.components

data class MessageEffectSpec(
    val emoji: String,
    val particleCount: Int = 7,
    val riseDistanceDp: Int = 260,
    val durationMs: Int = 2400,
    val startFontSizeSp: Int = 22,
    val endFontSizeSp: Int = 30
)

object MessageEffect {
    const val FIRE = "fire"
    const val HEART = "heart"
    const val CONFETTI = "confetti"
    const val SNOW = "snow"
    const val LIKE = "like"
    const val DISLIKE = "dislike"
}

object MessageEffects {
    val registry: Map<String, MessageEffectSpec> = mapOf(
        MessageEffect.FIRE to MessageEffectSpec(emoji = "🔥"),
        MessageEffect.HEART to MessageEffectSpec(emoji = "❤️"),
        MessageEffect.CONFETTI to MessageEffectSpec(emoji = "🎉", particleCount = 10, durationMs = 2600),
        MessageEffect.SNOW to MessageEffectSpec(emoji = "❄️", particleCount = 9, durationMs = 3000, riseDistanceDp = 220),
        MessageEffect.LIKE to MessageEffectSpec(emoji = "👍", particleCount = 5),
        MessageEffect.DISLIKE to MessageEffectSpec(emoji = "👎", particleCount = 5)
    )

    fun isValid(key: String?): Boolean = key != null && registry.containsKey(key)
}
