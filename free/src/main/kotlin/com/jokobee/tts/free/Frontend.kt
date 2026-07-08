package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.MapLexiconSource
import com.jokobee.tts.free.HomographAnnotator.Ann

public class Frontend(
    private val g2p: G2p,
    private val enG2p: ((String, String) -> String)? = null,
    private val verbalizer: Verbalizer = IcuVerbalizer(),
    /** Lexique custom (couche #1), universel */
    public val lexicon: MapLexiconSource = MapLexiconSource(),
    /** Points d'insertion Pro (normalisation, dictionnaires, accent). */
    public val adapters: AdapterRegistry = AdapterRegistry(),
) {
    private val pipeline = PhonemePipeline(
        AccentG2p(adapters.accent, LexiconG2p(lexicon, DictionaryG2p(adapters.dictionary, g2p))),
    )

    /** Phonémise un texte. */
    public fun toPhonemes(text: String, lang: String): String {
        val pre = adapters.normalization.apply(text, lang, adapters.accent.current?.id)
        val normalized = Normalizers.forLang(lang, verbalizer).normalize(pre)
        if ((lang == "en_US" || lang == "en_GB") && enG2p != null) {
            return enG2p.invoke(normalized, lang)   // G2P anglais (us/gb selon lang), pas de PhonemePost
        }
        return pipeline.phonemizeAnnotations(annotate(normalized, lang), lang)
    }

    /** Annotations mot-à-mot */
    private fun annotate(text: String, lang: String): List<Ann> =
        if (lang == "fr" || lang == "fr_CA") HomographAnnotator.annotate(text)
        else TOKEN_RE.findall(text).map { Ann(it, null) }

    private companion object {
        // Mot (lettres + marques + apostrophe) OU suite de non-espaces non-lettres (ponctuation).
        private val TOKEN_RE = Regex("""[\p{L}\p{M}']+|[^\s\p{L}\p{M}']+""")
        private fun Regex.findall(s: String): List<String> = findAll(s).map { it.value }.toList()
    }
}
