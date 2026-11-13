// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { after, before, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { ChildProcess, execFileSync, spawn } from 'child_process';
import fs from 'fs';
import os from 'os';
import path from 'path';
import { assertPrebuilt, br, cleanEnv, gz, request, webUiDir, writeFixture } from './helpers';

// runs the tsc output like the Docker image does, so missing files or broken relative requires fail here
describe('compiled server.js', () => {
    let workDir: string;
    let port: number;
    let server: ChildProcess | undefined;
    let output = '';

    before(async () => {
        workDir = fs.mkdtempSync(path.join(os.tmpdir(), 'tb-web-ui-server-test-'));
        const outDir = path.join(workDir, 'src');
        execFileSync(process.execPath, [require.resolve('typescript/bin/tsc'), '-p', webUiDir, '--outDir', outDir]);
        writeFixture(workDir, 'web/public/index.html', {withBr: true, withGz: true});
        writeFixture(workDir, 'web/public/app.js', {withBr: true, withGz: true});
        server = spawn(process.execPath, [path.join(outDir, 'server.js')], {
            cwd: outDir,
            env: cleanEnv({
                NODE_PATH: path.join(webUiDir, 'node_modules'),
                WEB_FOLDER: path.join(workDir, 'web'),
                HTTP_BIND_ADDRESS: '127.0.0.1',
                HTTP_BIND_PORT: '0'
            }),
            stdio: ['ignore', 'pipe', 'pipe']
        });
        server.stdout!.on('data', chunk => output += chunk);
        server.stderr!.on('data', chunk => output += chunk);
        port = await new Promise<number>((resolve, reject) => {
            const timer = setTimeout(() => reject(new Error('server.js did not start:\n' + output)), 30000);
            const check = () => {
                const listening = /Listening on port (\d+)/.exec(output);
                if (listening) {
                    clearTimeout(timer);
                    resolve(Number(listening[1]));
                } else if (server!.exitCode !== null) {
                    clearTimeout(timer);
                    reject(new Error('server.js exited:\n' + output));
                } else {
                    setTimeout(check, 50);
                }
            };
            check();
        });
    });

    after(async () => {
        if (server && server.exitCode === null) {
            await new Promise(resolve => {
                server!.once('exit', resolve);
                server!.kill();
            });
        }
        fs.rmSync(workDir, {recursive: true, force: true});
    });

    test('serves pre-compressed files configured by default.yml', async () => {
        assertPrebuilt(await request(port, '/dashboards/1', {'accept': 'text/html', 'accept-encoding': 'br'}), 'br', br, /^text\/html/);
        assertPrebuilt(await request(port, '/app.js', {'accept-encoding': 'gzip'}), 'gzip', gz);
    });

    test('answers client errors through errorHandler', async () => {
        const reply = await request(port, '/app.js', {'range': 'bytes=99999999-'});
        assert.equal(reply.status, 416);
        assert.equal(reply.body.toString(), 'Range Not Satisfiable');
        assert.doesNotMatch(output, /RangeNotSatisfiableError|\[errorHandler\] [A-Z]+:/);
    });
});
