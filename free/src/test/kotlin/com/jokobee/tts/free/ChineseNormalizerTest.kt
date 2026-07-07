package com.jokobee.tts.free

import org.junit.Test

/**
 * Cas de normalisation chinoise (zh). Verbalisation ICU (rulesets natifs) via icu4j
 * EMBARQUÉ — sortie déterministe et identique sur device (icu4j est une lib Java pure,
 * pas android.icu). Portée : normalisation texte→lecture ; le G2P CJK aval est hors :free.
 */
class ChineseNormalizerTest {
    @Test fun zhCases() = CaseRunner.run("test_cases_zh.json", ChineseNormalizer(IcuVerbalizer()))
}
