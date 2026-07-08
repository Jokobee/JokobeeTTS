package com.jokobee.tts.free

/** Normalisation coréenne (ko) */
public class KoreanNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "ko"

    private fun hour(h: Long): String = v.spellout(h, "ko", "%spellout-cardinal-native-attributive")
    private fun digits(s: String): String = s.map { if (it == '0') "영" else card(it.toString().toLong()) }.joinToString(" ")

    private fun rCurrency(text: String): String {
        var t = WON_PREFIX_RE.replace(text) { m -> card(m.groupValues[1].replace(",", "").toLong()) + " 원" }
        t = WON_SUFFIX_RE.replace(t) { m -> card(m.groupValues[1].replace(",", "").toLong()) + " 원" }
        return t
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "영하 " else ""
        val sb = StringBuilder(neg + card(m.groupValues[2].toLong()))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" 점 ").append(digits(frac))
        "$sb 도"
    }

    private fun rTime(text: String): String {
        fun word(h: Long, mn: Long): String {
            val sb = StringBuilder(hour(h) + " 시")
            if (mn > 0) sb.append(" ").append(card(mn)).append(" 분")
            return sb.toString()
        }
        var t = TIME_COLON_RE.replace(text) { m -> word(m.groupValues[1].toLong(), m.groupValues[2].toLong()) }
        t = TIME_HANGUL_RE.replace(t) { m ->
            word(m.groupValues[1].toLong(), if (m.groupValues[2].isNotEmpty()) m.groupValues[2].toLong() else 0)
        }
        return t
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val sb = StringBuilder()
        if (m.groupValues[1].isNotEmpty()) sb.append(card(m.groupValues[1].toLong())).append(" 년 ")
        sb.append(card(m.groupValues[2].toLong())).append(" 월 ")
        sb.append(card(m.groupValues[3].toLong())).append(" 일")
        sb.toString()
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " 점 " + digits(m.groupValues[2])
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(m.value.replace(",", "").toLong()) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rCurrency, this::rTemperature, this::rTime,
        this::rDate, this::rDecimal, this::rInteger,
    )

    private companion object {
        private val WON_PREFIX_RE = Regex("₩\\s*(\\d{1,3}(?:,\\d{3})*|\\d+)")
        private val WON_SUFFIX_RE = Regex("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*원")
        private val TEMP_RE = Regex("(?:(-|−|영하)\\s*)?(\\d+)(?:\\.(\\d+))?\\s*(?:°\\s*C|도)")
        private val TIME_COLON_RE = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)")
        private val TIME_HANGUL_RE = Regex("(\\d{1,2})시(?:\\s*(\\d{1,2})분)?")
        private val DATE_RE = Regex("(?:(\\d{4})년\\s*)?(\\d{1,2})월\\s*(\\d{1,2})일")
        private val DECIMAL_RE = Regex("(?<!\\d)(\\d+)\\.(\\d+)(?!\\d)")
        private val INTEGER_RE = Regex("\\d{1,3}(?:,\\d{3})+|\\d+")
    }
}
