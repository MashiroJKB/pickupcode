package com.example.pickupcode.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.components.IdentityCodeBannersSection
import com.example.pickupcode.ui.components.IdentityCodeType
import com.example.pickupcode.ui.components.StationGroupCard
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight
import com.example.pickupcode.ui.theme.liquidGlass

data class StationGroupSection(
    val stationNumber: Int,
    val badgeNumber: Int?,
    val displayName: String,
    val items: List<PickupItemEntity>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<PickupItemEntity>,
    stationRules: List<StationRule>,
    selectedItemIds: Set<Long>,
    isShareSelectionMode: Boolean,
    shareSelectedIds: Set<Long>,
    showTaobaoIdentityBanner: Boolean,
    showCainiaoIdentityBanner: Boolean,
    showPinduoduoIdentityBanner: Boolean,
    hasSmsPermission: Boolean,
    onOpenSettings: () -> Unit,
    onEnterShareMode: () -> Unit,
    onExitShareMode: () -> Unit,
    onToggleSelectAllForShare: () -> Unit,
    onToggleShareItem: (PickupItemEntity) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onQuickAddClick: () -> Unit,
    onToggleSelectItem: (PickupItemEntity) -> Unit,
    onQuickMarkPickedUp: (PickupItemEntity) -> Unit,
    onQuickDeleteItem: (PickupItemEntity) -> Unit,
    onLongPressItem: (PickupItemEntity) -> Unit,
    onClickMoreActions: (PickupItemEntity) -> Unit,
    onClickProductRemark: (PickupItemEntity) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var activeSwipedItemId by remember { mutableStateOf<Long?>(null) }

    // 列表滚动时优雅自动收起已左滑展开的卡片
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && activeSwipedItemId != null) {
            activeSwipedItemId = null
        }
    }

    val pendingItems = remember(items) {
        items.filter { !it.isPickedUp }
    }

    val activeStationCount = remember(pendingItems) {
        pendingItems.filter { it.stationNumber > 0 }.map { it.stationNumber }.distinct().size
    }

    val enabledIdentityTypes = remember(
        showTaobaoIdentityBanner,
        showCainiaoIdentityBanner,
        showPinduoduoIdentityBanner
    ) {
        buildList {
            if (showTaobaoIdentityBanner) add(IdentityCodeType.TAOBAO)
            if (showCainiaoIdentityBanner) add(IdentityCodeType.CAINIAO)
            if (showPinduoduoIdentityBanner) add(IdentityCodeType.PINDUODUO)
        }
    }

    val groupedSections = remember(pendingItems, stationRules) {
        val rulesByNum = stationRules.associateBy { it.stationNumber }
        val orderMap = stationRules.mapIndexed { index, rule -> rule.stationNumber to index }.toMap()

        pendingItems
            .groupBy { it.stationNumber }
            .map { (stationNum, groupItems) ->
                val rule = rulesByNum[stationNum]
                val displayName = if (stationNum > 0) {
                    rule?.displayName ?: "${stationNum}号快递房"
                } else {
                    "待分类快递房"
                }
                val badgeNumber = if (stationNum > 0) {
                    if (rule != null) rule.badgeNumber else stationNum
                } else {
                    null
                }
                StationGroupSection(
                    stationNumber = stationNum,
                    badgeNumber = badgeNumber,
                    displayName = displayName,
                    items = groupItems.sortedWith(PickupItemEntity.NaturalCodeComparator)
                )
            }
            .sortedBy { section ->
                if (section.stationNumber == 0) -1
                else orderMap[section.stationNumber] ?: (1000 + section.stationNumber)
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "home_header", contentType = "header") {
            val pendingCount = pendingItems.size
            val activeShareSelectedCount = remember(pendingItems, shareSelectedIds) {
                val pendingIds = pendingItems.asSequence().map { it.id }.toSet()
                shareSelectedIds.count { it in pendingIds }
            }
            HomeTopHeader(
                totalPending = pendingCount,
                activeStationCount = activeStationCount,
                hasSmsPermission = hasSmsPermission,
                isShareSelectionMode = isShareSelectionMode,
                shareSelectedCount = activeShareSelectedCount,
                allSelectedForShare = pendingCount > 0 && activeShareSelectedCount == pendingCount,
                onOpenSettings = onOpenSettings,
                onEnterShareMode = onEnterShareMode,
                onExitShareMode = onExitShareMode,
                onToggleSelectAllForShare = onToggleSelectAllForShare,
                onRequestSmsPermission = onRequestSmsPermission,
                isDark = isDark
            )
        }

        // 身份码入口：开启 1 个时显示完整 Banner；开启 2~3 个时自动切换为紧凑并排方块模式
        if (enabledIdentityTypes.isNotEmpty()) {
            item(key = "identity_code_section", contentType = "banner") {
                IdentityCodeBannersSection(
                    enabledTypes = enabledIdentityTypes,
                    onFeedbackMessage = onFeedbackMessage
                )
            }
        }

        if (groupedSections.isEmpty()) {
            item(key = "empty_state", contentType = "empty") {
                EmptyPickupStateCard(
                    hasSmsPermission = hasSmsPermission,
                    onRequestSmsPermission = onRequestSmsPermission,
                    onQuickAddClick = onQuickAddClick
                )
            }
        } else {
            items(
                items = groupedSections,
                key = { section -> "station_group_${section.stationNumber}" },
                contentType = { "station_group" }
            ) { section ->
                StationGroupCard(
                    stationNumber = section.stationNumber,
                    badgeNumber = section.badgeNumber,
                    stationDisplayName = section.displayName,
                    items = section.items,
                    selectedItemIds = if (isShareSelectionMode) shareSelectedIds else selectedItemIds,
                    isShareSelectionMode = isShareSelectionMode,
                    activeSwipedItemId = activeSwipedItemId,
                    onSwipeOpenChange = { id, open ->
                        activeSwipedItemId = if (open) id else if (activeSwipedItemId == id) null else activeSwipedItemId
                    },
                    onToggleSelectItem = if (isShareSelectionMode) onToggleShareItem else onToggleSelectItem,
                    onQuickMarkPickedUp = onQuickMarkPickedUp,
                    onQuickDeleteItem = onQuickDeleteItem,
                    onLongPressItem = onLongPressItem,
                    onClickMoreActions = onClickMoreActions,
                    onClickProductRemark = onClickProductRemark,
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = 400f
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeTopHeader(
    totalPending: Int,
    activeStationCount: Int,
    hasSmsPermission: Boolean,
    isShareSelectionMode: Boolean,
    shareSelectedCount: Int,
    allSelectedForShare: Boolean,
    onOpenSettings: () -> Unit,
    onEnterShareMode: () -> Unit,
    onExitShareMode: () -> Unit,
    onToggleSelectAllForShare: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    isDark: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        AnimatedContent(
            targetState = isShareSelectionMode,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = 460f)) togetherWith
                    fadeOut(animationSpec = spring(stiffness = 520f))
            },
            label = "home_header_mode_transition"
        ) { inShareMode ->
            if (inShareMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(AppleCorners.Pill))
                            .background(
                                if (allSelectedForShare) primaryColor
                                else primaryColor.copy(alpha = 0.14f)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleSelectAllForShare()
                            }
                            .padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = "全选",
                            tint = if (allSelectedForShare) Color.White else primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "全选",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = if (allSelectedForShare) Color.White else primaryColor,
                            softWrap = false,
                            maxLines = 1
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(AppleCorners.Pill))
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isDark) 0.14f else 0.08f
                                )
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExitShareMode()
                            }
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "取件码",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            softWrap = false,
                            maxLines = 1
                        )
                        if (!hasSmsPermission) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AppleCorners.Pill))
                                    .background(statusAmber.copy(alpha = 0.15f))
                                    .clickable(onClick = onRequestSmsPermission)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusAmber)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "授权短信",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = statusAmber,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 转发图标 + 分享取件码按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(primaryColor.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEnterShareMode()
                                }
                                .padding(horizontal = 12.dp)
                        ) {
                            ForwardArrowIcon(
                                tint = primaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "分享取件码",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = primaryColor,
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        // 极简设置齿轮圆形图标按钮
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onOpenSettings()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "设置",
                                tint = primaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = when {
                isShareSelectionMode ->
                    "已选 $shareSelectedCount / $totalPending 件 · 点击卡片或「全选」"
                totalPending > 0 && activeStationCount > 0 ->
                    "当前 $totalPending 件待取 · 分布在 $activeStationCount 个快递房"
                totalPending > 0 ->
                    "当前 $totalPending 件待取 · 待分类快递房"
                else ->
                    "每次打开自动扫描最近 7 天取件短信"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ForwardArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tipX = w * 0.88f
        val tipY = h * 0.38f

        val stem = Path().apply {
            moveTo(w * 0.12f, h * 0.84f)
            cubicTo(
                w * 0.14f, h * 0.44f,
                w * 0.42f, tipY,
                tipX, tipY
            )
        }
        val head = Path().apply {
            moveTo(w * 0.58f, h * 0.14f)
            lineTo(tipX, tipY)
            lineTo(w * 0.58f, h * 0.62f)
        }
        val stroke = Stroke(
            width = 1.9.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawPath(path = stem, color = tint, style = stroke)
        drawPath(path = head, color = tint, style = stroke)
    }
}

@Composable
private fun EmptyPickupStateCard(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onQuickAddClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = AppleCorners.OuterCard)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "暂无待取快递",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasSmsPermission) {
                "每次打开自动扫描最近 7 天取件短信，也可点击右下角「+」手动添加。"
            } else {
                "授予短信读取权限后，每次打开将自动扫描最近 7 天的取件短信。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = if (hasSmsPermission) onQuickAddClick else onRequestSmsPermission,
            shape = RoundedCornerShape(AppleCorners.InnerCard)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasSmsPermission) "添加取件码" else "授权短信读取权限",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
