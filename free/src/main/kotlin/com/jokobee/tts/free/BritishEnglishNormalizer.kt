package com.jokobee.tts.free

/** Variante britannique (en_GB) */
public class BritishEnglishNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : EnglishNormalizer(verbalizer, onWarning) {

    override val locale: String = "en_GB"

    private fun rCurrencyGbp(text: String): String = CURRENCY_GBP_RE.replace(text) { m ->
        val d = m.groupValues[1].replace(",", "").toLong()
        val sb = StringBuilder(card(d) + (if (d == 1L) " pound" else " pounds"))
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) {
            val c = cg.padEnd(2, '0').toLong()
            sb.append(" and ").append(card(c)).append(if (c == 1L) " penny" else " pence")
        }
        sb.toString()
    }

    override fun rDateNum(text: String): String = DATE_NUM_GB_RE.replace(text) { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12 || d < 1 || d > 31) return@replace m.value
        "the ${ordi(d)} of ${MONTHS_ARR[mo - 1]} ${year(yr.toLong())}"
    }

    private fun rDateGb(text: String): String = DATE_GB_RE.replace(text) { m ->
        val sb = StringBuilder("the " + ordi(m.groupValues[1].toInt()) + " of " + m.groupValues[2])
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" ").append(year(yr.toLong()))
        sb.toString()
    }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rSymbols, this::rAbbreviations, this::rRoman, this::rCardinal,
        this::rCurrencyGbp, this::rCurrency, this::rUnit, this::rTemperature, this::rTime,
        this::rDateNum, this::rDateGb, this::rDate,
        this::rRoom, this::rFraction, this::rRange, this::rLetters, this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val MONTHS = "January|February|March|April|May|June|July|August|" +
            "September|October|November|December"
        private val CURRENCY_GBP_RE = Regex("£\\s*(\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.(\\d{1,2}))?")
        private val DATE_GB_RE = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+($MONTHS)(?:\\s+(\\d{4}))?\\b")
        private val DATE_NUM_GB_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
    }
}
