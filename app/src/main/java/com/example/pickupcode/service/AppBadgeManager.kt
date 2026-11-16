package com.example.pickupcode.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pickupcode.MainActivity
import com.example.pickupcode.R

/**
 * 桌面图标角标管理器（根据待取快递件数实时同步桌面图标未读角标）：
 * 兼容华为/荣耀、小米(HyperOS/MIUI)、OPPO(ColorOS)、vivo(OriginOS)、三星及原生 Android Notification Badge。
 */
object AppBadgeManager {

    private const val BADGE_CHANNEL_ID = "pickup_badge_channel"
    private const val BADGE_NOTIFICATION_ID = 10086

    @Volatile
    private var lastAppliedCount: Int = -1

    /**
     * 确保桌面角标与通知渠道已在系统注册，使「应用通知管理」内完整显示通知与桌面图标角标开关
     */
    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val nm = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return
            val existing = nm.getNotificationChannel(BADGE_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    BADGE_CHANNEL_ID,
                    "待取件桌面角标",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "用于在桌面图标角标同步待取快递数量"
                    setShowBadge(true)
                    enableVibration(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 检测系统通知权限是否已授予（Android 13+ 同时校验 POST_NOTIFICATIONS 与系统通知总开关）
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val systemEnabled = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
        return runtimeGranted && systemEnabled
    }

    /**
     * 一键打开当前应用的系统「通知管理 / 桌面角标」设置页
     */
    fun openNotificationAndBadgeSettings(context: Context) {
        ensureNotificationChannel(context)
        val appNotificationIntent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appNotificationIntent)
            return
        } catch (_: Exception) {
        }

        val appDetailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appDetailsIntent)
        } catch (_: Exception) {
        }
    }

    fun updateBadgeCount(context: Context, pendingCount: Int, force: Boolean = false) {
        val count = pendingCount.coerceAtLeast(0)
        if (!force && count == lastAppliedCount) return
        lastAppliedCount = count

        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        updateHuaweiBadge(appContext, count)
        updateHonorBadge(appContext, count)
        updateVivoBadge(appContext, count)
        updateOppoBadge(appContext, count)
        updateSamsungAndDefaultBadge(appContext, count)
        updateXiaomiAndStandardNotificationBadge(appContext, count)
    }

    private fun updateHuaweiBadge(context: Context, count: Int) {
        try {
            val bundle = Bundle().apply {
                putString("package", context.packageName)
                putString("class", MainActivity::class.java.name)
                putInt("badgenumber", count)
            }
            context.contentResolver.call(
                Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
        } catch (_: Exception) {
        }
    }

    private fun updateHonorBadge(context: Context, count: Int) {
        try {
            val bundle = Bundle().apply {
                putString("package", context.packageName)
                putString("class", MainActivity::class.java.name)
                putInt("badgenumber", count)
            }
            context.contentResolver.call(
                Uri.parse("content://com.hihonor.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
        } catch (_: Exception) {
        }
    }

    private fun updateVivoBadge(context: Context, count: Int) {
        try {
            val intent = Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM").apply {
                putExtra("packageName", context.packageName)
                putExtra("className", MainActivity::class.java.name)
                putExtra("notificationNum", count)
                addFlags(0x01000000) // FLAG_RECEIVER_INCLUDE_BACKGROUND
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    private fun updateOppoBadge(context: Context, count: Int) {
        try {
            val extras = Bundle().apply {
                putInt("app_badge_count", count)
            }
            context.contentResolver.call(
                Uri.parse("content://com.android.badge/badge"),
                "setAppBadgeCount",
                null,
                extras
            )
        } catch (_: Exception) {
        }
    }

    private fun updateSamsungAndDefaultBadge(context: Context, count: Int) {
        try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", context.packageName)
                putExtra("badge_count_class_name", MainActivity::class.java.name)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    private fun updateXiaomiAndStandardNotificationBadge(context: Context, count: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            ensureNotificationChannel(context)

            if (count <= 0) {
                nm.cancel(BADGE_NOTIFICATION_ID)
                return
            }

            if (!hasNotificationPermission(context)) {
                return
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, BADGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_pickup)
                .setContentTitle("待取快递")
                .setContentText("当前有 $count 件快递待取")
                .setNumber(count)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .setSilent(true)
                .build()

            // 小米 MIUI / HyperOS 反射设置角标数量
            try {
                val field = notification.javaClass.getDeclaredField("extraNotification")
                val extraNotification = field.get(notification)
                val method = extraNotification.javaClass.getDeclaredMethod(
                    "setMessageCount",
                    Int::class.javaPrimitiveType
                )
                method.invoke(extraNotification, count)
            } catch (_: Exception) {
            }

            nm.notify(BADGE_NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }
}
