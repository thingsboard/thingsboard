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

import type { Plugin, PluginBuild, OutputFile } from 'esbuild';
import dirTree from 'directory-tree';
import * as packageJson from '../package.json';
import { gzip } from 'node:zlib';
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

const compressorPlugin: Plugin = {
  name: 'tb-compressor-plugin',
  setup(build) {
    build.onEnd(async result => {
      if (!result.outputFiles || !isProduction()) return;
      const outputExt = '.gz';
      const gzippedFiles: OutputFile[] = [];
      for (const file of result.outputFiles) {
        if (!compressFileTypes.some((ext) => ext === path.extname(file.path))) continue;
        if (file.contents.byteLength <= compressThreshold) continue;
        const compressedContent = await gzipContent(file.contents);
        const compressedFilePath = `${file.path}${outputExt}`;
        gzippedFiles.push(
          {
            path: compressedFilePath,
            hash: file.hash,
            contents: new Uint8Array(compressedContent),
            text: '',
          }
        );
      }
      result.outputFiles.push(...gzippedFiles);
    });
  },
};

async function gzipContent(content): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    gzip(content, (error, result) => {
      if (error) {
        reject(error);
      } else {
        resolve(result);
      }
    });
  });
}

function isProduction(): boolean {
  const configurationIndex = process.argv.indexOf('--configuration');
  let production = false;
  if (configurationIndex > -1) {
    const configurationValue = process.argv[configurationIndex + 1];
    production = configurationValue === 'production';
  }
  return production;
}

export default [defineTbVariablesPlugin, resolveJQueryPlugin, compressorPlugin];
