# THIRD-PARTY-NOTICES — JokobeeTTS

JokobeeTTS (ex-« kokoro-android ») est un produit **Jokobee** (jokobee.com,
contact@jokobee.com). Il **emballe et étend** des composants tiers ci-dessous.
JokobeeTTS n'est ni affilié ni endossé par les projets sources (Apache-2.0 §6 —
les marques des projets sources ne sont pas utilisées comme marque du produit).

## Composants du chemin distribué (artefact livré)

| Composant | Auteur/Projet | Licence | Rôle |
|---|---|---|---|
| **Kokoro-82M** | hexgrad | **Apache-2.0** | modèle de synthèse (poids ONNX). JokobeeTTS est « **based on Kokoro-82M** ». |
| **ONNX Runtime** | Microsoft | **MIT** | moteur d'inférence |
| **CharsiuG2P** | CharsiuNLP | **MIT** | phonémisation (grapheme→phoneme) |
| **ICU** (`android.icu.text`) | Unicode, Inc. | **Unicode/ICU License** (permissive) | verbalisation des nombres (RBNF), API 24+ |
| Code JokobeeTTS | Jokobee | propriétaire (Free = Apache-2.0 côté public) | frontend, registre de voix, orchestration |

**Aucune dépendance GPL/LGPL dans l'artefact distribué.**

## Obligations Apache-2.0 respectées
1. **Attribution conservée** : ce fichier + mention « based on Kokoro-82M » dans le
   README. Copyright hexgrad conservé.
2. **Pas d'usage de la marque source** comme marque produit : le produit s'appelle
   **JokobeeTTS**, pas « Kokoro » (évite toute apparence d'affiliation/endorsement).
3. **Changements documentés** : JokobeeTTS ajoute un frontend de normalisation
   multilingue, un registre de voix (import/blend), un segmenteur, un fallback
   gracieux — décrits dans `ARCHITECTURE.md` / `CLOSURE.md`. Les **poids du modèle
   ne sont pas modifiés** (emballage/orchestration seulement).

## ⚠ espeak-ng (GPL-3.0) — À RETIRER de l'artefact
Le scaffold actuel (`D:\Projects\KokoroTTS\kokoro-android`) **lie encore espeak-ng
(GPL-3.0)** en `.so` dynamique séparé — hérité du prototype. **Interdit dans une
distribution JokobeeTTS** : la GPL-3.0 contaminerait l'artefact. Le chemin
shippable le remplace par **CharsiuG2P (MIT)**. Action au port : **supprimer
espeak-ng du scaffold** et vérifier via un audit de licences (CI) qu'aucun `.so`
GPL n'est empaqueté. Tant que espeak-ng est présent, **ne rien publier**.

## Composants BANC D'ESSAI seulement (jamais distribués)
espeak-ng (GPL-3.0), num2words (LGPL-2.1), phonemizer (GPL-3.0),
faster-whisper (MIT) — utilisés pour mesurer/valider (WER, calibrations), **exclus
de l'artefact**. Gate CI recommandé : `pip-licenses` / `license-gradle-plugin` sur
l'arbre du chemin shippable uniquement.
