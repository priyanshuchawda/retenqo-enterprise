# Design System — Midnight Intelligence

## Palette

The website uses the **Midnight/Clean Intelligence** palette — a dark-first design language inspired by Linear, Raycast, and Vercel.

### CSS custom properties (defined in `src/styles/global.css`)

| Token                | Dark value  | Purpose                       |
| -------------------- | ----------- | ----------------------------- |
| `--bg-primary`       | `#080D16`   | Page background               |
| `--bg-secondary`     | `#0D1420`   | Card backgrounds              |
| `--bg-tertiary`      | `#121B2E`   | Hover states                  |
| `--text-primary`     | `#F0F4FF`   | Headings, body text           |
| `--text-secondary`   | `#8892A4`   | Muted supporting text         |
| `--text-muted`       | `#4A5568`   | Timestamps, labels            |
| `--accent-primary`   | `#67D8FF`   | Interactive elements, accents |
| `--accent-secondary` | `#5EEAD4`   | Secondary highlights          |
| `--accent-hover`     | `#89E4FF`   | Hover on accent elements      |
| `--accent-soft`      | `rgba(...)` | Soft accent badge backgrounds |
| `--border-subtle`    | `#1E2D45`   | Card borders                  |
| `--status-success`   | `#4ADE80`   | Success indicators            |

## Typography

- Font: `Inter` (variable), fallback system UI stack
- Hero H1: `text-5xl sm:text-6xl lg:text-7xl`, weight 700, tracking tight
- Page H1: `text-3xl sm:text-4xl`, weight 700
- Section eyebrow: `text-xs uppercase tracking-widest font-semibold accent-primary`
- Body: `text-base leading-relaxed`
- Small: `text-sm leading-relaxed`

## Components

### `Button.astro`

Props: `href`, `variant` (`primary` | `secondary` | `ghost`), `size` (`sm` | `md` | `lg`), `disabled`, `type`

```astro
<Button href="/download" variant="primary" size="lg">Get Retenqo</Button>
```

### `SectionHeading.astro`

Props: `eyebrow`, `heading`, `subheading`

```astro
<SectionHeading
  eyebrow="Capabilities"
  heading="Built for private recall."
  subheading="Every feature prioritizes user consent."
/>
```

### `PhoneFrame.astro`

Props: `label` (aria-label), `sublabel`
Renders a CSS phone frame with a slot for screen content.

## Utility classes

| Class          | Effect                                 |
| -------------- | -------------------------------------- |
| `card-hover`   | Subtle lift + border glow on hover     |
| `glow-bg-blue` | Radial cyan glow overlay (hero accent) |
| `glow-bg-teal` | Radial teal glow overlay (CTA accent)  |
| `grid-texture` | Subtle dot-grid background texture     |

## Accessibility targets

- Text contrast: ≥ 4.5:1 for body text, ≥ 3:1 for large text (WCAG 2.2 AA)
- Interactive targets: minimum 44×44 CSS pixels
- Focus: visible outline using `:focus-visible` (3px offset)
- Motion: all transitions gated on `prefers-reduced-motion: no-preference`
- Color: information is never conveyed by color alone
