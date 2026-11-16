package com.example.pickupcode.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.components.StationBadgeSymbol
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleRedDark
import com.example.pickupcode.ui.theme.AppleRedLight
import com.example.pickupcode.ui.theme.PickupCodeMonospaceStyle
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.ui.theme.stationAccentColor
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

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StationBadgeSymbol(
                        badgeNumber = badgeNum,
                        accentColor = accent,
                        isUnclassified = item.stationNumber == 0
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.code,
                            style = PickupCodeMonospaceStyle.copy(fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(Date(item.createdAt)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppleCorners.InnerCard))
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
                    )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    stationRules.forEach { rule ->
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
                                color = if (selected) Color.White else numAccent
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
                        fontWeight = FontWeight.SemiBold
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
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(AppleCorners.InnerCard)
                    ) {
                        Text(text = "完成", fontWeight = FontWeight.Bold)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "取件码 ${item.code}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("清空备注", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        onConfirm(text.trim())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                ) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 3. 添加取件码弹窗（默认选中「手动输入取件码」，切换标签时高度固定不跳动）
 */
@Composable
fun AddOrPastePickupDialog(
    stationRules: List<StationRule>,
    onDismiss: () -> Unit,
    onPasteParse: (String) -> Unit,
    onManualAdd: (code: String, stationNumber: Int, productName: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // 默认 tabIndex = 0 即「手动输入取件码」
    var tabIndex by remember { mutableIntStateOf(0) }
    var manualCode by remember { mutableStateOf("") }
    var manualStation by remember {
        mutableIntStateOf(stationRules.firstOrNull()?.stationNumber ?: 1)
    }
    var manualProduct by remember { mutableStateOf("") }
    var rawSmsInput by remember { mutableStateOf("") }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "添加取件码",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
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
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 248.dp)
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
                        if (stationRules.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "选择快递房：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                stationRules.forEach { rule ->
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
                                            color = if (selected) Color.White else accent
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
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            enabled = manualCode.isNotBlank(),
                            onClick = {
                                onManualAdd(manualCode, manualStation, manualProduct)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(AppleCorners.InnerCard)
                        ) {
                            Text("保存取件码", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "支持包含多个取件码的合并短信，将自动拆分为多条独立记录：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            enabled = rawSmsInput.isNotBlank(),
                            onClick = {
                                onPasteParse(rawSmsInput)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(AppleCorners.InnerCard)
                        ) {
                            Text("智能解析入库", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
