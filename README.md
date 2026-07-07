# JokobeeTTS — squelette multi-module (P0)

Synthèse vocale on-device pour Android, basé sur **Kokoro-82M** (Apache-2.0).
Ce dépôt est le **squelette Gradle** du port Kotlin ; le code métier (normaliseurs,
TextSplitter, homographes, registre de voix) sera porté depuis la référence Python
(`filesSDK/tts_frontend/`) — voir `SESSION-FINAL.md`.

## Modules (Open Core)
| Module | Contenu | Distribution |
|---|---|---|
| `:core` | contrats, `TextSplitter`, `LangRouter`, `Voice` | Maven Central — `com.jokobee:jokobeetts-core` |
| `:free` | normaliseurs 10 locales, homographes, voix officielles, API | Maven Central — `com.jokobee:jokobeetts` |
| `:pro` | `VoiceRegistry` (import/blend), streaming, `AutoLangRouter`, x86_64 | **Gumroad uniquement** — jamais Maven/public |

⚠ `:pro` n'a **pas** de plugin de publication (Gumroad only). À déplacer dans le
repo privé au moment du split Open Core.

## Cible
- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10 · Gradle 8.11.1
- Dépendances shippables : `android.icu` (plateforme) + CharsiuG2P (MIT) + ONNX
  Runtime (MIT). **Zéro GPL/LGPL** (voir `THIRD-PARTY-NOTICES.md`).

## Build
```
./gradlew :free:assembleRelease
```
> **Non vérifié localement** (pas de SDK Android dans l'environnement de génération
> du squelette) : lancer le build sur un poste avec le SDK Android ; ajuster
> AGP/Gradle si nécessaire.

Basé sur Kokoro-82M (hexgrad, Apache-2.0). JokobeeTTS n'est pas affilié au projet
source. Voir `THIRD-PARTY-NOTICES.md`.
