# Retenqo

Retenqo is a local-first Android memory and follow-through assistant. It captures user-approved information, keeps source-backed memory cards, answers questions with citations, and helps track commitments.

## What works

- Capture notifications from seven user-configurable app groups after Notification access is granted; capture starts paused until the user enables sources.
- Capture a screen on demand with Android MediaProjection, import selected images/PDFs, or share text and files to Retenqo.
- Run OCR and rules-first extraction on-device; an imported/downloaded GGUF can provide local generation through the bundled llama.cpp CPU runtime.
- Search saved memory, ask grounded questions, review task suggestions, edit/dismiss/complete/snooze/reopen commitments, inspect their history, and receive due reminders.
- See a privacy-preserving count of confirmed follow-ups in the home-screen widget; it never displays card text.
- Export password-encrypted backups and additively restore them; older unencrypted JSON backups are still importable.

## Privacy and permissions

Inference and memory search are local after model setup. Internet access is used for the explicit model download. Notification access, photo access for optional background WhatsApp photo scanning (including Android 14's selected-photos mode), microphone, screen capture, and Accessibility context are separately user-controlled. Retenqo does not request Android all-files access. Models stay in app-scoped storage. Selected PDFs and photos are processed from picker/share URIs and copied only to bounded app-private cache while OCR runs.

The Accessibility service is optional and remains off until the user enables it in Android settings. When enabled, it keeps a bounded, short-lived text snapshot of visible external-app content for a user-requested Ask. Do not enable it unless this behavior is wanted.

## Build and verification

```bash
./scripts/build-mneme.sh
./scripts/test-mneme.sh
```

Requires JDK 21 and the Android SDK. The debug APK targets ARM64. Device measurements are single-device observations, not performance guarantees.

## Current evidence and gaps

- A Qwen3.5-0.8B Q4_0 GGUF was loaded and generated a short answer on a Samsung Galaxy M32 (Android 13), including while network connectivity was disabled. Its observed process memory was about 831 MB; this small model and one device run are not evidence of broad assistant quality or target-device performance.
- JVM tests cover extraction, due-date parsing, retrieval relevance, citation guarding, routing, and helper logic. Current build and unit-test results are recorded in the implementation status. Real-device capture, voice, task follow-through, backup round-trip, and Galaxy M34 behavior still need end-to-end validation.
- Retrieval uses lexical matching; a learned semantic embedding model is not integrated. Screen context is a bounded text snapshot, not general phone control. The local model is not a reliable open-ended autonomous planner.
- New backups use AES-256-GCM with a passphrase-derived key; the passphrase is never saved. Older JSON backups remain readable but are not encrypted. The GGUF model is not part of the backup.

See [implementation status](./docs/IMPLEMENTATION_STATUS.md), [audit](./docs/IMPLEMENTATION_AUDIT.md), [follow-through plan](./docs/FOLLOW_THROUGH_PLAN.md), and [offline model notes](./docs/OFFLINE_MODELS.md).
