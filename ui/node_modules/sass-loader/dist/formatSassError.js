"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _path = _interopRequireDefault(require("path"));

var _os = _interopRequireDefault(require("os"));

var _fs = _interopRequireDefault(require("fs"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// A typical sass error looks like this
// const SassError = {
//   message: "invalid property name",
//   column: 14,
//   line: 1,
//   file: "stdin",
//   status: 1
// };

/**
 * Enhances the sass error with additional information about what actually went wrong.
 *
 * @param {SassError} error
 * @param {string} resourcePath
 */
function formatSassError(error, resourcePath) {
  // Instruct webpack to hide the JS stack from the console
  // Usually you're only interested in the SASS stack in this case.
  // eslint-disable-next-line no-param-reassign
  error.hideStack = true; // The file property is missing in rare cases.
  // No improvement in the error is possible.

  if (!error.file) {
    return;
  }

  let msg = error.message;

  if (error.file === 'stdin') {
    // eslint-disable-next-line no-param-reassign
    error.file = resourcePath;
  } // node-sass returns UNIX-style paths
  // eslint-disable-next-line no-param-reassign


  error.file = _path.default.normalize(error.file); // The 'Current dir' hint of node-sass does not help us, we're providing
  // additional information by reading the err.file property

  msg = msg.replace(/\s*Current dir:\s*/, ''); // msg = msg.replace(/(\s*)(stdin)(\s*)/, `$1${err.file}$3`);
  // eslint-disable-next-line no-param-reassign

  error.message = `${getFileExcerptIfPossible(error) + msg.charAt(0).toUpperCase() + msg.slice(1) + _os.default.EOL}      in ${error.file} (line ${error.line}, column ${error.column})`;
}
/**
 * Tries to get an excerpt of the file where the error happened.
 * Uses err.line and err.column.
 *
 * Returns an empty string if the excerpt could not be retrieved.
 *
 * @param {SassError} error
 * @returns {string}
 */


function getFileExcerptIfPossible(error) {
  try {
    const content = _fs.default.readFileSync(error.file, 'utf8');

    return `${_os.default.EOL + content.split(/\r?\n/)[error.line - 1] + _os.default.EOL + new Array(error.column - 1).join(' ')}^${_os.default.EOL}      `;
  } catch (ignoreError) {
    // If anything goes wrong here, we don't want any errors to be reported to the user
    return '';
  }
}

var _default = formatSassError;
exports.default = _default;