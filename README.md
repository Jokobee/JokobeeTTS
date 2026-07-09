# JokobeeTTS

[![CI](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml/badge.svg)](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml)

**On-device** text-to-speech for Android, based on **Kokoro-82M** (Apache-2.0).
Text → 24 kHz audio, 100% local. The Kokoro model and 37 official voices are **bundled in
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
- **37 official voices — bundled**, one sensible default per supported locale, used
  automatically by the zero-config `Tts.create(context)` API.
- **Validated on-device** (Pixel 7 Pro, arm64): text → audible WAV.
- **Extension hooks** (public API): `LexiconSource` (priority custom lexicon),
  `StyleResolver` (style/intonation) — reserved insertion points, pass-through by default.

## Modules & tiers

| Tier | Contents | Distribution |
|---|---|---|
| **`:core`** | contracts (`G2p`, `LexiconSource`, `StyleResolver`, `LanguageDetector`, `StreamingEngine`), `TextSplitter`, `AudioStitcher`, exceptions | Maven Central — `com.jokobee:jokobeetts-core` |
| **`:free`** | 6-language normalization, embedded G2P, Kokoro synthesis with the model + 37 official voices **bundled**, **multi-sentence stitching**, `Tts` API | Maven Central — `com.jokobee:jokobeetts` (aliases: `tts-ai-android`, `tts-android-ai`) |
| **`:pro`** | voice import, **blending**, **real-time streaming**, **`lang="auto"`** (detection), Vulkan GPU, x86_64, SSML, timestamps | **jokobee.com** (commercial license) — private repo |

**Free = 100% free, no language behind a paywall.** The Free/Pro split is about **features**
(power / real-time), not languages.

## Target

- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10
- **Permissive** dependencies (MIT / Apache-2.0 / Unicode), **zero GPL/LGPL** —
  see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Embedded assets

Everything ships **inside the AAR** (Git LFS) — no download, no first-launch setup, no
network call, ever:

- **G2P**: embedded models + lexicons (CharsiuG2P, misaki EN).
- **Kokoro synthesis model** (`model_quantized.onnx`, ~88 MB).
- **37 official voices** (`voices/*.bin`) covering the 6 supported languages, with one
  default voice per locale used automatically when none is passed explicitly.

Total AAR size: **~97 MB**. That's the trade-off for "it just works" out of the box — a
leaner, download-based variant may come in a future release for developers who need a
smaller APK.

## Roadmap

- Leaner, download-based AAR variant for developers who need a smaller APK footprint.

## Build

```
./gradlew :free:assembleRelease   # Free AAR (arm64-v8a)
./gradlew :free:test              # unit tests
```

## Licenses

- **Free** (`:core` + `:free`, this repo): **Apache-2.0** — see [LICENSE](LICENSE).
- **Pro**: commercial license (jokobee.com).
- Third-party components (Kokoro Apache-2.0, G2P engines / ONNX Runtime MIT, ICU permissive):
  **zero GPL/LGPL**. Details in [LICENSING.md](LICENSING.md) and
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- Based on **Kokoro-82M** (hexgrad); JokobeeTTS is not affiliated with the source projects.

The [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt) file is **bundled in the AAR assets**
at build time (readable at runtime via `context.assets.open("THIRD-PARTY-NOTICES.txt")`).
