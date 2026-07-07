package com.jokobee.tts.free

/** Normalisation portugaise brésilienne (pt_BR). Port de pt_br.py. Heures FÉMININES. */
public class BrazilianPortugueseNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "pt_BR"

    private fun intPt(s: String): Long = s.replace(".", "").toLong()
    private fun cardFemHour(h: Long): String = FEM[h] ?: card(h)

    // MOEDA : « R$ 1.234,50 » (symbole AVANT)
    private fun rCurrency(text: String): String = CURRENCY_RE.replace(text) { m ->
        val d = intPt(m.groupValues[1])
        val sb = StringBuilder(card(d) + (if (d == 1L) " real" else " reais"))
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) {
            val c = cg.padEnd(2, '0').toLong()
            sb.append(" e ").append(card(c)).append(if (c == 1L) " centavo" else " centavos")
        }
        sb.toString()
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

    // HORA : « 15h30 », « 9h » — heures FÉMININES (uma/duas)
    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong()
        val sb = StringBuilder(cardFemHour(h) + (if (h == 1L) " hora" else " horas"))
        val mg = m.groupValues[2]
        if (mg.isNotEmpty()) { val mn = mg.toLong(); if (mn > 0) sb.append(" e ").append(card(mn)) }
        sb.toString()
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
        this::rPercent, this::rCurrency, this::rTemperature, this::rTime,
        this::rDate, this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private val FEM = mapOf(1L to "uma", 2L to "duas")
        private const val MONTHS = "janeiro|fevereiro|março|marco|abril|maio|junho|julho|agosto|" +
            "setembro|outubro|novembro|dezembro"
        private val CURRENCY_RE = Regex("R\\\$\\s*(\\d{1,3}(?:\\.\\d{3})*|\\d+)(?:,(\\d{1,2}))?")
        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2})\\s*h\\s*(\\d{1,2})?\\b(?!\\w)")
        private val DATE_RE = Regex("\\b(1º|1o|\\d{1,2})\\s+de\\s+($MONTHS)(?:\\s+de\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val ORDINAL_RE = Regex("\\b(\\d+)(º|ª|°)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:\\.\\d{3})+\\b|\\b\\d+\\b")
    }
}
