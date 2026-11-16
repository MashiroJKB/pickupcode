package com.example.pickupcode.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
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
 * - 顶部提供原生苹果圆角裁剪的淘宝 / 菜鸟 / 拼多多图标快捷入口，使用直接 Activity PendingIntent 绕过 Android 12+ 后台启动限制
 * - 列表按快递房顺序与货架号从小到大排序展示，支持直接点击「已取」并同步桌面角标
 */
class PickupWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
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
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            app.repository.confirmPickedUpBatch(listOf(itemId))
                            val remaining = app.repository.allItemsFlow.first().count { !it.isPickedUp }
                            AppBadgeManager.updateBadgeCount(context, remaining)
                            notifyWidgetDataChanged(context)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(openIntent)
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

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_pickup_code)

            val pendingCount = try {
                val app = context.applicationContext as? PickupApplication
                if (app != null) {
                    runBlocking(Dispatchers.IO) {
                        app.repository.allItemsFlow.first().count { !it.isPickedUp }
                    }
                } else 0
            } catch (_: Exception) {
                0
            }

            views.setTextViewText(R.id.widget_pending_count, "待取 $pendingCount 件")

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

            // 顶部原生苹果圆角应用图标（淘宝 / 菜鸟 / 拼多多）
            views.setImageViewBitmap(
                R.id.widget_btn_taobao,
                loadAppleRoundedIconBitmapForWidget(context, IdentityCodeType.TAOBAO, 96)
            )
            views.setImageViewBitmap(
                R.id.widget_btn_cainiao,
                loadAppleRoundedIconBitmapForWidget(context, IdentityCodeType.CAINIAO, 96)
            )
            views.setImageViewBitmap(
                R.id.widget_btn_pdd,
                loadAppleRoundedIconBitmapForWidget(context, IdentityCodeType.PINDUODUO, 96)
            )

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

            // 绑定列表 RemoteViewsService
            val serviceIntent = Intent(context, PickupWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

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

            appWidgetManager.updateAppWidget(appWidgetId, views)
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
         * 当本地取件码数据发生变化时，主动刷新所有桌面小组件
         */
        @Suppress("DEPRECATION")
        fun notifyWidgetDataChanged(context: Context) {
            try {
                val appContext = context.applicationContext
                val manager = AppWidgetManager.getInstance(appContext)
                val componentName = ComponentName(appContext, PickupWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(componentName)
                if (ids != null && ids.isNotEmpty()) {
                    manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
                    for (id in ids) {
                        updateAppWidget(appContext, manager, id)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
