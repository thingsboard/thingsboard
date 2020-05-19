(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
    typeof define === 'function' && define.amd ? define(factory) :
    (global.InlineStylePrefixAll = factory());
}(this, function () { 'use strict';

    var babelHelpers = {};

    babelHelpers.classCallCheck = function (instance, Constructor) {
      if (!(instance instanceof Constructor)) {
        throw new TypeError("Cannot call a class as a function");
      }
    };

    babelHelpers.createClass = function () {
      function defineProperties(target, props) {
        for (var i = 0; i < props.length; i++) {
          var descriptor = props[i];
          descriptor.enumerable = descriptor.enumerable || false;
          descriptor.configurable = true;
          if ("value" in descriptor) descriptor.writable = true;
          Object.defineProperty(target, descriptor.key, descriptor);
        }
      }

      return function (Constructor, protoProps, staticProps) {
        if (protoProps) defineProperties(Constructor.prototype, protoProps);
        if (staticProps) defineProperties(Constructor, staticProps);
        return Constructor;
      };
    }();

    babelHelpers.defineProperty = function (obj, key, value) {
      if (key in obj) {
        Object.defineProperty(obj, key, {
          value: value,
          enumerable: true,
          configurable: true,
          writable: true
        });
      } else {
        obj[key] = value;
      }

      return obj;
    };

    babelHelpers;


    function __commonjs(fn, module) { return module = { exports: {} }, fn(module, module.exports), module.exports; }

    var prefixProps = { "Webkit": { "transform": true, "transformOrigin": true, "transformOriginX": true, "transformOriginY": true, "backfaceVisibility": true, "perspective": true, "perspectiveOrigin": true, "transformStyle": true, "transformOriginZ": true, "animation": true, "animationDelay": true, "animationDirection": true, "animationFillMode": true, "animationDuration": true, "animationIterationCount": true, "animationName": true, "animationPlayState": true, "animationTimingFunction": true, "appearance": true, "userSelect": true, "fontKerning": true, "textEmphasisPosition": true, "textEmphasis": true, "textEmphasisStyle": true, "textEmphasisColor": true, "boxDecorationBreak": true, "clipPath": true, "maskImage": true, "maskMode": true, "maskRepeat": true, "maskPosition": true, "maskClip": true, "maskOrigin": true, "maskSize": true, "maskComposite": true, "mask": true, "maskBorderSource": true, "maskBorderMode": true, "maskBorderSlice": true, "maskBorderWidth": true, "maskBorderOutset": true, "maskBorderRepeat": true, "maskBorder": true, "maskType": true, "textDecorationStyle": true, "textDecorationSkip": true, "textDecorationLine": true, "textDecorationColor": true, "filter": true, "fontFeatureSettings": true, "breakAfter": true, "breakBefore": true, "breakInside": true, "columnCount": true, "columnFill": true, "columnGap": true, "columnRule": true, "columnRuleColor": true, "columnRuleStyle": true, "columnRuleWidth": true, "columns": true, "columnSpan": true, "columnWidth": true, "flex": true, "flexBasis": true, "flexDirection": true, "flexGrow": true, "flexFlow": true, "flexShrink": true, "flexWrap": true, "alignContent": true, "alignItems": true, "alignSelf": true, "justifyContent": true, "order": true, "transition": true, "transitionDelay": true, "transitionDuration": true, "transitionProperty": true, "transitionTimingFunction": true, "backdropFilter": true, "scrollSnapType": true, "scrollSnapPointsX": true, "scrollSnapPointsY": true, "scrollSnapDestination": true, "scrollSnapCoordinate": true, "shapeImageThreshold": true, "shapeImageMargin": true, "shapeImageOutside": true, "hyphens": true, "flowInto": true, "flowFrom": true, "regionFragment": true, "textSizeAdjust": true }, "Moz": { "appearance": true, "userSelect": true, "boxSizing": true, "textAlignLast": true, "textDecorationStyle": true, "textDecorationSkip": true, "textDecorationLine": true, "textDecorationColor": true, "tabSize": true, "hyphens": true, "fontFeatureSettings": true, "breakAfter": true, "breakBefore": true, "breakInside": true, "columnCount": true, "columnFill": true, "columnGap": true, "columnRule": true, "columnRuleColor": true, "columnRuleStyle": true, "columnRuleWidth": true, "columns": true, "columnSpan": true, "columnWidth": true }, "ms": { "flex": true, "flexBasis": false, "flexDirection": true, "flexGrow": false, "flexFlow": true, "flexShrink": false, "flexWrap": true, "alignContent": false, "alignItems": false, "alignSelf": false, "justifyContent": false, "order": false, "transform": true, "transformOrigin": true, "transformOriginX": true, "transformOriginY": true, "userSelect": true, "wrapFlow": true, "wrapThrough": true, "wrapMargin": true, "scrollSnapType": true, "scrollSnapPointsX": true, "scrollSnapPointsY": true, "scrollSnapDestination": true, "scrollSnapCoordinate": true, "touchAction": true, "hyphens": true, "flowInto": true, "flowFrom": true, "breakBefore": true, "breakAfter": true, "breakInside": true, "regionFragment": true, "gridTemplateColumns": true, "gridTemplateRows": true, "gridTemplateAreas": true, "gridTemplate": true, "gridAutoColumns": true, "gridAutoRows": true, "gridAutoFlow": true, "grid": true, "gridRowStart": true, "gridColumnStart": true, "gridRowEnd": true, "gridRow": true, "gridColumn": true, "gridColumnEnd": true, "gridColumnGap": true, "gridRowGap": true, "gridArea": true, "gridGap": true, "textSizeAdjust": true } };

    // helper to capitalize strings
    var capitalizeString = (function (str) {
      return str.charAt(0).toUpperCase() + str.slice(1);
    });

    var isPrefixedProperty = (function (property) {
      return property.match(/^(Webkit|Moz|O|ms)/) !== null;
    });

    function sortPrefixedStyle(style) {
      return Object.keys(style).sort(function (left, right) {
        if (isPrefixedProperty(left) && !isPrefixedProperty(right)) {
          return -1;
        } else if (!isPrefixedProperty(left) && isPrefixedProperty(right)) {
          return 1;
        }
        return 0;
      }).reduce(function (sortedStyle, prop) {
        sortedStyle[prop] = style[prop];
        return sortedStyle;
      }, {});
    }

    function position(property, value) {
      if (property === 'position' && value === 'sticky') {
        return { position: ['-webkit-sticky', 'sticky'] };
      }
    }

    // returns a style object with a single concated prefixed value string
    var joinPrefixedValue = (function (property, value) {
      var replacer = arguments.length <= 2 || arguments[2] === undefined ? function (prefix, value) {
        return prefix + value;
      } : arguments[2];
      return babelHelpers.defineProperty({}, property, ['-webkit-', '-moz-', ''].map(function (prefix) {
        return replacer(prefix, value);
      }));
    });

    var isPrefixedValue = (function (value) {
      if (Array.isArray(value)) value = value.join(',');

      return value.match(/-webkit-|-moz-|-ms-/) !== null;
    });

    function calc(property, value) {
      if (typeof value === 'string' && !isPrefixedValue(value) && value.indexOf('calc(') > -1) {
        return joinPrefixedValue(property, value, function (prefix, value) {
          return value.replace(/calc\(/g, prefix + 'calc(');
        });
      }
    }

    var values = {
      'zoom-in': true,
      'zoom-out': true,
      grab: true,
      grabbing: true
    };

    function cursor(property, value) {
      if (property === 'cursor' && values[value]) {
        return joinPrefixedValue(property, value);
      }
    }

    var values$1 = { flex: true, 'inline-flex': true };

    function flex(property, value) {
      if (property === 'display' && values$1[value]) {
        return {
          display: ['-webkit-box', '-moz-box', '-ms-' + value + 'box', '-webkit-' + value, value]
        };
      }
    }

    var properties = {
      maxHeight: true,
      maxWidth: true,
      width: true,
      height: true,
      columnWidth: true,
      minWidth: true,
      minHeight: true
    };
    var values$2 = {
      'min-content': true,
      'max-content': true,
      'fill-available': true,
      'fit-content': true,
      'contain-floats': true
    };

    function sizing(property, value) {
      if (properties[property] && values$2[value]) {
        return joinPrefixedValue(property, value);
      }
    }

    var values$3 = /linear-gradient|radial-gradient|repeating-linear-gradient|repeating-radial-gradient/;

    function gradient(property, value) {
      if (typeof value === 'string' && !isPrefixedValue(value) && value.match(values$3) !== null) {
        return joinPrefixedValue(property, value);
      }
    }

    var index = __commonjs(function (module) {
    'use strict';

    var uppercasePattern = /[A-Z]/g;
    var msPattern = /^ms-/;

    function hyphenateStyleName(string) {
        return string.replace(uppercasePattern, '-$&').toLowerCase().replace(msPattern, '-ms-');
    }

    module.exports = hyphenateStyleName;
    });

    var hyphenateStyleName = (index && typeof index === 'object' && 'default' in index ? index['default'] : index);

    var properties$1 = {
      transition: true,
      transitionProperty: true,
      WebkitTransition: true,
      WebkitTransitionProperty: true
    };

    function transition(property, value) {
      // also check for already prefixed transitions
      if (typeof value === 'string' && properties$1[property]) {
        var _ref2;

        var outputValue = prefixValue(value);
        var webkitOutput = outputValue.split(/,(?![^()]*(?:\([^()]*\))?\))/g).filter(function (value) {
          return value.match(/-moz-|-ms-/) === null;
        }).join(',');

        // if the property is already prefixed
        if (property.indexOf('Webkit') > -1) {
          return babelHelpers.defineProperty({}, property, webkitOutput);
        }

        return _ref2 = {}, babelHelpers.defineProperty(_ref2, 'Webkit' + capitalizeString(property), webkitOutput), babelHelpers.defineProperty(_ref2, property, outputValue), _ref2;
      }
    }

    function prefixValue(value) {
      if (isPrefixedValue(value)) {
        return value;
      }

      // only split multi values, not cubic beziers
      var multipleValues = value.split(/,(?![^()]*(?:\([^()]*\))?\))/g);

      // iterate each single value and check for transitioned properties
      // that need to be prefixed as well
      multipleValues.forEach(function (val, index) {
        multipleValues[index] = Object.keys(prefixProps).reduce(function (out, prefix) {
          var dashCasePrefix = '-' + prefix.toLowerCase() + '-';

          Object.keys(prefixProps[prefix]).forEach(function (prop) {
            var dashCaseProperty = hyphenateStyleName(prop);

            if (val.indexOf(dashCaseProperty) > -1 && dashCaseProperty !== 'order') {
              // join all prefixes and create a new value
              out = val.replace(dashCaseProperty, dashCasePrefix + dashCaseProperty) + ',' + out;
            }
          });
          return out;
        }, val);
      });

      return multipleValues.join(',');
    }

    var alternativeValues = {
      'space-around': 'distribute',
      'space-between': 'justify',
      'flex-start': 'start',
      'flex-end': 'end'
    };
    var alternativeProps = {
      alignContent: 'msFlexLinePack',
      alignSelf: 'msFlexItemAlign',
      alignItems: 'msFlexAlign',
      justifyContent: 'msFlexPack',
      order: 'msFlexOrder',
      flexGrow: 'msFlexPositive',
      flexShrink: 'msFlexNegative',
      flexBasis: 'msPreferredSize'
    };

    function flexboxIE(property, value) {
      if (alternativeProps[property]) {
        return babelHelpers.defineProperty({}, alternativeProps[property], alternativeValues[value] || value);
      }
    }

    var alternativeValues$1 = {
      'space-around': 'justify',
      'space-between': 'justify',
      'flex-start': 'start',
      'flex-end': 'end',
      'wrap-reverse': 'multiple',
      wrap: 'multiple'
    };

    var alternativeProps$1 = {
      alignItems: 'WebkitBoxAlign',
      justifyContent: 'WebkitBoxPack',
      flexWrap: 'WebkitBoxLines'
    };

    function flexboxOld(property, value) {
      if (property === 'flexDirection' && typeof value === 'string') {
        return {
          WebkitBoxOrient: value.indexOf('column') > -1 ? 'vertical' : 'horizontal',
          WebkitBoxDirection: value.indexOf('reverse') > -1 ? 'reverse' : 'normal'
        };
      }
      if (alternativeProps$1[property]) {
        return babelHelpers.defineProperty({}, alternativeProps$1[property], alternativeValues$1[value] || value);
      }
    }

    var plugins = [position, calc, cursor, sizing, gradient, transition, flexboxIE, flexboxOld, flex];

    /**
     * Returns a prefixed version of the style object using all vendor prefixes
     * @param {Object} styles - Style object that gets prefixed properties added
     * @returns {Object} - Style object with prefixed properties and values
     */
    function prefixAll(styles) {
      Object.keys(styles).forEach(function (property) {
        var value = styles[property];
        if (value instanceof Object && !Array.isArray(value)) {
          // recurse through nested style objects
          styles[property] = prefixAll(value);
        } else {
          Object.keys(prefixProps).forEach(function (prefix) {
            var properties = prefixProps[prefix];
            // add prefixes if needed
            if (properties[property]) {
              styles[prefix + capitalizeString(property)] = value;
            }
          });
        }
      });

      Object.keys(styles).forEach(function (property) {
        [].concat(styles[property]).forEach(function (value, index) {
          // resolve every special plugins
          plugins.forEach(function (plugin) {
            return assignStyles(styles, plugin(property, value));
          });
        });
      });

      return sortPrefixedStyle(styles);
    }

    function assignStyles(base) {
      var extend = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];

      Object.keys(extend).forEach(function (property) {
        var baseValue = base[property];
        if (Array.isArray(baseValue)) {
          [].concat(extend[property]).forEach(function (value) {
            var valueIndex = baseValue.indexOf(value);
            if (valueIndex > -1) {
              base[property].splice(valueIndex, 1);
            }
            base[property].push(value);
          });
        } else {
          base[property] = extend[property];
        }
      });
    }

    return prefixAll;

}));
//# sourceMappingURL=inline-style-prefix-all.js.map