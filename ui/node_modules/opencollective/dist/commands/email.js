'use strict';

var _opn = require('opn');

var _opn2 = _interopRequireDefault(_opn);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var url = 'mailto:support@opencollective.com?subject=opencollective-cli%20support';
console.log("Opening", "mailto:support@opencollective.com");
(0, _opn2.default)(url);

process.exit(0);