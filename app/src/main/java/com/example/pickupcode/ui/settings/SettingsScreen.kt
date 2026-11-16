package com.example.pickupcode.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
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

@Composable
fun SettingsScreen(
    items: List<PickupItemEntity>,
    settings: AppSettings,
    hasSmsPermission: Boolean,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onToggleTaobaoBanner: (Boolean) -> Unit,
    onToggleCainiaoBanner: (Boolean) -> Unit,
    onAddStation: (displayName: String, keywords: List<String>) -> Unit,
    onDeleteStation: (stationNumber: Int) -> Unit,
    onAddStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onRemoveStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onUpdateStationDisplayName: (stationNumber: Int, newName: String, keywords: List<String>) -> Unit,
    onResetStationRules: () -> Unit,
    onRestorePickedUpItem: (PickupItemEntity) -> Unit,
    onDeletePickedUpItem: (PickupItemEntity) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val statusGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    var editingStationNumber by remember { mutableStateOf<Int?>(null) }
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
        // 顶部栏：优雅一体化圆角返回箭头 + 「设置」标题（固定 40.dp 行高，带非线性弹簧按压反馈）
        item(key = "settings_header") {
            var isBackPressed by remember { mutableStateOf(false) }
            val backScale by animateFloatAsState(
                targetValue = if (isBackPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "back_header_scale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .scale(backScale)
                        .clip(RoundedCornerShape(AppleCorners.SmallElement))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isBackPressed = true
                                    tryAwaitRelease()
                                    isBackPressed = false
                                },
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBack()
                                }
                            )
                        }
                        .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (-4).dp)
                    )
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.offset(x = (-4).dp)
                    )
                }
            }
        }

        // 1. 取件统计与离线隐私承诺卡片
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

        // 2. 仅当未开启短信权限时展示授权卡片
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

        // 3. 识别规则（一级菜单仅展示快递房名称与编辑/删除圆形图标，详细匹配关键词点击笔图标在弹窗中查看与编辑）
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

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    settings.stationRules.forEach { rule ->
                        val accent = stationAccentColor(rule.stationNumber, isDark)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassInnerCard(
                                    cornerRadius = AppleCorners.InnerCard,
                                    accentTint = accent
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    editingStationNumber = rule.stationNumber
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = rule.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 圆形笔图标按钮
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.14f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            editingStationNumber = rule.stationNumber
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "编辑识别规则",
                                        tint = accent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // 圆形删除按钮
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(deleteRed.copy(alpha = 0.12f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    }
                }
            }
        }

        // 4. 身份码跳转链路卡片（支持同时打开淘宝身份码与菜鸟身份码开关）
        item(key = "identity_code_switches_card") {
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    IdentitySwitchRow(
                        title = "淘宝身份码",
                        accentColor = TaobaoOrangeStart,
                        checked = settings.showTaobaoIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTaobaoBanner(it)
                        }
                    )

                    IdentitySwitchRow(
                        title = "菜鸟身份码",
                        accentColor = MaterialTheme.colorScheme.primary,
                        checked = settings.showCainiaoIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCainiaoBanner(it)
                        }
                    )
                }
            }
        }
    }

    // 快递房识别规则编辑弹窗（响应式读取最新关键词列表，在新增匹配关键词下方展示当前已配置关键词）
    val activeEditingRule = editingStationNumber?.let { num ->
        settings.stationRules.firstOrNull { it.stationNumber == num }
    }
    if (activeEditingRule != null) {
        EditStationRuleDialog(
            rule = activeEditingRule,
            onDismiss = { editingStationNumber = null },
            onAddKeyword = { newKw ->
                onAddStationKeyword(activeEditingRule.stationNumber, newKw)
            },
            onRemoveKeyword = { kw ->
                onRemoveStationKeyword(activeEditingRule.stationNumber, kw)
            },
            onSaveDisplayName = { newName ->
                onUpdateStationDisplayName(
                    activeEditingRule.stationNumber,
                    newName,
                    activeEditingRule.keywords
                )
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
private fun IdentitySwitchRow(
    title: String,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassInnerCard(
                cornerRadius = AppleCorners.InnerCard,
                accentTint = if (checked) accentColor else Color.Unspecified
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditStationRuleDialog(
    rule: StationRule,
    onDismiss: () -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onSaveDisplayName: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accent = stationAccentColor(rule.stationNumber, isDark)
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
                trailingIcon = {
                    if (newKeyword.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onAddKeyword(newKeyword.trim())
                                newKeyword = ""
                            }
                        ) {
                            Text("添加", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 在新增匹配关键词下方新开一栏展示详细匹配规则关键词
            Text(
                text = "当前匹配关键词",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp, max = 180.dp)
                    .clip(RoundedCornerShape(AppleCorners.InnerCard))
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
                    )
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (rule.keywords.isEmpty()) {
                    Text(
                        text = "暂无匹配关键词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rule.keywords.forEach { kw ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AppleCorners.Pill))
                                    .background(accent.copy(alpha = if (isDark) 0.20f else 0.12f))
                                    .padding(start = 11.dp, end = 7.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = kw,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "删除关键词 $kw",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .clickable { onRemoveKeyword(kw) }
                                )
                            }
                        }
                    }
                }
            }

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
