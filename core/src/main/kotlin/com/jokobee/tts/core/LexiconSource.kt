package com.jokobee.tts.core

/**
 * Source de phonèmes prioritaire (couche #1) du pipeline G2P : un lexique custom
 * consulté **avant** le G2P principal (misaki EN, règles…). Point d'insertion réservé
 * pour, plus tard, un `LexiconSource` chargé depuis un fichier TSV (marques, corrections
 * de prononciation), sans toucher au G2P ni au pipeline.
 *
 * Contrat : phonèmes IPA (convention Kokoro) pour un mot, ou `null` si absent.
 * L'implémentation par défaut ([EmptyLexiconSource]) renvoie toujours `null` (stub) :
 * le pipeline appelle donc bien la couche #1, mais elle n'a aucun effet tant qu'aucune
 * source n'est branchée.
 */
public interface LexiconSource {
    public fun lookup(word: String): String?
}

/** Stub par défaut : aucun mot custom (toujours `null`). */
public object EmptyLexiconSource : LexiconSource {
    override fun lookup(word: String): String? = null
}

/** Lexique custom en mémoire (clé insensible à la casse). Ex. marques : « jokobee » → IPA. */
public class MapLexiconSource(entries: Map<String, String>) : LexiconSource {
    private val map: Map<String, String> = entries.mapKeys { it.key.lowercase() }
    override fun lookup(word: String): String? = map[word.lowercase()]
}
