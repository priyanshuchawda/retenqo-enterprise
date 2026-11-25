# Retenqo Website — Engineering Instructions

These rules apply to all human and AI-assisted changes inside the `web/` directory.

## Stack and constraints

- **Framework:** Astro (static output) · TypeScript strict mode · Tailwind CSS v4
- **Package manager:** pnpm with frozen lockfile — always use `--frozen-lockfile`
- **No CI/CD:** All checks are local only. Do not create `.github/workflows/` files inside `web/`.
- **No backend, no database, no authentication, no analytics, no tracking cookies.**
- **No `any` type, no non-null assertions (`!`), no `@ts-ignore`.**
- **No `lucide-astro`** — use emoji icons or inline SVGs only.

## Directory layout

```
web/
  public/           Static assets served at root (favicon, _headers, security.txt, illustrations)
  src/
    components/
      home/         Page-specific hero/section components
      layout/       Header, Footer
      ui/           Reusable atoms: Button, PhoneFrame, SectionHeading
    config/         site.ts — single source of truth for URLs and metadata
    data/           faq.ts, release.generated.json (auto-generated — do not hand-edit)
    layouts/        BaseLayout.astro
    lib/releases/   validation.ts — Zod schemas for release metadata
    pages/          One .astro file per route
    styles/         global.css — design tokens, utility classes
  scripts/          sync-release.ts — fetches GitHub release metadata
  tests/            Vitest unit tests + Playwright browser tests
  AGENTS.md         This file
  docs/             Architecture and reference documentation
```

## Workflow

1. **Never touch `../android/`** — that directory is completely off-limits.
2. Run all checks from inside `web/`: `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build`.
3. Fix all reported errors before committing. Record exact command output as evidence.
4. Use `Button.astro` for all primary/secondary CTA actions — never raw `<a>` tags with inline button styles.
5. All text claims must reflect verified implementation facts. Do not claim "encrypted at rest", "ASVS-aligned" without documentation, or "guaranteed" AI accuracy.

## Commands (run from `web/`)

| Task         | Command                          |
| ------------ | -------------------------------- |
| Install      | `pnpm install --frozen-lockfile` |
| Lint         | `pnpm lint`                      |
| Type check   | `pnpm typecheck`                 |
| Test         | `pnpm test`                      |
| Build        | `pnpm build`                     |
| Sync release | `pnpm release:sync`              |

## Deploy (run from monorepo root `../`)

```powershell
wrangler pages deploy web/dist --project-name=retenqo
```

## Design system

- Design tokens: `src/styles/global.css` — `--bg-*`, `--text-*`, `--accent-*`, `--border-*`
- Dark theme default; light theme via `[data-theme="light"]`
- Utility classes: `card-hover`, `glow-bg-blue`, `glow-bg-teal`, `grid-texture`
- Typography scale: `text-5xl`/`6xl`/`7xl` for hero H1, `text-3xl`/`4xl` for page H1
- Touch targets: minimum 44×44 CSS pixels on all interactive elements
- Motion: respect `prefers-reduced-motion` — no autoplay, no forced animations

## Content rules

- **Privacy claims:** Do not say "encrypted at rest" (Room is not encrypted by default). Backup exports are password-protected — that is accurate.
- **AI claims:** Say "references" or "cites" the source — not "verifies" or "guarantees".
- **Accessibility:** State it as a "target" unless independently tested and confirmed.
- **Permissions:** Every Android permission must be listed and explained in `permissions.astro`.
