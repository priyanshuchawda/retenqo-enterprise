# Security architecture and threat model

Retenqo uses permission-scoped capture, local inference, explicit user approval and protected persistent storage. This engineering threat model maps the implemented security controls and trust boundaries; it is not an independent security certification.

| Boundary | Implemented control |
| --- | --- |
| Capture consent | Seven opt-in notification source groups; capture starts paused; explicit Android permissions |
| Stored memory | SQLCipher Room; database key wrapped by Android Keystore; explicit schema migration |
| Database lifecycle | Retained SQLCipher-owned key for pooled connections; cleared on database close; fail-closed key recovery |
| Imported content | User-selected picker/share flows, bounded media intake and app-private temporary files |
| AI output | Rules-first extraction, citation identifier validation and human approval of completion suggestions |
| Backup exports | PBKDF2-HMAC-SHA256 (210,000 rounds), AES-256-GCM and passphrase confirmation |
| Distribution | APK package identity, size, digest and pinned publisher signer verification |
| Resource use | Bounded queue, capture and model-processing paths |

## Security scope and engineering priorities

Application-level encryption covers Room memory records and new encrypted backup exports. Pending notification text in app-private SharedPreferences is not application-encrypted; Android sandbox protection applies to those preferences. Legacy plaintext backup imports remain supported.

Further hardening includes encrypted pending-queue storage, consent revocation and deletion synchronization, imported-content validation, crash recovery, resource lifecycle and adversarial AI testing. Database key loss fails closed; encrypted backups support recovery. Citation identifiers and human approval do not establish factual correctness by themselves.

## Implementation evidence

See [architecture](ARCHITECTURE.md), [traceability](REQUIREMENTS_TRACEABILITY.md) and [testing](TESTING.md) for implementation and verification links. The v0.1.4 database regression and signed upgrade are recorded in the verification document. For responsible disclosure, see [security policy](../SECURITY.md).
