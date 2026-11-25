import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { readFileSync } from 'node:fs';
import { parseReleaseState } from '../../src/lib/releases/validation';

const releaseData: unknown = JSON.parse(
  readFileSync(
    new URL('../../src/data/release.generated.json', import.meta.url),
    'utf8'
  )
);

const ROUTES = [
  '/',
  '/features',
  '/how-it-works',
  '/download',
  '/installation',
  '/faq',
  '/privacy',
  '/security',
  '/permissions',
  '/support',
  '/accessibility',
];

for (const route of ROUTES) {
  for (const theme of ['light', 'dark'] as const) {
    test(`${route} passes axe in ${theme} theme`, async ({
      page,
    }, testInfo) => {
      test.skip(
        testInfo.project.name === 'firefox' ||
          testInfo.project.name === 'webkit',
        'Full axe scans are run in Chromium desktop and mobile; cross-engine behavior has a dedicated smoke suite.'
      );
      const errors: string[] = [];
      page.on('pageerror', (error) => errors.push(error.message));
      await page.goto(route);
      await page.getByLabel('Color theme').selectOption(theme);
      await expect(page.locator('html')).toHaveAttribute('data-theme', theme);
      const results = await new AxeBuilder({ page }).analyze();
      expect(results.violations).toEqual([]);
      expect(errors).toEqual([]);
    });
  }
}

for (const route of ROUTES) {
  test(`${route} renders and switches themes in Firefox and WebKit`, async ({
    page,
  }, testInfo) => {
    test.skip(
      testInfo.project.name !== 'firefox' && testInfo.project.name !== 'webkit'
    );
    await page.goto(route);
    const theme = page.getByLabel('Color theme');
    await theme.selectOption('light');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
    await theme.selectOption('dark');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  });
}

test('system theme follows OS and persisted theme survives reload', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'dark' });
  await page.goto('/');
  const selector = page.getByLabel('Color theme');
  await expect(selector).toHaveValue('system');
  await expect(page.locator('html')).not.toHaveAttribute('data-theme');
  await selector.selectOption('light');
  await page.reload();
  await expect(selector).toHaveValue('light');
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
});

test('theme selector still works when browser storage throws', async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, 'localStorage', {
      configurable: true,
      get() {
        throw new DOMException('Storage denied', 'SecurityError');
      },
    });
  });
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/');
  await page.getByLabel('Color theme').selectOption('dark');
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
  expect(errors).toEqual([]);
});

test('copy button copies release data and announces success', async ({
  page,
  context,
  browserName,
}) => {
  test.skip(
    browserName !== 'chromium',
    'Clipboard permission automation is Chromium-only.'
  );
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  await page.goto('/download');
  const checksum = page.locator('[data-copy-value]').first();
  const value = await checksum.getAttribute('data-copy-value');
  await checksum.click();
  await expect(checksum.locator('..').getByRole('status')).toHaveText('Copied');
  await expect
    .poll(() => page.evaluate(() => navigator.clipboard.readText()))
    .toBe(value);
});

test('mobile menu opens by keyboard and internal routes resolve', async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 740 });
  await page.goto('/');
  const menu = page.getByLabel('Toggle navigation menu');
  await menu.focus();
  await page.keyboard.press('Enter');
  await expect(
    page.getByRole('navigation', { name: 'Mobile navigation' })
  ).toBeVisible();
  await page
    .getByRole('link', { name: 'Download', exact: true })
    .last()
    .click();
  await expect(page).toHaveURL(/\/download\/?$/);
});

test('site requests no third-party resources and does not overflow at target widths', async ({
  page,
}) => {
  const externalOrigins = new Set<string>();
  let localOrigin: string | null = null;
  page.on('request', (request) => {
    const url = new URL(request.url());
    if (localOrigin === null && request.isNavigationRequest()) {
      localOrigin = url.origin;
    } else if (url.origin !== localOrigin) {
      externalOrigins.add(url.origin);
    }
  });
  for (const width of [320, 768, 1920]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/');
    const dimensions = await page.evaluate(() => ({
      body: document.body.scrollWidth,
      viewport: document.documentElement.clientWidth,
    }));
    expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
  }
  expect(externalOrigins).toEqual(new Set());
});

test('all internal links resolve and external links use secure schemes', async ({
  page,
  request,
}) => {
  const internalPaths = new Set<string>();
  for (const route of ROUTES) {
    await page.goto(route);
    const links = await page.locator('a[href]').evaluateAll((anchors) =>
      anchors.flatMap((anchor) => {
        const href = anchor.getAttribute('href');
        return href ? [href] : [];
      })
    );
    for (const href of links) {
      const url = new URL(href, page.url());
      if (url.origin === new URL(page.url()).origin) {
        internalPaths.add(`${url.pathname}${url.search}`);
      } else {
        expect(['https:', 'mailto:']).toContain(url.protocol);
      }
    }
  }
  for (const path of internalPaths) {
    const response = await request.get(path);
    expect(response.status(), `Internal link ${path}`).toBe(200);
  }
});

test('sitemap, robots, canonical, and software metadata are present', async ({
  page,
  request,
}) => {
  const robots = await request.get('/robots.txt');
  expect(robots.status()).toBe(200);
  expect(await robots.text()).toContain('sitemap-index.xml');
  const sitemap = await request.get('/sitemap-index.xml');
  expect(sitemap.status()).toBe(200);
  await page.goto('/');
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
    'href',
    'https://retenqo.pages.dev/'
  );
  const structuredData = await page
    .locator('script[type="application/ld+json"]')
    .textContent();
  expect(JSON.parse(structuredData ?? '{}')).toMatchObject({
    '@type': 'SoftwareApplication',
    name: 'Retenqo',
    applicationCategory: 'ProductivityApplication',
  });
  await expect(page.locator('meta[property="og:image"]')).toHaveAttribute(
    'content',
    'https://retenqo.pages.dev/og-image.png'
  );
});

test('download link points to the verified release asset', async ({ page }) => {
  const state = parseReleaseState(releaseData);
  await page.goto('/download');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(
    'Retenqo for Android'
  );
  if (state.status === 'pending') {
    await expect(page.getByText('Awaiting first release')).toBeVisible();
    return;
  }

  const downloadLink = page
    .getByRole('link')
    .filter({ hasText: `Download ${state.release.version} APK` });
  await expect(downloadLink).toHaveAttribute('href', state.release.apkUrl);
});

test('404 page renders with a not-found response', async ({ page }) => {
  const response = await page.goto('/non-existent-route-12345');
  expect(response?.status()).toBe(404);
  await expect(
    page.getByRole('heading', { name: 'Page not found' })
  ).toBeVisible();
});
