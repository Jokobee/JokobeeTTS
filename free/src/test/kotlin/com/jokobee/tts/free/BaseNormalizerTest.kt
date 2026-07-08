package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test

/** Sous-classe stub sans règle de locale */
private class StubNormalizer(v: Verbalizer) : BaseNormalizer(v) {
    override val locale: String = "fr_CA"
    override fun rules(): List<(String) -> String> = emptyList()
}

/** Bloc 2 */
class BaseNormalizerTest {

    private val n = StubNormalizer(IcuVerbalizer())

    @Test fun videEtNull() {
        assertEquals("", n.normalize(null))
        assertEquals("", n.normalize(""))
    }

    @Test fun verbaliseTelUrl() {
        assertEquals("Appelez le quatre un huit, cinq cinq cinq, un deux trois quatre svp.", n.normalize("Appelez le 418-555-1234 svp."))
        assertEquals("Voir jokobee point com ici.", n.normalize("Voir https://jokobee.com ici."))
    }

    @Test fun whitelist() {
        assertEquals("Voir et cétéra ici.", n.normalize("Voir etc. ici."))
    }

    @Test fun ponctuation() {
        assertEquals("Attends… vraiment ? Oui !", n.normalize("Attends... vraiment ?? Oui !!"))
    }

    @Test fun fallbackEmojiRetire() {
        assertEquals("Bravo !", n.normalize("Bravo 🎉🎉 !"))
    }
}
