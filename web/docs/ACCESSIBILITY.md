# Accessibility Evidence

## Target standard

WCAG 2.2 Level AA — this is a **target**, not a certified result.

## Implemented measures

| Item                                                   | Status    | Notes                                                                           |
| ------------------------------------------------------ | --------- | ------------------------------------------------------------------------------- |
| Semantic landmarks (`header`, `nav`, `main`, `footer`) | ✅ Done   | All pages use correct HTML5 landmarks in BaseLayout                             |
| Logical heading hierarchy (H1 → H2 → H3)               | ✅ Done   | Each page has exactly one H1; sections use H2/H3                                |
| Skip-to-content link                                   | ✅ Done   | First focusable element in BaseLayout; visible on focus                         |
| Native keyboard navigation                               | ✅ Done   | Native controls and representative keyboard tests; full manual walkthrough pending                                 |
| Visible focus indicators                               | ✅ Done   | `:focus-visible` with 3px offset ring in global.css                             |
| Minimum 44×44 CSS pixel touch targets                  | ✅ Done   | 44px target for primary controls; audit remaining targets manually                                          |
| Color contrast ≥ 4.5:1 for body text                   | 🎯 Target | Tokens designed for AA compliance; not yet independently verified               |
| `prefers-reduced-motion` support                       | ✅ Done   | CSS transitions disabled via media query in global.css                          |
| System light/dark theme support                        | ✅ Done   | CSS custom properties respond to `[data-theme="light"]`                         |
| Decorative SVGs hidden from assistive technology       | ✅ Done   | `aria-hidden="true"` on all decorative elements                                 |
| Descriptive alt text for meaningful images             | ✅ Done   | Phone frame uses `aria-label`; illustrations have `role="img"` and `aria-label` |
| Screen-reader-friendly FAQ disclosures                 | ✅ Done   | Native `<details>/<summary>` — no JS, no ARIA hacks                             |
| No information conveyed by color alone                 | ✅ Done   | Icons and text labels accompany all color-coded states                          |
| 320px responsive layout                                | ✅ Done   | Mobile-first CSS; tested to 320px viewport                                      |
| Accurate accessibility statement                       | ✅ Done   | `accessibility.astro` reflects actual implementation                            |

## Items requiring manual verification

| Item                                  | How to verify                                                           |
| ------------------------------------- | ----------------------------------------------------------------------- |
| Color contrast ratios                 | Run axe DevTools or Colour Contrast Analyser on deployed site           |
| Screen reader — NVDA (Windows)        | Navigate all pages with NVDA + Chrome                                   |
| Screen reader — TalkBack (Android)    | Load site on Android device; navigate all interactive elements          |
| Screen reader — VoiceOver (macOS/iOS) | Navigate all pages with VoiceOver + Safari                              |
| Keyboard-only navigation              | Tab through every interactive element on every page                     |
| 200% and 400% browser zoom            | Test in Chrome at 200% and 400% zoom                                    |
| Download state accessibility          | Verify download button accessible name in both pending/available states |

## Known limitations

- CSP restricts executable scripts to self-hosted resources; inline styles remain allowed.
- Sticky header focus-scroll behavior has not been manually verified with screen readers.

## Automated testing

Run `pnpm test:e2e` to execute Playwright browser tests, which include basic accessibility checks. For comprehensive automated auditing:

```powershell
# Install axe-cli globally
npm install -g @axe-core/cli

# Run against local build
npx serve web/dist -l 4321 &
axe http://localhost:4321 --exit
axe http://localhost:4321/features --exit
axe http://localhost:4321/download --exit
axe http://localhost:4321/permissions --exit
```

Recorded browser baseline: Chromium desktop/mobile axe checks in light and dark themes; Firefox/WebKit smoke checks. Manual screen-reader and 200% zoom verification remain open. See [engineering audit](ENGINEERING_AUDIT.md).
