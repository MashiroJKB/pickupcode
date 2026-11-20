package com.example.pickupcode.ui.theme

import androidx.compose.ui.graphics.Color

// Apple HIG System Palette (Light & Dark Adaptive)
val AppleBlueLight = Color(0xFF007AFF)
val AppleBlueDark = Color(0xFF0A84FF)

val AppleGreenLight = Color(0xFF34C759)
val AppleGreenDark = Color(0xFF30D158)

val AppleOrangeLight = Color(0xFFFF9500)
val AppleOrangeDark = Color(0xFFFF9F0A)

val AppleRedLight = Color(0xFFFF3B30)
val AppleRedDark = Color(0xFFFF453A)

// 淘宝身份码品牌渐变强调色
val TaobaoOrangeStart = Color(0xFFFF6A00)
val TaobaoOrangeEnd = Color(0xFFFF2E00)

// 快递房专属高辨识度 Apple 色彩（支持任意编号循环映射）
fun stationAccentColor(stationNumber: Int, isDark: Boolean): Color {
    if (stationNumber <= 0) {
        return if (isDark) Color(0xFFFFD60A) else Color(0xFFD97706) // ？ 待分类琥珀黄
    }
    return when ((stationNumber - 1) % 7 + 1) {
        1 -> if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF) // 科技蓝
        2 -> if (isDark) Color(0xFF30D158) else Color(0xFF28B446) // 翡翠绿
        3 -> if (isDark) Color(0xFFFF9F0A) else Color(0xFFFF8800) // 活力橙
        4 -> if (isDark) Color(0xFF5E5CE6) else Color(0xFF5856D6) // 靛青紫
        5 -> if (isDark) Color(0xFF40C8E0) else Color(0xFF0099B8) // 冰川青
        6 -> if (isDark) Color(0xFFBF5AF2) else Color(0xFF9B38D0) // 霓虹紫
        else -> if (isDark) Color(0xFFFF375F) else Color(0xFFE8254D) // 珊瑚红
    }
}
