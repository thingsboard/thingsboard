'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _objectWithoutProperties2 = require('babel-runtime/helpers/objectWithoutProperties');

var _objectWithoutProperties3 = _interopRequireDefault(_objectWithoutProperties2);

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

var _classCallCheck2 = require('babel-runtime/helpers/classCallCheck');

var _classCallCheck3 = _interopRequireDefault(_classCallCheck2);

var _createClass2 = require('babel-runtime/helpers/createClass');

var _createClass3 = _interopRequireDefault(_createClass2);

var _possibleConstructorReturn2 = require('babel-runtime/helpers/possibleConstructorReturn');

var _possibleConstructorReturn3 = _interopRequireDefault(_possibleConstructorReturn2);

var _inherits2 = require('babel-runtime/helpers/inherits');

var _inherits3 = _interopRequireDefault(_inherits2);

exports.genCSSMotionList = genCSSMotionList;

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _reactLifecyclesCompat = require('react-lifecycles-compat');

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _CSSMotion = require('./CSSMotion');

var _CSSMotion2 = _interopRequireDefault(_CSSMotion);

var _motion = require('./util/motion');

var _diff = require('./util/diff');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var MOTION_PROP_NAMES = Object.keys(_CSSMotion.MotionPropTypes);

function genCSSMotionList(transitionSupport) {
  var CSSMotion = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : _CSSMotion2['default'];

  var CSSMotionList = function (_React$Component) {
    (0, _inherits3['default'])(CSSMotionList, _React$Component);

    function CSSMotionList() {
      var _ref;

      var _temp, _this, _ret;

      (0, _classCallCheck3['default'])(this, CSSMotionList);

      for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }

      return _ret = (_temp = (_this = (0, _possibleConstructorReturn3['default'])(this, (_ref = CSSMotionList.__proto__ || Object.getPrototypeOf(CSSMotionList)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
        keyEntities: []
      }, _this.removeKey = function (removeKey) {
        _this.setState(function (_ref2) {
          var keyEntities = _ref2.keyEntities;
          return {
            keyEntities: keyEntities.map(function (entity) {
              if (entity.key !== removeKey) return entity;
              return (0, _extends3['default'])({}, entity, {
                status: _diff.STATUS_REMOVED
              });
            })
          };
        });
      }, _temp), (0, _possibleConstructorReturn3['default'])(_this, _ret);
    }

    (0, _createClass3['default'])(CSSMotionList, [{
      key: 'render',
      value: function render() {
        var _this2 = this;

        var keyEntities = this.state.keyEntities;
        var _props = this.props,
            component = _props.component,
            children = _props.children,
            restProps = (0, _objectWithoutProperties3['default'])(_props, ['component', 'children']);


        var Component = component || _react2['default'].Fragment;

        var motionProps = {};
        MOTION_PROP_NAMES.forEach(function (prop) {
          motionProps[prop] = restProps[prop];
          delete restProps[prop];
        });
        delete restProps.keys;

        return _react2['default'].createElement(
          Component,
          restProps,
          keyEntities.map(function (_ref3) {
            var status = _ref3.status,
                eventProps = (0, _objectWithoutProperties3['default'])(_ref3, ['status']);

            var visible = status === _diff.STATUS_ADD || status === _diff.STATUS_KEEP;
            return _react2['default'].createElement(
              CSSMotion,
              (0, _extends3['default'])({}, motionProps, {
                key: eventProps.key,
                visible: visible,
                eventProps: eventProps,
                onLeaveEnd: function onLeaveEnd() {
                  if (motionProps.onLeaveEnd) {
                    motionProps.onLeaveEnd.apply(motionProps, arguments);
                  }
                  _this2.removeKey(eventProps.key);
                }
              }),
              children
            );
          })
        );
      }
    }], [{
      key: 'getDerivedStateFromProps',
      value: function getDerivedStateFromProps(_ref4, _ref5) {
        var keys = _ref4.keys;
        var keyEntities = _ref5.keyEntities;

        var parsedKeyObjects = (0, _diff.parseKeys)(keys);

        // Always as keep when motion not support
        if (!transitionSupport) {
          return {
            keyEntities: parsedKeyObjects.map(function (obj) {
              return (0, _extends3['default'])({}, obj, { status: _diff.STATUS_KEEP });
            })
          };
        }

        var mixedKeyEntities = (0, _diff.diffKeys)(keyEntities, parsedKeyObjects);

        var keyEntitiesLen = keyEntities.length;
        return {
          keyEntities: mixedKeyEntities.filter(function (entity) {
            // IE 9 not support Array.prototype.find
            var prevEntity = null;
            for (var i = 0; i < keyEntitiesLen; i += 1) {
              var currentEntity = keyEntities[i];
              if (currentEntity.key === entity.key) {
                prevEntity = currentEntity;
                break;
              }
            }

            // Remove if already mark as removed
            if (prevEntity && prevEntity.status === _diff.STATUS_REMOVED && entity.status === _diff.STATUS_REMOVE) {
              return false;
            }
            return true;
          })
        };
      }
    }]);
    return CSSMotionList;
  }(_react2['default'].Component);

  CSSMotionList.propTypes = (0, _extends3['default'])({}, CSSMotion.propTypes, {
    component: _propTypes2['default'].oneOfType([_propTypes2['default'].string, _propTypes2['default'].bool]),
    keys: _propTypes2['default'].array
  });
  CSSMotionList.defaultProps = {
    component: 'div'
  };


  (0, _reactLifecyclesCompat.polyfill)(CSSMotionList);

  return CSSMotionList;
}

exports['default'] = genCSSMotionList(_motion.supportTransition);