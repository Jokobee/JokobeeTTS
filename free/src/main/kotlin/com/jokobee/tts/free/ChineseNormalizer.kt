package com.jokobee.tts.free

import java.text.Normalizer

/**
 * Normalisation chinoise (zh, mandarin, minimale par conception). Port de zh.py.
 *
 * Stratégie : chiffres → HANZI, lectures et tons délégués au G2P zh dédié.
 *
 * - Années par CHIFFRES (二〇二六年) via le ruleset ICU %spellout-numbering-year.
 * - Jours en cardinal — %spellout-numbering-days est lunaire (初六, 廿五), PIÈGE évité.
 * - Heures : 15:30 → 十五点三十分 ; règle 两 : 2点 → 两点 (jamais 二点).
 * - Températures négatives : -25°C → 零下二十五度 (usage météo).
 */
public class ChineseNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "zh"

    private fun year(y: Long): String = v.spellout(y, "zh", "%spellout-numbering-year") // 二〇二六
    private fun hour(h: Long): String = if (h == 2L) "两" else card(h) // règle 两
    private fun digits(s: String): String = s.map { if (it == '0') "〇" else card(it.toString().toLong()) }.joinToString("")

    private fun rWidth(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in WIDTH_CHARS) Normalizer.normalize(c.toString(), Normalizer.Form.NFKC) else c.toString())
    }

    private fun rCurrency(text: String): String {
        var t = YUAN_PREFIX_RE.replace(text) { m -> card(m.groupValues[1].replace(",", "").toLong()) + "元" }
        t = YUAN_SUFFIX_RE.replace(t) { m -> card(m.groupValues[1].replace(",", "").toLong()) + "元" }
        return t
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "零下" else ""
        val sb = StringBuilder(neg + card(m.groupValues[2].toLong()))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append("点").append(digits(frac))
        "${sb}度"
    }

    private fun rTime(text: String): String {
        fun word(h: Long, mn: Long): String {
            val sb = StringBuilder(hour(h) + "点")
            if (mn > 0) sb.append(card(mn)).append("分")
            return sb.toString()
        }
        var t = TIME_COLON_RE.replace(text) { m -> word(m.groupValues[1].toLong(), m.groupValues[2].toLong()) }
        t = TIME_HANZI_RE.replace(t) { m ->
            word(m.groupValues[1].toLong(), if (m.groupValues[2].isNotEmpty()) m.groupValues[2].toLong() else 0)
        }
        return t
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val sb = StringBuilder()
        if (m.groupValues[1].isNotEmpty()) sb.append(year(m.groupValues[1].toLong())).append("年")
        sb.append(card(m.groupValues[2].toLong())).append("月")
        sb.append(card(m.groupValues[3].toLong())).append("日")
        sb.toString()
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + "点" + digits(m.groupValues[2])
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(m.value.replace(",", "").toLong()) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rWidth, this::rCurrency, this::rTemperature,
        this::rTime, this::rDate, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val WIDTH_CHARS = "０１２３４５６７８９：．，％￥"
        private val YUAN_PREFIX_RE = Regex("[¥￥]\\s*(\\d{1,3}(?:,\\d{3})*|\\d+)")
        private val YUAN_SUFFIX_RE = Regex("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*元")
        private val TEMP_RE = Regex("(?:(-|−|零下)\\s*)?(\\d+)(?:\\.(\\d+))?\\s*(?:°\\s*C|度)")
        private val TIME_COLON_RE = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)")
        private val TIME_HANZI_RE = Regex("(\\d{1,2})点(?:(\\d{1,2})分)?")
        private val DATE_RE = Regex("(?:(\\d{4})年)?(\\d{1,2})月(\\d{1,2})日")
        private val DECIMAL_RE = Regex("(?<!\\d)(\\d+)\\.(\\d+)(?!\\d)")
        private val INTEGER_RE = Regex("\\d{1,3}(?:,\\d{3})+|\\d+")
    }
}
