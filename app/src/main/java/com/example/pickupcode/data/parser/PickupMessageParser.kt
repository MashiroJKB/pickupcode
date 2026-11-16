package com.example.pickupcode.data.parser

import com.example.pickupcode.data.local.StationRule

/**
 * 单条解析出的取件记录结果
 */
data class ParsedPickupResult(
    val code: String,
    val stationNumber: Int,
    val stationRawName: String,
    val productName: String,
    val rawMessage: String
)

/**
 * 智能取件码解析引擎
 *
 * 核心能力：
 * 1. 前置干扰过滤：剔除验证码、已签收、已出库、超时收费、营销退换货等非待取件消息。
 * 2. 强特征词校验：必须包含取件码/提货码/驿站/快递/号房等强特征词。
 * 3. 快递房映射：匹配用户配置的识别规则（支持自定义添加/删除快递房与关键词），未识别归为 0（？待分类）。
 * 4. 一信多码全局拆分：支持 "凭取件码 1-2-3045、1-2-3046 或 2-1-1008 到3号房取件" 拆分为多条独立记录。
 * 5. 不识别也不显示任何快递品牌/代收点别名（如“京东快递”、“多多代收点”等）。
 */
object PickupMessageParser {

    // 1. 排除干扰信息关键词（验证码类 + 已完成/营销提醒类）
    private val EXCLUDE_VERIFICATION_KEYWORDS = listOf(
        "验证码", "动态码", "校验码", "请勿泄露", "登录密码", "修改密码", "安全码", "身份验证"
    )

    private val EXCLUDE_MARKETING_OR_DONE_KEYWORDS = listOf(
        "已签收", "已出库", "已代收完成", "已取件", "已被取走", "超时收费", "退换货", "优惠券", "直播间", "直播中"
    )

    // 2. 取件强特征词
    private val STRONG_PICKUP_KEYWORDS = listOf(
        "取件码", "提货码", "取货码", "取件编号", "包裹码", "开箱码",
        "凭", "快递", "驿站", "号房", "菜鸟", "丰巢", "兔喜", "妈妈驿站",
        "近邻宝", "熊猫快收", "速递易", "快递柜", "到站", "待取", "代收点"
    )

    // 干扰数字掩码正则（时间、日期、手机号、座机、客服热线、尾号、计件/金额）
    private val TIME_RANGE_REGEX = Regex("""\b\d{1,2}:\d{2}(?:\s*[-~至到]\s*\d{1,2}:\d{2})?\b""")
    private val DATE_REGEX = Regex("""\b(?:20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2}日?|\d{1,2}月\d{1,2}日)\b""")
    private val PHONE_REGEX = Regex("""(?<!\d)(?:1[3-9]\d{9}|0\d{2,3}-?\d{7,8}|95\d{3,5}|400-?\d{3}-?\d{4}|100\d{2})(?!\d)""")
    private val TAIL_NUMBER_REGEX = Regex("""(?:手机尾号|尾号|后四位)\s*[:：为是]?\s*\d{4}""")
    private val QUANTITY_REGEX = Regex("""\b\d+(?:\.\d+)?\s*(?:元|kg|克|件|个|袋|箱|小时|天|折|号快递房|号房|号驿站|号站|号柜|号门|号楼|栋|层)\b""")

    // 货架-层-号 横杠格式取件码（如 1-2-3045, 102-2-5, J8-5-3012, A1-2048）
    private val HYPHENATED_CODE_REGEX = Regex(
        """(?<![A-Za-z0-9-])(?:[A-Za-z0-9]{1,5}-\d{1,4}-\d{1,6}|[A-Za-z]{1,3}\d{0,3}-\d{3,6}|\d{1,4}-\d{3,6})(?![A-Za-z0-9-])"""
    )

    // 紧跟在取件码关键词后的单码或多码片段
    private val CONTEXTUAL_BLOCK_REGEX = Regex(
        """(?:取件码|提货码|取货码|取件编号|包裹码|开箱码|取件密码|凭码|凭)\s*(?:为|是)?\s*[:：]?\s*([A-Za-z0-9\-、,，/或及和与\s]{3,80}?)(?=到|至|前往|去|在|来|取件|提货|取货|领取|，|。|；|！|\(|（|请|及时|详询|联系|$)"""
    )

    // 拆分多码连接符
    private val MULTI_CODE_SPLIT_REGEX = Regex("""(?:\s*[、,，/]\s*|\s+(?:或|及|和|与)\s+|\s+)""")

    // 单个候选取件码合法性校验
    private val SINGLE_CODE_TOKEN_REGEX = Regex(
        """^(?:[A-Za-z0-9]{1,5}(?:-[A-Za-z0-9]{1,6})+|[A-Za-z]{1,4}\d{3,8}|\d{3,8}[A-Za-z]{1,3}|\d{4,8})$"""
    )

    // 内置快递房号兜底正则（仅匹配当前已启用的快递房编号）
    private val BUILTIN_STATION_REGEX_LIST = listOf(
        Regex("""(\d{1,2}|[一二三四五六七八九十])\s*号\s*(?:快递房|快递站|菜鸟驿站|驿站|快递柜|丰巢柜|丰巢|房|站|柜|点|门)"""),
        Regex("""(?:快递房|快递站|菜鸟驿站|驿站)\s*(\d{1,2}|[一二三四五六七八九十])\s*号?""")
    )

    // 商品名称提取正则（仅提取明确带有“您购买的【...】”等商品描述的文本，绝不提取快递或站点名）
    private val PRODUCT_NAME_PATTERNS = listOf(
        Regex("""(?:您购买的|购买的|选购的|订单商品)\s*[【「"“（(]([^】」"”）)\n]{2,30})[】」"”）)]"""),
        Regex("""(?:商品名称|商品名|物品名称)\s*[:：]\s*([^\n，,。；;【】]{2,26})""")
    )

    /**
     * 判断文本是否通过前置过滤（不含干扰词且包含取件强特征）
     */
    fun passesPreFilter(rawText: String): Boolean {
        val text = rawText.trim()
        if (text.length < 6) return false

        if (EXCLUDE_VERIFICATION_KEYWORDS.any { text.contains(it, ignoreCase = true) }) {
            return false
        }

        if (EXCLUDE_MARKETING_OR_DONE_KEYWORDS.any { text.contains(it, ignoreCase = true) }) {
            return false
        }

        return STRONG_PICKUP_KEYWORDS.any { text.contains(it, ignoreCase = true) }
    }

    /**
     * 解析原始短信文本，返回 0 ~ N 条拆分后的取件记录
     */
    fun parse(
        rawText: String,
        stationRules: List<StationRule>
    ): List<ParsedPickupResult> {
        val text = rawText.trim()
        if (!passesPreFilter(text)) {
            return emptyList()
        }

        val codes = extractPickupCodes(text)
        if (codes.isEmpty()) {
            return emptyList()
        }

        val (stationNumber, stationRawName) = matchStation(text, stationRules)
        val productName = extractProductName(text)

        return codes.map { code ->
            ParsedPickupResult(
                code = code,
                stationNumber = stationNumber,
                stationRawName = stationRawName,
                productName = productName,
                rawMessage = text
            )
        }
    }

    /**
     * 全局提取短信中的所有取件码（支持横杠多段码、字母数字混合码、纯数字柜码及多码并列）
     */
    fun extractPickupCodes(rawText: String): List<String> {
        var sanitized = rawText
        sanitized = TIME_RANGE_REGEX.replace(sanitized, " ")
        sanitized = DATE_REGEX.replace(sanitized, " ")
        sanitized = PHONE_REGEX.replace(sanitized, " ")
        sanitized = TAIL_NUMBER_REGEX.replace(sanitized, " ")
        sanitized = QUANTITY_REGEX.replace(sanitized, " ")

        val extractedCodes = linkedSetOf<String>()

        // 策略 A：优先从“取件码/提货码/凭...”上下文语句块中拆分多码
        for (blockMatch in CONTEXTUAL_BLOCK_REGEX.findAll(sanitized)) {
            val blockText = blockMatch.groupValues[1].trim()
            val candidateTokens = blockText.split(MULTI_CODE_SPLIT_REGEX)
            for (rawToken in candidateTokens) {
                val cleaned = rawToken.trim().trim(':', '：', '.', '。', ',', '，', '、', '[', ']', '【', '】', '"', '“', '”')
                if (isValidPickupCodeToken(cleaned)) {
                    extractedCodes.add(cleaned.uppercase())
                }
            }
        }

        // 策略 B：全局扫描标准货架横杠格式（如 1-2-3045、102-2-1048、J8-5-3012）
        for (hyphenMatch in HYPHENATED_CODE_REGEX.findAll(sanitized)) {
            val candidate = hyphenMatch.value.trim()
            if (isValidPickupCodeToken(candidate)) {
                extractedCodes.add(candidate.uppercase())
            }
        }

        return extractedCodes.toList()
    }

    private fun isValidPickupCodeToken(token: String): Boolean {
        if (token.length < 4 || token.length > 18) return false
        if (!SINGLE_CODE_TOKEN_REGEX.matches(token)) return false
        val digitCount = token.count { it.isDigit() }
        if (digitCount < 2) return false
        if (token.length == 11 && token.startsWith("1") && token.all { it.isDigit() }) return false
        if (token.matches(Regex("""^20[2-3]\d-\d{1,2}-\d{1,2}$"""))) return false
        return true
    }

    /**
     * 匹配快递房编号：优先匹配用户配置的识别规则关键词（按词长降序），再走内置号房正则（仅限已存在的快递房）
     */
    fun matchStation(
        text: String,
        stationRules: List<StationRule>
    ): Pair<Int, String> {
        val activeNumbers = stationRules.map { it.stationNumber }.toSet()

        val keywordPairs = stationRules
            .filter { it.stationNumber in 1..100 }
            .flatMap { rule ->
                rule.keywords
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { kw -> kw to rule.stationNumber }
            }
            .sortedByDescending { it.first.length }

        for ((keyword, stationNum) in keywordPairs) {
            if (text.contains(keyword, ignoreCase = true)) {
                return stationNum to keyword
            }
        }

        for (regex in BUILTIN_STATION_REGEX_LIST) {
            val match = regex.find(text)
            if (match != null) {
                val numToken = match.groupValues[1]
                val parsedNum = parseStationDigit(numToken)
                val targetStation = stationRules.firstOrNull { it.badgeNumber == parsedNum }?.stationNumber
                    ?: parsedNum.takeIf { it in activeNumbers }
                if (targetStation != null) {
                    return targetStation to match.value
                }
            }
        }

        return 0 to ""
    }

    private fun parseStationDigit(token: String): Int {
        val trimmed = token.trim()
        trimmed.toIntOrNull()?.let { return it }
        return when (trimmed) {
            "一" -> 1
            "二" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "七" -> 7
            "八" -> 8
            "九" -> 9
            "十" -> 10
            else -> 0
        }
    }

    /**
     * 提取明确的商品名称（不提取任何快递公司或代收点名称）
     */
    fun extractProductName(fullText: String): String {
        for (pattern in PRODUCT_NAME_PATTERNS) {
            val match = pattern.find(fullText)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length >= 2 && !candidate.contains("取件码") && !candidate.contains("提货码")) {
                    return candidate.take(24)
                }
            }
        }
        return ""
    }
}
