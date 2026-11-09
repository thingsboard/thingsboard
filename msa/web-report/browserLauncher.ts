// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Browser, LaunchOptions } from 'playwright-core';

export type LaunchTarget = { channel: 'chrome' } | { executablePath: string };

export interface LaunchPlan {
    targets: LaunchTarget[];
    warnings: string[];
}

const LINUX_CHROMIUM = ['/usr/bin/chromium', '/usr/bin/chromium-browser'];

export function browserLaunchTargets(override: string | undefined,
                                     platform: NodeJS.Platform,
                                     exists: (path: string) => boolean): LaunchPlan {
    const executable = override?.trim();
    if (executable && exists(executable)) {
        return { targets: [{ executablePath: executable }], warnings: [] };
    }
    const warnings = executable ? [`CHROME_EXECUTABLE '${executable}' does not exist (Chromium bundled by a previous version?), ` +
        'falling back to Google Chrome. Remove or fix CHROME_EXECUTABLE in the service configuration.'] : [];
    const chromium = platform === 'linux' ? LINUX_CHROMIUM.filter(exists).map(path => ({ executablePath: path })) : [];
    return { targets: [{ channel: 'chrome' }, ...chromium], warnings };
}

export function describeTarget(target: LaunchTarget): string {
    return 'channel' in target ? 'Google Chrome' : target.executablePath;
}

export async function launchBrowser(launch: (options: LaunchOptions) => Promise<Browser>,
                                    options: LaunchOptions,
                                    targets: LaunchTarget[],
                                    onSkipped: (message: string) => void): Promise<Browser> {
    const errors: string[] = [];
    for (const target of targets) {
        try {
            return await launch({ ...options, ...target });
        } catch (e: any) {
            const error = `${describeTarget(target)}: ${e?.message ?? e}`;
            errors.push(error);
            if (errors.length < targets.length) {
                onSkipped(`Failed to start ${error}. Trying the next browser.`);
            }
        }
    }
    throw new Error(`Failed to start a browser. Install Google Chrome, or set CHROME_EXECUTABLE to a Chrome or Chromium ` +
        `executable.\n${errors.join('\n')}`);
}

export function minChromeMajor(browsersJson: any): number {
    const chromium = browsersJson?.browsers?.find((browser: any) => browser.name === 'chromium');
    const major = parseInt(chromium?.browserVersion, 10);
    if (isNaN(major)) {
        throw new Error('Chromium version not found in playwright-core browsers.json');
    }
    return major - 1;
}

export function checkBrowserVersion(version: string, minMajor: number): void {
    const major = parseInt(version, 10);
    if (isNaN(major)) {
        throw new Error(`Unrecognized Chrome version '${version}'.`);
    }
    if (major < minMajor) {
        throw new Error(`Chrome ${version} is too old, version ${minMajor} or newer is required. Update Chrome.`);
    }
}

export async function smokeRender(browser: Browser): Promise<void> {
    const page = await browser.newPage();
    try {
        await page.setContent('<p>ok</p>');
        await page.pdf();
    } finally {
        await page.close();
    }
}
