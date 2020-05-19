'use strict';

var _chalk = require('chalk');

var _chalk2 = _interopRequireDefault(_chalk);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

console.log('\n' + _chalk2.default.bold('opencollective') + ' donate [collective] [amount] [frequency]\n\n  Open the donate page of [collective] (default: collective defined in ' + _chalk2.default.bold('package.json') + ')\n\n' + _chalk2.default.dim('Arguments:') + '\n\n  collective                      Slug of the collective (e.g. webpack)\n  amount                          Amount to give to the collective\n  frequency                       one-time, monthly, yearly (default: one-time)\n\n' + _chalk2.default.dim('Options:') + '\n\n  -h, --help                      Output usage information\n\n' + _chalk2.default.dim('Examples:') + '\n\n' + _chalk2.default.gray('–') + ' Opens the default donate page:\n\n    ' + _chalk2.default.cyan('$ opencollective donate webpack') + '\n\n' + _chalk2.default.gray('–') + ' Opens the donate page to donate $5 USD per month to Webpack:\n\n    ' + _chalk2.default.cyan('$ opencollective donate webpack 5 monthly') + '\n\n');