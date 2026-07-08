package com.jokobee.tts.core

import android.content.Context
import java.io.InputStream

/**
 * Charge un lexique de prononciation depuis un **TSV** (format de-facto WikiPron) :
 *   `mot<TAB>prononciation`
 * - Lignes vides et commentaires (`#` en début de ligne) ignorés ; UTF-8.
 * - **Alphabet déclaré** à la construction (défaut IPA) ; converti en IPA canonique au chargement.
 * - Mono-langue : la source ne répond que pour sa `lang`. Implémente [LexiconSource] → se branche
 *   au crochet lexique #1 via `tts.lexicon.load(...)`.
 *
 * NOTE LICENCE : les prononciations dérivées de Wiktionary (via WikiPron) sont **CC BY-SA 4.0**
 * (share-alike). Un dev peut les CHARGER et les utiliser ; Jokobee ne peut PAS les redistribuer
 * dans un pack propriétaire fermé. Les dictionnaires vendus par Jokobee doivent venir de sources
 * permissives (CMUdict pour l'anglais) ou de transcription maison.
 */
public class TsvLexiconLoader private constructor(
    private val lang: String,
    private val entries: Map<String, String>,
) : LexiconSource {

    override fun lookup(word: String, lang: String): String? =
        if (lang == this.lang) entries[word.lowercase()] else null

    public val size: Int get() = entries.size

    public companion object {
        /** Parse un flux TSV. `onWarn` : tokens ARPABET/lignes inconnus (jamais de crash). */
        public fun fromStream(
            input: InputStream,
            lang: String,
            alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA,
            onWarn: (String) -> Unit = {},
        ): TsvLexiconLoader {
            val map = HashMap<String, String>()
            input.bufferedReader().useLines { lines ->
                for (raw in lines) {
                    val line = raw.trimEnd('\n', '\r')
                    if (line.isBlank() || line.trimStart().startsWith("#")) continue
                    val tab = line.indexOf('\t')
                    if (tab < 0) { onWarn(line); continue }
                    val word = line.substring(0, tab).trim()
                    val pron = line.substring(tab + 1).trim()
                    if (word.isEmpty() || pron.isEmpty()) continue
                    map[word.lowercase()] = alphabet.toIpa(pron, onWarn)
                }
            }
            return TsvLexiconLoader(lang, map)
        }

        /** Charge depuis un asset : `TsvLexiconLoader.fromAsset(context, "custom.tsv", "en_US")`. */
        public fun fromAsset(
            context: Context,
            assetPath: String,
            lang: String,
            alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA,
        ): TsvLexiconLoader = fromStream(context.assets.open(assetPath), lang, alphabet)
    }
}
