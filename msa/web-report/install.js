// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');
const crypto = require('crypto');
const { execFile, execFileSync } = require('child_process');
const { promisify } = require('util');
const { Transform } = require('stream');
const { pipeline } = require('stream/promises');
// The helpers that Playwright downloads its browsers with: httpRequest follows HTTPS_PROXY and redirects, extractZip needs no unzip tool
const { httpRequest, extractZip } = require('playwright-core/lib/coreBundle').utils;
const { snapshot } = require('./chromium');

const DOWNLOAD_ATTEMPTS = 3;

const PLATFORMS = {
    linux: {
        ...snapshot.linux, binary: 'tb-web-report', pkgOutput: 'thingsboard-web-report-linux',
        // The version is written into the executable
        hasVersion: dir => fileContains(path.join(dir, 'chrome'), snapshot.version)
    },
    windows: {
        ...snapshot.windows, binary: 'tb-web-report.exe', pkgOutput: 'thingsboard-web-report-win.exe',
        hasVersion: dir => fse.pathExists(path.join(dir, `${snapshot.version}.manifest`))
    }
};

(async() => {
    checkPlaywrightVersion();
    await Promise.all(Object.entries(PLATFORMS).map(async ([targetDir, platform]) => {
        await installChromium(platform, path.join(projectRoot(), 'target', 'chromium-' + targetDir),
            path.join(targetPackageDir(targetDir), 'chromium'));
        await fse.move(path.join(projectRoot(), 'target', platform.pkgOutput),
            path.join(targetPackageDir(targetDir), 'bin', platform.binary),
            {overwrite: true});
    }));
})().catch(e => {
    console.error(e);
    process.exit(1);
});

function checkPlaywrightVersion() {
    const browsers = fse.readJsonSync(path.join(path.dirname(require.resolve('playwright-core/package.json')), 'browsers.json')).browsers;
    const chromium = browsers.find(browser => browser.name === 'chromium');
    if (!chromium) {
        throw new Error('playwright-core/browsers.json does not list chromium');
    }
    // The release is the first three numbers; the last one counts the fixes on its branch and differs between builds
    const build = version => version.split('.').slice(0, 3).join('.');
    if (build(chromium.browserVersion) !== build(snapshot.version)) {
        throw new Error(`playwright-core expects Chromium ${chromium.browserVersion}, the snapshot build in chromium.js is ${snapshot.version}`);
    }
}

async function installChromium(platform, downloadDir, chromiumDir) {
    await fse.emptyDir(downloadDir);
    const archive = path.join(downloadDir, 'chromium.zip');
    await download(`${snapshot.baseUrl}/${platform.path}`, archive, platform.sha256);
    await extractZip(archive, { dir: downloadDir });
    const unpacked = path.join(downloadDir, path.basename(platform.path, '.zip'));
    for await (const excluded of fs.promises.glob(platform.exclude || [], { cwd: unpacked })) {
        await fse.remove(path.join(unpacked, excluded));
    }
    if (platform.strip) {
        await stripExecutables(unpacked, platform.strip);
    }
    if (!await platform.hasVersion(unpacked)) {
        throw new Error(`${platform.path} is not Chromium ${snapshot.version}; check the build number in chromium.js`);
    }
    await fse.move(unpacked, chromiumDir, { overwrite: true });
    await fse.remove(downloadDir);
}

// Fails unless the SHA-256 of the downloaded file is the expected one. Tries again when the connection or the server
// fails, but not for an answer such as 404, which would be the same again.
async function download(url, file, sha256) {
    let actual;
    for (let attempt = 1; actual === undefined; attempt++) {
        try {
            actual = await downloadOnce(url, file);
        } catch (e) {
            if (e.statusCode < 500 || attempt === DOWNLOAD_ATTEMPTS) {
                throw e;
            }
            console.warn(`${e.message}; trying again`);
        }
    }
    if (actual !== sha256) {
        throw new Error(`${url} has SHA-256 ${actual}, expected ${sha256}`);
    }
}

// Writes the file while it is downloaded, and gives its SHA-256
function downloadOnce(url, file) {
    return new Promise((resolve, reject) => {
        const fail = (reason, statusCode) => reject(Object.assign(new Error(`Cannot download ${url}: ${reason}`), { statusCode }));
        httpRequest({ url, socketTimeout: 60000 }, response => {
            if (response.statusCode !== 200) {
                response.resume();
                fail(`HTTP ${response.statusCode}`, response.statusCode);
                return;
            }
            const hash = crypto.createHash('sha256');
            const hashing = new Transform({
                transform(chunk, encoding, callback) {
                    hash.update(chunk);
                    callback(null, chunk);
                }
            });
            pipeline(response, hashing, fs.createWriteStream(file)).then(() => resolve(hash.digest('hex')), e => fail(e.message));
        }, e => fail(e.message));
    });
}

// Strips the symbols with GNU strip, which reads the Linux executables on any build machine. Without it, as on macOS
// or where binutils is not installed, the executables are packaged as they are.
async function stripExecutables(dir, patterns) {
    if (!hasGnuStrip()) {
        console.warn('strip not found, Linux Chromium left unstripped (+~180 MB); install GNU binutils to strip it');
        return;
    }
    for await (const file of fs.promises.glob(patterns, { cwd: dir })) {
        await promisify(execFile)('strip', ['--strip-unneeded', path.join(dir, file)]);
    }
}

function hasGnuStrip() {
    try {
        return execFileSync('strip', ['--version'], { encoding: 'utf8' }).includes('GNU strip');
    } catch {
        return false;
    }
}

async function fileContains(file, text) {
    const needle = Buffer.from(text);
    let tail = Buffer.alloc(0);
    for await (const chunk of fs.createReadStream(file)) {
        const data = Buffer.concat([tail, chunk]);
        if (data.includes(needle)) {
            return true;
        }
        tail = data.subarray(data.length - needle.length + 1);
    }
    return false;
}

function projectRoot() {
    return __dirname;
}

function targetPackageDir(targetDir) {
    return path.join(projectRoot(), 'target', 'package', targetDir);
}
