package com.jokobee.tts.free

import org.junit.Test

/** Brazilian Portuguese normalization test cases (pt_BR). */
class BrazilianPortugueseNormalizerTest {
    @Test fun ptBrCases() = CaseRunner.run("test_cases_pt_br.json", BrazilianPortugueseNormalizer(IcuVerbalizer()))
}
