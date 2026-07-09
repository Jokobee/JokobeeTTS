package com.jokobee.tts.free

import org.junit.Test

/** Italian normalization test cases (it). */
class ItalianNormalizerTest {
    @Test fun itCases() = CaseRunner.run("test_cases_it.json", ItalianNormalizer(IcuVerbalizer()))
}
