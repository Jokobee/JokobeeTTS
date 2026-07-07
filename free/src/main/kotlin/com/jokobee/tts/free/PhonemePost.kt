package com.jokobee.tts.free

import java.text.Normalizer

/**
 * Étage 4 — qualité fine de phonémisation (cf. ARCHITECTURE §4).
 *
 * 1. **NFD imposé** : le tokeniseur du modèle Kokoro v1.0 attend les IPA en forme
 *    décomposée (ex. nasales = voyelle + U+0303). C'est l'équivalent Kotlin du
 *    `unicodedata.normalize("NFD", …)` du banc Python — sans NFD, les caractères
 *    composés tombent hors vocab et sont silencieusement perdus à la tokenisation.
 * 2. **Mapping OOV par traits** : certains symboles IPA produits par le G2P ne sont
 *    pas dans le vocab Kokoro → table statique par langue (ex. `ʏ→y`). Jamais de
 *    drop silencieux : un OOV mappé est remplacé, pas supprimé. La table est
 *    volontairement minimale ici ; elle s'étoffe par la boucle WER (mesurer avant
 *    d'ajouter).
 *
 * Les suprasegmentaux (stress ˈ ˌ, tons) sont **conservés** : aucun stripping.
 */
public object PhonemePost {

    /** Mapping OOV par langue : symbole IPA hors vocab Kokoro → substitut le plus proche. */
    private val OOV: Map<String, Map<Char, String>> = mapOf(
        // Vide pour l'instant : à peupler par la boucle WER (chaque entrée justifiée
        // par une mesure, pas devinée). Structure prête, ex. "de" to mapOf('ʏ' to "y").
    )

    /**
     * Applique NFD + le mapping OOV de `lang`. Idempotent modulo NFD.
     * Ne strippe jamais : un symbole non mappé est conservé tel quel.
     */
    public fun apply(ipa: String, lang: String): String {
        val nfd = Normalizer.normalize(ipa, Normalizer.Form.NFD)
        val map = OOV[lang] ?: return nfd
        if (map.isEmpty()) return nfd
        val sb = StringBuilder(nfd.length)
        for (ch in nfd) sb.append(map[ch] ?: ch.toString())
        return sb.toString()
    }
}
