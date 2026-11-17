package com.example.pickupcode

import com.example.pickupcode.data.local.SettingsDataStore
import com.example.pickupcode.data.parser.PickupMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupMessageParserTest {

    private val defaultRules = SettingsDataStore.defaultStationRules()

    @Test
    fun `rejects verification codes and completed or marketing messages`() {
        val verificationSms = "【淘宝】您的验证码为 392014，请勿泄露给他人。"
        val signedSms = "【菜鸟驿站】您的包裹（取件码 1-2-3045）已出库，已签收完成，感谢使用。"
        val marketingSms = "【京东】双11优惠券即将到期，快来直播间抢购！"

        assertTrue(PickupMessageParser.parse(verificationSms, defaultRules).isEmpty())
        assertTrue(PickupMessageParser.parse(signedSms, defaultRules).isEmpty())
        assertTrue(PickupMessageParser.parse(marketingSms, defaultRules).isEmpty())
    }

    @Test
    fun `splits multi-code sms into independent pickup records`() {
        val multiCodeSms = "【菜鸟驿站】凭取件码 1-2-3045、102-2-3046 或 J8-5-1008 到3号房取件，营业时间08:30-21:00。"
        val results = PickupMessageParser.parse(multiCodeSms, defaultRules)

        assertEquals(3, results.size)
        assertEquals(listOf("1-2-3045", "102-2-3046", "J8-5-1008"), results.map { it.code })
        assertTrue(results.all { it.stationNumber == 3 })
    }

    @Test
    fun `matches custom station keyword aliases and respects deleted stations`() {
        val customRules = defaultRules
            .filterNot { it.stationNumber == 7 }
            .map { rule ->
                if (rule.stationNumber == 1) {
                    rule.copy(keywords = rule.keywords + listOf("西区菜鸟", "一食堂旁"))
                } else {
                    rule
                }
            }

        val sms1 = "【中通快递】您的包裹已到西区菜鸟，请凭取件码 A12-3098 及时取件。"
        val results1 = PickupMessageParser.parse(sms1, customRules)
        assertEquals(1, results1.size)
        assertEquals("A12-3098", results1[0].code)
        assertEquals(1, results1[0].stationNumber)
        assertEquals("西区菜鸟", results1[0].stationRawName)

        // 7号房被删除后不再匹配为 7
        val sms7 = "您的包裹已到7号快递房，请凭取件码 9-1-4022 领取。"
        val results7 = PickupMessageParser.parse(sms7, customRules)
        assertEquals(1, results7.size)
        assertEquals(0, results7[0].stationNumber)
    }

    @Test
    fun `extracts product name when explicitly present`() {
        val smsText = "您购买的【高数教材同济第八版】已到达2号快递房，请凭提货码 6-3-1024 前往领取。"
        val results = PickupMessageParser.parse(
            rawText = smsText,
            stationRules = defaultRules
        )

        assertEquals(1, results.size)
        assertEquals("6-3-1024", results[0].code)
        assertEquals(2, results[0].stationNumber)
        assertEquals("高数教材同济第八版", results[0].productName)
    }

    @Test
    fun `assigns station 0 when station is unclassified`() {
        val sms = "【极兔速递】您的包裹已到学校南门服务点，请凭取件码 9-1-4022 领取，详询13800138000。"
        val results = PickupMessageParser.parse(sms, defaultRules)

        assertEquals(1, results.size)
        assertEquals("9-1-4022", results[0].code)
        assertEquals(0, results[0].stationNumber)
    }

    @Test
    fun `matches station via custom badgeNumber`() {
        val customRules = listOf(
            com.example.pickupcode.data.local.StationRule(
                stationNumber = 1,
                displayName = "自定义驿站",
                keywords = listOf("自定义驿站"),
                badgeNumber = 4
            )
        )
        val sms = "您的包裹已到4号快递房，请凭取件码 2-1-8844 领取。"
        val results = PickupMessageParser.parse(sms, customRules)

        assertEquals(1, results.size)
        assertEquals(1, results[0].stationNumber)
    }

    @Test
    fun `sorts pickup codes naturally by shelf and layer numbers`() {
        val codes = listOf("100-1-1", "30-3-2", "2-1-5", "94-3-2", "1-4-10")
        val items = codes.map {
            com.example.pickupcode.data.local.PickupItemEntity(
                code = it,
                stationNumber = 1,
                rawMessage = it
            )
        }
        val sortedCodes = items
            .sortedWith(com.example.pickupcode.data.local.PickupItemEntity.NaturalCodeComparator)
            .map { it.code }

        assertEquals(listOf("1-4-10", "2-1-5", "30-3-2", "94-3-2", "100-1-1"), sortedCodes)
    }

    @Test
    fun `formats grouped pickup items cleanly for sharing`() {
        val now = System.currentTimeMillis()
        val items = listOf(
            com.example.pickupcode.data.local.PickupItemEntity(
                id = 1L,
                code = "10-1-2",
                stationNumber = 1,
                productName = "数据线",
                rawMessage = "",
                createdAt = now
            ),
            com.example.pickupcode.data.local.PickupItemEntity(
                id = 2L,
                code = "2-1-5",
                stationNumber = 1,
                productName = "",
                rawMessage = "",
                createdAt = now
            )
        )
        val text = com.example.pickupcode.util.PickupShareHelper.buildShareText(items, defaultRules)

        assertTrue(text.contains("共 2 件"))
        assertTrue(text.contains("① 家属区1号 京东"))
        assertTrue(text.indexOf("2-1-5") < text.indexOf("10-1-2"))
        assertTrue(text.contains("10-1-2（数据线）"))
    }
}
