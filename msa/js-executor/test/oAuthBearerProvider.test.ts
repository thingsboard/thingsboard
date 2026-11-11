// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { describe, test, mock, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';

// ─── Stubs — must be injected before oAuthBearerProvider is first required ──

// Logger stub: captures warn() calls so tests can assert on warnings.
const logWarns: string[] = [];
const loggerStub = {
    _logger: (_name: string) => ({
        info: () => {},
        warn: (...args: unknown[]) => { logWarns.push(String(args[0])); },
        error: () => {},
    }),
};
// Pre-load the real module so its cache entry exists, then swap the exports.
require('../config/logger');
// eslint-disable-next-line @typescript-eslint/no-require-imports, @typescript-eslint/no-non-null-assertion
(require.cache[require.resolve('../config/logger')]!).exports = loggerStub;

// simple-oauth2 stub: tests control getToken behaviour via stubGetTokenFn.
let getTokenCallCount = 0;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let capturedClientConfig: any = null;
let resolveGetToken: ((v: unknown) => void) | null = null;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let stubGetTokenFn: (params: unknown) => Promise<any> = () =>
    Promise.resolve({ token: { access_token: 'default-token', expires_in: 3600 } });

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function MockClientCredentials(this: any, config: any): void {
    capturedClientConfig = config;
}
MockClientCredentials.prototype.getToken = function(params: unknown): Promise<unknown> {
    getTokenCallCount++;
    return stubGetTokenFn(params);
};

require('simple-oauth2');
// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
(require.cache[require.resolve('simple-oauth2')]!).exports = {
    ClientCredentials: MockClientCredentials,
};

// Load the module under test AFTER stubs are in place.
const providerModulePath = require.resolve('../queue/oAuthBearerProvider');
delete require.cache[providerModulePath]; // force fresh load with the stubs above
// eslint-disable-next-line @typescript-eslint/no-require-imports
const { oauthBearerProvider } = require('../queue/oAuthBearerProvider') as {
    oauthBearerProvider: (opts: {
        clientId: string;
        clientSecret: string;
        endpointUrl: string;
        refreshThresholdMs: number;
        scope?: string;
    }) => () => Promise<{ value: string }>;
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

const BASE_OPTS = {
    clientId: 'test-client',
    clientSecret: 'test-secret',
    endpointUrl: 'https://idp.example.com/oauth/token',
    refreshThresholdMs: 60_000,
};

function tok(accessToken: string, expiresIn: number): Promise<{ token: { access_token: string; expires_in: number } }> {
    return Promise.resolve({ token: { access_token: accessToken, expires_in: expiresIn } });
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('oauthBearerProvider (behavioral)', () => {

    beforeEach(() => {
        getTokenCallCount = 0;
        capturedClientConfig = null;
        resolveGetToken = null;
        logWarns.length = 0;
        stubGetTokenFn = () => tok('default-token', 3600);
        // Prevent background refresh timers from firing during tests.
        mock.timers.enable({ apis: ['setTimeout'] });
    });

    afterEach(() => {
        mock.timers.reset();
    });

    test('serves cached token without a second fetch', async () => {
        const provider = oauthBearerProvider(BASE_OPTS);
        const first = await provider();   // no cached token → triggers fetch
        const second = await provider();  // token cached → served without refetch

        assert.equal(first.value, 'default-token');
        assert.equal(second.value, 'default-token');
        assert.equal(getTokenCallCount, 1);
    });

    test('refetches when token is within EXPIRY_SAFETY_MS of its hard expiry', async () => {
        // expires_in=4s → EXPIRY_SAFETY_MS (5 s) exceeds the lifetime, so
        // cachedTokenExpiresAt - EXPIRY_SAFETY_MS < Date.now() immediately.
        // Every subsequent provider() call therefore refetches without time mocking.
        stubGetTokenFn = () => tok('tok-short', 4);
        const provider = oauthBearerProvider(BASE_OPTS);
        await provider(); // first fetch

        stubGetTokenFn = () => tok('tok-refreshed', 3600);
        const second = await provider(); // safety window → must refetch

        assert.equal(second.value, 'tok-refreshed');
        assert.equal(getTokenCallCount, 2);
    });

    test('concurrent provider() calls coalesce onto a single token request', async () => {
        // Use a deferred promise so getToken does not resolve until we decide.
        stubGetTokenFn = () => new Promise((res) => { resolveGetToken = res as (v: unknown) => void; });
        const provider = oauthBearerProvider(BASE_OPTS);

        // Both calls made before any getToken response arrives.
        const p1 = provider();
        const p2 = provider();
        assert.equal(getTokenCallCount, 1, 'only one in-flight request expected');

        resolveGetToken!({ token: { access_token: 'coalesced', expires_in: 3600 } });
        const [r1, r2] = await Promise.all([p1, p2]);

        assert.equal(r1.value, 'coalesced');
        assert.equal(r2.value, 'coalesced');
        assert.equal(getTokenCallCount, 1, 'still just one request after resolution');
    });

    test('caps threshold to lifetime/2 when threshold >= lifetime, and warns exactly once', async () => {
        // expires_in=4s keeps the token perpetually in the safety window, letting us
        // trigger a second fetch from the same provider instance to verify the once-guard.
        stubGetTokenFn = () => tok('tok-cap', 4);
        const provider = oauthBearerProvider({ ...BASE_OPTS, refreshThresholdMs: 60_000 });

        await provider(); // first fetch: threshold (60 s) >= lifetime (4 s) → cap + warn
        assert.equal(
            logWarns.filter(m => m.includes('capping threshold')).length, 1,
            'expected exactly one capping-threshold warning on first fetch',
        );

        await provider(); // second fetch (safety window): cap applies again but NO second warn
        assert.equal(
            logWarns.filter(m => m.includes('capping threshold')).length, 1,
            'expected no additional warning on subsequent fetches (once-guard)',
        );
    });

    test('passes scope to the token request', async () => {
        let capturedParams: unknown = undefined;
        stubGetTokenFn = (params) => { capturedParams = params; return tok('tok-scoped', 3600); };
        const provider = oauthBearerProvider({ ...BASE_OPTS, scope: 'api://my-app/.default' });
        await provider();

        assert.deepEqual(capturedParams, { scope: 'api://my-app/.default' });
    });

    test('drops whitespace-only scope from the token request', async () => {
        let capturedParams: unknown = undefined;
        stubGetTokenFn = (params) => { capturedParams = params; return tok('tok-noscope', 3600); };
        const provider = oauthBearerProvider({ ...BASE_OPTS, scope: '   ' });
        await provider();

        assert.deepEqual(capturedParams, {});
    });

    test('splits endpoint_url into tokenHost and tokenPath for simple-oauth2', async () => {
        const provider = oauthBearerProvider({
            ...BASE_OPTS,
            endpointUrl: 'https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token',
        });
        await provider();

        assert.ok(capturedClientConfig, 'ClientCredentials must have been constructed');
        assert.equal(capturedClientConfig.auth.tokenHost, 'https://login.microsoftonline.com');
        assert.equal(capturedClientConfig.auth.tokenPath, '/tenant-id/oauth2/v2.0/token');
    });

    test('warns when endpoint URL is not HTTPS', () => {
        oauthBearerProvider({ ...BASE_OPTS, endpointUrl: 'http://idp.example.com/token' });

        assert.ok(
            logWarns.some(m => m.includes('not HTTPS')),
            'expected a warning about the non-HTTPS endpoint',
        );
    });

}); // describe('oauthBearerProvider (behavioral)')
