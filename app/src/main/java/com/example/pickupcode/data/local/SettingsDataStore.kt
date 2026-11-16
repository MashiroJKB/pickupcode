package com.example.pickupcode.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pickup_settings")

/**
 * 单个快递房的映射规则配置
 */
data class StationRule(
    val stationNumber: Int,
    val displayName: String,
    val keywords: List<String>
) {
    val circleSymbol: String
        get() = PickupItemEntity.stationCircleSymbolFor(stationNumber)
}

/**
 * 应用全局设置快照
 * - useTaobaoExitCode: 默认 true 使用「淘宝出库码」，false 使用「菜鸟出库码」
 */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val useTaobaoExitCode: Boolean = true,
    val stationRules: List<StationRule> = SettingsDataStore.defaultStationRules()
)

class SettingsDataStore(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false
        // 默认使用淘宝出库码 (true)
        val useTaobaoExitCode = prefs[KEY_USE_TAOBAO_EXIT_CODE] ?: true
        val rulesJson = prefs[KEY_STATION_RULES_JSON]
        val stationRules = if (rulesJson.isNullOrBlank()) {
            defaultStationRules()
        } else {
            parseStationRulesJson(rulesJson)
        }

        AppSettings(
            onboardingCompleted = onboardingCompleted,
            useTaobaoExitCode = useTaobaoExitCode,
            stationRules = stationRules
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setUseTaobaoExitCode(useTaobao: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_TAOBAO_EXIT_CODE] = useTaobao
        }
    }

    suspend fun updateStationRule(
        stationNumber: Int,
        displayName: String,
        keywords: List<String>
    ) {
        context.dataStore.edit { prefs ->
            val currentRules = prefs[KEY_STATION_RULES_JSON]
                ?.let { parseStationRulesJson(it) }
                ?: defaultStationRules()
            val cleanedKeywords = keywords
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val updated = currentRules.map { rule ->
                if (rule.stationNumber == stationNumber) {
                    rule.copy(
                        displayName = displayName.trim().ifEmpty { "${stationNumber}号快递房" },
                        keywords = cleanedKeywords
                    )
                } else {
                    rule
                }
            }
            prefs[KEY_STATION_RULES_JSON] = serializeStationRulesJson(updated)
        }
    }

    /**
     * 新增快递房规则（自动分配最小可用编号 1~20）
     */
    suspend fun addStationRule(
        customName: String,
        initialKeywords: List<String>
    ): Int {
        var assignedNum = 1
        context.dataStore.edit { prefs ->
            val currentRules = prefs[KEY_STATION_RULES_JSON]
                ?.let { parseStationRulesJson(it) }
                ?: defaultStationRules()
            val usedNumbers = currentRules.map { it.stationNumber }.toSet()
            assignedNum = (1..20).firstOrNull { it !in usedNumbers }
                ?: ((usedNumbers.maxOrNull() ?: 7) + 1).coerceAtMost(20)

            val finalName = customName.trim().ifEmpty { "${assignedNum}号快递房" }
            val defaultKws = defaultKeywordsForNumber(assignedNum)
            val mergedKeywords = (initialKeywords.map { it.trim() }.filter { it.isNotEmpty() } +
                if (initialKeywords.isEmpty()) defaultKws else emptyList())
                .distinct()

            val updated = (currentRules + StationRule(
                stationNumber = assignedNum,
                displayName = finalName,
                keywords = mergedKeywords.ifEmpty { listOf(finalName) }
            )).sortedBy { it.stationNumber }

            prefs[KEY_STATION_RULES_JSON] = serializeStationRulesJson(updated)
        }
        return assignedNum
    }

    /**
     * 删除指定快递房规则
     */
    suspend fun removeStationRule(stationNumber: Int) {
        context.dataStore.edit { prefs ->
            val currentRules = prefs[KEY_STATION_RULES_JSON]
                ?.let { parseStationRulesJson(it) }
                ?: defaultStationRules()
            val updated = currentRules.filterNot { it.stationNumber == stationNumber }
            prefs[KEY_STATION_RULES_JSON] = serializeStationRulesJson(updated)
        }
    }

    suspend fun addKeywordToStation(stationNumber: Int, keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val currentRules = prefs[KEY_STATION_RULES_JSON]
                ?.let { parseStationRulesJson(it) }
                ?: defaultStationRules()
            val updated = currentRules.map { rule ->
                if (rule.stationNumber == stationNumber) {
                    val newKeywords = (rule.keywords + trimmed).distinct()
                    rule.copy(keywords = newKeywords)
                } else {
                    rule
                }
            }
            prefs[KEY_STATION_RULES_JSON] = serializeStationRulesJson(updated)
        }
    }

    suspend fun removeKeywordFromStation(stationNumber: Int, keyword: String) {
        context.dataStore.edit { prefs ->
            val currentRules = prefs[KEY_STATION_RULES_JSON]
                ?.let { parseStationRulesJson(it) }
                ?: defaultStationRules()
            val updated = currentRules.map { rule ->
                if (rule.stationNumber == stationNumber) {
                    rule.copy(keywords = rule.keywords.filterNot { it.equals(keyword, ignoreCase = true) })
                } else {
                    rule
                }
            }
            prefs[KEY_STATION_RULES_JSON] = serializeStationRulesJson(updated)
        }
    }

    suspend fun resetStationRulesToDefault() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_STATION_RULES_JSON)
        }
    }

    companion object {
        const val DEFAULT_SMS_SCAN_DAYS = 7

        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_USE_TAOBAO_EXIT_CODE = booleanPreferencesKey("use_taobao_exit_code_v2")
        private val KEY_STATION_RULES_JSON = stringPreferencesKey("station_rules_json")

        private val CHINESE_NUMERALS = mapOf(
            1 to "一",
            2 to "二",
            3 to "三",
            4 to "四",
            5 to "五",
            6 to "六",
            7 to "七",
            8 to "八",
            9 to "九",
            10 to "十"
        )

        fun defaultKeywordsForNumber(num: Int): List<String> {
            val cn = CHINESE_NUMERALS[num]
            return buildList {
                add("${num}号快递房")
                add("${num}号房")
                add("${num}号驿站")
                add("${num}号快递站")
                add("${num}号菜鸟")
                if (cn != null) {
                    add("${cn}号快递房")
                    add("${cn}号房")
                    add("${cn}号驿站")
                }
            }
        }

        fun defaultStationRules(): List<StationRule> {
            return (1..7).map { num ->
                StationRule(
                    stationNumber = num,
                    displayName = "${num}号快递房",
                    keywords = defaultKeywordsForNumber(num)
                )
            }
        }

        private fun serializeStationRulesJson(rules: List<StationRule>): String {
            val array = JSONArray()
            for (rule in rules) {
                val obj = JSONObject()
                obj.put("stationNumber", rule.stationNumber)
                obj.put("displayName", rule.displayName)
                val kwArray = JSONArray()
                rule.keywords.forEach { kwArray.put(it) }
                obj.put("keywords", kwArray)
                array.put(obj)
            }
            return array.toString()
        }

        private fun parseStationRulesJson(json: String): List<StationRule> {
            return try {
                val parsedList = mutableListOf<StationRule>()
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val num = obj.optInt("stationNumber", 0)
                    if (num in 1..20) {
                        val name = obj.optString("displayName", "${num}号快递房")
                        val kwArray = obj.optJSONArray("keywords")
                        val kws = mutableListOf<String>()
                        if (kwArray != null) {
                            for (j in 0 until kwArray.length()) {
                                val kw = kwArray.optString(j)?.trim().orEmpty()
                                if (kw.isNotEmpty()) kws.add(kw)
                            }
                        }
                        parsedList.add(
                            StationRule(
                                stationNumber = num,
                                displayName = name,
                                keywords = kws
                            )
                        )
                    }
                }
                parsedList.sortedBy { it.stationNumber }
            } catch (_: Exception) {
                defaultStationRules()
            }
        }
    }
}
