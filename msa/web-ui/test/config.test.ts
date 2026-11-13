// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'child_process';
import { precompressedEncodingsFromConfig } from '../middleware/precompressedStatic';
import { cleanEnv, webUiDir } from './helpers';

describe('precompressedEncodingsFromConfig', () => {
    const fakeConfig = (values: Record<string, unknown>) => ({
        has: (key: string) => key in values,
        get: (key: string) => values[key]
    });

    test('missing key disables pre-compressed serving', () => {
        assert.deepEqual(precompressedEncodingsFromConfig(fakeConfig({})), []);
    });

    test('reads the configured key', () => {
        assert.deepEqual(precompressedEncodingsFromConfig(fakeConfig({'server.compression.precompressed_encodings': 'gzip,br'})), ['gzip', 'br']);
    });

    function fromRealConfig(env: Record<string, string>): string[] {
        const script = "process.stdout.write(JSON.stringify(require('./middleware/precompressedStatic').precompressedEncodingsFromConfig(require('config'))))";
        const output = execFileSync(process.execPath, ['--require', 'ts-node/register', '-e', script], {
            cwd: webUiDir,
            env: cleanEnv(env),
            encoding: 'utf8'
        });
        return JSON.parse(output.slice(output.lastIndexOf('[')));
    }

    test('default.yml and the environment variable map to the key', () => {
        assert.deepEqual(fromRealConfig({}), ['br', 'gzip']);
        assert.deepEqual(fromRealConfig({HTTP_COMPRESSION_PRECOMPRESSED_ENCODINGS: 'gzip'}), ['gzip']);
        assert.deepEqual(fromRealConfig({HTTP_COMPRESSION_PRECOMPRESSED_ENCODINGS: 'none'}), []);
    });
});
