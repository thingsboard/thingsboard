// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');
const crypto = require('crypto');
const { Transform } = require('stream');
const { pipeline } = require('stream/promises');
// The helpers that Playwright downloads its browsers with: httpRequest follows HTTPS_PROXY and redirects, extractZip needs no unzip tool
const { httpRequest, extractZip } = require('playwright-core/lib/coreBundle').utils;
const { snapshot, debian } = require('./chromium');

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
    // For the Docker image, which installs the chromium package of Debian: see docker/Dockerfile
    await fse.outputFile(path.join(projectRoot(), 'target', 'chromium.env'),
        `CHROMIUM_DEBIAN_VERSION=${debian.version}\nCHROMIUM_DEBIAN_ARCHIVE=${debian.archive}\n`);
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
    for (const [name, version] of [['snapshot', snapshot.version], ['debian', debian.version]]) {
        if (build(chromium.browserVersion) !== build(version)) {
            throw new Error(`playwright-core expects Chromium ${chromium.browserVersion}, the ${name} build in chromium.js is ${version}`);
        }
    }
}

async function installChromium(platform, downloadDir, chromiumDir) {
    await fse.emptyDir(downloadDir);
    const archive = path.join(downloadDir, 'chromium.zip');
    await download(`${snapshot.baseUrl}/${platform.path}`, archive, platform.sha256);
    await extractZip(archive, { dir: downloadDir });
    const unpacked = path.join(downloadDir, path.basename(platform.path, '.zip'));
    for (const file of platform.exclude || []) {
        await fse.remove(path.join(unpacked, file));
    }
    if (!await platform.hasVersion(unpacked)) {
        throw new Error(`${platform.path} is not Chromium ${snapshot.version}; check the build number in chromium.js`);
    }
    await fse.move(unpacked, chromiumDir, { overwrite: true });
    await fse.remove(downloadDir);
}

// Writes the file while it is downloaded, and fails unless its SHA-256 is the expected one
function download(url, file, sha256) {
    return new Promise((resolve, reject) => {
        httpRequest({ url, socketTimeout: 60000 }, response => {
            if (response.statusCode !== 200) {
                response.resume();
                reject(new Error(`Cannot download ${url}: HTTP ${response.statusCode}`));
                return;
            }
            const hash = crypto.createHash('sha256');
            const hashing = new Transform({
                transform(chunk, encoding, callback) {
                    hash.update(chunk);
                    callback(null, chunk);
                }
            });
            pipeline(response, hashing, fs.createWriteStream(file)).then(() => {
                const actual = hash.digest('hex');
                if (actual === sha256) {
                    resolve();
                } else {
                    reject(new Error(`${url} has SHA-256 ${actual}, expected ${sha256}`));
                }
            }, reject);
        }, reject);
    });
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
