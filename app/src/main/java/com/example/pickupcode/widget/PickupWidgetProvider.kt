package com.example.pickupcode.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.example.pickupcode.MainActivity
import com.example.pickupcode.PickupApplication
import com.example.pickupcode.R
import com.example.pickupcode.ui.components.IdentityCodeType
import com.example.pickupcode.ui.components.TaobaoDeepLinkLauncher
import com.example.pickupcode.ui.components.loadAppleRoundedIconBitmapForWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 桌面液态玻璃小组件 Provider：
 * - 严格锁定 4×4 尺寸（targetCellWidth=4, targetCellHeight=4, minWidth/minHeight=296dp, resizeMode=none），
 *   彻底杜绝拖动缩放带来的部件大小变化与排版挤压；
 * - Android 12+ 使用单一灵活 RemoteViews + RemoteCollectionItems 原子更新列表，零跨进程重绘抖动；
 * - 顶部提供高清 Apple 连续圆角裁剪的菜鸟APP / 菜鸟 / 淘宝 / 拼多多 / 京东图标快捷入口（跟随设置开关动态显隐），绕过 Android 12+ 后台启动限制；
 * - 列表按快递房顺序与货架号自然升序排列，支持直接点击「已取」并同步刷新小组件。
 */
class PickupWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        widgetRefreshScope.launch {
            try {
                val snapshot = loadWidgetSnapshot(appContext)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidgetWithSnapshot(
                        context = appContext,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        snapshot = snapshot,
                        pendingCount = snapshot.totalPendingCount
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        widgetRefreshScope.launch {
            try {
                val snapshot = loadWidgetSnapshot(appContext)
                updateAppWidgetWithSnapshot(
                    context = appContext,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    snapshot = snapshot,
                    pendingCount = snapshot.totalPendingCount
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_LAUNCH_CAINIAO_APP -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.CAINIAO_APP,
                    onFeedbackMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ACTION_LAUNCH_CAINIAO -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.CAINIAO,
                    onFeedbackMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ACTION_LAUNCH_TAOBAO -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.TAOBAO,
                    onFeedbackMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ACTION_LAUNCH_PINDUODUO -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.PINDUODUO,
                    onFeedbackMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ACTION_LAUNCH_JD -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.JD,
                    onFeedbackMessage = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            ACTION_ITEM_CLICK -> {
                val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                val isMarkDone = intent.getBooleanExtra(EXTRA_MARK_DONE, false)
                if (isMarkDone && itemId > 0L) {
                    val app = context.applicationContext as? PickupApplication ?: return
                    val pendingResult = goAsync()
                    widgetRefreshScope.launch {
                        try {
                            app.repository.confirmPickedUpBatch(listOf(itemId))
                            val remaining = app.repository.allItemsFlow.first().count { !it.isPickedUp }
                            notifyWidgetDataChangedSync(context, pendingCountOverride = remaining)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    try {
                        context.startActivity(openIntent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_LAUNCH_CAINIAO_APP = "com.example.pickupcode.widget.ACTION_LAUNCH_CAINIAO_APP"
        const val ACTION_LAUNCH_CAINIAO = "com.example.pickupcode.widget.ACTION_LAUNCH_CAINIAO"
        const val ACTION_LAUNCH_TAOBAO = "com.example.pickupcode.widget.ACTION_LAUNCH_TAOBAO"
        const val ACTION_LAUNCH_PINDUODUO = "com.example.pickupcode.widget.ACTION_LAUNCH_PINDUODUO"
        const val ACTION_LAUNCH_JD = "com.example.pickupcode.widget.ACTION_LAUNCH_JD"
        const val ACTION_ITEM_CLICK = "com.example.pickupcode.widget.ACTION_ITEM_CLICK"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_MARK_DONE = "extra_mark_done"

        private data class WidgetBrandIconCache(
            val cainiaoApp: Bitmap,
            val cainiao: Bitmap,
            val taobao: Bitmap,
            val pinduoduo: Bitmap,
            val jd: Bitmap
        ) {
            fun isValid(): Boolean =
                !cainiaoApp.isRecycled &&
                    !cainiao.isRecycled &&
                    !taobao.isRecycled &&
                    !pinduoduo.isRecycled &&
                    !jd.isRecycled
        }

        private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val iconCacheLock = Any()

        @Volatile
        private var cachedIcons: WidgetBrandIconCache? = null

        private fun getCachedBrandIcons(context: Context): WidgetBrandIconCache {
            val existing = cachedIcons
            if (existing != null && existing.isValid()) {
                return existing
            }
            return synchronized(iconCacheLock) {
                val doubleChecked = cachedIcons
                if (doubleChecked != null && doubleChecked.isValid()) {
                    doubleChecked
                } else {
                    val appContext = context.applicationContext
                    WidgetBrandIconCache(
                        cainiaoApp = loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.CAINIAO_APP, 112),
                        cainiao = loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.CAINIAO, 112),
                        taobao = loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.TAOBAO, 112),
                        pinduoduo = loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.PINDUODUO, 112),
                        jd = loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.JD, 112)
                    ).also { cachedIcons = it }
                }
            }
        }

        private fun updateAppWidgetWithSnapshot(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: WidgetSnapshot,
            pendingCount: Int
        ) {
            val brandIcons = getCachedBrandIcons(context)
            val views = buildWidgetRemoteViews(
                context = context,
                appWidgetId = appWidgetId,
                snapshot = snapshot,
                pendingCount = pendingCount,
                brandIcons = brandIcons
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
            }
        }

        private fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            snapshot: WidgetSnapshot,
            pendingCount: Int,
            brandIcons: WidgetBrandIconCache
        ): RemoteViews {
            val rows = snapshot.rows
            val views = RemoteViews(context.packageName, R.layout.widget_pickup_code)

            views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_DIP, 16f)
            views.setTextViewText(R.id.widget_pending_count, "待取 $pendingCount 件")
            views.setTextViewTextSize(R.id.widget_pending_count, TypedValue.COMPLEX_UNIT_DIP, 12f)
            views.setTextViewTextSize(R.id.widget_empty_view, TypedValue.COMPLEX_UNIT_DIP, 13f)

            // 点击左侧标题打开主应用
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_title_area, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

            // 顶部原生 Apple 连续圆角应用图标（严格按设置页「身份码跳转链路」开关与顺序动态显隐）
            if (snapshot.showCainiaoApp) {
                views.setViewVisibility(R.id.widget_btn_cainiao_app, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_btn_cainiao_app, brandIcons.cainiaoApp)
                views.setOnClickPendingIntent(
                    R.id.widget_btn_cainiao_app,
                    createDirectPackageLaunchOrFallbackPendingIntent(
                        context = context,
                        targetPackage = TaobaoDeepLinkLauncher.CAINIAO_APP_PACKAGE,
                        fallbackBroadcastAction = ACTION_LAUNCH_CAINIAO_APP,
                        requestCode = 100
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_btn_cainiao_app, View.GONE)
            }

            if (snapshot.showCainiao) {
                views.setViewVisibility(R.id.widget_btn_cainiao, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_btn_cainiao, brandIcons.cainiao)
                views.setOnClickPendingIntent(
                    R.id.widget_btn_cainiao,
                    createDirectLaunchOrFallbackPendingIntent(
                        context = context,
                        primaryUri = TaobaoDeepLinkLauncher.CAINIAO_IDENTITY_DEEPLINK,
                        fallbackUri = TaobaoDeepLinkLauncher.TAOBAO_IDENTITY_DEEPLINK,
                        targetPackage = TaobaoDeepLinkLauncher.TAOBAO_PACKAGE,
                        allowImplicitFallback = false,
                        fallbackBroadcastAction = ACTION_LAUNCH_CAINIAO,
                        requestCode = 101
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_btn_cainiao, View.GONE)
            }

            if (snapshot.showTaobao) {
                views.setViewVisibility(R.id.widget_btn_taobao, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_btn_taobao, brandIcons.taobao)
                views.setOnClickPendingIntent(
                    R.id.widget_btn_taobao,
                    createDirectLaunchOrFallbackPendingIntent(
                        context = context,
                        primaryUri = TaobaoDeepLinkLauncher.TAOBAO_IDENTITY_DEEPLINK,
                        fallbackUri = TaobaoDeepLinkLauncher.CAINIAO_IDENTITY_DEEPLINK,
                        targetPackage = TaobaoDeepLinkLauncher.TAOBAO_PACKAGE,
                        allowImplicitFallback = false,
                        fallbackBroadcastAction = ACTION_LAUNCH_TAOBAO,
                        requestCode = 102
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_btn_taobao, View.GONE)
            }

            if (snapshot.showPinduoduo) {
                views.setViewVisibility(R.id.widget_btn_pdd, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_btn_pdd, brandIcons.pinduoduo)
                views.setOnClickPendingIntent(
                    R.id.widget_btn_pdd,
                    createDirectLaunchOrFallbackPendingIntent(
                        context = context,
                        primaryUri = TaobaoDeepLinkLauncher.PINDUODUO_IDENTITY_DEEPLINK,
                        fallbackUri = null,
                        targetPackage = TaobaoDeepLinkLauncher.PINDUODUO_PACKAGE,
                        allowImplicitFallback = true,
                        fallbackBroadcastAction = ACTION_LAUNCH_PINDUODUO,
                        requestCode = 103
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_btn_pdd, View.GONE)
            }

            if (snapshot.showJd) {
                views.setViewVisibility(R.id.widget_btn_jd, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_btn_jd, brandIcons.jd)
                views.setOnClickPendingIntent(
                    R.id.widget_btn_jd,
                    createDirectLaunchOrFallbackPendingIntent(
                        context = context,
                        primaryUri = TaobaoDeepLinkLauncher.JD_IDENTITY_DEEPLINK,
                        fallbackUri = null,
                        targetPackage = TaobaoDeepLinkLauncher.JD_PACKAGE,
                        allowImplicitFallback = true,
                        fallbackBroadcastAction = ACTION_LAUNCH_JD,
                        requestCode = 104
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_btn_jd, View.GONE)
            }

            // 绑定列表数据：Android 12+ 使用原子 RemoteCollectionItems，避免改变高度时跨进程列表重载错位
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val itemsBuilder = RemoteViews.RemoteCollectionItems.Builder()
                    .setHasStableIds(true)
                    .setViewTypeCount(1)
                for (row in rows) {
                    itemsBuilder.addItem(row.id, buildWidgetItemRemoteViews(context, row))
                }
                views.setRemoteAdapter(R.id.widget_list_view, itemsBuilder.build())
                views.setViewVisibility(
                    R.id.widget_list_view,
                    if (rows.isEmpty()) View.GONE else View.VISIBLE
                )
                views.setViewVisibility(
                    R.id.widget_empty_view,
                    if (rows.isEmpty()) View.VISIBLE else View.GONE
                )
            } else {
                val serviceIntent = Intent(context, PickupWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                @Suppress("DEPRECATION")
                views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)
            }

            // 列表项点击模板
            val itemClickIntent = Intent(context, PickupWidgetProvider::class.java).apply {
                action = ACTION_ITEM_CLICK
            }
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context,
                200,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)

            return views
        }

        private fun createDirectPackageLaunchOrFallbackPendingIntent(
            context: Context,
            targetPackage: String,
            fallbackBroadcastAction: String,
            requestCode: Int
        ): PendingIntent {
            val launchIntent = try {
                context.packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                null
            }
            if (launchIntent != null) {
                return PendingIntent.getActivity(
                    context,
                    requestCode,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val broadcastIntent = Intent(context, PickupWidgetProvider::class.java).apply {
                action = fallbackBroadcastAction
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createDirectLaunchOrFallbackPendingIntent(
            context: Context,
            primaryUri: String,
            fallbackUri: String?,
            targetPackage: String,
            allowImplicitFallback: Boolean,
            fallbackBroadcastAction: String,
            requestCode: Int
        ): PendingIntent {
            val pm = context.packageManager
            val candidates = listOfNotNull(primaryUri, fallbackUri)
            for (uri in candidates) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                    setPackage(targetPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    return PendingIntent.getActivity(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }
            if (allowImplicitFallback) {
                val implicitIntent = Intent(Intent.ACTION_VIEW, Uri.parse(primaryUri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (implicitIntent.resolveActivity(pm) != null) {
                    return PendingIntent.getActivity(
                        context,
                        requestCode,
                        implicitIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            // 未安装目标 App 时通过广播弹出轻提示
            val broadcastIntent = Intent(context, PickupWidgetProvider::class.java).apply {
                action = fallbackBroadcastAction
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * 当本地取件码数据发生变化时，异步刷新所有桌面小组件（避免在主线程阻塞 120Hz 渲染）
         */
        fun notifyWidgetDataChanged(context: Context, pendingCountOverride: Int? = null) {
            val appContext = context.applicationContext
            widgetRefreshScope.launch {
                notifyWidgetDataChangedSync(appContext, pendingCountOverride)
            }
        }

        /**
         * 在后台线程同步刷新所有桌面小组件（供 BroadcastReceiver goAsync 等后台任务调用）
         */
        fun notifyWidgetDataChangedSync(context: Context, pendingCountOverride: Int? = null) {
            try {
                val appContext = context.applicationContext
                val manager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, PickupWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(componentName)
                if (ids != null && ids.isNotEmpty()) {
                    val snapshot = runBlocking(Dispatchers.IO) {
                        loadWidgetSnapshot(appContext)
                    }
                    val count = pendingCountOverride ?: snapshot.totalPendingCount
                    for (id in ids) {
                        updateAppWidgetWithSnapshot(
                            context = appContext,
                            appWidgetManager = manager,
                            appWidgetId = id,
                            snapshot = snapshot,
                            pendingCount = count
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
