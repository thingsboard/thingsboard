// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

// The Chromium builds shipped with the packages. They must be the Chromium release that playwright-core is released with,
// which install.js checks, so a Playwright upgrade needs new builds here. The Docker image has the chromium package of
// Debian instead: see docker/Dockerfile.
//
// The Chromium project publishes builds of the main branch only, so these are the builds from where the release was
// branched off, without the fixes made on the release branch later. The Debian package of the image has those fixes.
//
// To update after a Playwright upgrade, for the version that the error of install.js names:
// 1. Find where the release was branched off: chromium_main_branch_position in
//    https://chromiumdash.appspot.com/fetch_version?version=<version>
// 2. For Linux_x64 and for Win_x64, take the highest snapshot build that is not above that position. Builds are listed by
//    the start of their number, so shorten the start until one appears:
//    https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o?delimiter=/&fields=prefixes&prefix=Linux_x64/<start of the position>
// 3. Set the version (the release from the error, ending in .0) and the two paths, and build the module. It fails with
//    the SHA-256 of an archive: put it in the sha256 of that platform and build again, until both pass.
//    install.js also fails if an unpacked build is not the version set here.
// 4. Update the credits, as chromium-credits/README.md says: the third-party notices stop until they are for this version.
module.exports = {
    // The packages: snapshot builds of the open source code, published by the Chromium project.
    // exclude: glob patterns of the files that are not packaged
    // strip: glob patterns of the executables to strip of their symbols, when GNU strip is installed
    snapshot: {
        version: '156.0.8078.0',
        baseUrl: 'https://commondatastorage.googleapis.com/chromium-browser-snapshots',
        // The .pak.info files, 70 MB, are left from the build of the translations; the browser does not read them
        linux: { path: 'Linux_x64/1706520/chrome-linux.zip', sha256: '6b62846a868ea3ee675f77600790877a428dfb2428c940f8eff99db1038f9815',
            exclude: ['locales/*.pak.info'],
            // The Linux builds come with their symbols, 180 MB
            strip: ['chrome', 'chrome_crashpad_handler', 'chrome_sandbox', '*.so*'] },
        // interactive_ui_tests.exe is a test program of 348 MB that the service does not need
        windows: { path: 'Win_x64/1706509/chrome-win.zip', sha256: '6bdfecb33dbfa7159e5666798bdc28c76347bef913573e899cb43930b78d26cc',
            exclude: ['interactive_ui_tests.exe'] }
    }
};
