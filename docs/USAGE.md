# JokobeeTTS — Guide d'intégration (développeurs)

Texte → audio 24 kHz on-device. Ce guide couvre le tier **Free** (`:core` + `:free`).

- [Installation](#installation)
- [Démarrage rapide](#démarrage-rapide)
- [Le modèle Kokoro](#le-modèle-kokoro)
- [Voix](#voix)
- [Langues & G2P](#langues--g2p)
- [Crochets d'extension](#crochets-dextension)
- [Perf & threading](#perf--threading)

---

## Installation

> Publication Maven Central prévue pour la 1.0. En attendant, build local
> (`./gradlew :free:assembleRelease`).

```kotlin
// build.gradle.kts (à la publication)
dependencies {
    implementation("com.jokobee:jokobeetts:1.0.0")   // :free (dépend de :core)
}
```

L'AAR embarque le G2P (CharsiuG2P tiny, fr/es) et le lexique **misaki** (en). Le **modèle
de synthèse Kokoro** (88 Mo) n'est pas embarqué — voir [Le modèle Kokoro](#le-modèle-kokoro).

---

## Démarrage rapide

Assembler le pipeline une fois (coûteux : charge les modèles), puis réutiliser.

```kotlin
import ai.onnxruntime.OrtEnvironment
import com.jokobee.tts.free.*

val env = OrtEnvironment.getEnvironment()

// 1. G2P : CharsiuG2P embarqué (fr/es) + cache mémoire.
val g2p = CachingG2p(CharsiuG2p.fromAssetsOrCache(context, env))

// 2. Anglais : misaki (lexique embarqué) + fallback CharsiuG2P + lexique custom optionnel.
val misakiEn = MisakiEnG2p.fromAssets(context, fallback = g2p)

// 3. Frontend : normalisation + G2P (route en_US/en_GB vers misaki).
val frontend = Frontend(g2p, enG2p = { misakiEn.phonemize(it) })

// 4. Synthèse Kokoro (le fichier modèle doit être présent — voir plus bas).
val synth = KokoroSynth.fromModelFile(env, modelPath, KokoroTokenizer.fromAsset(context))

// 5. Façade TTS.
val tts = Tts(frontend, synth)

// 6. Charger une voix et synthétiser.
val voice = Voice.of("ff_siwis", "fr", voiceBytes)          // .bin [510,256] f32
val wav: ByteArray = tts.synthesizeToWav("Bonjour le monde", "fr", voice)   // WAV 24 kHz
// -> écrire dans un fichier, ou lire via AudioTrack / MediaPlayer.
```

Formes de sortie :

```kotlin
val samples: FloatArray = tts.synthesize(text, lang, voice, speed = 1.0f)   // f32 [-1,1] 24 kHz
val wavBytes: ByteArray = tts.synthesizeToWav(text, lang, voice)            // WAV PCM 16 bits
```

Un **silence de tête (200 ms) / queue (100 ms)** est ajouté par défaut (évite la troncature
du 1er mot par la latence d'init du player). Ajuster via `leadMs` / `trailMs` (0 = brut).

---

## Le modèle Kokoro

Le modèle `model_quantized.onnx` (**88 Mo**, quantifié int8) est **téléchargé au 1er lancement**
(trop gros pour l'AAR / Maven Central), puis mis en cache.

- **v1.0** : un téléchargeur `:core` (URL épinglée + SHA256) le récupère dans
  `context.getExternalFilesDir("kokoro")` — *(en cours, voir Roadmap)*.
- **Aujourd'hui** : fournir le chemin du fichier à `KokoroSynth.fromModelFile(env, path, tokenizer)`.
  Le dev peut le placer lui-même (ex. dans `getExternalFilesDir("kokoro")`).

Le G2P suit le même principe de cache : `CharsiuG2p.fromAssetsOrCache(context, env)` lit un
modèle **téléchargé** s'il existe, sinon l'**asset embarqué** — un modèle déposé dans le cache
prend automatiquement le pas (chemin d'upgrade Pro), sans changement de code.

---

## Voix

Format autoritaire (Kokoro v1.0) : `float32` little-endian, C-order, `[510, 256]` (522240 octets).

```kotlin
val voice = Voice.of("af_heart", "en_US", bytes)     // depuis octets .bin
voice.styleFor(nTokens)                              // vecteur de style [256]

val catalog = VoiceCatalog()                         // catalogue en lecture (Free)
catalog.get("af_heart"); catalog.list()
```

L'**import de voix custom** et le **blending** (« créer sa voix ») sont des features **Pro**
(`VoiceRegistry`). En Free, les voix officielles se chargent depuis vos assets.

---

## Langues & G2P

| Locale | G2P | État |
|---|---|---|
| `fr`, `fr_CA` | CharsiuG2P tiny (embarqué) | ✅ validé, audité |
| `es` | CharsiuG2P tiny | ✅ validé, audité |
| `en_US` | **misaki** (porté) + fallback CharsiuG2P | ✅ validé, audité |
| `en_GB` | misaki (british) | routé, à valider |
| `it`, `pt_BR`, `hi`, `ja`, `zh`, `ko` | normalisation OK ; G2P — roadmap | normalisation ✅ |

La normalisation gère les nombres/dates/devises/acronymes **en amont** — passez du texte brut,
pas besoin de pré-traiter.

---

## Crochets d'extension

Deux points d'insertion réservés (pass-through par défaut, brancher sans refactor) :

```kotlin
// Lexique custom (marques, corrections) — consulté AVANT le G2P.
class BrandLexicon : com.jokobee.tts.core.LexiconSource {
    override fun lookup(word: String) = if (word.equals("jokobee", true)) "dʒoʊkoʊbi" else null
}
val misakiEn = MisakiEnG2p.fromAssets(context, fallback = g2p, customLexicon = BrandLexicon())

// Résolution de style/voix — le pipeline passe TOUJOURS par le StyleResolver.
val tts = Tts(frontend, synth, styleResolver = com.jokobee.tts.core.DefaultStyleResolver())
```

---

## Perf & threading

- Construire le pipeline (`fromAssets`/`fromModelFile`) est **coûteux** (chargement modèles) :
  le faire **une fois**, hors du thread UI.
- La synthèse est CPU-bound : l'exécuter sur un worker. G2P ≈ dizaines de ms/mot (cache LRU
  intégré via `CachingG2p`).
- `KokoroSynth` et les sessions ONNX sont réutilisables ; appeler `close()` à la fin de vie.
- ABI : Free = `arm64-v8a`. Le support `x86_64` (émulateur/Chromebook) est une feature Pro.
