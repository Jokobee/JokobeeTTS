# Raw Kokoro voicepacks (`.pt`)

`ff_marine.pt` is the same **Marine** voice bundled in the AAR (`ff_marine.bin`,
see `VOICES-GUIDE.md`), in the raw PyTorch tensor format `(510, 256)` used
directly by Kokoro-82M / `kokoro-onnx` — no JokobeeTTS SDK required. Works on
any platform Kokoro-82M runs on (Android, iOS, desktop, server).

```python
import torch
style = torch.load("ff_marine.pt", weights_only=True)  # (510, 256) float32
```

Use `ff_marine.bin` (flat float32, no header, `510*256*4` bytes) if you're
integrating with JokobeeTTS directly instead — `Voice.of(id, lang, bytes)`
expects exactly that format and rejects `.pt` bytes with an `"invalid size"`
error (523,894 bytes vs. the 522,240 expected).

License: custom voice trained by Jokobee on public-domain audio (LibriVox) —
not one of the original 37 Kokoro-82M official voices. Free to use.
