package com.jokobee.tts.free

import org.junit.Test

/** Bloc 3 */
class FrenchNormalizerTest {
    @Test fun frCases() {
        CaseRunner.run("test_cases_fr.json", FrenchNormalizer(IcuVerbalizer()))
    }
}
