"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.normalizeUrl = normalizeUrl;
exports.getFilter = getFilter;
exports.getModulesPlugins = getModulesPlugins;
exports.normalizeSourceMap = normalizeSourceMap;
exports.getImportCode = getImportCode;
exports.getModuleCode = getModuleCode;
exports.getExportCode = getExportCode;

var _path = _interopRequireDefault(require("path"));

var _loaderUtils = _interopRequireWildcard(require("loader-utils"));

var _normalizePath = _interopRequireDefault(require("normalize-path"));

var _cssesc = _interopRequireDefault(require("cssesc"));

var _postcssModulesValues = _interopRequireDefault(require("postcss-modules-values"));

var _postcssModulesLocalByDefault = _interopRequireDefault(require("postcss-modules-local-by-default"));

var _postcssModulesExtractImports = _interopRequireDefault(require("postcss-modules-extract-imports"));

var _postcssModulesScope = _interopRequireDefault(require("postcss-modules-scope"));

var _camelcase = _interopRequireDefault(require("camelcase"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function () { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } if (obj === null || typeof obj !== "object" && typeof obj !== "function") { return { default: obj }; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj.default = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/*
  MIT License http://www.opensource.org/licenses/mit-license.php
  Author Tobias Koppers @sokra
*/
const whitespace = '[\\x20\\t\\r\\n\\f]';
const unescapeRegExp = new RegExp(`\\\\([\\da-f]{1,6}${whitespace}?|(${whitespace})|.)`, 'ig');

function unescape(str) {
  return str.replace(unescapeRegExp, (_, escaped, escapedWhitespace) => {
    const high = `0x${escaped}` - 0x10000;
    /* eslint-disable line-comment-position */
    // NaN means non-codepoint
    // Workaround erroneous numeric interpretation of +"0x"
    // eslint-disable-next-line no-self-compare

    return high !== high || escapedWhitespace ? escaped : high < 0 ? // BMP codepoint
    String.fromCharCode(high + 0x10000) : // Supplemental Plane codepoint (surrogate pair)
    // eslint-disable-next-line no-bitwise
    String.fromCharCode(high >> 10 | 0xd800, high & 0x3ff | 0xdc00);
    /* eslint-enable line-comment-position */
  });
} // eslint-disable-next-line no-control-regex


const filenameReservedRegex = /[<>:"/\\|?*\x00-\x1F]/g; // eslint-disable-next-line no-control-regex

const reControlChars = /[\u0000-\u001f\u0080-\u009f]/g;
const reRelativePath = /^\.+/;

function getLocalIdent(loaderContext, localIdentName, localName, options) {
  if (!options.context) {
    // eslint-disable-next-line no-param-reassign
    options.context = loaderContext.rootContext;
  }

  const request = (0, _normalizePath.default)(_path.default.relative(options.context || '', loaderContext.resourcePath)); // eslint-disable-next-line no-param-reassign

  options.content = `${options.hashPrefix + request}+${unescape(localName)}`; // Using `[path]` placeholder outputs `/` we need escape their
  // Also directories can contains invalid characters for css we need escape their too

  return (0, _cssesc.default)(_loaderUtils.default.interpolateName(loaderContext, localIdentName, options) // For `[hash]` placeholder
  .replace(/^((-?[0-9])|--)/, '_$1').replace(filenameReservedRegex, '-').replace(reControlChars, '-').replace(reRelativePath, '-').replace(/\./g, '-'), {
    isIdentifier: true
  }).replace(/\\\[local\\\]/gi, localName);
}

function normalizeUrl(url, isStringValue) {
  let normalizedUrl = url;

  if (isStringValue && /\\[\n]/.test(normalizedUrl)) {
    normalizedUrl = normalizedUrl.replace(/\\[\n]/g, '');
  }

  return (0, _loaderUtils.urlToRequest)(decodeURIComponent(unescape(normalizedUrl)));
}

function getFilter(filter, resourcePath, defaultFilter = null) {
  return item => {
    if (defaultFilter && !defaultFilter(item)) {
      return false;
    }

    if (typeof filter === 'function') {
      return filter(item, resourcePath);
    }

    return true;
  };
}

function getModulesPlugins(options, loaderContext) {
  let modulesOptions = {
    mode: 'local',
    localIdentName: '[hash:base64]',
    getLocalIdent,
    hashPrefix: '',
    localIdentRegExp: null
  };

  if (typeof options.modules === 'boolean' || typeof options.modules === 'string') {
    modulesOptions.mode = typeof options.modules === 'string' ? options.modules : 'local';
  } else {
    modulesOptions = Object.assign({}, modulesOptions, options.modules);
  }

  return [_postcssModulesValues.default, (0, _postcssModulesLocalByDefault.default)({
    mode: modulesOptions.mode
  }), (0, _postcssModulesExtractImports.default)(), (0, _postcssModulesScope.default)({
    generateScopedName: function generateScopedName(exportName) {
      let localIdent = modulesOptions.getLocalIdent(loaderContext, modulesOptions.localIdentName, exportName, {
        context: modulesOptions.context,
        hashPrefix: modulesOptions.hashPrefix,
        regExp: modulesOptions.localIdentRegExp
      });

      if (!localIdent) {
        localIdent = getLocalIdent(loaderContext, modulesOptions.localIdentName, exportName, {
          context: modulesOptions.context,
          hashPrefix: modulesOptions.hashPrefix,
          regExp: modulesOptions.localIdentRegExp
        });
      }

      return localIdent;
    }
  })];
}

function normalizeSourceMap(map) {
  let newMap = map; // Some loader emit source map as string
  // Strip any JSON XSSI avoidance prefix from the string (as documented in the source maps specification), and then parse the string as JSON.

  if (typeof newMap === 'string') {
    newMap = JSON.parse(newMap);
  } // Source maps should use forward slash because it is URLs (https://github.com/mozilla/source-map/issues/91)
  // We should normalize path because previous loaders like `sass-loader` using backslash when generate source map


  if (newMap.file) {
    newMap.file = (0, _normalizePath.default)(newMap.file);
  }

  if (newMap.sourceRoot) {
    newMap.sourceRoot = (0, _normalizePath.default)(newMap.sourceRoot);
  }

  if (newMap.sources) {
    newMap.sources = newMap.sources.map(source => (0, _normalizePath.default)(source));
  }

  return newMap;
}

function getImportPrefix(loaderContext, importLoaders) {
  if (importLoaders === false) {
    return '';
  }

  const numberImportedLoaders = parseInt(importLoaders, 10) || 0;
  const loadersRequest = loaderContext.loaders.slice(loaderContext.loaderIndex, loaderContext.loaderIndex + 1 + numberImportedLoaders).map(x => x.request).join('!');
  return `-!${loadersRequest}!`;
}

function getImportCode(loaderContext, imports, exportType, sourceMap, importLoaders, esModule) {
  const importItems = [];
  const codeItems = [];
  const atRuleImportNames = new Map();
  const urlImportNames = new Map();
  let importPrefix;

  if (exportType === 'full') {
    importItems.push(esModule ? `import ___CSS_LOADER_API_IMPORT___ from ${(0, _loaderUtils.stringifyRequest)(loaderContext, require.resolve('./runtime/api'))};` : `var ___CSS_LOADER_API_IMPORT___ = require(${(0, _loaderUtils.stringifyRequest)(loaderContext, require.resolve('./runtime/api'))});`);
    codeItems.push(esModule ? `var exports = ___CSS_LOADER_API_IMPORT___(${sourceMap});` : `exports = ___CSS_LOADER_API_IMPORT___(${sourceMap});`);
  }

  imports.forEach(item => {
    // eslint-disable-next-line default-case
    switch (item.type) {
      case '@import':
        {
          const {
            url,
            media
          } = item;
          const preparedMedia = media ? `, ${JSON.stringify(media)}` : '';

          if (!(0, _loaderUtils.isUrlRequest)(url)) {
            codeItems.push(`exports.push([module.id, ${JSON.stringify(`@import url(${url});`)}${preparedMedia}]);`);
            return;
          }

          let importName = atRuleImportNames.get(url);

          if (!importName) {
            if (!importPrefix) {
              importPrefix = getImportPrefix(loaderContext, importLoaders);
            }

            importName = `___CSS_LOADER_AT_RULE_IMPORT_${atRuleImportNames.size}___`;
            importItems.push(esModule ? `import ${importName} from ${(0, _loaderUtils.stringifyRequest)(loaderContext, importPrefix + url)};` : `var ${importName} = require(${(0, _loaderUtils.stringifyRequest)(loaderContext, importPrefix + url)});`);
            atRuleImportNames.set(url, importName);
          }

          codeItems.push(`exports.i(${importName}${preparedMedia});`);
        }
        break;

      case 'url':
        {
          if (urlImportNames.size === 0) {
            importItems.push(esModule ? `import ___CSS_LOADER_GET_URL_IMPORT___ from ${(0, _loaderUtils.stringifyRequest)(loaderContext, require.resolve('./runtime/getUrl.js'))};` : `var ___CSS_LOADER_GET_URL_IMPORT___ = require(${(0, _loaderUtils.stringifyRequest)(loaderContext, require.resolve('./runtime/getUrl.js'))});`);
          }

          const {
            replacementName,
            url,
            hash,
            needQuotes
          } = item;
          let importName = urlImportNames.get(url);

          if (!importName) {
            importName = `___CSS_LOADER_URL_IMPORT_${urlImportNames.size}___`;
            importItems.push(esModule ? `import ${importName} from ${(0, _loaderUtils.stringifyRequest)(loaderContext, url)};` : `var ${importName} = require(${(0, _loaderUtils.stringifyRequest)(loaderContext, url)});`);
            urlImportNames.set(url, importName);
          }

          const getUrlOptions = [].concat(hash ? [`hash: ${JSON.stringify(hash)}`] : []).concat(needQuotes ? 'needQuotes: true' : []);
          const preparedOptions = getUrlOptions.length > 0 ? `, { ${getUrlOptions.join(', ')} }` : '';
          codeItems.push(`var ${replacementName} = ___CSS_LOADER_GET_URL_IMPORT___(${importName}${preparedOptions});`);
        }
        break;

      case 'icss-import':
        {
          const {
            importName,
            url,
            media
          } = item;
          const preparedMedia = media ? `, ${JSON.stringify(media)}` : ', ""';

          if (!importPrefix) {
            importPrefix = getImportPrefix(loaderContext, importLoaders);
          }

          importItems.push(esModule ? `import ${importName} from ${(0, _loaderUtils.stringifyRequest)(loaderContext, importPrefix + url)};` : `var ${importName} = require(${(0, _loaderUtils.stringifyRequest)(loaderContext, importPrefix + url)});`);

          if (exportType === 'full') {
            codeItems.push(`exports.i(${importName}${preparedMedia}, true);`);
          }
        }
        break;
    }
  });
  const items = importItems.concat(codeItems);
  return items.length > 0 ? `// Imports\n${items.join('\n')}\n` : '';
}

function getModuleCode(loaderContext, result, exportType, sourceMap, replacers) {
  if (exportType !== 'full') {
    return '';
  }

  const {
    css,
    map
  } = result;
  const sourceMapValue = sourceMap && map ? `,${map}` : '';
  let cssCode = JSON.stringify(css);
  replacers.forEach(replacer => {
    const {
      type,
      replacementName
    } = replacer;

    if (type === 'url') {
      cssCode = cssCode.replace(new RegExp(replacementName, 'g'), () => `" + ${replacementName} + "`);
    }

    if (type === 'icss-import') {
      const {
        importName,
        localName
      } = replacer;
      cssCode = cssCode.replace(new RegExp(replacementName, 'g'), () => `" + ${importName}.locals[${JSON.stringify(localName)}] + "`);
    }
  });
  return `// Module\nexports.push([module.id, ${cssCode}, ""${sourceMapValue}]);\n`;
}

function dashesCamelCase(str) {
  return str.replace(/-+(\w)/g, (match, firstLetter) => firstLetter.toUpperCase());
}

function getExportCode(loaderContext, exports, exportType, replacers, localsConvention, esModule) {
  const exportItems = [];
  let exportLocalsCode;

  if (exports.length > 0) {
    const exportLocals = [];

    const addExportedLocal = (name, value) => {
      exportLocals.push(`\t${JSON.stringify(name)}: ${JSON.stringify(value)}`);
    };

    exports.forEach(item => {
      const {
        name,
        value
      } = item;

      switch (localsConvention) {
        case 'camelCase':
          {
            addExportedLocal(name, value);
            const modifiedName = (0, _camelcase.default)(name);

            if (modifiedName !== name) {
              addExportedLocal(modifiedName, value);
            }

            break;
          }

        case 'camelCaseOnly':
          {
            addExportedLocal((0, _camelcase.default)(name), value);
            break;
          }

        case 'dashes':
          {
            addExportedLocal(name, value);
            const modifiedName = dashesCamelCase(name);

            if (modifiedName !== name) {
              addExportedLocal(modifiedName, value);
            }

            break;
          }

        case 'dashesOnly':
          {
            addExportedLocal(dashesCamelCase(name), value);
            break;
          }

        case 'asIs':
        default:
          addExportedLocal(name, value);
          break;
      }
    });
    exportLocalsCode = exportLocals.join(',\n');
    replacers.forEach(replacer => {
      if (replacer.type === 'icss-import') {
        const {
          replacementName,
          importName,
          localName
        } = replacer;
        exportLocalsCode = exportLocalsCode.replace(new RegExp(replacementName, 'g'), () => exportType === 'locals' ? `" + ${importName}[${JSON.stringify(localName)}] + "` : `" + ${importName}.locals[${JSON.stringify(localName)}] + "`);
      }
    });
  }

  if (exportType === 'locals') {
    exportItems.push(`${esModule ? 'export default' : 'module.exports ='} ${exportLocalsCode ? `{\n${exportLocalsCode}\n}` : '{}'};`);
  } else {
    if (exportLocalsCode) {
      exportItems.push(`exports.locals = {\n${exportLocalsCode}\n};`);
    }

    exportItems.push(`${esModule ? 'export default' : 'module.exports ='} exports;`);
  }

  return `// Exports\n${exportItems.join('\n')}\n`;
}