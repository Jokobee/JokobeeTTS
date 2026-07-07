package com.jokobee.tts.free

import org.junit.Test

/**
 * Cas de normalisation japonaise (ja). Verbalisation ICU (rulesets natifs) via icu4j
 * EMBARQUÉ — sortie déterministe et identique sur device (icu4j est une lib Java pure,
 * pas android.icu). Portée : normalisation texte→lecture ; le G2P CJK aval est hors :free.
 */
class JapaneseNormalizerTest {
    @Test fun jaCases() = CaseRunner.run("test_cases_ja.json", JapaneseNormalizer(IcuVerbalizer()))
}
