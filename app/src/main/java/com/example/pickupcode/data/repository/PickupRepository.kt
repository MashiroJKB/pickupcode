package com.example.pickupcode.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupDao
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.SettingsDataStore
import com.example.pickupcode.data.local.StationRule
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
 * 2. 自动排除已取快递，历史取件记录永久保存在本地数据库
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
        preferredStationNumber: Int? = null,
        preloadedStationRules: List<StationRule>? = null
    ): Int = withContext(Dispatchers.IO) {
        val stationRules = preloadedStationRules ?: settingsDataStore.settingsFlow.first().stationRules
        val parsedList = PickupMessageParser.parse(
            rawText = rawText,
            stationRules = stationRules
        )
        if (parsedList.isEmpty()) return@withContext 0

        val effectiveParsedList = if (preferredStationNumber != null) {
            parsedList.map { parsed ->
                parsed.copy(
                    stationNumber = preferredStationNumber.coerceIn(0, 100),
                    stationRawName = PickupItemEntity.STATION_MANUAL_LOCK_TAG
                )
            }
        } else {
            parsedList
        }
        val effectiveSender = if (preferredStationNumber != null && source == PickupItemEntity.SOURCE_MANUAL) {
            "user"
        } else {
            senderOrPackage
        }

        ingestMutex.withLock {
            var affectedCount = 0
            val windowStart = minOf(messageTimestamp, System.currentTimeMillis()) -
                RETENTION_SEVEN_DAYS_MILLIS -
                DEDUPE_BUFFER_MILLIS

            for (parsed in effectiveParsedList) {
                val recentCandidates = pickupDao.findRecentByCode(
                    code = parsed.code,
                    sinceTimestamp = windowStart
                )

                val matchedExisting = recentCandidates.firstOrNull { existing ->
                    val stationMatches = existing.stationNumber == parsed.stationNumber ||
                        existing.stationNumber == 0 ||
                        parsed.stationNumber == 0
                    if (!stationMatches) return@firstOrNull false
                    if (existing.isPickedUp && !shouldSkipPickedUpDuplicate(messageTimestamp, existing.pickedUpAt)) {
                        return@firstOrNull false
                    }
                    true
                }

                if (matchedExisting != null) {
                    // 核心规则：若该取件码已被标记为「已取件」且本次短信并非取件后的新包裹，自动跳过排除，绝不重新恢复为待取
                    if (matchedExisting.isPickedUp) {
                        continue
                    }

                    val merged = mergeExistingWithParsed(
                        existing = matchedExisting,
                        parsedStationNumber = parsed.stationNumber,
                        parsedStationRawName = parsed.stationRawName,
                        parsedProductName = parsed.productName,
                        parsedRawMessage = parsed.rawMessage,
                        forcePreferredStation = preferredStationNumber != null
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
                            senderOrPackage = effectiveSender,
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
        parsedRawMessage: String,
        forcePreferredStation: Boolean = false
    ): PickupItemEntity {
        val isManualStationLocked = existing.stationRawName == PickupItemEntity.STATION_MANUAL_LOCK_TAG ||
            (existing.source == PickupItemEntity.SOURCE_MANUAL && existing.senderOrPackage == "user")

        val finalStationNumber = when {
            forcePreferredStation -> parsedStationNumber
            isManualStationLocked -> existing.stationNumber
            existing.stationNumber > 0 -> existing.stationNumber
            parsedStationNumber > 0 -> parsedStationNumber
            else -> 0
        }

        val finalStationRawName = when {
            forcePreferredStation -> PickupItemEntity.STATION_MANUAL_LOCK_TAG
            isManualStationLocked -> existing.stationRawName
            else -> existing.stationRawName.ifBlank { parsedStationRawName }
        }

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
     * 扫描系统收件箱最近 7 天的历史短信（历史已取件记录永久保存，不再过期清理）
     */
    suspend fun scanRecentSmsInbox(
        context: Context,
        daysOverride: Int? = null
    ): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // 清理旧版遗留的示例数据
        pickupDao.deleteLegacyDemoItems()

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasSmsPermission) return@withContext 0

        val scanDays = daysOverride ?: SettingsDataStore.DEFAULT_SMS_SCAN_DAYS
        val sinceTimestamp = now - scanDays * 24L * 60L * 60L * 1000L
        val stationRules = settingsDataStore.settingsFlow.first().stationRules

        var totalAffected = 0
        totalAffected += scanSmsProvider(
            context = context,
            sinceTimestamp = sinceTimestamp,
            now = now,
            stationRules = stationRules
        )
        totalAffected += scanMmsProvider(
            context = context,
            sinceTimestamp = sinceTimestamp,
            now = now,
            stationRules = stationRules
        )

        totalAffected
    }

    /**
     * 扫描系统 SMS 表：优先扫描 content://sms 中所有非发件/草稿类型（兼容部分 ROM 将服务/增强短信归入自定义接收类型），
     * 失败时自动回退至标准 content://sms/inbox。
     */
    private suspend fun scanSmsProvider(
        context: Context,
        sinceTimestamp: Long,
        now: Long,
        stationRules: List<StationRule>
    ): Int {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val sortOrder = "${Telephony.Sms.DATE} ASC"
        val nonOutgoingSelection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.TYPE} NOT IN (2, 3, 4, 5, 6)"
        val inboxSelection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())

        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                nonOutgoingSelection,
                selectionArgs,
                sortOrder
            )
        } catch (_: Exception) {
            null
        } ?: try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                inboxSelection,
                selectionArgs,
                sortOrder
            )
        } catch (_: Exception) {
            null
        } ?: return 0

        var totalAffected = 0
        try {
            cursor.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    val address = if (addressIdx >= 0) c.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty() else ""
                    val rawDate = if (dateIdx >= 0) c.getLong(dateIdx) else now
                    val date = normalizeProviderTimestampMillis(rawDate, now)

                    if (body.isNotBlank()) {
                        totalAffected += ingestRawMessage(
                            rawText = body,
                            source = PickupItemEntity.SOURCE_SMS,
                            senderOrPackage = address,
                            messageTimestamp = date,
                            preloadedStationRules = stationRules
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // 忽略特定定制 ROM ContentProvider 异常
        }
        return totalAffected
    }

    /**
     * 补充扫描系统 MMS 收件箱（content://mms/inbox + content://mms/part）：
     * 兼容部分运营商/ROM 将 5G消息 / 增强信息 / 富媒体取件通知回落存储至 MMS 表的场景（复用 READ_SMS 权限）。
     */
    private suspend fun scanMmsProvider(
        context: Context,
        sinceTimestamp: Long,
        now: Long,
        stationRules: List<StationRule>
    ): Int {
        var totalAffected = 0
        val sinceSeconds = sinceTimestamp / 1000L
        val mmsProjection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.SUBJECT
        )
        val mmsSelection = "${Telephony.Mms.DATE} >= ?"
        val mmsSelectionArgs = arrayOf(sinceSeconds.toString())
        val mmsSortOrder = "${Telephony.Mms.DATE} ASC"

        try {
            context.contentResolver.query(
                Telephony.Mms.Inbox.CONTENT_URI,
                mmsProjection,
                mmsSelection,
                mmsSelectionArgs,
                mmsSortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(Telephony.Mms._ID)
                val dateIdx = cursor.getColumnIndex(Telephony.Mms.DATE)
                val subjectIdx = cursor.getColumnIndex(Telephony.Mms.SUBJECT)

                while (cursor.moveToNext()) {
                    if (idIdx < 0) continue
                    val mmsId = cursor.getLong(idIdx)
                    val rawDate = if (dateIdx >= 0) cursor.getLong(dateIdx) else now
                    val timestampMs = normalizeProviderTimestampMillis(rawDate, now)
                    val subject = if (subjectIdx >= 0) cursor.getString(subjectIdx).orEmpty().trim() else ""
                    val bodyText = readMmsPlainTextParts(context, mmsId)
                    val combined = listOf(subject, bodyText)
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    if (combined.isNotBlank()) {
                        totalAffected += ingestRawMessage(
                            rawText = combined,
                            source = PickupItemEntity.SOURCE_SMS,
                            senderOrPackage = "MMS",
                            messageTimestamp = timestampMs,
                            preloadedStationRules = stationRules
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // 忽略特定定制 ROM 不支持 MMS 查询的异常
        }
        return totalAffected
    }

    private fun readMmsPlainTextParts(context: Context, mmsId: Long): String {
        val parts = mutableListOf<String>()
        val partUri = Uri.parse("content://mms/part")
        val projection = arrayOf("_id", "ct", "text", "_data")
        try {
            context.contentResolver.query(
                partUri,
                projection,
                "mid = ?",
                arrayOf(mmsId.toString()),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val ctIdx = cursor.getColumnIndex("ct")
                val textIdx = cursor.getColumnIndex("text")
                val dataIdx = cursor.getColumnIndex("_data")

                while (cursor.moveToNext()) {
                    val contentType = if (ctIdx >= 0) cursor.getString(ctIdx).orEmpty().lowercase() else ""
                    if (contentType != "text/plain" && !contentType.startsWith("text/plain;")) {
                        continue
                    }
                    val inlineText = if (textIdx >= 0) cursor.getString(textIdx).orEmpty().trim() else ""
                    if (inlineText.isNotEmpty()) {
                        parts.add(inlineText)
                    } else if (idIdx >= 0 && dataIdx >= 0 && !cursor.getString(dataIdx).isNullOrBlank()) {
                        val partId = cursor.getLong(idIdx)
                        val streamText = readMmsPartStreamUtf8(context, partId)
                        if (streamText.isNotEmpty()) {
                            parts.add(streamText)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return parts.joinToString("\n")
    }

    private fun readMmsPartStreamUtf8(context: Context, partId: Long): String {
        return try {
            val uri = Uri.parse("content://mms/part/$partId")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(MAX_MMS_TEXT_PART_BYTES)
                val read = input.read(buffer)
                if (read > 0) String(buffer, 0, read, Charsets.UTF_8).trim() else ""
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * 重新应用当前识别规则到所有未取件记录（当用户修改了识别规则或增删了快递房时自动重分类）
     */
    suspend fun reapplyStationRules() = withContext(Dispatchers.IO) {
        ingestMutex.withLock {
            val settings = settingsDataStore.settingsFlow.first()
            val activeNumbers = settings.stationRules.map { it.stationNumber }.toSet()
            val allItems = pickupDao.observeAllItems().first()
            for (item in allItems) {
                if (item.isPickedUp) continue
                val isManualStationLocked = item.stationRawName == PickupItemEntity.STATION_MANUAL_LOCK_TAG ||
                    (item.source == PickupItemEntity.SOURCE_MANUAL && item.senderOrPackage == "user")
                // 用户手动指定过快递房的记录：仅在该快递房规则本身被删除时才重置回待分类
                if (isManualStationLocked) {
                    if (item.stationNumber > 0 && item.stationNumber !in activeNumbers) {
                        pickupDao.update(
                            item.copy(
                                stationNumber = 0,
                                stationRawName = "",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    continue
                }
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
    }

    /**
     * 批量确认已取件（确认后从待取列表中消失，并永久保存至本地历史包裹）
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
     * 手动新增一条取件码记录（若同一快递房已存在未取同码则原地合并备注并返回 false，避免重复插入）
     */
    suspend fun addManualPickupCode(
        code: String,
        stationNumber: Int,
        productName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isEmpty()) return@withContext false
        val safeStation = stationNumber.coerceIn(0, 100)
        val settings = settingsDataStore.settingsFlow.first()
        val stationLabel = if (safeStation > 0) {
            settings.stationRules.firstOrNull { it.stationNumber == safeStation }?.displayName
                ?: "${safeStation}号快递房"
        } else {
            "待分类快递房"
        }
        val cleanProduct = productName.trim()
        val now = System.currentTimeMillis()
        ingestMutex.withLock {
            val recentCandidates = pickupDao.findRecentByCode(
                code = cleanCode,
                sinceTimestamp = now - RETENTION_SEVEN_DAYS_MILLIS - DEDUPE_BUFFER_MILLIS
            )
            val existingPending = recentCandidates.firstOrNull { candidate ->
                !candidate.isPickedUp && candidate.stationNumber == safeStation
            }
            if (existingPending != null) {
                if (cleanProduct.isNotEmpty() && existingPending.productName != cleanProduct) {
                    pickupDao.update(
                        existingPending.copy(
                            productName = cleanProduct,
                            isCustomRemark = true,
                            updatedAt = now
                        )
                    )
                }
                return@withLock false
            }

            pickupDao.insert(
                PickupItemEntity(
                    code = cleanCode,
                    stationNumber = safeStation,
                    stationRawName = PickupItemEntity.STATION_MANUAL_LOCK_TAG,
                    expressCompany = "",
                    productName = cleanProduct,
                    isCustomRemark = cleanProduct.isNotEmpty(),
                    rawMessage = "【手动录入】取件码 $cleanCode，地点：$stationLabel${
                        if (cleanProduct.isNotEmpty()) "，商品：$cleanProduct" else ""
                    }",
                    source = PickupItemEntity.SOURCE_MANUAL,
                    senderOrPackage = "user",
                    isPickedUp = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            true
        }
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        pickupDao.deleteById(id)
    }

    suspend fun deleteItemsBatch(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        pickupDao.deleteByIds(ids)
    }

    companion object {
        // 扫描去重窗口覆盖最近 7 天（并预留 24 小时边界缓冲），历史已取记录永久保存在本地数据库
        const val RETENTION_SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L
        private const val DEDUPE_BUFFER_MILLIS = 24L * 60L * 60L * 1000L
        private const val PICKED_UP_REUSE_GRACE_MILLIS = 60_000L
        private const val MAX_MMS_TEXT_PART_BYTES = 8 * 1024

        /**
         * 判断已取件记录是否应拦截本次入库：
         * - 重扫旧短信（时间戳早于或等于取件时间 + 1 分钟容差）必须跳过，防止已取包裹复活；
         * - 若新短信到达时间严格晚于上次确认已取时间 + 1 分钟，视为驿站复用了同一货架取件码的新包裹，允许正常入库。
         */
        internal fun shouldSkipPickedUpDuplicate(
            messageTimestamp: Long,
            pickedUpAt: Long?
        ): Boolean {
            if (pickedUpAt == null) return true
            return messageTimestamp <= pickedUpAt + PICKED_UP_REUSE_GRACE_MILLIS
        }

        /**
         * 统一 SMS（毫秒）与 MMS（秒）ContentProvider 的时间戳为毫秒
         */
        internal fun normalizeProviderTimestampMillis(
            rawTimestamp: Long,
            fallbackNowMillis: Long
        ): Long {
            return when {
                rawTimestamp <= 0L -> fallbackNowMillis
                rawTimestamp < 10_000_000_000L -> rawTimestamp * 1000L
                else -> rawTimestamp
            }
        }
    }
}
