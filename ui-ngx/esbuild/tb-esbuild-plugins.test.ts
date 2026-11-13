// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { after, before, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import * as fs from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';
import * as zlib from 'node:zlib';
import { build, OutputFile, Plugin } from 'esbuild';
import plugins, { compressorPlugin, isProduction } from './tb-esbuild-plugins';

const bigJsFiles = Array.from({length: 6}, (_, i) => `big${i}.js`);
// every type the UI build must pre-compress; copied verbatim so the plugin sees them as build outputs
const bigAssetFiles = ['.css', '.html', '.svg', '.png', '.jpg', '.ttf', '.gif', '.woff', '.woff2', '.eot', '.json'].map(ext => `big${ext}`);
const compressedFiles = [...bigJsFiles, ...bigAssetFiles];
const uncompressedFiles = ['small.js', 'big.txt'];

function bigContent(seed: number): string {
  return Array.from({length: 1500}, (_, i) => `console.log(${(i * 7919 + seed) % 10007});`).join('\n');
}

describe('tb-compressor-plugin', () => {
  let root: string;

  before(() => {
    root = fs.mkdtempSync(path.join(os.tmpdir(), 'tb-esbuild-plugins-test-'));
    compressedFiles.forEach((name, i) => fs.writeFileSync(path.join(root, name), bigContent(i)));
    fs.writeFileSync(path.join(root, 'small.js'), 'console.log(1);');
    fs.writeFileSync(path.join(root, 'big.txt'), bigContent(100));
  });

  after(() => fs.rmSync(root, {recursive: true, force: true}));

  async function runBuild(plugin: Plugin): Promise<Map<string, OutputFile>> {
    const copyLoaders = Object.fromEntries(['.txt', ...bigAssetFiles.map(name => path.extname(name))].map(ext => [ext, 'copy' as const]));
    const result = await build({
      absWorkingDir: root,
      entryPoints: [...compressedFiles, ...uncompressedFiles],
      outdir: 'out',
      loader: copyLoaders,
      write: false,
      logLevel: 'silent',
      plugins: [plugin]
    });
    return new Map(result.outputFiles.map(file => [path.relative(path.join(root, 'out'), file.path), file]));
  }

  test('emits max-quality .br and .gz next to large files of every compressible type', async () => {
    const outputs = await runBuild(compressorPlugin(() => true));
    // must match precompressedExtensions in msa/web-ui/middleware/precompressedStatic.ts
    const expected = [...compressedFiles.flatMap(name => [name, `${name}.gz`, `${name}.br`]), ...uncompressedFiles];
    assert.deepEqual([...outputs.keys()].sort(), expected.sort());
    for (const name of compressedFiles) {
      const raw = Buffer.from(outputs.get(name)!.contents);
      const maxQualityBrotli = zlib.brotliCompressSync(raw, {params: {
        [zlib.constants.BROTLI_PARAM_QUALITY]: zlib.constants.BROTLI_MAX_QUALITY,
        [zlib.constants.BROTLI_PARAM_SIZE_HINT]: raw.length
      }});
      assert.deepEqual(Buffer.from(outputs.get(`${name}.br`)!.contents), maxQualityBrotli, `${name}.br`);
      assert.deepEqual(zlib.gunzipSync(outputs.get(`${name}.gz`)!.contents), raw, `${name}.gz`);
    }
  });

  test('non-production builds are not compressed', async () => {
    const outputs = await runBuild(compressorPlugin(() => false));
    assert.deepEqual([...outputs.keys()].sort(), [...compressedFiles, ...uncompressedFiles].sort());
  });

  test('the exported plugin list compresses only for --configuration production', async () => {
    const plugin = plugins.find(candidate => candidate.name === 'tb-compressor-plugin');
    assert.ok(plugin, 'tb-compressor-plugin is registered');
    const argv = process.argv;
    try {
      process.argv = [...argv, '--configuration', 'production'];
      assert.ok((await runBuild(plugin)).has('big0.js.br'));
      process.argv = [...argv, '--configuration', 'development'];
      assert.ok(!(await runBuild(plugin)).has('big0.js.br'));
    } finally {
      process.argv = argv;
    }
  });
});

describe('isProduction', () => {
  test('reads the value after --configuration', () => {
    assert.equal(isProduction(['node', 'ng', 'build', '--configuration', 'production']), true);
    assert.equal(isProduction(['node', 'ng', 'build', '--configuration', 'development']), false);
    assert.equal(isProduction(['node', 'ng', 'build']), false);
    assert.equal(isProduction(['node', 'ng', 'build', '--configuration']), false);
  });
});
