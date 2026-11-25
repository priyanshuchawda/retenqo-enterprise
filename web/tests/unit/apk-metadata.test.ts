import { describe, expect, it } from 'vitest';
import {
  parseApkBadging,
  parseApkSignerFingerprint,
} from '../../scripts/apk-metadata';

const BADGING = `package: name='com.mneme.app' versionCode='4' versionName='0.1.3' platformBuildVersionName='16' platformBuildVersionCode='36' compileSdkVersion='36' compileSdkVersionCodename='16'\nsdkVersion:'33'\ntargetSdkVersion:'36'\nnative-code: 'arm64-v8a'`;

describe('parseApkBadging', () => {
  it('reads package identity and compatibility from actual APK output', () => {
    expect(parseApkBadging(BADGING)).toEqual({
      packageName: 'com.mneme.app',
      versionCode: 4,
      versionName: '0.1.3',
      minimumAndroidApi: 33,
      architectures: ['arm64-v8a'],
    });
  });

  it('rejects incomplete or unsupported ABI metadata', () => {
    expect(() => parseApkBadging('package: name=unknown')).toThrow();
    expect(() =>
      parseApkBadging(BADGING.replace("'arm64-v8a'", "'mips'"))
    ).toThrow();
  });
});

describe('parseApkSignerFingerprint', () => {
  it('normalizes the signer SHA-256 from apksigner output', () => {
    expect(
      parseApkSignerFingerprint(
        `Signer #1 certificate SHA-256 digest: ${'AA:'.repeat(31)}AA`
      )
    ).toBe('aa'.repeat(32));
  });

  it('rejects missing and malformed fingerprints', () => {
    expect(() =>
      parseApkSignerFingerprint('Verified using v2 scheme')
    ).toThrow();
    expect(() =>
      parseApkSignerFingerprint('Signer #1 certificate SHA-256 digest: no')
    ).toThrow();
  });
});
