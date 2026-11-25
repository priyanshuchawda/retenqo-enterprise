import type { ApkMetadata } from './apk-metadata';
import { PRODUCT_INFO, RELEASE_CONFIG } from '../src/config/product';

export function isValidReleaseTag(tag: string): boolean {
  return /^v\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/.test(tag);
}

export interface ApkReleaseExpectations {
  readonly tag: string;
  readonly packageName: string;
  readonly expectedSizeBytes: number;
  readonly actualSizeBytes: number;
  readonly expectedSha256: string;
  readonly actualSha256: string;
  readonly signerSha256: string;
}

export function validateApkForRelease(
  metadata: ApkMetadata,
  expected: ApkReleaseExpectations
): void {
  if (!isValidReleaseTag(expected.tag)) {
    throw new Error(`Release tag is malformed: ${expected.tag}`);
  }
  if (metadata.packageName !== expected.packageName) {
    throw new Error(`APK package ${metadata.packageName} is not Retenqo.`);
  }
  if (metadata.versionName !== expected.tag.slice(1)) {
    throw new Error(
      `APK versionName ${metadata.versionName} does not match release ${expected.tag}.`
    );
  }
  if (metadata.minimumAndroidApi < PRODUCT_INFO.android.minimumApi) {
    throw new Error(
      `APK minimum Android API is below ${PRODUCT_INFO.android.minimumApi}.`
    );
  }
  if (
    !PRODUCT_INFO.android.architectures.some((abi) =>
      metadata.architectures.includes(abi)
    )
  ) {
    throw new Error('APK does not include a supported Retenqo architecture.');
  }
  if (expected.actualSizeBytes !== expected.expectedSizeBytes) {
    throw new Error(
      'Downloaded APK size does not match GitHub release metadata.'
    );
  }
  if (
    expected.actualSha256.toLowerCase() !==
    expected.expectedSha256.toLowerCase()
  ) {
    throw new Error(
      'Downloaded APK SHA-256 does not match the GitHub asset digest.'
    );
  }
  if (
    expected.signerSha256.toLowerCase() !==
    RELEASE_CONFIG.trustedSigningCertificateSha256
  ) {
    throw new Error(
      'APK signer does not match the trusted Retenqo publisher certificate.'
    );
  }
}

export function assertAllowedApkDestination(rawUrl: string): void {
  let destination: URL;
  try {
    destination = new URL(rawUrl);
  } catch {
    throw new Error('APK download resolved to an invalid destination.');
  }
  const allowedHosts = new Set([
    'github.com',
    'release-assets.githubusercontent.com',
  ]);
  if (
    destination.protocol !== 'https:' ||
    !allowedHosts.has(destination.hostname) ||
    destination.username !== '' ||
    destination.password !== ''
  ) {
    throw new Error('APK download redirected to an unexpected destination.');
  }
}
