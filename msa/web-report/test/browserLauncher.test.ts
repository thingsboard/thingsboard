// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { describe, it } from 'node:test';
import { strict as assert } from 'node:assert';
import { Browser, LaunchOptions } from 'playwright-core';
import {
    browserLaunchTargets,
    checkBrowserVersion,
    launchBrowser,
    minChromeMajor
} from '../browserLauncher';

const existing = (...paths: string[]) => (path: string) => paths.includes(path);

describe('browserLaunchTargets', () => {
    it('uses an existing CHROME_EXECUTABLE only', () => {
        const plan = browserLaunchTargets(' /opt/chrome/chrome ', 'linux', existing('/opt/chrome/chrome', '/usr/bin/chromium'));
        assert.deepEqual(plan, { targets: [{ executablePath: '/opt/chrome/chrome' }], warnings: [] });
    });

    it('warns about a missing CHROME_EXECUTABLE and falls back to Google Chrome', () => {
        const plan = browserLaunchTargets('/usr/share/tb-web-report/chromium/chrome', 'win32', existing());
        assert.deepEqual(plan.targets, [{ channel: 'chrome' }]);
        assert.equal(plan.warnings.length, 1);
        assert.match(plan.warnings[0], /CHROME_EXECUTABLE '\/usr\/share\/tb-web-report\/chromium\/chrome' does not exist/);
    });

    it('ignores a blank CHROME_EXECUTABLE', () => {
        assert.deepEqual(browserLaunchTargets('  ', 'darwin', existing()), { targets: [{ channel: 'chrome' }], warnings: [] });
    });

    it('falls back to the installed Linux Chromium after Google Chrome', () => {
        const plan = browserLaunchTargets(undefined, 'linux', existing('/usr/bin/chromium-browser'));
        assert.deepEqual(plan.targets, [{ channel: 'chrome' }, { executablePath: '/usr/bin/chromium-browser' }]);
    });

    it('does not look for Linux Chromium on other platforms', () => {
        for (const platform of ['win32', 'darwin'] as const) {
            const plan = browserLaunchTargets(undefined, platform, existing('/usr/bin/chromium'));
            assert.deepEqual(plan.targets, [{ channel: 'chrome' }], platform);
        }
    });
});

describe('launchBrowser', () => {
    const browser = {} as Browser;
    const base: LaunchOptions = { headless: true };

    it('launches the first target that starts and reports skipped ones', async () => {
        const calls: LaunchOptions[] = [];
        const skipped: string[] = [];
        const launch = async (options: LaunchOptions) => {
            calls.push(options);
            if (options.channel) {
                throw new Error('Chromium distribution \'chrome\' is not found');
            }
            return browser;
        };
        const result = await launchBrowser(launch, base, [{ channel: 'chrome' }, { executablePath: '/usr/bin/chromium' }],
            (message: string) => skipped.push(message));
        assert.equal(result, browser);
        assert.deepEqual(calls, [{ headless: true, channel: 'chrome' }, { headless: true, executablePath: '/usr/bin/chromium' }]);
        assert.equal(skipped.length, 1);
        assert.match(skipped[0], /Google Chrome.*is not found/);
    });

    it('throws the errors of every target when none starts', async () => {
        const launch = async (options: LaunchOptions): Promise<Browser> => {
            throw new Error(options.channel ? 'no chrome' : 'chromium crashed');
        };
        const skipped: string[] = [];
        await assert.rejects(launchBrowser(launch, base, [{ channel: 'chrome' }, { executablePath: '/usr/bin/chromium' }],
                (message: string) => skipped.push(message)),
            /Google Chrome: no chrome[\s\S]*\/usr\/bin\/chromium: chromium crashed/);
        assert.equal(skipped.length, 1, 'the last failure is thrown, not reported as skipped');
    });
});

describe('minChromeMajor', () => {
    it('is one major below the Chromium the Playwright release was built against', () => {
        const browsersJson = { browsers: [
            { name: 'chromium-headless-shell', browserVersion: '140.0.1.2' },
            { name: 'chromium', browserVersion: '156.0.8078.4' }
        ] };
        assert.equal(minChromeMajor(browsersJson), 155);
    });

    it('rejects browsers.json without Chromium', () => {
        assert.throws(() => minChromeMajor({ browsers: [{ name: 'firefox', browserVersion: '140.0' }] }), /browsers\.json/);
    });
});

describe('checkBrowserVersion', () => {
    it('accepts the minimum and newer majors', () => {
        checkBrowserVersion('155.0.7000.1', 155);
        checkBrowserVersion('190.0.1.1', 155);
    });

    it('rejects an older major', () => {
        assert.throws(() => checkBrowserVersion('154.0.7000.1', 155), /Chrome 154\.0\.7000\.1 is too old, version 155 or newer is required/);
    });

    it('rejects an unparsable version', () => {
        assert.throws(() => checkBrowserVersion('HeadlessChrome', 155), /HeadlessChrome/);
    });
});
