package com.jokobee.tts.free

import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale

/** Verbalizes numbers */
public interface Verbalizer {
    /** Cardinal */
    public fun cardinal(n: Long, locale: String): String

    /** Ordinal */
    public fun ordinal(n: Long, locale: String, feminine: Boolean = false): String

    /** Verbalization with a ruleset */
    public fun spellout(n: Long, locale: String, ruleset: String): String
}

/** icu4j implementation (production + test, identical output) */
public class IcuVerbalizer : Verbalizer {

    private val cache = HashMap<String, RuleBasedNumberFormat>()

    private fun fmt(locale: String): RuleBasedNumberFormat =
        cache.getOrPut(locale) {
            RuleBasedNumberFormat(ULocale(locale), RuleBasedNumberFormat.SPELLOUT)
        }

    private fun String.clean() = replace("­", "")   // purge soft hyphens (it)

    override fun cardinal(n: Long, locale: String): String = fmt(locale).format(n).clean()

    override fun ordinal(n: Long, locale: String, feminine: Boolean): String {
        val f = fmt(locale)
        val rs = if (feminine) "%spellout-ordinal-feminine" else "%spellout-ordinal-masculine"
        return try {
            f.format(n, rs).clean()
        } catch (e: Exception) {
            try {
                f.format(n, "%spellout-ordinal").clean()
            } catch (e2: Exception) {
                cardinal(n, locale)
            }
        }
    }

    override fun spellout(n: Long, locale: String, ruleset: String): String =
        try {
            fmt(locale).format(n, ruleset).clean()
        } catch (e: Exception) {
            cardinal(n, locale)
        }
}
