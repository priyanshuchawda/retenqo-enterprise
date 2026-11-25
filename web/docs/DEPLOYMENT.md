# Deployment Guide

## Prerequisites

- Node.js 20+
- pnpm 9+
- Wrangler CLI (`npm install -g wrangler`) — authenticated via `wrangler login`
- Access to Cloudflare Pages project `retenqo` under account `hellopriyanshu4@gmail.com`

## Build

```powershell
# From the web/ directory
cd C:\Users\Admin\Desktop\mneme\web
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

The build output is written to `web/dist/`.

## Deploy to Cloudflare Pages

**Must be run from the monorepo root** (not from inside `web/`):

```powershell
cd C:\Users\Admin\Desktop\mneme
wrangler pages deploy web/dist --project-name=retenqo
```

- **Project name:** `retenqo`
- **Production branch:** `main`
- **Live URL:** https://retenqo.pages.dev

## Update release metadata

After a new APK is published to GitHub Releases:

```powershell
cd C:\Users\Admin\Desktop\mneme\web
pnpm release:sync
```

This calls `scripts/sync-release.ts` which fetches the latest release from the GitHub API and writes `src/data/release.generated.json`. Commit this file and redeploy.

## Environment variables

| Variable                      | Purpose                                                                                                                                                            |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `RETENQO_SIGNING_CERT_SHA256` | SHA-256 of the APK signing certificate (not the file digest). Used by `sync-release.ts`. Optional — if unset, the cert fingerprint field is omitted from metadata. |
| `RELEASE_VERSION_CODE`        | Positive Android `versionCode` embedded in the APK being published. Required by `sync-release.ts`; read it from the app build configuration for that release.      |
| `CLOUDFLARE_API_TOKEN`        | Alternative to interactive `wrangler login`. Use in automated/non-interactive environments.                                                                        |

## Notes

- Wrangler uses an OAuth token stored from `wrangler login`. In non-interactive shell contexts, set `CLOUDFLARE_API_TOKEN` instead.
- Do not create GitHub Actions CI/CD workflows for this project.
- The build must pass all checks (`lint`, `typecheck`, `test`) before deploy.
