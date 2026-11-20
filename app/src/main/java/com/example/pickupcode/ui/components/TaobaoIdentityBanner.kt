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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.TaobaoOrangeEnd
import com.example.pickupcode.ui.theme.TaobaoOrangeStart
import com.example.pickupcode.ui.theme.liquidGlass

val CainiaoBlueStart = Color(0xFF006EFF)
val CainiaoBlueEnd = Color(0xFF0055D4)

// 拼多多品牌图标红
val PinduoduoRedStart = Color(0xFFE12A1F)
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
        title = "拼多多取件码",
        subtitle = "一键直达拼多多驿站取件码",
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

    // 拼多多取件码 DeepLink
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
                onFeedbackMessage("未检测到淘宝客户端，请先安装后再试")
            }

            IdentityCodeType.CAINIAO -> {
                if (tryStartAppUri(context, CAINIAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                if (tryStartAppUri(context, TAOBAO_IDENTITY_DEEPLINK, TAOBAO_PACKAGE)) return
                onFeedbackMessage("未检测到淘宝客户端，请先安装后再试")
            }

            IdentityCodeType.PINDUODUO -> {
                if (tryStartAppUri(context, PINDUODUO_IDENTITY_DEEPLINK, PINDUODUO_PACKAGE)) return
                if (tryStartAppUri(context, PINDUODUO_IDENTITY_DEEPLINK, null)) return
                onFeedbackMessage("未检测到拼多多客户端，请先安装后再试")
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
 * 首页顶部身份码/取件码入口容器：
 * - 当仅开启 1 个开关时，展示完整单栏液态玻璃 Banner
 * - 当同时开启 2 个或 3 个开关时，自动切换为标题为「身份码/取件码」的对称无框图标模式（均匀排布原生 Apple 连续圆角应用图标）
 */
@Composable
fun IdentityCodeBannersSection(
    enabledTypes: List<IdentityCodeType>,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (enabledTypes.isEmpty()) return

    if (enabledTypes.size == 1) {
        IdentityCodeBanner(
            type = enabledTypes.first(),
            onFeedbackMessage = onFeedbackMessage,
            modifier = modifier
        )
    } else {
        CompactIdentityCodeBar(
            enabledTypes = enabledTypes,
            onFeedbackMessage = onFeedbackMessage,
            modifier = modifier
        )
    }
}

/**
 * 多个身份码/取件码对称图标模式：
 * 仅保留外层液态玻璃卡片与统一层级标题「身份码/取件码」，去除图标外层小框，
 * 2～3 枚统一 Apple 连续圆角应用图标等距对称排布，兼顾美观与大面积舒适点按。
 */
@Composable
fun CompactIdentityCodeBar(
    enabledTypes: List<IdentityCodeType>,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val latestFeedback by rememberUpdatedState(onFeedbackMessage)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = AppleCorners.OuterCard)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "身份码/取件码",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = false,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            enabledTypes.forEach { type ->
                key(type) {
                    var isPressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.91f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "compact_icon_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scale)
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
                                            onFeedbackMessage = latestFeedback
                                        )
                                    }
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        AppBrandIcon(
                            type = type,
                            size = 54.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个身份码液态玻璃快捷 Banner（使用对应淘宝/菜鸟/拼多多应用图标）
 */
@Composable
fun IdentityCodeBanner(
    type: IdentityCodeType,
    onFeedbackMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val latestFeedback by rememberUpdatedState(onFeedbackMessage)
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
                            onFeedbackMessage = latestFeedback
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
                AppBrandIcon(
                    type = type,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(type.primaryColor.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "扫码出库",
                                style = MaterialTheme.typography.labelSmall,
                                color = type.primaryColor,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = type.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

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
                    color = Color.White,
                    softWrap = false,
                    maxLines = 1
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
