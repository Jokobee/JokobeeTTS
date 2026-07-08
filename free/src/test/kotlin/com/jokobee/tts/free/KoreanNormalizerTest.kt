package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation coréenne (ko) */
class KoreanNormalizerTest {
    @Test fun koCases() = CaseRunner.run("test_cases_ko.json", KoreanNormalizer(IcuVerbalizer()))
}
