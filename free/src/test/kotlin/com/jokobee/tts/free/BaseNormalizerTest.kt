package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test

/** Sous-classe stub sans règle de locale : teste le squelette universel. */
private class StubNormalizer(v: Verbalizer) : BaseNormalizer(v) {
    override val locale: String = "fr_CA"
    override fun rules(): List<(String) -> String> = emptyList()
}

/** Bloc 2 : le squelette (protect + whitelist + punctuation + fallback) tourne sans locale. */
class BaseNormalizerTest {

    private val n = StubNormalizer(IcuVerbalizer())

    @Test fun videEtNull() {
        assertEquals("", n.normalize(null))
        assertEquals("", n.normalize(""))
    }

    @Test fun protecteurTelUrl() {
        assertEquals("Appelez le 418-555-1234 svp.", n.normalize("Appelez le 418-555-1234 svp."))
        assertEquals("Voir https://jokobee.com ici.", n.normalize("Voir https://jokobee.com ici."))
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
