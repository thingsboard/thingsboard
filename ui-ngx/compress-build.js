// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0

// Writes brotli (<file>.br) and gzip (<file>.gz) copies next to every compressible file of the production build
// ("yarn run compress", run by Maven right after "build:prod"). The monolith (Spring resource chain) and msa/web-ui
// serve these copies as-is, so neither compresses UI files per request.
// This is not an esbuild plugin on purpose: Angular passes custom plugins only to the browser code bundle,
// so polyfills, global styles and scripts, and the copied assets (locales, hugerte, ace workers) never reach them.

const os = require('node:os');

// zlib runs on the libuv thread pool (4 threads by default); it must be sized before its first use
const concurrency = os.availableParallelism();
process.env.UV_THREADPOOL_SIZE = String(concurrency);

const fs = require('node:fs');
const path = require('node:path');
const { promisify } = require('node:util');
const zlib = require('node:zlib');

const gzip = promisify(zlib.gzip);
const brotliCompress = promisify(zlib.brotliCompress);

const buildDir = path.join(__dirname, 'target', 'generated-resources', 'public');
// already-compressed formats (png, webp, woff2, ...) are left out
const compressibleExtensions = new Set(['.js', '.mjs', '.css', '.html', '.json', '.svg', '.txt', '.md', '.ttf', '.otf', '.eot']);
const minSize = 10240;

// quality 10 is ~21% smaller than gzip -9 on the UI bundle; 11 saves only 2% more and takes 3x longer
const encoders = [
  {
    ext: '.br',
    encode: content => brotliCompress(content, {
      params: {
        [zlib.constants.BROTLI_PARAM_QUALITY]: 10,
        [zlib.constants.BROTLI_PARAM_SIZE_HINT]: content.length
      }
    })
  },
  {
    ext: '.gz',
    encode: content => gzip(content, { level: zlib.constants.Z_BEST_COMPRESSION })
  }
];

function listFiles(dir) {
  return fs.readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter(entry => entry.isFile())
    .map(entry => path.join(entry.parentPath, entry.name));
}

async function compress(file, { ext, encode }) {
  const content = await fs.promises.readFile(file);
  const compressed = await encode(content);
  if (compressed.length < content.length) {
    await fs.promises.writeFile(file + ext, compressed);
  }
}

async function main() {
  // largest first, so the main chunk does not end up last on a single thread
  const files = listFiles(buildDir)
    .map(file => ({ file, size: fs.statSync(file).size }))
    .filter(({ file, size }) => compressibleExtensions.has(path.extname(file).toLowerCase()) && size >= minSize)
    .sort((a, b) => b.size - a.size)
    .map(({ file }) => file);
  const jobs = files.flatMap(file => encoders.map(encoder => () => compress(file, encoder)));
  let next = 0;
  const worker = async () => {
    while (next < jobs.length) {
      await jobs[next++]();
    }
  };
  const start = Date.now();
  await Promise.all(Array.from({ length: concurrency }, worker));
  console.log(`Compressed ${files.length} files (${encoders.map(e => e.ext).join(', ')}) in ${path.relative(__dirname, buildDir)} ` +
    `in ${((Date.now() - start) / 1000).toFixed(1)} s on ${concurrency} threads`);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
