# Website testing guide

From the repository's `web/` directory:

```bash
pnpm verify
pnpm test:e2e
pnpm audit
pnpm test:coverage
```

Unit tests live in [tests/unit](../tests/unit/) and browser tests in [tests/e2e](../tests/e2e/). They cover release metadata and validation, APK identity, synchronization failure preservation, navigation, theme switching, clipboard, download links, layout and accessibility.

The recorded 25 November 2026 website baseline (before the v0.1.4 metadata refresh) was 25 unit tests and 100 browser tests passed (68 skipped). Full axe scans ran in Chromium desktop/mobile; Firefox and WebKit had smoke coverage. See [engineering audit](ENGINEERING_AUDIT.md). The latest [v0.1.4 release metadata](../src/data/release.generated.json) matches the published artifact; a new browser run can extend the baseline.

Record actual commands, revision, browser project and skips. Local preview checks do not establish deployed security headers; production verification is recorded separately.
