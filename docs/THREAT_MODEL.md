# Threat model

Scope: Android application, captured data, local model/runtime, backups, source repository and APK distribution. This is a qualitative threat model, not a completed penetration test or OWASP certification.

## Assets and trust boundaries

Personal memories, notifications, screen/voice content, commitment integrity, database, backup passphrases, model files, signing keys and release artifacts. Trust boundaries include Android IPC and permissions, imported files, native parsers, model-generated output, device storage and release publishing.

| Threat | Current control or context | Required verification / improvement |
| --- | --- | --- |
| Overbroad capture | Source controls and explicit platform permissions | Recheck consent at processing time, revocation and sensitive-content tests |
| Sensitive data exposure | SQLCipher Room database, Android Keystore-wrapped database key, encrypted new backup exports and Android sandbox | Encrypt pending notification queue/preferences; audit logs and lock-screen output |
| Malicious intents or files | Picker/share and bounded file intake | Validate schemes, MIME, decoded dimensions, native parser and resource limits |
| Prompt injection / false evidence | Rules-first extraction and citation guard | Treat content as data, verify cited IDs and supporting evidence, adversarial evaluation |
| Lost or duplicate memories | Queue and event identity logic | Durable atomic ingestion, crash recovery, backpressure and idempotency tests |
| Incorrect task closure | User confirmation required for suggestions | Test invalid, conflicting and stale suggestions |
| Backup tampering | AES-GCM and passphrase-derived key | Validate schema and limits; full device restore and interruption tests |
| Compromised model/APK | Pinned recommended model checksum; website sync verifies APK identity, digest, size and pinned signer | Verify dependencies and signed replacement release |
| Resource exhaustion | Some bounded background and intake paths | Measure memory, thermal, cancellation and lifecycle behavior |
| Demo state in production | Debug-only entry-point guard | Isolate release behavior and verify packaged output |

## Residual risk and verification

Local processing does not eliminate data exposure, malicious inputs or compromised-device risk. Room is encrypted at rest, but notification queue contents in preferences are not application-encrypted. Loss of the Android Keystore key makes the database unavailable; the app fails closed and users should maintain encrypted backups. Human review cannot guarantee AI factual accuracy. Maintain synthetic security regression tests and evidence for each mitigation. Review this document when a trust boundary changes.

See [security policy](../SECURITY.md) and [implementation audit](IMPLEMENTATION_AUDIT.md).
