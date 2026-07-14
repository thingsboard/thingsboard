///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { JsExecutor } from '../api/jsExecutor';

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

}); // describe('js-executor')
