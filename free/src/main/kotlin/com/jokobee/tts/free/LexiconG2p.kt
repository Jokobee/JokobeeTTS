package com.jokobee.tts.free

import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.LexiconSource

/** [G2p] decorator applying the custom lexicon hook (layer #1) before the delegated G2P */
public class LexiconG2p(
    private val lexicon: LexiconSource,
    private val delegate: G2p,
) : G2p {
    override fun phonemize(word: String, lang: String): String =
        lexicon.lookup(word, lang) ?: delegate.phonemize(word, lang)
}
