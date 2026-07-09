# Changelog

Format [Keep a Changelog](https://keepachangelog.com/), versioning [SemVer](https://semver.org/).
This changelog covers the **Free tier** (`:core` + `:free`). The **Pro** tier and the **SDK**
have their own tracking (see [README](README.md#modules--tiers)).

## [1.0.0] — in preparation

> **v1.0** scope: complete TTS engine, validated on-device (Pixel 7 Pro). The build coordinate
> stays at `0.1.0` for now until the first release (Maven Central + Kokoro model hosting).

### Added — Text processing
- **Normalization** for 6 languages (fr, en_US, en_GB, es, it, pt_BR):
  numbers, dates (named and numeric), times, currencies (per locale), ordinals, fractions,
  ranges, roman numerals, units of measure, acronyms, abbreviations (titles and addresses),
  symbols, phone numbers, email addresses, URLs and postal codes (read aloud).
- **Number verbalization** via ICU (embedded icu4j).

### Added — G2P (grapheme → phoneme)
- **Embedded G2P**, 100% offline, for the supported languages.

### Added — Synthesis
- **Kokoro** via ONNX Runtime, **WAV** PCM 16-bit 24 kHz export, `Tts` facade
  (text → audio), configurable lead/tail silence.
- **Voice registry**: read-only catalog of official voices (Free).

### Added — Stitching & download
- **`AudioStitcher`** (`:core`): multi-sentence stitching — anti-click crossfade at joins,
  configurable inter-sentence silence, peak normalization. Wired into `Tts.synthesize`
  (`stitchConfig`).
- **`ModelManager`** (`:core`): downloads the Kokoro model / voices —
  **cache > assets > download** priority, resume (`.part` + `Range`), progress,
  **SHA-256** verification, `Authorizer` hook (licensing). `manifest.json` manifest.

### Added — Extensibility (reserved hooks)
- **`LexiconSource`** (`:core`): priority custom lexicon (layer before G2P). Empty stub.
- **`StyleResolver`** (`:core`): style/voice resolution before synthesis. Pass-through in v1.0.
- **`StreamingEngine`** / **`StreamChunk`** (`:core`): streaming synthesis contract
  (implemented in the **Pro** tier; `ProRequiredException` in Free).
- **`LanguageDetector`** + `lang="auto"` (`:core`): language detection contract
  (implemented in **Pro**; `ProRequiredException` in Free).

### Validated
- **End-to-end on-device** (Pixel 7 Pro, arm64): text → audible WAV.
- Regression guarded by a unit test suite.

### Release notes
- **Free** = 100% free, all languages (no language paywall). Zero GPL/LGPL.
- **Pro** (real-time streaming, `lang="auto"`, blending, voice import, GPU…): commercial
  license, jokobee.com. *(The `ModelManager` downloader lives in `:core`/Free; the `Authorizer`
  hook lets you attach Pro licensing to it.)*
- **SDK** (per-language modules, contextual style, voice creation): future evolution.

## [0.1.0]

- Multi-module skeleton (`:core`/`:free`/`:pro`), build verified.
