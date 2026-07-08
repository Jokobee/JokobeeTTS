package com.jokobee.tts.core

import android.content.Context
import java.io.InputStream

/** Charge un lexique de prononciation depuis un TSV. */
public class TsvLexiconLoader private constructor(
    private val lang: String,
    private val entries: Map<String, String>,
) : LexiconSource {

    override fun lookup(word: String, lang: String): String? =
        if (lang == this.lang) entries[word.lowercase()] else null

    public val size: Int get() = entries.size

    public companion object {
        /** Parse un flux TSV */
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

        /** Charge depuis un asset */
        public fun fromAsset(
            context: Context,
            assetPath: String,
            lang: String,
            alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA,
        ): TsvLexiconLoader = fromStream(context.assets.open(assetPath), lang, alphabet)
    }
}
