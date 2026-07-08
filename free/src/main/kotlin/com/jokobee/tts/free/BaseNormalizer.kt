package com.jokobee.tts.free

import java.text.Normalizer

/** Base des normaliseurs */
public abstract class BaseNormalizer(
    protected val v: Verbalizer,
    private val onWarning: ((String) -> Unit)? = null,
) {
    public abstract val locale: String
    public val warnings: MutableList<String> = ArrayList()

    protected fun warn(msg: String) {
        warnings.add(msg)
        onWarning?.let { try { it(msg) } catch (e: Exception) { /* callback fautif ignoré */ } }
    }

    // --- helpers ---------------------------------------------------------
    protected fun card(n: Long): String = v.cardinal(n, locale)
    protected fun card(n: Int): String = v.cardinal(n.toLong(), locale)
    protected fun ordi(n: Int, feminine: Boolean = false): String = v.ordinal(n.toLong(), locale, feminine)
    protected fun intOf(s: String, thousands: Regex): Long = thousands.replace(s, "").toLong()

    private fun numWords(numStr: String, decimalWord: String): String {
        val m = Regex("(\\d+)[.,](\\d+)$").matchEntire(numStr)
        return if (m != null) {
            val frac = m.groupValues[2].map { card(it.toString().toLong()) }.joinToString(" ")
            card(m.groupValues[1].toLong()) + " " + decimalWord + " " + frac
        } else card(numStr.toLong())
    }

    // --- protecteur ------------------------------------------------------
    private fun protect(textIn: String): Pair<String, List<String>> {
        var text = textIn
        val store = ArrayList<String>()
        for (pat in PROTECT_PATTERNS) {
            text = pat.replace(text) { mr -> store.add(mr.value); (PROT_BASE + store.size - 1).toChar().toString() }
        }
        return text to store
    }

    private fun restore(textIn: String, store: List<String>): String {
        var text = textIn
        for ((i, orig) in store.withIndex()) text = text.replace((PROT_BASE + i).toChar().toString(), orig)
        return text
    }

    private fun stripUnknown(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c == '\n' || c == '\t' || Character.getType(c) in KEEP_TYPES) sb.append(c)
            else warn("symbole retiré '" + c + "' (U+" + c.code.toString(16).uppercase().padStart(4, '0') + ")")
        }
        return sb.toString()
    }

    // --- règles universelles --------------------------------------------
    protected fun rWhitelist(text: String): String {
        var t = text
        for ((pat, rep) in WHITELIST_WORDS[locale] ?: emptyList()) t = pat.replace(t) { rep }
        return t
    }

    protected fun rPunctuation(text: String): String {
        var t = text
        for ((pat, rep) in PUNCT_RULES) t = pat.replace(t) { rep }
        return t
    }

    protected fun rPercent(text: String): String {
        val info = PERCENT_WORDS[locale] ?: return text
        val (word, prefix, dec) = info
        return PERCENT_RE.replace(text) { mr ->
            val n = numWords(mr.groupValues[1], dec)
            if (prefix) word + n else "$n $word"
        }
    }

    protected fun rSymbols(text: String): String {
        var t = text
        for ((pat, rep) in SYMBOL_WORDS[locale] ?: emptyList()) t = pat.replace(t) { rep }
        return t
    }

    protected fun rFraction(text: String): String {
        val d = FRACTION_DATA[locale] ?: return text
        fun fw(n: Int, dd: Int) = d.known[n to dd] ?: (card(n) + " " + d.over + " " + card(dd))
        var t = FRAC_MIX_RE.replace(text) { mr ->
            val n = mr.groupValues[2].toInt(); val dd = mr.groupValues[3].toInt()
            val frac = if (n == 1 && dd == 2) d.half else fw(n, dd)
            card(mr.groupValues[1].toLong()) + " " + d.join + " " + frac
        }
        t = FRAC_SIMPLE_RE.replace(t) { mr -> fw(mr.groupValues[1].toInt(), mr.groupValues[2].toInt()) }
        return t
    }

    protected fun rRange(text: String): String {
        val word = RANGE_WORD[locale] ?: return text
        return RANGE_RE.replace(text) { mr -> card(mr.groupValues[1].toLong()) + " " + word + " " + card(mr.groupValues[2].toLong()) }
    }

    protected fun rRoman(text: String): String {
        var t = text
        when {
            locale.startsWith("fr") -> {
                t = ROMAN_KW_FR.replace(t) { mr ->
                    val n = romanToInt(mr.groupValues[2]); mr.groupValues[1] + " " + (if (n == 1) "premier" else card(n))
                }
                t = ROMAN_SIECLE_FR.replace(t) { mr -> ordi(romanToInt(mr.groupValues[1])) + " " + mr.groupValues[2] }
            }
            locale.startsWith("en") -> {
                t = ROMAN_MONARCH_EN.replace(t) { mr -> mr.groupValues[1] + " the " + ordi(romanToInt(mr.groupValues[2])) }
                t = ROMAN_SECTION_EN.replace(t) { mr -> mr.groupValues[1] + " " + card(romanToInt(mr.groupValues[2])) }
            }
            locale == "es" -> {
                t = ROMAN_MONARCH_ES.replace(t) { mr -> mr.groupValues[1] + " " + ordi(romanToInt(mr.groupValues[2])) }
                t = ROMAN_UNIT_ES.replace(t) { mr -> mr.groupValues[1] + " " + card(romanToInt(mr.groupValues[2])) }
            }
            locale == "pt_BR" -> {
                t = ROMAN_MONARCH_PT.replace(t) { mr -> mr.groupValues[1] + " " + ordi(romanToInt(mr.groupValues[2])) }
                t = ROMAN_UNIT_PT.replace(t) { mr -> mr.groupValues[1] + " " + card(romanToInt(mr.groupValues[2])) }
            }
            locale == "it" -> {
                t = ROMAN_MONARCH_IT.replace(t) { mr -> mr.groupValues[1] + " " + ordi(romanToInt(mr.groupValues[2])) }
                t = ROMAN_SECOLO_IT.replace(t) { mr -> ordi(romanToInt(mr.groupValues[1])) + " " + mr.groupValues[2] }
                t = ROMAN_UNIT_IT.replace(t) { mr -> mr.groupValues[1] + " " + card(romanToInt(mr.groupValues[2])) }
            }
        }
        return t
    }

    protected fun rRoom(text: String): String {
        val kw = ROOM_KW[locale] ?: return text
        val oh = ROOM_OH[locale] ?: "oh"
        fun chunk(c: String) = if (c.length == 2 && c[0] == '0') "$oh " + card(c[1].toString().toLong()) else card(c.toLong())
        return Regex("\\b($kw)\\s+(\\d{3,4})\\b", RegexOption.IGNORE_CASE).replace(text) { mr ->
            val digits = mr.groupValues[2]
            val parts = if (digits.length == 4) listOf(digits.substring(0, 2), digits.substring(2))
                        else listOf(digits.substring(0, 1), digits.substring(1))
            mr.groupValues[1] + " " + parts.joinToString(" ") { chunk(it) }
        }
    }

    protected fun rLetters(text: String): String {
        val keep = ACRONYM_KEEP[locale] ?: return text
        return ACR_RE.replace(text) { mr -> if (mr.value in keep) mr.value else mr.value.toCharArray().joinToString(" ") }
    }

    public fun normalize(text: String?): String {
        warnings.clear()
        if (text.isNullOrEmpty()) return ""
        var t = try { Normalizer.normalize(text, Normalizer.Form.NFC) } catch (e: Exception) { warn("NFC échouée"); text }
        val (protectedText, store) = try { protect(t) } catch (e: Exception) { warn("protecteur ignoré"); return finish(t, emptyList()) }
        t = protectedText
        t = safe("whitelist") { rWhitelist(t) } ?: t
        t = safe("punctuation") { rPunctuation(t) } ?: t
        for (rule in rules()) t = safe("règle") { rule(t) } ?: t
        return finish(t, store)
    }

    private fun finish(textIn: String, store: List<String>): String {
        var t = try { restore(textIn, store) } catch (e: Exception) { warn("restore ignoré"); textIn }
        t = stripUnknown(t)
        t = COLLAPSE_RE.replace(t, " ")
        return t.trim()
    }

    private inline fun safe(name: String, block: () -> String): String? =
        try { block() } catch (e: Exception) { warn("$name sautée: ${e.message}"); null }

    /** Ordre significatif des règles de la locale (à définir par sous-classe). */
    protected abstract fun rules(): List<(String) -> String>

    private data class FracData(val known: Map<Pair<Int, Int>, String>, val over: String, val join: String, val half: String)

    public companion object {
        private const val PROT_BASE = 0xE000

        private fun romanToInt(s: String): Int {
            val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
            val vals = s.uppercase().map { map.getValue(it) }
            var total = 0
            for (i in vals.indices) total += if (i + 1 < vals.size && vals[i] < vals[i + 1]) -vals[i] else vals[i]
            return total
        }

        private val COLLAPSE_RE = Regex("[ \\u00a0\\u202f]{2,}")
        private val PERCENT_RE = Regex("(\\d+(?:[.,]\\d+)?)\\s*%")
        private val FRAC_MIX_RE = Regex("\\b(\\d+)\\s+(\\d{1,2})/(\\d{1,2})\\b(?!/)")
        private val FRAC_SIMPLE_RE = Regex("(?<![\\d/])(\\d{1,2})/(\\d{1,2})\\b(?!/)")
        private val RANGE_RE = Regex("(?<![\\d./\\-])(\\d+)\\s*-\\s*(\\d+)(?![\\d/\\-])(?!\\.\\d)")
        private val ACR_RE = Regex("\\b[A-Z]{2,5}\\b")

        // catégories Unicode conservées par strip_unknown (L, M, N, P, Z)
        private val KEEP_TYPES = intArrayOf(
            Character.UPPERCASE_LETTER.toInt(), Character.LOWERCASE_LETTER.toInt(), Character.TITLECASE_LETTER.toInt(),
            Character.MODIFIER_LETTER.toInt(), Character.OTHER_LETTER.toInt(),
            Character.NON_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(),
            Character.DECIMAL_DIGIT_NUMBER.toInt(), Character.LETTER_NUMBER.toInt(), Character.OTHER_NUMBER.toInt(),
            Character.SPACE_SEPARATOR.toInt(), Character.LINE_SEPARATOR.toInt(), Character.PARAGRAPH_SEPARATOR.toInt(),
            Character.DASH_PUNCTUATION.toInt(), Character.START_PUNCTUATION.toInt(), Character.END_PUNCTUATION.toInt(),
            Character.CONNECTOR_PUNCTUATION.toInt(), Character.OTHER_PUNCTUATION.toInt(),
            Character.INITIAL_QUOTE_PUNCTUATION.toInt(), Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        )

        private val CI = setOf(RegexOption.IGNORE_CASE)
        private val PROTECT_PATTERNS = listOf(
            Regex("(?:https?://|www\\.)\\S+", CI),
            Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+", CI),
            Regex("(?:v|version|バージョン|버전|版本)\\s*\\.?\\s*\\d+(?:\\.\\d+)*", CI),
            Regex("\\+\\d{1,4}(?:[\\s.\\-]\\d{1,4}){2,}"),
            Regex("\\d{1,4}(?:[.\\-]\\d{1,4}){2,}"),
            Regex("〒\\s?\\d{3}-\\d{4}"),
            Regex("\\b[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d\\b"),
            Regex("\\b\\d{5}-\\d{3}\\b"),
            Regex("(?:ZIP|Zip|code postal)\\s*:?\\s*\\d{5}(?:-\\d{4})?", CI),
            Regex("\\b[A-Z]{2}\\s?\\d{3,4}\\b"),
            Regex("#\\d+\\b"),
        )

        private val PERCENT_WORDS = mapOf(
            "fr" to Triple("pour cent", false, "virgule"), "fr_CA" to Triple("pour cent", false, "virgule"),
            "en_US" to Triple("percent", false, "point"), "en_GB" to Triple("percent", false, "point"),
            "es" to Triple("por ciento", false, "coma"), "it" to Triple("per cento", false, "virgola"),
            "pt_BR" to Triple("por cento", false, "vírgula"), "hi" to Triple("प्रतिशत", false, "दशमलव"),
            "ja" to Triple("パーセント", false, "点"), "zh" to Triple("百分之", true, "点"),
            "ko" to Triple("퍼센트", false, "점"),
        )

        private val WL_FR = listOf(
            Regex("\\bc\\.-?\\s?à-?\\s?d\\.?", CI) to "c'est-à-dire",
            Regex("\\bp\\.\\s?ex\\.?", CI) to "par exemple",
            Regex("\\betc\\.?", CI) to "et cétéra",
            Regex("\\binc\\.") to "incorporée",
            Regex("\\bltée\\b", CI) to "limitée",
            Regex("\\bQC\\b") to "Québec",
        )
        private val WL_EN = listOf(
            Regex("\\be\\.g\\.?", CI) to "for example",
            Regex("\\bi\\.e\\.?", CI) to "that is",
            Regex("\\betc\\.?", CI) to "et cetera",
            Regex("\\bvs\\.?", CI) to "versus",
            Regex("\\bapprox\\.?", CI) to "approximately",
            Regex("\\bdept\\.?", CI) to "department",
        )
        private val WL_ES = listOf(
            Regex("\\betc\\.?", CI) to "etcétera",
            Regex("\\bp\\.\\s?ej\\.?", CI) to "por ejemplo",
            Regex("\\bEE\\.?\\s?UU\\.?") to "Estados Unidos",
            Regex("\\bUd\\b\\.?", CI) to "usted", Regex("\\bVd\\b\\.?", CI) to "usted",
        )
        private val WL_PT = listOf(
            Regex("\\betc\\.?", CI) to "et cetera",
            Regex("\\bp\\.\\s?ex\\.?", CI) to "por exemplo",
            Regex("\\bEUA\\b") to "Estados Unidos",
        )
        private val WL_IT = listOf(
            Regex("\\becc\\.?", CI) to "eccetera",
            Regex("\\bp\\.\\s?es\\.?", CI) to "per esempio",
            Regex("\\becc\\b\\.?", CI) to "eccetera",
        )
        private val WHITELIST_WORDS = mapOf(
            "fr_CA" to WL_FR, "fr" to WL_FR, "en_US" to WL_EN, "en_GB" to WL_EN,
            "es" to WL_ES, "pt_BR" to WL_PT, "it" to WL_IT,
        )

        private val PUNCT_RULES = listOf(
            Regex("…|\\.{3,}") to "…",
            Regex("!{2,}") to "!",
            Regex("\\?{2,}") to "?",
            Regex("\\s*(?:—|--+)\\s*") to ", ",
        )

        private fun symRules(mapping: Map<String, String>) = mapping.map { (s, w) ->
            //   (crash au chargement) alors qu'ils passent sur la JVM. Vérifié on-device.
            val wc = "[\\p{L}\\p{N}_]"
            Regex("(?<=$wc)\\s*" + Regex.escape(s) + "\\s*(?=$wc)") to " $w "
        }
        private val SYM_FR = symRules(mapOf("&" to "et", "=" to "égale", "+" to "plus", "×" to "fois", "→" to "vers", "@" to "arobase"))
        private val SYM_EN = symRules(mapOf("&" to "and", "=" to "equals", "+" to "plus", "×" to "times", "→" to "to", "@" to "at"))
        private val SYM_ES = symRules(mapOf("&" to "y", "=" to "igual", "+" to "más", "×" to "por", "→" to "a", "@" to "arroba"))
        private val SYM_PT = symRules(mapOf("&" to "e", "=" to "igual", "+" to "mais", "×" to "vezes", "→" to "para", "@" to "arroba"))
        private val SYM_IT = symRules(mapOf("&" to "e", "=" to "uguale", "+" to "più", "×" to "per", "→" to "a", "@" to "chiocciola"))
        private val SYMBOL_WORDS = mapOf(
            "fr_CA" to SYM_FR, "fr" to SYM_FR, "en_US" to SYM_EN, "en_GB" to SYM_EN,
            "es" to SYM_ES, "pt_BR" to SYM_PT, "it" to SYM_IT,
        )

        private val FRAC_FR = mapOf(
            (1 to 2) to "un demi", (1 to 3) to "un tiers", (1 to 4) to "un quart",
            (3 to 4) to "trois quarts", (2 to 3) to "deux tiers", (1 to 5) to "un cinquième",
        )
        private val FRAC_EN = mapOf(
            (1 to 2) to "one half", (1 to 3) to "one third", (1 to 4) to "one quarter",
            (3 to 4) to "three quarters", (2 to 3) to "two thirds", (1 to 5) to "one fifth",
        )
        private val FRAC_ES = mapOf(
            (1 to 2) to "un medio", (1 to 3) to "un tercio", (1 to 4) to "un cuarto",
            (3 to 4) to "tres cuartos", (2 to 3) to "dos tercios", (1 to 5) to "un quinto",
        )
        private val FRAC_PT = mapOf(
            (1 to 2) to "um meio", (1 to 3) to "um terço", (1 to 4) to "um quarto",
            (3 to 4) to "três quartos", (2 to 3) to "dois terços", (1 to 5) to "um quinto",
        )
        private val FRAC_IT = mapOf(
            (1 to 2) to "un mezzo", (1 to 3) to "un terzo", (1 to 4) to "un quarto",
            (3 to 4) to "tre quarti", (2 to 3) to "due terzi", (1 to 5) to "un quinto",
        )
        private val FRACTION_DATA = mapOf(
            "fr_CA" to FracData(FRAC_FR, "sur", "et", "demi"), "fr" to FracData(FRAC_FR, "sur", "et", "demi"),
            "en_US" to FracData(FRAC_EN, "over", "and", "a half"), "en_GB" to FracData(FRAC_EN, "over", "and", "a half"),
            "es" to FracData(FRAC_ES, "sobre", "y", "medio"),
            "pt_BR" to FracData(FRAC_PT, "sobre", "e", "meio"),
            "it" to FracData(FRAC_IT, "fratto", "e", "mezzo"),
        )
        private val RANGE_WORD = mapOf(
            "fr_CA" to "à", "fr" to "à", "en_US" to "to", "en_GB" to "to",
            "es" to "a", "pt_BR" to "a", "it" to "a",
        )
        private val ROOM_KW = mapOf(
            "fr_CA" to "chambre|salle|porte|suite|local|bureau", "fr" to "chambre|salle|porte|suite|local|bureau",
            "en_US" to "room|suite|gate|unit|office|apartment", "en_GB" to "room|suite|gate|unit|office|flat",
            "es" to "habitación|sala|puerta|suite|oficina|cuarto", "pt_BR" to "quarto|sala|porta|suíte|escritório",
            "it" to "camera|sala|porta|suite|ufficio|stanza",
        )
        private val ROOM_OH = mapOf(
            "fr_CA" to "zéro", "fr" to "zéro", "es" to "cero", "pt_BR" to "zero", "it" to "zero",
        )
        private val ACR_FR = setOf("NASA", "OTAN", "UNESCO", "REER", "CELI", "CLSC", "ONU", "OVNI", "SIDA", "OK", "RADAR", "LASER", "PME")
        private val ACR_EN = setOf("NASA", "NATO", "UNESCO", "NAFTA", "ASAP", "PIN", "SIM", "GUI", "OK", "RADAR", "LASER", "SCUBA", "AM", "PM")
        private val ACR_ES = setOf("NASA", "OTAN", "UNESCO", "ONU", "OVNI", "SIDA", "OK", "RADAR", "LASER", "PYME", "UE", "OTI")
        private val ACR_PT = setOf("NASA", "OTAN", "UNESCO", "ONU", "OVNI", "AIDS", "OK", "RADAR", "LASER", "PIB")
        private val ACR_IT = setOf("NASA", "NATO", "UNESCO", "ONU", "UFO", "AIDS", "OK", "RADAR", "LASER", "PIL")
        private val ACRONYM_KEEP = mapOf(
            "fr_CA" to ACR_FR, "fr" to ACR_FR, "en_US" to ACR_EN, "en_GB" to ACR_EN,
            "es" to ACR_ES, "pt_BR" to ACR_PT, "it" to ACR_IT,
        )

        private const val RN = "(?:[IVXLCDM]{2,8}|I)"
        private val ROMAN_KW_FR = Regex(
            "\\b(Louis|Charles|Henri|François|Napoléon|Georges|Philippe|Pie|Jean-Paul|Jean|Benoît|Léon|Élisabeth|" +
                "roi|reine|pape|chapitre|tome|acte|partie|volume|article)\\s+($RN)(?:er|re|e)?\\b")
        private val ROMAN_SIECLE_FR = Regex("\\b($RN)(?:e|ᵉ|er)?\\s+(siècle|arrondissement|République|guerre mondiale|guerre)\\b")
        private val ROMAN_MONARCH_EN = Regex("\\b(King|Queen|Pope|Louis|Charles|Henry|George|Edward|William|Elizabeth)\\s+($RN)\\b")
        private val ROMAN_SECTION_EN = Regex("\\b(Chapter|Part|Act|Volume|Book|Section)\\s+($RN)\\b")

        private val ROMAN_MONARCH_ES = Regex(
            "\\b(rey|reina|papa|Felipe|Carlos|Juan|Alfonso|Fernando|Isabel|Luis|Pío|Benedicto|Pablo|Enrique)\\s+($RN)\\b")
        private val ROMAN_UNIT_ES = Regex(
            "\\b(siglo|capítulo|tomo|volumen|acto|parte|artículo)\\s+($RN)\\b", CI)
        private val ROMAN_MONARCH_PT = Regex(
            "\\b(rei|rainha|papa|Pedro|João|Henrique|Carlos|Luís|Afonso|Manuel|Fernando|Bento)\\s+($RN)\\b")
        private val ROMAN_UNIT_PT = Regex(
            "\\b(século|capítulo|tomo|volume|ato|parte|artigo)\\s+($RN)\\b", CI)
        private val ROMAN_MONARCH_IT = Regex(
            "\\b(re|regina|papa|Luigi|Enrico|Carlo|Giovanni|Pio|Benedetto|Paolo|Vittorio|Umberto|Federico)\\s+($RN)\\b")
        private val ROMAN_SECOLO_IT = Regex("\\b($RN)\\s+(secolo)\\b")
        private val ROMAN_UNIT_IT = Regex("\\b(capitolo|tomo|volume|atto|parte|articolo)\\s+($RN)\\b", CI)
    }
}
