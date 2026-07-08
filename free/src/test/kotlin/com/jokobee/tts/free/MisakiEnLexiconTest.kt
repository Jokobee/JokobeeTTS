package com.jokobee.tts.free

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Port */
class MisakiEnLexiconTest {

    private val lexicon: MisakiEnLexicon by lazy {
        val dir = File(System.getProperty("user.dir"), "src/main/assets/misaki")
        MisakiEnLexicon.fromStreams(
            File(dir, "us_gold.json").inputStream(),
            File(dir, "us_silver.json").inputStream(),
        )
    }

    /** Étape finale de */
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

        assertTrue("taux de correspondance trop bas : %.1f%%".format(rate), rate >= 99.0)
    }
}
