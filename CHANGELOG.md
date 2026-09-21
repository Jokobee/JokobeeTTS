# Changelog

Format [Keep a Changelog](https://keepachangelog.com/), versioning [SemVer](https://semver.org/).
This changelog covers the **Free tier** (`:core` + `:free`). The **Pro** tier has its own
tracking (see [README](README.md#modules--tiers)).

## [Unreleased]

### Fixed — English dropped out-of-vocabulary phonemes, silently

- **`PhonemePost` is now applied on the English path too.** It is the safety net that
  rewrites IPA the model does not know into IPA it does: `ɫ`→`l`, `ɝ`→`ɜɹ`, `ɚ`→`əɹ`,
  `g`→`ɡ`, tie bars to ligatures. Without it `KokoroTokenizer.encode` simply **drops**
  any character missing from the vocabulary — `vocab[c]?.let { … }`, no error, no log.

  The English branch returned its G2P output raw, on the reasoning that misaki is already
  in-vocabulary. That holds for misaki's *own* output and for nothing else — and this
  branch now also splices in **caller-supplied** lexicon IPA, which is exactly the data
  that needs the net.

  Measured on a Pixel 8a, the same word given `ɫ` then `l`:

  | | `ɫ` | `l` | |
  |---|---|---|---|
  | French | 57 644 B | 57 644 B | identical — the net was running |
  | English, before | 73 244 B | 74 444 B | **different — the `ɫ` was dropped** |
  | English, after | 74 444 B | 74 444 B | identical |

  **Applying it regresses nothing, and that is verified rather than assumed**: the four
  misaki lexicons contain no `ɫ`, `ɝ`, `ɚ` or Latin `g`, and **zero** entries change
  under NFD. On misaki's own output it is a no-op.

  The two G2P paths stay separate, as designed — misaki for English, CharsiuG2P plus the
  loanword table for the Latin languages. Only the final normalisation is now shared.

### Not reproduced — secondary stress does not break words

- A working note claimed that the **123** entries of `loanwords_en_ipa.tsv` carrying a
  secondary stress `ˌ` broke words. **It does not reproduce.** `ˌ` is in the model's
  vocabulary, so nothing is dropped, and synthesising three of those entries with and
  without it moved the audio by −5.6 %, +4.5 % and +7.9 % — ordinary prosodic variation,
  nothing like the pause a split word would produce.

  Recorded rather than quietly dropped: a suspicion that was carried for weeks and turns
  out to be unfounded is worth the same line as one that is confirmed.

### Fixed — the custom lexicon was ignored in English, silently

- **`Frontend.toPhonemes` skipped `LexiconG2p` for `en_US` and `en_GB`.** The English
  branch returned `enG2p(text)` directly, and that call bypasses the chain where the
  layer-1 lexicon lives. So `Tts.lexicon.add(...)` worked in French, Spanish, Italian and
  Portuguese and **did nothing at all in English**.

  **Silent is the whole problem.** `add()` returned normally, the audio played, and
  nothing anywhere reported that the entry had been dropped — a caller would have
  concluded their IPA was wrong. Measured on a Pixel 8a, the same sentence with and
  without an entry:

  | | control | with entry | |
  |---|---|---|---|
  | French | 106 kB | **100 kB** | the entry arrives |
  | English, before | 116 kB | **116 kB** | byte-identical — dropped |
  | English, after | 116 kB | **113 kB** | the entry arrives |

  The text is now cut **only around the words the lexicon actually claims**, and every
  remaining run still goes to the English G2P whole — which is a better path for English
  than the generic chain, and stays the default. When nothing matches, the call is the
  old call on the old string: **the English control is 116 kB before and after**, which
  is what proves no one else is affected.

  The cost, stated rather than hidden: a claimed word cuts the sentence, so the English
  G2P sees two shorter runs instead of one. Only callers who asked for an override pay it.

### Changed

- **`FrenchExceptions` no longer registers each word twice.** It added a capitalised copy
  of every entry, on the belief that lookup was case-sensitive. `MapLexiconSource.key()`
  lowercases on **both** sides, so the second call overwrote the first with the same
  value. No behaviour change; the file and its KDoc now say what is true.

## [1.2.0]

### Added — the engine can now be closed

- **`Tts` implements `Closeable`.** `Tts.close()` releases the model session and the
  G2P sessions. `G2p` and `Synthesizer` gained a no-op `close()`, and every wrapper in
  the G2P chain forwards it, so closing the outermost releases the innermost.

  **Why it matters.** Both hold `OrtSession`s, which hold *native* memory a garbage
  collector does not reclaim promptly. Without a way to close, an app that opens and
  closes speech repeatedly leaks it. Measured on a Pixel 8a, three open/speak/close
  cycles: the native heap grew by about **350 MB per cycle** while the Java heap never
  moved, so the leak was invisible to any Java-side profiler. With `close()`, the same
  three cycles hold steady at **22 MB**.

  An instance is unusable after `close()`: a further call reaches a closed session and
  raises. That is deliberate — the alternative is synthesising against freed memory.

### Added — a caller can name the voice it is about to get

- **`Tts.defaultVoiceFor(lang)` is public.** It was private, so the zero-config path
  produced audio without telling anyone which voice made it: a caller could only log
  `null`, could not reproduce a rendering, and could not show the user what it picked.
  Asking the engine rather than re-deriving the rule caller-side keeps one source of
  truth.

### Fixed — the default voice now follows what is installed

- **`Tts` no longer demands a voice that was removed from the APK.** The zero-config
  path took its default from a fixed table, so it broke every app that trims its assets
  — and trimming is the documented way to shrink an APK, each voice file being ~510 kB.

  Measured: with `ff_siwis.bin` and `ff_marine.bin` removed, two French voices remained
  usable in the catalog, and `synthesize(lang = "fr")` with no explicit voice still
  demanded `ff_siwis` and threw. Passing a remaining voice explicitly worked, so only
  the lookup was wrong.

  The curated default is still preferred; it is simply checked against the catalog
  first, with any installed voice of that language as the fallback. A language with no
  installed voice at all still raises `UnsupportedLanguageException`, which is the
  honest answer.

### Added — a French exception dictionary

- **`FrenchExceptions`** — words whose spelling does not predict their pronunciation:
  the `-emment` adverbs (`/amɑ̃/`, not `/emɑ̃/`), silent letters (`automne`, `condamner`,
  `baptême`, `sculpteur`), `-ill-` read `/il/` (`ville`, `mille`, `tranquille`), the
  outright contradictions (`monsieur` `/məsjø/`, `femme` `/fam/`, `second` `/səɡɔ̃/`),
  and the loanwords a French-rule G2P mangles (`week-end`, `yacht`, `clown`, `bluetooth`).

  Grouped **by rule** rather than alphabetically: a group says which other words belong
  in it, and an entry that does not fit its rule is probably wrong.

  Context-dependent words are deliberately absent. `plus`, `tous` and `fils` each have
  two pronunciations that depend on meaning; a single entry would fix half the cases and
  break the other half, turning an occasional error into a systematic one.

## [1.1.1]

### Fixed — French G2P (CharsiuG2P)

- `"Jokobee"`: read `jɔkɔbi` (starting with the "y" glide, as in "yes") instead
  of `dʒɔkɔbi` — checked broadly against native French "j" words (jour, jardin,
  Jacques, Julie, Japon, etc.), all correct; this OOV-name misread was isolated
  to the brand name. Built-in `tts.lexicon` entry (`fr`/`fr_CA`).

## [1.1.0]

### Added

- **Marine** (`ff_marine`) — a 38th bundled official voice (free French female
  voice), wired identically to the 37 original official voices (same
  auto-discovery, same `VoiceCatalog` path, validated on-device).

### Fixed — French G2P (CharsiuG2P)

Four built-in `tts.lexicon` entries (`fr`/`fr_CA`), applied automatically by
`Tts.create(...)` — benefits Free and Pro (Pro reuses the same `Tts.create`):

- `"vraiment"`: missing its final nasal vowel (`vʁɛm` instead of `vʁɛmɑ̃`).
- `"ai"` (verb *avoir*, 1st person): read as a diphthong (`aj`) instead of `ɛ`,
  breaking common contractions like `"j'ai"`/`"n'ai"` (`ʒaj` instead of `ʒɛ`).
- `"JokobeeTTS"` (one word, brand styling): mangled to `jɔkɔbit` (the `TTS` part
  disappears).
- `"Android"`: read `ɑ̃dʁwa` (losing the `-oid` ending) instead of `ɑ̃dʁɔid`
  (as in *androïde*).

See `PRONUNCIATION-GUIDE.md` (Pro repo) for the diagnostic method — you can patch
similar cases yourself via `tts.lexicon`, no SDK update needed.

## [1.0.0] — 2026-07-09

> **v1.0.0**: complete TTS engine, validated on-device (Pixel 7 Pro), shipped as a single
> all-in-one AAR (~97 MB) — the Kokoro model and 37 official voices are bundled, not
> downloaded. Zero network, zero setup: `Tts.create(context)` and it speaks.

### Added — Zero-config API
- **`Tts.create(context)`**: builds a ready-to-use pipeline with the Kokoro model and the
  official voice catalog loaded directly from the AAR assets — no ONNX environment, no
  model path, no voice file to manage.
- **`synthesize`/`synthesizeToWav`** now accept an optional `voice` (defaults to a sensible
  official voice per language) and the short language alias `"en"` (→ `en_US`).
- **`VoiceCatalog.official(context)`**: auto-populated catalog of the 37 bundled voices.

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
- **37 official voices bundled** (`voices/*.bin`, ~20 MB) covering the 6 supported
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

### Published
- **Maven Central**: `com.jokobee:jokobeetts-core:1.0.0` and `com.jokobee:jokobeetts:1.0.0`,
  plus two discoverability aliases (same artifact, zero extra code):
  `com.jokobee:tts-ai-android:1.0.0` and `com.jokobee:tts-android-ai:1.0.0`.
- **JitPack**: `com.github.Jokobee.JokobeeTTS:jokobeetts:v1.0.1` (also `jokobeetts-core`,
  `tts-ai-android`, `tts-android-ai`) — same content as Maven Central 1.0.0; the git tag
  is v1.0.1 due to a JitPack build-cache quirk on the first v1.0.0 tag.

### Release notes
- **Free** = 100% free, all languages (no language paywall), zero download. Zero GPL/LGPL.
- **Pro** (real-time streaming, `lang="auto"`, blending, voice import, GPU…): commercial
  license, jokobee.com.
- `ModelManager` (`:core`, resumable/verified download of an external model) exists for
  advanced/Pro setups but is **not** part of the Free zero-config path — Free downloads
  nothing.

## [0.1.0]

- Multi-module skeleton (`:core`/`:free`/`:pro`), build verified.
