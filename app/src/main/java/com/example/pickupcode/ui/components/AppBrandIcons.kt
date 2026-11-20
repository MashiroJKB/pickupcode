package com.example.pickupcode.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pickupcode.R

/**
 * 统一的 Apple 连续圆角（iOS App Icon Squircle 曲线）形状：
 * 圆角半径约占边长的 22.37%，并在直线与圆弧过渡处采用三次贝塞尔平滑曲率，与 iPhone 桌面图标完全一致。
 */
val AppleIconSquircleShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = minOf(w, h) * 0.2237f
    val c = r * 1.28f // iOS continuous corner curve offset
    val k = r * 0.44f // Bezier handle factor for smooth G2 transition

    this.addAppleSquirclePath(w, h, c, k)
}

private fun Path.addAppleSquirclePath(w: Float, h: Float, c: Float, k: Float) {
    moveTo(c, 0f)
    lineTo(w - c, 0f)
    cubicTo(w - k, 0f, w, k, w, c)
    lineTo(w, h - c)
    cubicTo(w, h - k, w - k, h, w - c, h)
    lineTo(c, h)
    cubicTo(k, h, 0f, h - k, 0f, h - c)
    lineTo(0f, c)
    cubicTo(0f, k, k, 0f, c, 0f)
    close()
}

private fun buildAndroidAppleSquirclePath(sizePx: Float): AndroidPath {
    val r = sizePx * 0.2237f
    val c = r * 1.28f
    val k = r * 0.44f
    return AndroidPath().apply {
        moveTo(c, 0f)
        lineTo(sizePx - c, 0f)
        cubicTo(sizePx - k, 0f, sizePx, k, sizePx, c)
        lineTo(sizePx, sizePx - c)
        cubicTo(sizePx, sizePx - k, sizePx - k, sizePx, sizePx - c, sizePx)
        lineTo(c, sizePx)
        cubicTo(k, sizePx, 0f, sizePx - k, 0f, sizePx - c)
        lineTo(0f, c)
        cubicTo(0f, k, k, 0f, c, 0f)
        close()
    }
}

private fun brandDrawableRes(type: IdentityCodeType): Int = when (type) {
    IdentityCodeType.TAOBAO -> R.drawable.ic_brand_taobao
    IdentityCodeType.CAINIAO -> R.drawable.ic_brand_cainiao
    IdentityCodeType.PINDUODUO -> R.drawable.ic_brand_pinduoduo
}

/**
 * 淘宝 / 菜鸟 / 拼多多 原生应用图标组件：
 * - 加载用户指定的高清品牌图标资源（ic_brand_taobao / ic_brand_cainiao / ic_brand_pinduoduo）
 * - 统一使用 Apple 连续圆角（AppleIconSquircleShape）裁剪，无任何多余外圈或系统主题边框
 */
@Composable
fun AppBrandIcon(
    type: IdentityCodeType,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = brandDrawableRes(type)),
        contentDescription = type.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(AppleIconSquircleShape)
    )
}

/**
 * 为桌面液态玻璃小组件生成统一 Apple 连续圆角裁剪的高清应用图标 Bitmap（支持按目标尺寸降采样以控制内存）
 */
fun loadAppleRoundedIconBitmapForWidget(
    context: Context,
    type: IdentityCodeType,
    sizePx: Int = 128
): Bitmap {
    val drawableRes = brandDrawableRes(type)
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    canvas.clipPath(buildAndroidAppleSquirclePath(sizePx.toFloat()))

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, drawableRes, boundsOpts)
    val decoded = if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
        var sampleSize = 1
        while (boundsOpts.outWidth / (sampleSize * 2) >= sizePx &&
            boundsOpts.outHeight / (sampleSize * 2) >= sizePx
        ) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeResource(context.resources, drawableRes, decodeOpts)
    } else {
        null
    }

    if (decoded != null) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(decoded, null, RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), paint)
        decoded.recycle()
    } else {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
        if (drawable != null) {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.GRAY
            }
            canvas.drawRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), fallbackPaint)
        }
    }
    return output
}
