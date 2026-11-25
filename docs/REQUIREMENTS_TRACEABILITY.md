# Requirements Traceability Matrix

This document maps competition judging criteria to concrete evidence in the repository.

## Judging criteria overview

| Criterion                   | Android evidence                              | Website evidence                               |
| --------------------------- | --------------------------------------------- | ---------------------------------------------- |
| Code Quality                | See §1 below                                  | See §2 below                                   |
| Security                    | See §3 below                                  | See §4 below                                   |
| Efficiency                  | See §5 below                                  | See §6 below                                   |
| Testing                     | See §7 below                                  | See §8 below                                   |
| Accessibility               | See §9 below                                  | See §10 below                                  |
| Problem Statement Alignment | See §11 below                                 | See §12 below                                  |
| Documentation               | See §13 below                                 | See §14 below                                  |

---

## §1 — Android: Code Quality

| Item                                      | Location                                    |
| ----------------------------------------- | ------------------------------------------- |
| Kotlin with strict types                  | `android/mneme/app/src/main/`               |
| Jetpack Compose UI with immutable state   | `android/mneme/app/src/main/java/com/mneme/` |
| Room database with schema migrations      | `android/mneme/app/src/main/`               |
| WorkManager for background processing     | `android/mneme/app/src/main/`               |
| Dependency injection via Hilt/manual DI   | `android/mneme/app/src/main/`               |
| Kotlin Coroutines with cancellation       | `android/mneme/app/src/main/`               |
| Input validation and size bounds          | `android/mneme/app/src/main/`               |

## §2 — Website: Code Quality

| Item                                      | Location                                      |
| ----------------------------------------- | --------------------------------------------- |
| TypeScript strict mode, no `any`          | `web/tsconfig.json`, all `.ts`/`.astro` files |
| Zod schema validation for release data    | `web/src/lib/releases/validation.ts`          |
| Reusable component system                 | `web/src/components/ui/`                      |
| Single source of truth for config         | `web/src/config/site.ts`                      |
| Centralized design token system           | `web/src/styles/global.css`                   |
| pnpm frozen lockfile                      | `web/pnpm-lock.yaml`                          |
| ESLint with Astro and TS plugins          | `web/eslint.config.mjs`                       |
| Modular page component architecture       | `web/src/components/home/`, `web/src/pages/`  |

## §3 — Android: Security

| Item                                              | Location / notes                             |
| ------------------------------------------------- | -------------------------------------------- |
| NotificationListenerService with allowlist        | `android/mneme/app/src/main/`                |
| Bounded OCR input limits                          | `android/mneme/app/src/main/`                |
| AES-256-GCM encrypted backup exports              | `android/mneme/app/src/main/`                |
| SQLite `secure_delete` enabled                    | `android/mneme/app/src/main/`                |
| Scoped media access (no all-files permission)     | `android/mneme/app/src/main/`                |
| URI/MIME validation on shared content             | `android/mneme/app/src/main/`                |
| Voice recording bounded at 120 seconds            | `android/mneme/app/src/main/`                |
| Security policy                                   | `SECURITY.md`                                |

## §4 — Website: Security

| Item                                              | Location                                     |
| ------------------------------------------------- | -------------------------------------------- |
| Zero-backend architecture (no server, DB, auth)   | `web/astro.config.mjs` (static output)       |
| Content Security Policy                           | `web/public/_headers`                        |
| HSTS, X-Frame-Options, X-Content-Type-Options     | `web/public/_headers`                        |
| Referrer-Policy and Permissions-Policy            | `web/public/_headers`                        |
| `security.txt` with responsible disclosure        | `web/public/.well-known/security.txt`        |
| Trusted URL validator for APK download links      | `web/src/lib/releases/validation.ts`         |
| Schema-validated release metadata                 | `web/src/lib/releases/validation.ts`         |
| Threat model documentation                        | `web/docs/THREAT_MODEL.md`                   |
| No third-party tracking or analytics              | No external scripts in any page              |

## §5 — Android: Efficiency

| Item                                              | Notes                                         |
| ------------------------------------------------- | --------------------------------------------- |
| Local inference without cloud round-trip          | llama.cpp on-device                           |
| Rules-first extraction before model invocation    | Deterministic path avoids inference cost      |
| WorkManager for background task scheduling        | System-managed, battery-aware                 |
| Bounded notification queue (500 capacity)         | Prevents unbounded memory growth              |

## §6 — Website: Efficiency

| Item                                              | Location / notes                             |
| ------------------------------------------------- | -------------------------------------------- |
| Zero JavaScript shipped by default (Astro static) | All pages are pure HTML/CSS                  |
| Tailwind CSS — purged, only used classes          | Production CSS bundle < 30 KB target         |
| No third-party tracking scripts                   | Zero external requests on page load          |
| SVG illustrations (not raster bitmaps)            | `web/public/illustrations/`                  |
| Cloudflare Pages CDN with edge caching            | Global edge distribution                     |
| Performance targets documented                    | `web/docs/PERFORMANCE.md`                    |

## §7 — Android: Testing

| Item                                              | Location                                     |
| ------------------------------------------------- | -------------------------------------------- |
| Kotlin unit test files (19)                       | `android/mneme/app/src/test/`                |
| Instrumentation test files (2)                    | `android/mneme/app/src/androidTest/`         |
| Android Lint integration                          | `./gradlew :app:lintDebug`                   |
| PowerShell build/test script                      | `scripts/build-mneme.ps1`                    |

## §8 — Website: Testing

| Item                                              | Location                                     |
| ------------------------------------------------- | -------------------------------------------- |
| Vitest unit tests (8 cases)                       | `web/tests/unit/release-validation.test.ts`  |
| Playwright browser tests                          | `web/tests/browser/`                         |
| Test coverage: URL validators, Zod schemas        | `web/tests/unit/`                            |
| Testing documentation                             | `web/docs/TESTING.md`                        |

## §9 — Android: Accessibility

| Item                                              | Notes                                         |
| ------------------------------------------------- | --------------------------------------------- |
| Jetpack Compose semantic APIs                     | `android/mneme/app/src/main/`                 |
| Material 3 components (accessible by default)     | Compose Material3 library                     |
| Home-screen widget without exposing memory text   | Counts only, no content in widget             |

## §10 — Website: Accessibility

| Item                                              | Location                                     |
| ------------------------------------------------- | -------------------------------------------- |
| Semantic HTML5 landmarks on all pages             | `web/src/layouts/BaseLayout.astro`           |
| Skip-to-content link                              | `web/src/layouts/BaseLayout.astro`           |
| Minimum 44×44px interactive targets               | `web/src/components/ui/Button.astro`         |
| Native `<details>/<summary>` FAQ (no JS)         | `web/src/pages/faq.astro`                    |
| `prefers-reduced-motion` support                  | `web/src/styles/global.css`                  |
| `aria-hidden` on decorative elements              | All `.astro` component files                 |
| WCAG 2.2 AA target — documented                   | `web/src/pages/accessibility.astro`          |
| Accessibility evidence document                   | `web/docs/ACCESSIBILITY.md`                  |

## §11 — Android: Problem Statement Alignment

| Item                                              | Notes                                         |
| ------------------------------------------------- | --------------------------------------------- |
| Commitment tracking with source evidence          | Core differentiator of Retenqo                |
| On-device AI — no cloud service dependency        | llama.cpp local inference                     |
| Consent-first architecture                        | Capture disabled until user enables sources   |
| Personal productivity vertical                    | Memory, commitments, reminders                |
| Open-source under Apache 2.0                      | Full source in this repository                |

## §12 — Website: Problem Statement Alignment

| Item                                              | Location                                     |
| ------------------------------------------------- | -------------------------------------------- |
| Hero story — full commitment lifecycle            | `web/src/components/home/MemoryWorkflow.astro`|
| Feature grid aligned to product capabilities     | `web/src/components/home/FeatureGrid.astro`  |
| Download page with APK distribution              | `web/src/pages/download.astro`               |
| Permission transparency                          | `web/src/pages/permissions.astro`            |
| Privacy policy                                   | `web/src/pages/privacy.astro`                |
| Security architecture                            | `web/src/pages/security.astro`               |

## §13 — Android: Documentation

| Document                             | Location                      |
| ------------------------------------ | ----------------------------- |
| Main README                          | `README.md`                   |
| Architecture documentation           | `docs/ARCHITECTURE.md`        |
| Threat model                         | `docs/THREAT_MODEL.md`        |
| Release process                      | `docs/RELEASE_PROCESS.md`     |
| Security policy                      | `SECURITY.md`                 |
| Contributing guide                   | `CONTRIBUTING.md`             |
| Engineering rules (AI agents)        | `AGENTS.md`                   |

## §14 — Website: Documentation

| Document                             | Location                      |
| ------------------------------------ | ----------------------------- |
| Website architecture                 | `web/docs/ARCHITECTURE.md`    |
| Design system                        | `web/docs/DESIGN_SYSTEM.md`   |
| Threat model                         | `web/docs/THREAT_MODEL.md`    |
| Testing guide                        | `web/docs/TESTING.md`         |
| Performance budgets                  | `web/docs/PERFORMANCE.md`     |
| Deployment guide                     | `web/docs/DEPLOYMENT.md`      |
| Accessibility evidence               | `web/docs/ACCESSIBILITY.md`   |
| Engineering rules (AI agents)        | `web/AGENTS.md`               |
| This document                        | `docs/REQUIREMENTS_TRACEABILITY.md` |
