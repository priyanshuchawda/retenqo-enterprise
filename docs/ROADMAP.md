# Retenqo roadmap — issues & PRs

**Product:** On-device personal memory — WhatsApp, notifications, and Pulse become structured **cards**, then **brief / Ask / Due**.

Local open models at the core (Qwen + embeddings on phone; larger GGUF on Fedora / 16GB+ devices later).

---

## What we ship

1. Capture from WhatsApp, notifications, Pulse (and later share).
2. Turn events into cards: due, promise, waiting, fact, ask_for_you.
3. Morning / since-last-open brief (mostly without LLM).
4. Ask answers from retrieved cards only (local Qwen + embeddings).
5. Due reminders and optional watchers on card conditions.
6. Settings: which apps to capture, quiet hours.

**Out of scope:** a bot that taps through arbitrary apps for the user.

**Models:** Qwen on phone now; larger GGUF on Fedora / future 16GB+NPU devices without changing the product shape. Prefer retrieve-top-k cards over huge context windows.

---

## Epic tree (GitHub)

**Epic:** [#25](https://github.com/priyanshuchawda/mneme/issues/25)

| Priority | Issue | Title |
|----------|-------|--------|
| P0 | [#26](https://github.com/priyanshuchawda/mneme/issues/26) | Card schema + migrate notes |
| P0 | [#27](https://github.com/priyanshuchawda/mneme/issues/27) | Cheap extract → rare LLM |
| P0 | [#28](https://github.com/priyanshuchawda/mneme/issues/28) | Wire WA / notif / Pulse → cards |
| P0 | [#29](https://github.com/priyanshuchawda/mneme/issues/29) | Card actions (done / snooze / edit) |
| P0 | [#30](https://github.com/priyanshuchawda/mneme/issues/30) | Since-last-open + morning brief |
| P0 | [#31](https://github.com/priyanshuchawda/mneme/issues/31) | Ask over cards |
| P0 | [#32](https://github.com/priyanshuchawda/mneme/issues/32) | Notif allowlist + quiet hours |
| P1 | [#33](https://github.com/priyanshuchawda/mneme/issues/33) | Watchers on cards |
| P1 | [#34](https://github.com/priyanshuchawda/mneme/issues/34) | Share-sheet → card |
| P1 | [#35](https://github.com/priyanshuchawda/mneme/issues/35) | Export backup |
| P2 | [#36](https://github.com/priyanshuchawda/mneme/issues/36) | Deferred — limited app open from card |

Also: [#1](https://github.com/priyanshuchawda/mneme/issues/1) historical stack · [#6](https://github.com/priyanshuchawda/mneme/issues/6) voice · [#7](https://github.com/priyanshuchawda/mneme/issues/7) clipboard · [#8](https://github.com/priyanshuchawda/mneme/issues/8) CI.

---

## PR conventions

| Rule | Detail |
|------|--------|
| Branch | `feature/<issue-number>-short-slug` e.g. `feature/26-card-schema` |
| Size | One issue ≈ one PR |
| Title | `feat(#26): card schema + Room migration` |
| Test | `./scripts/test-mneme.sh` + phone install when UI |

### Build order

`#26 → #27 → #28 → #29 → #30 → #31 → #32` then `#33–#35`.

---

## Success demo

Airplane mode → cards from WA / notifs / Pulse → brief → Ask “what do I owe?” → mark done → optional watcher.
