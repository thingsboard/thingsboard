'use strict';

var _opn = require('opn');

var _opn2 = _interopRequireDefault(_opn);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var url = 'https://twitter.com/opencollect';
console.log("Opening", url);
(0, _opn2.default)(url);

process.exit(0);