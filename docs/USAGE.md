# JokobeeTTS — Guide d'intégration (développeurs)

Texte → audio 24 kHz on-device. Ce guide couvre le tier **Free** (`:core` + `:free`).

- [Installation](#installation)
- [Démarrage rapide](#démarrage-rapide)
- [Le modèle Kokoro](#le-modèle-kokoro)
- [Voix](#voix)
- [Langues](#langues)
- [Crochets d'extension](#crochets-dextension)
- [Téléchargement du modèle (ModelManager)](#téléchargement-du-modèle-modelmanager)
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

L'AAR embarque le G2P (offline). Le **modèle de synthèse Kokoro** n'est pas embarqué —
voir [Le modèle Kokoro](#le-modèle-kokoro).

---

## Démarrage rapide

Assembler le pipeline **une fois** (coûteux : charge les modèles), puis réutiliser.

```kotlin
import ai.onnxruntime.OrtEnvironment
import com.jokobee.tts.free.Tts
import com.jokobee.tts.free.Voice

val env = OrtEnvironment.getEnvironment()

// Pipeline prêt à l'emploi (normalisation + G2P embarqué + synthèse).
// `modelPath` = fichier modèle Kokoro local (voir « Le modèle Kokoro »).
val tts = Tts.create(context, env, modelPath)

// Charger une voix et synthétiser.
val voice = Voice.of("ff_siwis", "fr", voiceBytes)
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

Le modèle de synthèse (~88 Mo) est **téléchargé au 1er lancement** (trop gros pour l'AAR /
Maven Central), puis mis en cache.

- **v1.0** : un téléchargeur `:core` (URL épinglée + SHA256) le récupère dans
  `context.getExternalFilesDir("kokoro")` — *(en cours, voir Roadmap)*.
- **Aujourd'hui** : fournir son chemin à `Tts.create(context, env, modelPath)`.
  Le dev peut le placer lui-même (ex. dans `getExternalFilesDir("kokoro")`).

Le G2P suit le même principe de cache : un modèle **déposé dans le cache** prend automatiquement
le pas sur l'asset embarqué (chemin d'upgrade Pro), sans changement de code.

---

## Voix

```kotlin
val voice = Voice.of("af_heart", "en_US", bytes)     // depuis octets .bin

val catalog = VoiceCatalog()                         // catalogue en lecture (Free)
catalog.get("af_heart"); catalog.list()
```

L'**import de voix custom** et le **blending** (« créer sa voix ») sont des features **Pro**
(`VoiceRegistry`). En Free, les voix officielles se chargent depuis vos assets.

---

## Langues

6 langues normalisées **en amont** (nombres, dates, devises, acronymes, unités…) — passez du
texte brut, pas besoin de pré-traiter.

| Locale | État |
|---|---|
| `fr`, `fr_CA`, `en_US`, `en_GB`, `es`, `it`, `pt_BR` | ✅ validé |

---

## Crochets d'extension

Deux points d'insertion réservés (pass-through par défaut, brancher sans refactor) :

```kotlin
// Lexique custom (marques, corrections) — consulté AVANT le G2P.
class BrandLexicon : com.jokobee.tts.core.LexiconSource {
    override fun lookup(word: String, lang: String): String? =
        if (word.equals("jokobee", true)) "dʒoʊkoʊbi" else null
}
tts.lexicon.load(BrandLexicon())             // enregistré au pipeline

// Résolution de style/voix — le pipeline passe TOUJOURS par le StyleResolver.
val tts2 = Tts.create(context, env, modelPath,
    styleResolver = com.jokobee.tts.core.DefaultStyleResolver())
```

---

## Téléchargement du modèle (ModelManager)

`ModelManager` (`:core`) résout le modèle ONNX et les voix vers des fichiers locaux.
Priorité : **cache** (déjà téléchargé et vérifié) → **assets** embarqués → **téléchargement**
(Cloudflare). Téléchargement repris (`.part` + `Range`), progression, vérification SHA-256.

```kotlin
val manifest = com.jokobee.tts.core.ModelManifest.fromJson(
    context.assets.open("model-manifest.json").bufferedReader().readText(),
)
val mgr = com.jokobee.tts.core.ModelManager(
    cacheDir = java.io.File(context.filesDir, "kokoro"),
    assets = com.jokobee.tts.core.AssetProvider { name ->
        runCatching { context.assets.open(name) }.getOrNull()   // voix/modèle embarqués éventuels
    },
)
val files = mgr.ensureAll(manifest) { name, done, total ->
    // maj UI de progression (total = -1 si inconnu)
}
val tts = Tts.create(context, env, modelPath = files.getValue("kokoro.onnx").absolutePath)
```

Le manifeste (`docs/model-manifest.template.json`) liste chaque artefact
(`name`, `url`, `sha256`, `size`). Un `sha256` valant `"TODO"` ou vide **saute** la
vérification (artefact pas encore uploadé). Le crochet `Authorizer` (stub `{ true }`)
permet de conditionner le téléchargement à une licence (Pro) plus tard.

---

## Perf & threading

- Construire le pipeline (`Tts.create(...)`) est **coûteux** (chargement modèles) :
  le faire **une fois**, hors du thread UI.
- La synthèse est CPU-bound : l'exécuter sur un worker. Un cache G2P est intégré.
- Le pipeline et les sessions ONNX sont réutilisables ; libérer les ressources en fin de vie.
- ABI : Free = `arm64-v8a`. Le support `x86_64` (émulateur/Chromebook) est une feature Pro.
