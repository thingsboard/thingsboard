'use strict';

var tslib = require('tslib');
var path = require('path');
var url = require('url');
var CloneHelper = require('clone');

var environment = {
    encodeBase64: function encodeBase64(str) {
        // Avoid Buffer constructor on newer versions of Node.js.
        var buffer = (Buffer.from ? Buffer.from(str) : (new Buffer(str)));
        return buffer.toString('base64');
    },
    mimeLookup: function (filename) {
        return require('mime').lookup(filename);
    },
    charsetLookup: function (mime) {
        return require('mime').charsets.lookup(mime);
    },
    getSourceMapGenerator: function getSourceMapGenerator() {
        return require('source-map').SourceMapGenerator;
    }
};

var fs;
try {
    fs = require('graceful-fs');
}
catch (e) {
    fs = require('fs');
}
var fs$1 = fs;

var AbstractFileManager = /** @class */ (function () {
    function AbstractFileManager() {
    }
    AbstractFileManager.prototype.getPath = function (filename) {
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
    };
    AbstractFileManager.prototype.tryAppendExtension = function (path, ext) {
        return /(\.[a-z]*$)|([\?;].*)$/.test(path) ? path : path + ext;
    };
    AbstractFileManager.prototype.tryAppendLessExtension = function (path) {
        return this.tryAppendExtension(path, '.less');
    };
    AbstractFileManager.prototype.supportsSync = function () { return false; };
    AbstractFileManager.prototype.alwaysMakePathsAbsolute = function () { return false; };
    AbstractFileManager.prototype.isPathAbsolute = function (filename) {
        return (/^(?:[a-z-]+:|\/|\\|#)/i).test(filename);
    };
    // TODO: pull out / replace?
    AbstractFileManager.prototype.join = function (basePath, laterPath) {
        if (!basePath) {
            return laterPath;
        }
        return basePath + laterPath;
    };
    AbstractFileManager.prototype.pathDiff = function (url, baseUrl) {
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
            diff += urlDirectories[i] + "/";
        }
        return diff;
    };
    // helper function, not part of API
    AbstractFileManager.prototype.extractUrlParts = function (url, baseUrl) {
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
            throw new Error("Could not parse sheet href - '" + url + "'");
        }
        // Stylesheets in IE don't always return the full path
        if (baseUrl && (!urlParts[1] || urlParts[2])) {
            baseUrlParts = baseUrl.match(urlPartsRegex);
            if (!baseUrlParts) {
                throw new Error("Could not parse page url - '" + baseUrl + "'");
            }
            urlParts[1] = urlParts[1] || baseUrlParts[1] || '';
            if (!urlParts[2]) {
                urlParts[3] = baseUrlParts[3] + urlParts[3];
            }
        }
        if (urlParts[3]) {
            rawDirectories = urlParts[3].replace(/\\/g, '/').split('/');
            // collapse '..' and skip '.'
            for (i = 0; i < rawDirectories.length; i++) {
                if (rawDirectories[i] === '..') {
                    directories.pop();
                }
                else if (rawDirectories[i] !== '.') {
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
    };
    return AbstractFileManager;
}());

var FileManager = /** @class */ (function (_super) {
    tslib.__extends(FileManager, _super);
    function FileManager() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    FileManager.prototype.supports = function () {
        return true;
    };
    FileManager.prototype.supportsSync = function () {
        return true;
    };
    FileManager.prototype.loadFile = function (filename, currentDirectory, options, environment, callback) {
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
            paths.push.apply(paths, options.paths);
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
            }
            else {
                return result;
            }
        }
        else {
            // promise is guaranteed to be asyncronous
            // which helps as it allows the file handle
            // to be closed before it continues with the next file
            return new Promise(getFileData);
        }
        function returnData(data) {
            if (!data.filename) {
                result = { error: data };
            }
            else {
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
                                }
                                catch (e) {
                                    filenamesTried.push(npmPrefix + fullFilename);
                                    tryWithExtension();
                                }
                            }
                            else {
                                tryWithExtension();
                            }
                            function tryWithExtension() {
                                var extFilename = options.ext ? self.tryAppendExtension(fullFilename, options.ext) : fullFilename;
                                if (extFilename !== fullFilename && !explicit && paths[i] === '.') {
                                    try {
                                        fullFilename = require.resolve(extFilename);
                                        isNodeModule = true;
                                    }
                                    catch (e) {
                                        filenamesTried.push(npmPrefix + extFilename);
                                        fullFilename = extFilename;
                                    }
                                }
                                else {
                                    fullFilename = extFilename;
                                }
                            }
                            var readFileArgs = [fullFilename];
                            if (!options.rawBuffer) {
                                readFileArgs.push('utf-8');
                            }
                            if (options.syncImport) {
                                try {
                                    var data = fs$1.readFileSync.apply(this, readFileArgs);
                                    fulfill({ contents: data, filename: fullFilename });
                                }
                                catch (e) {
                                    filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                                    return tryPrefix(j + 1);
                                }
                            }
                            else {
                                readFileArgs.push(function (e, data) {
                                    if (e) {
                                        filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                                        return tryPrefix(j + 1);
                                    }
                                    fulfill({ contents: data, filename: fullFilename });
                                });
                                fs$1.readFile.apply(this, readFileArgs);
                            }
                        }
                        else {
                            tryPathIndex(i + 1);
                        }
                    })(0);
                }
                else {
                    reject({ type: 'File', message: "'" + filename + "' wasn't found. Tried - " + filenamesTried.join(',') });
                }
            }(0));
        }
    };
    FileManager.prototype.loadFileSync = function (filename, currentDirectory, options, environment) {
        options.syncImport = true;
        return this.loadFile(filename, currentDirectory, options, environment);
    };
    return FileManager;
}(AbstractFileManager));

var logger = {
    error: function (msg) {
        this._fireEvent('error', msg);
    },
    warn: function (msg) {
        this._fireEvent('warn', msg);
    },
    info: function (msg) {
        this._fireEvent('info', msg);
    },
    debug: function (msg) {
        this._fireEvent('debug', msg);
    },
    addListener: function (listener) {
        this._listeners.push(listener);
    },
    removeListener: function (listener) {
        for (var i_1 = 0; i_1 < this._listeners.length; i_1++) {
            if (this._listeners[i_1] === listener) {
                this._listeners.splice(i_1, 1);
                return;
            }
        }
    },
    _fireEvent: function (type, msg) {
        for (var i_2 = 0; i_2 < this._listeners.length; i_2++) {
            var logFunction = this._listeners[i_2][type];
            if (logFunction) {
                logFunction(msg);
            }
        }
    },
    _listeners: []
};

var isUrlRe = /^(?:https?:)?\/\//i;
var request;
var UrlFileManager = /** @class */ (function (_super) {
    tslib.__extends(UrlFileManager, _super);
    function UrlFileManager() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    UrlFileManager.prototype.supports = function (filename, currentDirectory, options, environment) {
        return isUrlRe.test(filename) || isUrlRe.test(currentDirectory);
    };
    UrlFileManager.prototype.loadFile = function (filename, currentDirectory, options, environment) {
        return new Promise(function (fulfill, reject) {
            if (request === undefined) {
                try {
                    request = require('request');
                }
                catch (e) {
                    request = null;
                }
            }
            if (!request) {
                reject({ type: 'File', message: 'optional dependency \'request\' required to import over http(s)\n' });
                return;
            }
            var urlStr = isUrlRe.test(filename) ? filename : url.resolve(currentDirectory, filename);
            var urlObj = url.parse(urlStr);
            if (!urlObj.protocol) {
                urlObj.protocol = 'http';
                urlStr = urlObj.format();
            }
            request.get({ uri: urlStr, strictSSL: !options.insecure }, function (error, res, body) {
                if (error) {
                    reject({ type: 'File', message: "resource '" + urlStr + "' gave this Error:\n  " + error + "\n" });
                    return;
                }
                if (res && res.statusCode === 404) {
                    reject({ type: 'File', message: "resource '" + urlStr + "' was not found\n" });
                    return;
                }
                if (!body) {
                    logger.warn("Warning: Empty body (HTTP " + res.statusCode + ") returned by \"" + urlStr + "\"");
                }
                fulfill({ contents: body, filename: urlStr });
            });
        });
    };
    return UrlFileManager;
}(AbstractFileManager));

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

var data = { colors: colors, unitConversions: unitConversions };

var Node = /** @class */ (function () {
    function Node() {
        this.parent = null;
        this.visibilityBlocks = undefined;
        this.nodeVisible = undefined;
        this.rootNode = null;
        this.parsed = null;
        var self = this;
        Object.defineProperty(this, 'currentFileInfo', {
            get: function () { return self.fileInfo(); }
        });
        Object.defineProperty(this, 'index', {
            get: function () { return self.getIndex(); }
        });
    }
    Node.prototype.setParent = function (nodes, parent) {
        function set(node) {
            if (node && node instanceof Node) {
                node.parent = parent;
            }
        }
        if (Array.isArray(nodes)) {
            nodes.forEach(set);
        }
        else {
            set(nodes);
        }
    };
    Node.prototype.getIndex = function () {
        return this._index || (this.parent && this.parent.getIndex()) || 0;
    };
    Node.prototype.fileInfo = function () {
        return this._fileInfo || (this.parent && this.parent.fileInfo()) || {};
    };
    Node.prototype.isRulesetLike = function () {
        return false;
    };
    Node.prototype.toCSS = function (context) {
        var strs = [];
        this.genCSS(context, {
            add: function (chunk, fileInfo, index) {
                strs.push(chunk);
            },
            isEmpty: function () {
                return strs.length === 0;
            }
        });
        return strs.join('');
    };
    Node.prototype.genCSS = function (context, output) {
        output.add(this.value);
    };
    Node.prototype.accept = function (visitor) {
        this.value = visitor.visit(this.value);
    };
    Node.prototype.eval = function () { return this; };
    Node.prototype._operate = function (context, op, a, b) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
        }
    };
    Node.prototype.fround = function (context, value) {
        var precision = context && context.numPrecision;
        // add "epsilon" to ensure numbers like 1.000000005 (represented as 1.000000004999...) are properly rounded:
        return (precision) ? Number((value + 2e-16).toFixed(precision)) : value;
    };
    // Returns true if this node represents root of ast imported by reference
    Node.prototype.blocksVisibility = function () {
        if (this.visibilityBlocks == null) {
            this.visibilityBlocks = 0;
        }
        return this.visibilityBlocks !== 0;
    };
    Node.prototype.addVisibilityBlock = function () {
        if (this.visibilityBlocks == null) {
            this.visibilityBlocks = 0;
        }
        this.visibilityBlocks = this.visibilityBlocks + 1;
    };
    Node.prototype.removeVisibilityBlock = function () {
        if (this.visibilityBlocks == null) {
            this.visibilityBlocks = 0;
        }
        this.visibilityBlocks = this.visibilityBlocks - 1;
    };
    // Turns on node visibility - if called node will be shown in output regardless
    // of whether it comes from import by reference or not
    Node.prototype.ensureVisibility = function () {
        this.nodeVisible = true;
    };
    // Turns off node visibility - if called node will NOT be shown in output regardless
    // of whether it comes from import by reference or not
    Node.prototype.ensureInvisibility = function () {
        this.nodeVisible = false;
    };
    // return values:
    // false - the node must not be visible
    // true - the node must be visible
    // undefined or null - the node has the same visibility as its parent
    Node.prototype.isVisible = function () {
        return this.nodeVisible;
    };
    Node.prototype.visibilityInfo = function () {
        return {
            visibilityBlocks: this.visibilityBlocks,
            nodeVisible: this.nodeVisible
        };
    };
    Node.prototype.copyVisibilityInfo = function (info) {
        if (!info) {
            return;
        }
        this.visibilityBlocks = info.visibilityBlocks;
        this.nodeVisible = info.nodeVisible;
    };
    return Node;
}());
Node.compare = function (a, b) {
    /* returns:
     -1: a < b
     0: a = b
     1: a > b
     and *any* other value for a != b (e.g. undefined, NaN, -2 etc.) */
    if ((a.compare) &&
        // for "symmetric results" force toCSS-based comparison
        // of Quoted or Anonymous if either value is one of those
        !(b.type === 'Quoted' || b.type === 'Anonymous')) {
        return a.compare(b);
    }
    else if (b.compare) {
        return -b.compare(a);
    }
    else if (a.type !== b.type) {
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
    for (var i_1 = 0; i_1 < a.length; i_1++) {
        if (Node.compare(a[i_1], b[i_1]) !== 0) {
            return undefined;
        }
    }
    return 0;
};
Node.numericCompare = function (a, b) { return a < b ? -1
    : a === b ? 0
        : a > b ? 1 : undefined; };

//
// RGB Colors - #ff0014, #eee
//
var Color = /** @class */ (function (_super) {
    tslib.__extends(Color, _super);
    function Color(rgb, a, originalForm) {
        var _this = _super.call(this) || this;
        var self = _this;
        //
        // The end goal here, is to parse the arguments
        // into an integer triplet, such as `128, 255, 0`
        //
        // This facilitates operations and conversions.
        //
        if (Array.isArray(rgb)) {
            _this.rgb = rgb;
        }
        else if (rgb.length >= 6) {
            _this.rgb = [];
            rgb.match(/.{2}/g).map(function (c, i) {
                if (i < 3) {
                    self.rgb.push(parseInt(c, 16));
                }
                else {
                    self.alpha = (parseInt(c, 16)) / 255;
                }
            });
        }
        else {
            _this.rgb = [];
            rgb.split('').map(function (c, i) {
                if (i < 3) {
                    self.rgb.push(parseInt(c + c, 16));
                }
                else {
                    self.alpha = (parseInt(c + c, 16)) / 255;
                }
            });
        }
        _this.alpha = _this.alpha || (typeof a === 'number' ? a : 1);
        if (typeof originalForm !== 'undefined') {
            _this.value = originalForm;
        }
        return _this;
    }
    Color.prototype.luma = function () {
        var r = this.rgb[0] / 255;
        var g = this.rgb[1] / 255;
        var b = this.rgb[2] / 255;
        r = (r <= 0.03928) ? r / 12.92 : Math.pow(((r + 0.055) / 1.055), 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow(((g + 0.055) / 1.055), 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow(((b + 0.055) / 1.055), 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    };
    Color.prototype.genCSS = function (context, output) {
        output.add(this.toCSS(context));
    };
    Color.prototype.toCSS = function (context, doNotCompress) {
        var compress = context && context.compress && !doNotCompress;
        var color;
        var alpha;
        var colorFunction;
        var args = [];
        // `value` is set if this color was originally
        // converted from a named color string so we need
        // to respect this and try to output named color too.
        alpha = this.fround(context, this.alpha);
        if (this.value) {
            if (this.value.indexOf('rgb') === 0) {
                if (alpha < 1) {
                    colorFunction = 'rgba';
                }
            }
            else if (this.value.indexOf('hsl') === 0) {
                if (alpha < 1) {
                    colorFunction = 'hsla';
                }
                else {
                    colorFunction = 'hsl';
                }
            }
            else {
                return this.value;
            }
        }
        else {
            if (alpha < 1) {
                colorFunction = 'rgba';
            }
        }
        switch (colorFunction) {
            case 'rgba':
                args = this.rgb.map(function (c) { return clamp(Math.round(c), 255); }).concat(clamp(alpha, 1));
                break;
            case 'hsla':
                args.push(clamp(alpha, 1));
            case 'hsl':
                color = this.toHSL();
                args = [
                    this.fround(context, color.h),
                    this.fround(context, color.s * 100) + "%",
                    this.fround(context, color.l * 100) + "%"
                ].concat(args);
        }
        if (colorFunction) {
            // Values are capped between `0` and `255`, rounded and zero-padded.
            return colorFunction + "(" + args.join("," + (compress ? '' : ' ')) + ")";
        }
        color = this.toRGB();
        if (compress) {
            var splitcolor = color.split('');
            // Convert color to short format
            if (splitcolor[1] === splitcolor[2] && splitcolor[3] === splitcolor[4] && splitcolor[5] === splitcolor[6]) {
                color = "#" + splitcolor[1] + splitcolor[3] + splitcolor[5];
            }
        }
        return color;
    };
    //
    // Operations have to be done per-channel, if not,
    // channels will spill onto each other. Once we have
    // our result, in the form of an integer triplet,
    // we create a new Color node to hold the result.
    //
    Color.prototype.operate = function (context, op, other) {
        var rgb = new Array(3);
        var alpha = this.alpha * (1 - other.alpha) + other.alpha;
        for (var c = 0; c < 3; c++) {
            rgb[c] = this._operate(context, op, this.rgb[c], other.rgb[c]);
        }
        return new Color(rgb, alpha);
    };
    Color.prototype.toRGB = function () {
        return toHex(this.rgb);
    };
    Color.prototype.toHSL = function () {
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
        }
        else {
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
        return { h: h * 360, s: s, l: l, a: a };
    };
    // Adapted from http://mjijackson.com/2008/02/rgb-to-hsl-and-rgb-to-hsv-color-model-conversion-algorithms-in-javascript
    Color.prototype.toHSV = function () {
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
        }
        else {
            s = d / max;
        }
        if (max === min) {
            h = 0;
        }
        else {
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
        return { h: h * 360, s: s, v: v, a: a };
    };
    Color.prototype.toARGB = function () {
        return toHex([this.alpha * 255].concat(this.rgb));
    };
    Color.prototype.compare = function (x) {
        return (x.rgb &&
            x.rgb[0] === this.rgb[0] &&
            x.rgb[1] === this.rgb[1] &&
            x.rgb[2] === this.rgb[2] &&
            x.alpha === this.alpha) ? 0 : undefined;
    };
    return Color;
}(Node));
Color.prototype.type = 'Color';
function clamp(v, max) {
    return Math.min(Math.max(v, 0), max);
}
function toHex(v) {
    return "#" + v.map(function (c) {
        c = clamp(Math.round(c), 255);
        return (c < 16 ? '0' : '') + c.toString(16);
    }).join('');
}
Color.fromKeyword = function (keyword) {
    var c;
    var key = keyword.toLowerCase();
    if (colors.hasOwnProperty(key)) {
        c = new Color(colors[key].slice(1));
    }
    else if (key === 'transparent') {
        c = new Color([0, 0, 0], 0);
    }
    if (c) {
        c.value = keyword;
        return c;
    }
};

var Paren = /** @class */ (function (_super) {
    tslib.__extends(Paren, _super);
    function Paren(node) {
        var _this = _super.call(this) || this;
        _this.value = node;
        return _this;
    }
    Paren.prototype.genCSS = function (context, output) {
        output.add('(');
        this.value.genCSS(context, output);
        output.add(')');
    };
    Paren.prototype.eval = function (context) {
        return new Paren(this.value.eval(context));
    };
    return Paren;
}(Node));
Paren.prototype.type = 'Paren';

var _noSpaceCombinators = {
    '': true,
    ' ': true,
    '|': true
};
var Combinator = /** @class */ (function (_super) {
    tslib.__extends(Combinator, _super);
    function Combinator(value) {
        var _this = _super.call(this) || this;
        if (value === ' ') {
            _this.value = ' ';
            _this.emptyOrWhitespace = true;
        }
        else {
            _this.value = value ? value.trim() : '';
            _this.emptyOrWhitespace = _this.value === '';
        }
        return _this;
    }
    Combinator.prototype.genCSS = function (context, output) {
        var spaceOrEmpty = (context.compress || _noSpaceCombinators[this.value]) ? '' : ' ';
        output.add(spaceOrEmpty + this.value + spaceOrEmpty);
    };
    return Combinator;
}(Node));
Combinator.prototype.type = 'Combinator';

var Element = /** @class */ (function (_super) {
    tslib.__extends(Element, _super);
    function Element(combinator, value, isVariable, index, currentFileInfo, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this.combinator = combinator instanceof Combinator ?
            combinator : new Combinator(combinator);
        if (typeof value === 'string') {
            _this.value = value.trim();
        }
        else if (value) {
            _this.value = value;
        }
        else {
            _this.value = '';
        }
        _this.isVariable = isVariable;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.copyVisibilityInfo(visibilityInfo);
        _this.setParent(_this.combinator, _this);
        return _this;
    }
    Element.prototype.accept = function (visitor) {
        var value = this.value;
        this.combinator = visitor.visit(this.combinator);
        if (typeof value === 'object') {
            this.value = visitor.visit(value);
        }
    };
    Element.prototype.eval = function (context) {
        return new Element(this.combinator, this.value.eval ? this.value.eval(context) : this.value, this.isVariable, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    };
    Element.prototype.clone = function () {
        return new Element(this.combinator, this.value, this.isVariable, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    };
    Element.prototype.genCSS = function (context, output) {
        output.add(this.toCSS(context), this.fileInfo(), this.getIndex());
    };
    Element.prototype.toCSS = function (context) {
        if (context === void 0) { context = {}; }
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
        }
        else {
            return this.combinator.toCSS(context) + value;
        }
    };
    return Element;
}(Node));
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
        line: line,
        column: column
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
        var defaults_1 = CloneHelper(obj1);
        newObj._defaults = defaults_1;
        var cloned = obj2 ? CloneHelper(obj2) : {};
        Object.assign(newObj, defaults_1, cloned);
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
    }
    // Back compat with changed relativeUrls option
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
function flattenArray(arr, result) {
    if (result === void 0) { result = []; }
    for (var i_1 = 0, length_1 = arr.length; i_1 < length_1; i_1++) {
        var value = arr[i_1];
        if (Array.isArray(value)) {
            flattenArray(value, result);
        }
        else {
            if (value !== undefined) {
                result.push(value);
            }
        }
    }
    return result;
}

var utils = /*#__PURE__*/Object.freeze({
    __proto__: null,
    getLocation: getLocation,
    copyArray: copyArray,
    clone: clone,
    defaults: defaults,
    copyOptions: copyOptions,
    merge: merge,
    flattenArray: flattenArray
});

var anonymousFunc = /(<anonymous>|Function):(\d+):(\d+)/;
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
            var found = this.stack.match(anonymousFunc);
            /**
             * We have to figure out how this environment stringifies anonymous functions
             * so we can correctly map plugin errors.
             *
             * Note, in Node 8, the output of anonymous funcs varied based on parameters
             * being present or not, so we inject dummy params.
             */
            var func = new Function('a', 'throw new Error()');
            var lineAdjust = 0;
            try {
                func();
            }
            catch (e) {
                var match = e.stack.match(anonymousFunc);
                var line_1 = parseInt(match[2]);
                lineAdjust = 1 - line_1;
            }
            if (found) {
                if (found[2]) {
                    this.line = parseInt(found[2]) + lineAdjust;
                }
                if (found[3]) {
                    this.column = parseInt(found[3]);
                }
            }
        }
        this.callLine = callLine + 1;
        this.callExtract = lines[callLine];
        this.extract = [
            lines[this.line - 2],
            lines[this.line - 1],
            lines[this.line]
        ];
    }
};
if (typeof Object.create === 'undefined') {
    var F = function () { };
    F.prototype = Error.prototype;
    LessError.prototype = new F();
}
else {
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
LessError.prototype.toString = function (options) {
    if (options === void 0) { options = {}; }
    var message = '';
    var extract = this.extract || [];
    var error = [];
    var stylize = function (str) { return str; };
    if (options.stylize) {
        var type = typeof options.stylize;
        if (type !== 'function') {
            throw Error("options.stylize should be a function, got a " + type + "!");
        }
        stylize = options.stylize;
    }
    if (this.line !== null) {
        if (typeof extract[0] === 'string') {
            error.push(stylize(this.line - 1 + " " + extract[0], 'grey'));
        }
        if (typeof extract[1] === 'string') {
            var errorTxt = this.line + " ";
            if (extract[1]) {
                errorTxt += extract[1].slice(0, this.column) +
                    stylize(stylize(stylize(extract[1].substr(this.column, 1), 'bold') +
                        extract[1].slice(this.column + 1), 'red'), 'inverse');
            }
            error.push(errorTxt);
        }
        if (typeof extract[2] === 'string') {
            error.push(stylize(this.line + 1 + " " + extract[2], 'grey'));
        }
        error = error.join('\n') + stylize('', 'reset') + "\n";
    }
    message += stylize(this.type + "Error: " + this.message, 'red');
    if (this.filename) {
        message += stylize(' in ', 'red') + this.filename;
    }
    if (this.line) {
        message += stylize(" on line " + this.line + ", column " + (this.column + 1) + ":", 'grey');
    }
    message += "\n" + error;
    if (this.callLine) {
        message += stylize('from ', 'red') + (this.filename || '') + "/n";
        message += stylize(this.callLine, 'grey') + " " + this.callExtract + "/n";
    }
    return message;
};

var Selector = /** @class */ (function (_super) {
    tslib.__extends(Selector, _super);
    function Selector(elements, extendList, condition, index, currentFileInfo, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this.extendList = extendList;
        _this.condition = condition;
        _this.evaldCondition = !condition;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.elements = _this.getElements(elements);
        _this.mixinElements_ = undefined;
        _this.copyVisibilityInfo(visibilityInfo);
        _this.setParent(_this.elements, _this);
        return _this;
    }
    Selector.prototype.accept = function (visitor) {
        if (this.elements) {
            this.elements = visitor.visitArray(this.elements);
        }
        if (this.extendList) {
            this.extendList = visitor.visitArray(this.extendList);
        }
        if (this.condition) {
            this.condition = visitor.visit(this.condition);
        }
    };
    Selector.prototype.createDerived = function (elements, extendList, evaldCondition) {
        elements = this.getElements(elements);
        var newSelector = new Selector(elements, extendList || this.extendList, null, this.getIndex(), this.fileInfo(), this.visibilityInfo());
        newSelector.evaldCondition = (evaldCondition != null) ? evaldCondition : this.evaldCondition;
        newSelector.mediaEmpty = this.mediaEmpty;
        return newSelector;
    };
    Selector.prototype.getElements = function (els) {
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
    };
    Selector.prototype.createEmptySelectors = function () {
        var el = new Element('', '&', false, this._index, this._fileInfo);
        var sels = [new Selector([el], null, null, this._index, this._fileInfo)];
        sels[0].mediaEmpty = true;
        return sels;
    };
    Selector.prototype.match = function (other) {
        var elements = this.elements;
        var len = elements.length;
        var olen;
        var i;
        other = other.mixinElements();
        olen = other.length;
        if (olen === 0 || len < olen) {
            return 0;
        }
        else {
            for (i = 0; i < olen; i++) {
                if (elements[i].value !== other[i]) {
                    return 0;
                }
            }
        }
        return olen; // return number of matched elements
    };
    Selector.prototype.mixinElements = function () {
        if (this.mixinElements_) {
            return this.mixinElements_;
        }
        var elements = this.elements.map(function (v) { return v.combinator.value + (v.value.value || v.value); }).join('').match(/[,&#\*\.\w-]([\w-]|(\\.))*/g);
        if (elements) {
            if (elements[0] === '&') {
                elements.shift();
            }
        }
        else {
            elements = [];
        }
        return (this.mixinElements_ = elements);
    };
    Selector.prototype.isJustParentSelector = function () {
        return !this.mediaEmpty &&
            this.elements.length === 1 &&
            this.elements[0].value === '&' &&
            (this.elements[0].combinator.value === ' ' || this.elements[0].combinator.value === '');
    };
    Selector.prototype.eval = function (context) {
        var evaldCondition = this.condition && this.condition.eval(context);
        var elements = this.elements;
        var extendList = this.extendList;
        elements = elements && elements.map(function (e) { return e.eval(context); });
        extendList = extendList && extendList.map(function (extend) { return extend.eval(context); });
        return this.createDerived(elements, extendList, evaldCondition);
    };
    Selector.prototype.genCSS = function (context, output) {
        var i;
        var element;
        if ((!context || !context.firstSelector) && this.elements[0].combinator.value === '') {
            output.add(' ', this.fileInfo(), this.getIndex());
        }
        for (i = 0; i < this.elements.length; i++) {
            element = this.elements[i];
            element.genCSS(context, output);
        }
    };
    Selector.prototype.getIsOutput = function () {
        return this.evaldCondition;
    };
    return Selector;
}(Node));
Selector.prototype.type = 'Selector';

var Value = /** @class */ (function (_super) {
    tslib.__extends(Value, _super);
    function Value(value) {
        var _this = _super.call(this) || this;
        if (!value) {
            throw new Error('Value requires an array argument');
        }
        if (!Array.isArray(value)) {
            _this.value = [value];
        }
        else {
            _this.value = value;
        }
        return _this;
    }
    Value.prototype.accept = function (visitor) {
        if (this.value) {
            this.value = visitor.visitArray(this.value);
        }
    };
    Value.prototype.eval = function (context) {
        if (this.value.length === 1) {
            return this.value[0].eval(context);
        }
        else {
            return new Value(this.value.map(function (v) { return v.eval(context); }));
        }
    };
    Value.prototype.genCSS = function (context, output) {
        var i;
        for (i = 0; i < this.value.length; i++) {
            this.value[i].genCSS(context, output);
            if (i + 1 < this.value.length) {
                output.add((context && context.compress) ? ',' : ', ');
            }
        }
    };
    return Value;
}(Node));
Value.prototype.type = 'Value';

var Keyword = /** @class */ (function (_super) {
    tslib.__extends(Keyword, _super);
    function Keyword(value) {
        var _this = _super.call(this) || this;
        _this.value = value;
        return _this;
    }
    Keyword.prototype.genCSS = function (context, output) {
        if (this.value === '%') {
            throw { type: 'Syntax', message: 'Invalid % without number' };
        }
        output.add(this.value);
    };
    return Keyword;
}(Node));
Keyword.prototype.type = 'Keyword';
Keyword.True = new Keyword('true');
Keyword.False = new Keyword('false');

var Anonymous = /** @class */ (function (_super) {
    tslib.__extends(Anonymous, _super);
    function Anonymous(value, index, currentFileInfo, mapLines, rulesetLike, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this.value = value;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.mapLines = mapLines;
        _this.rulesetLike = (typeof rulesetLike === 'undefined') ? false : rulesetLike;
        _this.allowRoot = true;
        _this.copyVisibilityInfo(visibilityInfo);
        return _this;
    }
    Anonymous.prototype.eval = function () {
        return new Anonymous(this.value, this._index, this._fileInfo, this.mapLines, this.rulesetLike, this.visibilityInfo());
    };
    Anonymous.prototype.compare = function (other) {
        return other.toCSS && this.toCSS() === other.toCSS() ? 0 : undefined;
    };
    Anonymous.prototype.isRulesetLike = function () {
        return this.rulesetLike;
    };
    Anonymous.prototype.genCSS = function (context, output) {
        this.nodeVisible = Boolean(this.value);
        if (this.nodeVisible) {
            output.add(this.value, this._fileInfo, this._index, this.mapLines);
        }
    };
    return Anonymous;
}(Node));
Anonymous.prototype.type = 'Anonymous';

var MATH = Math$1;
var Declaration = /** @class */ (function (_super) {
    tslib.__extends(Declaration, _super);
    function Declaration(name, value, important, merge, index, currentFileInfo, inline, variable) {
        var _this = _super.call(this) || this;
        _this.name = name;
        _this.value = (value instanceof Node) ? value : new Value([value ? new Anonymous(value) : null]);
        _this.important = important ? " " + important.trim() : '';
        _this.merge = merge;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.inline = inline || false;
        _this.variable = (variable !== undefined) ? variable
            : (name.charAt && (name.charAt(0) === '@'));
        _this.allowRoot = true;
        _this.setParent(_this.value, _this);
        return _this;
    }
    Declaration.prototype.genCSS = function (context, output) {
        output.add(this.name + (context.compress ? ':' : ': '), this.fileInfo(), this.getIndex());
        try {
            this.value.genCSS(context, output);
        }
        catch (e) {
            e.index = this._index;
            e.filename = this._fileInfo.filename;
            throw e;
        }
        output.add(this.important + ((this.inline || (context.lastRule && context.compress)) ? '' : ';'), this._fileInfo, this._index);
    };
    Declaration.prototype.eval = function (context) {
        var mathBypass = false;
        var prevMath;
        var name = this.name;
        var evaldValue;
        var variable = this.variable;
        if (typeof name !== 'string') {
            // expand 'primitive' name directly to get
            // things faster (~10% for benchmark.less):
            name = (name.length === 1) && (name[0] instanceof Keyword) ?
                name[0].value : evalName(context, name);
            variable = false; // never treat expanded interpolation as new variable name
        }
        // @todo remove when parens-division is default
        if (name === 'font' && context.math === MATH.ALWAYS) {
            mathBypass = true;
            prevMath = context.math;
            context.math = MATH.PARENS_DIVISION;
        }
        try {
            context.importantScope.push({});
            evaldValue = this.value.eval(context);
            if (!this.variable && evaldValue.type === 'DetachedRuleset') {
                throw { message: 'Rulesets cannot be evaluated on a property.',
                    index: this.getIndex(), filename: this.fileInfo().filename };
            }
            var important = this.important;
            var importantResult = context.importantScope.pop();
            if (!important && importantResult.important) {
                important = importantResult.important;
            }
            return new Declaration(name, evaldValue, important, this.merge, this.getIndex(), this.fileInfo(), this.inline, variable);
        }
        catch (e) {
            if (typeof e.index !== 'number') {
                e.index = this.getIndex();
                e.filename = this.fileInfo().filename;
            }
            throw e;
        }
        finally {
            if (mathBypass) {
                context.math = prevMath;
            }
        }
    };
    Declaration.prototype.makeImportant = function () {
        return new Declaration(this.name, this.value, '!important', this.merge, this.getIndex(), this.fileInfo(), this.inline);
    };
    return Declaration;
}(Node));
function evalName(context, name) {
    var value = '';
    var i;
    var n = name.length;
    var output = { add: function (s) { value += s; } };
    for (i = 0; i < n; i++) {
        name[i].eval(context).genCSS(context, output);
    }
    return value;
}
Declaration.prototype.type = 'Declaration';

var debugInfo = function (context, ctx, lineSeparator) {
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
debugInfo.asComment = function (ctx) { return "/* line " + ctx.debugInfo.lineNumber + ", " + ctx.debugInfo.fileName + " */\n"; };
debugInfo.asMediaQuery = function (ctx) {
    var filenameWithProtocol = ctx.debugInfo.fileName;
    if (!/^[a-z]+:\/\//i.test(filenameWithProtocol)) {
        filenameWithProtocol = "file://" + filenameWithProtocol;
    }
    return "@media -sass-debug-info{filename{font-family:" + filenameWithProtocol.replace(/([.:\/\\])/g, function (a) {
        if (a == '\\') {
            a = '\/';
        }
        return "\\" + a;
    }) + "}line{font-family:\\00003" + ctx.debugInfo.lineNumber + "}}\n";
};

var Comment = /** @class */ (function (_super) {
    tslib.__extends(Comment, _super);
    function Comment(value, isLineComment, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.value = value;
        _this.isLineComment = isLineComment;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.allowRoot = true;
        return _this;
    }
    Comment.prototype.genCSS = function (context, output) {
        if (this.debugInfo) {
            output.add(debugInfo(context, this), this.fileInfo(), this.getIndex());
        }
        output.add(this.value);
    };
    Comment.prototype.isSilent = function (context) {
        var isCompressed = context.compress && this.value[2] !== '!';
        return this.isLineComment || isCompressed;
    };
    return Comment;
}(Node));
Comment.prototype.type = 'Comment';

var contexts = {};
var copyFromOriginal = function copyFromOriginal(original, destination, propertiesToCopy) {
    if (!original) {
        return;
    }
    for (var i_1 = 0; i_1 < propertiesToCopy.length; i_1++) {
        if (original.hasOwnProperty(propertiesToCopy[i_1])) {
            destination[propertiesToCopy[i_1]] = original[propertiesToCopy[i_1]];
        }
    }
};
/*
 parse is used whilst parsing
 */
var parseCopyProperties = [
    // options
    'paths',
    'rewriteUrls',
    'rootpath',
    'strictImports',
    'insecure',
    'dumpLineNumbers',
    'compress',
    'syncImport',
    'chunkInput',
    'mime',
    'useFileCache',
    // context
    'processImports',
    // Used by the import manager to stop multiple import visitors being created.
    'pluginManager' // Used as the plugin manager for the session
];
contexts.Parse = function (options) {
    copyFromOriginal(options, this, parseCopyProperties);
    if (typeof this.paths === 'string') {
        this.paths = [this.paths];
    }
};
var evalCopyProperties = [
    'paths',
    'compress',
    'math',
    'strictUnits',
    'sourceMap',
    'importMultiple',
    'urlArgs',
    'javascriptEnabled',
    'pluginManager',
    'importantScope',
    'rewriteUrls' // option - whether to adjust URL's to be relative
];
function isPathRelative(path) {
    return !/^(?:[a-z-]+:|\/|#)/i.test(path);
}
function isPathLocalRelative(path) {
    return path.charAt(0) === '.';
}
contexts.Eval = /** @class */ (function () {
    function Eval(options, frames) {
        copyFromOriginal(options, this, evalCopyProperties);
        if (typeof this.paths === 'string') {
            this.paths = [this.paths];
        }
        this.frames = frames || [];
        this.importantScope = this.importantScope || [];
        this.inCalc = false;
        this.mathOn = true;
    }
    Eval.prototype.enterCalc = function () {
        if (!this.calcStack) {
            this.calcStack = [];
        }
        this.calcStack.push(true);
        this.inCalc = true;
    };
    Eval.prototype.exitCalc = function () {
        this.calcStack.pop();
        if (!this.calcStack) {
            this.inCalc = false;
        }
    };
    Eval.prototype.inParenthesis = function () {
        if (!this.parensStack) {
            this.parensStack = [];
        }
        this.parensStack.push(true);
    };
    Eval.prototype.outOfParenthesis = function () {
        this.parensStack.pop();
    };
    Eval.prototype.isMathOn = function (op) {
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
    };
    Eval.prototype.pathRequiresRewrite = function (path) {
        var isRelative = this.rewriteUrls === RewriteUrls.LOCAL ? isPathLocalRelative : isPathRelative;
        return isRelative(path);
    };
    Eval.prototype.rewritePath = function (path, rootpath) {
        var newPath;
        rootpath = rootpath || '';
        newPath = this.normalizePath(rootpath + path);
        // If a path was explicit relative and the rootpath was not an absolute path
        // we must ensure that the new path is also explicit relative.
        if (isPathLocalRelative(path) &&
            isPathRelative(rootpath) &&
            isPathLocalRelative(newPath) === false) {
            newPath = "./" + newPath;
        }
        return newPath;
    };
    Eval.prototype.normalizePath = function (path) {
        var segments = path.split('/').reverse();
        var segment;
        path = [];
        while (segments.length !== 0) {
            segment = segments.pop();
            switch (segment) {
                case '.':
                    break;
                case '..':
                    if ((path.length === 0) || (path[path.length - 1] === '..')) {
                        path.push(segment);
                    }
                    else {
                        path.pop();
                    }
                    break;
                default:
                    path.push(segment);
                    break;
            }
        }
        return path.join('/');
    };
    return Eval;
}());

function makeRegistry(base) {
    return {
        _data: {},
        add: function (name, func) {
            // precautionary case conversion, as later querying of
            // the registry by function-caller uses lower case as well.
            name = name.toLowerCase();
            if (this._data.hasOwnProperty(name)) ;
            this._data[name] = func;
        },
        addMultiple: function (functions) {
            var _this = this;
            Object.keys(functions).forEach(function (name) {
                _this.add(name, functions[name]);
            });
        },
        get: function (name) {
            return this._data[name] || (base && base.get(name));
        },
        getLocalFunctions: function () {
            return this._data;
        },
        inherit: function () {
            return makeRegistry(this);
        },
        create: function (base) {
            return makeRegistry(base);
        }
    };
}
var functionRegistry = makeRegistry(null);

var defaultFunc = {
    eval: function () {
        var v = this.value_;
        var e = this.error_;
        if (e) {
            throw e;
        }
        if (v != null) {
            return v ? Keyword.True : Keyword.False;
        }
    },
    value: function (v) {
        this.value_ = v;
    },
    error: function (e) {
        this.error_ = e;
    },
    reset: function () {
        this.value_ = this.error_ = null;
    }
};

var Ruleset = /** @class */ (function (_super) {
    tslib.__extends(Ruleset, _super);
    function Ruleset(selectors, rules, strictImports, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this.selectors = selectors;
        _this.rules = rules;
        _this._lookups = {};
        _this._variables = null;
        _this._properties = null;
        _this.strictImports = strictImports;
        _this.copyVisibilityInfo(visibilityInfo);
        _this.allowRoot = true;
        _this.setParent(_this.selectors, _this);
        _this.setParent(_this.rules, _this);
        return _this;
    }
    Ruleset.prototype.isRulesetLike = function () {
        return true;
    };
    Ruleset.prototype.accept = function (visitor) {
        if (this.paths) {
            this.paths = visitor.visitArray(this.paths, true);
        }
        else if (this.selectors) {
            this.selectors = visitor.visitArray(this.selectors);
        }
        if (this.rules && this.rules.length) {
            this.rules = visitor.visitArray(this.rules);
        }
    };
    Ruleset.prototype.eval = function (context) {
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
        }
        else {
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
        }
        // inherit a function registry from the frames stack when possible;
        // otherwise from the global registry
        ruleset.functionRegistry = (function (frames) {
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
        })(context.frames).inherit();
        // push the current ruleset to the frames stack
        var ctxFrames = context.frames;
        ctxFrames.unshift(ruleset);
        // currrent selectors
        var ctxSelectors = context.selectors;
        if (!ctxSelectors) {
            context.selectors = ctxSelectors = [];
        }
        ctxSelectors.unshift(this.selectors);
        // Evaluate imports
        if (ruleset.root || ruleset.allowImports || !ruleset.strictImports) {
            ruleset.evalImports(context);
        }
        // Store the frames around mixin definitions,
        // so they can be evaluated like closures when the time comes.
        var rsRules = ruleset.rules;
        for (i = 0; (rule = rsRules[i]); i++) {
            if (rule.evalFirst) {
                rsRules[i] = rule.eval(context);
            }
        }
        var mediaBlockCount = (context.mediaBlocks && context.mediaBlocks.length) || 0;
        // Evaluate mixin calls.
        for (i = 0; (rule = rsRules[i]); i++) {
            if (rule.type === 'MixinCall') {
                /* jshint loopfunc:true */
                rules = rule.eval(context).filter(function (r) {
                    if ((r instanceof Declaration) && r.variable) {
                        // do not pollute the scope if the variable is
                        // already there. consider returning false here
                        // but we need a way to "return" variable from mixins
                        return !(ruleset.variable(r.name));
                    }
                    return true;
                });
                rsRules.splice.apply(rsRules, [i, 1].concat(rules));
                i += rules.length - 1;
                ruleset.resetCache();
            }
            else if (rule.type === 'VariableCall') {
                /* jshint loopfunc:true */
                rules = rule.eval(context).rules.filter(function (r) {
                    if ((r instanceof Declaration) && r.variable) {
                        // do not pollute the scope at all
                        return false;
                    }
                    return true;
                });
                rsRules.splice.apply(rsRules, [i, 1].concat(rules));
                i += rules.length - 1;
                ruleset.resetCache();
            }
        }
        // Evaluate everything else
        for (i = 0; (rule = rsRules[i]); i++) {
            if (!rule.evalFirst) {
                rsRules[i] = rule = rule.eval ? rule.eval(context) : rule;
            }
        }
        // Evaluate everything else
        for (i = 0; (rule = rsRules[i]); i++) {
            // for rulesets, check if it is a css guard and can be removed
            if (rule instanceof Ruleset && rule.selectors && rule.selectors.length === 1) {
                // check if it can be folded in (e.g. & where)
                if (rule.selectors[0] && rule.selectors[0].isJustParentSelector()) {
                    rsRules.splice(i--, 1);
                    for (var j = 0; (subRule = rule.rules[j]); j++) {
                        if (subRule instanceof Node) {
                            subRule.copyVisibilityInfo(rule.visibilityInfo());
                            if (!(subRule instanceof Declaration) || !subRule.variable) {
                                rsRules.splice(++i, 0, subRule);
                            }
                        }
                    }
                }
            }
        }
        // Pop the stack
        ctxFrames.shift();
        ctxSelectors.shift();
        if (context.mediaBlocks) {
            for (i = mediaBlockCount; i < context.mediaBlocks.length; i++) {
                context.mediaBlocks[i].bubbleSelectors(selectors);
            }
        }
        return ruleset;
    };
    Ruleset.prototype.evalImports = function (context) {
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
                    rules.splice.apply(rules, [i, 1].concat(importRules));
                    i += importRules.length - 1;
                }
                else {
                    rules.splice(i, 1, importRules);
                }
                this.resetCache();
            }
        }
    };
    Ruleset.prototype.makeImportant = function () {
        var result = new Ruleset(this.selectors, this.rules.map(function (r) {
            if (r.makeImportant) {
                return r.makeImportant();
            }
            else {
                return r;
            }
        }), this.strictImports, this.visibilityInfo());
        return result;
    };
    Ruleset.prototype.matchArgs = function (args) {
        return !args || args.length === 0;
    };
    // lets you call a css selector with a guard
    Ruleset.prototype.matchCondition = function (args, context) {
        var lastSelector = this.selectors[this.selectors.length - 1];
        if (!lastSelector.evaldCondition) {
            return false;
        }
        if (lastSelector.condition &&
            !lastSelector.condition.eval(new contexts.Eval(context, context.frames))) {
            return false;
        }
        return true;
    };
    Ruleset.prototype.resetCache = function () {
        this._rulesets = null;
        this._variables = null;
        this._properties = null;
        this._lookups = {};
    };
    Ruleset.prototype.variables = function () {
        if (!this._variables) {
            this._variables = !this.rules ? {} : this.rules.reduce(function (hash, r) {
                if (r instanceof Declaration && r.variable === true) {
                    hash[r.name] = r;
                }
                // when evaluating variables in an import statement, imports have not been eval'd
                // so we need to go inside import statements.
                // guard against root being a string (in the case of inlined less)
                if (r.type === 'Import' && r.root && r.root.variables) {
                    var vars = r.root.variables();
                    for (var name_1 in vars) {
                        if (vars.hasOwnProperty(name_1)) {
                            hash[name_1] = r.root.variable(name_1);
                        }
                    }
                }
                return hash;
            }, {});
        }
        return this._variables;
    };
    Ruleset.prototype.properties = function () {
        if (!this._properties) {
            this._properties = !this.rules ? {} : this.rules.reduce(function (hash, r) {
                if (r instanceof Declaration && r.variable !== true) {
                    var name_2 = (r.name.length === 1) && (r.name[0] instanceof Keyword) ?
                        r.name[0].value : r.name;
                    // Properties don't overwrite as they can merge
                    if (!hash["$" + name_2]) {
                        hash["$" + name_2] = [r];
                    }
                    else {
                        hash["$" + name_2].push(r);
                    }
                }
                return hash;
            }, {});
        }
        return this._properties;
    };
    Ruleset.prototype.variable = function (name) {
        var decl = this.variables()[name];
        if (decl) {
            return this.parseValue(decl);
        }
    };
    Ruleset.prototype.property = function (name) {
        var decl = this.properties()[name];
        if (decl) {
            return this.parseValue(decl);
        }
    };
    Ruleset.prototype.lastDeclaration = function () {
        for (var i_1 = this.rules.length; i_1 > 0; i_1--) {
            var decl = this.rules[i_1 - 1];
            if (decl instanceof Declaration) {
                return this.parseValue(decl);
            }
        }
    };
    Ruleset.prototype.parseValue = function (toParse) {
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
                }
                else {
                    decl.parsed = true;
                }
                return decl;
            }
            else {
                return decl;
            }
        }
        if (!Array.isArray(toParse)) {
            return transformDeclaration.call(self, toParse);
        }
        else {
            var nodes_1 = [];
            toParse.forEach(function (n) {
                nodes_1.push(transformDeclaration.call(self, n));
            });
            return nodes_1;
        }
    };
    Ruleset.prototype.rulesets = function () {
        if (!this.rules) {
            return [];
        }
        var filtRules = [];
        var rules = this.rules;
        var i;
        var rule;
        for (i = 0; (rule = rules[i]); i++) {
            if (rule.isRuleset) {
                filtRules.push(rule);
            }
        }
        return filtRules;
    };
    Ruleset.prototype.prependRule = function (rule) {
        var rules = this.rules;
        if (rules) {
            rules.unshift(rule);
        }
        else {
            this.rules = [rule];
        }
        this.setParent(rule, this);
    };
    Ruleset.prototype.find = function (selector, self, filter) {
        if (self === void 0) { self = this; }
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
                                for (var i_2 = 0; i_2 < foundMixins.length; ++i_2) {
                                    foundMixins[i_2].path.push(rule);
                                }
                                Array.prototype.push.apply(rules, foundMixins);
                            }
                        }
                        else {
                            rules.push({ rule: rule, path: [] });
                        }
                        break;
                    }
                }
            }
        });
        this._lookups[key] = rules;
        return rules;
    };
    Ruleset.prototype.genCSS = function (context, output) {
        var i;
        var j;
        var charsetRuleNodes = [];
        var ruleNodes = [];
        var // Line number debugging
        debugInfo$1;
        var rule;
        var path;
        context.tabLevel = (context.tabLevel || 0);
        if (!this.root) {
            context.tabLevel++;
        }
        var tabRuleStr = context.compress ? '' : Array(context.tabLevel + 1).join('  ');
        var tabSetStr = context.compress ? '' : Array(context.tabLevel).join('  ');
        var sep;
        var charsetNodeIndex = 0;
        var importNodeIndex = 0;
        for (i = 0; (rule = this.rules[i]); i++) {
            if (rule instanceof Comment) {
                if (importNodeIndex === i) {
                    importNodeIndex++;
                }
                ruleNodes.push(rule);
            }
            else if (rule.isCharset && rule.isCharset()) {
                ruleNodes.splice(charsetNodeIndex, 0, rule);
                charsetNodeIndex++;
                importNodeIndex++;
            }
            else if (rule.type === 'Import') {
                ruleNodes.splice(importNodeIndex, 0, rule);
                importNodeIndex++;
            }
            else {
                ruleNodes.push(rule);
            }
        }
        ruleNodes = charsetRuleNodes.concat(ruleNodes);
        // If this is the root node, we don't render
        // a selector, or {}.
        if (!this.root) {
            debugInfo$1 = debugInfo(context, this, tabSetStr);
            if (debugInfo$1) {
                output.add(debugInfo$1);
                output.add(tabSetStr);
            }
            var paths = this.paths;
            var pathCnt = paths.length;
            var pathSubCnt = void 0;
            sep = context.compress ? ',' : (",\n" + tabSetStr);
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
        }
        // Compile rules and rulesets
        for (i = 0; (rule = ruleNodes[i]); i++) {
            if (i + 1 === ruleNodes.length) {
                context.lastRule = true;
            }
            var currentLastRule = context.lastRule;
            if (rule.isRulesetLike(rule)) {
                context.lastRule = false;
            }
            if (rule.genCSS) {
                rule.genCSS(context, output);
            }
            else if (rule.value) {
                output.add(rule.value.toString());
            }
            context.lastRule = currentLastRule;
            if (!context.lastRule && rule.isVisible()) {
                output.add(context.compress ? '' : ("\n" + tabRuleStr));
            }
            else {
                context.lastRule = false;
            }
        }
        if (!this.root) {
            output.add((context.compress ? '}' : "\n" + tabSetStr + "}"));
            context.tabLevel--;
        }
        if (!output.isEmpty() && !context.compress && this.firstRoot) {
            output.add('\n');
        }
    };
    Ruleset.prototype.joinSelectors = function (paths, context, selectors) {
        for (var s = 0; s < selectors.length; s++) {
            this.joinSelector(paths, context, selectors[s]);
        }
    };
    Ruleset.prototype.joinSelector = function (paths, context, selector) {
        function createParenthesis(elementsToPak, originalElement) {
            var replacementParen;
            var j;
            if (elementsToPak.length === 0) {
                replacementParen = new Paren(elementsToPak[0]);
            }
            else {
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
        }
        // joins selector path from `beginningPath` with selector path in `addPath`
        // `replacedElement` contains element that is being replaced by `addPath`
        // returns concatenated path
        function addReplacementIntoPath(beginningPath, addPath, replacedElement, originalSelector) {
            var newSelectorPath;
            var lastSelector;
            var newJoinedSelector;
            // our new selector path
            newSelectorPath = [];
            // construct the joined selector - if & is the first thing this will be empty,
            // if not newJoinedSelector will be the last set of elements in the selector
            if (beginningPath.length > 0) {
                newSelectorPath = copyArray(beginningPath);
                lastSelector = newSelectorPath.pop();
                newJoinedSelector = originalSelector.createDerived(copyArray(lastSelector.elements));
            }
            else {
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
                }
                // join the elements so far with the first part of the parent
                newJoinedSelector.elements.push(new Element(combinator, parentEl.value, replacedElement.isVariable, replacedElement._index, replacedElement._fileInfo));
                newJoinedSelector.elements = newJoinedSelector.elements.concat(addPath[0].elements.slice(1));
            }
            // now add the joined selector - but only if it is not empty
            if (newJoinedSelector.elements.length !== 0) {
                newSelectorPath.push(newJoinedSelector);
            }
            // put together the parent selectors after the join (e.g. the rest of the parent)
            if (addPath.length > 1) {
                var restOfPath = addPath.slice(1);
                restOfPath = restOfPath.map(function (selector) { return selector.createDerived(selector.elements, []); });
                newSelectorPath = newSelectorPath.concat(restOfPath);
            }
            return newSelectorPath;
        }
        // joins selector path from `beginningPath` with every selector path in `addPaths` array
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
            for (i = 0; (sel = selectors[i]); i++) {
                // if the previous thing in sel is a parent this needs to join on to it
                if (sel.length > 0) {
                    sel[sel.length - 1] = sel[sel.length - 1].createDerived(sel[sel.length - 1].elements.concat(elements));
                }
                else {
                    sel.push(new Selector(elements));
                }
            }
        }
        // replace all parent selectors inside `inSelector` by content of `context` array
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
            }
            // the elements from the current selector so far
            currentElements = [];
            // the current list of new selectors to add to the path.
            // We will build it up. We initiate it with one empty selector as we "multiply" the new selectors
            // by the parents
            newSelectors = [
                []
            ];
            for (i = 0; (el = inSelector.elements[i]); i++) {
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
                        hadParentSelector = hadParentSelector || replaced;
                        // the nestedPaths array should have only one member - replaceParentSelector does not multiply selectors
                        for (k = 0; k < nestedPaths.length; k++) {
                            var replacementSelector = createSelector(createParenthesis(nestedPaths[k], el), el);
                            addAllReplacementsIntoPath(newSelectors, [replacementSelector], el, inSelector, replacedNewSelectors);
                        }
                        newSelectors = replacedNewSelectors;
                        currentElements = [];
                    }
                    else {
                        currentElements.push(el);
                    }
                }
                else {
                    hadParentSelector = true;
                    // the new list of selectors to add
                    selectorsMultiplied = [];
                    // merge the current list of non parent selector elements
                    // on to the current list of selectors to add
                    mergeElementsOnToSelectors(currentElements, newSelectors);
                    // loop through our current selectors
                    for (j = 0; j < newSelectors.length; j++) {
                        sel = newSelectors[j];
                        // if we don't have any parent paths, the & might be in a mixin so that it can be used
                        // whether there are parents or not
                        if (context.length === 0) {
                            // the combinator used on el should now be applied to the next element instead so that
                            // it is not lost
                            if (sel.length > 0) {
                                sel[0].elements.push(new Element(el.combinator, '', el.isVariable, el._index, el._fileInfo));
                            }
                            selectorsMultiplied.push(sel);
                        }
                        else {
                            // and the parent selectors
                            for (k = 0; k < context.length; k++) {
                                // We need to put the current selectors
                                // then join the last selector's elements on to the parents selectors
                                var newSelectorPath = addReplacementIntoPath(sel, context[k], el, inSelector);
                                // add that to our new set of selectors
                                selectorsMultiplied.push(newSelectorPath);
                            }
                        }
                    }
                    // our new selectors has been multiplied, so reset the state
                    newSelectors = selectorsMultiplied;
                    currentElements = [];
                }
            }
            // if we have any elements left over (e.g. .a& .b == .b)
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
        }
        // joinSelector code follows
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
            }
            else {
                newPaths = [[selector]];
            }
        }
        for (i = 0; i < newPaths.length; i++) {
            paths.push(newPaths[i]);
        }
    };
    return Ruleset;
}(Node));
Ruleset.prototype.type = 'Ruleset';
Ruleset.prototype.isRuleset = true;

var AtRule = /** @class */ (function (_super) {
    tslib.__extends(AtRule, _super);
    function AtRule(name, value, rules, index, currentFileInfo, debugInfo, isRooted, visibilityInfo) {
        var _this = _super.call(this) || this;
        var i;
        _this.name = name;
        _this.value = (value instanceof Node) ? value : (value ? new Anonymous(value) : value);
        if (rules) {
            if (Array.isArray(rules)) {
                _this.rules = rules;
            }
            else {
                _this.rules = [rules];
                _this.rules[0].selectors = (new Selector([], null, null, index, currentFileInfo)).createEmptySelectors();
            }
            for (i = 0; i < _this.rules.length; i++) {
                _this.rules[i].allowImports = true;
            }
            _this.setParent(_this.rules, _this);
        }
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.debugInfo = debugInfo;
        _this.isRooted = isRooted || false;
        _this.copyVisibilityInfo(visibilityInfo);
        _this.allowRoot = true;
        return _this;
    }
    AtRule.prototype.accept = function (visitor) {
        var value = this.value;
        var rules = this.rules;
        if (rules) {
            this.rules = visitor.visitArray(rules);
        }
        if (value) {
            this.value = visitor.visit(value);
        }
    };
    AtRule.prototype.isRulesetLike = function () {
        return this.rules || !this.isCharset();
    };
    AtRule.prototype.isCharset = function () {
        return '@charset' === this.name;
    };
    AtRule.prototype.genCSS = function (context, output) {
        var value = this.value;
        var rules = this.rules;
        output.add(this.name, this.fileInfo(), this.getIndex());
        if (value) {
            output.add(' ');
            value.genCSS(context, output);
        }
        if (rules) {
            this.outputRuleset(context, output, rules);
        }
        else {
            output.add(';');
        }
    };
    AtRule.prototype.eval = function (context) {
        var mediaPathBackup;
        var mediaBlocksBackup;
        var value = this.value;
        var rules = this.rules;
        // media stored inside other atrule should not bubble over it
        // backpup media bubbling information
        mediaPathBackup = context.mediaPath;
        mediaBlocksBackup = context.mediaBlocks;
        // deleted media bubbling information
        context.mediaPath = [];
        context.mediaBlocks = [];
        if (value) {
            value = value.eval(context);
        }
        if (rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            rules = [rules[0].eval(context)];
            rules[0].root = true;
        }
        // restore media bubbling information
        context.mediaPath = mediaPathBackup;
        context.mediaBlocks = mediaBlocksBackup;
        return new AtRule(this.name, value, rules, this.getIndex(), this.fileInfo(), this.debugInfo, this.isRooted, this.visibilityInfo());
    };
    AtRule.prototype.variable = function (name) {
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.variable.call(this.rules[0], name);
        }
    };
    AtRule.prototype.find = function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.find.apply(this.rules[0], args);
        }
    };
    AtRule.prototype.rulesets = function () {
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.rulesets.apply(this.rules[0]);
        }
    };
    AtRule.prototype.outputRuleset = function (context, output, rules) {
        var ruleCnt = rules.length;
        var i;
        context.tabLevel = (context.tabLevel | 0) + 1;
        // Compressed
        if (context.compress) {
            output.add('{');
            for (i = 0; i < ruleCnt; i++) {
                rules[i].genCSS(context, output);
            }
            output.add('}');
            context.tabLevel--;
            return;
        }
        // Non-compressed
        var tabSetStr = "\n" + Array(context.tabLevel).join('  ');
        var tabRuleStr = tabSetStr + "  ";
        if (!ruleCnt) {
            output.add(" {" + tabSetStr + "}");
        }
        else {
            output.add(" {" + tabRuleStr);
            rules[0].genCSS(context, output);
            for (i = 1; i < ruleCnt; i++) {
                output.add(tabRuleStr);
                rules[i].genCSS(context, output);
            }
            output.add(tabSetStr + "}");
        }
        context.tabLevel--;
    };
    return AtRule;
}(Node));
AtRule.prototype.type = 'AtRule';

var DetachedRuleset = /** @class */ (function (_super) {
    tslib.__extends(DetachedRuleset, _super);
    function DetachedRuleset(ruleset, frames) {
        var _this = _super.call(this) || this;
        _this.ruleset = ruleset;
        _this.frames = frames;
        _this.setParent(_this.ruleset, _this);
        return _this;
    }
    DetachedRuleset.prototype.accept = function (visitor) {
        this.ruleset = visitor.visit(this.ruleset);
    };
    DetachedRuleset.prototype.eval = function (context) {
        var frames = this.frames || copyArray(context.frames);
        return new DetachedRuleset(this.ruleset, frames);
    };
    DetachedRuleset.prototype.callEval = function (context) {
        return this.ruleset.eval(this.frames ? new contexts.Eval(context, this.frames.concat(context.frames)) : context);
    };
    return DetachedRuleset;
}(Node));
DetachedRuleset.prototype.type = 'DetachedRuleset';
DetachedRuleset.prototype.evalFirst = true;

var Unit = /** @class */ (function (_super) {
    tslib.__extends(Unit, _super);
    function Unit(numerator, denominator, backupUnit) {
        var _this = _super.call(this) || this;
        _this.numerator = numerator ? copyArray(numerator).sort() : [];
        _this.denominator = denominator ? copyArray(denominator).sort() : [];
        if (backupUnit) {
            _this.backupUnit = backupUnit;
        }
        else if (numerator && numerator.length) {
            _this.backupUnit = numerator[0];
        }
        return _this;
    }
    Unit.prototype.clone = function () {
        return new Unit(copyArray(this.numerator), copyArray(this.denominator), this.backupUnit);
    };
    Unit.prototype.genCSS = function (context, output) {
        // Dimension checks the unit is singular and throws an error if in strict math mode.
        var strictUnits = context && context.strictUnits;
        if (this.numerator.length === 1) {
            output.add(this.numerator[0]); // the ideal situation
        }
        else if (!strictUnits && this.backupUnit) {
            output.add(this.backupUnit);
        }
        else if (!strictUnits && this.denominator.length) {
            output.add(this.denominator[0]);
        }
    };
    Unit.prototype.toString = function () {
        var i;
        var returnStr = this.numerator.join('*');
        for (i = 0; i < this.denominator.length; i++) {
            returnStr += "/" + this.denominator[i];
        }
        return returnStr;
    };
    Unit.prototype.compare = function (other) {
        return this.is(other.toString()) ? 0 : undefined;
    };
    Unit.prototype.is = function (unitString) {
        return this.toString().toUpperCase() === unitString.toUpperCase();
    };
    Unit.prototype.isLength = function () {
        return RegExp('^(px|em|ex|ch|rem|in|cm|mm|pc|pt|ex|vw|vh|vmin|vmax)$', 'gi').test(this.toCSS());
    };
    Unit.prototype.isEmpty = function () {
        return this.numerator.length === 0 && this.denominator.length === 0;
    };
    Unit.prototype.isSingular = function () {
        return this.numerator.length <= 1 && this.denominator.length === 0;
    };
    Unit.prototype.map = function (callback) {
        var i;
        for (i = 0; i < this.numerator.length; i++) {
            this.numerator[i] = callback(this.numerator[i], false);
        }
        for (i = 0; i < this.denominator.length; i++) {
            this.denominator[i] = callback(this.denominator[i], true);
        }
    };
    Unit.prototype.usedUnits = function () {
        var group;
        var result = {};
        var mapUnit;
        var groupName;
        mapUnit = function (atomicUnit) {
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
    };
    Unit.prototype.cancel = function () {
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
                }
                else if (count < 0) {
                    for (i = 0; i < -count; i++) {
                        this.denominator.push(atomicUnit);
                    }
                }
            }
        }
        this.numerator.sort();
        this.denominator.sort();
    };
    return Unit;
}(Node));
Unit.prototype.type = 'Unit';

//
// A number with a unit
//
var Dimension = /** @class */ (function (_super) {
    tslib.__extends(Dimension, _super);
    function Dimension(value, unit) {
        var _this = _super.call(this) || this;
        _this.value = parseFloat(value);
        if (isNaN(_this.value)) {
            throw new Error('Dimension is not a number.');
        }
        _this.unit = (unit && unit instanceof Unit) ? unit :
            new Unit(unit ? [unit] : undefined);
        _this.setParent(_this.unit, _this);
        return _this;
    }
    Dimension.prototype.accept = function (visitor) {
        this.unit = visitor.visit(this.unit);
    };
    Dimension.prototype.eval = function (context) {
        return this;
    };
    Dimension.prototype.toColor = function () {
        return new Color([this.value, this.value, this.value]);
    };
    Dimension.prototype.genCSS = function (context, output) {
        if ((context && context.strictUnits) && !this.unit.isSingular()) {
            throw new Error("Multiple units in dimension. Correct the units or use the unit function. Bad unit: " + this.unit.toString());
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
            }
            // Float values doesn't need a leading zero
            if (value > 0 && value < 1) {
                strValue = (strValue).substr(1);
            }
        }
        output.add(strValue);
        this.unit.genCSS(context, output);
    };
    // In an operation between two Dimensions,
    // we default to the first Dimension's unit,
    // so `1px + 2` will yield `3px`.
    Dimension.prototype.operate = function (context, op, other) {
        /* jshint noempty:false */
        var value = this._operate(context, op, this.value, other.value);
        var unit = this.unit.clone();
        if (op === '+' || op === '-') {
            if (unit.numerator.length === 0 && unit.denominator.length === 0) {
                unit = other.unit.clone();
                if (this.unit.backupUnit) {
                    unit.backupUnit = this.unit.backupUnit;
                }
            }
            else if (other.unit.numerator.length === 0 && unit.denominator.length === 0) ;
            else {
                other = other.convertTo(this.unit.usedUnits());
                if (context.strictUnits && other.unit.toString() !== unit.toString()) {
                    throw new Error("Incompatible units. Change the units or use the unit function. " +
                        ("Bad units: '" + unit.toString() + "' and '" + other.unit.toString() + "'."));
                }
                value = this._operate(context, op, this.value, other.value);
            }
        }
        else if (op === '*') {
            unit.numerator = unit.numerator.concat(other.unit.numerator).sort();
            unit.denominator = unit.denominator.concat(other.unit.denominator).sort();
            unit.cancel();
        }
        else if (op === '/') {
            unit.numerator = unit.numerator.concat(other.unit.denominator).sort();
            unit.denominator = unit.denominator.concat(other.unit.numerator).sort();
            unit.cancel();
        }
        return new Dimension(value, unit);
    };
    Dimension.prototype.compare = function (other) {
        var a;
        var b;
        if (!(other instanceof Dimension)) {
            return undefined;
        }
        if (this.unit.isEmpty() || other.unit.isEmpty()) {
            a = this;
            b = other;
        }
        else {
            a = this.unify();
            b = other.unify();
            if (a.unit.compare(b.unit) !== 0) {
                return undefined;
            }
        }
        return Node.numericCompare(a.value, b.value);
    };
    Dimension.prototype.unify = function () {
        return this.convertTo({ length: 'px', duration: 's', angle: 'rad' });
    };
    Dimension.prototype.convertTo = function (conversions) {
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
        applyUnit = function (atomicUnit, denominator) {
            /* jshint loopfunc:true */
            if (group.hasOwnProperty(atomicUnit)) {
                if (denominator) {
                    value = value / (group[atomicUnit] / group[targetUnit]);
                }
                else {
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
    };
    return Dimension;
}(Node));
Dimension.prototype.type = 'Dimension';

var MATH$1 = Math$1;
var Operation = /** @class */ (function (_super) {
    tslib.__extends(Operation, _super);
    function Operation(op, operands, isSpaced) {
        var _this = _super.call(this) || this;
        _this.op = op.trim();
        _this.operands = operands;
        _this.isSpaced = isSpaced;
        return _this;
    }
    Operation.prototype.accept = function (visitor) {
        this.operands = visitor.visitArray(this.operands);
    };
    Operation.prototype.eval = function (context) {
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
                throw { type: 'Operation',
                    message: 'Operation on an invalid type' };
            }
            return a.operate(context, op, b);
        }
        else {
            return new Operation(this.op, [a, b], this.isSpaced);
        }
    };
    Operation.prototype.genCSS = function (context, output) {
        this.operands[0].genCSS(context, output);
        if (this.isSpaced) {
            output.add(' ');
        }
        output.add(this.op);
        if (this.isSpaced) {
            output.add(' ');
        }
        this.operands[1].genCSS(context, output);
    };
    return Operation;
}(Node));
Operation.prototype.type = 'Operation';

var MATH$2 = Math$1;
var Expression = /** @class */ (function (_super) {
    tslib.__extends(Expression, _super);
    function Expression(value, noSpacing) {
        var _this = _super.call(this) || this;
        _this.value = value;
        _this.noSpacing = noSpacing;
        if (!value) {
            throw new Error('Expression requires an array parameter');
        }
        return _this;
    }
    Expression.prototype.accept = function (visitor) {
        this.value = visitor.visitArray(this.value);
    };
    Expression.prototype.eval = function (context) {
        var returnValue;
        var mathOn = context.isMathOn();
        var inParenthesis = this.parens &&
            (context.math !== MATH$2.STRICT_LEGACY || !this.parensInOp);
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
        }
        else if (this.value.length === 1) {
            if (this.value[0].parens && !this.value[0].parensInOp && !context.inCalc) {
                doubleParen = true;
            }
            returnValue = this.value[0].eval(context);
        }
        else {
            returnValue = this;
        }
        if (inParenthesis) {
            context.outOfParenthesis();
        }
        if (this.parens && this.parensInOp && !mathOn && !doubleParen
            && (!(returnValue instanceof Dimension))) {
            returnValue = new Paren(returnValue);
        }
        return returnValue;
    };
    Expression.prototype.genCSS = function (context, output) {
        for (var i_1 = 0; i_1 < this.value.length; i_1++) {
            this.value[i_1].genCSS(context, output);
            if (!this.noSpacing && i_1 + 1 < this.value.length) {
                output.add(' ');
            }
        }
    };
    Expression.prototype.throwAwayComments = function () {
        this.value = this.value.filter(function (v) { return !(v instanceof Comment); });
    };
    return Expression;
}(Node));
Expression.prototype.type = 'Expression';

var functionCaller = /** @class */ (function () {
    function functionCaller(name, context, index, currentFileInfo) {
        this.name = name.toLowerCase();
        this.index = index;
        this.context = context;
        this.currentFileInfo = currentFileInfo;
        this.func = context.frames[0].functionRegistry.get(this.name);
    }
    functionCaller.prototype.isValid = function () {
        return Boolean(this.func);
    };
    functionCaller.prototype.call = function (args) {
        // This code is terrible and should be replaced as per this issue...
        // https://github.com/less/less.js/issues/2477
        if (Array.isArray(args)) {
            args = args.filter(function (item) {
                if (item.type === 'Comment') {
                    return false;
                }
                return true;
            })
                .map(function (item) {
                if (item.type === 'Expression') {
                    var subNodes = item.value.filter(function (item) {
                        if (item.type === 'Comment') {
                            return false;
                        }
                        return true;
                    });
                    if (subNodes.length === 1) {
                        return subNodes[0];
                    }
                    else {
                        return new Expression(subNodes);
                    }
                }
                return item;
            });
        }
        return this.func.apply(this, args);
    };
    return functionCaller;
}());

//
// A function call node.
//
var Call = /** @class */ (function (_super) {
    tslib.__extends(Call, _super);
    function Call(name, args, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.name = name;
        _this.args = args;
        _this.calc = name === 'calc';
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        return _this;
    }
    Call.prototype.accept = function (visitor) {
        if (this.args) {
            this.args = visitor.visitArray(this.args);
        }
    };
    //
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
    Call.prototype.eval = function (context) {
        /**
         * Turn off math for calc(), and switch back on for evaluating nested functions
         */
        var currentMathContext = context.mathOn;
        context.mathOn = !this.calc;
        if (this.calc || context.inCalc) {
            context.enterCalc();
        }
        var args = this.args.map(function (a) { return a.eval(context); });
        if (this.calc || context.inCalc) {
            context.exitCalc();
        }
        context.mathOn = currentMathContext;
        var result;
        var funcCaller = new functionCaller(this.name, context, this.getIndex(), this.fileInfo());
        if (funcCaller.isValid()) {
            try {
                result = funcCaller.call(args);
            }
            catch (e) {
                throw {
                    type: e.type || 'Runtime',
                    message: "error evaluating function `" + this.name + "`" + (e.message ? ": " + e.message : ''),
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
                    }
                    else {
                        result = new Anonymous(result.toString());
                    }
                }
                result._index = this._index;
                result._fileInfo = this._fileInfo;
                return result;
            }
        }
        return new Call(this.name, args, this.getIndex(), this.fileInfo());
    };
    Call.prototype.genCSS = function (context, output) {
        output.add(this.name + "(", this.fileInfo(), this.getIndex());
        for (var i_1 = 0; i_1 < this.args.length; i_1++) {
            this.args[i_1].genCSS(context, output);
            if (i_1 + 1 < this.args.length) {
                output.add(', ');
            }
        }
        output.add(')');
    };
    return Call;
}(Node));
Call.prototype.type = 'Call';

var Variable = /** @class */ (function (_super) {
    tslib.__extends(Variable, _super);
    function Variable(name, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.name = name;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        return _this;
    }
    Variable.prototype.eval = function (context) {
        var variable;
        var name = this.name;
        if (name.indexOf('@@') === 0) {
            name = "@" + new Variable(name.slice(1), this.getIndex(), this.fileInfo()).eval(context).value;
        }
        if (this.evaluating) {
            throw { type: 'Name',
                message: "Recursive variable definition for " + name,
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
        this.evaluating = true;
        variable = this.find(context.frames, function (frame) {
            var v = frame.variable(name);
            if (v) {
                if (v.important) {
                    var importantScope = context.importantScope[context.importantScope.length - 1];
                    importantScope.important = v.important;
                }
                // If in calc, wrap vars in a function call to cascade evaluate args first
                if (context.inCalc) {
                    return (new Call('_SELF', [v.value])).eval(context);
                }
                else {
                    return v.value.eval(context);
                }
            }
        });
        if (variable) {
            this.evaluating = false;
            return variable;
        }
        else {
            throw { type: 'Name',
                message: "variable " + name + " is undefined",
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
    };
    Variable.prototype.find = function (obj, fun) {
        for (var i_1 = 0, r = void 0; i_1 < obj.length; i_1++) {
            r = fun.call(obj, obj[i_1]);
            if (r) {
                return r;
            }
        }
        return null;
    };
    return Variable;
}(Node));
Variable.prototype.type = 'Variable';

var Property = /** @class */ (function (_super) {
    tslib.__extends(Property, _super);
    function Property(name, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.name = name;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        return _this;
    }
    Property.prototype.eval = function (context) {
        var property;
        var name = this.name;
        // TODO: shorten this reference
        var mergeRules = context.pluginManager.less.visitors.ToCSSVisitor.prototype._mergeRules;
        if (this.evaluating) {
            throw { type: 'Name',
                message: "Recursive property reference for " + name,
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
        this.evaluating = true;
        property = this.find(context.frames, function (frame) {
            var v;
            var vArr = frame.property(name);
            if (vArr) {
                for (var i_1 = 0; i_1 < vArr.length; i_1++) {
                    v = vArr[i_1];
                    vArr[i_1] = new Declaration(v.name, v.value, v.important, v.merge, v.index, v.currentFileInfo, v.inline, v.variable);
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
        }
        else {
            throw { type: 'Name',
                message: "Property '" + name + "' is undefined",
                filename: this.currentFileInfo.filename,
                index: this.index };
        }
    };
    Property.prototype.find = function (obj, fun) {
        for (var i_2 = 0, r = void 0; i_2 < obj.length; i_2++) {
            r = fun.call(obj, obj[i_2]);
            if (r) {
                return r;
            }
        }
        return null;
    };
    return Property;
}(Node));
Property.prototype.type = 'Property';

var Attribute = /** @class */ (function (_super) {
    tslib.__extends(Attribute, _super);
    function Attribute(key, op, value) {
        var _this = _super.call(this) || this;
        _this.key = key;
        _this.op = op;
        _this.value = value;
        return _this;
    }
    Attribute.prototype.eval = function (context) {
        return new Attribute(this.key.eval ? this.key.eval(context) : this.key, this.op, (this.value && this.value.eval) ? this.value.eval(context) : this.value);
    };
    Attribute.prototype.genCSS = function (context, output) {
        output.add(this.toCSS(context));
    };
    Attribute.prototype.toCSS = function (context) {
        var value = this.key.toCSS ? this.key.toCSS(context) : this.key;
        if (this.op) {
            value += this.op;
            value += (this.value.toCSS ? this.value.toCSS(context) : this.value);
        }
        return "[" + value + "]";
    };
    return Attribute;
}(Node));
Attribute.prototype.type = 'Attribute';

var Quoted = /** @class */ (function (_super) {
    tslib.__extends(Quoted, _super);
    function Quoted(str, content, escaped, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.escaped = (escaped == null) ? true : escaped;
        _this.value = content || '';
        _this.quote = str.charAt(0);
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.variableRegex = /@\{([\w-]+)\}/g;
        _this.propRegex = /\$\{([\w-]+)\}/g;
        _this.allowRoot = escaped;
        return _this;
    }
    Quoted.prototype.genCSS = function (context, output) {
        if (!this.escaped) {
            output.add(this.quote, this.fileInfo(), this.getIndex());
        }
        output.add(this.value);
        if (!this.escaped) {
            output.add(this.quote);
        }
    };
    Quoted.prototype.containsVariables = function () {
        return this.value.match(this.variableRegex);
    };
    Quoted.prototype.eval = function (context) {
        var that = this;
        var value = this.value;
        var variableReplacement = function (_, name) {
            var v = new Variable("@" + name, that.getIndex(), that.fileInfo()).eval(context, true);
            return (v instanceof Quoted) ? v.value : v.toCSS();
        };
        var propertyReplacement = function (_, name) {
            var v = new Property("$" + name, that.getIndex(), that.fileInfo()).eval(context, true);
            return (v instanceof Quoted) ? v.value : v.toCSS();
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
    };
    Quoted.prototype.compare = function (other) {
        // when comparing quoted strings allow the quote to differ
        if (other.type === 'Quoted' && !this.escaped && !other.escaped) {
            return Node.numericCompare(this.value, other.value);
        }
        else {
            return other.toCSS && this.toCSS() === other.toCSS() ? 0 : undefined;
        }
    };
    return Quoted;
}(Node));
Quoted.prototype.type = 'Quoted';

var URL = /** @class */ (function (_super) {
    tslib.__extends(URL, _super);
    function URL(val, index, currentFileInfo, isEvald) {
        var _this = _super.call(this) || this;
        _this.value = val;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.isEvald = isEvald;
        return _this;
    }
    URL.prototype.accept = function (visitor) {
        this.value = visitor.visit(this.value);
    };
    URL.prototype.genCSS = function (context, output) {
        output.add('url(');
        this.value.genCSS(context, output);
        output.add(')');
    };
    URL.prototype.eval = function (context) {
        var val = this.value.eval(context);
        var rootpath;
        if (!this.isEvald) {
            // Add the rootpath if the URL requires a rewrite
            rootpath = this.fileInfo() && this.fileInfo().rootpath;
            if (typeof rootpath === 'string' &&
                typeof val.value === 'string' &&
                context.pathRequiresRewrite(val.value)) {
                if (!val.quote) {
                    rootpath = escapePath(rootpath);
                }
                val.value = context.rewritePath(val.value, rootpath);
            }
            else {
                val.value = context.normalizePath(val.value);
            }
            // Add url args if enabled
            if (context.urlArgs) {
                if (!val.value.match(/^\s*data:/)) {
                    var delimiter = val.value.indexOf('?') === -1 ? '?' : '&';
                    var urlArgs = delimiter + context.urlArgs;
                    if (val.value.indexOf('#') !== -1) {
                        val.value = val.value.replace('#', urlArgs + "#");
                    }
                    else {
                        val.value += urlArgs;
                    }
                }
            }
        }
        return new URL(val, this.getIndex(), this.fileInfo(), true);
    };
    return URL;
}(Node));
URL.prototype.type = 'Url';
function escapePath(path) {
    return path.replace(/[\(\)'"\s]/g, function (match) { return "\\" + match; });
}

var Media = /** @class */ (function (_super) {
    tslib.__extends(Media, _super);
    function Media(value, features, index, currentFileInfo, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        var selectors = (new Selector([], null, null, _this._index, _this._fileInfo)).createEmptySelectors();
        _this.features = new Value(features);
        _this.rules = [new Ruleset(selectors, value)];
        _this.rules[0].allowImports = true;
        _this.copyVisibilityInfo(visibilityInfo);
        _this.allowRoot = true;
        _this.setParent(selectors, _this);
        _this.setParent(_this.features, _this);
        _this.setParent(_this.rules, _this);
        return _this;
    }
    Media.prototype.isRulesetLike = function () {
        return true;
    };
    Media.prototype.accept = function (visitor) {
        if (this.features) {
            this.features = visitor.visit(this.features);
        }
        if (this.rules) {
            this.rules = visitor.visitArray(this.rules);
        }
    };
    Media.prototype.genCSS = function (context, output) {
        output.add('@media ', this._fileInfo, this._index);
        this.features.genCSS(context, output);
        this.outputRuleset(context, output, this.rules);
    };
    Media.prototype.eval = function (context) {
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
        return context.mediaPath.length === 0 ? media.evalTop(context) :
            media.evalNested(context);
    };
    Media.prototype.evalTop = function (context) {
        var result = this;
        // Render all dependent Media blocks.
        if (context.mediaBlocks.length > 1) {
            var selectors = (new Selector([], null, null, this.getIndex(), this.fileInfo())).createEmptySelectors();
            result = new Ruleset(selectors, context.mediaBlocks);
            result.multiMedia = true;
            result.copyVisibilityInfo(this.visibilityInfo());
            this.setParent(result, this);
        }
        delete context.mediaBlocks;
        delete context.mediaPath;
        return result;
    };
    Media.prototype.evalNested = function (context) {
        var i;
        var value;
        var path = context.mediaPath.concat([this]);
        // Extract the media-query conditions separated with `,` (OR).
        for (i = 0; i < path.length; i++) {
            value = path[i].features instanceof Value ?
                path[i].features.value : path[i].features;
            path[i] = Array.isArray(value) ? value : [value];
        }
        // Trace all permutations to generate the resulting media-query.
        //
        // (a, b and c) with nested (d, e) ->
        //    a and d
        //    a and e
        //    b and c and d
        //    b and c and e
        this.features = new Value(this.permute(path).map(function (path) {
            path = path.map(function (fragment) { return fragment.toCSS ? fragment : new Anonymous(fragment); });
            for (i = path.length - 1; i > 0; i--) {
                path.splice(i, 0, new Anonymous('and'));
            }
            return new Expression(path);
        }));
        this.setParent(this.features, this);
        // Fake a tree-node that doesn't output anything.
        return new Ruleset([], []);
    };
    Media.prototype.permute = function (arr) {
        if (arr.length === 0) {
            return [];
        }
        else if (arr.length === 1) {
            return arr[0];
        }
        else {
            var result = [];
            var rest = this.permute(arr.slice(1));
            for (var i_1 = 0; i_1 < rest.length; i_1++) {
                for (var j = 0; j < arr[0].length; j++) {
                    result.push([arr[0][j]].concat(rest[i_1]));
                }
            }
            return result;
        }
    };
    Media.prototype.bubbleSelectors = function (selectors) {
        if (!selectors) {
            return;
        }
        this.rules = [new Ruleset(copyArray(selectors), [this.rules[0]])];
        this.setParent(this.rules, this);
    };
    return Media;
}(AtRule));
Media.prototype.type = 'Media';

//
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
var Import = /** @class */ (function (_super) {
    tslib.__extends(Import, _super);
    function Import(path, features, options, index, currentFileInfo, visibilityInfo) {
        var _this = _super.call(this) || this;
        _this.options = options;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.path = path;
        _this.features = features;
        _this.allowRoot = true;
        if (_this.options.less !== undefined || _this.options.inline) {
            _this.css = !_this.options.less || _this.options.inline;
        }
        else {
            var pathValue = _this.getPath();
            if (pathValue && /[#\.\&\?]css([\?;].*)?$/.test(pathValue)) {
                _this.css = true;
            }
        }
        _this.copyVisibilityInfo(visibilityInfo);
        _this.setParent(_this.features, _this);
        _this.setParent(_this.path, _this);
        return _this;
    }
    Import.prototype.accept = function (visitor) {
        if (this.features) {
            this.features = visitor.visit(this.features);
        }
        this.path = visitor.visit(this.path);
        if (!this.options.isPlugin && !this.options.inline && this.root) {
            this.root = visitor.visit(this.root);
        }
    };
    Import.prototype.genCSS = function (context, output) {
        if (this.css && this.path._fileInfo.reference === undefined) {
            output.add('@import ', this._fileInfo, this._index);
            this.path.genCSS(context, output);
            if (this.features) {
                output.add(' ');
                this.features.genCSS(context, output);
            }
            output.add(';');
        }
    };
    Import.prototype.getPath = function () {
        return (this.path instanceof URL) ?
            this.path.value.value : this.path.value;
    };
    Import.prototype.isVariableImport = function () {
        var path = this.path;
        if (path instanceof URL) {
            path = path.value;
        }
        if (path instanceof Quoted) {
            return path.containsVariables();
        }
        return true;
    };
    Import.prototype.evalForImport = function (context) {
        var path = this.path;
        if (path instanceof URL) {
            path = path.value;
        }
        return new Import(path.eval(context), this.features, this.options, this._index, this._fileInfo, this.visibilityInfo());
    };
    Import.prototype.evalPath = function (context) {
        var path = this.path.eval(context);
        var fileInfo = this._fileInfo;
        if (!(path instanceof URL)) {
            // Add the rootpath if the URL requires a rewrite
            var pathValue = path.value;
            if (fileInfo &&
                pathValue &&
                context.pathRequiresRewrite(pathValue)) {
                path.value = context.rewritePath(pathValue, fileInfo.rootpath);
            }
            else {
                path.value = context.normalizePath(path.value);
            }
        }
        return path;
    };
    Import.prototype.eval = function (context) {
        var result = this.doEval(context);
        if (this.options.reference || this.blocksVisibility()) {
            if (result.length || result.length === 0) {
                result.forEach(function (node) {
                    node.addVisibilityBlock();
                });
            }
            else {
                result.addVisibilityBlock();
            }
        }
        return result;
    };
    Import.prototype.doEval = function (context) {
        var ruleset;
        var registry;
        var features = this.features && this.features.eval(context);
        if (this.options.isPlugin) {
            if (this.root && this.root.eval) {
                try {
                    this.root.eval(context);
                }
                catch (e) {
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
        }
        else if (this.css) {
            var newImport = new Import(this.evalPath(context), features, this.options, this._index);
            if (!newImport.css && this.error) {
                throw this.error;
            }
            return newImport;
        }
        else {
            ruleset = new Ruleset(null, copyArray(this.root.rules));
            ruleset.evalImports(context);
            return this.features ? new Media(ruleset.rules, this.features.value) : ruleset.rules;
        }
    };
    return Import;
}(Node));
Import.prototype.type = 'Import';

var JsEvalNode = /** @class */ (function (_super) {
    tslib.__extends(JsEvalNode, _super);
    function JsEvalNode() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    JsEvalNode.prototype.evaluateJavaScript = function (expression, context) {
        var result;
        var that = this;
        var evalContext = {};
        if (!context.javascriptEnabled) {
            throw { message: 'Inline JavaScript is not enabled. Is it set in your options?',
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
        expression = expression.replace(/@\{([\w-]+)\}/g, function (_, name) { return that.jsify(new Variable("@" + name, that.getIndex(), that.fileInfo()).eval(context)); });
        try {
            expression = new Function("return (" + expression + ")");
        }
        catch (e) {
            throw { message: "JavaScript evaluation error: " + e.message + " from `" + expression + "`",
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
        var variables = context.frames[0].variables();
        for (var k in variables) {
            if (variables.hasOwnProperty(k)) {
                /* jshint loopfunc:true */
                evalContext[k.slice(1)] = {
                    value: variables[k].value,
                    toJS: function () {
                        return this.value.eval(context).toCSS();
                    }
                };
            }
        }
        try {
            result = expression.call(evalContext);
        }
        catch (e) {
            throw { message: "JavaScript evaluation error: '" + e.name + ": " + e.message.replace(/["]/g, '\'') + "'",
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }
        return result;
    };
    JsEvalNode.prototype.jsify = function (obj) {
        if (Array.isArray(obj.value) && (obj.value.length > 1)) {
            return "[" + obj.value.map(function (v) { return v.toCSS(); }).join(', ') + "]";
        }
        else {
            return obj.toCSS();
        }
    };
    return JsEvalNode;
}(Node));

var JavaScript = /** @class */ (function (_super) {
    tslib.__extends(JavaScript, _super);
    function JavaScript(string, escaped, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.escaped = escaped;
        _this.expression = string;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        return _this;
    }
    JavaScript.prototype.eval = function (context) {
        var result = this.evaluateJavaScript(this.expression, context);
        var type = typeof result;
        if (type === 'number' && !isNaN(result)) {
            return new Dimension(result);
        }
        else if (type === 'string') {
            return new Quoted("\"" + result + "\"", result, this.escaped, this._index);
        }
        else if (Array.isArray(result)) {
            return new Anonymous(result.join(', '));
        }
        else {
            return new Anonymous(result);
        }
    };
    return JavaScript;
}(JsEvalNode));
JavaScript.prototype.type = 'JavaScript';

var Assignment = /** @class */ (function (_super) {
    tslib.__extends(Assignment, _super);
    function Assignment(key, val) {
        var _this = _super.call(this) || this;
        _this.key = key;
        _this.value = val;
        return _this;
    }
    Assignment.prototype.accept = function (visitor) {
        this.value = visitor.visit(this.value);
    };
    Assignment.prototype.eval = function (context) {
        if (this.value.eval) {
            return new Assignment(this.key, this.value.eval(context));
        }
        return this;
    };
    Assignment.prototype.genCSS = function (context, output) {
        output.add(this.key + "=");
        if (this.value.genCSS) {
            this.value.genCSS(context, output);
        }
        else {
            output.add(this.value);
        }
    };
    return Assignment;
}(Node));
Assignment.prototype.type = 'Assignment';

var Condition = /** @class */ (function (_super) {
    tslib.__extends(Condition, _super);
    function Condition(op, l, r, i, negate) {
        var _this = _super.call(this) || this;
        _this.op = op.trim();
        _this.lvalue = l;
        _this.rvalue = r;
        _this._index = i;
        _this.negate = negate;
        return _this;
    }
    Condition.prototype.accept = function (visitor) {
        this.lvalue = visitor.visit(this.lvalue);
        this.rvalue = visitor.visit(this.rvalue);
    };
    Condition.prototype.eval = function (context) {
        var result = (function (op, a, b) {
            switch (op) {
                case 'and': return a && b;
                case 'or': return a || b;
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
        })(this.op, this.lvalue.eval(context), this.rvalue.eval(context));
        return this.negate ? !result : result;
    };
    return Condition;
}(Node));
Condition.prototype.type = 'Condition';

var UnicodeDescriptor = /** @class */ (function (_super) {
    tslib.__extends(UnicodeDescriptor, _super);
    function UnicodeDescriptor(value) {
        var _this = _super.call(this) || this;
        _this.value = value;
        return _this;
    }
    return UnicodeDescriptor;
}(Node));
UnicodeDescriptor.prototype.type = 'UnicodeDescriptor';

var Negative = /** @class */ (function (_super) {
    tslib.__extends(Negative, _super);
    function Negative(node) {
        var _this = _super.call(this) || this;
        _this.value = node;
        return _this;
    }
    Negative.prototype.genCSS = function (context, output) {
        output.add('-');
        this.value.genCSS(context, output);
    };
    Negative.prototype.eval = function (context) {
        if (context.isMathOn()) {
            return (new Operation('*', [new Dimension(-1), this.value])).eval(context);
        }
        return new Negative(this.value.eval(context));
    };
    return Negative;
}(Node));
Negative.prototype.type = 'Negative';

var Extend = /** @class */ (function (_super) {
    tslib.__extends(Extend, _super);
    function Extend(selector, option, index, currentFileInfo, visibilityInfo) {
        var _this = _super.call(this) || this;
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
        _this.setParent(_this.selector, _this);
        return _this;
    }
    Extend.prototype.accept = function (visitor) {
        this.selector = visitor.visit(this.selector);
    };
    Extend.prototype.eval = function (context) {
        return new Extend(this.selector.eval(context), this.option, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    };
    Extend.prototype.clone = function (context) {
        return new Extend(this.selector, this.option, this.getIndex(), this.fileInfo(), this.visibilityInfo());
    };
    // it concatenates (joins) all selectors in selector array
    Extend.prototype.findSelfSelectors = function (selectors) {
        var selfElements = [];
        var i;
        var selectorElements;
        for (i = 0; i < selectors.length; i++) {
            selectorElements = selectors[i].elements;
            // duplicate the logic in genCSS function inside the selector node.
            // future TODO - move both logics into the selector joiner visitor
            if (i > 0 && selectorElements.length && selectorElements[0].combinator.value === '') {
                selectorElements[0].combinator.value = ' ';
            }
            selfElements = selfElements.concat(selectors[i].elements);
        }
        this.selfSelectors = [new Selector(selfElements)];
        this.selfSelectors[0].copyVisibilityInfo(this.visibilityInfo());
    };
    return Extend;
}(Node));
Extend.next_id = 0;
Extend.prototype.type = 'Extend';

var VariableCall = /** @class */ (function (_super) {
    tslib.__extends(VariableCall, _super);
    function VariableCall(variable, index, currentFileInfo) {
        var _this = _super.call(this) || this;
        _this.variable = variable;
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.allowRoot = true;
        return _this;
    }
    VariableCall.prototype.eval = function (context) {
        var rules;
        var detachedRuleset = new Variable(this.variable, this.getIndex(), this.fileInfo()).eval(context);
        var error = new LessError({ message: "Could not evaluate variable call " + this.variable });
        if (!detachedRuleset.ruleset) {
            if (detachedRuleset.rules) {
                rules = detachedRuleset;
            }
            else if (Array.isArray(detachedRuleset)) {
                rules = new Ruleset('', detachedRuleset);
            }
            else if (Array.isArray(detachedRuleset.value)) {
                rules = new Ruleset('', detachedRuleset.value);
            }
            else {
                throw error;
            }
            detachedRuleset = new DetachedRuleset(rules);
        }
        if (detachedRuleset.ruleset) {
            return detachedRuleset.callEval(context);
        }
        throw error;
    };
    return VariableCall;
}(Node));
VariableCall.prototype.type = 'VariableCall';

var NamespaceValue = /** @class */ (function (_super) {
    tslib.__extends(NamespaceValue, _super);
    function NamespaceValue(ruleCall, lookups, index, fileInfo) {
        var _this = _super.call(this) || this;
        _this.value = ruleCall;
        _this.lookups = lookups;
        _this._index = index;
        _this._fileInfo = fileInfo;
        return _this;
    }
    NamespaceValue.prototype.eval = function (context) {
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
            }
            else if (name.charAt(0) === '@') {
                if (name.charAt(1) === '@') {
                    name = "@" + new Variable(name.substr(1)).eval(context).value;
                }
                if (rules.variables) {
                    rules = rules.variable(name);
                }
                if (!rules) {
                    throw { type: 'Name',
                        message: "variable " + name + " not found",
                        filename: this.fileInfo().filename,
                        index: this.getIndex() };
                }
            }
            else {
                if (name.substring(0, 2) === '$@') {
                    name = "$" + new Variable(name.substr(1)).eval(context).value;
                }
                else {
                    name = name.charAt(0) === '$' ? name : "$" + name;
                }
                if (rules.properties) {
                    rules = rules.property(name);
                }
                if (!rules) {
                    throw { type: 'Name',
                        message: "property \"" + name.substr(1) + "\" not found",
                        filename: this.fileInfo().filename,
                        index: this.getIndex() };
                }
                // Properties are an array of values, since a ruleset can have multiple props.
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
    };
    return NamespaceValue;
}(Node));
NamespaceValue.prototype.type = 'NamespaceValue';

var Definition = /** @class */ (function (_super) {
    tslib.__extends(Definition, _super);
    function Definition(name, params, rules, condition, variadic, frames, visibilityInfo) {
        var _this = _super.call(this) || this;
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
            if (!p.name || (p.name && !p.value)) {
                return count + 1;
            }
            else {
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
    Definition.prototype.accept = function (visitor) {
        if (this.params && this.params.length) {
            this.params = visitor.visitArray(this.params);
        }
        this.rules = visitor.visitArray(this.rules);
        if (this.condition) {
            this.condition = visitor.visit(this.condition);
        }
    };
    Definition.prototype.evalParams = function (context, mixinEnv, args, evaldArguments) {
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
                if (name = (arg && arg.name)) {
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
                    }
                    else {
                        throw { type: 'Runtime', message: "Named argument for " + this.name + " " + args[i].name + " not found" };
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
                }
                else {
                    val = arg && arg.value;
                    if (val) {
                        // This was a mixin call, pass in a detached ruleset of it's eval'd rules
                        if (Array.isArray(val)) {
                            val = new DetachedRuleset(new Ruleset('', val));
                        }
                        else {
                            val = val.eval(context);
                        }
                    }
                    else if (params[i].value) {
                        val = params[i].value.eval(mixinEnv);
                        frame.resetCache();
                    }
                    else {
                        throw { type: 'Runtime', message: "wrong number of arguments for " + this.name + " (" + argsLength + " for " + this.arity + ")" };
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
    };
    Definition.prototype.makeImportant = function () {
        var rules = !this.rules ? this.rules : this.rules.map(function (r) {
            if (r.makeImportant) {
                return r.makeImportant(true);
            }
            else {
                return r;
            }
        });
        var result = new Definition(this.name, this.params, rules, this.condition, this.variadic, this.frames);
        return result;
    };
    Definition.prototype.eval = function (context) {
        return new Definition(this.name, this.params, this.rules, this.condition, this.variadic, this.frames || copyArray(context.frames));
    };
    Definition.prototype.evalCall = function (context, args, important) {
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
    };
    Definition.prototype.matchCondition = function (args, context) {
        if (this.condition && !this.condition.eval(new contexts.Eval(context, [this.evalParams(context, /* the parameter variables */ new contexts.Eval(context, this.frames ? this.frames.concat(context.frames) : context.frames), args, [])]
            .concat(this.frames || []) // the parent namespace/mixin frames
            .concat(context.frames)))) { // the current environment frames
            return false;
        }
        return true;
    };
    Definition.prototype.matchArgs = function (args, context) {
        var allArgsCnt = (args && args.length) || 0;
        var len;
        var optionalParameters = this.optionalParameters;
        var requiredArgsCnt = !args ? 0 : args.reduce(function (count, p) {
            if (optionalParameters.indexOf(p.name) < 0) {
                return count + 1;
            }
            else {
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
        }
        else {
            if (requiredArgsCnt < (this.required - 1)) {
                return false;
            }
        }
        // check patterns
        len = Math.min(requiredArgsCnt, this.arity);
        for (var i_1 = 0; i_1 < len; i_1++) {
            if (!this.params[i_1].name && !this.params[i_1].variadic) {
                if (args[i_1].value.eval(context).toCSS() != this.params[i_1].value.eval(context).toCSS()) {
                    return false;
                }
            }
        }
        return true;
    };
    return Definition;
}(Ruleset));
Definition.prototype.type = 'MixinDefinition';
Definition.prototype.evalFirst = true;

var MixinCall = /** @class */ (function (_super) {
    tslib.__extends(MixinCall, _super);
    function MixinCall(elements, args, index, currentFileInfo, important) {
        var _this = _super.call(this) || this;
        _this.selector = new Selector(elements);
        _this.arguments = args || [];
        _this._index = index;
        _this._fileInfo = currentFileInfo;
        _this.important = important;
        _this.allowRoot = true;
        _this.setParent(_this.selector, _this);
        return _this;
    }
    MixinCall.prototype.accept = function (visitor) {
        if (this.selector) {
            this.selector = visitor.visit(this.selector);
        }
        if (this.arguments.length) {
            this.arguments = visitor.visitArray(this.arguments);
        }
    };
    MixinCall.prototype.eval = function (context) {
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
                    return conditionResult[1] ?
                        defTrue : defFalse;
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
                    args.push({ value: argValue[m] });
                }
            }
            else {
                args.push({ name: arg.name, value: argValue });
            }
        }
        noArgumentsFilter = function (rule) { return rule.matchArgs(null, context); };
        for (i = 0; i < context.frames.length; i++) {
            if ((mixins = context.frames[i].find(this.selector, null, noArgumentsFilter)).length > 0) {
                isOneFound = true;
                // To make `default()` function independent of definition order we have two "subpasses" here.
                // At first we evaluate each guard *twice* (with `default() == true` and `default() == false`),
                // and build candidate list with corresponding flags. Then, when we know all possible matches,
                // we make a final decision.
                for (m = 0; m < mixins.length; m++) {
                    mixin = mixins[m].rule;
                    mixinPath = mixins[m].path;
                    isRecursive = false;
                    for (f = 0; f < context.frames.length; f++) {
                        if ((!(mixin instanceof Definition)) && mixin === (context.frames[f].originalRuleset || context.frames[f])) {
                            isRecursive = true;
                            break;
                        }
                    }
                    if (isRecursive) {
                        continue;
                    }
                    if (mixin.matchArgs(args, context)) {
                        candidate = { mixin: mixin, group: calcDefGroup(mixin, mixinPath) };
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
                }
                else {
                    defaultResult = defTrue;
                    if ((count[defTrue] + count[defFalse]) > 1) {
                        throw { type: 'Runtime',
                            message: "Ambiguous use of `default()` found when matching for `" + this.format(args) + "`",
                            index: this.getIndex(), filename: this.fileInfo().filename };
                    }
                }
                for (m = 0; m < candidates.length; m++) {
                    candidate = candidates[m].group;
                    if ((candidate === defNone) || (candidate === defaultResult)) {
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
                        }
                        catch (e) {
                            throw { message: e.message, index: this.getIndex(), filename: this.fileInfo().filename, stack: e.stack };
                        }
                    }
                }
                if (match) {
                    return rules;
                }
            }
        }
        if (isOneFound) {
            throw { type: 'Runtime',
                message: "No matching definition was found for `" + this.format(args) + "`",
                index: this.getIndex(), filename: this.fileInfo().filename };
        }
        else {
            throw { type: 'Name',
                message: this.selector.toCSS().trim() + " is undefined",
                index: this.getIndex(), filename: this.fileInfo().filename };
        }
    };
    MixinCall.prototype._setVisibilityToReplacement = function (replacement) {
        var i;
        var rule;
        if (this.blocksVisibility()) {
            for (i = 0; i < replacement.length; i++) {
                rule = replacement[i];
                rule.addVisibilityBlock();
            }
        }
    };
    MixinCall.prototype.format = function (args) {
        return this.selector.toCSS().trim() + "(" + (args ? args.map(function (a) {
            var argValue = '';
            if (a.name) {
                argValue += a.name + ":";
            }
            if (a.value.toCSS) {
                argValue += a.value.toCSS();
            }
            else {
                argValue += '???';
            }
            return argValue;
        }).join(', ') : '') + ")";
    };
    return MixinCall;
}(Node));
MixinCall.prototype.type = 'MixinCall';

var tree = {
    Node: Node, Color: Color, AtRule: AtRule, DetachedRuleset: DetachedRuleset, Operation: Operation,
    Dimension: Dimension, Unit: Unit, Keyword: Keyword, Variable: Variable, Property: Property,
    Ruleset: Ruleset, Element: Element, Attribute: Attribute, Combinator: Combinator, Selector: Selector,
    Quoted: Quoted, Expression: Expression, Declaration: Declaration, Call: Call, URL: URL, Import: Import,
    Comment: Comment, Anonymous: Anonymous, Value: Value, JavaScript: JavaScript, Assignment: Assignment,
    Condition: Condition, Paren: Paren, Media: Media, UnicodeDescriptor: UnicodeDescriptor, Negative: Negative,
    Extend: Extend, VariableCall: VariableCall, NamespaceValue: NamespaceValue,
    mixin: {
        Call: MixinCall,
        Definition: Definition
    }
};

/**
 * @todo Document why this abstraction exists, and the relationship between
 *       environment, file managers, and plugin manager
 */
var environment$1 = /** @class */ (function () {
    function environment(externalEnvironment, fileManagers) {
        this.fileManagers = fileManagers || [];
        externalEnvironment = externalEnvironment || {};
        var optionalFunctions = ['encodeBase64', 'mimeLookup', 'charsetLookup', 'getSourceMapGenerator'];
        var requiredFunctions = [];
        var functions = requiredFunctions.concat(optionalFunctions);
        for (var i_1 = 0; i_1 < functions.length; i_1++) {
            var propName = functions[i_1];
            var environmentFunc = externalEnvironment[propName];
            if (environmentFunc) {
                this[propName] = environmentFunc.bind(externalEnvironment);
            }
            else if (i_1 < requiredFunctions.length) {
                this.warn("missing required function in environment - " + propName);
            }
        }
    }
    environment.prototype.getFileManager = function (filename, currentDirectory, options, environment, isSync) {
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
        for (var i_2 = fileManagers.length - 1; i_2 >= 0; i_2--) {
            var fileManager = fileManagers[i_2];
            if (fileManager[isSync ? 'supportsSync' : 'supports'](filename, currentDirectory, options, environment)) {
                return fileManager;
            }
        }
        return null;
    };
    environment.prototype.addFileManager = function (fileManager) {
        this.fileManagers.push(fileManager);
    };
    environment.prototype.clearFileManagers = function () {
        this.fileManagers = [];
    };
    return environment;
}());

var AbstractPluginLoader = /** @class */ (function () {
    function AbstractPluginLoader() {
        // Implemented by Node.js plugin loader
        this.require = function () { return null; };
    }
    AbstractPluginLoader.prototype.evalPlugin = function (contents, context, imports, pluginOptions, fileInfo) {
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
            }
            else {
                filename = fileInfo.filename;
            }
        }
        var shortname = (new this.less.FileManager()).extractUrlParts(filename).filename;
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
                }
                catch (e) {
                    e.message = e.message || 'Error during @plugin call';
                    return new LessError(e, imports, filename);
                }
                return pluginObj;
            }
        }
        localModule = {
            exports: {},
            pluginManager: pluginManager,
            fileInfo: fileInfo
        };
        registry = functionRegistry.create();
        var registerPlugin = function (obj) {
            pluginObj = obj;
        };
        try {
            loader = new Function('module', 'require', 'registerPlugin', 'functions', 'tree', 'less', 'fileInfo', contents);
            loader(localModule, this.require(filename), registerPlugin, registry, this.less.tree, this.less, fileInfo);
        }
        catch (e) {
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
            pluginObj.filename = filename;
            // For < 3.x (or unspecified minVersion) - setOptions() before install()
            if (!pluginObj.minVersion || this.compareVersion('3.0.0', pluginObj.minVersion) < 0) {
                result = this.trySetOptions(pluginObj, filename, shortname, pluginOptions);
                if (result) {
                    return result;
                }
            }
            // Run on first load
            pluginManager.addPlugin(pluginObj, fileInfo.filename, registry);
            pluginObj.functions = registry.getLocalFunctions();
            // Need to call setOptions again because the pluginObj might have functions
            result = this.trySetOptions(pluginObj, filename, shortname, pluginOptions);
            if (result) {
                return result;
            }
            // Run every @plugin call
            try {
                if (pluginObj.use) {
                    pluginObj.use.call(this.context, pluginObj);
                }
            }
            catch (e) {
                e.message = e.message || 'Error during @plugin call';
                return new LessError(e, imports, filename);
            }
        }
        else {
            return new LessError({ message: 'Not a valid plugin' }, imports, filename);
        }
        return pluginObj;
    };
    AbstractPluginLoader.prototype.trySetOptions = function (plugin, filename, name, options) {
        if (options && !plugin.setOptions) {
            return new LessError({
                message: "Options have been provided but the plugin " + name + " does not support any options."
            });
        }
        try {
            plugin.setOptions && plugin.setOptions(options);
        }
        catch (e) {
            return new LessError(e);
        }
    };
    AbstractPluginLoader.prototype.validatePlugin = function (plugin, filename, name) {
        if (plugin) {
            // support plugins being a function
            // so that the plugin can be more usable programmatically
            if (typeof plugin === 'function') {
                plugin = new plugin();
            }
            if (plugin.minVersion) {
                if (this.compareVersion(plugin.minVersion, this.less.version) < 0) {
                    return new LessError({
                        message: "Plugin " + name + " requires version " + this.versionToString(plugin.minVersion)
                    });
                }
            }
            return plugin;
        }
        return null;
    };
    AbstractPluginLoader.prototype.compareVersion = function (aVersion, bVersion) {
        if (typeof aVersion === 'string') {
            aVersion = aVersion.match(/^(\d+)\.?(\d+)?\.?(\d+)?/);
            aVersion.shift();
        }
        for (var i_1 = 0; i_1 < aVersion.length; i_1++) {
            if (aVersion[i_1] !== bVersion[i_1]) {
                return parseInt(aVersion[i_1]) > parseInt(bVersion[i_1]) ? -1 : 1;
            }
        }
        return 0;
    };
    AbstractPluginLoader.prototype.versionToString = function (version) {
        var versionString = '';
        for (var i_2 = 0; i_2 < version.length; i_2++) {
            versionString += (versionString ? '.' : '') + version[i_2];
        }
        return versionString;
    };
    AbstractPluginLoader.prototype.printUsage = function (plugins) {
        for (var i_3 = 0; i_3 < plugins.length; i_3++) {
            var plugin = plugins[i_3];
            if (plugin.printUsage) {
                plugin.printUsage();
            }
        }
    };
    return AbstractPluginLoader;
}());

var _visitArgs = { visitDeeper: true };
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
var Visitor = /** @class */ (function () {
    function Visitor(implementation) {
        this._implementation = implementation;
        this._visitInCache = {};
        this._visitOutCache = {};
        if (!_hasIndexed) {
            indexNodeTypes(tree, 1);
            _hasIndexed = true;
        }
    }
    Visitor.prototype.visit = function (node) {
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
            fnName = "visit" + node.type;
            func = impl[fnName] || _noop;
            funcOut = impl[fnName + "Out"] || _noop;
            this._visitInCache[nodeTypeIndex] = func;
            this._visitOutCache[nodeTypeIndex] = funcOut;
        }
        if (func !== _noop) {
            var newNode = func.call(impl, node, visitArgs);
            if (node && impl.isReplacing) {
                node = newNode;
            }
        }
        if (visitArgs.visitDeeper && node) {
            if (node.length) {
                for (var i = 0, cnt = node.length; i < cnt; i++) {
                    if (node[i].accept) {
                        node[i].accept(this);
                    }
                }
            }
            else if (node.accept) {
                node.accept(this);
            }
        }
        if (funcOut != _noop) {
            funcOut.call(impl, node);
        }
        return node;
    };
    Visitor.prototype.visitArray = function (nodes, nonReplacing) {
        if (!nodes) {
            return nodes;
        }
        var cnt = nodes.length;
        var i;
        // Non-replacing
        if (nonReplacing || !this._implementation.isReplacing) {
            for (i = 0; i < cnt; i++) {
                this.visit(nodes[i]);
            }
            return nodes;
        }
        // Replacing
        var out = [];
        for (i = 0; i < cnt; i++) {
            var evald = this.visit(nodes[i]);
            if (evald === undefined) {
                continue;
            }
            if (!evald.splice) {
                out.push(evald);
            }
            else if (evald.length) {
                this.flatten(evald, out);
            }
        }
        return out;
    };
    Visitor.prototype.flatten = function (arr, out) {
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
                }
                else if (nestedItem.length) {
                    this.flatten(nestedItem, out);
                }
            }
        }
        return out;
    };
    return Visitor;
}());

var ImportSequencer = /** @class */ (function () {
    function ImportSequencer(onSequencerEmpty) {
        this.imports = [];
        this.variableImports = [];
        this._onSequencerEmpty = onSequencerEmpty;
        this._currentDepth = 0;
    }
    ImportSequencer.prototype.addImport = function (callback) {
        var importSequencer = this;
        var importItem = {
            callback: callback,
            args: null,
            isReady: false
        };
        this.imports.push(importItem);
        return function () {
            var args = [];
            for (var _i = 0; _i < arguments.length; _i++) {
                args[_i] = arguments[_i];
            }
            importItem.args = Array.prototype.slice.call(args, 0);
            importItem.isReady = true;
            importSequencer.tryRun();
        };
    };
    ImportSequencer.prototype.addVariableImport = function (callback) {
        this.variableImports.push(callback);
    };
    ImportSequencer.prototype.tryRun = function () {
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
        }
        finally {
            this._currentDepth--;
        }
        if (this._currentDepth === 0 && this._onSequencerEmpty) {
            this._onSequencerEmpty();
        }
    };
    return ImportSequencer;
}());

var ImportVisitor = function (importer, finish) {
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
    run: function (root) {
        try {
            // process the contents
            this._visitor.visit(root);
        }
        catch (e) {
            this.error = e;
        }
        this.isFinished = true;
        this._sequencer.tryRun();
    },
    _onSequencerEmpty: function () {
        if (!this.isFinished) {
            return;
        }
        this._finish(this.error);
    },
    visitImport: function (importNode, visitArgs) {
        var inlineCSS = importNode.options.inline;
        if (!importNode.css || inlineCSS) {
            var context = new contexts.Eval(this.context, copyArray(this.context.frames));
            var importParent = context.frames[0];
            this.importCount++;
            if (importNode.isVariableImport()) {
                this._sequencer.addVariableImport(this.processImportNode.bind(this, importNode, context, importParent));
            }
            else {
                this.processImportNode(importNode, context, importParent);
            }
        }
        visitArgs.visitDeeper = false;
    },
    processImportNode: function (importNode, context, importParent) {
        var evaldImportNode;
        var inlineCSS = importNode.options.inline;
        try {
            evaldImportNode = importNode.evalForImport(context);
        }
        catch (e) {
            if (!e.filename) {
                e.index = importNode.getIndex();
                e.filename = importNode.fileInfo().filename;
            }
            // attempt to eval properly and treat as css
            importNode.css = true;
            // if that fails, this error will be thrown
            importNode.error = e;
        }
        if (evaldImportNode && (!evaldImportNode.css || inlineCSS)) {
            if (evaldImportNode.options.multiple) {
                context.importMultiple = true;
            }
            // try appending if we haven't determined if it is css or not
            var tryAppendLessExtension = evaldImportNode.css === undefined;
            for (var i_1 = 0; i_1 < importParent.rules.length; i_1++) {
                if (importParent.rules[i_1] === importNode) {
                    importParent.rules[i_1] = evaldImportNode;
                    break;
                }
            }
            var onImported = this.onImported.bind(this, evaldImportNode, context);
            var sequencedOnImported = this._sequencer.addImport(onImported);
            this._importer.push(evaldImportNode.getPath(), tryAppendLessExtension, evaldImportNode.fileInfo(), evaldImportNode.options, sequencedOnImported);
        }
        else {
            this.importCount--;
            if (this.isFinished) {
                this._sequencer.tryRun();
            }
        }
    },
    onImported: function (importNode, context, e, root, importedAtRoot, fullPath) {
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
            }
            else {
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
                }
                catch (e) {
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
    visitDeclaration: function (declNode, visitArgs) {
        if (declNode.value.type === 'DetachedRuleset') {
            this.context.frames.unshift(declNode);
        }
        else {
            visitArgs.visitDeeper = false;
        }
    },
    visitDeclarationOut: function (declNode) {
        if (declNode.value.type === 'DetachedRuleset') {
            this.context.frames.shift();
        }
    },
    visitAtRule: function (atRuleNode, visitArgs) {
        this.context.frames.unshift(atRuleNode);
    },
    visitAtRuleOut: function (atRuleNode) {
        this.context.frames.shift();
    },
    visitMixinDefinition: function (mixinDefinitionNode, visitArgs) {
        this.context.frames.unshift(mixinDefinitionNode);
    },
    visitMixinDefinitionOut: function (mixinDefinitionNode) {
        this.context.frames.shift();
    },
    visitRuleset: function (rulesetNode, visitArgs) {
        this.context.frames.unshift(rulesetNode);
    },
    visitRulesetOut: function (rulesetNode) {
        this.context.frames.shift();
    },
    visitMedia: function (mediaNode, visitArgs) {
        this.context.frames.unshift(mediaNode.rules[0]);
    },
    visitMediaOut: function (mediaNode) {
        this.context.frames.shift();
    }
};

var SetTreeVisibilityVisitor = /** @class */ (function () {
    function SetTreeVisibilityVisitor(visible) {
        this.visible = visible;
    }
    SetTreeVisibilityVisitor.prototype.run = function (root) {
        this.visit(root);
    };
    SetTreeVisibilityVisitor.prototype.visitArray = function (nodes) {
        if (!nodes) {
            return nodes;
        }
        var cnt = nodes.length;
        var i;
        for (i = 0; i < cnt; i++) {
            this.visit(nodes[i]);
        }
        return nodes;
    };
    SetTreeVisibilityVisitor.prototype.visit = function (node) {
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
        }
        else {
            node.ensureInvisibility();
        }
        node.accept(this);
        return node;
    };
    return SetTreeVisibilityVisitor;
}());

/* jshint loopfunc:true */
var ExtendFinderVisitor = /** @class */ (function () {
    function ExtendFinderVisitor() {
        this._visitor = new Visitor(this);
        this.contexts = [];
        this.allExtendsStack = [[]];
    }
    ExtendFinderVisitor.prototype.run = function (root) {
        root = this._visitor.visit(root);
        root.allExtends = this.allExtendsStack[0];
        return root;
    };
    ExtendFinderVisitor.prototype.visitDeclaration = function (declNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    ExtendFinderVisitor.prototype.visitMixinDefinition = function (mixinDefinitionNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    ExtendFinderVisitor.prototype.visitRuleset = function (rulesetNode, visitArgs) {
        if (rulesetNode.root) {
            return;
        }
        var i;
        var j;
        var extend;
        var allSelectorsExtendList = [];
        var extendList;
        // get &:extend(.a); rules which apply to all selectors in this ruleset
        var rules = rulesetNode.rules;
        var ruleCnt = rules ? rules.length : 0;
        for (i = 0; i < ruleCnt; i++) {
            if (rulesetNode.rules[i] instanceof tree.Extend) {
                allSelectorsExtendList.push(rules[i]);
                rulesetNode.extendOnEveryPath = true;
            }
        }
        // now find every selector and apply the extends that apply to all extends
        // and the ones which apply to an individual extend
        var paths = rulesetNode.paths;
        for (i = 0; i < paths.length; i++) {
            var selectorPath = paths[i];
            var selector = selectorPath[selectorPath.length - 1];
            var selExtendList = selector.extendList;
            extendList = selExtendList ? copyArray(selExtendList).concat(allSelectorsExtendList)
                : allSelectorsExtendList;
            if (extendList) {
                extendList = extendList.map(function (allSelectorsExtend) { return allSelectorsExtend.clone(); });
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
    };
    ExtendFinderVisitor.prototype.visitRulesetOut = function (rulesetNode) {
        if (!rulesetNode.root) {
            this.contexts.length = this.contexts.length - 1;
        }
    };
    ExtendFinderVisitor.prototype.visitMedia = function (mediaNode, visitArgs) {
        mediaNode.allExtends = [];
        this.allExtendsStack.push(mediaNode.allExtends);
    };
    ExtendFinderVisitor.prototype.visitMediaOut = function (mediaNode) {
        this.allExtendsStack.length = this.allExtendsStack.length - 1;
    };
    ExtendFinderVisitor.prototype.visitAtRule = function (atRuleNode, visitArgs) {
        atRuleNode.allExtends = [];
        this.allExtendsStack.push(atRuleNode.allExtends);
    };
    ExtendFinderVisitor.prototype.visitAtRuleOut = function (atRuleNode) {
        this.allExtendsStack.length = this.allExtendsStack.length - 1;
    };
    return ExtendFinderVisitor;
}());
var ProcessExtendsVisitor = /** @class */ (function () {
    function ProcessExtendsVisitor() {
        this._visitor = new Visitor(this);
    }
    ProcessExtendsVisitor.prototype.run = function (root) {
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
    };
    ProcessExtendsVisitor.prototype.checkExtendsForNonMatched = function (extendList) {
        var indices = this.extendIndices;
        extendList.filter(function (extend) { return !extend.hasFoundMatches && extend.parent_ids.length == 1; }).forEach(function (extend) {
            var selector = '_unknown_';
            try {
                selector = extend.selector.toCSS({});
            }
            catch (_) { }
            if (!indices[extend.index + " " + selector]) {
                indices[extend.index + " " + selector] = true;
                logger.warn("extend '" + selector + "' has no matches");
            }
        });
    };
    ProcessExtendsVisitor.prototype.doExtendChaining = function (extendsList, extendsListTarget, iterationCount) {
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
        iterationCount = iterationCount || 0;
        // loop through comparing every extend with every target extend.
        // a target extend is the one on the ruleset we are looking at copy/edit/pasting in place
        // e.g.  .a:extend(.b) {}  and .b:extend(.c) {} then the first extend extends the second one
        // and the second is the target.
        // the separation into two lists allows us to process a subset of chains with a bigger set, as is the
        // case when processing media queries
        for (extendIndex = 0; extendIndex < extendsList.length; extendIndex++) {
            for (targetExtendIndex = 0; targetExtendIndex < extendsListTarget.length; targetExtendIndex++) {
                extend = extendsList[extendIndex];
                targetExtend = extendsListTarget[targetExtendIndex];
                // look for circular references
                if (extend.parent_ids.indexOf(targetExtend.object_id) >= 0) {
                    continue;
                }
                // find a match in the target extends self selector (the bit before :extend)
                selectorPath = [targetExtend.selfSelectors[0]];
                matches = extendVisitor.findMatch(extend, selectorPath);
                if (matches.length) {
                    extend.hasFoundMatches = true;
                    // we found a match, so for each self selector..
                    extend.selfSelectors.forEach(function (selfSelector) {
                        var info = targetExtend.visibilityInfo();
                        // process the extend as usual
                        newSelector = extendVisitor.extendSelector(matches, selectorPath, selfSelector, extend.isVisible());
                        // but now we create a new extend from it
                        newExtend = new (tree.Extend)(targetExtend.selector, targetExtend.option, 0, targetExtend.fileInfo(), info);
                        newExtend.selfSelectors = newSelector;
                        // add the extend onto the list of extends for that selector
                        newSelector[newSelector.length - 1].extendList = [newExtend];
                        // record that we need to add it.
                        extendsToAdd.push(newExtend);
                        newExtend.ruleset = targetExtend.ruleset;
                        // remember its parents for circular references
                        newExtend.parent_ids = newExtend.parent_ids.concat(targetExtend.parent_ids, extend.parent_ids);
                        // only process the selector once.. if we have :extend(.a,.b) then multiple
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
                }
                catch (e) { }
                throw { message: "extend circular reference detected. One of the circular extends is currently:" + selectorOne + ":extend(" + selectorTwo + ")" };
            }
            // now process the new extends on the existing rules so that we can handle a extending b extending c extending
            // d extending e...
            return extendsToAdd.concat(extendVisitor.doExtendChaining(extendsToAdd, extendsListTarget, iterationCount + 1));
        }
        else {
            return extendsToAdd;
        }
    };
    ProcessExtendsVisitor.prototype.visitDeclaration = function (ruleNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    ProcessExtendsVisitor.prototype.visitMixinDefinition = function (mixinDefinitionNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    ProcessExtendsVisitor.prototype.visitSelector = function (selectorNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    ProcessExtendsVisitor.prototype.visitRuleset = function (rulesetNode, visitArgs) {
        if (rulesetNode.root) {
            return;
        }
        var matches;
        var pathIndex;
        var extendIndex;
        var allExtends = this.allExtendsStack[this.allExtendsStack.length - 1];
        var selectorsToAdd = [];
        var extendVisitor = this;
        var selectorPath;
        // look at each selector path in the ruleset, find any extend matches and then copy, find and replace
        for (extendIndex = 0; extendIndex < allExtends.length; extendIndex++) {
            for (pathIndex = 0; pathIndex < rulesetNode.paths.length; pathIndex++) {
                selectorPath = rulesetNode.paths[pathIndex];
                // extending extends happens initially, before the main pass
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
    };
    ProcessExtendsVisitor.prototype.findMatch = function (extend, haystackSelectorPath) {
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
        var matches = [];
        // loop through the haystack elements
        for (haystackSelectorIndex = 0; haystackSelectorIndex < haystackSelectorPath.length; haystackSelectorIndex++) {
            hackstackSelector = haystackSelectorPath[haystackSelectorIndex];
            for (hackstackElementIndex = 0; hackstackElementIndex < hackstackSelector.elements.length; hackstackElementIndex++) {
                haystackElement = hackstackSelector.elements[hackstackElementIndex];
                // if we allow elements before our match we can add a potential match every time. otherwise only at the first element.
                if (extend.allowBefore || (haystackSelectorIndex === 0 && hackstackElementIndex === 0)) {
                    potentialMatches.push({ pathIndex: haystackSelectorIndex, index: hackstackElementIndex, matched: 0,
                        initialCombinator: haystackElement.combinator });
                }
                for (i = 0; i < potentialMatches.length; i++) {
                    potentialMatch = potentialMatches[i];
                    // selectors add " " onto the first element. When we use & it joins the selectors together, but if we don't
                    // then each selector in haystackSelectorPath has a space before it added in the toCSS phase. so we need to
                    // work out what the resulting combinator will be
                    targetCombinator = haystackElement.combinator.value;
                    if (targetCombinator === '' && hackstackElementIndex === 0) {
                        targetCombinator = ' ';
                    }
                    // if we don't match, null our match to indicate failure
                    if (!extendVisitor.isElementValuesEqual(needleElements[potentialMatch.matched].value, haystackElement.value) ||
                        (potentialMatch.matched > 0 && needleElements[potentialMatch.matched].combinator.value !== targetCombinator)) {
                        potentialMatch = null;
                    }
                    else {
                        potentialMatch.matched++;
                    }
                    // if we are still valid and have finished, test whether we have elements after and whether these are allowed
                    if (potentialMatch) {
                        potentialMatch.finished = potentialMatch.matched === needleElements.length;
                        if (potentialMatch.finished &&
                            (!extend.allowAfter &&
                                (hackstackElementIndex + 1 < hackstackSelector.elements.length || haystackSelectorIndex + 1 < haystackSelectorPath.length))) {
                            potentialMatch = null;
                        }
                    }
                    // if null we remove, if not, we are still valid, so either push as a valid match or continue
                    if (potentialMatch) {
                        if (potentialMatch.finished) {
                            potentialMatch.length = needleElements.length;
                            potentialMatch.endPathIndex = haystackSelectorIndex;
                            potentialMatch.endPathElementIndex = hackstackElementIndex + 1; // index after end of match
                            potentialMatches.length = 0; // we don't allow matches to overlap, so start matching again
                            matches.push(potentialMatch);
                        }
                    }
                    else {
                        potentialMatches.splice(i, 1);
                        i--;
                    }
                }
            }
        }
        return matches;
    };
    ProcessExtendsVisitor.prototype.isElementValuesEqual = function (elementValue1, elementValue2) {
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
            for (var i_1 = 0; i_1 < elementValue1.elements.length; i_1++) {
                if (elementValue1.elements[i_1].combinator.value !== elementValue2.elements[i_1].combinator.value) {
                    if (i_1 !== 0 || (elementValue1.elements[i_1].combinator.value || ' ') !== (elementValue2.elements[i_1].combinator.value || ' ')) {
                        return false;
                    }
                }
                if (!this.isElementValuesEqual(elementValue1.elements[i_1].value, elementValue2.elements[i_1].value)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    };
    ProcessExtendsVisitor.prototype.extendSelector = function (matches, selectorPath, replacementSelector, isVisible) {
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
                path[path.length - 1].elements = path[path.length - 1]
                    .elements.concat(selectorPath[currentSelectorPathIndex].elements.slice(currentSelectorPathElementIndex));
                currentSelectorPathElementIndex = 0;
                currentSelectorPathIndex++;
            }
            newElements = selector.elements
                .slice(currentSelectorPathElementIndex, match.index)
                .concat([firstElement])
                .concat(replacementSelector.elements.slice(1));
            if (currentSelectorPathIndex === match.pathIndex && matchIndex > 0) {
                path[path.length - 1].elements =
                    path[path.length - 1].elements.concat(newElements);
            }
            else {
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
            path[path.length - 1].elements = path[path.length - 1]
                .elements.concat(selectorPath[currentSelectorPathIndex].elements.slice(currentSelectorPathElementIndex));
            currentSelectorPathIndex++;
        }
        path = path.concat(selectorPath.slice(currentSelectorPathIndex, selectorPath.length));
        path = path.map(function (currentValue) {
            // we can re-use elements here, because the visibility property matters only for selectors
            var derived = currentValue.createDerived(currentValue.elements);
            if (isVisible) {
                derived.ensureVisibility();
            }
            else {
                derived.ensureInvisibility();
            }
            return derived;
        });
        return path;
    };
    ProcessExtendsVisitor.prototype.visitMedia = function (mediaNode, visitArgs) {
        var newAllExtends = mediaNode.allExtends.concat(this.allExtendsStack[this.allExtendsStack.length - 1]);
        newAllExtends = newAllExtends.concat(this.doExtendChaining(newAllExtends, mediaNode.allExtends));
        this.allExtendsStack.push(newAllExtends);
    };
    ProcessExtendsVisitor.prototype.visitMediaOut = function (mediaNode) {
        var lastIndex = this.allExtendsStack.length - 1;
        this.allExtendsStack.length = lastIndex;
    };
    ProcessExtendsVisitor.prototype.visitAtRule = function (atRuleNode, visitArgs) {
        var newAllExtends = atRuleNode.allExtends.concat(this.allExtendsStack[this.allExtendsStack.length - 1]);
        newAllExtends = newAllExtends.concat(this.doExtendChaining(newAllExtends, atRuleNode.allExtends));
        this.allExtendsStack.push(newAllExtends);
    };
    ProcessExtendsVisitor.prototype.visitAtRuleOut = function (atRuleNode) {
        var lastIndex = this.allExtendsStack.length - 1;
        this.allExtendsStack.length = lastIndex;
    };
    return ProcessExtendsVisitor;
}());

var JoinSelectorVisitor = /** @class */ (function () {
    function JoinSelectorVisitor() {
        this.contexts = [[]];
        this._visitor = new Visitor(this);
    }
    JoinSelectorVisitor.prototype.run = function (root) {
        return this._visitor.visit(root);
    };
    JoinSelectorVisitor.prototype.visitDeclaration = function (declNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    JoinSelectorVisitor.prototype.visitMixinDefinition = function (mixinDefinitionNode, visitArgs) {
        visitArgs.visitDeeper = false;
    };
    JoinSelectorVisitor.prototype.visitRuleset = function (rulesetNode, visitArgs) {
        var context = this.contexts[this.contexts.length - 1];
        var paths = [];
        var selectors;
        this.contexts.push(paths);
        if (!rulesetNode.root) {
            selectors = rulesetNode.selectors;
            if (selectors) {
                selectors = selectors.filter(function (selector) { return selector.getIsOutput(); });
                rulesetNode.selectors = selectors.length ? selectors : (selectors = null);
                if (selectors) {
                    rulesetNode.joinSelectors(paths, context, selectors);
                }
            }
            if (!selectors) {
                rulesetNode.rules = null;
            }
            rulesetNode.paths = paths;
        }
    };
    JoinSelectorVisitor.prototype.visitRulesetOut = function (rulesetNode) {
        this.contexts.length = this.contexts.length - 1;
    };
    JoinSelectorVisitor.prototype.visitMedia = function (mediaNode, visitArgs) {
        var context = this.contexts[this.contexts.length - 1];
        mediaNode.rules[0].root = (context.length === 0 || context[0].multiMedia);
    };
    JoinSelectorVisitor.prototype.visitAtRule = function (atRuleNode, visitArgs) {
        var context = this.contexts[this.contexts.length - 1];
        if (atRuleNode.rules && atRuleNode.rules.length) {
            atRuleNode.rules[0].root = (atRuleNode.isRooted || context.length === 0 || null);
        }
    };
    return JoinSelectorVisitor;
}());

var CSSVisitorUtils = /** @class */ (function () {
    function CSSVisitorUtils(context) {
        this._visitor = new Visitor(this);
        this._context = context;
    }
    CSSVisitorUtils.prototype.containsSilentNonBlockedChild = function (bodyRules) {
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
    };
    CSSVisitorUtils.prototype.keepOnlyVisibleChilds = function (owner) {
        if (owner && owner.rules) {
            owner.rules = owner.rules.filter(function (thing) { return thing.isVisible(); });
        }
    };
    CSSVisitorUtils.prototype.isEmpty = function (owner) {
        return (owner && owner.rules)
            ? (owner.rules.length === 0) : true;
    };
    CSSVisitorUtils.prototype.hasVisibleSelector = function (rulesetNode) {
        return (rulesetNode && rulesetNode.paths)
            ? (rulesetNode.paths.length > 0) : false;
    };
    CSSVisitorUtils.prototype.resolveVisibility = function (node, originalRules) {
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
    };
    CSSVisitorUtils.prototype.isVisibleRuleset = function (rulesetNode) {
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
    };
    return CSSVisitorUtils;
}());
var ToCSSVisitor = function (context) {
    this._visitor = new Visitor(this);
    this._context = context;
    this.utils = new CSSVisitorUtils(context);
};
ToCSSVisitor.prototype = {
    isReplacing: true,
    run: function (root) {
        return this._visitor.visit(root);
    },
    visitDeclaration: function (declNode, visitArgs) {
        if (declNode.blocksVisibility() || declNode.variable) {
            return;
        }
        return declNode;
    },
    visitMixinDefinition: function (mixinNode, visitArgs) {
        // mixin definitions do not get eval'd - this means they keep state
        // so we have to clear that state here so it isn't used if toCSS is called twice
        mixinNode.frames = [];
    },
    visitExtend: function (extendNode, visitArgs) {
    },
    visitComment: function (commentNode, visitArgs) {
        if (commentNode.blocksVisibility() || commentNode.isSilent(this._context)) {
            return;
        }
        return commentNode;
    },
    visitMedia: function (mediaNode, visitArgs) {
        var originalRules = mediaNode.rules[0].rules;
        mediaNode.accept(this._visitor);
        visitArgs.visitDeeper = false;
        return this.utils.resolveVisibility(mediaNode, originalRules);
    },
    visitImport: function (importNode, visitArgs) {
        if (importNode.blocksVisibility()) {
            return;
        }
        return importNode;
    },
    visitAtRule: function (atRuleNode, visitArgs) {
        if (atRuleNode.rules && atRuleNode.rules.length) {
            return this.visitAtRuleWithBody(atRuleNode, visitArgs);
        }
        else {
            return this.visitAtRuleWithoutBody(atRuleNode, visitArgs);
        }
    },
    visitAnonymous: function (anonymousNode, visitArgs) {
        if (!anonymousNode.blocksVisibility()) {
            anonymousNode.accept(this._visitor);
            return anonymousNode;
        }
    },
    visitAtRuleWithBody: function (atRuleNode, visitArgs) {
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
        }
        // it is still true that it is only one ruleset in array
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
    visitAtRuleWithoutBody: function (atRuleNode, visitArgs) {
        if (atRuleNode.blocksVisibility()) {
            return;
        }
        if (atRuleNode.name === '@charset') {
            // Only output the debug info together with subsequent @charset definitions
            // a comment (or @media statement) before the actual @charset atrule would
            // be considered illegal css as it has to be on the first line
            if (this.charset) {
                if (atRuleNode.debugInfo) {
                    var comment = new tree.Comment("/* " + atRuleNode.toCSS(this._context).replace(/\n/g, '') + " */\n");
                    comment.debugInfo = atRuleNode.debugInfo;
                    return this._visitor.visit(comment);
                }
                return;
            }
            this.charset = true;
        }
        return atRuleNode;
    },
    checkValidNodes: function (rules, isRoot) {
        if (!rules) {
            return;
        }
        for (var i_1 = 0; i_1 < rules.length; i_1++) {
            var ruleNode = rules[i_1];
            if (isRoot && ruleNode instanceof tree.Declaration && !ruleNode.variable) {
                throw { message: 'Properties must be inside selector blocks. They cannot be in the root',
                    index: ruleNode.getIndex(), filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename };
            }
            if (ruleNode instanceof tree.Call) {
                throw { message: "Function '" + ruleNode.name + "' is undefined",
                    index: ruleNode.getIndex(), filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename };
            }
            if (ruleNode.type && !ruleNode.allowRoot) {
                throw { message: ruleNode.type + " node returned by a function is not valid here",
                    index: ruleNode.getIndex(), filename: ruleNode.fileInfo() && ruleNode.fileInfo().filename };
            }
        }
    },
    visitRuleset: function (rulesetNode, visitArgs) {
        // at this point rulesets are nested into each other
        var rule;
        var rulesets = [];
        this.checkValidNodes(rulesetNode.rules, rulesetNode.firstRoot);
        if (!rulesetNode.root) {
            // remove invisible paths
            this._compileRulesetPaths(rulesetNode);
            // remove rulesets from this ruleset body and compile them separately
            var nodeRules = rulesetNode.rules;
            var nodeRuleCnt = nodeRules ? nodeRules.length : 0;
            for (var i_2 = 0; i_2 < nodeRuleCnt;) {
                rule = nodeRules[i_2];
                if (rule && rule.rules) {
                    // visit because we are moving them out from being a child
                    rulesets.push(this._visitor.visit(rule));
                    nodeRules.splice(i_2, 1);
                    nodeRuleCnt--;
                    continue;
                }
                i_2++;
            }
            // accept the visitor to remove rules and refactor itself
            // then we can decide nogw whether we want it or not
            // compile body
            if (nodeRuleCnt > 0) {
                rulesetNode.accept(this._visitor);
            }
            else {
                rulesetNode.rules = null;
            }
            visitArgs.visitDeeper = false;
        }
        else { // if (! rulesetNode.root) {
            rulesetNode.accept(this._visitor);
            visitArgs.visitDeeper = false;
        }
        if (rulesetNode.rules) {
            this._mergeRules(rulesetNode.rules);
            this._removeDuplicateRules(rulesetNode.rules);
        }
        // now decide whether we keep the ruleset
        if (this.utils.isVisibleRuleset(rulesetNode)) {
            rulesetNode.ensureVisibility();
            rulesets.splice(0, 0, rulesetNode);
        }
        if (rulesets.length === 1) {
            return rulesets[0];
        }
        return rulesets;
    },
    _compileRulesetPaths: function (rulesetNode) {
        if (rulesetNode.paths) {
            rulesetNode.paths = rulesetNode.paths
                .filter(function (p) {
                var i;
                if (p[0].elements[0].combinator.value === ' ') {
                    p[0].elements[0].combinator = new (tree.Combinator)('');
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
    _removeDuplicateRules: function (rules) {
        if (!rules) {
            return;
        }
        // remove duplicates
        var ruleCache = {};
        var ruleList;
        var rule;
        var i;
        for (i = rules.length - 1; i >= 0; i--) {
            rule = rules[i];
            if (rule instanceof tree.Declaration) {
                if (!ruleCache[rule.name]) {
                    ruleCache[rule.name] = rule;
                }
                else {
                    ruleList = ruleCache[rule.name];
                    if (ruleList instanceof tree.Declaration) {
                        ruleList = ruleCache[rule.name] = [ruleCache[rule.name].toCSS(this._context)];
                    }
                    var ruleCSS = rule.toCSS(this._context);
                    if (ruleList.indexOf(ruleCSS) !== -1) {
                        rules.splice(i, 1);
                    }
                    else {
                        ruleList.push(ruleCSS);
                    }
                }
            }
        }
    },
    _mergeRules: function (rules) {
        if (!rules) {
            return;
        }
        var groups = {};
        var groupsArr = [];
        for (var i_3 = 0; i_3 < rules.length; i_3++) {
            var rule = rules[i_3];
            if (rule.merge) {
                var key = rule.name;
                groups[key] ? rules.splice(i_3--, 1) :
                    groupsArr.push(groups[key] = []);
                groups[key].push(rule);
            }
        }
        groupsArr.forEach(function (group) {
            if (group.length > 0) {
                var result_1 = group[0];
                var space_1 = [];
                var comma_1 = [new tree.Expression(space_1)];
                group.forEach(function (rule) {
                    if ((rule.merge === '+') && (space_1.length > 0)) {
                        comma_1.push(new tree.Expression(space_1 = []));
                    }
                    space_1.push(rule.value);
                    result_1.important = result_1.important || rule.important;
                });
                result_1.value = new tree.Value(comma_1);
            }
        });
    }
};

var visitors = {
    Visitor: Visitor,
    ImportVisitor: ImportVisitor,
    MarkVisibleSelectorsVisitor: SetTreeVisibilityVisitor,
    ExtendVisitor: ProcessExtendsVisitor,
    JoinSelectorVisitor: JoinSelectorVisitor,
    ToCSSVisitor: ToCSSVisitor
};

// Split the input into chunks.
var chunker = (function (input, fail) {
    var len = input.length;
    var level = 0;
    var parenLevel = 0;
    var lastOpening;
    var lastOpeningParen;
    var lastMultiComment;
    var lastMultiCommentEndBrace;
    var chunks = [];
    var emitFrom = 0;
    var chunkerCurrentIndex;
    var currentChunkStartIndex;
    var cc;
    var cc2;
    var matched;
    function emitChunk(force) {
        var len = chunkerCurrentIndex - emitFrom;
        if (((len < 512) && !force) || !len) {
            return;
        }
        chunks.push(input.slice(emitFrom, chunkerCurrentIndex + 1));
        emitFrom = chunkerCurrentIndex + 1;
    }
    for (chunkerCurrentIndex = 0; chunkerCurrentIndex < len; chunkerCurrentIndex++) {
        cc = input.charCodeAt(chunkerCurrentIndex);
        if (((cc >= 97) && (cc <= 122)) || (cc < 34)) {
            // a-z or whitespace
            continue;
        }
        switch (cc) {
            case 40: // (
                parenLevel++;
                lastOpeningParen = chunkerCurrentIndex;
                continue;
            case 41: // )
                if (--parenLevel < 0) {
                    return fail('missing opening `(`', chunkerCurrentIndex);
                }
                continue;
            case 59: // ;
                if (!parenLevel) {
                    emitChunk();
                }
                continue;
            case 123: // {
                level++;
                lastOpening = chunkerCurrentIndex;
                continue;
            case 125: // }
                if (--level < 0) {
                    return fail('missing opening `{`', chunkerCurrentIndex);
                }
                if (!level && !parenLevel) {
                    emitChunk();
                }
                continue;
            case 92: // \
                if (chunkerCurrentIndex < len - 1) {
                    chunkerCurrentIndex++;
                    continue;
                }
                return fail('unescaped `\\`', chunkerCurrentIndex);
            case 34:
            case 39:
            case 96: // ", ' and `
                matched = 0;
                currentChunkStartIndex = chunkerCurrentIndex;
                for (chunkerCurrentIndex = chunkerCurrentIndex + 1; chunkerCurrentIndex < len; chunkerCurrentIndex++) {
                    cc2 = input.charCodeAt(chunkerCurrentIndex);
                    if (cc2 > 96) {
                        continue;
                    }
                    if (cc2 == cc) {
                        matched = 1;
                        break;
                    }
                    if (cc2 == 92) { // \
                        if (chunkerCurrentIndex == len - 1) {
                            return fail('unescaped `\\`', chunkerCurrentIndex);
                        }
                        chunkerCurrentIndex++;
                    }
                }
                if (matched) {
                    continue;
                }
                return fail("unmatched `" + String.fromCharCode(cc) + "`", currentChunkStartIndex);
            case 47: // /, check for comment
                if (parenLevel || (chunkerCurrentIndex == len - 1)) {
                    continue;
                }
                cc2 = input.charCodeAt(chunkerCurrentIndex + 1);
                if (cc2 == 47) {
                    // //, find lnfeed
                    for (chunkerCurrentIndex = chunkerCurrentIndex + 2; chunkerCurrentIndex < len; chunkerCurrentIndex++) {
                        cc2 = input.charCodeAt(chunkerCurrentIndex);
                        if ((cc2 <= 13) && ((cc2 == 10) || (cc2 == 13))) {
                            break;
                        }
                    }
                }
                else if (cc2 == 42) {
                    // /*, find */
                    lastMultiComment = currentChunkStartIndex = chunkerCurrentIndex;
                    for (chunkerCurrentIndex = chunkerCurrentIndex + 2; chunkerCurrentIndex < len - 1; chunkerCurrentIndex++) {
                        cc2 = input.charCodeAt(chunkerCurrentIndex);
                        if (cc2 == 125) {
                            lastMultiCommentEndBrace = chunkerCurrentIndex;
                        }
                        if (cc2 != 42) {
                            continue;
                        }
                        if (input.charCodeAt(chunkerCurrentIndex + 1) == 47) {
                            break;
                        }
                    }
                    if (chunkerCurrentIndex == len - 1) {
                        return fail('missing closing `*/`', currentChunkStartIndex);
                    }
                    chunkerCurrentIndex++;
                }
                continue;
            case 42: // *, check for unmatched */
                if ((chunkerCurrentIndex < len - 1) && (input.charCodeAt(chunkerCurrentIndex + 1) == 47)) {
                    return fail('unmatched `/*`', chunkerCurrentIndex);
                }
                continue;
        }
    }
    if (level !== 0) {
        if ((lastMultiComment > lastOpening) && (lastMultiCommentEndBrace > lastMultiComment)) {
            return fail('missing closing `}` or `*/`', lastOpening);
        }
        else {
            return fail('missing closing `}`', lastOpening);
        }
    }
    else if (parenLevel !== 0) {
        return fail('missing closing `)`', lastOpeningParen);
    }
    emitChunk(true);
    return chunks;
});

var getParserInput = (function () {
    var // Less input string
    input;
    var // current chunk
    j;
    var // holds state for backtracking
    saveStack = [];
    var // furthest index the parser has gone to
    furthest;
    var // if this is furthest we got to, this is the probably cause
    furthestPossibleErrorMessage;
    var // chunkified input
    chunks;
    var // current chunk
    current;
    var // index of current chunk, in `input`
    currentPos;
    var parserInput = {};
    var CHARCODE_SPACE = 32;
    var CHARCODE_TAB = 9;
    var CHARCODE_LF = 10;
    var CHARCODE_CR = 13;
    var CHARCODE_PLUS = 43;
    var CHARCODE_COMMA = 44;
    var CHARCODE_FORWARD_SLASH = 47;
    var CHARCODE_9 = 57;
    function skipWhitespace(length) {
        var oldi = parserInput.i;
        var oldj = j;
        var curr = parserInput.i - currentPos;
        var endIndex = parserInput.i + current.length - curr;
        var mem = (parserInput.i += length);
        var inp = input;
        var c;
        var nextChar;
        var comment;
        for (; parserInput.i < endIndex; parserInput.i++) {
            c = inp.charCodeAt(parserInput.i);
            if (parserInput.autoCommentAbsorb && c === CHARCODE_FORWARD_SLASH) {
                nextChar = inp.charAt(parserInput.i + 1);
                if (nextChar === '/') {
                    comment = { index: parserInput.i, isLineComment: true };
                    var nextNewLine = inp.indexOf('\n', parserInput.i + 2);
                    if (nextNewLine < 0) {
                        nextNewLine = endIndex;
                    }
                    parserInput.i = nextNewLine;
                    comment.text = inp.substr(comment.index, parserInput.i - comment.index);
                    parserInput.commentStore.push(comment);
                    continue;
                }
                else if (nextChar === '*') {
                    var nextStarSlash = inp.indexOf('*/', parserInput.i + 2);
                    if (nextStarSlash >= 0) {
                        comment = {
                            index: parserInput.i,
                            text: inp.substr(parserInput.i, nextStarSlash + 2 - parserInput.i),
                            isLineComment: false
                        };
                        parserInput.i += comment.text.length - 1;
                        parserInput.commentStore.push(comment);
                        continue;
                    }
                }
                break;
            }
            if ((c !== CHARCODE_SPACE) && (c !== CHARCODE_LF) && (c !== CHARCODE_TAB) && (c !== CHARCODE_CR)) {
                break;
            }
        }
        current = current.slice(length + parserInput.i - mem + curr);
        currentPos = parserInput.i;
        if (!current.length) {
            if (j < chunks.length - 1) {
                current = chunks[++j];
                skipWhitespace(0); // skip space at the beginning of a chunk
                return true; // things changed
            }
            parserInput.finished = true;
        }
        return oldi !== parserInput.i || oldj !== j;
    }
    parserInput.save = function () {
        currentPos = parserInput.i;
        saveStack.push({ current: current, i: parserInput.i, j: j });
    };
    parserInput.restore = function (possibleErrorMessage) {
        if (parserInput.i > furthest || (parserInput.i === furthest && possibleErrorMessage && !furthestPossibleErrorMessage)) {
            furthest = parserInput.i;
            furthestPossibleErrorMessage = possibleErrorMessage;
        }
        var state = saveStack.pop();
        current = state.current;
        currentPos = parserInput.i = state.i;
        j = state.j;
    };
    parserInput.forget = function () {
        saveStack.pop();
    };
    parserInput.isWhitespace = function (offset) {
        var pos = parserInput.i + (offset || 0);
        var code = input.charCodeAt(pos);
        return (code === CHARCODE_SPACE || code === CHARCODE_CR || code === CHARCODE_TAB || code === CHARCODE_LF);
    };
    // Specialization of $(tok)
    parserInput.$re = function (tok) {
        if (parserInput.i > currentPos) {
            current = current.slice(parserInput.i - currentPos);
            currentPos = parserInput.i;
        }
        var m = tok.exec(current);
        if (!m) {
            return null;
        }
        skipWhitespace(m[0].length);
        if (typeof m === 'string') {
            return m;
        }
        return m.length === 1 ? m[0] : m;
    };
    parserInput.$char = function (tok) {
        if (input.charAt(parserInput.i) !== tok) {
            return null;
        }
        skipWhitespace(1);
        return tok;
    };
    parserInput.$str = function (tok) {
        var tokLength = tok.length;
        // https://jsperf.com/string-startswith/21
        for (var i_1 = 0; i_1 < tokLength; i_1++) {
            if (input.charAt(parserInput.i + i_1) !== tok.charAt(i_1)) {
                return null;
            }
        }
        skipWhitespace(tokLength);
        return tok;
    };
    parserInput.$quoted = function (loc) {
        var pos = loc || parserInput.i;
        var startChar = input.charAt(pos);
        if (startChar !== '\'' && startChar !== '"') {
            return;
        }
        var length = input.length;
        var currentPosition = pos;
        for (var i_2 = 1; i_2 + currentPosition < length; i_2++) {
            var nextChar = input.charAt(i_2 + currentPosition);
            switch (nextChar) {
                case '\\':
                    i_2++;
                    continue;
                case '\r':
                case '\n':
                    break;
                case startChar:
                    var str = input.substr(currentPosition, i_2 + 1);
                    if (!loc && loc !== 0) {
                        skipWhitespace(i_2 + 1);
                        return str;
                    }
                    return [startChar, str];
            }
        }
        return null;
    };
    /**
     * Permissive parsing. Ignores everything except matching {} [] () and quotes
     * until matching token (outside of blocks)
     */
    parserInput.$parseUntil = function (tok) {
        var quote = '';
        var returnVal = null;
        var inComment = false;
        var blockDepth = 0;
        var blockStack = [];
        var parseGroups = [];
        var length = input.length;
        var startPos = parserInput.i;
        var lastPos = parserInput.i;
        var i = parserInput.i;
        var loop = true;
        var testChar;
        if (typeof tok === 'string') {
            testChar = function (char) { return char === tok; };
        }
        else {
            testChar = function (char) { return tok.test(char); };
        }
        do {
            var nextChar = input.charAt(i);
            if (blockDepth === 0 && testChar(nextChar)) {
                returnVal = input.substr(lastPos, i - lastPos);
                if (returnVal) {
                    parseGroups.push(returnVal);
                }
                else {
                    parseGroups.push(' ');
                }
                returnVal = parseGroups;
                skipWhitespace(i - startPos);
                loop = false;
            }
            else {
                if (inComment) {
                    if (nextChar === '*' &&
                        input.charAt(i + 1) === '/') {
                        i++;
                        blockDepth--;
                        inComment = false;
                    }
                    i++;
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        i++;
                        nextChar = input.charAt(i);
                        parseGroups.push(input.substr(lastPos, i - lastPos + 1));
                        lastPos = i + 1;
                        break;
                    case '/':
                        if (input.charAt(i + 1) === '*') {
                            i++;
                            inComment = true;
                            blockDepth++;
                        }
                        break;
                    case '\'':
                    case '"':
                        quote = parserInput.$quoted(i);
                        if (quote) {
                            parseGroups.push(input.substr(lastPos, i - lastPos), quote);
                            i += quote[1].length - 1;
                            lastPos = i + 1;
                        }
                        else {
                            skipWhitespace(i - startPos);
                            returnVal = nextChar;
                            loop = false;
                        }
                        break;
                    case '{':
                        blockStack.push('}');
                        blockDepth++;
                        break;
                    case '(':
                        blockStack.push(')');
                        blockDepth++;
                        break;
                    case '[':
                        blockStack.push(']');
                        blockDepth++;
                        break;
                    case '}':
                    case ')':
                    case ']':
                        var expected = blockStack.pop();
                        if (nextChar === expected) {
                            blockDepth--;
                        }
                        else {
                            // move the parser to the error and return expected
                            skipWhitespace(i - startPos);
                            returnVal = expected;
                            loop = false;
                        }
                }
                i++;
                if (i > length) {
                    loop = false;
                }
            }
        } while (loop);
        return returnVal ? returnVal : null;
    };
    parserInput.autoCommentAbsorb = true;
    parserInput.commentStore = [];
    parserInput.finished = false;
    // Same as $(), but don't change the state of the parser,
    // just return the match.
    parserInput.peek = function (tok) {
        if (typeof tok === 'string') {
            // https://jsperf.com/string-startswith/21
            for (var i_3 = 0; i_3 < tok.length; i_3++) {
                if (input.charAt(parserInput.i + i_3) !== tok.charAt(i_3)) {
                    return false;
                }
            }
            return true;
        }
        else {
            return tok.test(current);
        }
    };
    // Specialization of peek()
    // TODO remove or change some currentChar calls to peekChar
    parserInput.peekChar = function (tok) { return input.charAt(parserInput.i) === tok; };
    parserInput.currentChar = function () { return input.charAt(parserInput.i); };
    parserInput.prevChar = function () { return input.charAt(parserInput.i - 1); };
    parserInput.getInput = function () { return input; };
    parserInput.peekNotNumeric = function () {
        var c = input.charCodeAt(parserInput.i);
        // Is the first char of the dimension 0-9, '.', '+' or '-'
        return (c > CHARCODE_9 || c < CHARCODE_PLUS) || c === CHARCODE_FORWARD_SLASH || c === CHARCODE_COMMA;
    };
    parserInput.start = function (str, chunkInput, failFunction) {
        input = str;
        parserInput.i = j = currentPos = furthest = 0;
        // chunking apparently makes things quicker (but my tests indicate
        // it might actually make things slower in node at least)
        // and it is a non-perfect parse - it can't recognise
        // unquoted urls, meaning it can't distinguish comments
        // meaning comments with quotes or {}() in them get 'counted'
        // and then lead to parse errors.
        // In addition if the chunking chunks in the wrong place we might
        // not be able to parse a parser statement in one go
        // this is officially deprecated but can be switched on via an option
        // in the case it causes too much performance issues.
        if (chunkInput) {
            chunks = chunker(str, failFunction);
        }
        else {
            chunks = [str];
        }
        current = chunks[0];
        skipWhitespace(0);
    };
    parserInput.end = function () {
        var message;
        var isFinished = parserInput.i >= input.length;
        if (parserInput.i < furthest) {
            message = furthestPossibleErrorMessage;
            parserInput.i = furthest;
        }
        return {
            isFinished: isFinished,
            furthest: parserInput.i,
            furthestPossibleErrorMessage: message,
            furthestReachedEnd: parserInput.i >= input.length - 1,
            furthestChar: input[parserInput.i]
        };
    };
    return parserInput;
});

//
// less.js - parser
//
//    A relatively straight-forward predictive parser.
//    There is no tokenization/lexing stage, the input is parsed
//    in one sweep.
//
//    To make the parser fast enough to run in the browser, several
//    optimization had to be made:
//
//    - Matching and slicing on a huge input is often cause of slowdowns.
//      The solution is to chunkify the input into smaller strings.
//      The chunks are stored in the `chunks` var,
//      `j` holds the current chunk index, and `currentPos` holds
//      the index of the current chunk in relation to `input`.
//      This gives us an almost 4x speed-up.
//
//    - In many cases, we don't need to match individual tokens;
//      for example, if a value doesn't hold any variables, operations
//      or dynamic references, the parser can effectively 'skip' it,
//      treating it as a literal.
//      An example would be '1px solid #000' - which evaluates to itself,
//      we don't need to know what the individual components are.
//      The drawback, of course is that you don't get the benefits of
//      syntax-checking on the CSS. This gives us a 50% speed-up in the parser,
//      and a smaller speed-up in the code-gen.
//
//
//    Token matching is done with the `$` function, which either takes
//    a terminal string or regexp, or a non-terminal function to call.
//    It also takes care of moving all the indices forwards.
//
var Parser = function Parser(context, imports, fileInfo) {
    var parsers;
    var parserInput = getParserInput();
    function error(msg, type) {
        throw new LessError({
            index: parserInput.i,
            filename: fileInfo.filename,
            type: type || 'Syntax',
            message: msg
        }, imports);
    }
    function expect(arg, msg) {
        // some older browsers return typeof 'function' for RegExp
        var result = (arg instanceof Function) ? arg.call(parsers) : parserInput.$re(arg);
        if (result) {
            return result;
        }
        error(msg || (typeof arg === 'string'
            ? "expected '" + arg + "' got '" + parserInput.currentChar() + "'"
            : 'unexpected token'));
    }
    // Specialization of expect()
    function expectChar(arg, msg) {
        if (parserInput.$char(arg)) {
            return arg;
        }
        error(msg || "expected '" + arg + "' got '" + parserInput.currentChar() + "'");
    }
    function getDebugInfo(index) {
        var filename = fileInfo.filename;
        return {
            lineNumber: getLocation(index, parserInput.getInput()).line + 1,
            fileName: filename
        };
    }
    /**
     *  Used after initial parsing to create nodes on the fly
     *
     *  @param {String} str          - string to parse
     *  @param {Array}  parseList    - array of parsers to run input through e.g. ["value", "important"]
     *  @param {Number} currentIndex - start number to begin indexing
     *  @param {Object} fileInfo     - fileInfo to attach to created nodes
     */
    function parseNode(str, parseList, currentIndex, fileInfo, callback) {
        var result;
        var returnNodes = [];
        var parser = parserInput;
        try {
            parser.start(str, false, function fail(msg, index) {
                callback({
                    message: msg,
                    index: index + currentIndex
                });
            });
            for (var x = 0, p = void 0, i_1; (p = parseList[x]); x++) {
                i_1 = parser.i;
                result = parsers[p]();
                if (result) {
                    try {
                        result._index = i_1 + currentIndex;
                        result._fileInfo = fileInfo;
                    }
                    catch (e) { }
                    returnNodes.push(result);
                }
                else {
                    returnNodes.push(null);
                }
            }
            var endInfo = parser.end();
            if (endInfo.isFinished) {
                callback(null, returnNodes);
            }
            else {
                callback(true, null);
            }
        }
        catch (e) {
            throw new LessError({
                index: e.index + currentIndex,
                message: e.message
            }, imports, fileInfo.filename);
        }
    }
    //
    // The Parser
    //
    return {
        parserInput: parserInput,
        imports: imports,
        fileInfo: fileInfo,
        parseNode: parseNode,
        //
        // Parse an input string into an abstract syntax tree,
        // @param str A string containing 'less' markup
        // @param callback call `callback` when done.
        // @param [additionalData] An optional map which can contains vars - a map (key, value) of variables to apply
        //
        parse: function (str, callback, additionalData) {
            var root;
            var error = null;
            var globalVars;
            var modifyVars;
            var ignored;
            var preText = '';
            globalVars = (additionalData && additionalData.globalVars) ? Parser.serializeVars(additionalData.globalVars) + "\n" : '';
            modifyVars = (additionalData && additionalData.modifyVars) ? "\n" + Parser.serializeVars(additionalData.modifyVars) : '';
            if (context.pluginManager) {
                var preProcessors = context.pluginManager.getPreProcessors();
                for (var i_2 = 0; i_2 < preProcessors.length; i_2++) {
                    str = preProcessors[i_2].process(str, { context: context, imports: imports, fileInfo: fileInfo });
                }
            }
            if (globalVars || (additionalData && additionalData.banner)) {
                preText = ((additionalData && additionalData.banner) ? additionalData.banner : '') + globalVars;
                ignored = imports.contentsIgnoredChars;
                ignored[fileInfo.filename] = ignored[fileInfo.filename] || 0;
                ignored[fileInfo.filename] += preText.length;
            }
            str = str.replace(/\r\n?/g, '\n');
            // Remove potential UTF Byte Order Mark
            str = preText + str.replace(/^\uFEFF/, '') + modifyVars;
            imports.contents[fileInfo.filename] = str;
            // Start with the primary rule.
            // The whole syntax tree is held under a Ruleset node,
            // with the `root` property set to true, so no `{}` are
            // output. The callback is called when the input is parsed.
            try {
                parserInput.start(str, context.chunkInput, function fail(msg, index) {
                    throw new LessError({
                        index: index,
                        type: 'Parse',
                        message: msg,
                        filename: fileInfo.filename
                    }, imports);
                });
                tree.Node.prototype.parse = this;
                root = new tree.Ruleset(null, this.parsers.primary());
                tree.Node.prototype.rootNode = root;
                root.root = true;
                root.firstRoot = true;
                root.functionRegistry = functionRegistry.inherit();
            }
            catch (e) {
                return callback(new LessError(e, imports, fileInfo.filename));
            }
            // If `i` is smaller than the `input.length - 1`,
            // it means the parser wasn't able to parse the whole
            // string, so we've got a parsing error.
            //
            // We try to extract a \n delimited string,
            // showing the line where the parse error occurred.
            // We split it up into two parts (the part which parsed,
            // and the part which didn't), so we can color them differently.
            var endInfo = parserInput.end();
            if (!endInfo.isFinished) {
                var message = endInfo.furthestPossibleErrorMessage;
                if (!message) {
                    message = 'Unrecognised input';
                    if (endInfo.furthestChar === '}') {
                        message += '. Possibly missing opening \'{\'';
                    }
                    else if (endInfo.furthestChar === ')') {
                        message += '. Possibly missing opening \'(\'';
                    }
                    else if (endInfo.furthestReachedEnd) {
                        message += '. Possibly missing something';
                    }
                }
                error = new LessError({
                    type: 'Parse',
                    message: message,
                    index: endInfo.furthest,
                    filename: fileInfo.filename
                }, imports);
            }
            var finish = function (e) {
                e = error || e || imports.error;
                if (e) {
                    if (!(e instanceof LessError)) {
                        e = new LessError(e, imports, fileInfo.filename);
                    }
                    return callback(e);
                }
                else {
                    return callback(null, root);
                }
            };
            if (context.processImports !== false) {
                new visitors.ImportVisitor(imports, finish)
                    .run(root);
            }
            else {
                return finish();
            }
        },
        //
        // Here in, the parsing rules/functions
        //
        // The basic structure of the syntax tree generated is as follows:
        //
        //   Ruleset ->  Declaration -> Value -> Expression -> Entity
        //
        // Here's some Less code:
        //
        //    .class {
        //      color: #fff;
        //      border: 1px solid #000;
        //      width: @w + 4px;
        //      > .child {...}
        //    }
        //
        // And here's what the parse tree might look like:
        //
        //     Ruleset (Selector '.class', [
        //         Declaration ("color",  Value ([Expression [Color #fff]]))
        //         Declaration ("border", Value ([Expression [Dimension 1px][Keyword "solid"][Color #000]]))
        //         Declaration ("width",  Value ([Expression [Operation " + " [Variable "@w"][Dimension 4px]]]))
        //         Ruleset (Selector [Element '>', '.child'], [...])
        //     ])
        //
        //  In general, most rules will try to parse a token with the `$re()` function, and if the return
        //  value is truly, will return a new node, of the relevant type. Sometimes, we need to check
        //  first, before parsing, that's when we use `peek()`.
        //
        parsers: parsers = {
            //
            // The `primary` rule is the *entry* and *exit* point of the parser.
            // The rules here can appear at any level of the parse tree.
            //
            // The recursive nature of the grammar is an interplay between the `block`
            // rule, which represents `{ ... }`, the `ruleset` rule, and this `primary` rule,
            // as represented by this simplified grammar:
            //
            //     primary  →  (ruleset | declaration)+
            //     ruleset  →  selector+ block
            //     block    →  '{' primary '}'
            //
            // Only at one point is the primary rule not called from the
            // block rule: at the root level.
            //
            primary: function () {
                var mixin = this.mixin;
                var root = [];
                var node;
                while (true) {
                    while (true) {
                        node = this.comment();
                        if (!node) {
                            break;
                        }
                        root.push(node);
                    }
                    // always process comments before deciding if finished
                    if (parserInput.finished) {
                        break;
                    }
                    if (parserInput.peek('}')) {
                        break;
                    }
                    node = this.extendRule();
                    if (node) {
                        root = root.concat(node);
                        continue;
                    }
                    node = mixin.definition() || this.declaration() || mixin.call(false, false) ||
                        this.ruleset() || this.variableCall() || this.entities.call() || this.atrule();
                    if (node) {
                        root.push(node);
                    }
                    else {
                        var foundSemiColon = false;
                        while (parserInput.$char(';')) {
                            foundSemiColon = true;
                        }
                        if (!foundSemiColon) {
                            break;
                        }
                    }
                }
                return root;
            },
            // comments are collected by the main parsing mechanism and then assigned to nodes
            // where the current structure allows it
            comment: function () {
                if (parserInput.commentStore.length) {
                    var comment = parserInput.commentStore.shift();
                    return new (tree.Comment)(comment.text, comment.isLineComment, comment.index, fileInfo);
                }
            },
            //
            // Entities are tokens which can be found inside an Expression
            //
            entities: {
                mixinLookup: function () {
                    return parsers.mixin.call(true, true);
                },
                //
                // A string, which supports escaping " and '
                //
                //     "milky way" 'he\'s the one!'
                //
                quoted: function (forceEscaped) {
                    var str;
                    var index = parserInput.i;
                    var isEscaped = false;
                    parserInput.save();
                    if (parserInput.$char('~')) {
                        isEscaped = true;
                    }
                    else if (forceEscaped) {
                        parserInput.restore();
                        return;
                    }
                    str = parserInput.$quoted();
                    if (!str) {
                        parserInput.restore();
                        return;
                    }
                    parserInput.forget();
                    return new (tree.Quoted)(str.charAt(0), str.substr(1, str.length - 2), isEscaped, index, fileInfo);
                },
                //
                // A catch-all word, such as:
                //
                //     black border-collapse
                //
                keyword: function () {
                    var k = parserInput.$char('%') || parserInput.$re(/^\[?(?:[\w-]|\\(?:[A-Fa-f0-9]{1,6} ?|[^A-Fa-f0-9]))+\]?/);
                    if (k) {
                        return tree.Color.fromKeyword(k) || new (tree.Keyword)(k);
                    }
                },
                //
                // A function call
                //
                //     rgb(255, 0, 255)
                //
                // The arguments are parsed with the `entities.arguments` parser.
                //
                call: function () {
                    var name;
                    var args;
                    var func;
                    var index = parserInput.i;
                    // http://jsperf.com/case-insensitive-regex-vs-strtolower-then-regex/18
                    if (parserInput.peek(/^url\(/i)) {
                        return;
                    }
                    parserInput.save();
                    name = parserInput.$re(/^([\w-]+|%|progid:[\w\.]+)\(/);
                    if (!name) {
                        parserInput.forget();
                        return;
                    }
                    name = name[1];
                    func = this.customFuncCall(name);
                    if (func) {
                        args = func.parse();
                        if (args && func.stop) {
                            parserInput.forget();
                            return args;
                        }
                    }
                    args = this.arguments(args);
                    if (!parserInput.$char(')')) {
                        parserInput.restore('Could not parse call arguments or missing \')\'');
                        return;
                    }
                    parserInput.forget();
                    return new (tree.Call)(name, args, index, fileInfo);
                },
                //
                // Parsing rules for functions with non-standard args, e.g.:
                //
                //     boolean(not(2 > 1))
                //
                //     This is a quick prototype, to be modified/improved when
                //     more custom-parsed funcs come (e.g. `selector(...)`)
                //
                customFuncCall: function (name) {
                    /* Ideally the table is to be moved out of here for faster perf.,
                       but it's quite tricky since it relies on all these `parsers`
                       and `expect` available only here */
                    return {
                        alpha: f(parsers.ieAlpha, true),
                        boolean: f(condition),
                        'if': f(condition)
                    }[name.toLowerCase()];
                    function f(parse, stop) {
                        return {
                            parse: parse,
                            stop: stop // when true - stop after parse() and return its result, 
                            // otherwise continue for plain args
                        };
                    }
                    function condition() {
                        return [expect(parsers.condition, 'expected condition')];
                    }
                },
                arguments: function (prevArgs) {
                    var argsComma = prevArgs || [];
                    var argsSemiColon = [];
                    var isSemiColonSeparated;
                    var value;
                    parserInput.save();
                    while (true) {
                        if (prevArgs) {
                            prevArgs = false;
                        }
                        else {
                            value = parsers.detachedRuleset() || this.assignment() || parsers.expression();
                            if (!value) {
                                break;
                            }
                            if (value.value && value.value.length == 1) {
                                value = value.value[0];
                            }
                            argsComma.push(value);
                        }
                        if (parserInput.$char(',')) {
                            continue;
                        }
                        if (parserInput.$char(';') || isSemiColonSeparated) {
                            isSemiColonSeparated = true;
                            value = (argsComma.length < 1) ? argsComma[0]
                                : new tree.Value(argsComma);
                            argsSemiColon.push(value);
                            argsComma = [];
                        }
                    }
                    parserInput.forget();
                    return isSemiColonSeparated ? argsSemiColon : argsComma;
                },
                literal: function () {
                    return this.dimension() ||
                        this.color() ||
                        this.quoted() ||
                        this.unicodeDescriptor();
                },
                // Assignments are argument entities for calls.
                // They are present in ie filter properties as shown below.
                //
                //     filter: progid:DXImageTransform.Microsoft.Alpha( *opacity=50* )
                //
                assignment: function () {
                    var key;
                    var value;
                    parserInput.save();
                    key = parserInput.$re(/^\w+(?=\s?=)/i);
                    if (!key) {
                        parserInput.restore();
                        return;
                    }
                    if (!parserInput.$char('=')) {
                        parserInput.restore();
                        return;
                    }
                    value = parsers.entity();
                    if (value) {
                        parserInput.forget();
                        return new (tree.Assignment)(key, value);
                    }
                    else {
                        parserInput.restore();
                    }
                },
                //
                // Parse url() tokens
                //
                // We use a specific rule for urls, because they don't really behave like
                // standard function calls. The difference is that the argument doesn't have
                // to be enclosed within a string, so it can't be parsed as an Expression.
                //
                url: function () {
                    var value;
                    var index = parserInput.i;
                    parserInput.autoCommentAbsorb = false;
                    if (!parserInput.$str('url(')) {
                        parserInput.autoCommentAbsorb = true;
                        return;
                    }
                    value = this.quoted() || this.variable() || this.property() ||
                        parserInput.$re(/^(?:(?:\\[\(\)'"])|[^\(\)'"])+/) || '';
                    parserInput.autoCommentAbsorb = true;
                    expectChar(')');
                    return new (tree.URL)((value.value != null ||
                        value instanceof tree.Variable ||
                        value instanceof tree.Property) ?
                        value : new (tree.Anonymous)(value, index), index, fileInfo);
                },
                //
                // A Variable entity, such as `@fink`, in
                //
                //     width: @fink + 2px
                //
                // We use a different parser for variable definitions,
                // see `parsers.variable`.
                //
                variable: function () {
                    var ch;
                    var name;
                    var index = parserInput.i;
                    parserInput.save();
                    if (parserInput.currentChar() === '@' && (name = parserInput.$re(/^@@?[\w-]+/))) {
                        ch = parserInput.currentChar();
                        if (ch === '(' || ch === '[' && !parserInput.prevChar().match(/^\s/)) {
                            // this may be a VariableCall lookup
                            var result = parsers.variableCall(name);
                            if (result) {
                                parserInput.forget();
                                return result;
                            }
                        }
                        parserInput.forget();
                        return new (tree.Variable)(name, index, fileInfo);
                    }
                    parserInput.restore();
                },
                // A variable entity using the protective {} e.g. @{var}
                variableCurly: function () {
                    var curly;
                    var index = parserInput.i;
                    if (parserInput.currentChar() === '@' && (curly = parserInput.$re(/^@\{([\w-]+)\}/))) {
                        return new (tree.Variable)("@" + curly[1], index, fileInfo);
                    }
                },
                //
                // A Property accessor, such as `$color`, in
                //
                //     background-color: $color
                //
                property: function () {
                    var name;
                    var index = parserInput.i;
                    if (parserInput.currentChar() === '$' && (name = parserInput.$re(/^\$[\w-]+/))) {
                        return new (tree.Property)(name, index, fileInfo);
                    }
                },
                // A property entity useing the protective {} e.g. ${prop}
                propertyCurly: function () {
                    var curly;
                    var index = parserInput.i;
                    if (parserInput.currentChar() === '$' && (curly = parserInput.$re(/^\$\{([\w-]+)\}/))) {
                        return new (tree.Property)("$" + curly[1], index, fileInfo);
                    }
                },
                //
                // A Hexadecimal color
                //
                //     #4F3C2F
                //
                // `rgb` and `hsl` colors are parsed through the `entities.call` parser.
                //
                color: function () {
                    var rgb;
                    parserInput.save();
                    if (parserInput.currentChar() === '#' && (rgb = parserInput.$re(/^#([A-Fa-f0-9]{8}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{3,4})([\w.#\[])?/))) {
                        if (!rgb[2]) {
                            parserInput.forget();
                            return new (tree.Color)(rgb[1], undefined, rgb[0]);
                        }
                    }
                    parserInput.restore();
                },
                colorKeyword: function () {
                    parserInput.save();
                    var autoCommentAbsorb = parserInput.autoCommentAbsorb;
                    parserInput.autoCommentAbsorb = false;
                    var k = parserInput.$re(/^[_A-Za-z-][_A-Za-z0-9-]+/);
                    parserInput.autoCommentAbsorb = autoCommentAbsorb;
                    if (!k) {
                        parserInput.forget();
                        return;
                    }
                    parserInput.restore();
                    var color = tree.Color.fromKeyword(k);
                    if (color) {
                        parserInput.$str(k);
                        return color;
                    }
                },
                //
                // A Dimension, that is, a number and a unit
                //
                //     0.5em 95%
                //
                dimension: function () {
                    if (parserInput.peekNotNumeric()) {
                        return;
                    }
                    var value = parserInput.$re(/^([+-]?\d*\.?\d+)(%|[a-z_]+)?/i);
                    if (value) {
                        return new (tree.Dimension)(value[1], value[2]);
                    }
                },
                //
                // A unicode descriptor, as is used in unicode-range
                //
                // U+0??  or U+00A1-00A9
                //
                unicodeDescriptor: function () {
                    var ud;
                    ud = parserInput.$re(/^U\+[0-9a-fA-F?]+(\-[0-9a-fA-F?]+)?/);
                    if (ud) {
                        return new (tree.UnicodeDescriptor)(ud[0]);
                    }
                },
                //
                // JavaScript code to be evaluated
                //
                //     `window.location.href`
                //
                javascript: function () {
                    var js;
                    var index = parserInput.i;
                    parserInput.save();
                    var escape = parserInput.$char('~');
                    var jsQuote = parserInput.$char('`');
                    if (!jsQuote) {
                        parserInput.restore();
                        return;
                    }
                    js = parserInput.$re(/^[^`]*`/);
                    if (js) {
                        parserInput.forget();
                        return new (tree.JavaScript)(js.substr(0, js.length - 1), Boolean(escape), index, fileInfo);
                    }
                    parserInput.restore('invalid javascript definition');
                }
            },
            //
            // The variable part of a variable definition. Used in the `rule` parser
            //
            //     @fink:
            //
            variable: function () {
                var name;
                if (parserInput.currentChar() === '@' && (name = parserInput.$re(/^(@[\w-]+)\s*:/))) {
                    return name[1];
                }
            },
            //
            // Call a variable value to retrieve a detached ruleset
            // or a value from a detached ruleset's rules.
            //
            //     @fink();
            //     @fink;
            //     color: @fink[@color];
            //
            variableCall: function (parsedName) {
                var lookups;
                var i = parserInput.i;
                var inValue = !!parsedName;
                var name = parsedName;
                parserInput.save();
                if (name || (parserInput.currentChar() === '@'
                    && (name = parserInput.$re(/^(@[\w-]+)(\(\s*\))?/)))) {
                    lookups = this.mixin.ruleLookups();
                    if (!lookups && ((inValue && parserInput.$str('()') !== '()') || (name[2] !== '()'))) {
                        parserInput.restore('Missing \'[...]\' lookup in variable call');
                        return;
                    }
                    if (!inValue) {
                        name = name[1];
                    }
                    var call = new tree.VariableCall(name, i, fileInfo);
                    if (!inValue && parsers.end()) {
                        parserInput.forget();
                        return call;
                    }
                    else {
                        parserInput.forget();
                        return new tree.NamespaceValue(call, lookups, i, fileInfo);
                    }
                }
                parserInput.restore();
            },
            //
            // extend syntax - used to extend selectors
            //
            extend: function (isRule) {
                var elements;
                var e;
                var index = parserInput.i;
                var option;
                var extendList;
                var extend;
                if (!parserInput.$str(isRule ? '&:extend(' : ':extend(')) {
                    return;
                }
                do {
                    option = null;
                    elements = null;
                    while (!(option = parserInput.$re(/^(all)(?=\s*(\)|,))/))) {
                        e = this.element();
                        if (!e) {
                            break;
                        }
                        if (elements) {
                            elements.push(e);
                        }
                        else {
                            elements = [e];
                        }
                    }
                    option = option && option[1];
                    if (!elements) {
                        error('Missing target selector for :extend().');
                    }
                    extend = new (tree.Extend)(new (tree.Selector)(elements), option, index, fileInfo);
                    if (extendList) {
                        extendList.push(extend);
                    }
                    else {
                        extendList = [extend];
                    }
                } while (parserInput.$char(','));
                expect(/^\)/);
                if (isRule) {
                    expect(/^;/);
                }
                return extendList;
            },
            //
            // extendRule - used in a rule to extend all the parent selectors
            //
            extendRule: function () {
                return this.extend(true);
            },
            //
            // Mixins
            //
            mixin: {
                //
                // A Mixin call, with an optional argument list
                //
                //     #mixins > .square(#fff);
                //     #mixins.square(#fff);
                //     .rounded(4px, black);
                //     .button;
                //
                // We can lookup / return a value using the lookup syntax:
                //
                //     color: #mixin.square(#fff)[@color];
                //
                // The `while` loop is there because mixins can be
                // namespaced, but we only support the child and descendant
                // selector for now.
                //
                call: function (inValue, getLookup) {
                    var s = parserInput.currentChar();
                    var important = false;
                    var lookups;
                    var index = parserInput.i;
                    var elements;
                    var args;
                    var hasParens;
                    if (s !== '.' && s !== '#') {
                        return;
                    }
                    parserInput.save(); // stop us absorbing part of an invalid selector
                    elements = this.elements();
                    if (elements) {
                        if (parserInput.$char('(')) {
                            args = this.args(true).args;
                            expectChar(')');
                            hasParens = true;
                        }
                        if (getLookup !== false) {
                            lookups = this.ruleLookups();
                        }
                        if (getLookup === true && !lookups) {
                            parserInput.restore();
                            return;
                        }
                        if (inValue && !lookups && !hasParens) {
                            // This isn't a valid in-value mixin call
                            parserInput.restore();
                            return;
                        }
                        if (!inValue && parsers.important()) {
                            important = true;
                        }
                        if (inValue || parsers.end()) {
                            parserInput.forget();
                            var mixin = new (tree.mixin.Call)(elements, args, index, fileInfo, !lookups && important);
                            if (lookups) {
                                return new tree.NamespaceValue(mixin, lookups);
                            }
                            else {
                                return mixin;
                            }
                        }
                    }
                    parserInput.restore();
                },
                /**
                 * Matching elements for mixins
                 * (Start with . or # and can have > )
                 */
                elements: function () {
                    var elements;
                    var e;
                    var c;
                    var elem;
                    var elemIndex;
                    var re = /^[#.](?:[\w-]|\\(?:[A-Fa-f0-9]{1,6} ?|[^A-Fa-f0-9]))+/;
                    while (true) {
                        elemIndex = parserInput.i;
                        e = parserInput.$re(re);
                        if (!e) {
                            break;
                        }
                        elem = new (tree.Element)(c, e, false, elemIndex, fileInfo);
                        if (elements) {
                            elements.push(elem);
                        }
                        else {
                            elements = [elem];
                        }
                        c = parserInput.$char('>');
                    }
                    return elements;
                },
                args: function (isCall) {
                    var entities = parsers.entities;
                    var returner = { args: null, variadic: false };
                    var expressions = [];
                    var argsSemiColon = [];
                    var argsComma = [];
                    var isSemiColonSeparated;
                    var expressionContainsNamed;
                    var name;
                    var nameLoop;
                    var value;
                    var arg;
                    var expand;
                    var hasSep = true;
                    parserInput.save();
                    while (true) {
                        if (isCall) {
                            arg = parsers.detachedRuleset() || parsers.expression();
                        }
                        else {
                            parserInput.commentStore.length = 0;
                            if (parserInput.$str('...')) {
                                returner.variadic = true;
                                if (parserInput.$char(';') && !isSemiColonSeparated) {
                                    isSemiColonSeparated = true;
                                }
                                (isSemiColonSeparated ? argsSemiColon : argsComma)
                                    .push({ variadic: true });
                                break;
                            }
                            arg = entities.variable() || entities.property() || entities.literal() || entities.keyword() || this.call(true);
                        }
                        if (!arg || !hasSep) {
                            break;
                        }
                        nameLoop = null;
                        if (arg.throwAwayComments) {
                            arg.throwAwayComments();
                        }
                        value = arg;
                        var val = null;
                        if (isCall) {
                            // Variable
                            if (arg.value && arg.value.length == 1) {
                                val = arg.value[0];
                            }
                        }
                        else {
                            val = arg;
                        }
                        if (val && (val instanceof tree.Variable || val instanceof tree.Property)) {
                            if (parserInput.$char(':')) {
                                if (expressions.length > 0) {
                                    if (isSemiColonSeparated) {
                                        error('Cannot mix ; and , as delimiter types');
                                    }
                                    expressionContainsNamed = true;
                                }
                                value = parsers.detachedRuleset() || parsers.expression();
                                if (!value) {
                                    if (isCall) {
                                        error('could not understand value for named argument');
                                    }
                                    else {
                                        parserInput.restore();
                                        returner.args = [];
                                        return returner;
                                    }
                                }
                                nameLoop = (name = val.name);
                            }
                            else if (parserInput.$str('...')) {
                                if (!isCall) {
                                    returner.variadic = true;
                                    if (parserInput.$char(';') && !isSemiColonSeparated) {
                                        isSemiColonSeparated = true;
                                    }
                                    (isSemiColonSeparated ? argsSemiColon : argsComma)
                                        .push({ name: arg.name, variadic: true });
                                    break;
                                }
                                else {
                                    expand = true;
                                }
                            }
                            else if (!isCall) {
                                name = nameLoop = val.name;
                                value = null;
                            }
                        }
                        if (value) {
                            expressions.push(value);
                        }
                        argsComma.push({ name: nameLoop, value: value, expand: expand });
                        if (parserInput.$char(',')) {
                            hasSep = true;
                            continue;
                        }
                        hasSep = parserInput.$char(';') === ';';
                        if (hasSep || isSemiColonSeparated) {
                            if (expressionContainsNamed) {
                                error('Cannot mix ; and , as delimiter types');
                            }
                            isSemiColonSeparated = true;
                            if (expressions.length > 1) {
                                value = new (tree.Value)(expressions);
                            }
                            argsSemiColon.push({ name: name, value: value, expand: expand });
                            name = null;
                            expressions = [];
                            expressionContainsNamed = false;
                        }
                    }
                    parserInput.forget();
                    returner.args = isSemiColonSeparated ? argsSemiColon : argsComma;
                    return returner;
                },
                //
                // A Mixin definition, with a list of parameters
                //
                //     .rounded (@radius: 2px, @color) {
                //        ...
                //     }
                //
                // Until we have a finer grained state-machine, we have to
                // do a look-ahead, to make sure we don't have a mixin call.
                // See the `rule` function for more information.
                //
                // We start by matching `.rounded (`, and then proceed on to
                // the argument list, which has optional default values.
                // We store the parameters in `params`, with a `value` key,
                // if there is a value, such as in the case of `@radius`.
                //
                // Once we've got our params list, and a closing `)`, we parse
                // the `{...}` block.
                //
                definition: function () {
                    var name;
                    var params = [];
                    var match;
                    var ruleset;
                    var cond;
                    var variadic = false;
                    if ((parserInput.currentChar() !== '.' && parserInput.currentChar() !== '#') ||
                        parserInput.peek(/^[^{]*\}/)) {
                        return;
                    }
                    parserInput.save();
                    match = parserInput.$re(/^([#.](?:[\w-]|\\(?:[A-Fa-f0-9]{1,6} ?|[^A-Fa-f0-9]))+)\s*\(/);
                    if (match) {
                        name = match[1];
                        var argInfo = this.args(false);
                        params = argInfo.args;
                        variadic = argInfo.variadic;
                        // .mixincall("@{a}");
                        // looks a bit like a mixin definition..
                        // also
                        // .mixincall(@a: {rule: set;});
                        // so we have to be nice and restore
                        if (!parserInput.$char(')')) {
                            parserInput.restore('Missing closing \')\'');
                            return;
                        }
                        parserInput.commentStore.length = 0;
                        if (parserInput.$str('when')) { // Guard
                            cond = expect(parsers.conditions, 'expected condition');
                        }
                        ruleset = parsers.block();
                        if (ruleset) {
                            parserInput.forget();
                            return new (tree.mixin.Definition)(name, params, ruleset, cond, variadic);
                        }
                        else {
                            parserInput.restore();
                        }
                    }
                    else {
                        parserInput.restore();
                    }
                },
                ruleLookups: function () {
                    var rule;
                    var lookups = [];
                    if (parserInput.currentChar() !== '[') {
                        return;
                    }
                    while (true) {
                        parserInput.save();
                        rule = this.lookupValue();
                        if (!rule && rule !== '') {
                            parserInput.restore();
                            break;
                        }
                        lookups.push(rule);
                        parserInput.forget();
                    }
                    if (lookups.length > 0) {
                        return lookups;
                    }
                },
                lookupValue: function () {
                    parserInput.save();
                    if (!parserInput.$char('[')) {
                        parserInput.restore();
                        return;
                    }
                    var name = parserInput.$re(/^(?:[@$]{0,2})[_a-zA-Z0-9-]*/);
                    if (!parserInput.$char(']')) {
                        parserInput.restore();
                        return;
                    }
                    if (name || name === '') {
                        parserInput.forget();
                        return name;
                    }
                    parserInput.restore();
                }
            },
            //
            // Entities are the smallest recognized token,
            // and can be found inside a rule's value.
            //
            entity: function () {
                var entities = this.entities;
                return this.comment() || entities.literal() || entities.variable() || entities.url() ||
                    entities.property() || entities.call() || entities.keyword() || this.mixin.call(true) ||
                    entities.javascript();
            },
            //
            // A Declaration terminator. Note that we use `peek()` to check for '}',
            // because the `block` rule will be expecting it, but we still need to make sure
            // it's there, if ';' was omitted.
            //
            end: function () {
                return parserInput.$char(';') || parserInput.peek('}');
            },
            //
            // IE's alpha function
            //
            //     alpha(opacity=88)
            //
            ieAlpha: function () {
                var value;
                // http://jsperf.com/case-insensitive-regex-vs-strtolower-then-regex/18
                if (!parserInput.$re(/^opacity=/i)) {
                    return;
                }
                value = parserInput.$re(/^\d+/);
                if (!value) {
                    value = expect(parsers.entities.variable, 'Could not parse alpha');
                    value = "@{" + value.name.slice(1) + "}";
                }
                expectChar(')');
                return new tree.Quoted('', "alpha(opacity=" + value + ")");
            },
            //
            // A Selector Element
            //
            //     div
            //     + h1
            //     #socks
            //     input[type="text"]
            //
            // Elements are the building blocks for Selectors,
            // they are made out of a `Combinator` (see combinator rule),
            // and an element name, such as a tag a class, or `*`.
            //
            element: function () {
                var e;
                var c;
                var v;
                var index = parserInput.i;
                c = this.combinator();
                e = parserInput.$re(/^(?:\d+\.\d+|\d+)%/) ||
                    parserInput.$re(/^(?:[.#]?|:*)(?:[\w-]|[^\x00-\x9f]|\\(?:[A-Fa-f0-9]{1,6} ?|[^A-Fa-f0-9]))+/) ||
                    parserInput.$char('*') || parserInput.$char('&') || this.attribute() ||
                    parserInput.$re(/^\([^&()@]+\)/) || parserInput.$re(/^[\.#:](?=@)/) ||
                    this.entities.variableCurly();
                if (!e) {
                    parserInput.save();
                    if (parserInput.$char('(')) {
                        if ((v = this.selector(false)) && parserInput.$char(')')) {
                            e = new (tree.Paren)(v);
                            parserInput.forget();
                        }
                        else {
                            parserInput.restore('Missing closing \')\'');
                        }
                    }
                    else {
                        parserInput.forget();
                    }
                }
                if (e) {
                    return new (tree.Element)(c, e, e instanceof tree.Variable, index, fileInfo);
                }
            },
            //
            // Combinators combine elements together, in a Selector.
            //
            // Because our parser isn't white-space sensitive, special care
            // has to be taken, when parsing the descendant combinator, ` `,
            // as it's an empty space. We have to check the previous character
            // in the input, to see if it's a ` ` character. More info on how
            // we deal with this in *combinator.js*.
            //
            combinator: function () {
                var c = parserInput.currentChar();
                if (c === '/') {
                    parserInput.save();
                    var slashedCombinator = parserInput.$re(/^\/[a-z]+\//i);
                    if (slashedCombinator) {
                        parserInput.forget();
                        return new (tree.Combinator)(slashedCombinator);
                    }
                    parserInput.restore();
                }
                if (c === '>' || c === '+' || c === '~' || c === '|' || c === '^') {
                    parserInput.i++;
                    if (c === '^' && parserInput.currentChar() === '^') {
                        c = '^^';
                        parserInput.i++;
                    }
                    while (parserInput.isWhitespace()) {
                        parserInput.i++;
                    }
                    return new (tree.Combinator)(c);
                }
                else if (parserInput.isWhitespace(-1)) {
                    return new (tree.Combinator)(' ');
                }
                else {
                    return new (tree.Combinator)(null);
                }
            },
            //
            // A CSS Selector
            // with less extensions e.g. the ability to extend and guard
            //
            //     .class > div + h1
            //     li a:hover
            //
            // Selectors are made out of one or more Elements, see above.
            //
            selector: function (isLess) {
                var index = parserInput.i;
                var elements;
                var extendList;
                var c;
                var e;
                var allExtends;
                var when;
                var condition;
                isLess = isLess !== false;
                while ((isLess && (extendList = this.extend())) || (isLess && (when = parserInput.$str('when'))) || (e = this.element())) {
                    if (when) {
                        condition = expect(this.conditions, 'expected condition');
                    }
                    else if (condition) {
                        error('CSS guard can only be used at the end of selector');
                    }
                    else if (extendList) {
                        if (allExtends) {
                            allExtends = allExtends.concat(extendList);
                        }
                        else {
                            allExtends = extendList;
                        }
                    }
                    else {
                        if (allExtends) {
                            error('Extend can only be used at the end of selector');
                        }
                        c = parserInput.currentChar();
                        if (elements) {
                            elements.push(e);
                        }
                        else {
                            elements = [e];
                        }
                        e = null;
                    }
                    if (c === '{' || c === '}' || c === ';' || c === ',' || c === ')') {
                        break;
                    }
                }
                if (elements) {
                    return new (tree.Selector)(elements, allExtends, condition, index, fileInfo);
                }
                if (allExtends) {
                    error('Extend must be used to extend a selector, it cannot be used on its own');
                }
            },
            selectors: function () {
                var s;
                var selectors;
                while (true) {
                    s = this.selector();
                    if (!s) {
                        break;
                    }
                    if (selectors) {
                        selectors.push(s);
                    }
                    else {
                        selectors = [s];
                    }
                    parserInput.commentStore.length = 0;
                    if (s.condition && selectors.length > 1) {
                        error("Guards are only currently allowed on a single selector.");
                    }
                    if (!parserInput.$char(',')) {
                        break;
                    }
                    if (s.condition) {
                        error("Guards are only currently allowed on a single selector.");
                    }
                    parserInput.commentStore.length = 0;
                }
                return selectors;
            },
            attribute: function () {
                if (!parserInput.$char('[')) {
                    return;
                }
                var entities = this.entities;
                var key;
                var val;
                var op;
                if (!(key = entities.variableCurly())) {
                    key = expect(/^(?:[_A-Za-z0-9-\*]*\|)?(?:[_A-Za-z0-9-]|\\.)+/);
                }
                op = parserInput.$re(/^[|~*$^]?=/);
                if (op) {
                    val = entities.quoted() || parserInput.$re(/^[0-9]+%/) || parserInput.$re(/^[\w-]+/) || entities.variableCurly();
                }
                expectChar(']');
                return new (tree.Attribute)(key, op, val);
            },
            //
            // The `block` rule is used by `ruleset` and `mixin.definition`.
            // It's a wrapper around the `primary` rule, with added `{}`.
            //
            block: function () {
                var content;
                if (parserInput.$char('{') && (content = this.primary()) && parserInput.$char('}')) {
                    return content;
                }
            },
            blockRuleset: function () {
                var block = this.block();
                if (block) {
                    block = new tree.Ruleset(null, block);
                }
                return block;
            },
            detachedRuleset: function () {
                var argInfo;
                var params;
                var variadic;
                parserInput.save();
                if (parserInput.$re(/^[.#]\(/)) {
                    /**
                     * DR args currently only implemented for each() function, and not
                     * yet settable as `@dr: #(@arg) {}`
                     * This should be done when DRs are merged with mixins.
                     * See: https://github.com/less/less-meta/issues/16
                     */
                    argInfo = this.mixin.args(false);
                    params = argInfo.args;
                    variadic = argInfo.variadic;
                    if (!parserInput.$char(')')) {
                        parserInput.restore();
                        return;
                    }
                }
                var blockRuleset = this.blockRuleset();
                if (blockRuleset) {
                    parserInput.forget();
                    if (params) {
                        return new tree.mixin.Definition(null, params, blockRuleset, null, variadic);
                    }
                    return new tree.DetachedRuleset(blockRuleset);
                }
                parserInput.restore();
            },
            //
            // div, .class, body > p {...}
            //
            ruleset: function () {
                var selectors;
                var rules;
                var debugInfo;
                parserInput.save();
                if (context.dumpLineNumbers) {
                    debugInfo = getDebugInfo(parserInput.i);
                }
                selectors = this.selectors();
                if (selectors && (rules = this.block())) {
                    parserInput.forget();
                    var ruleset = new (tree.Ruleset)(selectors, rules, context.strictImports);
                    if (context.dumpLineNumbers) {
                        ruleset.debugInfo = debugInfo;
                    }
                    return ruleset;
                }
                else {
                    parserInput.restore();
                }
            },
            declaration: function () {
                var name;
                var value;
                var index = parserInput.i;
                var hasDR;
                var c = parserInput.currentChar();
                var important;
                var merge;
                var isVariable;
                if (c === '.' || c === '#' || c === '&' || c === ':') {
                    return;
                }
                parserInput.save();
                name = this.variable() || this.ruleProperty();
                if (name) {
                    isVariable = typeof name === 'string';
                    if (isVariable) {
                        value = this.detachedRuleset();
                        if (value) {
                            hasDR = true;
                        }
                    }
                    parserInput.commentStore.length = 0;
                    if (!value) {
                        // a name returned by this.ruleProperty() is always an array of the form:
                        // [string-1, ..., string-n, ""] or [string-1, ..., string-n, "+"]
                        // where each item is a tree.Keyword or tree.Variable
                        merge = !isVariable && name.length > 1 && name.pop().value;
                        // Custom property values get permissive parsing
                        if (name[0].value && name[0].value.slice(0, 2) === '--') {
                            value = this.permissiveValue();
                        }
                        // Try to store values as anonymous
                        // If we need the value later we'll re-parse it in ruleset.parseValue
                        else {
                            value = this.anonymousValue();
                        }
                        if (value) {
                            parserInput.forget();
                            // anonymous values absorb the end ';' which is required for them to work
                            return new (tree.Declaration)(name, value, false, merge, index, fileInfo);
                        }
                        if (!value) {
                            value = this.value();
                        }
                        if (value) {
                            important = this.important();
                        }
                        else if (isVariable) {
                            // As a last resort, try permissiveValue
                            value = this.permissiveValue();
                        }
                    }
                    if (value && (this.end() || hasDR)) {
                        parserInput.forget();
                        return new (tree.Declaration)(name, value, important, merge, index, fileInfo);
                    }
                    else {
                        parserInput.restore();
                    }
                }
                else {
                    parserInput.restore();
                }
            },
            anonymousValue: function () {
                var index = parserInput.i;
                var match = parserInput.$re(/^([^.#@\$+\/'"*`(;{}-]*);/);
                if (match) {
                    return new (tree.Anonymous)(match[1], index);
                }
            },
            /**
             * Used for custom properties, at-rules, and variables (as fallback)
             * Parses almost anything inside of {} [] () "" blocks
             * until it reaches outer-most tokens.
             *
             * First, it will try to parse comments and entities to reach
             * the end. This is mostly like the Expression parser except no
             * math is allowed.
             */
            permissiveValue: function (untilTokens) {
                var i;
                var e;
                var done;
                var value;
                var tok = untilTokens || ';';
                var index = parserInput.i;
                var result = [];
                function testCurrentChar() {
                    var char = parserInput.currentChar();
                    if (typeof tok === 'string') {
                        return char === tok;
                    }
                    else {
                        return tok.test(char);
                    }
                }
                if (testCurrentChar()) {
                    return;
                }
                value = [];
                do {
                    e = this.comment();
                    if (e) {
                        value.push(e);
                        continue;
                    }
                    e = this.entity();
                    if (e) {
                        value.push(e);
                    }
                } while (e);
                done = testCurrentChar();
                if (value.length > 0) {
                    value = new (tree.Expression)(value);
                    if (done) {
                        return value;
                    }
                    else {
                        result.push(value);
                    }
                    // Preserve space before $parseUntil as it will not
                    if (parserInput.prevChar() === ' ') {
                        result.push(new tree.Anonymous(' ', index));
                    }
                }
                parserInput.save();
                value = parserInput.$parseUntil(tok);
                if (value) {
                    if (typeof value === 'string') {
                        error("Expected '" + value + "'", 'Parse');
                    }
                    if (value.length === 1 && value[0] === ' ') {
                        parserInput.forget();
                        return new tree.Anonymous('', index);
                    }
                    var item = void 0;
                    for (i = 0; i < value.length; i++) {
                        item = value[i];
                        if (Array.isArray(item)) {
                            // Treat actual quotes as normal quoted values
                            result.push(new tree.Quoted(item[0], item[1], true, index, fileInfo));
                        }
                        else {
                            if (i === value.length - 1) {
                                item = item.trim();
                            }
                            // Treat like quoted values, but replace vars like unquoted expressions
                            var quote = new tree.Quoted('\'', item, true, index, fileInfo);
                            quote.variableRegex = /@([\w-]+)/g;
                            quote.propRegex = /\$([\w-]+)/g;
                            result.push(quote);
                        }
                    }
                    parserInput.forget();
                    return new tree.Expression(result, true);
                }
                parserInput.restore();
            },
            //
            // An @import atrule
            //
            //     @import "lib";
            //
            // Depending on our environment, importing is done differently:
            // In the browser, it's an XHR request, in Node, it would be a
            // file-system operation. The function used for importing is
            // stored in `import`, which we pass to the Import constructor.
            //
            'import': function () {
                var path;
                var features;
                var index = parserInput.i;
                var dir = parserInput.$re(/^@import?\s+/);
                if (dir) {
                    var options_1 = (dir ? this.importOptions() : null) || {};
                    if ((path = this.entities.quoted() || this.entities.url())) {
                        features = this.mediaFeatures();
                        if (!parserInput.$char(';')) {
                            parserInput.i = index;
                            error('missing semi-colon or unrecognised media features on import');
                        }
                        features = features && new (tree.Value)(features);
                        return new (tree.Import)(path, features, options_1, index, fileInfo);
                    }
                    else {
                        parserInput.i = index;
                        error('malformed import statement');
                    }
                }
            },
            importOptions: function () {
                var o;
                var options = {};
                var optionName;
                var value;
                // list of options, surrounded by parens
                if (!parserInput.$char('(')) {
                    return null;
                }
                do {
                    o = this.importOption();
                    if (o) {
                        optionName = o;
                        value = true;
                        switch (optionName) {
                            case 'css':
                                optionName = 'less';
                                value = false;
                                break;
                            case 'once':
                                optionName = 'multiple';
                                value = false;
                                break;
                        }
                        options[optionName] = value;
                        if (!parserInput.$char(',')) {
                            break;
                        }
                    }
                } while (o);
                expectChar(')');
                return options;
            },
            importOption: function () {
                var opt = parserInput.$re(/^(less|css|multiple|once|inline|reference|optional)/);
                if (opt) {
                    return opt[1];
                }
            },
            mediaFeature: function () {
                var entities = this.entities;
                var nodes = [];
                var e;
                var p;
                parserInput.save();
                do {
                    e = entities.keyword() || entities.variable() || entities.mixinLookup();
                    if (e) {
                        nodes.push(e);
                    }
                    else if (parserInput.$char('(')) {
                        p = this.property();
                        e = this.value();
                        if (parserInput.$char(')')) {
                            if (p && e) {
                                nodes.push(new (tree.Paren)(new (tree.Declaration)(p, e, null, null, parserInput.i, fileInfo, true)));
                            }
                            else if (e) {
                                nodes.push(new (tree.Paren)(e));
                            }
                            else {
                                error('badly formed media feature definition');
                            }
                        }
                        else {
                            error('Missing closing \')\'', 'Parse');
                        }
                    }
                } while (e);
                parserInput.forget();
                if (nodes.length > 0) {
                    return new (tree.Expression)(nodes);
                }
            },
            mediaFeatures: function () {
                var entities = this.entities;
                var features = [];
                var e;
                do {
                    e = this.mediaFeature();
                    if (e) {
                        features.push(e);
                        if (!parserInput.$char(',')) {
                            break;
                        }
                    }
                    else {
                        e = entities.variable() || entities.mixinLookup();
                        if (e) {
                            features.push(e);
                            if (!parserInput.$char(',')) {
                                break;
                            }
                        }
                    }
                } while (e);
                return features.length > 0 ? features : null;
            },
            media: function () {
                var features;
                var rules;
                var media;
                var debugInfo;
                var index = parserInput.i;
                if (context.dumpLineNumbers) {
                    debugInfo = getDebugInfo(index);
                }
                parserInput.save();
                if (parserInput.$str('@media')) {
                    features = this.mediaFeatures();
                    rules = this.block();
                    if (!rules) {
                        error('media definitions require block statements after any features');
                    }
                    parserInput.forget();
                    media = new (tree.Media)(rules, features, index, fileInfo);
                    if (context.dumpLineNumbers) {
                        media.debugInfo = debugInfo;
                    }
                    return media;
                }
                parserInput.restore();
            },
            //
            // A @plugin directive, used to import plugins dynamically.
            //
            //     @plugin (args) "lib";
            //
            plugin: function () {
                var path;
                var args;
                var options;
                var index = parserInput.i;
                var dir = parserInput.$re(/^@plugin?\s+/);
                if (dir) {
                    args = this.pluginArgs();
                    if (args) {
                        options = {
                            pluginArgs: args,
                            isPlugin: true
                        };
                    }
                    else {
                        options = { isPlugin: true };
                    }
                    if ((path = this.entities.quoted() || this.entities.url())) {
                        if (!parserInput.$char(';')) {
                            parserInput.i = index;
                            error('missing semi-colon on @plugin');
                        }
                        return new (tree.Import)(path, null, options, index, fileInfo);
                    }
                    else {
                        parserInput.i = index;
                        error('malformed @plugin statement');
                    }
                }
            },
            pluginArgs: function () {
                // list of options, surrounded by parens
                parserInput.save();
                if (!parserInput.$char('(')) {
                    parserInput.restore();
                    return null;
                }
                var args = parserInput.$re(/^\s*([^\);]+)\)\s*/);
                if (args[1]) {
                    parserInput.forget();
                    return args[1].trim();
                }
                else {
                    parserInput.restore();
                    return null;
                }
            },
            //
            // A CSS AtRule
            //
            //     @charset "utf-8";
            //
            atrule: function () {
                var index = parserInput.i;
                var name;
                var value;
                var rules;
                var nonVendorSpecificName;
                var hasIdentifier;
                var hasExpression;
                var hasUnknown;
                var hasBlock = true;
                var isRooted = true;
                if (parserInput.currentChar() !== '@') {
                    return;
                }
                value = this['import']() || this.plugin() || this.media();
                if (value) {
                    return value;
                }
                parserInput.save();
                name = parserInput.$re(/^@[a-z-]+/);
                if (!name) {
                    return;
                }
                nonVendorSpecificName = name;
                if (name.charAt(1) == '-' && name.indexOf('-', 2) > 0) {
                    nonVendorSpecificName = "@" + name.slice(name.indexOf('-', 2) + 1);
                }
                switch (nonVendorSpecificName) {
                    case '@charset':
                        hasIdentifier = true;
                        hasBlock = false;
                        break;
                    case '@namespace':
                        hasExpression = true;
                        hasBlock = false;
                        break;
                    case '@keyframes':
                    case '@counter-style':
                        hasIdentifier = true;
                        break;
                    case '@document':
                    case '@supports':
                        hasUnknown = true;
                        isRooted = false;
                        break;
                    default:
                        hasUnknown = true;
                        break;
                }
                parserInput.commentStore.length = 0;
                if (hasIdentifier) {
                    value = this.entity();
                    if (!value) {
                        error("expected " + name + " identifier");
                    }
                }
                else if (hasExpression) {
                    value = this.expression();
                    if (!value) {
                        error("expected " + name + " expression");
                    }
                }
                else if (hasUnknown) {
                    value = this.permissiveValue(/^[{;]/);
                    hasBlock = (parserInput.currentChar() === '{');
                    if (!value) {
                        if (!hasBlock && parserInput.currentChar() !== ';') {
                            error(name + " rule is missing block or ending semi-colon");
                        }
                    }
                    else if (!value.value) {
                        value = null;
                    }
                }
                if (hasBlock) {
                    rules = this.blockRuleset();
                }
                if (rules || (!hasBlock && value && parserInput.$char(';'))) {
                    parserInput.forget();
                    return new (tree.AtRule)(name, value, rules, index, fileInfo, context.dumpLineNumbers ? getDebugInfo(index) : null, isRooted);
                }
                parserInput.restore('at-rule options not recognised');
            },
            //
            // A Value is a comma-delimited list of Expressions
            //
            //     font-family: Baskerville, Georgia, serif;
            //
            // In a Rule, a Value represents everything after the `:`,
            // and before the `;`.
            //
            value: function () {
                var e;
                var expressions = [];
                var index = parserInput.i;
                do {
                    e = this.expression();
                    if (e) {
                        expressions.push(e);
                        if (!parserInput.$char(',')) {
                            break;
                        }
                    }
                } while (e);
                if (expressions.length > 0) {
                    return new (tree.Value)(expressions, index);
                }
            },
            important: function () {
                if (parserInput.currentChar() === '!') {
                    return parserInput.$re(/^! *important/);
                }
            },
            sub: function () {
                var a;
                var e;
                parserInput.save();
                if (parserInput.$char('(')) {
                    a = this.addition();
                    if (a && parserInput.$char(')')) {
                        parserInput.forget();
                        e = new (tree.Expression)([a]);
                        e.parens = true;
                        return e;
                    }
                    parserInput.restore('Expected \')\'');
                    return;
                }
                parserInput.restore();
            },
            multiplication: function () {
                var m;
                var a;
                var op;
                var operation;
                var isSpaced;
                m = this.operand();
                if (m) {
                    isSpaced = parserInput.isWhitespace(-1);
                    while (true) {
                        if (parserInput.peek(/^\/[*\/]/)) {
                            break;
                        }
                        parserInput.save();
                        op = parserInput.$char('/') || parserInput.$char('*') || parserInput.$str('./');
                        if (!op) {
                            parserInput.forget();
                            break;
                        }
                        a = this.operand();
                        if (!a) {
                            parserInput.restore();
                            break;
                        }
                        parserInput.forget();
                        m.parensInOp = true;
                        a.parensInOp = true;
                        operation = new (tree.Operation)(op, [operation || m, a], isSpaced);
                        isSpaced = parserInput.isWhitespace(-1);
                    }
                    return operation || m;
                }
            },
            addition: function () {
                var m;
                var a;
                var op;
                var operation;
                var isSpaced;
                m = this.multiplication();
                if (m) {
                    isSpaced = parserInput.isWhitespace(-1);
                    while (true) {
                        op = parserInput.$re(/^[-+]\s+/) || (!isSpaced && (parserInput.$char('+') || parserInput.$char('-')));
                        if (!op) {
                            break;
                        }
                        a = this.multiplication();
                        if (!a) {
                            break;
                        }
                        m.parensInOp = true;
                        a.parensInOp = true;
                        operation = new (tree.Operation)(op, [operation || m, a], isSpaced);
                        isSpaced = parserInput.isWhitespace(-1);
                    }
                    return operation || m;
                }
            },
            conditions: function () {
                var a;
                var b;
                var index = parserInput.i;
                var condition;
                a = this.condition(true);
                if (a) {
                    while (true) {
                        if (!parserInput.peek(/^,\s*(not\s*)?\(/) || !parserInput.$char(',')) {
                            break;
                        }
                        b = this.condition(true);
                        if (!b) {
                            break;
                        }
                        condition = new (tree.Condition)('or', condition || a, b, index);
                    }
                    return condition || a;
                }
            },
            condition: function (needsParens) {
                var result;
                var logical;
                var next;
                function or() {
                    return parserInput.$str('or');
                }
                result = this.conditionAnd(needsParens);
                if (!result) {
                    return;
                }
                logical = or();
                if (logical) {
                    next = this.condition(needsParens);
                    if (next) {
                        result = new (tree.Condition)(logical, result, next);
                    }
                    else {
                        return;
                    }
                }
                return result;
            },
            conditionAnd: function (needsParens) {
                var result;
                var logical;
                var next;
                var self = this;
                function insideCondition() {
                    var cond = self.negatedCondition(needsParens) || self.parenthesisCondition(needsParens);
                    if (!cond && !needsParens) {
                        return self.atomicCondition(needsParens);
                    }
                    return cond;
                }
                function and() {
                    return parserInput.$str('and');
                }
                result = insideCondition();
                if (!result) {
                    return;
                }
                logical = and();
                if (logical) {
                    next = this.conditionAnd(needsParens);
                    if (next) {
                        result = new (tree.Condition)(logical, result, next);
                    }
                    else {
                        return;
                    }
                }
                return result;
            },
            negatedCondition: function (needsParens) {
                if (parserInput.$str('not')) {
                    var result = this.parenthesisCondition(needsParens);
                    if (result) {
                        result.negate = !result.negate;
                    }
                    return result;
                }
            },
            parenthesisCondition: function (needsParens) {
                function tryConditionFollowedByParenthesis(me) {
                    var body;
                    parserInput.save();
                    body = me.condition(needsParens);
                    if (!body) {
                        parserInput.restore();
                        return;
                    }
                    if (!parserInput.$char(')')) {
                        parserInput.restore();
                        return;
                    }
                    parserInput.forget();
                    return body;
                }
                var body;
                parserInput.save();
                if (!parserInput.$str('(')) {
                    parserInput.restore();
                    return;
                }
                body = tryConditionFollowedByParenthesis(this);
                if (body) {
                    parserInput.forget();
                    return body;
                }
                body = this.atomicCondition(needsParens);
                if (!body) {
                    parserInput.restore();
                    return;
                }
                if (!parserInput.$char(')')) {
                    parserInput.restore("expected ')' got '" + parserInput.currentChar() + "'");
                    return;
                }
                parserInput.forget();
                return body;
            },
            atomicCondition: function (needsParens) {
                var entities = this.entities;
                var index = parserInput.i;
                var a;
                var b;
                var c;
                var op;
                function cond() {
                    return this.addition() || entities.keyword() || entities.quoted() || entities.mixinLookup();
                }
                cond = cond.bind(this);
                a = cond();
                if (a) {
                    if (parserInput.$char('>')) {
                        if (parserInput.$char('=')) {
                            op = '>=';
                        }
                        else {
                            op = '>';
                        }
                    }
                    else if (parserInput.$char('<')) {
                        if (parserInput.$char('=')) {
                            op = '<=';
                        }
                        else {
                            op = '<';
                        }
                    }
                    else if (parserInput.$char('=')) {
                        if (parserInput.$char('>')) {
                            op = '=>';
                        }
                        else if (parserInput.$char('<')) {
                            op = '=<';
                        }
                        else {
                            op = '=';
                        }
                    }
                    if (op) {
                        b = cond();
                        if (b) {
                            c = new (tree.Condition)(op, a, b, index, false);
                        }
                        else {
                            error('expected expression');
                        }
                    }
                    else {
                        c = new (tree.Condition)('=', a, new (tree.Keyword)('true'), index, false);
                    }
                    return c;
                }
            },
            //
            // An operand is anything that can be part of an operation,
            // such as a Color, or a Variable
            //
            operand: function () {
                var entities = this.entities;
                var negate;
                if (parserInput.peek(/^-[@\$\(]/)) {
                    negate = parserInput.$char('-');
                }
                var o = this.sub() || entities.dimension() ||
                    entities.color() || entities.variable() ||
                    entities.property() || entities.call() ||
                    entities.quoted(true) || entities.colorKeyword() ||
                    entities.mixinLookup();
                if (negate) {
                    o.parensInOp = true;
                    o = new (tree.Negative)(o);
                }
                return o;
            },
            //
            // Expressions either represent mathematical operations,
            // or white-space delimited Entities.
            //
            //     1px solid black
            //     @var * 2
            //
            expression: function () {
                var entities = [];
                var e;
                var delim;
                var index = parserInput.i;
                do {
                    e = this.comment();
                    if (e) {
                        entities.push(e);
                        continue;
                    }
                    e = this.addition() || this.entity();
                    if (e) {
                        entities.push(e);
                        // operations do not allow keyword "/" dimension (e.g. small/20px) so we support that here
                        if (!parserInput.peek(/^\/[\/*]/)) {
                            delim = parserInput.$char('/');
                            if (delim) {
                                entities.push(new (tree.Anonymous)(delim, index));
                            }
                        }
                    }
                } while (e);
                if (entities.length > 0) {
                    return new (tree.Expression)(entities);
                }
            },
            property: function () {
                var name = parserInput.$re(/^(\*?-?[_a-zA-Z0-9-]+)\s*:/);
                if (name) {
                    return name[1];
                }
            },
            ruleProperty: function () {
                var name = [];
                var index = [];
                var s;
                var k;
                parserInput.save();
                var simpleProperty = parserInput.$re(/^([_a-zA-Z0-9-]+)\s*:/);
                if (simpleProperty) {
                    name = [new (tree.Keyword)(simpleProperty[1])];
                    parserInput.forget();
                    return name;
                }
                function match(re) {
                    var i = parserInput.i;
                    var chunk = parserInput.$re(re);
                    if (chunk) {
                        index.push(i);
                        return name.push(chunk[1]);
                    }
                }
                match(/^(\*?)/);
                while (true) {
                    if (!match(/^((?:[\w-]+)|(?:[@\$]\{[\w-]+\}))/)) {
                        break;
                    }
                }
                if ((name.length > 1) && match(/^((?:\+_|\+)?)\s*:/)) {
                    parserInput.forget();
                    // at last, we have the complete match now. move forward,
                    // convert name particles to tree objects and return:
                    if (name[0] === '') {
                        name.shift();
                        index.shift();
                    }
                    for (k = 0; k < name.length; k++) {
                        s = name[k];
                        name[k] = (s.charAt(0) !== '@' && s.charAt(0) !== '$') ?
                            new (tree.Keyword)(s) :
                            (s.charAt(0) === '@' ?
                                new (tree.Variable)("@" + s.slice(2, -1), index[k], fileInfo) :
                                new (tree.Property)("$" + s.slice(2, -1), index[k], fileInfo));
                    }
                    return name;
                }
                parserInput.restore();
            }
        }
    };
};
Parser.serializeVars = function (vars) {
    var s = '';
    for (var name_1 in vars) {
        if (Object.hasOwnProperty.call(vars, name_1)) {
            var value = vars[name_1];
            s += ((name_1[0] === '@') ? '' : '@') + name_1 + ": " + value + ((String(value).slice(-1) === ';') ? '' : ';');
        }
    }
    return s;
};

function boolean(condition) {
    return condition ? Keyword.True : Keyword.False;
}
function If(condition, trueValue, falseValue) {
    return condition ? trueValue
        : (falseValue || new Anonymous);
}
var boolean$1 = { boolean: boolean, 'if': If };

var colorFunctions;
function clamp$1(val) {
    return Math.min(1, Math.max(0, val));
}
function hsla(origColor, hsl) {
    var color = colorFunctions.hsla(hsl.h, hsl.s, hsl.l, hsl.a);
    if (color) {
        if (origColor.value &&
            /^(rgb|hsl)/.test(origColor.value)) {
            color.value = origColor.value;
        }
        else {
            color.value = 'rgb';
        }
        return color;
    }
}
function toHSL(color) {
    if (color.toHSL) {
        return color.toHSL();
    }
    else {
        throw new Error('Argument cannot be evaluated to a color');
    }
}
function toHSV(color) {
    if (color.toHSV) {
        return color.toHSV();
    }
    else {
        throw new Error('Argument cannot be evaluated to a color');
    }
}
function number(n) {
    if (n instanceof Dimension) {
        return parseFloat(n.unit.is('%') ? n.value / 100 : n.value);
    }
    else if (typeof n === 'number') {
        return n;
    }
    else {
        throw {
            type: 'Argument',
            message: 'color functions take numbers as parameters'
        };
    }
}
function scaled(n, size) {
    if (n instanceof Dimension && n.unit.is('%')) {
        return parseFloat(n.value * size / 100);
    }
    else {
        return number(n);
    }
}
colorFunctions = {
    rgb: function (r, g, b) {
        var color = colorFunctions.rgba(r, g, b, 1.0);
        if (color) {
            color.value = 'rgb';
            return color;
        }
    },
    rgba: function (r, g, b, a) {
        try {
            if (r instanceof Color) {
                if (g) {
                    a = number(g);
                }
                else {
                    a = r.alpha;
                }
                return new Color(r.rgb, a, 'rgba');
            }
            var rgb = [r, g, b].map(function (c) { return scaled(c, 255); });
            a = number(a);
            return new Color(rgb, a, 'rgba');
        }
        catch (e) { }
    },
    hsl: function (h, s, l) {
        var color = colorFunctions.hsla(h, s, l, 1.0);
        if (color) {
            color.value = 'hsl';
            return color;
        }
    },
    hsla: function (h, s, l, a) {
        try {
            if (h instanceof Color) {
                if (s) {
                    a = number(s);
                }
                else {
                    a = h.alpha;
                }
                return new Color(h.rgb, a, 'hsla');
            }
            var m1_1;
            var m2_1;
            function hue(h) {
                h = h < 0 ? h + 1 : (h > 1 ? h - 1 : h);
                if (h * 6 < 1) {
                    return m1_1 + (m2_1 - m1_1) * h * 6;
                }
                else if (h * 2 < 1) {
                    return m2_1;
                }
                else if (h * 3 < 2) {
                    return m1_1 + (m2_1 - m1_1) * (2 / 3 - h) * 6;
                }
                else {
                    return m1_1;
                }
            }
            h = (number(h) % 360) / 360;
            s = clamp$1(number(s));
            l = clamp$1(number(l));
            a = clamp$1(number(a));
            m2_1 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
            m1_1 = l * 2 - m2_1;
            var rgb = [
                hue(h + 1 / 3) * 255,
                hue(h) * 255,
                hue(h - 1 / 3) * 255
            ];
            a = number(a);
            return new Color(rgb, a, 'hsla');
        }
        catch (e) { }
    },
    hsv: function (h, s, v) {
        return colorFunctions.hsva(h, s, v, 1.0);
    },
    hsva: function (h, s, v, a) {
        h = ((number(h) % 360) / 360) * 360;
        s = number(s);
        v = number(v);
        a = number(a);
        var i;
        var f;
        i = Math.floor((h / 60) % 6);
        f = (h / 60) - i;
        var vs = [v,
            v * (1 - s),
            v * (1 - f * s),
            v * (1 - (1 - f) * s)];
        var perm = [[0, 3, 1],
            [2, 0, 1],
            [1, 0, 3],
            [1, 2, 0],
            [3, 1, 0],
            [0, 1, 2]];
        return colorFunctions.rgba(vs[perm[i][0]] * 255, vs[perm[i][1]] * 255, vs[perm[i][2]] * 255, a);
    },
    hue: function (color) {
        return new Dimension(toHSL(color).h);
    },
    saturation: function (color) {
        return new Dimension(toHSL(color).s * 100, '%');
    },
    lightness: function (color) {
        return new Dimension(toHSL(color).l * 100, '%');
    },
    hsvhue: function (color) {
        return new Dimension(toHSV(color).h);
    },
    hsvsaturation: function (color) {
        return new Dimension(toHSV(color).s * 100, '%');
    },
    hsvvalue: function (color) {
        return new Dimension(toHSV(color).v * 100, '%');
    },
    red: function (color) {
        return new Dimension(color.rgb[0]);
    },
    green: function (color) {
        return new Dimension(color.rgb[1]);
    },
    blue: function (color) {
        return new Dimension(color.rgb[2]);
    },
    alpha: function (color) {
        return new Dimension(toHSL(color).a);
    },
    luma: function (color) {
        return new Dimension(color.luma() * color.alpha * 100, '%');
    },
    luminance: function (color) {
        var luminance = (0.2126 * color.rgb[0] / 255) +
            (0.7152 * color.rgb[1] / 255) +
            (0.0722 * color.rgb[2] / 255);
        return new Dimension(luminance * color.alpha * 100, '%');
    },
    saturate: function (color, amount, method) {
        // filter: saturate(3.2);
        // should be kept as is, so check for color
        if (!color.rgb) {
            return null;
        }
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.s += hsl.s * amount.value / 100;
        }
        else {
            hsl.s += amount.value / 100;
        }
        hsl.s = clamp$1(hsl.s);
        return hsla(color, hsl);
    },
    desaturate: function (color, amount, method) {
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.s -= hsl.s * amount.value / 100;
        }
        else {
            hsl.s -= amount.value / 100;
        }
        hsl.s = clamp$1(hsl.s);
        return hsla(color, hsl);
    },
    lighten: function (color, amount, method) {
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.l += hsl.l * amount.value / 100;
        }
        else {
            hsl.l += amount.value / 100;
        }
        hsl.l = clamp$1(hsl.l);
        return hsla(color, hsl);
    },
    darken: function (color, amount, method) {
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.l -= hsl.l * amount.value / 100;
        }
        else {
            hsl.l -= amount.value / 100;
        }
        hsl.l = clamp$1(hsl.l);
        return hsla(color, hsl);
    },
    fadein: function (color, amount, method) {
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.a += hsl.a * amount.value / 100;
        }
        else {
            hsl.a += amount.value / 100;
        }
        hsl.a = clamp$1(hsl.a);
        return hsla(color, hsl);
    },
    fadeout: function (color, amount, method) {
        var hsl = toHSL(color);
        if (typeof method !== 'undefined' && method.value === 'relative') {
            hsl.a -= hsl.a * amount.value / 100;
        }
        else {
            hsl.a -= amount.value / 100;
        }
        hsl.a = clamp$1(hsl.a);
        return hsla(color, hsl);
    },
    fade: function (color, amount) {
        var hsl = toHSL(color);
        hsl.a = amount.value / 100;
        hsl.a = clamp$1(hsl.a);
        return hsla(color, hsl);
    },
    spin: function (color, amount) {
        var hsl = toHSL(color);
        var hue = (hsl.h + amount.value) % 360;
        hsl.h = hue < 0 ? 360 + hue : hue;
        return hsla(color, hsl);
    },
    //
    // Copyright (c) 2006-2009 Hampton Catlin, Natalie Weizenbaum, and Chris Eppstein
    // http://sass-lang.com
    //
    mix: function (color1, color2, weight) {
        if (!weight) {
            weight = new Dimension(50);
        }
        var p = weight.value / 100.0;
        var w = p * 2 - 1;
        var a = toHSL(color1).a - toHSL(color2).a;
        var w1 = (((w * a == -1) ? w : (w + a) / (1 + w * a)) + 1) / 2.0;
        var w2 = 1 - w1;
        var rgb = [color1.rgb[0] * w1 + color2.rgb[0] * w2,
            color1.rgb[1] * w1 + color2.rgb[1] * w2,
            color1.rgb[2] * w1 + color2.rgb[2] * w2];
        var alpha = color1.alpha * p + color2.alpha * (1 - p);
        return new Color(rgb, alpha);
    },
    greyscale: function (color) {
        return colorFunctions.desaturate(color, new Dimension(100));
    },
    contrast: function (color, dark, light, threshold) {
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
        }
        // Figure out which is actually light and dark:
        if (dark.luma() > light.luma()) {
            var t = light;
            light = dark;
            dark = t;
        }
        if (typeof threshold === 'undefined') {
            threshold = 0.43;
        }
        else {
            threshold = number(threshold);
        }
        if (color.luma() < threshold) {
            return light;
        }
        else {
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
    argb: function (color) {
        return new Anonymous(color.toARGB());
    },
    color: function (c) {
        if ((c instanceof Quoted) &&
            (/^#([A-Fa-f0-9]{8}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{3,4})$/i.test(c.value))) {
            var val = c.value.slice(1);
            return new Color(val, undefined, "#" + val);
        }
        if ((c instanceof Color) || (c = Color.fromKeyword(c.value))) {
            c.value = undefined;
            return c;
        }
        throw {
            type: 'Argument',
            message: 'argument must be a color keyword or 3|4|6|8 digit hex e.g. #FFF'
        };
    },
    tint: function (color, amount) {
        return colorFunctions.mix(colorFunctions.rgb(255, 255, 255), color, amount);
    },
    shade: function (color, amount) {
        return colorFunctions.mix(colorFunctions.rgb(0, 0, 0), color, amount);
    }
};
var color = colorFunctions;

// Color Blending
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
    for (var i_1 = 0; i_1 < 3; i_1++) {
        cb = color1.rgb[i_1] / 255;
        cs = color2.rgb[i_1] / 255;
        cr = mode(cb, cs);
        if (ar) {
            cr = (as * cs + ab * (cb -
                as * (cb + cs - cr))) / ar;
        }
        r[i_1] = cr * 255;
    }
    return new Color(r, ar);
}
var colorBlendModeFunctions = {
    multiply: function (cb, cs) {
        return cb * cs;
    },
    screen: function (cb, cs) {
        return cb + cs - cb * cs;
    },
    overlay: function (cb, cs) {
        cb *= 2;
        return (cb <= 1) ?
            colorBlendModeFunctions.multiply(cb, cs) :
            colorBlendModeFunctions.screen(cb - 1, cs);
    },
    softlight: function (cb, cs) {
        var d = 1;
        var e = cb;
        if (cs > 0.5) {
            e = 1;
            d = (cb > 0.25) ? Math.sqrt(cb)
                : ((16 * cb - 12) * cb + 4) * cb;
        }
        return cb - (1 - 2 * cs) * e * (d - cb);
    },
    hardlight: function (cb, cs) {
        return colorBlendModeFunctions.overlay(cs, cb);
    },
    difference: function (cb, cs) {
        return Math.abs(cb - cs);
    },
    exclusion: function (cb, cs) {
        return cb + cs - 2 * cb * cs;
    },
    // non-w3c functions:
    average: function (cb, cs) {
        return (cb + cs) / 2;
    },
    negation: function (cb, cs) {
        return 1 - Math.abs(cb + cs - 1);
    }
};
for (var f in colorBlendModeFunctions) {
    if (colorBlendModeFunctions.hasOwnProperty(f)) {
        colorBlend[f] = colorBlend.bind(null, colorBlendModeFunctions[f]);
    }
}

var dataUri = (function (environment) {
    var fallback = function (functionThis, node) { return new URL(node, functionThis.index, functionThis.currentFileInfo).eval(functionThis.context); };
    return { 'data-uri': function (mimetypeNode, filePathNode) {
            if (!filePathNode) {
                filePathNode = mimetypeNode;
                mimetypeNode = null;
            }
            var mimetype = mimetypeNode && mimetypeNode.value;
            var filePath = filePathNode.value;
            var currentFileInfo = this.currentFileInfo;
            var currentDirectory = currentFileInfo.rewriteUrls ?
                currentFileInfo.currentDirectory : currentFileInfo.entryPath;
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
            var useBase64 = false;
            // detect the mimetype if not given
            if (!mimetypeNode) {
                mimetype = environment.mimeLookup(filePath);
                if (mimetype === 'image/svg+xml') {
                    useBase64 = false;
                }
                else {
                    // use base 64 unless it's an ASCII or UTF-8 format
                    var charset = environment.charsetLookup(mimetype);
                    useBase64 = ['US-ASCII', 'UTF-8'].indexOf(charset) < 0;
                }
                if (useBase64) {
                    mimetype += ';base64';
                }
            }
            else {
                useBase64 = /;base64$/.test(mimetype);
            }
            var fileSync = fileManager.loadFileSync(filePath, currentDirectory, context, environment);
            if (!fileSync.contents) {
                logger.warn("Skipped data-uri embedding of " + filePath + " because file not found");
                return fallback(this, filePathNode || mimetypeNode);
            }
            var buf = fileSync.contents;
            if (useBase64 && !environment.encodeBase64) {
                return fallback(this, filePathNode);
            }
            buf = useBase64 ? environment.encodeBase64(buf) : encodeURIComponent(buf);
            var uri = "data:" + mimetype + "," + buf + fragment;
            return new URL(new Quoted("\"" + uri + "\"", uri, false, this.index, this.currentFileInfo), this.index, this.currentFileInfo);
        } };
});

var getItemsFromNode = function (node) {
    // handle non-array values as an array of length 1
    // return 'undefined' if index is invalid
    var items = Array.isArray(node.value) ?
        node.value : Array(node);
    return items;
};
var list = {
    _SELF: function (n) {
        return n;
    },
    extract: function (values, index) {
        // (1-based index)
        index = index.value - 1;
        return getItemsFromNode(values)[index];
    },
    length: function (values) {
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
    range: function (start, end, step) {
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
        }
        else {
            from = 1;
            to = start;
        }
        for (var i_1 = from; i_1 <= to.value; i_1 += stepValue) {
            list.push(new Dimension(i_1, to.unit));
        }
        return new Expression(list);
    },
    each: function (list, rs) {
        var rules = [];
        var newRules;
        var iterator;
        if (list.value && !(list instanceof Quoted)) {
            if (Array.isArray(list.value)) {
                iterator = list.value;
            }
            else {
                iterator = [list.value];
            }
        }
        else if (list.ruleset) {
            iterator = list.ruleset.rules;
        }
        else if (list.rules) {
            iterator = list.rules;
        }
        else if (Array.isArray(list)) {
            iterator = list;
        }
        else {
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
        }
        else {
            rs = rs.ruleset;
        }
        for (var i_2 = 0; i_2 < iterator.length; i_2++) {
            var key = void 0;
            var value = void 0;
            var item = iterator[i_2];
            if (item instanceof Declaration) {
                key = typeof item.name === 'string' ? item.name : item.name[0].value;
                value = item.value;
            }
            else {
                key = new Dimension(i_2 + 1);
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
                newRules.push(new Declaration(indexName, new Dimension(i_2 + 1), false, false, this.index, this.currentFileInfo));
            }
            if (keyName) {
                newRules.push(new Declaration(keyName, key, false, false, this.index, this.currentFileInfo));
            }
            rules.push(new Ruleset([new (Selector)([new Element("", '&')])], newRules, rs.strictImports, rs.visibilityInfo()));
        }
        return new Ruleset([new (Selector)([new Element("", '&')])], rules, rs.strictImports, rs.visibilityInfo()).eval(this.context);
    }
};

var MathHelper = function (fn, unit, n) {
    if (!(n instanceof Dimension)) {
        throw { type: 'Argument', message: 'argument must be a number' };
    }
    if (unit == null) {
        unit = n.unit;
    }
    else {
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
    return MathHelper(function (num) { return num.toFixed(fraction); }, null, n);
};

var minMax = function (isMin, args) {
    args = Array.prototype.slice.call(args);
    switch (args.length) {
        case 0: throw { type: 'Argument', message: 'one or more arguments required' };
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
    var values = {};
    // value is the index into the order array.
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
                throw { type: 'Argument', message: 'incompatible types' };
            }
            values[unit] = order.length;
            order.push(current);
            continue;
        }
        referenceUnified = order[j].unit.toString() === '' && unitClone !== undefined ? new Dimension(order[j].value, unitClone).unify() : order[j].unify();
        if (isMin && currentUnified.value < referenceUnified.value ||
            !isMin && currentUnified.value > referenceUnified.value) {
            order[j] = current;
        }
    }
    if (order.length == 1) {
        return order[0];
    }
    args = order.map(function (a) { return a.toCSS(this.context); }).join(this.context.compress ? ',' : ', ');
    return new Anonymous((isMin ? 'min' : 'max') + "(" + args + ")");
};
var number$1 = {
    min: function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return minMax(true, args);
    },
    max: function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return minMax(false, args);
    },
    convert: function (val, unit) {
        return val.convertTo(unit.value);
    },
    pi: function () {
        return new Dimension(Math.PI);
    },
    mod: function (a, b) {
        return new Dimension(a.value % b.value, a.unit);
    },
    pow: function (x, y) {
        if (typeof x === 'number' && typeof y === 'number') {
            x = new Dimension(x);
            y = new Dimension(y);
        }
        else if (!(x instanceof Dimension) || !(y instanceof Dimension)) {
            throw { type: 'Argument', message: 'arguments must be numbers' };
        }
        return new Dimension(Math.pow(x.value, y.value), x.unit);
    },
    percentage: function (n) {
        var result = MathHelper(function (num) { return num * 100; }, '%', n);
        return result;
    }
};

var string = {
    e: function (str) {
        return new Quoted('"', str instanceof JavaScript ? str.evaluated : str.value, true);
    },
    escape: function (str) {
        return new Anonymous(encodeURI(str.value).replace(/=/g, '%3D').replace(/:/g, '%3A').replace(/#/g, '%23').replace(/;/g, '%3B')
            .replace(/\(/g, '%28').replace(/\)/g, '%29'));
    },
    replace: function (string, pattern, replacement, flags) {
        var result = string.value;
        replacement = (replacement.type === 'Quoted') ?
            replacement.value : replacement.toCSS();
        result = result.replace(new RegExp(pattern.value, flags ? flags.value : ''), replacement);
        return new Quoted(string.quote || '', result, string.escaped);
    },
    '%': function (string /* arg, arg, ... */) {
        var args = Array.prototype.slice.call(arguments, 1);
        var result = string.value;
        var _loop_1 = function (i_1) {
            /* jshint loopfunc:true */
            result = result.replace(/%[sda]/i, function (token) {
                var value = ((args[i_1].type === 'Quoted') &&
                    token.match(/s/i)) ? args[i_1].value : args[i_1].toCSS();
                return token.match(/[A-Z]$/) ? encodeURIComponent(value) : value;
            });
        };
        for (var i_1 = 0; i_1 < args.length; i_1++) {
            _loop_1(i_1);
        }
        result = result.replace(/%%/g, '%');
        return new Quoted(string.quote || '', result, string.escaped);
    }
};

var svg = (function (environment) {
    return { 'svg-gradient': function (direction) {
            var stops;
            var gradientDirectionSvg;
            var gradientType = 'linear';
            var rectangleDimension = 'x="0" y="0" width="1" height="1"';
            var renderEnv = { compress: false };
            var returner;
            var directionValue = direction.toCSS(renderEnv);
            var i;
            var color;
            var position;
            var positionValue;
            var alpha;
            function throwArgumentDescriptor() {
                throw { type: 'Argument',
                    message: 'svg-gradient expects direction, start_color [start_position], [color position,]...,' +
                        ' end_color [end_position] or direction, color list' };
            }
            if (arguments.length == 2) {
                if (arguments[1].value.length < 2) {
                    throwArgumentDescriptor();
                }
                stops = arguments[1].value;
            }
            else if (arguments.length < 3) {
                throwArgumentDescriptor();
            }
            else {
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
                    throw { type: 'Argument', message: 'svg-gradient direction must be \'to bottom\', \'to right\',' +
                            ' \'to bottom right\', \'to top right\' or \'ellipse at center\'' };
            }
            returner = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><" + gradientType + "Gradient id=\"g\" " + gradientDirectionSvg + ">";
            for (i = 0; i < stops.length; i += 1) {
                if (stops[i] instanceof Expression) {
                    color = stops[i].value[0];
                    position = stops[i].value[1];
                }
                else {
                    color = stops[i];
                    position = undefined;
                }
                if (!(color instanceof Color) || (!((i === 0 || i + 1 === stops.length) && position === undefined) && !(position instanceof Dimension))) {
                    throwArgumentDescriptor();
                }
                positionValue = position ? position.toCSS(renderEnv) : i === 0 ? '0%' : '100%';
                alpha = color.alpha;
                returner += "<stop offset=\"" + positionValue + "\" stop-color=\"" + color.toRGB() + "\"" + (alpha < 1 ? " stop-opacity=\"" + alpha + "\"" : '') + "/>";
            }
            returner += "</" + gradientType + "Gradient><rect " + rectangleDimension + " fill=\"url(#g)\" /></svg>";
            returner = encodeURIComponent(returner);
            returner = "data:image/svg+xml," + returner;
            return new URL(new Quoted("'" + returner + "'", returner, false, this.index, this.currentFileInfo), this.index, this.currentFileInfo);
        } };
});

var isa = function (n, Type) { return (n instanceof Type) ? Keyword.True : Keyword.False; };
var isunit = function (n, unit) {
    if (unit === undefined) {
        throw { type: 'Argument', message: 'missing the required second argument to isunit.' };
    }
    unit = typeof unit.value === 'string' ? unit.value : unit;
    if (typeof unit !== 'string') {
        throw { type: 'Argument', message: 'Second argument to isunit should be a unit or a string.' };
    }
    return (n instanceof Dimension) && n.unit.is(unit) ? Keyword.True : Keyword.False;
};
var types = {
    isruleset: function (n) {
        return isa(n, DetachedRuleset);
    },
    iscolor: function (n) {
        return isa(n, Color);
    },
    isnumber: function (n) {
        return isa(n, Dimension);
    },
    isstring: function (n) {
        return isa(n, Quoted);
    },
    iskeyword: function (n) {
        return isa(n, Keyword);
    },
    isurl: function (n) {
        return isa(n, URL);
    },
    ispixel: function (n) {
        return isunit(n, 'px');
    },
    ispercentage: function (n) {
        return isunit(n, '%');
    },
    isem: function (n) {
        return isunit(n, 'em');
    },
    isunit: isunit,
    unit: function (val, unit) {
        if (!(val instanceof Dimension)) {
            throw { type: 'Argument',
                message: "the first argument to unit must be a number" + (val instanceof Operation ? '. Have you forgotten parenthesis?' : '') };
        }
        if (unit) {
            if (unit instanceof Keyword) {
                unit = unit.value;
            }
            else {
                unit = unit.toCSS();
            }
        }
        else {
            unit = '';
        }
        return new Dimension(val.value, unit);
    },
    'get-unit': function (n) {
        return new Anonymous(n.unit);
    }
};

var Functions = (function (environment) {
    var functions = { functionRegistry: functionRegistry, functionCaller: functionCaller };
    // register functions
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
    var SourceMapOutput = /** @class */ (function () {
        function SourceMapOutput(options) {
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
            }
            else {
                this._sourceMapRootpath = '';
            }
            this._outputSourceFiles = options.outputSourceFiles;
            this._sourceMapGeneratorConstructor = environment.getSourceMapGenerator();
            this._lineNumber = 0;
            this._column = 0;
        }
        SourceMapOutput.prototype.removeBasepath = function (path) {
            if (this._sourceMapBasepath && path.indexOf(this._sourceMapBasepath) === 0) {
                path = path.substring(this._sourceMapBasepath.length);
                if (path.charAt(0) === '\\' || path.charAt(0) === '/') {
                    path = path.substring(1);
                }
            }
            return path;
        };
        SourceMapOutput.prototype.normalizeFilename = function (filename) {
            filename = filename.replace(/\\/g, '/');
            filename = this.removeBasepath(filename);
            return (this._sourceMapRootpath || '') + filename;
        };
        SourceMapOutput.prototype.add = function (chunk, fileInfo, index, mapLines) {
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
                var inputSource = this._contentsMap[fileInfo.filename];
                // remove vars/banner added to the top of the file
                if (this._contentsIgnoredCharsMap[fileInfo.filename]) {
                    // adjust the index
                    index -= this._contentsIgnoredCharsMap[fileInfo.filename];
                    if (index < 0) {
                        index = 0;
                    }
                    // adjust the source
                    inputSource = inputSource.slice(this._contentsIgnoredCharsMap[fileInfo.filename]);
                }
                // ignore empty content
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
                    this._sourceMapGenerator.addMapping({ generated: { line: this._lineNumber + 1, column: this._column },
                        original: { line: sourceLines.length, column: sourceColumns.length },
                        source: this.normalizeFilename(fileInfo.filename) });
                }
                else {
                    for (i = 0; i < lines.length; i++) {
                        this._sourceMapGenerator.addMapping({ generated: { line: this._lineNumber + i + 1, column: i === 0 ? this._column : 0 },
                            original: { line: sourceLines.length + i, column: i === 0 ? sourceColumns.length : 0 },
                            source: this.normalizeFilename(fileInfo.filename) });
                    }
                }
            }
            if (lines.length === 1) {
                this._column += columns.length;
            }
            else {
                this._lineNumber += lines.length - 1;
                this._column = columns.length;
            }
            this._css.push(chunk);
        };
        SourceMapOutput.prototype.isEmpty = function () {
            return this._css.length === 0;
        };
        SourceMapOutput.prototype.toCSS = function (context) {
            this._sourceMapGenerator = new this._sourceMapGeneratorConstructor({ file: this._outputFilename, sourceRoot: null });
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
                var sourceMapURL = void 0;
                var sourceMapContent = JSON.stringify(this._sourceMapGenerator.toJSON());
                if (this.sourceMapURL) {
                    sourceMapURL = this.sourceMapURL;
                }
                else if (this._sourceMapFilename) {
                    sourceMapURL = this._sourceMapFilename;
                }
                this.sourceMapURL = sourceMapURL;
                this.sourceMap = sourceMapContent;
            }
            return this._css.join('');
        };
        return SourceMapOutput;
    }());
    return SourceMapOutput;
});

var sourceMapBuilder = (function (SourceMapOutput, environment) {
    var SourceMapBuilder = /** @class */ (function () {
        function SourceMapBuilder(options) {
            this.options = options;
        }
        SourceMapBuilder.prototype.toCSS = function (rootNode, options, imports) {
            var sourceMapOutput = new SourceMapOutput({
                contentsIgnoredCharsMap: imports.contentsIgnoredChars,
                rootNode: rootNode,
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
        };
        SourceMapBuilder.prototype.getCSSAppendage = function () {
            var sourceMapURL = this.sourceMapURL;
            if (this.options.sourceMapFileInline) {
                if (this.sourceMap === undefined) {
                    return '';
                }
                sourceMapURL = "data:application/json;base64," + environment.encodeBase64(this.sourceMap);
            }
            if (sourceMapURL) {
                return "/*# sourceMappingURL=" + sourceMapURL + " */";
            }
            return '';
        };
        SourceMapBuilder.prototype.getExternalSourceMap = function () {
            return this.sourceMap;
        };
        SourceMapBuilder.prototype.setExternalSourceMap = function (sourceMap) {
            this.sourceMap = sourceMap;
        };
        SourceMapBuilder.prototype.isInline = function () {
            return this.options.sourceMapFileInline;
        };
        SourceMapBuilder.prototype.getSourceMapURL = function () {
            return this.sourceMapURL;
        };
        SourceMapBuilder.prototype.getOutputFilename = function () {
            return this.options.sourceMapOutputFilename;
        };
        SourceMapBuilder.prototype.getInputFilename = function () {
            return this.sourceMapInputFilename;
        };
        return SourceMapBuilder;
    }());
    return SourceMapBuilder;
});

var transformTree = (function (root, options) {
    if (options === void 0) { options = {}; }
    var evaldRoot;
    var variables = options.variables;
    var evalEnv = new contexts.Eval(options);
    //
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
            return new tree.Declaration("@" + k, value, false, null, 0);
        });
        evalEnv.frames = [new tree.Ruleset(null, variables)];
    }
    var visitors$1 = [
        new visitors.JoinSelectorVisitor(),
        new visitors.MarkVisibleSelectorsVisitor(true),
        new visitors.ExtendVisitor(),
        new visitors.ToCSSVisitor({ compress: Boolean(options.compress) })
    ];
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
            while ((v = visitorIterator.get())) {
                if (v.isPreEvalVisitor) {
                    if (i === 0 || preEvalVisitors.indexOf(v) === -1) {
                        preEvalVisitors.push(v);
                        v.run(root);
                    }
                }
                else {
                    if (i === 0 || visitors$1.indexOf(v) === -1) {
                        if (v.isPreVisitor) {
                            visitors$1.unshift(v);
                        }
                        else {
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
    }
    // Run any remaining visitors added after eval pass
    if (options.pluginManager) {
        visitorIterator.first();
        while ((v = visitorIterator.get())) {
            if (visitors$1.indexOf(v) === -1 && preEvalVisitors.indexOf(v) === -1) {
                v.run(evaldRoot);
            }
        }
    }
    return evaldRoot;
});

var parseTree = (function (SourceMapBuilder) {
    var ParseTree = /** @class */ (function () {
        function ParseTree(root, imports) {
            this.root = root;
            this.imports = imports;
        }
        ParseTree.prototype.toCSS = function (options) {
            var evaldRoot;
            var result = {};
            var sourceMapBuilder;
            try {
                evaldRoot = transformTree(this.root, options);
            }
            catch (e) {
                throw new LessError(e, this.imports);
            }
            try {
                var compress = Boolean(options.compress);
                if (compress) {
                    logger.warn('The compress option has been deprecated. ' +
                        'We recommend you use a dedicated css minifier, for instance see less-plugin-clean-css.');
                }
                var toCSSOptions = {
                    compress: compress,
                    dumpLineNumbers: options.dumpLineNumbers,
                    strictUnits: Boolean(options.strictUnits),
                    numPrecision: 8
                };
                if (options.sourceMap) {
                    sourceMapBuilder = new SourceMapBuilder(options.sourceMap);
                    result.css = sourceMapBuilder.toCSS(evaldRoot, toCSSOptions, this.imports);
                }
                else {
                    result.css = evaldRoot.toCSS(toCSSOptions);
                }
            }
            catch (e) {
                throw new LessError(e, this.imports);
            }
            if (options.pluginManager) {
                var postProcessors = options.pluginManager.getPostProcessors();
                for (var i_1 = 0; i_1 < postProcessors.length; i_1++) {
                    result.css = postProcessors[i_1].process(result.css, { sourceMap: sourceMapBuilder, options: options, imports: this.imports });
                }
            }
            if (options.sourceMap) {
                result.map = sourceMapBuilder.getExternalSourceMap();
            }
            result.imports = [];
            for (var file_1 in this.imports.files) {
                if (this.imports.files.hasOwnProperty(file_1) && file_1 !== this.imports.rootFilename) {
                    result.imports.push(file_1);
                }
            }
            return result;
        };
        return ParseTree;
    }());
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
    var ImportManager = /** @class */ (function () {
        function ImportManager(less, context, rootFileInfo) {
            this.less = less;
            this.rootFilename = rootFileInfo.filename;
            this.paths = context.paths || []; // Search paths, when importing
            this.contents = {}; // map - filename to contents of all the files
            this.contentsIgnoredChars = {}; // map - filename to lines at the beginning of each file to ignore
            this.mime = context.mime;
            this.error = null;
            this.context = context;
            // Deprecated? Unused outside of here, could be useful.
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
        ImportManager.prototype.push = function (path, tryAppendExtension, currentFileInfo, importOptions, callback) {
            var importManager = this;
            var pluginLoader = this.context.pluginManager.Loader;
            this.queue.push(path);
            var fileParsedFunc = function (e, root, fullPath) {
                importManager.queue.splice(importManager.queue.indexOf(path), 1); // Remove the path from the queue
                var importedEqualsRoot = fullPath === importManager.rootFilename;
                if (importOptions.optional && e) {
                    callback(null, { rules: [] }, false, null);
                    logger.info("The file " + fullPath + " was skipped because it was not found and the import was marked optional.");
                }
                else {
                    // Inline imports aren't cached here.
                    // If we start to cache them, please make sure they won't conflict with non-inline imports of the
                    // same name as they used to do before this comment and the condition below have been added.
                    if (!importManager.files[fullPath] && !importOptions.inline) {
                        importManager.files[fullPath] = { root: root, options: importOptions };
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
                fileParsedFunc({ message: "Could not find a file-manager for " + path });
                return;
            }
            var loadFileCallback = function (loadedFile) {
                var plugin;
                var resolvedFilename = loadedFile.filename;
                var contents = loadedFile.contents.replace(/^\uFEFF/, '');
                // Pass on an updated rootpath if path of imported file is relative and file
                // is in a (sub|sup) directory
                //
                // Examples:
                // - If path of imported file is 'module/nav/nav.less' and rootpath is 'less/',
                //   then rootpath should become 'less/module/nav/'
                // - If path of imported file is '../mixins.less' and rootpath is 'less/',
                //   then rootpath should become 'less/../'
                newFileInfo.currentDirectory = fileManager.getPath(resolvedFilename);
                if (newFileInfo.rewriteUrls) {
                    newFileInfo.rootpath = fileManager.join((importManager.context.rootpath || ''), fileManager.pathDiff(newFileInfo.currentDirectory, newFileInfo.entryPath));
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
                    }
                    else {
                        fileParsedFunc(null, plugin, resolvedFilename);
                    }
                }
                else if (importOptions.inline) {
                    fileParsedFunc(null, contents, resolvedFilename);
                }
                else {
                    // import (multiple) parse trees apparently get altered and can't be cached.
                    // TODO: investigate why this is
                    if (importManager.files[resolvedFilename]
                        && !importManager.files[resolvedFilename].options.multiple
                        && !importOptions.multiple) {
                        fileParsedFunc(null, importManager.files[resolvedFilename].root, resolvedFilename);
                    }
                    else {
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
            }
            else {
                promise = fileManager.loadFile(path, currentFileInfo.currentDirectory, context, environment, function (err, loadedFile) {
                    if (err) {
                        fileParsedFunc(err);
                    }
                    else {
                        loadFileCallback(loadedFile);
                    }
                });
            }
            if (promise) {
                promise.then(loadFileCallback, fileParsedFunc);
            }
        };
        return ImportManager;
    }());
    return ImportManager;
});

var Render = (function (environment, ParseTree, ImportManager) {
    var render = function (input, options, callback) {
        if (typeof options === 'function') {
            callback = options;
            options = copyOptions(this.options, {});
        }
        else {
            options = copyOptions(this.options, options || {});
        }
        if (!callback) {
            var self_1 = this;
            return new Promise(function (resolve, reject) {
                render.call(self_1, input, options, function (err, output) {
                    if (err) {
                        reject(err);
                    }
                    else {
                        resolve(output);
                    }
                });
            });
        }
        else {
            this.parse(input, options, function (err, root, imports, options) {
                if (err) {
                    return callback(err);
                }
                var result;
                try {
                    var parseTree = new ParseTree(root, imports);
                    result = parseTree.toCSS(options);
                }
                catch (err) {
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
var PluginManager = /** @class */ (function () {
    function PluginManager(less) {
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
    PluginManager.prototype.addPlugins = function (plugins) {
        if (plugins) {
            for (var i_1 = 0; i_1 < plugins.length; i_1++) {
                this.addPlugin(plugins[i_1]);
            }
        }
    };
    /**
     *
     * @param plugin
     * @param {String} filename
     */
    PluginManager.prototype.addPlugin = function (plugin, filename, functionRegistry) {
        this.installedPlugins.push(plugin);
        if (filename) {
            this.pluginCache[filename] = plugin;
        }
        if (plugin.install) {
            plugin.install(this.less, this, functionRegistry || this.less.functions.functionRegistry);
        }
    };
    /**
     *
     * @param filename
     */
    PluginManager.prototype.get = function (filename) {
        return this.pluginCache[filename];
    };
    /**
     * Adds a visitor. The visitor object has options on itself to determine
     * when it should run.
     * @param visitor
     */
    PluginManager.prototype.addVisitor = function (visitor) {
        this.visitors.push(visitor);
    };
    /**
     * Adds a pre processor object
     * @param {object} preProcessor
     * @param {number} priority - guidelines 1 = before import, 1000 = import, 2000 = after import
     */
    PluginManager.prototype.addPreProcessor = function (preProcessor, priority) {
        var indexToInsertAt;
        for (indexToInsertAt = 0; indexToInsertAt < this.preProcessors.length; indexToInsertAt++) {
            if (this.preProcessors[indexToInsertAt].priority >= priority) {
                break;
            }
        }
        this.preProcessors.splice(indexToInsertAt, 0, { preProcessor: preProcessor, priority: priority });
    };
    /**
     * Adds a post processor object
     * @param {object} postProcessor
     * @param {number} priority - guidelines 1 = before compression, 1000 = compression, 2000 = after compression
     */
    PluginManager.prototype.addPostProcessor = function (postProcessor, priority) {
        var indexToInsertAt;
        for (indexToInsertAt = 0; indexToInsertAt < this.postProcessors.length; indexToInsertAt++) {
            if (this.postProcessors[indexToInsertAt].priority >= priority) {
                break;
            }
        }
        this.postProcessors.splice(indexToInsertAt, 0, { postProcessor: postProcessor, priority: priority });
    };
    /**
     *
     * @param manager
     */
    PluginManager.prototype.addFileManager = function (manager) {
        this.fileManagers.push(manager);
    };
    /**
     *
     * @returns {Array}
     * @private
     */
    PluginManager.prototype.getPreProcessors = function () {
        var preProcessors = [];
        for (var i_2 = 0; i_2 < this.preProcessors.length; i_2++) {
            preProcessors.push(this.preProcessors[i_2].preProcessor);
        }
        return preProcessors;
    };
    /**
     *
     * @returns {Array}
     * @private
     */
    PluginManager.prototype.getPostProcessors = function () {
        var postProcessors = [];
        for (var i_3 = 0; i_3 < this.postProcessors.length; i_3++) {
            postProcessors.push(this.postProcessors[i_3].postProcessor);
        }
        return postProcessors;
    };
    /**
     *
     * @returns {Array}
     * @private
     */
    PluginManager.prototype.getVisitors = function () {
        return this.visitors;
    };
    PluginManager.prototype.visitor = function () {
        var self = this;
        return {
            first: function () {
                self.iterator = -1;
                return self.visitors[self.iterator];
            },
            get: function () {
                self.iterator += 1;
                return self.visitors[self.iterator];
            }
        };
    };
    /**
     *
     * @returns {Array}
     * @private
     */
    PluginManager.prototype.getFileManagers = function () {
        return this.fileManagers;
    };
    return PluginManager;
}());
var pm;
function PluginManagerFactory(less, newFactory) {
    if (newFactory || !pm) {
        pm = new PluginManager(less);
    }
    return pm;
}

var Parse = (function (environment, ParseTree, ImportManager) {
    var parse = function (input, options, callback) {
        if (typeof options === 'function') {
            callback = options;
            options = copyOptions(this.options, {});
        }
        else {
            options = copyOptions(this.options, options || {});
        }
        if (!callback) {
            var self_1 = this;
            return new Promise(function (resolve, reject) {
                parse.call(self_1, input, options, function (err, output) {
                    if (err) {
                        reject(err);
                    }
                    else {
                        resolve(output);
                    }
                });
            });
        }
        else {
            var context_1;
            var rootFileInfo = void 0;
            var pluginManager_1 = new PluginManagerFactory(this, !options.reUsePluginManager);
            options.pluginManager = pluginManager_1;
            context_1 = new contexts.Parse(options);
            if (options.rootFileInfo) {
                rootFileInfo = options.rootFileInfo;
            }
            else {
                var filename = options.filename || 'input';
                var entryPath = filename.replace(/[^\/\\]*$/, '');
                rootFileInfo = {
                    filename: filename,
                    rewriteUrls: context_1.rewriteUrls,
                    rootpath: context_1.rootpath || '',
                    currentDirectory: entryPath,
                    entryPath: entryPath,
                    rootFilename: filename
                };
                // add in a missing trailing slash
                if (rootFileInfo.rootpath && rootFileInfo.rootpath.slice(-1) !== '/') {
                    rootFileInfo.rootpath += '/';
                }
            }
            var imports_1 = new ImportManager(this, context_1, rootFileInfo);
            this.importManager = imports_1;
            // TODO: allow the plugins to be just a list of paths or names
            // Do an async plugin queue like lessc
            if (options.plugins) {
                options.plugins.forEach(function (plugin) {
                    var evalResult;
                    var contents;
                    if (plugin.fileContent) {
                        contents = plugin.fileContent.replace(/^\uFEFF/, '');
                        evalResult = pluginManager_1.Loader.evalPlugin(contents, context_1, imports_1, plugin.options, plugin.filename);
                        if (evalResult instanceof LessError) {
                            return callback(evalResult);
                        }
                    }
                    else {
                        pluginManager_1.addPlugin(plugin);
                    }
                });
            }
            new Parser(context_1, imports_1, rootFileInfo)
                .parse(input, function (e, root) {
                if (e) {
                    return callback(e);
                }
                callback(null, root, imports_1, options);
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
        version: [3, 11, 1],
        data: data,
        tree: tree,
        Environment: environment$1,
        AbstractFileManager: AbstractFileManager,
        AbstractPluginLoader: AbstractPluginLoader,
        environment: environment,
        visitors: visitors,
        Parser: Parser,
        functions: functions,
        contexts: contexts,
        SourceMapOutput: SourceMapOutput,
        SourceMapBuilder: SourceMapBuilder,
        ParseTree: ParseTree,
        ImportManager: ImportManager,
        render: render,
        parse: parse,
        LessError: LessError,
        transformTree: transformTree,
        utils: utils,
        PluginManager: PluginManagerFactory,
        logger: logger
    };
    // Create a public API
    var ctor = function (t) { return function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return new (t.bind.apply(t, tslib.__spreadArrays([void 0], args)))();
    }; };
    var t;
    var api = Object.create(initial);
    for (var n in initial.tree) {
        /* eslint guard-for-in: 0 */
        t = initial.tree[n];
        if (typeof t === 'function') {
            api[n.toLowerCase()] = ctor(t);
        }
        else {
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
        stylize: function (str, style) {
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
            return "\u001B[" + styles[style][0] + "m" + str + "\u001B[" + styles[style][1] + "m";
        },
        // Print command line options
        printUsage: function () {
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
    };
    // Exports helper functions
    for (var h in lessc_helper) {
        if (lessc_helper.hasOwnProperty(h)) {
            exports[h] = lessc_helper[h];
        }
    }
});

/**
 * Node Plugin Loader
 */
var PluginLoader = /** @class */ (function (_super) {
    tslib.__extends(PluginLoader, _super);
    function PluginLoader(less) {
        var _this = _super.call(this) || this;
        _this.less = less;
        _this.require = function (prefix) {
            prefix = path.dirname(prefix);
            return function (id) {
                var str = id.substr(0, 2);
                if (str === '..' || str === './') {
                    return require(path.join(prefix, id));
                }
                else {
                    return require(id);
                }
            };
        };
        return _this;
    }
    PluginLoader.prototype.loadPlugin = function (filename, basePath, context, environment, fileManager) {
        var prefix = filename.slice(0, 1);
        var explicit = prefix === '.' || prefix === '/' || filename.slice(-3).toLowerCase() === '.js';
        if (!explicit) {
            context.prefixes = ['less-plugin-', ''];
        }
        return new Promise(function (fulfill, reject) {
            fileManager.loadFile(filename, basePath, context, environment).then(function (data) {
                try {
                    fulfill(data);
                }
                catch (e) {
                    console.log(e);
                    reject(e);
                }
            }).catch(function (err) {
                reject(err);
            });
        });
    };
    return PluginLoader;
}(AbstractPluginLoader));

// Export a new default each time
var defaultOptions = (function () { return ({
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
}); });

var imageSize = (function (environment) {
    function imageSize(functionContext, filePathNode) {
        var filePath = filePathNode.value;
        var currentFileInfo = functionContext.currentFileInfo;
        var currentDirectory = currentFileInfo.rewriteUrls ?
            currentFileInfo.currentDirectory : currentFileInfo.entryPath;
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
                message: "Can not set up FileManager for " + filePathNode
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
        'image-size': function (filePathNode) {
            var size = imageSize(this, filePathNode);
            return new Expression([
                new Dimension(size.width, 'px'),
                new Dimension(size.height, 'px')
            ]);
        },
        'image-width': function (filePathNode) {
            var size = imageSize(this, filePathNode);
            return new Dimension(size.width, 'px');
        },
        'image-height': function (filePathNode) {
            var size = imageSize(this, filePathNode);
            return new Dimension(size.height, 'px');
        }
    };
    functionRegistry.addMultiple(imageFunctions);
});

var less = createFromEnvironment(environment, [new FileManager(), new UrlFileManager()]);
// allow people to create less with their own environment
less.createFromEnvironment = createFromEnvironment;
less.lesscHelper = lesscHelper;
less.PluginLoader = PluginLoader;
less.fs = fs$1;
less.FileManager = FileManager;
less.UrlFileManager = UrlFileManager;
// Set up options
less.options = defaultOptions();
// provide image-size functionality
imageSize(less.environment);

module.exports = less;
