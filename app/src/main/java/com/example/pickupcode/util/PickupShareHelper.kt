package com.example.pickupcode.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.StationRule
import com.example.pickupcode.ui.theme.stationAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShareStationGroup(
    val stationNumber: Int,
    val badgeNumber: Int?,
    val circleSymbol: String,
    val displayName: String,
    val items: List<PickupItemEntity>
)

object PickupShareHelper {

    fun groupItemsForShare(
        items: List<PickupItemEntity>,
        stationRules: List<StationRule>
    ): List<ShareStationGroup> {
        val rulesByNum = stationRules.associateBy { it.stationNumber }
        val orderMap = stationRules.mapIndexed { index, rule -> rule.stationNumber to index }.toMap()

        return items
            .filter { !it.isPickedUp }
            .groupBy { it.stationNumber }
            .entries
            .sortedBy { (stationNum, _) ->
                if (stationNum == 0) -1
                else orderMap[stationNum] ?: (1000 + stationNum)
            }
            .map { (stationNum, groupItems) ->
                val rule = rulesByNum[stationNum]
                val displayName = if (stationNum > 0) {
                    rule?.displayName ?: "${stationNum}号快递房"
                } else {
                    "待分类快递房"
                }
                val badgeNumber = if (stationNum > 0) {
                    if (rule != null) rule.badgeNumber else stationNum
                } else {
                    null
                }
                val symbol = if (stationNum > 0) {
                    rule?.circleSymbol ?: PickupItemEntity.stationCircleSymbolFor(stationNum)
                } else {
                    "？"
                }
                ShareStationGroup(
                    stationNumber = stationNum,
                    badgeNumber = badgeNumber,
                    circleSymbol = symbol,
                    displayName = displayName,
                    items = groupItems.sortedWith(PickupItemEntity.NaturalCodeComparator)
                )
            }
    }

    suspend fun launchImageShare(
        context: Context,
        items: List<PickupItemEntity>,
        stationRules: List<StationRule>,
        includeRemarks: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val groups = groupItemsForShare(items, stationRules)
        if (groups.isEmpty()) return@withContext false

        val uri = try {
            val bitmap = renderShareBitmap(groups, includeRemarks = includeRemarks)
            val cacheDir = File(context.cacheDir, "shared_images").apply {
                mkdirs()
                listFiles()?.forEach { oldFile ->
                    if (oldFile.isFile) oldFile.delete()
                }
            }
            val file = File(cacheDir, "pickup_codes_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        } ?: return@withContext false

        withContext(Dispatchers.Main) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "取件码清单图片", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "分享取件码图片").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun renderShareBitmap(
        groups: List<ShareStationGroup>,
        includeRemarks: Boolean
    ): Bitmap {
        val width = 1080
        val outerPad = 56f
        val cardWidth = width - outerPad * 2f
        val cardInnerPadX = 36f
        val cardInnerPadY = 30f
        val headerHeight = 148f
        val groupGap = 28f
        val groupHeaderH = 68f
        val itemRowSpacing = 18f
        val footerHeight = 96f

        val nowMs = System.currentTimeMillis()
        val timeText = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(nowMs))
        val totalCount = groups.sumOf { it.items.size }

        fun rowHeightFor(item: PickupItemEntity): Float {
            return if (includeRemarks && item.productName.isNotBlank()) 114f else 76f
        }

        var contentHeight = outerPad + headerHeight
        groups.forEachIndexed { idx, group ->
            if (idx > 0) contentHeight += groupGap
            val itemsHeight = group.items.sumOf { rowHeightFor(it).toDouble() }.toFloat() +
                (group.items.size - 1).coerceAtLeast(0) * itemRowSpacing
            contentHeight += cardInnerPadY * 2f + groupHeaderH + 16f + itemsHeight
        }
        contentHeight += footerHeight + 24f

        val height = contentHeight.toInt().coerceAtLeast(480)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景液态玻璃淡雅渐变
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFFF3F7FD.toInt(), 0xFFE9F0FA.toInt(), 0xFFF4F7FC.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 顶部柔和蓝色光晕
        val glowPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.16f, height * 0.12f, width * 0.85f,
                intArrayOf(0x28007AFF, 0x00007AFF),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.16f, height * 0.12f, width * 0.85f, glowPaint1)

        // 标题与件数汇总
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pillTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val titleY = outerPad + 56f
        canvas.drawText("待取快递清单", outerPad, titleY, titlePaint)
        canvas.drawText("生成时间 $timeText", outerPad, titleY + 46f, subtitlePaint)

        val countLabel = "共 $totalCount 件"
        val countTextW = pillTextPaint.measureText(countLabel)
        val countPillW = countTextW + 44f
        val countPillH = 56f
        val countPillRect = RectF(
            width - outerPad - countPillW,
            outerPad + 14f,
            width - outerPad,
            outerPad + 14f + countPillH
        )
        pillBgPaint.color = 0xFF007AFF.toInt()
        canvas.drawRoundRect(countPillRect, countPillH / 2f, countPillH / 2f, pillBgPaint)
        pillTextPaint.color = 0xFFFFFFFF.toInt()
        val pillBaseY = countPillRect.centerY() - (pillTextPaint.descent() + pillTextPaint.ascent()) / 2f
        canvas.drawText(countLabel, countPillRect.left + 22f, pillBaseY, pillTextPaint)

        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFAFFFFFF.toInt()
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFFD6E0ED.toInt()
        }
        val rowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
        }
        val rowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = 0xFFE2E8F0.toInt()
        }
        val stationTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val symbolPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0F172A.toInt()
            textSize = 44f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        val remarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF475569.toInt()
            textSize = 27f
        }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var cursorY = outerPad + headerHeight
        groups.forEach { group ->
            val accentColor = stationAccentColor(group.badgeNumber ?: group.stationNumber, isDark = false).toArgb()
            val itemsHeight = group.items.sumOf { rowHeightFor(it).toDouble() }.toFloat() +
                (group.items.size - 1).coerceAtLeast(0) * itemRowSpacing
            val cardH = cardInnerPadY * 2f + groupHeaderH + 16f + itemsHeight

            val cardRect = RectF(outerPad, cursorY, outerPad + cardWidth, cursorY + cardH)
            canvas.drawRoundRect(cardRect, 36f, 36f, cardFillPaint)
            canvas.drawRoundRect(cardRect, 36f, 36f, cardBorderPaint)

            // 快递房标题行
            val headerCenterY = cursorY + cardInnerPadY + groupHeaderH / 2f
            val badgeStartX = outerPad + cardInnerPadX
            if (group.stationNumber == 0 || (group.badgeNumber != null && group.badgeNumber in 1..20)) {
                symbolPaint.color = accentColor
                val baseY = headerCenterY - (symbolPaint.descent() + symbolPaint.ascent()) / 2f
                canvas.drawText(group.circleSymbol, badgeStartX, baseY, symbolPaint)
            } else {
                pillBgPaint.color = accentColor
                canvas.drawCircle(badgeStartX + 16f, headerCenterY, 15f, pillBgPaint)
            }

            val countText = "${group.items.size} 件"
            pillTextPaint.textSize = 25f
            val groupPillW = pillTextPaint.measureText(countText) + 34f
            val groupPillRect = RectF(
                outerPad + cardWidth - cardInnerPadX - groupPillW,
                headerCenterY - 22f,
                outerPad + cardWidth - cardInnerPadX,
                headerCenterY + 22f
            )
            pillBgPaint.color = accentColor
            canvas.drawRoundRect(groupPillRect, 22f, 22f, pillBgPaint)
            pillTextPaint.color = 0xFFFFFFFF.toInt()
            val gpBaseY = groupPillRect.centerY() - (pillTextPaint.descent() + pillTextPaint.ascent()) / 2f
            canvas.drawText(countText, groupPillRect.left + 17f, gpBaseY, pillTextPaint)

            val maxTitleW = (groupPillRect.left - (badgeStartX + 48f) - 18f).coerceAtLeast(100f)
            val ellipsizedStation = TextUtils.ellipsize(
                group.displayName,
                stationTitlePaint,
                maxTitleW,
                TextUtils.TruncateAt.END
            ).toString()
            val stBaseY = headerCenterY - (stationTitlePaint.descent() + stationTitlePaint.ascent()) / 2f
            canvas.drawText(ellipsizedStation, badgeStartX + 46f, stBaseY, stationTitlePaint)

            var rowY = cursorY + cardInnerPadY + groupHeaderH + 16f
            group.items.forEach { item ->
                val rh = rowHeightFor(item)
                val rowRect = RectF(
                    outerPad + cardInnerPadX,
                    rowY,
                    outerPad + cardWidth - cardInnerPadX,
                    rowY + rh
                )
                canvas.drawRoundRect(rowRect, 24f, 24f, rowFillPaint)
                canvas.drawRoundRect(rowRect, 24f, 24f, rowBorderPaint)

                val rowLeft = rowRect.left + 26f
                val rowRight = rowRect.right - 26f
                val retentionDays = ((nowMs - item.createdAt) / 3_600_000L / 24L).toInt()
                val showRemark = includeRemarks && item.productName.isNotBlank()

                var tagWidth = 0f
                val tagLabel = if (retentionDays >= 1) "已到 $retentionDays 天" else ""
                if (tagLabel.isNotEmpty()) {
                    tagWidth = badgeTextPaint.measureText(tagLabel) + 28f + 16f
                }

                val maxCodeWidth = (rowRight - rowLeft - tagWidth).coerceAtLeast(180f)
                val codeText = TextUtils.ellipsize(
                    item.code,
                    codePaint,
                    maxCodeWidth,
                    TextUtils.TruncateAt.END
                ).toString()

                val codeCenterY = if (showRemark) rowRect.top + 42f else rowRect.centerY()
                val codeBaseY = codeCenterY - (codePaint.descent() + codePaint.ascent()) / 2f
                canvas.drawText(codeText, rowLeft, codeBaseY, codePaint)

                if (tagLabel.isNotEmpty()) {
                    val codeMeasuredW = codePaint.measureText(codeText)
                    val tagLeft = (rowLeft + codeMeasuredW + 16f).coerceAtMost(rowRight - (tagWidth - 16f))
                    val tagRect = RectF(
                        tagLeft,
                        codeCenterY - 19f,
                        tagLeft + (tagWidth - 16f),
                        codeCenterY + 19f
                    )
                    val isUrgent = retentionDays >= 2
                    pillBgPaint.color = if (isUrgent) 0x26FF3B30 else 0x26FF9500
                    canvas.drawRoundRect(tagRect, 10f, 10f, pillBgPaint)
                    badgeTextPaint.color = if (isUrgent) 0xFFFF3B30.toInt() else 0xFFFF9500.toInt()
                    val tBaseY = tagRect.centerY() - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
                    canvas.drawText(tagLabel, tagRect.left + 14f, tBaseY, badgeTextPaint)
                }

                if (showRemark) {
                    val ellipsizedRemark = TextUtils.ellipsize(
                        item.productName,
                        remarkPaint,
                        rowRight - rowLeft,
                        TextUtils.TruncateAt.END
                    ).toString()
                    canvas.drawText(ellipsizedRemark, rowLeft, rowRect.top + 92f, remarkPaint)
                }

                rowY += rh + itemRowSpacing
            }

            cursorY += cardH + groupGap
        }

        // 底部水印
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "取件码 · 纯本地离线生成",
            width / 2f,
            height - 46f,
            footerPaint
        )

        return bitmap
    }
}
