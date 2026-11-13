// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import type { Plugin, PluginBuild, OutputFile } from 'esbuild';
import dirTree from 'directory-tree';
import * as packageJson from '../package.json';
import { brotliCompress, constants as zlibConstants, gzip } from 'node:zlib';
import { promisify } from 'node:util';
import * as path from 'node:path';

const defineTbVariablesPlugin: Plugin = {
  name: 'tb-define-variables',
  setup(build: PluginBuild) {
    const options = build.initialOptions;

    const langs: string[] = [];

    dirTree("./src/assets/locale/", {extensions: /\.json$/}, (item) => {
      /* It is expected what the name of a locale file has the following format: */
      /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
      langs.push(item.name.slice(item.name.lastIndexOf("-") + 1, -5));
    });
    options.define.TB_VERSION = JSON.stringify(packageJson.version);
    options.define.SUPPORTED_LANGS = JSON.stringify(langs);
    options.define.ngJitMode = 'true';
  },
};

const resolveJQueryPlugin: Plugin = {
  name: 'tb-resolve-jquery-plugin',
  setup(build: PluginBuild) {
    if (isProduction()) {
      const jQueryPath = require.resolve('jquery');
      build.onResolve({filter: /^(jquery|\$)$/}, () => {
        return {path: jQueryPath};
      })
    }
  }
};

const compressFileTypes = ['.js', '.css', '.html', '.svg', '.png', '.jpg', '.ttf', '.gif', '.woff', '.woff2', '.eot', '.json'];
const compressThreshold = 10240;

const gzipAsync = promisify(gzip);
const brotliAsync = promisify(brotliCompress);

const compressors: { ext: string; compress: (content: Uint8Array) => Promise<Buffer> }[] = [
  {ext: '.gz', compress: (content) => gzipAsync(content)},
  {
    ext: '.br',
    compress: (content) => brotliAsync(content, {
      params: {
        [zlibConstants.BROTLI_PARAM_QUALITY]: zlibConstants.BROTLI_MAX_QUALITY,
        [zlibConstants.BROTLI_PARAM_SIZE_HINT]: content.byteLength
      }
    })
  }
];

// zlib runs on the libuv thread pool (default size 4); more jobs in flight only add memory
const compressConcurrency = Number(process.env.UV_THREADPOOL_SIZE) || 4;

async function runWithConcurrency<T>(jobs: (() => Promise<T>)[], limit: number): Promise<T[]> {
  const results: T[] = new Array(jobs.length);
  let nextIndex = 0;
  const worker = async () => {
    while (nextIndex < jobs.length) {
      const index = nextIndex++;
      results[index] = await jobs[index]();
    }
  };
  await Promise.all(Array.from({length: Math.min(limit, jobs.length)}, worker));
  return results;
}

export function compressorPlugin(production: () => boolean = () => isProduction()): Plugin {
  return {
    name: 'tb-compressor-plugin',
    setup(build) {
      build.onEnd(async result => {
        if (!result.outputFiles || !production()) return;
        const filesToCompress = result.outputFiles.filter((file) =>
          compressFileTypes.includes(path.extname(file.path)) && file.contents.byteLength > compressThreshold);
        const jobs = filesToCompress.flatMap((file) => compressors.map(({ext, compress}) => async (): Promise<OutputFile> => ({
          path: `${file.path}${ext}`,
          hash: file.hash,
          contents: new Uint8Array(await compress(file.contents)),
          text: '',
        })));
        result.outputFiles.push(...await runWithConcurrency(jobs, compressConcurrency));
      });
    },
  };
}

export function isProduction(argv: string[] = process.argv): boolean {
  const configurationIndex = argv.indexOf('--configuration');
  let production = false;
  if (configurationIndex > -1) {
    const configurationValue = argv[configurationIndex + 1];
    production = configurationValue === 'production';
  }
  return production;
}

export default [defineTbVariablesPlugin, resolveJQueryPlugin, compressorPlugin()];
