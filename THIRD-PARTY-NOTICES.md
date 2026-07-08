# THIRD-PARTY-NOTICES — JokobeeTTS

JokobeeTTS est un produit **Jokobee** (jokobee.com, contact@jokobee.com). Il **emballe et
étend** des composants tiers ci-dessous. JokobeeTTS n'est ni affilié ni endossé par les
projets sources (Apache-2.0 §6 — les marques des projets sources ne sont pas utilisées comme
marque du produit).

## Composants du chemin distribué (artefact livré)

| Composant | Auteur/Projet | Licence | Rôle |
|---|---|---|---|
| **Kokoro-82M** | hexgrad | **Apache-2.0** | modèle de synthèse. JokobeeTTS est « **based on Kokoro-82M** ». |
| **ONNX Runtime** | Microsoft | **MIT** | moteur d'inférence |
| **CharsiuG2P** | CharsiuNLP | **MIT** | phonémisation (grapheme → phoneme) |
| **misaki** | hexgrad (Kokoro) | **MIT** | phonémisation anglaise (grapheme → phoneme) |
| **ICU4J** (`com.ibm.icu:icu4j`) | Unicode, Inc. | **Unicode/ICU License** (permissive) | verbalisation des nombres (RBNF spellout) |
| Code JokobeeTTS | Jokobee | Apache-2.0 (tier Free) | normalisation, registre de voix, orchestration |

**Aucune dépendance GPL/LGPL dans l'artefact distribué.**

## Obligations Apache-2.0 respectées
1. **Attribution conservée** : ce fichier + mention « based on Kokoro-82M » dans le
   README. Copyright hexgrad conservé.
2. **Pas d'usage de la marque source** comme marque produit : le produit s'appelle
   **JokobeeTTS**, pas « Kokoro » (évite toute apparence d'affiliation/endorsement).
3. **Changements documentés** : JokobeeTTS ajoute une couche de normalisation multilingue,
   un registre de voix, une orchestration et un traitement gracieux des erreurs. Les **poids
   du modèle ne sont pas modifiés** (emballage/orchestration seulement).
