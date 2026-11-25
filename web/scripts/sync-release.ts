import { rename, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { z } from 'zod';
import {
  isTrustedApkDownloadUrl,
  isTrustedReleasePageUrl,
  releaseStateSchema,
  type ReleaseState,
} from '../src/lib/releases/validation';

const GITHUB_API_URL =
  'https://api.github.com/repos/priyanshuchawda/retenqo-enterprise/releases/latest';
const GITHUB_TAG_API_URL = (tag: string) =>
  `https://api.github.com/repos/priyanshuchawda/retenqo-enterprise/releases/tags/${tag}`;
const OUTPUT_PATH = resolve(process.cwd(), 'src/data/release.generated.json');

const githubAssetSchema = z.object({
  name: z.string(),
  size: z.number().int().positive(),
  browser_download_url: z.string(),
  digest: z.string().nullable().optional(),
});

const githubReleaseSchema = z.object({
  tag_name: z.string().min(1),
  draft: z.boolean(),
  prerelease: z.boolean(),
  published_at: z.string().datetime(),
  html_url: z.string(),
  assets: z.array(githubAssetSchema),
});

async function writeAtomicJson(filePath: string, data: unknown): Promise<void> {
  const tmpPath = `${filePath}.tmp`;
  await writeFile(tmpPath, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
  await rename(tmpPath, filePath);
}

async function main(): Promise<void> {
  const now = new Date().toISOString();
  // Try /releases/latest first; fall back to tag-specific endpoint when given via RELEASE_TAG env var
  const targetTag = process.env.RELEASE_TAG;
  const apiUrl = targetTag ? GITHUB_TAG_API_URL(targetTag) : GITHUB_API_URL;

  const response = await fetch(apiUrl, {
    headers: {
      Accept: 'application/vnd.github+json',
      'User-Agent': 'retenqo-web-release-sync',
    },
    signal: AbortSignal.timeout(15_000),
  });

  if (response.status === 404) {
    const pending: ReleaseState = {
      status: 'pending',
      syncedAt: now,
      reason: 'Awaiting first official public release on GitHub Releases.',
    };
    await writeAtomicJson(OUTPUT_PATH, pending);
    console.log('No public release found (404). Wrote pending state.');
    return;
  }

  if (!response.ok) {
    throw new Error(`GitHub API returned HTTP ${response.status}`);
  }

  const parsed = githubReleaseSchema.parse(await response.json());
  // Allow pre-releases when explicitly syncing a specific tag
  if (!targetTag && (parsed.draft || parsed.prerelease)) {
    throw new Error(
      'Latest release is draft or prerelease; use RELEASE_TAG=<tag> to sync a specific release.'
    );
  }

  const apkAssets = parsed.assets.filter(
    (asset) => asset.name.endsWith('.apk') && !asset.name.includes('debug')
  );
  if (apkAssets.length !== 1 || !apkAssets[0]) {
    throw new Error(
      `Expected exactly 1 release APK asset, found ${apkAssets.length}.`
    );
  }
  const apkAsset = apkAssets[0];

  if (
    !isTrustedReleasePageUrl(parsed.html_url) ||
    !isTrustedApkDownloadUrl(apkAsset.browser_download_url, parsed.tag_name)
  ) {
    throw new Error('Untrusted release or APK URL detected.');
  }

  const digestHex = apkAsset.digest?.startsWith('sha256:')
    ? apkAsset.digest.slice(7)
    : '';
  if (!/^[a-fA-F0-9]{64}$/.test(digestHex)) {
    throw new Error('Missing or invalid SHA-256 digest on APK asset.');
  }

  const signingCertSha256 = process.env.RETENQO_SIGNING_CERT_SHA256 ?? '';
  const certField: { signingCertificateSha256?: string } =
    /^[a-fA-F0-9]{64}$/.test(signingCertSha256)
      ? { signingCertificateSha256: signingCertSha256 }
      : {};

  const state = releaseStateSchema.parse({
    status: 'available',
    syncedAt: now,
    release: {
      version: parsed.tag_name,
      versionCode: 1,
      publishedAt: parsed.published_at,
      releaseUrl: parsed.html_url,
      apkUrl: apkAsset.browser_download_url,
      apkSizeBytes: apkAsset.size,
      sha256: digestHex,
      ...certField,
      minimumAndroidApi: 33,
      architectures: ['arm64-v8a'],
    },
  });

  await writeAtomicJson(OUTPUT_PATH, state);
  if (state.status === 'available') {
    console.log(`Synced release ${state.release.version}`);
  }
}

main().catch((err: unknown) => {
  console.error('Release sync failed:', err);
  process.exitCode = 1;
});
