# Extract pipeline (rules → rare LLM)

Retenqo turns raw text (Pulse OCR, WhatsApp/notif, media) into memory cards.

## Order

1. **Rules** (`RulesExtract`) — ₹ amounts, calendar due dates, “pay by / I’ll send / waiting”, `From Name:` who, [CardClassifier] type.
2. If confidence ≥ **0.7** → save card **without** loading Qwen.
3. Else → local Qwen JSON extract, then **merge** rule anchors (due / amount / who).

## Tuning

- Threshold: `RulesExtract` `needsLlm = confidence < 0.7f`
- Fedora prompt eval (optional): `./scripts/eval-fedora-text-models.sh` using existing pulse/ask fixtures under `eval-results/`

## Phone

Notif/Pulse workers still construct `LlmBridge`, but `ensureLoaded()` runs only when rules ask for LLM.
