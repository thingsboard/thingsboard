'use strict';

exports.__esModule = true;

var _createHelper = require('./createHelper');

var _createHelper2 = _interopRequireDefault(_createHelper);

var _createEagerFactory = require('./createEagerFactory');

var _createEagerFactory2 = _interopRequireDefault(_createEagerFactory);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var identity = function identity(Component) {
  return Component;
};

var branch = function branch(test, left) {
  var right = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : identity;
  return function (BaseComponent) {
    var leftFactory = void 0;
    var rightFactory = void 0;
    return function (props) {
      if (test(props)) {
        leftFactory = leftFactory || (0, _createEagerFactory2.default)(left(BaseComponent));
        return leftFactory(props);
      }
      rightFactory = rightFactory || (0, _createEagerFactory2.default)(right(BaseComponent));
      return rightFactory(props);
    };
  };
};

exports.default = (0, _createHelper2.default)(branch, 'branch');