package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/**
 * Orchestration phrase de misaki EN (port de `G2P.__call__`) : texte normalisé anglais
 * → chaîne IPA (convention Kokoro). Contrairement au [PhonemePipeline] mot-à-mot, misaki
 * a besoin du **contexte de phrase** (la voyelle du mot suivant décide a/an/the/to), d'où
 * un traitement droite→gauche.
 *
 * Chaîne : tokenisation → pour chaque token (droite→gauche) lookup [MisakiEnLexicon] avec
 * `futureVowel`, sinon [fallback] (CharsiuG2P) → jointure → finalisation `ɾ→T, ʔ→t`.
 * Les nombres/devise sont déjà verbalisés par le normaliseur en amont.
 *
 * Trois couches de résolution d'un mot :
 *   #1 [brandLexicon] — lexique custom (marques, ex. Jokobee). Point d'insertion prioritaire.
 *   #2 [MisakiEnLexicon] — le lexique misaki (99,6% des mots courants). AUCUN post-traitement.
 *   #3 [fallback] — CharsiuG2P mot-à-mot (mots hors-lexique). **CLAMPÉ par [PhonemePost]** :
 *      contrairement à misaki (jeu de phonèmes = vocab Kokoro par construction), CharsiuG2P
 *      peut produire des phonèmes hors-vocab (cf. fix ɫ→l/ɝ→ɜɹ/g→ɡ) → doit passer le clamp.
 */
public class MisakiEnG2p(
    private val lexicon: MisakiEnLexicon,
    private val fallback: G2p? = null,
    private val brandLexicon: Map<String, String> = emptyMap(),
    private val unk: String = "❓",
) {
    private class Tok(val text: String, val ws: String, val isWord: Boolean) {
        var ph: String = ""
    }

    /** Texte normalisé anglais → IPA Kokoro. */
    public fun phonemize(text: String): String {
        val toks = tokenize(text)
        var fv: Boolean? = null
        for (i in toks.indices.reversed()) {
            val t = toks[i]
            t.ph = if (t.isWord) {
                brandLexicon[t.text.lowercase()]                          // #1 lexique custom
                    ?: lexicon.phonemize(t.text, FUNCTION_TAGS[t.text.lowercase()], fv).first  // #2 misaki
                    ?: fallback?.phonemize(t.text, "en_US")?.ifEmpty { null }
                        ?.let { PhonemePost.apply(it, "en_US") }          // #3 CharsiuG2P CLAMPÉ
                    ?: unk
            } else {
                t.text.filter { it in NON_QUOTE_PUNCTS }
            }
            fv = tokenContext(fv, t.ph)
        }
        val out = buildString { for (t in toks) { append(t.ph); append(t.ws) } }
        return out.replace('ɾ', 'T').replace('ʔ', 't').trim()   // finalize misaki (version != 2.0)
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

        /**
         * Tag POS heuristique pour les mots-outils ultra-fréquents (lecture dominante),
         * en attendant le tagger léger de la Phase 3. Résout a/an/the (article, `ɐ`/`ði`),
         * I (pronom, `ˌI`), in (préposition, `ɪn`). Le lexique misaki fait le reste via le tag.
         */
        private val FUNCTION_TAGS: Map<String, String> = mapOf(
            "a" to "DT", "an" to "DT", "the" to "DT", "i" to "PRP", "in" to "IN",
        )

        /** Fabrique depuis les assets misaki + un fallback (CharsiuG2P en prod) + lexique custom. */
        public fun fromAssets(
            context: android.content.Context,
            fallback: G2p? = null,
            brandLexicon: Map<String, String> = emptyMap(),
            british: Boolean = false,
        ): MisakiEnG2p = MisakiEnG2p(MisakiEnLexicon.fromAssets(context, british), fallback, brandLexicon)
    }
}
