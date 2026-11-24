package com.example.pickupcode.ui.theme

import android.graphics.Path as AndroidPath
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Apple 连续圆角（iOS CALayerCornerCurve.continuous / G2 曲率平滑超椭圆）通用形状：
 * - 当圆角半径 r <= min(w, h) / 2 / 1.52866 时，严格采用 iOS 原生三段三次贝塞尔连续曲线；
 * - 当圆角趋近短边一半时平滑过渡至标准半圆胶囊（零自交、零折角），兼顾卡片、按钮与胶囊。
 */
@Immutable
class AppleContinuousCornerShape(
    val cornerRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        }
        val halfMin = minOf(w, h) * 0.5f
        val rPx = with(density) { cornerRadius.toPx() }.coerceAtLeast(0f)
        if (rPx <= 0.01f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        }
        if (rPx >= halfMin * 0.99f) {
            return Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = w,
                    bottom = h,
                    cornerRadius = CornerRadius(halfMin, halfMin)
                )
            )
        }
        val path = Path().apply {
            addAppleContinuousRectPath(w = w, h = h, cornerRadiusPx = rPx)
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is AppleContinuousCornerShape && cornerRadius == other.cornerRadius
    }

    override fun hashCode(): Int = cornerRadius.hashCode()
}

/**
 * 在 Compose [Path] 上绘制严格对齐 iOS 原生连续曲率（CALayerCornerCurve.continuous）的圆角矩形路径
 */
fun Path.addAppleContinuousRectPath(w: Float, h: Float, cornerRadiusPx: Float) {
    val halfMin = minOf(w, h) * 0.5f
    val r = cornerRadiusPx.coerceIn(0f, halfMin)
    if (r <= 0.01f) {
        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        return
    }

    // Apple 原生连续圆角延伸系数 p = 1.52866483 * r；当延伸超过短边一半时平滑向标准圆弧收敛
    val maxContinuousR = halfMin / 1.52866483f
    val smooth = if (r <= maxContinuousR) {
        1f
    } else {
        ((halfMin - r) / (halfMin - maxContinuousR)).coerceIn(0f, 1f)
    }

    fun lerp(a0: Float, a1: Float): Float = a0 + (a1 - a0) * smooth

    val p = (lerp(1.00000000f, 1.52866483f) * r).coerceAtMost(halfMin)
    val a = lerp(1.00000000f, 1.08849323f) * r
    val b = lerp(1.00000000f, 0.86840689f) * r
    val c = lerp(1.00000000f, 0.63149399f) * r
    val d = lerp(0.00000000f, 0.07491176f) * r
    val e = lerp(0.44771525f, 0.37282392f) * r
    val f = lerp(0.00000000f, 0.16906013f) * r

    moveTo(p, 0f)
    lineTo(w - p, 0f)

    // 右上角（3 段 G2 连续曲线）
    cubicTo(w - a, 0f, w - b, 0f, w - c, d)
    cubicTo(w - e, f, w - f, e, w - d, c)
    cubicTo(w, b, w, a, w, p)

    lineTo(w, h - p)

    // 右下角
    cubicTo(w, h - a, w, h - b, w - d, h - c)
    cubicTo(w - f, h - e, w - e, h - f, w - c, h - d)
    cubicTo(w - b, h, w - a, h, w - p, h)

    lineTo(p, h)

    // 左下角
    cubicTo(a, h, b, h, c, h - d)
    cubicTo(e, h - f, f, h - e, d, h - c)
    cubicTo(0f, h - b, 0f, h - a, 0f, h - p)

    lineTo(0f, p)

    // 左上角
    cubicTo(0f, a, 0f, b, d, c)
    cubicTo(f, e, e, f, c, d)
    cubicTo(b, 0f, a, 0f, p, 0f)

    close()
}

/**
 * 构建 Android 原生 Canvas 用的严格 Apple 连续圆角矩形 [AndroidPath]（桌面小组件图标与分享长图卡片共用）
 */
fun buildAndroidAppleContinuousRectPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadiusPx: Float
): AndroidPath {
    val w = (right - left).coerceAtLeast(0f)
    val h = (bottom - top).coerceAtLeast(0f)
    val path = AndroidPath()
    if (w <= 0f || h <= 0f) return path

    val halfMin = minOf(w, h) * 0.5f
    val r = cornerRadiusPx.coerceIn(0f, halfMin)
    if (r <= 0.01f) {
        path.addRect(left, top, right, bottom, AndroidPath.Direction.CW)
        return path
    }

    val maxContinuousR = halfMin / 1.52866483f
    val smooth = if (r <= maxContinuousR) {
        1f
    } else {
        ((halfMin - r) / (halfMin - maxContinuousR)).coerceIn(0f, 1f)
    }

    fun lerp(a0: Float, a1: Float): Float = a0 + (a1 - a0) * smooth

    val p = (lerp(1.00000000f, 1.52866483f) * r).coerceAtMost(halfMin)
    val a = lerp(1.00000000f, 1.08849323f) * r
    val b = lerp(1.00000000f, 0.86840689f) * r
    val c = lerp(1.00000000f, 0.63149399f) * r
    val d = lerp(0.00000000f, 0.07491176f) * r
    val e = lerp(0.44771525f, 0.37282392f) * r
    val f = lerp(0.00000000f, 0.16906013f) * r

    path.moveTo(left + p, top)
    path.lineTo(right - p, top)

    // 右上角
    path.cubicTo(right - a, top, right - b, top, right - c, top + d)
    path.cubicTo(right - e, top + f, right - f, top + e, right - d, top + c)
    path.cubicTo(right, top + b, right, top + a, right, top + p)

    path.lineTo(right, bottom - p)

    // 右下角
    path.cubicTo(right, bottom - a, right, bottom - b, right - d, bottom - c)
    path.cubicTo(right - f, bottom - e, right - e, bottom - f, right - c, bottom - d)
    path.cubicTo(right - b, bottom, right - a, bottom, right - p, bottom)

    path.lineTo(left + p, bottom)

    // 左下角
    path.cubicTo(left + a, bottom, left + b, bottom, left + c, bottom - d)
    path.cubicTo(left + e, bottom - f, left + f, bottom - e, left + d, bottom - c)
    path.cubicTo(left, bottom - b, left, bottom - a, left, bottom - p)

    path.lineTo(left, top + p)

    // 左上角
    path.cubicTo(left, top + a, left, top + b, left + d, top + c)
    path.cubicTo(left + f, top + e, left + e, top + f, left + c, top + d)
    path.cubicTo(left + b, top, left + a, top, left + p, top)

    path.close()
    return path
}

/**
 * Apple Concentric & Continuous Corners 圆角规范（全局统一单例缓存，零重组开销）
 */
object AppleCorners {
    val OuterCard = 24.dp
    val InnerCard = 16.dp
    val SmallElement = 12.dp
    val MicroElement = 8.dp
    val Pill = 999.dp

    val OuterCardShape: Shape = AppleContinuousCornerShape(OuterCard)
    val InnerCardShape: Shape = AppleContinuousCornerShape(InnerCard)
    val SmallElementShape: Shape = AppleContinuousCornerShape(SmallElement)
    val MicroElementShape: Shape = AppleContinuousCornerShape(MicroElement)
    val PillShape: Shape = RoundedCornerShape(Pill)

    fun shapeFor(cornerRadius: Dp): Shape = when (cornerRadius) {
        OuterCard -> OuterCardShape
        InnerCard -> InnerCardShape
        SmallElement -> SmallElementShape
        MicroElement -> MicroElementShape
        Pill -> PillShape
        else -> AppleContinuousCornerShape(cornerRadius)
    }
}

/**
 * 120Hz 高帧率优化版液态玻璃（Liquid Glass）修饰符
 *
 * 采用预缓存 Apple 连续圆角 Shape 与 Brush，移除滚动列表中的重度离屏阴影与实时模糊开销，确保 120 帧丝滑滚动。
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = AppleCorners.OuterCard,
    tintColor: Color = Color.Unspecified
): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = remember(cornerRadius) { AppleCorners.shapeFor(cornerRadius) }

    val surfaceBrush = remember(isDark, tintColor) {
        val topSurface = if (isDark) {
            Color(0xFF232734).copy(alpha = 0.86f)
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.88f)
        }

        val bottomSurface = if (isDark) {
            Color(0xFF181B24).copy(alpha = 0.80f)
        } else {
            Color(0xFFF6F9FF).copy(alpha = 0.82f)
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
        .drawWithCache {
            val w = size.width
            val h = size.height
            val rPx = cornerRadius.toPx().coerceIn(0f, minOf(w, h) * 0.5f)
            val strokePx = 0.85.dp.toPx()
            val halfStroke = strokePx * 0.5f
            val fillPath = Path().apply {
                addAppleContinuousRectPath(w = w, h = h, cornerRadiusPx = rPx)
            }
            val borderPath = Path().apply {
                addAppleContinuousRectPath(
                    w = (w - strokePx).coerceAtLeast(0f),
                    h = (h - strokePx).coerceAtLeast(0f),
                    cornerRadiusPx = (rPx - halfStroke).coerceAtLeast(0f)
                )
            }
            onDrawBehind {
                drawPath(path = fillPath, brush = surfaceBrush)
                translate(left = halfStroke, top = halfStroke) {
                    drawPath(
                        path = borderPath,
                        brush = borderBrush,
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }
        .clip(shape)
}

/**
 * 内部子卡片专用高帧率液态玻璃修饰符（16.dp 同心嵌套圆角）
 */
@Composable
fun Modifier.liquidGlassInnerCard(
    cornerRadius: Dp = AppleCorners.InnerCard,
    accentTint: Color = Color.Unspecified
): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = remember(cornerRadius) { AppleCorners.shapeFor(cornerRadius) }

    val surfaceBrush = remember(isDark, accentTint) {
        val topFill = if (isDark) {
            Color(0xFF2D3140).copy(alpha = 0.78f)
        } else {
            Color.White.copy(alpha = 0.92f)
        }
        val bottomFill = if (isDark) {
            Color(0xFF232633).copy(alpha = 0.72f)
        } else {
            Color(0xFFF8FAFC).copy(alpha = 0.85f)
        }

        val colors = if (accentTint != Color.Unspecified) {
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

    val borderColor = remember(isDark) {
        if (isDark) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color(0xFFE2E8F0).copy(alpha = 0.85f)
        }
    }

    return this
        .drawWithCache {
            val w = size.width
            val h = size.height
            val rPx = cornerRadius.toPx().coerceIn(0f, minOf(w, h) * 0.5f)
            val strokePx = 0.75.dp.toPx()
            val halfStroke = strokePx * 0.5f
            val fillPath = Path().apply {
                addAppleContinuousRectPath(w = w, h = h, cornerRadiusPx = rPx)
            }
            val borderPath = Path().apply {
                addAppleContinuousRectPath(
                    w = (w - strokePx).coerceAtLeast(0f),
                    h = (h - strokePx).coerceAtLeast(0f),
                    cornerRadiusPx = (rPx - halfStroke).coerceAtLeast(0f)
                )
            }
            onDrawBehind {
                drawPath(path = fillPath, brush = surfaceBrush)
                translate(left = halfStroke, top = halfStroke) {
                    drawPath(
                        path = borderPath,
                        color = borderColor,
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }
        .clip(shape)
}

/**
 * 位图/海报专用硬件抗锯齿 Apple 连续圆角裁剪修饰符（避免部分 GPU 上 clipPath 模板裁剪产生 1px 锯齿）
 */
fun Modifier.clipAppleContinuous(cornerRadius: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val w = size.width
        val h = size.height
        val rPx = cornerRadius.toPx().coerceIn(0f, minOf(w, h) * 0.5f)
        val inverseMask = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(-4f, -4f, w + 4f, h + 4f))
            addAppleContinuousRectPath(w = w, h = h, cornerRadiusPx = rPx)
        }
        onDrawWithContent {
            drawContent()
            drawPath(path = inverseMask, color = Color.Black, blendMode = BlendMode.Clear)
        }
    }

/**
 * 应用图标专用硬件抗锯齿 iOS App Icon Squircle（r = 0.2237 * min(w, h)）裁剪修饰符
 */
fun Modifier.clipAppleIconSquircle(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val w = size.width
        val h = size.height
        val rPx = minOf(w, h) * 0.2237f
        val inverseMask = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(-4f, -4f, w + 4f, h + 4f))
            addAppleContinuousRectPath(w = w, h = h, cornerRadiusPx = rPx)
        }
        onDrawWithContent {
            drawContent()
            drawPath(path = inverseMask, color = Color.Black, blendMode = BlendMode.Clear)
        }
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

    key(isDark) {
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
}
