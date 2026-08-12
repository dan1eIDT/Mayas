package com.dan1eidtj.mayas.core_ui.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.dan1eidtj.mayas.core.ui.theme.MayasTheme.GlowGreen
import com.dan1eidtj.mayas.ui.R

data class CornerDecor(
    @DrawableRes val topStart: Int? = null,
    @DrawableRes val topEnd: Int? = null,
    @DrawableRes val bottomStart: Int? = null,
    @DrawableRes val bottomEnd: Int? = null,
    val sizeDp: Int = 40
)

data class FrameSpec(
    @DrawableRes val drawableRes: Int,
    val insets: NinePatchInsets,
    val contentPaddingPx: Int,
    val textColor: Color = Color.White,
    val cornerDecor: CornerDecor? = null
)

object FrameStyles {
    val registry: Map<String, FrameSpec> = mapOf(
        "matrix" to FrameSpec(
            drawableRes = R.drawable.frame_1010,
            insets = NinePatchInsets(32, 32, 32, 32),
            contentPaddingPx = 17,
            textColor = GlowGreen,
            cornerDecor = CornerDecor(
                topStart = R.drawable.frame_1010_corner_tl,
                topEnd = R.drawable.frame_1010_corner_tr,
                bottomStart = R.drawable.frame_1010_corner_bl,
                bottomEnd = R.drawable.frame_1010_corner_br,
                sizeDp = 45
            )
        ),

        "dani" to FrameSpec(
            drawableRes = R.drawable.frame_dan1,
            insets = NinePatchInsets(32, 32, 32, 32),
            contentPaddingPx = 17,
            textColor = Color(0xFFFFF3C4),
                    cornerDecor = CornerDecor(
                    topStart = R.drawable.frame_dan1_corner_tl,
                        sizeDp = 15
                        )
        )

         //    "dani" to FrameSpec(
        //     drawableRes = R.drawable.frame_gold,
        //     insets = NinePatchInsets(28, 28, 28, 28),
        //     contentPaddingPx = 14,
        //     textColor = Color(0xFFFFF3C4)
        // )
    )

    fun isFrameStyle(messageStyle: String?): Boolean =
        messageStyle != null && registry.containsKey(messageStyle)
}