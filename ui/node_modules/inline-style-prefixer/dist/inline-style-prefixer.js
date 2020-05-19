(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
    typeof define === 'function' && define.amd ? define(factory) :
    (global.InlineStylePrefixer = factory());
}(this, function () { 'use strict';

    var babelHelpers = {};
    babelHelpers.typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) {
      return typeof obj;
    } : function (obj) {
      return obj && typeof Symbol === "function" && obj.constructor === Symbol ? "symbol" : typeof obj;
    };

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

    var plugins$1 = [position, calc, cursor, sizing, gradient, transition, flexboxIE, flexboxOld, flex];

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
          plugins$1.forEach(function (plugin) {
            return assignStyles$1(styles, plugin(property, value));
          });
        });
      });

      return sortPrefixedStyle(styles);
    }

    function assignStyles$1(base) {
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

    var bowser = __commonjs(function (module) {
    /*!
     * Bowser - a browser detector
     * https://github.com/ded/bowser
     * MIT License | (c) Dustin Diaz 2015
     */

    !function (name, definition) {
      if (typeof module != 'undefined' && module.exports) module.exports = definition();else if (typeof define == 'function' && define.amd) define(definition);else this[name] = definition();
    }('bowser', function () {
      /**
        * See useragents.js for examples of navigator.userAgent
        */

      var t = true;

      function detect(ua) {

        function getFirstMatch(regex) {
          var match = ua.match(regex);
          return match && match.length > 1 && match[1] || '';
        }

        function getSecondMatch(regex) {
          var match = ua.match(regex);
          return match && match.length > 1 && match[2] || '';
        }

        var iosdevice = getFirstMatch(/(ipod|iphone|ipad)/i).toLowerCase(),
            likeAndroid = /like android/i.test(ua),
            android = !likeAndroid && /android/i.test(ua),
            nexusMobile = /nexus\s*[0-6]\s*/i.test(ua),
            nexusTablet = !nexusMobile && /nexus\s*[0-9]+/i.test(ua),
            chromeos = /CrOS/.test(ua),
            silk = /silk/i.test(ua),
            sailfish = /sailfish/i.test(ua),
            tizen = /tizen/i.test(ua),
            webos = /(web|hpw)os/i.test(ua),
            windowsphone = /windows phone/i.test(ua),
            windows = !windowsphone && /windows/i.test(ua),
            mac = !iosdevice && !silk && /macintosh/i.test(ua),
            linux = !android && !sailfish && !tizen && !webos && /linux/i.test(ua),
            edgeVersion = getFirstMatch(/edge\/(\d+(\.\d+)?)/i),
            versionIdentifier = getFirstMatch(/version\/(\d+(\.\d+)?)/i),
            tablet = /tablet/i.test(ua),
            mobile = !tablet && /[^-]mobi/i.test(ua),
            xbox = /xbox/i.test(ua),
            result;

        if (/opera|opr|opios/i.test(ua)) {
          result = {
            name: 'Opera',
            opera: t,
            version: versionIdentifier || getFirstMatch(/(?:opera|opr|opios)[\s\/](\d+(\.\d+)?)/i)
          };
        } else if (/coast/i.test(ua)) {
          result = {
            name: 'Opera Coast',
            coast: t,
            version: versionIdentifier || getFirstMatch(/(?:coast)[\s\/](\d+(\.\d+)?)/i)
          };
        } else if (/yabrowser/i.test(ua)) {
          result = {
            name: 'Yandex Browser',
            yandexbrowser: t,
            version: versionIdentifier || getFirstMatch(/(?:yabrowser)[\s\/](\d+(\.\d+)?)/i)
          };
        } else if (/ucbrowser/i.test(ua)) {
          result = {
            name: 'UC Browser',
            ucbrowser: t,
            version: getFirstMatch(/(?:ucbrowser)[\s\/](\d+(?:\.\d+)+)/i)
          };
        } else if (/mxios/i.test(ua)) {
          result = {
            name: 'Maxthon',
            maxthon: t,
            version: getFirstMatch(/(?:mxios)[\s\/](\d+(?:\.\d+)+)/i)
          };
        } else if (/epiphany/i.test(ua)) {
          result = {
            name: 'Epiphany',
            epiphany: t,
            version: getFirstMatch(/(?:epiphany)[\s\/](\d+(?:\.\d+)+)/i)
          };
        } else if (/puffin/i.test(ua)) {
          result = {
            name: 'Puffin',
            puffin: t,
            version: getFirstMatch(/(?:puffin)[\s\/](\d+(?:\.\d+)?)/i)
          };
        } else if (/sleipnir/i.test(ua)) {
          result = {
            name: 'Sleipnir',
            sleipnir: t,
            version: getFirstMatch(/(?:sleipnir)[\s\/](\d+(?:\.\d+)+)/i)
          };
        } else if (/k-meleon/i.test(ua)) {
          result = {
            name: 'K-Meleon',
            kMeleon: t,
            version: getFirstMatch(/(?:k-meleon)[\s\/](\d+(?:\.\d+)+)/i)
          };
        } else if (windowsphone) {
          result = {
            name: 'Windows Phone',
            windowsphone: t
          };
          if (edgeVersion) {
            result.msedge = t;
            result.version = edgeVersion;
          } else {
            result.msie = t;
            result.version = getFirstMatch(/iemobile\/(\d+(\.\d+)?)/i);
          }
        } else if (/msie|trident/i.test(ua)) {
          result = {
            name: 'Internet Explorer',
            msie: t,
            version: getFirstMatch(/(?:msie |rv:)(\d+(\.\d+)?)/i)
          };
        } else if (chromeos) {
          result = {
            name: 'Chrome',
            chromeos: t,
            chromeBook: t,
            chrome: t,
            version: getFirstMatch(/(?:chrome|crios|crmo)\/(\d+(\.\d+)?)/i)
          };
        } else if (/chrome.+? edge/i.test(ua)) {
          result = {
            name: 'Microsoft Edge',
            msedge: t,
            version: edgeVersion
          };
        } else if (/vivaldi/i.test(ua)) {
          result = {
            name: 'Vivaldi',
            vivaldi: t,
            version: getFirstMatch(/vivaldi\/(\d+(\.\d+)?)/i) || versionIdentifier
          };
        } else if (sailfish) {
          result = {
            name: 'Sailfish',
            sailfish: t,
            version: getFirstMatch(/sailfish\s?browser\/(\d+(\.\d+)?)/i)
          };
        } else if (/seamonkey\//i.test(ua)) {
          result = {
            name: 'SeaMonkey',
            seamonkey: t,
            version: getFirstMatch(/seamonkey\/(\d+(\.\d+)?)/i)
          };
        } else if (/firefox|iceweasel|fxios/i.test(ua)) {
          result = {
            name: 'Firefox',
            firefox: t,
            version: getFirstMatch(/(?:firefox|iceweasel|fxios)[ \/](\d+(\.\d+)?)/i)
          };
          if (/\((mobile|tablet);[^\)]*rv:[\d\.]+\)/i.test(ua)) {
            result.firefoxos = t;
          }
        } else if (silk) {
          result = {
            name: 'Amazon Silk',
            silk: t,
            version: getFirstMatch(/silk\/(\d+(\.\d+)?)/i)
          };
        } else if (/phantom/i.test(ua)) {
          result = {
            name: 'PhantomJS',
            phantom: t,
            version: getFirstMatch(/phantomjs\/(\d+(\.\d+)?)/i)
          };
        } else if (/slimerjs/i.test(ua)) {
          result = {
            name: 'SlimerJS',
            slimer: t,
            version: getFirstMatch(/slimerjs\/(\d+(\.\d+)?)/i)
          };
        } else if (/blackberry|\bbb\d+/i.test(ua) || /rim\stablet/i.test(ua)) {
          result = {
            name: 'BlackBerry',
            blackberry: t,
            version: versionIdentifier || getFirstMatch(/blackberry[\d]+\/(\d+(\.\d+)?)/i)
          };
        } else if (webos) {
          result = {
            name: 'WebOS',
            webos: t,
            version: versionIdentifier || getFirstMatch(/w(?:eb)?osbrowser\/(\d+(\.\d+)?)/i)
          };
          /touchpad\//i.test(ua) && (result.touchpad = t);
        } else if (/bada/i.test(ua)) {
          result = {
            name: 'Bada',
            bada: t,
            version: getFirstMatch(/dolfin\/(\d+(\.\d+)?)/i)
          };
        } else if (tizen) {
          result = {
            name: 'Tizen',
            tizen: t,
            version: getFirstMatch(/(?:tizen\s?)?browser\/(\d+(\.\d+)?)/i) || versionIdentifier
          };
        } else if (/qupzilla/i.test(ua)) {
          result = {
            name: 'QupZilla',
            qupzilla: t,
            version: getFirstMatch(/(?:qupzilla)[\s\/](\d+(?:\.\d+)+)/i) || versionIdentifier
          };
        } else if (/chromium/i.test(ua)) {
          result = {
            name: 'Chromium',
            chromium: t,
            version: getFirstMatch(/(?:chromium)[\s\/](\d+(?:\.\d+)?)/i) || versionIdentifier
          };
        } else if (/chrome|crios|crmo/i.test(ua)) {
          result = {
            name: 'Chrome',
            chrome: t,
            version: getFirstMatch(/(?:chrome|crios|crmo)\/(\d+(\.\d+)?)/i)
          };
        } else if (android) {
          result = {
            name: 'Android',
            version: versionIdentifier
          };
        } else if (/safari|applewebkit/i.test(ua)) {
          result = {
            name: 'Safari',
            safari: t
          };
          if (versionIdentifier) {
            result.version = versionIdentifier;
          }
        } else if (iosdevice) {
          result = {
            name: iosdevice == 'iphone' ? 'iPhone' : iosdevice == 'ipad' ? 'iPad' : 'iPod'
          };
          // WTF: version is not part of user agent in web apps
          if (versionIdentifier) {
            result.version = versionIdentifier;
          }
        } else if (/googlebot/i.test(ua)) {
          result = {
            name: 'Googlebot',
            googlebot: t,
            version: getFirstMatch(/googlebot\/(\d+(\.\d+))/i) || versionIdentifier
          };
        } else {
          result = {
            name: getFirstMatch(/^(.*)\/(.*) /),
            version: getSecondMatch(/^(.*)\/(.*) /)
          };
        }

        // set webkit or gecko flag for browsers based on these engines
        if (!result.msedge && /(apple)?webkit/i.test(ua)) {
          if (/(apple)?webkit\/537\.36/i.test(ua)) {
            result.name = result.name || "Blink";
            result.blink = t;
          } else {
            result.name = result.name || "Webkit";
            result.webkit = t;
          }
          if (!result.version && versionIdentifier) {
            result.version = versionIdentifier;
          }
        } else if (!result.opera && /gecko\//i.test(ua)) {
          result.name = result.name || "Gecko";
          result.gecko = t;
          result.version = result.version || getFirstMatch(/gecko\/(\d+(\.\d+)?)/i);
        }

        // set OS flags for platforms that have multiple browsers
        if (!result.msedge && (android || result.silk)) {
          result.android = t;
        } else if (iosdevice) {
          result[iosdevice] = t;
          result.ios = t;
        } else if (mac) {
          result.mac = t;
        } else if (xbox) {
          result.xbox = t;
        } else if (windows) {
          result.windows = t;
        } else if (linux) {
          result.linux = t;
        }

        // OS version extraction
        var osVersion = '';
        if (result.windowsphone) {
          osVersion = getFirstMatch(/windows phone (?:os)?\s?(\d+(\.\d+)*)/i);
        } else if (iosdevice) {
          osVersion = getFirstMatch(/os (\d+([_\s]\d+)*) like mac os x/i);
          osVersion = osVersion.replace(/[_\s]/g, '.');
        } else if (android) {
          osVersion = getFirstMatch(/android[ \/-](\d+(\.\d+)*)/i);
        } else if (result.webos) {
          osVersion = getFirstMatch(/(?:web|hpw)os\/(\d+(\.\d+)*)/i);
        } else if (result.blackberry) {
          osVersion = getFirstMatch(/rim\stablet\sos\s(\d+(\.\d+)*)/i);
        } else if (result.bada) {
          osVersion = getFirstMatch(/bada\/(\d+(\.\d+)*)/i);
        } else if (result.tizen) {
          osVersion = getFirstMatch(/tizen[\/\s](\d+(\.\d+)*)/i);
        }
        if (osVersion) {
          result.osversion = osVersion;
        }

        // device type extraction
        var osMajorVersion = osVersion.split('.')[0];
        if (tablet || nexusTablet || iosdevice == 'ipad' || android && (osMajorVersion == 3 || osMajorVersion >= 4 && !mobile) || result.silk) {
          result.tablet = t;
        } else if (mobile || iosdevice == 'iphone' || iosdevice == 'ipod' || android || nexusMobile || result.blackberry || result.webos || result.bada) {
          result.mobile = t;
        }

        // Graded Browser Support
        // http://developer.yahoo.com/yui/articles/gbs
        if (result.msedge || result.msie && result.version >= 10 || result.yandexbrowser && result.version >= 15 || result.vivaldi && result.version >= 1.0 || result.chrome && result.version >= 20 || result.firefox && result.version >= 20.0 || result.safari && result.version >= 6 || result.opera && result.version >= 10.0 || result.ios && result.osversion && result.osversion.split(".")[0] >= 6 || result.blackberry && result.version >= 10.1 || result.chromium && result.version >= 20) {
          result.a = t;
        } else if (result.msie && result.version < 10 || result.chrome && result.version < 20 || result.firefox && result.version < 20.0 || result.safari && result.version < 6 || result.opera && result.version < 10.0 || result.ios && result.osversion && result.osversion.split(".")[0] < 6 || result.chromium && result.version < 20) {
          result.c = t;
        } else result.x = t;

        return result;
      }

      var bowser = detect(typeof navigator !== 'undefined' ? navigator.userAgent : '');

      bowser.test = function (browserList) {
        for (var i = 0; i < browserList.length; ++i) {
          var browserItem = browserList[i];
          if (typeof browserItem === 'string') {
            if (browserItem in bowser) {
              return true;
            }
          }
        }
        return false;
      };

      /**
       * Get version precisions count
       *
       * @example
       *   getVersionPrecision("1.10.3") // 3
       *
       * @param  {string} version
       * @return {number}
       */
      function getVersionPrecision(version) {
        return version.split(".").length;
      }

      /**
       * Array::map polyfill
       *
       * @param  {Array} arr
       * @param  {Function} iterator
       * @return {Array}
       */
      function map(arr, iterator) {
        var result = [],
            i;
        if (Array.prototype.map) {
          return Array.prototype.map.call(arr, iterator);
        }
        for (i = 0; i < arr.length; i++) {
          result = iterator(arr[i]);
        }
        return result;
      }

      /**
       * Calculate browser version weight
       *
       * @example
       *   compareVersions(['1.10.2.1',  '1.8.2.1.90'])    // 1
       *   compareVersions(['1.010.2.1', '1.09.2.1.90']);  // 1
       *   compareVersions(['1.10.2.1',  '1.10.2.1']);     // 0
       *   compareVersions(['1.10.2.1',  '1.0800.2']);     // -1
       *
       * @param  {Array<String>} versions versions to compare
       * @return {Number} comparison result
       */
      function compareVersions(versions) {
        // 1) get common precision for both versions, for example for "10.0" and "9" it should be 2
        var precision = Math.max(getVersionPrecision(versions[0]), getVersionPrecision(versions[1]));
        var chunks = map(versions, function (version) {
          var delta = precision - getVersionPrecision(version);

          // 2) "9" -> "9.0" (for precision = 2)
          version = version + new Array(delta + 1).join(".0");

          // 3) "9.0" -> ["000000000"", "000000009"]
          return map(version.split("."), function (chunk) {
            return new Array(20 - chunk.length).join("0") + chunk;
          }).reverse();
        });

        // iterate in reverse order by reversed chunks array
        while (--precision >= 0) {
          // 4) compare: "000000009" > "000000010" = false (but "9" > "10" = true)
          if (chunks[0][precision] > chunks[1][precision]) {
            return 1;
          } else if (chunks[0][precision] === chunks[1][precision]) {
            if (precision === 0) {
              // all version chunks are same
              return 0;
            }
          } else {
            return -1;
          }
        }
      }

      /**
       * Check if browser is unsupported
       *
       * @example
       *   bowser.isUnsupportedBrowser({
       *     msie: "10",
       *     firefox: "23",
       *     chrome: "29",
       *     safari: "5.1",
       *     opera: "16",
       *     phantom: "534"
       *   });
       *
       * @param  {Object}  minVersions map of minimal version to browser
       * @param  {Boolean} [strictMode = false] flag to return false if browser wasn't found in map
       * @param  {String}  [ua] user agent string
       * @return {Boolean}
       */
      function isUnsupportedBrowser(minVersions, strictMode, ua) {
        var _bowser = bowser;

        // make strictMode param optional with ua param usage
        if (typeof strictMode === 'string') {
          ua = strictMode;
          strictMode = void 0;
        }

        if (strictMode === void 0) {
          strictMode = false;
        }
        if (ua) {
          _bowser = detect(ua);
        }

        var version = "" + _bowser.version;
        for (var browser in minVersions) {
          if (minVersions.hasOwnProperty(browser)) {
            if (_bowser[browser]) {
              // browser version and min supported version.
              if (compareVersions([version, minVersions[browser]]) < 0) {
                return true; // unsupported
              }
            }
          }
        }
        return strictMode; // not found
      }

      /**
       * Check if browser is supported
       *
       * @param  {Object} minVersions map of minimal version to browser
       * @param  {Boolean} [strictMode = false] flag to return false if browser wasn't found in map
       * @return {Boolean}
       */
      function check(minVersions, strictMode) {
        return !isUnsupportedBrowser(minVersions, strictMode);
      }

      bowser.isUnsupportedBrowser = isUnsupportedBrowser;
      bowser.compareVersions = compareVersions;
      bowser.check = check;

      /*
       * Set our detect method to the main bowser object so we can
       * reuse it to test other user agents.
       * This is needed to implement future tests.
       */
      bowser._detect = detect;

      return bowser;
    });
    });

    var bowser$1 = (bowser && typeof bowser === 'object' && 'default' in bowser ? bowser['default'] : bowser);

    var vendorPrefixes = {
      Webkit: ['chrome', 'safari', 'ios', 'android', 'phantom', 'opera', 'webos', 'blackberry', 'bada', 'tizen', 'chromium', 'vivaldi'],
      Moz: ['firefox', 'seamonkey', 'sailfish'],
      ms: ['msie', 'msedge']
    };
    var browsers = {
      chrome: [['chrome'], ['chromium']],
      safari: [['safari']],
      firefox: [['firefox']],
      edge: [['msedge']],
      opera: [['opera'], ['vivaldi']],
      ios_saf: [['ios', 'mobile'], ['ios', 'tablet']],
      ie: [['msie']],
      op_mini: [['opera', 'mobile'], ['opera', 'tablet']],
      and_uc: [['android', 'mobile'], ['android', 'tablet']],
      android: [['android', 'mobile'], ['android', 'tablet']]
    };

    var browserByInfo = function browserByInfo(info) {
      if (info.firefox) {
        return 'firefox';
      }
      var name = '';

      Object.keys(browsers).forEach(function (browser) {
        browsers[browser].forEach(function (condition) {
          var match = 0;
          condition.forEach(function (single) {
            if (info[single]) {
              match += 1;
            }
          });
          if (condition.length === match) {
            name = browser;
          }
        });
      });

      return name;
    };

    /**
     * Uses bowser to get default browser information such as version and name
     * Evaluates bowser info and adds vendorPrefix information
     * @param {string} userAgent - userAgent that gets evaluated
     */
    var getBrowserInformation = (function (userAgent) {
      if (!userAgent) {
        return false;
      }
      var info = bowser$1._detect(userAgent);

      Object.keys(vendorPrefixes).forEach(function (prefix) {
        vendorPrefixes[prefix].forEach(function (browser) {
          if (info[browser]) {
            info.prefix = {
              inline: prefix,
              css: '-' + prefix.toLowerCase() + '-'
            };
          }
        });
      });

      info.browser = browserByInfo(info);

      // For cordova IOS 8 the version is missing, set truncated osversion to prevent NaN
      info.version = info.version ? parseFloat(info.version) : parseInt(parseFloat(info.osversion), 10);
      info.osversion = parseFloat(info.osversion);

      // iOS forces all browsers to use Safari under the hood
      // as the Safari version seems to match the iOS version
      // we just explicitely use the osversion instead
      // https://github.com/rofrischmann/inline-style-prefixer/issues/72
      if (info.browser === 'ios_saf' && info.version > info.osversion) {
        info.version = info.osversion;
        info.safari = true;
      }

      // seperate native android chrome
      // https://github.com/rofrischmann/inline-style-prefixer/issues/45
      if (info.browser === 'android' && info.chrome && info.version > 37) {
        info.browser = 'and_chr';
      }

      // For android < 4.4 we want to check the osversion
      // not the chrome version, see issue #26
      // https://github.com/rofrischmann/inline-style-prefixer/issues/26
      if (info.browser === 'android' && info.osversion < 5) {
        info.version = info.osversion;
      }

      return info;
    });

    var getPrefixedKeyframes = (function (_ref) {
      var browser = _ref.browser;
      var version = _ref.version;
      var prefix = _ref.prefix;

      var prefixedKeyframes = 'keyframes';

      if (browser === 'chrome' && version < 43 || (browser === 'safari' || browser === 'ios_saf') && version < 9 || browser === 'opera' && version < 30 || browser === 'android' && version <= 4.4 || browser === 'and_uc') {
        prefixedKeyframes = prefix.css + prefixedKeyframes;
      }
      return prefixedKeyframes;
    });

    var prefixProps$1 = { "chrome": { "transform": 35, "transformOrigin": 35, "transformOriginX": 35, "transformOriginY": 35, "backfaceVisibility": 35, "perspective": 35, "perspectiveOrigin": 35, "transformStyle": 35, "transformOriginZ": 35, "animation": 42, "animationDelay": 42, "animationDirection": 42, "animationFillMode": 42, "animationDuration": 42, "animationIterationCount": 42, "animationName": 42, "animationPlayState": 42, "animationTimingFunction": 42, "appearance": 55, "userSelect": 55, "fontKerning": 32, "textEmphasisPosition": 55, "textEmphasis": 55, "textEmphasisStyle": 55, "textEmphasisColor": 55, "boxDecorationBreak": 55, "clipPath": 55, "maskImage": 55, "maskMode": 55, "maskRepeat": 55, "maskPosition": 55, "maskClip": 55, "maskOrigin": 55, "maskSize": 55, "maskComposite": 55, "mask": 55, "maskBorderSource": 55, "maskBorderMode": 55, "maskBorderSlice": 55, "maskBorderWidth": 55, "maskBorderOutset": 55, "maskBorderRepeat": 55, "maskBorder": 55, "maskType": 55, "textDecorationStyle": 55, "textDecorationSkip": 55, "textDecorationLine": 55, "textDecorationColor": 55, "filter": 52, "fontFeatureSettings": 47, "breakAfter": 49, "breakBefore": 49, "breakInside": 49, "columnCount": 49, "columnFill": 49, "columnGap": 49, "columnRule": 49, "columnRuleColor": 49, "columnRuleStyle": 49, "columnRuleWidth": 49, "columns": 49, "columnSpan": 49, "columnWidth": 49 }, "safari": { "flex": 8, "flexBasis": 8, "flexDirection": 8, "flexGrow": 8, "flexFlow": 8, "flexShrink": 8, "flexWrap": 8, "alignContent": 8, "alignItems": 8, "alignSelf": 8, "justifyContent": 8, "order": 8, "transition": 6, "transitionDelay": 6, "transitionDuration": 6, "transitionProperty": 6, "transitionTimingFunction": 6, "transform": 8, "transformOrigin": 8, "transformOriginX": 8, "transformOriginY": 8, "backfaceVisibility": 8, "perspective": 8, "perspectiveOrigin": 8, "transformStyle": 8, "transformOriginZ": 8, "animation": 8, "animationDelay": 8, "animationDirection": 8, "animationFillMode": 8, "animationDuration": 8, "animationIterationCount": 8, "animationName": 8, "animationPlayState": 8, "animationTimingFunction": 8, "appearance": 10, "userSelect": 10, "backdropFilter": 10, "fontKerning": 9, "scrollSnapType": 10, "scrollSnapPointsX": 10, "scrollSnapPointsY": 10, "scrollSnapDestination": 10, "scrollSnapCoordinate": 10, "textEmphasisPosition": 7, "textEmphasis": 7, "textEmphasisStyle": 7, "textEmphasisColor": 7, "boxDecorationBreak": 10, "clipPath": 10, "maskImage": 10, "maskMode": 10, "maskRepeat": 10, "maskPosition": 10, "maskClip": 10, "maskOrigin": 10, "maskSize": 10, "maskComposite": 10, "mask": 10, "maskBorderSource": 10, "maskBorderMode": 10, "maskBorderSlice": 10, "maskBorderWidth": 10, "maskBorderOutset": 10, "maskBorderRepeat": 10, "maskBorder": 10, "maskType": 10, "textDecorationStyle": 10, "textDecorationSkip": 10, "textDecorationLine": 10, "textDecorationColor": 10, "shapeImageThreshold": 10, "shapeImageMargin": 10, "shapeImageOutside": 10, "filter": 9, "hyphens": 10, "flowInto": 10, "flowFrom": 10, "breakBefore": 8, "breakAfter": 8, "breakInside": 8, "regionFragment": 10, "columnCount": 8, "columnFill": 8, "columnGap": 8, "columnRule": 8, "columnRuleColor": 8, "columnRuleStyle": 8, "columnRuleWidth": 8, "columns": 8, "columnSpan": 8, "columnWidth": 8 }, "firefox": { "appearance": 51, "userSelect": 51, "boxSizing": 28, "textAlignLast": 48, "textDecorationStyle": 35, "textDecorationSkip": 35, "textDecorationLine": 35, "textDecorationColor": 35, "tabSize": 51, "hyphens": 42, "fontFeatureSettings": 33, "breakAfter": 51, "breakBefore": 51, "breakInside": 51, "columnCount": 51, "columnFill": 51, "columnGap": 51, "columnRule": 51, "columnRuleColor": 51, "columnRuleStyle": 51, "columnRuleWidth": 51, "columns": 51, "columnSpan": 51, "columnWidth": 51 }, "opera": { "flex": 16, "flexBasis": 16, "flexDirection": 16, "flexGrow": 16, "flexFlow": 16, "flexShrink": 16, "flexWrap": 16, "alignContent": 16, "alignItems": 16, "alignSelf": 16, "justifyContent": 16, "order": 16, "transform": 22, "transformOrigin": 22, "transformOriginX": 22, "transformOriginY": 22, "backfaceVisibility": 22, "perspective": 22, "perspectiveOrigin": 22, "transformStyle": 22, "transformOriginZ": 22, "animation": 29, "animationDelay": 29, "animationDirection": 29, "animationFillMode": 29, "animationDuration": 29, "animationIterationCount": 29, "animationName": 29, "animationPlayState": 29, "animationTimingFunction": 29, "appearance": 41, "userSelect": 41, "fontKerning": 19, "textEmphasisPosition": 41, "textEmphasis": 41, "textEmphasisStyle": 41, "textEmphasisColor": 41, "boxDecorationBreak": 41, "clipPath": 41, "maskImage": 41, "maskMode": 41, "maskRepeat": 41, "maskPosition": 41, "maskClip": 41, "maskOrigin": 41, "maskSize": 41, "maskComposite": 41, "mask": 41, "maskBorderSource": 41, "maskBorderMode": 41, "maskBorderSlice": 41, "maskBorderWidth": 41, "maskBorderOutset": 41, "maskBorderRepeat": 41, "maskBorder": 41, "maskType": 41, "textDecorationStyle": 41, "textDecorationSkip": 41, "textDecorationLine": 41, "textDecorationColor": 41, "filter": 39, "fontFeatureSettings": 34, "breakAfter": 36, "breakBefore": 36, "breakInside": 36, "columnCount": 36, "columnFill": 36, "columnGap": 36, "columnRule": 36, "columnRuleColor": 36, "columnRuleStyle": 36, "columnRuleWidth": 36, "columns": 36, "columnSpan": 36, "columnWidth": 36 }, "ie": { "flex": 10, "flexDirection": 10, "flexFlow": 10, "flexWrap": 10, "transform": 9, "transformOrigin": 9, "transformOriginX": 9, "transformOriginY": 9, "userSelect": 11, "wrapFlow": 11, "wrapThrough": 11, "wrapMargin": 11, "scrollSnapType": 11, "scrollSnapPointsX": 11, "scrollSnapPointsY": 11, "scrollSnapDestination": 11, "scrollSnapCoordinate": 11, "touchAction": 10, "hyphens": 11, "flowInto": 11, "flowFrom": 11, "breakBefore": 11, "breakAfter": 11, "breakInside": 11, "regionFragment": 11, "gridTemplateColumns": 11, "gridTemplateRows": 11, "gridTemplateAreas": 11, "gridTemplate": 11, "gridAutoColumns": 11, "gridAutoRows": 11, "gridAutoFlow": 11, "grid": 11, "gridRowStart": 11, "gridColumnStart": 11, "gridRowEnd": 11, "gridRow": 11, "gridColumn": 11, "gridColumnEnd": 11, "gridColumnGap": 11, "gridRowGap": 11, "gridArea": 11, "gridGap": 11, "textSizeAdjust": 11 }, "edge": { "userSelect": 14, "wrapFlow": 14, "wrapThrough": 14, "wrapMargin": 14, "scrollSnapType": 14, "scrollSnapPointsX": 14, "scrollSnapPointsY": 14, "scrollSnapDestination": 14, "scrollSnapCoordinate": 14, "hyphens": 14, "flowInto": 14, "flowFrom": 14, "breakBefore": 14, "breakAfter": 14, "breakInside": 14, "regionFragment": 14, "gridTemplateColumns": 14, "gridTemplateRows": 14, "gridTemplateAreas": 14, "gridTemplate": 14, "gridAutoColumns": 14, "gridAutoRows": 14, "gridAutoFlow": 14, "grid": 14, "gridRowStart": 14, "gridColumnStart": 14, "gridRowEnd": 14, "gridRow": 14, "gridColumn": 14, "gridColumnEnd": 14, "gridColumnGap": 14, "gridRowGap": 14, "gridArea": 14, "gridGap": 14 }, "ios_saf": { "flex": 8.1, "flexBasis": 8.1, "flexDirection": 8.1, "flexGrow": 8.1, "flexFlow": 8.1, "flexShrink": 8.1, "flexWrap": 8.1, "alignContent": 8.1, "alignItems": 8.1, "alignSelf": 8.1, "justifyContent": 8.1, "order": 8.1, "transition": 6, "transitionDelay": 6, "transitionDuration": 6, "transitionProperty": 6, "transitionTimingFunction": 6, "transform": 8.1, "transformOrigin": 8.1, "transformOriginX": 8.1, "transformOriginY": 8.1, "backfaceVisibility": 8.1, "perspective": 8.1, "perspectiveOrigin": 8.1, "transformStyle": 8.1, "transformOriginZ": 8.1, "animation": 8.1, "animationDelay": 8.1, "animationDirection": 8.1, "animationFillMode": 8.1, "animationDuration": 8.1, "animationIterationCount": 8.1, "animationName": 8.1, "animationPlayState": 8.1, "animationTimingFunction": 8.1, "appearance": 9.3, "userSelect": 9.3, "backdropFilter": 9.3, "fontKerning": 9.3, "scrollSnapType": 9.3, "scrollSnapPointsX": 9.3, "scrollSnapPointsY": 9.3, "scrollSnapDestination": 9.3, "scrollSnapCoordinate": 9.3, "boxDecorationBreak": 9.3, "clipPath": 9.3, "maskImage": 9.3, "maskMode": 9.3, "maskRepeat": 9.3, "maskPosition": 9.3, "maskClip": 9.3, "maskOrigin": 9.3, "maskSize": 9.3, "maskComposite": 9.3, "mask": 9.3, "maskBorderSource": 9.3, "maskBorderMode": 9.3, "maskBorderSlice": 9.3, "maskBorderWidth": 9.3, "maskBorderOutset": 9.3, "maskBorderRepeat": 9.3, "maskBorder": 9.3, "maskType": 9.3, "textSizeAdjust": 9.3, "textDecorationStyle": 9.3, "textDecorationSkip": 9.3, "textDecorationLine": 9.3, "textDecorationColor": 9.3, "shapeImageThreshold": 9.3, "shapeImageMargin": 9.3, "shapeImageOutside": 9.3, "filter": 9, "hyphens": 9.3, "flowInto": 9.3, "flowFrom": 9.3, "breakBefore": 8.1, "breakAfter": 8.1, "breakInside": 8.1, "regionFragment": 9.3, "columnCount": 8.1, "columnFill": 8.1, "columnGap": 8.1, "columnRule": 8.1, "columnRuleColor": 8.1, "columnRuleStyle": 8.1, "columnRuleWidth": 8.1, "columns": 8.1, "columnSpan": 8.1, "columnWidth": 8.1 }, "android": { "flex": 4.2, "flexBasis": 4.2, "flexDirection": 4.2, "flexGrow": 4.2, "flexFlow": 4.2, "flexShrink": 4.2, "flexWrap": 4.2, "alignContent": 4.2, "alignItems": 4.2, "alignSelf": 4.2, "justifyContent": 4.2, "order": 4.2, "transition": 4.2, "transitionDelay": 4.2, "transitionDuration": 4.2, "transitionProperty": 4.2, "transitionTimingFunction": 4.2, "transform": 4.4, "transformOrigin": 4.4, "transformOriginX": 4.4, "transformOriginY": 4.4, "backfaceVisibility": 4.4, "perspective": 4.4, "perspectiveOrigin": 4.4, "transformStyle": 4.4, "transformOriginZ": 4.4, "animation": 4.4, "animationDelay": 4.4, "animationDirection": 4.4, "animationFillMode": 4.4, "animationDuration": 4.4, "animationIterationCount": 4.4, "animationName": 4.4, "animationPlayState": 4.4, "animationTimingFunction": 4.4, "appearance": 51, "userSelect": 51, "fontKerning": 4.4, "textEmphasisPosition": 51, "textEmphasis": 51, "textEmphasisStyle": 51, "textEmphasisColor": 51, "boxDecorationBreak": 51, "clipPath": 51, "maskImage": 51, "maskMode": 51, "maskRepeat": 51, "maskPosition": 51, "maskClip": 51, "maskOrigin": 51, "maskSize": 51, "maskComposite": 51, "mask": 51, "maskBorderSource": 51, "maskBorderMode": 51, "maskBorderSlice": 51, "maskBorderWidth": 51, "maskBorderOutset": 51, "maskBorderRepeat": 51, "maskBorder": 51, "maskType": 51, "filter": 51, "fontFeatureSettings": 4.4, "breakAfter": 51, "breakBefore": 51, "breakInside": 51, "columnCount": 51, "columnFill": 51, "columnGap": 51, "columnRule": 51, "columnRuleColor": 51, "columnRuleStyle": 51, "columnRuleWidth": 51, "columns": 51, "columnSpan": 51, "columnWidth": 51 }, "and_chr": { "appearance": 51, "userSelect": 51, "textEmphasisPosition": 51, "textEmphasis": 51, "textEmphasisStyle": 51, "textEmphasisColor": 51, "boxDecorationBreak": 51, "clipPath": 51, "maskImage": 51, "maskMode": 51, "maskRepeat": 51, "maskPosition": 51, "maskClip": 51, "maskOrigin": 51, "maskSize": 51, "maskComposite": 51, "mask": 51, "maskBorderSource": 51, "maskBorderMode": 51, "maskBorderSlice": 51, "maskBorderWidth": 51, "maskBorderOutset": 51, "maskBorderRepeat": 51, "maskBorder": 51, "maskType": 51, "textDecorationStyle": 51, "textDecorationSkip": 51, "textDecorationLine": 51, "textDecorationColor": 51, "filter": 51 }, "and_uc": { "flex": 9.9, "flexBasis": 9.9, "flexDirection": 9.9, "flexGrow": 9.9, "flexFlow": 9.9, "flexShrink": 9.9, "flexWrap": 9.9, "alignContent": 9.9, "alignItems": 9.9, "alignSelf": 9.9, "justifyContent": 9.9, "order": 9.9, "transition": 9.9, "transitionDelay": 9.9, "transitionDuration": 9.9, "transitionProperty": 9.9, "transitionTimingFunction": 9.9, "transform": 9.9, "transformOrigin": 9.9, "transformOriginX": 9.9, "transformOriginY": 9.9, "backfaceVisibility": 9.9, "perspective": 9.9, "perspectiveOrigin": 9.9, "transformStyle": 9.9, "transformOriginZ": 9.9, "animation": 9.9, "animationDelay": 9.9, "animationDirection": 9.9, "animationFillMode": 9.9, "animationDuration": 9.9, "animationIterationCount": 9.9, "animationName": 9.9, "animationPlayState": 9.9, "animationTimingFunction": 9.9, "appearance": 9.9, "userSelect": 9.9, "fontKerning": 9.9, "textEmphasisPosition": 9.9, "textEmphasis": 9.9, "textEmphasisStyle": 9.9, "textEmphasisColor": 9.9, "maskImage": 9.9, "maskMode": 9.9, "maskRepeat": 9.9, "maskPosition": 9.9, "maskClip": 9.9, "maskOrigin": 9.9, "maskSize": 9.9, "maskComposite": 9.9, "mask": 9.9, "maskBorderSource": 9.9, "maskBorderMode": 9.9, "maskBorderSlice": 9.9, "maskBorderWidth": 9.9, "maskBorderOutset": 9.9, "maskBorderRepeat": 9.9, "maskBorder": 9.9, "maskType": 9.9, "textSizeAdjust": 9.9, "filter": 9.9, "hyphens": 9.9, "flowInto": 9.9, "flowFrom": 9.9, "breakBefore": 9.9, "breakAfter": 9.9, "breakInside": 9.9, "regionFragment": 9.9, "fontFeatureSettings": 9.9, "columnCount": 9.9, "columnFill": 9.9, "columnGap": 9.9, "columnRule": 9.9, "columnRuleColor": 9.9, "columnRuleStyle": 9.9, "columnRuleWidth": 9.9, "columns": 9.9, "columnSpan": 9.9, "columnWidth": 9.9 }, "op_mini": {} };

    var getPrefixedValue = (function (prefixedValue, value, keepUnprefixed) {
      return keepUnprefixed ? [prefixedValue, value] : prefixedValue;
    });

    function position$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var browser = _ref.browserInfo.browser;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if (property === 'position' && value === 'sticky' && (browser === 'safari' || browser === 'ios_saf')) {
        return babelHelpers.defineProperty({}, property, getPrefixedValue(css + value, value, keepUnprefixed));
      }
    }

    function calc$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if (typeof value === 'string' && value.indexOf('calc(') > -1 && (browser === 'firefox' && version < 15 || browser === 'chrome' && version < 25 || browser === 'safari' && version < 6.1 || browser === 'ios_saf' && version < 7)) {
        return babelHelpers.defineProperty({}, property, getPrefixedValue(value.replace(/calc\(/g, css + 'calc('), value, keepUnprefixed));
      }
    }

    var values$4 = { 'zoom-in': true, 'zoom-out': true };

    function zoomCursor(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if (property === 'cursor' && values$4[value] && (browser === 'firefox' && version < 24 || browser === 'chrome' && version < 37 || browser === 'safari' && version < 9 || browser === 'opera' && version < 24)) {
        return {
          cursor: getPrefixedValue(css + value, value, keepUnprefixed)
        };
      }
    }

    var values$5 = { grab: true, grabbing: true };

    function grabCursor(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var browser = _ref.browserInfo.browser;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      // adds prefixes for firefox, chrome, safari, and opera regardless of version until a reliable brwoser support info can be found (see: https://github.com/rofrischmann/inline-style-prefixer/issues/79)
      if (property === 'cursor' && values$5[value] && (browser === 'firefox' || browser === 'chrome' || browser === 'safari' || browser === 'opera')) {
        return {
          cursor: getPrefixedValue(css + value, value, keepUnprefixed)
        };
      }
    }

    var values$6 = { flex: true, 'inline-flex': true };

    function flex$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if (property === 'display' && values$6[value] && (browser === 'chrome' && version < 29 && version > 20 || (browser === 'safari' || browser === 'ios_saf') && version < 9 && version > 6 || browser === 'opera' && (version == 15 || version == 16))) {
        return {
          display: getPrefixedValue(css + value, value, keepUnprefixed)
        };
      }
    }

    var properties$2 = {
      maxHeight: true,
      maxWidth: true,
      width: true,
      height: true,
      columnWidth: true,
      minWidth: true,
      minHeight: true
    };
    var values$7 = {
      'min-content': true,
      'max-content': true,
      'fill-available': true,
      'fit-content': true,
      'contain-floats': true
    };

    function sizing$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      // This might change in the future
      // Keep an eye on it
      if (properties$2[property] && values$7[value]) {
        return babelHelpers.defineProperty({}, property, getPrefixedValue(css + value, value, keepUnprefixed));
      }
    }

    var values$8 = /linear-gradient|radial-gradient|repeating-linear-gradient|repeating-radial-gradient/;

    function gradient$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if (typeof value === 'string' && value.match(values$8) !== null && (browser === 'firefox' && version < 16 || browser === 'chrome' && version < 26 || (browser === 'safari' || browser === 'ios_saf') && version < 7 || (browser === 'opera' || browser === 'op_mini') && version < 12.1 || browser === 'android' && version < 4.4 || browser === 'and_uc')) {
        return babelHelpers.defineProperty({}, property, getPrefixedValue(css + value, value, keepUnprefixed));
      }
    }

    var unprefixProperty = (function (property) {
      var unprefixed = property.replace(/^(ms|Webkit|Moz|O)/, '');
      return unprefixed.charAt(0).toLowerCase() + unprefixed.slice(1);
    });

    var properties$3 = { transition: true, transitionProperty: true };

    function transition$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var css = _ref.prefix.css;
      var requiresPrefix = _ref.requiresPrefix;
      var keepUnprefixed = _ref.keepUnprefixed;

      // also check for already prefixed transitions
      var unprefixedProperty = unprefixProperty(property);

      if (typeof value === 'string' && properties$3[unprefixedProperty]) {
        var _ret = function () {
          // TODO: memoize this array
          var requiresPrefixDashCased = Object.keys(requiresPrefix).map(function (prop) {
            return hyphenateStyleName(prop);
          });

          // only split multi values, not cubic beziers
          var multipleValues = value.split(/,(?![^()]*(?:\([^()]*\))?\))/g);

          requiresPrefixDashCased.forEach(function (prop) {
            multipleValues.forEach(function (val, index) {
              if (val.indexOf(prop) > -1 && prop !== 'order') {
                multipleValues[index] = val.replace(prop, css + prop) + (keepUnprefixed ? ',' + val : '');
              }
            });
          });

          return {
            v: babelHelpers.defineProperty({}, property, multipleValues.join(','))
          };
        }();

        if ((typeof _ret === 'undefined' ? 'undefined' : babelHelpers.typeof(_ret)) === "object") return _ret.v;
      }
    }

    var alternativeValues$2 = {
      'space-around': 'distribute',
      'space-between': 'justify',
      'flex-start': 'start',
      'flex-end': 'end',
      flex: 'flexbox',
      'inline-flex': 'inline-flexbox'
    };
    var alternativeProps$2 = {
      alignContent: 'msFlexLinePack',
      alignSelf: 'msFlexItemAlign',
      alignItems: 'msFlexAlign',
      justifyContent: 'msFlexPack',
      order: 'msFlexOrder',
      flexGrow: 'msFlexPositive',
      flexShrink: 'msFlexNegative',
      flexBasis: 'msPreferredSize'
    };

    function flexboxIE$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var styles = _ref.styles;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if ((alternativeProps$2[property] || property === 'display' && typeof value === 'string' && value.indexOf('flex') > -1) && (browser === 'ie_mob' || browser === 'ie') && version == 10) {
        if (!keepUnprefixed && !Array.isArray(styles[property])) {
          delete styles[property];
        }
        if (property === 'display' && alternativeValues$2[value]) {
          return {
            display: getPrefixedValue(css + alternativeValues$2[value], value, keepUnprefixed)
          };
        }
        if (alternativeProps$2[property]) {
          return babelHelpers.defineProperty({}, alternativeProps$2[property], alternativeValues$2[value] || value);
        }
      }
    }

    var alternativeValues$3 = {
      'space-around': 'justify',
      'space-between': 'justify',
      'flex-start': 'start',
      'flex-end': 'end',
      'wrap-reverse': 'multiple',
      wrap: 'multiple',
      flex: 'box',
      'inline-flex': 'inline-box'
    };

    var alternativeProps$3 = {
      alignItems: 'WebkitBoxAlign',
      justifyContent: 'WebkitBoxPack',
      flexWrap: 'WebkitBoxLines'
    };

    var otherProps = ['alignContent', 'alignSelf', 'order', 'flexGrow', 'flexShrink', 'flexBasis', 'flexDirection'];
    var properties$4 = Object.keys(alternativeProps$3).concat(otherProps);

    function flexboxOld$1(_ref) {
      var property = _ref.property;
      var value = _ref.value;
      var styles = _ref.styles;
      var _ref$browserInfo = _ref.browserInfo;
      var browser = _ref$browserInfo.browser;
      var version = _ref$browserInfo.version;
      var css = _ref.prefix.css;
      var keepUnprefixed = _ref.keepUnprefixed;

      if ((properties$4.indexOf(property) > -1 || property === 'display' && typeof value === 'string' && value.indexOf('flex') > -1) && (browser === 'firefox' && version < 22 || browser === 'chrome' && version < 21 || (browser === 'safari' || browser === 'ios_saf') && version <= 6.1 || browser === 'android' && version < 4.4 || browser === 'and_uc')) {
        if (!keepUnprefixed && !Array.isArray(styles[property])) {
          delete styles[property];
        }
        if (property === 'flexDirection' && typeof value === 'string') {
          return {
            WebkitBoxOrient: value.indexOf('column') > -1 ? 'vertical' : 'horizontal',
            WebkitBoxDirection: value.indexOf('reverse') > -1 ? 'reverse' : 'normal'
          };
        }
        if (property === 'display' && alternativeValues$3[value]) {
          return {
            display: getPrefixedValue(css + alternativeValues$3[value], value, keepUnprefixed)
          };
        }
        if (alternativeProps$3[property]) {
          return babelHelpers.defineProperty({}, alternativeProps$3[property], alternativeValues$3[value] || value);
        }
      }
    }

    var plugins = [position$1, calc$1, zoomCursor, grabCursor, sizing$1, gradient$1, transition$1, flexboxIE$1, flexboxOld$1,
    // this must be run AFTER the flexbox specs
    flex$1];

    var Prefixer = function () {
      /**
       * Instantiante a new prefixer
       * @param {string} userAgent - userAgent to gather prefix information according to caniuse.com
       * @param {string} keepUnprefixed - keeps unprefixed properties and values
       */

      function Prefixer() {
        var _this = this;

        var options = arguments.length <= 0 || arguments[0] === undefined ? {} : arguments[0];
        babelHelpers.classCallCheck(this, Prefixer);

        var defaultUserAgent = typeof navigator !== 'undefined' ? navigator.userAgent : undefined;

        this._userAgent = options.userAgent || defaultUserAgent;
        this._keepUnprefixed = options.keepUnprefixed || false;

        this._browserInfo = getBrowserInformation(this._userAgent);

        // Checks if the userAgent was resolved correctly
        if (this._browserInfo && this._browserInfo.prefix) {
          // set additional prefix information
          this.cssPrefix = this._browserInfo.prefix.css;
          this.jsPrefix = this._browserInfo.prefix.inline;
          this.prefixedKeyframes = getPrefixedKeyframes(this._browserInfo);
        } else {
          this._usePrefixAllFallback = true;
          return false;
        }

        var data = this._browserInfo.browser && prefixProps$1[this._browserInfo.browser];
        if (data) {
          this._requiresPrefix = Object.keys(data).filter(function (key) {
            return data[key] >= _this._browserInfo.version;
          }).reduce(function (result, name) {
            result[name] = true;
            return result;
          }, {});
          this._hasPropsRequiringPrefix = Object.keys(this._requiresPrefix).length > 0;
        } else {
          this._usePrefixAllFallback = true;
        }
      }

      /**
       * Returns a prefixed version of the style object
       * @param {Object} styles - Style object that gets prefixed properties added
       * @returns {Object} - Style object with prefixed properties and values
       */


      babelHelpers.createClass(Prefixer, [{
        key: 'prefix',
        value: function prefix(styles) {
          var _this2 = this;

          // use prefixAll as fallback if userAgent can not be resolved
          if (this._usePrefixAllFallback) {
            return prefixAll(styles);
          }

          // only add prefixes if needed
          if (!this._hasPropsRequiringPrefix) {
            return styles;
          }

          Object.keys(styles).forEach(function (property) {
            var value = styles[property];
            if (value instanceof Object && !Array.isArray(value)) {
              // recurse through nested style objects
              styles[property] = _this2.prefix(value);
            } else {
              // add prefixes if needed
              if (_this2._requiresPrefix[property]) {
                styles[_this2.jsPrefix + capitalizeString(property)] = value;
                if (!_this2._keepUnprefixed) {
                  delete styles[property];
                }
              }
            }
          });

          Object.keys(styles).forEach(function (property) {
            [].concat(styles[property]).forEach(function (value) {
              // resolve plugins
              plugins.forEach(function (plugin) {
                // generates a new plugin interface with current data
                assignStyles(styles, plugin({
                  property: property,
                  value: value,
                  styles: styles,
                  browserInfo: _this2._browserInfo,
                  prefix: {
                    js: _this2.jsPrefix,
                    css: _this2.cssPrefix,
                    keyframes: _this2.prefixedKeyframes
                  },
                  keepUnprefixed: _this2._keepUnprefixed,
                  requiresPrefix: _this2._requiresPrefix
                }), value, _this2._keepUnprefixed);
              });
            });
          });

          return sortPrefixedStyle(styles);
        }

        /**
         * Returns a prefixed version of the style object using all vendor prefixes
         * @param {Object} styles - Style object that gets prefixed properties added
         * @returns {Object} - Style object with prefixed properties and values
         */

      }], [{
        key: 'prefixAll',
        value: function prefixAll$$(styles) {
          return prefixAll(styles);
        }
      }]);
      return Prefixer;
    }();

    function assignStyles(base) {
      var extend = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];
      var value = arguments[2];
      var keepUnprefixed = arguments[3];

      Object.keys(extend).forEach(function (property) {
        var baseValue = base[property];
        if (Array.isArray(baseValue)) {
          [].concat(extend[property]).forEach(function (val) {
            if (base[property].indexOf(val) === -1) {
              base[property].splice(baseValue.indexOf(value), keepUnprefixed ? 0 : 1, val);
            }
          });
        } else {
          base[property] = extend[property];
        }
      });
    }

    return Prefixer;

}));
//# sourceMappingURL=inline-style-prefixer.js.map