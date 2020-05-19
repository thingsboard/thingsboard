'use strict';

exports.__esModule = true;

var _createHelper = require('./createHelper');

var _createHelper2 = _interopRequireDefault(_createHelper);

var _createEagerFactory = require('./createEagerFactory');

var _createEagerFactory2 = _interopRequireDefault(_createEagerFactory);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var renderComponent = function renderComponent(Component) {
  return function (_) {
    var factory = (0, _createEagerFactory2.default)(Component);
    var RenderComponent = function RenderComponent(props) {
      return factory(props);
    };
    if (process.env.NODE_ENV !== 'production') {
      /* eslint-disable global-require */
      var wrapDisplayName = require('./wrapDisplayName').default;
      /* eslint-enable global-require */
      RenderComponent.displayName = wrapDisplayName(Component, 'renderComponent');
    }
    return RenderComponent;
  };
};

exports.default = (0, _createHelper2.default)(renderComponent, 'renderComponent', false);