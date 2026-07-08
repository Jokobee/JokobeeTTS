package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/** Transforme un texte normalisé en chaîne IPA. */
public class PhonemePipeline(
    private val g2p: G2p,
    private val post: (String, String) -> String = PhonemePost::apply,
) {
    /** Phonémise un texte normalisé. */
    public fun phonemize(
        text: String,
        lang: String,
        annotate: (String) -> List<HomographAnnotator.Ann> = HomographAnnotator::annotate,
    ): String = phonemizeAnnotations(annotate(text), lang)

    /** Phonémise à partir d'annotations déjà calculées. */
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
