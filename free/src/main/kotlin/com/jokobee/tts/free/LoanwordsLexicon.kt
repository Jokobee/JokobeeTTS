package com.jokobee.tts.free

import android.content.Context
import java.io.InputStream

/** Anglicismes universels (IPA anglais), avec exclusion par langue (le natif gagne si collision). */
public class LoanwordsLexicon private constructor(
    private val map: Map<String, String>,
    private val excluded: Map<String, Set<String>>,
) {

    /** IPA anglais si le mot est un anglicisme non-exclu pour cette langue, sinon null. */
    public fun lookup(word: String, lang: String): String? {
        val w = word.lowercase()
        if (excluded[w]?.contains(langKey(lang)) == true) return null   // mot courant natif : natif gagne
        return map[w]
    }

    public val size: Int get() = map.size

    /** Toutes les entrées (mot lowercase → IPA), pour l'audit. */
    public val all: Map<String, String> get() = map

    private fun langKey(lang: String): String = lang.substringBefore('_')   // fr_CA→fr, pt_BR→pt

    public companion object {
        public val EMPTY: LoanwordsLexicon = LoanwordsLexicon(emptyMap(), emptyMap())

        public fun fromStreams(main: InputStream, exclude: InputStream?): LoanwordsLexicon {
            val map = HashMap<String, String>()
            main.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val cols = line.split('\t')
                    if (cols.size >= 2) {
                        val w = cols[0].trim(); val ipa = cols[1].trim()
                        if (w.isNotEmpty() && ipa.isNotEmpty()) map[w.lowercase()] = ipa
                    }
                }
            }
            val excluded = HashMap<String, Set<String>>()
            exclude?.bufferedReader(Charsets.UTF_8)?.useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val cols = line.split('\t')
                    if (cols.size >= 2) excluded[cols[0].trim().lowercase()] =
                        cols[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                }
            }
            return LoanwordsLexicon(map, excluded)
        }

        public fun fromAssets(context: Context): LoanwordsLexicon {
            val main = context.assets.open("jokobeetts/loanwords_en_ipa.tsv")
            val exclude = try {
                context.assets.open("jokobeetts/loanwords_exclude.tsv")
            } catch (e: Exception) {
                null
            }
            return main.use { m -> if (exclude != null) exclude.use { e -> fromStreams(m, e) } else fromStreams(m, null) }
        }
    }
}
