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

val CainiaoBlueStart = Color(0xFF0A84FF)
val CainiaoBlueEnd = Color(0xFF0055D4)

// 拼多多品牌图标红
val PinduoduoRedStart = Color(0xFFE02E24)
val PinduoduoRedEnd = Color(0xFFC8161D)

enum class IdentityCodeType(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    TAOBAO(
        title = "淘宝身份码",
        subtitle = "一键直达淘宝驿站身份码",
        primaryColor = TaobaoOrangeStart,
        secondaryColor = TaobaoOrangeEnd
    ),
    CAINIAO(
        title = "菜鸟身份码",
        subtitle = "一键直达菜鸟驿站身份码",
        primaryColor = CainiaoBlueStart,
        secondaryColor = CainiaoBlueEnd
    ),
    PINDUODUO(
        title = "拼多多身份码",
        subtitle = "一键直达拼多多驿站身份码",
        primaryColor = PinduoduoRedStart,
        secondaryColor = PinduoduoRedEnd
    )
}

object TaobaoDeepLinkLauncher {
    const val TAOBAO_PACKAGE = "com.taobao.taobao"
    const val PINDUODUO_PACKAGE = "com.xunmeng.pinduoduo"

    // 菜鸟身份码 DeepLink
    const val CAINIAO_IDENTITY_DEEPLINK =
        "tbopen://m.taobao.com/tbopen/index.html?h5Url=https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html"

    // 淘宝身份码 DeepLink
    const val TAOBAO_IDENTITY_DEEPLINK =
        "taobao://m.taobao.com/tbopen/index.html?h5Url=https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code"

    // 拼多多身份码 DeepLink
    const val PINDUODUO_IDENTITY_DEEPLINK =
        "pinduoduo://com.xunmeng.pinduoduo/mdkd/package"

    fun launchIdentityCode(
        context: Context,
        type: IdentityCodeType,
        onFeedbackMessage: (String) -> Unit
    ) {
        when (type) {
            IdentityCodeType.TAOBAO -> {
                if (tryStartAppUri(context, TAOBAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                if (tryStartAppUri(context, CAINIAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                onFeedbackMessage("未检测到淘宝客户端，请先安装手机淘宝后再试")
            }

            IdentityCodeType.CAINIAO -> {
                if (tryStartAppUri(context, CAINIAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                if (tryStartAppUri(context, TAOBAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                onFeedbackMessage("未检测到淘宝客户端，请先安装手机淘宝后再试")
            }

            IdentityCodeType.PINDUODUO -> {
                if (tryStartAppUri(context, PINDUODUO_IDENTITY_DEEPLINK, PINDUODUO_PACKAGE)) return
                if (tryStartAppUri(context, PINDUODUO_IDENTITY_DEEPLINK, null)) return
                onFeedbackMessage("未检测到拼多多客户端，请先安装拼多多后再试")
            }
        }
    }

    private fun tryStartAppUri(
        context: Context,
        uriString: String,
        targetPackage: String?
    ): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                if (!targetPackage.isNullOrBlank()) {
                    setPackage(targetPackage)
                }
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
 * 顶部「淘宝身份码 / 菜鸟身份码 / 拼多多身份码」液态玻璃快捷 Banner
 */
@Composable
fun IdentityCodeBanner(
    type: IdentityCodeType,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "banner_press_scale"
    )

    val brandGradient = remember(type) {
        Brush.linearGradient(colors = listOf(type.primaryColor, type.secondaryColor))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .liquidGlass(
                cornerRadius = AppleCorners.OuterCard,
                tintColor = type.primaryColor
            )
            .pointerInput(type) {
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
                            type = type,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp)
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
                        .background(brush = brandGradient)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = type.title,
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = type.title,
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
                                .background(type.primaryColor.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "扫码出库",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = type.primaryColor
                            )
                        }
                    }

                    Text(
                        text = type.subtitle,
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
                    .background(brush = brandGradient)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        TaobaoDeepLinkLauncher.launchIdentityCode(
                            context = context,
                            type = type,
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
