package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bloc 1 : cardinaux/ordinaux ICU sur les 6 locales latines (via icu4j). */
class VerbalizerTest {

    private val v: Verbalizer = IcuVerbalizer()   // icu4j embarqué (= android.icu au device)

    @Test fun fr() {
        assertEquals("quatorze", v.cardinal(14, "fr_CA"))
        assertEquals("quatre-vingts", v.cardinal(80, "fr_CA"))
        assertEquals("mille deux cent trente-quatre", v.cardinal(1234, "fr_CA"))
        assertEquals("première", v.ordinal(1, "fr_CA", true))
        assertEquals("deuxième", v.ordinal(2, "fr_CA"))
    }

    @Test fun enUs() {
        assertEquals("two thousand five", v.cardinal(2005, "en_US"))   // PAS de "and"
        assertEquals("one thousand two hundred thirty-four", v.cardinal(1234, "en_US"))
        assertEquals("third", v.ordinal(3, "en_US"))
        assertEquals("twenty-first", v.ordinal(21, "en_US"))
    }

    @Test fun enGb() {
        assertEquals("two thousand five", v.cardinal(2005, "en_GB"))
    }

    @Test fun es() {
        assertEquals("quinientos cincuenta mil", v.cardinal(550000, "es"))
    }

    @Test fun it() {
        val c = v.cardinal(42, "it")
        assertEquals("quarantadue", c)
        assertFalse(c.contains('­'))    // soft hyphens purgés
    }

    @Test fun ptBr() {
        assertEquals("cinquenta", v.cardinal(50, "pt_BR"))
        assertTrue(v.cardinal(1234, "pt_BR").contains(" e "))   // pt utilise « e »
    }
}
