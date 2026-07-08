# Changelog

Format [Keep a Changelog](https://keepachangelog.com/), versionnage [SemVer](https://semver.org/).
Ce changelog couvre le **tier Free** (`:core` + `:free`). Le tier **Pro** et le **SDK** ont
leur propre suivi (voir [README](README.md#modules--tiers)).

## [1.0.0] — en préparation

> Périmètre **v1.0** : moteur TTS complet, validé on-device (Pixel 7 Pro). La coordinate de
> build reste temporairement `0.1.0` jusqu'à la première publication (Maven Central +
> hébergement du modèle Kokoro).

### Ajouté — Frontend
- **Normalisation** 10 locales (fr, en_US, en_GB, es, it, pt_BR, hi, ja, zh, ko) :
  nombres, dates, heures, devises, ordinaux, fractions, plages, romains, pièces.
- **Acronymes/abréviations EN** : épellation (TV → « T V », FBI ; NASA gardé), titres
  (Dr → Doctor, St → Street/Saint contextuel), contractions préservées pour misaki.
- **Homographes fr** : désambiguïsation contextuelle légère.
- **Verbalisation** des nombres via ICU `RuleBasedNumberFormat` (icu4j embarqué).

### Ajouté — G2P (grapheme → phoneme)
- **fr, es** : CharsiuG2P ByT5 *tiny* int8 (~20 Mo, MIT) **embarqué** ; clamp de vocab
  (`PhonemePost` : g→ɡ, ʼ, ɫ→l, ɝ→ɜɹ) trouvé par audit.
- **en** : **misaki** (G2P officiel de Kokoro, MIT) **porté nativement en Kotlin** — lexique
  90 k+93 k entrées embarqué, stemming `-s/-ed/-ing`, `apply_stress`, épellation NNP,
  POS heuristique pour les homographes (object/use/live…), fallback CharsiuG2P clampé.
- **Audit qualité** : 0 phonème hors-vocab Kokoro sur fr (99,2 %), es (100 %), en.

### Ajouté — Synthèse
- **Kokoro** `model_quantized` (88 Mo) via ONNX Runtime, tokeniseur (vocab embarqué),
  export **WAV** PCM 16 bits 24 kHz, façade **`Tts`** (texte → audio), silence de tête/queue.
- **Registre de voix** : format autoritaire `[510, 256]` f32, catalogue en lecture (Free).

### Ajouté — Extensibilité (crochets réservés)
- **`LexiconSource`** (`:core`) : lexique custom prioritaire (couche #1 avant le G2P). Stub vide.
- **`StyleResolver`** (`:core`) : résolution du style/voix avant synthèse. Pass-through v1.0.

### Validé
- **Bout-en-bout on-device** (Pixel 7 Pro, arm64) : fr et en, texte → WAV audible.
- Régression garantie par tests (golden misaki, audits de vocab, normalisation, crochets).

### Notes de version
- **Free** = 100 % gratuit, toutes les langues (pas de paywall langue). Zéro GPL/LGPL.
- **Pro** (streaming, blending, import de voix, GPU, Model Manager…) : licence commerciale,
  jokobee.com.
- **SDK** (modules par langue, style contextuel, création de voix) : évolution future.

## [0.1.0]

- Squelette multi-module (`:core`/`:free`/`:pro`), build vérifié. Base du port depuis la
  référence Python.
