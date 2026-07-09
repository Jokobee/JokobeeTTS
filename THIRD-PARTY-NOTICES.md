# THIRD-PARTY-NOTICES — JokobeeTTS

JokobeeTTS is a **Jokobee** product (jokobee.com, contact@jokobee.com). It **packages and
extends** the third-party components below. JokobeeTTS is neither affiliated with nor endorsed
by the source projects (Apache-2.0 §6 — the source projects' trademarks are not used as the
product's trademark).

## Components in the distributed path (shipped artifact)

| Component | Author/Project | License | Role |
|---|---|---|---|
| **Kokoro-82M** | hexgrad | **Apache-2.0** | synthesis model. JokobeeTTS is "**based on Kokoro-82M**". |
| **ONNX Runtime** | Microsoft | **MIT** | inference engine |
| **CharsiuG2P** | CharsiuNLP | **MIT** | phonemization (grapheme → phoneme) |
| **misaki** | hexgrad (Kokoro) | **MIT** | English phonemization (grapheme → phoneme) |
| **ICU4J** (`com.ibm.icu:icu4j`) | Unicode, Inc. | **Unicode/ICU License** (permissive) | number verbalization (RBNF spellout) |
| JokobeeTTS code | Jokobee | Apache-2.0 (Free tier) | normalization, voice registry, orchestration |

**No GPL/LGPL dependency in the distributed artifact.**

## Apache-2.0 obligations fulfilled
1. **Attribution kept**: this file + the "based on Kokoro-82M" mention in the README.
   hexgrad copyright preserved.
2. **No source trademark used** as a product trademark: the product is called **JokobeeTTS**,
   not "Kokoro" (avoids any appearance of affiliation/endorsement).
3. **Changes documented**: JokobeeTTS adds a multilingual normalization layer, a voice
   registry, orchestration and graceful error handling. The **model weights are not modified**
   (packaging/orchestration only).
