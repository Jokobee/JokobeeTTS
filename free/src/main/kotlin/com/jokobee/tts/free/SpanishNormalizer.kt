package com.jokobee.tts.free

/** Normalisation espagnole (es). Port de es.py. Milliers = point, décimale = virgule. */
public class SpanishNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "es"

    private fun intEs(s: String): Long = s.replace(".", "").toLong()

    // DEVISE : symbole AVANT (« $5,50 ») OU APRÈS (« 5,50 € »), €/$/£ parlés par locale.
    private fun money(sym: String, intStr: String, centStr: String): String {
        val d = intEs(intStr); val c = CUR.getValue(sym)
        val sb = StringBuilder(card(d) + " " + if (d == 1L) c[0] else c[1])
        if (centStr.isNotEmpty()) {
            val cc = centStr.padEnd(2, '0').toLong()
            sb.append(" con ").append(card(cc)).append(" ").append(if (cc == 1L) c[2] else c[3])
        }
        return sb.toString()
    }

    private fun rCurrency(text: String): String {
        var t = CURRENCY_BEFORE.replace(text) { m -> money(m.groupValues[1], m.groupValues[2], m.groupValues[3]) }
        t = CURRENCY_AFTER.replace(t) { m -> money(m.groupValues[3], m.groupValues[1], m.groupValues[2]) }
        return t
    }

    // ABRÉVIATIONS : Sr./Sra./Dr.… (plus spécifiques d'abord).
    private fun rAbbreviations(text: String): String {
        var t = text
        for ((pat, rep) in ABBREV) t = pat.replace(t) { rep }
        return t
    }

    // POINTS CARDINAUX : en contexte de direction (hacia/al/…), sans lookbehind (ICU-safe).
    private fun rCardinal(text: String): String = CARDINAL_RE.replace(text) { m ->
        m.groupValues[1] + m.groupValues[2] + CARD.getValue(m.groupValues[3].uppercase())
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "menos " else ""
        val whole = m.groupValues[2].toLong()
        val sb = StringBuilder(neg + card(whole))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" coma ").append(card(frac.toLong()))
        val unit = if (m.groupValues[4] == "C") "Celsius" else "Fahrenheit"
        val deg = if (whole == 1L && m.groupValues[1].isEmpty() && frac.isEmpty()) "grado" else "grados"
        "$sb $deg $unit"
    }

    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong(); val mn = m.groupValues[2].toLong()
        val sb = StringBuilder(card(h))
        if (mn > 0) sb.append(" ").append(if (mn < 10) "cero " + card(mn) else card(mn))
        sb.toString()
    }

    // DATE numérique « 15/03/2024 » → « quince de marzo de dos mil veinticuatro ».
    private fun rDateNum(text: String): String = DATE_NUM_RE.replace(text) { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12) return@replace m.value
        val dayW = if (d == 1) "primero" else card(d.toLong())
        "$dayW de ${MONTHS_ARR[mo - 1]} de ${card(yr.toLong())}"
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val day = m.groupValues[1].lowercase()
        val dayW = if (day == "1º" || day == "1o" || day == "1er") "primero" else card(day.toLong())
        val sb = StringBuilder("$dayW de ${m.groupValues[2]}")
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" de ").append(card(yr.toLong()))
        sb.toString()
    }

    private fun rOrdinal(text: String): String = ORDINAL_RE.replace(text) { m ->
        ordi(m.groupValues[1].toInt(), feminine = m.groupValues[2] == "ª")
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " coma " + card(m.groupValues[2].toLong())
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(intEs(m.value)) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rSymbols, this::rAbbreviations, this::rRoman, this::rCardinal,
        this::rCurrency, this::rTemperature, this::rTime, this::rDateNum, this::rDate,
        this::rRoom, this::rFraction, this::rRange, this::rLetters,
        this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val MONTHS = "enero|febrero|marzo|abril|mayo|junio|julio|agosto|" +
            "septiembre|octubre|noviembre|diciembre"
        private val MONTHS_ARR = MONTHS.split("|")

        // symbole → [unité sing, unité plur, sous-unité sing, sous-unité plur]
        private val CUR = mapOf(
            "€" to listOf("euro", "euros", "céntimo", "céntimos"),
            "$" to listOf("dólar", "dólares", "centavo", "centavos"),
            "£" to listOf("libra", "libras", "penique", "peniques"),
        )
        private val NUM = "\\d{1,3}(?:\\.\\d{3})*|\\d+"
        private val CURRENCY_BEFORE = Regex("([€\\\$£])\\s*($NUM)(?:,(\\d{1,2}))?")
        private val CURRENCY_AFTER = Regex("($NUM)(?:,(\\d{1,2}))?\\s*([€\\\$£])")

        private val ABBREV: List<Pair<Regex, String>> = listOf(
            Regex("\\bSrta\\b\\.?") to "Señorita", Regex("\\bSra\\b\\.?") to "Señora",
            Regex("\\bSr\\b\\.?") to "Señor", Regex("\\bDra\\b\\.?") to "Doctora",
            Regex("\\bDr\\b\\.?") to "Doctor", Regex("\\bProf\\b\\.?") to "Profesor",
            // adresses
            Regex("\\bAvda\\b\\.?") to "Avenida", Regex("\\bAv\\b\\.?") to "Avenida",
            Regex("\\bPza\\b\\.?") to "Plaza", Regex("\\bDepto\\b\\.?") to "Departamento",
            Regex("\\bDpto\\b\\.?") to "Departamento", Regex("\\bnúm\\b\\.?", RegexOption.IGNORE_CASE) to "número",
        )

        private val CARD = mapOf(
            "N" to "Norte", "S" to "Sur", "E" to "Este", "O" to "Oeste",
            "NE" to "Noreste", "NO" to "Noroeste", "SE" to "Sureste", "SO" to "Suroeste",
        )
        // (prep)(article optionnel + espace)(cardinal) — pas de lookbehind.
        private val CARDINAL_RE = Regex(
            "\\b(hacia|hasta|desde|rumbo|al)((?:\\s+(?:el|la|los|las))?\\s+)(NO|NE|SO|SE|N|S|E|O)\\b")

        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2}):(\\d{2})\\b")
        private val DATE_NUM_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
        private val DATE_RE = Regex("\\b(1º|1o|1er|\\d{1,2})\\s+de\\s+($MONTHS)(?:\\s+de\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val ORDINAL_RE = Regex("\\b(\\d+)(º|ª|er|do|ro|to|va|ma)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:\\.\\d{3})+\\b|\\b\\d+\\b")
    }
}
