package com.example.pickupcode.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pickupcode.ui.theme.AppleCorners

/**
 * 淘宝 / 菜鸟 / 拼多多 应用品牌图标组件：
 * 优先加载手机本机已安装的官方 App 图标；若未安装对应独立客户端（如仅安装淘宝未装菜鸟），则渲染高辨识度原生品牌矢量图标。
 */
@Composable
fun AppBrandIcon(
    type: IdentityCodeType,
    size: Dp = 46.dp,
    cornerRadius: Dp = AppleCorners.InnerCard,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedBitmap = remember(type) {
        val pkg = when (type) {
            IdentityCodeType.TAOBAO -> "com.taobao.taobao"
            IdentityCodeType.CAINIAO -> "com.cainiao.wireless"
            IdentityCodeType.PINDUODUO -> "com.xunmeng.pinduoduo"
        }
        loadInstalledAppIconBitmap(context, pkg)
    }

    val fallbackBitmap = remember(type) {
        createBrandFallbackBitmap(type)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(type.primaryColor, type.secondaryColor)
                )
            )
    ) {
        Image(
            bitmap = installedBitmap ?: fallbackBitmap,
            contentDescription = type.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun loadInstalledAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val width = drawable.intrinsicWidth.coerceAtLeast(144)
        val height = drawable.intrinsicHeight.coerceAtLeast(144)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun createBrandFallbackBitmap(type: IdentityCodeType): ImageBitmap {
    val sizePx = 192
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

    when (type) {
        IdentityCodeType.TAOBAO -> {
            textPaint.textSize = 104f
            val fontMetrics = textPaint.fontMetrics
            val baseline = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText("淘", sizePx / 2f, baseline, textPaint)
        }

        IdentityCodeType.CAINIAO -> {
            textPaint.textSize = 72f
            val fontMetrics = textPaint.fontMetrics
            val baseline = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText("菜鸟", sizePx / 2f, baseline, textPaint)
        }

        IdentityCodeType.PINDUODUO -> {
            textPaint.textSize = 104f
            val fontMetrics = textPaint.fontMetrics
            val baseline = sizePx / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText("拼", sizePx / 2f, baseline, textPaint)
        }
    }

    return bitmap.asImageBitmap()
}
