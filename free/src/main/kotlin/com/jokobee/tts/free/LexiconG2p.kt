package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LexiconSource

/**
 * Décorateur [G2p] appliquant le crochet lexique custom (couche #1) **avant** le G2P
 * délégué — pour les langues au G2P mot-à-mot (fr/es via CharsiuG2P). L'anglais applique
 * son crochet à l'intérieur de [MisakiEnG2p] ; ce décorateur rend le crochet **universel**.
 */
public class LexiconG2p(
    private val lexicon: LexiconSource,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        lexicon.lookup(word, lang) ?: delegate.phonemize(word, lang)
}
