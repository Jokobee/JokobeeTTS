package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.free.HomographAnnotator.Ann

/**
 * Frontend texte → IPA (étages 1→4 du pipeline TTS). Compose :
 *   normalisation (locale) → annotations (homographes fr / tokenisation) →
 *   G2P mot-à-mot ([PhonemePipeline]) → post-traitement (NFD).
 *
 * L'étage aval (tokenisation Kokoro + inférence ONNX → audio) est branché plus tard ;
 * ce frontend s'arrête à la chaîne IPA prête à tokeniser.
 *
 * Le [G2p] est injecté : en production `CharsiuG2p.fromAssetsOrCache(context, env)` ;
 * en test un stub. Le [Verbalizer] par défaut est [IcuVerbalizer] (icu4j embarqué).
 *
 * **Anglais** : si un [enG2p] (misaki, niveau phrase) est fourni, `en_US`/`en_GB`
 * y sont routés — misaki gère le contexte de phrase et son jeu de phonèmes = le vocab
 * Kokoro (aucun PhonemePost ; son fallback CharsiuG2P est déjà clampé en interne). Sinon
 * (ou autres locales) : [PhonemePipeline] mot-à-mot + PhonemePost.
 */
public class Frontend(
    private val g2p: G2p,
    private val enG2p: ((String) -> String)? = null,
    private val verbalizer: Verbalizer = IcuVerbalizer(),
) {
    private val pipeline = PhonemePipeline(g2p)

    /** Texte brut → IPA, pour une des 10 locales supportées. */
    public fun toPhonemes(text: String, lang: String): String {
        val normalized = Normalizers.forLang(lang, verbalizer).normalize(text)
        if ((lang == "en_US" || lang == "en_GB") && enG2p != null) {
            return enG2p.invoke(normalized)   // misaki : pas de PhonemePost (déjà propre)
        }
        return pipeline.phonemizeAnnotations(annotate(normalized, lang), lang)
    }

    /**
     * Annotations mot-à-mot. Le français passe par [HomographAnnotator] (overrides IPA
     * des homographes/mots-outils) ; les autres locales par une tokenisation simple
     * (chaque token délégué au G2P, aucun override — ipa=null).
     */
    private fun annotate(text: String, lang: String): List<Ann> =
        if (lang == "fr" || lang == "fr_CA") HomographAnnotator.annotate(text)
        else TOKEN_RE.findall(text).map { Ann(it, null) }

    private companion object {
        // Mot (lettres + marques + apostrophe) OU suite de non-espaces non-lettres (ponctuation).
        private val TOKEN_RE = Regex("""[\p{L}\p{M}']+|[^\s\p{L}\p{M}']+""")
        private fun Regex.findall(s: String): List<String> = findAll(s).map { it.value }.toList()
    }
}
