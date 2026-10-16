// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import fs from 'fs';
import path from 'path';

// Google Chrome is the supported browser; CHROME_EXECUTABLE overrides its default location.
function defaultExecutable(): string {
    switch (process.platform) {
        case 'win32':
            return path.win32.join(process.env.ProgramFiles || 'C:\\Program Files', 'Google', 'Chrome', 'Application', 'chrome.exe');
        case 'darwin':
            return '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
        default:
            return '/usr/bin/google-chrome-stable';
    }
}

const DEFAULT_EXECUTABLE = defaultExecutable();

export interface BrowserExecutable {
    path: string;
    source: string;
}

// Returns why the path can't be used as a browser executable, or undefined if it can.
function checkExecutable(executable: string): string | undefined {
    let stat: fs.Stats;
    try {
        stat = fs.statSync(executable);
    } catch {
        return 'not found';
    }
    if (stat.isDirectory()) {
        return 'is a directory';
    }
    if (!stat.isFile()) {
        return 'is not a regular file';
    }
    if (process.platform !== 'win32') {
        try {
            fs.accessSync(executable, fs.constants.X_OK);
        } catch {
            return 'is not executable';
        }
    }
    return undefined;
}

// Chromium bundled by previous versions, which a kept config file may still point to.
// TODO: remove a couple of releases after bundled Chromium was dropped.
function isLegacyBundledPath(executable: string): boolean {
    if (!(process as any).pkg) {
        return false;
    }
    const installFolder = path.dirname(path.dirname(process.execPath));
    const legacy = path.join(installFolder, 'chromium', process.platform === 'win32' ? 'chrome.exe' : 'chrome');
    const normalize = (p: string) => process.platform === 'win32' ? path.resolve(p).toLowerCase() : path.resolve(p);
    return normalize(executable) === normalize(legacy);
}

export function resolveBrowserExecutable(warn: (message: string) => void): BrowserExecutable {
    const override = process.env.CHROME_EXECUTABLE?.trim();
    if (override) {
        if (isLegacyBundledPath(override) && !fs.existsSync(override)) {
            warn(`CHROME_EXECUTABLE '${override}' is the Chromium bundled by previous versions, which is no longer shipped; ` +
                'ignoring it and using Google Chrome. Remove CHROME_EXECUTABLE from the service configuration.');
        } else {
            const reason = checkExecutable(override);
            if (reason) {
                throw new Error(`Invalid CHROME_EXECUTABLE '${override}': ${reason}.`);
            }
            return { path: override, source: 'CHROME_EXECUTABLE' };
        }
    }
    const reason = checkExecutable(DEFAULT_EXECUTABLE);
    if (reason) {
        throw new Error(`Google Chrome '${DEFAULT_EXECUTABLE}': ${reason}. Install Google Chrome, or set CHROME_EXECUTABLE ` +
            'to the path of another Chrome or Chromium executable.');
    }
    return { path: DEFAULT_EXECUTABLE, source: 'default' };
}
