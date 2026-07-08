package com.jokobee.tts.free

/** Normalisation italienne (it). Port de it.py. euro invariable ; milliers = point. */
public class ItalianNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "it"

    private fun intIt(s: String): Long = s.replace(".", "").toLong()

    // VALUTA : €/$/£ (avant ou après). « 5,50 € »→« cinque euro e cinquanta centesimi ».
    private fun money(sym: String, intStr: String, centStr: String): String {
        val d = intIt(intStr); val c = CUR.getValue(sym)
        val sb = StringBuilder(card(d) + " " + if (d == 1L) c[0] else c[1])
        if (centStr.isNotEmpty()) {
            val cc = centStr.padEnd(2, '0').toLong()
            sb.append(" e ").append(card(cc)).append(" ").append(if (cc == 1L) c[2] else c[3])
        }
        return sb.toString()
    }

    private fun rCurrency(text: String): String {
        var t = CURRENCY_BEFORE.replace(text) { m -> money(m.groupValues[1], m.groupValues[2], m.groupValues[3]) }
        t = CURRENCY_AFTER.replace(t) { m -> money(m.groupValues[3], m.groupValues[1], m.groupValues[2]) }
        return t
    }

    private fun rAbbreviations(text: String): String {
        var t = text
        for ((pat, rep) in ABBREV) t = pat.replace(t) { rep }
        return t
    }

    private fun rCardinal(text: String): String = CARDINAL_RE.replace(text) { m ->
        m.groupValues[1] + m.groupValues[2] + CARD.getValue(m.groupValues[3].uppercase())
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "meno " else ""
        val whole = m.groupValues[2].toLong()
        val sb = StringBuilder(neg + card(whole))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" virgola ").append(card(frac.toLong()))
        val unit = if (m.groupValues[4] == "C") "Celsius" else "Fahrenheit"
        val deg = if (whole == 1L && m.groupValues[1].isEmpty() && frac.isEmpty()) "grado" else "gradi"
        "$sb $deg $unit"
    }

    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong(); val mn = m.groupValues[2].toLong()
        if (mn > 0) card(h) + " e " + card(mn) else card(h)
    }

    private fun rDateNum(text: String): String = DATE_NUM_RE.replace(text) { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12) return@replace m.value
        val dayW = if (d == 1) "primo" else card(d.toLong())
        "$dayW ${MONTHS_ARR[mo - 1]} ${card(yr.toLong())}"
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val day = m.groupValues[1].lowercase()
        val dayW = if (day == "1º" || day == "1o") "primo" else card(day.toLong())
        val sb = StringBuilder("$dayW ${m.groupValues[2]}")
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" ").append(card(yr.toLong()))
        sb.toString()
    }

    private fun rOrdinal(text: String): String = ORDINAL_RE.replace(text) { m ->
        ordi(m.groupValues[1].toInt(), feminine = m.groupValues[2] == "ª")
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " virgola " + card(m.groupValues[2].toLong())
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(intIt(m.value)) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rCurrency, this::rAbbreviations, this::rCardinal, this::rTemperature,
        this::rTime, this::rDateNum, this::rDate, this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val MONTHS = "gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|" +
            "settembre|ottobre|novembre|dicembre"
        private val MONTHS_ARR = MONTHS.split("|")

        private val CUR = mapOf(
            "€" to listOf("euro", "euro", "centesimo", "centesimi"),       // euro invariable
            "$" to listOf("dollaro", "dollari", "centesimo", "centesimi"),
            "£" to listOf("sterlina", "sterline", "penny", "penny"),
        )
        private val NUM = "\\d{1,3}(?:\\.\\d{3})*|\\d+"
        private val CURRENCY_BEFORE = Regex("([€\\\$£])\\s*($NUM)(?:,(\\d{1,2}))?")
        private val CURRENCY_AFTER = Regex("($NUM)(?:,(\\d{1,2}))?\\s*([€\\\$£])")

        private val ABBREV: List<Pair<Regex, String>> = listOf(
            Regex("\\bSig\\.?ra\\b\\.?") to "Signora", Regex("\\bSig\\.?na\\b\\.?") to "Signorina",
            Regex("\\bSig\\b\\.?") to "Signor", Regex("\\bDott\\.?ssa\\b\\.?") to "Dottoressa",
            Regex("\\bDott\\b\\.?") to "Dottor", Regex("\\bProf\\.?ssa\\b\\.?") to "Professoressa",
            Regex("\\bProf\\b\\.?") to "Professor", Regex("\\bDr\\b\\.?") to "Dottor",
        )

        private val CARD = mapOf(
            "N" to "Nord", "S" to "Sud", "E" to "Est", "O" to "Ovest",
            "NE" to "Nord-est", "NO" to "Nord-ovest", "SE" to "Sud-est", "SO" to "Sud-ovest",
        )
        private val CARDINAL_RE = Regex(
            "\\b(verso|a)((?:\\s+(?:il|lo|la|i|gli|le))?\\s+)(NO|NE|SO|SE|N|S|E|O)\\b")

        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2}):(\\d{2})\\b")
        private val DATE_NUM_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
        private val DATE_RE = Regex("\\b(1º|1o|\\d{1,2})\\s+($MONTHS)(?:\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val ORDINAL_RE = Regex("\\b(\\d+)(º|ª|°)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:\\.\\d{3})+\\b|\\b\\d+\\b")
    }
}
