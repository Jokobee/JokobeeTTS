package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation anglaise (en_US). */
class EnglishNormalizerTest {
    @Test fun enCases() = CaseRunner.run("test_cases_en.json", EnglishNormalizer(IcuVerbalizer()))
}
