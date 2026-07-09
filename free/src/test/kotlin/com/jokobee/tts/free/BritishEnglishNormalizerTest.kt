package com.jokobee.tts.free

import org.junit.Test

/** British English normalization test cases (en_GB). */
class BritishEnglishNormalizerTest {
    @Test fun enGbCases() = CaseRunner.run("test_cases_en_gb.json", BritishEnglishNormalizer(IcuVerbalizer()))
}
