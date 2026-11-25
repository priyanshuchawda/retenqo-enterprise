import { describe, expect, it } from 'vitest';
import fc from 'fast-check';
import {
  formatBytes,
  isTrustedApkDownloadUrl,
  isTrustedReleasePageUrl,
  parseReleaseState,
} from '../../src/lib/releases/validation';

describe('isTrustedReleasePageUrl', () => {
  it('accepts official github.com release tag URLs', () => {
    expect(isTrustedReleasePageUrl('https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0')).toBe(
      true
    );
  });

  it('rejects non-HTTPS, custom ports, wrong repo, query params, and path traversal', () => {
    expect(isTrustedReleasePageUrl('http://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0')).toBe(
      false
    );
    expect(
      isTrustedReleasePageUrl('https://github.com:8443/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0')
    ).toBe(false);
    expect(isTrustedReleasePageUrl('https://github.com/other/retenqo-enterprise/releases/tag/v1.0.0')).toBe(false);
    expect(
      isTrustedReleasePageUrl('https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0?x=1')
    ).toBe(false);
    expect(isTrustedReleasePageUrl('https://github.com/priyanshuchawda/retenqo-enterprise/releases/../evil')).toBe(
      false
    );
  });
});

describe('isTrustedApkDownloadUrl', () => {
  it('accepts version-specific APK download URLs matching expectedTag', () => {
    expect(
      isTrustedApkDownloadUrl(
        'https://github.com/priyanshuchawda/retenqo-enterprise/releases/download/v1.0.0/Retenqo.apk',
        'v1.0.0'
      )
    ).toBe(true);
  });

  it('rejects tag mismatch, non-APK files, and release page URLs', () => {
    expect(
      isTrustedApkDownloadUrl(
        'https://github.com/priyanshuchawda/retenqo-enterprise/releases/download/v1.0.0/Retenqo.apk',
        'v1.0.1'
      )
    ).toBe(false);
    expect(
      isTrustedApkDownloadUrl('https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0')
    ).toBe(false);
    expect(
      isTrustedApkDownloadUrl(
        'https://github.com/priyanshuchawda/retenqo-enterprise/releases/download/v1.0.0/Retenqo.zip'
      )
    ).toBe(false);
  });

  it('never accepts arbitrary non-github URLs (property-based)', () => {
    fc.assert(
      fc.property(fc.webUrl(), (url) => {
        if (!url.startsWith('https://github.com/priyanshuchawda/retenqo-enterprise/releases/download/')) {
          expect(isTrustedApkDownloadUrl(url)).toBe(false);
        }
      })
    );
  });
});

describe('parseReleaseState', () => {
  it('parses a valid pending release state', () => {
    const parsed = parseReleaseState({
      status: 'pending',
      syncedAt: '2026-11-25T08:00:00.000Z',
      reason: 'Awaiting first release.',
    });
    expect(parsed.status).toBe('pending');
  });

  it('rejects an available release with an untrusted apkUrl or API < 33', () => {
    expect(() =>
      parseReleaseState({
        status: 'available',
        syncedAt: '2026-11-25T08:00:00.000Z',
        release: {
          version: 'v1.0.0',
          versionCode: 1,
          publishedAt: '2026-11-25T08:00:00.000Z',
          releaseUrl: 'https://github.com/priyanshuchawda/retenqo-enterprise/releases/tag/v1.0.0',
          apkUrl: 'https://evil.com/Retenqo.apk',
          apkSizeBytes: 1024,
          sha256: 'a'.repeat(64),
          signingCertificateSha256: 'b'.repeat(64),
          minimumAndroidApi: 26,
          architectures: ['arm64-v8a'],
        },
      })
    ).toThrow();
  });
});

describe('formatBytes', () => {
  it('formats byte sizes cleanly', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(1024)).toBe('1.0 KB');
    expect(formatBytes(1024 * 1024 * 25)).toBe('25.0 MB');
  });
});
