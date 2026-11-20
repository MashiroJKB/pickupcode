package com.example.pickupcode

import android.app.AppOpsManager
import com.example.pickupcode.util.RestrictedSettingsHelper
import com.example.pickupcode.util.RestrictedSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictedSettingsHelperTest {

    @Test
    fun `detects major OEM ROM guides accurately`() {
        assertEquals("OriginOS", RestrictedSettingsHelper.detectRomGuide("vivo", "vivo", 36).romName)
        assertEquals("OriginOS", RestrictedSettingsHelper.detectRomGuide("vivo", "iQOO", 35).romName)
        assertEquals("HyperOS", RestrictedSettingsHelper.detectRomGuide("Xiaomi", "Redmi", 35).romName)
        assertEquals("MIUI", RestrictedSettingsHelper.detectRomGuide("Xiaomi", "Xiaomi", 33).romName)
        assertEquals("ColorOS", RestrictedSettingsHelper.detectRomGuide("OPPO", "OnePlus", 36).romName)
        assertEquals("ColorOS", RestrictedSettingsHelper.detectRomGuide("realme", "realme", 35).romName)
        assertEquals("MagicOS", RestrictedSettingsHelper.detectRomGuide("HONOR", "HONOR", 35).romName)
        assertEquals("HarmonyOS", RestrictedSettingsHelper.detectRomGuide("HUAWEI", "HUAWEI", 31).romName)
        assertEquals("One UI", RestrictedSettingsHelper.detectRomGuide("samsung", "samsung", 35).romName)
        assertEquals("Flyme", RestrictedSettingsHelper.detectRomGuide("meizu", "meizu", 35).romName)
        assertEquals("Android", RestrictedSettingsHelper.detectRomGuide("Google", "google", 36).romName)

        // 确认所有 ROM 指引均为均衡两行，无超长单行引发孤儿折行
        val samples = listOf(
            RestrictedSettingsHelper.detectRomGuide("vivo", "vivo", 36),
            RestrictedSettingsHelper.detectRomGuide("Xiaomi", "Xiaomi", 35),
            RestrictedSettingsHelper.detectRomGuide("OPPO", "OPPO", 35),
            RestrictedSettingsHelper.detectRomGuide("HONOR", "HONOR", 35),
            RestrictedSettingsHelper.detectRomGuide("HUAWEI", "HUAWEI", 35),
            RestrictedSettingsHelper.detectRomGuide("samsung", "samsung", 35),
            RestrictedSettingsHelper.detectRomGuide("meizu", "meizu", 35),
            RestrictedSettingsHelper.detectRomGuide("Google", "google", 36)
        )
        samples.forEach { guide ->
            val lines = guide.stepTwoText.split('\n')
            assertEquals(2, lines.size)
            assertTrue(lines.all { it.length in 10..16 })
        }
    }

    @Test
    fun `resolves restricted settings state across Android versions and appOp modes`() {
        // Android 12 及以下无需受限设置步骤
        assertEquals(
            RestrictedSettingsState.NOT_NEEDED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 31,
                appOpMode = AppOpsManager.MODE_ERRORED,
                isTrustedStore = false
            )
        )

        // Android 13~14 默认不拦截短信，但若系统显式返回 ERRORED/IGNORED 仍按受限处理
        assertEquals(
            RestrictedSettingsState.NOT_NEEDED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 34,
                appOpMode = AppOpsManager.MODE_DEFAULT,
                isTrustedStore = false
            )
        )
        assertEquals(
            RestrictedSettingsState.RESTRICTED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 34,
                appOpMode = AppOpsManager.MODE_IGNORED,
                isTrustedStore = false
            )
        )

        // Android 15/16 侧载默认判定为受限；官方应用商店免除；用户解除后返回 ALLOWED
        assertEquals(
            RestrictedSettingsState.RESTRICTED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 35,
                appOpMode = AppOpsManager.MODE_DEFAULT,
                isTrustedStore = false
            )
        )
        assertEquals(
            RestrictedSettingsState.RESTRICTED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 36,
                appOpMode = null,
                isTrustedStore = false
            )
        )
        assertEquals(
            RestrictedSettingsState.NOT_NEEDED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 36,
                appOpMode = AppOpsManager.MODE_DEFAULT,
                isTrustedStore = true
            )
        )
        assertEquals(
            RestrictedSettingsState.ALLOWED,
            RestrictedSettingsHelper.resolveRestrictedSettingsState(
                sdkInt = 36,
                appOpMode = AppOpsManager.MODE_ALLOWED,
                isTrustedStore = false
            )
        )
    }
}
