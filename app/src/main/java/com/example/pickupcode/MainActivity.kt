package com.example.pickupcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.service.AppBadgeManager
import com.example.pickupcode.ui.PickupViewModel
import com.example.pickupcode.ui.components.HomeBottomFloatingBar
import com.example.pickupcode.ui.home.AddOrPastePickupDialog
import com.example.pickupcode.ui.home.EditProductRemarkDialog
import com.example.pickupcode.ui.home.HomeScreen
import com.example.pickupcode.ui.home.LongPressQuickActionDialog
import com.example.pickupcode.ui.home.NotificationAndBadgePermissionDialog
import com.example.pickupcode.ui.home.RawMessageDetailDialog
import com.example.pickupcode.ui.settings.SettingsScreen
import com.example.pickupcode.ui.theme.AppleCorners
import com.example.pickupcode.ui.theme.LiquidGlassBackdrop
import com.example.pickupcode.ui.theme.PickupCodeTheme
import com.example.pickupcode.ui.theme.liquidGlass
import com.example.pickupcode.widget.PickupWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: PickupViewModel by viewModels {
        val app = application as PickupApplication
        PickupViewModel.Factory(app.repository, app.settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()

        setContent {
            val isDark = isSystemInDarkTheme()
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark }
                )
                onDispose {}
            }

            PickupCodeTheme(darkTheme = isDark) {
                PickupMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
    }

    /**
     * 适配 120Hz / 最高刷新率显示模式
     */
    private fun enableHighRefreshRate() {
        try {
            val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            val modes = currentDisplay.supportedModes
            val currentMode = currentDisplay.mode
            val bestMode = modes
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: modes.maxByOrNull { it.refreshRate }

            if (bestMode != null) {
                val layoutParams = window.attributes
                if (layoutParams.preferredDisplayModeId != bestMode.modeId) {
                    layoutParams.preferredDisplayModeId = bestMode.modeId
                    layoutParams.preferredRefreshRate = bestMode.refreshRate
                    window.attributes = layoutParams
                }
            }
        } catch (_: Exception) {
        }
    }
}

@Composable
fun PickupMainApp(viewModel: PickupViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val items by viewModel.itemsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val undoableItemIds by viewModel.undoableItemIds.collectAsState()
    val capsuleToast by viewModel.capsuleToastMessage.collectAsState()
    val latestItems by rememberUpdatedState(items)

    var showSettingsScreen by remember { mutableStateOf(false) }
    val homeListState = rememberLazyListState()
    val settingsListState = rememberLazyListState()

    BackHandler(enabled = showSettingsScreen) {
        showSettingsScreen = false
    }

    var hasSmsPermission by remember {
        mutableStateOf(checkSmsPermission(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(AppBadgeManager.hasNotificationPermission(context))
    }

    // 弹窗控制状态
    var activeDetailItem by remember { mutableStateOf<PickupItemEntity?>(null) }
    var activeEditProductItem by remember { mutableStateOf<PickupItemEntity?>(null) }
    var activeLongPressItem by remember { mutableStateOf<PickupItemEntity?>(null) }
    var showAddOrPasteDialog by remember { mutableStateOf(false) }
    var showNotificationBadgeDialog by remember { mutableStateOf(false) }
    var awaitingBadgeDialogAfterStartupPrompt by remember { mutableStateOf(false) }
    var hasTriggeredInitialPermissionFlow by remember { mutableStateOf(false) }

    val startupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val smsGranted = checkSmsPermission(context)
        hasSmsPermission = smsGranted
        hasNotificationPermission = AppBadgeManager.hasNotificationPermission(context)
        if (smsGranted) {
            viewModel.scanRecentSmsOnAppOpen(context)
        }
        AppBadgeManager.updateBadgeCount(context, latestItems.count { !it.isPickedUp }, force = true)
        if (awaitingBadgeDialogAfterStartupPrompt) {
            awaitingBadgeDialogAfterStartupPrompt = false
            if (!settings.badgePermissionPrompted) {
                showNotificationBadgeDialog = true
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val grantedNow = AppBadgeManager.hasNotificationPermission(context)
        hasNotificationPermission = grantedNow
        AppBadgeManager.updateBadgeCount(context, latestItems.count { !it.isPickedUp }, force = true)
        if (!grantedNow) {
            AppBadgeManager.openNotificationAndBadgeSettings(context)
        }
    }

    val requestSmsPermissions = {
        val perms = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        startupPermissionLauncher.launch(perms)
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AppBadgeManager.openNotificationAndBadgeSettings(context)
        }
    }

    // 首次打开：请求短信与通知权限（Android 13+），并在完成后弹出桌面角标权限引导弹窗
    LaunchedEffect(isSettingsLoaded) {
        if (!isSettingsLoaded || hasTriggeredInitialPermissionFlow) return@LaunchedEffect
        hasTriggeredInitialPermissionFlow = true

        val smsNow = checkSmsPermission(context)
        val notifNow = AppBadgeManager.hasNotificationPermission(context)
        hasSmsPermission = smsNow
        hasNotificationPermission = notifNow

        val startupPerms = buildList {
            if (!smsNow) {
                add(Manifest.permission.READ_SMS)
                add(Manifest.permission.RECEIVE_SMS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED &&
                (!smsNow || !settings.badgePermissionPrompted)
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (startupPerms.isNotEmpty()) {
            awaitingBadgeDialogAfterStartupPrompt = !settings.badgePermissionPrompted
            startupPermissionLauncher.launch(startupPerms.toTypedArray())
        } else if (!settings.badgePermissionPrompted) {
            showNotificationBadgeDialog = true
        }
    }

    // 每次打开应用（冷启动与热启动）刷新权限状态、自动扫描最近 7 天短信并同步桌面角标
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val smsNow = checkSmsPermission(context)
                hasSmsPermission = smsNow
                hasNotificationPermission = AppBadgeManager.hasNotificationPermission(context)
                if (smsNow) {
                    viewModel.scanRecentSmsOnAppOpen(context)
                }
                AppBadgeManager.updateBadgeCount(
                    context,
                    latestItems.count { !it.isPickedUp },
                    force = true
                )
                PickupWidgetProvider.notifyWidgetDataChanged(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 根据待取快递数量实时更新桌面应用角标与桌面液态玻璃小组件
    LaunchedEffect(items, settings.stationRules) {
        val pendingCount = items.count { !it.isPickedUp }
        AppBadgeManager.updateBadgeCount(context, pendingCount)
        PickupWidgetProvider.notifyWidgetDataChanged(context)
    }

    val activeSelectedCount = remember(items, selectedItemIds) {
        val pendingIds = items.asSequence().filter { !it.isPickedUp }.map { it.id }.toSet()
        selectedItemIds.count { it in pendingIds }
    }

    val settingsProgress by animateFloatAsState(
        targetValue = if (showSettingsScreen) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "settings_transition_progress"
    )

    LiquidGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. 待取件主页图层
            if (settingsProgress < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (1f - settingsProgress).coerceIn(0f, 1f)
                            translationX = -settingsProgress * 24.dp.toPx()
                        }
                ) {
                    HomeScreen(
                        items = items,
                        stationRules = settings.stationRules,
                        selectedItemIds = selectedItemIds,
                        showTaobaoIdentityBanner = settings.showTaobaoIdentityBanner,
                        showCainiaoIdentityBanner = settings.showCainiaoIdentityBanner,
                        showPinduoduoIdentityBanner = settings.showPinduoduoIdentityBanner,
                        hasSmsPermission = hasSmsPermission,
                        onOpenSettings = {
                            if (!showSettingsScreen) showSettingsScreen = true
                        },
                        onRequestSmsPermission = requestSmsPermissions,
                        onQuickAddClick = { showAddOrPasteDialog = true },
                        onToggleSelectItem = viewModel::toggleSelectItem,
                        onQuickMarkPickedUp = viewModel::markSingleItemPickedUp,
                        onQuickDeleteItem = viewModel::deleteItem,
                        onLongPressItem = { activeLongPressItem = it },
                        onClickMoreActions = { activeDetailItem = it },
                        onClickProductRemark = { activeEditProductItem = it },
                        onFeedbackMessage = viewModel::showCapsuleToast,
                        listState = homeListState
                    )

                    HomeBottomFloatingBar(
                        selectedCount = activeSelectedCount,
                        canUndo = undoableItemIds.isNotEmpty(),
                        onConfirmPickedUp = viewModel::confirmSelectedPickedUp,
                        onUndoPickedUp = viewModel::undoRecentPickedUp,
                        onQuickAddClick = { showAddOrPasteDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    )
                }
            }

            // 2. 设置页图层
            if (settingsProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = settingsProgress.coerceIn(0f, 1f)
                            translationX = (1f - settingsProgress) * 32.dp.toPx()
                        }
                ) {
                    SettingsScreen(
                        items = items,
                        settings = settings,
                        hasSmsPermission = hasSmsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onBack = {
                            if (showSettingsScreen) showSettingsScreen = false
                        },
                        onRequestSmsPermission = requestSmsPermissions,
                        onRequestNotificationPermission = requestNotificationPermission,
                        onOpenBadgeSettings = { AppBadgeManager.openNotificationAndBadgeSettings(context) },
                        onToggleTaobaoBanner = viewModel::setShowTaobaoIdentityBanner,
                        onToggleCainiaoBanner = viewModel::setShowCainiaoIdentityBanner,
                        onTogglePinduoduoBanner = viewModel::setShowPinduoduoIdentityBanner,
                        onAddStation = viewModel::addStationRule,
                        onDeleteStation = viewModel::deleteStationRule,
                        onReorderStations = viewModel::reorderStationRules,
                        onAddStationKeyword = viewModel::addStationKeyword,
                        onRemoveStationKeyword = viewModel::removeStationKeyword,
                        onUpdateStationRuleInfo = viewModel::updateStationRuleInfo,
                        onResetStationRules = viewModel::resetStationRules,
                        onExportStationRules = { viewModel.exportStationRulesToClipboard(context) },
                        onImportStationRules = viewModel::importStationRulesFromText,
                        onRestorePickedUpItem = viewModel::restorePickedUpItem,
                        onDeletePickedUpItem = viewModel::deleteItem,
                        listState = settingsListState
                    )
                }
            }

            // 顶部液态玻璃胶囊轻提示
            AnimatedVisibility(
                visible = capsuleToast != null,
                enter = slideInVertically(
                    initialOffsetY = { -it * 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .liquidGlass(cornerRadius = AppleCorners.Pill)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = capsuleToast.orEmpty(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // 1. 原始短信全文与归类弹窗
    activeDetailItem?.let { currentItem ->
        val latestItem = items.firstOrNull { it.id == currentItem.id } ?: currentItem
        RawMessageDetailDialog(
            item = latestItem,
            stationRules = settings.stationRules,
            onDismiss = { activeDetailItem = null },
            onEditProduct = { activeEditProductItem = it },
            onChangeStation = { newStation ->
                viewModel.updateStationNumber(latestItem.id, newStation)
            },
            onDelete = { viewModel.deleteItem(it) }
        )
    }

    // 2. 商品名称快速备注弹窗
    activeEditProductItem?.let { currentItem ->
        val latestItem = items.firstOrNull { it.id == currentItem.id } ?: currentItem
        EditProductRemarkDialog(
            item = latestItem,
            onDismiss = { activeEditProductItem = null },
            onConfirm = { newRemark ->
                viewModel.updateProductRemark(latestItem.id, newRemark)
            }
        )
    }

    // 3. 长按取件码弹出「已取」或「删除」快捷弹窗
    activeLongPressItem?.let { currentItem ->
        val latestItem = items.firstOrNull { it.id == currentItem.id } ?: currentItem
        LongPressQuickActionDialog(
            item = latestItem,
            onDismiss = { activeLongPressItem = null },
            onConfirmPickedUp = viewModel::markSingleItemPickedUp,
            onDelete = viewModel::deleteItem
        )
    }

    // 4. 添加取件码弹窗（默认手动输入取件码）
    if (showAddOrPasteDialog) {
        AddOrPastePickupDialog(
            stationRules = settings.stationRules,
            onDismiss = { showAddOrPasteDialog = false },
            onPasteParse = viewModel::pasteAndParseSms,
            onManualAdd = viewModel::addManualPickupCode
        )
    }

    // 5. 首次打开通知与桌面角标权限引导弹窗
    if (showNotificationBadgeDialog) {
        NotificationAndBadgePermissionDialog(
            hasNotificationPermission = hasNotificationPermission,
            onDismiss = {
                showNotificationBadgeDialog = false
                viewModel.markBadgePermissionPrompted()
            },
            onRequestNotificationPermission = requestNotificationPermission,
            onOpenBadgeSettings = {
                viewModel.markBadgePermissionPrompted()
                AppBadgeManager.openNotificationAndBadgeSettings(context)
            }
        )
    }
}

private fun checkSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
}
