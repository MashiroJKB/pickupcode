package com.example.pickupcode.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.pickupcode.R
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.components.AppBrandIcon
import com.example.pickupcode.ui.components.IdentityCodeType
import com.example.pickupcode.ui.components.StationBadgeSymbol
import com.example.pickupcode.ui.home.SpringGlassDialog
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.AppleGreenDark
import com.example.pickupcode.ui.theme.AppleGreenLight
import com.example.pickupcode.ui.theme.AppleOrangeDark
import com.example.pickupcode.ui.theme.AppleOrangeLight
import com.example.pickupcode.ui.theme.AppleRedDark
import com.example.pickupcode.ui.theme.AppleRedLight
import com.example.pickupcode.ui.theme.PickupCodeMonospaceStyle
import com.example.pickupcode.ui.theme.clipAppleContinuous
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.ui.theme.liquidGlassInnerCard
import com.example.pickupcode.ui.theme.stationAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val GITHUB_REPO_URL = "https://github.com/MashiroJKB/pickupcode"
private const val GITHUB_RELEASES_URL = "https://github.com/MashiroJKB/pickupcode/releases"
private const val LANZOU_UPDATE_URL = "https://mashirojk.lanzoub.com/b00ctlhpta"
private const val LANZOU_PASSWORD = "43tf"

@Composable
fun SettingsScreen(
    items: List<PickupItemEntity>,
    settings: AppSettings,
    hasSmsPermission: Boolean,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onToggleCainiaoAppBanner: (Boolean) -> Unit,
    onToggleCainiaoBanner: (Boolean) -> Unit,
    onToggleTaobaoBanner: (Boolean) -> Unit,
    onTogglePinduoduoBanner: (Boolean) -> Unit,
    onToggleJdBanner: (Boolean) -> Unit,
    onAddStation: (displayName: String, badgeNumber: Int?, keywords: List<String>) -> Unit,
    onDeleteStation: (stationNumber: Int) -> Unit,
    onReorderStations: (List<StationRule>) -> Unit,
    onAddStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onRemoveStationKeyword: (stationNumber: Int, keyword: String) -> Unit,
    onUpdateStationRuleInfo: (stationNumber: Int, newName: String, badgeNumber: Int?, keywords: List<String>, pendingNewKeyword: String) -> Unit,
    onResetStationRules: () -> Unit,
    onExportStationRules: () -> Unit,
    onImportStationRules: (String) -> Unit,
    onRestorePickedUpItem: (PickupItemEntity) -> Unit,
    onDeletePickedUpItem: (PickupItemEntity) -> Unit,
    onFeedbackMessage: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val statusAmber = if (isDark) AppleOrangeDark else AppleOrangeLight
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    val appVersionName = remember(context) {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName.orEmpty().ifBlank { "2.6.3" }
        } catch (_: Exception) {
            "2.6.3"
        }
    }

    var editingStationNumber by remember { mutableStateOf<Int?>(null) }
    var showAddStationDialog by remember { mutableStateOf(false) }
    var showImportRulesDialog by remember { mutableStateOf(false) }
    var showResetRulesConfirmDialog by remember { mutableStateOf(false) }
    var showPickedUpHistoryDialog by remember { mutableStateOf(false) }
    var showSupportPosterDialog by remember { mutableStateOf(false) }

    // 支持按住拖动排序的本地规则列表状态
    var localRules by remember { mutableStateOf(settings.stationRules) }
    var draggingStationNumber by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemRowHeightPx by remember { mutableIntStateOf(with(density) { 52.dp.roundToPx() }) }
    val itemSpacingPx = remember(density) { with(density) { 9.dp.toPx() } }
    val latestOnReorderStations by rememberUpdatedState(onReorderStations)
    val latestPersistedRules by rememberUpdatedState(settings.stationRules)

    LaunchedEffect(settings.stationRules) {
        if (draggingStationNumber == null) {
            localRules = settings.stationRules
        }
    }

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
            bottom = 28.dp + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部栏：仅保留带灰色底色圆圈的小于号返回功能（小于号严格几何居中），「设置」二字静态展示无点击联动
        item(key = "settings_header") {
            val iconColor = MaterialTheme.colorScheme.onBackground
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
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isDark) 0.14f else 0.08f
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val w = size.width
                        val h = size.height
                        // 严格以圆心 (0.50w, 0.50h) 对称绘制小于号 `<`
                        val xLeft = w * 0.32f
                        val xRight = w * 0.68f
                        val yTop = h * 0.16f
                        val yMid = h * 0.50f
                        val yBottom = h * 0.84f
                        val path = Path().apply {
                            moveTo(xRight, yTop)
                            lineTo(xLeft, yMid)
                            lineTo(xRight, yBottom)
                        }
                        drawPath(
                            path = path,
                            color = iconColor,
                            style = Stroke(
                                width = 2.3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    softWrap = false,
                    maxLines = 1
                )
            }
        }

        // 1. 历史包裹入口卡片（永久保存取件记录，大标题无前置图标）
        item(key = "history_packages_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .liquidGlass(
                        cornerRadius = AppleCorners.OuterCard,
                        tintColor = statusGreen
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showPickedUpHistoryDialog = true
                    }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "历史包裹",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "取件记录永久保存 · 点击查看",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$pickedUpCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = statusGreen,
                            softWrap = false,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "件",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = "查看历史包裹",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 2. 仅当未开启短信读取权限时展示授权卡片
        if (!hasSmsPermission) {
            item(key = "sms_permission_card") {
                PermissionStatusCard(
                    title = "短信读取权限未开启",
                    subtitle = "开启后自动扫描最近 7 天短信",
                    accentColor = statusAmber,
                    buttonText = "开启权限",
                    onClick = onRequestSmsPermission,
                    modifier = Modifier.animateItem()
                )
            }
        }

        // 3. 识别规则（支持按住并拖动排序，前缀 ①② 等与实心圆可在笔图标弹窗内自定义）
        item(key = "station_rules_card") {
            Column(
                modifier = Modifier
                    .animateItem()
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(AppleCorners.PillShape)
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
                                color = MaterialTheme.colorScheme.primary,
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(AppleCorners.PillShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showResetRulesConfirmDialog = true
                                }
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 识别规则一键导出 / 导入口令操作栏
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = AppleCorners.SmallElementShape
                            )
                            .clip(AppleCorners.SmallElementShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExportStationRules()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = "导出规则口令",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "导出规则口令",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            softWrap = false,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = AppleCorners.SmallElementShape
                            )
                            .clip(AppleCorners.SmallElementShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showImportRulesDialog = true
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = "导入规则口令",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "导入规则口令",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (localRules.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "暂无快递房规则，可点击上方添加或重置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    localRules.forEach { rule ->
                        key(rule.stationNumber) {
                            val isDragging = draggingStationNumber == rule.stationNumber
                            val accent = stationAccentColor(rule.badgeNumber ?: rule.stationNumber, isDark)
                            val dragScale by animateFloatAsState(
                                targetValue = if (isDragging) 1.03f else 1f,
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 500f
                                ),
                                label = "station_drag_scale"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .onGloballyPositioned { coords ->
                                        if (coords.size.height > 0) {
                                            itemRowHeightPx = coords.size.height
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationY = dragOffsetY
                                        }
                                        scaleX = dragScale
                                        scaleY = dragScale
                                    }
                                    .liquidGlassInnerCard(
                                        cornerRadius = AppleCorners.InnerCard,
                                        accentTint = accent
                                    )
                                    .pointerInput(rule.stationNumber) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                draggingStationNumber = rule.stationNumber
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val stepPx = itemRowHeightPx + itemSpacingPx
                                                if (stepPx > 0f) {
                                                    while (dragOffsetY > stepPx * 0.55f) {
                                                        val idx = localRules.indexOfFirst { it.stationNumber == rule.stationNumber }
                                                        if (idx == -1 || idx >= localRules.lastIndex) break
                                                        val mutable = localRules.toMutableList()
                                                        val moved = mutable.removeAt(idx)
                                                        mutable.add(idx + 1, moved)
                                                        localRules = mutable
                                                        dragOffsetY -= stepPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    while (dragOffsetY < -stepPx * 0.55f) {
                                                        val idx = localRules.indexOfFirst { it.stationNumber == rule.stationNumber }
                                                        if (idx <= 0) break
                                                        val mutable = localRules.toMutableList()
                                                        val moved = mutable.removeAt(idx)
                                                        mutable.add(idx - 1, moved)
                                                        localRules = mutable
                                                        dragOffsetY += stepPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggingStationNumber = null
                                                dragOffsetY = 0f
                                                if (localRules != latestPersistedRules) {
                                                    latestOnReorderStations(localRules)
                                                }
                                            },
                                            onDragCancel = {
                                                draggingStationNumber = null
                                                dragOffsetY = 0f
                                                if (localRules != latestPersistedRules) {
                                                    latestOnReorderStations(localRules)
                                                }
                                            }
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StationBadgeSymbol(
                                        badgeNumber = rule.badgeNumber,
                                        accentColor = accent
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = rule.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                                draggingStationNumber = null
                                                dragOffsetY = 0f
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
            }
        }

        // 4. 身份码跳转链路卡片（菜鸟APP、菜鸟身份码、淘宝身份码、拼多多取件码、京东身份码五开关独立控制）
        item(key = "identity_code_switches_card") {
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(18.dp)
            ) {
                Text(
                    text = "身份码跳转链路",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    IdentitySwitchRow(
                        type = IdentityCodeType.CAINIAO_APP,
                        checked = settings.showCainiaoAppIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCainiaoAppBanner(it)
                        }
                    )

                    IdentitySwitchRow(
                        type = IdentityCodeType.CAINIAO,
                        checked = settings.showCainiaoIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleCainiaoBanner(it)
                        }
                    )

                    IdentitySwitchRow(
                        type = IdentityCodeType.TAOBAO,
                        checked = settings.showTaobaoIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTaobaoBanner(it)
                        }
                    )

                    IdentitySwitchRow(
                        type = IdentityCodeType.PINDUODUO,
                        checked = settings.showPinduoduoIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePinduoduoBanner(it)
                        }
                    )

                    IdentitySwitchRow(
                        type = IdentityCodeType.JD,
                        checked = settings.showJdIdentityBanner,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleJdBanner(it)
                        }
                    )
                }
            }
        }

        // 5. 关于（版本号 + GitHub 开源入口，不展示具体 URL，点击调用默认浏览器打开）
        item(key = "about_card") {
            SettingsLinkCard(
                badgeAccent = MaterialTheme.colorScheme.primary,
                title = "关于",
                badgeText = "v$appVersionName",
                subtitle = "GitHub 开源项目",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!openUrlInDefaultBrowser(context, GITHUB_REPO_URL)) {
                        onFeedbackMessage("未检测到可用浏览器")
                    }
                },
                modifier = Modifier.animateItem()
            )
        }

        // 6. 检查更新（提供 GitHub Release 与蓝奏云双渠道，不展示具体 URL，点击调用默认浏览器打开）
        item(key = "check_update_card") {
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .padding(18.dp)
            ) {
                Text(
                    text = "检查更新",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    UpdateChannelRow(
                        title = "GitHub Release",
                        badgeText = null,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (!openUrlInDefaultBrowser(context, GITHUB_RELEASES_URL)) {
                                onFeedbackMessage("未检测到可用浏览器")
                            }
                        }
                    )

                    UpdateChannelRow(
                        title = "蓝奏云",
                        badgeText = "密码 $LANZOU_PASSWORD",
                        accentColor = statusGreen,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("蓝奏云密码", LANZOU_PASSWORD))
                            if (openUrlInDefaultBrowser(context, LANZOU_UPDATE_URL)) {
                                onFeedbackMessage("已复制密码 $LANZOU_PASSWORD，正在打开蓝奏云")
                            } else {
                                onFeedbackMessage("已复制密码 $LANZOU_PASSWORD，未检测到浏览器")
                            }
                        }
                    )
                }
            }
        }

        // 7. 支持（点击居中弹出支付宝扫码领红包海报）
        item(key = "support_card") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = AppleCorners.OuterCard)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSupportPosterDialog = true
                    }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "支持",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "扫码领红包，支持开发者",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "打开支持页面",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    // 快递房识别规则编辑弹窗
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
            onSaveRuleInfo = { newName, newBadgeNum, pendingKw ->
                onUpdateStationRuleInfo(
                    activeEditingRule.stationNumber,
                    newName,
                    newBadgeNum,
                    activeEditingRule.keywords,
                    pendingKw
                )
            }
        )
    }

    // 添加新快递房弹窗
    if (showAddStationDialog) {
        AddNewStationDialog(
            onDismiss = { showAddStationDialog = false },
            onConfirm = { name, badgeNum, keywords ->
                onAddStation(name, badgeNum, keywords)
                showAddStationDialog = false
            }
        )
    }

    // 导入识别规则口令弹窗
    if (showImportRulesDialog) {
        ImportStationRulesDialog(
            onDismiss = { showImportRulesDialog = false },
            onImport = { tokenText ->
                onImportStationRules(tokenText)
                showImportRulesDialog = false
            },
            onFeedbackMessage = onFeedbackMessage
        )
    }

    // 历史包裹记录弹窗
    if (showPickedUpHistoryDialog) {
        PickedUpHistoryDialog(
            pickedUpItems = pickedUpItems,
            stationRules = settings.stationRules,
            onDismiss = { showPickedUpHistoryDialog = false },
            onRestoreItem = onRestorePickedUpItem,
            onDeleteItem = onDeletePickedUpItem
        )
    }

    // 支持弹窗（支付宝扫码领红包海报）
    if (showSupportPosterDialog) {
        SupportPosterDialog(
            onDismiss = { showSupportPosterDialog = false },
            onFeedbackMessage = onFeedbackMessage
        )
    }

    // 重置默认识别规则确认弹窗（防止误触直接清空自定义配置）
    if (showResetRulesConfirmDialog) {
        ResetStationRulesConfirmDialog(
            onDismiss = { showResetRulesConfirmDialog = false },
            onConfirmReset = {
                showResetRulesConfirmDialog = false
                onResetStationRules()
            }
        )
    }
}

@Composable
private fun SettingsLinkCard(
    badgeAccent: Color,
    title: String,
    badgeText: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = AppleCorners.OuterCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(AppleCorners.PillShape)
                        .background(badgeAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeAccent,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "在默认浏览器打开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun UpdateChannelRow(
    title: String,
    badgeText: String?,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassInnerCard(
                cornerRadius = AppleCorners.InnerCard,
                accentTint = accentColor
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(AppleCorners.PillShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "在默认浏览器打开 $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

private fun openUrlInDefaultBrowser(
    context: Context,
    url: String
): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = AppleCorners.OuterCard)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(AppleCorners.PillShape)
                .background(accentColor.copy(alpha = 0.16f))
                .border(0.8.dp, accentColor.copy(alpha = 0.4f), AppleCorners.PillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IdentitySwitchRow(
    type: IdentityCodeType,
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
                accentTint = if (checked) type.primaryColor else Color.Unspecified
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            AppBrandIcon(
                type = type,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = type.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = type.primaryColor
            )
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
    onSaveRuleInfo: (newName: String, badgeNumber: Int?, pendingNewKeyword: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var displayName by remember(rule.stationNumber) { mutableStateOf(rule.displayName) }
    var badgeNumberText by remember(rule.stationNumber) {
        mutableStateOf(rule.badgeNumber?.toString().orEmpty())
    }
    var newKeyword by remember(rule.stationNumber) { mutableStateOf("") }

    val previewBadgeNum = badgeNumberText.trim().toIntOrNull()?.takeIf { it in 1..20 }
    val accent = stationAccentColor(previewBadgeNum ?: rule.stationNumber, isDark)

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StationBadgeSymbol(
                    badgeNumber = previewBadgeNum,
                    accentColor = accent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "配置识别规则",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )
            }
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

            // 在快递房显示名称后新添加一栏：输入 1 即显示 ①，留空则为实心等大圆
            val isBadgeInvalid = badgeNumberText.isNotEmpty() && previewBadgeNum == null
            OutlinedTextField(
                value = badgeNumberText,
                onValueChange = { input ->
                    badgeNumberText = input.filter { it.isDigit() }.take(2)
                },
                label = { Text("序号（1~20，留空为实心圆）", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = isBadgeInvalid,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it.take(20) },
                label = { Text("新增匹配关键词", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                trailingIcon = {
                    if (newKeyword.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onAddKeyword(newKeyword.trim())
                                newKeyword = ""
                            },
                            shape = AppleCorners.SmallElementShape
                        ) {
                            Text("添加", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                        }
                    }
                },
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "当前匹配关键词",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp, max = 160.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f),
                        shape = AppleCorners.InnerCardShape
                    )
                    .border(
                        width = 0.7.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.06f),
                        shape = AppleCorners.InnerCardShape
                    )
                    .clip(AppleCorners.InnerCardShape)
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
                                    .clip(AppleCorners.PillShape)
                                    .background(accent.copy(alpha = if (isDark) 0.20f else 0.12f))
                                    .padding(start = 11.dp, end = 7.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                Text(
                                    text = kw,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                TextButton(
                    onClick = onDismiss,
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    enabled = badgeNumberText.isEmpty() || previewBadgeNum != null,
                    onClick = {
                        val finalName = displayName.trim().ifEmpty { rule.displayName }
                        onSaveRuleInfo(finalName, previewBadgeNum, newKeyword.trim())
                        onDismiss()
                    },
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("保存", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AddNewStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (displayName: String, badgeNumber: Int?, keywords: List<String>) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var displayName by remember { mutableStateOf("") }
    var badgeNumberText by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("") }
    val previewBadgeNum = badgeNumberText.trim().toIntOrNull()?.takeIf { it in 1..20 }
    val isBadgeInvalid = badgeNumberText.isNotEmpty() && previewBadgeNum == null
    val previewAccent = stationAccentColor(previewBadgeNum ?: 1, isDark)

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StationBadgeSymbol(
                    badgeNumber = previewBadgeNum,
                    accentColor = previewAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "添加快递房",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )
            }
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
                value = badgeNumberText,
                onValueChange = { input ->
                    badgeNumberText = input.filter { it.isDigit() }.take(2)
                },
                label = { Text("序号（1~20，留空为实心圆）", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = isBadgeInvalid,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = keywordsText,
                onValueChange = { keywordsText = it },
                label = { Text("匹配关键词（留空使用默认）", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("多个词用空格或逗号分隔", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    enabled = !isBadgeInvalid,
                    onClick = {
                        val kws = keywordsText
                            .split(Regex("""[、,，/\s]+"""))
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onConfirm(displayName.trim(), previewBadgeNum, kws)
                    },
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("添加", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ResetStationRulesConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmReset: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "恢复默认识别规则？",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "将恢复为默认 1～8 号快递房规则\n当前自定义名称与关键词将被覆盖",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.buttonColors(containerColor = deleteRed),
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text(
                        text = "确认重置",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportStationRulesDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onFeedbackMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var inputToken by remember { mutableStateOf("") }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "导入识别规则",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )

                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip
                        val clipText = if (clip != null && clip.itemCount > 0) {
                            clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        } else {
                            ""
                        }
                        if (clipText.isNotBlank()) {
                            inputToken = clipText
                        } else {
                            onFeedbackMessage("剪贴板暂无内容")
                        }
                    },
                    shape = AppleCorners.SmallElementShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("粘贴剪贴板", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = inputToken,
                onValueChange = { inputToken = it },
                placeholder = { Text("在此粘贴【取件码识别规则】口令...") },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(AppleCorners.InnerCard),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("取消", softWrap = false, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    enabled = inputToken.isNotBlank(),
                    onClick = { onImport(inputToken) },
                    shape = AppleCorners.InnerCardShape
                ) {
                    Text("导入规则", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
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
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val sameYearFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val rulesByNum = remember(stationRules) { stationRules.associateBy { it.stationNumber } }
    val deleteRed = if (isDark) AppleRedDark else AppleRedLight

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "历史包裹",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        maxLines = 1
                    )
                    Text(
                        text = "共 ${pickedUpItems.size} 件 · 永久保存",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        text = "暂无历史包裹记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = pickedUpItems,
                        key = { it.id }
                    ) { item ->
                        val rule = rulesByNum[item.stationNumber]
                        val badgeNum = if (item.stationNumber > 0) {
                            if (rule != null) rule.badgeNumber else item.stationNumber
                        } else {
                            null
                        }
                        val accent = stationAccentColor(badgeNum ?: item.stationNumber, isDark)
                        val stationName = if (item.stationNumber > 0) {
                            rule?.displayName ?: "${item.stationNumber}号快递房"
                        } else {
                            "待分类快递房"
                        }
                        val timestamp = item.pickedUpAt ?: item.updatedAt
                        val timeText = remember(timestamp, currentYear) {
                            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                            if (cal.get(Calendar.YEAR) == currentYear) {
                                sameYearFormat.format(Date(timestamp))
                            } else {
                                fullDateFormat.format(Date(timestamp))
                            }
                        }
                        val codeFontSize = when {
                            item.code.length >= 14 -> 15.sp
                            item.code.length >= 11 -> 17.sp
                            else -> 19.sp
                        }

                        Column(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .liquidGlassInnerCard(
                                    cornerRadius = AppleCorners.InnerCard,
                                    accentTint = accent
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // 第一行：快递房徽章与名称 + 完整取件时间，确保互不挤压
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StationBadgeSymbol(
                                        badgeNumber = badgeNum,
                                        accentColor = accent,
                                        isUnclassified = item.stationNumber == 0
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stationName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 第二行：完整取件码 + 恢复/删除操作按钮
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.code,
                                    style = PickupCodeMonospaceStyle.copy(
                                        fontSize = codeFontSize,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(AppleCorners.PillShape)
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
                                            color = MaterialTheme.colorScheme.primary,
                                            softWrap = false,
                                            maxLines = 1
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

                            // 第三行（可选）：独立全宽商品备注，避免与右侧按钮互相挤压截断
                            if (item.productName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportPosterDialog(
    onDismiss: () -> Unit,
    onFeedbackMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val statusGreen = if (isDark) AppleGreenDark else AppleGreenLight
    val configuration = LocalConfiguration.current
    val maxPosterHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * 0.56f).dp.coerceIn(280.dp, 460.dp)
    }
    var isSaving by remember { mutableStateOf(false) }
    var saveSucceeded by remember { mutableStateOf(false) }

    SpringGlassDialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "支持",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxPosterHeight)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_support_alipay),
                    contentDescription = "支付宝扫码领红包",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .aspectRatio(705f / 1024f)
                        .clipAppleContinuous(AppleCorners.InnerCard)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val saveBtnBg by animateColorAsState(
                targetValue = if (saveSucceeded) {
                    statusGreen.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "support_save_btn_bg"
            )
            val saveBtnFg by animateColorAsState(
                targetValue = if (saveSucceeded) statusGreen else Color.White,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "support_save_btn_fg"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = saveBtnBg, shape = AppleCorners.InnerCardShape)
                    .clip(AppleCorners.InnerCardShape)
                    .clickable(enabled = !isSaving) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isSaving = true
                        scope.launch {
                            val saved = saveSupportPosterToGallery(context)
                            isSaving = false
                            saveSucceeded = saved
                            onFeedbackMessage(
                                if (saved) "已保存到相册，可打开支付宝扫一扫" else "保存失败，请截图后在支付宝扫码"
                            )
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (saveSucceeded) Icons.Rounded.Check else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = saveBtnFg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isSaving -> "正在保存..."
                        saveSucceeded -> "已保存 · 可打开支付宝扫码"
                        else -> "保存图片到相册"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = saveBtnFg,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private suspend fun saveSupportPosterToGallery(context: Context): Boolean =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = "pickupcode_support_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/取件码")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { out ->
                val rawCopied = runCatching {
                    context.resources.openRawResource(R.drawable.img_support_alipay).use { input ->
                        input.copyTo(out)
                    }
                    true
                }.getOrDefault(false)
                if (!rawCopied) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_support_alipay)
                        ?: return@withContext false
                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                    } finally {
                        bitmap.recycle()
                    }
                }
            } ?: return@withContext false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
            }
            true
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }
