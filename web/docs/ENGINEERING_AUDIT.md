# Website engineering audit

## Current release distribution

[Retenqo v0.1.4](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.4) is the signed stable Android release. [Generated release metadata](../src/data/release.generated.json) was refreshed on 25 November 2026 for version code 5, package `com.mneme.app`, ARM64, 39,374,307 bytes and the published SHA-256. The pinned single signer was verified in the release record. The detailed website verification results below describe the earlier v0.1.3-era baseline; they remain useful evidence of the distribution architecture and are not a fresh browser or Lighthouse run for v0.1.4.

**Review date:** 2026-11-25<br>
**Scope:** Static Astro website, release metadata and APK synchronization scripts, accessibility, browser behavior, security headers, and search metadata.<br>
**Status key:** **Implemented** means a code change exists; **Verified** means a named automated or live check passed; **Pending** means evidence is still required. Targets are not presented as achieved results.

This document tracks every website finding and improvement ID supplied for the audit. It is a status ledger, not a claim that every item has been independently certified. Exact command results should be appended after the final run in the release commit.

## Website code quality and functionality

| ID     | Status            | Evidence / remaining work                                                                                                                                            |
| ------ | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WEB-01 | Implemented       | Theme script applies the initial theme immediately and binds the selector after DOM readiness.                                                                       |
| WEB-02 | Implemented       | System, light, and dark modes share CSS tokens and the selector. Browser theme tests cover routes and persistence.                                                   |
| WEB-03 | Implemented       | Theme storage reads/writes are guarded; browser test simulates storage exceptions.                                                                                   |
| WEB-04 | Verified          | Release synchronization inspects the downloaded APK's manifest metadata and signer; `pnpm release:sync` validated GitHub release v0.1.3 at the recorded baseline.                     |
| WEB-05 | Implemented       | Synchronization writes metadata atomically only after validation; failure regression tests assert the previous metadata is preserved.                                |
| WEB-06 | Implemented       | Product and release constants live in `src/config/product.ts`.                                                                                                       |
| WEB-07 | Implemented       | Shared configuration replaces repeated app ID, API, architecture, model size and trusted release values. Continue consolidating only when a real duplicate is found. |
| WEB-08 | Verified          | Strict TypeScript checks are part of `pnpm verify`; avoid unsafe assertions in new code.                                                                             |
| WEB-09 | Verified          | Removed unused `lucide-astro`; lint and type checks cover remaining source.                                                                                          |
| WEB-10 | Pending           | macOS checksum command has not been exercised on macOS; verify platform-specific shell syntax before claiming portability.                                           |
| WEB-11 | Implemented       | Release sync uses typed validation and explicit failure paths, preserving the last valid metadata.                                                                   |
| WEB-12 | Verified          | Playwright checks generated internal links and external link schemes. A full external uptime audit remains network-dependent.                                        |
| WEB-13 | Implemented       | Astro routes, data and shared UI components remain separated by page/layout/component responsibility.                                                                |
| WEB-14 | Verified | `pnpm verify` runs formatting, ESLint, Astro/TypeScript checks, unit tests, and production build. Recorded baseline output below.                  |

## Website security

| ID    | Status                         | Evidence / remaining work                                                                                                                                                     |
| ----- | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WS-01 | Verified                       | `apksigner verify` is run against the actual downloaded APK during synchronization.                                                                                           |
| WS-02 | Verified                       | APK certificate SHA-256 is compared with the pinned publisher fingerprint in `src/config/product.ts`.                                                                         |
| WS-03 | Verified                       | Actual APK package name must equal the configured application ID.                                                                                                             |
| WS-04 | Implemented                    | Response body is streamed to a temporary file and aborted when configured maximum size is exceeded.                                                                           |
| WS-05 | Verified                       | Actual streamed byte count is compared with release metadata.                                                                                                                 |
| WS-06 | Implemented                    | Tag, asset count/name, URL protocol/host, and final redirect destination are validated.                                                                                       |
| WS-07 | Verified                       | Metadata is replaced only after all artifact checks succeed; failure-path unit tests cover preservation.                                                                      |
| WS-08 | Verified                       | `pnpm audit` reported no known vulnerabilities after dependency updates on 2026-11-25. This is a point-in-time registry audit, not a guarantee against unknown issues.        |
| WS-09 | Verified live                  | On 2026-11-25, production returned CSP (`script-src 'self'`), HSTS, `nosniff`, `DENY`, `no-referrer`, and restrictive permissions policy headers.                             |
| WS-10 | Verified with scope            | Browser test observes same-origin page requests and checks that page visits make no third-party requests. This does not cover all browser extensions or network environments. |
| WS-11 | Implemented                    | CSP `script-src` allows self-hosted scripts only; `style-src` retains inline allowance required by current generated markup. Reassess if markup changes.                      |
| WS-12 | Implemented                    | Temporary download directory is removed in `finally`, including validation failure paths.                                                                                     |
| WS-13 | Implemented                    | Unit cases cover malformed metadata, package/version/size/digest/tag/signer mismatches and failed release preservation. Malformed APK byte corpus testing remains limited.    |
| WS-14 | Pending synchronization review | Privacy and security pages are updated to describe current application boundaries; confirm against the Android implementation before each release.                            |
| WS-15 | Pending artifact scan          | No dedicated production artifact secret/sensitive-string scan has been run for this audit.                                                                                    |

## Website UI, UX and product presentation

| ID     | Status              | Evidence / remaining work                                                                                                                             |
| ------ | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| WUX-01 | Implemented         | Homepage hero states the lost-context/forgotten-commitment problem and product response.                                                              |
| WUX-02 | Implemented         | Feature content explains contextual capture and follow-through compared with ordinary notes/reminders.                                                |
| WUX-03 | Implemented         | Download CTA shows current version and APK size from generated metadata.                                                                              |
| WUX-04 | Implemented         | Primary download journey is surfaced on the homepage and download page with installation steps. Manual usability study remains pending.               |
| WUX-05 | Implemented         | Shared tokens, spacing and typography are used across layouts and components.                                                                         |
| WUX-06 | Implemented         | Feature icons and reusable components follow the site's existing icon approach; inspect new additions during review.                                  |
| WUX-07 | Verified with scope | Playwright viewports cover 320, 768 and 1920 CSS px for overflow and navigation. Visual QA on physical devices remains pending.                       |
| WUX-08 | Implemented         | Student, professional and freelancer examples are present.                                                                                            |
| WUX-09 | Implemented         | Feature descriptions emphasize user outcomes alongside implementation details.                                                                        |
| WUX-10 | Implemented         | Illustrative use cases and synthetic interface data are explicitly labeled.                                                                           |
| WUX-11 | Partial             | Website documents historical Galaxy M32 model-memory observation (~831 MB PSS) with caveat; a reproducible current benchmark report is still pending. |
| WUX-12 | Implemented         | Installation page explains APK/model separation and setup.                                                                                            |
| WUX-13 | Implemented         | APK size, optional model download size and observed runtime memory are explained separately.                                                          |
| WUX-14 | Pending             | No interactive synthetic capture-to-commitment demo is implemented. This remains optional.                                                            |

## Website efficiency

| ID    | Status              | Evidence / remaining work                                                                                                                                                           |
| ----- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WP-01 | Measured            | Lighthouse 13.6.0 mobile-simulated run against production: performance 92, accessibility 100, SEO 100, best practices 100. Single run; repeat before treating as a stable baseline. |
| WP-02 | Verified with scope | Build output and browser request tests cover current script loading; recheck after future additions.                                                                                |
| WP-03 | Implemented         | Social preview is a compact raster PNG; review asset size in final build.                                                                                                           |
| WP-04 | Pending measurement | No bundle-size budget comparison has been recorded.                                                                                                                                 |
| WP-05 | Pending measurement | Inspect font requests and rendering in Lighthouse/network trace.                                                                                                                    |
| WP-06 | Implemented         | Theme is applied before CSS paints; browser viewport/theme checks cover the main path. CLS still needs production measurement.                                                      |
| WP-07 | Partial             | Production cache header is `public, max-age=0, must-revalidate`. Compression behavior was not established by the captured response; verify separately.                              |
| WP-08 | Measured, limited   | Lighthouse default mobile emulation (`simulate`) ran against production. This is not a separate recorded slow-network profile.                                                      |
| WP-09 | Verified with scope | Current browser test asserts no unexpected third-party page requests.                                                                                                               |
| WP-10 | Measured            | One mobile-simulated Lighthouse 13.6.0 run: FCP 1.3 s, LCP 1.263 s, CLS 0, TBT 244 ms, Speed Index 4.971 s. No INP measurement was made.                                            |

**Proposed targets and current evidence:** performance ≥95 (current single run 92; target missed); accessibility 100 (single run 100); LCP ≤2.5 s (single run 1.263 s); CLS ≤0.1 (single run 0); INP ≤200 ms (not measured); unnecessary external requests 0 (browser test observed none). Scores and timings are a single Lighthouse run on 2026-11-25 using Lighthouse 13.6.0 with default mobile emulation and simulated throttling against `https://retenqo.pages.dev/`; they are not repeatable performance guarantees.

## Website testing and accessibility

| ID    | Status                  | Evidence / remaining work                                                                                                                                                                               |
| ----- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| WT-01 | Implemented             | Browser tests cover theme switching and persistence.                                                                                                                                                    |
| WT-02 | Implemented             | Copy-control tests cover checksum/verification copy behavior.                                                                                                                                           |
| WT-03 | Implemented             | Unit tests cover wrong signer/package identities and malformed release validation.                                                                                                                      |
| WT-04 | Implemented             | Release-sync tests cover 404, malformed metadata and mismatch failure preservation.                                                                                                                     |
| WT-05 | Implemented             | Browser tests verify the generated download destination and metadata.                                                                                                                                   |
| WT-06 | Verified with scope     | Axe checks run in Chromium desktop and mobile projects for both themes. Firefox/WebKit axe scans are skipped because axe caused browser page crashes; cross-browser theme/route smoke checks still run. |
| WT-07 | Implemented             | Keyboard navigation coverage exists; manual keyboard-only acceptance across every page remains pending.                                                                                                 |
| WT-08 | Implemented             | Mobile Chromium route and navigation checks exist.                                                                                                                                                      |
| WT-09 | Implemented             | Storage and clipboard failure paths are covered where browser APIs permit.                                                                                                                              |
| WT-10 | Implemented             | Playwright projects cover Chromium, Firefox and WebKit; recorded baseline result below.                                                                                                |
| WT-11 | Implemented             | Responsive checks cover 320–1920 px representative widths.                                                                                                                                              |
| WT-12 | Pending manual evidence | Axe catches detectable contrast violations; no complete manual WCAG 2.2 AA contrast audit is claimed.                                                                                                   |
| WT-13 | Partial                 | Focusable overflow regions and keyboard controls are tested; full focus-management review remains pending.                                                                                              |
| WT-14 | Pending                 | No human screen-reader compatibility test has been run.                                                                                                                                                 |
| WT-15 | Pending                 | Text scaling/zoom/reflow has not been manually checked at 200%.                                                                                                                                         |
| WT-16 | Implemented             | Site CSS respects reduced-motion preferences; browser-specific manual review remains pending.                                                                                                           |
| WT-17 | Implemented             | Unit tests cover malformed/missing release metadata and preservation behavior.                                                                                                                          |
| WT-18 | Pending final run       | Coverage is available with `pnpm test:coverage`; no percentage target or coverage result is claimed until run and reviewed.                                                                             |

## Website SEO

| ID     | Status        | Evidence / remaining work                                                                                              |
| ------ | ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| SEO-01 | Implemented   | Homepage emits SoftwareApplication structured data based on centralized product configuration; browser test checks it. |
| SEO-02 | Implemented   | Sitemap and robots resources are generated; browser tests check their availability and references.                     |
| SEO-03 | Pending       | Search Console ownership and indexing cannot be verified from this repository.                                         |
| SEO-04 | Implemented   | Canonicals are emitted by the shared layout; browser checks inspect route metadata.                                    |
| SEO-05 | Implemented   | Shared page layout supplies metadata and page-specific titles/descriptions.                                            |
| SEO-06 | Implemented   | Open Graph/Twitter metadata includes a 1200×630 PNG preview; social crawler preview validation remains pending.        |
| SEO-07 | Implemented   | Header/footer and page content cross-link related routes.                                                              |
| SEO-08 | Verified live | Production unknown route returned HTTP 404 on 2026-11-25.                                                              |
| SEO-09 | Implemented   | Privacy page has a revision date.                                                                                      |
| SEO-10 | Implemented   | Product and release details use centralized configuration; review all pages on release.                                |

## Verification record

Recorded baseline outcomes (25 November 2026; later source changes require new verification):

| Command                            | Result                                                                                                                                                                                   |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `git diff --check`                 | Passed 2026-11-25; only Git's LF-to-CRLF working-tree notices were emitted.                                                                                                              |
| `pnpm verify`                      | Passed 2026-11-25: Prettier, ESLint, Astro/TypeScript (0 errors/warnings/hints), 25 unit tests, and static build (12 pages).                                                             |
| `pnpm test:e2e`                    | Passed on 2026-11-25: 100 passed, 68 skipped across Chromium, mobile Chromium, Firefox and WebKit; skips are axe scans in Firefox/WebKit and project-inapplicable duplicate smoke tests. |
| `pnpm audit`                       | Passed on 2026-11-25: no known vulnerabilities.                                                                                                                                          |
| `pnpm release:sync`                | Passed against v0.1.3: actual APK manifest, digest, size and pinned signer validated.                                                                                                    |
| Production Lighthouse              | Measured 2026-11-25; values and limitations recorded in Website efficiency above.                                                                                                        |
| Production headers/404/compression | CSP and security headers plus 404 verified live; cache-control observed; compression remains unverified.                                                                                 |

## Deferred evidence and residual risks

- No manual screen-reader, Switch Access, 200% zoom, physical-device, or cross-platform macOS verification is claimed.
- Axe automation is scoped to Chromium desktop/mobile because Firefox/WebKit axe scans crashed in this environment; this limitation is explicit above.
- Lighthouse performance scored 92 against the proposed 95 target. Investigate mobile Speed Index/TBT before making a performance claim; repeat runs and collect field INP where available.
- Automated link checks establish internal route consistency and external URL scheme safety, not uninterrupted availability of every third-party destination.
- The certificate fingerprint is a trust anchor: changing the publisher key requires a deliberate, reviewed update to release configuration.
- The audit records implementation evidence; it does not certify the Android application's full security posture.
