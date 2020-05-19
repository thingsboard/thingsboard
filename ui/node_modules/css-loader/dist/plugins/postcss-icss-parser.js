"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _postcss = _interopRequireDefault(require("postcss"));

var _icssUtils = require("icss-utils");

var _loaderUtils = require("loader-utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const pluginName = 'postcss-icss-parser';

function normalizeIcssImports(icssImports) {
  return Object.keys(icssImports).reduce((accumulator, url) => {
    const tokensMap = icssImports[url];
    const tokens = Object.keys(tokensMap);

    if (tokens.length === 0) {
      return accumulator;
    }

    const normalizedUrl = (0, _loaderUtils.urlToRequest)(url);

    if (!accumulator[normalizedUrl]) {
      // eslint-disable-next-line no-param-reassign
      accumulator[normalizedUrl] = tokensMap;
    } else {
      // eslint-disable-next-line no-param-reassign
      accumulator[normalizedUrl] = { ...accumulator[normalizedUrl],
        ...tokensMap
      };
    }

    return accumulator;
  }, {});
}

var _default = _postcss.default.plugin(pluginName, () => function process(css, result) {
  const importReplacements = Object.create(null);
  const {
    icssImports,
    icssExports
  } = (0, _icssUtils.extractICSS)(css);
  const normalizedIcssImports = normalizeIcssImports(icssImports);
  Object.keys(normalizedIcssImports).forEach((url, importIndex) => {
    const importName = `___CSS_LOADER_ICSS_IMPORT_${importIndex}___`;
    result.messages.push({
      pluginName,
      type: 'import',
      value: {
        type: 'icss-import',
        importName,
        url
      }
    });
    const tokenMap = normalizedIcssImports[url];
    const tokens = Object.keys(tokenMap);
    tokens.forEach((token, replacementIndex) => {
      const replacementName = `___CSS_LOADER_ICSS_IMPORT_${importIndex}_REPLACEMENT_${replacementIndex}___`;
      const localName = tokenMap[token];
      importReplacements[token] = replacementName;
      result.messages.push({
        pluginName,
        type: 'replacer',
        value: {
          type: 'icss-import',
          importName,
          replacementName,
          localName
        }
      });
    });
  });

  if (Object.keys(importReplacements).length > 0) {
    (0, _icssUtils.replaceSymbols)(css, importReplacements);
  }

  Object.keys(icssExports).forEach(name => {
    const value = (0, _icssUtils.replaceValueSymbols)(icssExports[name], importReplacements);
    result.messages.push({
      pluginName,
      type: 'export',
      value: {
        name,
        value
      }
    });
  });
});

exports.default = _default;