package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation japonaise (ja) */
class JapaneseNormalizerTest {
    @Test fun jaCases() = CaseRunner.run("test_cases_ja.json", JapaneseNormalizer(IcuVerbalizer()))
}
