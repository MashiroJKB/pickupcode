package com.example.pickupcode.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.components.StationBadgeSymbol
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight
import com.example.pickupcode.ui.theme.AppleRedDark
import com.example.pickupcode.ui.theme.AppleRedLight
import com.example.pickupcode.ui.theme.PickupCodeMonospaceStyle
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.ui.theme.liquidGlassInnerCard
import com.example.pickupcode.ui.theme.stationAccentColor
import com.example.pickupcode.util.RomRestrictedGuide
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局统一的屏幕正中心缩放弹窗容器：
 * 使用全屏 Box + Alignment.Center + graphicsLayer 中心原点缩放，确保弹窗 100% 从屏幕正中央浮现，绝不从右下角偏移。
 */
@Composable
fun SpringGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(0.90f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 140)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                        transformOrigin = TransformOrigin.Center
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(20.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 1. 原始短信全文与归类编辑弹窗（点击取件码条右侧三个竖点按钮触发）
 */
@Composable
fun RawMessageDetailDialog(
    item: PickupItemEntity,
    stationRules: List<StationRule>,
    onDismiss: () -> Unit,
    onEditProduct: (PickupItemEntity) -> Unit,
    onChangeStation: (Int) -> Unit,
    onDelete: (PickupItemEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val matchedRule = stationRules.firstOrNull { it.stationNumber == item.stationNumber }
    val badgeNum = if (item.stationNumber > 0) {
        if (matchedRule != null) matchedRule.badgeNumber else item.stationNumber
    } else {
        null
    }
    val accent = stationAccentColor(badgeNum ?: item.stationNumber, isDark)
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val stationRowState = rememberLazyListState()

    LaunchedEffect(item.id, item.stationNumber, stationRules) {
        val targetIndex = stationRules.indexOfFirst { it.stationNumber == item.stationNumber }
        if (targetIndex >= 0) {
            stationRowState.scrollToItem(targetIndex)
        }
    }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    StationBadgeSymbol(
                        badgeNumber = badgeNum,
                        accentColor = accent,
                        isUnclassified = item.stationNumber == 0
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.code,
                            style = PickupCodeMonospaceStyle.copy(fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormat.format(Date(item.createdAt)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "原始短信全文",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(AppleCorners.InnerCard))
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = item.rawMessage,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (stationRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "归属快递房",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    state = stationRowState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = stationRules,
                        key = { "detail_station_${it.stationNumber}" }
                    ) { rule ->
                        val num = rule.stationNumber
                        val selected = item.stationNumber == num
                        val numAccent = stationAccentColor(rule.badgeNumber ?: num, isDark)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                .background(
                                    if (selected) numAccent else numAccent.copy(alpha = 0.12f)
                                )
                                .clickable { onChangeStation(num) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${rule.circleSymbol} ${rule.displayName}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) Color.White else numAccent,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        onDelete(item)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除记录",
                        tint = if (isDark) AppleRedDark else AppleRedLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "删除",
                        color = if (isDark) AppleRedDark else AppleRedLight,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = false,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onEditProduct(item)
                        }
                    ) {
                        Text(
                            text = "备注商品",
                            fontWeight = FontWeight.SemiBold,
                            softWrap = false,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(AppleCorners.InnerCard)
                    ) {
                        Text(
                            text = "完成",
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. 商品名称快速备注弹窗
 */
@Composable
fun EditProductRemarkDialog(
    item: PickupItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(item.id) { mutableStateOf(item.productName) }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "备注商品名称",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "取件码 ${item.code}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(30) },
                label = { Text("输入商品名称") },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (item.productName.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onConfirm("")
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "清空备注",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        onConfirm(text.trim())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                ) {
                    Text("保存", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }
        }
    }
}

/**
 * 3. 添加取件码弹窗（支持指定默认快递房直达添加，默认选中「手动输入取件码」，切换标签时高度固定不跳动）
 */
@Composable
fun AddOrPastePickupDialog(
    stationRules: List<StationRule>,
    initialStationNumber: Int? = null,
    onDismiss: () -> Unit,
    onPasteParse: (String, Int?) -> Unit,
    onManualAdd: (code: String, stationNumber: Int, productName: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    // 默认 tabIndex = 0 即「手动输入取件码」
    var tabIndex by remember { mutableIntStateOf(0) }
    var manualCode by remember { mutableStateOf("") }
    var manualStation by remember(initialStationNumber, stationRules) {
        mutableIntStateOf(initialStationNumber ?: stationRules.firstOrNull()?.stationNumber ?: 0)
    }
    var manualProduct by remember { mutableStateOf("") }
    var rawSmsInput by remember { mutableStateOf("") }

    val stationListState = rememberLazyListState()
    val targetRule = remember(initialStationNumber, stationRules) {
        initialStationNumber?.let { target ->
            stationRules.firstOrNull { it.stationNumber == target }
        }
    }
    val targetStationLabel = remember(initialStationNumber, targetRule) {
        when {
            initialStationNumber == null -> null
            initialStationNumber == 0 -> "？ 待分类快递房"
            targetRule != null -> "${targetRule.circleSymbol} ${targetRule.displayName}"
            else -> "${PickupItemEntity.stationCircleSymbolFor(initialStationNumber)} ${initialStationNumber}号快递房"
        }
    }
    val targetStationAccent = remember(initialStationNumber, targetRule, isDark) {
        initialStationNumber?.let { target ->
            stationAccentColor(targetRule?.badgeNumber ?: target, isDark)
        } ?: primaryColor
    }

    LaunchedEffect(initialStationNumber, stationRules) {
        val target = initialStationNumber ?: return@LaunchedEffect
        if (target == 0) {
            stationListState.scrollToItem(0)
        } else {
            val targetIndex = stationRules.indexOfFirst { it.stationNumber == target }
            if (targetIndex >= 0) {
                stationListState.scrollToItem(targetIndex)
            }
        }
    }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "添加取件码",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )
                if (targetStationLabel != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(AppleCorners.Pill))
                            .background(targetStationAccent.copy(alpha = 0.14f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = targetStationLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = targetStationAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppleCorners.InnerCard))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .padding(4.dp)
            ) {
                listOf("手动输入取件码", "粘贴短信解析").forEachIndexed { idx, title ->
                    val selected = tabIndex == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppleCorners.SmallElement))
                            .background(
                                if (selected) primaryColor else Color.Transparent
                            )
                            .clickable { tabIndex = idx }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 276.dp)
            ) {
                if (tabIndex == 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it.take(20) },
                            label = { Text("取件码") },
                            singleLine = true,
                            shape = RoundedCornerShape(AppleCorners.InnerCard),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (stationRules.isNotEmpty() || initialStationNumber == 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "选择快递房：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                state = stationListState,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (initialStationNumber == 0) {
                                    item(key = "station_chip_0") {
                                        val selected = manualStation == 0
                                        val accent = stationAccentColor(0, isDark)
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                                .background(if (selected) accent else accent.copy(alpha = 0.14f))
                                                .clickable { manualStation = 0 }
                                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "？ 待分类",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (selected) Color.White else accent,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = stationRules,
                                    key = { "station_chip_${it.stationNumber}" }
                                ) { rule ->
                                    val num = rule.stationNumber
                                    val selected = manualStation == num
                                    val accent = stationAccentColor(rule.badgeNumber ?: num, isDark)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                            .background(if (selected) accent else accent.copy(alpha = 0.14f))
                                            .clickable { manualStation = num }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "${rule.circleSymbol} ${rule.displayName}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selected) Color.White else accent,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = manualProduct,
                            onValueChange = { manualProduct = it.take(24) },
                            label = { Text("商品备注（选填）") },
                            singleLine = true,
                            shape = RoundedCornerShape(AppleCorners.InnerCard),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", softWrap = false, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            enabled = manualCode.isNotBlank(),
                            onClick = {
                                onManualAdd(manualCode, manualStation, manualProduct)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(AppleCorners.InnerCard)
                        ) {
                            Text("保存取件码", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (targetStationLabel != null) {
                                "粘贴短信解析后将直接归入「$targetStationLabel」"
                            } else {
                                "支持多码合并短信，自动拆分为独立记录"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = rawSmsInput,
                            onValueChange = { rawSmsInput = it },
                            placeholder = {
                                Text("在此粘贴快递短信全文...")
                            },
                            minLines = 4,
                            maxLines = 5,
                            shape = RoundedCornerShape(AppleCorners.InnerCard),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", softWrap = false, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            enabled = rawSmsInput.isNotBlank(),
                            onClick = {
                                onPasteParse(rawSmsInput, initialStationNumber)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(AppleCorners.InnerCard)
                        ) {
                            Text("智能解析入库", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. 长按取件码快捷操作弹窗（弹出「已取」或「删除」按钮）
 */
@Composable
fun LongPressQuickActionDialog(
    item: PickupItemEntity,
    onDismiss: () -> Unit,
    onConfirmPickedUp: (PickupItemEntity) -> Unit,
    onDelete: (PickupItemEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val checkGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.code,
                style = PickupCodeMonospaceStyle.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.productName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        onConfirmPickedUp(item)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = checkGreen),
                    shape = RoundedCornerShape(AppleCorners.InnerCard),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "已取",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        onDelete(item)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deleteRed),
                    shape = RoundedCornerShape(AppleCorners.InnerCard),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "删除",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 首次安装/未授权时的分步短信权限引导阶段
 */
enum class SmsOnboardingStep {
    RESTRICTED_SETTINGS,
    SMS_PERMISSION
}

/**
 * 5. 首次打开权限引导弹窗：
 * - Android 15+ 侧载受限时，先按当前系统 ROM 清晰指引解除「设置限制 / 受限设置」并一键直达应用信息；
 * - 返回检测到解除后，丝滑过渡至小白友好的「授权短信读取权限」步骤；
 * - Android 10~14 或无受限状态时直接展示单步短信权限引导。
 */
@Composable
fun SmsOnboardingPermissionDialog(
    step: SmsOnboardingStep,
    showStepIndicator: Boolean,
    romGuide: RomRestrictedGuide,
    returnedWithoutLift: Boolean,
    hasTriedDirectSmsAfterReturn: Boolean,
    justUnlockedRestricted: Boolean,
    isSmsPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestSmsPermission: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val statusGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showStepIndicator) {
                OnboardingStepIndicatorRow(
                    step = step,
                    primaryColor = primaryColor,
                    statusGreen = statusGreen
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState == SmsOnboardingStep.SMS_PERMISSION
                    val enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> if (forward) fullWidth / 5 else -fullWidth / 5 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> if (forward) -fullWidth / 5 else fullWidth / 5 }
                    ) + fadeOut(animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing))
                    (enter togetherWith exit).using(
                        SizeTransform(clip = false) { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        }
                    )
                },
                label = "sms_onboarding_step_transition"
            ) { currentStep ->
                when (currentStep) {
                    SmsOnboardingStep.RESTRICTED_SETTINGS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(statusAmber.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = statusAmber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "先解除设置限制",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Android 15+ 侧载安全保护",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlassInnerCard(
                                        cornerRadius = AppleCorners.InnerCard,
                                        accentTint = statusAmber
                                    )
                                    .padding(horizontal = 14.dp, vertical = 13.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AppleCorners.Pill))
                                        .background(primaryColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "当前系统 · ${romGuide.romName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(9.dp))

                                Text(
                                    text = "通过浏览器或安装包安装的应用\n需先解除系统保护，再开启短信权限",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.7.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OnboardingNumberStepRow(
                                        stepNumber = "1",
                                        text = "点击下方「前往应用信息」",
                                        accentColor = primaryColor
                                    )
                                    OnboardingNumberStepRow(
                                        stepNumber = "2",
                                        text = romGuide.stepTwoText,
                                        accentColor = statusAmber
                                    )
                                    OnboardingNumberStepRow(
                                        stepNumber = "3",
                                        text = "返回本应用，继续授权短信读取",
                                        accentColor = statusGreen
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = returnedWithoutLift,
                                enter = expandVertically(
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                                exit = shrinkVertically(
                                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 120))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                        .background(statusAmber.copy(alpha = 0.12f))
                                        .border(
                                            width = 0.8.dp,
                                            color = statusAmber.copy(alpha = 0.30f),
                                            shape = RoundedCornerShape(AppleCorners.SmallElement)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = if (hasTriedDirectSmsAfterReturn) {
                                            "已触发系统安全提示，请点「再次前往」\n在应用信息页解除限制后返回即可"
                                        } else {
                                            "暂未检测到解除，若未找到受限设置\n可先点「直接授权」激活后再前往设置"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 17.sp),
                                        color = statusAmber
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (returnedWithoutLift) {
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onRequestSmsPermission()
                                        }
                                    ) {
                                        Text(
                                            text = "直接授权",
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                } else {
                                    TextButton(onClick = onDismiss) {
                                        Text(
                                            text = "暂不设置",
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenAppInfo()
                                    },
                                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                                ) {
                                    Text(
                                        text = if (returnedWithoutLift) "再次前往" else "前往应用信息",
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    SmsOnboardingStep.SMS_PERMISSION -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = 0.14f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sms,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "授权短信读取权限",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (justUnlockedRestricted) {
                                                "已解除限制 · 开启后自动识别"
                                            } else {
                                                "自动整理近 7 天快递取件短信"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (justUnlockedRestricted) {
                                                statusGreen
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                OnboardingFeatureRow(
                                    icon = Icons.Rounded.MarkEmailRead,
                                    title = "自动提取快递取件码",
                                    subtitle = "扫描近 7 天短信，过滤验证码",
                                    accentColor = primaryColor
                                )
                                OnboardingFeatureRow(
                                    icon = Icons.Rounded.VerifiedUser,
                                    title = "纯本地运行，绝不联网",
                                    subtitle = "无网络权限，短信绝不上传云端",
                                    accentColor = statusGreen
                                )
                            }

                            AnimatedVisibility(
                                visible = isSmsPermanentlyDenied,
                                enter = expandVertically(
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                                exit = shrinkVertically(
                                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(durationMillis = 120))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                        .background(statusAmber.copy(alpha = 0.12f))
                                        .border(
                                            width = 0.8.dp,
                                            color = statusAmber.copy(alpha = 0.30f),
                                            shape = RoundedCornerShape(AppleCorners.SmallElement)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = "系统已限制弹窗，请点「前往应用信息」\n在「权限 → 短信」中允许读取即可",
                                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 17.sp),
                                        color = statusAmber
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text(
                                        text = "暂不设置",
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSmsPermanentlyDenied) {
                                            onOpenAppInfo()
                                        } else {
                                            onRequestSmsPermission()
                                        }
                                    },
                                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                                ) {
                                    Text(
                                        text = if (isSmsPermanentlyDenied) "前往应用信息" else "授予短信权限",
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepIndicatorRow(
    step: SmsOnboardingStep,
    primaryColor: Color,
    statusGreen: Color
) {
    val isStepTwo = step == SmsOnboardingStep.SMS_PERMISSION
    val mutedCircleBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    val step1CircleColor by animateColorAsState(
        targetValue = if (isStepTwo) statusGreen else primaryColor,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step1_circle_color"
    )
    val step1LabelColor by animateColorAsState(
        targetValue = if (isStepTwo) statusGreen else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step1_label_color"
    )
    val connectorColor by animateColorAsState(
        targetValue = if (isStepTwo) statusGreen.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step_connector_color"
    )
    val step2CircleColor by animateColorAsState(
        targetValue = if (isStepTwo) primaryColor else mutedCircleBg,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step2_circle_color"
    )
    val step2NumColor by animateColorAsState(
        targetValue = if (isStepTwo) Color.White else mutedText,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step2_num_color"
    )
    val step2LabelColor by animateColorAsState(
        targetValue = if (isStepTwo) MaterialTheme.colorScheme.onSurface else mutedText,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "step2_label_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(step1CircleColor)
            ) {
                if (isStepTwo) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isStepTwo) "已解除限制" else "解除设置限制",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isStepTwo) FontWeight.SemiBold else FontWeight.Bold
                ),
                color = step1LabelColor,
                softWrap = false,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(1.5.dp)
                .clip(RoundedCornerShape(AppleCorners.Pill))
                .background(connectorColor)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(step2CircleColor)
            ) {
                Text(
                    text = "2",
                    style = MaterialTheme.typography.labelSmall,
                    color = step2NumColor,
                    softWrap = false,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "授权短信读取",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isStepTwo) FontWeight.Bold else FontWeight.Medium
                ),
                color = step2LabelColor,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OnboardingNumberStepRow(
    stepNumber: String,
    text: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                softWrap = false,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassInnerCard(
                cornerRadius = AppleCorners.InnerCard,
                accentTint = accentColor
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(AppleCorners.SmallElement))
                .background(accentColor.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 6. 分享取件码弹窗（仅图片分享，支持切换是否分享备注）
 */
@Composable
fun SharePickupMethodDialog(
    selectedCount: Int,
    isGeneratingImage: Boolean,
    onDismiss: () -> Unit,
    onShareAsImage: (includeRemarks: Boolean) -> Unit
) {
    val imageAccent = MaterialTheme.colorScheme.primary
    var includeRemarks by remember { mutableStateOf(false) }

    SpringGlassDialog(onDismissRequest = { if (!isGeneratingImage) onDismiss() }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(imageAccent.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = imageAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "分享取件码",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "已选 $selectedCount 件 · 可分享到微信等应用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    enabled = !isGeneratingImage,
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ShareOptionCard(
                icon = Icons.Rounded.Image,
                title = if (isGeneratingImage) "正在准备清单图片..." else "以图片分享",
                accent = imageAccent,
                enabled = !isGeneratingImage,
                onClick = { onShareAsImage(includeRemarks) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassInnerCard(
                        cornerRadius = AppleCorners.InnerCard,
                        accentTint = if (includeRemarks) imageAccent else Color.Transparent
                    )
                    .clickable(enabled = !isGeneratingImage) {
                        includeRemarks = !includeRemarks
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .then(
                            if (includeRemarks) {
                                Modifier.background(imageAccent)
                            } else {
                                Modifier.border(
                                    width = 1.6.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                            }
                        )
                ) {
                    if (includeRemarks) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (includeRemarks) "分享备注" else "不分享备注",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (includeRemarks) "附带商品备注" else "不附带商品备注",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    enabled = !isGeneratingImage,
                    onClick = onDismiss
                ) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ShareOptionCard(
    icon: ImageVector,
    title: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassInnerCard(
                cornerRadius = AppleCorners.InnerCard,
                accentTint = accent
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(AppleCorners.SmallElement))
                .background(accent.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 7. 新安装引导弹窗：询问学校快递房数量并一键生成对应识别规则
 */
@Composable
fun StationCountSetupDialog(
    onDismiss: () -> Unit,
    onConfirmCount: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var countText by remember { mutableStateOf("8") }
    val parsedCount = countText.trim().toIntOrNull()
    val isValidCount = parsedCount != null && parsedCount in 1..20

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "你的学校有几个快递房？",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "将自动生成对应数量的识别规则",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(4, 6, 8, 10, 12).forEach { preset ->
                    val selected = parsedCount == preset
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(AppleCorners.SmallElement))
                            .background(
                                if (selected) primaryColor else primaryColor.copy(alpha = 0.12f)
                            )
                            .clickable { countText = preset.toString() }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${preset}个",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (selected) Color.White else primaryColor,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = countText,
                onValueChange = { input ->
                    countText = input.filter { it.isDigit() }.take(2)
                },
                label = { Text("输入快递房数量（1~20）", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = countText.isNotEmpty() && !isValidCount,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("暂不设置", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    enabled = isValidCount,
                    onClick = {
                        parsedCount?.let { onConfirmCount(it) }
                    },
                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                ) {
                    Text("确认配置", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }
        }
    }
}
