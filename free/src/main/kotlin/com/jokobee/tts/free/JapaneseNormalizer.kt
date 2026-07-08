package com.jokobee.tts.free

import java.text.Normalizer

/** Normalisation japonaise (ja, minimale par conception) */
public class JapaneseNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "ja"

    private fun year(y: Long): String = card(y) // cardinal : 二千二十六
    private fun digits(s: String): String = s.map { card(it.toString().toLong()) }.joinToString("")

    private fun rWidth(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in WIDTH_CHARS) Normalizer.normalize(c.toString(), Normalizer.Form.NFKC) else c.toString())
    }

    private fun rCurrency(text: String): String {
        var t = YEN_PREFIX_RE.replace(text) { m -> card(m.groupValues[1].replace(",", "").toLong()) + "円" }
        t = YEN_SUFFIX_RE.replace(t) { m -> card(m.groupValues[1].replace(",", "").toLong()) + "円" }
        return t
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "マイナス" else ""
        val sb = StringBuilder(neg + card(m.groupValues[2].toLong()))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append("点").append(digits(frac))
        "${sb}度"
    }

    private fun rTime(text: String): String {
        fun word(h: Long, mn: Long): String {
            val sb = StringBuilder(card(h) + "時")
            if (mn > 0) sb.append(card(mn)).append("分")
            return sb.toString()
        }
        var t = TIME_COLON_RE.replace(text) { m -> word(m.groupValues[1].toLong(), m.groupValues[2].toLong()) }
        t = TIME_KANJI_RE.replace(t) { m ->
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
        private val YEN_PREFIX_RE = Regex("[¥￥]\\s*(\\d{1,3}(?:,\\d{3})*|\\d+)")
        private val YEN_SUFFIX_RE = Regex("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*円")
        private val TEMP_RE = Regex("(?:(-|−|マイナス)\\s*)?(\\d+)(?:\\.(\\d+))?\\s*(?:°\\s*C|度)")
        private val TIME_COLON_RE = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)")
        private val TIME_KANJI_RE = Regex("(\\d{1,2})時(?:(\\d{1,2})分)?")
        private val DATE_RE = Regex("(?:(\\d{4})年)?(\\d{1,2})月(\\d{1,2})日")
        private val DECIMAL_RE = Regex("(?<!\\d)(\\d+)\\.(\\d+)(?!\\d)")
        private val INTEGER_RE = Regex("\\d{1,3}(?:,\\d{3})+|\\d+")
    }
}
