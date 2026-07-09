# Licensing — JokobeeTTS

Overview of the licensing model, per product tier and per component.

## JokobeeTTS code, by tier

| Tier | License | Scope |
|---|---|---|
| **Free** (`:core` + `:free`, this repo) | **Apache-2.0** — see [LICENSE](LICENSE) | Fully open source. All languages. Free use, including commercial, subject to the Apache-2.0 obligations (below). |
| **Pro** (`:pro`, private repo) | **Commercial license** — jokobee.com | Voice import, blending, streaming, GPU, Model Manager, etc. Not open source. |

**Free = 100% free and open source, no language behind a paywall.** The Pro tier is a
superset of paid features; it restricts no language.

## Bundled/linked third-party components (distributed path)

All **permissive** — no GPL/LGPL dependency in the shipped artifact.

| Component | License |
|---|---|
| Kokoro-82M (model) | Apache-2.0 |
| Phonemization engines (G2P) | MIT |
| ONNX Runtime | MIT |
| ICU4J (embedded icu4j) | Unicode/ICU (permissive) |

Component names, authors, copyrights and repositories: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
A text version is bundled in the AAR (`assets/THIRD-PARTY-NOTICES.txt`, readable at runtime
via `context.assets.open(...)` for an "open source licenses" screen).

## Obligations if you redistribute (Apache-2.0)

1. Provide a copy of the Apache-2.0 license ([LICENSE](LICENSE)).
2. Keep the attribution notices ([THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) / `.txt`),
   in particular "**based on Kokoro-82M**".
3. State modified files.
4. Do not use the source projects' trademarks as your product's trademark.

## "Zero GPL" guarantee

The shippable path (`:core` + `:free`) references **no** GPL/LGPL dependency. A guard test
fails on the slightest contamination.
