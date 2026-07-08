package com.jokobee.tts.free

import com.jokobee.tts.core.EmptyLexiconSource
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LexiconSource

/** Orchestration au niveau phrase du G2P anglais */
public class MisakiEnG2p(
    private val lexicon: MisakiEnLexicon,
    private val fallback: G2p? = null,
    private val customLexicon: LexiconSource = EmptyLexiconSource,
    private val lang: String = "en_US",
    private val unk: String = "❓",
    private val dictionary: DictionaryRegistry? = null,
    private val accent: AccentRegistry? = null,
) {
    private class Tok(val text: String, var ws: String, val isWord: Boolean) {
        var ph: String = ""
    }

    /** Texte normalisé anglais */
    public fun phonemize(text: String): String {
        val toks = tokenize(text)
        val pre = multiWord(toks)   // séquences multi-mots du dico (greedy) résolues d'avance
        var fv: Boolean? = null
        for (i in toks.indices.reversed()) {
            val t = toks[i]
            t.ph = when {
                pre[i] != null -> withAccent(pre[i]!!, t)      // tête multi-mots ("" pour les consommés)
                t.isWord -> {
                    val resolved = customLexicon.lookup(t.text, lang)     // #1 tts.lexicon (EN TÊTE)
                        ?: dictionary?.lookup(t.text, lang)?.let { PhonemePost.apply(it, lang) }  // #2 tts.dictionary
                        ?: lexicon.phonemize(t.text, tagOf(toks, i), fv).first  // #3 lexique (tag)
                        ?: fallback?.phonemize(t.text, "en_US")?.ifEmpty { null }
                            ?.let { PhonemePost.apply(it, "en_US") }      // #4 fallback CLAMPÉ
                        ?: unk
                    withAccent(resolved, t)
                }
                else -> t.text.filter { it in NON_QUOTE_PUNCTS }
            }
            fv = tokenContext(fv, t.ph)
        }
        val out = buildString { for (t in toks) { append(t.ph); append(t.ws) } }
        return out.replace('ɾ', 'T').replace('ʔ', 't').trim()   // finalisation
    }

    private fun withAccent(ipa: String, t: Tok): String =
        if (t.isWord && accent?.current != null && ipa.isNotEmpty())
            PhonemePost.apply(accent.apply(ipa, t.text, lang), lang) else ipa

    // Fenêtre glissante greedy (séquences les plus longues d'abord) sur les mots consécutifs.
    private fun multiWord(toks: List<Tok>): Array<String?> {
        val pre = arrayOfNulls<String>(toks.size)
        val dict = dictionary ?: return pre
        var i = 0
        while (i < toks.size) {
            if (toks[i].isWord) {
                var j = i
                while (j + 1 < toks.size && toks[j + 1].isWord) j++
                var end = j
                var hit: String? = null
                while (end > i) {
                    val phrase = (i..end).joinToString(" ") { toks[it].text }.lowercase()
                    hit = dict.lookup(phrase, lang)
                    if (hit != null) break
                    end--
                }
                if (hit != null) {
                    pre[i] = PhonemePost.apply(hit, lang)
                    for (k in i + 1..end) { pre[k] = ""; toks[k].ws = "" }
                    toks[i].ws = toks[end].ws
                    i = end + 1
                    continue
                }
            }
            i++
        }
        return pre
    }

    private fun tokenize(text: String): List<Tok> {
        val toks = ArrayList<Tok>()
        val matches = TOKEN_RE.findAll(text).toList()
        for ((idx, m) in matches.withIndex()) {
            val nextStart = if (idx + 1 < matches.size) matches[idx + 1].range.first else text.length
            val ws = text.substring(m.range.last + 1, nextStart)
            toks.add(Tok(m.value, ws, m.value.any { it.isLetter() }))
        }
        return toks
    }

    /** Étiquette catégorielle légère. */
    private fun tagOf(toks: List<Tok>, i: Int): String? {
        FUNCTION_TAGS[toks[i].text.lowercase()]?.let { return it }
        var p = i - 1; var steps = 0
        while (p >= 0 && steps < 3) {
            if (toks[p].isWord) {
                val w = toks[p].text.lowercase()
                if (w in VERB_TRIGGERS) return "VB"
                if (w in DETERMINERS || w in PREPOSITIONS) return "NN"
                steps++
            }
            p--
        }
        if (nextWord(toks, i) in DETERMINERS) return "VB"   // « close the door » (impératif) → verbe
        return null
    }

    private fun nextWord(toks: List<Tok>, i: Int): String? {
        var p = i + 1
        while (p < toks.size && !toks[p].isWord) p++
        return if (p < toks.size) toks[p].text.lowercase() else null
    }

    /** Le mot courant (ps) commence-t-il par une voyelle ? (null si ponctuation/inconnu). */
    private fun tokenContext(prev: Boolean?, ps: String): Boolean? {
        for (c in ps) {
            if (c in NON_QUOTE_PUNCTS) return null
            if (c in VOWELS) return true
            if (c in CONSONANTS) return false
        }
        return prev
    }

    public companion object {
        // Mot (lettres, apostrophe/tiret internes) OU un caractère non-mot (ponctuation).
        private val TOKEN_RE = Regex("""[A-Za-z]+(?:['’-][A-Za-z]+)*|[^\sA-Za-z]""")
        private const val VOWELS = "AIOQWYaiuæɑɒɔəɛɜɪʊʌᵻ"
        private const val CONSONANTS = "bdfhjklmnpstvwzðŋɡɹɾʃʒʤʧθ"
        private const val NON_QUOTE_PUNCTS = ";:,.!?—…"

        /** Tags pour les mots-outils ultra-fréquents (lecture dominante) */
        private val FUNCTION_TAGS: Map<String, String> = mapOf(
            "a" to "DT", "an" to "DT", "the" to "DT", "i" to "PRP", "in" to "IN",
        )

        // Contexte de désambiguïsation par le tag.
        private val DETERMINERS = setOf(
            "the", "a", "an", "this", "that", "these", "those", "my", "your", "his", "her",
            "its", "our", "their", "no", "some", "any", "each", "every", "another", "such",
        )
        private val PREPOSITIONS = setOf(
            "of", "in", "on", "at", "for", "with", "from", "by", "about", "into", "over",
            "under", "through", "between", "against", "without", "upon", "onto",
        )
        private val VERB_TRIGGERS = setOf(
            "to", "will", "would", "can", "could", "shall", "should", "may", "might", "must",
            "do", "does", "did", "let", "please", "not", "we", "they", "i", "you", "he", "she", "it",
        )

        /** Fabrique depuis les lexiques embarqués + un fallback + lexique custom. */
        public fun fromAssets(
            context: android.content.Context,
            fallback: G2p? = null,
            customLexicon: LexiconSource = EmptyLexiconSource,
            british: Boolean = false,
            dictionary: DictionaryRegistry? = null,
            accent: AccentRegistry? = null,
        ): MisakiEnG2p = MisakiEnG2p(
            MisakiEnLexicon.fromAssets(context, british), fallback, customLexicon,
            lang = if (british) "en_GB" else "en_US",
            dictionary = dictionary, accent = accent,
        )
    }
}
