"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _micromatch = require("micromatch");

var _linter = _interopRequireDefault(require("./linter"));

var _utils = require("./utils");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class LintDirtyModulesPlugin {
  constructor(lint, compiler, options) {
    this.lint = lint;
    this.compiler = compiler;
    this.options = options;
    this.startTime = Date.now();
    this.prevTimestamps = {};
    this.isFirstRun = true;
  }

  apply(compilation, callback) {
    const fileTimestamps = compilation.fileTimestamps || new Map();

    if (this.isFirstRun) {
      this.isFirstRun = false;
      this.prevTimestamps = fileTimestamps;
      callback();
      return;
    }

    const dirtyOptions = { ...this.options
    };
    const glob = (0, _utils.replaceBackslashes)(dirtyOptions.files.join('|'));
    const changedFiles = this.getChangedFiles(fileTimestamps, glob);
    this.prevTimestamps = fileTimestamps;

    if (changedFiles.length) {
      dirtyOptions.files = changedFiles;
      (0, _linter.default)(this.lint, dirtyOptions, this.compiler, callback);
    } else {
      callback();
    }
  }

  getChangedFiles(fileTimestamps, glob) {
    const getTimestamps = fileSystemInfoEntry => {
      return fileSystemInfoEntry && fileSystemInfoEntry.timestamp ? fileSystemInfoEntry.timestamp : fileSystemInfoEntry;
    };

    const hasFileChanged = (filename, fileSystemInfoEntry) => {
      const prevTimestamp = getTimestamps(this.prevTimestamps.get(filename));
      const timestamp = getTimestamps(fileSystemInfoEntry);
      return (prevTimestamp || this.startTime) < (timestamp || Infinity);
    };

    const changedFiles = [];

    for (const [filename, timestamp] of fileTimestamps.entries()) {
      if (hasFileChanged(filename, timestamp) && (0, _micromatch.isMatch)(filename, glob)) {
        changedFiles.push((0, _utils.replaceBackslashes)(filename));
      }
    }

    return changedFiles;
  }

}

exports.default = LintDirtyModulesPlugin;