package com.jokobee.tts.free

import com.jokobee.tts.core.G2p

/** Transforms a normalized text into an IPA string. */
public class PhonemePipeline(
    private val g2p: G2p,
    private val post: (String, String) -> String = PhonemePost::apply,
) {
    /** Phonemizes a normalized text. */
    public fun phonemize(
        text: String,
        lang: String,
        annotate: (String) -> List<HomographAnnotator.Ann> = HomographAnnotator::annotate,
    ): String = phonemizeAnnotations(annotate(text), lang)

    /** Phonemizes from already computed annotations. */
    public fun phonemizeAnnotations(anns: List<HomographAnnotator.Ann>, lang: String): String {
        val sb = StringBuilder()
        for (ann in anns) {
            val piece = when {
                ann.ipa != null -> ann.ipa                       // forced override (bypass G2P)
                isWord(ann.token) -> g2p.phonemize(ann.token, lang)
                else -> ann.token                                // punctuation / symbol: literal
            }
            if (piece.isEmpty()) continue
            if (sb.isNotEmpty() && isWord(ann.token)) sb.append(' ')  // space before a word
            else if (sb.isNotEmpty() && ann.ipa != null) sb.append(' ')
            sb.append(piece)
        }
        return post(collapse(sb.toString()), lang)
    }

    private fun isWord(token: String): Boolean = token.any { it.isLetter() }

    private companion object {
        // Reattaches punctuation (removes the space preceding it) and collapses spaces.
        private val SPACE_BEFORE_PUNCT = Regex("""\s+([,.;:!?…»)\]])""")
        private val MULTISPACE = Regex("""\s{2,}""")
        fun collapse(s: String): String =
            MULTISPACE.replace(SPACE_BEFORE_PUNCT.replace(s.trim(), "$1"), " ")
    }
}
