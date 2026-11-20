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
import com.example.pickupcode.service.AppBadgeManager
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
 * - 最低宽度 4 格、最低高度 2 格（targetCellWidth=4, targetCellHeight=2, minHeight=110dp），
 *   避免改变小组件高度时触发桌面宿主的 shrink-to-fit 整体缩放或位置偏移；
 * - Android 12+ 使用单一灵活 RemoteViews + RemoteCollectionItems 原子更新列表，零跨进程重绘抖动；
 * - 顶部提供高清 Apple 连续圆角裁剪的淘宝 / 菜鸟 / 拼多多图标快捷入口，绕过 Android 12+ 后台启动限制；
 * - 列表按快递房顺序与货架号自然升序排列，支持直接点击「已取」并同步桌面角标。
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
                    updateAppWidgetWithRows(
                        context = appContext,
                        appWidgetManager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        rows = snapshot.rows,
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
                updateAppWidgetWithRows(
                    context = appContext,
                    appWidgetManager = appWidgetManager,
                    appWidgetId = appWidgetId,
                    rows = snapshot.rows,
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
            ACTION_LAUNCH_TAOBAO -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.TAOBAO,
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

            ACTION_LAUNCH_PINDUODUO -> {
                TaobaoDeepLinkLauncher.launchIdentityCode(
                    context = context,
                    type = IdentityCodeType.PINDUODUO,
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
                            AppBadgeManager.updateBadgeCount(context, remaining)
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
        const val ACTION_LAUNCH_TAOBAO = "com.example.pickupcode.widget.ACTION_LAUNCH_TAOBAO"
        const val ACTION_LAUNCH_CAINIAO = "com.example.pickupcode.widget.ACTION_LAUNCH_CAINIAO"
        const val ACTION_LAUNCH_PINDUODUO = "com.example.pickupcode.widget.ACTION_LAUNCH_PINDUODUO"
        const val ACTION_ITEM_CLICK = "com.example.pickupcode.widget.ACTION_ITEM_CLICK"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_MARK_DONE = "extra_mark_done"

        private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val iconCacheLock = Any()

        @Volatile
        private var cachedIcons: Triple<Bitmap, Bitmap, Bitmap>? = null

        private fun getCachedBrandIcons(context: Context): Triple<Bitmap, Bitmap, Bitmap> {
            val existing = cachedIcons
            if (existing != null &&
                !existing.first.isRecycled &&
                !existing.second.isRecycled &&
                !existing.third.isRecycled
            ) {
                return existing
            }
            return synchronized(iconCacheLock) {
                val doubleChecked = cachedIcons
                if (doubleChecked != null &&
                    !doubleChecked.first.isRecycled &&
                    !doubleChecked.second.isRecycled &&
                    !doubleChecked.third.isRecycled
                ) {
                    doubleChecked
                } else {
                    val appContext = context.applicationContext
                    Triple(
                        loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.TAOBAO, 112),
                        loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.CAINIAO, 112),
                        loadAppleRoundedIconBitmapForWidget(appContext, IdentityCodeType.PINDUODUO, 112)
                    ).also { cachedIcons = it }
                }
            }
        }

        private fun updateAppWidgetWithRows(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            rows: List<WidgetDisplayRow>,
            pendingCount: Int
        ) {
            val (taobaoBmp, cainiaoBmp, pddBmp) = getCachedBrandIcons(context)
            val views = buildWidgetRemoteViews(
                context = context,
                appWidgetId = appWidgetId,
                rows = rows,
                pendingCount = pendingCount,
                taobaoBmp = taobaoBmp,
                cainiaoBmp = cainiaoBmp,
                pddBmp = pddBmp
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
            rows: List<WidgetDisplayRow>,
            pendingCount: Int,
            taobaoBmp: Bitmap,
            cainiaoBmp: Bitmap,
            pddBmp: Bitmap
        ): RemoteViews {
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

            // 顶部原生 Apple 连续圆角应用图标（淘宝 / 菜鸟 / 拼多多）
            views.setImageViewBitmap(R.id.widget_btn_taobao, taobaoBmp)
            views.setImageViewBitmap(R.id.widget_btn_cainiao, cainiaoBmp)
            views.setImageViewBitmap(R.id.widget_btn_pdd, pddBmp)

            views.setOnClickPendingIntent(
                R.id.widget_btn_taobao,
                createDirectLaunchOrFallbackPendingIntent(
                    context = context,
                    primaryUri = TaobaoDeepLinkLauncher.TAOBAO_IDENTITY_DEEPLINK,
                    fallbackUri = TaobaoDeepLinkLauncher.CAINIAO_IDENTITY_DEEPLINK,
                    targetPackage = TaobaoDeepLinkLauncher.TAOBAO_PACKAGE,
                    fallbackBroadcastAction = ACTION_LAUNCH_TAOBAO,
                    requestCode = 101
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_cainiao,
                createDirectLaunchOrFallbackPendingIntent(
                    context = context,
                    primaryUri = TaobaoDeepLinkLauncher.CAINIAO_IDENTITY_DEEPLINK,
                    fallbackUri = TaobaoDeepLinkLauncher.TAOBAO_IDENTITY_DEEPLINK,
                    targetPackage = TaobaoDeepLinkLauncher.TAOBAO_PACKAGE,
                    fallbackBroadcastAction = ACTION_LAUNCH_CAINIAO,
                    requestCode = 102
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_pdd,
                createDirectLaunchOrFallbackPendingIntent(
                    context = context,
                    primaryUri = TaobaoDeepLinkLauncher.PINDUODUO_IDENTITY_DEEPLINK,
                    fallbackUri = null,
                    targetPackage = TaobaoDeepLinkLauncher.PINDUODUO_PACKAGE,
                    fallbackBroadcastAction = ACTION_LAUNCH_PINDUODUO,
                    requestCode = 103
                )
            )

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

        private fun createDirectLaunchOrFallbackPendingIntent(
            context: Context,
            primaryUri: String,
            fallbackUri: String?,
            targetPackage: String,
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
                        updateAppWidgetWithRows(
                            context = appContext,
                            appWidgetManager = manager,
                            appWidgetId = id,
                            rows = snapshot.rows,
                            pendingCount = count
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
