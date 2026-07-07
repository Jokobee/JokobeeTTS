package com.jokobee.tts.free

import org.junit.Test

/** Cas de normalisation portugaise brésilienne (pt_BR). */
class BrazilianPortugueseNormalizerTest {
    @Test fun ptBrCases() = CaseRunner.run("test_cases_pt_br.json", BrazilianPortugueseNormalizer(IcuVerbalizer()))
}
