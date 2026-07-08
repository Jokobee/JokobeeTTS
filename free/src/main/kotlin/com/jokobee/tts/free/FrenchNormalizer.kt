package com.jokobee.tts.free

/** Normalisation française (fr/fr_CA). Port de fr.py. */
public class FrenchNormalizer(
    verbalizer: Verbalizer,
    onWarning: ((String) -> Unit)? = null,
) : BaseNormalizer(verbalizer, onWarning) {

    override val locale: String = "fr_CA"

    // DEVISE : « 1 234,50 $ » — la subdivision suit la DEVISE : $ → sou/sous.
    private fun rCurrency(text: String): String = CURRENCY_RE.replace(text) { m ->
        val d = intOf(m.groupValues[1], TH_RE)
        val sb = StringBuilder(card(d) + (if (d == 1L) " dollar" else " dollars"))
        val cg = m.groupValues[2]
        if (cg.isNotEmpty()) {
            val c = cg.padEnd(2, '0').toLong()
            if (c > 0) sb.append(" et ").append(card(c)).append(if (c == 1L) " sou" else " sous")
        }
        sb.toString()
    }

    // TEMP : « -25 °C », « 37,5 °C », « 350 °F »
    private fun rTemperature(text: String): String = TEMP_RE.replace(text) { m ->
        val neg = if (m.groupValues[1].isNotEmpty()) "moins " else ""
        val whole = m.groupValues[2].toLong()
        val sb = StringBuilder(neg + card(whole))
        val frac = m.groupValues[3]
        if (frac.isNotEmpty()) sb.append(" virgule ").append(card(frac.toLong()))
        val unit = if (m.groupValues[4] == "C") "Celsius" else "Fahrenheit"
        val deg = if (whole == 1L && m.groupValues[1].isEmpty() && frac.isEmpty()) "degré" else "degrés"
        "$sb $deg $unit"
    }

    // HEURE : « 15 h 30 », « 8 h 05 », « 9 h »
    private fun rTime(text: String): String = TIME_RE.replace(text) { m ->
        val h = m.groupValues[1].toLong()
        val sb = StringBuilder(card(h) + (if (h == 1L) " heure" else " heures"))
        val mg = m.groupValues[2]
        if (mg.isNotEmpty()) { val mn = mg.toLong(); if (mn > 0) sb.append(" ").append(card(mn)) }
        sb.toString()
    }

    // DATE numérique « 15/03/2024 » (JJ/MM/AAAA, usage fr_CA) → « quinze mars deux mille vingt-quatre ».
    private fun rDateNum(text: String): String = DATE_NUM_RE.replace(text) { m ->
        val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val yr = m.groupValues[3]
        if (mo < 1 || mo > 12 || d < 1 || d > 31) return@replace m.value
        val dayW = if (d == 1) "premier" else card(d.toLong())
        "$dayW ${MONTHS_ARR[mo - 1]} ${card(yr.toLong())}"
    }

    // DATE : « 6 juillet 2026 », « 1er mars »
    private fun rDate(text: String): String = DATE_RE.replace(text) { m ->
        val day = m.groupValues[1].lowercase()
        val dayW = if (day == "1er" || day == "1re" || day == "1") "premier" else card(day.toLong())
        val sb = StringBuilder("$dayW ${m.groupValues[2]}")
        val year = m.groupValues[3]
        if (year.isNotEmpty()) sb.append(" ").append(card(year.toLong()))
        sb.toString()
    }

    // ORDINAL : « 1er », « 1re », « 2e », « 3ème »
    private fun rOrdinal(text: String): String = ORDINAL_RE.replace(text) { m ->
        ordi(m.groupValues[1].toInt(), feminine = m.groupValues[2] == "re")
    }

    // DÉCIMAL : « 3,14 » → « trois virgule quatorze »
    private fun rDecimal(text: String): String = DECIMAL_RE.replace(text) { m ->
        card(m.groupValues[1].toLong()) + " virgule " + card(m.groupValues[2].toLong())
    }

    // ENTIER résiduel (avec séparateurs de milliers)
    private fun rInteger(text: String): String = INTEGER_RE.replace(text) { m ->
        card(intOf(m.value, TH_RE))
    }

    // B — titres/abréviations
    private fun rAbbreviations(text: String): String {
        var t = text
        for ((pat, rep) in ABBREV_FR) t = pat.replace(t) { rep }
        return t
    }

    // A — cardinaux CONTEXTUELS (mot-clé navigation + numéro)
    private fun rCardinal(text: String): String = CARD_FR_RE.replace(text) { m ->
        m.groupValues[1] + " " + CARD_FR.getValue(m.groupValues[2].uppercase())
    }

    private fun measure(whole: Long, frac: String, base: String, feminine: Boolean = false): String {
        val plural = whole >= 2
        var head = card(whole)
        if (frac.isNotEmpty()) head += " virgule " + card(frac.toLong())
        if (whole == 1L && frac.isEmpty() && feminine) head = "une"
        return head + " " + base + (if (plural) "s" else "")
    }

    // VITESSE (avant DISTANCE : km/h contient km)
    private fun rSpeed(text: String): String = SPEED_RE.replace(text) { m ->
        val sb = StringBuilder(card(m.groupValues[1].toLong()))
        val frac = m.groupValues[2]
        if (frac.isNotEmpty()) sb.append(" virgule ").append(card(frac.toLong()))
        sb.append(" ").append(SPEED_UN.getValue(m.groupValues[3].lowercase()))
        sb.toString()
    }

    // DISTANCE : « 5 km », « 250 m », « 10 miles »
    private fun rDistance(text: String): String = DISTANCE_RE.replace(text) { m ->
        measure(m.groupValues[1].toLong(), m.groupValues[2], DISTANCE_UN.getValue(m.groupValues[3].lowercase()))
    }

    // POIDS : « 3 kg », « 2 t », « 5 lb »
    private fun rWeight(text: String): String = WEIGHT_RE.replace(text) { m ->
        val (base, fem) = WEIGHT_UN.getValue(m.groupValues[3].lowercase())
        measure(m.groupValues[1].toLong(), m.groupValues[2], base, fem)
    }

    override fun rules(): List<(String) -> String> = listOf(
        this::rPercent, this::rSymbols, this::rAbbreviations, this::rRoman, this::rCardinal,
        this::rCurrency, this::rSpeed, this::rDistance, this::rWeight, this::rTemperature,
        this::rTime, this::rDateNum, this::rDate, this::rRoom, this::rFraction, this::rRange, this::rLetters,
        this::rOrdinal, this::rDecimal, this::rInteger,
    )

    private companion object {
        private const val TH = "[ \\u00A0\\u202F\\u2009]"
        private val TH_RE = Regex(TH)
        private const val MONTHS = "janvier|février|fevrier|mars|avril|mai|juin|juillet|août|aout|" +
            "septembre|octobre|novembre|décembre|decembre"
        private val MONTHS_ARR = listOf("janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre")

        private val CURRENCY_RE = Regex("(\\d{1,3}(?:$TH?\\d{3})*)(?:,(\\d{1,2}))?\\s*\\$")
        private val TEMP_RE = Regex("(?:(-|−)\\s*)?(\\d+)(?:,(\\d+))?\\s*°\\s*([CF])")
        private val TIME_RE = Regex("\\b(\\d{1,2})\\s*h\\s*(\\d{1,2})?\\b(?!\\w)")
        private val DATE_RE = Regex("\\b(1er|1re|\\d{1,2})\\s+($MONTHS)(?:\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
        private val DATE_NUM_RE = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b")
        private val ORDINAL_RE = Regex("\\b(\\d+)(er|re|e|ème|eme)\\b")
        private val DECIMAL_RE = Regex("\\b(\\d+),(\\d+)\\b")
        private val INTEGER_RE = Regex("\\b\\d{1,3}(?:$TH\\d{3})+\\b|\\b\\d+\\b")

        private val CARD_FR = mapOf(
            "NE" to "Nord-Est", "NO" to "Nord-Ouest", "SE" to "Sud-Est", "SO" to "Sud-Ouest",
            "N" to "Nord", "S" to "Sud", "E" to "Est", "O" to "Ouest",
        )
        private val CARD_FR_RE = Regex(
            "((?i:sortie|autoroute|route|rte|boulevard|boul|rue|rang|chemin)" +
                "\\b[^.,;\\n]{0,15}?\\d+)\\s*(NE|NO|SE|SO|[NSEO])\\b")

        private val ABBREV_FR: List<Pair<Regex, String>> = listOf(
            Regex("\\bSte-") to "Sainte-", Regex("\\bSt-") to "Saint-",
            Regex("\\bMme\\b\\.?") to "madame", Regex("\\bMlle\\b\\.?") to "mademoiselle",
            Regex("\\bM\\.(?=\\s)") to "monsieur",
            Regex("\\bDre\\b\\.?") to "docteure", Regex("\\bDr\\b\\.?") to "docteur",
            Regex("\\bMe\\b\\.?(?=\\s+[A-ZÀ-Ý])") to "maître",
            Regex("\\bBoul\\b\\.?") to "boulevard", Regex("\\bBlvd\\b\\.?") to "boulevard",
            Regex("\\bAve?\\b\\.?") to "avenue", Regex("\\bRte\\b\\.?") to "route",
            Regex("\\bCh\\b\\.?") to "chemin",
            Regex("\\bapt\\b\\.?", RegexOption.IGNORE_CASE) to "appartement",
            Regex("\\bapp\\b\\.?", RegexOption.IGNORE_CASE) to "appartement",
            Regex("\\bQc\\b") to "Québec",
            Regex("\\bn[o°]\\b\\.?", RegexOption.IGNORE_CASE) to "numéro",
        )

        private val SPEED_UN = mapOf(
            "km/h" to "kilomètres à l'heure", "km/hr" to "kilomètres à l'heure",
            "kph" to "kilomètres à l'heure", "m/s" to "mètres par seconde", "mph" to "milles à l'heure")
        private val SPEED_RE = Regex("\\b(\\d+)(?:,(\\d+))?\\s*(km/h|km/hr|kph|m/s|mph)\\b", RegexOption.IGNORE_CASE)

        private val DISTANCE_UN = mapOf(
            "km" to "kilomètre", "cm" to "centimètre", "mm" to "millimètre", "m" to "mètre",
            "mille" to "mille", "milles" to "mille", "mile" to "mille", "miles" to "mille", "mi" to "mille")
        private val DISTANCE_RE = Regex("\\b(\\d+)(?:,(\\d+))?\\s*(km|cm|mm|m|milles?|miles?|mi)\\b")

        private val WEIGHT_UN = mapOf(
            "kg" to ("kilogramme" to false), "mg" to ("milligramme" to false), "g" to ("gramme" to false),
            "t" to ("tonne" to true), "lbs" to ("livre" to true), "lb" to ("livre" to true))
        private val WEIGHT_RE = Regex("\\b(\\d+)(?:,(\\d+))?\\s*(kg|mg|lbs|lb|g|t)\\b")
    }
}
