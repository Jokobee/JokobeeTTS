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

    /**
     * Mapping GLOBAL (toutes langues) : tics de sortie de CharsiuG2P hors vocab Kokoro.
     * `g` (U+0067, g ASCII) → `ɡ` (U+0261, g IPA) : le vocab Kokoro n'a QUE le ɡ IPA ;
     * sans ce mapping, le g ASCII est droppé silencieusement (audit : 64 mots fr + 42 es,
     * ex. « grand→gʁɑ̃ », « tengo→teŋgo »). Vérifié par audit OOV sur top-2000 mots fr/es.
     */
    private val GLOBAL: Map<Char, String> = mapOf(
        'g' to "ɡ",
        // ʼ (U+02BC) : marqueur de h muet/liaison de CharsiuG2P, non phonémique dans nos
        // langues (fr/es/romanes) → suppression EXPLICITE (« hommes→ʼɔm » donne « ɔm »,
        // la bonne prononciation du h français muet). Audit OOV : 16 mots fr.
        'ʼ' to "",
        // Chars IPA de CharsiuG2P (anglais) absents du vocab Kokoro → convention Kokoro.
        // Nécessaires quand CharsiuG2P sert de FALLBACK OOV anglais (Paul→pˈɔɫ, world→wˈɝld) ;
        // sans eux, ɫ/ɝ sont droppés (« world »→« wd »). N'apparaissent pas en fr/es.
        'ɫ' to "l",     // L vélarisé (dark L) → l
        'ɝ' to "ɜɹ",    // schwa rhotique accentué → ɜɹ
        'ɚ' to "əɹ",    // schwa rhotique → əɹ
    )

    /** Mapping OOV par langue : symbole IPA hors vocab Kokoro → substitut le plus proche. */
    private val OOV: Map<String, Map<Char, String>> = mapOf(
        // À peupler par l'audit OOV / la boucle WER (chaque entrée justifiée par une mesure).
        // Structure prête, ex. "de" to mapOf('ʏ' to "y").
    )

    /**
     * Applique NFD + le mapping GLOBAL + le mapping OOV de `lang`. Idempotent modulo NFD.
     * Ne strippe jamais : un symbole non mappé est conservé tel quel.
     */
    public fun apply(ipa: String, lang: String): String {
        val nfd = Normalizer.normalize(ipa, Normalizer.Form.NFD)
        val perLang = OOV[lang] ?: emptyMap()
        if (GLOBAL.isEmpty() && perLang.isEmpty()) return nfd
        val sb = StringBuilder(nfd.length)
        for (ch in nfd) sb.append(GLOBAL[ch] ?: perLang[ch] ?: ch.toString())
        return sb.toString()
    }
}
