# Website Threat Model

## Scope

This threat model covers the Retenqo **website** (`web/`) only. It does not cover the Android application, GitHub account security, or the Cloudflare account.

## Assets to protect

| Asset                       | Sensitivity | Notes                                       |
| --------------------------- | ----------- | ------------------------------------------- |
| APK download link integrity | High        | A tampered link could serve a malicious APK |
| Release metadata            | Medium      | Schema-validated JSON committed to the repo |
| Visitor browser security    | Medium      | No user data collected, but headers matter  |
| Repository credentials      | High        | Out of scope (GitHub account security)      |

## Architecture summary

The website is a **fully static site** with no backend, no database, no user authentication, and no server-side logic. All pages are pre-rendered at build time. The only dynamic behavior is APK download link rendering, which is driven by a committed JSON file (`release.generated.json`) that is generated offline and schema-validated before build.

## Threat surface

### In scope (what this website can affect)

- HTTP response headers served to visitors
- HTML and content delivered to browsers
- APK download link correctness
- Cloudflare Pages deployment pipeline

### Out of scope (handled elsewhere)

- APK signing and provenance (Android build process)
- GitHub release integrity (GitHub platform)
- Cloudflare account security (Cloudflare platform)
- User device security

## Identified threats and mitigations

### T-01 — Malicious APK link substitution

**Threat:** An attacker modifies `release.generated.json` to point to a malicious APK.  
**Likelihood:** Low (requires GitHub push access)  
**Mitigation:**

- `release.generated.json` is schema-validated by Zod at build time
- URL validator (`isTrustedApkDownloadUrl`) only accepts GitHub release asset URLs for `priyanshuchawda/retenqo-enterprise`
- Tag in URL must match the release version tag
- Port numbers in URLs are rejected

### T-02 — Clickjacking

**Threat:** Embedding the website in an iframe to trick users into clicking buttons.  
**Mitigation:** `X-Frame-Options: DENY` header enforced at Cloudflare Pages edge.

### T-03 — MIME-type sniffing

**Threat:** Browser interprets served files as a different content type.  
**Mitigation:** `X-Content-Type-Options: nosniff` header enforced.

### T-04 — Cross-site scripting (XSS)

**Threat:** Injected scripts execute in visitor browsers.  
**Mitigation:**

- No user input or dynamic content — all HTML is static and pre-rendered
- CSP header restricts script/style/connect sources in `public/_headers`
- Referrer-Policy and Permissions-Policy headers restrict browser capabilities

### T-05 — Sensitive data leakage via Referer

**Threat:** Browser sends Referer header exposing navigation context to third parties.  
**Mitigation:** `Referrer-Policy: no-referrer` enforced.

### T-06 — Browser-level capability abuse

**Threat:** Malicious content accessed via camera, microphone, or geolocation.  
**Mitigation:** `Permissions-Policy` disables camera, microphone, geolocation at the browser level.

### T-07 — Supply chain compromise via dependencies

**Threat:** A compromised npm package is included in the build.  
**Mitigation:**

- `pnpm` frozen lockfile — exact versions pinned
- Run `pnpm audit` before each release
- Minimal dependency surface (Astro, Tailwind, Zod, Vitest, Playwright)

### T-08 — Protocol downgrade / MITM

**Threat:** Attacker downgrades connection from HTTPS to HTTP.  
**Mitigation:** HSTS header with 1-year max-age; Cloudflare Pages enforces HTTPS.

### T-09 — Release metadata staleness

**Threat:** A revoked or yanked APK remains linked on the website.  
**Mitigation:** `pnpm release:sync` regenerates `release.generated.json`. Re-deploy is manual but fast via Wrangler. Future work: add a `revoked` release state.

## Residual risks

| Risk                                                   | Status                                                                  |
| ------------------------------------------------------ | ----------------------------------------------------------------------- |
| APK cryptographic signature not verified at build time | Accepted — this requires `apksigner` tooling and a trusted cert env var |
| Cloudflare account not under attacker control          | Assumed — MFA recommended                                               |
| `unsafe-inline` in CSP for Astro inline scripts        | Accepted — Astro generates inline JSON-LD and style attributes          |
| No automated secret scanning                           | Recommended — run `git secret scan` or Trufflehog periodically          |

## Controls not implemented (intentionally)

- No Content-Security-Policy nonce (Astro static output uses inline styles/scripts)
- No Subresource Integrity for self-hosted assets (all assets are self-hosted, no CDN injection)
- No server-side rate limiting (zero backend)
- No automated dependency vulnerability scanning in CI (no CI by design)
