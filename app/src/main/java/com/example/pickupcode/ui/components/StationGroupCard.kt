package com.example.pickupcode.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * 快递房分组卡片（常驻展开）
 */
@Composable
fun StationGroupCard(
    stationNumber: Int,
    stationDisplayName: String,
    items: List<PickupItemEntity>,
    selectedItemIds: Set<Long>,
    onToggleSelectItem: (PickupItemEntity) -> Unit,
    onClickMoreActions: (PickupItemEntity) -> Unit,
    onClickProductRemark: (PickupItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = stationAccentColor(stationNumber, isDark)
    val circleSymbol = PickupItemEntity.stationCircleSymbolFor(stationNumber)

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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isDark) 0.24f else 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                ) {
                    Text(
                        text = circleSymbol,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                    PickupItemCard(
                        item = item,
                        isSelected = item.id in selectedItemIds,
                        accentColor = accentColor,
                        onToggleSelect = { onToggleSelectItem(item) },
                        onClickMoreActions = { onClickMoreActions(item) },
                        onClickProductRemark = { onClickProductRemark(item) }
                    )
                }
            }
        }
    }
}

/**
 * 极简取件码子卡片：
 * - 左侧：多选勾选圆圈
 * - 中部上方：完整显示取件码（大字号等宽特粗）
 * - 中部下方：显示「点击备注商品」或已备注的商品名称，点击弹出备注框
 * - 右侧：三个竖点按钮（⋮），点击弹出原文与编辑归类弹窗（点击卡片空白处不再触发弹窗）
 */
@Composable
fun PickupItemCard(
    item: PickupItemEntity,
    isSelected: Boolean,
    accentColor: Color,
    onToggleSelect: () -> Unit,
    onClickMoreActions: () -> Unit,
    onClickProductRemark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val checkGreen = if (isDark) AppleGreenDark else AppleGreenLight

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
                isPickedUp = false,
                accentTint = if (isSelected) checkGreen else accentColor
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

        // 主体区域：上方完整取件码 + 下方商品备注入口
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.code,
                style = PickupCodeMonospaceStyle.copy(fontSize = codeFontSize),
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false
            )

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
