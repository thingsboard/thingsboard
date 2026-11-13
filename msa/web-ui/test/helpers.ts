// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TestContext } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import http from 'http';
import path from 'path';
import zlib from 'zlib';
import { AddressInfo } from 'net';
import express from 'express';
import { staticMiddlewares } from '../middleware/staticMiddlewares';
import { errorHandler } from '../middleware/errorHandler';

export const content = Array.from({length: 2000}, (_, i) => `console.log(${(i * 7919) % 10007});`).join('\n');
export const gz = zlib.gzipSync(content);
export const br = zlib.brotliCompressSync(content);
export const webUiDir = path.join(__dirname, '..');

export interface Reply {
    status: number;
    headers: http.IncomingHttpHeaders;
    body: Buffer;
}

export interface Fixture {
    withBr?: boolean;
    withGz?: boolean;
    unreadable?: ('br' | 'gz')[];
}

export function writeFixture(root: string, name: string, {withBr = false, withGz = false, unreadable = []}: Fixture) {
    const file = path.join(root, name);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, content);
    if (withBr) {
        fs.writeFileSync(file + '.br', br, {mode: unreadable.includes('br') ? 0o000 : 0o644});
    }
    if (withGz) {
        fs.writeFileSync(file + '.gz', gz, {mode: unreadable.includes('gz') ? 0o000 : 0o644});
    }
}

export function request(port: number, urlPath: string, headers: http.OutgoingHttpHeaders = {}, method = 'GET'): Promise<Reply> {
    return new Promise((resolve, reject) => {
        http.request({host: '127.0.0.1', port, path: urlPath, method, headers}, res => {
            const chunks: Buffer[] = [];
            res.on('data', chunk => chunks.push(chunk));
            res.on('end', () => resolve({status: res.statusCode!, headers: res.headers, body: Buffer.concat(chunks)}));
        }).on('error', reject).end();
    });
}

// child processes must not inherit a developer's config overrides
export function cleanEnv(env: Record<string, string>): NodeJS.ProcessEnv {
    const {HTTP_COMPRESSION_PRECOMPRESSED_ENCODINGS, HTTP_COMPRESSION_ENABLED, HTTP_BIND_PORT, NODE_CONFIG, NODE_ENV, ...inherited} = process.env;
    return {...inherited, NODE_CONFIG_DIR: path.join(webUiDir, 'config'), ...env};
}

// passes everything through: node:test also reports over stdout, so assert on log-shaped text only
export function captureOutput(t: TestContext): () => string {
    const chunks: string[] = [];
    for (const stream of [process.stdout, process.stderr]) {
        const write = stream.write.bind(stream) as (...args: any[]) => boolean;
        t.mock.method(stream, 'write', (chunk: any, ...args: any[]) => {
            chunks.push(String(chunk));
            return write(chunk, ...args);
        });
    }
    return () => chunks.join('');
}

const servers: http.Server[] = [];

export function closeServers() {
    for (const server of servers.splice(0)) {
        server.closeAllConnections();
        server.close();
    }
}

export async function startServer(root: string, encodings: string[], compressionEnabled = true,
                                  configure?: (app: express.Express) => void): Promise<number> {
    const app = express();
    configure?.(app);
    for (const middleware of staticMiddlewares(root, {compressionEnabled, precompressedEncodings: encodings})) {
        app.use(middleware);
    }
    app.use(errorHandler);
    const server = http.createServer(app);
    await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
    servers.push(server);
    return (server.address() as AddressInfo).port;
}

export function assertPrebuilt(reply: Reply, encoding: string, expected: Buffer, contentType = /^text\/javascript/) {
    assert.equal(reply.status, 200);
    assert.equal(reply.headers['content-encoding'], encoding);
    assert.equal(reply.headers['content-length'], String(expected.length));
    assert.match(reply.headers['content-type']!, contentType);
    assert.match(reply.headers['vary']!, /Accept-Encoding/i);
    assert.deepEqual(reply.body, expected);
}

export function assertRaw(reply: Reply) {
    assert.equal(reply.status, 200);
    assert.equal(reply.headers['content-encoding'], undefined);
    assert.equal(reply.body.toString(), content);
}

export function assertCompressedOnTheFly(reply: Reply, encoding: string) {
    assert.equal(reply.status, 200);
    assert.equal(reply.headers['content-encoding'], encoding);
    assert.equal(reply.headers['transfer-encoding'], 'chunked');
    const decompress = encoding === 'br' ? zlib.brotliDecompressSync : zlib.gunzipSync;
    assert.equal(decompress(reply.body).toString(), content);
}
