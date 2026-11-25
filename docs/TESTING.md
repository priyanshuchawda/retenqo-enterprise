# Verification evidence

Recorded outcomes are scoped to their artifacts and revisions. This documentation pass does not constitute a fresh test run.

| Scope | Recorded outcome | Source |
| --- | --- | --- |
| Published v0.1.3 | 114 JVM tests, 5 instrumentation tests passed; lint 0 errors, 34 warnings, 1 hint; 14 web unit tests at release time | [Release notes](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.3) |
| Post-release database fix | 6 instrumentation tests passed on Xiaomi 2406ERN9CI, Android 16/API 36, ARM64; debug launch successful | [Fix commit](https://github.com/priyanshuchawda/retenqo-enterprise/commit/3edfbc0cff94d360d4127b92948309e5e656a686), [implementation status](IMPLEMENTATION_STATUS.md) |
| Website baseline | 25 unit tests, 100 browser tests passed, 68 skipped; format/lint/type/build passed; dependency audit reported no known vulnerabilities | [Audit](../web/docs/ENGINEERING_AUDIT.md), [baseline](https://github.com/priyanshuchawda/retenqo-enterprise/commit/7a0ef6ec48cba2077486db7baa202cdc7dffb079) |
| Historical offline generation | Short general question answered with connectivity disabled | [Implementation status](IMPLEMENTATION_STATUS.md) |
| Earlier model experiments | Exploratory host and phone measurements | [Research index](RESEARCH_INDEX.md) |

The concurrent-read device test reproduced the SQLCipher authentication failure before the fix. After retaining the required key array, the reported six tests passed. See [MnemeDatabaseDeviceTest.kt](../android/mneme/app/src/androidTest/java/com/mneme/app/data/MnemeDatabaseDeviceTest.kt) and [MnemeDatabase.kt](../android/mneme/app/src/main/java/com/mneme/app/data/MnemeDatabase.kt). The published APK predates the fix; API 33 and signed upgrade verification remain open.

## Reproduction

From the root on Windows: `./scripts/build-mneme.ps1 -Task verify`.

From `android/mneme/`:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

From `web/`:

```bash
pnpm verify
pnpm test:e2e
pnpm audit
pnpm release:sync
```

Record source revision, variant, device/API, model, commands, passes/failures/skips and scope for every new run. Full axe scans in the website baseline were limited to Chromium desktop/mobile; Firefox/WebKit had smoke coverage. Later source changes need fresh verification.
