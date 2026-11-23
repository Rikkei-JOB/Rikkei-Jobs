// Run from repository root: node docs/design/ptit-reference/capture.cjs
// Uses the existing web Playwright dependency; does not submit applications.
const { chromium } = require('../../../web/node_modules/playwright');
const fs = require('node:fs/promises');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const records = [];
  try {
    for (const [device, viewport] of Object.entries({
      desktop: { width: 1440, height: 1000 },
      tablet: { width: 834, height: 1112 },
      mobile: { width: 390, height: 844 },
    })) {
      const page = await browser.newPage({ viewport, deviceScaleFactor: 1 });
      async function capture(step) {
        await page.evaluate(() => document.fonts.ready);
        await page.waitForTimeout(800); // Allow route and overlay animations to finish.
        await page.evaluate(() => window.scrollTo(0, 0));
        const file = `${device}-${step}.png`;
        await page.screenshot({ path: path.join(__dirname, file), animations: 'disabled' });
        await page.screenshot({ path: path.join(__dirname, `${device}-${step}-full.png`), fullPage: true, animations: 'disabled' });
        const inspection = await page.evaluate(() => {
          const selectors = ['h1', 'h2', 'input', 'button'];
          return selectors.flatMap(selector => Array.from(document.querySelectorAll(selector))
            .filter(el => el.getBoundingClientRect().width && el.getBoundingClientRect().height)
            .slice(0, 8).map(el => {
              const s = getComputedStyle(el), r = el.getBoundingClientRect();
              return { tag: el.tagName, text: el.textContent.trim().slice(0, 100), font: s.fontFamily,
                size: s.fontSize, weight: s.fontWeight, lineHeight: s.lineHeight,
                color: s.color, background: s.backgroundColor, radius: s.borderRadius,
                padding: s.padding, width: Math.round(r.width), height: Math.round(r.height) };
            }));
        });
        records.push({ device, viewport, step, file, url: page.url(), capturedAt: new Date().toISOString(),
          text: await page.locator('body').innerText(), inspection });
        console.log(`${device}: ${step} ${page.url()}`);
      }
      await page.goto('https://jobs.ptit.edu.vn/jobs', { waitUntil: 'networkidle', timeout: 60000 });
      await page.getByText('Việc làm nổi bật', { exact: false }).first().waitFor();
      await capture('01-jobs');
      // Navigate directly to the detail URL observed in the earlier reference capture.
      // Hovering a card can open a preview instead of navigating to the detail page.
      await page.goto('https://jobs.ptit.edu.vn/jobs/6ad6de815067945568f0d6ee', { waitUntil: 'networkidle', timeout: 60000 });
      await page.getByRole('button', { name: 'Ứng tuyển ngay', exact: true }).waitFor();
      await capture('02-detail');
      await page.getByRole('button', { name: 'Ứng tuyển ngay', exact: true }).click();
      await page.getByText('Đăng nhập Sinh Viên', { exact: true }).waitFor();
      await capture('03-apply-login');
      if (device === 'desktop') {
        await page.goto('https://jobs.ptit.edu.vn/jobs/all', { waitUntil: 'networkidle', timeout: 60000 });
        await page.getByText('Yêu cầu đăng nhập', { exact: true }).waitFor();
        await capture('04-all-login-required');
      }
      await page.close();
    }
  } finally {
    await fs.writeFile(path.join(__dirname, 'observations.json'), JSON.stringify(records, null, 2));
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
