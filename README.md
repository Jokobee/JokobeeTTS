# JokobeeTTS

[![CI](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml/badge.svg)](https://github.com/Jokobee/JokobeeTTS/actions/workflows/ci.yml)

**On-device** text-to-speech for Android, based on **Kokoro-82M** (Apache-2.0).
Text → 24 kHz audio, 100% local, no network after the model is installed.
**Open Core** model: public **Free** tier (this repo) + commercial **Pro** tier.

> **Version 1.0.** The scope described below is that of **v1.0**, validated end-to-end
> on-device (Pixel 7 Pro). The build coordinate stays at `0.1.0` for now while the **first
> release** is finalized (Maven Central + model hosting) — see [CHANGELOG.md](CHANGELOG.md)
> and the [Roadmap](#roadmap).

## What ships (v1.0)

- **Normalization** — 6 languages (fr, en_US, en_GB, es, it, pt_BR):
  numbers, dates, times, currencies, ordinals, fractions, ranges, roman numerals, units,
  acronyms, abbreviations and symbols. Feed raw text; normalization handles the rest.
- **G2P (grapheme → phoneme)** — **embedded**, 100% offline, for the supported languages.
- **Kokoro synthesis** — ONNX engine, WAV PCM 16-bit 24 kHz export, `Tts` facade
  (text → audio), configurable lead/tail silence.
- **Voice registry** — read-only catalog of official voices (Free).
- **Validated on-device** (Pixel 7 Pro, arm64): text → audible WAV.
- **Extension hooks** (public API): `LexiconSource` (priority custom lexicon),
  `StyleResolver` (style/intonation) — reserved insertion points, pass-through by default.

## Modules & tiers

| Tier | Contents | Distribution |
|---|---|---|
| **`:core`** | contracts (`G2p`, `LexiconSource`, `StyleResolver`, `LanguageDetector`, `StreamingEngine`), `TextSplitter`, `AudioStitcher`, **`ModelManager`** (downloader), exceptions | Maven Central — `com.jokobee:jokobeetts-core` *(coming)* |
| **`:free`** | 6-language normalization, embedded G2P, Kokoro synthesis, voice registry, **multi-sentence stitching**, `Tts` API | Maven Central — `com.jokobee:jokobeetts` *(coming)* |
| **`:pro`** | voice import, **blending**, **real-time streaming**, **`lang="auto"`** (detection), Vulkan GPU, x86_64, SSML, timestamps | **jokobee.com** (commercial license) — private repo |

**Free = 100% free, no language behind a paywall.** The Free/Pro split is about **features**
(power / real-time), not languages.

## Target

- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10
- **Permissive** dependencies (MIT / Apache-2.0 / Unicode), **zero GPL/LGPL** —
  see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Embedded / downloaded models

- **G2P**: **embedded** in the AAR (assets, Git LFS) → offline out of the box, no configuration.
- **Kokoro synthesis model** (~88 MB): **downloaded on first launch** (too large for the AAR)
  via **`ModelManager`** (`:core`) — cache > assets > download priority, resumable + SHA-256
  verification, then cached locally. *(Model hosting: see Roadmap.)*

## Roadmap

**Remaining before the first release** (the v1.0 engine is ready; distribution is what's missing)
- **Hosting** the Kokoro model (Cloudflare) + filling in `manifest.json` (`sha256`/`size` —
  the `ModelManager` downloader in `:core` is **ready**, see
  [`docs/model-manifest.template.json`](docs/model-manifest.template.json)).
- Official voices for the 6 languages bundled in `:free`.
- Maven Central publication (`:core` + `:free`), bump the coordinate `0.1.0` → `1.0.0`.

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
