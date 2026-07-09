package com.jokobee.tts.free

import java.text.Normalizer

/** Post-G2P filter for phoneme vocabulary compatibility. */
public object PhonemePost {

    /** Global substitutions (all languages). */
    private val GLOBAL: Map<Char, String> = mapOf(
        'g' to "ɡ",
        'ʼ' to "",
        'ɫ' to "l",
        'ɝ' to "ɜɹ",
        'ɚ' to "əɹ",
        '͡' to "",
    )

    /** Sequences to substitute. */
    private val TIE_BAR: List<Pair<String, String>> = listOf(
        "d͡ʒ" to "ʤ", "t͡ʃ" to "ʧ", "d͡z" to "ʣ", "t͡s" to "ʦ",
    )

    /** Per-language substitutions. */
    private val OOV: Map<String, Map<Char, String>> = mapOf(
        "it" to mapOf('h' to ""),
    )

    // The Kokoro vocab stores the precomposed cedilla; it's the only token that NFD decomposes.
    private const val CEDILLA_NFD: String = "c\u0327"

    /** Normalizes and replaces out-of-vocabulary symbols. */
    public fun apply(ipa: String, lang: String): String {
        var nfd = Normalizer.normalize(ipa, Normalizer.Form.NFD)
        if (nfd.contains(CEDILLA_NFD)) nfd = nfd.replace(CEDILLA_NFD, "\u00E7")
        for ((seq, lig) in TIE_BAR) if (nfd.contains('͡')) nfd = nfd.replace(seq, lig)
        val perLang = OOV[lang] ?: emptyMap()
        val sb = StringBuilder(nfd.length)
        for (ch in nfd) sb.append(GLOBAL[ch] ?: perLang[ch] ?: ch.toString())
        return sb.toString()
    }
}
