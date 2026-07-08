package com.jokobee.tts.free

/** Normalisation portugaise brésilienne (pt_BR). Port de pt_br.py. Heures FÉMININES. */
public class BrazilianPortugueseNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "pt_BR"

    private fun intPt(s: String): Long = s.replace(".", "").toLong()
    private fun cardFemHour(h: Long): String = FEM[h] ?: card(h)

    // MOEDA : R$ (avant) + €/$/£ (avant ou après). « R$ 5,50 »→« cinco reais e cinquenta centavos ».
    private fun money(sym: String, intStr: String, centStr: String): String {
        val d = intPt(intStr); val c = CUR.getValue(sym)
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
        val neg = if (m.groupValues[1].isNotEmpty()) "menos " else ""
        val whole = m.groupValues[2].toLong()
        val sb = StringBuilder(neg + card(whole))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" vírgula ").append(card(frac.toLong()))
        val unit = if (m.groupValues[4] == "C") "Celsius" else "Fahrenheit"
        val deg = if (whole == 1L && m.groupValues[1].isEmpty() && frac.isEmpty()) "grau" else "graus"
        "$sb $deg $unit"
    }

    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong()
        val sb = StringBuilder(cardFemHour(h) + (if (h == 1L) " hora" else " horas"))
        val mg = m.groupValues[2]
        if (mg.isNotEmpty()) { val mn = mg.toLong(); if (mn > 0) sb.append(" e ").append(card(mn)) }
        sb.toString()
    }

    private fun rDateNum(text: String): String = DATE_NUM_RE.replace(text) { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12) return@replace m.value
        val dayW = if (d == 1) "primeiro" else card(d.toLong())
        "$dayW de ${MONTHS_ARR[mo - 1]} de ${card(yr.toLong())}"
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val day = m.groupValues[1].lowercase()
        val dayW = if (day == "1º" || day == "1o") "primeiro" else card(day.toLong())
        val sb = StringBuilder("$dayW de ${m.groupValues[2]}")
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" de ").append(card(yr.toLong()))
        sb.toString()
    }

    private fun rOrdinal(text: String): String = ORDINAL_RE.replace(text) { m ->
        ordi(m.groupValues[1].toInt(), feminine = m.groupValues[2] == "ª")
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " vírgula " + card(m.groupValues[2].toLong())
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(intPt(m.value)) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rCurrency, this::rAbbreviations, this::rCardinal, this::rTemperature,
        this::rTime, this::rDateNum, this::rDate, this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private val FEM = mapOf(1L to "uma", 2L to "duas")
        private const val MONTHS = "janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|" +
            "setembro|outubro|novembro|dezembro"
        private val MONTHS_ARR = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")

        private val CUR = mapOf(
            "R\$" to listOf("real", "reais", "centavo", "centavos"),
            "€" to listOf("euro", "euros", "centavo", "centavos"),
            "$" to listOf("dólar", "dólares", "centavo", "centavos"),
            "£" to listOf("libra", "libras", "centavo", "centavos"),
        )
        private val NUM = "\\d{1,3}(?:\\.\\d{3})*|\\d+"
        private val CURRENCY_BEFORE = Regex("(R\\\$|[€\\\$£])\\s*($NUM)(?:,(\\d{1,2}))?")
        private val CURRENCY_AFTER = Regex("($NUM)(?:,(\\d{1,2}))?\\s*([€\\\$£])")

        private val ABBREV: List<Pair<Regex, String>> = listOf(
            Regex("\\bSrta\\b\\.?") to "Senhorita", Regex("\\bSra\\b\\.?") to "Senhora",
            Regex("\\bSr\\b\\.?") to "Senhor", Regex("\\bDra\\b\\.?") to "Doutora",
            Regex("\\bDr\\b\\.?") to "Doutor", Regex("\\bProf\\b\\.?") to "Professor",
        )

        private val CARD = mapOf(
            "N" to "Norte", "S" to "Sul", "E" to "Este", "L" to "Leste", "O" to "Oeste",
            "NE" to "Nordeste", "NO" to "Noroeste", "SE" to "Sudeste", "SO" to "Sudoeste",
        )
        private val CARDINAL_RE = Regex(
            "\\b(para|até|rumbo|a)((?:\\s+(?:o|a|os|as))?\\s+)(NO|NE|SO|SE|N|S|E|L|O)\\b")

        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2})\\s*h\\s*(\\d{1,2})?\\b(?!\\w)")
        private val DATE_NUM_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
        private val DATE_RE = Regex("\\b(1º|1o|\\d{1,2})\\s+de\\s+($MONTHS)(?:\\s+de\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val ORDINAL_RE = Regex("\\b(\\d+)(º|ª|°)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:\\.\\d{3})+\\b|\\b\\d+\\b")
    }
}
