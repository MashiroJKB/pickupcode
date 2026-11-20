package com.example.pickupcode.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.pickupcode.PickupApplication
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.repository.PickupRepository
import com.example.pickupcode.widget.PickupWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 引擎一（广播部分）：监听新短信到达广播 SMS_RECEIVED，实现秒级解析入库
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // 将同一发送方的多段长短信拼接为完整正文并保留各自时间戳
        val groupedBySender = linkedMapOf<String, Pair<StringBuilder, Long>>()
        val fallbackNow = System.currentTimeMillis()

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: "SMS"
            val body = sms.displayMessageBody ?: continue
            val ts = if (sms.timestampMillis > 0) sms.timestampMillis else fallbackNow
            val existing = groupedBySender[sender]
            if (existing == null) {
                groupedBySender[sender] = StringBuilder(body) to ts
            } else {
                existing.first.append(body)
                if (ts > existing.second) {
                    groupedBySender[sender] = existing.first to ts
                }
            }
        }

        if (groupedBySender.isEmpty()) return

        val app = context.applicationContext as? PickupApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                var totalAffected = 0
                for ((sender, entry) in groupedBySender) {
                    totalAffected += app.repository.ingestRawMessage(
                        rawText = entry.first.toString(),
                        source = PickupItemEntity.SOURCE_SMS,
                        senderOrPackage = sender,
                        messageTimestamp = entry.second
                    )
                }
                if (totalAffected > 0) {
                    PickupWidgetProvider.notifyWidgetDataChangedSync(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 引擎一（数据库监听部分）：监听 content://sms 变化，补充部分 ROM 限制广播的场景
 */
class SmsContentObserver(
    private val context: Context,
    private val repository: PickupRepository
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRegistered = false
    private var lastTriggerTime = 0L

    fun register() {
        if (isRegistered) return
        try {
            context.contentResolver.registerContentObserver(
                Uri.parse("content://sms"),
                true,
                this
            )
            isRegistered = true
        } catch (_: SecurityException) {
            // 尚未授予 READ_SMS 时静默忽略，授权后会再次触发扫描
        } catch (_: Exception) {
        }
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val now = System.currentTimeMillis()
        // 800ms 防抖，避免单条短信状态变更触发多次查询
        if (now - lastTriggerTime < 800L) return
        lastTriggerTime = now

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        scope.launch {
            val added = repository.scanRecentSmsInbox(context = context, daysOverride = 1)
            if (added > 0) {
                PickupWidgetProvider.notifyWidgetDataChangedSync(context)
            }
        }
    }
}
