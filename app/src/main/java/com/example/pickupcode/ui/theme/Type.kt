package com.example.pickupcode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 强光可读性排版规范：
 * 取件码数字采用等宽 + 特粗字重 + 微字距，在室外阳光下看手机一眼识别。
 */
val PickupCodeMonospaceStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 21.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.6.sp
)

/**
 * 全局分层排版规范（严格统一字体、字号、行高与字重层级）：
 * - headlineLarge : 页面主标题（「取件码」「设置」）28sp Bold
 * - titleLarge    : 弹窗主标题（「历史包裹」「支持」等）19sp Bold
 * - titleMedium   : 一级卡片大标题（主页与设置页全部区块标题）17sp Bold
 * - titleSmall    : 二级列表项标题（快递房规则、身份码开关、更新渠道等）15sp SemiBold
 * - bodyLarge     : 常规正文（短信全文、输入框等）15sp Normal
 * - bodyMedium    : 卡片副标题与辅助说明 13sp Normal
 * - labelMedium   : 胶囊按钮、次级标签、时间戳 12sp Medium
 * - labelSmall    : 紧凑徽章文字（版本号、密码标签、状态标识）11sp SemiBold
 */
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
