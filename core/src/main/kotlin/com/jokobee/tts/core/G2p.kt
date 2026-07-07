package com.jokobee.tts.core

/**
 * Grapheme→Phoneme : convertit un mot en chaîne IPA pour une locale donnée.
 *
 * Étage 3 du pipeline (cf. ARCHITECTURE §1). L'implémentation Free est
 * [com.jokobee.tts.free.CharsiuG2p] (modèle CharsiuG2P, licence MIT), pilotée
 * mot par mot : les overrides d'homographes/mots-outils (étage 2) court-circuitent
 * le G2P en fournissant l'IPA directement — d'où le grain « un mot ».
 *
 * Contrat : entrée = UN mot graphémique (sans espace) ; sortie = IPA (peut être
 * vide si le modèle ne produit rien). Jamais d'exception sur mot inconnu : le G2P
 * fait de son mieux (le mapping OOV fin est traité en aval par PhonemePost).
 */
public interface G2p {
    public fun phonemize(word: String, lang: String): String
}
