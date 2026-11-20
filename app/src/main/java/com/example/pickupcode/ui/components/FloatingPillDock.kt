package com.example.pickupcode.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight
import com.example.pickupcode.ui.theme.AppleRedDark
import com.example.pickupcode.ui.theme.AppleRedLight
import com.example.pickupcode.ui.theme.liquidGlass

private enum class CenterActionMode {
    NONE,
    CONFIRM_PICKED_UP,
    UNDO_PICKED_UP,
    SHARE_SELECTED
}

/**
 * 首页底部悬浮交互层：
 * 1. 普通模式下，勾选待取件时呈现对称三按钮布局：
 *    左侧 60.dp 红色删除圆形按钮 · 中间「确认已取」胶囊 · 右侧 60.dp 蓝色「+」添加按钮；
 *    确认后中间原位置切换为 3 秒可点击的「撤销已取」。
 * 2. 分享选择模式下，中间展示「分享已选 (N)」胶囊入口。
 */
@Composable
fun HomeBottomFloatingBar(
    selectedCount: Int,
    canUndo: Boolean,
    isShareSelectionMode: Boolean,
    shareSelectedCount: Int,
    onConfirmPickedUp: () -> Unit,
    onDeleteSelected: () -> Unit,
    onUndoPickedUp: () -> Unit,
    onQuickAddClick: () -> Unit,
    onConfirmShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val confirmGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val undoOrange = if (isDark) AppleOrangeDark else AppleOrangeLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    val actionMode = when {
        isShareSelectionMode -> CenterActionMode.SHARE_SELECTED
        selectedCount > 0 -> CenterActionMode.CONFIRM_PICKED_UP
        canUndo -> CenterActionMode.UNDO_PICKED_UP
        else -> CenterActionMode.NONE
    }

    // 保留退场动画期间的最后一次有效态与计数，避免胶囊退出时闪现「确认已取 (0)」或缩成空白点
    var displayedActionMode by remember { mutableStateOf(CenterActionMode.CONFIRM_PICKED_UP) }
    var displayedSelectedCount by remember { mutableIntStateOf(1) }
    var displayedShareCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(actionMode, selectedCount, shareSelectedCount) {
        if (actionMode != CenterActionMode.NONE) {
            displayedActionMode = actionMode
        }
        if (selectedCount > 0) {
            displayedSelectedCount = selectedCount
        }
        displayedShareCount = shareSelectedCount
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp)
    ) {
        // 左侧对称 60.dp 红色垃圾桶删除圆形按钮（仅普通多选状态展示）
        AnimatedVisibility(
            visible = !isShareSelectionMode && selectedCount > 0,
            enter = scaleIn(
                initialScale = 0.80f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            exit = scaleOut(
                targetScale = 0.80f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            FloatingCircleActionButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "删除选中取件码",
                baseColor = deleteRed,
                iconSize = 29.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteSelected()
                }
            )
        }

        // 中间胶囊主操作按钮（预留两侧对称间距，永不与左右 60.dp 圆形按钮碰撞）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (isShareSelectionMode) 16.dp else 72.dp)
        ) {
            AnimatedVisibility(
                visible = actionMode != CenterActionMode.NONE,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                exit = scaleOut(
                    targetScale = 0.88f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                AnimatedContent(
                    targetState = displayedActionMode,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))) togetherWith
                            (scaleOut(
                                targetScale = 0.92f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                    },
                    label = "confirm_or_undo_transition"
                ) { mode ->
                    when (mode) {
                        CenterActionMode.CONFIRM_PICKED_UP -> {
                            CenterActionPill(
                                icon = Icons.Rounded.CheckCircle,
                                text = "确认已取 ($displayedSelectedCount)",
                                accentColor = confirmGreen,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onConfirmPickedUp()
                                }
                            )
                        }

                        CenterActionMode.UNDO_PICKED_UP -> {
                            CenterActionPill(
                                icon = Icons.AutoMirrored.Rounded.Undo,
                                text = "撤销已取",
                                accentColor = undoOrange,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUndoPickedUp()
                                }
                            )
                        }

                        CenterActionMode.SHARE_SELECTED -> {
                            if (displayedShareCount > 0) {
                                CenterActionPill(
                                    icon = Icons.Rounded.Share,
                                    text = "分享已选 ($displayedShareCount)",
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onConfirmShareClick()
                                    }
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .liquidGlass(cornerRadius = AppleCorners.Pill)
                                        .padding(horizontal = 20.dp, vertical = 13.dp)
                                ) {
                                    Text(
                                        text = "请勾选要分享的取件码",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        CenterActionMode.NONE -> {
                            Spacer(modifier = Modifier.size(1.dp))
                        }
                    }
                }
            }
        }

        // 右下角 60.dp 蓝色「添加取件码」圆形按钮（分享模式下隐藏以保持专注）
        AnimatedVisibility(
            visible = !isShareSelectionMode,
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            FloatingCircleActionButton(
                icon = Icons.Rounded.Add,
                contentDescription = "添加取件码",
                baseColor = MaterialTheme.colorScheme.primary,
                iconSize = 33.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickAddClick()
                }
            )
        }
    }
}

@Composable
private fun FloatingCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    baseColor: Color,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(60.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = baseColor.copy(alpha = 0.35f),
                spotColor = baseColor.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, baseColor.copy(alpha = 0.86f))
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.38f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun CenterActionPill(
    icon: ImageVector,
    text: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(AppleCorners.Pill),
                ambientColor = accentColor.copy(alpha = 0.35f),
                spotColor = accentColor.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(AppleCorners.Pill))
            .background(accentColor)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(AppleCorners.Pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = Color.White,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
