// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fse = require('fs-extra');
const path = require('path');

const PLATFORMS = {
    linux: { binary: 'tb-web-report', pkgOutput: 'thingsboard-web-report-linux' },
    windows: { binary: 'tb-web-report.exe', pkgOutput: 'thingsboard-web-report-win.exe' }
};

(async() => {
    for (const [targetDir, platform] of Object.entries(PLATFORMS)) {
        await fse.move(path.join(projectRoot(), 'target', platform.pkgOutput),
            path.join(targetPackageDir(targetDir), 'bin', platform.binary),
            {overwrite: true});
    }
})().catch(e => {
    console.error(e);
    process.exit(1);
});

function projectRoot() {
    return __dirname;
}

function targetPackageDir(targetDir) {
    return path.join(projectRoot(), 'target', 'package', targetDir);
}
