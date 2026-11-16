package com.example.pickupcode.ui.home

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
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val NUMBER_TOKEN_REGEX = Regex("""\d+""")

/**
 * 同一个快递房内的取件码按照数字大小从小到大自然排序
 */
private val NaturalPickupCodeComparator = Comparator<PickupItemEntity> { a, b ->
    val numsA = NUMBER_TOKEN_REGEX.findAll(a.code).map { it.value.toLongOrNull() ?: 0L }.toList()
    val numsB = NUMBER_TOKEN_REGEX.findAll(b.code).map { it.value.toLongOrNull() ?: 0L }.toList()
    val minSize = minOf(numsA.size, numsB.size)
    for (i in 0 until minSize) {
        val cmp = numsA[i].compareTo(numsB[i])
        if (cmp != 0) return@Comparator cmp
    }
    if (numsA.size != numsB.size) {
        return@Comparator numsA.size.compareTo(numsB.size)
    }
    a.code.compareTo(b.code, ignoreCase = true)
}

@Composable
fun HomeScreen(
    items: List<PickupItemEntity>,
    stationRules: List<StationRule>,
    selectedItemIds: Set<Long>,
    showTaobaoIdentityBanner: Boolean,
    showCainiaoIdentityBanner: Boolean,
    showPinduoduoIdentityBanner: Boolean,
    hasSmsPermission: Boolean,
    onOpenSettings: () -> Unit,
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
                    items = groupItems.sortedWith(NaturalPickupCodeComparator)
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
            HomeTopHeader(
                totalPending = pendingItems.size,
                activeStationCount = activeStationCount,
                hasSmsPermission = hasSmsPermission,
                onOpenSettings = onOpenSettings,
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
                    selectedItemIds = selectedItemIds,
                    onToggleSelectItem = onToggleSelectItem,
                    onQuickMarkPickedUp = onQuickMarkPickedUp,
                    onQuickDeleteItem = onQuickDeleteItem,
                    onLongPressItem = onLongPressItem,
                    onClickMoreActions = onClickMoreActions,
                    onClickProductRemark = onClickProductRemark
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
    onOpenSettings: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    isDark: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "取件码",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!hasSmsPermission) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppleCorners.Pill))
                            .background(statusAmber.copy(alpha = 0.15f))
                            .clickable(onClick = onRequestSmsPermission)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusAmber)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "点击授权短信读取",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = statusAmber
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppleCorners.Pill))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSettings()
                    }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = if (totalPending > 0) {
                "当前 $totalPending 件待取 · 分布在 $activeStationCount 个快递房"
            } else {
                "每次打开自动扫描最近 7 天取件短信"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
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
                "每次打开应用会自动扫描最近 7 天的快递短信并排除已取包裹。您也可以点击右下角「+」手动添加或粘贴短信。"
            } else {
                "请先授予短信读取权限，以便每次打开应用时自动提取最近 7 天的取件码。"
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
