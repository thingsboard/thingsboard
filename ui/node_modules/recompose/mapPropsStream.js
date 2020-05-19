'use strict';

exports.__esModule = true;
exports.mapPropsStreamWithConfig = undefined;

var _symbolObservable = require('symbol-observable');

var _symbolObservable2 = _interopRequireDefault(_symbolObservable);

var _createEagerFactory = require('./createEagerFactory');

var _createEagerFactory2 = _interopRequireDefault(_createEagerFactory);

var _createHelper = require('./createHelper');

var _createHelper2 = _interopRequireDefault(_createHelper);

var _componentFromStream = require('./componentFromStream');

var _setObservableConfig = require('./setObservableConfig');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var identity = function identity(t) {
  return t;
};
var componentFromStream = (0, _componentFromStream.componentFromStreamWithConfig)({
  fromESObservable: identity,
  toESObservable: identity
});

var mapPropsStreamWithConfig = exports.mapPropsStreamWithConfig = function mapPropsStreamWithConfig(config) {
  return function (transform) {
    return function (BaseComponent) {
      var factory = (0, _createEagerFactory2.default)(BaseComponent);
      var fromESObservable = config.fromESObservable,
          toESObservable = config.toESObservable;

      return componentFromStream(function (props$) {
        var _ref;

        return _ref = {
          subscribe: function subscribe(observer) {
            var subscription = toESObservable(transform(fromESObservable(props$))).subscribe({
              next: function next(childProps) {
                return observer.next(factory(childProps));
              }
            });
            return {
              unsubscribe: function unsubscribe() {
                return subscription.unsubscribe();
              }
            };
          }
        }, _ref[_symbolObservable2.default] = function () {
          return this;
        }, _ref;
      });
    };
  };
};

var mapPropsStream = mapPropsStreamWithConfig(_setObservableConfig.config);

exports.default = (0, _createHelper2.default)(mapPropsStream, 'mapPropsStream');