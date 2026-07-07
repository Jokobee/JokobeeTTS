# JokobeeTTS — squelette multi-module (P0)

Synthèse vocale on-device pour Android, basé sur **Kokoro-82M** (Apache-2.0).
Ce dépôt est le **squelette Gradle** du port Kotlin ; le code métier (normaliseurs,
TextSplitter, homographes, registre de voix) sera porté depuis la référence Python
(`filesSDK/tts_frontend/`) — voir `SESSION-FINAL.md`.

## Modules (Open Core — repo PUBLIC)
| Module | Contenu | Distribution |
|---|---|---|
| `:core` | contrats, `TextSplitter`, `LangRouter`, `Voice` | Maven Central — `com.jokobee:jokobeetts-core` |
| `:free` | normaliseurs 10 locales, homographes, voix officielles, API | Maven Central — `com.jokobee:jokobeetts` |

Le tier **`:pro`** (VoiceRegistry, blending, streaming, x86_64) vit dans le repo
**PRIVÉ** `JokobeeTTS-private` (Gumroad uniquement, jamais Maven/public), relié à
ce repo par **composite build**.

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
