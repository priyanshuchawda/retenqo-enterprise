# Retenqo — Private personal intelligence

**Remember what matters. Follow through. Keep personal context local.**

Retenqo is a local-first Android AI memory and commitment assistant. It turns authorized phone context into searchable memories, source-linked answers and reviewable commitments, helping people recover important information and act on deadlines without sending their prompts or memories to a cloud AI service.

[Official website](https://retenqo.pages.dev) · [Download](https://retenqo.pages.dev/download) · [Evaluation guide](docs/EVALUATION_GUIDE.md) · [Architecture](docs/ARCHITECTURE.md) · [Verification](docs/TESTING.md)

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> **Latest stable:** [Retenqo v0.1.4 for Android 13+ (ARM64)](https://github.com/priyanshuchawda/retenqo-enterprise/releases/latest). It includes the SQLCipher pooled-connection fix, verified against the pinned publisher certificate and installed as an in-place upgrade on Android 16.

## Engineering highlights

| Capability | Engineering achievement |
| --- | --- |
| Local intelligence | Quantized GGUF inference via llama.cpp; rules-first extraction and lexical retrieval |
| Contextual capture | Seven opt-in notification source groups; explicit screen and selected-media intake |
| Commitment lifecycle | Review, source evidence, reminders, completion suggestions and decision history |
| Protected persistence | SQLCipher Room database, Android Keystore-backed key protection, explicit migrations |
| Recovery | Passphrase-derived AES-256-GCM backup exports |
| Trusted distribution | Static Astro website; streamed APK digest, package identity and pinned signer verification |

## Verification at a glance

| Scope | Recorded result |
| --- | --- |
| Published Android [v0.1.3](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.3) | 114 JVM tests passed; 5 device tests passed; lint 0 errors (34 warnings, 1 hint) |
| Subsequent [database fix](https://github.com/priyanshuchawda/retenqo-enterprise/commit/3edfbc0cff94d360d4127b92948309e5e656a686) | 6 instrumentation tests passed on Android 16; concurrent encrypted connection regression and debug launch |
| Website [verification baseline](https://github.com/priyanshuchawda/retenqo-enterprise/commit/7a0ef6ec48cba2077486db7baa202cdc7dffb079) | 25 unit tests, 100 browser tests passed (68 skipped); dependency audit reported no known vulnerabilities |
| Production Lighthouse, one mobile-simulated run | Performance 92, accessibility 100, best practices 100, SEO 100; LCP 1.263 s, CLS 0 |

These results refer to distinct recorded revisions, not a new run of the documentation revision. See [verification scope](docs/TESTING.md).

## Problem and solution

Important information is scattered across notifications, conversations, screenshots, documents and personal notes. Finding the original message is often easier than remembering the responsibility it contained. Retenqo connects contextual capture, private memory, evidence-aware recall and human-controlled follow-through in the personal productivity / on-device AI vertical.

Unlike an autonomous phone agent, Retenqo uses supported Android APIs, explicit permissions and user approval of uncertain task updates.

## How it works

```mermaid
flowchart TD
 A[Authorized phone context] --> B[Bounded preprocessing and OCR]
 B --> C[Deterministic extraction]
 C --> D{Model needed and available?}
 D -->|Yes| E[Local GGUF inference]
 D -->|No| F[Rule-based candidate]
 E --> G[Memory or reviewable commitment]
 F --> G
 G --> H[SQLCipher Room database]
 H --> I[Search and Ask]
 H --> J[Review and reminders]
 J --> K[User-confirmed updates and history]
 K --> H
```

| Capability | Current implementation |
| --- | --- |
| Capture | Configurable notification groups, user-initiated MediaProjection, optional bounded accessibility context |
| Selected media | System picker and share targets; on-device OCR |
| Recall | Lexical retrieval over bounded recent candidates, evidence-linked answers, general local questions |
| Commitments | Review, accept, dismiss, edit, complete, snooze, reopen; completion suggestions |
| Reminders and widget | Local reminders and a count of confirmed follow-ups |
| Voice | Whisper bridge and Android TTS; speech-model provisioning and full device workflow verification remain in scope |
| Backup | Encrypted exports and additive restore, including legacy plaintext imports |

Example: an authorized message asks for slides by Friday. Retenqo can produce a reviewable commitment, preserve its source, support a reminder and later completion review. This illustrates the intended workflow; full end-to-end device verification is tracked in [implementation status](docs/IMPLEMENTATION_STATUS.md).

## Technology and design

| Layer | Technology |
| --- | --- |
| Android | Kotlin, Compose, Material 3, ViewModel, coroutines, Flow |
| Persistence | Room / SQLCipher, Android Keystore, schema migrations |
| Processing | WorkManager, deterministic extraction, ML Kit OCR |
| AI | llama.cpp, JNI, GGUF, whisper.cpp and Android TTS |
| Toolchain | Gradle Kotlin DSL, JDK 21, Android SDK 36, NDK |
| Website | Astro static output, TypeScript, Tailwind CSS v4, Zod, Vitest, Playwright |

Recommended language model: Qwen3.5-0.8B Q4_0, a separate 563,036,064-byte download. See [model setup](docs/OFFLINE_MODELS.md) and [AI implementation](docs/AI_IMPLEMENTATION.md).

## Download and installation

The latest published stable binary is [v0.1.3](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.3), version code 4, Android 13+, ARM64, 39,374,311 bytes. The pooled-connection database fix is in subsequent source and has not yet been published as a signed replacement. Affected devices require verification of the fixed build before relying on it.

Use the [download page](https://retenqo.pages.dev/download) for checksum and signer details and the [installation guide](https://retenqo.pages.dev/installation). Keep Android security protections enabled and back up personal data before updating.

## Privacy and security

Local inference does not require cloud AI endpoints. Notification capture starts paused; source groups are opt-in. The core database is encrypted at rest and new backup exports are authenticated and encrypted. Pending notification text is app-private but not application-encrypted. See [threat model](docs/THREAT_MODEL.md), [security policy](SECURITY.md) and [current status](docs/IMPLEMENTATION_STATUS.md).

## Build from source

Prerequisites: Git, Bash (Linux/WSL) or PowerShell on Windows, JDK 21, Android SDK platform 36, NDK 28.2.13676358 and CMake 4.1.2.

```bash
git clone https://github.com/priyanshuchawda/retenqo-enterprise.git
cd retenqo-enterprise
./scripts/build-mneme.sh
```

Windows: `./scripts/build-mneme.ps1 -Task verify` or `-Task assembleDebug -Install` with an authorized connected device. Debug and release application identities differ. See [release process](docs/RELEASE_PROCESS.md).

## Engineering documentation

| Document | Purpose |
| --- | --- |
| [Evaluation guide](docs/EVALUATION_GUIDE.md) | All eleven criteria and evidence |
| [Requirements traceability](docs/REQUIREMENTS_TRACEABILITY.md) | Exact implementation and test references |
| [Architecture](docs/ARCHITECTURE.md) | Component relationships, data flow and trust boundaries |
| [AI implementation](docs/AI_IMPLEMENTATION.md) | Extraction, retrieval, inference and research |
| [Testing](docs/TESTING.md) | Revision-specific results |
| [Performance](docs/PERFORMANCE.md) | Resource bounds and recorded measurements |
| [Accessibility](docs/ACCESSIBILITY.md) | Implemented measures and verification |
| [Implementation status](docs/IMPLEMENTATION_STATUS.md) | Current boundaries and historical milestones |
| [Website engineering audit](web/docs/ENGINEERING_AUDIT.md) | Distribution, browser coverage and production measurements |
| [Research index](docs/RESEARCH_INDEX.md) | Historical experiments and design evolution |
| [Changelog](CHANGELOG.md) | Releases and unreleased changes |

Source: [Android](android/mneme/), [website](web/), [build helpers](scripts/), [research artifacts](eval-results/). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md). License: [Apache 2.0](LICENSE); [third-party notices](docs/THIRD_PARTY_NOTICES.md).
