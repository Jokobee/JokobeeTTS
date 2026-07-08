package com.jokobee.tts.free

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Port misaki EN — test de FIDÉLITÉ contre le golden misaki (2877 mots fréquents,
 * phonémisés par le vrai misaki). Mesure le % de correspondance mot-à-mot du lexique
 * porté. Le seuil monte au fur et à mesure du port (phase par phase).
 */
class MisakiEnLexiconTest {

    private val lexicon: MisakiEnLexicon by lazy {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        MisakiEnLexicon.fromStreams(
            File(dir, "us_gold.json").inputStream(),
            File(dir, "us_silver.json").inputStream(),
        )
    }

    /** Étape finale de misaki (version != 2.0) : ɾ→T, ʔ→t. */
    private fun finalize(ps: String): String = ps.replace('ɾ', 'T').replace('ʔ', 't')

    @Test fun matchesMisakiGolden() {
        val json = javaClass.getResourceAsStream("/misaki_golden_en.json")!!.bufferedReader().readText()
        val golden = JSONObject(json)

        var total = 0; var match = 0
        val misses = ArrayList<String>()
        for (word in golden.keys()) {
            val expected = golden.getString(word)
            val got = lexicon.phonemize(word).first?.let { finalize(it) }
            total++
            if (got == expected) match++
            else if (misses.size < 30) misses.add("$word: attendu=$expected got=$got")
        }
        val rate = 100.0 * match / total
        println("=== Fidélité lexique misaki : $match/$total (%.1f%%) ===".format(rate))
        misses.forEach { println("  MISS  $it") }

        // Phase 1 : lexique + stemming + stress + special cases, SANS POS spaCy.
        // 99,6% atteint ; le résidu = homographes POS (object/use/progress…) → phase POS.
        assertTrue("taux de correspondance trop bas : %.1f%%".format(rate), rate >= 99.0)
    }
}
