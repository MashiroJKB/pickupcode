package com.example.pickupcode.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.components.TaobaoDeepLinkLauncher
import com.example.pickupcode.ui.home.SpringGlassDialog
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight
import com.example.pickupcode.ui.theme.AppleRedDark
import com.example.pickupcode.ui.theme.AppleRedLight
import com.example.pickupcode.ui.theme.PickupCodeMonospaceStyle
import com.example.pickupcode.ui.theme.TaobaoOrangeStart
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.ui.theme.liquidGlassInnerCard
import com.example.pickupcode.ui.theme.stationAccentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    items: List<PickupItemEntity>,
    settings: AppSettings,
    hasSmsPermission: Boolean,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onSelectExitCodeMode: (Boolean) -> Unit,
    onAddStation: (displayName: String, keywords: List<String>) -> Unit,
    onDeleteStation: (stationNumber: Int) -> Unit,
    onAddStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onRemoveStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onUpdateStationDisplayName: (stationNumber: Int, newName: String, keywords: List<String>) -> Unit,
    onResetStationRules: () -> Unit,
    onRestorePickedUpItem: (PickupItemEntity) -> Unit,
    onDeletePickedUpItem: (PickupItemEntity) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val statusGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    var editingStationRule by remember { mutableStateOf<StationRule?>(null) }
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showPickedUpHistoryDialog by remember { mutableStateOf(false) }

    val pendingCount = items.count { !it.isPickedUp }
    val pickedUpItems = remember(items) {
        items.filter { it.isPickedUp }
            .sortedByDescending { it.pickedUpAt ?: it.updatedAt }
    }
    val pickedUpCount = pickedUpItems.size

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部栏：固定 40.dp 行高，与首页顶部栏严格对齐，切换页面绝不移位
        item(key = "settings_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .height(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 1. 取件统计与离线隐私承诺卡片（去除右上角“48h 跨引擎幂等去重”文字）
        item(key = "stats_and_privacy_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = statusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% 纯本地离线存储 · 零联网权限",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusGreen
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBox(
                        title = "待取包裹",
                        value = "$pendingCount",
                        unit = "件",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "7天内已取",
                        value = "$pickedUpCount",
                        unit = "件",
                        accent = statusGreen,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showPickedUpHistoryDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "覆盖快递房",
                        value = "${items.filter { !it.isPickedUp && it.stationNumber > 0 }.map { it.stationNumber }.distinct().size}",
                        unit = "个",
                        accent = TaobaoOrangeStart,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. 仅当未开启短信权限时展示授权卡片（开启权限后不再显示抓取状态卡片）
        if (!hasSmsPermission) {
            item(key = "sms_permission_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                        .padding(18.dp)
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
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusAmber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "监听短信权限未开启",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "授权后每次打开应用自动扫描最近 7 天取件短信",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(statusAmber.copy(alpha = 0.16f))
                                .border(0.8.dp, statusAmber.copy(alpha = 0.4f), RoundedCornerShape(AppleCorners.Pill))
                                .clickable(onClick = onRequestSmsPermission)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "开启权限",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusAmber
                            )
                        }
                    }
                }
            }
        }

        // 3. 识别规则（支持添加/删除快递房、添加/删除各号房关键词）
        item(key = "station_rules_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "识别规则",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 添加快递房按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .clickable { showAddStationDialog = true }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "添加快递房",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "添加快递房",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 重置按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppleCorners.Pill))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .clickable(onClick = onResetStationRules)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "重置默认",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "重置",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    settings.stationRules.forEach { rule ->
                        val accent = stationAccentColor(rule.stationNumber, isDark)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassInnerCard(
                                    cornerRadius = AppleCorners.InnerCard,
                                    accentTint = accent
                                )
                                .padding(12.dp)
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
                                    Text(
                                        text = rule.circleSymbol,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 21.sp
                                        ),
                                        color = accent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 关键词 / 改名圆形笔图标按钮
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.14f))
                                            .clickable { editingStationRule = rule }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "编辑规则与名称",
                                            tint = accent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // 删除该快递房按钮
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(deleteRed.copy(alpha = 0.12f))
                                            .clickable {
                                                onDeleteStation(rule.stationNumber)
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "删除快递房",
                                            tint = deleteRed,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rule.keywords.forEach { kw ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(AppleCorners.Pill))
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = if (isDark) 0.10f else 0.06f
                                                )
                                            )
                                            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Text(
                                            text = kw,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "删除关键词 $kw",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onRemoveStationKeyword(rule.stationNumber, kw)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. 出库码链路选择卡片（淘宝出库码 [默认] / 菜鸟出库码，删除加载示例数据按钮）
        item(key = "exit_code_deeplink_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = TaobaoOrangeStart,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "身份码跳转链路",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 淘宝出库码（默认） vs 菜鸟出库码 切换器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppleCorners.InnerCard))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(4.dp)
                ) {
                    val options = listOf(
                        true to "淘宝出库码",
                        false to "菜鸟出库码"
                    )
                    options.forEach { (isTaobaoMode, label) ->
                        val selected = settings.useTaobaoExitCode == isTaobaoMode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppleCorners.SmallElement))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable {
                                    onSelectExitCodeMode(isTaobaoMode)
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        TaobaoDeepLinkLauncher.launchIdentityCode(
                            context = context,
                            useTaobaoExitCode = settings.useTaobaoExitCode,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    },
                    shape = RoundedCornerShape(AppleCorners.InnerCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (settings.useTaobaoExitCode) "测试唤起淘宝出库码" else "测试唤起菜鸟出库码",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 快递房新增关键词/改名弹窗
    editingStationRule?.let { rule ->
        EditStationRuleDialog(
            rule = rule,
            onDismiss = { editingStationRule = null },
            onAddKeyword = { newKw ->
                onAddStationKeyword(rule.stationNumber, newKw)
            },
            onSaveDisplayName = { newName ->
                onUpdateStationDisplayName(rule.stationNumber, newName, rule.keywords)
            }
        )
    }

    // 添加新快递房弹窗
    if (showAddStationDialog) {
        AddNewStationDialog(
            onDismiss = { showAddStationDialog = false },
            onConfirm = { name, keywords ->
                onAddStation(name, keywords)
                showAddStationDialog = false
            }
        )
    }

    // 7天内已取记录弹窗
    if (showPickedUpHistoryDialog) {
        PickedUpHistoryDialog(
            pickedUpItems = pickedUpItems,
            stationRules = settings.stationRules,
            onDismiss = { showPickedUpHistoryDialog = false },
            onRestoreItem = onRestorePickedUpItem,
            onDeleteItem = onDeletePickedUpItem
        )
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    unit: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .liquidGlassInnerCard(cornerRadius = AppleCorners.InnerCard, accentTint = accent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = accent
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditStationRuleDialog(
    rule: StationRule,
    onDismiss: () -> Unit,
    onAddKeyword: (String) -> Unit,
    onSaveDisplayName: (String) -> Unit
) {
    var displayName by remember(rule.stationNumber) { mutableStateOf(rule.displayName) }
    var newKeyword by remember(rule.stationNumber) { mutableStateOf("") }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "配置 ${rule.circleSymbol} 识别规则",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(18) },
                label = { Text("快递房显示名称") },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it.take(20) },
                label = { Text("新增匹配关键词") },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (displayName.isNotBlank() && displayName != rule.displayName) {
                            onSaveDisplayName(displayName.trim())
                        }
                        if (newKeyword.isNotBlank()) {
                            onAddKeyword(newKeyword.trim())
                        }
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

@Composable
private fun AddNewStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, keywords: List<String>) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("") }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "添加快递房",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(18) },
                label = { Text("快递房名称") },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = keywordsText,
                onValueChange = { keywordsText = it },
                label = { Text("匹配关键词（多个词用空格或逗号分隔）") },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        val kws = keywordsText
                            .split(Regex("""[、,，/\s]+"""))
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onConfirm(displayName.trim(), kws)
                    },
                    shape = RoundedCornerShape(AppleCorners.InnerCard)
                ) {
                    Text("添加", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PickedUpHistoryDialog(
    pickedUpItems: List<PickupItemEntity>,
    stationRules: List<StationRule>,
    onDismiss: () -> Unit,
    onRestoreItem: (PickupItemEntity) -> Unit,
    onDeleteItem: (PickupItemEntity) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val rulesByNum = remember(stationRules) { stationRules.associateBy { it.stationNumber } }
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "7天内已取记录",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "共 ${pickedUpItems.size} 件 · 满 7 天自动清理",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (pickedUpItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp)
                ) {
                    Text(
                        text = "最近 7 天暂无已取件记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = pickedUpItems,
                        key = { it.id }
                    ) { item ->
                        val accent = stationAccentColor(item.stationNumber, isDark)
                        val stationName = if (item.stationNumber > 0) {
                            rulesByNum[item.stationNumber]?.displayName ?: "${item.stationNumber}号快递房"
                        } else {
                            "待分类"
                        }
                        val timeText = dateFormat.format(Date(item.pickedUpAt ?: item.updatedAt))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassInnerCard(
                                    cornerRadius = AppleCorners.InnerCard,
                                    accentTint = accent
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.16f))
                                ) {
                                    Text(
                                        text = item.stationCircleSymbol,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = accent
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = item.code,
                                        style = PickupCodeMonospaceStyle.copy(fontSize = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = false
                                    )
                                    val subInfo = buildString {
                                        append(stationName)
                                        if (item.productName.isNotBlank()) {
                                            append(" · ").append(item.productName)
                                        }
                                        append(" · ").append(timeText)
                                    }
                                    Text(
                                        text = subInfo,
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 恢复至待取按钮
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AppleCorners.Pill))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRestoreItem(item)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                                        contentDescription = "恢复待取",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "恢复",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // 删除按钮
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(deleteRed.copy(alpha = 0.12f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDeleteItem(item)
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "删除记录",
                                        tint = deleteRed,
                                        modifier = Modifier.size(15.dp)
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
