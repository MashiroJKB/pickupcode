package com.example.pickupcode.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.data.local.PickupItemEntity
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 统一的快递房前缀序号/实心圆组件（主页与设置页识别规则共用）
 */
@Composable
fun StationBadgeSymbol(
    badgeNumber: Int?,
    accentColor: Color,
    isUnclassified: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(22.dp)
    ) {
        when {
            isUnclassified -> {
                Text(
                    text = "？",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 22.sp
                    ),
                    color = accentColor
                )
            }

            badgeNumber != null && badgeNumber in 1..20 -> {
                Text(
                    text = PickupItemEntity.stationCircleSymbolFor(badgeNumber),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        lineHeight = 22.sp
                    ),
                    color = accentColor
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
    }
}

/**
 * 快递房分组卡片
 */
@Composable
fun StationGroupCard(
    stationNumber: Int,
    badgeNumber: Int?,
    stationDisplayName: String,
    items: List<PickupItemEntity>,
    selectedItemIds: Set<Long>,
    onToggleSelectItem: (PickupItemEntity) -> Unit,
    onQuickMarkPickedUp: (PickupItemEntity) -> Unit,
    onQuickDeleteItem: (PickupItemEntity) -> Unit,
    onLongPressItem: (PickupItemEntity) -> Unit,
    onClickMoreActions: (PickupItemEntity) -> Unit,
    onClickProductRemark: (PickupItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = stationAccentColor(badgeNumber ?: stationNumber, isDark)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                cornerRadius = AppleCorners.OuterCard,
                tintColor = accentColor
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                StationBadgeSymbol(
                    badgeNumber = badgeNumber,
                    accentColor = accentColor,
                    isUnclassified = stationNumber == 0
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = stationDisplayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (stationNumber == 0) {
                        Text(
                            text = "点击右侧竖点按钮可指定所属快递房",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleCorners.Pill))
                    .background(accentColor)
                    .padding(horizontal = 11.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${items.size} 件",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            for (item in items) {
                key(item.id) {
                    SwipeablePickupItemCard(
                        item = item,
                        isSelected = item.id in selectedItemIds,
                        accentColor = accentColor,
                        onToggleSelect = { onToggleSelectItem(item) },
                        onQuickMarkPickedUp = { onQuickMarkPickedUp(item) },
                        onQuickDelete = { onQuickDeleteItem(item) },
                        onLongPress = { onLongPressItem(item) },
                        onClickMoreActions = { onClickMoreActions(item) },
                        onClickProductRemark = { onClickProductRemark(item) }
                    )
                }
            }
        }
    }
}

/**
 * 支持左滑快捷操作（已取 / 删除）、长按弹出快捷菜单、滞留天数提醒角标的取件码卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeablePickupItemCard(
    item: PickupItemEntity,
    isSelected: Boolean,
    accentColor: Color,
    onToggleSelect: () -> Unit,
    onQuickMarkPickedUp: () -> Unit,
    onQuickDelete: () -> Unit,
    onLongPress: () -> Unit,
    onClickMoreActions: () -> Unit,
    onClickProductRemark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val checkGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight
    val warnOrange = if (isDark) AppleOrangeDark else AppleOrangeLight

    val maxRevealPx = remember(density) { with(density) { 136.dp.toPx() } }
    val offsetX = remember { Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val next = (offsetX.value + delta).coerceIn(-maxRevealPx, 0f)
            offsetX.snapTo(next)
        }
    }

    // 计算滞留天数角标（超过 24 小时显示柔和橙色/红色「已到 N 天」标签）
    val retentionDays = remember(item.createdAt) {
        val elapsedHours = (System.currentTimeMillis() - item.createdAt) / (1000L * 60L * 60L)
        if (elapsedHours >= 24L) (elapsedHours / 24L).toInt() else 0
    }

    val checkScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_circle_scale"
    )

    val codeFontSize = when {
        item.code.length >= 14 -> 18.sp
        item.code.length >= 11 -> 20.sp
        else -> 22.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppleCorners.InnerCard))
    ) {
        // 底层：左滑漏出的「已取」与「删除」快捷操作按钮
        if (offsetX.value < -2f) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(62.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(checkGreen)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { offsetX. snapTo(0f) }
                            onQuickMarkPickedUp()
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "已取",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "已取",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(62.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(deleteRed)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { offsetX.snapTo(0f) }
                            onQuickDelete()
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 顶层：取件码主卡片（支持左滑拖拽与长按弹出快捷菜单）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .liquidGlassInnerCard(
                    cornerRadius = AppleCorners.InnerCard,
                    isPickedUp = false,
                    accentTint = if (isSelected) checkGreen else accentColor
                )
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            val shouldOpen = offsetX.value < -maxRevealPx * 0.4f || velocity < -600f
                            val target = if (shouldOpen) -maxRevealPx else 0f
                            offsetX.animateTo(
                                targetValue = target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )
                .combinedClickable(
                    onClick = {
                        if (offsetX.value < -10f) {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // 左侧多选圆形勾选框
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(checkScale)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) checkGreen else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) checkGreen else accentColor.copy(alpha = 0.65f),
                        shape = CircleShape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSelect()
                    }
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "已勾选",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中部：取件码 + 滞留天数角标 + 下方商品备注
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.code,
                        style = PickupCodeMonospaceStyle.copy(fontSize = codeFontSize),
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false
                    )

                    if (retentionDays >= 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val tagColor = if (retentionDays >= 2) deleteRed else warnOrange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tagColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "已到 ${retentionDays} 天",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = tagColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClickProductRemark()
                        }
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    val hasRemark = item.productName.isNotBlank()
                    Text(
                        text = if (hasRemark) item.productName else "点击备注商品",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (hasRemark) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (hasRemark) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            accentColor
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = "备注商品",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // 右侧三个竖点按钮（⋮）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClickMoreActions()
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
