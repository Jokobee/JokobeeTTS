package com.jokobee.tts.free

import com.jokobee.tts.core.AdapterContext
import com.jokobee.tts.core.AdapterIncompatibleException
import com.jokobee.tts.core.AdapterLoader
import com.jokobee.tts.core.DictAdapter
import com.jokobee.tts.core.G2p
import com.jokobee.tts.core.MapLexiconSource
import com.jokobee.tts.core.NormalizationAdapter
import com.jokobee.tts.core.NormalizationContext
import com.jokobee.tts.core.PhoneticAlphabet
import com.jokobee.tts.core.PhonemeAdapter
import com.jokobee.tts.core.ProRequiredException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private inline fun <reified E : Throwable> expect(block: () -> Unit) {
    val t = try { block(); null } catch (e: Throwable) { e }
    assertTrue("attendu ${E::class.simpleName}, obtenu $t", t is E)
}

private class StubNorm(override val id: String, override val langs: Set<String>, val f: (String) -> String) :
    NormalizationAdapter {
    override fun adapt(text: String, lang: String, context: NormalizationContext): String = f(text)
}

private class StubDict(override val id: String, override val langs: Set<String>, val map: Map<String, String>) :
    DictAdapter {
    override val alphabet: PhoneticAlphabet = PhoneticAlphabet.IPA
    override fun lookup(word: String, lang: String): String? = map[word.lowercase()]
}

private class StubAccent(
    override val id: String,
    override val baseLang: String,
    override val targetLocale: String,
    val f: (String) -> String,
) : PhonemeAdapter {
    override fun adapt(phonemes: String, context: AdapterContext): String = f(phonemes)
}

private class FakeLoader(
    private val norms: Map<String, NormalizationAdapter> = emptyMap(),
    private val dicts: Map<String, DictAdapter> = emptyMap(),
    private val accents: Map<String, PhonemeAdapter> = emptyMap(),
) : AdapterLoader {
    override fun loadNormalization(id: String) = norms.getValue(id)
    override fun loadDictionary(id: String) = dicts.getValue(id)
    override fun loadAccent(id: String) = accents.getValue(id)
}

private val G2P = object : G2p { override fun phonemize(word: String, lang: String) = "G2P" }

class AdapterRegistryTest {

    // ----- Free : tout load() lève ProRequired sans loader -----
    @Test fun proRequiredWithoutLoader() {
        val reg = AdapterRegistry()
        expect<ProRequiredException> { reg.normalization.load("x") }
        expect<ProRequiredException> { reg.dictionary.load("x") }
        expect<ProRequiredException> { reg.accent.load("x") }
    }

    // ----- normalization -----
    @Test fun normNotLoaded_passthrough() {
        assertEquals("abc", AdapterRegistry().normalization.apply("abc", "fr", null))
    }

    @Test fun normLoaded_transformsThenUnloadRestores() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(norms = mapOf("up" to StubNorm("up", setOf("fr")) { it.uppercase() })))
        reg.normalization.load("up")
        assertEquals("ABC", reg.normalization.apply("abc", "fr", null))
        assertEquals("up", reg.normalization.current?.id)
        reg.normalization.unload()
        assertEquals("abc", reg.normalization.apply("abc", "fr", null))
    }

    @Test fun normIncompatibleLang_throws() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(norms = mapOf("q" to StubNorm("q", setOf("fr")) { it })))
        reg.normalization.load("q")
        expect<AdapterIncompatibleException> { reg.normalization.apply("x", "en_US", null) }
    }

    @Test fun normMultiLang_acceptsBothRejectsOther() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(norms = mapOf("m" to StubNorm("m", setOf("fr", "fr_CA")) { it + "!" })))
        reg.normalization.load("m")
        assertEquals("x!", reg.normalization.apply("x", "fr", null))
        assertEquals("x!", reg.normalization.apply("x", "fr_CA", null))
        expect<AdapterIncompatibleException> { reg.normalization.apply("x", "es", null) }
    }

    // ----- dictionary : priorité lexicon > dict > g2p -----
    @Test fun dictPriorityLexiconThenDictThenG2p() {
        val reg = AdapterRegistry()
        reg.installLoader(
            FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("es"), mapOf("hola" to "DICT", "mesa" to "MESA")))),
        )
        reg.dictionary.load("d")
        val lex = MapLexiconSource().apply { add("hola", "LEX", "es") }
        val chain = AccentG2p(reg.accent, LexiconG2p(lex, DictionaryG2p(reg.dictionary, G2P)))
        assertEquals("LEX", chain.phonemize("hola", "es"))   // #1 lexicon gagne
        assertEquals("MESA", chain.phonemize("mesa", "es"))  // #2 dict (absent du lexicon)
        assertEquals("G2P", chain.phonemize("otro", "es"))   // #3 fallback
    }

    @Test fun dictMultipleLoaded_firstWins_andUnloadOne() {
        val reg = AdapterRegistry()
        reg.installLoader(
            FakeLoader(
                dicts = mapOf(
                    "d1" to StubDict("d1", setOf("es"), mapOf("mesa" to "D1")),
                    "d2" to StubDict("d2", setOf("es"), mapOf("mesa" to "D2", "silla" to "D2S")),
                ),
            ),
        )
        reg.dictionary.load("d1"); reg.dictionary.load("d2")
        assertEquals(listOf("d1", "d2"), reg.dictionary.loaded)
        assertEquals("D1", reg.dictionary.lookup("mesa", "es"))   // premier chargé gagne
        assertEquals("D2S", reg.dictionary.lookup("silla", "es")) // trouvé dans le 2e
        reg.dictionary.unload("d1")
        assertEquals("D2", reg.dictionary.lookup("mesa", "es"))   // d1 retiré, d2 reste
        reg.dictionary.unloadAll()
        assertEquals(null, reg.dictionary.lookup("mesa", "es"))
    }

    @Test fun dictIncompatibleLang_throws() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(dicts = mapOf("en" to StubDict("en", setOf("en_US"), mapOf("a" to "b")))))
        reg.dictionary.load("en")
        expect<AdapterIncompatibleException> { reg.dictionary.lookup("a", "fr") }
    }

    // ----- accent : IPA -> IPA après résolution -----
    @Test fun accentNotLoaded_passthrough() {
        assertEquals("G2P", AccentG2p(AdapterRegistry().accent, G2P).phonemize("x", "fr"))
    }

    @Test fun accentLoaded_appliedThenUnload() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(accents = mapOf("q" to StubAccent("q", "fr", "fr_CA") { it + "+A" })))
        reg.accent.load("q")
        val chain = AccentG2p(reg.accent, G2P)
        assertEquals("G2P+A", chain.phonemize("x", "fr"))
        assertEquals("G2P+A", chain.phonemize("x", "fr_CA"))  // famille fr acceptée
        reg.accent.unload()
        assertEquals("G2P", chain.phonemize("x", "fr"))
    }

    @Test fun accentIncompatibleLang_throws() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(accents = mapOf("q" to StubAccent("q", "fr", "fr_CA") { it })))
        reg.accent.load("q")
        expect<AdapterIncompatibleException> { AccentG2p(reg.accent, G2P).phonemize("x", "es") }
    }

    // ----- CharsiuG2P (fr/es/it/pt) : lexicon/dict AVANT le modèle -----
    @Test fun charsiuLexiconDictBeforeModel() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("es"), mapOf("mesa" to "MESA")))))
        reg.dictionary.load("d")
        val calls = ArrayList<String>()
        val spy = object : com.jokobee.tts.core.G2p {
            override fun phonemize(word: String, lang: String): String { calls += word; return "SPY" }
        }
        val chain = AccentG2p(reg.accent, LexiconG2p(MapLexiconSource(), DictionaryG2p(reg.dictionary, spy)))
        assertEquals("MESA", chain.phonemize("mesa", "es"))
        assertEquals(0, calls.size)                    // mot du dico → modèle NON appelé
        chain.phonemize("otro", "es")
        assertEquals(listOf("otro"), calls)            // mot absent → modèle appelé
    }

    // ----- chemin misaki EN : dict/accent dans la chaîne -----
    private fun misakiLex(): MisakiEnLexicon {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        return MisakiEnLexicon.fromStreams(
            File(dir, "us_gold.json").inputStream(), File(dir, "us_silver.json").inputStream(),
        )
    }

    @Test fun dictResolvesInMisakiChain() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("en_US"), mapOf("hello" to "zzz")))))
        reg.dictionary.load("d")
        val out = MisakiEnG2p(misakiLex(), dictionary = reg.dictionary).phonemize("hello world")
        assertTrue("dict doit résoudre hello", out.startsWith("zzz"))
    }

    @Test fun lexiconBeforeDictInMisaki() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("en_US"), mapOf("hello" to "zzz")))))
        reg.dictionary.load("d")
        val custom = MapLexiconSource(mapOf("hello" to "LEX"), "en_US")
        val out = MisakiEnG2p(misakiLex(), customLexicon = custom, dictionary = reg.dictionary).phonemize("hello world")
        assertTrue("tts.lexicon prime sur tts.dictionary", out.startsWith("LEX"))
    }

    @Test fun accentAppliedInMisaki() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(accents = mapOf("a" to StubAccent("a", "en", "en_US") { "z$it" })))
        reg.accent.load("a")
        val withAccent = MisakiEnG2p(misakiLex(), accent = reg.accent).phonemize("hello world")
        val plain = MisakiEnG2p(misakiLex()).phonemize("hello world")
        assertTrue("accent préfixe chaque mot", withAccent.startsWith("z"))
        assertTrue("sans accent : inchangé", !plain.startsWith("z"))
    }

    @Test fun multiWordDictInMisaki() {
        val reg = AdapterRegistry()
        reg.installLoader(
            FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("en_US"), mapOf("habeas corpus" to "zzz")))),
        )
        reg.dictionary.load("d")
        val out = MisakiEnG2p(misakiLex(), dictionary = reg.dictionary).phonemize("the habeas corpus writ")
        assertTrue("séquence multi-mots résolue", out.contains("zzz"))
        assertEquals("greedy : une seule fusion", 1, Regex("zzz").findAll(out).count())
        val writ = MisakiEnG2p(misakiLex()).phonemize("writ").trim()
        assertTrue("mot suivant résolu normalement", out.trim().endsWith(writ))
    }

    @Test fun neighborsUnaffectedByOverride() {
        val reg = AdapterRegistry()
        reg.installLoader(FakeLoader(dicts = mapOf("d" to StubDict("d", setOf("en_US"), mapOf("cat" to "zzz")))))
        reg.dictionary.load("d")
        val overridden = MisakiEnG2p(misakiLex(), dictionary = reg.dictionary).phonemize("the cat")
        val plain = MisakiEnG2p(misakiLex()).phonemize("the cat")
        val theGold = plain.substringBefore(' ')
        assertTrue("le voisin 'the' garde sa résolution contextuelle", overridden.startsWith(theGold))
        assertTrue(overridden.endsWith("zzz"))
    }

    // ----- indépendance des trois systèmes -----
    @Test fun allThreeIndependent() {
        val reg = AdapterRegistry()
        reg.installLoader(
            FakeLoader(
                norms = mapOf("n" to StubNorm("n", setOf("fr")) { it }),
                dicts = mapOf("d" to StubDict("d", setOf("fr"), mapOf("a" to "b"))),
                accents = mapOf("a" to StubAccent("a", "fr", "fr_CA") { it }),
            ),
        )
        reg.normalization.load("n"); reg.dictionary.load("d"); reg.accent.load("a")
        reg.normalization.unload()
        assertEquals(null, reg.normalization.current)
        assertEquals(listOf("d"), reg.dictionary.loaded)   // dict intact
        assertEquals("a", reg.accent.current?.id)          // accent intact
    }
}
