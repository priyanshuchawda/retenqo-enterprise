# Performance Budgets and Measurements

## Engineering targets

These are targets, not measured results. Run Lighthouse against the deployed site to establish actual values.

| Metric                         | Target    |
| ------------------------------ | --------- |
| Lighthouse Performance score   | ≥ 95      |
| Largest Contentful Paint (LCP) | ≤ 2.5 s   |
| Cumulative Layout Shift (CLS)  | ≤ 0.1     |
| Interaction to Next Paint (INP)| ≤ 200 ms  |
| Homepage transfer size         | ≤ 500 KB  |
| Executable JavaScript          | 0–10 KB   |
| Compressed CSS                 | ≤ 30 KB   |
| Third-party tracking requests  | 0         |

## Why these targets are achievable

- **Astro static output** — zero JS shipped by default; all pages are static HTML
- **Tailwind CSS** — Tailwind purges unused classes; production CSS is small
- **No third-party scripts** — no analytics, no ads, no tracking pixels
- **SVG illustrations** — custom SVGs are lightweight and scale perfectly
- **System fonts / Inter variable** — one WOFF2 file, cached aggressively

## Asset checklist

| Asset type         | Requirement                                        |
| ------------------ | -------------------------------------------------- |
| App screenshots    | WebP or AVIF, `width`/`height` set, lazy-load below fold |
| Hero image         | `fetchpriority="high"`, not lazy                   |
| SVG illustrations  | Optimized with SVGO, `aria-hidden` on decorative   |
| Fonts              | Self-hosted WOFF2 variable font; `font-display: swap` |
| CSS                | Tailwind purge enabled; no duplicate utilities     |
| JavaScript         | Astro static mode; no client-side hydration        |

## Measuring performance

### Lighthouse (local)
```powershell
# After pnpm build, serve dist and run Lighthouse
npx serve web/dist -l 4321
npx lighthouse http://localhost:4321 --output=html --output-path=web/eval-results/lighthouse.html
```

### Bundle size
```powershell
cd web
pnpm build
# Check dist/_astro/ directory for JS and CSS sizes
Get-ChildItem web/dist/_astro -Recurse | Select-Object Name, Length | Sort-Object Length -Descending
```

### Transfer size
Use Chrome DevTools → Network tab with cache disabled, throttled to Slow 3G, to measure total transfer for the homepage.

## Caching strategy

Cloudflare Pages automatically sets:
- `Cache-Control: public, max-age=31536000, immutable` for fingerprinted assets (`_astro/*.css`, `_astro/*.js`)
- `Cache-Control: no-cache` for HTML pages (re-validated on each visit)

No additional cache configuration is required for a static site.

## Actual measurements

> **TODO:** Run Lighthouse after deploy and record results here.

| Date | Commit | LCP | CLS | INP | Performance score |
| ---- | ------ | --- | --- | --- | ----------------- |
| TBD  | TBD    | TBD | TBD | TBD | TBD               |
