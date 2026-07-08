# Licences — JokobeeTTS

Vue d'ensemble du modèle de licence, par tier produit et par composant.

## Code JokobeeTTS, par tier

| Tier | Licence | Portée |
|---|---|---|
| **Free** (`:core` + `:free`, ce dépôt) | **Apache-2.0** — voir [LICENSE](LICENSE) | Open source complet. Toutes les langues. Usage libre, y compris commercial, sous réserve des obligations Apache-2.0 (ci-dessous). |
| **Pro** (`:pro`, dépôt privé) | **Licence commerciale** — jokobee.com | Import de voix, blending, streaming, GPU, Model Manager, etc. Non open source. |
| **SDK** (JokobeeSDK, futur) | à définir | Évolution (modules par langue, style contextuel, création de voix). |

**Free = 100 % gratuit et open source, aucune langue derrière un paywall.** Le tier Pro est
un sur-ensemble de features payantes ; il ne restreint aucune langue.

## Composants tiers embarqués/liés (chemin distribué)

Tous **permissifs** — aucune dépendance GPL/LGPL dans l'artefact livré.

| Composant | Licence |
|---|---|
| Kokoro-82M (modèle) | Apache-2.0 |
| Moteurs de phonémisation (G2P) | MIT |
| ONNX Runtime | MIT |
| ICU4J (icu4j embarqué) | Unicode/ICU (permissive) |

Noms, auteurs, copyrights et dépôts des composants : [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
Version texte embarquée dans l'AAR (`assets/THIRD-PARTY-NOTICES.txt`, lisible au runtime
via `context.assets.open(...)` pour un écran « licences open source »).

## Obligations si vous redistribuez (Apache-2.0)

1. Fournir une copie de la licence Apache-2.0 ([LICENSE](LICENSE)).
2. Conserver les notices d'attribution ([THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) /
   `.txt`), notamment « **based on Kokoro-82M** ».
3. Signaler les fichiers modifiés.
4. Ne pas utiliser les marques des projets sources comme marque de votre produit.

## Garantie « zéro GPL »

Le chemin shippable (`:core` + `:free`) ne référence **aucune** dépendance GPL/LGPL. Un test
de garde échoue à la moindre contamination.
