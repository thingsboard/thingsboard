'use strict';

var _opn = require('opn');

var _opn2 = _interopRequireDefault(_opn);

var _utils = require('../lib/utils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var collective = (0, _utils.getCollective)();

if (!collective) {
  console.log("Usage: opencollective open <collective>");
  console.log("E.g. opencollective open webpack");
  process.exit(0);
}

console.log("Opening", collective.url);
(0, _opn2.default)(collective.url);

process.exit(0);