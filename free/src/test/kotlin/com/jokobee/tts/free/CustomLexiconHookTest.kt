package com.jokobee.tts.free

import com.jokobee.tts.core.EmptyLexiconSource
import com.jokobee.tts.core.LexiconSource
import com.jokobee.tts.core.MapLexiconSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * BLOC 3 — crochet lexique custom (couche #1). Vérifie que le pipeline appelle la couche
 * custom AVANT misaki, que le stub vide est inerte (aucun effet), et qu'une entrée custom
 * a bien la priorité (override) sur misaki.
 */
class CustomLexiconHookTest {

    private fun lexicon(): MisakiEnLexicon {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        return MisakiEnLexicon.fromStreams(
            File(dir, "us_gold.json").inputStream(),
            File(dir, "us_silver.json").inputStream(),
        )
    }

    @Test fun emptyStubDoesNotChangeResult() {
        val lex = lexicon()
        val withStub = MisakiEnG2p(lex, customLexicon = EmptyLexiconSource).phonemize("hello world")
        val without = MisakiEnG2p(lex).phonemize("hello world")   // défaut = EmptyLexiconSource
        assertEquals(without, withStub)
    }

    @Test fun customLayerCalledBeforeMisakiForEveryWord() {
        val seen = mutableListOf<String>()
        val spy = object : LexiconSource {
            override fun lookup(word: String, lang: String): String? { seen += word.lowercase(); return null }
        }
        val out = MisakiEnG2p(lexicon(), customLexicon = spy).phonemize("hello world")
        // la couche #1 a été consultée pour chaque mot (avant misaki), mais renvoie null -> misaki
        assertTrue("hello" in seen && "world" in seen)
        assertTrue(out.isNotBlank())
    }

    @Test fun customEntryOverridesMisaki() {
        val custom = MapLexiconSource(mapOf("hello" to "ZZZ"), "en_US")
        val out = MisakiEnG2p(lexicon(), customLexicon = custom).phonemize("hello world")
        // « hello » vient du lexique custom (couche #1), pas de misaki
        assertTrue("l'entrée custom doit primer sur misaki", out.startsWith("ZZZ"))
    }
}
