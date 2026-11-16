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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight

private enum class CenterActionMode {
    NONE,
    CONFIRM_PICKED_UP,
    UNDO_PICKED_UP
}

/**
 * 首页底部悬浮交互层：
 * 1. 中间位置：
 *    - 当有勾选快递时弹出「确认已取」按钮
 *    - 点击「确认已取」后勾选快递消失，并在完全相同的位置弹出「撤销已取」按钮（无倒计时后缀）
 * 2. 右下角独立「+」添加取件码按钮（60.dp）
 */
@Composable
fun HomeBottomFloatingBar(
    selectedCount: Int,
    canUndo: Boolean,
    onConfirmPickedUp: () -> Unit,
    onUndoPickedUp: () -> Unit,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val confirmGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val undoOrange = if (isDark) AppleOrangeDark else AppleOrangeLight

    val actionMode = when {
        selectedCount > 0 -> CenterActionMode.CONFIRM_PICKED_UP
        canUndo -> CenterActionMode.UNDO_PICKED_UP
        else -> CenterActionMode.NONE
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
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
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AnimatedContent(
                targetState = actionMode,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(AppleCorners.Pill),
                                    ambientColor = confirmGreen.copy(alpha = 0.35f),
                                    spotColor = confirmGreen.copy(alpha = 0.35f)
                                )
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(confirmGreen)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(AppleCorners.Pill)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onConfirmPickedUp()
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "确认已取",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "确认已取 ($selectedCount)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    CenterActionMode.UNDO_PICKED_UP -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(AppleCorners.Pill),
                                    ambientColor = undoOrange.copy(alpha = 0.35f),
                                    spotColor = undoOrange.copy(alpha = 0.35f)
                                )
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(undoOrange)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(AppleCorners.Pill)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUndoPickedUp()
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "撤销已取",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "撤销已取",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    CenterActionMode.NONE -> {
                        Spacer(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }

        // 右下角独立出来的「添加取件码」按钮（60.dp）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(60.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.38f),
                    shape = CircleShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onQuickAddClick()
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "添加取件码",
                tint = Color.White,
                modifier = Modifier.size(33.dp)
            )
        }
    }
}
