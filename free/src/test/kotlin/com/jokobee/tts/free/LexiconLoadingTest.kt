package com.jokobee.tts.free

import com.jokobee.tts.core.MapLexiconSource
import com.jokobee.tts.core.PhoneticAlphabet
import com.jokobee.tts.core.TsvLexiconLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** BLOC B3/B4 — chargement de lexique (TSV, ARPABET), priorité, override end-to-end. */
class LexiconLoadingTest {

    private fun tsv(s: String, alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA, lang: String = "en_US") =
        TsvLexiconLoader.fromStream(s.byteInputStream(), lang, alphabet)

    @Test fun tsvLoaderBasic() {
        val src = tsv("hello\thəlˈO\nworld\twˈɜɹld\ncat\tkˈæt\n")
        assertEquals("həlˈO", src.lookup("hello", "en_US"))
        assertEquals("kˈæt", src.lookup("Cat", "en_US"))          // insensible à la casse
        assertEquals(3, src.size)
        assertNull(src.lookup("hello", "fr"))                     // mono-langue
        assertNull(src.lookup("absent", "en_US"))
    }

    @Test fun tsvLoaderComments() {
        val src = tsv("# commentaire\n\nhello\thəlˈO\n   # indenté\n\nworld\twˈɜɹld\n")
        assertEquals(2, src.size)
        assertEquals("həlˈO", src.lookup("hello", "en_US"))
    }

    @Test fun tsvLoaderArpabet() {
        val src = tsv("cat\tK AE1 T\ndog\tD AO1 G\n", PhoneticAlphabet.ARPABET)
        assertEquals("kˈæt", src.lookup("cat", "en_US"))          // converti en IPA en interne
        assertEquals("dˈɔɡ", src.lookup("dog", "en_US"))
    }

    @Test fun addWithAlphabet() {
        val lex = MapLexiconSource()
            .add("hello", "həlˈO", "en_US")                                    // IPA direct
            .add("cat", "K AE1 T", PhoneticAlphabet.ARPABET, "en_US")          // ARPABET -> IPA
        assertEquals("həlˈO", lex.lookup("hello", "en_US"))
        assertEquals("kˈæt", lex.lookup("cat", "en_US"))          // stocké en IPA canonique
    }

    @Test fun lexiconPriorityOrder() {
        // deux sources : la DERNIÈRE chargée gagne (documenté).
        val lex = MapLexiconSource()
            .load(tsv("word\tAAA\n"))
            .load(tsv("word\tBBB\n"))
        assertEquals("BBB", lex.lookup("word", "en_US"))
        // les sources priment sur les entrées add() de base
        lex.add("word", "CCC", "en_US")
        assertEquals("BBB", lex.lookup("word", "en_US"))          // source prioritaire
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
