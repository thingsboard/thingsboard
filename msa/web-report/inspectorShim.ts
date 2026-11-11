// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import Module from 'module';

// The Node.js runtime embedded by pkg is built without the inspector, but Playwright loads the 'inspector' module
// at startup and only uses it in page.pause() to check whether a debugger is attached. Must be imported before Playwright.
const moduleLoader = Module as unknown as { _load: (request: string, ...args: unknown[]) => unknown };
const load = moduleLoader._load;
moduleLoader._load = function(request: string, ...args: unknown[]) {
    if (request === 'inspector' || request === 'node:inspector') {
        try {
            return load.call(this, request, ...args);
        } catch (e: any) {
            if (e?.code === 'ERR_INSPECTOR_NOT_AVAILABLE') {
                return { url: () => undefined };
            }
            throw e;
        }
    }
    return load.call(this, request, ...args);
};
