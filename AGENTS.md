# Retenqo engineering instructions

These rules apply to human and AI-assisted changes. Read the nearest relevant project documentation and inspect the actual implementation before editing.

## Workflow

1. Inspect current git status, affected code, tests and existing issues. Preserve unrelated changes.
2. Define the issue, acceptance criteria and risk. Work on a focused branch; use small coherent commits.
3. Implement the simplest correct change. Avoid speculative architecture, unrelated rewrites and unnecessary dependencies.
4. Add regression tests and verify failure paths, not just happy paths.
5. Run applicable local checks: `git diff --check`, `./scripts/test-mneme.sh`, `./scripts/build-mneme.sh`, and `./gradlew :app:lintDebug` from android/mneme. Run instrumentation on a compatible device where needed. Record real results and unrun checks.
6. Open a PR linking the issue, summarize evidence and remaining risks, then obtain maintainer review. Never force-push, bypass checks or overwrite other changes.

## Kotlin and Android

- Prefer explicit domain types, small focused functions, immutable UI state, lifecycle-aware collection and dependency injection when useful.
- Preserve cancellation; avoid blocking the UI, unmanaged scopes and unbounded jobs, buffers or queues. Make native and Android resource cleanup deterministic and idempotent.
- Treat IPC, intents, URIs, notification content, OCR, model files and model outputs as untrusted. Validate inputs, cap sizes/time and handle errors without sensitive logging.
- Preserve consent, source allowlists, revocation behavior and human confirmation for consequential actions. Never broaden capture silently.
- Use Room migrations and transactions for related updates. Do not add destructive fallback or silently discard data. Test interrupted work, duplicates and migration paths.
- Keep deterministic rules where appropriate; validate model outputs and citations against real evidence. Never invent accuracy or performance results.
- Keep production and demo behavior separate. Use synthetic fixtures; never commit personal data, API keys or signing credentials.
- Verify accessibility semantics, contrast, text scaling and touch targets for UI changes.
- Pin compatible build/native dependencies, preserve upstream license notices and document dependency changes.

## Evidence and release

Update relevant docs with actual findings. Report exact commands and outcomes; a test source file is not proof of a passing test. Do not claim a feature is device-verified without executing it. Follow [release guidance](docs/RELEASE_PROCESS.md) and [security policy](SECURITY.md). CI workflows may run repository verification checks but must not publish releases or deploy without explicit maintainer authorization.
