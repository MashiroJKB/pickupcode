package com.example.pickupcode.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 淘宝 / 菜鸟 / 拼多多 应用原生图标组件：
 * - 保持应用原生图标外观，不添加任何多余外圈或渐变衬底
 * - 统一按 Apple iOS 图标圆角比例（22.5%）裁剪
 */
@Composable
fun AppBrandIcon(
    type: IdentityCodeType,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap = remember(type) {
        loadPureAppIconBitmap(context, type, 192).asImageBitmap()
    }

    Image(
        bitmap = iconBitmap,
        contentDescription = type.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 22))
    )
}

/**
 * 为桌面小组件生成裁切好苹果圆角的原生应用图标 Bitmap
 */
fun loadAppleRoundedIconBitmapForWidget(
    context: Context,
    type: IdentityCodeType,
    sizePx: Int = 96
): Bitmap {
    val raw = loadPureAppIconBitmap(context, type, sizePx)
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    val radius = sizePx * 0.2237f
    val path = Path().apply {
        addRoundRect(
            RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
            radius,
            radius,
            Path.Direction.CW
        )
    }
    canvas.clipPath(path)
    canvas.drawBitmap(raw, null, RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return output
}

private fun loadPureAppIconBitmap(
    context: Context,
    type: IdentityCodeType,
    targetSizePx: Int
): Bitmap {
    val packageName = when (type) {
        IdentityCodeType.TAOBAO -> TaobaoDeepLinkLauncher.TAOBAO_PACKAGE
        IdentityCodeType.CAINIAO -> "com.cainiao.wireless"
        IdentityCodeType.PINDUODUO -> TaobaoDeepLinkLauncher.PINDUODUO_PACKAGE
    }

    return extractRawAppIconBitmap(context, packageName, targetSizePx)
        ?: createBrandFallbackBitmap(type, targetSizePx)
}

/**
 * 优先直接读取目标安装包原生未加系统外框的资源图标（AdaptiveIconDrawable 取原始 background + foreground 层，
 * 避免 MIUI/HyperOS 等系统图标包在外圈添加透明留白与异形蒙版）；普通 Drawable 则自动裁去透明外边距。
 */
private fun extractRawAppIconBitmap(
    context: Context,
    packageName: String,
    targetSizePx: Int
): Bitmap? {
    val pm = context.packageManager

    // 1. 尝试直接从目标 App 资源读取原始 AdaptiveIconDrawable / mipmap
    val rawResourceDrawable: Drawable? = try {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        if (appInfo.icon != 0) {
            pm.getResourcesForApplication(appInfo).getDrawable(appInfo.icon, null)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    val drawable = rawResourceDrawable ?: try {
        pm.getApplicationIcon(packageName)
    } catch (_: Exception) {
        return null
    }

    if (drawable is AdaptiveIconDrawable) {
        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        // AdaptiveIconDrawable 全画布为 108dp，安全区为 72dp，裁去外围 13% 溢出边缘以全幅填充正方形
        val bleed = (targetSizePx * 0.14f).roundToInt()
        drawable.background?.let { bg ->
            bg.setBounds(-bleed, -bleed, targetSizePx + bleed, targetSizePx + bleed)
            bg.draw(canvas)
        }
        drawable.foreground?.let { fg ->
            fg.setBounds(-bleed, -bleed, targetSizePx + bleed, targetSizePx + bleed)
            fg.draw(canvas)
        }
        return bitmap
    }

    // 2. 普通 Drawable：先绘制后再紧致裁切掉四周透明空白
    val renderSize = targetSizePx.coerceAtLeast(256)
    val rawBitmap = Bitmap.createBitmap(renderSize, renderSize, Bitmap.Config.ARGB_8888)
    val rawCanvas = AndroidCanvas(rawBitmap)
    drawable.setBounds(0, 0, renderSize, renderSize)
    drawable.draw(rawCanvas)

    return trimTransparentMargins(rawBitmap, targetSizePx)
}

private fun trimTransparentMargins(source: Bitmap, targetSizePx: Int): Bitmap {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1

    for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > 20) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX <= minX || maxY <= minY) {
        return Bitmap.createScaledBitmap(source, targetSizePx, targetSizePx, true)
    }

    val cropW = maxX - minX + 1
    val cropH = maxY - minY + 1
    val cropped = Bitmap.createBitmap(source, minX, minY, cropW, cropH)
    return Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true)
}

private fun createBrandFallbackBitmap(type: IdentityCodeType, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (type) {
            IdentityCodeType.TAOBAO -> android.graphics.Color.parseColor("#FF5500")
            IdentityCodeType.CAINIAO -> android.graphics.Color.parseColor("#0066FF")
            IdentityCodeType.PINDUODUO -> android.graphics.Color.parseColor("#E02E24")
        }
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val glyph = when (type) {
        IdentityCodeType.TAOBAO -> "淘"
        IdentityCodeType.CAINIAO -> "菜鸟"
        IdentityCodeType.PINDUODUO -> "拼"
    }
    textPaint.textSize = when (type) {
        IdentityCodeType.CAINIAO -> sizePx * 0.375f
        else -> sizePx * 0.54f
    }
    val fontMetrics = textPaint.fontMetrics
    val baseline = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(glyph, sizePx / 2f, baseline, textPaint)

    return bitmap
}
