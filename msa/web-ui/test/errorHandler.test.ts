// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { after, before, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import http from 'http';
import os from 'os';
import path from 'path';
import { captureOutput, closeServers, content, request, startServer, writeFixture } from './helpers';

after(closeServers);

describe('errorHandler', () => {
    let root: string;
    let port: number;

    before(async () => {
        root = fs.mkdtempSync(path.join(os.tmpdir(), 'tb-web-ui-error-test-'));
        writeFixture(root, 'app.js', {});
        port = await startServer(root, [], true, app => {
            app.get('/boom', () => {
                throw new Error('secret internals');
            });
            app.get('/mixed-status', (_req, _res, next) => next(Object.assign(new Error('mixed'), {status: 200, statusCode: 404})));
            app.get('/encoded-error', (_req, _res, next) => next(Object.assign(new Error('auth'),
                {status: 401, headers: {'Content-Encoding': 'gzip', 'WWW-Authenticate': 'Basic'}})));
            app.get('/partial', (_req, res, next) => {
                res.writeHead(200, {'content-type': 'text/plain'});
                res.write('partial');
                next(new Error('failed mid-response'));
            });
        });
    });

    after(() => fs.rmSync(root, {recursive: true, force: true}));

    test('client errors are answered without logging', async (t) => {
        const output = captureOutput(t);
        const range = await request(port, '/app.js', {'range': 'bytes=99999999-'});
        assert.equal(range.status, 416);
        assert.equal(range.headers['content-range'], `bytes */${Buffer.byteLength(content)}`);
        assert.equal(range.body.toString(), 'Range Not Satisfiable');
        const precondition = await request(port, '/app.js', {'if-match': '"no-such-etag"'});
        assert.equal(precondition.status, 412);
        assert.doesNotMatch(output(), /\[errorHandler\] [A-Z]+:|RangeNotSatisfiableError|PreconditionFailedError/);
    });

    test('server errors are logged but not leaked to the client', async (t) => {
        const output = captureOutput(t);
        const reply = await request(port, '/boom');
        assert.equal(reply.status, 500);
        assert.equal(reply.body.toString(), 'Internal Server Error');
        assert.match(output(), /\[errorHandler\] ERROR: GET \/boom failed: Error: secret internals/);
    });

    test('status is taken from the first error code in the 4xx/5xx range', async () => {
        const reply = await request(port, '/mixed-status');
        assert.equal(reply.status, 404);
    });

    test('error headers cannot re-add content encoding headers', async () => {
        const reply = await request(port, '/encoded-error');
        assert.equal(reply.status, 401);
        assert.equal(reply.headers['content-encoding'], undefined);
        assert.equal(reply.headers['www-authenticate'], 'Basic');
        assert.equal(reply.body.toString(), 'Unauthorized');
    });

    test('error after headers were sent closes the connection', {timeout: 5000}, async () => {
        const complete = await new Promise<boolean>(resolve => {
            http.get({host: '127.0.0.1', port, path: '/partial'}, res => {
                res.on('error', () => {});
                res.on('data', () => {});
                res.on('close', () => resolve(res.complete));
            }).on('error', () => resolve(false));
        });
        assert.equal(complete, false);
    });
});
