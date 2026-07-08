# JokobeeTTS

Synthèse vocale **on-device** pour Android, basée sur **Kokoro-82M** (Apache-2.0).
Texte → audio 24 kHz, 100 % local, sans réseau après installation du modèle.
Modèle **Open Core** : tier **Free** public (ce dépôt) + tier **Pro** commercial.

> **Version 1.0.** Le périmètre décrit ci-dessous est celui de la **v1.0**, validé de bout
> en bout on-device (Pixel 7 Pro). La coordinate de build reste temporairement `0.1.0` le
> temps de finaliser la **première publication** (Maven Central + hébergement du modèle) —
> voir [CHANGELOG.md](CHANGELOG.md) et la [Roadmap](#roadmap).

## Ce qui est livré (v1.0)

- **Normalisation** — 6 langues (fr, en_US, en_GB, es, it, pt_BR) :
  nombres, dates, heures, devises, ordinaux, fractions, plages, chiffres romains, unités,
  acronymes, abréviations et symboles. Passez du texte brut, la normalisation s'occupe du reste.
- **G2P (grapheme → phoneme)** — **embarqué**, 100 % offline, pour les langues supportées.
- **Synthèse Kokoro** — moteur ONNX, export WAV PCM 16 bits 24 kHz, façade `Tts`
  (texte → audio), silence de tête/queue configurable.
- **Registre de voix** — catalogue de voix officielles en lecture (Free).
- **Validé on-device** (Pixel 7 Pro, arm64) : texte → WAV audible.
- **Crochets d'extension** (API publique) : `LexiconSource` (lexique custom prioritaire),
  `StyleResolver` (style/intonation) — points d'insertion réservés, pass-through par défaut.

## Modules & tiers

| Tier | Contenu | Distribution |
|---|---|---|
| **`:core`** | contrats (`G2p`, `LexiconSource`, `StyleResolver`), `TextSplitter`, `LangRouter`, exceptions | Maven Central — `com.jokobee:jokobeetts-core` *(à venir)* |
| **`:free`** | normalisation 6 langues, G2P embarqué, synthèse Kokoro, registre de voix, API `Tts` | Maven Central — `com.jokobee:jokobeetts` *(à venir)* |
| **`:pro`** | import de voix, **blending**, streaming temps réel, GPU Vulkan, x86_64, `lang="auto"`, SSML, timestamps, **Model Manager** | **jokobee.com** (licence commerciale) — repo privé |
| **JokobeeSDK** *(futur)* | SDK IA complet : modules par langue (`:lang-*`), **moteur de style contextuel** (`StyleResolver`), création de voix, pipelines multimodaux | — |

**Free = 100 % gratuit, aucune langue derrière un paywall.** Le split Free/Pro porte sur les
**features** (puissance/temps réel), pas sur les langues.

## Cible

- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10
- Dépendances **permissives** (MIT / Apache-2.0 / Unicode), **zéro GPL/LGPL** —
  voir [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Modèles embarqués / téléchargés

- **G2P** : **embarqué** dans l'AAR (assets, Git LFS) → offline direct, aucune configuration.
- **Modèle de synthèse Kokoro** (~88 Mo) : **téléchargé au 1er lancement** (trop gros pour
  l'AAR), mis en cache local ensuite. *(Téléchargeur `:core` + hébergement : voir Roadmap.)*

## Roadmap

**Reste pour la première publication** (le moteur v1.0 est prêt ; il manque la distribution)
- Téléchargeur `:core` (URL épinglée + SHA256) + hébergement du modèle Kokoro (release GitHub).
- Voix officielles des 6 langues embarquées dans `:free`.
- Publication Maven Central (`:core` + `:free`), bump de la coordinate `0.1.0` → `1.0.0`.

**Au-delà (SDK)**
- Modules par langue (`:lang-*`), moteur de style contextuel (`StyleResolver`), création de voix.

## Build

```
./gradlew :free:assembleRelease   # AAR Free (arm64-v8a)
./gradlew :free:test              # tests unitaires
```

## Licences

- **Free** (`:core` + `:free`, ce dépôt) : **Apache-2.0** — voir [LICENSE](LICENSE).
- **Pro** : licence commerciale (jokobee.com). **SDK** : futur.
- Composants tiers (Kokoro Apache-2.0, moteurs G2P / ONNX Runtime MIT, ICU permissive) :
  **zéro GPL/LGPL**. Détails dans [LICENSING.md](LICENSING.md) et
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- Basé sur **Kokoro-82M** (hexgrad) ; JokobeeTTS n'est pas affilié aux projets sources.

Le fichier [THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt) est **embarqué dans les assets
de l'AAR** au build (lisible au runtime via `context.assets.open("THIRD-PARTY-NOTICES.txt")`).
