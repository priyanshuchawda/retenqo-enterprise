export const PRODUCT_INFO = {
  android: {
    applicationId: 'com.mneme.app',
    minimumApi: 33,
    minimumOsLabel: 'Android 13',
    architectures: ['arm64-v8a'] as const,
  },
  model: {
    name: 'Qwen3.5-0.8B Q4_0',
    downloadSizeLabel: '~563 MB',
  },
} as const;

export const RELEASE_CONFIG = {
  githubOwner: 'priyanshuchawda',
  githubRepository: 'retenqo-enterprise',
  trustedSigningCertificateSha256:
    'b975fe4b5d929586e9bf0133ebde25c03d0acf990ddbba5c14f0f2b31ae38cca',
  maxApkSizeBytes: 250 * 1024 * 1024,
  apiTimeoutMs: 15_000,
  apkTimeoutMs: 60_000,
} as const;
