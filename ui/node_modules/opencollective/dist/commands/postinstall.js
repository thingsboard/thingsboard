'use strict';

var _utils = require('../lib/utils');

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _path = require('path');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * If we are not on a fancy TTY, just show a barebone message
 * without fancy emoji, centering, fetching data, etc.
 */
var defaultCmd = (0, _utils.isFancyEnvironment)() ? 'rich' : 'plain';

var argv = (0, _minimist2.default)(process.argv.slice(2), {
  alias: {
    help: 'h'
  }
});

var cmd = void 0;
if (argv.help) {
  cmd = 'help';
} else {
  cmd = argv.plain ? 'plain' : defaultCmd;
}

var bin = (0, _path.resolve)(__dirname, './postinstall-' + cmd + '.js');
require(bin, 'may-exclude');