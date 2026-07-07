package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation anglaise britannique (en_GB). */
class BritishEnglishNormalizerTest {
    @Test fun enGbCases() = CaseRunner.run("test_cases_en_gb.json", BritishEnglishNormalizer(IcuVerbalizer()))
}
