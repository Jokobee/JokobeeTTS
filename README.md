# JokobeeTTS

Synthèse vocale **on-device** pour Android, basée sur **Kokoro-82M** (Apache-2.0).
Texte → audio 24 kHz, 100 % local, sans réseau après installation du modèle.
Modèle **Open Core** : tier **Free** public (ce dépôt) + tier **Pro** commercial.

> **Version 1.0.** Le périmètre décrit ci-dessous est celui de la **v1.0**, validé de bout
> en bout on-device (Pixel 7 Pro). La coordinate de build reste temporairement `0.1.0` le
> temps de finaliser la **première publication** (Maven Central + hébergement du modèle) —
> voir [CHANGELOG.md](CHANGELOG.md) et la [Roadmap](#roadmap).

## Ce qui est livré (v1.0)

- **Normalisation** — 10 locales (fr, en_US, en_GB, es, it, pt_BR, hi, ja, zh, ko) :
  nombres, dates, heures, devises, ordinaux, acronymes (TV → « T V »), abréviations
  (Dr → Doctor), symboles. Verbalisation via ICU (`RuleBasedNumberFormat`).
- **G2P (grapheme → phoneme)** validé et **audité (0 phonème hors-vocab)** sur 3 langues :
  - **fr, es** — CharsiuG2P ByT5 *tiny* int8 (~20 Mo, MIT), **embarqué** (offline).
  - **en** — **misaki** (le G2P officiel de Kokoro, MIT) **porté nativement en Kotlin** :
    lexique 90 k+93 k entrées embarqué, stemming, stress, POS heuristique pour homographes ;
    fallback CharsiuG2P (clampé) pour les mots hors-lexique.
- **Homographes fr** — désambiguïsation contextuelle légère (est/plus/…).
- **Synthèse Kokoro** — tokeniseur, moteur ONNX (`model_quantized`, 88 Mo), export WAV
  PCM 16 bits 24 kHz, façade `Tts` (texte → audio), silence de tête/queue.
- **Registre de voix** — format autoritaire `[510, 256]` f32, catalogue en lecture (Free).
- **Validé on-device** (Pixel 7 Pro, arm64) : fr et en, texte → WAV audible.
- **Crochets d'extension réservés** (stubs) : `LexiconSource` (lexique custom prioritaire),
  `StyleResolver` (style/intonation) — l'interface existe, l'implémentation viendra.

## Modules & tiers

| Tier | Contenu | Distribution |
|---|---|---|
| **`:core`** | contrats (`G2p`, `LexiconSource`, `StyleResolver`), `TextSplitter`, `LangRouter`, exceptions | Maven Central — `com.jokobee:jokobeetts-core` *(à venir)* |
| **`:free`** | normalisation 10 locales, G2P (CharsiuG2P + misaki EN), homographes, synthèse Kokoro, registre de voix, API `Tts` | Maven Central — `com.jokobee:jokobeetts` *(à venir)* |
| **`:pro`** | import de voix, **blending**, streaming temps réel, GPU Vulkan, x86_64, `lang="auto"`, SSML, timestamps, **Model Manager** | **jokobee.com** (licence commerciale) — repo privé |
| **JokobeeSDK** *(futur)* | SDK IA complet : modules par langue (`:lang-*`), **moteur de style contextuel** (`StyleResolver`), création de voix, pipelines multimodaux | — |

**Free = 100 % gratuit, aucune langue derrière un paywall.** Le split Free/Pro porte sur les
**features** (puissance/temps réel), pas sur les langues.

## Cible

- minSdk 24 · compileSdk 36 · Kotlin 2.1 · AGP 8.10
- Dépendances shippables : ICU (icu4j embarqué) + CharsiuG2P (MIT) + misaki (MIT) +
  ONNX Runtime (MIT). **Zéro GPL/LGPL** (voir [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)).

## Modèles embarqués / téléchargés

- **G2P CharsiuG2P tiny int8** (~20 Mo) : **embarqué** (assets, Git LFS) → offline direct.
- **misaki EN** (lexiques ~6 Mo) : **embarqué** (assets, Git LFS).
- **Kokoro `model_quantized`** (88 Mo) : **téléchargé au 1er lancement** (trop gros pour l'AAR),
  mis en cache local ensuite. *(Téléchargeur `:core` + hébergement : voir Roadmap.)*

## Roadmap

**Reste pour la première publication** (le moteur v1.0 est prêt ; il manque la distribution)
- Téléchargeur `:core` (URL épinglée + SHA256) + hébergement du modèle Kokoro (release GitHub).
- 54 voix officielles embarquées.
- G2P des langues secondaires (it, pt, ja, zh, ko, hi) — CharsiuG2P / misaki / règles selon la langue.
- Publication Maven Central (`:core` + `:free`), bump de la coordinate `0.1.0` → `1.0.0`.

**Au-delà (SDK)**
- Modules par langue (`:lang-*`), moteur de style contextuel (`StyleResolver`), création de voix.

## Build

```
./gradlew :free:assembleRelease   # AAR Free (arm64-v8a)
./gradlew :free:test              # tests unitaires
```

## Licence & attribution

Basé sur **Kokoro-82M** (hexgrad, Apache-2.0). G2P : **CharsiuG2P** et **misaki** (MIT).
JokobeeTTS n'est pas affilié aux projets sources. Voir [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Third-Party Notices

Les composants tiers et leurs licences sont listés dans
[THIRD-PARTY-NOTICES.txt](THIRD-PARTY-NOTICES.txt), copié aussi dans les assets de l'AAR au build.
