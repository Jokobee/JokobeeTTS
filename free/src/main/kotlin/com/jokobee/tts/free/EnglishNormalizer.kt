package com.jokobee.tts.free

/** Normalisation anglaise (en_US). Port de en.py. */
public open class EnglishNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "en_US"

    private fun intOfEn(s: String): Long = s.replace(",", "").toLong()

    /** Années : 2000-2009 en toutes lettres ; 1100-9999 (≠ multiple de 100) en paires. */
    protected fun year(y: Long): String {
        if (y in 2000..2009) return card(y)
        if (y in 1100..9999 && y % 100 != 0L) {
            val hi = y / 100; val lo = y % 100
            val loW = if (lo < 10) "oh " + card(lo) else card(lo)
            return card(hi) + " " + loW
        }
        return card(y)
    }

    // CURRENCY : « $1,234.50 » ($ avant, point décimal, virgule de milliers)
    protected open fun rCurrency(text: String): String = CURRENCY_RE.replace(text) { m ->
        val d = intOfEn(m.groupValues[1])
        val sb = StringBuilder(card(d) + (if (d == 1L) " dollar" else " dollars"))
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) {
            val c = cg.padEnd(2, '0').toLong()
            if (c > 0) sb.append(" and ").append(card(c)).append(if (c == 1L) " cent" else " cents")
        }
        sb.toString()
    }

    // TEMP : « -25°F », « 98.6 °F », « 20 °C »
    protected fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "minus " else ""
        val whole = m.groupValues[2].toLong()
        val sb = StringBuilder(neg + card(whole))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" point ").append(frac.map { card(it.toString().toLong()) }.joinToString(" "))
        val unit = if (m.groupValues[4] == "C") "Celsius" else "Fahrenheit"
        val deg = if (whole == 1L && m.groupValues[1].isEmpty() && frac.isEmpty()) "degree" else "degrees"
        "$sb $deg $unit"
    }

    // TIME : « 3:30 PM », « 15:30 », « 3 PM », « 3:05 »
    protected fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h: Long; val mn: Long; val ap: String?
        when {
            m.groupValues[4].isNotEmpty() -> { h = m.groupValues[4].toLong(); mn = 0; ap = m.groupValues[5] }
            m.groupValues[6].isNotEmpty() -> { h = m.groupValues[6].toLong(); mn = m.groupValues[7].toLong(); ap = null }
            else -> { h = m.groupValues[1].toLong(); mn = m.groupValues[2].toLong(); ap = m.groupValues[3] }
        }
        val sb = StringBuilder(card(h))
        if (mn > 0) sb.append(" ").append(if (mn < 10) "oh " + card(mn) else card(mn))
        if (ap != null && ap.isNotEmpty()) sb.append(" ").append(ap.replace(".", "").uppercase())
        sb.toString()
    }

    // DATE : « July 6, 2026 », « March 1st », « July 6 »
    protected fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val sb = StringBuilder(m.groupValues[1] + " " + ordi(m.groupValues[2].toInt()))
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" ").append(year(yr.toLong()))
        sb.toString()
    }

    // DATE numérique US « 03/15/2024 » (MM/DD/YYYY) → « March fifteenth twenty twenty-four ».
    protected open fun rDateNum(text: String): String = DATE_NUM_RE.replace(text) { m ->
        val mo = m.groupValues[1].toInt(); val d = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12 || d < 1 || d > 31) return@replace m.value
        "${MONTHS_ARR[mo - 1]} ${ordi(d)} ${year(yr.toLong())}"
    }

    // ORDINAL : « 21st », « 2nd », « 3rd », « 4th »
    protected fun rOrdinal(text: String): String = ORDINAL_RE.replace(text) { m -> ordi(m.groupValues[1].toInt()) }

    // DECIMAL : « 3.14 » → « three point one four »
    protected fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " point " + m.groupValues[2].map { card(it.toString().toLong()) }.joinToString(" ")
    }

    // INTEGER résiduel
    protected fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(intOfEn(m.value)) }

    protected fun rAbbreviations(text: String): String {
        var t = text
        for ((pat, rep) in ABBREV_EN) t = pat.replace(t) { rep }
        return t
    }

    protected fun rCardinal(text: String): String = CARD_EN_RE.replace(text) { m ->
        m.groupValues[1] + " " + CARD_EN.getValue(m.groupValues[2].uppercase())
    }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rSymbols, this::rAbbreviations, this::rRoman, this::rCardinal,
        this::rCurrency, this::rTemperature, this::rTime, this::rDateNum, this::rDate,
        this::rRoom, this::rFraction, this::rRange, this::rLetters,
        this::rOrdinal, this::rDecimal, this::rInteger,
    )

    protected companion object {
        private const val MONTHS = "January|February|March|April|May|June|July|August|" +
            "September|October|November|December"
        @JvmStatic protected val MONTHS_ARR: List<String> = MONTHS.split("|")

        private val CURRENCY_RE = Regex("\\\$\\s*(\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.(\\d{1,2}))?")
        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:\\.(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex(
            "\\b(\\d{1,2}):(\\d{2})\\s+(AM|PM|am|pm|a\\.m\\.|p\\.m\\.)\\b" +
                "|\\b(\\d{1,2})\\s+(AM|PM|am|pm|a\\.m\\.|p\\.m\\.)\\b" +
                "|\\b(\\d{1,2}):(\\d{2})(?!\\d)")
        private val DATE_RE = Regex("\\b($MONTHS)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?\\b")
        private val DATE_NUM_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
        private val ORDINAL_RE = Regex("\\b(\\d+)(st|nd|rd|th)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+)\\.(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:,\\d{3})+\\b|\\b\\d+\\b")

        private val CARD_EN = mapOf(
            "NE" to "Northeast", "NW" to "Northwest", "SE" to "Southeast", "SW" to "Southwest",
            "N" to "North", "S" to "South", "E" to "East", "W" to "West")
        private val CARD_EN_RE = Regex(
            "((?i:exit|highway|hwy|freeway|interstate|route|rte|street|road)" +
                "\\b[^.,;\\n]{0,15}?\\d+)\\s*(NE|NW|SE|SW|[NSEW])\\b")

        private val ABBREV_EN: List<Pair<Regex, String>> = listOf(
            Regex("\\bMrs\\b\\.?") to "Misses", Regex("\\bMr\\b\\.?") to "Mister",
            Regex("\\bDr\\b\\.?") to "Doctor", Regex("\\bJr\\b\\.?") to "Junior",
            Regex("\\bSr\\b\\.?") to "Senior", Regex("\\bProf\\b\\.?") to "Professor",
            Regex("\\bAve\\b\\.?") to "Avenue", Regex("\\bBlvd\\b\\.?") to "Boulevard",
            Regex("\\bRd\\b\\.?") to "Road", Regex("\\bApt\\b\\.?") to "Apartment",
            Regex("\\bHwy\\b\\.?") to "Highway", Regex("\\bRte\\b\\.?") to "Route",
            Regex("\\bSt\\.\\s+(?=[A-Z])") to "Saint ",
            Regex("\\bSt\\b\\.?(?!\\s+[A-Z])") to "Street",
        )
    }
}
