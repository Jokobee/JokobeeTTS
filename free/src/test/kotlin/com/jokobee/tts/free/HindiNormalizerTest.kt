package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation hindi (hi) */
class HindiNormalizerTest {
    @Test fun hiCases() = CaseRunner.run("test_cases_hi.json", HindiNormalizer(IcuVerbalizer()))
}
