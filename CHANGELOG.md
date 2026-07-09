# Changelog

Format [Keep a Changelog](https://keepachangelog.com/), versioning [SemVer](https://semver.org/).
This changelog covers the **Free tier** (`:core` + `:free`). The **Pro** tier has its own
tracking (see [README](README.md#modules--tiers)).

## [1.0.0] — 2026-07-09

> **v1.0.0**: complete TTS engine, validated on-device (Pixel 7 Pro), shipped as a single
> all-in-one AAR (~110 MB) — the Kokoro model and 38 official voices are bundled, not
> downloaded. Zero network, zero setup: `Tts.create(context)` and it speaks.

### Added — Zero-config API
- **`Tts.create(context)`**: builds a ready-to-use pipeline with the Kokoro model and the
  official voice catalog loaded directly from the AAR assets — no ONNX environment, no
  model path, no voice file to manage.
- **`synthesize`/`synthesizeToWav`** now accept an optional `voice` (defaults to a sensible
  official voice per language) and the short language alias `"en"` (→ `en_US`).
- **`VoiceCatalog.official(context)`**: auto-populated catalog of the 38 bundled voices.

### Added — Text processing
- **Normalization** for 6 languages (fr, en_US, en_GB, es, it, pt_BR):
  numbers, dates (named and numeric), times, currencies (per locale), ordinals, fractions,
  ranges, roman numerals, units of measure, acronyms, abbreviations (titles and addresses),
  symbols, phone numbers, email addresses, URLs and postal codes (read aloud).
- **Number verbalization** via ICU (embedded icu4j).

### Added — G2P (grapheme → phoneme)
- **Embedded G2P**, 100% offline, for the supported languages.

### Added — Synthesis
- **Kokoro** via ONNX Runtime, model **bundled in the AAR** (`model_quantized.onnx`,
  ~88 MB), export **WAV** PCM 16-bit 24 kHz, `Tts` facade (text → audio), configurable
  lead/tail silence.
- **38 official voices bundled** (`voices/*.bin`, ~20 MB) covering the 6 supported
  languages; read-only catalog (Free).

### Added — Stitching
- **`AudioStitcher`** (`:core`): multi-sentence stitching — anti-click crossfade at joins,
  configurable inter-sentence silence, peak normalization. Wired into `Tts.synthesize`
  (`stitchConfig`).

### Added — Extensibility (reserved hooks)
- **`LexiconSource`** (`:core`): priority custom lexicon (layer before G2P). Empty stub.
- **`StyleResolver`** (`:core`): style/voice resolution before synthesis. Pass-through in v1.0.
- **`StreamingEngine`** / **`StreamChunk`** (`:core`): streaming synthesis contract
  (implemented in the **Pro** tier; `ProRequiredException` in Free).
- **`LanguageDetector`** + `lang="auto"` (`:core`): language detection contract
  (implemented in **Pro**; `ProRequiredException` in Free).

### Validated
- **End-to-end on-device** (Pixel 7 Pro, arm64): text → audible WAV, zero-config path.
- Regression guarded by a unit test suite.

### Release notes
- **Free** = 100% free, all languages (no language paywall), zero download. Zero GPL/LGPL.
- **Pro** (real-time streaming, `lang="auto"`, blending, voice import, GPU…): commercial
  license, jokobee.com.
- `ModelManager` (`:core`, resumable/verified download of an external model) exists for
  advanced/Pro setups but is **not** part of the Free zero-config path — Free downloads
  nothing.

## [0.1.0]

- Multi-module skeleton (`:core`/`:free`/`:pro`), build verified.
