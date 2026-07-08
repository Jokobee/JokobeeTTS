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
                    ?: lexicon.phonemize(t.text, tagOf(toks, i), fv).first  // #2 misaki (tag heuristique)
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

    /**
     * Tag POS **heuristique** (Phase 3, sans tagger neural). Résout les homographes du
     * lexique (close/live/object/use…) par le contexte local : mot-outil fixe, sinon
     * déterminant/préposition avant → nom (NN), pronom-sujet/modal/impératif → verbe (VB).
     * Le lexique misaki n'utilise le tag QUE pour ses ~790 entrées-dict ; inoffensif ailleurs.
     */
    private fun tagOf(toks: List<Tok>, i: Int): String? {
        FUNCTION_TAGS[toks[i].text.lowercase()]?.let { return it }
        // Remonte jusqu'à 3 mots : le 1er déclencheur gagne (pronom/modal→VB, dét/prép→NN).
        // Le lookback saute les adjectifs (« a clever use », « a new record » → NN).
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

        /**
         * Tag POS heuristique pour les mots-outils ultra-fréquents (lecture dominante),
         * en attendant le tagger léger de la Phase 3. Résout a/an/the (article, `ɐ`/`ði`),
         * I (pronom, `ˌI`), in (préposition, `ɪn`). Le lexique misaki fait le reste via le tag.
         */
        private val FUNCTION_TAGS: Map<String, String> = mapOf(
            "a" to "DT", "an" to "DT", "the" to "DT", "i" to "PRP", "in" to "IN",
        )

        // Contexte pour le POS heuristique (Phase 3).
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

        /** Fabrique depuis les assets misaki + un fallback (CharsiuG2P en prod) + lexique custom. */
        public fun fromAssets(
            context: android.content.Context,
            fallback: G2p? = null,
            brandLexicon: Map<String, String> = emptyMap(),
            british: Boolean = false,
        ): MisakiEnG2p = MisakiEnG2p(MisakiEnLexicon.fromAssets(context, british), fallback, brandLexicon)
    }
}
