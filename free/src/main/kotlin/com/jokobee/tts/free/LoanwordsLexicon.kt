package com.jokobee.tts.free

import android.content.Context
import java.io.InputStream

/** Lexique interne des anglicismes universels (IPA anglais). */
public class LoanwordsLexicon private constructor(private val map: Map<String, String>) {

    public fun lookup(word: String): String? = map[word.lowercase()]

    public val size: Int get() = map.size

    /** Toutes les entrées (mot lowercase → IPA), pour l'audit. */
    public val all: Map<String, String> get() = map

    public companion object {
        public val EMPTY: LoanwordsLexicon = LoanwordsLexicon(emptyMap())

        public fun fromStream(stream: InputStream): LoanwordsLexicon {
            val map = HashMap<String, String>()
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val cols = line.split('\t')
                    if (cols.size >= 2) {
                        val w = cols[0].trim()
                        val ipa = cols[1].trim()
                        if (w.isNotEmpty() && ipa.isNotEmpty()) map[w.lowercase()] = ipa
                    }
                }
            }
            return LoanwordsLexicon(map)
        }

        public fun fromAssets(context: Context): LoanwordsLexicon =
            context.assets.open("jokobeetts/loanwords_en_ipa.tsv").use { fromStream(it) }
    }
}
