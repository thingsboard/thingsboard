// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import type { Plugin, PluginBuild } from 'esbuild';
import dirTree from 'directory-tree';
import * as packageJson from '../package.json';

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

function isProduction(): boolean {
  const configurationIndex = process.argv.indexOf('--configuration');
  let production = false;
  if (configurationIndex > -1) {
    const configurationValue = process.argv[configurationIndex + 1];
    production = configurationValue === 'production';
  }
  return production;
}

export default [defineTbVariablesPlugin, resolveJQueryPlugin];
