import { describe, expect, it } from 'vitest';
import { parseApkBadging } from '../../scripts/apk-metadata';

const BADGING = `package: name='com.mneme.app' versionCode='3' versionName='0.1.2' platformBuildVersionName='16' platformBuildVersionCode='36' compileSdkVersion='36' compileSdkVersionCodename='16'\nsdkVersion:'33'\ntargetSdkVersion:'36'\nnative-code: 'arm64-v8a'`;

describe('parseApkBadging', () => {
  it('reads release identity from the APK manifest output', () => {
    expect(parseApkBadging(BADGING)).toEqual({
      versionCode: 3,
      versionName: '0.1.2',
      minimumAndroidApi: 33,
      architectures: ['arm64-v8a'],
    });
  });

  it('rejects missing or unsupported ABI metadata', () => {
    expect(() => parseApkBadging('package: name=unknown')).toThrow();
    expect(() =>
      parseApkBadging(BADGING.replace("'arm64-v8a'", "'mips'"))
    ).toThrow();
  });
});
