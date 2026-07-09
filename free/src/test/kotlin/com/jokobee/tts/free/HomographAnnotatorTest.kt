package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test

/** Homograph disambiguation */
class HomographAnnotatorTest {

    private data class Case(val sentence: String, val target: String, val expected: String)

    private val cases = listOf(
        Case("Paris est la capitale.", "est", "ɛ"),
        Case("Le soleil se lève à l'est.", "est", "ɛst"),
        Case("Le vent d'est souffle fort.", "est", "ɛst"),
        Case("Tu es en retard.", "es", "ɛ"),
        Case("Il n'en veut plus.", "plus", "ply"),
        Case("Il en veut plus.", "plus", "plys"),
        Case("J'ai deux fils.", "fils", "fis"),
        Case("Les os du squelette.", "os", "o"),
        Case("Un os pour le chien.", "os", "ɔs"),
        Case("Tous les jours.", "tous", "tu"),
        Case("Ils sont tous là.", "tous", "tus"),
        Case("Le couvent est ancien.", "couvent", "kuvɑ̃"),
        Case("Les poules couvent.", "couvent", "kuv"),
        Case("Le président parle.", "président", "pʁezidɑ̃"),
        Case("Ils président la séance.", "président", "pʁezid"),
        Case("Nous portions des caisses.", "portions", "pɔʁtjɔ̃"),
        Case("Des portions généreuses.", "portions", "pɔʁsjɔ̃"),
        Case("Je sens la fumée.", "sens", "sɑ̃"),
        Case("Le sens de la vie.", "sens", "sɑ̃s"),
    )

    @Test fun homographCases() {
        for (c in cases) {
            val got = HomographAnnotator.annotate(c.sentence)
                .firstOrNull { it.token.lowercase() == c.target && it.ipa != null }?.ipa
            assertEquals("« ${c.sentence} » → ${c.target}", c.expected, got)
        }
    }
}
