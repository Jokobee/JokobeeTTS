package com.jokobee.tts.free

import com.jokobee.tts.core.ArpabetToIpa
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.Normalizer

/** BLOC B1 */
class ArpabetToIpaTest {

    private val vocab: Set<Char> by lazy {
        val f = File(System.getProperty("user.dir"), "src/main/assets/kokoro/vocab.json")
        JSONObject(f.readText()).keys().asSequence().filter { it.length == 1 }.map { it[0] }.toSet()
    }
    private fun i(a: String) = ArpabetToIpa.toIpa(a)

    @Test fun consonantsMapCorrectly() {
        assertEquals("b", i("B")); assertEquals("ʧ", i("CH")); assertEquals("ð", i("DH"))
        assertEquals("ʤ", i("JH")); assertEquals("ŋ", i("NG")); assertEquals("θ", i("TH"))
        assertEquals("ʃ", i("SH")); assertEquals("ʒ", i("ZH")); assertEquals("j", i("Y"))
    }

    @Test fun vowelsWithThreeStressLevels() {
        assertEquals("ˈi", i("IY1")); assertEquals("ˌi", i("IY2")); assertEquals("i", i("IY0"))
        assertEquals("ˈoʊ", i("OW1")); assertEquals("æ", i("AE0")); assertEquals("ˈaɪ", i("AY1"))
    }

    @Test fun arpabetSchwaVsWedge() {
        assertEquals("ə", i("AH0"))
        assertEquals("ˈʌ", i("AH1"))
        assertEquals("ˌʌ", i("AH2"))
    }

    @Test fun arpabetGandR() {
        assertEquals("ɡ", i("G"))            // U+0261, PAS g ASCII
        assertEquals('ɡ', i("G")[0]); assertEquals(0x0261, i("G")[0].code)
        assertEquals("ɹ", i("R"))            // U+0279, PAS r ASCII
        assertEquals(0x0279, i("R")[0].code)
    }

    @Test fun erUsesKokoroForm() {
        assertEquals("əɹ", i("ER0"))         // pas ɚ
        assertEquals("ˈɜɹ", i("ER1"))        // pas ɝ (absent du vocab)
    }

    @Test fun arpabetJokobee() {
        val ipa = i("JH OW1 K AH0 B IY1")
        assertEquals("ʤˈoʊkəbˈi", ipa)
        val oov = Normalizer.normalize(ipa, Normalizer.Form.NFD).filter { !it.isWhitespace() && it !in vocab }
        assertTrue("0 phonème hors-vocab Kokoro attendu, trouvé: $oov", oov.isEmpty())
    }

    @Test fun unknownTokenIgnoredNoCrash() {
        val warns = mutableListOf<String>()
        assertEquals("kæt", ArpabetToIpa.toIpa("K AE0 XX T") { warns += it })
        assertEquals(listOf("XX"), warns)
    }
}
