package com.dan1eidtj.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
        ShopItem("folly", "Фолли", ItemType.BUBBLE, 280, description = "Дочя блять!"),
        ShopItem("dani", "Дани", ItemType.BUBBLE, 320, description = "Что он тут забыл?"),
        ShopItem("sakura", "Сакура", ItemType.BUBBLE, 240, description = "Розовый закат"),
        ShopItem("intel", "Intel", ItemType.BUBBLE, 260, description = "Синий с голубым текстом"),
        ShopItem("aurora", "Аврора", ItemType.BUBBLE, 340, description = "Северное сияние", isNew = true),
        ShopItem("void", "Пустота", ItemType.BUBBLE, 380, description = "Чёрная дыра с фиолетовым свечением", isNew = true),
        ShopItem("hanami", "Ханами", ItemType.BUBBLE, 360, description = "Зелёное свечение сквозь розовую дымку", isNew = true),
        ShopItem("rosevoid", "Розовая бездна", ItemType.BUBBLE, 380, description = "Чёрная дыра с розовым свечением", isNew = true),
        ShopItem("paper", "Чистый лист", ItemType.BUBBLE, 90, description = "Белый фон, чёрный текст"),
        ShopItem("bloodmoon", "Кровавая луна", ItemType.BUBBLE, 260, description = "Чёрный фон, кроваво-красный текст"),
        ShopItem("obsidian", "Обсидиан", ItemType.BUBBLE, 300, description = "Чёрный с золотым текстом"),
        ShopItem("onyx", "Оникс", ItemType.BUBBLE, 200, description = "Чёрный с белоснежным текстом"),
        ShopItem("shadow", "Тень", ItemType.BUBBLE, 280, description = "Чёрный с фиолетовым текстом"),
        ShopItem("abyss", "Пучина", ItemType.BUBBLE, 280, description = "Чёрный с ледяным голубым текстом"),
    )

    val EMOJI_STATUSES = listOf(

        "🍩" to 15, "🍉" to 15, "🎃" to 15,
        "👻" to 20, "💀" to 20, "🍄" to 20,
        "⭐" to 25, "🍀" to 25, "🌸" to 25,
        "🐱" to 30, "❄️" to 30, "🌊" to 30,

        "⚡" to 40, "🔥" to 40,
        "🎯" to 45, "🎧" to 45, "🛹" to 45,
        "🌈" to 50, "🦋" to 50,
        "🎮" to 55, "🧠" to 60,
        "🧿" to 65, "🛡️" to 75,  "🥥" to 50, "🤍" to 50,


        "🛸" to 100, "🚀" to 120,
        "🥷" to 150, "💎" to 2000,
        "💯" to 100,


        "🦄" to 350, "🐉" to 500,
        "🏆" to 800,

        "👑" to 5000,
        "✨" to 7500,
        "💸" to 10000
    ).map { (emoji, price) -> ShopItem(emoji, "Эмодзи $emoji", ItemType.EMOJI_STATUS, price) }


    val ICON_STATUSES = listOf(
        ShopItem("icon:status_folly", "Фолли", ItemType.EMOJI_STATUS, 120, isNew = true)
    )

    private const val ICON_STATUS_PREFIX = "icon:"

    fun isIconStatus(id: String): Boolean = id.startsWith(ICON_STATUS_PREFIX)

    fun iconStatusResourceName(id: String): String = id.removePrefix(ICON_STATUS_PREFIX)

    val ALL_STATUSES: List<ShopItem> get() = EMOJI_STATUSES + ICON_STATUSES


    val FONT_STYLES = listOf(
        ShopItem("default", "Стандартный", ItemType.FONT, 0),
        ShopItem("sans", "Геометрический", ItemType.FONT, 120),
        ShopItem("serif", "Классический", ItemType.FONT, 140),
        ShopItem("mono", "Кодовый", ItemType.FONT, 160, description = "Моноширинный, как в терминале"),
        ShopItem("cursive", "Рукописный", ItemType.FONT, 180, isNew = true)
    )

    val EFFECT_STYLES = listOf(
        ShopItem("none", "Без эффекта", ItemType.EFFECT, 0),
        ShopItem("confetti", "Конфетти", ItemType.EFFECT, 100, icon = "🎉"),
        ShopItem("hearts", "Сердечки", ItemType.EFFECT, 90, icon = "💕"),
        ShopItem("snow", "Снегопад", ItemType.EFFECT, 110, icon = "❄️"),
        ShopItem("fireworks", "Салют", ItemType.EFFECT, 150, icon = "🎆", isNew = true),
        ShopItem("sparkles", "Блёстки", ItemType.EFFECT, 80, icon = "✨")
    )

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
        "folly" -> Color(0xFFD32F2F)
        "dani" -> Color(0xFF39FF14)
        "sakura" -> Color(0xFFFF6FB5)
        "intel" -> Color(0xFF00AEEF)
        "aurora" -> Color(0xFF35E0C8)
        "void" -> Color(0xFF9B5CFF)
        "hanami" -> Color(0xFF39FF6A)
        "rosevoid" -> Color(0xFFFF4D9E)
        "paper" -> Color(0xFF1C1C1E)
        "bloodmoon" -> Color(0xFFFF3B30)
        "obsidian" -> Color(0xFFFFD700)
        "onyx" -> Color(0xFFE8E8E8)
        "shadow" -> Color(0xFF9B6DFF)
        "abyss" -> Color(0xFF5AC8FA)
        "custom_frame" -> Color(0xFF39FF6A)
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
        "intel" -> listOf(Color(0xFF003D82), Color(0xFF0071C5))
        "aurora" -> listOf(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF35E0C8))
        "void" -> listOf(Color(0xFF0A0014), Color(0xFF3D0A5C), Color(0xFF9B5CFF))
        "hanami" -> listOf(Color(0xFF06140C), Color(0xFF1F8C55), Color(0xFFFFB6D9))
        "rosevoid" -> listOf(Color(0xFF0A0007), Color(0xFF5C0A3D), Color(0xFFFF4D9E))
        "paper" -> listOf(Color(0xFFFFFFFF), Color(0xFFF2F2F2))
        "bloodmoon" -> listOf(Color(0xFF0A0A0A), Color(0xFF1A0505))
        "obsidian" -> listOf(Color(0xFF0A0A0A), Color(0xFF1A1608))
        "onyx" -> listOf(Color(0xFF0A0A0A), Color(0xFF161616))
        "shadow" -> listOf(Color(0xFF0A0A0A), Color(0xFF150A1F))
        "abyss" -> listOf(Color(0xFF0A0A0A), Color(0xFF081620))
        "custom_frame" -> listOf(Color(0xFF000000), Color(0xFF39FF6A))
        else -> listOf(getStyleColor(id), getStyleColor(id))
    }

    fun getStyleTextColor(id: String): Color = when (id) {
        "dani" -> Color(0xFF39FF14)
        "intel" -> Color(0xFF00E5FF)
        "folly" -> Color(0xFF7A0000)
        "sakura" -> Color(0xFFFFFFFF)

        "lavender" -> Color(0xFF3D2C5C)
        "mint" -> Color(0xFF0F5C48)
        "coral" -> Color(0xFF7A2E22)
        "steel" -> Color(0xFF2B2F36)
        "amber" -> Color(0xFF5C3D00)
        "lime" -> Color(0xFF4B5314)
        "sky" -> Color(0xFF0D3A66)
        "plum" -> Color(0xFF3D1F5C)

        "hanami" -> Color(0xFFFFFFFF)
        "rosevoid" -> Color(0xFFFFFFFF)
        "paper" -> Color(0xFF0A0A0A)
        "bloodmoon" -> Color(0xFFFF3B30)
        "obsidian" -> Color(0xFFFFD700)
        "onyx" -> Color(0xFFFFFFFF)
        "shadow" -> Color(0xFFB794FF)
        "abyss" -> Color(0xFF5AC8FA)
        else -> Color.White
    }


    fun getWallpaperGradient(id: String): List<Color> = when (id) {
        "dusk" -> listOf(Color(0xFF2C3E70), Color(0xFF8759A8), Color(0xFFE8836E))
        "ocean" -> listOf(Color(0xFF003F5C), Color(0xFF2F9E8F), Color(0xFF7FE0C4))
        "candy" -> listOf(Color(0xFFFF6FB5), Color(0xFFFFA6D9), Color(0xFFFFD9EC))
        "carbon" -> listOf(Color(0xFF0D0D0D), Color(0xFF2B2B2B), Color(0xFF3F3F3F))
        "peach" -> listOf(Color(0xFFFFB199), Color(0xFFFFD9B0), Color(0xFFFFF0DC))
        "emerald" -> listOf(Color(0xFF014D40), Color(0xFF0E8A6D), Color(0xFF4EE0A8))
        "galaxy" -> listOf(Color(0xFF03001C), Color(0xFF301E67), Color(0xFF5B8FB9))
        "lava" -> listOf(Color(0xFF1A0000), Color(0xFFB3200A), Color(0xFFFF7A18))
        else -> listOf(Color(0xFF3A3A3A), Color(0xFF1F1F1F))
    }


    fun getFontFamily(id: String): FontFamily = when (id) {
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }


    fun getEffectPreview(id: String): String = EFFECT_STYLES.find { it.id == id }?.icon ?: "✨"


    fun rarityOf(item: ShopItem): ItemRarity = when {
        item.price <= 0 -> ItemRarity.COMMON
        item.price < 100 -> ItemRarity.COMMON
        item.price < 220 -> ItemRarity.RARE
        item.price < 400 -> ItemRarity.EPIC
        else -> ItemRarity.LEGENDARY
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

        val pool = (BUBBLE_STYLES + EMOJI_STATUSES + ICON_STATUSES + FONT_STYLES + EFFECT_STYLES)
            .filter { it.price > 0 }
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