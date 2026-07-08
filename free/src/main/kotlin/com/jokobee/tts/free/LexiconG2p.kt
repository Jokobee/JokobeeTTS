package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LexiconSource

/** Décorateur [G2p] appliquant le crochet lexique custom (couche #1) avant le G2P délégué */
public class LexiconG2p(
    private val lexicon: LexiconSource,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        lexicon.lookup(word, lang) ?: delegate.phonemize(word, lang)
}
