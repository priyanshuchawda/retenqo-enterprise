# Release process

Retenqo v0.1.4 is the current stable APK release. This document records the manual release gate.

1. Select an approved commit; update version name/code and changelog. Confirm app identity, supported Android versions and architectures.
2. Run local JVM tests, lint, debug/release build and device tests. Verify permissions, migration, backup/restore, capture, reminders, AI behavior and accessibility. Record actual results and blockers.
3. Review dependency and model licenses, security findings and third-party notices. Verify pinned native sources and model provenance.
4. Build a release APK. Sign with the maintainer-controlled release key through the Android signing toolchain; keep secrets outside the repository and never publish unsigned/debug APKs as stable releases.
5. Verify the signed APK with `apksigner verify --verbose --print-certs Retenqo.apk`, inspect manifest/version/ABI and test installation/update. Generate SHA-256 with `sha256sum Retenqo.apk`.
6. Publish a versioned GitHub release with the APK, checksum, signing certificate fingerprint, compatibility, release notes and known limitations. Protect the signing key and publisher account.
7. Verify the public version-specific asset download and checksum. Update the official website only after release verification. Remove or clearly revoke unsafe artifacts; communicate corrective action and preserve version history.

Never claim an APK is signed, verified or downloadable before checking the actual published artifact.
