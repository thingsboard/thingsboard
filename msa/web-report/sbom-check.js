// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fs = require('fs');
const path = require('path');

const TARGET_DIR = path.join(__dirname, 'target');
const SBOM_FILE = path.join(TARGET_DIR, 'sbom', 'tb-web-report.cdx.json');
const VERSIONS_FILE = path.join(TARGET_DIR, 'sbom', 'bundled-versions.properties');
const PKG_LOGS = { linux: 'pkg-linux.log', windows: 'pkg-win.log' };
const ADDED_FILE = /(?:Content of|Bytecode of|Stat info of|Directory) (\S+) is added to queue/g;
const BASE_BINARY = /fetched-v(\d+\.\d+\.\d+)-/;

(() => {
    const listed = listedPackages(JSON.parse(fs.readFileSync(SBOM_FILE, 'utf8')));
    const versions = { 'chromium.version': require('./chromium-snapshot').version };
    for (const [os, log] of Object.entries(PKG_LOGS)) {
        const output = fs.readFileSync(path.join(TARGET_DIR, log), 'utf8').replace(/\x1b\[[0-9;]*m/g, '');
        const baseBinary = BASE_BINARY.exec(output);
        if (!baseBinary) {
            throw new Error(`Cannot find the Node.js base binary in ${log}`);
        }
        versions[`node.${os}.version`] = baseBinary[1];
        const bundled = bundledPackages(output);
        if (bundled.size === 0) {
            throw new Error(`No npm packages found in ${log}; was pkg run with --debug?`);
        }
        const missing = [...bundled].filter(id => !listed.has(id)).sort();
        if (missing.length > 0) {
            throw new Error(`The ${os} binary contains npm packages that ${SBOM_FILE} does not list: ${missing.join(', ')}`);
        }
        console.log(`The ${os} binary contains ${bundled.size} npm packages, all listed in the SBOM (${listed.size} listed)`);
    }
    // The --debug executables were built only for their logs
    fs.rmSync(path.join(TARGET_DIR, 'pkg-debug'), { recursive: true, force: true });
    fs.writeFileSync(VERSIONS_FILE, Object.entries(versions).map(([key, value]) => `${key}=${value}\n`).join(''));
    console.log(`Wrote ${VERSIONS_FILE}: ${JSON.stringify(versions)}`);
})();

function listedPackages(bom) {
    const ids = new Set();
    for (const component of bom.components || []) {
        if ((component.purl || '').startsWith('pkg:npm/')) {
            ids.add(`${component.group ? component.group + '/' : ''}${component.name}@${component.version}`);
        }
    }
    return ids;
}

function bundledPackages(output) {
    const packageDirs = new Set();
    for (const match of output.matchAll(ADDED_FILE)) {
        const dir = packageDir(match[1]);
        if (dir) {
            packageDirs.add(dir);
        }
    }
    const ids = new Set();
    for (const dir of packageDirs) {
        const manifest = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8'));
        ids.add(`${manifest.name}@${manifest.version}`);
    }
    return ids;
}

function packageDir(file) {
    const parts = file.split(/[\\/]/);
    const index = parts.lastIndexOf('node_modules');
    if (index < 0 || index + 1 >= parts.length) {
        return null;
    }
    const scoped = parts[index + 1].startsWith('@');
    if (scoped && index + 2 >= parts.length) {
        return null;
    }
    return parts.slice(0, index + (scoped ? 3 : 2)).join(path.sep);
}
