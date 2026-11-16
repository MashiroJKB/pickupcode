package com.example.pickupcode.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Concentric & Continuous Corners 圆角规范
 */
object AppleCorners {
    val OuterCard = 24.dp
    val InnerCard = 16.dp
    val SmallElement = 12.dp
    val Pill = 999.dp
}

/**
 * 120Hz 高帧率优化版液态玻璃（Liquid Glass）修饰符
 *
 * 采用预缓存 Shape 与 Brush，移除滚动列表中的重度离屏阴影与实时模糊开销，确保 120 帧丝滑滚动。
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = AppleCorners.OuterCard,
    tintColor: Color = Color.Unspecified,
    surfaceAlphaMultiplier: Float = 1f,
    @Suppress("UNUSED_PARAMETER") shadowElevation: Dp = 0.dp
): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val surfaceBrush = remember(isDark, tintColor, surfaceAlphaMultiplier) {
        val topSurface = if (isDark) {
            Color(0xFF232734).copy(alpha = (0.86f * surfaceAlphaMultiplier).coerceIn(0f, 0.96f))
        } else {
            Color(0xFFFFFFFF).copy(alpha = (0.88f * surfaceAlphaMultiplier).coerceIn(0f, 0.96f))
        }

        val bottomSurface = if (isDark) {
            Color(0xFF181B24).copy(alpha = (0.80f * surfaceAlphaMultiplier).coerceIn(0f, 0.94f))
        } else {
            Color(0xFFF6F9FF).copy(alpha = (0.82f * surfaceAlphaMultiplier).coerceIn(0f, 0.94f))
        }

        val colors = if (tintColor != Color.Unspecified) {
            listOf(
                tintColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                topSurface,
                bottomSurface
            )
        } else {
            listOf(topSurface, bottomSurface)
        }
        Brush.verticalGradient(colors = colors)
    }

    val borderBrush = remember(isDark) {
        val topSpecularBorder = if (isDark) {
            Color.White.copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.92f)
        }
        val bottomRefractionBorder = if (isDark) {
            Color.White.copy(alpha = 0.06f)
        } else {
            Color(0xFFCBD5E1).copy(alpha = 0.42f)
        }
        Brush.verticalGradient(listOf(topSpecularBorder, bottomRefractionBorder))
    }

    return this
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 0.85.dp, brush = borderBrush, shape = shape)
}

/**
 * 内部子卡片专用高帧率液态玻璃修饰符（16.dp 同心嵌套圆角）
 */
@Composable
fun Modifier.liquidGlassInnerCard(
    cornerRadius: Dp = AppleCorners.InnerCard,
    isPickedUp: Boolean = false,
    accentTint: Color = Color.Unspecified
): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val surfaceBrush = remember(isDark, isPickedUp, accentTint) {
        val fillAlpha = if (isPickedUp) 0.42f else 0.78f
        val topFill = if (isDark) {
            Color(0xFF2D3140).copy(alpha = fillAlpha)
        } else {
            Color.White.copy(alpha = if (isPickedUp) 0.55f else 0.92f)
        }
        val bottomFill = if (isDark) {
            Color(0xFF232633).copy(alpha = fillAlpha * 0.92f)
        } else {
            Color(0xFFF8FAFC).copy(alpha = if (isPickedUp) 0.48f else 0.85f)
        }

        val colors = if (accentTint != Color.Unspecified && !isPickedUp) {
            listOf(
                accentTint.copy(alpha = if (isDark) 0.14f else 0.08f),
                topFill,
                bottomFill
            )
        } else {
            listOf(topFill, bottomFill)
        }
        Brush.horizontalGradient(colors = colors)
    }

    val borderColor = remember(isDark, isPickedUp) {
        if (isDark) {
            Color.White.copy(alpha = if (isPickedUp) 0.06f else 0.14f)
        } else {
            Color(0xFFE2E8F0).copy(alpha = if (isPickedUp) 0.45f else 0.85f)
        }
    }

    return this
        .clip(shape)
        .background(brush = surfaceBrush, shape = shape)
        .border(width = 0.75.dp, color = borderColor, shape = shape)
}

/**
 * 全局液态光影背景容器（使用 drawWithCache 缓存径向渐变 Brush，零帧率损耗，满帧 120Hz）
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) Color(0xFF0B0D13) else Color(0xFFEEF2F9)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawWithCache {
                val w = size.width
                val h = size.height

                val c1 = Offset(w * 0.18f, h * 0.10f)
                val r1 = w * 0.85f
                val b1 = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x300A84FF) else Color(0x28007AFF),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = r1
                )

                val c2 = Offset(w * 0.88f, h * 0.08f)
                val r2 = w * 0.70f
                val b2 = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x26FF6A00) else Color(0x20FF7A22),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = r2
                )

                val c3 = Offset(w * 0.75f, h * 0.55f)
                val r3 = w * 0.90f
                val b3 = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x225E5CE6) else Color(0x1E30B0C7),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = r3
                )

                val c4 = Offset(w * 0.25f, h * 0.88f)
                val r4 = w * 0.80f
                val b4 = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x2030D158) else Color(0x1C34C759),
                        Color.Transparent
                    ),
                    center = c4,
                    radius = r4
                )

                onDrawBehind {
                    drawCircle(brush = b1, center = c1, radius = r1)
                    drawCircle(brush = b2, center = c2, radius = r2)
                    drawCircle(brush = b3, center = c3, radius = r3)
                    drawCircle(brush = b4, center = c4, radius = r4)
                }
            }
    ) {
        content()
    }
}
