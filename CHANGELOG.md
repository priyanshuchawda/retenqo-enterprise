# Changelog

## [v0.1.4](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.4) — 2026-11-25

- Fixed SQLCipher passphrase lifetime for pooled Room/WAL connections without disabling database encryption or replacing user data.
- Verified the signed ARM64 APK against the pinned publisher certificate and upgraded the existing production installation on Android 16 while preserving app data.
- Passed the SQLCipher instrumentation suite (6/6) and launched successfully on the connected Android 16 device.

## Unreleased

- Website verification and browser coverage expanded; see [website audit](web/docs/ENGINEERING_AUDIT.md).
- See [current verification evidence](docs/IMPLEMENTATION_STATUS.md) for the published release and source revision.

## [v0.1.3](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.3) — 2026-11-25

- Stable notification source identifiers, legacy queue normalization and revoked-source filtering.
- Retenqo launcher icon and improved release verification and privacy disclosures.
- Recorded 114 JVM tests and 5 instrumentation tests passed.

## [v0.1.2](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.2) — 2026-11-25

- SQLCipher encryption, Keystore-wrapped passphrase and plaintext database migration.
- Recorded 79 JVM tests and 5 instrumentation tests passed. Superseded by v0.1.3.

## [v0.1.1](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.1) and [v0.1.0](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.0) — 2026-11-25

Earlier engineering previews, superseded by stable releases. See release-specific notes.

See [verification evidence](docs/TESTING.md).
