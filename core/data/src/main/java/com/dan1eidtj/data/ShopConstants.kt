package com.dan1eidtj.data

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import kotlin.random.Random

object ShopConstants {
    val BUBBLE_STYLES = listOf(
        ShopItem("neon", "Неоновый", ItemType.BUBBLE, 150),
        ShopItem("gold", "Золотой", ItemType.BUBBLE, 300),
        ShopItem("fire", "Пламенный", ItemType.BUBBLE, 200),
        ShopItem("ice", "Ледяной", ItemType.BUBBLE, 180),
        ShopItem("matrix", "Матрица", ItemType.BUBBLE, 250),
        ShopItem("sunset", "Закат", ItemType.BUBBLE, 220),
        ShopItem("forest", "Лес", ItemType.BUBBLE, 190),
        ShopItem("midnight", "Полночь", ItemType.BUBBLE, 210),
        ShopItem("lavender", "Лаванда", ItemType.BUBBLE, 60),
        ShopItem("mint", "Мята", ItemType.BUBBLE, 60),
        ShopItem("coral", "Коралл", ItemType.BUBBLE, 65),
        ShopItem("steel", "Сталь", ItemType.BUBBLE, 50),
        ShopItem("amber", "Янтарь", ItemType.BUBBLE, 70),
        ShopItem("lime", "Лайм", ItemType.BUBBLE, 55),
        ShopItem("sky", "Небо", ItemType.BUBBLE, 60),
        ShopItem("plum", "Слива", ItemType.BUBBLE, 65),
        ShopItem("folli", "Фолли", ItemType.BUBBLE, 280, description = "Красный на белом"),
        ShopItem("dani", "Дани", ItemType.BUBBLE, 320, description = "Чёрный терминал с зелёным текстом"),
        ShopItem("sakura", "Сакура", ItemType.BUBBLE, 240, description = "Розовый закат"),
        ShopItem("intel", "Intel", ItemType.BUBBLE, 260, description = "Синий с голубым текстом")
    )
    val EMOJI_STATUSES = listOf(
        "⚡" to 30, "🔥" to 30, "💎" to 50, "👑" to 10000,
        "🛸" to 40, "🍀" to 25, "🧿" to 35, "🚀" to 60,
        "👻" to 20, "💀" to 20, "🐱" to 25, "🌈" to 45,
        // новые
        "🎯" to 30, "🎮" to 35, "🦄" to 90, "🐉" to 120,
        "🌊" to 30, "⭐" to 25, "🍉" to 20, "🎧" to 40,
        "🛹" to 35, "🧠" to 55, "🦋" to 45, "🍩" to 20,
        "🎃" to 15, "❄️" to 30, "🌸" to 30, "🥷" to 70,
        "🛡️" to 50, "🏆" to 500, "💯" to 35, "🍄" to 25
    ).map { (emoji, price) -> ShopItem(emoji, "Эмодзи $emoji", ItemType.EMOJI_STATUS, price) }
    fun getStyleColor(id: String): Color = when (id) {
        "neon" -> Color(0xFF39FF8A)
        "gold" -> Color(0xFFF5C842)
        "fire" -> Color(0xFFFF5E3A)
        "ice" -> Color(0xFF5EB8FF)
        "matrix" -> Color(0xFF00FF41)
        "sunset" -> Color(0xFFFF8C42)
        "forest" -> Color(0xFF3ECF70)
        "midnight" -> Color(0xFF6C63FF)
        "lavender" -> Color(0xFFB39DDB)
        "mint" -> Color(0xFF7BE0C4)
        "coral" -> Color(0xFFFF7A6E)
        "steel" -> Color(0xFF9AA5B1)
        "amber" -> Color(0xFFFFB300)
        "lime" -> Color(0xFFCDDC39)
        "sky" -> Color(0xFF74C0FC)
        "plum" -> Color(0xFF9C6ADE)
        "folli" -> Color(0xFFD32F2F)
        "dani" -> Color(0xFF39FF14)
        "sakura" -> Color(0xFFFF6FB5)
        "intel" -> Color(0xFF00AEEF)
        else -> Color.Gray
    }

    fun getStyleGradient(id: String): List<Color> = when (id) {
        "neon" -> listOf(Color(0xFF00C46A), Color(0xFF39FF8A))
        "gold" -> listOf(Color(0xFFB8860B), Color(0xFFF5C842))
        "fire" -> listOf(Color(0xFFB2340B), Color(0xFFFF5E3A))
        "ice" -> listOf(Color(0xFF1E6FA8), Color(0xFF5EB8FF))
        "matrix" -> listOf(Color(0xFF003300), Color(0xFF00FF41))
        "sunset" -> listOf(Color(0xFFB24A17), Color(0xFFFF8C42))
        "forest" -> listOf(Color(0xFF1E5C3A), Color(0xFF3ECF70))
        "midnight" -> listOf(Color(0xFF2A2668), Color(0xFF6C63FF))
        "lavender" -> listOf(Color(0xFF8E7CC3), Color(0xFFB39DDB))
        "mint" -> listOf(Color(0xFF4FB897), Color(0xFF7BE0C4))
        "coral" -> listOf(Color(0xFFCC5546), Color(0xFFFF7A6E))
        "steel" -> listOf(Color(0xFF616B76), Color(0xFF9AA5B1))
        "amber" -> listOf(Color(0xFFB37D00), Color(0xFFFFB300))
        "lime" -> listOf(Color(0xFF95A32A), Color(0xFFCDDC39))
        "sky" -> listOf(Color(0xFF3A8FD1), Color(0xFF74C0FC))
        "plum" -> listOf(Color(0xFF6B4499), Color(0xFF9C6ADE))
        "folly" -> listOf(Color(0xFFD32F2F), Color(0xFFFDEDEC))
        "dani" -> listOf(Color(0xFF050505), Color(0xFF1A1A1A))
        "sakura" -> listOf(Color(0xFFFF6FB5), Color(0xFFFFD1E8))
        "intel1" -> listOf(Color(0xFF003D82), Color(0xFF0071C5))
        else -> listOf(getStyleColor(id), getStyleColor(id))
    }

    fun getStyleTextColor(id: String): Color = when (id) {
        "dani" -> Color(0xFF39FF14)
        "intel" -> Color(0xFF00E5FF)
        "folly" -> Color(0xFF7A0000)
        "sakura" -> Color(0xFFFFFFFF)
        else -> Color.White
    }
    private fun dailySeed(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
    }

    data class DailyDeal(val item: ShopItem, val discountPercent: Int) {
        val discountedPrice: Int
            get() = (item.price * (100 - discountPercent) / 100).coerceAtLeast(1)
    }
    fun getDailyDeals(count: Int = 4): List<DailyDeal> {
        val pool = BUBBLE_STYLES + EMOJI_STATUSES
        val seed = dailySeed()
        val chosen = pool.shuffled(Random(seed)).take(count)
        return chosen.map { item ->
            val discount = listOf(10, 15, 20, 25, 30)
                .shuffled(Random(seed * 31 + item.id.hashCode()))
                .first()
            DailyDeal(item, discount)
        }
    }
    fun millisUntilNextDailyReset(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0)
    }
}