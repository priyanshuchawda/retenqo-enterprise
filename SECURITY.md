# Security policy

Retenqo handles sensitive personal context. We welcome responsible disclosure and prioritize vulnerabilities that affect user consent, confidentiality, integrity or safe software distribution.

## Supported versions

The latest published stable release is [v0.1.3](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.3). The subsequent SQLCipher pooled-connection fix has not yet shipped as a signed replacement. No guaranteed security support window is offered. Report issues affecting current main or any published release; include the commit or version. A fix in main does not imply that an installed APK has been updated.

## Reporting a vulnerability

Use [GitHub private vulnerability reporting](https://github.com/priyanshuchawda/retenqo-enterprise/security) if available. If private reporting is unavailable, contact the repository maintainer privately; do not publish sensitive details in an issue. Include a minimal synthetic reproduction, affected versions, prerequisites, impact and proposed mitigation if known. Never send actual personal messages, authentication secrets, signing keys or unredacted backups.

Please allow a reasonable opportunity to investigate and coordinate disclosure. Do not access other people's devices or data, perform destructive tests, or interfere with services. No response-time guarantee or bug bounty is currently offered.

## Scope

Android permission and intent boundaries; notification and screen capture; local storage and backups; native model parsing and inference; model and APK supply chain; source grounding; build and release integrity. See [threat model](docs/THREAT_MODEL.md) for known risks and planned mitigations.

## Handling

Maintain confidential triage, reproduce with synthetic data, assess impact, prepare a minimal fix and regression tests, verify affected release artifacts, and communicate affected versions and mitigations. Avoid claiming comprehensive security assurance without evidence.
