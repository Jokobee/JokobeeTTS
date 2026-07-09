package com.jokobee.tts.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Port of tests/test_text_splitter.py (15 checks). */
class TextSplitterTest {

    private fun words(s: String) = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    @Test fun courtInchange() {
        val s = TextSplitter(200)
        val short = "Bonjour, comment allez-vous ?"
        assertEquals(listOf(short), s.split(short))
    }

    @Test fun videEtNull() {
        val s = TextSplitter(200)
        assertEquals(emptyList<String>(), s.split(""))
        assertEquals(emptyList<String>(), s.split(null))
        assertEquals(emptyList<String>(), s.split("   "))
    }

    @Test fun troisPhrases() {
        val sp = TextSplitter(40)
        val txt = "Première phrase courte. Deuxième phrase ici. Troisième et dernière phrase."
        val segs = sp.split(txt)
        assertEquals(3, segs.size)
        assertTrue(segs.all { it.endsWith(".") || it.endsWith("!") || it.endsWith("?") })
    }

    @Test fun cascadeVirgule() {
        val v = TextSplitter(30)
        val virg = "un, deux, trois, quatre, cinq, six, sept, huit, neuf, dix"
        val segs = v.split(virg)
        assertTrue(segs.all { it.length <= 30 })
        assertTrue(segs.size > 1)
    }

    @Test fun sansPonctuationEspace() {
        val nop = TextSplitter(25)
        val longue = "alpha bravo charlie delta echo foxtrot golf hotel india juliett kilo"
        val segs = nop.split(longue)
        assertTrue(segs.all { it.length <= 25 })
        assertEquals(words(longue), words(segs.joinToString(" ")))   // no word cut off
    }

    @Test fun cjk() {
        val cjk = TextSplitter(20)
        val t = "今日は晴れです。明日は雨かもしれません。明後日は曇りです。"
        val segs = cjk.split(t)
        assertEquals(3, segs.size)
        assertTrue(segs.all { it.endsWith("。") })
    }

    @Test fun realisteInvariants() {
        val real = "Il fait moins vingt-cinq degrés Celsius ce matin à Québec, avec un " +
            "refroidissement éolien important. Habillez-vous chaudement avant de sortir. " +
            "Le trajet en autobus prend environ quarante minutes."
        val r = TextSplitter(80)
        val rs = r.split(real)
        assertTrue(rs.all { it.length <= 80 })
        assertEquals(words(real), words(rs.joinToString(" ")))
        assertTrue(rs.none { it.isBlank() })
    }
}
