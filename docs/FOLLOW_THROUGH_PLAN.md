# Retenqo personal follow-through plan

## Product outcome

Retenqo helps a person keep commitments made and received across the phone. It notices actionable context from sources the user enabled, turns it into a traceable commitment, keeps the commitment current as new evidence arrives, and helps complete it with a reliable action. It runs inference locally after model setup.

The product is not a general-purpose phone autopilot. It should use supported Android APIs, explain what it read, ask before consequential actions, and report only outcomes it can verify.

## Core loop

1. **Observe** — Receive notification text, a user-approved screen capture, shared text/files, selected media, voice, and Retenqo actions. Every source has a permission state and source identity.
2. **Interpret** — Use cheap deterministic extraction first and the local model for ambiguous meaning. Produce typed candidates: promise, request, waiting-on, deadline, reminder, completion evidence, or ordinary memory. Include source, confidence, dates, parties, and supporting text.
3. **Review** — Show candidates that need confirmation. Never silently turn uncertain language into a task or mark a task complete from a weak match.
4. **Track** — Keep an open commitment linked to its originating evidence and any later evidence. New evidence may suggest an update or completion; the person can accept or dismiss it.
5. **Act** — Offer a small set of typed local tools (create/edit/complete/snooze task, set reminder, open app, draft/share content). Require confirmation for external or consequential actions and verify the result.
6. **Explain** — Every card and action can show its source, why Retenqo surfaced it, current state, and what Retenqo actually did.

## Differentiator to prove

The differentiator is long-term, private follow-through across authorized phone context: a message creates a commitment, later context can update it, and Retenqo can help close it. Screen understanding and app actions by themselves are not differentiators.

The first proof scenario should be fully reproducible on a physical phone:

1. A test notification says, “Can you send me the slides by Friday?”
2. Retenqo creates a reviewable request with the app/source and extracted due date.
3. The user accepts it; it appears in the open commitments view with a reminder.
4. A later test notification says the recipient received the slides. Retenqo suggests a completion match, showing both pieces of evidence.
5. The user confirms; the task is marked done and the suggestion is retained in history.

No completion should be automatic in this first milestone. A wrong completion is more costly than asking once.

## Implementation phases

### Phase A — Reliable commitment records

- [ ] Complete provenance across every capture source. Cards now retain notification source labels, capture time, bounded evidence, and a hashed Android notification event key; Pulse/media/voice still lack stable upstream IDs.
- [x] Add typed follow-through suggestions linked to an open commitment, with source, bounded evidence text, confidence, state, and Room migration. User confirmation is required to mark done.
- [x] Build duplicate detection around stable source/event identifiers where available, with content/time fallback. Notifications now carry a hashed event key through the durable queue and enforce uniqueness on cards; completion suggestions still dedupe by evidence hash.
- Keep existing cards readable and preserve existing user data during migration.

### Phase B — Review and current state

- [x] Add review state and accept/dismiss controls for captured actionable cards. Pending cards do not count as tasks, Ask retrieval results, or due reminders.
- [x] Add user-facing per-source notification controls for WhatsApp, Gmail, Messages, Telegram, Outlook, Calendar, and Slack; ignore unknown notification apps by default.
- [x] Add a dedicated review filter for new candidates and suggested completions in the Tasks workbench, backed by durable decision history.
- [x] Expose active, due-soon, and completed commitments as task-focused views.
- [x] Allow accept, edit, dismiss, done, reopen, and snooze; record source evidence and each decision in the commitment timeline.
- [x] Filter notification sources in Settings and make capture state visible.

### Phase C — Model planner and local tools

- Replace isolated natural-language routes with a planner that returns a constrained typed action proposal.
- Validate every proposal against a local tool registry and current permission state.
- Require confirmation before sending, deleting, purchasing, changing device settings, or any other externally visible/high-impact action.
- Execute with Android intents/APIs; check the returned result and show success only when verified.
- Keep deterministic handlers for simple, time-critical actions and graceful behavior when no model is installed.

### Phase D — More context and durable operation

- [x] Add opt-in, one-question screen context from a user-enabled accessibility service; keep snapshots bounded and in memory only. On-device extraction/model verification remains pending.
- Add share intake and SAF/Photo Picker paths; remove broad storage access where possible.
- Improve notification, document, screenshot, and voice ingestion with cancellable work and battery-aware scheduling.
- Verify boot recovery, reminders, offline voice/TTS, source controls, and permission recovery on the phone.
- Add export/restore, retention controls, and deletion by source/person/date.

### Phase E — Quality and release

- Build a labeled evaluation set for request/promise/deadline/completion extraction and false completion matches.
- Measure latency, peak memory, thermal behavior, battery cost, and long-session stability on the target phone(s).
- Run end-to-end scenarios on supported Android versions, accessibility review, migration/restore checks, and offline checks.
- Remove or label all demo data; document supported devices, permissions, model provenance/license, and known gaps.

## Current baseline

- Local Qwen3.5-0.8B inference has been observed working on a Samsung Galaxy M32 (SM-M325F, Android 13), including a prior run with network connectivity disabled.
- Notifications are read through `NotificationListenerService` after the user grants access; Settings has per-source allowlists for seven supported apps.
- Pulse uses user-started MediaProjection, OCR, rules-first/model-assisted extraction, and saves a note card.
- Voice routes through fixed patterns; reminders and basic card status actions exist.
- Captured actionable cards have a review state, and model-assisted completion suggestions retain evidence until the user confirms. Stable source-event identity and a dedicated inbox/history remain.
- Ask can include a recent visible-screen snapshot for one question after explicit opt-in. The M32 accessibility service remains disabled until the user enables it; capture accuracy and on-device context reasoning are unverified.
- The connected phone is a Galaxy M32, not the Redmi 13 or Galaxy M34 mentioned in older notes. Target-device performance and broad end-to-end flows are unverified.
- The working tree already contains substantial uncommitted implementation changes. Preserve them and review before any reset, cleanup, or commit.

## Definition of done

Retenqo is ready for a first public/pilot release when Phases A–E are implemented to the supported-device scope; the proof scenario works end to end; uncertain extraction and completion are reviewable; actions are permission-aware and verified; offline use is honest and tested; user data survives app upgrades and can be exported or deleted; and the documentation matches observed behavior.
