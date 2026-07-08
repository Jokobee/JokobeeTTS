package com.jokobee.tts.core

/** Conversion d'une prononciation ARPABET vers l'IPA. */
public object ArpabetToIpa {

    private val TOKEN = Regex("^([A-Za-z]+)([0-2]?)$")

    /** Voyelles à IPA fixe (le marqueur de stress est préfixé). */
    private val VOWELS: Map<String, String> = mapOf(
        "AA" to "ɑ", "AE" to "æ", "AO" to "ɔ", "AW" to "aʊ", "AY" to "aɪ",
        "EH" to "ɛ", "EY" to "eɪ", "IH" to "ɪ", "IY" to "i",
        "OW" to "oʊ", "OY" to "ɔɪ", "UH" to "ʊ", "UW" to "u",
    )

    /** Consonnes (aucun stress). */
    private val CONSONANTS: Map<String, String> = mapOf(
        "B" to "b", "CH" to "ʧ", "D" to "d", "DH" to "ð", "F" to "f", "G" to "ɡ", "HH" to "h",
        "JH" to "ʤ", "K" to "k", "L" to "l", "M" to "m", "N" to "n", "NG" to "ŋ", "P" to "p",
        "R" to "ɹ", "S" to "s", "SH" to "ʃ", "T" to "t", "TH" to "θ", "V" to "v", "W" to "w",
        "Y" to "j", "Z" to "z", "ZH" to "ʒ",
    )

    private fun marker(stress: String): String = when (stress) { "1" -> "ˈ"; "2" -> "ˌ"; else -> "" }

    /** Convertit une prononciation ARPABET en IPA. */
    public fun toIpa(arpabet: String, onWarn: (String) -> Unit = {}): String {
        val sb = StringBuilder()
        for (tok in arpabet.trim().split(Regex("\\s+"))) {
            if (tok.isEmpty()) continue
            val m = TOKEN.matchEntire(tok)
            if (m == null) { onWarn(tok); continue }
            val base = m.groupValues[1].uppercase()
            val stress = m.groupValues[2]
            when (base) {
                // Voyelles à IPA dépendant du stress (schwa vs wedge, r-schwa).
                "AH" -> sb.append(if (stress == "0" || stress.isEmpty()) "ə" else marker(stress) + "ʌ")
                "ER" -> sb.append(if (stress == "0" || stress.isEmpty()) "əɹ" else marker(stress) + "ɜɹ")
                else -> {
                    val vowel = VOWELS[base]
                    val cons = CONSONANTS[base]
                    when {
                        vowel != null -> sb.append(marker(stress) + vowel)
                        cons != null -> sb.append(cons)
                        else -> onWarn(tok)                                            // hors 39 → ignoré
                    }
                }
            }
        }
        return sb.toString()
    }
}
