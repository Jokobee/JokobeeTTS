# JokobeeTTS — Integration guide (developers)

Text → 24 kHz audio on-device. This guide covers the **Free** tier (`:core` + `:free`).

- [Installation](#installation)
- [Quick start](#quick-start)
- [The Kokoro model](#the-kokoro-model)
- [Voices](#voices)
- [Languages](#languages)
- [Extension hooks](#extension-hooks)
- [Model download (ModelManager)](#model-download-modelmanager)
- [Performance & threading](#performance--threading)

---

## Installation

> Maven Central publication is planned for 1.0. In the meantime, build locally
> (`./gradlew :free:assembleRelease`).

```kotlin
// build.gradle.kts (once published)
dependencies {
    implementation("com.jokobee:jokobeetts:1.0.0")   // :free (depends on :core)
}
```

The AAR embeds the G2P (offline). The **Kokoro synthesis model** is not embedded —
see [The Kokoro model](#the-kokoro-model).

---

## Quick start

Assemble the pipeline **once** (expensive: loads the models), then reuse it.

```kotlin
import ai.onnxruntime.OrtEnvironment
import com.jokobee.tts.free.Tts
import com.jokobee.tts.free.Voice

val env = OrtEnvironment.getEnvironment()

// Ready-to-use pipeline (normalization + embedded G2P + synthesis).
// `modelPath` = local Kokoro model file (see "The Kokoro model").
val tts = Tts.create(context, env, modelPath)

// Load a voice and synthesize.
val voice = Voice.of("ff_siwis", "fr", voiceBytes)
val wav: ByteArray = tts.synthesizeToWav("Bonjour le monde", "fr", voice)   // 24 kHz WAV
// -> write to a file, or play via AudioTrack / MediaPlayer.
```

Output forms:

```kotlin
val samples: FloatArray = tts.synthesize(text, lang, voice, speed = 1.0f)   // f32 [-1,1] 24 kHz
val wavBytes: ByteArray = tts.synthesizeToWav(text, lang, voice)            // WAV PCM 16-bit
```

A **lead (200 ms) / tail (100 ms) silence** is added by default (avoids the first word being
clipped by the player's init latency). Adjust via `leadMs` / `trailMs` (0 = raw).

---

## The Kokoro model

The synthesis model (~88 MB) is **downloaded on first launch** (too large for the AAR /
Maven Central), then cached.

- **v1.0**: a `:core` downloader (pinned URL + SHA256) fetches it into
  `context.getExternalFilesDir("kokoro")` — *(in progress, see Roadmap)*.
- **Today**: pass its path to `Tts.create(context, env, modelPath)`.
  You can place it yourself (e.g. under `getExternalFilesDir("kokoro")`).

The G2P follows the same cache principle: a model **dropped into the cache** automatically
takes precedence over the embedded asset, with no code change.

---

## Voices

```kotlin
val voice = Voice.of("af_heart", "en_US", bytes)     // from .bin bytes

val catalog = VoiceCatalog()                         // read-only catalog (Free)
catalog.get("af_heart"); catalog.list()
```

**Custom voice import** and **blending** ("create your voice") are **Pro** features
(`VoiceRegistry`). In Free, official voices load from your assets.

---

## Languages

6 languages normalized **upstream** (numbers, dates, currencies, acronyms, units…) — feed raw
text, no need to pre-process.

| Locale | Status |
|---|---|
| `fr`, `fr_CA`, `en_US`, `en_GB`, `es`, `it`, `pt_BR` | ✅ validated |

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
val tts2 = Tts.create(context, env, modelPath,
    styleResolver = com.jokobee.tts.core.DefaultStyleResolver())
```

---

## Model download (ModelManager)

`ModelManager` (`:core`) resolves the ONNX model and voices to local files.
Priority: **cache** (already downloaded and verified) → embedded **assets** → **download**
(Cloudflare). Resumable download (`.part` + `Range`), progress, SHA-256 verification.

```kotlin
val manifest = com.jokobee.tts.core.ModelManifest.fromJson(
    context.assets.open("model-manifest.json").bufferedReader().readText(),
)
val mgr = com.jokobee.tts.core.ModelManager(
    cacheDir = java.io.File(context.filesDir, "kokoro"),
    assets = com.jokobee.tts.core.AssetProvider { name ->
        runCatching { context.assets.open(name) }.getOrNull()   // any embedded voices/model
    },
)
val files = mgr.ensureAll(manifest) { name, done, total ->
    // update progress UI (total = -1 if unknown)
}
val tts = Tts.create(context, env, modelPath = files.getValue("kokoro.onnx").absolutePath)
```

The manifest (`docs/model-manifest.template.json`) lists each artifact
(`name`, `url`, `sha256`, `size`). A `sha256` of `"TODO"` or empty **skips** verification
(artifact not uploaded yet). The `Authorizer` hook (stub `{ true }`) lets you gate the
download on a license (Pro) later.

---

## Performance & threading

- Building the pipeline (`Tts.create(...)`) is **expensive** (model loading):
  do it **once**, off the UI thread.
- Synthesis is CPU-bound: run it on a worker. A G2P cache is built in.
- The pipeline and ONNX sessions are reusable; release resources at end of life.
- ABI: Free = `arm64-v8a`. `x86_64` support (emulator/Chromebook) is a Pro feature.
