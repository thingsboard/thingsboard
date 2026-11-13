// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { after, before, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import os from 'os';
import path from 'path';
import zlib from 'zlib';
import { parsePrecompressedEncodings } from '../middleware/precompressedStatic';
import {
    assertCompressedOnTheFly, assertPrebuilt, assertRaw, br, captureOutput, closeServers, content, gz, request, startServer, writeFixture
} from './helpers';

const secret = 'TOP-SECRET outside the web root';
const skipAsRoot = {skip: process.getuid?.() === 0 && 'file permissions are not enforced for root'};

after(closeServers);

describe('precompressedStatic', () => {
    let parent: string;
    let root: string;
    let port: number;
    let gzipFirstPort: number;

    before(async () => {
        parent = fs.mkdtempSync(path.join(os.tmpdir(), 'tb-web-ui-test-'));
        root = path.join(parent, 'public');
        fs.mkdirSync(root);
        fs.writeFileSync(path.join(parent, 'secret.js'), secret);
        fs.writeFileSync(path.join(parent, 'secret.js.br'), zlib.brotliCompressSync(secret));
        fs.writeFileSync(path.join(parent, 'secret.js.gz'), zlib.gzipSync(secret));
        writeFixture(root, 'index.html', {withBr: true, withGz: true});
        writeFixture(root, 'app.js', {withBr: true, withGz: true});
        writeFixture(root, 'nested/app.js', {withBr: true, withGz: true});
        writeFixture(root, 'gz-only.js', {withGz: true});
        writeFixture(root, 'raw-only.js', {});
        writeFixture(root, 'logo.png', {withBr: true});
        writeFixture(root, 'unreadable-br.js', {withBr: true, withGz: true, unreadable: ['br']});
        writeFixture(root, 'unreadable-all.js', {withBr: true, withGz: true, unreadable: ['br', 'gz']});
        port = await startServer(root, ['br', 'gzip']);
        gzipFirstPort = await startServer(root, ['gzip', 'br']);
    });

    after(() => fs.rmSync(parent, {recursive: true, force: true}));

    describe('negotiation', () => {
        const cases: { name: string, gzipFirst?: boolean, acceptEncoding: string, expected: 'br' | 'gzip' | 'raw' }[] = [
            {name: 'equal weights: configured br first', acceptEncoding: 'gzip, deflate, br, zstd', expected: 'br'},
            {name: 'equal weights: configured gzip first', gzipFirst: true, acceptEncoding: 'br, gzip', expected: 'gzip'},
            {name: 'q-values beat configured br first', acceptEncoding: 'gzip;q=1, br;q=0.5', expected: 'gzip'},
            {name: 'q-values beat configured gzip first', gzipFirst: true, acceptEncoding: 'gzip;q=0.5, br', expected: 'br'},
            {name: 'only gzip accepted', acceptEncoding: 'gzip', expected: 'gzip'},
            {name: 'br refused with q=0', acceptEncoding: 'br;q=0, gzip', expected: 'gzip'},
            {name: 'wildcard refused', acceptEncoding: 'gzip, *;q=0', expected: 'gzip'},
            {name: 'malformed q-value ignores the entry', acceptEncoding: 'br;q=abc, gzip', expected: 'gzip'},
            {name: 'wildcard uses configured order', acceptEncoding: '*', expected: 'br'},
            {name: 'all refused', acceptEncoding: 'gzip;q=0, br;q=0, identity', expected: 'raw'},
            {name: 'identity preferred over codings', acceptEncoding: 'gzip;q=0.5, identity', expected: 'raw'},
            {name: 'identity with equal weight loses to codings', acceptEncoding: 'gzip, identity', expected: 'gzip'},
            {name: 'implicit identity does not beat a low q-value', acceptEncoding: 'gzip;q=0.5', expected: 'gzip'},
            {name: 'empty header', acceptEncoding: '', expected: 'raw'},
        ];
        for (const {name, gzipFirst, acceptEncoding, expected} of cases) {
            test(name, async () => {
                const reply = await request(gzipFirst ? gzipFirstPort : port, '/app.js', {'accept-encoding': acceptEncoding});
                if (expected === 'raw') {
                    assertRaw(reply);
                } else {
                    assertPrebuilt(reply, expected, expected === 'br' ? br : gz);
                }
            });
        }
    });

    test('missing encoding falls back to the next one', async () => {
        assertPrebuilt(await request(port, '/gz-only.js', {'accept-encoding': 'gzip, br'}), 'gzip', gz);
    });

    test('nested paths and query strings', async () => {
        assertPrebuilt(await request(port, '/nested/app.js?v=1', {'accept-encoding': 'br'}), 'br', br);
    });

    test('SPA routes are served from the pre-compressed index.html', async () => {
        const reply = await request(port, '/dashboards/123', {'accept': 'text/html', 'accept-encoding': 'br'});
        assertPrebuilt(reply, 'br', br, /^text\/html/);
    });

    test('files without a pre-compressed variant are compressed on the fly', async () => {
        assertCompressedOnTheFly(await request(port, '/raw-only.js', {'accept-encoding': 'gzip'}), 'gzip');
    });

    test('pre-compressed files requested directly are not handled', async () => {
        const reply = await request(port, '/app.js.br', {'accept-encoding': 'identity'});
        assert.equal(reply.headers['content-encoding'], undefined);
        assert.deepEqual(reply.body, br);
    });

    test('HEAD and Range are served from the pre-compressed file', async () => {
        const head = await request(port, '/app.js', {'accept-encoding': 'br'}, 'HEAD');
        assert.equal(head.status, 200);
        assert.equal(head.headers['content-length'], String(br.length));
        const range = await request(port, '/app.js', {'accept-encoding': 'br', 'range': 'bytes=0-99'});
        assert.equal(range.status, 206);
        assert.equal(range.headers['content-range'], `bytes 0-99/${br.length}`);
        assert.deepEqual(range.body, br.subarray(0, 100));
    });

    test('unsatisfiable range is answered for the negotiated encoding, not retried with the next one', async () => {
        assert.ok(gz.length > br.length + 10);
        const reply = await request(port, '/app.js', {'accept-encoding': 'br, gzip', 'range': `bytes=${br.length + 10}-`});
        assert.equal(reply.status, 416);
        assert.equal(reply.headers['content-range'], `bytes */${br.length}`);
        assert.equal(reply.headers['content-encoding'], undefined);
        assert.match(reply.headers['vary']!, /Accept-Encoding/i);
    });

    test('failed precondition is not retried with the next encoding', async () => {
        const gzipEtag = (await request(port, '/app.js', {'accept-encoding': 'gzip'})).headers['etag']!;
        const reply = await request(port, '/app.js', {'accept-encoding': 'br, gzip', 'if-match': gzipEtag});
        assert.equal(reply.status, 412);
        assert.equal(reply.headers['content-encoding'], undefined);
    });

    test('unreadable file falls back to the next encoding with its own headers', skipAsRoot, async () => {
        const reply = await request(port, '/unreadable-br.js', {'accept-encoding': 'gzip, br'});
        assertPrebuilt(reply, 'gzip', gz);
        assert.match(reply.headers['etag']!, new RegExp(`^W/"${gz.length.toString(16)}-`));
    });

    test('no readable pre-compressed file falls back to on-the-fly compression', skipAsRoot, async () => {
        assertCompressedOnTheFly(await request(port, '/unreadable-all.js', {'accept-encoding': 'gzip, br'}), 'br');
    });

    for (const urlPath of ['/..%2fsecret.js', '/%2e%2e/secret.js', '/%2e%2e%2fsecret.js', '/nested/..%2f..%2fsecret.js', '/app.js%00']) {
        test(`path traversal is not resolved: ${urlPath}`, async () => {
            for (const acceptEncoding of ['br', 'gzip', 'identity']) {
                const reply = await request(port, urlPath, {'accept-encoding': acceptEncoding});
                assert.notEqual(reply.status, 200, acceptEncoding);
                assert.equal(reply.headers['content-encoding'], undefined, acceptEncoding);
                assert.ok(!reply.body.toString().includes('TOP-SECRET'), acceptEncoding);
            }
        });
    }

    // a png: compression() would add Vary itself for compressible types
    test('responses vary on Accept-Encoding even when the request has none', async () => {
        const reply = await request(port, '/logo.png');
        assertRaw(reply);
        assert.match(reply.headers['vary']!, /Accept-Encoding/i);
    });

    test('no pre-compressed encodings: everything is compressed on the fly', async () => {
        const nonePort = await startServer(root, parsePrecompressedEncodings('none'));
        assertCompressedOnTheFly(await request(nonePort, '/app.js', {'accept-encoding': 'gzip'}), 'gzip');
    });

    test('compression disabled: pre-compressed files are not served', async () => {
        const disabledPort = await startServer(root, ['br', 'gzip'], false);
        const reply = await request(disabledPort, '/app.js', {'accept-encoding': 'br, gzip'});
        assertRaw(reply);
        assert.equal(reply.headers['vary'], undefined);
    });

    test('non-GET/HEAD requests are not served from pre-compressed files', async () => {
        const reply = await request(port, '/app.js', {'accept-encoding': 'br'}, 'POST');
        assert.equal(reply.status, 404);
        assert.notEqual(reply.headers['content-encoding'], 'br');
    });

    test('malformed percent-encoding falls through', async () => {
        const reply = await request(port, '/%E0%A4%A.js', {'accept-encoding': 'br'});
        assert.equal(reply.status, 404);
        assert.notEqual(reply.headers['content-encoding'], 'br');
    });

    test('range of a file without a pre-compressed variant is not compressed on the fly', async () => {
        const length = Buffer.byteLength(content);
        const reply = await request(port, '/raw-only.js', {'accept-encoding': 'gzip', 'range': 'bytes=10-'});
        assert.equal(reply.status, 206);
        assert.equal(reply.headers['content-encoding'], undefined);
        assert.equal(reply.headers['content-range'], `bytes 10-${length - 1}/${length}`);
        assert.match(reply.headers['vary']!, /Accept-Encoding/i);
        assert.equal(reply.body.toString(), content.slice(10));
    });

    test('missing web root falls through', async () => {
        const missingPort = await startServer(path.join(root, 'missing'), ['br', 'gzip']);
        assert.equal((await request(missingPort, '/app.js', {'accept-encoding': 'br'})).status, 404);
    });
});

describe('parsePrecompressedEncodings', () => {
    test('parses, normalizes and dedupes', () => {
        assert.deepEqual(parsePrecompressedEncodings(' BR, gzip ,br'), ['br', 'gzip']);
        assert.deepEqual(parsePrecompressedEncodings(['gzip', 'br']), ['gzip', 'br']);
    });

    test('drops unsupported encodings and "none"', () => {
        assert.deepEqual(parsePrecompressedEncodings('gzip,bogus'), ['gzip']);
        assert.deepEqual(parsePrecompressedEncodings('none'), []);
        assert.deepEqual(parsePrecompressedEncodings(''), []);
        assert.deepEqual(parsePrecompressedEncodings(undefined), []);
    });

    test('warns when nothing usable is configured, but not for "none"', (t) => {
        const output = captureOutput(t);
        assert.deepEqual(parsePrecompressedEncodings('  '), []);
        assert.match(output(), /No supported pre-compressed file encodings/);
        const afterBlank = output().length;
        parsePrecompressedEncodings('none');
        assert.doesNotMatch(output().slice(afterBlank), /No supported/);
    });
});
