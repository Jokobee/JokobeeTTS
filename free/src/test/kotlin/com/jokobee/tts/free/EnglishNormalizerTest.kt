package com.jokobee.tts.free

import org.junit.Test

/** English normalization test cases (en_US). */
class EnglishNormalizerTest {
    @Test fun enCases() = CaseRunner.run("test_cases_en.json", EnglishNormalizer(IcuVerbalizer()))
}
