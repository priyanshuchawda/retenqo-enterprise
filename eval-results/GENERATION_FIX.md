# Fedora generation fix (2026-08-23)

> Historical development record. Current implementation and release evidence: [engineering status](../docs/IMPLEMENTATION_STATUS.md), [verification](../docs/TESTING.md), [research index](../docs/RESEARCH_INDEX.md).

**Canonical results (what each model answered):** [REPORT.md](./REPORT.md)

## Why blank / garbage before

Not an app Kotlin bug. Three separate issues:

| What we tried | File size | Arch | Symptom | Root cause |
|---------------|-----------|------|---------|------------|
| Qwen3-4B-Instruct-2507 Q4 | exact HF match (aria2 leftover only) | `qwen3` | newlines / `77777` / `is is is` | Degenerate decode on this llama.cpp host build — **not** “4B too dumb” |
| Qwen2.5-3B-Instruct Q4 | exact bartowski size | `qwen2` | garbage tokens | Same class of bad decode |
| Qwen3.5-2B (phone) | OK | `qwen35` | real answers | Works |
| Qwen3.5-4B (new) | exact HF `2740937888` | `qwen35` | real answers | Works with correct prompt mode |

Stale `.aria2` files made downloads look incomplete; sizes already matched Hugging Face.

## Why Qwen3.5 looked empty on Ask

Default **conversation mode** + thinking: model fills `<think>…</think>` and burns `-n` tokens. With `-n 80` you often see only blanks after stripping an unfinished think block.

`--reasoning off` / `--reasoning-budget 0` alone were **not** reliable here without jinja (jinja crashed this binary). `/no_think` alone also insufficient.

## Fix (Fedora)

Use **`-no-cnv`** and end the assistant turn with a **closed empty think**:

```text
<|im_start|>assistant
<think>
</think>

```

Then the model continues with the visible answer.

Verified on `models/eval/Qwen3.5-4B-Q4_K_M.gguf` (from [unsloth/Qwen3.5-4B-GGUF](https://huggingface.co/unsloth/Qwen3.5-4B-GGUF)):

```text
Your airport Uber cost ₹620. [1]
```

Script: `scripts/eval-fedora-text-models.sh`

## Qwen3-VL (vision collection)

Different stack: need **LLM GGUF + `mmproj`** and `llama-mtmd-cli` / server with `--image`. Text-only `llama-completion` cannot “see”. Prefer ML Kit OCR → text LLM for Mneme unless we add mtmd.

## Practical stack

- Redmi 6 GB: keep **Qwen3.5-2B**
- Fedora A/B + iQOO headroom: **Qwen3.5-4B** Q4 (same `qwen35` family as 2B)
- Avoid relying on Qwen2.5-3B / Qwen3-4B-Instruct on this host until decode is proven clean
