package com.jokobee.tts.free

import com.jokobee.tts.core.MapLexiconSource
import com.jokobee.tts.core.PhoneticAlphabet
import com.jokobee.tts.core.TsvLexiconLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** BLOCK B3/B4 */
class LexiconLoadingTest {

    private fun tsv(s: String, alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA, lang: String = "en_US") =
        TsvLexiconLoader.fromStream(s.byteInputStream(), lang, alphabet)

    @Test fun tsvLoaderBasic() {
        val src = tsv("hello\thəlˈO\nworld\twˈɜɹld\ncat\tkˈæt\n")
        assertEquals("həlˈO", src.lookup("hello", "en_US"))
        assertEquals("kˈæt", src.lookup("Cat", "en_US"))          // case-insensitive
        assertEquals(3, src.size)
        assertNull(src.lookup("hello", "fr"))                     // single language only
        assertNull(src.lookup("absent", "en_US"))
    }

    @Test fun tsvLoaderComments() {
        val src = tsv("# commentaire\n\nhello\thəlˈO\n   # indenté\n\nworld\twˈɜɹld\n")
        assertEquals(2, src.size)
        assertEquals("həlˈO", src.lookup("hello", "en_US"))
    }

    @Test fun tsvLoaderArpabet() {
        val src = tsv("cat\tK AE1 T\ndog\tD AO1 G\n", PhoneticAlphabet.ARPABET)
        assertEquals("kˈæt", src.lookup("cat", "en_US"))          // converted to IPA internally
        assertEquals("dˈɔɡ", src.lookup("dog", "en_US"))
    }

    @Test fun addWithAlphabet() {
        val lex = MapLexiconSource()
            .add("hello", "həlˈO", "en_US")                                    // direct IPA
            .add("cat", "K AE1 T", PhoneticAlphabet.ARPABET, "en_US")          // ARPABET -> IPA
        assertEquals("həlˈO", lex.lookup("hello", "en_US"))
        assertEquals("kˈæt", lex.lookup("cat", "en_US"))          // stored in canonical IPA
    }

    @Test fun lexiconPriorityOrder() {
        val lex = MapLexiconSource()
            .load(tsv("word\tAAA\n"))
            .load(tsv("word\tBBB\n"))
        assertEquals("BBB", lex.lookup("word", "en_US"))
        // sources take priority over base add() entries
        lex.add("word", "CCC", "en_US")
        assertEquals("BBB", lex.lookup("word", "en_US"))          // priority source
        assertEquals("CCC", lex.lookup("word2", "en_US") ?: "CCC")
    }

    @Test fun lexiconOverridesMisaki() {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        val misaki = MisakiEnLexicon.fromStreams(
            File(dir, "us_gold.json").inputStream(), File(dir, "us_silver.json").inputStream(),
        )
        val lex = MapLexiconSource().add("hello", "ZZZ", "en_US")
        val out = MisakiEnG2p(misaki, customLexicon = lex).phonemize("hello world")
        assertTrue("le lexique custom prime sur misaki", out.startsWith("ZZZ"))
    }
}
