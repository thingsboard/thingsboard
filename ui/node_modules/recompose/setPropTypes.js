'use strict';

exports.__esModule = true;

var _setStatic = require('./setStatic');

var _setStatic2 = _interopRequireDefault(_setStatic);

var _createHelper = require('./createHelper');

var _createHelper2 = _interopRequireDefault(_createHelper);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var setPropTypes = function setPropTypes(propTypes) {
  return (0, _setStatic2.default)('propTypes', propTypes);
};

exports.default = (0, _createHelper2.default)(setPropTypes, 'setPropTypes', false);