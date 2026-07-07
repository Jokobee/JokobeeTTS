package com.jokobee.tts.free

/** Normalisation italienne (it). Port de it.py. euro invariable ; milliers = point. */
public class ItalianNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "it"

    private fun intIt(s: String): Long = s.replace(".", "").toLong()

    private fun rCurrency(text: String): String = CURRENCY_RE.replace(text) { m ->
        val d = intIt(m.groupValues[1])
        val sb = StringBuilder(card(d) + " euro")   // euro invariable
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) {
            val c = cg.padEnd(2, '0').toLong()
            sb.append(" e ").append(card(c)).append(if (c == 1L) " centesimo" else " centesimi")
        }
        sb.toString()
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
        this::rPercent, this::rCurrency, this::rTemperature, this::rTime,
        this::rDate, this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val MONTHS = "gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|" +
            "settembre|ottobre|novembre|dicembre"
        private val CURRENCY_RE = Regex("(\\d{1,3}(?:\\.\\d{3})*|\\d+)(?:,(\\d{1,2}))?\\s*€")
        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2}):(\\d{2})\\b")
        private val DATE_RE = Regex("\\b(1º|1o|\\d{1,2})\\s+($MONTHS)(?:\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val ORDINAL_RE = Regex("\\b(\\d+)(º|ª|°)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:\\.\\d{3})+\\b|\\b\\d+\\b")
    }
}
