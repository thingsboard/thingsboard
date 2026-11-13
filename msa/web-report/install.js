// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fse = require('fs-extra');
const path = require('path');
const { execFileSync } = require('child_process');
const crypto = require('crypto');
const snapshot = require('./chromium-snapshot');

const PLATFORMS = {
    linux: { ...snapshot.linux, binary: 'tb-web-report', pkgOutput: 'thingsboard-web-report-linux' },
    windows: { ...snapshot.windows, binary: 'tb-web-report.exe', pkgOutput: 'thingsboard-web-report-win.exe' }
};

(async() => {
    const expected = fse.readJsonSync(path.join(path.dirname(require.resolve('playwright-core/package.json')), 'browsers.json'))
        .browsers.find(b => b.name === 'chromium').browserVersion;
    // The last number counts the fixes on the release branch, which a snapshot build never has
    const build = version => version.split('.').slice(0, 3).join('.');
    if (build(expected) !== build(snapshot.version)) {
        throw new Error(`playwright-core expects Chromium ${expected}, the snapshot builds in chromium-snapshot.js are ${snapshot.version}`);
    }
    for (const [targetDir, platform] of Object.entries(PLATFORMS)) {
        const downloadDir = path.join(projectRoot(), 'target', 'chromium-' + targetDir);
        await installChromium(platform, downloadDir);
        const chromiumDir = path.join(targetPackageDir(targetDir), 'chromium');
        await fse.emptyDir(chromiumDir);
        await fse.copy(path.join(downloadDir, platform.chromeDir), chromiumDir);
        await fse.move(path.join(projectRoot(), 'target', platform.pkgOutput),
            path.join(targetPackageDir(targetDir), 'bin', platform.binary),
            {overwrite: true});
    }
})().catch(e => {
    console.error(e);
    process.exit(1);
});

async function installChromium(platform, downloadDir) {
    await fse.emptyDir(downloadDir);
    const archive = path.join(downloadDir, 'chromium.zip');
    const response = await fetch(`${snapshot.baseUrl}/${platform.path}`);
    if (!response.ok) {
        throw new Error(`Cannot download ${platform.path}: HTTP ${response.status}`);
    }
    const content = Buffer.from(await response.arrayBuffer());
    const sha256 = crypto.createHash('sha256').update(content).digest('hex');
    if (sha256 !== platform.sha256) {
        throw new Error(`${platform.path} has SHA-256 ${sha256}, expected ${platform.sha256}`);
    }
    await fse.writeFile(archive, content);
    execFileSync('unzip', ['-q', archive, '-d', downloadDir, ...platform.exclude.flatMap(file => ['-x', file])], { stdio: 'inherit' });
    await fse.remove(archive);
}

function projectRoot() {
    return __dirname;
}

function targetPackageDir(targetDir) {
    return path.join(projectRoot(), 'target', 'package', targetDir);
}
