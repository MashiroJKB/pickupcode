package com.example.pickupcode.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.TaobaoOrangeEnd
import com.example.pickupcode.ui.theme.TaobaoOrangeStart
import com.example.pickupcode.ui.theme.liquidGlass

object TaobaoDeepLinkLauncher {
    const val TAOBAO_PACKAGE = "com.taobao.taobao"

    // 菜鸟出库码（原主链路）
    const val CAINIAO_EXIT_CODE_DEEPLINK =
        "tbopen://m.taobao.com/tbopen/index.html?h5Url=https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html"

    // 淘宝出库码（原备用链路，现为默认链路）
    const val TAOBAO_EXIT_CODE_DEEPLINK =
        "taobao://m.taobao.com/tbopen/index.html?h5Url=https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code"

    /**
     * 唤起本地淘宝/菜鸟出库码页：
     * 默认使用「淘宝出库码」，若异常则自动降级尝试「菜鸟出库码」；未安装淘宝则弹出轻提示。
     */
    fun launchIdentityCode(
        context: Context,
        useTaobaoExitCode: Boolean,
        onFeedbackMessage: (String) -> Unit
    ) {
        val firstUrl = if (useTaobaoExitCode) TAOBAO_EXIT_CODE_DEEPLINK else CAINIAO_EXIT_CODE_DEEPLINK
        val secondUrl = if (useTaobaoExitCode) CAINIAO_EXIT_CODE_DEEPLINK else TAOBAO_EXIT_CODE_DEEPLINK

        if (tryStartTaobaoUri(context, firstUrl)) {
            return
        }
        if (tryStartTaobaoUri(context, secondUrl)) {
            return
        }
        onFeedbackMessage("未检测到淘宝客户端，请先安装手机淘宝后再试")
    }

    private fun tryStartTaobaoUri(context: Context, uriString: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage(TAOBAO_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * 顶部「淘宝身份码 / 扫码出库」液态玻璃快捷 Banner
 */
@Composable
fun TaobaoIdentityBanner(
    useTaobaoExitCode: Boolean,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "banner_press_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .liquidGlass(
                cornerRadius = AppleCorners.OuterCard,
                tintColor = TaobaoOrangeStart,
                shadowElevation = 12.dp
            )
            .pointerInput(useTaobaoExitCode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        TaobaoDeepLinkLauncher.launchIdentityCode(
                            context = context,
                            useTaobaoExitCode = useTaobaoExitCode,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
            }
            .padding(horizontal = 18.dp, vertical = 15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(AppleCorners.InnerCard))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TaobaoOrangeStart, TaobaoOrangeEnd)
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "淘宝身份码",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "淘宝身份码",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TaobaoOrangeStart.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "扫码出库",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = TaobaoOrangeStart
                            )
                        }
                    }

                    Text(
                        text = "一键直达驿站身份码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleCorners.SmallElement))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(TaobaoOrangeStart, TaobaoOrangeEnd)
                        )
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        TaobaoDeepLinkLauncher.launchIdentityCode(
                            context = context,
                            useTaobaoExitCode = useTaobaoExitCode,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "打开",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
