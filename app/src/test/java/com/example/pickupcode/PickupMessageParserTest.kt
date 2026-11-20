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
    fun `parses multiline sms with cross-line locker pickup code`() {
        val lockerSms = """
            丰巢快递柜
            取件码
            839201
            凭此码开箱取件
        """.trimIndent()
        val lockerResults = PickupMessageParser.parse(lockerSms, defaultRules)
        assertEquals(listOf("839201"), lockerResults.map { it.code })
    }

    @Test
    fun `provides default rules for stations 1 through 8`() {
        assertEquals((1..8).toList(), defaultRules.map { it.stationNumber })
        assertEquals((1..8).map { "${it}号快递房" }, defaultRules.map { it.displayName })

        val sms4 = PickupMessageParser.parse("您的包裹已到4号快递房，凭取件码 3-1-2088 取件。", defaultRules)
        val sms8 = PickupMessageParser.parse("【菜鸟】请凭取件码 8-2-9012 前往8号房领取。", defaultRules)

        assertEquals(4, sms4.single().stationNumber)
        assertEquals(8, sms8.single().stationNumber)
    }

    @Test
    fun `groups and naturally sorts pickup items for share image export`() {
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
            ),
            com.example.pickupcode.data.local.PickupItemEntity(
                id = 3L,
                code = "8-1-1",
                stationNumber = 1,
                isPickedUp = true,
                rawMessage = "",
                createdAt = now
            )
        )
        val groups = com.example.pickupcode.util.PickupShareHelper.groupItemsForShare(items, defaultRules)

        assertEquals(1, groups.size)
        assertEquals("① 1号快递房", "${groups[0].circleSymbol} ${groups[0].displayName}")
        assertEquals(listOf("2-1-5", "10-1-2"), groups[0].items.map { it.code })
        assertEquals("数据线", groups[0].items[1].productName)
    }

    @Test
    fun `masks chinese dates and quantities without misreading them as pickup codes`() {
        val sms = "【菜鸟驿站】您的2件包裹于2026年11月17日到3号房，请于11月19日前24小时内凭取件码 5-2-8019 取件，超时2026号柜不收费。"
        val results = PickupMessageParser.parse(sms, defaultRules)

        assertEquals(1, results.size)
        assertEquals("5-2-8019", results[0].code)
        assertEquals(3, results[0].stationNumber)
    }

    @Test
    fun `does not confuse large locker or room numbers with contextual pickup codes`() {
        val sms = "【丰巢】请凭取件码 682914 到1024号柜领取，共1件包裹，营业时间08:00-22:00。"
        val results = PickupMessageParser.parse(sms, defaultRules)

        assertEquals(1, results.size)
        assertEquals("682914", results[0].code)
    }

    @Test
    fun `matches extended station keywords and chinese numerals up to 20`() {
        val rules15 = SettingsDataStore.defaultStationRules(15)
        val sms12 = "您的包裹已放至十二号快递点，请凭自提码 4-1-9023 及时取件。"
        val sms15 = "【兔喜】已入库15号快递超市，提取码 A8-3021 请及时领取。"
        val smsHash = "已到达3#快递房，货架号 2-3-1180 请及时自提。"

        val r12 = PickupMessageParser.parse(sms12, rules15)
        val r15 = PickupMessageParser.parse(sms15, rules15)
        val rHash = PickupMessageParser.parse(smsHash, rules15)

        assertEquals(12, r12.single().stationNumber)
        assertEquals("4-1-9023", r12.single().code)
        assertEquals(15, r15.single().stationNumber)
        assertEquals("A8-3021", r15.single().code)
        assertEquals(3, rHash.single().stationNumber)
        assertEquals("2-3-1180", rHash.single().code)
    }

    @Test
    fun `prevents numeric prefix collision such as station 12 matching station 2`() {
        val smsPrefix = "您的包裹已到达12号快递房，请凭取件码 7-2-6610 领取。"
        val resultsPrefix = PickupMessageParser.parse(smsPrefix, defaultRules) // defaultRules only has 1..8
        assertEquals(1, resultsPrefix.size)
        assertEquals("7-2-6610", resultsPrefix[0].code)
        assertEquals(0, resultsPrefix[0].stationNumber)

        val smsSuffix = "您的包裹已到达快递房12，请凭取件码 7-2-6611 领取。"
        val resultsSuffix = PickupMessageParser.parse(smsSuffix, defaultRules)
        assertEquals(1, resultsSuffix.size)
        assertEquals("7-2-6611", resultsSuffix[0].code)
        assertEquals(0, resultsSuffix[0].stationNumber)

        val smsChinese = "您的包裹已到达十二号快递房，请凭取件码 7-2-6612 领取。"
        val resultsChinese = PickupMessageParser.parse(smsChinese, defaultRules)
        assertEquals(1, resultsChinese.size)
        assertEquals("7-2-6612", resultsChinese[0].code)
        assertEquals(0, resultsChinese[0].stationNumber)
    }
}
