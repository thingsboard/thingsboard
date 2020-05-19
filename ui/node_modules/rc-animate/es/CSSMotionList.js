import _objectWithoutProperties from 'babel-runtime/helpers/objectWithoutProperties';
import _extends from 'babel-runtime/helpers/extends';
import _classCallCheck from 'babel-runtime/helpers/classCallCheck';
import _createClass from 'babel-runtime/helpers/createClass';
import _possibleConstructorReturn from 'babel-runtime/helpers/possibleConstructorReturn';
import _inherits from 'babel-runtime/helpers/inherits';
import React from 'react';
import { polyfill } from 'react-lifecycles-compat';
import PropTypes from 'prop-types';
import OriginCSSMotion, { MotionPropTypes } from './CSSMotion';
import { supportTransition } from './util/motion';
import { STATUS_ADD, STATUS_KEEP, STATUS_REMOVE, STATUS_REMOVED, diffKeys, parseKeys } from './util/diff';

var MOTION_PROP_NAMES = Object.keys(MotionPropTypes);

export function genCSSMotionList(transitionSupport) {
  var CSSMotion = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : OriginCSSMotion;

  var CSSMotionList = function (_React$Component) {
    _inherits(CSSMotionList, _React$Component);

    function CSSMotionList() {
      var _ref;

      var _temp, _this, _ret;

      _classCallCheck(this, CSSMotionList);

      for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }

      return _ret = (_temp = (_this = _possibleConstructorReturn(this, (_ref = CSSMotionList.__proto__ || Object.getPrototypeOf(CSSMotionList)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
        keyEntities: []
      }, _this.removeKey = function (removeKey) {
        _this.setState(function (_ref2) {
          var keyEntities = _ref2.keyEntities;
          return {
            keyEntities: keyEntities.map(function (entity) {
              if (entity.key !== removeKey) return entity;
              return _extends({}, entity, {
                status: STATUS_REMOVED
              });
            })
          };
        });
      }, _temp), _possibleConstructorReturn(_this, _ret);
    }

    _createClass(CSSMotionList, [{
      key: 'render',
      value: function render() {
        var _this2 = this;

        var keyEntities = this.state.keyEntities;

        var _props = this.props,
            component = _props.component,
            children = _props.children,
            restProps = _objectWithoutProperties(_props, ['component', 'children']);

        var Component = component || React.Fragment;

        var motionProps = {};
        MOTION_PROP_NAMES.forEach(function (prop) {
          motionProps[prop] = restProps[prop];
          delete restProps[prop];
        });
        delete restProps.keys;

        return React.createElement(
          Component,
          restProps,
          keyEntities.map(function (_ref3) {
            var status = _ref3.status,
                eventProps = _objectWithoutProperties(_ref3, ['status']);

            var visible = status === STATUS_ADD || status === STATUS_KEEP;
            return React.createElement(
              CSSMotion,
              _extends({}, motionProps, {
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

        var parsedKeyObjects = parseKeys(keys);

        // Always as keep when motion not support
        if (!transitionSupport) {
          return {
            keyEntities: parsedKeyObjects.map(function (obj) {
              return _extends({}, obj, { status: STATUS_KEEP });
            })
          };
        }

        var mixedKeyEntities = diffKeys(keyEntities, parsedKeyObjects);

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
            if (prevEntity && prevEntity.status === STATUS_REMOVED && entity.status === STATUS_REMOVE) {
              return false;
            }
            return true;
          })
        };
      }
    }]);

    return CSSMotionList;
  }(React.Component);

  CSSMotionList.propTypes = _extends({}, CSSMotion.propTypes, {
    component: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]),
    keys: PropTypes.array
  });
  CSSMotionList.defaultProps = {
    component: 'div'
  };


  polyfill(CSSMotionList);

  return CSSMotionList;
}

export default genCSSMotionList(supportTransition);