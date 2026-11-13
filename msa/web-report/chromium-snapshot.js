// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

// The Chromium builds packaged with the service: snapshot builds of the open source code, published by the Chromium project.
// They are the builds closest to the browser version that playwright-core is released with, so a Playwright upgrade needs new builds here.
module.exports = {
    version: '156.0.8078.0',
    baseUrl: 'https://commondatastorage.googleapis.com/chromium-browser-snapshots',
    linux: { path: 'Linux_x64/1706520/chrome-linux.zip', sha256: '6b62846a868ea3ee675f77600790877a428dfb2428c940f8eff99db1038f9815',
        chromeDir: 'chrome-linux', exclude: [] },
    windows: { path: 'Win_x64/1706509/chrome-win.zip', sha256: '6bdfecb33dbfa7159e5666798bdc28c76347bef913573e899cb43930b78d26cc',
        chromeDir: 'chrome-win', exclude: ['chrome-win/interactive_ui_tests.exe'] }
};
