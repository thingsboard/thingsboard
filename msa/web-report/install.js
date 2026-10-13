// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fse = require('fs-extra');
const path = require('path');
const { execFileSync } = require('child_process');

// Chromium builds packaged with the service, downloaded by the Playwright CLI as if it ran on the target platform.
const PLATFORMS = {
    linux: { hostPlatform: 'ubuntu24.04-x64', downloadDir: 'chromiumLinux', chromeDir: 'chrome-linux64', binary: 'tb-web-report', pkgOutput: 'thingsboard-web-report-linux' },
    windows: { hostPlatform: 'win64', downloadDir: 'chromiumWin', chromeDir: 'chrome-win64', binary: 'tb-web-report.exe', pkgOutput: 'thingsboard-web-report-win.exe' }
};

(async() => {
    for (const [targetDir, platform] of Object.entries(PLATFORMS)) {
        const downloadDir = path.join(projectRoot(), 'target', platform.downloadDir);
        installChromium(platform.hostPlatform, downloadDir);
        const chromiumDir = path.join(targetPackageDir(targetDir), 'chromium');
        await fse.emptyDir(chromiumDir);
        await fse.copy(path.join(downloadDir, chromiumRevisionDir(), platform.chromeDir), chromiumDir);
        await fse.move(path.join(projectRoot(), 'target', platform.pkgOutput),
            path.join(targetPackageDir(targetDir), 'bin', platform.binary),
            {overwrite: true});
    }
})().catch(e => {
    console.error(e);
    process.exit(1);
});

function installChromium(hostPlatform, downloadDir) {
    execFileSync(process.execPath, [path.join(path.dirname(require.resolve('playwright-core/package.json')), 'cli.js'), 'install', '--no-shell', 'chromium'], {
        stdio: 'inherit',
        env: {
            ...process.env,
            PLAYWRIGHT_HOST_PLATFORM_OVERRIDE: hostPlatform,
            PLAYWRIGHT_BROWSERS_PATH: downloadDir,
            PLAYWRIGHT_SKIP_BROWSER_GC: '1'
        }
    });
}

function chromiumRevisionDir() {
    const browsers = fse.readJsonSync(path.join(projectRoot(), 'node_modules', 'playwright-core', 'browsers.json')).browsers;
    return 'chromium-' + browsers.find(b => b.name === 'chromium').revision;
}

function projectRoot() {
    return __dirname;
}

function targetPackageDir(targetDir) {
    return path.join(projectRoot(), 'target', 'package', targetDir);
}
