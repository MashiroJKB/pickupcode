package com.example.pickupcode.util

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android 13+ / Android 15+ 原生「受限设置（Restricted Settings / ECM）」状态
 */
enum class RestrictedSettingsState {
    /** 已由用户在应用信息页手动解除受限设置 */
    ALLOWED,

    /** 当前被系统「设置限制 / 受限设置」拦截，需先前往应用信息解除 */
    RESTRICTED,

    /** 当前系统版本或安装来源不触发短信受限设置 */
    NOT_NEEDED
}

/**
 * 按当前手机 ROM 定制的「解除设置限制」引导文案
 */
data class RomRestrictedGuide(
    val romName: String,
    val stepTwoText: String
)

object RestrictedSettingsHelper {

    private const val OPSTR_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"

    private val TRUSTED_APP_STORES = setOf(
        "com.android.vending",          // Google Play
        "com.bbk.appstore",             // vivo 应用商店
        "com.xiaomi.market",            // 小米应用商店
        "com.heytap.market",            // OPPO / 一加 / 真我软件商店
        "com.oppo.market",
        "com.huawei.appmarket",         // 华为应用市场
        "com.hihonor.appmarket",        // 荣耀应用市场
        "com.sec.android.app.samsungapps", // 三星应用商店
        "com.meizu.mstore"              // 魅族应用商店
    )

    /**
     * 识别当前设备的系统 ROM 并返回对应应用信息页的操作指引
     */
    fun detectRomGuide(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty(),
        sdkInt: Int = Build.VERSION.SDK_INT
    ): RomRestrictedGuide {
        val signature = "$manufacturer $brand".lowercase()
        return when {
            "vivo" in signature || "iqoo" in signature -> RomRestrictedGuide(
                romName = "OriginOS",
                stepTwoText = "点页面右上角「⋮」更多菜单\n开启「允许受限设置」并验证密码"
            )

            "xiaomi" in signature || "redmi" in signature || "poco" in signature -> RomRestrictedGuide(
                romName = if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    "HyperOS"
                } else {
                    "MIUI"
                },
                stepTwoText = "在右上角「⋮」或页面最底部\n开启「允许受限设置」并验证密码"
            )

            "oppo" in signature || "oneplus" in signature || "realme" in signature -> RomRestrictedGuide(
                romName = "ColorOS",
                stepTwoText = "点页面右上角「⋮」更多菜单\n开启「允许受限设置」并验证密码"
            )

            "honor" in signature -> RomRestrictedGuide(
                romName = "MagicOS",
                stepTwoText = "点页面右上角「⋮」更多菜单\n开启「允许受限设置」并验证密码"
            )

            "huawei" in signature -> RomRestrictedGuide(
                romName = "HarmonyOS",
                stepTwoText = "在右上角「⋮」或页面底部菜单\n开启「允许受限设置」并验证密码"
            )

            "samsung" in signature -> RomRestrictedGuide(
                romName = "One UI",
                stepTwoText = "点页面右上角「⋮」更多菜单\n开启「允许受限设置」并验证密码"
            )

            "meizu" in signature -> RomRestrictedGuide(
                romName = "Flyme",
                stepTwoText = "点页面右上角菜单并进入设置\n开启「允许受限设置」并验证密码"
            )

            else -> RomRestrictedGuide(
                romName = "Android",
                stepTwoText = "点页面右上角「⋮」更多菜单\n开启「允许受限设置」并验证密码"
            )
        }
    }

    /**
     * 检测当前应用是否受到 Android「受限设置（Restricted Settings / ECM）」拦截：
     * - Android 15 (API 35) 及 Android 16 原生增强确认模式会将非应用商店侧载 APK 的短信权限纳入受限设置；
     * - 用户在应用信息页解除后，`android:access_restricted_settings` 会变为 `MODE_ALLOWED`。
     */
    fun checkRestrictedSettingsState(context: Context): RestrictedSettingsState {
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return RestrictedSettingsState.NOT_NEEDED
        }
        val appOpMode = queryRestrictedSettingsAppOp(context)
        val trustedStore = isInstalledFromTrustedStore(context)
        return resolveRestrictedSettingsState(
            sdkInt = sdkInt,
            appOpMode = appOpMode,
            isTrustedStore = trustedStore
        )
    }

    internal fun resolveRestrictedSettingsState(
        sdkInt: Int,
        appOpMode: Int?,
        isTrustedStore: Boolean
    ): RestrictedSettingsState {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return RestrictedSettingsState.NOT_NEEDED
        }
        return when (appOpMode) {
            AppOpsManager.MODE_ALLOWED -> RestrictedSettingsState.ALLOWED
            AppOpsManager.MODE_ERRORED,
            AppOpsManager.MODE_IGNORED -> RestrictedSettingsState.RESTRICTED
            else -> {
                if (sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM && !isTrustedStore) {
                    RestrictedSettingsState.RESTRICTED
                } else {
                    RestrictedSettingsState.NOT_NEEDED
                }
            }
        }
    }

    private fun queryRestrictedSettingsAppOp(context: Context): Int? {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return null
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(context.packageName, 0)
            }
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                OPSTR_ACCESS_RESTRICTED_SETTINGS,
                appInfo.uid,
                context.packageName
            )
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun isInstalledFromTrustedStore(context: Context): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer != null && installer in TRUSTED_APP_STORES
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 一键打开当前应用的系统「应用信息」页面
     */
    fun openAppDetailsSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 判断系统是否已抑制短信运行时授权弹窗（用户曾选择拒绝且不再询问）
     */
    fun isSmsPermissionPermanentlyDenied(
        context: Context,
        hasRequestedBefore: Boolean
    ): Boolean {
        if (!hasRequestedBefore) return false
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (readGranted) return false
        val activity = context.findActivity() ?: return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.READ_SMS
        )
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
