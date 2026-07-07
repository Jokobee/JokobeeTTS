package com.jokobee.tts.free

import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale

/**
 * Verbalise les nombres. Port de la couche Verbalizer de core.py.
 *
 * Prod = [IcuVerbalizer] (icu4j EMBARQUÉ, RuleBasedNumberFormat SPELLOUT).
 * ⚠ android.icu N'EXPOSE PAS RuleBasedNumberFormat (spellout) → on bundle icu4j.
 * PAS de Num2WordsVerbalizer (banc Python LGPL, ne traverse pas au Kotlin).
 * Interface → une impl maison compacte pourra remplacer icu4j (poids) plus tard.
 */
public interface Verbalizer {
    /** Cardinal : 1234 → « mille deux cent trente-quatre » (ruleset par défaut). */
    public fun cardinal(n: Long, locale: String): String

    /** Ordinal : 1 → « premier »/« première » (feminine). */
    public fun ordinal(n: Long, locale: String, feminine: Boolean = false): String

    /**
     * Verbalisation avec un ruleset ICU explicite (ex. zh « %spellout-numbering-year »,
     * ko « %spellout-cardinal-native-attributive »). Fallback sur cardinal si absent.
     */
    public fun spellout(n: Long, locale: String, ruleset: String): String
}

/**
 * Implémentation icu4j (production + test, sortie identique). Purge soft hyphens.
 *
 * TODO(v1.1) : icu4j embarqué (+~13 Mo pré-R8) parce qu'`android.icu` ne publie PAS
 * `RuleBasedNumberFormat`. Cible v1.1 : impl maison compacte (nombre→mots des 6
 * latines) OU proguard/R8 ciblé pour repasser l'AAR sous 5 Mo. Réversible : le reste
 * du code dépend de l'interface [Verbalizer], pas de cette classe.
 */
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
