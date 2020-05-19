"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _os = _interopRequireDefault(require("os"));

var _crypto = _interopRequireDefault(require("crypto"));

var _url = _interopRequireDefault(require("url"));

var _path = _interopRequireDefault(require("path"));

var _neoAsync = _interopRequireDefault(require("neo-async"));

var _RawSource = _interopRequireDefault(require("webpack-sources/lib/RawSource"));

var _ModuleFilenameHelpers = _interopRequireDefault(require("webpack/lib/ModuleFilenameHelpers"));

var _cacache = _interopRequireDefault(require("cacache"));

var _findCacheDir = _interopRequireDefault(require("find-cache-dir"));

var _serializeJavascript = _interopRequireDefault(require("serialize-javascript"));

var _schemaUtils = _interopRequireDefault(require("schema-utils"));

var _package = _interopRequireDefault(require("../package.json"));

var _options = _interopRequireDefault(require("./options.json"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/*
MIT License http://www.opensource.org/licenses/mit-license.php
Author Tobias Koppers @sokra
*/
class CompressionPlugin {
  constructor(options = {}) {
    (0, _schemaUtils.default)(_options.default, options, {
      name: 'Compression Plugin',
      baseDataPath: 'options'
    });
    const {
      test,
      include,
      exclude,
      cache = false,
      algorithm = 'gzip',
      compressionOptions = {},
      filename = '[path].gz[query]',
      threshold = 0,
      minRatio = 0.8,
      deleteOriginalAssets = false
    } = options;
    this.options = {
      test,
      include,
      exclude,
      cache,
      algorithm,
      compressionOptions,
      filename,
      threshold,
      minRatio,
      deleteOriginalAssets
    };

    if (typeof algorithm === 'string') {
      // eslint-disable-next-line global-require
      const zlib = require('zlib');

      this.options.algorithm = zlib[this.options.algorithm];

      if (!this.options.algorithm) {
        throw new Error('Algorithm not found in zlib');
      }

      const defaultCompressionOptions = {
        level: 9
      };
      this.options.compressionOptions = Object.assign({}, defaultCompressionOptions, this.options.compressionOptions);
    }
  }

  apply(compiler) {
    compiler.hooks.emit.tapAsync({
      name: 'CompressionPlugin'
    }, (compilation, callback) => {
      const {
        cache,
        threshold,
        minRatio,
        filename,
        deleteOriginalAssets
      } = this.options;
      const cacheDir = cache === true ? (0, _findCacheDir.default)({
        name: 'compression-webpack-plugin'
      }) || _os.default.tmpdir() : cache;
      const {
        assets
      } = compilation; // eslint-disable-next-line consistent-return

      _neoAsync.default.forEach(Object.keys(assets), (file, cb) => {
        if (!_ModuleFilenameHelpers.default.matchObject(this.options, file)) {
          return cb();
        }

        const asset = assets[file];
        let input = asset.source();

        if (!Buffer.isBuffer(input)) {
          input = Buffer.from(input);
        }

        const originalSize = input.length;

        if (originalSize < threshold) {
          return cb();
        }

        return Promise.resolve().then(() => {
          if (cache) {
            const {
              outputPath
            } = compiler;
            const cacheKey = (0, _serializeJavascript.default)({
              // Invalidate cache after upgrade `zlib` module (build-in in `nodejs`)
              node: process.version,
              'compression-webpack-plugin': _package.default.version,
              'compression-webpack-plugin-options': this.options,
              path: `${outputPath ? `${outputPath}/` : ''}${file}`,
              hash: _crypto.default.createHash('md4').update(input).digest('hex')
            });
            return _cacache.default.get(cacheDir, cacheKey).then(result => result.data, () => Promise.resolve().then(() => this.compress(input)).then(data => _cacache.default.put(cacheDir, cacheKey, data).then(() => data)));
          }

          return this.compress(input);
        }).then(result => {
          if (result.length / originalSize > minRatio) {
            return cb();
          }

          const parse = _url.default.parse(file);

          const {
            pathname
          } = parse;

          const {
            dir,
            name,
            ext
          } = _path.default.parse(pathname);

          const info = {
            file,
            path: pathname,
            dir: dir ? `${dir}/` : '',
            name,
            ext,
            query: parse.query ? `?${parse.query}` : ''
          };
          const newAssetName = typeof filename === 'function' ? filename(info) : filename.replace(/\[(file|path|query|dir|name|ext)\]/g, (p0, p1) => info[p1]);
          assets[newAssetName] = new _RawSource.default(result);

          if (deleteOriginalAssets) {
            delete assets[file];
          }

          return cb();
        }).catch(error => {
          compilation.errors.push(error);
          return cb();
        });
      }, callback);
    });
  }

  compress(input) {
    return new Promise((resolve, reject) => {
      const {
        algorithm,
        compressionOptions
      } = this.options;
      algorithm(input, compressionOptions, (error, result) => {
        if (error) {
          return reject(error);
        }

        return resolve(result);
      });
    });
  }

}

var _default = CompressionPlugin;
exports.default = _default;