package com.example.pickupcode.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupDao
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.SettingsDataStore
import com.example.pickupcode.data.parser.PickupMessageParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 核心数据仓库
 *
 * 负责：
 * 1. 短信读取与监听（默认扫描最近 7 天短信）
 * 2. 自动排除已取快递，已取件记录在本地数据库保留 7 天用于防重复导入，超过 7 天自动清理
 * 3. 提供多选确认已取与 3 秒内撤销恢复接口
 */
class PickupRepository(
    private val pickupDao: PickupDao,
    private val settingsDataStore: SettingsDataStore
) {
    private val ingestMutex = Mutex()

    val allItemsFlow: Flow<List<PickupItemEntity>> = pickupDao.observeAllItems()
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow

    /**
     * 解析并入库一条原始短信，自动排除已取快递，返回新增或合并更新的记录数
     */
    suspend fun ingestRawMessage(
        rawText: String,
        source: String,
        senderOrPackage: String = "",
        messageTimestamp: Long = System.currentTimeMillis(),
        isOcrText: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settingsFlow.first()
        val parsedList = if (isOcrText) {
            PickupMessageParser.parseFromOcr(
                rawOcrText = rawText,
                stationRules = settings.stationRules
            )
        } else {
            PickupMessageParser.parse(
                rawText = rawText,
                stationRules = settings.stationRules
            )
        }
        if (parsedList.isEmpty()) return@withContext 0

        ingestMutex.withLock {
            var affectedCount = 0
            val windowStart = System.currentTimeMillis() - RETENTION_SEVEN_DAYS_MILLIS

            for (parsed in parsedList) {
                val recentCandidates = pickupDao.findRecentByCode(
                    code = parsed.code,
                    sinceTimestamp = windowStart
                )

                val matchedExisting = recentCandidates.firstOrNull { existing ->
                    existing.stationNumber == parsed.stationNumber ||
                        existing.stationNumber == 0 ||
                        parsed.stationNumber == 0
                }

                if (matchedExisting != null) {
                    // 核心规则：若该取件码已被标记为「已取件」，自动跳过排除，绝不重新恢复为待取
                    if (matchedExisting.isPickedUp) {
                        continue
                    }

                    val merged = mergeExistingWithParsed(
                        existing = matchedExisting,
                        parsedStationNumber = parsed.stationNumber,
                        parsedStationRawName = parsed.stationRawName,
                        parsedProductName = parsed.productName,
                        parsedRawMessage = parsed.rawMessage
                    )
                    if (merged != matchedExisting) {
                        pickupDao.update(merged)
                        affectedCount++
                    }
                } else {
                    pickupDao.insert(
                        PickupItemEntity(
                            code = parsed.code,
                            stationNumber = parsed.stationNumber,
                            stationRawName = parsed.stationRawName,
                            expressCompany = "",
                            productName = parsed.productName,
                            isCustomRemark = false,
                            rawMessage = parsed.rawMessage,
                            source = source,
                            senderOrPackage = senderOrPackage,
                            isPickedUp = false,
                            pickedUpAt = null,
                            createdAt = messageTimestamp,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    affectedCount++
                }
            }
            affectedCount
        }
    }

    private fun mergeExistingWithParsed(
        existing: PickupItemEntity,
        parsedStationNumber: Int,
        parsedStationRawName: String,
        parsedProductName: String,
        parsedRawMessage: String
    ): PickupItemEntity {
        val finalStationNumber = when {
            existing.stationNumber > 0 -> existing.stationNumber
            parsedStationNumber > 0 -> parsedStationNumber
            else -> 0
        }

        val finalStationRawName = existing.stationRawName.ifBlank { parsedStationRawName }

        val finalProductName = when {
            existing.isCustomRemark && existing.productName.isNotBlank() -> existing.productName
            existing.productName.isNotBlank() -> existing.productName
            else -> parsedProductName
        }

        val finalRawMessage = when {
            existing.rawMessage.contains(parsedRawMessage) -> existing.rawMessage
            parsedRawMessage.contains(existing.rawMessage) -> parsedRawMessage
            else -> existing.rawMessage
        }

        val hasChanged = finalStationNumber != existing.stationNumber ||
            finalStationRawName != existing.stationRawName ||
            finalProductName != existing.productName ||
            finalRawMessage != existing.rawMessage

        return if (hasChanged) {
            existing.copy(
                stationNumber = finalStationNumber,
                stationRawName = finalStationRawName,
                productName = finalProductName,
                expressCompany = "",
                rawMessage = finalRawMessage,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            existing
        }
    }

    /**
     * 扫描系统收件箱最近 7 天的历史短信，并自动清理超过 7 天的已取件记录
     */
    suspend fun scanRecentSmsInbox(
        context: Context,
        daysOverride: Int? = null
    ): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // 清理旧版遗留的示例数据及超过 7 天的已取件记录
        pickupDao.deleteLegacyDemoItems()
        pickupDao.deleteExpiredPickedUp(now - RETENTION_SEVEN_DAYS_MILLIS)

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasSmsPermission) return@withContext 0

        val scanDays = daysOverride ?: SettingsDataStore.DEFAULT_SMS_SCAN_DAYS
        val sinceTimestamp = now - scanDays * 24L * 60L * 60L * 1000L

        var totalAffected = 0
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val address = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else now

                    if (body.isNotBlank()) {
                        totalAffected += ingestRawMessage(
                            rawText = body,
                            source = PickupItemEntity.SOURCE_SMS,
                            senderOrPackage = address,
                            messageTimestamp = date
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // 忽略特定定制 ROM ContentProvider 异常
        }

        totalAffected
    }

    /**
     * 重新应用当前识别规则到所有未取件记录（当用户修改了识别规则或增删了快递房时自动重分类）
     */
    suspend fun reapplyStationRules() = withContext(Dispatchers.IO) {
        val settings = settingsDataStore.settingsFlow.first()
        val activeNumbers = settings.stationRules.map { it.stationNumber }.toSet()
        val allItems = pickupDao.observeAllItems().first()
        for (item in allItems) {
            if (item.isPickedUp) continue
            val (matchedNum, matchedRaw) = PickupMessageParser.matchStation(
                text = item.rawMessage,
                stationRules = settings.stationRules
            )
            val newStation = when {
                matchedNum in activeNumbers -> matchedNum
                item.stationNumber in activeNumbers -> item.stationNumber
                else -> 0
            }
            if (newStation != item.stationNumber) {
                pickupDao.update(
                    item.copy(
                        stationNumber = newStation,
                        stationRawName = matchedRaw,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * 批量确认已取件（确认后从待取列表中消失，并在数据库保留 7 天以防扫描短信重复导入）
     */
    suspend fun confirmPickedUpBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        pickupDao.updatePickedUpStatusBatch(
            ids = ids,
            isPickedUp = true,
            pickedUpAt = now,
            updatedAt = now
        )
    }

    /**
     * 3 秒内撤销已取件操作，立即恢复到待取列表
     */
    suspend fun undoPickedUpBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        pickupDao.updatePickedUpStatusBatch(
            ids = ids,
            isPickedUp = false,
            pickedUpAt = null,
            updatedAt = now
        )
    }

    /**
     * 手动备注或修改商品名称
     */
    suspend fun updateProductRemark(itemId: Long, productName: String) = withContext(Dispatchers.IO) {
        val cleaned = productName.trim()
        pickupDao.updateProductName(
            id = itemId,
            productName = cleaned,
            isCustomRemark = cleaned.isNotEmpty(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 手动指定快递房编号
     */
    suspend fun updateStationNumber(itemId: Long, stationNumber: Int) = withContext(Dispatchers.IO) {
        pickupDao.updateStationNumber(
            id = itemId,
            stationNumber = stationNumber.coerceIn(0, 100),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 手动新增一条取件码记录
     */
    suspend fun addManualPickupCode(
        code: String,
        stationNumber: Int,
        productName: String
    ) = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        pickupDao.insert(
            PickupItemEntity(
                code = cleanCode,
                stationNumber = stationNumber.coerceIn(0, 100),
                stationRawName = if (stationNumber > 0) "${stationNumber}号快递房" else "",
                expressCompany = "",
                productName = productName.trim(),
                isCustomRemark = productName.isNotBlank(),
                rawMessage = "【手动录入】取件码 $cleanCode，地点：${
                    if (stationNumber > 0) "${stationNumber}号快递房" else "待分类"
                }${if (productName.isNotBlank()) "，商品：${productName.trim()}" else ""}",
                source = PickupItemEntity.SOURCE_MANUAL,
                senderOrPackage = "user",
                isPickedUp = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        pickupDao.deleteById(id)
    }

    suspend fun deleteItemsBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        pickupDao.deleteByIds(ids)
    }

    companion object {
        // 已取件记录与扫描去重统一保留 7 天
        const val RETENTION_SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
