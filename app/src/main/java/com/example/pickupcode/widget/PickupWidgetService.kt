package com.example.pickupcode.widget

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.pickupcode.PickupApplication
import com.example.pickupcode.R
import com.example.pickupcode.data.local.PickupItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal data class WidgetDisplayRow(
    val id: Long,
    val stationHeaderText: String,
    val code: String,
    val productName: String
)

internal data class WidgetSnapshot(
    val totalPendingCount: Int,
    val rows: List<WidgetDisplayRow>,
    val showCainiaoApp: Boolean = false,
    val showCainiao: Boolean = false,
    val showTaobao: Boolean = true,
    val showPinduoduo: Boolean = false,
    val showJd: Boolean = false
)

private const val MAX_WIDGET_ROWS = 100

internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot =
    withContext(Dispatchers.IO) {
        val app = context.applicationContext as? PickupApplication
            ?: return@withContext WidgetSnapshot(0, emptyList())
        try {
            val allItems = app.repository.allItemsFlow.first().filter { !it.isPickedUp }
            val totalCount = allItems.size
            val settings = app.repository.settingsFlow.first()
            val rulesByNum = settings.stationRules.associateBy { it.stationNumber }
            val orderMap = settings.stationRules.mapIndexed { idx, r -> r.stationNumber to idx }.toMap()

            val sortedGroups = allItems
                .groupBy { it.stationNumber }
                .entries
                .sortedBy { (stationNum, _) ->
                    if (stationNum == 0) -1
                    else orderMap[stationNum] ?: (1000 + stationNum)
                }

            val result = mutableListOf<WidgetDisplayRow>()
            for ((stationNum, groupItems) in sortedGroups) {
                val rule = rulesByNum[stationNum]
                val stationLabel = if (stationNum > 0) {
                    val symbol = rule?.circleSymbol ?: PickupItemEntity.stationCircleSymbolFor(stationNum)
                    val name = rule?.displayName ?: "${stationNum}号快递房"
                    "$symbol $name"
                } else {
                    "？ 待分类快递房"
                }

                val sortedItems = groupItems.sortedWith(PickupItemEntity.NaturalCodeComparator)
                for (item in sortedItems) {
                    result.add(
                        WidgetDisplayRow(
                            id = item.id,
                            stationHeaderText = stationLabel,
                            code = item.code,
                            productName = item.productName
                        )
                    )
                    if (result.size >= MAX_WIDGET_ROWS) {
                        return@withContext WidgetSnapshot(
                            totalPendingCount = totalCount,
                            rows = result,
                            showCainiaoApp = settings.showCainiaoAppIdentityBanner,
                            showCainiao = settings.showCainiaoIdentityBanner,
                            showTaobao = settings.showTaobaoIdentityBanner,
                            showPinduoduo = settings.showPinduoduoIdentityBanner,
                            showJd = settings.showJdIdentityBanner
                        )
                    }
                }
            }
            WidgetSnapshot(
                totalPendingCount = totalCount,
                rows = result,
                showCainiaoApp = settings.showCainiaoAppIdentityBanner,
                showCainiao = settings.showCainiaoIdentityBanner,
                showTaobao = settings.showTaobaoIdentityBanner,
                showPinduoduo = settings.showPinduoduoIdentityBanner,
                showJd = settings.showJdIdentityBanner
            )
        } catch (_: Exception) {
            WidgetSnapshot(0, emptyList())
        }
    }

internal fun buildWidgetItemRemoteViews(
    context: Context,
    row: WidgetDisplayRow
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_pickup_item)

    views.setTextViewText(R.id.widget_item_station, row.stationHeaderText)
    views.setTextViewTextSize(R.id.widget_item_station, TypedValue.COMPLEX_UNIT_DIP, 11f)
    views.setTextViewText(R.id.widget_item_code, row.code)
    views.setTextViewTextSize(R.id.widget_item_code, TypedValue.COMPLEX_UNIT_DIP, 17f)
    views.setTextViewTextSize(R.id.widget_item_btn_done, TypedValue.COMPLEX_UNIT_DIP, 11f)

    if (row.productName.isNotBlank()) {
        views.setTextViewText(R.id.widget_item_product, "· ${row.productName}")
        views.setTextViewTextSize(R.id.widget_item_product, TypedValue.COMPLEX_UNIT_DIP, 11f)
        views.setViewVisibility(R.id.widget_item_product, View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.widget_item_product, View.GONE)
    }

    // 点击整行打开主应用
    val openFillIn = Intent().apply {
        putExtra(PickupWidgetProvider.EXTRA_ITEM_ID, row.id)
        putExtra(PickupWidgetProvider.EXTRA_MARK_DONE, false)
    }
    views.setOnClickFillInIntent(R.id.widget_item_root, openFillIn)

    // 点击右侧「已取」按钮直接在桌面完成取件
    val doneFillIn = Intent().apply {
        putExtra(PickupWidgetProvider.EXTRA_ITEM_ID, row.id)
        putExtra(PickupWidgetProvider.EXTRA_MARK_DONE, true)
    }
    views.setOnClickFillInIntent(R.id.widget_item_btn_done, doneFillIn)

    return views
}

class PickupWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PickupRemoteViewsFactory(applicationContext)
    }
}

private class PickupRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetDisplayRow> = emptyList()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        rows = runBlocking(Dispatchers.IO) {
            loadWidgetSnapshot(context).rows
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? {
        val row = rows.getOrNull(position) ?: return null
        return buildWidgetItemRemoteViews(context, row)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
