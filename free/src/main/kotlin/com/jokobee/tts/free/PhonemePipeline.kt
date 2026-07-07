package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/**
 * Orchestrateur de phonémisation (étages 2→4). Transforme du texte NORMALISÉ en
 * chaîne IPA prête pour la tokenisation Kokoro.
 *
 * (Nommé `PhonemePipeline` et non « Phonemi*r » pour éviter toute confusion — et le
 * faux positif de la garde anti-GPL — avec la lib GPL du même nom, jamais embarquée.)
 *
 * Chaîne :
 *   1. annotations mot-à-mot (homographes/mots-outils fr → IPA forcée, sinon null) ;
 *   2. pour chaque token : override IPA s'il existe, sinon [G2p] mot-à-mot si c'est
 *      un mot, sinon le token littéral (ponctuation conservée) ;
 *   3. jointure (espace entre mots, ponctuation recollée) ;
 *   4. [PhonemePost] (NFD + OOV) sur le résultat.
 *
 * Le G2P est injecté ([G2p]) → testable avec un stub, et remplaçable (CharsiuG2P
 * Free, autre moteur Pro). Le grain « un mot » est compatible avec CharsiuG2P et
 * avec le bypass des overrides (une IPA forcée court-circuite le modèle).
 */
public class PhonemePipeline(
    private val g2p: G2p,
    private val post: (String, String) -> String = PhonemePost::apply,
) {
    /** Texte normalisé → IPA. `annotate` = fournisseur d'annotations (défaut : fr). */
    public fun phonemize(
        text: String,
        lang: String,
        annotate: (String) -> List<HomographAnnotator.Ann> = HomographAnnotator::annotate,
    ): String = phonemizeAnnotations(annotate(text), lang)

    /** Variante à partir d'annotations déjà calculées (langues sans homographes : ipa=null). */
    public fun phonemizeAnnotations(anns: List<HomographAnnotator.Ann>, lang: String): String {
        val sb = StringBuilder()
        for (ann in anns) {
            val piece = when {
                ann.ipa != null -> ann.ipa                       // override forcé (bypass G2P)
                isWord(ann.token) -> g2p.phonemize(ann.token, lang)
                else -> ann.token                                // ponctuation / symbole : littéral
            }
            if (piece.isEmpty()) continue
            if (sb.isNotEmpty() && isWord(ann.token)) sb.append(' ')  // espace avant un mot
            else if (sb.isNotEmpty() && ann.ipa != null) sb.append(' ')
            sb.append(piece)
        }
        return post(collapse(sb.toString()), lang)
    }

    private fun isWord(token: String): Boolean = token.any { it.isLetter() }

    private companion object {
        // Recolle la ponctuation (retire l'espace qui la précède) et compacte les espaces.
        private val SPACE_BEFORE_PUNCT = Regex("""\s+([,.;:!?…»)\]])""")
        private val MULTISPACE = Regex("""\s{2,}""")
        fun collapse(s: String): String =
            MULTISPACE.replace(SPACE_BEFORE_PUNCT.replace(s.trim(), "$1"), " ")
    }
}
