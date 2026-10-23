package com.dan1eidtj.mayas.core_ui.ui.components

internal enum class SeasonalEffectKind { SNOW, HEARTS, PETALS, SPARKLES, LEAVES }

internal fun seasonalEffectForDate(month: Int, day: Int): SeasonalEffectKind = when {
    month == 2 && day == 14 -> SeasonalEffectKind.HEARTS
    month == 2 && day == 23 -> SeasonalEffectKind.SPARKLES
    month == 3 && day == 8 -> SeasonalEffectKind.PETALS
    month == 4 && day == 20 -> SeasonalEffectKind.SPARKLES
    month == 5 && day == 9 -> SeasonalEffectKind.SPARKLES
    month == 1 && day <= 7 -> SeasonalEffectKind.SNOW
    month in listOf(12, 1, 2) -> SeasonalEffectKind.SNOW
    month in listOf(3, 4, 5) -> SeasonalEffectKind.PETALS
    month in listOf(6, 7, 8) -> SeasonalEffectKind.SPARKLES
    else -> SeasonalEffectKind.LEAVES
}
