package com.jokobee.tts.free

import org.junit.Assert.assertEquals
import org.junit.Test

/** BLOC 2 */
class EnglishNormalizationTest {

    private val norm = EnglishNormalizer(IcuVerbalizer())
    private fun n(s: String) = norm.normalize(s)

    @Test fun acronymsSpelledOut() {
        assertEquals("I watch T V every day", n("I watch TV every day"))
        assertEquals("I live in the U S A and the U K", n("I live in the USA and the UK"))
        assertEquals("NASA and the F B I", n("NASA and the FBI"))   // NASA gardé (mot), FBI épelé
    }

    @Test fun titlesAndAbbreviations() {
        assertEquals("Doctor Smith lives on Main Street", n("Dr. Smith lives on Main St."))
        assertEquals("Meet me at Saint Peter", n("Meet me at St. Peter"))   // St.+majuscule -> Saint
    }

    @Test fun contractionsKeptForMisaki() {
        assertEquals("I don't know", n("I don't know"))
    }

    @Test fun numbersDatesCurrency() {
        assertEquals("It costs five dollars", n("It costs \$5"))
        assertEquals("March third twenty twenty-four", n("March 3rd 2024"))
    }
}
