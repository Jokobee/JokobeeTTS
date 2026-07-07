package com.jokobee.tts.free

import org.junit.Test

/** Bloc 3 — locale fr : les 62 cas de test_cases_fr.json (sortie identique au Python). */
class FrenchNormalizerTest {
    @Test fun frCases() {
        CaseRunner.run("test_cases_fr.json", FrenchNormalizer(IcuVerbalizer()))
    }
}
