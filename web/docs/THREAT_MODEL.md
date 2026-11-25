# Retenqo website and release synchronization threat model

**Reviewed:** 2026-11-25<br>
**Scope:** Static website, browser-delivered assets, published release metadata, and the local release synchronization tool. The Android application has separate security documentation and is outside this website threat model.

## Assets

- Integrity and availability of the official website and APK download link.
- Authenticity, package identity, version, size, digest, Android API/ABI metadata and signing identity of a stable APK.
- Integrity of the generated release metadata consumed by website pages.
- User trust in privacy, security, compatibility and product claims.
- Developer machine temporary download files and release synchronization credentials/environment.

## Trust boundaries

1. **Browser to static host:** visitors receive prebuilt HTML, CSS, JavaScript, images and generated JSON from the website host.
2. **Release tool to GitHub:** the sync script reads release API metadata and downloads an APK asset over HTTPS. API metadata is untrusted until cross-checked with the actual artifact.
3. **APK to Android SDK tools:** the downloaded file is parsed by `aapt` and verified by `apksigner`; parsing and tool output can fail and must not produce accepted metadata.
4. **Verified output to repository/build:** only fully verified release metadata is atomically written and then included in the static build.

## Threats and controls

| Threat                                                           | Control                                                                                                                             | Residual risk                                                                                                                    |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| GitHub release metadata advertises a different version/tag/asset | Strict tag, version, asset-count and filename checks; compare actual APK manifest version/package/API/ABI.                          | A compromised GitHub account can publish a malicious APK signed by the trusted key only if signing material is also compromised. |
| APK replaced or redirected to an untrusted host                  | HTTPS-only allowlisted hosts, final redirect host validation, streamed size cap, digest and actual size checks.                     | DNS/TLS/platform trust failures are delegated to the operating system and host infrastructure.                                   |
| APK signed by an unexpected key                                  | `apksigner verify` and exact certificate SHA-256 pin comparison.                                                                    | Key rotation requires a reviewed pin update; compromised trusted signing key defeats this control.                               |
| Malformed or oversized APK consumes disk/memory                  | Download is streamed to a bounded temporary file; APK parsing follows size validation; temporary directory is removed in `finally`. | Native/platform parser vulnerabilities remain possible; tooling should be kept updated.                                          |
| Failed sync destroys last good website metadata                  | Validate before atomic replacement; regression tests check failure preservation.                                                    | Filesystem failure during atomic replacement can still fail the sync; old file should remain available.                          |
| Personal information leaks to website analytics/third parties    | Static site has no analytics/tracking dependency; browser test checks no unexpected third-party requests.                           | External browser extensions, network operators and hosting access logs are outside page code control.                            |
| Script injection through the website                             | Self-hosted scripts only via CSP `script-src 'self'`; no backend or user-generated HTML rendering.                                  | `style-src` currently allows inline styles for existing markup. CSP headers must be checked on production.                       |
| False privacy/security or AI claims                              | Shared product configuration and evidence-led copy; privacy/security pages describe scope and limitations.                          | Android implementation can change; documentation needs release-by-release reconciliation.                                        |
| Stale or misleading search/social metadata                       | Canonicals, structured data, sitemap, robots and social preview are generated from site configuration.                              | Search engine indexing and social crawler rendering require external verification.                                               |

## Operational requirements

- Keep the trusted signing certificate fingerprint under review and never store private signing credentials in this repository.
- Do not replace generated release metadata by hand; run the verifier and inspect its reported artifact identity.
- Keep dependency lockfile changes reviewed and rerun `pnpm audit` periodically. A clean audit means no known advisories at query time, not that code is vulnerability-free.
- Verify production security headers, cache/compression behavior, 404 response, and Lighthouse metrics after deployment.
- Preserve last-known-valid release metadata when GitHub or artifact validation fails.
- Keep privacy and security copy synchronized with the Android implementation and clearly distinguish implemented controls from planned work.

## Out of scope

This document does not establish end-to-end Android database encryption, Android Keystore correctness, notification consent behavior, backup security, model safety, device integrity, GitHub account security, or Cloudflare account configuration. Those require separate implementation evidence and audits.
