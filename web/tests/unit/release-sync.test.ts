import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { synchronizeRelease } from '../../scripts/sync-release';
import {
  validateApkForRelease,
  isValidReleaseTag,
} from '../../scripts/release-validation';
import { RELEASE_CONFIG, PRODUCT_INFO } from '../../src/config/product';
import type { ApkMetadata } from '../../scripts/apk-metadata';

const metadata: ApkMetadata = {
  packageName: PRODUCT_INFO.android.applicationId,
  versionCode: 4,
  versionName: '0.1.3',
  minimumAndroidApi: 33,
  architectures: ['arm64-v8a'],
};
const digest = 'a'.repeat(64);
const validExpectation = {
  tag: 'v0.1.3',
  packageName: PRODUCT_INFO.android.applicationId,
  expectedSizeBytes: 128,
  actualSizeBytes: 128,
  expectedSha256: digest,
  actualSha256: digest,
  signerSha256: RELEASE_CONFIG.trustedSigningCertificateSha256,
};
const temporaryDirectories: string[] = [];

async function createExistingMetadata(): Promise<{
  path: string;
  original: string;
}> {
  const directory = await mkdtemp(join(tmpdir(), 'retenqo-release-test-'));
  temporaryDirectories.push(directory);
  const path = join(directory, 'release.generated.json');
  const original = '{"status":"last-known-good"}\n';
  await writeFile(path, original, 'utf8');
  return { path, original };
}

afterEach(async () => {
  await Promise.all(
    temporaryDirectories
      .splice(0)
      .map((directory) => rm(directory, { recursive: true, force: true }))
  );
});

describe('release artifact validation', () => {
  it('accepts only conventional version tags', () => {
    expect(isValidReleaseTag('v0.1.3')).toBe(true);
    expect(isValidReleaseTag('v0.1.3-rc.1')).toBe(true);
    for (const tag of [
      'latest',
      '../v0.1.3',
      'v0.1',
      'v0.1.3/evil',
      'v1.0.0%2f',
    ]) {
      expect(isValidReleaseTag(tag)).toBe(false);
    }
  });

  it('accepts a release only when APK identity, size, digest, and signer match', () => {
    expect(() =>
      validateApkForRelease(metadata, validExpectation)
    ).not.toThrow();
  });

  it.each([
    ['wrong package', { ...validExpectation, packageName: 'com.attacker.app' }],
    ['wrong size', { ...validExpectation, actualSizeBytes: 129 }],
    ['wrong digest', { ...validExpectation, actualSha256: 'b'.repeat(64) }],
    ['untrusted signer', { ...validExpectation, signerSha256: 'c'.repeat(64) }],
    ['malformed tag', { ...validExpectation, tag: 'latest' }],
  ])('rejects %s', (_caseName, expectation) => {
    expect(() => validateApkForRelease(metadata, expectation)).toThrow();
  });

  it('preserves existing verified metadata when GitHub has no release', async () => {
    const { path, original } = await createExistingMetadata();
    await expect(
      synchronizeRelease({
        outputPath: path,
        fetcher: async () => new Response(null, { status: 404 }),
      })
    ).resolves.toBe('preserved');
    await expect(readFile(path, 'utf8')).resolves.toBe(original);
  });

  it('preserves existing metadata when release data is malformed', async () => {
    const { path, original } = await createExistingMetadata();
    const fetcher = async () =>
      new Response(JSON.stringify({ tag_name: '../invalid' }), { status: 200 });
    await expect(
      synchronizeRelease({ outputPath: path, fetcher })
    ).rejects.toThrow();
    await expect(readFile(path, 'utf8')).resolves.toBe(original);
  });
});
