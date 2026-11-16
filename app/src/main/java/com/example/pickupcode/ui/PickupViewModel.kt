package com.example.pickupcode.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pickupcode.data.local.AppSettings
import com.example.pickupcode.data.local.PickupItemEntity
import com.example.pickupcode.data.local.SettingsDataStore
import com.example.pickupcode.data.repository.PickupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PickupViewModel(
    private val repository: PickupRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _isSettingsLoaded = MutableStateFlow(false)
    val isSettingsLoaded: StateFlow<Boolean> = _isSettingsLoaded.asStateFlow()

    val itemsState: StateFlow<List<PickupItemEntity>> = repository.allItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .onEach { _isSettingsLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // 多选勾选的记录 ID 集合
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItemIds: StateFlow<Set<Long>> = _selectedItemIds.asStateFlow()

    // 确认已取后 3 秒内可撤销的记录 ID 列表
    private val _undoableItemIds = MutableStateFlow<List<Long>>(emptyList())
    val undoableItemIds: StateFlow<List<Long>> = _undoableItemIds.asStateFlow()

    private val _capsuleToastMessage = MutableStateFlow<String?>(null)
    val capsuleToastMessage: StateFlow<String?> = _capsuleToastMessage.asStateFlow()

    private var toastJob: Job? = null
    private var undoTimerJob: Job? = null
    private var isSmsScanRunning: Boolean = false
    private var lastSmsScanCompletedAtMs: Long = 0L

    fun showCapsuleToast(message: String) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            _capsuleToastMessage.value = message
            delay(2500L)
            _capsuleToastMessage.value = null
        }
    }

    /**
     * 每次打开应用（冷启动与热启动）自动扫描最近 7 天短信，自动排除已取快递并清理超过 7 天的已取记录
     */
    fun scanRecentSmsOnAppOpen(context: Context) {
        val now = System.currentTimeMillis()
        if (isSmsScanRunning || now - lastSmsScanCompletedAtMs < 1200L) return
        isSmsScanRunning = true
        viewModelScope.launch {
            try {
                val count = repository.scanRecentSmsInbox(context)
                lastSmsScanCompletedAtMs = System.currentTimeMillis()
                if (count > 0) {
                    showCapsuleToast("已自动同步 $count 条新取件码")
                }
            } finally {
                isSmsScanRunning = false
            }
        }
    }

    /**
     * 切换单条记录的勾选状态（支持多选，不自动沉底）
     */
    fun toggleSelectItem(item: PickupItemEntity) {
        val current = _selectedItemIds.value.toMutableSet()
        if (item.id in current) {
            current.remove(item.id)
        } else {
            current.add(item.id)
        }
        _selectedItemIds.value = current
    }

    /**
     * 点击底部中间「确认已取」按钮：
     * 勾选的快递立即消失，并在相同位置显示 3 秒内可点击的「撤销」按钮
     */
    fun confirmSelectedPickedUp() {
        val idsToConfirm = _selectedItemIds.value.toList()
        if (idsToConfirm.isEmpty()) return

        _selectedItemIds.value = emptySet()
        undoTimerJob?.cancel()

        viewModelScope.launch {
            repository.confirmPickedUpBatch(idsToConfirm)
            _undoableItemIds.value = idsToConfirm
            undoTimerJob = launch {
                delay(3000L)
                _undoableItemIds.value = emptyList()
            }
        }
    }

    /**
     * 左滑或长按单条取件码直接标记为已取（同样支持 3 秒底部撤销）
     */
    fun markSingleItemPickedUp(item: PickupItemEntity) {
        _selectedItemIds.value = _selectedItemIds.value - item.id
        undoTimerJob?.cancel()

        viewModelScope.launch {
            repository.confirmPickedUpBatch(listOf(item.id))
            _undoableItemIds.value = listOf(item.id)
            undoTimerJob = launch {
                delay(3000L)
                _undoableItemIds.value = emptyList()
            }
        }
    }

    /**
     * 3 秒内点击底部中间「撤销」按钮：恢复刚刚确认已取的快递
     */
    fun undoRecentPickedUp() {
        val idsToRestore = _undoableItemIds.value
        if (idsToRestore.isEmpty()) return

        undoTimerJob?.cancel()
        _undoableItemIds.value = emptyList()

        viewModelScope.launch {
            repository.undoPickedUpBatch(idsToRestore)
            showCapsuleToast("已撤销，恢复 ${idsToRestore.size} 件待取快递")
        }
    }

    /**
     * 从「7天内已取记录」中将单条已取快递恢复回待取列表
     */
    fun restorePickedUpItem(item: PickupItemEntity) {
        viewModelScope.launch {
            repository.undoPickedUpBatch(listOf(item.id))
            showCapsuleToast("已将 ${item.code} 恢复至待取列表")
        }
    }

    fun updateProductRemark(itemId: Long, productName: String) {
        viewModelScope.launch {
            repository.updateProductRemark(itemId, productName)
            showCapsuleToast(if (productName.isBlank()) "已清空商品备注" else "已备注商品：${productName.trim()}")
        }
    }

    fun updateStationNumber(itemId: Long, stationNumber: Int) {
        viewModelScope.launch {
            repository.updateStationNumber(itemId, stationNumber)
            val rule = settingsState.value.stationRules.firstOrNull { it.stationNumber == stationNumber }
            val symbol = rule?.circleSymbol ?: PickupItemEntity.stationCircleSymbolFor(stationNumber)
            val name = rule?.displayName ?: "${stationNumber}号快递房"
            showCapsuleToast("已归入 $symbol $name")
        }
    }

    fun deleteItem(item: PickupItemEntity) {
        viewModelScope.launch {
            _selectedItemIds.value = _selectedItemIds.value - item.id
            repository.deleteItem(item.id)
            showCapsuleToast("已删除取件码 ${item.code}")
        }
    }

    fun pasteAndParseSms(rawText: String) {
        viewModelScope.launch {
            val count = repository.ingestRawMessage(
                rawText = rawText,
                source = PickupItemEntity.SOURCE_MANUAL,
                senderOrPackage = "clipboard"
            )
            if (count > 0) {
                showCapsuleToast("智能解析成功：提取 $count 个取件码")
            } else {
                showCapsuleToast("未识别到有效新取件码（已排除已取或干扰短信）")
            }
        }
    }

    fun addManualPickupCode(code: String, stationNumber: Int, productName: String) {
        viewModelScope.launch {
            repository.addManualPickupCode(
                code = code,
                stationNumber = stationNumber,
                productName = productName
            )
            showCapsuleToast("已添加取件码 ${code.uppercase()}")
        }
    }

    fun markBadgePermissionPrompted() {
        viewModelScope.launch {
            settingsDataStore.setBadgePermissionPrompted(true)
        }
    }

    fun setShowTaobaoIdentityBanner(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowTaobaoIdentityBanner(enabled)
            showCapsuleToast(
                if (enabled) "已开启：淘宝身份码" else "已关闭：淘宝身份码"
            )
        }
    }

    fun setShowCainiaoIdentityBanner(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowCainiaoIdentityBanner(enabled)
            showCapsuleToast(
                if (enabled) "已开启：菜鸟身份码" else "已关闭：菜鸟身份码"
            )
        }
    }

    fun setShowPinduoduoIdentityBanner(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowPinduoduoIdentityBanner(enabled)
            showCapsuleToast(
                if (enabled) "已开启：拼多多取件码" else "已关闭：拼多多取件码"
            )
        }
    }

    fun exportStationRulesToClipboard(context: Context) {
        val rules = settingsState.value.stationRules
        val token = settingsDataStore.exportStationRulesToken(rules)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("取件码识别规则", token))
        showCapsuleToast("已复制识别规则口令到剪贴板，可直接分享给同学")
    }

    fun importStationRulesFromText(rawText: String) {
        viewModelScope.launch {
            val count = settingsDataStore.importStationRulesToken(rawText)
            if (count > 0) {
                repository.reapplyStationRules()
                showCapsuleToast("成功导入 $count 个快递房识别规则")
            } else {
                showCapsuleToast("未识别到有效的识别规则口令")
            }
        }
    }

    fun addStationRule(displayName: String, badgeNumber: Int?, keywords: List<String>) {
        viewModelScope.launch {
            val num = settingsDataStore.addStationRule(displayName, badgeNumber, keywords)
            repository.reapplyStationRules()
            val symbol = badgeNumber?.takeIf { it in 1..20 }?.let {
                PickupItemEntity.stationCircleSymbolFor(it)
            } ?: "●"
            showCapsuleToast("已添加 $symbol ${displayName.ifBlank { "${num}号快递房" }}")
        }
    }

    fun reorderStationRules(orderedRules: List<com.example.pickupcode.data.local.StationRule>) {
        viewModelScope.launch {
            settingsDataStore.reorderStationRules(orderedRules)
        }
    }

    fun deleteStationRule(stationNumber: Int) {
        viewModelScope.launch {
            val symbol = settingsState.value.stationRules
                .firstOrNull { it.stationNumber == stationNumber }
                ?.circleSymbol
                ?: PickupItemEntity.stationCircleSymbolFor(stationNumber)
            settingsDataStore.removeStationRule(stationNumber)
            repository.reapplyStationRules()
            showCapsuleToast("已删除 $symbol 快递房规则")
        }
    }

    fun addStationKeyword(stationNumber: Int, keyword: String) {
        viewModelScope.launch {
            settingsDataStore.addKeywordToStation(stationNumber, keyword)
            repository.reapplyStationRules()
            val symbol = settingsState.value.stationRules
                .firstOrNull { it.stationNumber == stationNumber }
                ?.circleSymbol
                ?: PickupItemEntity.stationCircleSymbolFor(stationNumber)
            showCapsuleToast("已将「$keyword」添加至 $symbol")
        }
    }

    fun removeStationKeyword(stationNumber: Int, keyword: String) {
        viewModelScope.launch {
            settingsDataStore.removeKeywordFromStation(stationNumber, keyword)
            repository.reapplyStationRules()
            showCapsuleToast("已移除关键词「$keyword」")
        }
    }

    fun updateStationRuleInfo(
        stationNumber: Int,
        displayName: String,
        badgeNumber: Int?,
        keywords: List<String>,
        pendingNewKeyword: String = ""
    ) {
        viewModelScope.launch {
            val extra = pendingNewKeyword.trim()
            val mergedKeywords = if (extra.isNotEmpty()) {
                (keywords + extra).distinct()
            } else {
                keywords
            }
            settingsDataStore.updateStationRule(stationNumber, displayName, badgeNumber, mergedKeywords)
            repository.reapplyStationRules()
            showCapsuleToast("已保存规则修改")
        }
    }

    fun resetStationRules() {
        viewModelScope.launch {
            settingsDataStore.resetStationRulesToDefault()
            repository.reapplyStationRules()
            showCapsuleToast("已恢复默认识别规则")
        }
    }

    class Factory(
        private val repository: PickupRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PickupViewModel(repository, settingsDataStore) as T
        }
    }
}
