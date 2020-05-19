'use strict';

var path = require('path');
var url = require('url');
var CloneHelper = require('clone');
var Parser = require('../less/parser/parser');

var environment = {
  encodeBase64: function encodeBase64(str) {
    // Avoid Buffer constructor on newer versions of Node.js.
    var buffer = Buffer.from ? Buffer.from(str) : new Buffer(str);
    return buffer.toString('base64');
  },
  mimeLookup: function mimeLookup(filename) {
    return require('mime').lookup(filename);
  },
  charsetLookup: function charsetLookup(mime) {
    return require('mime').charsets.lookup(mime);
  },
  getSourceMapGenerator: function getSourceMapGenerator() {
    return require('source-map').SourceMapGenerator;
  }
};

function _classCallCheck(instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError("Cannot call a class as a function");
  }
}

function _defineProperties(target, props) {
  for (var i = 0; i < props.length; i++) {
    var descriptor = props[i];
    descriptor.enumerable = descriptor.enumerable || false;
    descriptor.configurable = true;
    if ("value" in descriptor) descriptor.writable = true;
    Object.defineProperty(target, descriptor.key, descriptor);
  }
}

function _createClass(Constructor, protoProps, staticProps) {
  if (protoProps) _defineProperties(Constructor.prototype, protoProps);
  if (staticProps) _defineProperties(Constructor, staticProps);
  return Constructor;
}

function _inherits(subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function");
  }

  subClass.prototype = Object.create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      writable: true,
      configurable: true
    }
  });
  if (superClass) _setPrototypeOf(subClass, superClass);
}

function _getPrototypeOf(o) {
  _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) {
    return o.__proto__ || Object.getPrototypeOf(o);
  };
  return _getPrototypeOf(o);
}

function _setPrototypeOf(o, p) {
  _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) {
    o.__proto__ = p;
    return o;
  };

  return _setPrototypeOf(o, p);
}

function isNativeReflectConstruct() {
  if (typeof Reflect === "undefined" || !Reflect.construct) return false;
  if (Reflect.construct.sham) return false;
  if (typeof Proxy === "function") return true;

  try {
    Date.prototype.toString.call(Reflect.construct(Date, [], function () {}));
    return true;
  } catch (e) {
    return false;
  }
}

function _construct(Parent, args, Class) {
  if (isNativeReflectConstruct()) {
    _construct = Reflect.construct;
  } else {
    _construct = function _construct(Parent, args, Class) {
      var a = [null];
      a.push.apply(a, args);
      var Constructor = Function.bind.apply(Parent, a);
      var instance = new Constructor();
      if (Class) _setPrototypeOf(instance, Class.prototype);
      return instance;
    };
  }

  return _construct.apply(null, arguments);
}

function _assertThisInitialized(self) {
  if (self === void 0) {
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  }

  return self;
}

function _possibleConstructorReturn(self, call) {
  if (call && (typeof call === "object" || typeof call === "function")) {
    return call;
  }

  return _assertThisInitialized(self);
}

function _toConsumableArray(arr) {
  return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _nonIterableSpread();
}

function _arrayWithoutHoles(arr) {
  if (Array.isArray(arr)) {
    for (var i = 0, arr2 = new Array(arr.length); i < arr.length; i++) arr2[i] = arr[i];

    return arr2;
  }
}

function _iterableToArray(iter) {
  if (Symbol.iterator in Object(iter) || Object.prototype.toString.call(iter) === "[object Arguments]") return Array.from(iter);
}

function _nonIterableSpread() {
  throw new TypeError("Invalid attempt to spread non-iterable instance");
}

var fs;

try {
  fs = require('graceful-fs');
} catch (e) {
  fs = require('fs');
}

var fs$1 = fs;

var AbstractFileManager =
/*#__PURE__*/
function () {
  function AbstractFileManager() {
    _classCallCheck(this, AbstractFileManager);
  }

  _createClass(AbstractFileManager, [{
    key: "getPath",
    value: function getPath(filename) {
      var j = filename.lastIndexOf('?');

      if (j > 0) {
        filename = filename.slice(0, j);
      }

      j = filename.lastIndexOf('/');

      if (j < 0) {
        j = filename.lastIndexOf('\\');
      }

      if (j < 0) {
        return '';
      }

      return filename.slice(0, j + 1);
    }
  }, {
    key: "tryAppendExtension",
    value: function tryAppendExtension(path, ext) {
      return /(\.[a-z]*$)|([\?;].*)$/.test(path) ? path : path + ext;
    }
  }, {
    key: "tryAppendLessExtension",
    value: function tryAppendLessExtension(path) {
      return this.tryAppendExtension(path, '.less');
    }
  }, {
    key: "supportsSync",
    value: function supportsSync() {
      return false;
    }
  }, {
    key: "alwaysMakePathsAbsolute",
    value: function alwaysMakePathsAbsolute() {
      return false;
    }
  }, {
    key: "isPathAbsolute",
    value: function isPathAbsolute(filename) {
      return /^(?:[a-z-]+:|\/|\\|#)/i.test(filename);
    } // TODO: pull out / replace?

  }, {
    key: "join",
    value: function join(basePath, laterPath) {
      if (!basePath) {
        return laterPath;
      }

      return basePath + laterPath;
    }
  }, {
    key: "pathDiff",
    value: function pathDiff(url, baseUrl) {
      // diff between two paths to create a relative path
      var urlParts = this.extractUrlParts(url);
      var baseUrlParts = this.extractUrlParts(baseUrl);
      var i;
      var max;
      var urlDirectories;
      var baseUrlDirectories;
      var diff = '';

      if (urlParts.hostPart !== baseUrlParts.hostPart) {
        return '';
      }

      max = Math.max(baseUrlParts.directories.length, urlParts.directories.length);

      for (i = 0; i < max; i++) {
        if (baseUrlParts.directories[i] !== urlParts.directories[i]) {
          break;
        }
      }

      baseUrlDirectories = baseUrlParts.directories.slice(i);
      urlDirectories = urlParts.directories.slice(i);

      for (i = 0; i < baseUrlDirectories.length - 1; i++) {
        diff += '../';
      }

      for (i = 0; i < urlDirectories.length - 1; i++) {
        diff += `${urlDirectories[i]}/`;
      }

      return diff;
    }
  }, {
    key: "extractUrlParts",
    // helper function, not part of API
    value: function extractUrlParts(url, baseUrl) {
      // urlParts[1] = protocol://hostname/ OR /
      // urlParts[2] = / if path relative to host base
      // urlParts[3] = directories
      // urlParts[4] = filename
      // urlParts[5] = parameters
      var urlPartsRegex = /^((?:[a-z-]+:)?\/{2}(?:[^\/\?#]*\/)|([\/\\]))?((?:[^\/\\\?#]*[\/\\])*)([^\/\\\?#]*)([#\?].*)?$/i;
      var urlParts = url.match(urlPartsRegex);
      var returner = {};
      var rawDirectories = [];
      var directories = [];
      var i;
      var baseUrlParts;

      if (!urlParts) {
        throw new Error(`Could not parse sheet href - '${url}'`);
      } // Stylesheets in IE don't always return the full path


      if (baseUrl && (!urlParts[1] || urlParts[2])) {
        baseUrlParts = baseUrl.match(urlPartsRegex);

        if (!baseUrlParts) {
          throw new Error(`Could not parse page url - '${baseUrl}'`);
        }

        urlParts[1] = urlParts[1] || baseUrlParts[1] || '';

        if (!urlParts[2]) {
          urlParts[3] = baseUrlParts[3] + urlParts[3];
        }
      }

      if (urlParts[3]) {
        rawDirectories = urlParts[3].replace(/\\/g, '/').split('/'); // collapse '..' and skip '.'

        for (i = 0; i < rawDirectories.length; i++) {
          if (rawDirectories[i] === '..') {
            directories.pop();
          } else if (rawDirectories[i] !== '.') {
            directories.push(rawDirectories[i]);
          }
        }
      }

      returner.hostPart = urlParts[1];
      returner.directories = directories;
      returner.rawPath = (urlParts[1] || '') + rawDirectories.join('/');
      returner.path = (urlParts[1] || '') + directories.join('/');
      returner.filename = urlParts[4];
      returner.fileUrl = returner.path + (urlParts[4] || '');
      returner.url = returner.fileUrl + (urlParts[5] || '');
      return returner;
    }
  }]);

  return AbstractFileManager;
}();

var FileManager =
/*#__PURE__*/
function (_AbstractFileManager) {
  _inherits(FileManager, _AbstractFileManager);

  function FileManager() {
    var _this;

    _classCallCheck(this, FileManager);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(FileManager).call(this));
    _this.contents = {};
    return _this;
  }

  _createClass(FileManager, [{
    key: "supports",
    value: function supports(filename, currentDirectory, options, environment) {
      return true;
    }
  }, {
    key: "supportsSync",
    value: function supportsSync(filename, currentDirectory, options, environment) {
      return true;
    }
  }, {
    key: "loadFile",
    value: function loadFile(filename, currentDirectory, options, environment, callback) {
      var fullFilename;
      var isAbsoluteFilename = this.isPathAbsolute(filename);
      var filenamesTried = [];
      var self = this;
      var prefix = filename.slice(0, 1);
      var explicit = prefix === '.' || prefix === '/';
      var result = null;
      var isNodeModule = false;
      var npmPrefix = 'npm://';
      options = options || {};
      var paths = isAbsoluteFilename ? [''] : [currentDirectory];

      if (options.paths) {
        paths.push.apply(paths, _toConsumableArray(options.paths));
      }

      if (!isAbsoluteFilename && paths.indexOf('.') === -1) {
        paths.push('.');
      }

      var prefixes = options.prefixes || [''];
      var fileParts = this.extractUrlParts(filename);

      if (options.syncImport) {
        getFileData(returnData, returnData);

        if (callback) {
          callback(result.error, result);
        } else {
          return result;
        }
      } else {
        // promise is guaranteed to be asyncronous
        // which helps as it allows the file handle
        // to be closed before it continues with the next file
        return new Promise(getFileData);
      }

      function returnData(data) {
        if (!data.filename) {
          result = {
            error: data
          };
        } else {
          result = data;
        }
      }

      function getFileData(fulfill, reject) {
        (function tryPathIndex(i) {
          if (i < paths.length) {
            (function tryPrefix(j) {
              if (j < prefixes.length) {
                isNodeModule = false;
                fullFilename = fileParts.rawPath + prefixes[j] + fileParts.filename;

                if (paths[i]) {
                  fullFilename = path.join(paths[i], fullFilename);
                }

                if (!explicit && paths[i] === '.') {
                  try {
                    fullFilename = require.resolve(fullFilename);
                    isNodeModule = true;
                  } catch (e) {
                    filenamesTried.push(npmPrefix + fullFilename);
                    tryWithExtension();
                  }
                } else {
                  tryWithExtension();
                }

                function tryWithExtension() {
                  var extFilename = options.ext ? self.tryAppendExtension(fullFilename, options.ext) : fullFilename;

                  if (extFilename !== fullFilename && !explicit && paths[i] === '.') {
                    try {
                      fullFilename = require.resolve(extFilename);
                      isNodeModule = true;
                    } catch (e) {
                      filenamesTried.push(npmPrefix + extFilename);
                      fullFilename = extFilename;
                    }
                  } else {
                    fullFilename = extFilename;
                  }
                }

                var modified = false;

                if (self.contents[fullFilename]) {
                  try {
                    var stat = fs$1.statSync.apply(this, [fullFilename]);

                    if (stat.mtime.getTime() === self.contents[fullFilename].mtime.getTime()) {
                      fulfill({
                        contents: self.contents[fullFilename].data,
                        filename: fullFilename
                      });
                    } else {
                      modified = true;
                    }
                  } catch (e) {
                    modified = true;
                  }
                }

                if (modified || !self.contents[fullFilename]) {
                  var readFileArgs = [fullFilename];

                  if (!options.rawBuffer) {
                    readFileArgs.push('utf-8');
                  }

                  if (options.syncImport) {
                    try {
                      var data = fs$1.readFileSync.apply(this, readFileArgs);
                      var stat = fs$1.statSync.apply(this, [fullFilename]);
                      self.contents[fullFilename] = {
                        data,
                        mtime: stat.mtime
                      };
                      fulfill({
                        contents: data,
                        filename: fullFilename
                      });
                    } catch (e) {
                      filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                      return tryPrefix(j + 1);
                    }
                  } else {
                    readFileArgs.push(function (e, data) {
                      if (e) {
                        filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                        return tryPrefix(j + 1);
                      }

                      var stat = fs$1.statSync.apply(this, [fullFilename]);
                      self.contents[fullFilename] = {
                        data,
                        mtime: stat.mtime
                      };
                      fulfill({
                        contents: data,
                        filename: fullFilename
                      });
                    });
                    fs$1.readFile.apply(this, readFileArgs);
                  }
                }
              } else {
                tryPathIndex(i + 1);
              }
            })(0);
          } else {
            reject({
              type: 'File',
              message: `'${filename}' wasn't found. Tried - ${filenamesTried.join(',')}`
            });
          }
        })(0);
      }
    }
  }, {
    key: "loadFileSync",
    value: function loadFileSync(filename, currentDirectory, options, environment) {
      options.syncImport = true;
      return this.loadFile(filename, currentDirectory, options, environment);
    }
  }]);

  return FileManager;
}(AbstractFileManager);

var logger = {
  error: function error(msg) {
    this._fireEvent('error', msg);
  },
  warn: function warn(msg) {
    this._fireEvent('warn', msg);
  },
  info: function info(msg) {
    this._fireEvent('info', msg);
  },
  debug: function debug(msg) {
    this._fireEvent('debug', msg);
  },
  addListener: function addListener(listener) {
    this._listeners.push(listener);
  },
  removeListener: function removeListener(listener) {
    for (var i = 0; i < this._listeners.length; i++) {
      if (this._listeners[i] === listener) {
        this._listeners.splice(i, 1);

        return;
      }
    }
  },
  _fireEvent: function _fireEvent(type, msg) {
    for (var i = 0; i < this._listeners.length; i++) {
      var logFunction = this._listeners[i][type];

      if (logFunction) {
        logFunction(msg);
      }
    }
  },
  _listeners: []
};

var isUrlRe = /^(?:https?:)?\/\//i;
var request;

var UrlFileManager =
/*#__PURE__*/
function (_AbstractFileManager) {
  _inherits(UrlFileManager, _AbstractFileManager);

  function UrlFileManager() {
    _classCallCheck(this, UrlFileManager);

    return _possibleConstructorReturn(this, _getPrototypeOf(UrlFileManager).apply(this, arguments));
  }

  _createClass(UrlFileManager, [{
    key: "supports",
    value: function supports(filename, currentDirectory, options, environment) {
      return isUrlRe.test(filename) || isUrlRe.test(currentDirectory);
    }
  }, {
    key: "loadFile",
    value: function loadFile(filename, currentDirectory, options, environment) {
      return new Promise(function (fulfill, reject) {
        if (request === undefined) {
          try {
            request = require('request');
          } catch (e) {
            request = null;
          }
        }

        if (!request) {
          reject({
            type: 'File',
            message: 'optional dependency \'request\' required to import over http(s)\n'
          });
          return;
        }

        var urlStr = isUrlRe.test(filename) ? filename : url.resolve(currentDirectory, filename);
        var urlObj = url.parse(urlStr);

        if (!urlObj.protocol) {
          urlObj.protocol = 'http';
          urlStr = urlObj.format();
        }

        request.get({
          uri: urlStr,
          strictSSL: !options.insecure
        }, function (error, res, body) {
          if (error) {
            reject({
              type: 'File',
              message: `resource '${urlStr}' gave this Error:\n  ${error}\n`
            });
            return;
          }

          if (res && res.statusCode === 404) {
            reject({
              type: 'File',
              message: `resource '${urlStr}' was not found\n`
            });
            return;
          }

          if (!body) {
            logger.warn(`Warning: Empty body (HTTP ${res.statusCode}) returned by "${urlStr}"`);
          }

          fulfill({
            contents: body,
            filename: urlStr
          });
        });
      });
    }
  }]);

  return UrlFileManager;
}(AbstractFileManager);

var colors = {
  'aliceblue': '#f0f8ff',
  'antiquewhite': '#faebd7',
  'aqua': '#00ffff',
  'aquamarine': '#7fffd4',
  'azure': '#f0ffff',
  'beige': '#f5f5dc',
  'bisque': '#ffe4c4',
  'black': '#000000',
  'blanchedalmond': '#ffebcd',
  'blue': '#0000ff',
  'blueviolet': '#8a2be2',
  'brown': '#a52a2a',
  'burlywood': '#deb887',
  'cadetblue': '#5f9ea0',
  'chartreuse': '#7fff00',
  'chocolate': '#d2691e',
  'coral': '#ff7f50',
  'cornflowerblue': '#6495ed',
  'cornsilk': '#fff8dc',
  'crimson': '#dc143c',
  'cyan': '#00ffff',
  'darkblue': '#00008b',
  'darkcyan': '#008b8b',
  'darkgoldenrod': '#b8860b',
  'darkgray': '#a9a9a9',
  'darkgrey': '#a9a9a9',
  'darkgreen': '#006400',
  'darkkhaki': '#bdb76b',
  'darkmagenta': '#8b008b',
  'darkolivegreen': '#556b2f',
  'darkorange': '#ff8c00',
  'darkorchid': '#9932cc',
  'darkred': '#8b0000',
  'darksalmon': '#e9967a',
  'darkseagreen': '#8fbc8f',
  'darkslateblue': '#483d8b',
  'darkslategray': '#2f4f4f',
  'darkslategrey': '#2f4f4f',
  'darkturquoise': '#00ced1',
  'darkviolet': '#9400d3',
  'deeppink': '#ff1493',
  'deepskyblue': '#00bfff',
  'dimgray': '#696969',
  'dimgrey': '#696969',
  'dodgerblue': '#1e90ff',
  'firebrick': '#b22222',
  'floralwhite': '#fffaf0',
  'forestgreen': '#228b22',
  'fuchsia': '#ff00ff',
  'gainsboro': '#dcdcdc',
  'ghostwhite': '#f8f8ff',
  'gold': '#ffd700',
  'goldenrod': '#daa520',
  'gray': '#808080',
  'grey': '#808080',
  'green': '#008000',
  'greenyellow': '#adff2f',
  'honeydew': '#f0fff0',
  'hotpink': '#ff69b4',
  'indianred': '#cd5c5c',
  'indigo': '#4b0082',
  'ivory': '#fffff0',
  'khaki': '#f0e68c',
  'lavender': '#e6e6fa',
  'lavenderblush': '#fff0f5',
  'lawngreen': '#7cfc00',
  'lemonchiffon': '#fffacd',
  'lightblue': '#add8e6',
  'lightcoral': '#f08080',
  'lightcyan': '#e0ffff',
  'lightgoldenrodyellow': '#fafad2',
  'lightgray': '#d3d3d3',
  'lightgrey': '#d3d3d3',
  'lightgreen': '#90ee90',
  'lightpink': '#ffb6c1',
  'lightsalmon': '#ffa07a',
  'lightseagreen': '#20b2aa',
  'lightskyblue': '#87cefa',
  'lightslategray': '#778899',
  'lightslategrey': '#778899',
  'lightsteelblue': '#b0c4de',
  'lightyellow': '#ffffe0',
  'lime': '#00ff00',
  'limegreen': '#32cd32',
  'linen': '#faf0e6',
  'magenta': '#ff00ff',
  'maroon': '#800000',
  'mediumaquamarine': '#66cdaa',
  'mediumblue': '#0000cd',
  'mediumorchid': '#ba55d3',
  'mediumpurple': '#9370d8',
  'mediumseagreen': '#3cb371',
  'mediumslateblue': '#7b68ee',
  'mediumspringgreen': '#00fa9a',
  'mediumturquoise': '#48d1cc',
  'mediumvioletred': '#c71585',
  'midnightblue': '#191970',
  'mintcream': '#f5fffa',
  'mistyrose': '#ffe4e1',
  'moccasin': '#ffe4b5',
  'navajowhite': '#ffdead',
  'navy': '#000080',
  'oldlace': '#fdf5e6',
  'olive': '#808000',
  'olivedrab': '#6b8e23',
  'orange': '#ffa500',
  'orangered': '#ff4500',
  'orchid': '#da70d6',
  'palegoldenrod': '#eee8aa',
  'palegreen': '#98fb98',
  'paleturquoise': '#afeeee',
  'palevioletred': '#d87093',
  'papayawhip': '#ffefd5',
  'peachpuff': '#ffdab9',
  'peru': '#cd853f',
  'pink': '#ffc0cb',
  'plum': '#dda0dd',
  'powderblue': '#b0e0e6',
  'purple': '#800080',
  'rebeccapurple': '#663399',
  'red': '#ff0000',
  'rosybrown': '#bc8f8f',
  'royalblue': '#4169e1',
  'saddlebrown': '#8b4513',
  'salmon': '#fa8072',
  'sandybrown': '#f4a460',
  'seagreen': '#2e8b57',
  'seashell': '#fff5ee',
  'sienna': '#a0522d',
  'silver': '#c0c0c0',
  'skyblue': '#87ceeb',
  'slateblue': '#6a5acd',
  'slategray': '#708090',
  'slategrey': '#708090',
  'snow': '#fffafa',
  'springgreen': '#00ff7f',
  'steelblue': '#4682b4',
  'tan': '#d2b48c',
  'teal': '#008080',
  'thistle': '#d8bfd8',
  'tomato': '#ff6347',
  'turquoise': '#40e0d0',
  'violet': '#ee82ee',
  'wheat': '#f5deb3',
  'white': '#ffffff',
  'whitesmoke': '#f5f5f5',
  'yellow': '#ffff00',
  'yellowgreen': '#9acd32'
};

var unitConversions = {
  length: {
    'm': 1,
    'cm': 0.01,
    'mm': 0.001,
    'in': 0.0254,
    'px': 0.0254 / 96,
    'pt': 0.0254 / 72,
    'pc': 0.0254 / 72 * 12
  },
  duration: {
    's': 1,
    'ms': 0.001
  },
  angle: {
    'rad': 1 / (2 * Math.PI),
    'deg': 1 / 360,
    'grad': 1 / 400,
    'turn': 1
  }
};

var data = {
  colors,
  unitConversions
};

var Node =
/*#__PURE__*/
function () {
  function Node() {
    _classCallCheck(this, Node);

    this.parent = null;
    this.visibilityBlocks = undefined;
    this.nodeVisible = undefined;
    this.rootNode = null;
    this.parsed = null;
    var self = this;
    Object.defineProperty(this, 'currentFileInfo', {
      get: function get() {
        return self.fileInfo();
      }
    });
    Object.defineProperty(this, 'index', {
      get: function get() {
        return self.getIndex();
      }
    });
  }

  _createClass(Node, [{
    key: "setParent",
    value: function setParent(nodes, parent) {
      function set(node) {
        if (node && node instanceof Node) {
          node.parent = parent;
        }
      }

      if (Array.isArray(nodes)) {
        nodes.forEach(set);
      } else {
        set(nodes);
      }
    }
  }, {
    key: "getIndex",
    value: function getIndex() {
      return this._index || this.parent && this.parent.getIndex() || 0;
    }
  }, {
    key: "fileInfo",
    value: function fileInfo() {
      return this._fileInfo || this.parent && this.parent.fileInfo() || {};
    }
  }, {
    key: "isRulesetLike",
    value: function isRulesetLike() {
      return false;
    }
  }, {
    key: "toCSS",
    value: function toCSS(context) {
      var strs = [];
      this.genCSS(context, {
        add: function add(chunk, fileInfo, index) {
          strs.push(chunk);
        },
        isEmpty: function isEmpty() {
          return strs.length === 0;
        }
      });
      return strs.join('');
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(this.value);
    }
  }, {
    key: "accept",
    value: function accept(visitor) {
      this.value = visitor.visit(this.value);
    }
  }, {
    key: "eval",
    value: function _eval() {
      return this;
    }
  }, {
    key: "_operate",
    value: function _operate(context, op, a, b) {
      switch (op) {
        case '+':
          return a + b;

        case '-':
          return a - b;

        case '*':
          return a * b;

        case '/':
          return a / b;
      }
    }
  }, {
    key: "fround",
    value: function fround(context, value) {
      var precision = context && context.numPrecision; // add "epsilon" to ensure numbers like 1.000000005 (represented as 1.000000004999...) are properly rounded:

      return precision ? Number((value + 2e-16).toFixed(precision)) : value;
    } // Returns true if this node represents root of ast imported by reference

  }, {
    key: "blocksVisibility",
    value: function blocksVisibility() {
      if (this.visibilityBlocks == null) {
        this.visibilityBlocks = 0;
      }

      return this.visibilityBlocks !== 0;
    }
  }, {
    key: "addVisibilityBlock",
    value: function addVisibilityBlock() {
      if (this.visibilityBlocks == null) {
        this.visibilityBlocks = 0;
      }

      this.visibilityBlocks = this.visibilityBlocks + 1;
    }
  }, {
    key: "removeVisibilityBlock",
    value: function removeVisibilityBlock() {
      if (this.visibilityBlocks == null) {
        this.visibilityBlocks = 0;
      }

      this.visibilityBlocks = this.visibilityBlocks - 1;
    } // Turns on node visibility - if called node will be shown in output regardless
    // of whether it comes from import by reference or not

  }, {
    key: "ensureVisibility",
    value: function ensureVisibility() {
      this.nodeVisible = true;
    } // Turns off node visibility - if called node will NOT be shown in output regardless
    // of whether it comes from import by reference or not

  }, {
    key: "ensureInvisibility",
    value: function ensureInvisibility() {
      this.nodeVisible = false;
    } // return values:
    // false - the node must not be visible
    // true - the node must be visible
    // undefined or null - the node has the same visibility as its parent

  }, {
    key: "isVisible",
    value: function isVisible() {
      return this.nodeVisible;
    }
  }, {
    key: "visibilityInfo",
    value: function visibilityInfo() {
      return {
        visibilityBlocks: this.visibilityBlocks,
        nodeVisible: this.nodeVisible
      };
    }
  }, {
    key: "copyVisibilityInfo",
    value: function copyVisibilityInfo(info) {
      if (!info) {
        return;
      }

      this.visibilityBlocks = info.visibilityBlocks;
      this.nodeVisible = info.nodeVisible;
    }
  }]);

  return Node;
}();

Node.compare = function (a, b) {
  /* returns:
   -1: a < b
   0: a = b
   1: a > b
   and *any* other value for a != b (e.g. undefined, NaN, -2 etc.) */
  if (a.compare && // for "symmetric results" force toCSS-based comparison
  // of Quoted or Anonymous if either value is one of those
  !(b.type === 'Quoted' || b.type === 'Anonymous')) {
    return a.compare(b);
  } else if (b.compare) {
    return -b.compare(a);
  } else if (a.type !== b.type) {
    return undefined;
  }

  a = a.value;
  b = b.value;

  if (!Array.isArray(a)) {
    return a === b ? 0 : undefined;
  }

  if (a.length !== b.length) {
    return undefined;
  }

  for (var i = 0; i < a.length; i++) {
    if (Node.compare(a[i], b[i]) !== 0) {
      return undefined;
    }
  }

  return 0;
};

Node.numericCompare = function (a, b) {
  return a < b ? -1 : a === b ? 0 : a > b ? 1 : undefined;
};

// RGB Colors - #ff0014, #eee
//

var Color =
/*#__PURE__*/
function (_Node) {
  _inherits(Color, _Node);

  function Color(rgb, a, originalForm) {
    var _this;

    _classCallCheck(this, Color);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Color).call(this));

    var self = _assertThisInitialized(_this); //
    // The end goal here, is to parse the arguments
    // into an integer triplet, such as `128, 255, 0`
    //
    // This facilitates operations and conversions.
    //


    if (Array.isArray(rgb)) {
      _this.rgb = rgb;
    } else if (rgb.length >= 6) {
      _this.rgb = [];
      rgb.match(/.{2}/g).map(function (c, i) {
        if (i < 3) {
          self.rgb.push(parseInt(c, 16));
        } else {
          self.alpha = parseInt(c, 16) / 255;
        }
      });
    } else {
      _this.rgb = [];
      rgb.split('').map(function (c, i) {
        if (i < 3) {
          self.rgb.push(parseInt(c + c, 16));
        } else {
          self.alpha = parseInt(c + c, 16) / 255;
        }
      });
    }

    _this.alpha = _this.alpha || (typeof a === 'number' ? a : 1);

    if (typeof originalForm !== 'undefined') {
      _this.value = originalForm;
    }

    return _this;
  }

  _createClass(Color, [{
    key: "luma",
    value: function luma() {
      var r = this.rgb[0] / 255;
      var g = this.rgb[1] / 255;
      var b = this.rgb[2] / 255;
      r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
      g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
      b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
      return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(this.toCSS(context));
    }
  }, {
    key: "toCSS",
    value: function toCSS(context, doNotCompress) {
      var compress = context && context.compress && !doNotCompress;
      var color;
      var alpha;
      var colorFunction;
      var args = []; // `value` is set if this color was originally
      // converted from a named color string so we need
      // to respect this and try to output named color too.

      alpha = this.fround(context, this.alpha);

      if (this.value) {
        if (this.value.indexOf('rgb') === 0) {
          if (alpha < 1) {
            colorFunction = 'rgba';
          }
        } else if (this.value.indexOf('hsl') === 0) {
          if (alpha < 1) {
            colorFunction = 'hsla';
          } else {
            colorFunction = 'hsl';
          }
        } else {
          return this.value;
        }
      } else {
        if (alpha < 1) {
          colorFunction = 'rgba';
        }
      }

      switch (colorFunction) {
        case 'rgba':
          args = this.rgb.map(function (c) {
            return clamp(Math.round(c), 255);
          }).concat(clamp(alpha, 1));
          break;

        case 'hsla':
          args.push(clamp(alpha, 1));

        case 'hsl':
          color = this.toHSL();
          args = [this.fround(context, color.h), `${this.fround(context, color.s * 100)}%`, `${this.fround(context, color.l * 100)}%`].concat(args);
      }

      if (colorFunction) {
        // Values are capped between `0` and `255`, rounded and zero-padded.
        return `${colorFunction}(${args.join(`,${compress ? '' : ' '}`)})`;
      }

      color = this.toRGB();

      if (compress) {
        var splitcolor = color.split(''); // Convert color to short format

        if (splitcolor[1] === splitcolor[2] && splitcolor[3] === splitcolor[4] && splitcolor[5] === splitcolor[6]) {
          color = `#${splitcolor[1]}${splitcolor[3]}${splitcolor[5]}`;
        }
      }

      return color;
    } //
    // Operations have to be done per-channel, if not,
    // channels will spill onto each other. Once we have
    // our result, in the form of an integer triplet,
    // we create a new Color node to hold the result.
    //

  }, {
    key: "operate",
    value: function operate(context, op, other) {
      var rgb = new Array(3);
      var alpha = this.alpha * (1 - other.alpha) + other.alpha;

      for (var c = 0; c < 3; c++) {
        rgb[c] = this._operate(context, op, this.rgb[c], other.rgb[c]);
      }

      return new Color(rgb, alpha);
    }
  }, {
    key: "toRGB",
    value: function toRGB() {
      return toHex(this.rgb);
    }
  }, {
    key: "toHSL",
    value: function toHSL() {
      var r = this.rgb[0] / 255;
      var g = this.rgb[1] / 255;
      var b = this.rgb[2] / 255;
      var a = this.alpha;
      var max = Math.max(r, g, b);
      var min = Math.min(r, g, b);
      var h;
      var s;
      var l = (max + min) / 2;
      var d = max - min;

      if (max === min) {
        h = s = 0;
      } else {
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

        switch (max) {
          case r:
            h = (g - b) / d + (g < b ? 6 : 0);
            break;

          case g:
            h = (b - r) / d + 2;
            break;

          case b:
            h = (r - g) / d + 4;
            break;
        }

        h /= 6;
      }

      return {
        h: h * 360,
        s,
        l,
        a
      };
    } // Adapted from http://mjijackson.com/2008/02/rgb-to-hsl-and-rgb-to-hsv-color-model-conversion-algorithms-in-javascript

  }, {
    key: "toHSV",
    value: function toHSV() {
      var r = this.rgb[0] / 255;
      var g = this.rgb[1] / 255;
      var b = this.rgb[2] / 255;
      var a = this.alpha;
      var max = Math.max(r, g, b);
      var min = Math.min(r, g, b);
      var h;
      var s;
      var v = max;
      var d = max - min;

      if (max === 0) {
        s = 0;
      } else {
        s = d / max;
      }

      if (max === min) {
        h = 0;
      } else {
        switch (max) {
          case r:
            h = (g - b) / d + (g < b ? 6 : 0);
            break;

          case g:
            h = (b - r) / d + 2;
            break;

          case b:
            h = (r - g) / d + 4;
            break;
        }

        h /= 6;
      }

      return {
        h: h * 360,
        s,
        v,
        a
      };
    }
  }, {
    key: "toARGB",
    value: function toARGB() {
      return toHex([this.alpha * 255].concat(this.rgb));
    }
  }, {
    key: "compare",
    value: function compare(x) {
      return x.rgb && x.rgb[0] === this.rgb[0] && x.rgb[1] === this.rgb[1] && x.rgb[2] === this.rgb[2] && x.alpha === this.alpha ? 0 : undefined;
    }
  }]);

  return Color;
}(Node);

Color.prototype.type = 'Color';

function clamp(v, max) {
  return Math.min(Math.max(v, 0), max);
}

function toHex(v) {
  return `#${v.map(function (c) {
    c = clamp(Math.round(c), 255);
    return (c < 16 ? '0' : '') + c.toString(16);
  }).join('')}`;
}

Color.fromKeyword = function (keyword) {
  var c;
  var key = keyword.toLowerCase();

  if (colors.hasOwnProperty(key)) {
    c = new Color(colors[key].slice(1));
  } else if (key === 'transparent') {
    c = new Color([0, 0, 0], 0);
  }

  if (c) {
    c.value = keyword;
    return c;
  }
};

var Paren =
/*#__PURE__*/
function (_Node) {
  _inherits(Paren, _Node);

  function Paren(node) {
    var _this;

    _classCallCheck(this, Paren);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Paren).call(this));
    _this.value = node;
    return _this;
  }

  _createClass(Paren, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add('(');
      this.value.genCSS(context, output);
      output.add(')');
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      return new Paren(this.value.eval(context));
    }
  }]);

  return Paren;
}(Node);

Paren.prototype.type = 'Paren';

var _noSpaceCombinators = {
  '': true,
  ' ': true,
  '|': true
};

var Combinator =
/*#__PURE__*/
function (_Node) {
  _inherits(Combinator, _Node);

  function Combinator(value) {
    var _this;

    _classCallCheck(this, Combinator);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Combinator).call(this));

    if (value === ' ') {
      _this.value = ' ';
      _this.emptyOrWhitespace = true;
    } else {
      _this.value = value ? value.trim() : '';
      _this.emptyOrWhitespace = _this.value === '';
    }

    return _this;
  }

  _createClass(Combinator, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      var spaceOrEmpty = context.compress || _noSpaceCombinators[this.value] ? '' : ' ';
      output.add(spaceOrEmpty + this.value + spaceOrEmpty);
    }
  }]);

  return Combinator;
}(Node);

Combinator.prototype.type = 'Combinator';

var Element =
/*#__PURE__*/
function (_Node) {
  _inherits(Element, _Node);

  function Element(combinator, value, isVariable, index, currentFileInfo, visibilityInfo) {
    var _this;

    _classCallCheck(this, Element);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Element).call(this));
    _this.combinator = combinator instanceof Combinator ? combinator : new Combinator(combinator);

    if (typeof value === 'string') {
      _this.value = value.trim();
    } else if (value) {
      _this.value = value;
    } else {
      _this.value = '';
    }

    _this.isVariable = isVariable;
    _this._index = index;
    _this._fileInfo = currentFileInfo;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.setParent(_this.combinator, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Element, [{
    key: "accept",
    value: function accept(visitor) {
      var value = this.value;
      this.combinator = visitor.visit(this.combinator);

      if (typeof value === 'object') {
        this.value = visitor.visit(value);
      }
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      return new Element(this.combinator, this.value.eval ? this.value.eval(context) : this.value, this.isVariable, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    }
  }, {
    key: "clone",
    value: function clone() {
      return new Element(this.combinator, this.value, this.isVariable, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(this.toCSS(context), this.fileInfo(), this.getIndex());
    }
  }, {
    key: "toCSS",
    value: function toCSS() {
      var context = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
      var value = this.value;
      var firstSelector = context.firstSelector;

      if (value instanceof Paren) {
        // selector in parens should not be affected by outer selector
        // flags (breaks only interpolated selectors - see #1973)
        context.firstSelector = true;
      }

      value = value.toCSS ? value.toCSS(context) : value;
      context.firstSelector = firstSelector;

      if (value === '' && this.combinator.value.charAt(0) === '&') {
        return '';
      } else {
        return this.combinator.toCSS(context) + value;
      }
    }
  }]);

  return Element;
}(Node);

Element.prototype.type = 'Element';

var Math$1 = {
  ALWAYS: 0,
  PARENS_DIVISION: 1,
  PARENS: 2,
  STRICT_LEGACY: 3
};
var RewriteUrls = {
  OFF: 0,
  LOCAL: 1,
  ALL: 2
};

/* jshint proto: true */
function getLocation(index, inputStream) {
  var n = index + 1;
  var line = null;
  var column = -1;

  while (--n >= 0 && inputStream.charAt(n) !== '\n') {
    column++;
  }

  if (typeof index === 'number') {
    line = (inputStream.slice(0, index).match(/\n/g) || '').length;
  }

  return {
    line,
    column
  };
}
function copyArray(arr) {
  var i;
  var length = arr.length;
  var copy = new Array(length);

  for (i = 0; i < length; i++) {
    copy[i] = arr[i];
  }

  return copy;
}
function clone(obj) {
  var cloned = {};

  for (var prop in obj) {
    if (obj.hasOwnProperty(prop)) {
      cloned[prop] = obj[prop];
    }
  }

  return cloned;
}
function defaults(obj1, obj2) {
  var newObj = obj2 || {};

  if (!obj2._defaults) {
    newObj = {};

    var _defaults = CloneHelper(obj1);

    newObj._defaults = _defaults;
    var cloned = obj2 ? CloneHelper(obj2) : {};
    Object.assign(newObj, _defaults, cloned);
  }

  return newObj;
}
function copyOptions(obj1, obj2) {
  if (obj2 && obj2._defaults) {
    return obj2;
  }

  var opts = defaults(obj1, obj2);

  if (opts.strictMath) {
    opts.math = Math$1.STRICT_LEGACY;
  } // Back compat with changed relativeUrls option


  if (opts.relativeUrls) {
    opts.rewriteUrls = RewriteUrls.ALL;
  }

  if (typeof opts.math === 'string') {
    switch (opts.math.toLowerCase()) {
      case 'always':
        opts.math = Math$1.ALWAYS;
        break;

      case 'parens-division':
        opts.math = Math$1.PARENS_DIVISION;
        break;

      case 'strict':
      case 'parens':
        opts.math = Math$1.PARENS;
        break;

      case 'strict-legacy':
        opts.math = Math$1.STRICT_LEGACY;
    }
  }

  if (typeof opts.rewriteUrls === 'string') {
    switch (opts.rewriteUrls.toLowerCase()) {
      case 'off':
        opts.rewriteUrls = RewriteUrls.OFF;
        break;

      case 'local':
        opts.rewriteUrls = RewriteUrls.LOCAL;
        break;

      case 'all':
        opts.rewriteUrls = RewriteUrls.ALL;
        break;
    }
  }

  return opts;
}
function merge(obj1, obj2) {
  for (var prop in obj2) {
    if (obj2.hasOwnProperty(prop)) {
      obj1[prop] = obj2[prop];
    }
  }

  return obj1;
}
function flattenArray(arr) {
  var result = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];

  for (var i = 0, length = arr.length; i < length; i++) {
    var value = arr[i];

    if (Array.isArray(value)) {
      flattenArray(value, result);
    } else {
      if (value !== undefined) {
        result.push(value);
      }
    }
  }

  return result;
}

var utils = /*#__PURE__*/Object.freeze({
    getLocation: getLocation,
    copyArray: copyArray,
    clone: clone,
    defaults: defaults,
    copyOptions: copyOptions,
    merge: merge,
    flattenArray: flattenArray
});

/**
 * This is a centralized class of any error that could be thrown internally (mostly by the parser).
 * Besides standard .message it keeps some additional data like a path to the file where the error
 * occurred along with line and column numbers.
 *
 * @class
 * @extends Error
 * @type {module.LessError}
 *
 * @prop {string} type
 * @prop {string} filename
 * @prop {number} index
 * @prop {number} line
 * @prop {number} column
 * @prop {number} callLine
 * @prop {number} callExtract
 * @prop {string[]} extract
 *
 * @param {Object} e              - An error object to wrap around or just a descriptive object
 * @param {Object} fileContentMap - An object with file contents in 'contents' property (like importManager) @todo - move to fileManager?
 * @param {string} [currentFilename]
 */

var LessError = function LessError(e, fileContentMap, currentFilename) {
  Error.call(this);
  var filename = e.filename || currentFilename;
  this.message = e.message;
  this.stack = e.stack;

  if (fileContentMap && filename) {
    var input = fileContentMap.contents[filename];
    var loc = getLocation(e.index, input);
    var line = loc.line;
    var col = loc.column;
    var callLine = e.call && getLocation(e.call, input).line;
    var lines = input ? input.split('\n') : '';
    this.type = e.type || 'Syntax';
    this.filename = filename;
    this.index = e.index;
    this.line = typeof line === 'number' ? line + 1 : null;
    this.column = col;

    if (!this.line && this.stack) {
      var found = this.stack.match(/(<anonymous>|Function):(\d+):(\d+)/);

      if (found) {
        if (found[2]) {
          this.line = parseInt(found[2]) - 2;
        }

        if (found[3]) {
          this.column = parseInt(found[3]);
        }
      }
    }

    this.callLine = callLine + 1;
    this.callExtract = lines[callLine];
    this.extract = [lines[this.line - 2], lines[this.line - 1], lines[this.line]];
  }
};

if (typeof Object.create === 'undefined') {
  var F = function F() {};

  F.prototype = Error.prototype;
  LessError.prototype = new F();
} else {
  LessError.prototype = Object.create(Error.prototype);
}

LessError.prototype.constructor = LessError;
/**
 * An overridden version of the default Object.prototype.toString
 * which uses additional information to create a helpful message.
 *
 * @param {Object} options
 * @returns {string}
 */

LessError.prototype.toString = function () {
  var options = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
  var message = '';
  var extract = this.extract || [];
  var error = [];

  var stylize = function stylize(str) {
    return str;
  };

  if (options.stylize) {
    var type = typeof options.stylize;

    if (type !== 'function') {
      throw Error(`options.stylize should be a function, got a ${type}!`);
    }

    stylize = options.stylize;
  }

  if (this.line !== null) {
    if (typeof extract[0] === 'string') {
      error.push(stylize(`${this.line - 1} ${extract[0]}`, 'grey'));
    }

    if (typeof extract[1] === 'string') {
      var errorTxt = `${this.line} `;

      if (extract[1]) {
        errorTxt += extract[1].slice(0, this.column) + stylize(stylize(stylize(extract[1].substr(this.column, 1), 'bold') + extract[1].slice(this.column + 1), 'red'), 'inverse');
      }

      error.push(errorTxt);
    }

    if (typeof extract[2] === 'string') {
      error.push(stylize(`${this.line + 1} ${extract[2]}`, 'grey'));
    }

    error = `${error.join('\n') + stylize('', 'reset')}\n`;
  }

  message += stylize(`${this.type}Error: ${this.message}`, 'red');

  if (this.filename) {
    message += stylize(' in ', 'red') + this.filename;
  }

  if (this.line) {
    message += stylize(` on line ${this.line}, column ${this.column + 1}:`, 'grey');
  }

  message += `\n${error}`;

  if (this.callLine) {
    message += `${stylize('from ', 'red') + (this.filename || '')}/n`;
    message += `${stylize(this.callLine, 'grey')} ${this.callExtract}/n`;
  }

  return message;
};

var Selector =
/*#__PURE__*/
function (_Node) {
  _inherits(Selector, _Node);

  function Selector(elements, extendList, condition, index, currentFileInfo, visibilityInfo) {
    var _this;

    _classCallCheck(this, Selector);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Selector).call(this));
    _this.extendList = extendList;
    _this.condition = condition;
    _this.evaldCondition = !condition;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.elements = _this.getElements(elements);
    _this.mixinElements_ = undefined;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.setParent(_this.elements, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Selector, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.elements) {
        this.elements = visitor.visitArray(this.elements);
      }

      if (this.extendList) {
        this.extendList = visitor.visitArray(this.extendList);
      }

      if (this.condition) {
        this.condition = visitor.visit(this.condition);
      }
    }
  }, {
    key: "createDerived",
    value: function createDerived(elements, extendList, evaldCondition) {
      elements = this.getElements(elements);
      var newSelector = new Selector(elements, extendList || this.extendList, null, this.getIndex(), this.fileInfo(), this.visibilityInfo());
      newSelector.evaldCondition = evaldCondition != null ? evaldCondition : this.evaldCondition;
      newSelector.mediaEmpty = this.mediaEmpty;
      return newSelector;
    }
  }, {
    key: "getElements",
    value: function getElements(els) {
      if (!els) {
        return [new Element('', '&', false, this._index, this._fileInfo)];
      }

      if (typeof els === 'string') {
        this.parse.parseNode(els, ['selector'], this._index, this._fileInfo, function (err, result) {
          if (err) {
            throw new LessError({
              index: err.index,
              message: err.message
            }, this.parse.imports, this._fileInfo.filename);
          }

          els = result[0].elements;
        });
      }

      return els;
    }
  }, {
    key: "createEmptySelectors",
    value: function createEmptySelectors() {
      var el = new Element('', '&', false, this._index, this._fileInfo);
      var sels = [new Selector([el], null, null, this._index, this._fileInfo)];
      sels[0].mediaEmpty = true;
      return sels;
    }
  }, {
    key: "match",
    value: function match(other) {
      var elements = this.elements;
      var len = elements.length;
      var olen;
      var i;
      other = other.mixinElements();
      olen = other.length;

      if (olen === 0 || len < olen) {
        return 0;
      } else {
        for (i = 0; i < olen; i++) {
          if (elements[i].value !== other[i]) {
            return 0;
          }
        }
      }

      return olen; // return number of matched elements
    }
  }, {
    key: "mixinElements",
    value: function mixinElements() {
      if (this.mixinElements_) {
        return this.mixinElements_;
      }

      var elements = this.elements.map(function (v) {
        return v.combinator.value + (v.value.value || v.value);
      }).join('').match(/[,&#\*\.\w-]([\w-]|(\\.))*/g);

      if (elements) {
        if (elements[0] === '&') {
          elements.shift();
        }
      } else {
        elements = [];
      }

      return this.mixinElements_ = elements;
    }
  }, {
    key: "isJustParentSelector",
    value: function isJustParentSelector() {
      return !this.mediaEmpty && this.elements.length === 1 && this.elements[0].value === '&' && (this.elements[0].combinator.value === ' ' || this.elements[0].combinator.value === '');
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var evaldCondition = this.condition && this.condition.eval(context);
      var elements = this.elements;
      var extendList = this.extendList;
      elements = elements && elements.map(function (e) {
        return e.eval(context);
      });
      extendList = extendList && extendList.map(function (extend) {
        return extend.eval(context);
      });
      return this.createDerived(elements, extendList, evaldCondition);
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      var i;
      var element;

      if ((!context || !context.firstSelector) && this.elements[0].combinator.value === '') {
        output.add(' ', this.fileInfo(), this.getIndex());
      }

      for (i = 0; i < this.elements.length; i++) {
        element = this.elements[i];
        element.genCSS(context, output);
      }
    }
  }, {
    key: "getIsOutput",
    value: function getIsOutput() {
      return this.evaldCondition;
    }
  }]);

  return Selector;
}(Node);

Selector.prototype.type = 'Selector';

var Value =
/*#__PURE__*/
function (_Node) {
  _inherits(Value, _Node);

  function Value(value) {
    var _this;

    _classCallCheck(this, Value);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Value).call(this));

    if (!value) {
      throw new Error('Value requires an array argument');
    }

    if (!Array.isArray(value)) {
      _this.value = [value];
    } else {
      _this.value = value;
    }

    return _this;
  }

  _createClass(Value, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.value) {
        this.value = visitor.visitArray(this.value);
      }
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      if (this.value.length === 1) {
        return this.value[0].eval(context);
      } else {
        return new Value(this.value.map(function (v) {
          return v.eval(context);
        }));
      }
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      var i;

      for (i = 0; i < this.value.length; i++) {
        this.value[i].genCSS(context, output);

        if (i + 1 < this.value.length) {
          output.add(context && context.compress ? ',' : ', ');
        }
      }
    }
  }]);

  return Value;
}(Node);

Value.prototype.type = 'Value';

var Keyword =
/*#__PURE__*/
function (_Node) {
  _inherits(Keyword, _Node);

  function Keyword(value) {
    var _this;

    _classCallCheck(this, Keyword);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Keyword).call(this));
    _this.value = value;
    return _this;
  }

  _createClass(Keyword, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      if (this.value === '%') {
        throw {
          type: 'Syntax',
          message: 'Invalid % without number'
        };
      }

      output.add(this.value);
    }
  }]);

  return Keyword;
}(Node);

Keyword.prototype.type = 'Keyword';
Keyword.True = new Keyword('true');
Keyword.False = new Keyword('false');

var Anonymous =
/*#__PURE__*/
function (_Node) {
  _inherits(Anonymous, _Node);

  function Anonymous(value, index, currentFileInfo, mapLines, rulesetLike, visibilityInfo) {
    var _this;

    _classCallCheck(this, Anonymous);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Anonymous).call(this));
    _this.value = value;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.mapLines = mapLines;
    _this.rulesetLike = typeof rulesetLike === 'undefined' ? false : rulesetLike;
    _this.allowRoot = true;

    _this.copyVisibilityInfo(visibilityInfo);

    return _this;
  }

  _createClass(Anonymous, [{
    key: "eval",
    value: function _eval() {
      return new Anonymous(this.value, this._index, this._fileInfo, this.mapLines, this.rulesetLike, this.visibilityInfo());
    }
  }, {
    key: "compare",
    value: function compare(other) {
      return other.toCSS && this.toCSS() === other.toCSS() ? 0 : undefined;
    }
  }, {
    key: "isRulesetLike",
    value: function isRulesetLike() {
      return this.rulesetLike;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      this.nodeVisible = Boolean(this.value);

      if (this.nodeVisible) {
        output.add(this.value, this._fileInfo, this._index, this.mapLines);
      }
    }
  }]);

  return Anonymous;
}(Node);

Anonymous.prototype.type = 'Anonymous';

var MATH = Math$1;

var Declaration =
/*#__PURE__*/
function (_Node) {
  _inherits(Declaration, _Node);

  function Declaration(name, value, important, merge, index, currentFileInfo, inline, variable) {
    var _this;

    _classCallCheck(this, Declaration);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Declaration).call(this));
    _this.name = name;
    _this.value = value instanceof Node ? value : new Value([value ? new Anonymous(value) : null]);
    _this.important = important ? ` ${important.trim()}` : '';
    _this.merge = merge;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.inline = inline || false;
    _this.variable = variable !== undefined ? variable : name.charAt && name.charAt(0) === '@';
    _this.allowRoot = true;

    _this.setParent(_this.value, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Declaration, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(this.name + (context.compress ? ':' : ': '), this.fileInfo(), this.getIndex());

      try {
        this.value.genCSS(context, output);
      } catch (e) {
        e.index = this._index;
        e.filename = this._fileInfo.filename;
        throw e;
      }

      output.add(this.important + (this.inline || context.lastRule && context.compress ? '' : ';'), this._fileInfo, this._index);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var mathBypass = false;
      var prevMath;
      var name = this.name;
      var evaldValue;
      var variable = this.variable;

      if (typeof name !== 'string') {
        // expand 'primitive' name directly to get
        // things faster (~10% for benchmark.less):
        name = name.length === 1 && name[0] instanceof Keyword ? name[0].value : evalName(context, name);
        variable = false; // never treat expanded interpolation as new variable name
      } // @todo remove when parens-division is default


      if (name === 'font' && context.math === MATH.ALWAYS) {
        mathBypass = true;
        prevMath = context.math;
        context.math = MATH.PARENS_DIVISION;
      }

      try {
        context.importantScope.push({});
        evaldValue = this.value.eval(context);

        if (!this.variable && evaldValue.type === 'DetachedRuleset') {
          throw {
            message: 'Rulesets cannot be evaluated on a property.',
            index: this.getIndex(),
            filename: this.fileInfo().filename
          };
        }

        var important = this.important;
        var importantResult = context.importantScope.pop();

        if (!important && importantResult.important) {
          important = importantResult.important;
        }

        return new Declaration(name, evaldValue, important, this.merge, this.getIndex(), this.fileInfo(), this.inline, variable);
      } catch (e) {
        if (typeof e.index !== 'number') {
          e.index = this.getIndex();
          e.filename = this.fileInfo().filename;
        }

        throw e;
      } finally {
        if (mathBypass) {
          context.math = prevMath;
        }
      }
    }
  }, {
    key: "makeImportant",
    value: function makeImportant() {
      return new Declaration(this.name, this.value, '!important', this.merge, this.getIndex(), this.fileInfo(), this.inline);
    }
  }]);

  return Declaration;
}(Node);

function evalName(context, name) {
  var value = '';
  var i;
  var n = name.length;
  var output = {
    add: function add(s) {
      value += s;
    }
  };

  for (i = 0; i < n; i++) {
    name[i].eval(context).genCSS(context, output);
  }

  return value;
}

Declaration.prototype.type = 'Declaration';

var debugInfo = function debugInfo(context, ctx, lineSeparator) {
  var result = '';

  if (context.dumpLineNumbers && !context.compress) {
    switch (context.dumpLineNumbers) {
      case 'comments':
        result = debugInfo.asComment(ctx);
        break;

      case 'mediaquery':
        result = debugInfo.asMediaQuery(ctx);
        break;

      case 'all':
        result = debugInfo.asComment(ctx) + (lineSeparator || '') + debugInfo.asMediaQuery(ctx);
        break;
    }
  }

  return result;
};

debugInfo.asComment = function (ctx) {
  return `/* line ${ctx.debugInfo.lineNumber}, ${ctx.debugInfo.fileName} */\n`;
};

debugInfo.asMediaQuery = function (ctx) {
  var filenameWithProtocol = ctx.debugInfo.fileName;

  if (!/^[a-z]+:\/\//i.test(filenameWithProtocol)) {
    filenameWithProtocol = `file://${filenameWithProtocol}`;
  }

  return `@media -sass-debug-info{filename{font-family:${filenameWithProtocol.replace(/([.:\/\\])/g, function (a) {
    if (a == '\\') {
      a = '\/';
    }

    return `\\${a}`;
  })}}line{font-family:\\00003${ctx.debugInfo.lineNumber}}}\n`;
};

var Comment =
/*#__PURE__*/
function (_Node) {
  _inherits(Comment, _Node);

  function Comment(value, isLineComment, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, Comment);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Comment).call(this));
    _this.value = value;
    _this.isLineComment = isLineComment;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.allowRoot = true;
    return _this;
  }

  _createClass(Comment, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      if (this.debugInfo) {
        output.add(debugInfo(context, this), this.fileInfo(), this.getIndex());
      }

      output.add(this.value);
    }
  }, {
    key: "isSilent",
    value: function isSilent(context) {
      var isCompressed = context.compress && this.value[2] !== '!';
      return this.isLineComment || isCompressed;
    }
  }]);

  return Comment;
}(Node);

Comment.prototype.type = 'Comment';

var contexts = {};

var copyFromOriginal = function copyFromOriginal(original, destination, propertiesToCopy) {
  if (!original) {
    return;
  }

  for (var i = 0; i < propertiesToCopy.length; i++) {
    if (original.hasOwnProperty(propertiesToCopy[i])) {
      destination[propertiesToCopy[i]] = original[propertiesToCopy[i]];
    }
  }
};
/*
 parse is used whilst parsing
 */


var parseCopyProperties = [// options
'paths', // option - unmodified - paths to search for imports on
'rewriteUrls', // option - whether to adjust URL's to be relative
'rootpath', // option - rootpath to append to URL's
'strictImports', // option -
'insecure', // option - whether to allow imports from insecure ssl hosts
'dumpLineNumbers', // option - whether to dump line numbers
'compress', // option - whether to compress
'syncImport', // option - whether to import synchronously
'chunkInput', // option - whether to chunk input. more performant but causes parse issues.
'mime', // browser only - mime type for sheet import
'useFileCache', // browser only - whether to use the per file session cache
// context
'processImports', // option & context - whether to process imports. if false then imports will not be imported.
// Used by the import manager to stop multiple import visitors being created.
'pluginManager' // Used as the plugin manager for the session
];

contexts.Parse = function (options) {
  copyFromOriginal(options, this, parseCopyProperties);

  if (typeof this.paths === 'string') {
    this.paths = [this.paths];
  }
};

var evalCopyProperties = ['paths', // additional include paths
'compress', // whether to compress
'math', // whether math has to be within parenthesis
'strictUnits', // whether units need to evaluate correctly
'sourceMap', // whether to output a source map
'importMultiple', // whether we are currently importing multiple copies
'urlArgs', // whether to add args into url tokens
'javascriptEnabled', // option - whether Inline JavaScript is enabled. if undefined, defaults to false
'pluginManager', // Used as the plugin manager for the session
'importantScope', // used to bubble up !important statements
'rewriteUrls' // option - whether to adjust URL's to be relative
];

function isPathRelative(path) {
  return !/^(?:[a-z-]+:|\/|#)/i.test(path);
}

function isPathLocalRelative(path) {
  return path.charAt(0) === '.';
}

contexts.Eval =
/*#__PURE__*/
function () {
  function _class(options, frames) {
    _classCallCheck(this, _class);

    copyFromOriginal(options, this, evalCopyProperties);

    if (typeof this.paths === 'string') {
      this.paths = [this.paths];
    }

    this.frames = frames || [];
    this.importantScope = this.importantScope || [];
    this.inCalc = false;
    this.mathOn = true;
  }

  _createClass(_class, [{
    key: "enterCalc",
    value: function enterCalc() {
      if (!this.calcStack) {
        this.calcStack = [];
      }

      this.calcStack.push(true);
      this.inCalc = true;
    }
  }, {
    key: "exitCalc",
    value: function exitCalc() {
      this.calcStack.pop();

      if (!this.calcStack) {
        this.inCalc = false;
      }
    }
  }, {
    key: "inParenthesis",
    value: function inParenthesis() {
      if (!this.parensStack) {
        this.parensStack = [];
      }

      this.parensStack.push(true);
    }
  }, {
    key: "outOfParenthesis",
    value: function outOfParenthesis() {
      this.parensStack.pop();
    }
  }, {
    key: "isMathOn",
    value: function isMathOn(op) {
      if (!this.mathOn) {
        return false;
      }

      if (op === '/' && this.math !== Math$1.ALWAYS && (!this.parensStack || !this.parensStack.length)) {
        return false;
      }

      if (this.math > Math$1.PARENS_DIVISION) {
        return this.parensStack && this.parensStack.length;
      }

      return true;
    }
  }, {
    key: "pathRequiresRewrite",
    value: function pathRequiresRewrite(path) {
      var isRelative = this.rewriteUrls === RewriteUrls.LOCAL ? isPathLocalRelative : isPathRelative;
      return isRelative(path);
    }
  }, {
    key: "rewritePath",
    value: function rewritePath(path, rootpath) {
      var newPath;
      rootpath = rootpath || '';
      newPath = this.normalizePath(rootpath + path); // If a path was explicit relative and the rootpath was not an absolute path
      // we must ensure that the new path is also explicit relative.

      if (isPathLocalRelative(path) && isPathRelative(rootpath) && isPathLocalRelative(newPath) === false) {
        newPath = `./${newPath}`;
      }

      return newPath;
    }
  }, {
    key: "normalizePath",
    value: function normalizePath(path) {
      var segments = path.split('/').reverse();
      var segment;
      path = [];

      while (segments.length !== 0) {
        segment = segments.pop();

        switch (segment) {
          case '.':
            break;

          case '..':
            if (path.length === 0 || path[path.length - 1] === '..') {
              path.push(segment);
            } else {
              path.pop();
            }

            break;

          default:
            path.push(segment);
            break;
        }
      }

      return path.join('/');
    }
  }]);

  return _class;
}();

function makeRegistry(base) {
  return {
    _data: {},
    add: function add(name, func) {
      // precautionary case conversion, as later querying of
      // the registry by function-caller uses lower case as well.
      name = name.toLowerCase();

      if (this._data.hasOwnProperty(name)) ;

      this._data[name] = func;
    },
    addMultiple: function addMultiple(functions) {
      var _this = this;

      Object.keys(functions).forEach(function (name) {
        _this.add(name, functions[name]);
      });
    },
    get: function get(name) {
      return this._data[name] || base && base.get(name);
    },
    getLocalFunctions: function getLocalFunctions() {
      return this._data;
    },
    inherit: function inherit() {
      return makeRegistry(this);
    },
    create: function create(base) {
      return makeRegistry(base);
    }
  };
}

var functionRegistry = makeRegistry(null);

var defaultFunc = {
  eval: function _eval() {
    var v = this.value_;
    var e = this.error_;

    if (e) {
      throw e;
    }

    if (v != null) {
      return v ? Keyword.True : Keyword.False;
    }
  },
  value: function value(v) {
    this.value_ = v;
  },
  error: function error(e) {
    this.error_ = e;
  },
  reset: function reset() {
    this.value_ = this.error_ = null;
  }
};

var Ruleset =
/*#__PURE__*/
function (_Node) {
  _inherits(Ruleset, _Node);

  function Ruleset(selectors, rules, strictImports, visibilityInfo) {
    var _this;

    _classCallCheck(this, Ruleset);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Ruleset).call(this));
    _this.selectors = selectors;
    _this.rules = rules;
    _this._lookups = {};
    _this._variables = null;
    _this._properties = null;
    _this.strictImports = strictImports;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.allowRoot = true;

    _this.setParent(_this.selectors, _assertThisInitialized(_this));

    _this.setParent(_this.rules, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Ruleset, [{
    key: "isRulesetLike",
    value: function isRulesetLike() {
      return true;
    }
  }, {
    key: "accept",
    value: function accept(visitor) {
      if (this.paths) {
        this.paths = visitor.visitArray(this.paths, true);
      } else if (this.selectors) {
        this.selectors = visitor.visitArray(this.selectors);
      }

      if (this.rules && this.rules.length) {
        this.rules = visitor.visitArray(this.rules);
      }
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var selectors;
      var selCnt;
      var selector;
      var i;
      var hasVariable;
      var hasOnePassingSelector = false;

      if (this.selectors && (selCnt = this.selectors.length)) {
        selectors = new Array(selCnt);
        defaultFunc.error({
          type: 'Syntax',
          message: 'it is currently only allowed in parametric mixin guards,'
        });

        for (i = 0; i < selCnt; i++) {
          selector = this.selectors[i].eval(context);

          for (var j = 0; j < selector.elements.length; j++) {
            if (selector.elements[j].isVariable) {
              hasVariable = true;
              break;
            }
          }

          selectors[i] = selector;

          if (selector.evaldCondition) {
            hasOnePassingSelector = true;
          }
        }

        if (hasVariable) {
          var toParseSelectors = new Array(selCnt);

          for (i = 0; i < selCnt; i++) {
            selector = selectors[i];
            toParseSelectors[i] = selector.toCSS(context);
          }

          this.parse.parseNode(toParseSelectors.join(','), ["selectors"], selectors[0].getIndex(), selectors[0].fileInfo(), function (err, result) {
            if (result) {
              selectors = flattenArray(result);
            }
          });
        }

        defaultFunc.reset();
      } else {
        hasOnePassingSelector = true;
      }

      var rules = this.rules ? copyArray(this.rules) : null;
      var ruleset = new Ruleset(selectors, rules, this.strictImports, this.visibilityInfo());
      var rule;
      var subRule;
      ruleset.originalRuleset = this;
      ruleset.root = this.root;
      ruleset.firstRoot = this.firstRoot;
      ruleset.allowImports = this.allowImports;

      if (this.debugInfo) {
        ruleset.debugInfo = this.debugInfo;
      }

      if (!hasOnePassingSelector) {
        rules.length = 0;
      } // inherit a function registry from the frames stack when possible;
      // otherwise from the global registry


      ruleset.functionRegistry = function (frames) {
        var i = 0;
        var n = frames.length;
        var found;

        for (; i !== n; ++i) {
          found = frames[i].functionRegistry;

          if (found) {
            return found;
          }
        }

        return functionRegistry;
      }(context.frames).inherit(); // push the current ruleset to the frames stack


      var ctxFrames = context.frames;
      ctxFrames.unshift(ruleset); // currrent selectors

      var ctxSelectors = context.selectors;

      if (!ctxSelectors) {
        context.selectors = ctxSelectors = [];
      }

      ctxSelectors.unshift(this.selectors); // Evaluate imports

      if (ruleset.root || ruleset.allowImports || !ruleset.strictImports) {
        ruleset.evalImports(context);
      } // Store the frames around mixin definitions,
      // so they can be evaluated like closures when the time comes.


      var rsRules = ruleset.rules;

      for (i = 0; rule = rsRules[i]; i++) {
        if (rule.evalFirst) {
          rsRules[i] = rule.eval(context);
        }
      }

      var mediaBlockCount = context.mediaBlocks && context.mediaBlocks.length || 0; // Evaluate mixin calls.

      for (i = 0; rule = rsRules[i]; i++) {
        if (rule.type === 'MixinCall') {
          /* jshint loopfunc:true */
          rules = rule.eval(context).filter(function (r) {
            if (r instanceof Declaration && r.variable) {
              // do not pollute the scope if the variable is
              // already there. consider returning false here
              // but we need a way to "return" variable from mixins
              return !ruleset.variable(r.name);
            }

            return true;
          });
          rsRules.splice.apply(rsRules, _toConsumableArray([i, 1].concat(rules)));
          i += rules.length - 1;
          ruleset.resetCache();
        } else if (rule.type === 'VariableCall') {
          /* jshint loopfunc:true */
          rules = rule.eval(context).rules.filter(function (r) {
            if (r instanceof Declaration && r.variable) {
              // do not pollute the scope at all
              return false;
            }

            return true;
          });
          rsRules.splice.apply(rsRules, _toConsumableArray([i, 1].concat(rules)));
          i += rules.length - 1;
          ruleset.resetCache();
        }
      } // Evaluate everything else


      for (i = 0; rule = rsRules[i]; i++) {
        if (!rule.evalFirst) {
          rsRules[i] = rule = rule.eval ? rule.eval(context) : rule;
        }
      } // Evaluate everything else


      for (i = 0; rule = rsRules[i]; i++) {
        // for rulesets, check if it is a css guard and can be removed
        if (rule instanceof Ruleset && rule.selectors && rule.selectors.length === 1) {
          // check if it can be folded in (e.g. & where)
          if (rule.selectors[0] && rule.selectors[0].isJustParentSelector()) {
            rsRules.splice(i--, 1);

            for (var j = 0; subRule = rule.rules[j]; j++) {
              if (subRule instanceof Node) {
                subRule.copyVisibilityInfo(rule.visibilityInfo());

                if (!(subRule instanceof Declaration) || !subRule.variable) {
                  rsRules.splice(++i, 0, subRule);
                }
              }
            }
          }
        }
      } // Pop the stack


      ctxFrames.shift();
      ctxSelectors.shift();

      if (context.mediaBlocks) {
        for (i = mediaBlockCount; i < context.mediaBlocks.length; i++) {
          context.mediaBlocks[i].bubbleSelectors(selectors);
        }
      }

      return ruleset;
    }
  }, {
    key: "evalImports",
    value: function evalImports(context) {
      var rules = this.rules;
      var i;
      var importRules;

      if (!rules) {
        return;
      }

      for (i = 0; i < rules.length; i++) {
        if (rules[i].type === 'Import') {
          importRules = rules[i].eval(context);

          if (importRules && (importRules.length || importRules.length === 0)) {
            rules.splice.apply(rules, _toConsumableArray([i, 1].concat(importRules)));
            i += importRules.length - 1;
          } else {
            rules.splice(i, 1, importRules);
          }

          this.resetCache();
        }
      }
    }
  }, {
    key: "makeImportant",
    value: function makeImportant() {
      var result = new Ruleset(this.selectors, this.rules.map(function (r) {
        if (r.makeImportant) {
          return r.makeImportant();
        } else {
          return r;
        }
      }), this.strictImports, this.visibilityInfo());
      return result;
    }
  }, {
    key: "matchArgs",
    value: function matchArgs(args) {
      return !args || args.length === 0;
    } // lets you call a css selector with a guard

  }, {
    key: "matchCondition",
    value: function matchCondition(args, context) {
      var lastSelector = this.selectors[this.selectors.length - 1];

      if (!lastSelector.evaldCondition) {
        return false;
      }

      if (lastSelector.condition && !lastSelector.condition.eval(new contexts.Eval(context, context.frames))) {
        return false;
      }

      return true;
    }
  }, {
    key: "resetCache",
    value: function resetCache() {
      this._rulesets = null;
      this._variables = null;
      this._properties = null;
      this._lookups = {};
    }
  }, {
    key: "variables",
    value: function variables() {
      if (!this._variables) {
        this._variables = !this.rules ? {} : this.rules.reduce(function (hash, r) {
          if (r instanceof Declaration && r.variable === true) {
            hash[r.name] = r;
          } // when evaluating variables in an import statement, imports have not been eval'd
          // so we need to go inside import statements.
          // guard against root being a string (in the case of inlined less)


          if (r.type === 'Import' && r.root && r.root.variables) {
            var vars = r.root.variables();

            for (var name in vars) {
              if (vars.hasOwnProperty(name)) {
                hash[name] = r.root.variable(name);
              }
            }
          }

          return hash;
        }, {});
      }

      return this._variables;
    }
  }, {
    key: "properties",
    value: function properties() {
      if (!this._properties) {
        this._properties = !this.rules ? {} : this.rules.reduce(function (hash, r) {
          if (r instanceof Declaration && r.variable !== true) {
            var name = r.name.length === 1 && r.name[0] instanceof Keyword ? r.name[0].value : r.name; // Properties don't overwrite as they can merge

            if (!hash[`$${name}`]) {
              hash[`$${name}`] = [r];
            } else {
              hash[`$${name}`].push(r);
            }
          }

          return hash;
        }, {});
      }

      return this._properties;
    }
  }, {
    key: "variable",
    value: function variable(name) {
      var decl = this.variables()[name];

      if (decl) {
        return this.parseValue(decl);
      }
    }
  }, {
    key: "property",
    value: function property(name) {
      var decl = this.properties()[name];

      if (decl) {
        return this.parseValue(decl);
      }
    }
  }, {
    key: "lastDeclaration",
    value: function lastDeclaration() {
      for (var i = this.rules.length; i > 0; i--) {
        var decl = this.rules[i - 1];

        if (decl instanceof Declaration) {
          return this.parseValue(decl);
        }
      }
    }
  }, {
    key: "parseValue",
    value: function parseValue(toParse) {
      var self = this;

      function transformDeclaration(decl) {
        if (decl.value instanceof Anonymous && !decl.parsed) {
          if (typeof decl.value.value === 'string') {
            this.parse.parseNode(decl.value.value, ['value', 'important'], decl.value.getIndex(), decl.fileInfo(), function (err, result) {
              if (err) {
                decl.parsed = true;
              }

              if (result) {
                decl.value = result[0];
                decl.important = result[1] || '';
                decl.parsed = true;
              }
            });
          } else {
            decl.parsed = true;
          }

          return decl;
        } else {
          return decl;
        }
      }

      if (!Array.isArray(toParse)) {
        return transformDeclaration.call(self, toParse);
      } else {
        var nodes = [];
        toParse.forEach(function (n) {
          nodes.push(transformDeclaration.call(self, n));
        });
        return nodes;
      }
    }
  }, {
    key: "rulesets",
    value: function rulesets() {
      if (!this.rules) {
        return [];
      }

      var filtRules = [];
      var rules = this.rules;
      var i;
      var rule;

      for (i = 0; rule = rules[i]; i++) {
        if (rule.isRuleset) {
          filtRules.push(rule);
        }
      }

      return filtRules;
    }
  }, {
    key: "prependRule",
    value: function prependRule(rule) {
      var rules = this.rules;

      if (rules) {
        rules.unshift(rule);
      } else {
        this.rules = [rule];
      }

      this.setParent(rule, this);
    }
  }, {
    key: "find",
    value: function find(selector) {
      var self = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : this;
      var filter = arguments.length > 2 ? arguments[2] : undefined;
      var rules = [];
      var match;
      var foundMixins;
      var key = selector.toCSS();

      if (key in this._lookups) {
        return this._lookups[key];
      }

      this.rulesets().forEach(function (rule) {
        if (rule !== self) {
          for (var j = 0; j < rule.selectors.length; j++) {
            match = selector.match(rule.selectors[j]);

            if (match) {
              if (selector.elements.length > match) {
                if (!filter || filter(rule)) {
                  foundMixins = rule.find(new Selector(selector.elements.slice(match)), self, filter);

                  for (var i = 0; i < foundMixins.length; ++i) {
                    foundMixins[i].path.push(rule);
                  }

                  Array.prototype.push.apply(rules, foundMixins);
                }
              } else {
                rules.push({
                  rule,
                  path: []
                });
              }

              break;
            }
          }
        }
      });
      this._lookups[key] = rules;
      return rules;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      var i;
      var j;
      var charsetRuleNodes = [];
      var ruleNodes = [];
      var // Line number debugging
      debugInfo$1;
      var rule;
      var path;
      context.tabLevel = context.tabLevel || 0;

      if (!this.root) {
        context.tabLevel++;
      }

      var tabRuleStr = context.compress ? '' : Array(context.tabLevel + 1).join('  ');
      var tabSetStr = context.compress ? '' : Array(context.tabLevel).join('  ');
      var sep;
      var charsetNodeIndex = 0;
      var importNodeIndex = 0;

      for (i = 0; rule = this.rules[i]; i++) {
        if (rule instanceof Comment) {
          if (importNodeIndex === i) {
            importNodeIndex++;
          }

          ruleNodes.push(rule);
        } else if (rule.isCharset && rule.isCharset()) {
          ruleNodes.splice(charsetNodeIndex, 0, rule);
          charsetNodeIndex++;
          importNodeIndex++;
        } else if (rule.type === 'Import') {
          ruleNodes.splice(importNodeIndex, 0, rule);
          importNodeIndex++;
        } else {
          ruleNodes.push(rule);
        }
      }

      ruleNodes = charsetRuleNodes.concat(ruleNodes); // If this is the root node, we don't render
      // a selector, or {}.

      if (!this.root) {
        debugInfo$1 = debugInfo(context, this, tabSetStr);

        if (debugInfo$1) {
          output.add(debugInfo$1);
          output.add(tabSetStr);
        }

        var paths = this.paths;
        var pathCnt = paths.length;
        var pathSubCnt;
        sep = context.compress ? ',' : `,\n${tabSetStr}`;

        for (i = 0; i < pathCnt; i++) {
          path = paths[i];

          if (!(pathSubCnt = path.length)) {
            continue;
          }

          if (i > 0) {
            output.add(sep);
          }

          context.firstSelector = true;
          path[0].genCSS(context, output);
          context.firstSelector = false;

          for (j = 1; j < pathSubCnt; j++) {
            path[j].genCSS(context, output);
          }
        }

        output.add((context.compress ? '{' : ' {\n') + tabRuleStr);
      } // Compile rules and rulesets


      for (i = 0; rule = ruleNodes[i]; i++) {
        if (i + 1 === ruleNodes.length) {
          context.lastRule = true;
        }

        var currentLastRule = context.lastRule;

        if (rule.isRulesetLike(rule)) {
          context.lastRule = false;
        }

        if (rule.genCSS) {
          rule.genCSS(context, output);
        } else if (rule.value) {
          output.add(rule.value.toString());
        }

        context.lastRule = currentLastRule;

        if (!context.lastRule && rule.isVisible()) {
          output.add(context.compress ? '' : `\n${tabRuleStr}`);
        } else {
          context.lastRule = false;
        }
      }

      if (!this.root) {
        output.add(context.compress ? '}' : `\n${tabSetStr}}`);
        context.tabLevel--;
      }

      if (!output.isEmpty() && !context.compress && this.firstRoot) {
        output.add('\n');
      }
    }
  }, {
    key: "joinSelectors",
    value: function joinSelectors(paths, context, selectors) {
      for (var s = 0; s < selectors.length; s++) {
        this.joinSelector(paths, context, selectors[s]);
      }
    }
  }, {
    key: "joinSelector",
    value: function joinSelector(paths, context, selector) {
      function createParenthesis(elementsToPak, originalElement) {
        var replacementParen;
        var j;

        if (elementsToPak.length === 0) {
          replacementParen = new Paren(elementsToPak[0]);
        } else {
          var insideParent = new Array(elementsToPak.length);

          for (j = 0; j < elementsToPak.length; j++) {
            insideParent[j] = new Element(null, elementsToPak[j], originalElement.isVariable, originalElement._index, originalElement._fileInfo);
          }

          replacementParen = new Paren(new Selector(insideParent));
        }

        return replacementParen;
      }

      function createSelector(containedElement, originalElement) {
        var element;
        var selector;
        element = new Element(null, containedElement, originalElement.isVariable, originalElement._index, originalElement._fileInfo);
        selector = new Selector([element]);
        return selector;
      } // joins selector path from `beginningPath` with selector path in `addPath`
      // `replacedElement` contains element that is being replaced by `addPath`
      // returns concatenated path


      function addReplacementIntoPath(beginningPath, addPath, replacedElement, originalSelector) {
        var newSelectorPath;
        var lastSelector;
        var newJoinedSelector; // our new selector path

        newSelectorPath = []; // construct the joined selector - if & is the first thing this will be empty,
        // if not newJoinedSelector will be the last set of elements in the selector

        if (beginningPath.length > 0) {
          newSelectorPath = copyArray(beginningPath);
          lastSelector = newSelectorPath.pop();
          newJoinedSelector = originalSelector.createDerived(copyArray(lastSelector.elements));
        } else {
          newJoinedSelector = originalSelector.createDerived([]);
        }

        if (addPath.length > 0) {
          // /deep/ is a CSS4 selector - (removed, so should deprecate)
          // that is valid without anything in front of it
          // so if the & does not have a combinator that is "" or " " then
          // and there is a combinator on the parent, then grab that.
          // this also allows + a { & .b { .a & { ... though not sure why you would want to do that
          var combinator = replacedElement.combinator;
          var parentEl = addPath[0].elements[0];

          if (combinator.emptyOrWhitespace && !parentEl.combinator.emptyOrWhitespace) {
            combinator = parentEl.combinator;
          } // join the elements so far with the first part of the parent


          newJoinedSelector.elements.push(new Element(combinator, parentEl.value, replacedElement.isVariable, replacedElement._index, replacedElement._fileInfo));
          newJoinedSelector.elements = newJoinedSelector.elements.concat(addPath[0].elements.slice(1));
        } // now add the joined selector - but only if it is not empty


        if (newJoinedSelector.elements.length !== 0) {
          newSelectorPath.push(newJoinedSelector);
        } // put together the parent selectors after the join (e.g. the rest of the parent)


        if (addPath.length > 1) {
          var restOfPath = addPath.slice(1);
          restOfPath = restOfPath.map(function (selector) {
            return selector.createDerived(selector.elements, []);
          });
          newSelectorPath = newSelectorPath.concat(restOfPath);
        }

        return newSelectorPath;
      } // joins selector path from `beginningPath` with every selector path in `addPaths` array
      // `replacedElement` contains element that is being replaced by `addPath`
      // returns array with all concatenated paths


      function addAllReplacementsIntoPath(beginningPath, addPaths, replacedElement, originalSelector, result) {
        var j;

        for (j = 0; j < beginningPath.length; j++) {
          var newSelectorPath = addReplacementIntoPath(beginningPath[j], addPaths, replacedElement, originalSelector);
          result.push(newSelectorPath);
        }

        return result;
      }

      function mergeElementsOnToSelectors(elements, selectors) {
        var i;
        var sel;

        if (elements.length === 0) {
          return;
        }

        if (selectors.length === 0) {
          selectors.push([new Selector(elements)]);
          return;
        }

        for (i = 0; sel = selectors[i]; i++) {
          // if the previous thing in sel is a parent this needs to join on to it
          if (sel.length > 0) {
            sel[sel.length - 1] = sel[sel.length - 1].createDerived(sel[sel.length - 1].elements.concat(elements));
          } else {
            sel.push(new Selector(elements));
          }
        }
      } // replace all parent selectors inside `inSelector` by content of `context` array
      // resulting selectors are returned inside `paths` array
      // returns true if `inSelector` contained at least one parent selector


      function replaceParentSelector(paths, context, inSelector) {
        // The paths are [[Selector]]
        // The first list is a list of comma separated selectors
        // The inner list is a list of inheritance separated selectors
        // e.g.
        // .a, .b {
        //   .c {
        //   }
        // }
        // == [[.a] [.c]] [[.b] [.c]]
        //
        var i;
        var j;
        var k;
        var currentElements;
        var newSelectors;
        var selectorsMultiplied;
        var sel;
        var el;
        var hadParentSelector = false;
        var length;
        var lastSelector;

        function findNestedSelector(element) {
          var maybeSelector;

          if (!(element.value instanceof Paren)) {
            return null;
          }

          maybeSelector = element.value.value;

          if (!(maybeSelector instanceof Selector)) {
            return null;
          }

          return maybeSelector;
        } // the elements from the current selector so far


        currentElements = []; // the current list of new selectors to add to the path.
        // We will build it up. We initiate it with one empty selector as we "multiply" the new selectors
        // by the parents

        newSelectors = [[]];

        for (i = 0; el = inSelector.elements[i]; i++) {
          // non parent reference elements just get added
          if (el.value !== '&') {
            var nestedSelector = findNestedSelector(el);

            if (nestedSelector != null) {
              // merge the current list of non parent selector elements
              // on to the current list of selectors to add
              mergeElementsOnToSelectors(currentElements, newSelectors);
              var nestedPaths = [];
              var replaced = void 0;
              var replacedNewSelectors = [];
              replaced = replaceParentSelector(nestedPaths, context, nestedSelector);
              hadParentSelector = hadParentSelector || replaced; // the nestedPaths array should have only one member - replaceParentSelector does not multiply selectors

              for (k = 0; k < nestedPaths.length; k++) {
                var replacementSelector = createSelector(createParenthesis(nestedPaths[k], el), el);
                addAllReplacementsIntoPath(newSelectors, [replacementSelector], el, inSelector, replacedNewSelectors);
              }

              newSelectors = replacedNewSelectors;
              currentElements = [];
            } else {
              currentElements.push(el);
            }
          } else {
            hadParentSelector = true; // the new list of selectors to add

            selectorsMultiplied = []; // merge the current list of non parent selector elements
            // on to the current list of selectors to add

            mergeElementsOnToSelectors(currentElements, newSelectors); // loop through our current selectors

            for (j = 0; j < newSelectors.length; j++) {
              sel = newSelectors[j]; // if we don't have any parent paths, the & might be in a mixin so that it can be used
              // whether there are parents or not

              if (context.length === 0) {
                // the combinator used on el should now be applied to the next element instead so that
                // it is not lost
                if (sel.length > 0) {
                  sel[0].elements.push(new Element(el.combinator, '', el.isVariable, el._index, el._fileInfo));
                }

                selectorsMultiplied.push(sel);
              } else {
                // and the parent selectors
                for (k = 0; k < context.length; k++) {
                  // We need to put the current selectors
                  // then join the last selector's elements on to the parents selectors
                  var newSelectorPath = addReplacementIntoPath(sel, context[k], el, inSelector); // add that to our new set of selectors

                  selectorsMultiplied.push(newSelectorPath);
                }
              }
            } // our new selectors has been multiplied, so reset the state


            newSelectors = selectorsMultiplied;
            currentElements = [];
          }
        } // if we have any elements left over (e.g. .a& .b == .b)
        // add them on to all the current selectors


        mergeElementsOnToSelectors(currentElements, newSelectors);

        for (i = 0; i < newSelectors.length; i++) {
          length = newSelectors[i].length;

          if (length > 0) {
            paths.push(newSelectors[i]);
            lastSelector = newSelectors[i][length - 1];
            newSelectors[i][length - 1] = lastSelector.createDerived(lastSelector.elements, inSelector.extendList);
          }
        }

        return hadParentSelector;
      }

      function deriveSelector(visibilityInfo, deriveFrom) {
        var newSelector = deriveFrom.createDerived(deriveFrom.elements, deriveFrom.extendList, deriveFrom.evaldCondition);
        newSelector.copyVisibilityInfo(visibilityInfo);
        return newSelector;
      } // joinSelector code follows


      var i;
      var newPaths;
      var hadParentSelector;
      newPaths = [];
      hadParentSelector = replaceParentSelector(newPaths, context, selector);

      if (!hadParentSelector) {
        if (context.length > 0) {
          newPaths = [];

          for (i = 0; i < context.length; i++) {
            var concatenated = context[i].map(deriveSelector.bind(this, selector.visibilityInfo()));
            concatenated.push(selector);
            newPaths.push(concatenated);
          }
        } else {
          newPaths = [[selector]];
        }
      }

      for (i = 0; i < newPaths.length; i++) {
        paths.push(newPaths[i]);
      }
    }
  }]);

  return Ruleset;
}(Node);

Ruleset.prototype.type = 'Ruleset';
Ruleset.prototype.isRuleset = true;

var AtRule =
/*#__PURE__*/
function (_Node) {
  _inherits(AtRule, _Node);

  function AtRule(name, value, rules, index, currentFileInfo, debugInfo, isRooted, visibilityInfo) {
    var _this;

    _classCallCheck(this, AtRule);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(AtRule).call(this));
    var i;
    _this.name = name;
    _this.value = value instanceof Node ? value : value ? new Anonymous(value) : value;

    if (rules) {
      if (Array.isArray(rules)) {
        _this.rules = rules;
      } else {
        _this.rules = [rules];
        _this.rules[0].selectors = new Selector([], null, null, index, currentFileInfo).createEmptySelectors();
      }

      for (i = 0; i < _this.rules.length; i++) {
        _this.rules[i].allowImports = true;
      }

      _this.setParent(_this.rules, _assertThisInitialized(_this));
    }

    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.debugInfo = debugInfo;
    _this.isRooted = isRooted || false;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.allowRoot = true;
    return _this;
  }

  _createClass(AtRule, [{
    key: "accept",
    value: function accept(visitor) {
      var value = this.value;
      var rules = this.rules;

      if (rules) {
        this.rules = visitor.visitArray(rules);
      }

      if (value) {
        this.value = visitor.visit(value);
      }
    }
  }, {
    key: "isRulesetLike",
    value: function isRulesetLike() {
      return this.rules || !this.isCharset();
    }
  }, {
    key: "isCharset",
    value: function isCharset() {
      return '@charset' === this.name;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      var value = this.value;
      var rules = this.rules;
      output.add(this.name, this.fileInfo(), this.getIndex());

      if (value) {
        output.add(' ');
        value.genCSS(context, output);
      }

      if (rules) {
        this.outputRuleset(context, output, rules);
      } else {
        output.add(';');
      }
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var mediaPathBackup;
      var mediaBlocksBackup;
      var value = this.value;
      var rules = this.rules; // media stored inside other atrule should not bubble over it
      // backpup media bubbling information

      mediaPathBackup = context.mediaPath;
      mediaBlocksBackup = context.mediaBlocks; // deleted media bubbling information

      context.mediaPath = [];
      context.mediaBlocks = [];

      if (value) {
        value = value.eval(context);
      }

      if (rules) {
        // assuming that there is only one rule at this point - that is how parser constructs the rule
        rules = [rules[0].eval(context)];
        rules[0].root = true;
      } // restore media bubbling information


      context.mediaPath = mediaPathBackup;
      context.mediaBlocks = mediaBlocksBackup;
      return new AtRule(this.name, value, rules, this.getIndex(), this.fileInfo(), this.debugInfo, this.isRooted, this.visibilityInfo());
    }
  }, {
    key: "variable",
    value: function variable(name) {
      if (this.rules) {
        // assuming that there is only one rule at this point - that is how parser constructs the rule
        return Ruleset.prototype.variable.call(this.rules[0], name);
      }
    }
  }, {
    key: "find",
    value: function find() {
      if (this.rules) {
        for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
          args[_key] = arguments[_key];
        }

        // assuming that there is only one rule at this point - that is how parser constructs the rule
        return Ruleset.prototype.find.apply(this.rules[0], args);
      }
    }
  }, {
    key: "rulesets",
    value: function rulesets() {
      if (this.rules) {
        // assuming that there is only one rule at this point - that is how parser constructs the rule
        return Ruleset.prototype.rulesets.apply(this.rules[0]);
      }
    }
  }, {
    key: "outputRuleset",
    value: function outputRuleset(context, output, rules) {
      var ruleCnt = rules.length;
      var i;
      context.tabLevel = (context.tabLevel | 0) + 1; // Compressed

      if (context.compress) {
        output.add('{');

        for (i = 0; i < ruleCnt; i++) {
          rules[i].genCSS(context, output);
        }

        output.add('}');
        context.tabLevel--;
        return;
      } // Non-compressed


      var tabSetStr = `\n${Array(context.tabLevel).join('  ')}`;
      var tabRuleStr = `${tabSetStr}  `;

      if (!ruleCnt) {
        output.add(` {${tabSetStr}}`);
      } else {
        output.add(` {${tabRuleStr}`);
        rules[0].genCSS(context, output);

        for (i = 1; i < ruleCnt; i++) {
          output.add(tabRuleStr);
          rules[i].genCSS(context, output);
        }

        output.add(`${tabSetStr}}`);
      }

      context.tabLevel--;
    }
  }]);

  return AtRule;
}(Node);

AtRule.prototype.type = 'AtRule';

var DetachedRuleset =
/*#__PURE__*/
function (_Node) {
  _inherits(DetachedRuleset, _Node);

  function DetachedRuleset(ruleset, frames) {
    var _this;

    _classCallCheck(this, DetachedRuleset);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(DetachedRuleset).call(this));
    _this.ruleset = ruleset;
    _this.frames = frames;

    _this.setParent(_this.ruleset, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(DetachedRuleset, [{
    key: "accept",
    value: function accept(visitor) {
      this.ruleset = visitor.visit(this.ruleset);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var frames = this.frames || copyArray(context.frames);
      return new DetachedRuleset(this.ruleset, frames);
    }
  }, {
    key: "callEval",
    value: function callEval(context) {
      return this.ruleset.eval(this.frames ? new contexts.Eval(context, this.frames.concat(context.frames)) : context);
    }
  }]);

  return DetachedRuleset;
}(Node);

DetachedRuleset.prototype.type = 'DetachedRuleset';
DetachedRuleset.prototype.evalFirst = true;

var Unit =
/*#__PURE__*/
function (_Node) {
  _inherits(Unit, _Node);

  function Unit(numerator, denominator, backupUnit) {
    var _this;

    _classCallCheck(this, Unit);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Unit).call(this));
    _this.numerator = numerator ? copyArray(numerator).sort() : [];
    _this.denominator = denominator ? copyArray(denominator).sort() : [];

    if (backupUnit) {
      _this.backupUnit = backupUnit;
    } else if (numerator && numerator.length) {
      _this.backupUnit = numerator[0];
    }

    return _this;
  }

  _createClass(Unit, [{
    key: "clone",
    value: function clone() {
      return new Unit(copyArray(this.numerator), copyArray(this.denominator), this.backupUnit);
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      // Dimension checks the unit is singular and throws an error if in strict math mode.
      var strictUnits = context && context.strictUnits;

      if (this.numerator.length === 1) {
        output.add(this.numerator[0]); // the ideal situation
      } else if (!strictUnits && this.backupUnit) {
        output.add(this.backupUnit);
      } else if (!strictUnits && this.denominator.length) {
        output.add(this.denominator[0]);
      }
    }
  }, {
    key: "toString",
    value: function toString() {
      var i;
      var returnStr = this.numerator.join('*');

      for (i = 0; i < this.denominator.length; i++) {
        returnStr += `/${this.denominator[i]}`;
      }

      return returnStr;
    }
  }, {
    key: "compare",
    value: function compare(other) {
      return this.is(other.toString()) ? 0 : undefined;
    }
  }, {
    key: "is",
    value: function is(unitString) {
      return this.toString().toUpperCase() === unitString.toUpperCase();
    }
  }, {
    key: "isLength",
    value: function isLength() {
      return RegExp('^(px|em|ex|ch|rem|in|cm|mm|pc|pt|ex|vw|vh|vmin|vmax)$', 'gi').test(this.toCSS());
    }
  }, {
    key: "isEmpty",
    value: function isEmpty() {
      return this.numerator.length === 0 && this.denominator.length === 0;
    }
  }, {
    key: "isSingular",
    value: function isSingular() {
      return this.numerator.length <= 1 && this.denominator.length === 0;
    }
  }, {
    key: "map",
    value: function map(callback) {
      var i;

      for (i = 0; i < this.numerator.length; i++) {
        this.numerator[i] = callback(this.numerator[i], false);
      }

      for (i = 0; i < this.denominator.length; i++) {
        this.denominator[i] = callback(this.denominator[i], true);
      }
    }
  }, {
    key: "usedUnits",
    value: function usedUnits() {
      var group;
      var result = {};
      var mapUnit;
      var groupName;

      mapUnit = function mapUnit(atomicUnit) {
        /* jshint loopfunc:true */
        if (group.hasOwnProperty(atomicUnit) && !result[groupName]) {
          result[groupName] = atomicUnit;
        }

        return atomicUnit;
      };

      for (groupName in unitConversions) {
        if (unitConversions.hasOwnProperty(groupName)) {
          group = unitConversions[groupName];
          this.map(mapUnit);
        }
      }

      return result;
    }
  }, {
    key: "cancel",
    value: function cancel() {
      var counter = {};
      var atomicUnit;
      var i;

      for (i = 0; i < this.numerator.length; i++) {
        atomicUnit = this.numerator[i];
        counter[atomicUnit] = (counter[atomicUnit] || 0) + 1;
      }

      for (i = 0; i < this.denominator.length; i++) {
        atomicUnit = this.denominator[i];
        counter[atomicUnit] = (counter[atomicUnit] || 0) - 1;
      }

      this.numerator = [];
      this.denominator = [];

      for (atomicUnit in counter) {
        if (counter.hasOwnProperty(atomicUnit)) {
          var count = counter[atomicUnit];

          if (count > 0) {
            for (i = 0; i < count; i++) {
              this.numerator.push(atomicUnit);
            }
          } else if (count < 0) {
            for (i = 0; i < -count; i++) {
              this.denominator.push(atomicUnit);
            }
          }
        }
      }

      this.numerator.sort();
      this.denominator.sort();
    }
  }]);

  return Unit;
}(Node);

Unit.prototype.type = 'Unit';

// A number with a unit
//

var Dimension =
/*#__PURE__*/
function (_Node) {
  _inherits(Dimension, _Node);

  function Dimension(value, unit) {
    var _this;

    _classCallCheck(this, Dimension);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Dimension).call(this));
    _this.value = parseFloat(value);

    if (isNaN(_this.value)) {
      throw new Error('Dimension is not a number.');
    }

    _this.unit = unit && unit instanceof Unit ? unit : new Unit(unit ? [unit] : undefined);

    _this.setParent(_this.unit, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Dimension, [{
    key: "accept",
    value: function accept(visitor) {
      this.unit = visitor.visit(this.unit);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      return this;
    }
  }, {
    key: "toColor",
    value: function toColor() {
      return new Color([this.value, this.value, this.value]);
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      if (context && context.strictUnits && !this.unit.isSingular()) {
        throw new Error(`Multiple units in dimension. Correct the units or use the unit function. Bad unit: ${this.unit.toString()}`);
      }

      var value = this.fround(context, this.value);
      var strValue = String(value);

      if (value !== 0 && value < 0.000001 && value > -0.000001) {
        // would be output 1e-6 etc.
        strValue = value.toFixed(20).replace(/0+$/, '');
      }

      if (context && context.compress) {
        // Zero values doesn't need a unit
        if (value === 0 && this.unit.isLength()) {
          output.add(strValue);
          return;
        } // Float values doesn't need a leading zero


        if (value > 0 && value < 1) {
          strValue = strValue.substr(1);
        }
      }

      output.add(strValue);
      this.unit.genCSS(context, output);
    } // In an operation between two Dimensions,
    // we default to the first Dimension's unit,
    // so `1px + 2` will yield `3px`.

  }, {
    key: "operate",
    value: function operate(context, op, other) {
      /* jshint noempty:false */
      var value = this._operate(context, op, this.value, other.value);

      var unit = this.unit.clone();

      if (op === '+' || op === '-') {
        if (unit.numerator.length === 0 && unit.denominator.length === 0) {
          unit = other.unit.clone();

          if (this.unit.backupUnit) {
            unit.backupUnit = this.unit.backupUnit;
          }
        } else if (other.unit.numerator.length === 0 && unit.denominator.length === 0) ; else {
          other = other.convertTo(this.unit.usedUnits());

          if (context.strictUnits && other.unit.toString() !== unit.toString()) {
            throw new Error(`Incompatible units. Change the units or use the unit function. ` + `Bad units: '${unit.toString()}' and '${other.unit.toString()}'.`);
          }

          value = this._operate(context, op, this.value, other.value);
        }
      } else if (op === '*') {
        unit.numerator = unit.numerator.concat(other.unit.numerator).sort();
        unit.denominator = unit.denominator.concat(other.unit.denominator).sort();
        unit.cancel();
      } else if (op === '/') {
        unit.numerator = unit.numerator.concat(other.unit.denominator).sort();
        unit.denominator = unit.denominator.concat(other.unit.numerator).sort();
        unit.cancel();
      }

      return new Dimension(value, unit);
    }
  }, {
    key: "compare",
    value: function compare(other) {
      var a;
      var b;

      if (!(other instanceof Dimension)) {
        return undefined;
      }

      if (this.unit.isEmpty() || other.unit.isEmpty()) {
        a = this;
        b = other;
      } else {
        a = this.unify();
        b = other.unify();

        if (a.unit.compare(b.unit) !== 0) {
          return undefined;
        }
      }

      return Node.numericCompare(a.value, b.value);
    }
  }, {
    key: "unify",
    value: function unify() {
      return this.convertTo({
        length: 'px',
        duration: 's',
        angle: 'rad'
      });
    }
  }, {
    key: "convertTo",
    value: function convertTo(conversions) {
      var value = this.value;
      var unit = this.unit.clone();
      var i;
      var groupName;
      var group;
      var targetUnit;
      var derivedConversions = {};
      var applyUnit;

      if (typeof conversions === 'string') {
        for (i in unitConversions) {
          if (unitConversions[i].hasOwnProperty(conversions)) {
            derivedConversions = {};
            derivedConversions[i] = conversions;
          }
        }

        conversions = derivedConversions;
      }

      applyUnit = function applyUnit(atomicUnit, denominator) {
        /* jshint loopfunc:true */
        if (group.hasOwnProperty(atomicUnit)) {
          if (denominator) {
            value = value / (group[atomicUnit] / group[targetUnit]);
          } else {
            value = value * (group[atomicUnit] / group[targetUnit]);
          }

          return targetUnit;
        }

        return atomicUnit;
      };

      for (groupName in conversions) {
        if (conversions.hasOwnProperty(groupName)) {
          targetUnit = conversions[groupName];
          group = unitConversions[groupName];
          unit.map(applyUnit);
        }
      }

      unit.cancel();
      return new Dimension(value, unit);
    }
  }]);

  return Dimension;
}(Node);

Dimension.prototype.type = 'Dimension';

var MATH$1 = Math$1;

var Operation =
/*#__PURE__*/
function (_Node) {
  _inherits(Operation, _Node);

  function Operation(op, operands, isSpaced) {
    var _this;

    _classCallCheck(this, Operation);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Operation).call(this));
    _this.op = op.trim();
    _this.operands = operands;
    _this.isSpaced = isSpaced;
    return _this;
  }

  _createClass(Operation, [{
    key: "accept",
    value: function accept(visitor) {
      this.operands = visitor.visitArray(this.operands);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var a = this.operands[0].eval(context);
      var b = this.operands[1].eval(context);
      var op;

      if (context.isMathOn(this.op)) {
        op = this.op === './' ? '/' : this.op;

        if (a instanceof Dimension && b instanceof Color) {
          a = a.toColor();
        }

        if (b instanceof Dimension && a instanceof Color) {
          b = b.toColor();
        }

        if (!a.operate) {
          if (a instanceof Operation && a.op === '/' && context.math === MATH$1.PARENS_DIVISION) {
            return new Operation(this.op, [a, b], this.isSpaced);
          }

          throw {
            type: 'Operation',
            message: 'Operation on an invalid type'
          };
        }

        return a.operate(context, op, b);
      } else {
        return new Operation(this.op, [a, b], this.isSpaced);
      }
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      this.operands[0].genCSS(context, output);

      if (this.isSpaced) {
        output.add(' ');
      }

      output.add(this.op);

      if (this.isSpaced) {
        output.add(' ');
      }

      this.operands[1].genCSS(context, output);
    }
  }]);

  return Operation;
}(Node);

Operation.prototype.type = 'Operation';

var MATH$2 = Math$1;

var Expression =
/*#__PURE__*/
function (_Node) {
  _inherits(Expression, _Node);

  function Expression(value, noSpacing) {
    var _this;

    _classCallCheck(this, Expression);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Expression).call(this));
    _this.value = value;
    _this.noSpacing = noSpacing;

    if (!value) {
      throw new Error('Expression requires an array parameter');
    }

    return _this;
  }

  _createClass(Expression, [{
    key: "accept",
    value: function accept(visitor) {
      this.value = visitor.visitArray(this.value);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var returnValue;
      var mathOn = context.isMathOn();
      var inParenthesis = this.parens && (context.math !== MATH$2.STRICT_LEGACY || !this.parensInOp);
      var doubleParen = false;

      if (inParenthesis) {
        context.inParenthesis();
      }

      if (this.value.length > 1) {
        returnValue = new Expression(this.value.map(function (e) {
          if (!e.eval) {
            return e;
          }

          return e.eval(context);
        }), this.noSpacing);
      } else if (this.value.length === 1) {
        if (this.value[0].parens && !this.value[0].parensInOp && !context.inCalc) {
          doubleParen = true;
        }

        returnValue = this.value[0].eval(context);
      } else {
        returnValue = this;
      }

      if (inParenthesis) {
        context.outOfParenthesis();
      }

      if (this.parens && this.parensInOp && !mathOn && !doubleParen && !(returnValue instanceof Dimension)) {
        returnValue = new Paren(returnValue);
      }

      return returnValue;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      for (var i = 0; i < this.value.length; i++) {
        this.value[i].genCSS(context, output);

        if (!this.noSpacing && i + 1 < this.value.length) {
          output.add(' ');
        }
      }
    }
  }, {
    key: "throwAwayComments",
    value: function throwAwayComments() {
      this.value = this.value.filter(function (v) {
        return !(v instanceof Comment);
      });
    }
  }]);

  return Expression;
}(Node);

Expression.prototype.type = 'Expression';

var functionCaller =
/*#__PURE__*/
function () {
  function functionCaller(name, context, index, currentFileInfo) {
    _classCallCheck(this, functionCaller);

    this.name = name.toLowerCase();
    this.index = index;
    this.context = context;
    this.currentFileInfo = currentFileInfo;
    this.func = context.frames[0].functionRegistry.get(this.name);
  }

  _createClass(functionCaller, [{
    key: "isValid",
    value: function isValid() {
      return Boolean(this.func);
    }
  }, {
    key: "call",
    value: function call(args) {
      // This code is terrible and should be replaced as per this issue...
      // https://github.com/less/less.js/issues/2477
      if (Array.isArray(args)) {
        args = args.filter(function (item) {
          if (item.type === 'Comment') {
            return false;
          }

          return true;
        }).map(function (item) {
          if (item.type === 'Expression') {
            var subNodes = item.value.filter(function (item) {
              if (item.type === 'Comment') {
                return false;
              }

              return true;
            });

            if (subNodes.length === 1) {
              return subNodes[0];
            } else {
              return new Expression(subNodes);
            }
          }

          return item;
        });
      }

      return this.func.apply(this, _toConsumableArray(args));
    }
  }]);

  return functionCaller;
}();

// A function call node.
//

var Call =
/*#__PURE__*/
function (_Node) {
  _inherits(Call, _Node);

  function Call(name, args, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, Call);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Call).call(this));
    _this.name = name;
    _this.args = args;
    _this.calc = name === 'calc';
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    return _this;
  }

  _createClass(Call, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.args) {
        this.args = visitor.visitArray(this.args);
      }
    } //
    // When evaluating a function call,
    // we either find the function in the functionRegistry,
    // in which case we call it, passing the  evaluated arguments,
    // if this returns null or we cannot find the function, we
    // simply print it out as it appeared originally [2].
    //
    // The reason why we evaluate the arguments, is in the case where
    // we try to pass a variable to a function, like: `saturate(@color)`.
    // The function should receive the value, not the variable.
    //

  }, {
    key: "eval",
    value: function _eval(context) {
      /**
       * Turn off math for calc(), and switch back on for evaluating nested functions
       */
      var currentMathContext = context.mathOn;
      context.mathOn = !this.calc;

      if (this.calc || context.inCalc) {
        context.enterCalc();
      }

      var args = this.args.map(function (a) {
        return a.eval(context);
      });

      if (this.calc || context.inCalc) {
        context.exitCalc();
      }

      context.mathOn = currentMathContext;
      var result;
      var funcCaller = new functionCaller(this.name, context, this.getIndex(), this.fileInfo());

      if (funcCaller.isValid()) {
        try {
          result = funcCaller.call(args);
        } catch (e) {
          throw {
            type: e.type || 'Runtime',
            message: `error evaluating function \`${this.name}\`${e.message ? `: ${e.message}` : ''}`,
            index: this.getIndex(),
            filename: this.fileInfo().filename,
            line: e.lineNumber,
            column: e.columnNumber
          };
        }

        if (result !== null && result !== undefined) {
          // Results that that are not nodes are cast as Anonymous nodes
          // Falsy values or booleans are returned as empty nodes
          if (!(result instanceof Node)) {
            if (!result || result === true) {
              result = new Anonymous(null);
            } else {
              result = new Anonymous(result.toString());
            }
          }

          result._index = this._index;
          result._fileInfo = this._fileInfo;
          return result;
        }
      }

      return new Call(this.name, args, this.getIndex(), this.fileInfo());
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(`${this.name}(`, this.fileInfo(), this.getIndex());

      for (var i = 0; i < this.args.length; i++) {
        this.args[i].genCSS(context, output);

        if (i + 1 < this.args.length) {
          output.add(', ');
        }
      }

      output.add(')');
    }
  }]);

  return Call;
}(Node);

Call.prototype.type = 'Call';

var Variable =
/*#__PURE__*/
function (_Node) {
  _inherits(Variable, _Node);

  function Variable(name, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, Variable);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Variable).call(this));
    _this.name = name;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    return _this;
  }

  _createClass(Variable, [{
    key: "eval",
    value: function _eval(context) {
      var variable;
      var name = this.name;

      if (name.indexOf('@@') === 0) {
        name = `@${new Variable(name.slice(1), this.getIndex(), this.fileInfo()).eval(context).value}`;
      }

      if (this.evaluating) {
        throw {
          type: 'Name',
          message: `Recursive variable definition for ${name}`,
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }

      this.evaluating = true;
      variable = this.find(context.frames, function (frame) {
        var v = frame.variable(name);

        if (v) {
          if (v.important) {
            var importantScope = context.importantScope[context.importantScope.length - 1];
            importantScope.important = v.important;
          } // If in calc, wrap vars in a function call to cascade evaluate args first


          if (context.inCalc) {
            return new Call('_SELF', [v.value]).eval(context);
          } else {
            return v.value.eval(context);
          }
        }
      });

      if (variable) {
        this.evaluating = false;
        return variable;
      } else {
        throw {
          type: 'Name',
          message: `variable ${name} is undefined`,
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }
    }
  }, {
    key: "find",
    value: function find(obj, fun) {
      for (var i = 0, r; i < obj.length; i++) {
        r = fun.call(obj, obj[i]);

        if (r) {
          return r;
        }
      }

      return null;
    }
  }]);

  return Variable;
}(Node);

Variable.prototype.type = 'Variable';

var Property =
/*#__PURE__*/
function (_Node) {
  _inherits(Property, _Node);

  function Property(name, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, Property);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Property).call(this));
    _this.name = name;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    return _this;
  }

  _createClass(Property, [{
    key: "eval",
    value: function _eval(context) {
      var property;
      var name = this.name; // TODO: shorten this reference

      var mergeRules = context.pluginManager.less.visitors.ToCSSVisitor.prototype._mergeRules;

      if (this.evaluating) {
        throw {
          type: 'Name',
          message: `Recursive property reference for ${name}`,
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }

      this.evaluating = true;
      property = this.find(context.frames, function (frame) {
        var v;
        var vArr = frame.property(name);

        if (vArr) {
          for (var i = 0; i < vArr.length; i++) {
            v = vArr[i];
            vArr[i] = new Declaration(v.name, v.value, v.important, v.merge, v.index, v.currentFileInfo, v.inline, v.variable);
          }

          mergeRules(vArr);
          v = vArr[vArr.length - 1];

          if (v.important) {
            var importantScope = context.importantScope[context.importantScope.length - 1];
            importantScope.important = v.important;
          }

          v = v.value.eval(context);
          return v;
        }
      });

      if (property) {
        this.evaluating = false;
        return property;
      } else {
        throw {
          type: 'Name',
          message: `Property '${name}' is undefined`,
          filename: this.currentFileInfo.filename,
          index: this.index
        };
      }
    }
  }, {
    key: "find",
    value: function find(obj, fun) {
      for (var i = 0, r; i < obj.length; i++) {
        r = fun.call(obj, obj[i]);

        if (r) {
          return r;
        }
      }

      return null;
    }
  }]);

  return Property;
}(Node);

Property.prototype.type = 'Property';

var Attribute =
/*#__PURE__*/
function (_Node) {
  _inherits(Attribute, _Node);

  function Attribute(key, op, value) {
    var _this;

    _classCallCheck(this, Attribute);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Attribute).call(this));
    _this.key = key;
    _this.op = op;
    _this.value = value;
    return _this;
  }

  _createClass(Attribute, [{
    key: "eval",
    value: function _eval(context) {
      return new Attribute(this.key.eval ? this.key.eval(context) : this.key, this.op, this.value && this.value.eval ? this.value.eval(context) : this.value);
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(this.toCSS(context));
    }
  }, {
    key: "toCSS",
    value: function toCSS(context) {
      var value = this.key.toCSS ? this.key.toCSS(context) : this.key;

      if (this.op) {
        value += this.op;
        value += this.value.toCSS ? this.value.toCSS(context) : this.value;
      }

      return `[${value}]`;
    }
  }]);

  return Attribute;
}(Node);

Attribute.prototype.type = 'Attribute';

var Quoted =
/*#__PURE__*/
function (_Node) {
  _inherits(Quoted, _Node);

  function Quoted(str, content, escaped, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, Quoted);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Quoted).call(this));
    _this.escaped = escaped == null ? true : escaped;
    _this.value = content || '';
    _this.quote = str.charAt(0);
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.variableRegex = /@\{([\w-]+)\}/g;
    _this.propRegex = /\$\{([\w-]+)\}/g;
    _this.allowRoot = escaped;
    return _this;
  }

  _createClass(Quoted, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      if (!this.escaped) {
        output.add(this.quote, this.fileInfo(), this.getIndex());
      }

      output.add(this.value);

      if (!this.escaped) {
        output.add(this.quote);
      }
    }
  }, {
    key: "containsVariables",
    value: function containsVariables() {
      return this.value.match(this.variableRegex);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var that = this;
      var value = this.value;

      var variableReplacement = function variableReplacement(_, name) {
        var v = new Variable(`@${name}`, that.getIndex(), that.fileInfo()).eval(context, true);
        return v instanceof Quoted ? v.value : v.toCSS();
      };

      var propertyReplacement = function propertyReplacement(_, name) {
        var v = new Property(`$${name}`, that.getIndex(), that.fileInfo()).eval(context, true);
        return v instanceof Quoted ? v.value : v.toCSS();
      };

      function iterativeReplace(value, regexp, replacementFnc) {
        var evaluatedValue = value;

        do {
          value = evaluatedValue.toString();
          evaluatedValue = value.replace(regexp, replacementFnc);
        } while (value !== evaluatedValue);

        return evaluatedValue;
      }

      value = iterativeReplace(value, this.variableRegex, variableReplacement);
      value = iterativeReplace(value, this.propRegex, propertyReplacement);
      return new Quoted(this.quote + value + this.quote, value, this.escaped, this.getIndex(), this.fileInfo());
    }
  }, {
    key: "compare",
    value: function compare(other) {
      // when comparing quoted strings allow the quote to differ
      if (other.type === 'Quoted' && !this.escaped && !other.escaped) {
        return Node.numericCompare(this.value, other.value);
      } else {
        return other.toCSS && this.toCSS() === other.toCSS() ? 0 : undefined;
      }
    }
  }]);

  return Quoted;
}(Node);

Quoted.prototype.type = 'Quoted';

var URL =
/*#__PURE__*/
function (_Node) {
  _inherits(URL, _Node);

  function URL(val, index, currentFileInfo, isEvald) {
    var _this;

    _classCallCheck(this, URL);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(URL).call(this));
    _this.value = val;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.isEvald = isEvald;
    return _this;
  }

  _createClass(URL, [{
    key: "accept",
    value: function accept(visitor) {
      this.value = visitor.visit(this.value);
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add('url(');
      this.value.genCSS(context, output);
      output.add(')');
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var val = this.value.eval(context);
      var rootpath;

      if (!this.isEvald) {
        // Add the rootpath if the URL requires a rewrite
        rootpath = this.fileInfo() && this.fileInfo().rootpath;

        if (typeof rootpath === 'string' && typeof val.value === 'string' && context.pathRequiresRewrite(val.value)) {
          if (!val.quote) {
            rootpath = escapePath(rootpath);
          }

          val.value = context.rewritePath(val.value, rootpath);
        } else {
          val.value = context.normalizePath(val.value);
        } // Add url args if enabled


        if (context.urlArgs) {
          if (!val.value.match(/^\s*data:/)) {
            var delimiter = val.value.indexOf('?') === -1 ? '?' : '&';
            var urlArgs = delimiter + context.urlArgs;

            if (val.value.indexOf('#') !== -1) {
              val.value = val.value.replace('#', `${urlArgs}#`);
            } else {
              val.value += urlArgs;
            }
          }
        }
      }

      return new URL(val, this.getIndex(), this.fileInfo(), true);
    }
  }]);

  return URL;
}(Node);

URL.prototype.type = 'Url';

function escapePath(path) {
  return path.replace(/[\(\)'"\s]/g, function (match) {
    return `\\${match}`;
  });
}

var Media =
/*#__PURE__*/
function (_AtRule) {
  _inherits(Media, _AtRule);

  function Media(value, features, index, currentFileInfo, visibilityInfo) {
    var _this;

    _classCallCheck(this, Media);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Media).call(this));
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    var selectors = new Selector([], null, null, _this._index, _this._fileInfo).createEmptySelectors();
    _this.features = new Value(features);
    _this.rules = [new Ruleset(selectors, value)];
    _this.rules[0].allowImports = true;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.allowRoot = true;

    _this.setParent(selectors, _assertThisInitialized(_this));

    _this.setParent(_this.features, _assertThisInitialized(_this));

    _this.setParent(_this.rules, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Media, [{
    key: "isRulesetLike",
    value: function isRulesetLike() {
      return true;
    }
  }, {
    key: "accept",
    value: function accept(visitor) {
      if (this.features) {
        this.features = visitor.visit(this.features);
      }

      if (this.rules) {
        this.rules = visitor.visitArray(this.rules);
      }
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add('@media ', this._fileInfo, this._index);
      this.features.genCSS(context, output);
      this.outputRuleset(context, output, this.rules);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      if (!context.mediaBlocks) {
        context.mediaBlocks = [];
        context.mediaPath = [];
      }

      var media = new Media(null, [], this._index, this._fileInfo, this.visibilityInfo());

      if (this.debugInfo) {
        this.rules[0].debugInfo = this.debugInfo;
        media.debugInfo = this.debugInfo;
      }

      media.features = this.features.eval(context);
      context.mediaPath.push(media);
      context.mediaBlocks.push(media);
      this.rules[0].functionRegistry = context.frames[0].functionRegistry.inherit();
      context.frames.unshift(this.rules[0]);
      media.rules = [this.rules[0].eval(context)];
      context.frames.shift();
      context.mediaPath.pop();
      return context.mediaPath.length === 0 ? media.evalTop(context) : media.evalNested(context);
    }
  }, {
    key: "evalTop",
    value: function evalTop(context) {
      var result = this; // Render all dependent Media blocks.

      if (context.mediaBlocks.length > 1) {
        var selectors = new Selector([], null, null, this.getIndex(), this.fileInfo()).createEmptySelectors();
        result = new Ruleset(selectors, context.mediaBlocks);
        result.multiMedia = true;
        result.copyVisibilityInfo(this.visibilityInfo());
        this.setParent(result, this);
      }

      delete context.mediaBlocks;
      delete context.mediaPath;
      return result;
    }
  }, {
    key: "evalNested",
    value: function evalNested(context) {
      var i;
      var value;
      var path = context.mediaPath.concat([this]); // Extract the media-query conditions separated with `,` (OR).

      for (i = 0; i < path.length; i++) {
        value = path[i].features instanceof Value ? path[i].features.value : path[i].features;
        path[i] = Array.isArray(value) ? value : [value];
      } // Trace all permutations to generate the resulting media-query.
      //
      // (a, b and c) with nested (d, e) ->
      //    a and d
      //    a and e
      //    b and c and d
      //    b and c and e


      this.features = new Value(this.permute(path).map(function (path) {
        path = path.map(function (fragment) {
          return fragment.toCSS ? fragment : new Anonymous(fragment);
        });

        for (i = path.length - 1; i > 0; i--) {
          path.splice(i, 0, new Anonymous('and'));
        }

        return new Expression(path);
      }));
      this.setParent(this.features, this); // Fake a tree-node that doesn't output anything.

      return new Ruleset([], []);
    }
  }, {
    key: "permute",
    value: function permute(arr) {
      if (arr.length === 0) {
        return [];
      } else if (arr.length === 1) {
        return arr[0];
      } else {
        var result = [];
        var rest = this.permute(arr.slice(1));

        for (var i = 0; i < rest.length; i++) {
          for (var j = 0; j < arr[0].length; j++) {
            result.push([arr[0][j]].concat(rest[i]));
          }
        }

        return result;
      }
    }
  }, {
    key: "bubbleSelectors",
    value: function bubbleSelectors(selectors) {
      if (!selectors) {
        return;
      }

      this.rules = [new Ruleset(copyArray(selectors), [this.rules[0]])];
      this.setParent(this.rules, this);
    }
  }]);

  return Media;
}(AtRule);

Media.prototype.type = 'Media';

// CSS @import node
//
// The general strategy here is that we don't want to wait
// for the parsing to be completed, before we start importing
// the file. That's because in the context of a browser,
// most of the time will be spent waiting for the server to respond.
//
// On creation, we push the import path to our import queue, though
// `import,push`, we also pass it a callback, which it'll call once
// the file has been fetched, and parsed.
//

var Import =
/*#__PURE__*/
function (_Node) {
  _inherits(Import, _Node);

  function Import(path, features, options, index, currentFileInfo, visibilityInfo) {
    var _this;

    _classCallCheck(this, Import);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Import).call(this));
    _this.options = options;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.path = path;
    _this.features = features;
    _this.allowRoot = true;

    if (_this.options.less !== undefined || _this.options.inline) {
      _this.css = !_this.options.less || _this.options.inline;
    } else {
      var pathValue = _this.getPath();

      if (pathValue && /[#\.\&\?]css([\?;].*)?$/.test(pathValue)) {
        _this.css = true;
      }
    }

    _this.copyVisibilityInfo(visibilityInfo);

    _this.setParent(_this.features, _assertThisInitialized(_this));

    _this.setParent(_this.path, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Import, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.features) {
        this.features = visitor.visit(this.features);
      }

      this.path = visitor.visit(this.path);

      if (!this.options.isPlugin && !this.options.inline && this.root) {
        this.root = visitor.visit(this.root);
      }
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      if (this.css && this.path._fileInfo.reference === undefined) {
        output.add('@import ', this._fileInfo, this._index);
        this.path.genCSS(context, output);

        if (this.features) {
          output.add(' ');
          this.features.genCSS(context, output);
        }

        output.add(';');
      }
    }
  }, {
    key: "getPath",
    value: function getPath() {
      return this.path instanceof URL ? this.path.value.value : this.path.value;
    }
  }, {
    key: "isVariableImport",
    value: function isVariableImport() {
      var path = this.path;

      if (path instanceof URL) {
        path = path.value;
      }

      if (path instanceof Quoted) {
        return path.containsVariables();
      }

      return true;
    }
  }, {
    key: "evalForImport",
    value: function evalForImport(context) {
      var path = this.path;

      if (path instanceof URL) {
        path = path.value;
      }

      return new Import(path.eval(context), this.features, this.options, this._index, this._fileInfo, this.visibilityInfo());
    }
  }, {
    key: "evalPath",
    value: function evalPath(context) {
      var path = this.path.eval(context);
      var fileInfo = this._fileInfo;

      if (!(path instanceof URL)) {
        // Add the rootpath if the URL requires a rewrite
        var pathValue = path.value;

        if (fileInfo && pathValue && context.pathRequiresRewrite(pathValue)) {
          path.value = context.rewritePath(pathValue, fileInfo.rootpath);
        } else {
          path.value = context.normalizePath(path.value);
        }
      }

      return path;
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var result = this.doEval(context);

      if (this.options.reference || this.blocksVisibility()) {
        if (result.length || result.length === 0) {
          result.forEach(function (node) {
            node.addVisibilityBlock();
          });
        } else {
          result.addVisibilityBlock();
        }
      }

      return result;
    }
  }, {
    key: "doEval",
    value: function doEval(context) {
      var ruleset;
      var registry;
      var features = this.features && this.features.eval(context);

      if (this.options.isPlugin) {
        if (this.root && this.root.eval) {
          try {
            this.root.eval(context);
          } catch (e) {
            e.message = 'Plugin error during evaluation';
            throw new LessError(e, this.root.imports, this.root.filename);
          }
        }

        registry = context.frames[0] && context.frames[0].functionRegistry;

        if (registry && this.root && this.root.functions) {
          registry.addMultiple(this.root.functions);
        }

        return [];
      }

      if (this.skip) {
        if (typeof this.skip === 'function') {
          this.skip = this.skip();
        }

        if (this.skip) {
          return [];
        }
      }

      if (this.options.inline) {
        var contents = new Anonymous(this.root, 0, {
          filename: this.importedFilename,
          reference: this.path._fileInfo && this.path._fileInfo.reference
        }, true, true);
        return this.features ? new Media([contents], this.features.value) : [contents];
      } else if (this.css) {
        var newImport = new Import(this.evalPath(context), features, this.options, this._index);

        if (!newImport.css && this.error) {
          throw this.error;
        }

        return newImport;
      } else {
        ruleset = new Ruleset(null, copyArray(this.root.rules));
        ruleset.evalImports(context);
        return this.features ? new Media(ruleset.rules, this.features.value) : ruleset.rules;
      }
    }
  }]);

  return Import;
}(Node);

Import.prototype.type = 'Import';

var JsEvalNode =
/*#__PURE__*/
function (_Node) {
  _inherits(JsEvalNode, _Node);

  function JsEvalNode() {
    _classCallCheck(this, JsEvalNode);

    return _possibleConstructorReturn(this, _getPrototypeOf(JsEvalNode).apply(this, arguments));
  }

  _createClass(JsEvalNode, [{
    key: "evaluateJavaScript",
    value: function evaluateJavaScript(expression, context) {
      var result;
      var that = this;
      var evalContext = {};

      if (!context.javascriptEnabled) {
        throw {
          message: 'Inline JavaScript is not enabled. Is it set in your options?',
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }

      expression = expression.replace(/@\{([\w-]+)\}/g, function (_, name) {
        return that.jsify(new Variable(`@${name}`, that.getIndex(), that.fileInfo()).eval(context));
      });

      try {
        expression = new Function(`return (${expression})`);
      } catch (e) {
        throw {
          message: `JavaScript evaluation error: ${e.message} from \`${expression}\``,
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }

      var variables = context.frames[0].variables();

      for (var k in variables) {
        if (variables.hasOwnProperty(k)) {
          /* jshint loopfunc:true */
          evalContext[k.slice(1)] = {
            value: variables[k].value,
            toJS: function toJS() {
              return this.value.eval(context).toCSS();
            }
          };
        }
      }

      try {
        result = expression.call(evalContext);
      } catch (e) {
        throw {
          message: `JavaScript evaluation error: '${e.name}: ${e.message.replace(/["]/g, '\'')}'`,
          filename: this.fileInfo().filename,
          index: this.getIndex()
        };
      }

      return result;
    }
  }, {
    key: "jsify",
    value: function jsify(obj) {
      if (Array.isArray(obj.value) && obj.value.length > 1) {
        return `[${obj.value.map(function (v) {
          return v.toCSS();
        }).join(', ')}]`;
      } else {
        return obj.toCSS();
      }
    }
  }]);

  return JsEvalNode;
}(Node);

var JavaScript =
/*#__PURE__*/
function (_JsEvalNode) {
  _inherits(JavaScript, _JsEvalNode);

  function JavaScript(string, escaped, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, JavaScript);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(JavaScript).call(this));
    _this.escaped = escaped;
    _this.expression = string;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    return _this;
  }

  _createClass(JavaScript, [{
    key: "eval",
    value: function _eval(context) {
      var result = this.evaluateJavaScript(this.expression, context);
      var type = typeof result;

      if (type === 'number' && !isNaN(result)) {
        return new Dimension(result);
      } else if (type === 'string') {
        return new Quoted(`"${result}"`, result, this.escaped, this._index);
      } else if (Array.isArray(result)) {
        return new Anonymous(result.join(', '));
      } else {
        return new Anonymous(result);
      }
    }
  }]);

  return JavaScript;
}(JsEvalNode);

JavaScript.prototype.type = 'JavaScript';

var Assignment =
/*#__PURE__*/
function (_Node) {
  _inherits(Assignment, _Node);

  function Assignment(key, val) {
    var _this;

    _classCallCheck(this, Assignment);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Assignment).call(this));
    _this.key = key;
    _this.value = val;
    return _this;
  }

  _createClass(Assignment, [{
    key: "accept",
    value: function accept(visitor) {
      this.value = visitor.visit(this.value);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      if (this.value.eval) {
        return new Assignment(this.key, this.value.eval(context));
      }

      return this;
    }
  }, {
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add(`${this.key}=`);

      if (this.value.genCSS) {
        this.value.genCSS(context, output);
      } else {
        output.add(this.value);
      }
    }
  }]);

  return Assignment;
}(Node);

Assignment.prototype.type = 'Assignment';

var Condition =
/*#__PURE__*/
function (_Node) {
  _inherits(Condition, _Node);

  function Condition(op, l, r, i, negate) {
    var _this;

    _classCallCheck(this, Condition);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Condition).call(this));
    _this.op = op.trim();
    _this.lvalue = l;
    _this.rvalue = r;
    _this._index = i;
    _this.negate = negate;
    return _this;
  }

  _createClass(Condition, [{
    key: "accept",
    value: function accept(visitor) {
      this.lvalue = visitor.visit(this.lvalue);
      this.rvalue = visitor.visit(this.rvalue);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var result = function (op, a, b) {
        switch (op) {
          case 'and':
            return a && b;

          case 'or':
            return a || b;

          default:
            switch (Node.compare(a, b)) {
              case -1:
                return op === '<' || op === '=<' || op === '<=';

              case 0:
                return op === '=' || op === '>=' || op === '=<' || op === '<=';

              case 1:
                return op === '>' || op === '>=';

              default:
                return false;
            }

        }
      }(this.op, this.lvalue.eval(context), this.rvalue.eval(context));

      return this.negate ? !result : result;
    }
  }]);

  return Condition;
}(Node);

Condition.prototype.type = 'Condition';

var UnicodeDescriptor =
/*#__PURE__*/
function (_Node) {
  _inherits(UnicodeDescriptor, _Node);

  function UnicodeDescriptor(value) {
    var _this;

    _classCallCheck(this, UnicodeDescriptor);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(UnicodeDescriptor).call(this));
    _this.value = value;
    return _this;
  }

  return UnicodeDescriptor;
}(Node);

UnicodeDescriptor.prototype.type = 'UnicodeDescriptor';

var Negative =
/*#__PURE__*/
function (_Node) {
  _inherits(Negative, _Node);

  function Negative(node) {
    var _this;

    _classCallCheck(this, Negative);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Negative).call(this));
    _this.value = node;
    return _this;
  }

  _createClass(Negative, [{
    key: "genCSS",
    value: function genCSS(context, output) {
      output.add('-');
      this.value.genCSS(context, output);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      if (context.isMathOn()) {
        return new Operation('*', [new Dimension(-1), this.value]).eval(context);
      }

      return new Negative(this.value.eval(context));
    }
  }]);

  return Negative;
}(Node);

Negative.prototype.type = 'Negative';

var Extend =
/*#__PURE__*/
function (_Node) {
  _inherits(Extend, _Node);

  function Extend(selector, option, index, currentFileInfo, visibilityInfo) {
    var _this;

    _classCallCheck(this, Extend);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Extend).call(this));
    _this.selector = selector;
    _this.option = option;
    _this.object_id = Extend.next_id++;
    _this.parent_ids = [_this.object_id];
    _this._index = index;
    _this._fileInfo = currentFileInfo;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.allowRoot = true;

    switch (option) {
      case 'all':
        _this.allowBefore = true;
        _this.allowAfter = true;
        break;

      default:
        _this.allowBefore = false;
        _this.allowAfter = false;
        break;
    }

    _this.setParent(_this.selector, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(Extend, [{
    key: "accept",
    value: function accept(visitor) {
      this.selector = visitor.visit(this.selector);
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      return new Extend(this.selector.eval(context), this.option, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    }
  }, {
    key: "clone",
    value: function clone(context) {
      return new Extend(this.selector, this.option, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    } // it concatenates (joins) all selectors in selector array

  }, {
    key: "findSelfSelectors",
    value: function findSelfSelectors(selectors) {
      var selfElements = [];
      var i;
      var selectorElements;

      for (i = 0; i < selectors.length; i++) {
        selectorElements = selectors[i].elements; // duplicate the logic in genCSS function inside the selector node.
        // future TODO - move both logics into the selector joiner visitor

        if (i > 0 && selectorElements.length && selectorElements[0].combinator.value === '') {
          selectorElements[0].combinator.value = ' ';
        }

        selfElements = selfElements.concat(selectors[i].elements);
      }

      this.selfSelectors = [new Selector(selfElements)];
      this.selfSelectors[0].copyVisibilityInfo(this.visibilityInfo());
    }
  }]);

  return Extend;
}(Node);

Extend.next_id = 0;
Extend.prototype.type = 'Extend';

var VariableCall =
/*#__PURE__*/
function (_Node) {
  _inherits(VariableCall, _Node);

  function VariableCall(variable, index, currentFileInfo) {
    var _this;

    _classCallCheck(this, VariableCall);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(VariableCall).call(this));
    _this.variable = variable;
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.allowRoot = true;
    return _this;
  }

  _createClass(VariableCall, [{
    key: "eval",
    value: function _eval(context) {
      var rules;
      var detachedRuleset = new Variable(this.variable, this.getIndex(), this.fileInfo()).eval(context);
      var error = new LessError({
        message: `Could not evaluate variable call ${this.variable}`
      });

      if (!detachedRuleset.ruleset) {
        if (detachedRuleset.rules) {
          rules = detachedRuleset;
        } else if (Array.isArray(detachedRuleset)) {
          rules = new Ruleset('', detachedRuleset);
        } else if (Array.isArray(detachedRuleset.value)) {
          rules = new Ruleset('', detachedRuleset.value);
        } else {
          throw error;
        }

        detachedRuleset = new DetachedRuleset(rules);
      }

      if (detachedRuleset.ruleset) {
        return detachedRuleset.callEval(context);
      }

      throw error;
    }
  }]);

  return VariableCall;
}(Node);

VariableCall.prototype.type = 'VariableCall';

var NamespaceValue =
/*#__PURE__*/
function (_Node) {
  _inherits(NamespaceValue, _Node);

  function NamespaceValue(ruleCall, lookups, important, index, fileInfo) {
    var _this;

    _classCallCheck(this, NamespaceValue);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(NamespaceValue).call(this));
    _this.value = ruleCall;
    _this.lookups = lookups;
    _this.important = important;
    _this._index = index;
    _this._fileInfo = fileInfo;
    return _this;
  }

  _createClass(NamespaceValue, [{
    key: "eval",
    value: function _eval(context) {
      var i;
      var name;
      var rules = this.value.eval(context);

      for (i = 0; i < this.lookups.length; i++) {
        name = this.lookups[i];
        /**
         * Eval'd DRs return rulesets.
         * Eval'd mixins return rules, so let's make a ruleset if we need it.
         * We need to do this because of late parsing of values
         */

        if (Array.isArray(rules)) {
          rules = new Ruleset([new Selector()], rules);
        }

        if (name === '') {
          rules = rules.lastDeclaration();
        } else if (name.charAt(0) === '@') {
          if (name.charAt(1) === '@') {
            name = `@${new Variable(name.substr(1)).eval(context).value}`;
          }

          if (rules.variables) {
            rules = rules.variable(name);
          }

          if (!rules) {
            throw {
              type: 'Name',
              message: `variable ${name} not found`,
              filename: this.fileInfo().filename,
              index: this.getIndex()
            };
          }
        } else {
          if (name.substring(0, 2) === '$@') {
            name = `$${new Variable(name.substr(1)).eval(context).value}`;
          } else {
            name = name.charAt(0) === '$' ? name : `$${name}`;
          }

          if (rules.properties) {
            rules = rules.property(name);
          }

          if (!rules) {
            throw {
              type: 'Name',
              message: `property "${name.substr(1)}" not found`,
              filename: this.fileInfo().filename,
              index: this.getIndex()
            };
          } // Properties are an array of values, since a ruleset can have multiple props.
          // We pick the last one (the "cascaded" value)


          rules = rules[rules.length - 1];
        }

        if (rules.value) {
          rules = rules.eval(context).value;
        }

        if (rules.ruleset) {
          rules = rules.ruleset.eval(context);
        }
      }

      return rules;
    }
  }]);

  return NamespaceValue;
}(Node);

NamespaceValue.prototype.type = 'NamespaceValue';

var Definition =
/*#__PURE__*/
function (_Ruleset) {
  _inherits(Definition, _Ruleset);

  function Definition(name, params, rules, condition, variadic, frames, visibilityInfo) {
    var _this;

    _classCallCheck(this, Definition);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(Definition).call(this));
    _this.name = name || 'anonymous mixin';
    _this.selectors = [new Selector([new Element(null, name, false, _this._index, _this._fileInfo)])];
    _this.params = params;
    _this.condition = condition;
    _this.variadic = variadic;
    _this.arity = params.length;
    _this.rules = rules;
    _this._lookups = {};
    var optionalParameters = [];
    _this.required = params.reduce(function (count, p) {
      if (!p.name || p.name && !p.value) {
        return count + 1;
      } else {
        optionalParameters.push(p.name);
        return count;
      }
    }, 0);
    _this.optionalParameters = optionalParameters;
    _this.frames = frames;

    _this.copyVisibilityInfo(visibilityInfo);

    _this.allowRoot = true;
    return _this;
  }

  _createClass(Definition, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.params && this.params.length) {
        this.params = visitor.visitArray(this.params);
      }

      this.rules = visitor.visitArray(this.rules);

      if (this.condition) {
        this.condition = visitor.visit(this.condition);
      }
    }
  }, {
    key: "evalParams",
    value: function evalParams(context, mixinEnv, args, evaldArguments) {
      /* jshint boss:true */
      var frame = new Ruleset(null, null);
      var varargs;
      var arg;
      var params = copyArray(this.params);
      var i;
      var j;
      var val;
      var name;
      var isNamedFound;
      var argIndex;
      var argsLength = 0;

      if (mixinEnv.frames && mixinEnv.frames[0] && mixinEnv.frames[0].functionRegistry) {
        frame.functionRegistry = mixinEnv.frames[0].functionRegistry.inherit();
      }

      mixinEnv = new contexts.Eval(mixinEnv, [frame].concat(mixinEnv.frames));

      if (args) {
        args = copyArray(args);
        argsLength = args.length;

        for (i = 0; i < argsLength; i++) {
          arg = args[i];

          if (name = arg && arg.name) {
            isNamedFound = false;

            for (j = 0; j < params.length; j++) {
              if (!evaldArguments[j] && name === params[j].name) {
                evaldArguments[j] = arg.value.eval(context);
                frame.prependRule(new Declaration(name, arg.value.eval(context)));
                isNamedFound = true;
                break;
              }
            }

            if (isNamedFound) {
              args.splice(i, 1);
              i--;
              continue;
            } else {
              throw {
                type: 'Runtime',
                message: `Named argument for ${this.name} ${args[i].name} not found`
              };
            }
          }
        }
      }

      argIndex = 0;

      for (i = 0; i < params.length; i++) {
        if (evaldArguments[i]) {
          continue;
        }

        arg = args && args[argIndex];

        if (name = params[i].name) {
          if (params[i].variadic) {
            varargs = [];

            for (j = argIndex; j < argsLength; j++) {
              varargs.push(args[j].value.eval(context));
            }

            frame.prependRule(new Declaration(name, new Expression(varargs).eval(context)));
          } else {
            val = arg && arg.value;

            if (val) {
              // This was a mixin call, pass in a detached ruleset of it's eval'd rules
              if (Array.isArray(val)) {
                val = new DetachedRuleset(new Ruleset('', val));
              } else {
                val = val.eval(context);
              }
            } else if (params[i].value) {
              val = params[i].value.eval(mixinEnv);
              frame.resetCache();
            } else {
              throw {
                type: 'Runtime',
                message: `wrong number of arguments for ${this.name} (${argsLength} for ${this.arity})`
              };
            }

            frame.prependRule(new Declaration(name, val));
            evaldArguments[i] = val;
          }
        }

        if (params[i].variadic && args) {
          for (j = argIndex; j < argsLength; j++) {
            evaldArguments[j] = args[j].value.eval(context);
          }
        }

        argIndex++;
      }

      return frame;
    }
  }, {
    key: "makeImportant",
    value: function makeImportant() {
      var rules = !this.rules ? this.rules : this.rules.map(function (r) {
        if (r.makeImportant) {
          return r.makeImportant(true);
        } else {
          return r;
        }
      });
      var result = new Definition(this.name, this.params, rules, this.condition, this.variadic, this.frames);
      return result;
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      return new Definition(this.name, this.params, this.rules, this.condition, this.variadic, this.frames || copyArray(context.frames));
    }
  }, {
    key: "evalCall",
    value: function evalCall(context, args, important) {
      var _arguments = [];
      var mixinFrames = this.frames ? this.frames.concat(context.frames) : context.frames;
      var frame = this.evalParams(context, new contexts.Eval(context, mixinFrames), args, _arguments);
      var rules;
      var ruleset;
      frame.prependRule(new Declaration('@arguments', new Expression(_arguments).eval(context)));
      rules = copyArray(this.rules);
      ruleset = new Ruleset(null, rules);
      ruleset.originalRuleset = this;
      ruleset = ruleset.eval(new contexts.Eval(context, [this, frame].concat(mixinFrames)));

      if (important) {
        ruleset = ruleset.makeImportant();
      }

      return ruleset;
    }
  }, {
    key: "matchCondition",
    value: function matchCondition(args, context) {
      if (this.condition && !this.condition.eval(new contexts.Eval(context, [this.evalParams(context,
      /* the parameter variables */
      new contexts.Eval(context, this.frames ? this.frames.concat(context.frames) : context.frames), args, [])].concat(this.frames || []) // the parent namespace/mixin frames
      .concat(context.frames)))) {
        // the current environment frames
        return false;
      }

      return true;
    }
  }, {
    key: "matchArgs",
    value: function matchArgs(args, context) {
      var allArgsCnt = args && args.length || 0;
      var len;
      var optionalParameters = this.optionalParameters;
      var requiredArgsCnt = !args ? 0 : args.reduce(function (count, p) {
        if (optionalParameters.indexOf(p.name) < 0) {
          return count + 1;
        } else {
          return count;
        }
      }, 0);

      if (!this.variadic) {
        if (requiredArgsCnt < this.required) {
          return false;
        }

        if (allArgsCnt > this.params.length) {
          return false;
        }
      } else {
        if (requiredArgsCnt < this.required - 1) {
          return false;
        }
      } // check patterns


      len = Math.min(requiredArgsCnt, this.arity);

      for (var i = 0; i < len; i++) {
        if (!this.params[i].name && !this.params[i].variadic) {
          if (args[i].value.eval(context).toCSS() != this.params[i].value.eval(context).toCSS()) {
            return false;
          }
        }
      }

      return true;
    }
  }]);

  return Definition;
}(Ruleset);

Definition.prototype.type = 'MixinDefinition';
Definition.prototype.evalFirst = true;

var MixinCall =
/*#__PURE__*/
function (_Node) {
  _inherits(MixinCall, _Node);

  function MixinCall(elements, args, index, currentFileInfo, important) {
    var _this;

    _classCallCheck(this, MixinCall);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(MixinCall).call(this));
    _this.selector = new Selector(elements);
    _this.arguments = args || [];
    _this._index = index;
    _this._fileInfo = currentFileInfo;
    _this.important = important;
    _this.allowRoot = true;

    _this.setParent(_this.selector, _assertThisInitialized(_this));

    return _this;
  }

  _createClass(MixinCall, [{
    key: "accept",
    value: function accept(visitor) {
      if (this.selector) {
        this.selector = visitor.visit(this.selector);
      }

      if (this.arguments.length) {
        this.arguments = visitor.visitArray(this.arguments);
      }
    }
  }, {
    key: "eval",
    value: function _eval(context) {
      var mixins;
      var mixin;
      var mixinPath;
      var args = [];
      var arg;
      var argValue;
      var rules = [];
      var match = false;
      var i;
      var m;
      var f;
      var isRecursive;
      var isOneFound;
      var candidates = [];
      var candidate;
      var conditionResult = [];
      var defaultResult;
      var defFalseEitherCase = -1;
      var defNone = 0;
      var defTrue = 1;
      var defFalse = 2;
      var count;
      var originalRuleset;
      var noArgumentsFilter;
      this.selector = this.selector.eval(context);

      function calcDefGroup(mixin, mixinPath) {
        var f;
        var p;
        var namespace;

        for (f = 0; f < 2; f++) {
          conditionResult[f] = true;
          defaultFunc.value(f);

          for (p = 0; p < mixinPath.length && conditionResult[f]; p++) {
            namespace = mixinPath[p];

            if (namespace.matchCondition) {
              conditionResult[f] = conditionResult[f] && namespace.matchCondition(null, context);
            }
          }

          if (mixin.matchCondition) {
            conditionResult[f] = conditionResult[f] && mixin.matchCondition(args, context);
          }
        }

        if (conditionResult[0] || conditionResult[1]) {
          if (conditionResult[0] != conditionResult[1]) {
            return conditionResult[1] ? defTrue : defFalse;
          }

          return defNone;
        }

        return defFalseEitherCase;
      }

      for (i = 0; i < this.arguments.length; i++) {
        arg = this.arguments[i];
        argValue = arg.value.eval(context);

        if (arg.expand && Array.isArray(argValue.value)) {
          argValue = argValue.value;

          for (m = 0; m < argValue.length; m++) {
            args.push({
              value: argValue[m]
            });
          }
        } else {
          args.push({
            name: arg.name,
            value: argValue
          });
        }
      }

      noArgumentsFilter = function noArgumentsFilter(rule) {
        return rule.matchArgs(null, context);
      };

      for (i = 0; i < context.frames.length; i++) {
        if ((mixins = context.frames[i].find(this.selector, null, noArgumentsFilter)).length > 0) {
          isOneFound = true; // To make `default()` function independent of definition order we have two "subpasses" here.
          // At first we evaluate each guard *twice* (with `default() == true` and `default() == false`),
          // and build candidate list with corresponding flags. Then, when we know all possible matches,
          // we make a final decision.

          for (m = 0; m < mixins.length; m++) {
            mixin = mixins[m].rule;
            mixinPath = mixins[m].path;
            isRecursive = false;

            for (f = 0; f < context.frames.length; f++) {
              if (!(mixin instanceof Definition) && mixin === (context.frames[f].originalRuleset || context.frames[f])) {
                isRecursive = true;
                break;
              }
            }

            if (isRecursive) {
              continue;
            }

            if (mixin.matchArgs(args, context)) {
              candidate = {
                mixin,
                group: calcDefGroup(mixin, mixinPath)
              };

              if (candidate.group !== defFalseEitherCase) {
                candidates.push(candidate);
              }

              match = true;
            }
          }

          defaultFunc.reset();
          count = [0, 0, 0];

          for (m = 0; m < candidates.length; m++) {
            count[candidates[m].group]++;
          }

          if (count[defNone] > 0) {
            defaultResult = defFalse;
          } else {
            defaultResult = defTrue;

            if (count[defTrue] + count[defFalse] > 1) {
              throw {
                type: 'Runtime',
                message: `Ambiguous use of \`default()\` found when matching for \`${this.format(args)}\``,
                index: this.getIndex(),
                filename: this.fileInfo().filename
              };
            }
          }

          for (m = 0; m < candidates.length; m++) {
            candidate = candidates[m].group;

            if (candidate === defNone || candidate === defaultResult) {
              try {
                mixin = candidates[m].mixin;

                if (!(mixin instanceof Definition)) {
                  originalRuleset = mixin.originalRuleset || mixin;
                  mixin = new Definition('', [], mixin.rules, null, false, null, originalRuleset.visibilityInfo());
                  mixin.originalRuleset = originalRuleset;
                }

                var newRules = mixin.evalCall(context, args, this.important).rules;

                this._setVisibilityToReplacement(newRules);

                Array.prototype.push.apply(rules, newRules);
              } catch (e) {
                throw {
                  message: e.message,
                  index: this.getIndex(),
                  filename: this.fileInfo().filename,
                  stack: e.stack
                };
              }
            }
          }

          if (match) {
            return rules;
          }
        }
      }

      if (isOneFound) {
        throw {
          type: 'Runtime',
          message: `No matching definition was found for \`${this.format(args)}\``,
          index: this.getIndex(),
          filename: this.fileInfo().filename
        };
      } else {
        throw {
          type: 'Name',
          message: `${this.selector.toCSS().trim()} is undefined`,
          index: this.getIndex(),
          filename: this.fileInfo().filename
        };
      }
    }
  }, {
    key: "_setVisibilityToReplacement",
    value: function _setVisibilityToReplacement(replacement) {
      var i;
      var rule;

      if (this.blocksVisibility()) {
        for (i = 0; i < replacement.length; i++) {
          rule = replacement[i];
          rule.addVisibilityBlock();
        }
      }
    }
  }, {
    key: "format",
    value: function format(args) {
      return `${this.selector.toCSS().trim()}(${args ? args.map(function (a) {
        var argValue = '';

        if (a.name) {
          argValue += `${a.name}:`;
        }

        if (a.value.toCSS) {
          argValue += a.value.toCSS();
        } else {
          argValue += '???';
        }

        return argValue;
      }).join(', ') : ''})`;
    }
  }]);

  return MixinCall;
}(Node);

MixinCall.prototype.type = 'MixinCall';

var tree = {
  Node,
  Color,
  AtRule,
  DetachedRuleset,
  Operation,
  Dimension,
  Unit,
  Keyword,
  Variable,
  Property,
  Ruleset,
  Element,
  Attribute,
  Combinator,
  Selector,
  Quoted,
  Expression,
  Declaration,
  Call,
  URL,
  Import,
  Comment,
  Anonymous,
  Value,
  JavaScript,
  Assignment,
  Condition,
  Paren,
  Media,
  UnicodeDescriptor,
  Negative,
  Extend,
  VariableCall,
  NamespaceValue,
  mixin: {
    Call: MixinCall,
    Definition: Definition
  }
};

var environment$1 =
/*#__PURE__*/
function () {
  function environment(externalEnvironment, fileManagers) {
    _classCallCheck(this, environment);

    this.fileManagers = fileManagers || [];
    externalEnvironment = externalEnvironment || {};
    var optionalFunctions = ['encodeBase64', 'mimeLookup', 'charsetLookup', 'getSourceMapGenerator'];
    var requiredFunctions = [];
    var functions = requiredFunctions.concat(optionalFunctions);

    for (var i = 0; i < functions.length; i++) {
      var propName = functions[i];
      var environmentFunc = externalEnvironment[propName];

      if (environmentFunc) {
        this[propName] = environmentFunc.bind(externalEnvironment);
      } else if (i < requiredFunctions.length) {
        this.warn(`missing required function in environment - ${propName}`);
      }
    }
  }

  _createClass(environment, [{
    key: "getFileManager",
    value: function getFileManager(filename, currentDirectory, options, environment, isSync) {
      if (!filename) {
        logger.warn('getFileManager called with no filename.. Please report this issue. continuing.');
      }

      if (currentDirectory == null) {
        logger.warn('getFileManager called with null directory.. Please report this issue. continuing.');
      }

      var fileManagers = this.fileManagers;

      if (options.pluginManager) {
        fileManagers = [].concat(fileManagers).concat(options.pluginManager.getFileManagers());
      }

      for (var i = fileManagers.length - 1; i >= 0; i--) {
        var fileManager = fileManagers[i];

        if (fileManager[isSync ? 'supportsSync' : 'supports'](filename, currentDirectory, options, environment)) {
          return fileManager;
        }
      }

      return null;
    }
  }, {
    key: "addFileManager",
    value: function addFileManager(fileManager) {
      this.fileManagers.push(fileManager);
    }
  }, {
    key: "clearFileManagers",
    value: function clearFileManagers() {
      this.fileManagers = [];
    }
  }]);

  return environment;
}();

var AbstractPluginLoader =
/*#__PURE__*/
function () {
  function AbstractPluginLoader() {
    _classCallCheck(this, AbstractPluginLoader);

    // Implemented by Node.js plugin loader
    this.require = function () {
      return null;
    };
  }

  _createClass(AbstractPluginLoader, [{
    key: "evalPlugin",
    value: function evalPlugin(contents, context, imports, pluginOptions, fileInfo) {
      var loader;
      var registry;
      var pluginObj;
      var localModule;
      var pluginManager;
      var filename;
      var result;
      pluginManager = context.pluginManager;

      if (fileInfo) {
        if (typeof fileInfo === 'string') {
          filename = fileInfo;
        } else {
          filename = fileInfo.filename;
        }
      }

      var shortname = new this.less.FileManager().extractUrlParts(filename).filename;

      if (filename) {
        pluginObj = pluginManager.get(filename);

        if (pluginObj) {
          result = this.trySetOptions(pluginObj, filename, shortname, pluginOptions);

          if (result) {
            return result;
          }

          try {
            if (pluginObj.use) {
              pluginObj.use.call(this.context, pluginObj);
            }
          } catch (e) {
            e.message = e.message || 'Error during @plugin call';
            return new LessError(e, imports, filename);
          }

          return pluginObj;
        }
      }

      localModule = {
        exports: {},
        pluginManager,
        fileInfo
      };
      registry = functionRegistry.create();

      var registerPlugin = function registerPlugin(obj) {
        pluginObj = obj;
      };

      try {
        loader = new Function('module', 'require', 'registerPlugin', 'functions', 'tree', 'less', 'fileInfo', contents);
        loader(localModule, this.require(filename), registerPlugin, registry, this.less.tree, this.less, fileInfo);
      } catch (e) {
        return new LessError(e, imports, filename);
      }

      if (!pluginObj) {
        pluginObj = localModule.exports;
      }

      pluginObj = this.validatePlugin(pluginObj, filename, shortname);

      if (pluginObj instanceof LessError) {
        return pluginObj;
      }

      if (pluginObj) {
        pluginObj.imports = imports;
        pluginObj.filename = filename; // For < 3.x (or unspecified minVersion) - setOptions() before install()

        if (!pluginObj.minVersion || this.compareVersion('3.0.0', pluginObj.minVersion) < 0) {
          result = this.trySetOptions(pluginObj, filename, shortname, pluginOptions);

          if (result) {
            return result;
          }
        } // Run on first load


        pluginManager.addPlugin(pluginObj, fileInfo.filename, registry);
        pluginObj.functions = registry.getLocalFunctions(); // Need to call setOptions again because the pluginObj might have functions

        result = this.trySetOptions(pluginObj, filename, shortname, pluginOptions);

        if (result) {
          return result;
        } // Run every @plugin call


        try {
          if (pluginObj.use) {
            pluginObj.use.call(this.context, pluginObj);
          }
        } catch (e) {
          e.message = e.message || 'Error during @plugin call';
          return new LessError(e, imports, filename);
        }
      } else {
        return new LessError({
          message: 'Not a valid plugin'
        }, imports, filename);
      }

      return pluginObj;
    }
  }, {
    key: "trySetOptions",
    value: function trySetOptions(plugin, filename, name, options) {
      if (options && !plugin.setOptions) {
        return new LessError({
          message: `Options have been provided but the plugin ${name} does not support any options.`
        });
      }

      try {
        plugin.setOptions && plugin.setOptions(options);
      } catch (e) {
        return new LessError(e);
      }
    }
  }, {
    key: "validatePlugin",
    value: function validatePlugin(plugin, filename, name) {
      if (plugin) {
        // support plugins being a function
        // so that the plugin can be more usable programmatically
        if (typeof plugin === 'function') {
          plugin = new plugin();
        }

        if (plugin.minVersion) {
          if (this.compareVersion(plugin.minVersion, this.less.version) < 0) {
            return new LessError({
              message: `Plugin ${name} requires version ${this.versionToString(plugin.minVersion)}`
            });
          }
        }

        return plugin;
      }

      return null;
    }
  }, {
    key: "compareVersion",
    value: function compareVersion(aVersion, bVersion) {
      if (typeof aVersion === 'string') {
        aVersion = aVersion.match(/^(\d+)\.?(\d+)?\.?(\d+)?/);
        aVersion.shift();
      }

      for (var i = 0; i < aVersion.length; i++) {
        if (aVersion[i] !== bVersion[i]) {
          return parseInt(aVersion[i]) > parseInt(bVersion[i]) ? -1 : 1;
        }
      }

      return 0;
    }
  }, {
    key: "versionToString",
    value: function versionToString(version) {
      var versionString = '';

      for (var i = 0; i < version.length; i++) {
        versionString += (versionString ? '.' : '') + version[i];
      }

      return versionString;
    }
  }, {
    key: "printUsage",
    value: function printUsage(plugins) {
      for (var i = 0; i < plugins.length; i++) {
        var plugin = plugins[i];

        if (plugin.printUsage) {
          plugin.printUsage();
        }
      }
    }
  }]);

  return AbstractPluginLoader;
}();

var _visitArgs = {
  visitDeeper: true
};
var _hasIndexed = false;

function _noop(node) {
  return node;
}

function indexNodeTypes(parent, ticker) {
  // add .typeIndex to tree node types for lookup table
  var key;
  var child;

  for (key in parent) {
    /* eslint guard-for-in: 0 */
    child = parent[key];

    switch (typeof child) {
      case 'function':
        // ignore bound functions directly on tree which do not have a prototype
        // or aren't nodes
        if (child.prototype && child.prototype.type) {
          child.prototype.typeIndex = ticker++;
        }

        break;

      case 'object':
        ticker = indexNodeTypes(child, ticker);
        break;
    }
  }

  return ticker;
}

var Visitor =
/*#__PURE__*/
function () {
  function Visitor(implementation) {
    _classCallCheck(this, Visitor);

    this._implementation = implementation;
    this._visitInCache = {};
    this._visitOutCache = {};

    if (!_hasIndexed) {
      indexNodeTypes(tree, 1);
      _hasIndexed = true;
    }
  }

  _createClass(Visitor, [{
    key: "visit",
    value: function visit(node) {
      if (!node) {
        return node;
      }

      var nodeTypeIndex = node.typeIndex;

      if (!nodeTypeIndex) {
        // MixinCall args aren't a node type?
        if (node.value && node.value.typeIndex) {
          this.visit(node.value);
        }

        return node;
      }

      var impl = this._implementation;
      var func = this._visitInCache[nodeTypeIndex];
      var funcOut = this._visitOutCache[nodeTypeIndex];
      var visitArgs = _visitArgs;
      var fnName;
      visitArgs.visitDeeper = true;

      if (!func) {
        fnName = `visit${node.type}`;
        func = impl[fnName] || _noop;
        funcOut = impl[`${fnName}Out`] || _noop;
        this._visitInCache[nodeTypeIndex] = func;
        this._visitOutCache[nodeTypeIndex] = funcOut;
      }

      if (func !== _noop) {
        var newNode = func.call(impl, node, visitArgs);

        if (node && impl.isReplacing) {
          node = newNode;
        }
      }

      if (visitArgs.visitDeeper && node && node.accept) {
        node.accept(this);
      }

      if (funcOut != _noop) {
        funcOut.call(impl, node);
      }

      return node;
    }
  }, {
    key: "visitArray",
    value: function visitArray(nodes, nonReplacing) {
      if (!nodes) {
        return nodes;
      }

      var cnt = nodes.length;
      var i; // Non-replacing

      if (nonReplacing || !this._implementation.isReplacing) {
        for (i = 0; i < cnt; i++) {
          this.visit(nodes[i]);
        }

        return nodes;
      } // Replacing


      var out = [];

      for (i = 0; i < cnt; i++) {
        var evald = this.visit(nodes[i]);

        if (evald === undefined) {
          continue;
        }

        if (!evald.splice) {
          out.push(evald);
        } else if (evald.length) {
          this.flatten(evald, out);
        }
      }

      return out;
    }
  }, {
    key: "flatten",
    value: function flatten(arr, out) {
      if (!out) {
        out = [];
      }

      var cnt;
      var i;
      var item;
      var nestedCnt;
      var j;
      var nestedItem;

      for (i = 0, cnt = arr.length; i < cnt; i++) {
        item = arr[i];

        if (item === undefined) {
          continue;
        }

        if (!item.splice) {
          out.push(item);
          continue;
        }

        for (j = 0, nestedCnt = item.length; j < nestedCnt; j++) {
          nestedItem = item[j];

          if (nestedItem === undefined) {
            continue;
          }

          if (!nestedItem.splice) {
            out.push(nestedItem);
          } else if (nestedItem.length) {
            this.flatten(nestedItem, out);
          }
        }
      }

      return out;
    }
  }]);

  return Visitor;
}();

var ImportSequencer =
/*#__PURE__*/
function () {
  function ImportSequencer(onSequencerEmpty) {
    _classCallCheck(this, ImportSequencer);

    this.imports = [];
    this.variableImports = [];
    this._onSequencerEmpty = onSequencerEmpty;
    this._currentDepth = 0;
  }

  _createClass(ImportSequencer, [{
    key: "addImport",
    value: function addImport(callback) {
      var importSequencer = this;
      var importItem = {
        callback,
        args: null,
        isReady: false
      };
      this.imports.push(importItem);
      return function () {
        for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
          args[_key] = arguments[_key];
        }

        importItem.args = Array.prototype.slice.call(args, 0);
        importItem.isReady = true;
        importSequencer.tryRun();
      };
    }
  }, {
    key: "addVariableImport",
    value: function addVariableImport(callback) {
      this.variableImports.push(callback);
    }
  }, {
    key: "tryRun",
    value: function tryRun() {
      this._currentDepth++;

      try {
        while (true) {
          while (this.imports.length > 0) {
            var importItem = this.imports[0];

            if (!importItem.isReady) {
              return;
            }

            this.imports = this.imports.slice(1);
            importItem.callback.apply(null, importItem.args);
          }

          if (this.variableImports.length === 0) {
            break;
          }

          var variableImport = this.variableImports[0];
          this.variableImports = this.variableImports.slice(1);
          variableImport();
        }
      } finally {
        this._currentDepth--;
      }

      if (this._currentDepth === 0 && this._onSequencerEmpty) {
        this._onSequencerEmpty();
      }
    }
  }]);

  return ImportSequencer;
}();

var ImportVisitor = function ImportVisitor(importer, finish) {
  this._visitor = new Visitor(this);
  this._importer = importer;
  this._finish = finish;
  this.context = new contexts.Eval();
  this.importCount = 0;
  this.onceFileDetectionMap = {};
  this.recursionDetector = {};
  this._sequencer = new ImportSequencer(this._onSequencerEmpty.bind(this));
};

ImportVisitor.prototype = {
  isReplacing: false,
  run: function run(root) {
    try {
      // process the contents
      this._visitor.visit(root);
    } catch (e) {
      this.error = e;
    }

    this.isFinished = true;

    this._sequencer.tryRun();
  },
  _onSequencerEmpty: function _onSequencerEmpty() {
    if (!this.isFinished) {
      return;
    }

    this._finish(this.error);
  },
  visitImport: function visitImport(importNode, visitArgs) {
    var inlineCSS = importNode.options.inline;

    if (!importNode.css || inlineCSS) {
      var context = new contexts.Eval(this.context, copyArray(this.context.frames));
      var importParent = context.frames[0];
      this.importCount++;

      if (importNode.isVariableImport()) {
        this._sequencer.addVariableImport(this.processImportNode.bind(this, importNode, context, importParent));
      } else {
        this.processImportNode(importNode, context, importParent);
      }
    }

    visitArgs.visitDeeper = false;
  },
  processImportNode: function processImportNode(importNode, context, importParent) {
    var evaldImportNode;
    var inlineCSS = importNode.options.inline;

    try {
      evaldImportNode = importNode.evalForImport(context);
    } catch (e) {
      if (!e.filename) {
        e.index = importNode.getIndex();
        e.filename = importNode.fileInfo().filename;
      } // attempt to eval properly and treat as css


      importNode.css = true; // if that fails, this error will be thrown

      importNode.error = e;
    }

    if (evaldImportNode && (!evaldImportNode.css || inlineCSS)) {
      if (evaldImportNode.options.multiple) {
        context.importMultiple = true;
      } // try appending if we haven't determined if it is css or not


      var tryAppendLessExtension = evaldImportNode.css === undefined;

      for (var i = 0; i < importParent.rules.length; i++) {
        if (importParent.rules[i] === importNode) {
          importParent.rules[i] = evaldImportNode;
          break;
        }
      }

      var onImported = this.onImported.bind(this, evaldImportNode, context);

      var sequencedOnImported = this._sequencer.addImport(onImported);

      this._importer.push(evaldImportNode.getPath(), tryAppendLessExtension, evaldImportNode.fileInfo(), evaldImportNode.options, sequencedOnImported);
    } else {
      this.importCount--;

      if (this.isFinished) {
        this._sequencer.tryRun();
      }
    }
  },
  onImported: function onImported(importNode, context, e, root, importedAtRoot, fullPath) {
    if (e) {
      if (!e.filename) {
        e.index = importNode.getIndex();
        e.filename = importNode.fileInfo().filename;
      }

      this.error = e;
    }

    var importVisitor = this;
    var inlineCSS = importNode.options.inline;
    var isPlugin = importNode.options.isPlugin;
    var isOptional = importNode.options.optional;
    var duplicateImport = importedAtRoot || fullPath in importVisitor.recursionDetector;

    if (!context.importMultiple) {
      if (duplicateImport) {
        importNode.skip = true;
      } else {
        importNode.skip = function () {
          if (fullPath in importVisitor.onceFileDetectionMap) {
            return true;
          }

          importVisitor.onceFileDetectionMap[fullPath] = true;
          return false;
        };
      }
    }

    if (!fullPath && isOptional) {
      importNode.skip = true;
    }

    if (root) {
      importNode.root = root;
      importNode.importedFilename = fullPath;

      if (!inlineCSS && !isPlugin && (context.importMultiple || !duplicateImport)) {
        importVisitor.recursionDetector[fullPath] = true;
        var oldContext = this.context;
        this.context = context;

        try {
          this._visitor.visit(root);
        } catch (e) {
          this.error = e;
        }

        this.context = oldContext;
      }
    }

    importVisitor.importCount--;

    if (importVisitor.isFinished) {
      importVisitor._sequencer.tryRun();
    }
  },
  visitDeclaration: function visitDeclaration(declNode, visitArgs) {
    if (declNode.value.type === 'DetachedRuleset') {
      this.context.frames.unshift(declNode);
    } else {
      visitArgs.visitDeeper = false;
    }
  },
  visitDeclarationOut: function visitDeclarationOut(declNode) {
    if (declNode.value.type === 'DetachedRuleset') {
      this.context.frames.shift();
    }
  },
  visitAtRule: function visitAtRule(atRuleNode, visitArgs) {
    this.context.frames.unshift(atRuleNode);
  },
  visitAtRuleOut: function visitAtRuleOut(atRuleNode) {
    this.context.frames.shift();
  },
  visitMixinDefinition: function visitMixinDefinition(mixinDefinitionNode, visitArgs) {
    this.context.frames.unshift(mixinDefinitionNode);
  },
  visitMixinDefinitionOut: function visitMixinDefinitionOut(mixinDefinitionNode) {
    this.context.frames.shift();
  },
  visitRuleset: function visitRuleset(rulesetNode, visitArgs) {
    this.context.frames.unshift(rulesetNode);
  },
  visitRulesetOut: function visitRulesetOut(rulesetNode) {
    this.context.frames.shift();
  },
  visitMedia: function visitMedia(mediaNode, visitArgs) {
    this.context.frames.unshift(mediaNode.rules[0]);
  },
  visitMediaOut: function visitMediaOut(mediaNode) {
    this.context.frames.shift();
  }
};

var SetTreeVisibilityVisitor =
/*#__PURE__*/
function () {
  function SetTreeVisibilityVisitor(visible) {
    _classCallCheck(this, SetTreeVisibilityVisitor);

    this.visible = visible;
  }

  _createClass(SetTreeVisibilityVisitor, [{
    key: "run",
    value: function run(root) {
      this.visit(root);
    }
  }, {
    key: "visitArray",
    value: function visitArray(nodes) {
      if (!nodes) {
        return nodes;
      }

      var cnt = nodes.length;
      var i;

      for (i = 0; i < cnt; i++) {
        this.visit(nodes[i]);
      }

      return nodes;
    }
  }, {
    key: "visit",
    value: function visit(node) {
      if (!node) {
        return node;
      }

      if (node.constructor === Array) {
        return this.visitArray(node);
      }

      if (!node.blocksVisibility || node.blocksVisibility()) {
        return node;
      }

      if (this.visible) {
        node.ensureVisibility();
      } else {
        node.ensureInvisibility();
      }

      node.accept(this);
      return node;
    }
  }]);

  return SetTreeVisibilityVisitor;
}();

/* jshint loopfunc:true */

var ExtendFinderVisitor =
/*#__PURE__*/
function () {
  function ExtendFinderVisitor() {
    _classCallCheck(this, ExtendFinderVisitor);

    this._visitor = new Visitor(this);
    this.contexts = [];
    this.allExtendsStack = [[]];
  }

  _createClass(ExtendFinderVisitor, [{
    key: "run",
    value: function run(root) {
      root = this._visitor.visit(root);
      root.allExtends = this.allExtendsStack[0];
      return root;
    }
  }, {
    key: "visitDeclaration",
    value: function visitDeclaration(declNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitMixinDefinition",
    value: function visitMixinDefinition(mixinDefinitionNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitRuleset",
    value: function visitRuleset(rulesetNode, visitArgs) {
      if (rulesetNode.root) {
        return;
      }

      var i;
      var j;
      var extend;
      var allSelectorsExtendList = [];
      var extendList; // get &:extend(.a); rules which apply to all selectors in this ruleset

      var rules = rulesetNode.rules;
      var ruleCnt = rules ? rules.length : 0;

      for (i = 0; i < ruleCnt; i++) {
        if (rulesetNode.rules[i] instanceof tree.Extend) {
          allSelectorsExtendList.push(rules[i]);
          rulesetNode.extendOnEveryPath = true;
        }
      } // now find every selector and apply the extends that apply to all extends
      // and the ones which apply to an individual extend


      var paths = rulesetNode.paths;

      for (i = 0; i < paths.length; i++) {
        var selectorPath = paths[i];
        var selector = selectorPath[selectorPath.length - 1];
        var selExtendList = selector.extendList;
        extendList = selExtendList ? copyArray(selExtendList).concat(allSelectorsExtendList) : allSelectorsExtendList;

        if (extendList) {
          extendList = extendList.map(function (allSelectorsExtend) {
            return allSelectorsExtend.clone();
          });
        }

        for (j = 0; j < extendList.length; j++) {
          this.foundExtends = true;
          extend = extendList[j];
          extend.findSelfSelectors(selectorPath);
          extend.ruleset = rulesetNode;

          if (j === 0) {
            extend.firstExtendOnThisSelectorPath = true;
          }

          this.allExtendsStack[this.allExtendsStack.length - 1].push(extend);
        }
      }

      this.contexts.push(rulesetNode.selectors);
    }
  }, {
    key: "visitRulesetOut",
    value: function visitRulesetOut(rulesetNode) {
      if (!rulesetNode.root) {
        this.contexts.length = this.contexts.length - 1;
      }
    }
  }, {
    key: "visitMedia",
    value: function visitMedia(mediaNode, visitArgs) {
      mediaNode.allExtends = [];
      this.allExtendsStack.push(mediaNode.allExtends);
    }
  }, {
    key: "visitMediaOut",
    value: function visitMediaOut(mediaNode) {
      this.allExtendsStack.length = this.allExtendsStack.length - 1;
    }
  }, {
    key: "visitAtRule",
    value: function visitAtRule(atRuleNode, visitArgs) {
      atRuleNode.allExtends = [];
      this.allExtendsStack.push(atRuleNode.allExtends);
    }
  }, {
    key: "visitAtRuleOut",
    value: function visitAtRuleOut(atRuleNode) {
      this.allExtendsStack.length = this.allExtendsStack.length - 1;
    }
  }]);

  return ExtendFinderVisitor;
}();

var ProcessExtendsVisitor =
/*#__PURE__*/
function () {
  function ProcessExtendsVisitor() {
    _classCallCheck(this, ProcessExtendsVisitor);

    this._visitor = new Visitor(this);
  }

  _createClass(ProcessExtendsVisitor, [{
    key: "run",
    value: function run(root) {
      var extendFinder = new ExtendFinderVisitor();
      this.extendIndices = {};
      extendFinder.run(root);

      if (!extendFinder.foundExtends) {
        return root;
      }

      root.allExtends = root.allExtends.concat(this.doExtendChaining(root.allExtends, root.allExtends));
      this.allExtendsStack = [root.allExtends];

      var newRoot = this._visitor.visit(root);

      this.checkExtendsForNonMatched(root.allExtends);
      return newRoot;
    }
  }, {
    key: "checkExtendsForNonMatched",
    value: function checkExtendsForNonMatched(extendList) {
      var indices = this.extendIndices;
      extendList.filter(function (extend) {
        return !extend.hasFoundMatches && extend.parent_ids.length == 1;
      }).forEach(function (extend) {
        var selector = '_unknown_';

        try {
          selector = extend.selector.toCSS({});
        } catch (_) {}

        if (!indices[`${extend.index} ${selector}`]) {
          indices[`${extend.index} ${selector}`] = true;
          logger.warn(`extend '${selector}' has no matches`);
        }
      });
    }
  }, {
    key: "doExtendChaining",
    value: function doExtendChaining(extendsList, extendsListTarget, iterationCount) {
      //
      // chaining is different from normal extension.. if we extend an extend then we are not just copying, altering
      // and pasting the selector we would do normally, but we are also adding an extend with the same target selector
      // this means this new extend can then go and alter other extends
      //
      // this method deals with all the chaining work - without it, extend is flat and doesn't work on other extend selectors
      // this is also the most expensive.. and a match on one selector can cause an extension of a selector we had already
      // processed if we look at each selector at a time, as is done in visitRuleset
      var extendIndex;
      var targetExtendIndex;
      var matches;
      var extendsToAdd = [];
      var newSelector;
      var extendVisitor = this;
      var selectorPath;
      var extend;
      var targetExtend;
      var newExtend;
      iterationCount = iterationCount || 0; // loop through comparing every extend with every target extend.
      // a target extend is the one on the ruleset we are looking at copy/edit/pasting in place
      // e.g.  .a:extend(.b) {}  and .b:extend(.c) {} then the first extend extends the second one
      // and the second is the target.
      // the separation into two lists allows us to process a subset of chains with a bigger set, as is the
      // case when processing media queries

      for (extendIndex = 0; extendIndex < extendsList.length; extendIndex++) {
        for (targetExtendIndex = 0; targetExtendIndex < extendsListTarget.length; targetExtendIndex++) {
          extend = extendsList[extendIndex];
          targetExtend = extendsListTarget[targetExtendIndex]; // look for circular references

          if (extend.parent_ids.indexOf(targetExtend.object_id) >= 0) {
            continue;
          } // find a match in the target extends self selector (the bit before :extend)


          selectorPath = [targetExtend.selfSelectors[0]];
          matches = extendVisitor.findMatch(extend, selectorPath);

          if (matches.length) {
            extend.hasFoundMatches = true; // we found a match, so for each self selector..

            extend.selfSelectors.forEach(function (selfSelector) {
              var info = targetExtend.visibilityInfo(); // process the extend as usual

              newSelector = extendVisitor.extendSelector(matches, selectorPath, selfSelector, extend.isVisible()); // but now we create a new extend from it

              newExtend = new tree.Extend(targetExtend.selector, targetExtend.option, 0, targetExtend.fileInfo(), info);
              newExtend.selfSelectors = newSelector; // add the extend onto the list of extends for that selector

              newSelector[newSelector.length - 1].extendList = [newExtend]; // record that we need to add it.

              extendsToAdd.push(newExtend);
              newExtend.ruleset = targetExtend.ruleset; // remember its parents for circular references

              newExtend.parent_ids = newExtend.parent_ids.concat(targetExtend.parent_ids, extend.parent_ids); // only process the selector once.. if we have :extend(.a,.b) then multiple
              // extends will look at the same selector path, so when extending
              // we know that any others will be duplicates in terms of what is added to the css

              if (targetExtend.firstExtendOnThisSelectorPath) {
                newExtend.firstExtendOnThisSelectorPath = true;
                targetExtend.ruleset.paths.push(newSelector);
              }
            });
          }
        }
      }

      if (extendsToAdd.length) {
        // try to detect circular references to stop a stack overflow.
        // may no longer be needed.
        this.extendChainCount++;

        if (iterationCount > 100) {
          var selectorOne = '{unable to calculate}';
          var selectorTwo = '{unable to calculate}';

          try {
            selectorOne = extendsToAdd[0].selfSelectors[0].toCSS();
            selectorTwo = extendsToAdd[0].selector.toCSS();
          } catch (e) {}

          throw {
            message: `extend circular reference detected. One of the circular extends is currently:${selectorOne}:extend(${selectorTwo})`
          };
        } // now process the new extends on the existing rules so that we can handle a extending b extending c extending
        // d extending e...


        return extendsToAdd.concat(extendVisitor.doExtendChaining(extendsToAdd, extendsListTarget, iterationCount + 1));
      } else {
        return extendsToAdd;
      }
    }
  }, {
    key: "visitDeclaration",
    value: function visitDeclaration(ruleNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitMixinDefinition",
    value: function visitMixinDefinition(mixinDefinitionNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitSelector",
    value: function visitSelector(selectorNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitRuleset",
    value: function visitRuleset(rulesetNode, visitArgs) {
      if (rulesetNode.root) {
        return;
      }

      var matches;
      var pathIndex;
      var extendIndex;
      var allExtends = this.allExtendsStack[this.allExtendsStack.length - 1];
      var selectorsToAdd = [];
      var extendVisitor = this;
      var selectorPath; // look at each selector path in the ruleset, find any extend matches and then copy, find and replace

      for (extendIndex = 0; extendIndex < allExtends.length; extendIndex++) {
        for (pathIndex = 0; pathIndex < rulesetNode.paths.length; pathIndex++) {
          selectorPath = rulesetNode.paths[pathIndex]; // extending extends happens initially, before the main pass

          if (rulesetNode.extendOnEveryPath) {
            continue;
          }

          var extendList = selectorPath[selectorPath.length - 1].extendList;

          if (extendList && extendList.length) {
            continue;
          }

          matches = this.findMatch(allExtends[extendIndex], selectorPath);

          if (matches.length) {
            allExtends[extendIndex].hasFoundMatches = true;
            allExtends[extendIndex].selfSelectors.forEach(function (selfSelector) {
              var extendedSelectors;
              extendedSelectors = extendVisitor.extendSelector(matches, selectorPath, selfSelector, allExtends[extendIndex].isVisible());
              selectorsToAdd.push(extendedSelectors);
            });
          }
        }
      }

      rulesetNode.paths = rulesetNode.paths.concat(selectorsToAdd);
    }
  }, {
    key: "findMatch",
    value: function findMatch(extend, haystackSelectorPath) {
      //
      // look through the haystack selector path to try and find the needle - extend.selector
      // returns an array of selector matches that can then be replaced
      //
      var haystackSelectorIndex;
      var hackstackSelector;
      var hackstackElementIndex;
      var haystackElement;
      var targetCombinator;
      var i;
      var extendVisitor = this;
      var needleElements = extend.selector.elements;
      var potentialMatches = [];
      var potentialMatch;
      var matches = []; // loop through the haystack elements

      for (haystackSelectorIndex = 0; haystackSelectorIndex < haystackSelectorPath.length; haystackSelectorIndex++) {
        hackstackSelector = haystackSelectorPath[haystackSelectorIndex];

        for (hackstackElementIndex = 0; hackstackElementIndex < hackstackSelector.elements.length; hackstackElementIndex++) {
          haystackElement = hackstackSelector.elements[hackstackElementIndex]; // if we allow elements before our match we can add a potential match every time. otherwise only at the first element.

          if (extend.allowBefore || haystackSelectorIndex === 0 && hackstackElementIndex === 0) {
            potentialMatches.push({
              pathIndex: haystackSelectorIndex,
              index: hackstackElementIndex,
              matched: 0,
              initialCombinator: haystackElement.combinator
            });
          }

          for (i = 0; i < potentialMatches.length; i++) {
            potentialMatch = potentialMatches[i]; // selectors add " " onto the first element. When we use & it joins the selectors together, but if we don't
            // then each selector in haystackSelectorPath has a space before it added in the toCSS phase. so we need to
            // work out what the resulting combinator will be

            targetCombinator = haystackElement.combinator.value;

            if (targetCombinator === '' && hackstackElementIndex === 0) {
              targetCombinator = ' ';
            } // if we don't match, null our match to indicate failure


            if (!extendVisitor.isElementValuesEqual(needleElements[potentialMatch.matched].value, haystackElement.value) || potentialMatch.matched > 0 && needleElements[potentialMatch.matched].combinator.value !== targetCombinator) {
              potentialMatch = null;
            } else {
              potentialMatch.matched++;
            } // if we are still valid and have finished, test whether we have elements after and whether these are allowed


            if (potentialMatch) {
              potentialMatch.finished = potentialMatch.matched === needleElements.length;

              if (potentialMatch.finished && !extend.allowAfter && (hackstackElementIndex + 1 < hackstackSelector.elements.length || haystackSelectorIndex + 1 < haystackSelectorPath.length)) {
                potentialMatch = null;
              }
            } // if null we remove, if not, we are still valid, so either push as a valid match or continue


            if (potentialMatch) {
              if (potentialMatch.finished) {
                potentialMatch.length = needleElements.length;
                potentialMatch.endPathIndex = haystackSelectorIndex;
                potentialMatch.endPathElementIndex = hackstackElementIndex + 1; // index after end of match

                potentialMatches.length = 0; // we don't allow matches to overlap, so start matching again

                matches.push(potentialMatch);
              }
            } else {
              potentialMatches.splice(i, 1);
              i--;
            }
          }
        }
      }

      return matches;
    }
  }, {
    key: "isElementValuesEqual",
    value: function isElementValuesEqual(elementValue1, elementValue2) {
      if (typeof elementValue1 === 'string' || typeof elementValue2 === 'string') {
        return elementValue1 === elementValue2;
      }

      if (elementValue1 instanceof tree.Attribute) {
        if (elementValue1.op !== elementValue2.op || elementValue1.key !== elementValue2.key) {
          return false;
        }

        if (!elementValue1.value || !elementValue2.value) {
          if (elementValue1.value || elementValue2.value) {
            return false;
          }

          return true;
        }

        elementValue1 = elementValue1.value.value || elementValue1.value;
        elementValue2 = elementValue2.value.value || elementValue2.value;
        return elementValue1 === elementValue2;
      }

      elementValue1 = elementValue1.value;
      elementValue2 = elementValue2.value;

      if (elementValue1 instanceof tree.Selector) {
        if (!(elementValue2 instanceof tree.Selector) || elementValue1.elements.length !== elementValue2.elements.length) {
          return false;
        }

        for (var i = 0; i < elementValue1.elements.length; i++) {
          if (elementValue1.elements[i].combinator.value !== elementValue2.elements[i].combinator.value) {
            if (i !== 0 || (elementValue1.elements[i].combinator.value || ' ') !== (elementValue2.elements[i].combinator.value || ' ')) {
              return false;
            }
          }

          if (!this.isElementValuesEqual(elementValue1.elements[i].value, elementValue2.elements[i].value)) {
            return false;
          }
        }

        return true;
      }

      return false;
    }
  }, {
    key: "extendSelector",
    value: function extendSelector(matches, selectorPath, replacementSelector, isVisible) {
      // for a set of matches, replace each match with the replacement selector
      var currentSelectorPathIndex = 0;
      var currentSelectorPathElementIndex = 0;
      var path = [];
      var matchIndex;
      var selector;
      var firstElement;
      var match;
      var newElements;

      for (matchIndex = 0; matchIndex < matches.length; matchIndex++) {
        match = matches[matchIndex];
        selector = selectorPath[match.pathIndex];
        firstElement = new tree.Element(match.initialCombinator, replacementSelector.elements[0].value, replacementSelector.elements[0].isVariable, replacementSelector.elements[0].getIndex(), replacementSelector.elements[0].fileInfo());

        if (match.pathIndex > currentSelectorPathIndex && currentSelectorPathElementIndex > 0) {
          path[path.length - 1].elements = path[path.length - 1].elements.concat(selectorPath[currentSelectorPathIndex].elements.slice(currentSelectorPathElementIndex));
          currentSelectorPathElementIndex = 0;
          currentSelectorPathIndex++;
        }

        newElements = selector.elements.slice(currentSelectorPathElementIndex, match.index).concat([firstElement]).concat(replacementSelector.elements.slice(1));

        if (currentSelectorPathIndex === match.pathIndex && matchIndex > 0) {
          path[path.length - 1].elements = path[path.length - 1].elements.concat(newElements);
        } else {
          path = path.concat(selectorPath.slice(currentSelectorPathIndex, match.pathIndex));
          path.push(new tree.Selector(newElements));
        }

        currentSelectorPathIndex = match.endPathIndex;
        currentSelectorPathElementIndex = match.endPathElementIndex;

        if (currentSelectorPathElementIndex >= selectorPath[currentSelectorPathIndex].elements.length) {
          currentSelectorPathElementIndex = 0;
          currentSelectorPathIndex++;
        }
      }

      if (currentSelectorPathIndex < selectorPath.length && currentSelectorPathElementIndex > 0) {
        path[path.length - 1].elements = path[path.length - 1].elements.concat(selectorPath[currentSelectorPathIndex].elements.slice(currentSelectorPathElementIndex));
        currentSelectorPathIndex++;
      }

      path = path.concat(selectorPath.slice(currentSelectorPathIndex, selectorPath.length));
      path = path.map(function (currentValue) {
        // we can re-use elements here, because the visibility property matters only for selectors
        var derived = currentValue.createDerived(currentValue.elements);

        if (isVisible) {
          derived.ensureVisibility();
        } else {
          derived.ensureInvisibility();
        }

        return derived;
      });
      return path;
    }
  }, {
    key: "visitMedia",
    value: function visitMedia(mediaNode, visitArgs) {
      var newAllExtends = mediaNode.allExtends.concat(this.allExtendsStack[this.allExtendsStack.length - 1]);
      newAllExtends = newAllExtends.concat(this.doExtendChaining(newAllExtends, mediaNode.allExtends));
      this.allExtendsStack.push(newAllExtends);
    }
  }, {
    key: "visitMediaOut",
    value: function visitMediaOut(mediaNode) {
      var lastIndex = this.allExtendsStack.length - 1;
      this.allExtendsStack.length = lastIndex;
    }
  }, {
    key: "visitAtRule",
    value: function visitAtRule(atRuleNode, visitArgs) {
      var newAllExtends = atRuleNode.allExtends.concat(this.allExtendsStack[this.allExtendsStack.length - 1]);
      newAllExtends = newAllExtends.concat(this.doExtendChaining(newAllExtends, atRuleNode.allExtends));
      this.allExtendsStack.push(newAllExtends);
    }
  }, {
    key: "visitAtRuleOut",
    value: function visitAtRuleOut(atRuleNode) {
      var lastIndex = this.allExtendsStack.length - 1;
      this.allExtendsStack.length = lastIndex;
    }
  }]);

  return ProcessExtendsVisitor;
}();

var JoinSelectorVisitor =
/*#__PURE__*/
function () {
  function JoinSelectorVisitor() {
    _classCallCheck(this, JoinSelectorVisitor);

    this.contexts = [[]];
    this._visitor = new Visitor(this);
  }

  _createClass(JoinSelectorVisitor, [{
    key: "run",
    value: function run(root) {
      return this._visitor.visit(root);
    }
  }, {
    key: "visitDeclaration",
    value: function visitDeclaration(declNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitMixinDefinition",
    value: function visitMixinDefinition(mixinDefinitionNode, visitArgs) {
      visitArgs.visitDeeper = false;
    }
  }, {
    key: "visitRuleset",
    value: function visitRuleset(rulesetNode, visitArgs) {
      var context = this.contexts[this.contexts.length - 1];
      var paths = [];
      var selectors;
      this.contexts.push(paths);

      if (!rulesetNode.root) {
        selectors = rulesetNode.selectors;

        if (selectors) {
          selectors = selectors.filter(function (selector) {
            return selector.getIsOutput();
          });
          rulesetNode.selectors = selectors.length ? selectors : selectors = null;

          if (selectors) {
            rulesetNode.joinSelectors(paths, context, selectors);
          }
        }

        if (!selectors) {
          rulesetNode.rules = null;
        }

        rulesetNode.paths = paths;
      }
    }
  }, {
    key: "visitRulesetOut",
    value: function visitRulesetOut(rulesetNode) {
      this.contexts.length = this.contexts.length - 1;
    }
  }, {
    key: "visitMedia",
    value: function visitMedia(mediaNode, visitArgs) {
      var context = this.contexts[this.contexts.length - 1];
      mediaNode.rules[0].root = context.length === 0 || context[0].multiMedia;
    }
  }, {
    key: "visitAtRule",
    value: function visitAtRule(atRuleNode, visitArgs) {
      var context = this.contexts[this.contexts.length - 1];

      if (atRuleNode.rules && atRuleNode.rules.length) {
        atRuleNode.rules[0].root = atRuleNode.isRooted || context.length === 0 || null;
      }
    }
  }]);

  return JoinSelectorVisitor;
}();

var CSSVisitorUtils =
/*#__PURE__*/
function () {
  function CSSVisitorUtils(context) {
    _classCallCheck(this, CSSVisitorUtils);

    this._visitor = new Visitor(this);
    this._context = context;
  }

  _createClass(CSSVisitorUtils, [{
    key: "containsSilentNonBlockedChild",
    value: function containsSilentNonBlockedChild(bodyRules) {
      var rule;

      if (!bodyRules) {
        return false;
      }

      for (var r = 0; r < bodyRules.length; r++) {
        rule = bodyRules[r];

        if (rule.isSilent && rule.isSilent(this._context) && !rule.blocksVisibility()) {
          // the atrule contains something that was referenced (likely by extend)
          // therefore it needs to be shown in output too
          return true;
        }
      }

      return false;
    }
  }, {
    key: "keepOnlyVisibleChilds",
    value: function keepOnlyVisibleChilds(owner) {
      if (owner && owner.rules) {
        owner.rules = owner.rules.filter(function (thing) {
          return thing.isVisible();
        });
      }
    }
  }, {
    key: "isEmpty",
    value: function isEmpty(owner) {
      return owner && owner.rules ? owner.rules.length === 0 : true;
    }
  }, {
    key: "hasVisibleSelector",
    value: function hasVisibleSelector(rulesetNode) {
      return rulesetNode && rulesetNode.paths ? rulesetNode.paths.length > 0 : false;
    }
  }, {
    key: "resolveVisibility",
    value: function resolveVisibility(node, originalRules) {
      if (!node.blocksVisibility()) {
        if (this.isEmpty(node) && !this.containsSilentNonBlockedChild(originalRules)) {
          return;
        }

        return node;
      }

      var compiledRulesBody = node.rules[0];
      this.keepOnlyVisibleChilds(compiledRulesBody);

      if (this.isEmpty(compiledRulesBody)) {
        return;
      }

      node.ensureVisibility();
      node.removeVisibilityBlock();
      return node;
    }
  }, {
    key: "isVisibleRuleset",
    value: function isVisibleRuleset(rulesetNode) {
      if (rulesetNode.firstRoot) {
        return true;
      }

      if (this.isEmpty(rulesetNode)) {
        return false;
      }

      if (!rulesetNode.root && !this.hasVisibleSelector(rulesetNode)) {
        return false;
      }

      return true;
    }
  }]);

  return CSSVisitorUtils;
}();

var ToCSSVisitor = function ToCSSVisitor(context) {
  this._visitor = new Visitor(this);
  this._context = context;
  this.utils = new CSSVisitorUtils(context);
};

ToCSSVisitor.prototype = {
  isReplacing: true,
  run: function run(root) {
    return this._visitor.visit(root);
  },
  visitDeclaration: function visitDeclaration(declNode, visitArgs) {
    if (declNode.blocksVisibility() || declNode.variable) {
      return;
    }

    return declNode;
  },
  visitMixinDefinition: function visitMixinDefinition(mixinNode, visitArgs) {
    // mixin definitions do not get eval'd - this means they keep state
    // so we have to clear that state here so it isn't used if toCSS is called twice
    mixinNode.frames = [];
  },
  visitExtend: function visitExtend(extendNode, visitArgs) {},
  visitComment: function visitComment(commentNode, visitArgs) {
    if (commentNode.blocksVisibility() || commentNode.isSilent(this._context)) {
      return;
    }

    return commentNode;
  },
  visitMedia: function visitMedia(mediaNode, visitArgs) {
    var originalRules = mediaNode.rules[0].rules;
    mediaNode.accept(this._visitor);
    visitArgs.visitDeeper = false;
    return this.utils.resolveVisibility(mediaNode, originalRules);
  },
  visitImport: function visitImport(importNode, visitArgs) {
    if (importNode.blocksVisibility()) {
      return;
    }

    return importNode;
  },
  visitAtRule: function visitAtRule(atRuleNode, visitArgs) {
    if (atRuleNode.rules && atRuleNode.rules.length) {
      return this.visitAtRuleWithBody(atRuleNode, visitArgs);
    } else {
      return this.visitAtRuleWithoutBody(atRuleNode, visitArgs);
    }
  },
  visitAnonymous: function visitAnonymous(anonymousNode, visitArgs) {
    if (!anonymousNode.blocksVisibility()) {
      anonymousNode.accept(this._visitor);
      return anonymousNode;
    }
  },
  visitAtRuleWithBody: function visitAtRuleWithBody(atRuleNode, visitArgs) {
    // if there is only one nested ruleset and that one has no path, then it is
    // just fake ruleset
    function hasFakeRuleset(atRuleNode) {
      var bodyRules = atRuleNode.rules;
      return bodyRules.length === 1 && (!bodyRules[0].paths || bodyRules[0].paths.length === 0);
    }

    function getBodyRules(atRuleNode) {
      var nodeRules = atRuleNode.rules;

      if (hasFakeRuleset(atRuleNode)) {
        return nodeRules[0].rules;
      }

      return nodeRules;
    } // it is still true that it is only one ruleset in array
    // this is last such moment
    // process childs


    var originalRules = getBodyRules(atRuleNode);
    atRuleNode.accept(this._visitor);
    visitArgs.visitDeeper = false;

    if (!this.utils.isEmpty(atRuleNode)) {
      this._mergeRules(atRuleNode.rules[0].rules);
    }

    return this.utils.resolveVisibility(atRuleNode, originalRules);
  },
  visitAtRuleWithoutBody: function visitAtRuleWithoutBody(atRuleNode, visitArgs) {
    if (atRuleNode.blocksVisibility()) {
      return;
    }

    if (atRuleNode.name === '@charset') {
      // Only output the debug info together with subsequent @charset definitions
      // a comment (or @media statement) before the actual @charset atrule would
      // be considered illegal css as it has to be on the first line
      if (this.charset) {
        if (atRuleNode.debugInfo) {
          var comment = new tree.Comment(`/* ${atRuleNode.toCSS(this._context).replace(/\n/g, '')} */\n`);
          comment.debugInfo = atRuleNode.debugInfo;
          return this._visitor.visit(comment);
        }

        return;
      }

      this.charset = true;
    }

    return atRuleNode;
  },
  checkValidNodes: function checkValidNodes(rules, isRoot) {
    if (!rules) {
      return;
    }

    for (var i = 0; i < rules.length; i++) {
      var ruleNode = rules[i];

      if (isRoot && ruleNode instanceof tree.Declaration && !ruleNode.variable) {
        throw {
          message: 'Properties must be inside selector blocks. They cannot be in the root',
          index: ruleNode.getIndex(),
          filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename
        };
      }

      if (ruleNode instanceof tree.Call) {
        throw {
          message: `Function '${ruleNode.name}' is undefined`,
          index: ruleNode.getIndex(),
          filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename
        };
      }

      if (ruleNode.type && !ruleNode.allowRoot) {
        throw {
          message: `${ruleNode.type} node returned by a function is not valid here`,
          index: ruleNode.getIndex(),
          filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename
        };
      }
    }
  },
  visitRuleset: function visitRuleset(rulesetNode, visitArgs) {
    // at this point rulesets are nested into each other
    var rule;
    var rulesets = [];
    this.checkValidNodes(rulesetNode.rules, rulesetNode.firstRoot);

    if (!rulesetNode.root) {
      // remove invisible paths
      this._compileRulesetPaths(rulesetNode); // remove rulesets from this ruleset body and compile them separately


      var nodeRules = rulesetNode.rules;
      var nodeRuleCnt = nodeRules ? nodeRules.length : 0;

      for (var i = 0; i < nodeRuleCnt;) {
        rule = nodeRules[i];

        if (rule && rule.rules) {
          // visit because we are moving them out from being a child
          rulesets.push(this._visitor.visit(rule));
          nodeRules.splice(i, 1);
          nodeRuleCnt--;
          continue;
        }

        i++;
      } // accept the visitor to remove rules and refactor itself
      // then we can decide nogw whether we want it or not
      // compile body


      if (nodeRuleCnt > 0) {
        rulesetNode.accept(this._visitor);
      } else {
        rulesetNode.rules = null;
      }

      visitArgs.visitDeeper = false;
    } else {
      // if (! rulesetNode.root) {
      rulesetNode.accept(this._visitor);
      visitArgs.visitDeeper = false;
    }

    if (rulesetNode.rules) {
      this._mergeRules(rulesetNode.rules);

      this._removeDuplicateRules(rulesetNode.rules);
    } // now decide whether we keep the ruleset


    if (this.utils.isVisibleRuleset(rulesetNode)) {
      rulesetNode.ensureVisibility();
      rulesets.splice(0, 0, rulesetNode);
    }

    if (rulesets.length === 1) {
      return rulesets[0];
    }

    return rulesets;
  },
  _compileRulesetPaths: function _compileRulesetPaths(rulesetNode) {
    if (rulesetNode.paths) {
      rulesetNode.paths = rulesetNode.paths.filter(function (p) {
        var i;

        if (p[0].elements[0].combinator.value === ' ') {
          p[0].elements[0].combinator = new tree.Combinator('');
        }

        for (i = 0; i < p.length; i++) {
          if (p[i].isVisible() && p[i].getIsOutput()) {
            return true;
          }
        }

        return false;
      });
    }
  },
  _removeDuplicateRules: function _removeDuplicateRules(rules) {
    if (!rules) {
      return;
    } // remove duplicates


    var ruleCache = {};
    var ruleList;
    var rule;
    var i;

    for (i = rules.length - 1; i >= 0; i--) {
      rule = rules[i];

      if (rule instanceof tree.Declaration) {
        if (!ruleCache[rule.name]) {
          ruleCache[rule.name] = rule;
        } else {
          ruleList = ruleCache[rule.name];

          if (ruleList instanceof tree.Declaration) {
            ruleList = ruleCache[rule.name] = [ruleCache[rule.name].toCSS(this._context)];
          }

          var ruleCSS = rule.toCSS(this._context);

          if (ruleList.indexOf(ruleCSS) !== -1) {
            rules.splice(i, 1);
          } else {
            ruleList.push(ruleCSS);
          }
        }
      }
    }
  },
  _mergeRules: function _mergeRules(rules) {
    if (!rules) {
      return;
    }

    var groups = {};
    var groupsArr = [];

    for (var i = 0; i < rules.length; i++) {
      var rule = rules[i];

      if (rule.merge) {
        var key = rule.name;
        groups[key] ? rules.splice(i--, 1) : groupsArr.push(groups[key] = []);
        groups[key].push(rule);
      }
    }

    groupsArr.forEach(function (group) {
      if (group.length > 0) {
        var result = group[0];
        var space = [];
        var comma = [new tree.Expression(space)];
        group.forEach(function (rule) {
          if (rule.merge === '+' && space.length > 0) {
            comma.push(new tree.Expression(space = []));
          }

          space.push(rule.value);
          result.important = result.important || rule.important;
        });
        result.value = new tree.Value(comma);
      }
    });
  }
};

var visitors = {
  Visitor,
  ImportVisitor,
  MarkVisibleSelectorsVisitor: SetTreeVisibilityVisitor,
  ExtendVisitor: ProcessExtendsVisitor,
  JoinSelectorVisitor,
  ToCSSVisitor
};

function boolean(condition) {
  return condition ? Keyword.True : Keyword.False;
}

function If(condition, trueValue, falseValue) {
  return condition ? trueValue : falseValue || new Anonymous();
}

var boolean$1 = {
  boolean,
  'if': If
};

var colorFunctions;

function clamp$1(val) {
  return Math.min(1, Math.max(0, val));
}

function hsla(origColor, hsl) {
  var color = colorFunctions.hsla(hsl.h, hsl.s, hsl.l, hsl.a);

  if (color) {
    if (origColor.value && /^(rgb|hsl)/.test(origColor.value)) {
      color.value = origColor.value;
    } else {
      color.value = 'rgb';
    }

    return color;
  }
}

function toHSL(color) {
  if (color.toHSL) {
    return color.toHSL();
  } else {
    throw new Error('Argument cannot be evaluated to a color');
  }
}

function toHSV(color) {
  if (color.toHSV) {
    return color.toHSV();
  } else {
    throw new Error('Argument cannot be evaluated to a color');
  }
}

function number(n) {
  if (n instanceof Dimension) {
    return parseFloat(n.unit.is('%') ? n.value / 100 : n.value);
  } else if (typeof n === 'number') {
    return n;
  } else {
    throw {
      type: 'Argument',
      message: 'color functions take numbers as parameters'
    };
  }
}

function scaled(n, size) {
  if (n instanceof Dimension && n.unit.is('%')) {
    return parseFloat(n.value * size / 100);
  } else {
    return number(n);
  }
}

colorFunctions = {
  rgb: function rgb(r, g, b) {
    var color = colorFunctions.rgba(r, g, b, 1.0);

    if (color) {
      color.value = 'rgb';
      return color;
    }
  },
  rgba: function rgba(r, g, b, a) {
    try {
      if (r instanceof Color) {
        if (g) {
          a = number(g);
        } else {
          a = r.alpha;
        }

        return new Color(r.rgb, a, 'rgba');
      }

      var rgb = [r, g, b].map(function (c) {
        return scaled(c, 255);
      });
      a = number(a);
      return new Color(rgb, a, 'rgba');
    } catch (e) {}
  },
  hsl: function hsl(h, s, l) {
    var color = colorFunctions.hsla(h, s, l, 1.0);

    if (color) {
      color.value = 'hsl';
      return color;
    }
  },
  hsla: function hsla(h, s, l, a) {
    try {
      if (h instanceof Color) {
        if (s) {
          a = number(s);
        } else {
          a = h.alpha;
        }

        return new Color(h.rgb, a, 'hsla');
      }

      var m1;
      var m2;

      function hue(h) {
        h = h < 0 ? h + 1 : h > 1 ? h - 1 : h;

        if (h * 6 < 1) {
          return m1 + (m2 - m1) * h * 6;
        } else if (h * 2 < 1) {
          return m2;
        } else if (h * 3 < 2) {
          return m1 + (m2 - m1) * (2 / 3 - h) * 6;
        } else {
          return m1;
        }
      }

      h = number(h) % 360 / 360;
      s = clamp$1(number(s));
      l = clamp$1(number(l));
      a = clamp$1(number(a));
      m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
      m1 = l * 2 - m2;
      var rgb = [hue(h + 1 / 3) * 255, hue(h) * 255, hue(h - 1 / 3) * 255];
      a = number(a);
      return new Color(rgb, a, 'hsla');
    } catch (e) {}
  },
  hsv: function hsv(h, s, v) {
    return colorFunctions.hsva(h, s, v, 1.0);
  },
  hsva: function hsva(h, s, v, a) {
    h = number(h) % 360 / 360 * 360;
    s = number(s);
    v = number(v);
    a = number(a);
    var i;
    var f;
    i = Math.floor(h / 60 % 6);
    f = h / 60 - i;
    var vs = [v, v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)];
    var perm = [[0, 3, 1], [2, 0, 1], [1, 0, 3], [1, 2, 0], [3, 1, 0], [0, 1, 2]];
    return colorFunctions.rgba(vs[perm[i][0]] * 255, vs[perm[i][1]] * 255, vs[perm[i][2]] * 255, a);
  },
  hue: function hue(color) {
    return new Dimension(toHSL(color).h);
  },
  saturation: function saturation(color) {
    return new Dimension(toHSL(color).s * 100, '%');
  },
  lightness: function lightness(color) {
    return new Dimension(toHSL(color).l * 100, '%');
  },
  hsvhue: function hsvhue(color) {
    return new Dimension(toHSV(color).h);
  },
  hsvsaturation: function hsvsaturation(color) {
    return new Dimension(toHSV(color).s * 100, '%');
  },
  hsvvalue: function hsvvalue(color) {
    return new Dimension(toHSV(color).v * 100, '%');
  },
  red: function red(color) {
    return new Dimension(color.rgb[0]);
  },
  green: function green(color) {
    return new Dimension(color.rgb[1]);
  },
  blue: function blue(color) {
    return new Dimension(color.rgb[2]);
  },
  alpha: function alpha(color) {
    return new Dimension(toHSL(color).a);
  },
  luma: function luma(color) {
    return new Dimension(color.luma() * color.alpha * 100, '%');
  },
  luminance: function luminance(color) {
    var luminance = 0.2126 * color.rgb[0] / 255 + 0.7152 * color.rgb[1] / 255 + 0.0722 * color.rgb[2] / 255;
    return new Dimension(luminance * color.alpha * 100, '%');
  },
  saturate: function saturate(color, amount, method) {
    // filter: saturate(3.2);
    // should be kept as is, so check for color
    if (!color.rgb) {
      return null;
    }

    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.s += hsl.s * amount.value / 100;
    } else {
      hsl.s += amount.value / 100;
    }

    hsl.s = clamp$1(hsl.s);
    return hsla(color, hsl);
  },
  desaturate: function desaturate(color, amount, method) {
    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.s -= hsl.s * amount.value / 100;
    } else {
      hsl.s -= amount.value / 100;
    }

    hsl.s = clamp$1(hsl.s);
    return hsla(color, hsl);
  },
  lighten: function lighten(color, amount, method) {
    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.l += hsl.l * amount.value / 100;
    } else {
      hsl.l += amount.value / 100;
    }

    hsl.l = clamp$1(hsl.l);
    return hsla(color, hsl);
  },
  darken: function darken(color, amount, method) {
    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.l -= hsl.l * amount.value / 100;
    } else {
      hsl.l -= amount.value / 100;
    }

    hsl.l = clamp$1(hsl.l);
    return hsla(color, hsl);
  },
  fadein: function fadein(color, amount, method) {
    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.a += hsl.a * amount.value / 100;
    } else {
      hsl.a += amount.value / 100;
    }

    hsl.a = clamp$1(hsl.a);
    return hsla(color, hsl);
  },
  fadeout: function fadeout(color, amount, method) {
    var hsl = toHSL(color);

    if (typeof method !== 'undefined' && method.value === 'relative') {
      hsl.a -= hsl.a * amount.value / 100;
    } else {
      hsl.a -= amount.value / 100;
    }

    hsl.a = clamp$1(hsl.a);
    return hsla(color, hsl);
  },
  fade: function fade(color, amount) {
    var hsl = toHSL(color);
    hsl.a = amount.value / 100;
    hsl.a = clamp$1(hsl.a);
    return hsla(color, hsl);
  },
  spin: function spin(color, amount) {
    var hsl = toHSL(color);
    var hue = (hsl.h + amount.value) % 360;
    hsl.h = hue < 0 ? 360 + hue : hue;
    return hsla(color, hsl);
  },
  //
  // Copyright (c) 2006-2009 Hampton Catlin, Natalie Weizenbaum, and Chris Eppstein
  // http://sass-lang.com
  //
  mix: function mix(color1, color2, weight) {
    if (!weight) {
      weight = new Dimension(50);
    }

    var p = weight.value / 100.0;
    var w = p * 2 - 1;
    var a = toHSL(color1).a - toHSL(color2).a;
    var w1 = ((w * a == -1 ? w : (w + a) / (1 + w * a)) + 1) / 2.0;
    var w2 = 1 - w1;
    var rgb = [color1.rgb[0] * w1 + color2.rgb[0] * w2, color1.rgb[1] * w1 + color2.rgb[1] * w2, color1.rgb[2] * w1 + color2.rgb[2] * w2];
    var alpha = color1.alpha * p + color2.alpha * (1 - p);
    return new Color(rgb, alpha);
  },
  greyscale: function greyscale(color) {
    return colorFunctions.desaturate(color, new Dimension(100));
  },
  contrast: function contrast(color, dark, light, threshold) {
    // filter: contrast(3.2);
    // should be kept as is, so check for color
    if (!color.rgb) {
      return null;
    }

    if (typeof light === 'undefined') {
      light = colorFunctions.rgba(255, 255, 255, 1.0);
    }

    if (typeof dark === 'undefined') {
      dark = colorFunctions.rgba(0, 0, 0, 1.0);
    } // Figure out which is actually light and dark:


    if (dark.luma() > light.luma()) {
      var t = light;
      light = dark;
      dark = t;
    }

    if (typeof threshold === 'undefined') {
      threshold = 0.43;
    } else {
      threshold = number(threshold);
    }

    if (color.luma() < threshold) {
      return light;
    } else {
      return dark;
    }
  },
  // Changes made in 2.7.0 - Reverted in 3.0.0
  // contrast: function (color, color1, color2, threshold) {
  //     // Return which of `color1` and `color2` has the greatest contrast with `color`
  //     // according to the standard WCAG contrast ratio calculation.
  //     // http://www.w3.org/TR/WCAG20/#contrast-ratiodef
  //     // The threshold param is no longer used, in line with SASS.
  //     // filter: contrast(3.2);
  //     // should be kept as is, so check for color
  //     if (!color.rgb) {
  //         return null;
  //     }
  //     if (typeof color1 === 'undefined') {
  //         color1 = colorFunctions.rgba(0, 0, 0, 1.0);
  //     }
  //     if (typeof color2 === 'undefined') {
  //         color2 = colorFunctions.rgba(255, 255, 255, 1.0);
  //     }
  //     var contrast1, contrast2;
  //     var luma = color.luma();
  //     var luma1 = color1.luma();
  //     var luma2 = color2.luma();
  //     // Calculate contrast ratios for each color
  //     if (luma > luma1) {
  //         contrast1 = (luma + 0.05) / (luma1 + 0.05);
  //     } else {
  //         contrast1 = (luma1 + 0.05) / (luma + 0.05);
  //     }
  //     if (luma > luma2) {
  //         contrast2 = (luma + 0.05) / (luma2 + 0.05);
  //     } else {
  //         contrast2 = (luma2 + 0.05) / (luma + 0.05);
  //     }
  //     if (contrast1 > contrast2) {
  //         return color1;
  //     } else {
  //         return color2;
  //     }
  // },
  argb: function argb(color) {
    return new Anonymous(color.toARGB());
  },
  color: function color(c) {
    if (c instanceof Quoted && /^#([A-Fa-f0-9]{8}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{3,4})$/i.test(c.value)) {
      var val = c.value.slice(1);
      return new Color(val, undefined, `#${val}`);
    }

    if (c instanceof Color || (c = Color.fromKeyword(c.value))) {
      c.value = undefined;
      return c;
    }

    throw {
      type: 'Argument',
      message: 'argument must be a color keyword or 3|4|6|8 digit hex e.g. #FFF'
    };
  },
  tint: function tint(color, amount) {
    return colorFunctions.mix(colorFunctions.rgb(255, 255, 255), color, amount);
  },
  shade: function shade(color, amount) {
    return colorFunctions.mix(colorFunctions.rgb(0, 0, 0), color, amount);
  }
};
var color = colorFunctions;

// ref: http://www.w3.org/TR/compositing-1

function colorBlend(mode, color1, color2) {
  var ab = color1.alpha; // result

  var // backdrop
  cb;
  var as = color2.alpha;
  var // source
  cs;
  var ar;
  var cr;
  var r = [];
  ar = as + ab * (1 - as);

  for (var i = 0; i < 3; i++) {
    cb = color1.rgb[i] / 255;
    cs = color2.rgb[i] / 255;
    cr = mode(cb, cs);

    if (ar) {
      cr = (as * cs + ab * (cb - as * (cb + cs - cr))) / ar;
    }

    r[i] = cr * 255;
  }

  return new Color(r, ar);
}

var colorBlendModeFunctions = {
  multiply: function multiply(cb, cs) {
    return cb * cs;
  },
  screen: function screen(cb, cs) {
    return cb + cs - cb * cs;
  },
  overlay: function overlay(cb, cs) {
    cb *= 2;
    return cb <= 1 ? colorBlendModeFunctions.multiply(cb, cs) : colorBlendModeFunctions.screen(cb - 1, cs);
  },
  softlight: function softlight(cb, cs) {
    var d = 1;
    var e = cb;

    if (cs > 0.5) {
      e = 1;
      d = cb > 0.25 ? Math.sqrt(cb) : ((16 * cb - 12) * cb + 4) * cb;
    }

    return cb - (1 - 2 * cs) * e * (d - cb);
  },
  hardlight: function hardlight(cb, cs) {
    return colorBlendModeFunctions.overlay(cs, cb);
  },
  difference: function difference(cb, cs) {
    return Math.abs(cb - cs);
  },
  exclusion: function exclusion(cb, cs) {
    return cb + cs - 2 * cb * cs;
  },
  // non-w3c functions:
  average: function average(cb, cs) {
    return (cb + cs) / 2;
  },
  negation: function negation(cb, cs) {
    return 1 - Math.abs(cb + cs - 1);
  }
};

for (var f in colorBlendModeFunctions) {
  if (colorBlendModeFunctions.hasOwnProperty(f)) {
    colorBlend[f] = colorBlend.bind(null, colorBlendModeFunctions[f]);
  }
}

var dataUri = (function (environment) {
  var fallback = function fallback(functionThis, node) {
    return new URL(node, functionThis.index, functionThis.currentFileInfo).eval(functionThis.context);
  };

  return {
    'data-uri': function dataUri(mimetypeNode, filePathNode) {
      if (!filePathNode) {
        filePathNode = mimetypeNode;
        mimetypeNode = null;
      }

      var mimetype = mimetypeNode && mimetypeNode.value;
      var filePath = filePathNode.value;
      var currentFileInfo = this.currentFileInfo;
      var currentDirectory = currentFileInfo.rewriteUrls ? currentFileInfo.currentDirectory : currentFileInfo.entryPath;
      var fragmentStart = filePath.indexOf('#');
      var fragment = '';

      if (fragmentStart !== -1) {
        fragment = filePath.slice(fragmentStart);
        filePath = filePath.slice(0, fragmentStart);
      }

      var context = clone(this.context);
      context.rawBuffer = true;
      var fileManager = environment.getFileManager(filePath, currentDirectory, context, environment, true);

      if (!fileManager) {
        return fallback(this, filePathNode);
      }

      var useBase64 = false; // detect the mimetype if not given

      if (!mimetypeNode) {
        mimetype = environment.mimeLookup(filePath);

        if (mimetype === 'image/svg+xml') {
          useBase64 = false;
        } else {
          // use base 64 unless it's an ASCII or UTF-8 format
          var charset = environment.charsetLookup(mimetype);
          useBase64 = ['US-ASCII', 'UTF-8'].indexOf(charset) < 0;
        }

        if (useBase64) {
          mimetype += ';base64';
        }
      } else {
        useBase64 = /;base64$/.test(mimetype);
      }

      var fileSync = fileManager.loadFileSync(filePath, currentDirectory, context, environment);

      if (!fileSync.contents) {
        logger.warn(`Skipped data-uri embedding of ${filePath} because file not found`);
        return fallback(this, filePathNode || mimetypeNode);
      }

      var buf = fileSync.contents;

      if (useBase64 && !environment.encodeBase64) {
        return fallback(this, filePathNode);
      }

      buf = useBase64 ? environment.encodeBase64(buf) : encodeURIComponent(buf);
      var uri = `data:${mimetype},${buf}${fragment}`;
      return new URL(new Quoted(`"${uri}"`, uri, false, this.index, this.currentFileInfo), this.index, this.currentFileInfo);
    }
  };
});

var getItemsFromNode = function getItemsFromNode(node) {
  // handle non-array values as an array of length 1
  // return 'undefined' if index is invalid
  var items = Array.isArray(node.value) ? node.value : Array(node);
  return items;
};

var list = {
  _SELF: function _SELF(n) {
    return n;
  },
  extract: function extract(values, index) {
    // (1-based index)
    index = index.value - 1;
    return getItemsFromNode(values)[index];
  },
  length: function length(values) {
    return new Dimension(getItemsFromNode(values).length);
  },

  /**
   * Creates a Less list of incremental values.
   * Modeled after Lodash's range function, also exists natively in PHP
   * 
   * @param {Dimension} [start=1]
   * @param {Dimension} end  - e.g. 10 or 10px - unit is added to output
   * @param {Dimension} [step=1] 
   */
  range: function range(start, end, step) {
    var from;
    var to;
    var stepValue = 1;
    var list = [];

    if (end) {
      to = end;
      from = start.value;

      if (step) {
        stepValue = step.value;
      }
    } else {
      from = 1;
      to = start;
    }

    for (var i = from; i <= to.value; i += stepValue) {
      list.push(new Dimension(i, to.unit));
    }

    return new Expression(list);
  },
  each: function each(list, rs) {
    var rules = [];
    var newRules;
    var iterator;

    if (list.value && !(list instanceof Quoted)) {
      if (Array.isArray(list.value)) {
        iterator = list.value;
      } else {
        iterator = [list.value];
      }
    } else if (list.ruleset) {
      iterator = list.ruleset.rules;
    } else if (list.rules) {
      iterator = list.rules;
    } else if (Array.isArray(list)) {
      iterator = list;
    } else {
      iterator = [list];
    }

    var valueName = '@value';
    var keyName = '@key';
    var indexName = '@index';

    if (rs.params) {
      valueName = rs.params[0] && rs.params[0].name;
      keyName = rs.params[1] && rs.params[1].name;
      indexName = rs.params[2] && rs.params[2].name;
      rs = rs.rules;
    } else {
      rs = rs.ruleset;
    }

    for (var i = 0; i < iterator.length; i++) {
      var key = void 0;
      var value = void 0;
      var item = iterator[i];

      if (item instanceof Declaration) {
        key = typeof item.name === 'string' ? item.name : item.name[0].value;
        value = item.value;
      } else {
        key = new Dimension(i + 1);
        value = item;
      }

      if (item instanceof Comment) {
        continue;
      }

      newRules = rs.rules.slice(0);

      if (valueName) {
        newRules.push(new Declaration(valueName, value, false, false, this.index, this.currentFileInfo));
      }

      if (indexName) {
        newRules.push(new Declaration(indexName, new Dimension(i + 1), false, false, this.index, this.currentFileInfo));
      }

      if (keyName) {
        newRules.push(new Declaration(keyName, key, false, false, this.index, this.currentFileInfo));
      }

      rules.push(new Ruleset([new Selector([new Element("", '&')])], newRules, rs.strictImports, rs.visibilityInfo()));
    }

    return new Ruleset([new Selector([new Element("", '&')])], rules, rs.strictImports, rs.visibilityInfo()).eval(this.context);
  }
};

var MathHelper = function MathHelper(fn, unit, n) {
  if (!(n instanceof Dimension)) {
    throw {
      type: 'Argument',
      message: 'argument must be a number'
    };
  }

  if (unit == null) {
    unit = n.unit;
  } else {
    n = n.unify();
  }

  return new Dimension(fn(parseFloat(n.value)), unit);
};

var mathFunctions = {
  // name,  unit
  ceil: null,
  floor: null,
  sqrt: null,
  abs: null,
  tan: '',
  sin: '',
  cos: '',
  atan: 'rad',
  asin: 'rad',
  acos: 'rad'
};

for (var f$1 in mathFunctions) {
  if (mathFunctions.hasOwnProperty(f$1)) {
    mathFunctions[f$1] = MathHelper.bind(null, Math[f$1], mathFunctions[f$1]);
  }
}

mathFunctions.round = function (n, f) {
  var fraction = typeof f === 'undefined' ? 0 : f.value;
  return MathHelper(function (num) {
    return num.toFixed(fraction);
  }, null, n);
};

var minMax = function minMax(isMin, args) {
  args = Array.prototype.slice.call(args);

  switch (args.length) {
    case 0:
      throw {
        type: 'Argument',
        message: 'one or more arguments required'
      };
  }

  var i; // key is the unit.toString() for unified Dimension values,

  var j;
  var current;
  var currentUnified;
  var referenceUnified;
  var unit;
  var unitStatic;
  var unitClone;
  var // elems only contains original argument values.
  order = [];
  var values = {}; // value is the index into the order array.

  for (i = 0; i < args.length; i++) {
    current = args[i];

    if (!(current instanceof Dimension)) {
      if (Array.isArray(args[i].value)) {
        Array.prototype.push.apply(args, Array.prototype.slice.call(args[i].value));
      }

      continue;
    }

    currentUnified = current.unit.toString() === '' && unitClone !== undefined ? new Dimension(current.value, unitClone).unify() : current.unify();
    unit = currentUnified.unit.toString() === '' && unitStatic !== undefined ? unitStatic : currentUnified.unit.toString();
    unitStatic = unit !== '' && unitStatic === undefined || unit !== '' && order[0].unify().unit.toString() === '' ? unit : unitStatic;
    unitClone = unit !== '' && unitClone === undefined ? current.unit.toString() : unitClone;
    j = values[''] !== undefined && unit !== '' && unit === unitStatic ? values[''] : values[unit];

    if (j === undefined) {
      if (unitStatic !== undefined && unit !== unitStatic) {
        throw {
          type: 'Argument',
          message: 'incompatible types'
        };
      }

      values[unit] = order.length;
      order.push(current);
      continue;
    }

    referenceUnified = order[j].unit.toString() === '' && unitClone !== undefined ? new Dimension(order[j].value, unitClone).unify() : order[j].unify();

    if (isMin && currentUnified.value < referenceUnified.value || !isMin && currentUnified.value > referenceUnified.value) {
      order[j] = current;
    }
  }

  if (order.length == 1) {
    return order[0];
  }

  args = order.map(function (a) {
    return a.toCSS(this.context);
  }).join(this.context.compress ? ',' : ', ');
  return new Anonymous(`${isMin ? 'min' : 'max'}(${args})`);
};

var number$1 = {
  min: function min() {
    for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return minMax(true, args);
  },
  max: function max() {
    for (var _len2 = arguments.length, args = new Array(_len2), _key2 = 0; _key2 < _len2; _key2++) {
      args[_key2] = arguments[_key2];
    }

    return minMax(false, args);
  },
  convert: function convert(val, unit) {
    return val.convertTo(unit.value);
  },
  pi: function pi() {
    return new Dimension(Math.PI);
  },
  mod: function mod(a, b) {
    return new Dimension(a.value % b.value, a.unit);
  },
  pow: function pow(x, y) {
    if (typeof x === 'number' && typeof y === 'number') {
      x = new Dimension(x);
      y = new Dimension(y);
    } else if (!(x instanceof Dimension) || !(y instanceof Dimension)) {
      throw {
        type: 'Argument',
        message: 'arguments must be numbers'
      };
    }

    return new Dimension(Math.pow(x.value, y.value), x.unit);
  },
  percentage: function percentage(n) {
    var result = MathHelper(function (num) {
      return num * 100;
    }, '%', n);
    return result;
  }
};

var string = {
  e: function e(str) {
    return new Quoted('"', str instanceof JavaScript ? str.evaluated : str.value, true);
  },
  escape: function escape(str) {
    return new Anonymous(encodeURI(str.value).replace(/=/g, '%3D').replace(/:/g, '%3A').replace(/#/g, '%23').replace(/;/g, '%3B').replace(/\(/g, '%28').replace(/\)/g, '%29'));
  },
  replace: function replace(string, pattern, replacement, flags) {
    var result = string.value;
    replacement = replacement.type === 'Quoted' ? replacement.value : replacement.toCSS();
    result = result.replace(new RegExp(pattern.value, flags ? flags.value : ''), replacement);
    return new Quoted(string.quote || '', result, string.escaped);
  },
  '%': function _(string
  /* arg, arg, ... */
  ) {
    var args = Array.prototype.slice.call(arguments, 1);
    var result = string.value;

    var _loop = function _loop(i) {
      /* jshint loopfunc:true */
      result = result.replace(/%[sda]/i, function (token) {
        var value = args[i].type === 'Quoted' && token.match(/s/i) ? args[i].value : args[i].toCSS();
        return token.match(/[A-Z]$/) ? encodeURIComponent(value) : value;
      });
    };

    for (var i = 0; i < args.length; i++) {
      _loop(i);
    }

    result = result.replace(/%%/g, '%');
    return new Quoted(string.quote || '', result, string.escaped);
  }
};

var svg = (function (environment) {
  return {
    'svg-gradient': function svgGradient(direction) {
      var stops;
      var gradientDirectionSvg;
      var gradientType = 'linear';
      var rectangleDimension = 'x="0" y="0" width="1" height="1"';
      var renderEnv = {
        compress: false
      };
      var returner;
      var directionValue = direction.toCSS(renderEnv);
      var i;
      var color;
      var position;
      var positionValue;
      var alpha;

      function throwArgumentDescriptor() {
        throw {
          type: 'Argument',
          message: 'svg-gradient expects direction, start_color [start_position], [color position,]...,' + ' end_color [end_position] or direction, color list'
        };
      }

      if (arguments.length == 2) {
        if (arguments[1].value.length < 2) {
          throwArgumentDescriptor();
        }

        stops = arguments[1].value;
      } else if (arguments.length < 3) {
        throwArgumentDescriptor();
      } else {
        stops = Array.prototype.slice.call(arguments, 1);
      }

      switch (directionValue) {
        case 'to bottom':
          gradientDirectionSvg = 'x1="0%" y1="0%" x2="0%" y2="100%"';
          break;

        case 'to right':
          gradientDirectionSvg = 'x1="0%" y1="0%" x2="100%" y2="0%"';
          break;

        case 'to bottom right':
          gradientDirectionSvg = 'x1="0%" y1="0%" x2="100%" y2="100%"';
          break;

        case 'to top right':
          gradientDirectionSvg = 'x1="0%" y1="100%" x2="100%" y2="0%"';
          break;

        case 'ellipse':
        case 'ellipse at center':
          gradientType = 'radial';
          gradientDirectionSvg = 'cx="50%" cy="50%" r="75%"';
          rectangleDimension = 'x="-50" y="-50" width="101" height="101"';
          break;

        default:
          throw {
            type: 'Argument',
            message: 'svg-gradient direction must be \'to bottom\', \'to right\',' + ' \'to bottom right\', \'to top right\' or \'ellipse at center\''
          };
      }

      returner = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1 1"><${gradientType}Gradient id="g" ${gradientDirectionSvg}>`;

      for (i = 0; i < stops.length; i += 1) {
        if (stops[i] instanceof Expression) {
          color = stops[i].value[0];
          position = stops[i].value[1];
        } else {
          color = stops[i];
          position = undefined;
        }

        if (!(color instanceof Color) || !((i === 0 || i + 1 === stops.length) && position === undefined) && !(position instanceof Dimension)) {
          throwArgumentDescriptor();
        }

        positionValue = position ? position.toCSS(renderEnv) : i === 0 ? '0%' : '100%';
        alpha = color.alpha;
        returner += `<stop offset="${positionValue}" stop-color="${color.toRGB()}"${alpha < 1 ? ` stop-opacity="${alpha}"` : ''}/>`;
      }

      returner += `</${gradientType}Gradient><rect ${rectangleDimension} fill="url(#g)" /></svg>`;
      returner = encodeURIComponent(returner);
      returner = `data:image/svg+xml,${returner}`;
      return new URL(new Quoted(`'${returner}'`, returner, false, this.index, this.currentFileInfo), this.index, this.currentFileInfo);
    }
  };
});

var isa = function isa(n, Type) {
  return n instanceof Type ? Keyword.True : Keyword.False;
};

var isunit = function isunit(n, unit) {
  if (unit === undefined) {
    throw {
      type: 'Argument',
      message: 'missing the required second argument to isunit.'
    };
  }

  unit = typeof unit.value === 'string' ? unit.value : unit;

  if (typeof unit !== 'string') {
    throw {
      type: 'Argument',
      message: 'Second argument to isunit should be a unit or a string.'
    };
  }

  return n instanceof Dimension && n.unit.is(unit) ? Keyword.True : Keyword.False;
};

var types = {
  isruleset: function isruleset(n) {
    return isa(n, DetachedRuleset);
  },
  iscolor: function iscolor(n) {
    return isa(n, Color);
  },
  isnumber: function isnumber(n) {
    return isa(n, Dimension);
  },
  isstring: function isstring(n) {
    return isa(n, Quoted);
  },
  iskeyword: function iskeyword(n) {
    return isa(n, Keyword);
  },
  isurl: function isurl(n) {
    return isa(n, URL);
  },
  ispixel: function ispixel(n) {
    return isunit(n, 'px');
  },
  ispercentage: function ispercentage(n) {
    return isunit(n, '%');
  },
  isem: function isem(n) {
    return isunit(n, 'em');
  },
  isunit,
  unit: function unit(val, _unit) {
    if (!(val instanceof Dimension)) {
      throw {
        type: 'Argument',
        message: `the first argument to unit must be a number${val instanceof Operation ? '. Have you forgotten parenthesis?' : ''}`
      };
    }

    if (_unit) {
      if (_unit instanceof Keyword) {
        _unit = _unit.value;
      } else {
        _unit = _unit.toCSS();
      }
    } else {
      _unit = '';
    }

    return new Dimension(val.value, _unit);
  },
  'get-unit': function getUnit(n) {
    return new Anonymous(n.unit);
  }
};

var Functions = (function (environment) {
  var functions = {
    functionRegistry,
    functionCaller
  }; // register functions

  functionRegistry.addMultiple(boolean$1);
  functionRegistry.add('default', defaultFunc.eval.bind(defaultFunc));
  functionRegistry.addMultiple(color);
  functionRegistry.addMultiple(colorBlend);
  functionRegistry.addMultiple(dataUri(environment));
  functionRegistry.addMultiple(list);
  functionRegistry.addMultiple(mathFunctions);
  functionRegistry.addMultiple(number$1);
  functionRegistry.addMultiple(string);
  functionRegistry.addMultiple(svg());
  functionRegistry.addMultiple(types);
  return functions;
});

var sourceMapOutput = (function (environment) {
  var SourceMapOutput =
  /*#__PURE__*/
  function () {
    function SourceMapOutput(options) {
      _classCallCheck(this, SourceMapOutput);

      this._css = [];
      this._rootNode = options.rootNode;
      this._contentsMap = options.contentsMap;
      this._contentsIgnoredCharsMap = options.contentsIgnoredCharsMap;

      if (options.sourceMapFilename) {
        this._sourceMapFilename = options.sourceMapFilename.replace(/\\/g, '/');
      }

      this._outputFilename = options.outputFilename;
      this.sourceMapURL = options.sourceMapURL;

      if (options.sourceMapBasepath) {
        this._sourceMapBasepath = options.sourceMapBasepath.replace(/\\/g, '/');
      }

      if (options.sourceMapRootpath) {
        this._sourceMapRootpath = options.sourceMapRootpath.replace(/\\/g, '/');

        if (this._sourceMapRootpath.charAt(this._sourceMapRootpath.length - 1) !== '/') {
          this._sourceMapRootpath += '/';
        }
      } else {
        this._sourceMapRootpath = '';
      }

      this._outputSourceFiles = options.outputSourceFiles;
      this._sourceMapGeneratorConstructor = environment.getSourceMapGenerator();
      this._lineNumber = 0;
      this._column = 0;
    }

    _createClass(SourceMapOutput, [{
      key: "removeBasepath",
      value: function removeBasepath(path) {
        if (this._sourceMapBasepath && path.indexOf(this._sourceMapBasepath) === 0) {
          path = path.substring(this._sourceMapBasepath.length);

          if (path.charAt(0) === '\\' || path.charAt(0) === '/') {
            path = path.substring(1);
          }
        }

        return path;
      }
    }, {
      key: "normalizeFilename",
      value: function normalizeFilename(filename) {
        filename = filename.replace(/\\/g, '/');
        filename = this.removeBasepath(filename);
        return (this._sourceMapRootpath || '') + filename;
      }
    }, {
      key: "add",
      value: function add(chunk, fileInfo, index, mapLines) {
        // ignore adding empty strings
        if (!chunk) {
          return;
        }

        var lines;
        var sourceLines;
        var columns;
        var sourceColumns;
        var i;

        if (fileInfo && fileInfo.filename) {
          var inputSource = this._contentsMap[fileInfo.filename]; // remove vars/banner added to the top of the file

          if (this._contentsIgnoredCharsMap[fileInfo.filename]) {
            // adjust the index
            index -= this._contentsIgnoredCharsMap[fileInfo.filename];

            if (index < 0) {
              index = 0;
            } // adjust the source


            inputSource = inputSource.slice(this._contentsIgnoredCharsMap[fileInfo.filename]);
          } // ignore empty content


          if (inputSource === undefined) {
            return;
          }

          inputSource = inputSource.substring(0, index);
          sourceLines = inputSource.split('\n');
          sourceColumns = sourceLines[sourceLines.length - 1];
        }

        lines = chunk.split('\n');
        columns = lines[lines.length - 1];

        if (fileInfo && fileInfo.filename) {
          if (!mapLines) {
            this._sourceMapGenerator.addMapping({
              generated: {
                line: this._lineNumber + 1,
                column: this._column
              },
              original: {
                line: sourceLines.length,
                column: sourceColumns.length
              },
              source: this.normalizeFilename(fileInfo.filename)
            });
          } else {
            for (i = 0; i < lines.length; i++) {
              this._sourceMapGenerator.addMapping({
                generated: {
                  line: this._lineNumber + i + 1,
                  column: i === 0 ? this._column : 0
                },
                original: {
                  line: sourceLines.length + i,
                  column: i === 0 ? sourceColumns.length : 0
                },
                source: this.normalizeFilename(fileInfo.filename)
              });
            }
          }
        }

        if (lines.length === 1) {
          this._column += columns.length;
        } else {
          this._lineNumber += lines.length - 1;
          this._column = columns.length;
        }

        this._css.push(chunk);
      }
    }, {
      key: "isEmpty",
      value: function isEmpty() {
        return this._css.length === 0;
      }
    }, {
      key: "toCSS",
      value: function toCSS(context) {
        this._sourceMapGenerator = new this._sourceMapGeneratorConstructor({
          file: this._outputFilename,
          sourceRoot: null
        });

        if (this._outputSourceFiles) {
          for (var filename in this._contentsMap) {
            if (this._contentsMap.hasOwnProperty(filename)) {
              var source = this._contentsMap[filename];

              if (this._contentsIgnoredCharsMap[filename]) {
                source = source.slice(this._contentsIgnoredCharsMap[filename]);
              }

              this._sourceMapGenerator.setSourceContent(this.normalizeFilename(filename), source);
            }
          }
        }

        this._rootNode.genCSS(context, this);

        if (this._css.length > 0) {
          var sourceMapURL;
          var sourceMapContent = JSON.stringify(this._sourceMapGenerator.toJSON());

          if (this.sourceMapURL) {
            sourceMapURL = this.sourceMapURL;
          } else if (this._sourceMapFilename) {
            sourceMapURL = this._sourceMapFilename;
          }

          this.sourceMapURL = sourceMapURL;
          this.sourceMap = sourceMapContent;
        }

        return this._css.join('');
      }
    }]);

    return SourceMapOutput;
  }();

  return SourceMapOutput;
});

var sourceMapBuilder = (function (SourceMapOutput, environment) {
  var SourceMapBuilder =
  /*#__PURE__*/
  function () {
    function SourceMapBuilder(options) {
      _classCallCheck(this, SourceMapBuilder);

      this.options = options;
    }

    _createClass(SourceMapBuilder, [{
      key: "toCSS",
      value: function toCSS(rootNode, options, imports) {
        var sourceMapOutput = new SourceMapOutput({
          contentsIgnoredCharsMap: imports.contentsIgnoredChars,
          rootNode,
          contentsMap: imports.contents,
          sourceMapFilename: this.options.sourceMapFilename,
          sourceMapURL: this.options.sourceMapURL,
          outputFilename: this.options.sourceMapOutputFilename,
          sourceMapBasepath: this.options.sourceMapBasepath,
          sourceMapRootpath: this.options.sourceMapRootpath,
          outputSourceFiles: this.options.outputSourceFiles,
          sourceMapGenerator: this.options.sourceMapGenerator,
          sourceMapFileInline: this.options.sourceMapFileInline
        });
        var css = sourceMapOutput.toCSS(options);
        this.sourceMap = sourceMapOutput.sourceMap;
        this.sourceMapURL = sourceMapOutput.sourceMapURL;

        if (this.options.sourceMapInputFilename) {
          this.sourceMapInputFilename = sourceMapOutput.normalizeFilename(this.options.sourceMapInputFilename);
        }

        if (this.options.sourceMapBasepath !== undefined && this.sourceMapURL !== undefined) {
          this.sourceMapURL = sourceMapOutput.removeBasepath(this.sourceMapURL);
        }

        return css + this.getCSSAppendage();
      }
    }, {
      key: "getCSSAppendage",
      value: function getCSSAppendage() {
        var sourceMapURL = this.sourceMapURL;

        if (this.options.sourceMapFileInline) {
          if (this.sourceMap === undefined) {
            return '';
          }

          sourceMapURL = `data:application/json;base64,${environment.encodeBase64(this.sourceMap)}`;
        }

        if (sourceMapURL) {
          return `/*# sourceMappingURL=${sourceMapURL} */`;
        }

        return '';
      }
    }, {
      key: "getExternalSourceMap",
      value: function getExternalSourceMap() {
        return this.sourceMap;
      }
    }, {
      key: "setExternalSourceMap",
      value: function setExternalSourceMap(sourceMap) {
        this.sourceMap = sourceMap;
      }
    }, {
      key: "isInline",
      value: function isInline() {
        return this.options.sourceMapFileInline;
      }
    }, {
      key: "getSourceMapURL",
      value: function getSourceMapURL() {
        return this.sourceMapURL;
      }
    }, {
      key: "getOutputFilename",
      value: function getOutputFilename() {
        return this.options.sourceMapOutputFilename;
      }
    }, {
      key: "getInputFilename",
      value: function getInputFilename() {
        return this.sourceMapInputFilename;
      }
    }]);

    return SourceMapBuilder;
  }();

  return SourceMapBuilder;
});

var transformTree = (function (root) {
  var options = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
  var evaldRoot;
  var variables = options.variables;
  var evalEnv = new contexts.Eval(options); //
  // Allows setting variables with a hash, so:
  //
  //   `{ color: new tree.Color('#f01') }` will become:
  //
  //   new tree.Declaration('@color',
  //     new tree.Value([
  //       new tree.Expression([
  //         new tree.Color('#f01')
  //       ])
  //     ])
  //   )
  //

  if (typeof variables === 'object' && !Array.isArray(variables)) {
    variables = Object.keys(variables).map(function (k) {
      var value = variables[k];

      if (!(value instanceof tree.Value)) {
        if (!(value instanceof tree.Expression)) {
          value = new tree.Expression([value]);
        }

        value = new tree.Value([value]);
      }

      return new tree.Declaration(`@${k}`, value, false, null, 0);
    });
    evalEnv.frames = [new tree.Ruleset(null, variables)];
  }

  var visitors$1 = [new visitors.JoinSelectorVisitor(), new visitors.MarkVisibleSelectorsVisitor(true), new visitors.ExtendVisitor(), new visitors.ToCSSVisitor({
    compress: Boolean(options.compress)
  })];
  var preEvalVisitors = [];
  var v;
  var visitorIterator;
  /**
   * first() / get() allows visitors to be added while visiting
   * 
   * @todo Add scoping for visitors just like functions for @plugin; right now they're global
   */

  if (options.pluginManager) {
    visitorIterator = options.pluginManager.visitor();

    for (var i = 0; i < 2; i++) {
      visitorIterator.first();

      while (v = visitorIterator.get()) {
        if (v.isPreEvalVisitor) {
          if (i === 0 || preEvalVisitors.indexOf(v) === -1) {
            preEvalVisitors.push(v);
            v.run(root);
          }
        } else {
          if (i === 0 || visitors$1.indexOf(v) === -1) {
            if (v.isPreVisitor) {
              visitors$1.unshift(v);
            } else {
              visitors$1.push(v);
            }
          }
        }
      }
    }
  }

  evaldRoot = root.eval(evalEnv);

  for (var i = 0; i < visitors$1.length; i++) {
    visitors$1[i].run(evaldRoot);
  } // Run any remaining visitors added after eval pass


  if (options.pluginManager) {
    visitorIterator.first();

    while (v = visitorIterator.get()) {
      if (visitors$1.indexOf(v) === -1 && preEvalVisitors.indexOf(v) === -1) {
        v.run(evaldRoot);
      }
    }
  }

  return evaldRoot;
});

var parseTree = (function (SourceMapBuilder) {
  var ParseTree =
  /*#__PURE__*/
  function () {
    function ParseTree(root, imports) {
      _classCallCheck(this, ParseTree);

      this.root = root;
      this.imports = imports;
    }

    _createClass(ParseTree, [{
      key: "toCSS",
      value: function toCSS(options) {
        var evaldRoot;
        var result = {};
        var sourceMapBuilder;

        try {
          evaldRoot = transformTree(this.root, options);
        } catch (e) {
          throw new LessError(e, this.imports);
        }

        try {
          var compress = Boolean(options.compress);

          if (compress) {
            logger.warn('The compress option has been deprecated. ' + 'We recommend you use a dedicated css minifier, for instance see less-plugin-clean-css.');
          }

          var toCSSOptions = {
            compress,
            dumpLineNumbers: options.dumpLineNumbers,
            strictUnits: Boolean(options.strictUnits),
            numPrecision: 8
          };

          if (options.sourceMap) {
            sourceMapBuilder = new SourceMapBuilder(options.sourceMap);
            result.css = sourceMapBuilder.toCSS(evaldRoot, toCSSOptions, this.imports);
          } else {
            result.css = evaldRoot.toCSS(toCSSOptions);
          }
        } catch (e) {
          throw new LessError(e, this.imports);
        }

        if (options.pluginManager) {
          var postProcessors = options.pluginManager.getPostProcessors();

          for (var i = 0; i < postProcessors.length; i++) {
            result.css = postProcessors[i].process(result.css, {
              sourceMap: sourceMapBuilder,
              options,
              imports: this.imports
            });
          }
        }

        if (options.sourceMap) {
          result.map = sourceMapBuilder.getExternalSourceMap();
        }

        result.imports = [];

        for (var file in this.imports.files) {
          if (this.imports.files.hasOwnProperty(file) && file !== this.imports.rootFilename) {
            result.imports.push(file);
          }
        }

        return result;
      }
    }]);

    return ParseTree;
  }();

  return ParseTree;
});

var importManager = (function (environment) {
  // FileInfo = {
  //  'rewriteUrls' - option - whether to adjust URL's to be relative
  //  'filename' - full resolved filename of current file
  //  'rootpath' - path to append to normal URLs for this node
  //  'currentDirectory' - path to the current file, absolute
  //  'rootFilename' - filename of the base file
  //  'entryPath' - absolute path to the entry file
  //  'reference' - whether the file should not be output and only output parts that are referenced
  var ImportManager =
  /*#__PURE__*/
  function () {
    function ImportManager(less, context, rootFileInfo) {
      _classCallCheck(this, ImportManager);

      this.less = less;
      this.rootFilename = rootFileInfo.filename;
      this.paths = context.paths || []; // Search paths, when importing

      this.contents = {}; // map - filename to contents of all the files

      this.contentsIgnoredChars = {}; // map - filename to lines at the beginning of each file to ignore

      this.mime = context.mime;
      this.error = null;
      this.context = context; // Deprecated? Unused outside of here, could be useful.

      this.queue = []; // Files which haven't been imported yet

      this.files = {}; // Holds the imported parse trees.
    }
    /**
     * Add an import to be imported
     * @param path - the raw path
     * @param tryAppendExtension - whether to try appending a file extension (.less or .js if the path has no extension)
     * @param currentFileInfo - the current file info (used for instance to work out relative paths)
     * @param importOptions - import options
     * @param callback - callback for when it is imported
     */


    _createClass(ImportManager, [{
      key: "push",
      value: function push(path, tryAppendExtension, currentFileInfo, importOptions, callback) {
        var importManager = this;
        var pluginLoader = this.context.pluginManager.Loader;
        this.queue.push(path);

        var fileParsedFunc = function fileParsedFunc(e, root, fullPath) {
          importManager.queue.splice(importManager.queue.indexOf(path), 1); // Remove the path from the queue

          var importedEqualsRoot = fullPath === importManager.rootFilename;

          if (importOptions.optional && e) {
            callback(null, {
              rules: []
            }, false, null);
            logger.info(`The file ${fullPath} was skipped because it was not found and the import was marked optional.`);
          } else {
            // Inline imports aren't cached here.
            // If we start to cache them, please make sure they won't conflict with non-inline imports of the
            // same name as they used to do before this comment and the condition below have been added.
            if (!importManager.files[fullPath] && !importOptions.inline) {
              importManager.files[fullPath] = {
                root,
                options: importOptions
              };
            }

            if (e && !importManager.error) {
              importManager.error = e;
            }

            callback(e, root, importedEqualsRoot, fullPath);
          }
        };

        var newFileInfo = {
          rewriteUrls: this.context.rewriteUrls,
          entryPath: currentFileInfo.entryPath,
          rootpath: currentFileInfo.rootpath,
          rootFilename: currentFileInfo.rootFilename
        };
        var fileManager = environment.getFileManager(path, currentFileInfo.currentDirectory, this.context, environment);

        if (!fileManager) {
          fileParsedFunc({
            message: `Could not find a file-manager for ${path}`
          });
          return;
        }

        var loadFileCallback = function loadFileCallback(loadedFile) {
          var plugin;
          var resolvedFilename = loadedFile.filename;
          var contents = loadedFile.contents.replace(/^\uFEFF/, ''); // Pass on an updated rootpath if path of imported file is relative and file
          // is in a (sub|sup) directory
          //
          // Examples:
          // - If path of imported file is 'module/nav/nav.less' and rootpath is 'less/',
          //   then rootpath should become 'less/module/nav/'
          // - If path of imported file is '../mixins.less' and rootpath is 'less/',
          //   then rootpath should become 'less/../'

          newFileInfo.currentDirectory = fileManager.getPath(resolvedFilename);

          if (newFileInfo.rewriteUrls) {
            newFileInfo.rootpath = fileManager.join(importManager.context.rootpath || '', fileManager.pathDiff(newFileInfo.currentDirectory, newFileInfo.entryPath));

            if (!fileManager.isPathAbsolute(newFileInfo.rootpath) && fileManager.alwaysMakePathsAbsolute()) {
              newFileInfo.rootpath = fileManager.join(newFileInfo.entryPath, newFileInfo.rootpath);
            }
          }

          newFileInfo.filename = resolvedFilename;
          var newEnv = new contexts.Parse(importManager.context);
          newEnv.processImports = false;
          importManager.contents[resolvedFilename] = contents;

          if (currentFileInfo.reference || importOptions.reference) {
            newFileInfo.reference = true;
          }

          if (importOptions.isPlugin) {
            plugin = pluginLoader.evalPlugin(contents, newEnv, importManager, importOptions.pluginArgs, newFileInfo);

            if (plugin instanceof LessError) {
              fileParsedFunc(plugin, null, resolvedFilename);
            } else {
              fileParsedFunc(null, plugin, resolvedFilename);
            }
          } else if (importOptions.inline) {
            fileParsedFunc(null, contents, resolvedFilename);
          } else {
            // import (multiple) parse trees apparently get altered and can't be cached.
            // TODO: investigate why this is
            if (importManager.files[resolvedFilename] && !importManager.files[resolvedFilename].options.multiple && !importOptions.multiple) {
              fileParsedFunc(null, importManager.files[resolvedFilename].root, resolvedFilename);
            } else {
              new Parser(newEnv, importManager, newFileInfo).parse(contents, function (e, root) {
                fileParsedFunc(e, root, resolvedFilename);
              });
            }
          }
        };

        var promise;
        var context = clone(this.context);

        if (tryAppendExtension) {
          context.ext = importOptions.isPlugin ? '.js' : '.less';
        }

        if (importOptions.isPlugin) {
          context.mime = 'application/javascript';
          promise = pluginLoader.loadPlugin(path, currentFileInfo.currentDirectory, context, environment, fileManager);
        } else {
          promise = fileManager.loadFile(path, currentFileInfo.currentDirectory, context, environment, function (err, loadedFile) {
            if (err) {
              fileParsedFunc(err);
            } else {
              loadFileCallback(loadedFile);
            }
          });
        }

        if (promise) {
          promise.then(loadFileCallback, fileParsedFunc);
        }
      }
    }]);

    return ImportManager;
  }();

  return ImportManager;
});

var Render = (function (environment, ParseTree, ImportManager) {
  var render = function render(input, options, callback) {
    if (typeof options === 'function') {
      callback = options;
      options = copyOptions(this.options, {});
    } else {
      options = copyOptions(this.options, options || {});
    }

    if (!callback) {
      var self = this;
      return new Promise(function (resolve, reject) {
        render.call(self, input, options, function (err, output) {
          if (err) {
            reject(err);
          } else {
            resolve(output);
          }
        });
      });
    } else {
      this.parse(input, options, function (err, root, imports, options) {
        if (err) {
          return callback(err);
        }

        var result;

        try {
          var parseTree = new ParseTree(root, imports);
          result = parseTree.toCSS(options);
        } catch (err) {
          return callback(err);
        }

        callback(null, result);
      });
    }
  };

  return render;
});

/**
 * Plugin Manager
 */
var PluginManager =
/*#__PURE__*/
function () {
  function PluginManager(less) {
    _classCallCheck(this, PluginManager);

    this.less = less;
    this.visitors = [];
    this.preProcessors = [];
    this.postProcessors = [];
    this.installedPlugins = [];
    this.fileManagers = [];
    this.iterator = -1;
    this.pluginCache = {};
    this.Loader = new less.PluginLoader(less);
  }
  /**
   * Adds all the plugins in the array
   * @param {Array} plugins
   */


  _createClass(PluginManager, [{
    key: "addPlugins",
    value: function addPlugins(plugins) {
      if (plugins) {
        for (var i = 0; i < plugins.length; i++) {
          this.addPlugin(plugins[i]);
        }
      }
    }
    /**
     *
     * @param plugin
     * @param {String} filename
     */

  }, {
    key: "addPlugin",
    value: function addPlugin(plugin, filename, functionRegistry) {
      this.installedPlugins.push(plugin);

      if (filename) {
        this.pluginCache[filename] = plugin;
      }

      if (plugin.install) {
        plugin.install(this.less, this, functionRegistry || this.less.functions.functionRegistry);
      }
    }
    /**
     *
     * @param filename
     */

  }, {
    key: "get",
    value: function get(filename) {
      return this.pluginCache[filename];
    }
    /**
     * Adds a visitor. The visitor object has options on itself to determine
     * when it should run.
     * @param visitor
     */

  }, {
    key: "addVisitor",
    value: function addVisitor(visitor) {
      this.visitors.push(visitor);
    }
    /**
     * Adds a pre processor object
     * @param {object} preProcessor
     * @param {number} priority - guidelines 1 = before import, 1000 = import, 2000 = after import
     */

  }, {
    key: "addPreProcessor",
    value: function addPreProcessor(preProcessor, priority) {
      var indexToInsertAt;

      for (indexToInsertAt = 0; indexToInsertAt < this.preProcessors.length; indexToInsertAt++) {
        if (this.preProcessors[indexToInsertAt].priority >= priority) {
          break;
        }
      }

      this.preProcessors.splice(indexToInsertAt, 0, {
        preProcessor,
        priority
      });
    }
    /**
     * Adds a post processor object
     * @param {object} postProcessor
     * @param {number} priority - guidelines 1 = before compression, 1000 = compression, 2000 = after compression
     */

  }, {
    key: "addPostProcessor",
    value: function addPostProcessor(postProcessor, priority) {
      var indexToInsertAt;

      for (indexToInsertAt = 0; indexToInsertAt < this.postProcessors.length; indexToInsertAt++) {
        if (this.postProcessors[indexToInsertAt].priority >= priority) {
          break;
        }
      }

      this.postProcessors.splice(indexToInsertAt, 0, {
        postProcessor,
        priority
      });
    }
    /**
     *
     * @param manager
     */

  }, {
    key: "addFileManager",
    value: function addFileManager(manager) {
      this.fileManagers.push(manager);
    }
    /**
     *
     * @returns {Array}
     * @private
     */

  }, {
    key: "getPreProcessors",
    value: function getPreProcessors() {
      var preProcessors = [];

      for (var i = 0; i < this.preProcessors.length; i++) {
        preProcessors.push(this.preProcessors[i].preProcessor);
      }

      return preProcessors;
    }
    /**
     *
     * @returns {Array}
     * @private
     */

  }, {
    key: "getPostProcessors",
    value: function getPostProcessors() {
      var postProcessors = [];

      for (var i = 0; i < this.postProcessors.length; i++) {
        postProcessors.push(this.postProcessors[i].postProcessor);
      }

      return postProcessors;
    }
    /**
     *
     * @returns {Array}
     * @private
     */

  }, {
    key: "getVisitors",
    value: function getVisitors() {
      return this.visitors;
    }
  }, {
    key: "visitor",
    value: function visitor() {
      var self = this;
      return {
        first: function first() {
          self.iterator = -1;
          return self.visitors[self.iterator];
        },
        get: function get() {
          self.iterator += 1;
          return self.visitors[self.iterator];
        }
      };
    }
    /**
     *
     * @returns {Array}
     * @private
     */

  }, {
    key: "getFileManagers",
    value: function getFileManagers() {
      return this.fileManagers;
    }
  }]);

  return PluginManager;
}();

var pm;

function PluginManagerFactory(less, newFactory) {
  if (newFactory || !pm) {
    pm = new PluginManager(less);
  }

  return pm;
}

var Parse = (function (environment, ParseTree, ImportManager) {
  var parse = function parse(input, options, callback) {
    if (typeof options === 'function') {
      callback = options;
      options = copyOptions(this.options, {});
    } else {
      options = copyOptions(this.options, options || {});
    }

    if (!callback) {
      var self = this;
      return new Promise(function (resolve, reject) {
        parse.call(self, input, options, function (err, output) {
          if (err) {
            reject(err);
          } else {
            resolve(output);
          }
        });
      });
    } else {
      var context;
      var rootFileInfo;
      var pluginManager = new PluginManagerFactory(this, !options.reUsePluginManager);
      options.pluginManager = pluginManager;
      context = new contexts.Parse(options);

      if (options.rootFileInfo) {
        rootFileInfo = options.rootFileInfo;
      } else {
        var filename = options.filename || 'input';
        var entryPath = filename.replace(/[^\/\\]*$/, '');
        rootFileInfo = {
          filename,
          rewriteUrls: context.rewriteUrls,
          rootpath: context.rootpath || '',
          currentDirectory: entryPath,
          entryPath,
          rootFilename: filename
        }; // add in a missing trailing slash

        if (rootFileInfo.rootpath && rootFileInfo.rootpath.slice(-1) !== '/') {
          rootFileInfo.rootpath += '/';
        }
      }

      var imports = new ImportManager(this, context, rootFileInfo);
      this.importManager = imports; // TODO: allow the plugins to be just a list of paths or names
      // Do an async plugin queue like lessc

      if (options.plugins) {
        options.plugins.forEach(function (plugin) {
          var evalResult;
          var contents;

          if (plugin.fileContent) {
            contents = plugin.fileContent.replace(/^\uFEFF/, '');
            evalResult = pluginManager.Loader.evalPlugin(contents, context, imports, plugin.options, plugin.filename);

            if (evalResult instanceof LessError) {
              return callback(evalResult);
            }
          } else {
            pluginManager.addPlugin(plugin);
          }
        });
      }

      new Parser(context, imports, rootFileInfo).parse(input, function (e, root) {
        if (e) {
          return callback(e);
        }

        callback(null, root, imports, options);
      }, options);
    }
  };

  return parse;
});

var createFromEnvironment = (function (environment, fileManagers) {
  /**
   * @todo
   * This original code could be improved quite a bit.
   * Many classes / modules currently add side-effects / mutations to passed in objects,
   * which makes it hard to refactor and reason about. 
   */
  environment = new environment$1(environment, fileManagers);
  var SourceMapOutput = sourceMapOutput(environment);
  var SourceMapBuilder = sourceMapBuilder(SourceMapOutput, environment);
  var ParseTree = parseTree(SourceMapBuilder);
  var ImportManager = importManager(environment);
  var render = Render(environment, ParseTree);
  var parse = Parse(environment, ParseTree, ImportManager);
  var functions = Functions(environment);
  /**
   * @todo
   * This root properties / methods need to be organized.
   * It's not clear what should / must be public and why.
   */

  var initial = {
    version: [3, 10, 3],
    data,
    tree,
    Environment: environment$1,
    AbstractFileManager,
    AbstractPluginLoader,
    environment,
    visitors,
    Parser,
    functions,
    contexts,
    SourceMapOutput,
    SourceMapBuilder,
    ParseTree,
    ImportManager,
    render,
    parse,
    LessError,
    transformTree,
    utils,
    PluginManager: PluginManagerFactory,
    logger
  }; // Create a public API

  var ctor = function ctor(t) {
    return function () {
      for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }

      return _construct(t, args);
    };
  };

  var t;
  var api = Object.create(initial);

  for (var n in initial.tree) {
    /* eslint guard-for-in: 0 */
    t = initial.tree[n];

    if (typeof t === 'function') {
      api[n.toLowerCase()] = ctor(t);
    } else {
      api[n] = Object.create(null);

      for (var o in t) {
        /* eslint guard-for-in: 0 */
        api[n][o.toLowerCase()] = ctor(t[o]);
      }
    }
  }

  return api;
});

function createCommonjsModule(fn, module) {
	return module = { exports: {} }, fn(module, module.exports), module.exports;
}

var lesscHelper = createCommonjsModule(function (module, exports) {
  // lessc_helper.js
  //
  //      helper functions for lessc
  var lessc_helper = {
    // Stylize a string
    stylize: function stylize(str, style) {
      var styles = {
        'reset': [0, 0],
        'bold': [1, 22],
        'inverse': [7, 27],
        'underline': [4, 24],
        'yellow': [33, 39],
        'green': [32, 39],
        'red': [31, 39],
        'grey': [90, 39]
      };
      return `\x1b[${styles[style][0]}m${str}\x1b[${styles[style][1]}m`;
    },
    // Print command line options
    printUsage: function printUsage() {
      console.log('usage: lessc [option option=parameter ...] <source> [destination]');
      console.log('');
      console.log('If source is set to `-\' (dash or hyphen-minus), input is read from stdin.');
      console.log('');
      console.log('options:');
      console.log('  -h, --help                   Prints help (this message) and exit.');
      console.log('  --include-path=PATHS         Sets include paths. Separated by `:\'. `;\' also supported on windows.');
      console.log('  -M, --depends                Outputs a makefile import dependency list to stdout.');
      console.log('  --no-color                   Disables colorized output.');
      console.log('  --ie-compat                  Enables IE8 compatibility checks.');
      console.log('  --js                         Enables inline JavaScript in less files');
      console.log('  -l, --lint                   Syntax check only (lint).');
      console.log('  -s, --silent                 Suppresses output of error messages.');
      console.log('  --strict-imports             Forces evaluation of imports.');
      console.log('  --insecure                   Allows imports from insecure https hosts.');
      console.log('  -v, --version                Prints version number and exit.');
      console.log('  --verbose                    Be verbose.');
      console.log('  --source-map[=FILENAME]      Outputs a v3 sourcemap to the filename (or output filename.map).');
      console.log('  --source-map-rootpath=X      Adds this path onto the sourcemap filename and less file paths.');
      console.log('  --source-map-basepath=X      Sets sourcemap base path, defaults to current working directory.');
      console.log('  --source-map-include-source  Puts the less files into the map instead of referencing them.');
      console.log('  --source-map-inline          Puts the map (and any less files) as a base64 data uri into the output css file.');
      console.log('  --source-map-url=URL         Sets a custom URL to map file, for sourceMappingURL comment');
      console.log('                               in generated CSS file.');
      console.log('  -rp, --rootpath=URL          Sets rootpath for url rewriting in relative imports and urls');
      console.log('                               Works with or without the relative-urls option.');
      console.log('  -ru=, --rewrite-urls=        Rewrites URLs to make them relative to the base less file.');
      console.log('    all|local|off              \'all\' rewrites all URLs, \'local\' just those starting with a \'.\'');
      console.log('');
      console.log('  -m=, --math=');
      console.log('     always                    Less will eagerly perform math operations always.');
      console.log('     parens-division           Math performed except for division (/) operator');
      console.log('     parens | strict           Math only performed inside parentheses');
      console.log('     strict-legacy             Parens required in very strict terms (legacy --strict-math)');
      console.log('');
      console.log('  -su=on|off                   Allows mixed units, e.g. 1px+1em or 1px*1px which have units');
      console.log('  --strict-units=on|off        that cannot be represented.');
      console.log('  --global-var=\'VAR=VALUE\'     Defines a variable that can be referenced by the file.');
      console.log('  --modify-var=\'VAR=VALUE\'     Modifies a variable already declared in the file.');
      console.log('  --url-args=\'QUERYSTRING\'     Adds params into url tokens (e.g. 42, cb=42 or \'a=1&b=2\')');
      console.log('  --plugin=PLUGIN=OPTIONS      Loads a plugin. You can also omit the --plugin= if the plugin begins');
      console.log('                               less-plugin. E.g. the clean css plugin is called less-plugin-clean-css');
      console.log('                               once installed (npm install less-plugin-clean-css), use either with');
      console.log('                               --plugin=less-plugin-clean-css or just --clean-css');
      console.log('                               specify options afterwards e.g. --plugin=less-plugin-clean-css="advanced"');
      console.log('                               or --clean-css="advanced"');
      console.log('');
      console.log('-------------------------- Deprecated ----------------');
      console.log('  -sm=on|off               Legacy parens-only math. Use --math');
      console.log('  --strict-math=on|off     ');
      console.log('');
      console.log('  --line-numbers=TYPE      Outputs filename and line numbers.');
      console.log('                           TYPE can be either \'comments\', which will output');
      console.log('                           the debug info within comments, \'mediaquery\'');
      console.log('                           that will output the information within a fake');
      console.log('                           media query which is compatible with the SASS');
      console.log('                           format, and \'all\' which will do both.');
      console.log('  -x, --compress           Compresses output by removing some whitespaces.');
      console.log('                           We recommend you use a dedicated minifer like less-plugin-clean-css');
      console.log('');
      console.log('Report bugs to: http://github.com/less/less.js/issues');
      console.log('Home page: <http://lesscss.org/>');
    }
  }; // Exports helper functions

  for (var h in lessc_helper) {
    if (lessc_helper.hasOwnProperty(h)) {
      exports[h] = lessc_helper[h];
    }
  }
});

/**
 * Node Plugin Loader
 */

var PluginLoader =
/*#__PURE__*/
function (_AbstractPluginLoader) {
  _inherits(PluginLoader, _AbstractPluginLoader);

  function PluginLoader(less) {
    var _this;

    _classCallCheck(this, PluginLoader);

    _this = _possibleConstructorReturn(this, _getPrototypeOf(PluginLoader).call(this));
    _this.less = less;

    _this.require = function (prefix) {
      prefix = path.dirname(prefix);
      return function (id) {
        var str = id.substr(0, 2);

        if (str === '..' || str === './') {
          return require(path.join(prefix, id));
        } else {
          return require(id);
        }
      };
    };

    return _this;
  }

  _createClass(PluginLoader, [{
    key: "loadPlugin",
    value: function loadPlugin(filename, basePath, context, environment, fileManager) {
      var prefix = filename.slice(0, 1);
      var explicit = prefix === '.' || prefix === '/' || filename.slice(-3).toLowerCase() === '.js';

      if (!explicit) {
        context.prefixes = ['less-plugin-', ''];
      }

      return new Promise(function (fulfill, reject) {
        fileManager.loadFile(filename, basePath, context, environment).then(function (data) {
          try {
            fulfill(data);
          } catch (e) {
            console.log(e);
            reject(e);
          }
        }).catch(function (err) {
          reject(err);
        });
      });
    }
  }]);

  return PluginLoader;
}(AbstractPluginLoader);

// Export a new default each time
var defaultOptions = (function () {
  return {
    /* Inline Javascript - @plugin still allowed */
    javascriptEnabled: false,

    /* Outputs a makefile import dependency list to stdout. */
    depends: false,

    /* (DEPRECATED) Compress using less built-in compression. 
    * This does an okay job but does not utilise all the tricks of 
    * dedicated css compression. */
    compress: false,

    /* Runs the less parser and just reports errors without any output. */
    lint: false,

    /* Sets available include paths.
    * If the file in an @import rule does not exist at that exact location, 
    * less will look for it at the location(s) passed to this option. 
    * You might use this for instance to specify a path to a library which 
    * you want to be referenced simply and relatively in the less files. */
    paths: [],

    /* color output in the terminal */
    color: true,

    /* The strictImports controls whether the compiler will allow an @import inside of either 
    * @media blocks or (a later addition) other selector blocks.
    * See: https://github.com/less/less.js/issues/656 */
    strictImports: false,

    /* Allow Imports from Insecure HTTPS Hosts */
    insecure: false,

    /* Allows you to add a path to every generated import and url in your css. 
    * This does not affect less import statements that are processed, just ones 
    * that are left in the output css. */
    rootpath: '',

    /* By default URLs are kept as-is, so if you import a file in a sub-directory 
    * that references an image, exactly the same URL will be output in the css. 
    * This option allows you to re-write URL's in imported files so that the 
    * URL is always relative to the base imported file */
    rewriteUrls: false,

    /* How to process math 
    *   0 always           - eagerly try to solve all operations
    *   1 parens-division  - require parens for division "/"
    *   2 parens | strict  - require parens for all operations
    *   3 strict-legacy    - legacy strict behavior (super-strict)
    */
    math: 0,

    /* Without this option, less attempts to guess at the output unit when it does maths. */
    strictUnits: false,

    /* Effectively the declaration is put at the top of your base Less file, 
    * meaning it can be used but it also can be overridden if this variable 
    * is defined in the file. */
    globalVars: null,

    /* As opposed to the global variable option, this puts the declaration at the
    * end of your base file, meaning it will override anything defined in your Less file. */
    modifyVars: null,

    /* This option allows you to specify a argument to go on to every URL.  */
    urlArgs: ''
  };
});

var imageSize = (function (environment) {
  function _imageSize(functionContext, filePathNode) {
    var filePath = filePathNode.value;
    var currentFileInfo = functionContext.currentFileInfo;
    var currentDirectory = currentFileInfo.rewriteUrls ? currentFileInfo.currentDirectory : currentFileInfo.entryPath;
    var fragmentStart = filePath.indexOf('#');
    var fragment = '';

    if (fragmentStart !== -1) {
      fragment = filePath.slice(fragmentStart);
      filePath = filePath.slice(0, fragmentStart);
    }

    var fileManager = environment.getFileManager(filePath, currentDirectory, functionContext.context, environment, true);

    if (!fileManager) {
      throw {
        type: 'File',
        message: `Can not set up FileManager for ${filePathNode}`
      };
    }

    var fileSync = fileManager.loadFileSync(filePath, currentDirectory, functionContext.context, environment);

    if (fileSync.error) {
      throw fileSync.error;
    }

    var sizeOf = require('image-size');

    return sizeOf(fileSync.filename);
  }

  var imageFunctions = {
    'image-size': function imageSize(filePathNode) {
      var size = _imageSize(this, filePathNode);

      return new Expression([new Dimension(size.width, 'px'), new Dimension(size.height, 'px')]);
    },
    'image-width': function imageWidth(filePathNode) {
      var size = _imageSize(this, filePathNode);

      return new Dimension(size.width, 'px');
    },
    'image-height': function imageHeight(filePathNode) {
      var size = _imageSize(this, filePathNode);

      return new Dimension(size.height, 'px');
    }
  };
  functionRegistry.addMultiple(imageFunctions);
});

var less = createFromEnvironment(environment, [new FileManager(), new UrlFileManager()]); // allow people to create less with their own environment

less.createFromEnvironment = createFromEnvironment;
less.lesscHelper = lesscHelper;
less.PluginLoader = PluginLoader;
less.fs = fs$1;
less.FileManager = FileManager;
less.UrlFileManager = UrlFileManager; // Set up options

less.options = defaultOptions(); // provide image-size functionality

imageSize(less.environment);

module.exports = less;
