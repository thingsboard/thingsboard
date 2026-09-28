// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fse = require('fs-extra');
const path = require('path');
const server = require('playwright-core/lib/server');

const EXECUTABLE_PATHS = {
    'ubuntu22.04-x64': ['chrome-linux', 'chrome'],
    'win64': ['chrome-win', 'chrome.exe']
};

let _projectRoot = null;
let browsersJSON = null;
let chromium = null;

(async() => {
    browsersJSON = fse.readJsonSync(path.join(projectRoot(), 'node_modules', 'playwright-core', 'browsers.json')).browsers;
    chromium = browsersJSON.find(d => d.name === 'chromium');
    await server.registry.install([createdExecutables('ubuntu22.04-x64'), createdExecutables('win64')], true);
    await copyChromeFromPkg('ubuntu22.04-x64');
    await copyChromeFromPkg('win64');
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-linux'),
        path.join(targetPackageDir('ubuntu22.04-x64'), 'bin', 'tb-web-report'),
        {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-win.exe'),
        path.join(targetPackageDir('win64'), 'bin', 'tb-web-report.exe'),
        {overwrite: true});
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platformTargetDir(platform));
}

function targetChromiumDir(platform) {
    return path.join(targetPackageDir(platform), 'chromium');
}

function downloadChromiumDir(platform) {
    let platformDir;
    if (platform === 'ubuntu22.04-x64') {
        platformDir = 'chromiumLinux';
    } else if (platform === 'win64') {
        platformDir = 'chromiumWin';
    }
    return  path.join(projectRoot(), 'target', platformDir);
}

function platformTargetDir(platform) {
    if (platform === 'ubuntu22.04-x64') {
        return 'linux';
    } else if (platform === 'win64') {
        return 'windows';
    }
    return '';
}

async function copyChromeFromPkg(platform) {
    const chromiumDir = targetChromiumDir(platform);
    await fse.emptyDir(chromiumDir);
    let fromDir = path.join(downloadChromiumDir(platform), EXECUTABLE_PATHS[platform][0])
    await fse.copy(fromDir, chromiumDir);
}

function createdExecutables(platform) {
    const chromiumData = JSON.parse(JSON.stringify(chromium));
    chromiumData.dir = downloadChromiumDir(platform);
    chromiumData.platform = platform;
    const executablePath = path.join(chromiumData.dir, ...EXECUTABLE_PATHS[platform]);
    return {
        type: 'browser',
        name: 'chromium',
        browserName: 'chromium',
        directory: chromium.dir,
        platform: platform,
        executablePath: () => executablePath,
        executablePathOrDie: sdkLanguage => server.registry.executablePathOrDie('chromium', executablePath, true, sdkLanguage),
        installType: 'download-by-default',
        _validateHostRequirements: (sdkLanguage) => server.registry._validateHostRequirements(sdkLanguage, 'chromium', chromium.dir, ['chrome-linux'], [], ['chrome-win']),
        downloadURLs: server.registry._downloadURLs(chromiumData),
        browserVersion: chromiumData.browserVersion,
        _install: () => server.registry._downloadExecutable(chromiumData, executablePath),
        _dependencyGroup: 'chromium',
        _isHermeticInstallation: true,
    };
}
