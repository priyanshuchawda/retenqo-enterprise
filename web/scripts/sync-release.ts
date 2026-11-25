import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import type { FileHandle } from 'node:fs/promises';
import {
  open,
  mkdtemp,
  readdir,
  rename,
  rm,
  unlink,
  writeFile,
} from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { z } from 'zod';
import { format } from 'prettier';
import {
  parseApkBadging,
  parseApkSignerFingerprint,
  type ApkMetadata,
} from './apk-metadata';
import {
  validateApkForRelease,
  assertAllowedApkDestination,
  isValidReleaseTag,
} from './release-validation';
import { PRODUCT_INFO, RELEASE_CONFIG } from '../src/config/product';
import {
  isTrustedApkDownloadUrl,
  isTrustedReleasePageUrl,
  releaseStateSchema,
} from '../src/lib/releases/validation';

const GITHUB_API_URL = `https://api.github.com/repos/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}/releases/latest`;
const GITHUB_TAG_API_URL = (tag: string) =>
  `https://api.github.com/repos/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}/releases/tags/${encodeURIComponent(tag)}`;
const OUTPUT_PATH = resolve(process.cwd(), 'src/data/release.generated.json');
const HEX_SHA256 = /^[a-fA-F0-9]{64}$/;

const githubAssetSchema = z.object({
  name: z.string().min(1),
  size: z.number().int().positive(),
  browser_download_url: z.url(),
  digest: z.string().nullable().optional(),
});

const githubReleaseSchema = z.object({
  tag_name: z
    .string()
    .regex(/^v\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$/),
  body: z.string().nullable().optional(),
  draft: z.boolean(),
  prerelease: z.boolean(),
  published_at: z.iso.datetime(),
  html_url: z.url(),
  assets: z.array(githubAssetSchema),
});

async function writeAtomicJson(filePath: string, data: unknown): Promise<void> {
  const temporaryPath = `${filePath}.${process.pid}.${Date.now()}.tmp`;
  try {
    const json = await format(`${JSON.stringify(data, null, 2)}\n`, {
      parser: 'json',
    });
    await writeFile(temporaryPath, json, { encoding: 'utf8', flag: 'wx' });
    await rename(temporaryPath, filePath);
  } catch (error) {
    await unlink(temporaryPath).catch(() => undefined);
    throw error;
  }
}

async function resolveAndroidTool(
  toolName: 'aapt' | 'apksigner'
): Promise<string> {
  const explicitPath = process.env[toolName === 'aapt' ? 'AAPT' : 'APKSIGNER'];
  if (explicitPath) {
    if (!existsSync(explicitPath))
      throw new Error(
        `${toolName} executable was not found at AAPT/APKSIGNER path.`
      );
    return explicitPath;
  }
  const sdkRoot = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT;
  if (!sdkRoot)
    throw new Error(
      `Set ANDROID_HOME or ${toolName === 'aapt' ? 'AAPT' : 'APKSIGNER'} to inspect release APKs.`
    );

  const toolsPath = join(sdkRoot, 'build-tools');
  const versions = (await readdir(toolsPath, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort(new Intl.Collator(undefined, { numeric: true }).compare)
    .reverse();
  const executableName =
    process.platform === 'win32'
      ? toolName === 'apksigner'
        ? 'lib/apksigner.jar'
        : `${toolName}.exe`
      : toolName;
  const candidates = versions.map((version) =>
    join(toolsPath, version, executableName)
  );
  const executable = candidates.find((candidate) => existsSync(candidate));
  if (!executable)
    throw new Error(`No Android build-tools ${toolName} executable was found.`);
  return executable;
}

interface InspectedApk {
  readonly metadata: ApkMetadata;
  readonly sizeBytes: number;
  readonly sha256: string;
  readonly signerSha256: string;
}

async function inspectPublishedApk(
  url: string,
  expectedSizeBytes: number,
  expectedDigest: string
): Promise<InspectedApk> {
  const response = await fetch(url, {
    signal: AbortSignal.timeout(RELEASE_CONFIG.apkTimeoutMs),
  });
  if (!response.ok)
    throw new Error(`APK download returned HTTP ${response.status}.`);
  assertAllowedApkDestination(response.url);
  const headerSize = response.headers.get('content-length');
  if (headerSize && Number(headerSize) > RELEASE_CONFIG.maxApkSizeBytes) {
    throw new Error('APK exceeds the configured maximum download size.');
  }
  if (!response.body)
    throw new Error('APK download response has no readable body.');

  const temporaryDirectory = await mkdtemp(join(tmpdir(), 'retenqo-release-'));
  const filename = basename(new URL(url).pathname);
  const apkPath = join(temporaryDirectory, filename);
  const file: FileHandle = await open(apkPath, 'wx');
  let fileClosed = false;
  try {
    const reader = response.body.getReader();
    const hash = createHash('sha256');
    let sizeBytes = 0;
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        sizeBytes += value.byteLength;
        if (sizeBytes > RELEASE_CONFIG.maxApkSizeBytes) {
          throw new Error('APK exceeds the configured maximum download size.');
        }
        hash.update(value);
        await file.writeFile(value);
      }
    } catch (error) {
      await reader.cancel().catch(() => undefined);
      throw error;
    } finally {
      reader.releaseLock();
    }
    await file.close();
    fileClosed = true;

    if (sizeBytes !== expectedSizeBytes) {
      throw new Error(
        'Downloaded APK size does not match GitHub release metadata.'
      );
    }
    const sha256 = hash.digest('hex');
    if (sha256.toLowerCase() !== expectedDigest.toLowerCase()) {
      throw new Error(
        'Downloaded APK SHA-256 does not match the GitHub asset digest.'
      );
    }

    const aapt = await resolveAndroidTool('aapt');
    const aaptOutput = execFileSync(aapt, ['dump', 'badging', apkPath], {
      encoding: 'utf8',
      timeout: 30_000,
      windowsHide: true,
    });
    const apksigner = await resolveAndroidTool('apksigner');
    const signerCommand = process.platform === 'win32' ? 'java' : apksigner;
    const signerArguments =
      process.platform === 'win32'
        ? ['-jar', apksigner, 'verify', '--verbose', '--print-certs', apkPath]
        : ['verify', '--verbose', '--print-certs', apkPath];
    const signerOutput = execFileSync(signerCommand, signerArguments, {
      encoding: 'utf8',
      timeout: 30_000,
      windowsHide: true,
    });
    return {
      metadata: parseApkBadging(aaptOutput),
      sizeBytes,
      sha256,
      signerSha256: parseApkSignerFingerprint(signerOutput),
    };
  } finally {
    if (!fileClosed) await file.close().catch(() => undefined);
    await rm(temporaryDirectory, { recursive: true, force: true });
  }
}

export interface SyncReleaseDependencies {
  readonly fetcher?: typeof fetch;
  readonly outputPath?: string;
  readonly releaseTag?: string;
  readonly inspectApk?: typeof inspectPublishedApk;
  readonly now?: () => Date;
}

export async function synchronizeRelease(
  dependencies: SyncReleaseDependencies = {}
): Promise<'synced' | 'preserved'> {
  const fetcher = dependencies.fetcher ?? fetch;
  const outputPath = dependencies.outputPath ?? OUTPUT_PATH;
  const targetTag = dependencies.releaseTag;
  const inspectApk = dependencies.inspectApk ?? inspectPublishedApk;
  const now = (dependencies.now ?? (() => new Date()))().toISOString();

  if (targetTag && !isValidReleaseTag(targetTag))
    throw new Error('RELEASE_TAG is malformed.');
  const apiUrl = targetTag ? GITHUB_TAG_API_URL(targetTag) : GITHUB_API_URL;
  const response = await fetcher(apiUrl, {
    headers: {
      Accept: 'application/vnd.github+json',
      'User-Agent': 'retenqo-web-release-sync',
    },
    signal: AbortSignal.timeout(RELEASE_CONFIG.apiTimeoutMs),
  });
  if (response.status === 404) {
    console.log(`No matching GitHub release found; preserved ${outputPath}.`);
    return 'preserved';
  }
  if (!response.ok)
    throw new Error(`GitHub API returned HTTP ${response.status}.`);

  const release = githubReleaseSchema.parse(await response.json());
  if (targetTag && release.tag_name !== targetTag)
    throw new Error('GitHub returned a different release tag than requested.');
  if (release.draft) throw new Error('Draft releases cannot be synchronized.');
  if (!targetTag && release.prerelease)
    throw new Error('Latest release is a preview; select a tag explicitly.');
  const apkAssets = release.assets.filter(
    (asset) =>
      asset.name.toLowerCase().endsWith('.apk') &&
      !asset.name.toLowerCase().includes('debug')
  );
  if (apkAssets.length !== 1 || !apkAssets[0])
    throw new Error(
      `Expected exactly one non-debug release APK, found ${apkAssets.length}.`
    );

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
  if (!HEX_SHA256.test(digestHex))
    throw new Error('Missing or invalid SHA-256 digest on APK asset.');

  const inspected = await inspectApk(
    apkAsset.browser_download_url,
    apkAsset.size,
    digestHex
  );
  validateApkForRelease(inspected.metadata, {
    tag: release.tag_name,
    packageName: PRODUCT_INFO.android.applicationId,
    expectedSizeBytes: apkAsset.size,
    actualSizeBytes: inspected.sizeBytes,
    expectedSha256: digestHex,
    actualSha256: inspected.sha256,
    signerSha256: inspected.signerSha256,
  });

  const channel = release.prerelease ? 'preview' : 'stable';
  const state = releaseStateSchema.parse({
    status: 'available',
    syncedAt: now,
    release: {
      channel,
      version: release.tag_name,
      versionCode: inspected.metadata.versionCode,
      publishedAt: release.published_at,
      releaseUrl: release.html_url,
      apkUrl: apkAsset.browser_download_url,
      apkSizeBytes: inspected.sizeBytes,
      sha256: inspected.sha256,
      signingCertificateSha256: inspected.signerSha256,
      minimumAndroidApi: inspected.metadata.minimumAndroidApi,
      architectures: inspected.metadata.architectures,
    },
  });
  await writeAtomicJson(outputPath, state);
  return 'synced';
}

async function main(): Promise<void> {
  const result = await synchronizeRelease({
    ...(process.env.RELEASE_TAG ? { releaseTag: process.env.RELEASE_TAG } : {}),
  });
  if (result === 'synced')
    console.log('Synchronized and verified GitHub release metadata.');
}

const invokedPath = process.argv[1]
  ? pathToFileURL(resolve(process.argv[1])).href
  : '';
if (import.meta.url === invokedPath) {
  main().catch((error: unknown) => {
    const message =
      error instanceof Error
        ? error.message
        : 'Unknown release synchronization error.';
    console.error(`Release sync failed: ${message}`);
    process.exitCode = 1;
  });
}
