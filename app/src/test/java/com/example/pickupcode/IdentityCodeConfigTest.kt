package com.example.pickupcode

import com.example.pickupcode.ui.components.IdentityCodeType
import com.example.pickupcode.ui.components.TaobaoDeepLinkLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityCodeConfigTest {

    @Test
    fun `maintains exact five-brand identity code ordering`() {
        assertEquals(
            listOf(
                IdentityCodeType.CAINIAO_APP,
                IdentityCodeType.CAINIAO,
                IdentityCodeType.TAOBAO,
                IdentityCodeType.PINDUODUO,
                IdentityCodeType.JD
            ),
            IdentityCodeType.entries
        )
        assertEquals("菜鸟APP", IdentityCodeType.CAINIAO_APP.title)
        assertEquals("菜鸟身份码", IdentityCodeType.CAINIAO.title)
        assertEquals("淘宝身份码", IdentityCodeType.TAOBAO.title)
        assertEquals("拼多多取件码", IdentityCodeType.PINDUODUO.title)
        assertEquals("京东身份码", IdentityCodeType.JD.title)
        assertEquals("com.cainiao.wireless", TaobaoDeepLinkLauncher.CAINIAO_APP_PACKAGE)
    }

    @Test
    fun `exposes exact JD identity code deeplink and package`() {
        assertEquals("com.jingdong.app.mall", TaobaoDeepLinkLauncher.JD_PACKAGE)
        assertEquals(
            "openapp.jdmobile://virtual?params=%7B%22category%22%3A%22jump%22%2C%22des%22%3A%22m%22%2C%22url%22%3A%22https%3A%2F%2Fconvenient-h5.jd.com%2FidentityCode%3Fchannel%3Dxueshengjirukou%22%7D",
            TaobaoDeepLinkLauncher.JD_IDENTITY_DEEPLINK
        )
        assertTrue(TaobaoDeepLinkLauncher.JD_IDENTITY_DEEPLINK.startsWith("openapp.jdmobile://virtual?"))
    }
}
