export interface ApkMetadata {
  readonly packageName: string;
  readonly versionCode: number;
  readonly versionName: string;
  readonly minimumAndroidApi: number;
  readonly architectures: readonly ('arm64-v8a' | 'armeabi-v7a' | 'x86_64')[];
}

export function parseApkBadging(output: string): ApkMetadata {
  const packageLine =
    /^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'/m.exec(
      output
    );
  const minApi = /^sdkVersion:'(\d+)'/m.exec(output);
  const nativeCode = /^native-code:\s*(.+)$/m.exec(output);
  if (!packageLine || !minApi || !nativeCode) {
    throw new Error(
      'Unable to read version, minSdkVersion, or ABI from APK metadata.'
    );
  }
  const packageName = packageLine[1];
  const versionCode = packageLine[2];
  const versionName = packageLine[3];
  const minimumAndroidApi = minApi[1];
  const nativeCodeList = nativeCode[1];
  if (
    !packageName ||
    !versionCode ||
    !versionName ||
    !minimumAndroidApi ||
    !nativeCodeList
  ) {
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
    packageName,
    versionCode: Number(versionCode),
    versionName,
    minimumAndroidApi: Number(minimumAndroidApi),
    architectures,
  };
}

export function parseApkSignerFingerprint(output: string): string {
  const match = /certificate SHA-256 digest:\s*([a-fA-F0-9:]{64,95})/i.exec(
    output
  );
  const fingerprint = match?.[1]?.replaceAll(':', '').toLowerCase();
  if (!fingerprint || !/^[a-f0-9]{64}$/.test(fingerprint)) {
    throw new Error(
      'apksigner did not report a valid SHA-256 signer fingerprint.'
    );
  }
  return fingerprint;
}
