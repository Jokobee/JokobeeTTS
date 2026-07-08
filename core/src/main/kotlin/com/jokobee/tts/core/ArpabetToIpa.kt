package com.jokobee.tts.core

/**
 * Convertit de l'**ARPABET** (CMUdict, 39 phonèmes, chiffre de stress collé aux voyelles)
 * vers l'**IPA** dans le vocab Kokoro. Pur, zéro dépendance.
 *
 * Corrections vérifiées contre le vocab Kokoro réel + la sortie misaki :
 * - affriquées **CH→ʧ, JH→ʤ** (ligatures U+02A7/U+02A4, présentes dans le vocab), pas `tʃ/dʒ` ;
 * - **ER0→əɹ, ER1/2→ɜɹ** (`ɝ` U+025D absent du vocab ; misaki écrit `ɜɹ`/`əɹ`, ex. `bird→bˈɜɹd`) ;
 * - **G→ɡ** (U+0261, pas g ASCII) et **R→ɹ** (pas r ASCII).
 *
 * Stress (chiffre suffixé à la voyelle) : 1→`ˈ`, 2→`ˌ`, 0→aucun ; marqueur émis **avant** la
 * voyelle (convention misaki, ex. `ʧˈɜɹʧ`). Token hors des 39 → ignoré + `onWarn` (jamais de crash).
 * La sortie doit **toujours** passer ensuite par le clamp de vocab (garde-fou).
 */
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

    /** ARPABET (tokens séparés par espaces) → IPA. `onWarn` appelé sur token inconnu. */
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
