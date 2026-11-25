import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import {
  mkdtemp,
  readdir,
  rename,
  rm,
  unlink,
  writeFile,
} from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { z } from 'zod';
import { format } from 'prettier';
import { parseApkBadging } from './apk-metadata';
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
const HEX_SHA256 = /^[a-fA-F0-9]{64}$/;

const githubAssetSchema = z.object({
  name: z.string(),
  size: z.number().int().positive(),
  browser_download_url: z.string(),
  digest: z.string().nullable().optional(),
});

const githubReleaseSchema = z.object({
  tag_name: z.string().min(1),
  body: z.string().nullable().optional(),
  draft: z.boolean(),
  prerelease: z.boolean(),
  published_at: z.string().datetime(),
  html_url: z.string(),
  assets: z.array(githubAssetSchema),
});

async function writeAtomicJson(filePath: string, data: unknown): Promise<void> {
  const tmpPath = `${filePath}.${process.pid}.${Date.now()}.tmp`;
  try {
    const json = await format(`${JSON.stringify(data, null, 2)}\n`, {
      parser: 'json',
    });
    await writeFile(tmpPath, json, 'utf8');
    await rename(tmpPath, filePath);
  } catch (err) {
    await unlink(tmpPath).catch(() => {});
    throw err;
  }
}

async function resolveAapt(): Promise<string> {
  if (process.env.AAPT) return process.env.AAPT;
  const sdkRoot = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT;
  if (!sdkRoot) {
    throw new Error(
      'Set ANDROID_HOME or AAPT so release sync can inspect the downloaded APK.'
    );
  }
  const toolsPath = join(sdkRoot, 'build-tools');
  const versions = (await readdir(toolsPath, { withFileTypes: true }))
    .filter((item) => item.isDirectory())
    .map((item) => item.name)
    .sort(new Intl.Collator(undefined, { numeric: true }).compare)
    .reverse();
  const executable = process.platform === 'win32' ? 'aapt.exe' : 'aapt';
  const candidate = versions.map((version) =>
    join(toolsPath, version, executable)
  )[0];
  if (!candidate)
    throw new Error('No Android build-tools aapt executable was found.');
  return candidate;
}

async function inspectPublishedApk(url: string, expectedDigest: string) {
  const response = await fetch(url, { signal: AbortSignal.timeout(60_000) });
  if (!response.ok)
    throw new Error(`APK download returned HTTP ${response.status}.`);
  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length > 250 * 1024 * 1024)
    throw new Error('APK exceeds the 250 MiB release limit.');
  const actualDigest = createHash('sha256').update(bytes).digest('hex');
  if (actualDigest.toLowerCase() !== expectedDigest.toLowerCase()) {
    throw new Error(
      'Downloaded APK SHA-256 does not match the GitHub asset digest.'
    );
  }

  const directory = await mkdtemp(join(tmpdir(), 'retenqo-release-'));
  const apkPath = join(directory, basename(new URL(url).pathname));
  try {
    await writeFile(apkPath, bytes);
    const aapt = await resolveAapt();
    const output = execFileSync(aapt, ['dump', 'badging', apkPath], {
      encoding: 'utf8',
      timeout: 30_000,
      windowsHide: true,
    });
    return parseApkBadging(output);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

async function main(): Promise<void> {
  const now = new Date().toISOString();
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
  if (!response.ok)
    throw new Error(`GitHub API returned HTTP ${response.status}`);

  const release = githubReleaseSchema.parse(await response.json());
  if (release.draft) throw new Error('Draft releases cannot be synchronized.');
  if (!targetTag && release.prerelease) {
    throw new Error(
      'Latest release is a preview; use RELEASE_TAG=<tag> to sync it explicitly.'
    );
  }
  const apkAssets = release.assets.filter(
    (asset) =>
      asset.name.toLowerCase().endsWith('.apk') &&
      !asset.name.toLowerCase().includes('debug')
  );
  if (apkAssets.length !== 1 || !apkAssets[0]) {
    throw new Error(
      `Expected exactly one non-debug release APK, found ${apkAssets.length}.`
    );
  }
  const apkAsset = apkAssets[0];
  if (
    !isTrustedReleasePageUrl(release.html_url, release.tag_name) ||
    !isTrustedApkDownloadUrl(apkAsset.browser_download_url, release.tag_name)
  ) {
    throw new Error(
      'Release page and APK must use the same trusted release tag.'
    );
  }
  const digestHex = apkAsset.digest?.startsWith('sha256:')
    ? apkAsset.digest.slice(7)
    : '';
  if (!HEX_SHA256.test(digestHex)) {
    throw new Error('Missing or invalid SHA-256 digest on APK asset.');
  }

  const metadata = await inspectPublishedApk(
    apkAsset.browser_download_url,
    digestHex
  );
  const expectedVersionName = release.tag_name.replace(/^v/, '');
  if (metadata.versionName !== expectedVersionName) {
    throw new Error(
      `APK versionName ${metadata.versionName} does not match release ${release.tag_name}.`
    );
  }
  if (metadata.minimumAndroidApi < 33) {
    throw new Error(
      'APK minSdkVersion is below Retenqo’s supported Android API 33.'
    );
  }

  const channel = release.prerelease ? 'preview' : 'stable';
  const bodyFingerprint =
    /Signing certificate SHA-256:\s*`?([a-f0-9]{64})`?/i.exec(
      release.body ?? ''
    )?.[1];
  const signingCertSha256 =
    process.env.RETENQO_SIGNING_CERT_SHA256 ?? bodyFingerprint;
  if (signingCertSha256 && !HEX_SHA256.test(signingCertSha256)) {
    throw new Error(
      'Signing certificate fingerprint must be a SHA-256 hex digest.'
    );
  }
  if (channel === 'stable' && !signingCertSha256) {
    throw new Error(
      'Stable releases must publish the signing certificate SHA-256 fingerprint.'
    );
  }

  const state = releaseStateSchema.parse({
    status: 'available',
    syncedAt: now,
    release: {
      channel,
      version: release.tag_name,
      versionCode: metadata.versionCode,
      publishedAt: release.published_at,
      releaseUrl: release.html_url,
      apkUrl: apkAsset.browser_download_url,
      apkSizeBytes: apkAsset.size,
      sha256: digestHex,
      ...(signingCertSha256
        ? { signingCertificateSha256: signingCertSha256 }
        : {}),
      minimumAndroidApi: metadata.minimumAndroidApi,
      architectures: metadata.architectures,
    },
  });
  await writeAtomicJson(OUTPUT_PATH, state);
  console.log(
    `Synced ${channel} release ${state.status === 'available' ? state.release.version : ''}.`
  );
}

const invokedPath = process.argv[1]
  ? pathToFileURL(resolve(process.argv[1])).href
  : '';
if (import.meta.url === invokedPath) {
  main().catch((err: unknown) => {
    console.error('Release sync failed:', err);
    process.exitCode = 1;
  });
}
