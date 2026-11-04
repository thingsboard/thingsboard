// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import path from 'node:path';
import { promisify } from 'node:util';
import { JsExecutor } from '../api/jsExecutor';
import { oauthBearerProvider } from '../queue/oAuthBearerProvider';

// describe('js-executor') groups all cases under <testsuite name="js-executor">
// in the JUnit XML so they show up under that suite in TeamCity's Tests tab,
// alongside thousands of Java tests.
describe('js-executor', () => {

test('sandbox isolates args from host realm (JVN#16937365)', async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        var F = args.constructor.constructor;
        var p = F("return process")();
        return p && p.mainModule ? 'reached-host' : 'isolated';
    }`);
    await assert.rejects(
        exec.executeScript(script, ['{}', '{}', 'POST_TELEMETRY_REQUEST'], 5000),
        /process is not defined/,
        'host process must not be reachable from inside the sandbox',
    );
});

test('sandbox passes string args through unchanged', async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        return { msgIsString: typeof msg === 'string', count: args.length, first: args[0] };
    }`);
    const out = await exec.executeScript(script, ['hello', '{}', 'X'], 5000);
    // Field-by-field: the returned object is owned by the sandbox realm, so
    // its prototype is not the host Object.prototype and deepStrictEqual would
    // reject it on prototype mismatch even when the values match.
    assert.equal(out.msgIsString, true);
    assert.equal(out.count, 3);
    assert.equal(out.first, 'hello');
});

const sandboxRealmProbes: [string, string][] = [
    ['btoa', 'btoa.constructor'],
    ['atob', 'atob.constructor'],
    ['TextDecoder', 'TextDecoder.constructor'],
    ['TextDecoder instance', 'new TextDecoder().constructor.constructor'],
    ['TextDecoder.prototype.decode', 'TextDecoder.prototype.decode.constructor'],
    ['TextDecoder getter', 'Object.getOwnPropertyDescriptor(TextDecoder.prototype, "encoding").get.constructor'],
    ['TextDecoder constructor error', '(function(){ try { new TextDecoder("no-such-encoding"); } catch (e) { return e.constructor.constructor; } })()'],
    ['TextDecoder decode error', '(function(){ try { new TextDecoder().decode("not a buffer"); } catch (e) { return e.constructor.constructor; } })()'],
    ['TextDecoder fatal decode error', '(function(){ try { new TextDecoder("utf-8", {fatal: true}).decode(new Uint8Array([0xff])); } catch (e) { return e.constructor.constructor; } })()'],
    ['getter on foreign receiver', '(function(){ try { Object.getOwnPropertyDescriptor(TextDecoder.prototype, "fatal").get.call({}); } catch (e) { return e.constructor.constructor; } })()'],
    ['patched WeakMap/String', '(function(){ var leak; WeakMap.prototype.get = function(k){ leak = this; }; String = function(x){ return x; }; var d = new TextDecoder(); d.decode(new Uint8Array([104])); try { btoa("\\u0100"); } catch (e) {} return leak ? leak.constructor.constructor : Function; })()'],
    ['Error.prepareStackTrace', '(function(){ var fr; Error.prepareStackTrace = function(e, s){ fr = s; return ""; }; try { new TextDecoder("nope"); } catch (e) { e.stack; } for (var i = 0; fr && i < fr.length; i++) { var f = fr[i].getFunction && fr[i].getFunction(); if (f) return f.constructor; var t = fr[i].getThis && fr[i].getThis(); if (t && t.constructor) return t.constructor.constructor; } return Function; })()'],
    // Node formats some ERR_INVALID_ARG_TYPE messages with util.inspect, which calls a custom inspect hook.
    ...['utf-8', 'latin1', 'utf-16le'].map((encoding): [string, string] => [`TextDecoder.decode inspect hook (${encoding})`,
        `(function(){ var F; var o = Object.create(null); o[Symbol.for("nodejs.util.inspect.custom")] = function(d, opts, insp){ F = insp && insp.constructor; return "x"; };
            try { new TextDecoder("${encoding}").decode(o); } catch (e) {}
            var p = {}; Object.setPrototypeOf(p, ArrayBuffer.prototype); Object.defineProperty(p, "constructor", {value: ArrayBuffer}); p[Symbol.for("nodejs.util.inspect.custom")] = o[Symbol.for("nodejs.util.inspect.custom")];
            try { new TextDecoder("${encoding}").decode(p); } catch (e) {}
            return F || Function; })()`]),
    ...['btoa', 'atob'].map((fn): [string, string] => [`${fn} with patched String returning an inspectable object`,
        `(function(){ var F; String = function(x){ var o = Object.create(null); o[Symbol.for("nodejs.util.inspect.custom")] = function(d, opts, insp){ F = insp && insp.constructor; return "x"; }; return o; };
            try { ${fn}("a"); } catch (e) {}
            return F || Function; })()`]),
];

for (const [name, functionCtor] of sandboxRealmProbes) {
    test(`sandbox globals belong to the sandbox realm: ${name}`, async () => {
        const exec = new JsExecutor(true);
        const script = await exec.compileScript(`function(msg, metadata, msgType){
            var F = ${functionCtor};
            return F('return process')();
        }`);
        await assert.rejects(
            exec.executeScript(script, ['{}', '{}', 'X'], 5000),
            /process is not defined/,
            'host process must not be reachable from inside the sandbox',
        );
    });
}

test('sandbox globals belong to the sandbox realm: resolving functions of a returned thenable', async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        return {then: function(resolve){ resolve(resolve.constructor.constructor('return process')()); }};
    }`);
    await assert.rejects(
        exec.executeScript(script, ['{}', '{}', 'X'], 5000),
        /process is not defined/,
    );
});

// A stack overflow inside a helper reproduces only while its host code path is cold,
// so each probe runs as the first sandbox execution of a fresh process.
const coldPathCalls = [
    "btoa('a')",
    "atob('YQ==')",
    "new TextDecoder('nope')",
    "new TextDecoder().decode(new Uint8Array([104]))",
    "new TextDecoder().encoding",
    "Object.getOwnPropertyDescriptor(TextDecoder.prototype, 'fatal').get.call({})",
];

test('sandbox helpers rethrow only sandbox-realm errors on stack overflow in cold host code', async () => {
    const run = promisify(execFile);
    const results = await Promise.all(coldPathCalls.map(async (call) => {
        const {stdout} = await run(process.execPath, ['--require', 'ts-node/register', path.join(__dirname, 'coldPathProbe.ts'), call],
            {cwd: path.join(__dirname, '..'), env: {...process.env, TS_NODE_TRANSPILE_ONLY: 'true'}, timeout: 60000});
        return [call, stdout];
    }));
    assert.deepEqual(results, coldPathCalls.map((call) => [call, 'isolated']));
});

test('sandbox btoa/atob encode, decode and coerce like the browser API', async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        return JSON.stringify([btoa('hello'), atob('aGVsbG8='), atob(btoa('\\u00ff\\u0000x')) === '\\u00ff\\u0000x', btoa(123),
            btoa.name, atob.name, Object.getPrototypeOf(btoa) === Function.prototype]);
    }`);
    const out = await exec.executeScript(script, ['{}', '{}', 'X'], 5000);
    assert.equal(out, JSON.stringify(['aGVsbG8=', 'hello', true, 'MTIz', 'btoa', 'atob', true]));
});

test('sandbox TextDecoder decodes like the host TextDecoder', async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        var bytes = new Uint8Array([0xEF, 0xBB, 0xBF, 0x68, 0xC3, 0xA9, 0xE2, 0x82, 0xAC]);
        var d = new TextDecoder();
        var stream = new TextDecoder();
        var streamed = stream.decode(new Uint8Array([0xE2, 0x82]), {stream: true}) + stream.decode(new Uint8Array([0xAC]));
        var latin = new TextDecoder('latin1');
        function errorType(fn) { try { fn(); return 'none'; } catch (e) { return e instanceof RangeError ? 'RangeError' : e instanceof TypeError ? 'TypeError' : 'other'; } }
        return JSON.stringify([
            d.decode(bytes), d.decode(bytes.buffer), d.decode(new DataView(bytes.buffer, 3)), d.decode(),
            d.decode(bytes.subarray(3)), d.decode(bytes.subarray(4, 6)), d.decode(new Uint16Array(bytes.buffer, 2, 2)),
            new TextDecoder('utf-8', {ignoreBOM: true}).decode(bytes).length, streamed,
            d.encoding, d.fatal, d.ignoreBOM, new TextDecoder('UTF8', {fatal: true}).fatal,
            latin.encoding, latin.decode(new Uint8Array([0xE9])),
            errorType(function(){ new TextDecoder('utf-8', {fatal: true}).decode(new Uint8Array([0xFF])); }),
            errorType(function(){ new TextDecoder('no-such-encoding'); }),
            errorType(function(){ TextDecoder.prototype.decode.call({}, bytes); }),
            errorType(function(){ TextDecoder(); }),
            d instanceof TextDecoder, Object.prototype.toString.call(d)]);
    }`);
    const out = await exec.executeScript(script, ['{}', '{}', 'X'], 5000);
    assert.equal(out, JSON.stringify([
        'hé€', 'hé€', 'hé€', '',
        'hé€', 'é', '\uFFFDhé',
        4, '€',
        'utf-8', false, false, true,
        'windows-1252', 'é',
        'TypeError', 'RangeError', 'TypeError', 'TypeError',
        true, '[object TextDecoder]']));
});

// The use_sandbox=false path is intentionally non-isolating: scripts compile
// and run in the host realm via vm.compileFunction. The two tests below codify
// that documented contract so any future behavior change shows up as a test
// failure and forces a deliberate update of the docs and threat model.

test('non-sandbox path does not isolate from host realm (documented contract)', async () => {
    const exec = new JsExecutor(false);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        // Non-destructive host-reach probe: typeof process.platform is 'string'
        // only if the host process object is reachable.
        var F = args.constructor.constructor;
        return F('return typeof process.platform')();
    }`);
    const out = await exec.executeScript(script, ['{}', '{}', 'X']);
    assert.equal(out, 'string',
        'use_sandbox=false is documented as non-isolating; if this fails, the path was changed and docs/threat model must be updated');
});

test('non-sandbox path passes string args through unchanged', async () => {
    const exec = new JsExecutor(false);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        return { msgIsString: typeof msg === 'string', count: args.length, first: args[0] };
    }`);
    const out = await exec.executeScript(script, ['hello', '{}', 'X']);
    assert.equal(out.msgIsString, true);
    assert.equal(out.count, 3);
    assert.equal(out.first, 'hello');
});

test('oauthBearerProvider rejects missing client credentials', () => {
    assert.throws(
        () => oauthBearerProvider({ clientId: '', clientSecret: 'x', endpointUrl: 'https://idp/token', refreshThresholdMs: 60000 }),
        /client_id, client_secret and endpoint_url/,
    );
    assert.throws(
        () => oauthBearerProvider({ clientId: 'x', clientSecret: '', endpointUrl: 'https://idp/token', refreshThresholdMs: 60000 }),
        /client_id, client_secret and endpoint_url/,
    );
    assert.throws(
        () => oauthBearerProvider({ clientId: 'x', clientSecret: 'y', endpointUrl: '', refreshThresholdMs: 60000 }),
        /client_id, client_secret and endpoint_url/,
    );
});

test('oauthBearerProvider rejects non-numeric refresh threshold', () => {
    assert.throws(
        () => oauthBearerProvider({
            clientId: 'x', clientSecret: 'y', endpointUrl: 'https://idp/token',
            refreshThresholdMs: 'soon' as unknown as number,
        }),
        /refresh_threshold must be a non-negative number/,
    );
    assert.throws(
        () => oauthBearerProvider({
            clientId: 'x', clientSecret: 'y', endpointUrl: 'https://idp/token',
            refreshThresholdMs: -1,
        }),
        /refresh_threshold must be a non-negative number/,
    );
});

}); // describe('js-executor')
