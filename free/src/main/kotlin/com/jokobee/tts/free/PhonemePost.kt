package com.jokobee.tts.free

import java.text.Normalizer

/** Filtre post-G2P pour la compatibilité du vocabulaire de phonèmes. */
public object PhonemePost {

    /** Substitutions globales (toutes langues). */
    private val GLOBAL: Map<Char, String> = mapOf(
        'g' to "ɡ",
        'ʼ' to "",
        'ɫ' to "l",
        'ɝ' to "ɜɹ",
        'ɚ' to "əɹ",
        '͡' to "",
    )

    /** Séquences à substituer. */
    private val TIE_BAR: List<Pair<String, String>> = listOf(
        "d͡ʒ" to "ʤ", "t͡ʃ" to "ʧ", "d͡z" to "ʣ", "t͡s" to "ʦ",
    )

    /** Substitutions par langue. */
    private val OOV: Map<String, Map<Char, String>> = mapOf(
        "it" to mapOf('h' to ""),
    )

    /** Normalise et remplace les symboles hors vocabulaire. */
    public fun apply(ipa: String, lang: String): String {
        var nfd = Normalizer.normalize(ipa, Normalizer.Form.NFD)
        for ((seq, lig) in TIE_BAR) if (nfd.contains('͡')) nfd = nfd.replace(seq, lig)
        val perLang = OOV[lang] ?: emptyMap()
        val sb = StringBuilder(nfd.length)
        for (ch in nfd) sb.append(GLOBAL[ch] ?: perLang[ch] ?: ch.toString())
        return sb.toString()
    }
}
