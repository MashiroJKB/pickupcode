package com.example.pickupcode

import android.app.Application
import com.example.pickupcode.data.local.PickupDatabase
import com.example.pickupcode.data.local.SettingsDataStore
import com.example.pickupcode.data.repository.PickupRepository
import com.example.pickupcode.service.AppBadgeManager
import com.example.pickupcode.service.SmsContentObserver

class PickupApplication : Application() {

    val database: PickupDatabase by lazy {
        PickupDatabase.getInstance(this)
    }

    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(this)
    }

    val repository: PickupRepository by lazy {
        PickupRepository(
            pickupDao = database.pickupDao(),
            settingsDataStore = settingsDataStore
        )
    }

    private var smsContentObserver: SmsContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        // 预先注册桌面角标通知通道，确保系统「应用通知管理」立即可见通知与角标开关
        AppBadgeManager.ensureNotificationChannel(applicationContext)
        // 注册短信数据库 ContentObserver，配合 SMS_RECEIVED 广播实现秒级入库
        val observer = SmsContentObserver(applicationContext, repository)
        observer.register()
        smsContentObserver = observer
    }

    /**
     * 授予 READ_SMS 权限后确保短信内容观察者已完成注册
     */
    fun ensureSmsObserverRegistered() {
        val observer = smsContentObserver ?: SmsContentObserver(applicationContext, repository).also {
            smsContentObserver = it
        }
        observer.register()
    }
}
