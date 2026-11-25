# Verification evidence

The current stable release is v0.1.4. Recorded outcomes identify the tested artifact, source and device.

| Scope | Recorded outcome | Source |
| --- | --- | --- |
| Published v0.1.4 | Signed APK built; pinned single signer verified; in-place production upgrade and launch successful on Xiaomi 2406ERN9CI, Android 16/API 36, ARM64 | [Release notes](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.4) |
| v0.1.4 source before version-only bump | 114 JVM tests passed; 6/6 Android instrumentation tests passed; Android Lint reported zero errors | [Implementation status](IMPLEMENTATION_STATUS.md), [database fix](https://github.com/priyanshuchawda/retenqo-enterprise/commit/3edfbc0cff94d360d4127b92948309e5e656a686) |
| Website baseline | 25 unit tests, 100 browser tests passed, 68 skipped; format/lint/type/build passed; dependency audit reported no known vulnerabilities | [Audit](../web/docs/ENGINEERING_AUDIT.md), [baseline](https://github.com/priyanshuchawda/retenqo-enterprise/commit/7a0ef6ec48cba2077486db7baa202cdc7dffb079) |
| Historical offline generation | Short general question answered with connectivity disabled | [Implementation status](IMPLEMENTATION_STATUS.md) |
| Earlier model experiments | Exploratory host and phone measurements | [Research index](RESEARCH_INDEX.md) |

The concurrent-read device test reproduced the SQLCipher authentication failure before the fix. After retaining the required key array, the reported six tests passed. See [MnemeDatabaseDeviceTest.kt](../android/mneme/app/src/androidTest/java/com/mneme/app/data/MnemeDatabaseDeviceTest.kt) and [MnemeDatabase.kt](../android/mneme/app/src/main/java/com/mneme/app/data/MnemeDatabase.kt). The published v0.1.4 APK includes this fix. Signed upgrade verification succeeded on API 36; the same release's API 33 upgrade coverage remains to be established.

## Published artifact identity

| Field | Value |
| --- | --- |
| Version | v0.1.4 (version code 5) |
| Package | `com.mneme.app` |
| Compatibility | Android 13+ / API 33+, ARM64 |
| APK | [Retenqo-v0.1.4-arm64-v8a.apk](https://github.com/priyanshuchawda/retenqo-enterprise/releases/download/v0.1.4/Retenqo-v0.1.4-arm64-v8a.apk) |
| Size | 39,374,307 bytes |
| SHA-256 | `b31770666bf7b860e9dc3e562a97de7286c6ad31347f67a1ce8fa96fdabf8f98` |
| Signing certificate SHA-256 | `b975fe4b5d929586e9bf0133ebde25c03d0acf990ddbba5c14f0f2b31ae38cca` |

Source: [release notes](https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v0.1.4) and [generated website metadata](../web/src/data/release.generated.json). The release reports signature verification and successful in-place upgrade without uninstall or data reset.

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
