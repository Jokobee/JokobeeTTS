# JokobeeTTS — Integration guide (developers)

Text → 24 kHz audio on-device. This guide covers the **Free** tier (`:core` + `:free`).
Everything (model, voices, G2P) ships **inside the AAR** — no download, no network, ever.

- [Installation](#installation)
- [Quick start](#quick-start)
- [Bundled model & voices](#bundled-model--voices)
- [Voices](#voices)
- [Languages](#languages)
- [Extension hooks](#extension-hooks)
- [Advanced: explicit model/voice loading](#advanced-explicit-modelvoice-loading)
- [Performance & threading](#performance--threading)

---

## Installation

> Maven Central publication is planned for 1.0. In the meantime, build locally
> (`./gradlew :free:assembleRelease`).

```kotlin
// build.gradle.kts (once published)
dependencies {
    implementation("com.jokobee:jokobeetts-free:1.0.0")   // depends on :core
}
```

The AAR embeds **everything**: G2P, the Kokoro synthesis model, and 38 official voices.
No further setup.

---

## Quick start

```kotlin
import com.jokobee.tts.free.Tts

val tts = Tts.create(context)                                   // zero-config, ~110 MB AAR does the rest
val wav: ByteArray = tts.synthesizeToWav("Bonjour le monde", "fr")  // 24 kHz WAV, default fr voice
// -> write to a file, or play via AudioTrack / MediaPlayer.
```

`Tts.create(context)` builds the whole pipeline **once** (expensive: loads the models) — do
it **once**, off the UI thread, and reuse the instance.

Output forms:

```kotlin
val samples: FloatArray = tts.synthesize(text, lang)               // f32 [-1,1] 24 kHz, default voice
val wavBytes: ByteArray = tts.synthesizeToWav(text, lang)           // WAV PCM 16-bit, default voice
```

`lang` accepts the short alias `"en"` (→ `en_US`) in addition to full locale codes
(`fr`, `en_US`, `en_GB`, `es`, `it`, `pt_BR`).

A **lead (200 ms) / tail (100 ms) silence** is added by default (avoids the first word being
clipped by the player's init latency). Adjust via `leadMs` / `trailMs` (0 = raw).

---

## Bundled model & voices

The Kokoro synthesis model (`model_quantized.onnx`, ~88 MB) and 38 official voice files
(`voices/*.bin`, ~20 MB) are packaged as AAR assets and loaded directly from memory —
`Tts.create(context)` never touches the filesystem or the network.

One default voice is picked automatically per language (see [Voices](#voices)). This is
why the total AAR is **~110 MB**: that's the cost of "it just works" with zero setup.

---

## Voices

```kotlin
val tts = Tts.create(context)

tts.voices?.list()             // all 38 bundled official voices
tts.voices?.get("af_heart")    // a specific one by id
```

`synthesize`/`synthesizeToWav` use a sensible default voice per language when none is
passed. To pick a specific one instead:

```kotlin
val voice = tts.voices?.get("bf_emma")
val wav = tts.synthesizeToWav("Good afternoon", "en_GB", voice)
```

**Custom voice import** and **blending** ("create your voice") are **Pro** features — see
[jokobee.com](https://jokobee.com).

---

## Languages

6 languages normalized **upstream** (numbers, dates, currencies, acronyms, units…) — feed raw
text, no need to pre-process.

| Locale | Status |
|---|---|
| `fr`, `fr_CA`, `en_US`, `en_GB`, `es`, `it`, `pt_BR` (`en` accepted as an alias of `en_US`) | ✅ validated |

---

## Extension hooks

Two reserved insertion points (pass-through by default, plug in without refactoring):

```kotlin
// Custom lexicon (brands, corrections) — consulted BEFORE G2P.
class BrandLexicon : com.jokobee.tts.core.LexiconSource {
    override fun lookup(word: String, lang: String): String? =
        if (word.equals("jokobee", true)) "dʒoʊkoʊbi" else null
}
tts.lexicon.load(BrandLexicon())             // registered in the pipeline

// Style/voice resolution — the pipeline ALWAYS goes through the StyleResolver.
val tts2 = Tts.create(context,
    styleResolver = com.jokobee.tts.core.DefaultStyleResolver())
```

---

## Advanced: explicit model/voice loading

For custom setups (e.g. the Pro tier swapping in an updated model), the lower-level API
still takes an explicit ONNX environment and model path/bytes instead of the bundled asset:

```kotlin
val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
val tts = Tts.create(context, env, modelPath = "/path/to/model.onnx")

val voiceBytes = context.assets.open("voices/ff_siwis.bin").use { it.readBytes() }
val voice = com.jokobee.tts.free.Voice.of("ff_siwis", "fr", voiceBytes)
val wav = tts.synthesizeToWav("Bonjour le monde", "fr", voice)
```

---

## Performance & threading

- Building the pipeline (`Tts.create(...)`) is **expensive** (model loading):
  do it **once**, off the UI thread.
- Synthesis is CPU-bound: run it on a worker. A G2P cache is built in.
- The pipeline and ONNX sessions are reusable; release resources at end of life.
- ABI: Free = `arm64-v8a`. `x86_64` support (emulator/Chromebook) is a Pro feature.
- The AAR is **~110 MB** (bundled model + voices). If APK size matters more than zero-setup
  for your use case, that's noted as a possible future option — not available in v1.0.0.
