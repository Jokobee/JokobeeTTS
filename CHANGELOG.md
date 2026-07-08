# Changelog

Format [Keep a Changelog](https://keepachangelog.com/), versionnage [SemVer](https://semver.org/).
Ce changelog couvre le **tier Free** (`:core` + `:free`). Le tier **Pro** et le **SDK** ont
leur propre suivi (voir [README](README.md#modules--tiers)).

## [1.0.0] — en préparation

> Périmètre **v1.0** : moteur TTS complet, validé on-device (Pixel 7 Pro). La coordinate de
> build reste temporairement `0.1.0` jusqu'à la première publication (Maven Central +
> hébergement du modèle Kokoro).

### Ajouté — Traitement du texte
- **Normalisation** 6 langues (fr, en_US, en_GB, es, it, pt_BR) :
  nombres, dates (nommées et numériques), heures, devises (par locale), ordinaux, fractions,
  plages, chiffres romains, unités de mesure, acronymes, abréviations (titres et adresses),
  symboles, numéros de téléphone, adresses e-mail, URL et codes postaux (lus à voix haute).
- **Verbalisation** des nombres via ICU (icu4j embarqué).

### Ajouté — G2P (grapheme → phoneme)
- **G2P embarqué**, 100 % offline, pour les langues supportées.

### Ajouté — Synthèse
- **Kokoro** via ONNX Runtime, export **WAV** PCM 16 bits 24 kHz, façade **`Tts`**
  (texte → audio), silence de tête/queue configurable.
- **Registre de voix** : catalogue de voix officielles en lecture (Free).

### Ajouté — Extensibilité (crochets réservés)
- **`LexiconSource`** (`:core`) : lexique custom prioritaire (couche avant le G2P). Stub vide.
- **`StyleResolver`** (`:core`) : résolution du style/voix avant synthèse. Pass-through v1.0.

### Validé
- **Bout-en-bout on-device** (Pixel 7 Pro, arm64) : texte → WAV audible.
- Régression garantie par une suite de tests unitaires.

### Notes de version
- **Free** = 100 % gratuit, toutes les langues (pas de paywall langue). Zéro GPL/LGPL.
- **Pro** (streaming, blending, import de voix, GPU, Model Manager…) : licence commerciale,
  jokobee.com.
- **SDK** (modules par langue, style contextuel, création de voix) : évolution future.

## [0.1.0]

- Squelette multi-module (`:core`/`:free`/`:pro`), build vérifié.
