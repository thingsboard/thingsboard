'use strict';

var _opn = require('opn');

var _opn2 = _interopRequireDefault(_opn);

var _utils = require('../lib/utils');

var _path = require('path');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var collective = (0, _utils.getCollective)();

var args = (0, _utils.getArgs)('collective', 'amount', 'frequency');

if (!collective) {
  var bin = (0, _path.resolve)(__dirname, './donate-help.js');
  require(bin, 'may-exclude');
  process.exit(0);
}

var url = collective.url + '/donate';
if (args.amount) {
  url += '/' + args.amount;
  if (args.frequency) {
    url += '/' + args.frequency;
  }
}
console.log("Opening", url);
(0, _opn2.default)(url);

process.exit(0);