package com.example.pickupcode.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 取件码本地数据库实体
 */
@Entity(
    tableName = "pickup_items",
    indices = [
        Index(value = ["code", "station_number", "created_at"]),
        Index(value = ["is_picked_up", "created_at"])
    ]
)
data class PickupItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "code")
    val code: String,

    /** 1~20 代表 ①~⑳ 号快递房；0 代表 ？待分类 */
    @ColumnInfo(name = "station_number")
    val stationNumber: Int = 0,

    /** 原始识别到的驿站/快递房文本 */
    @ColumnInfo(name = "station_raw_name")
    val stationRawName: String = "",

    /** 预留字段（已不再展示快递品牌别名） */
    @ColumnInfo(name = "express_company")
    val expressCompany: String = "",

    /** 商品名称（用户点击备注或从短信中提取） */
    @ColumnInfo(name = "product_name")
    val productName: String = "",

    /** 是否由用户手动编辑过商品名称 */
    @ColumnInfo(name = "is_custom_remark")
    val isCustomRemark: Boolean = false,

    /** 原始短信全文（点击卡片空白处弹窗核对） */
    @ColumnInfo(name = "raw_message")
    val rawMessage: String,

    /** 消息来源："SMS" | "MANUAL" */
    @ColumnInfo(name = "source")
    val source: String = SOURCE_SMS,

    /** 短信发送方号码 */
    @ColumnInfo(name = "sender_or_package")
    val senderOrPackage: String = "",

    /** 是否已确认取件（已取件记录在数据库中保留 7 天用于自动去重排除，满 7 天自动清理） */
    @ColumnInfo(name = "is_picked_up")
    val isPickedUp: Boolean = false,

    /** 确认已取件的时间戳 */
    @ColumnInfo(name = "picked_up_at")
    val pickedUpAt: Long? = null,

    /** 记录创建/短信接收时间戳 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** 最后更新时间戳 */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val stationCircleSymbol: String
        get() = stationCircleSymbolFor(stationNumber)

    companion object {
        const val SOURCE_SMS = "SMS"
        const val SOURCE_MANUAL = "MANUAL"

        fun stationCircleSymbolFor(number: Int): String = when (number) {
            in 1..20 -> (0x2460 + number - 1).toChar().toString() // ① ~ ⑳
            else -> "？"
        }
    }
}
