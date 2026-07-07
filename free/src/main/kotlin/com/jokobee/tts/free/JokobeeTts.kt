package com.jokobee.tts.free

/**
 * :free — API publique + normaliseurs (tier gratuit, Maven Central).
 *
 * À porter ici (voir SESSION-FINAL.md §1) :
 *  - `Normalizer` (base)     ← tts_normalize/core.py
 *  - 10 locales Fr..Ko       ← tts_normalize/{fr,en,…,ko}.py
 *  - `FrHomographs`          ← homographs_fr.py
 *  - `VoiceCatalog`          ← tts_voices/registry.py
 *  - `IcuVerbalizer`         ← android.icu.text.RuleBasedNumberFormat
 *
 * Dépendances shippables : android.icu (plateforme) + CharsiuG2P (MIT) +
 * ONNX Runtime (MIT). AUCUNE GPL/LGPL. Placeholder — squelette de build P0.
 */
public object JokobeeTts {
    public const val VERSION: String = "0.1.0"
}
