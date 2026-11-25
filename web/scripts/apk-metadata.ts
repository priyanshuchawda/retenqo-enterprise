export interface ApkMetadata {
  readonly versionCode: number;
  readonly versionName: string;
  readonly minimumAndroidApi: number;
  readonly architectures: readonly ('arm64-v8a' | 'armeabi-v7a' | 'x86_64')[];
}

export function parseApkBadging(output: string): ApkMetadata {
  const packageLine =
    /^package:.*?versionCode='(\d+)'.*?versionName='([^']+)'/m.exec(output);
  const minApi = /^sdkVersion:'(\d+)'/m.exec(output);
  const nativeCode = /^native-code:\s*(.+)$/m.exec(output);
  if (!packageLine || !minApi || !nativeCode) {
    throw new Error(
      'Unable to read version, minSdkVersion, or ABI from APK metadata.'
    );
  }
  const versionCode = packageLine[1];
  const versionName = packageLine[2];
  const minimumAndroidApi = minApi[1];
  const nativeCodeList = nativeCode[1];
  if (!versionCode || !versionName || !minimumAndroidApi || !nativeCodeList) {
    throw new Error('APK metadata is incomplete.');
  }

  const rawAbis = [...nativeCodeList.matchAll(/'([^']+)'/g)].map(
    (match) => match[1]
  );
  const architectures = rawAbis.filter(
    (abi): abi is ApkMetadata['architectures'][number] =>
      abi === 'arm64-v8a' || abi === 'armeabi-v7a' || abi === 'x86_64'
  );
  if (architectures.length === 0) {
    throw new Error('APK has no supported Android architecture.');
  }

  return {
    versionCode: Number(versionCode),
    versionName,
    minimumAndroidApi: Number(minimumAndroidApi),
    architectures,
  };
}
