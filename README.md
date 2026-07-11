# JokobeeTTS

[![CI](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml/badge.svg)](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.jokobee/jokobeetts.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.jokobee/jokobeetts)
[![JitPack](https://jitpack.io/v/Jokobee/JokobeeTTS.svg)](https://jitpack.io/#Jokobee/JokobeeTTS)

**On-device** text-to-speech for Android, based on **Kokoro-82M** (Apache-2.0).
Text → 24 kHz audio, 100% local. The Kokoro model and 38 official voices are **bundled in
the AAR** — zero download, zero network, zero setup.
**Open Core** model: public **Free** tier (this repo) + commercial **Pro** tier.

> **Version 1.0.0.** Complete TTS engine, validated end-to-end on-device (Pixel 7 Pro),
> shipped as a single all-in-one AAR — see [CHANGELOG.md](CHANGELOG.md).

## Quickstart

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.jokobee:jokobeetts:1.0.0")
}
```

> Also published under the aliases `com.jokobee:tts-ai-android:1.0.0` and
> `com.jokobee:tts-android-ai:1.0.0` (same artifact, easier to find by generic search) —
> pick whichever coordinate you prefer, they resolve to the same engine.

Alternative — via [JitPack](https://jitpack.io/#Jokobee/JokobeeTTS):

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.Jokobee.JokobeeTTS:jokobeetts:v1.0.1")
}
```

```kotlin
// 3 lines — it speaks
val tts = Tts.create(context)
val audio = tts.synthesize("Hello world", lang = "en")
// done. no download, no setup, no API key.
```

## What ships (v1.0.0)

- **Normalization** — 6 languages (fr, en_US, en_GB, es, it, pt_BR):
  numbers, dates, times, currencies, ordinals, fractions, ranges, roman numerals, units,
  acronyms, abbreviations and symbols. Feed raw text; normalization handles the rest.
- **G2P (grapheme → phoneme)** — **embedded**, 100% offline, for the supported languages.
- **Kokoro synthesis** — ONNX engine, **model bundled in the AAR** (no download), WAV PCM
  16-bit 24 kHz export, `Tts` facade (text → audio), configurable lead/tail silence.
- **38 official voices — bundled**, one sensible default per supported locale, used
  automatically by the zero-config `Tts.create(context)` API.
- **Validated on-device** (Pixel 7 Pro, arm64): text → audible WAV.
- **Extension hooks** (public API): `LexiconSource` (priority custom lexicon),
  `StyleResolver` (style/intonation) — reserved insertion points, pass-through by default.

## Modules & tiers

| Tier | Contents | Distribution |
|---|---|---|
| **`:core`** | contracts (`G2p`, `LexiconSource`, `StyleResolver`, `LanguageDetector`, `StreamingEngine`), `TextSplitter`, `AudioStitcher`, exceptions | Maven Central — `com.jokobee:jokobeetts-core` |
| **`:free`** | 6-language normalization, embedded G2P, Kokoro synthesis with the model + 38 official voices **bundled**, **multi-sentence stitching**, `Tts` API | Maven Central — `com.jokobee:jokobeetts` (aliases: `tts-ai-android`, `tts-android-ai`) |
| **`:pro`** | see below | [**jokobee.com**](https://jokobee.com) (commercial license) — private repo |

**Free = 100% free, no language behind a paywall.** The Free/Pro split is about **features**
(power / real-time), not languages.

### What Pro adds

Drop-in on top of Free — same `Tts` API, `JokobeeTtsPro.enable(...)` unlocks the rest:

- **Real-time streaming** — sentence-by-sentence audio delivery, minimal perceived latency.
- **`lang="auto"`** — automatic language detection (fr, en_US, es, it, pt_BR).
- **Voice import & blending** — bring your own voice, mix existing ones.
- **Adapters** — normalization / dictionary / accent, including encrypted distribution.
- **Vulkan GPU acceleration** and **x86_64** (emulator/Chromebook) support.
- **SSML** and **word-level timestamps**.

Hit a `ProRequiredException` in Free? → [**jokobee.com**](https://jokobee.com) for the commercial license.

## Target

- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10
- **Permissive** dependencies (MIT / Apache-2.0 / Unicode), **zero GPL/LGPL** —
  see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Embedded assets

Everything ships **inside the AAR** (Git LFS) — no download, no first-launch setup, no
network call, ever:

- **G2P**: embedded models + lexicons (CharsiuG2P, misaki EN).
- **Kokoro synthesis model** (`model_quantized.onnx`, ~88 MB).
- **38 official voices** (`voices/*.bin`) covering the 6 supported languages, with one
  default voice per locale used automatically when none is passed explicitly.

Total AAR size: **~97 MB**. That's the trade-off for "it just works" out of the box — a
leaner, download-based variant may come in a future release for developers who need a
smaller APK.

**Using Kokoro-82M / `kokoro-onnx` directly instead of the JokobeeTTS SDK?**
[`ff_Marine.pt`](https://github.com/Jokobee/JokobeeTTS/releases/latest) (release asset)
is the same French voice (Marine), in the raw Kokoro-82M format — works on Android, iOS,
desktop, server, anywhere Kokoro-82M runs. Also available as a standalone Maven artifact:
`com.jokobee:kokoro-french-voice-female`.

Note: `.pt` is a PyTorch container (523,894 bytes, includes tensor metadata) — **not**
directly usable with `Voice.of(id, lang, bytes)`, which expects the flat `.bin` format
(exactly 522,240 bytes, no header). Passing `.pt` bytes raw throws a clear
`"invalid size"` error. Convert first — `MarineVoice.bin` (same Maven artifact) is
already in the right format, or extract it yourself in a few lines of Python
(`torch.load(...).numpy().astype('<f4').tobytes()`).

## Roadmap

- Leaner, download-based AAR variant for developers who need a smaller APK footprint.

## Build

```
./gradlew :free:assembleRelease   # Free AAR (arm64-v8a)
./gradlew :free:test              # unit tests
```

## Licenses

- **Free** (`:core` + `:free`, this repo): **Apache-2.0** — see [LICENSE](LICENSE).
- **Pro**: commercial license — [jokobee.com](https://jokobee.com).
- Third-party components (Kokoro Apache-2.0, G2P engines / ONNX Runtime MIT, ICU permissive):
  **zero GPL/LGPL**. Details in [LICENSING.md](LICENSING.md) and
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- Based on **Kokoro-82M** (hexgrad); JokobeeTTS is not affiliated with the source projects.

The [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt) file is **bundled in the AAR assets**
at build time (readable at runtime via `context.assets.open("THIRD-PARTY-NOTICES.txt")`).
