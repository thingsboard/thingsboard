// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

// The Chromium builds shipped with the service. Each must be the Chromium release that playwright-core is released with,
// which install.js checks, so a Playwright upgrade needs new builds here.
module.exports = {
    // The packages: snapshot builds of the open source code, published by the Chromium project
    snapshot: {
        version: '156.0.8078.0',
        baseUrl: 'https://commondatastorage.googleapis.com/chromium-browser-snapshots',
        linux: { path: 'Linux_x64/1706520/chrome-linux.zip', sha256: '6b62846a868ea3ee675f77600790877a428dfb2428c940f8eff99db1038f9815' },
        // interactive_ui_tests.exe is a test program of 348 MB that the service does not need
        windows: { path: 'Win_x64/1706509/chrome-win.zip', sha256: '6bdfecb33dbfa7159e5666798bdc28c76347bef913573e899cb43930b78d26cc',
            exclude: ['interactive_ui_tests.exe'] }
    },
    // The Docker image, for amd64 and arm64: the chromium package of Debian. bookworm-security keeps only its latest version,
    // so the image installs it from snapshot.debian.org, which keeps every version. The archive is the time of a snapshot
    // whose package index lists the version. It is plain HTTP like the default sources of the image, which has no CA
    // certificates; apt checks the signature of the index.
    debian: {
        version: '156.0.8078.79-1~deb12u1',
        archive: 'http://snapshot.debian.org/archive/debian-security/20261103T120618Z'
    }
};
