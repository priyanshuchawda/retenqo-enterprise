# Testing Guide

## Test stack

| Layer         | Tool              | Coverage                                          |
| ------------- | ----------------- | ------------------------------------------------- |
| Unit tests    | Vitest            | Release validation schemas, URL validators, utils |
| Browser tests | Playwright        | Page rendering, navigation, accessibility, download flow |

## Run all tests

```powershell
cd C:\Users\Admin\Desktop\mneme\web
pnpm test
```

## Unit tests only

```powershell
pnpm test:unit
# or directly:
npx vitest run
```

Unit test files live in `tests/unit/`.

## Browser tests only

```powershell
pnpm test:e2e
# or directly:
npx playwright test
```

Browser test files live in `tests/browser/`.

## What is tested

### Unit (Vitest)
- Zod schema validation for `release.generated.json`
- `isTrustedReleasePageUrl()` — rejects non-GitHub URLs, wrong repos, custom ports
- `isTrustedApkDownloadUrl()` — tag matching, path traversal rejection, extension validation
- `ReleaseState` discriminated union narrowing

### Browser (Playwright)
- Homepage renders H1 and primary CTA button
- Navigation links work across all pages
- Download page shows "pending" state when no release is available
- 404 page returns correct HTTP status
- CSP and security headers present
- Skip-to-content link is first focusable element
- Mobile viewport rendering (375px)
- Desktop viewport rendering (1440px)

## Adding tests

Place unit tests in `tests/unit/*.test.ts`. Place browser tests in `tests/browser/*.spec.ts`. Both are picked up automatically by their respective runners.

## Evidence requirement

Before claiming tests pass, run the actual command and record the output. A test file existing is not proof of a passing test.
