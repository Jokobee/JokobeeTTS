package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation chinoise (zh) */
class ChineseNormalizerTest {
    @Test fun zhCases() = CaseRunner.run("test_cases_zh.json", ChineseNormalizer(IcuVerbalizer()))
}
