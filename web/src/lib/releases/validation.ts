import { z } from 'zod';

const TRUSTED_OWNER = 'priyanshuchawda';
const TRUSTED_REPO = 'retenqo-enterprise';
const TRUSTED_PREFIX = `/${TRUSTED_OWNER}/${TRUSTED_REPO}/releases/`;

function hasValidBaseUrl(rawUrl: string): URL | null {
  try {
    const parsed = new URL(rawUrl);
    if (parsed.protocol !== 'https:') return null;
    if (parsed.hostname !== 'github.com') return null;
    if (parsed.port !== '') return null;
    if (parsed.username !== '' || parsed.password !== '') return null;
    if (parsed.search !== '' || parsed.hash !== '') return null;
    const lowerPath = parsed.pathname.toLowerCase();
    if (
      lowerPath.includes('..') ||
      lowerPath.includes('%2e') ||
      lowerPath.includes('%2f')
    ) {
      return null;
    }
    if (!parsed.pathname.startsWith(TRUSTED_PREFIX)) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function isTrustedReleasePageUrl(
  rawUrl: string,
  expectedTag?: string
): boolean {
  const parsed = hasValidBaseUrl(rawUrl);
  if (!parsed) return false;
  const match =
    /^\/priyanshuchawda\/retenqo-enterprise\/releases\/tag\/([A-Za-z0-9._-]+)$/.exec(
      parsed.pathname
    );
  return (
    match !== null && (expectedTag === undefined || match[1] === expectedTag)
  );
}

export function isTrustedApkDownloadUrl(
  rawUrl: string,
  expectedTag?: string
): boolean {
  const parsed = hasValidBaseUrl(rawUrl);
  if (!parsed) return false;
  const match =
    /^\/priyanshuchawda\/retenqo-enterprise\/releases\/download\/([A-Za-z0-9._-]+)\/([A-Za-z0-9._-]+\.apk)$/.exec(
      parsed.pathname
    );
  if (!match) return false;
  if (expectedTag !== undefined && match[1] !== expectedTag) return false;
  return true;
}

export const sha256Schema = z
  .string()
  .regex(/^[a-fA-F0-9]{64}$/, 'Invalid SHA-256 hex digest');

export const androidReleaseSchema = z
  .object({
    channel: z.enum(['stable', 'preview']),
    version: z.string().min(1).max(32),
    versionCode: z.number().int().positive(),
    publishedAt: z.string().datetime(),
    releaseUrl: z.string(),
    apkUrl: z
      .string()
      .refine(
        (url) => isTrustedApkDownloadUrl(url),
        'Untrusted APK download URL'
      ),
    apkSizeBytes: z.number().int().positive(),
    sha256: sha256Schema,
    signingCertificateSha256: sha256Schema.optional(),
    minimumAndroidApi: z.number().int().min(33),
    architectures: z
      .array(z.enum(['arm64-v8a', 'armeabi-v7a', 'x86_64']))
      .min(1),
  })
  .refine((data) => isTrustedReleasePageUrl(data.releaseUrl, data.version), {
    message: 'Release page tag must match release version',
    path: ['releaseUrl'],
  })
  .refine((data) => isTrustedApkDownloadUrl(data.apkUrl, data.version), {
    message: 'APK download URL tag must match release version',
    path: ['apkUrl'],
  })
  .refine(
    (data) =>
      data.channel !== 'stable' || data.signingCertificateSha256 !== undefined,
    {
      message:
        'Stable releases must include the signing certificate SHA-256 fingerprint',
      path: ['signingCertificateSha256'],
    }
  );

export const releaseStateSchema = z.discriminatedUnion('status', [
  z.object({
    status: z.literal('available'),
    syncedAt: z.string().datetime(),
    release: androidReleaseSchema,
  }),
  z.object({
    status: z.literal('pending'),
    syncedAt: z.string().datetime(),
    reason: z.string().min(1),
  }),
]);

export type AndroidRelease = z.infer<typeof androidReleaseSchema>;
export type ReleaseState = z.infer<typeof releaseStateSchema>;

export function parseReleaseState(input: unknown): ReleaseState {
  return releaseStateSchema.parse(input);
}

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KiB', 'MiB', 'GiB'] as const;
  const idx = Math.min(
    Math.floor(Math.log(bytes) / Math.log(1024)),
    units.length - 1
  );
  const value = bytes / 1024 ** idx;
  return `${value.toFixed(idx === 0 ? 0 : 1)} ${units[idx]}`;
}
