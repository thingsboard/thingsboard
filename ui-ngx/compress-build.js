// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0

// Writes a gzip copy (<file>.gz) next to every compressible file of the production build ("yarn run compress",
// run by Maven right after "build:prod"). The monolith (Spring resource chain) and msa/web-ui serve these copies
// as-is, so neither compresses UI files per request.
// This is not an esbuild plugin on purpose: Angular passes custom plugins only to the browser code bundle,
// so polyfills, global styles and scripts, and the copied assets (locales, hugerte, ace workers) never reach them.

const fs = require('node:fs');
const path = require('node:path');
const { promisify } = require('node:util');
const gzip = promisify(require('node:zlib').gzip);
const { constants } = require('node:zlib');

const buildDir = path.join(__dirname, 'target', 'generated-resources', 'public');
// already-compressed formats (png, webp, woff2, ...) are left out
const compressibleExtensions = new Set(['.js', '.mjs', '.css', '.html', '.json', '.svg', '.txt', '.md', '.ttf', '.otf', '.eot']);
const minSize = 10240;
// zlib runs on the libuv thread pool (4 threads by default)
const concurrency = 4;

function listFiles(dir) {
  return fs.readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter(entry => entry.isFile())
    .map(entry => path.join(entry.parentPath, entry.name));
}

async function compress(file) {
  const content = await fs.promises.readFile(file);
  const compressed = await gzip(content, { level: constants.Z_BEST_COMPRESSION });
  if (compressed.length < content.length) {
    await fs.promises.writeFile(`${file}.gz`, compressed);
    return content.length - compressed.length;
  }
  return 0;
}

async function main() {
  const files = listFiles(buildDir).filter(file =>
    compressibleExtensions.has(path.extname(file).toLowerCase()) && fs.statSync(file).size >= minSize);
  let next = 0;
  let saved = 0;
  const worker = async () => {
    while (next < files.length) {
      const fileSaved = await compress(files[next++]);
      saved += fileSaved;
    }
  };
  await Promise.all(Array.from({ length: concurrency }, worker));
  console.log(`Compressed ${files.length} files in ${path.relative(__dirname, buildDir)}, ${(saved / 1048576).toFixed(1)} MiB saved`);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
