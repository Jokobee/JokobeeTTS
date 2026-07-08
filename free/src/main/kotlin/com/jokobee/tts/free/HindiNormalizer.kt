package com.jokobee.tts.free

/** Normalisation hindi (hi) */
public class HindiNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "hi"

    private fun intHi(s: String): Long = s.replace(",", "").toLong()
    private fun digits(s: String): String = s.map { card(it.toString().toLong()) }.joinToString(" ")

    private fun rDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            val idx = DEVANAGARI.indexOf(c)
            sb.append(if (idx >= 0) ('0' + idx) else c)
        }
        return sb.toString()
    }

    private fun rCurrency(text: String): String = CURRENCY_RE.replace(text) { m ->
        val d = intHi(m.groupValues[1])
        val sb = StringBuilder(card(d) + (if (d == 1L) " रुपया" else " रुपये"))
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) sb.append(" ").append(card(cg.padEnd(2, '0').toLong())).append(" पैसे")
        sb.toString()
    }

    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "माइनस " else ""
        val sb = StringBuilder(neg + card(m.groupValues[2].toLong()))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" दशमलव ").append(digits(frac))
        val unit = if (m.groupValues[4] == "C") "सेल्सियस" else "फ़ारेनहाइट"
        "$sb डिग्री $unit"
    }

    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong(); val mn = m.groupValues[2].toLong()
        if (mn == 0L) card(h) + " बजे" else card(h) + " बजकर " + card(mn) + " मिनट"
    }

    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val sb = StringBuilder(card(m.groupValues[1].toLong()) + " " + m.groupValues[2])
        val yr = m.groupValues[3]
        if (yr.isNotEmpty()) sb.append(" ").append(card(yr.toLong()))
        sb.toString()
    }

    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " दशमलव " + digits(m.groupValues[2])
    }

    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m -> card(intHi(m.value)) }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rDigits, this::rCurrency, this::rTemperature,
        this::rTime, this::rDate, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val DEVANAGARI = "०१२३४५६७८९"
        private const val MONTHS = "जनवरी|फ़रवरी|फरवरी|मार्च|अप्रैल|मई|जून|जुलाई|अगस्त|" +
            "सितंबर|सितम्बर|अक्टूबर|अक्तूबर|नवंबर|नवम्बर|दिसंबर|दिसम्बर"
        // entier avec groupement indien (2,2,3) OU occidental OU nu
        private const val NUM = "\\d{1,2}(?:,\\d{2})*,\\d{3}|\\d{1,3}(?:,\\d{3})+|\\d+"
        private val CURRENCY_RE = Regex("₹\\s*($NUM)(?:\\.(\\d{1,2}))?")
        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:\\.(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)")
        private val DATE_RE = Regex("\\b(\\d{1,2})\\s+($MONTHS)(?:\\s+(\\d{4}))?")
        private val DECIMAL_RE = Regex("(?<!\\d)(\\d+)\\.(\\d+)(?!\\d)")
        private val INTEGER_RE = Regex("\\b(?:$NUM)\\b")
    }
}
