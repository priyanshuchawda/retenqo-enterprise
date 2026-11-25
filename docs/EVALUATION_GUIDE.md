# Engineering evaluation guide

Retenqo addresses fragmented personal context and forgotten commitments with a local-first memory and follow-through system. This guide covers all eleven criteria; the [traceability matrix](REQUIREMENTS_TRACEABILITY.md) links specific code and tests.

| Criterion | Engineering contribution |
| --- | --- |
| Innovation | Combines authorized capture, evidence-linked recall and human-approved commitment completion rather than merely generating chat responses. |
| Functionality | Notification capture, OCR, memory search, task review, reminders and encrypted backup paths. |
| Technical Implementation | Kotlin, Room/SQLCipher, Android Keystore, JNI/llama.cpp, WorkManager and verified static APK distribution. |
| UI/UX | Existing review, search and permission flows; responsive website and clear installation instructions. |
| Practical Impact | Helps users retrieve context and recognize responsibilities without requiring cloud AI. |
| Code Quality | Typed card models, explicit migrations, focused processing classes, Zod schemas and reusable components. |
| Security | Encrypted database and backups, explicit capture consent, pinned APK signer and restrictive site headers. |
| Efficiency | Deterministic extraction, quantized local inference, bounded workloads and small static web scripts. |
| Testing | Recorded Android JVM and device tests, pooled-connection regression, web browser coverage and historical AI experiments. |
| Accessibility | Existing labeled controls, semantic HTML, keyboard controls and theme-aware Chromium axe checks. |
| Problem Statement Alignment | Capture → remember → retrieve → follow through, with local processing and user control. |

## Illustrative workflow

An authorized message asks for slides by Friday. The processing pipeline produces a reviewable commitment. The user confirms it, retrieves its context through search or Ask, receives a reminder and can review later completion evidence. This is a representative scenario, not a claim of an executed full device workflow.

## Review order

1. [Architecture](ARCHITECTURE.md) — system and trust boundaries.
2. [Traceability](REQUIREMENTS_TRACEABILITY.md) — implementation and test references.
3. [Testing](TESTING.md) — results with release/revision scope.
4. [AI implementation](AI_IMPLEMENTATION.md) and [research](RESEARCH_INDEX.md) — algorithms and experimental rationale.
5. [Threat model](THREAT_MODEL.md) and [performance](PERFORMANCE.md) — engineering trade-offs.

The current source includes the post-v0.1.3 SQLCipher fix. The published APK and source verification are separate; remaining verification is recorded in [implementation status](IMPLEMENTATION_STATUS.md).
