package com.example.pickupcode.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.PickupCodeMonospaceStyle
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.ui.theme.liquidGlassInnerCard
import com.example.pickupcode.ui.theme.stationAccentColor

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
 * 快递房分组卡片（支持 120Hz 平滑高度过渡）
 */
@Composable
fun StationGroupCard(
    stationNumber: Int,
    badgeNumber: Int?,
    stationDisplayName: String,
    items: List<PickupItemEntity>,
    selectedItemIds: Set<Long>,
    isShareSelectionMode: Boolean = false,
    onToggleSelectItem: (PickupItemEntity) -> Unit,
    onLongPressItem: (PickupItemEntity) -> Unit,
    onClickMoreActions: (PickupItemEntity) -> Unit,
    onClickProductRemark: (PickupItemEntity) -> Unit,
    onQuickAddToStation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val accentColor = stationAccentColor(badgeNumber ?: stationNumber, isDark)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                cornerRadius = AppleCorners.OuterCard,
                tintColor = accentColor
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 420f
                )
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .heightIn(min = 28.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (stationNumber == 0) {
                        Text(
                            text = "点击右侧竖点按钮指定快递房",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(AppleCorners.PillShape)
                        .background(accentColor)
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${items.size} 件",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }

                AnimatedVisibility(
                    visible = !isShareSelectionMode,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                    ) + expandHorizontally(
                        expandFrom = Alignment.End,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                    ) + shrinkHorizontally(
                        shrinkTowards = Alignment.End,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onQuickAddToStation()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "添加取件码到$stationDisplayName",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
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
                    PickupItemCard(
                        item = item,
                        isSelected = item.id in selectedItemIds,
                        isShareSelectionMode = isShareSelectionMode,
                        accentColor = accentColor,
                        onToggleSelect = { onToggleSelectItem(item) },
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
 * 液态玻璃取件码卡片（支持多选勾选、长按快捷操作、点击备注与右侧竖点菜单）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PickupItemCard(
    item: PickupItemEntity,
    isSelected: Boolean,
    isShareSelectionMode: Boolean = false,
    accentColor: Color,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onClickMoreActions: () -> Unit,
    onClickProductRemark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val checkGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val activeCheckColor = if (isShareSelectionMode) MaterialTheme.colorScheme.primary else checkGreen

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassInnerCard(
                cornerRadius = AppleCorners.InnerCard,
                accentTint = if (isSelected) activeCheckColor else accentColor
            )
            .combinedClickable(
                onClick = {
                    if (isShareSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    if (isShareSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSelect()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                }
            )
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        // 左侧多选圆形勾选框
        val checkFillColor by animateColorAsState(
            targetValue = if (isSelected) activeCheckColor else Color.Transparent,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "check_circle_fill"
        )
        val checkBorderColor by animateColorAsState(
            targetValue = if (isSelected) activeCheckColor else accentColor.copy(alpha = 0.65f),
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "check_circle_border"
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(checkScale)
                .size(28.dp)
                .background(color = checkFillColor, shape = CircleShape)
                .border(
                    width = 2.dp,
                    color = checkBorderColor,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSelect()
                }
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(
                    initialScale = 0.55f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(durationMillis = 120)),
                exit = scaleOut(
                    targetScale = 0.55f,
                    animationSpec = tween(durationMillis = 90)
                ) + fadeOut(animationSpec = tween(durationMillis = 90))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已勾选",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 中部：取件码与商品备注
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.code,
                style = PickupCodeMonospaceStyle.copy(fontSize = codeFontSize),
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(AppleCorners.MicroElementShape)
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
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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
