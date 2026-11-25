# Contributing to Retenqo

Thank you for helping build a local-first Android memory and follow-through assistant. Contributions of code, tests, accessibility feedback, documentation and reproducible evaluations are welcome.

## Getting started

Read the [README](README.md), [architecture](docs/ARCHITECTURE.md), [implementation status](docs/IMPLEMENTATION_STATUS.md), [security policy](SECURITY.md) and [agent guidance](AGENTS.md). Follow the [code of conduct](CODE_OF_CONDUCT.md). Look for an existing issue before starting work.

For a new bug, include a minimal synthetic reproduction, Android version, device architecture, expected/actual behavior and redacted diagnostics. For a feature, explain the problem, alternatives, privacy implications and measurable acceptance criteria. Discuss major changes before investing in implementation.

## Development

Use JDK 21 and the Android SDK/NDK configured by the project. Fork the repository, create a focused branch from main, and build and test locally:

```bash
./scripts/build-mneme.sh
./scripts/test-mneme.sh
cd android/mneme
./gradlew :app:lintDebug :app:assembleDebug
# With a compatible authorized device:
./gradlew :app:connectedDebugAndroidTest
```

Use synthetic fixtures, never personal notifications, passwords, private screenshots or memory databases. Keep the diff small and respect unrelated work. Do not introduce a cloud dependency, broaden permissions, change licensing or introduce a destructive migration without prior discussion.

## Quality gates

- Use idiomatic Kotlin, immutable state where practical, structured concurrency, explicit cancellation and bounded resource use.
- Write regression tests for fixes and boundary/failure cases; test affected flows on a physical device where platform behavior matters.
- Preserve database compatibility and validate migrations, backup/restore and permission revocation when affected.
- Explain accessibility, privacy, performance and dependency impacts. Do not weaken checks merely to obtain passing results.
- Run `git diff --check`, relevant tests, lint and builds; report commands, outcomes, skipped checks and limitations. There is no automated CI gate at present.
- Keep documentation and verified claims synchronized with behavior.

## Pull requests

Link the issue, explain the change and tradeoffs, include test evidence and note risks, migration/rollback implications and screenshots for UI changes (synthetic data only). Use small commits with descriptive messages. Maintainers review and merge after verification; do not assume approval from a successful build alone.

## Licensing

By intentionally submitting a contribution for inclusion, you agree to license it under [Apache License 2.0](LICENSE) unless explicitly agreed otherwise, and confirm that you have the right to submit it. Preserve all applicable third-party notices and identify any imported code/assets and their licenses. See [third-party notices](docs/THIRD_PARTY_NOTICES.md).
