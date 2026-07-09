package com.jokobee.tts.free

import org.junit.Test

/** Spanish normalization cases (es). */
class SpanishNormalizerTest {
    @Test fun esCases() = CaseRunner.run("test_cases_es.json", SpanishNormalizer(IcuVerbalizer()))
}
