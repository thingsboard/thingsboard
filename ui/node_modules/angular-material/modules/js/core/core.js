/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
(function( window, angular, undefined ){
"use strict";

/**
 * Initialization function that validates environment
 * requirements.
 */
DetectNgTouch['$inject'] = ["$log", "$injector"];
MdCoreConfigure['$inject'] = ["$provide", "$mdThemingProvider"];
rAFDecorator['$inject'] = ["$delegate"];
qDecorator['$inject'] = ["$delegate"];
angular
  .module('material.core', [
    'ngAnimate',
    'material.core.animate',
    'material.core.layout',
    'material.core.interaction',
    'material.core.gestures',
    'material.core.theming'
  ])
  .config(MdCoreConfigure)
  .run(DetectNgTouch);


/**
 * Detect if the ng-Touch module is also being used.
 * Warn if detected.
 * ngInject
 */
function DetectNgTouch($log, $injector) {
  if ($injector.has('$swipe')) {
    var msg = "" +
      "You are using the ngTouch module. \n" +
      "AngularJS Material already has mobile click, tap, and swipe support... \n" +
      "ngTouch is not supported with AngularJS Material!";
    $log.warn(msg);
  }
}

/**
 * ngInject
 */
function MdCoreConfigure($provide, $mdThemingProvider) {

  $provide.decorator('$$rAF', ['$delegate', rAFDecorator]);
  $provide.decorator('$q', ['$delegate', qDecorator]);

  $mdThemingProvider.theme('default')
    .primaryPalette('indigo')
    .accentPalette('pink')
    .warnPalette('deep-orange')
    .backgroundPalette('grey');
}

/**
 * ngInject
 */
function rAFDecorator($delegate) {
  /**
   * Use this to throttle events that come in often.
   * The throttled function will always use the *last* invocation before the
   * coming frame.
   *
   * For example, window resize events that fire many times a second:
   * If we set to use an raf-throttled callback on window resize, then
   * our callback will only be fired once per frame, with the last resize
   * event that happened before that frame.
   *
   * @param {function} callback function to debounce
   */
  $delegate.throttle = function(cb) {
    var queuedArgs, alreadyQueued, queueCb, context;
    return function debounced() {
      queuedArgs = arguments;
      context = this;
      queueCb = cb;
      if (!alreadyQueued) {
        alreadyQueued = true;
        $delegate(function() {
          queueCb.apply(context, Array.prototype.slice.call(queuedArgs));
          alreadyQueued = false;
        });
      }
    };
  };
  return $delegate;
}

/**
 * ngInject
 */
function qDecorator($delegate) {
  /**
   * Adds a shim for $q.resolve for AngularJS version that don't have it,
   * so we don't have to think about it.
   *
   * via https://github.com/angular/angular.js/pull/11987
   */

  // TODO(crisbeto): this won't be necessary once we drop AngularJS 1.3
  if (!$delegate.resolve) {
    $delegate.resolve = $delegate.when;
  }
  return $delegate;
}


MdAutofocusDirective['$inject'] = ["$parse"];angular.module('material.core')
  .directive('mdAutofocus', MdAutofocusDirective)

  // Support the deprecated md-auto-focus and md-sidenav-focus as well
  .directive('mdAutoFocus', MdAutofocusDirective)
  .directive('mdSidenavFocus', MdAutofocusDirective);

/**
 * @ngdoc directive
 * @name mdAutofocus
 * @module material.core.util
 *
 * @description
 *
 * `[md-autofocus]` provides an optional way to identify the focused element when a `$mdDialog`,
 * `$mdBottomSheet`, `$mdMenu` or `$mdSidenav` opens or upon page load for input-like elements.
 *
 * When one of these opens, it will find the first nested element with the `[md-autofocus]`
 * attribute directive and optional expression. An expression may be specified as the directive
 * value to enable conditional activation of the autofocus.
 *
 * @usage
 *
 * ### Dialog
 * <hljs lang="html">
 * <md-dialog>
 *   <form>
 *     <md-input-container>
 *       <label for="testInput">Label</label>
 *       <input id="testInput" type="text" md-autofocus>
 *     </md-input-container>
 *   </form>
 * </md-dialog>
 * </hljs>
 *
 * ### Bottomsheet
 * <hljs lang="html">
 * <md-bottom-sheet class="md-list md-has-header">
 *  <md-subheader>Comment Actions</md-subheader>
 *  <md-list>
 *    <md-list-item ng-repeat="item in items">
 *
 *      <md-button md-autofocus="$index == 2">
 *        <md-icon md-svg-src="{{item.icon}}"></md-icon>
 *        <span class="md-inline-list-icon-label">{{ item.name }}</span>
 *      </md-button>
 *
 *    </md-list-item>
 *  </md-list>
 * </md-bottom-sheet>
 * </hljs>
 *
 * ### Autocomplete
 * <hljs lang="html">
 *   <md-autocomplete
 *       md-autofocus
 *       md-selected-item="selectedItem"
 *       md-search-text="searchText"
 *       md-items="item in getMatches(searchText)"
 *       md-item-text="item.display">
 *     <span md-highlight-text="searchText">{{item.display}}</span>
 *   </md-autocomplete>
 * </hljs>
 *
 * ### Sidenav
 * <hljs lang="html">
 * <div layout="row" ng-controller="MyController">
 *   <md-sidenav md-component-id="left" class="md-sidenav-left">
 *     Left Nav!
 *   </md-sidenav>
 *
 *   <md-content>
 *     Center Content
 *     <md-button ng-click="openLeftMenu()">
 *       Open Left Menu
 *     </md-button>
 *   </md-content>
 *
 *   <md-sidenav md-component-id="right"
 *     md-is-locked-open="$mdMedia('min-width: 333px')"
 *     class="md-sidenav-right">
 *     <form>
 *       <md-input-container>
 *         <label for="testInput">Test input</label>
 *         <input id="testInput" type="text"
 *                ng-model="data" md-autofocus>
 *       </md-input-container>
 *     </form>
 *   </md-sidenav>
 * </div>
 * </hljs>
 **/
function MdAutofocusDirective($parse) {
  return {
    restrict: 'A',
    link: {
      pre: preLink
    }
  };

  function preLink(scope, element, attr) {
    var attrExp = attr.mdAutoFocus || attr.mdAutofocus || attr.mdSidenavFocus;

    // Initially update the expression by manually parsing the expression as per $watch source.
    updateExpression($parse(attrExp)(scope));

    // Only watch the expression if it is not empty.
    if (attrExp) {
      scope.$watch(attrExp, updateExpression);
    }

    /**
     * Updates the autofocus class which is used to determine whether the attribute
     * expression evaluates to true or false.
     * @param {string|boolean} value Attribute Value
     */
    function updateExpression(value) {

      // Rather than passing undefined to the jqLite toggle class function we explicitly set the
      // value to true. Otherwise the class will be just toggled instead of being forced.
      if (angular.isUndefined(value)) {
        value = true;
      }

      element.toggleClass('md-autofocus', !!value);
    }
  }

}

/**
 * @ngdoc module
 * @name material.core.colorUtil
 * @description
 * Color Util
 */
angular
  .module('material.core')
  .factory('$mdColorUtil', ColorUtilFactory);

function ColorUtilFactory() {
  /**
   * Converts hex value to RGBA string
   * @param color {string}
   * @returns {string}
   */
  function hexToRgba (color) {
    var hex   = color[ 0 ] === '#' ? color.substr(1) : color,
      dig   = hex.length / 3,
      red   = hex.substr(0, dig),
      green = hex.substr(dig, dig),
      blue  = hex.substr(dig * 2);
    if (dig === 1) {
      red += red;
      green += green;
      blue += blue;
    }
    return 'rgba(' + parseInt(red, 16) + ',' + parseInt(green, 16) + ',' + parseInt(blue, 16) + ',0.1)';
  }

  /**
   * Converts rgba value to hex string
   * @param {string} color
   * @returns {string}
   */
  function rgbaToHex(color) {
    color = color.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);

    var hex = (color && color.length === 4) ? "#" +
    ("0" + parseInt(color[1],10).toString(16)).slice(-2) +
    ("0" + parseInt(color[2],10).toString(16)).slice(-2) +
    ("0" + parseInt(color[3],10).toString(16)).slice(-2) : '';

    return hex.toUpperCase();
  }

  /**
   * Converts an RGB color to RGBA
   * @param {string} color
   * @returns {string}
   */
  function rgbToRgba (color) {
    return color.replace(')', ', 0.1)').replace('(', 'a(');
  }

  /**
   * Converts an RGBA color to RGB
   * @param {string} color
   * @returns {string}
   */
  function rgbaToRgb (color) {
    return color
      ? color.replace('rgba', 'rgb').replace(/,[^),]+\)/, ')')
      : 'rgb(0,0,0)';
  }

  return {
    rgbaToHex: rgbaToHex,
    hexToRgba: hexToRgba,
    rgbToRgba: rgbToRgba,
    rgbaToRgb: rgbaToRgb
  };
}

angular.module('material.core')
.factory('$mdConstant', MdConstantFactory);

/**
 * Factory function that creates the grab-bag $mdConstant service.
 * ngInject
 */
function MdConstantFactory() {

  var prefixTestEl = document.createElement('div');
  var vendorPrefix = getVendorPrefix(prefixTestEl);
  var isWebkit = /webkit/i.test(vendorPrefix);
  var SPECIAL_CHARS_REGEXP = /([:\-_]+(.))/g;

  function vendorProperty(name) {
    // Add a dash between the prefix and name, to be able to transform the string into camelcase.
    var prefixedName = vendorPrefix + '-' + name;
    var ucPrefix = camelCase(prefixedName);
    var lcPrefix = ucPrefix.charAt(0).toLowerCase() + ucPrefix.substring(1);

    return hasStyleProperty(prefixTestEl, name)     ? name     :       // The current browser supports the un-prefixed property
           hasStyleProperty(prefixTestEl, ucPrefix) ? ucPrefix :       // The current browser only supports the prefixed property.
           hasStyleProperty(prefixTestEl, lcPrefix) ? lcPrefix : name; // Some browsers are only supporting the prefix in lowercase.
  }

  function hasStyleProperty(testElement, property) {
    return angular.isDefined(testElement.style[property]);
  }

  function camelCase(input) {
    return input.replace(SPECIAL_CHARS_REGEXP, function(matches, separator, letter, offset) {
      return offset ? letter.toUpperCase() : letter;
    });
  }

  function getVendorPrefix(testElement) {
    var prop, match;
    var vendorRegex = /^(Moz|webkit|ms)(?=[A-Z])/;

    for (prop in testElement.style) {
      if (match = vendorRegex.exec(prop)) {
        return match[0];
      }
    }
  }

  var self = {
    isInputKey : function(e) { return (e.keyCode >= 31 && e.keyCode <= 90); },
    isNumPadKey : function(e) { return (3 === e.location && e.keyCode >= 97 && e.keyCode <= 105); },
    isMetaKey: function(e) { return (e.keyCode >= 91 && e.keyCode <= 93); },
    isFnLockKey: function(e) { return (e.keyCode >= 112 && e.keyCode <= 145); },
    isNavigationKey : function(e) {
      var kc = self.KEY_CODE, NAVIGATION_KEYS =  [kc.SPACE, kc.ENTER, kc.UP_ARROW, kc.DOWN_ARROW];
      return (NAVIGATION_KEYS.indexOf(e.keyCode) != -1);
    },
    hasModifierKey: function(e) {
      return e.ctrlKey || e.metaKey || e.altKey;
    },

    /**
     * Maximum size, in pixels, that can be explicitly set to an element. The actual value varies
     * between browsers, but IE11 has the very lowest size at a mere 1,533,917px. Ideally we could
     * compute this value, but Firefox always reports an element to have a size of zero if it
     * goes over the max, meaning that we'd have to binary search for the value.
     */
    ELEMENT_MAX_PIXELS: 1533917,

    /**
     * Priority for a directive that should run before the directives from ngAria.
     */
    BEFORE_NG_ARIA: 210,

    /**
     * Common Keyboard actions and their associated keycode.
     */
    KEY_CODE: {
      COMMA: 188,
      SEMICOLON : 186,
      ENTER: 13,
      ESCAPE: 27,
      SPACE: 32,
      PAGE_UP: 33,
      PAGE_DOWN: 34,
      END: 35,
      HOME: 36,
      LEFT_ARROW : 37,
      UP_ARROW : 38,
      RIGHT_ARROW : 39,
      DOWN_ARROW : 40,
      TAB : 9,
      BACKSPACE: 8,
      DELETE: 46
    },

    /**
     * Vendor prefixed CSS properties to be used to support the given functionality in older browsers
     * as well.
     */
    CSS: {
      /* Constants */
      TRANSITIONEND: 'transitionend' + (isWebkit ? ' webkitTransitionEnd' : ''),
      ANIMATIONEND: 'animationend' + (isWebkit ? ' webkitAnimationEnd' : ''),

      TRANSFORM: vendorProperty('transform'),
      TRANSFORM_ORIGIN: vendorProperty('transformOrigin'),
      TRANSITION: vendorProperty('transition'),
      TRANSITION_DURATION: vendorProperty('transitionDuration'),
      ANIMATION_PLAY_STATE: vendorProperty('animationPlayState'),
      ANIMATION_DURATION: vendorProperty('animationDuration'),
      ANIMATION_NAME: vendorProperty('animationName'),
      ANIMATION_TIMING: vendorProperty('animationTimingFunction'),
      ANIMATION_DIRECTION: vendorProperty('animationDirection')
    },

    /**
     * As defined in core/style/variables.scss
     *
     * $layout-breakpoint-xs:     600px !default;
     * $layout-breakpoint-sm:     960px !default;
     * $layout-breakpoint-md:     1280px !default;
     * $layout-breakpoint-lg:     1920px !default;
     *
     */
    MEDIA: {
      'xs'        : '(max-width: 599px)'                         ,
      'gt-xs'     : '(min-width: 600px)'                         ,
      'sm'        : '(min-width: 600px) and (max-width: 959px)'  ,
      'gt-sm'     : '(min-width: 960px)'                         ,
      'md'        : '(min-width: 960px) and (max-width: 1279px)' ,
      'gt-md'     : '(min-width: 1280px)'                        ,
      'lg'        : '(min-width: 1280px) and (max-width: 1919px)',
      'gt-lg'     : '(min-width: 1920px)'                        ,
      'xl'        : '(min-width: 1920px)'                        ,
      'landscape' : '(orientation: landscape)'                   ,
      'portrait'  : '(orientation: portrait)'                    ,
      'print' : 'print'
    },

    MEDIA_PRIORITY: [
      'xl',
      'gt-lg',
      'lg',
      'gt-md',
      'md',
      'gt-sm',
      'sm',
      'gt-xs',
      'xs',
      'landscape',
      'portrait',
      'print'
    ]
  };

  return self;
}

  angular
    .module('material.core')
    .config(["$provide", function($provide){
       $provide.decorator('$mdUtil', ['$delegate', function ($delegate){
           /**
            * Inject the iterator facade to easily support iteration and accessors
            * @see iterator below
            */
           $delegate.iterator = MdIterator;

           return $delegate;
         }
       ]);
     }]);

  /**
   * iterator is a list facade to easily support iteration and accessors
   *
   * @param items Array list which this iterator will enumerate
   * @param reloop Boolean enables iterator to consider the list as an endless reloop
   */
  function MdIterator(items, reloop) {
    var trueFn = function() { return true; };

    if (items && !angular.isArray(items)) {
      items = Array.prototype.slice.call(items);
    }

    reloop = !!reloop;
    var _items = items || [];

    // Published API
    return {
      items: getItems,
      count: count,

      inRange: inRange,
      contains: contains,
      indexOf: indexOf,
      itemAt: itemAt,

      findBy: findBy,

      add: add,
      remove: remove,

      first: first,
      last: last,
      next: angular.bind(null, findSubsequentItem, false),
      previous: angular.bind(null, findSubsequentItem, true),

      hasPrevious: hasPrevious,
      hasNext: hasNext

    };

    /**
     * Publish copy of the enumerable set
     * @returns {Array|*}
     */
    function getItems() {
      return [].concat(_items);
    }

    /**
     * Determine length of the list
     * @returns {Array.length|*|number}
     */
    function count() {
      return _items.length;
    }

    /**
     * Is the index specified valid
     * @param index
     * @returns {Array.length|*|number|boolean}
     */
    function inRange(index) {
      return _items.length && (index > -1) && (index < _items.length);
    }

    /**
     * Can the iterator proceed to the next item in the list; relative to
     * the specified item.
     *
     * @param item
     * @returns {Array.length|*|number|boolean}
     */
    function hasNext(item) {
      return item ? inRange(indexOf(item) + 1) : false;
    }

    /**
     * Can the iterator proceed to the previous item in the list; relative to
     * the specified item.
     *
     * @param item
     * @returns {Array.length|*|number|boolean}
     */
    function hasPrevious(item) {
      return item ? inRange(indexOf(item) - 1) : false;
    }

    /**
     * Get item at specified index/position
     * @param index
     * @returns {*}
     */
    function itemAt(index) {
      return inRange(index) ? _items[index] : null;
    }

    /**
     * Find all elements matching the key/value pair
     * otherwise return null
     *
     * @param val
     * @param key
     *
     * @return array
     */
    function findBy(key, val) {
      return _items.filter(function(item) {
        return item[key] === val;
      });
    }

    /**
     * Add item to list
     * @param item
     * @param index
     * @returns {*}
     */
    function add(item, index) {
      if (!item) return -1;

      if (!angular.isNumber(index)) {
        index = _items.length;
      }

      _items.splice(index, 0, item);

      return indexOf(item);
    }

    /**
     * Remove item from list...
     * @param item
     */
    function remove(item) {
      if (contains(item)){
        _items.splice(indexOf(item), 1);
      }
    }

    /**
     * Get the zero-based index of the target item
     * @param item
     * @returns {*}
     */
    function indexOf(item) {
      return _items.indexOf(item);
    }

    /**
     * Boolean existence check
     * @param item
     * @returns {boolean}
     */
    function contains(item) {
      return item && (indexOf(item) > -1);
    }

    /**
     * Return first item in the list
     * @returns {*}
     */
    function first() {
      return _items.length ? _items[0] : null;
    }

    /**
     * Return last item in the list...
     * @returns {*}
     */
    function last() {
      return _items.length ? _items[_items.length - 1] : null;
    }

    /**
     * Find the next item. If reloop is true and at the end of the list, it will go back to the
     * first item. If given, the `validate` callback will be used to determine whether the next item
     * is valid. If not valid, it will try to find the next item again.
     *
     * @param {boolean} backwards Specifies the direction of searching (forwards/backwards)
     * @param {*} item The item whose subsequent item we are looking for
     * @param {Function=} validate The `validate` function
     * @param {integer=} limit The recursion limit
     *
     * @returns {*} The subsequent item or null
     */
    function findSubsequentItem(backwards, item, validate, limit) {
      validate = validate || trueFn;

      var curIndex = indexOf(item);
      while (true) {
        if (!inRange(curIndex)) return null;

        var nextIndex = curIndex + (backwards ? -1 : 1);
        var foundItem = null;
        if (inRange(nextIndex)) {
          foundItem = _items[nextIndex];
        } else if (reloop) {
          foundItem = backwards ? last() : first();
          nextIndex = indexOf(foundItem);
        }

        if ((foundItem === null) || (nextIndex === limit)) return null;
        if (validate(foundItem)) return foundItem;

        if (angular.isUndefined(limit)) limit = nextIndex;

        curIndex = nextIndex;
      }
    }
  }



mdMediaFactory['$inject'] = ["$mdConstant", "$rootScope", "$window"];angular.module('material.core')
.factory('$mdMedia', mdMediaFactory);

/**
 * @ngdoc service
 * @name $mdMedia
 * @module material.core
 *
 * @description
 * `$mdMedia` is used to evaluate whether a given media query is true or false given the
 * current device's screen / window size. The media query will be re-evaluated on resize, allowing
 * you to register a watch.
 *
 * `$mdMedia` also has pre-programmed support for media queries that match the layout breakpoints:
 *
 *  <table class="md-api-table">
 *    <thead>
 *    <tr>
 *      <th>Breakpoint</th>
 *      <th>mediaQuery</th>
 *    </tr>
 *    </thead>
 *    <tbody>
 *    <tr>
 *      <td>xs</td>
 *      <td>(max-width: 599px)</td>
 *    </tr>
 *    <tr>
 *      <td>gt-xs</td>
 *      <td>(min-width: 600px)</td>
 *    </tr>
 *    <tr>
 *      <td>sm</td>
 *      <td>(min-width: 600px) and (max-width: 959px)</td>
 *    </tr>
 *    <tr>
 *      <td>gt-sm</td>
 *      <td>(min-width: 960px)</td>
 *    </tr>
 *    <tr>
 *      <td>md</td>
 *      <td>(min-width: 960px) and (max-width: 1279px)</td>
 *    </tr>
 *    <tr>
 *      <td>gt-md</td>
 *      <td>(min-width: 1280px)</td>
 *    </tr>
 *    <tr>
 *      <td>lg</td>
 *      <td>(min-width: 1280px) and (max-width: 1919px)</td>
 *    </tr>
 *    <tr>
 *      <td>gt-lg</td>
 *      <td>(min-width: 1920px)</td>
 *    </tr>
 *    <tr>
 *      <td>xl</td>
 *      <td>(min-width: 1920px)</td>
 *    </tr>
 *    <tr>
 *      <td>landscape</td>
 *      <td>landscape</td>
 *    </tr>
 *    <tr>
 *      <td>portrait</td>
 *      <td>portrait</td>
 *    </tr>
 *    <tr>
 *      <td>print</td>
 *      <td>print</td>
 *    </tr>
 *    </tbody>
 *  </table>
 *
 *  See Material Design's <a href="https://material.google.com/layout/responsive-ui.html">Layout - Adaptive UI</a> for more details.
 *
 *  <a href="https://material.io/archive/guidelines/layout/responsive-ui.html#">
 *  <img src="https://material-design.storage.googleapis.com/publish/material_v_4/material_ext_publish/0B8olV15J7abPSGFxemFiQVRtb1k/layout_adaptive_breakpoints_01.png" width="100%" height="100%"></img>
 *  </a>
 *
 * @returns {boolean} a boolean representing whether or not the given media query is true or false.
 *
 * @usage
 * <hljs lang="js">
 * app.controller('MyController', function($mdMedia, $scope) {
 *   $scope.$watch(function() { return $mdMedia('lg'); }, function(big) {
 *     $scope.bigScreen = big;
 *   });
 *
 *   $scope.screenIsSmall = $mdMedia('sm');
 *   $scope.customQuery = $mdMedia('(min-width: 1234px)');
 *   $scope.anotherCustom = $mdMedia('max-width: 300px');
 * });
 * </hljs>
 */

/* ngInject */
function mdMediaFactory($mdConstant, $rootScope, $window) {
  var queries = {};
  var mqls = {};
  var results = {};
  var normalizeCache = {};

  $mdMedia.getResponsiveAttribute = getResponsiveAttribute;
  $mdMedia.getQuery = getQuery;
  $mdMedia.watchResponsiveAttributes = watchResponsiveAttributes;

  return $mdMedia;

  function $mdMedia(query) {
    var validated = queries[query];
    if (angular.isUndefined(validated)) {
      validated = queries[query] = validate(query);
    }

    var result = results[validated];
    if (angular.isUndefined(result)) {
      result = add(validated);
    }

    return result;
  }

  function validate(query) {
    return $mdConstant.MEDIA[query] ||
           ((query.charAt(0) !== '(') ? ('(' + query + ')') : query);
  }

  function add(query) {
    var result = mqls[query];
    if (!result) {
      result = mqls[query] = $window.matchMedia(query);
    }

    result.addListener(onQueryChange);
    return (results[result.media] = !!result.matches);
  }

  function onQueryChange(query) {
    $rootScope.$evalAsync(function() {
      results[query.media] = !!query.matches;
    });
  }

  function getQuery(name) {
    return mqls[name];
  }

  function getResponsiveAttribute(attrs, attrName) {
    for (var i = 0; i < $mdConstant.MEDIA_PRIORITY.length; i++) {
      var mediaName = $mdConstant.MEDIA_PRIORITY[i];
      if (!mqls[queries[mediaName]].matches) {
        continue;
      }

      var normalizedName = getNormalizedName(attrs, attrName + '-' + mediaName);
      if (attrs[normalizedName]) {
        return attrs[normalizedName];
      }
    }

    // fallback on unprefixed
    return attrs[getNormalizedName(attrs, attrName)];
  }

  function watchResponsiveAttributes(attrNames, attrs, watchFn) {
    var unwatchFns = [];
    attrNames.forEach(function(attrName) {
      var normalizedName = getNormalizedName(attrs, attrName);
      if (angular.isDefined(attrs[normalizedName])) {
        unwatchFns.push(
            attrs.$observe(normalizedName, angular.bind(void 0, watchFn, null)));
      }

      for (var mediaName in $mdConstant.MEDIA) {
        normalizedName = getNormalizedName(attrs, attrName + '-' + mediaName);
        if (angular.isDefined(attrs[normalizedName])) {
          unwatchFns.push(
              attrs.$observe(normalizedName, angular.bind(void 0, watchFn, mediaName)));
        }
      }
    });

    return function unwatch() {
      unwatchFns.forEach(function(fn) { fn(); });
    };
  }

  // Improves performance dramatically
  function getNormalizedName(attrs, attrName) {
    return normalizeCache[attrName] ||
        (normalizeCache[attrName] = attrs.$normalize(attrName));
  }
}

angular
  .module('material.core')
  .config(["$provide", function($provide) {
    $provide.decorator('$mdUtil', ['$delegate', function ($delegate) {

      // Inject the prefixer into our original $mdUtil service.
      $delegate.prefixer = MdPrefixer;

      return $delegate;
    }]);
  }]);

function MdPrefixer(initialAttributes, buildSelector) {
  var PREFIXES = ['data', 'x'];

  if (initialAttributes) {
    // The prefixer also accepts attributes as a parameter, and immediately builds a list or selector for
    // the specified attributes.
    return buildSelector ? _buildSelector(initialAttributes) : _buildList(initialAttributes);
  }

  return {
    buildList: _buildList,
    buildSelector: _buildSelector,
    hasAttribute: _hasAttribute,
    removeAttribute: _removeAttribute
  };

  function _buildList(attributes) {
    attributes = angular.isArray(attributes) ? attributes : [attributes];

    attributes.forEach(function(item) {
      PREFIXES.forEach(function(prefix) {
        attributes.push(prefix + '-' + item);
      });
    });

    return attributes;
  }

  function _buildSelector(attributes) {
    attributes = angular.isArray(attributes) ? attributes : [attributes];

    return _buildList(attributes)
      .map(function(item) {
        return '[' + item + ']';
      })
      .join(',');
  }

  function _hasAttribute(element, attribute) {
    element = _getNativeElement(element);

    if (!element) {
      return false;
    }

    var prefixedAttrs = _buildList(attribute);

    for (var i = 0; i < prefixedAttrs.length; i++) {
      if (element.hasAttribute(prefixedAttrs[i])) {
        return true;
      }
    }

    return false;
  }

  function _removeAttribute(element, attribute) {
    element = _getNativeElement(element);

    if (!element) {
      return;
    }

    _buildList(attribute).forEach(function(prefixedAttribute) {
      element.removeAttribute(prefixedAttribute);
    });
  }

  /**
   * Transforms a jqLite or DOM element into a HTML element.
   * This is useful when supporting jqLite elements and DOM elements at
   * same time.
   * @param element {JQLite|Element} Element to be parsed
   * @returns {HTMLElement} Parsed HTMLElement
   */
  function _getNativeElement(element) {
    element =  element[0] || element;

    if (element.nodeType) {
      return element;
    }
  }

}

/*
 * This var has to be outside the angular factory, otherwise when
 * there are multiple material apps on the same page, each app
 * will create its own instance of this array and the app's IDs
 * will not be unique.
 */
UtilFactory['$inject'] = ["$document", "$timeout", "$compile", "$rootScope", "$$mdAnimate", "$interpolate", "$log", "$rootElement", "$window", "$$rAF"];
var nextUniqueId = 0;

/**
 * @ngdoc module
 * @name material.core.util
 * @description
 * Util
 */
angular
.module('material.core')
.factory('$mdUtil', UtilFactory);

/**
 * ngInject
 */
function UtilFactory($document, $timeout, $compile, $rootScope, $$mdAnimate, $interpolate, $log, $rootElement, $window, $$rAF) {
  // Setup some core variables for the processTemplate method
  var startSymbol = $interpolate.startSymbol(),
    endSymbol = $interpolate.endSymbol(),
    usesStandardSymbols = ((startSymbol === '{{') && (endSymbol === '}}'));

  /**
   * Checks if the target element has the requested style by key
   * @param {DOMElement|JQLite} target Target element
   * @param {string} key Style key
   * @param {string=} expectedVal Optional expected value
   * @returns {boolean} Whether the target element has the style or not
   */
  var hasComputedStyle = function (target, key, expectedVal) {
    var hasValue = false;

    if (target && target.length) {
      var computedStyles = $window.getComputedStyle(target[0]);
      hasValue = angular.isDefined(computedStyles[key]) && (expectedVal ? computedStyles[key] == expectedVal : true);
    }

    return hasValue;
  };

  function validateCssValue(value) {
    return !value       ? '0'   :
      hasPx(value) || hasPercent(value) ? value : value + 'px';
  }

  function hasPx(value) {
    return String(value).indexOf('px') > -1;
  }

  function hasPercent(value) {
    return String(value).indexOf('%') > -1;

  }

  var $mdUtil = {
    dom: {},
    now: window.performance && window.performance.now ?
      angular.bind(window.performance, window.performance.now) : Date.now || function() {
      return new Date().getTime();
    },

    /**
     * Cross-version compatibility method to retrieve an option of a ngModel controller,
     * which supports the breaking changes in the AngularJS snapshot (SHA 87a2ff76af5d0a9268d8eb84db5755077d27c84c).
     * @param {!angular.ngModelCtrl} ngModelCtrl
     * @param {!string} optionName
     * @returns {Object|undefined}
     */
    getModelOption: function (ngModelCtrl, optionName) {
      if (!ngModelCtrl.$options) {
        return;
      }

      var $options = ngModelCtrl.$options;

      // The newer versions of AngularJS introduced a `getOption function and made the option values no longer
      // visible on the $options object.
      return $options.getOption ? $options.getOption(optionName) : $options[optionName];
    },

    /**
     * Bi-directional accessor/mutator used to easily update an element's
     * property based on the current 'dir'ectional value.
     */
    bidi : function(element, property, lValue, rValue) {
      var ltr = !($document[0].dir == 'rtl' || $document[0].body.dir == 'rtl');

      // If accessor
      if (arguments.length == 0) return ltr ? 'ltr' : 'rtl';

      // If mutator
      var elem = angular.element(element);

      if (ltr && angular.isDefined(lValue)) {
        elem.css(property, validateCssValue(lValue));
      }
      else if (!ltr && angular.isDefined(rValue)) {
        elem.css(property, validateCssValue(rValue));
      }
    },

    bidiProperty: function (element, lProperty, rProperty, value) {
      var ltr = !($document[0].dir == 'rtl' || $document[0].body.dir == 'rtl');

      var elem = angular.element(element);

      if (ltr && angular.isDefined(lProperty)) {
        elem.css(lProperty, validateCssValue(value));
        elem.css(rProperty, '');
      }
      else if (!ltr && angular.isDefined(rProperty)) {
        elem.css(rProperty, validateCssValue(value));
        elem.css(lProperty, '');
      }
    },

    clientRect: function(element, offsetParent, isOffsetRect) {
      var node = getNode(element);
      offsetParent = getNode(offsetParent || node.offsetParent || document.body);
      var nodeRect = node.getBoundingClientRect();

      // The user can ask for an offsetRect: a rect relative to the offsetParent,
      // or a clientRect: a rect relative to the page
      var offsetRect = isOffsetRect ?
        offsetParent.getBoundingClientRect() :
        {left: 0, top: 0, width: 0, height: 0};
      return {
        left: nodeRect.left - offsetRect.left,
        top: nodeRect.top - offsetRect.top,
        width: nodeRect.width,
        height: nodeRect.height
      };
    },
    offsetRect: function(element, offsetParent) {
      return $mdUtil.clientRect(element, offsetParent, true);
    },

    // Annoying method to copy nodes to an array, thanks to IE
    nodesToArray: function(nodes) {
      nodes = nodes || [];

      var results = [];
      for (var i = 0; i < nodes.length; ++i) {
        results.push(nodes.item(i));
      }
      return results;
    },

    /**
     * Determines the absolute position of the viewport.
     * Useful when making client rectangles absolute.
     * @returns {number}
     */
    getViewportTop: function() {
      // If body scrolling is disabled, then use the cached viewport top value, otherwise get it
      // fresh from the $window.
      if ($mdUtil.disableScrollAround._count && $mdUtil.disableScrollAround._viewPortTop) {
        return $mdUtil.disableScrollAround._viewPortTop;
      } else {
        return $window.scrollY || $window.pageYOffset || 0;
      }
    },

    /**
     * Finds the proper focus target by searching the DOM.
     *
     * @param containerEl
     * @param attributeVal
     * @returns {*}
     */
    findFocusTarget: function(containerEl, attributeVal) {
      var AUTO_FOCUS = this.prefixer('md-autofocus', true);
      var elToFocus;

      elToFocus = scanForFocusable(containerEl, attributeVal || AUTO_FOCUS);

      if (!elToFocus && attributeVal != AUTO_FOCUS) {
        // Scan for deprecated attribute
        elToFocus = scanForFocusable(containerEl, this.prefixer('md-auto-focus', true));

        if (!elToFocus) {
          // Scan for fallback to 'universal' API
          elToFocus = scanForFocusable(containerEl, AUTO_FOCUS);
        }
      }

      return elToFocus;

      /**
       * Can target and nested children for specified Selector (attribute)
       * whose value may be an expression that evaluates to True/False.
       */
      function scanForFocusable(target, selector) {
        var elFound, items = target[0].querySelectorAll(selector);

        // Find the last child element with the focus attribute
        if (items && items.length){
          items.length && angular.forEach(items, function(it) {
            it = angular.element(it);

            // Check the element for the md-autofocus class to ensure any associated expression
            // evaluated to true.
            var isFocusable = it.hasClass('md-autofocus');
            if (isFocusable) elFound = it;
          });
        }
        return elFound;
      }
    },

    /**
     * Disables scroll around the passed parent element.
     * @param element Unused
     * @param {!Element|!angular.JQLite} parent Element to disable scrolling within.
     *   Defaults to body if none supplied.
     * @param options Object of options to modify functionality
     *   - disableScrollMask Boolean of whether or not to create a scroll mask element or
     *     use the passed parent element.
     */
    disableScrollAround: function(element, parent, options) {
      options = options || {};

      $mdUtil.disableScrollAround._count = Math.max(0, $mdUtil.disableScrollAround._count || 0);
      $mdUtil.disableScrollAround._count++;

      if ($mdUtil.disableScrollAround._restoreScroll) {
        return $mdUtil.disableScrollAround._restoreScroll;
      }

      var body = $document[0].body;
      var restoreBody = disableBodyScroll();
      var restoreElement = disableElementScroll(parent);

      return $mdUtil.disableScrollAround._restoreScroll = function() {
        if (--$mdUtil.disableScrollAround._count <= 0) {
          delete $mdUtil.disableScrollAround._viewPortTop;
          restoreBody();
          restoreElement();
          delete $mdUtil.disableScrollAround._restoreScroll;
        }
      };

      /**
       * Creates a virtual scrolling mask to prevent touchmove, keyboard, scrollbar clicking,
       * and wheel events
       */
      function disableElementScroll(element) {
        element = angular.element(element || body);

        var scrollMask;

        if (options.disableScrollMask) {
          scrollMask = element;
        } else {
          scrollMask = angular.element(
            '<div class="md-scroll-mask">' +
            '  <div class="md-scroll-mask-bar"></div>' +
            '</div>');
          element.append(scrollMask);
        }

        scrollMask.on('wheel', preventDefault);
        scrollMask.on('touchmove', preventDefault);

        return function restoreElementScroll() {
          scrollMask.off('wheel');
          scrollMask.off('touchmove');

          if (!options.disableScrollMask && scrollMask[0].parentNode) {
            scrollMask[0].parentNode.removeChild(scrollMask[0]);
          }
        };

        function preventDefault(e) {
          e.preventDefault();
        }
      }

      // Converts the body to a position fixed block and translate it to the proper scroll position
      function disableBodyScroll() {
        var documentElement = $document[0].documentElement;

        var prevDocumentStyle = documentElement.style.cssText || '';
        var prevBodyStyle = body.style.cssText || '';

        var viewportTop = $mdUtil.getViewportTop();
        $mdUtil.disableScrollAround._viewPortTop = viewportTop;
        var clientWidth = body.clientWidth;
        var hasVerticalScrollbar = body.scrollHeight > body.clientHeight + 1;

        // Scroll may be set on <html> element (for example by overflow-y: scroll)
        // but Chrome is reporting the scrollTop position always on <body>.
        // scrollElement will allow to restore the scrollTop position to proper target.
        var scrollElement = documentElement.scrollTop > 0 ? documentElement : body;

        if (hasVerticalScrollbar) {
          angular.element(body).css({
            position: 'fixed',
            width: '100%',
            top: -viewportTop + 'px'
          });
        }

        if (body.clientWidth < clientWidth) {
          body.style.overflow = 'hidden';
        }

        return function restoreScroll() {
          // Reset the inline style CSS to the previous.
          body.style.cssText = prevBodyStyle;
          documentElement.style.cssText = prevDocumentStyle;

          // The scroll position while being fixed
          scrollElement.scrollTop = viewportTop;
        };
      }

    },

    enableScrolling: function() {
      var restoreFn = this.disableScrollAround._restoreScroll;
      restoreFn && restoreFn();
    },

    floatingScrollbars: function() {
      if (this.floatingScrollbars.cached === undefined) {
        var tempNode = angular.element('<div><div></div></div>').css({
          width: '100%',
          'z-index': -1,
          position: 'absolute',
          height: '35px',
          'overflow-y': 'scroll'
        });
        tempNode.children().css('height', '60px');

        $document[0].body.appendChild(tempNode[0]);
        this.floatingScrollbars.cached = (tempNode[0].offsetWidth == tempNode[0].childNodes[0].offsetWidth);
        tempNode.remove();
      }
      return this.floatingScrollbars.cached;
    },

    // Mobile safari only allows you to set focus in click event listeners...
    forceFocus: function(element) {
      var node = element[0] || element;

      document.addEventListener('click', function focusOnClick(ev) {
        if (ev.target === node && ev.$focus) {
          node.focus();
          ev.stopImmediatePropagation();
          ev.preventDefault();
          node.removeEventListener('click', focusOnClick);
        }
      }, true);

      var newEvent = document.createEvent('MouseEvents');
      newEvent.initMouseEvent('click', false, true, window, {}, 0, 0, 0, 0,
        false, false, false, false, 0, null);
      newEvent.$material = true;
      newEvent.$focus = true;
      node.dispatchEvent(newEvent);
    },

    /**
     * facade to build md-backdrop element with desired styles
     * NOTE: Use $compile to trigger backdrop postLink function
     */
    createBackdrop: function(scope, addClass) {
      return $compile($mdUtil.supplant('<md-backdrop class="{0}">', [addClass]))(scope);
    },

    /**
     * supplant() method from Crockford's `Remedial Javascript`
     * Equivalent to use of $interpolate; without dependency on
     * interpolation symbols and scope. Note: the '{<token>}' can
     * be property names, property chains, or array indices.
     */
    supplant: function(template, values, pattern) {
      pattern = pattern || /\{([^{}]*)\}/g;
      return template.replace(pattern, function(a, b) {
        var p = b.split('.'),
          r = values;
        try {
          for (var s in p) {
            if (p.hasOwnProperty(s)) {
              r = r[p[s]];
            }
          }
        } catch (e) {
          r = a;
        }
        return (typeof r === 'string' || typeof r === 'number') ? r : a;
      });
    },

    fakeNgModel: function() {
      return {
        $fake: true,
        $setTouched: angular.noop,
        $setViewValue: function(value) {
          this.$viewValue = value;
          this.$render(value);
          this.$viewChangeListeners.forEach(function(cb) {
            cb();
          });
        },
        $isEmpty: function(value) {
          return ('' + value).length === 0;
        },
        $parsers: [],
        $formatters: [],
        $viewChangeListeners: [],
        $render: angular.noop
      };
    },

    // Returns a function, that, as long as it continues to be invoked, will not
    // be triggered. The function will be called after it stops being called for
    // N milliseconds.
    // @param wait Integer value of msecs to delay (since last debounce reset); default value 10 msecs
    // @param invokeApply should the $timeout trigger $digest() dirty checking
    debounce: function(func, wait, scope, invokeApply) {
      var timer;

      return function debounced() {
        var context = scope,
          args = Array.prototype.slice.call(arguments);

        $timeout.cancel(timer);
        timer = $timeout(function() {

          timer = undefined;
          func.apply(context, args);

        }, wait || 10, invokeApply);
      };
    },

    // Returns a function that can only be triggered every `delay` milliseconds.
    // In other words, the function will not be called unless it has been more
    // than `delay` milliseconds since the last call.
    throttle: function throttle(func, delay) {
      var recent;
      return function throttled() {
        var context = this;
        var args = arguments;
        var now = $mdUtil.now();

        if (!recent || (now - recent > delay)) {
          func.apply(context, args);
          recent = now;
        }
      };
    },

    /**
     * Measures the number of milliseconds taken to run the provided callback
     * function. Uses a high-precision timer if available.
     */
    time: function time(cb) {
      var start = $mdUtil.now();
      cb();
      return $mdUtil.now() - start;
    },

    /**
     * Create an implicit getter that caches its `getter()`
     * lookup value
     */
    valueOnUse : function (scope, key, getter) {
      var value = null, args = Array.prototype.slice.call(arguments);
      var params = (args.length > 3) ? args.slice(3) : [];

      Object.defineProperty(scope, key, {
        get: function () {
          if (value === null) value = getter.apply(scope, params);
          return value;
        }
      });
    },

    /**
     * Get a unique ID.
     *
     * @returns {string} an unique numeric string
     */
    nextUid: function() {
      return '' + nextUniqueId++;
    },

    // Stop watchers and events from firing on a scope without destroying it,
    // by disconnecting it from its parent and its siblings' linked lists.
    disconnectScope: function disconnectScope(scope) {
      if (!scope) return;

      // we can't destroy the root scope or a scope that has been already destroyed
      if (scope.$root === scope) return;
      if (scope.$$destroyed) return;

      var parent = scope.$parent;
      scope.$$disconnected = true;

      // See Scope.$destroy
      if (parent.$$childHead === scope) parent.$$childHead = scope.$$nextSibling;
      if (parent.$$childTail === scope) parent.$$childTail = scope.$$prevSibling;
      if (scope.$$prevSibling) scope.$$prevSibling.$$nextSibling = scope.$$nextSibling;
      if (scope.$$nextSibling) scope.$$nextSibling.$$prevSibling = scope.$$prevSibling;

      scope.$$nextSibling = scope.$$prevSibling = null;

    },

    // Undo the effects of disconnectScope above.
    reconnectScope: function reconnectScope(scope) {
      if (!scope) return;

      // we can't disconnect the root node or scope already disconnected
      if (scope.$root === scope) return;
      if (!scope.$$disconnected) return;

      var child = scope;

      var parent = child.$parent;
      child.$$disconnected = false;
      // See Scope.$new for this logic...
      child.$$prevSibling = parent.$$childTail;
      if (parent.$$childHead) {
        parent.$$childTail.$$nextSibling = child;
        parent.$$childTail = child;
      } else {
        parent.$$childHead = parent.$$childTail = child;
      }
    },

    /**
     * getClosest replicates jQuery.closest() to walk up the DOM tree until it finds a matching
     * nodeName.
     *
     * @param {Node} el Element to start walking the DOM from
     * @param {string|function} validateWith If a string is passed, it will be evaluated against
     * each of the parent nodes' tag name. If a function is passed, the loop will call it with each
     * of the parents and will use the return value to determine whether the node is a match.
     * @param {boolean=} onlyParent Only start checking from the parent element, not `el`.
     * @returns {Node|null} closest matching parent Node or null if not found
     */
    getClosest: function getClosest(el, validateWith, onlyParent) {
      if (angular.isString(validateWith)) {
        var tagName = validateWith.toUpperCase();
        validateWith = function(el) {
          return el.nodeName.toUpperCase() === tagName;
        };
      }

      if (el instanceof angular.element) el = el[0];
      if (onlyParent) el = el.parentNode;
      if (!el) return null;

      do {
        if (validateWith(el)) {
          return el;
        }
      } while (el = el.parentNode);

      return null;
    },

    /**
     * Build polyfill for the Node.contains feature (if needed)
     * @param {Node} node
     * @param {Node} child
     * @returns {Node}
     */
    elementContains: function(node, child) {
      var hasContains = (window.Node && window.Node.prototype && Node.prototype.contains);
      var findFn = hasContains ? angular.bind(node, node.contains) : angular.bind(node, function(arg) {
        // compares the positions of two nodes and returns a bitmask
        return (node === child) || !!(this.compareDocumentPosition(arg) & 16);
      });

      return findFn(child);
    },

    /**
     * Functional equivalent for $element.filter(‘md-bottom-sheet’)
     * useful with interimElements where the element and its container are important...
     *
     * @param {angular.JQLite} element to scan
     * @param {string} nodeName of node to find (e.g. 'md-dialog')
     * @param {boolean=} scanDeep optional flag to allow deep scans; defaults to 'false'.
     * @param {boolean=} warnNotFound optional flag to enable log warnings; defaults to false
     */
    extractElementByName: function(element, nodeName, scanDeep, warnNotFound) {
      var found = scanTree(element);
      if (!found && !!warnNotFound) {
        $log.warn($mdUtil.supplant("Unable to find node '{0}' in element '{1}'.",[nodeName, element[0].outerHTML]));
      }

      return angular.element(found || element);

      /**
       * Breadth-First tree scan for element with matching `nodeName`
       */
      function scanTree(element) {
        return scanLevel(element) || (scanDeep ? scanChildren(element) : null);
      }

      /**
       * Case-insensitive scan of current elements only (do not descend).
       */
      function scanLevel(element) {
        if (element) {
          for (var i = 0, len = element.length; i < len; i++) {
            if (element[i].nodeName.toLowerCase() === nodeName) {
              return element[i];
            }
          }
        }
        return null;
      }

      /**
       * Scan children of specified node
       */
      function scanChildren(element) {
        var found;
        if (element) {
          for (var i = 0, len = element.length; i < len; i++) {
            var target = element[i];
            if (!found) {
              for (var j = 0, numChild = target.childNodes.length; j < numChild; j++) {
                found = found || scanTree([target.childNodes[j]]);
              }
            }
          }
        }
        return found;
      }

    },

    /**
     * Give optional properties with no value a boolean true if attr provided or false otherwise
     */
    initOptionalProperties: function(scope, attr, defaults) {
      defaults = defaults || {};
      angular.forEach(scope.$$isolateBindings, function(binding, key) {
        if (binding.optional && angular.isUndefined(scope[key])) {
          var attrIsDefined = angular.isDefined(attr[binding.attrName]);
          scope[key] = angular.isDefined(defaults[key]) ? defaults[key] : attrIsDefined;
        }
      });
    },

    /**
     * Alternative to $timeout calls with 0 delay.
     * nextTick() coalesces all calls within a single frame
     * to minimize $digest thrashing
     *
     * @param {Function} callback function to be called after the tick
     * @param {boolean} digest true to call $rootScope.$digest() after callback
     * @param scope scope associated with callback. If the scope is destroyed, the callback will
     *  be skipped.
     * @returns {*}
     */
    nextTick: function(callback, digest, scope) {
      // grab function reference for storing state details
      var nextTick = $mdUtil.nextTick;
      var timeout = nextTick.timeout;
      var queue = nextTick.queue || [];

      // add callback to the queue
      queue.push({scope: scope, callback: callback});

      // set default value for digest
      if (digest == null) digest = true;

      // store updated digest/queue values
      nextTick.digest = nextTick.digest || digest;
      nextTick.queue = queue;

      // either return existing timeout or create a new one
      return timeout || (nextTick.timeout = $timeout(processQueue, 0, false));

      /**
       * Grab a copy of the current queue
       * Clear the queue for future use
       * Process the existing queue
       * Trigger digest if necessary
       */
      function processQueue() {
        var queue = nextTick.queue;
        var digest = nextTick.digest;

        nextTick.queue = [];
        nextTick.timeout = null;
        nextTick.digest = false;

        queue.forEach(function(queueItem) {
          var skip = queueItem.scope && queueItem.scope.$$destroyed;
          if (!skip) {
            queueItem.callback();
          }
        });

        if (digest) $rootScope.$digest();
      }
    },

    /**
     * Processes a template and replaces the start/end symbols if the application has
     * overridden them.
     *
     * @param template The template to process whose start/end tags may be replaced.
     * @returns {*}
     */
    processTemplate: function(template) {
      if (usesStandardSymbols) {
        return template;
      } else {
        if (!template || !angular.isString(template)) return template;
        return template.replace(/\{\{/g, startSymbol).replace(/}}/g, endSymbol);
      }
    },

    /**
     * Scan up dom hierarchy for enabled parent;
     */
    getParentWithPointerEvents: function (element) {
      var parent = element.parent();

      // jqLite might return a non-null, but still empty, parent; so check for parent and length
      while (hasComputedStyle(parent, 'pointer-events', 'none')) {
        parent = parent.parent();
      }

      return parent;
    },

    getNearestContentElement: function (element) {
      var current = element.parent()[0];
      // Look for the nearest parent md-content, stopping at the rootElement.
      while (current && current !== $rootElement[0] && current !== document.body && current.nodeName.toUpperCase() !== 'MD-CONTENT') {
        current = current.parentNode;
      }
      return current;
    },

    /**
     * Checks if the current browser is natively supporting the `sticky` position.
     * @returns {string} supported sticky property name
     */
    checkStickySupport: function() {
      var stickyProp;
      var testEl = angular.element('<div>');
      $document[0].body.appendChild(testEl[0]);

      var stickyProps = ['sticky', '-webkit-sticky'];
      for (var i = 0; i < stickyProps.length; ++i) {
        testEl.css({
          position: stickyProps[i],
          top: 0,
          'z-index': 2
        });

        if (testEl.css('position') == stickyProps[i]) {
          stickyProp = stickyProps[i];
          break;
        }
      }

      testEl.remove();

      return stickyProp;
    },

    /**
     * Parses an attribute value, mostly a string.
     * By default checks for negated values and returns `false´ if present.
     * Negated values are: (native falsy) and negative strings like:
     * `false` or `0`.
     * @param value Attribute value which should be parsed.
     * @param negatedCheck When set to false, won't check for negated values.
     * @returns {boolean}
     */
    parseAttributeBoolean: function(value, negatedCheck) {
      return value === '' || !!value && (negatedCheck === false || value !== 'false' && value !== '0');
    },

    hasComputedStyle: hasComputedStyle,

    /**
     * Returns true if the parent form of the element has been submitted.
     *
     * @param element An AngularJS or HTML5 element.
     *
     * @returns {boolean}
     */
    isParentFormSubmitted: function(element) {
      var parent = $mdUtil.getClosest(element, 'form');
      var form = parent ? angular.element(parent).controller('form') : null;

      return form ? form.$submitted : false;
    },

    /**
     * Animate the requested element's scrollTop to the requested scrollPosition with basic easing.
     *
     * @param {!Element} element The element to scroll.
     * @param {number} scrollEnd The new/final scroll position.
     * @param {number=} duration Duration of the scroll. Default is 1000ms.
     */
    animateScrollTo: function(element, scrollEnd, duration) {
      var scrollStart = element.scrollTop;
      var scrollChange = scrollEnd - scrollStart;
      var scrollingDown = scrollStart < scrollEnd;
      var startTime = $mdUtil.now();

      $$rAF(scrollChunk);

      function scrollChunk() {
        var newPosition = calculateNewPosition();

        element.scrollTop = newPosition;

        if (scrollingDown ? newPosition < scrollEnd : newPosition > scrollEnd) {
          $$rAF(scrollChunk);
        }
      }

      function calculateNewPosition() {
        var easeDuration = duration || 1000;
        var currentTime = $mdUtil.now() - startTime;

        return ease(currentTime, scrollStart, scrollChange, easeDuration);
      }

      function ease(currentTime, start, change, duration) {
        // If the duration has passed (which can occur if our app loses focus due to $$rAF), jump
        // straight to the proper position
        if (currentTime > duration) {
          return start + change;
        }

        var ts = (currentTime /= duration) * currentTime;
        var tc = ts * currentTime;

        return start + change * (-2 * tc + 3 * ts);
      }
    },

    /**
     * Provides an easy mechanism for removing duplicates from an array.
     *
     *    var myArray = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4];
     *
     *    $mdUtil.uniq(myArray) => [1, 2, 3, 4]
     *
     * @param {array} array The array whose unique values should be returned.
     *
     * @returns {array} A copy of the array containing only unique values.
     */
    uniq: function(array) {
      if (!array) { return; }

      return array.filter(function(value, index, self) {
        return self.indexOf(value) === index;
      });
    },

    /**
     * Gets the inner HTML content of the given HTMLElement.
     * Only intended for use with SVG or Symbol elements in IE11.
     * @param {Element} element
     * @returns {string} the inner HTML of the element passed in
     */
    getInnerHTML: function(element) {
      // For SVG or Symbol elements, innerHTML returns `undefined` in IE.
      // Reference: https://stackoverflow.com/q/28129956/633107
      // The XMLSerializer API is supported on IE11 and is the recommended workaround.
      var serializer = new XMLSerializer();

      return Array.prototype.map.call(element.childNodes, function (child) {
        return serializer.serializeToString(child);
      }).join('');
    },

    /**
     * Gets the outer HTML content of the given HTMLElement.
     * Only intended for use with SVG or Symbol elements in IE11.
     * @param {Element} element
     * @returns {string} the outer HTML of the element passed in
     */
    getOuterHTML: function(element) {
      // For SVG or Symbol elements, outerHTML returns `undefined` in IE.
      // Reference: https://stackoverflow.com/q/29888050/633107
      // The XMLSerializer API is supported on IE11 and is the recommended workaround.
      var serializer = new XMLSerializer();
      return serializer.serializeToString(element);
    },

    /**
     * Support: IE 9-11 only
     * documentMode is an IE-only property
     * http://msdn.microsoft.com/en-us/library/ie/cc196988(v=vs.85).aspx
     */
    msie: window.document.documentMode
  };

  // Instantiate other namespace utility methods

  $mdUtil.dom.animator = $$mdAnimate($mdUtil);

  return $mdUtil;

  function getNode(el) {
    return el[0] || el;
  }
}

/*
 * Since removing jQuery from the demos, some code that uses `element.focus()` is broken.
 * We need to add `element.focus()`, because it's testable unlike `element[0].focus`.
 */

angular.element.prototype.focus = angular.element.prototype.focus || function() {
  if (this.length) {
    this[0].focus();
  }
  return this;
};
angular.element.prototype.blur = angular.element.prototype.blur || function() {
  if (this.length) {
    this[0].blur();
  }
  return this;
};

/**
 * @ngdoc module
 * @name material.core.aria
 * @description
 * Aria Expectations for AngularJS Material components.
 */
MdAriaService['$inject'] = ["$$rAF", "$log", "$window", "$interpolate"];
angular
  .module('material.core')
  .provider('$mdAria', MdAriaProvider);

/**
 * @ngdoc service
 * @name $mdAriaProvider
 * @module material.core.aria
 *
 * @description
 *
 * Modify options of the `$mdAria` service, which will be used by most of the AngularJS Material
 * components.
 *
 * You are able to disable `$mdAria` warnings, by using the following markup.
 *
 * <hljs lang="js">
 *   app.config(function($mdAriaProvider) {
 *     // Globally disables all ARIA warnings.
 *     $mdAriaProvider.disableWarnings();
 *   });
 * </hljs>
 *
 */
function MdAriaProvider() {

  var config = {
    /** Whether we should show ARIA warnings in the console if labels are missing on the element */
    showWarnings: true
  };

  return {
    disableWarnings: disableWarnings,
    $get: ["$$rAF", "$log", "$window", "$interpolate", function($$rAF, $log, $window, $interpolate) {
      return MdAriaService.apply(config, arguments);
    }]
  };

  /**
   * @ngdoc method
   * @name $mdAriaProvider#disableWarnings
   * @description Disables all ARIA warnings generated by AngularJS Material.
   */
  function disableWarnings() {
    config.showWarnings = false;
  }
}

/*
 * ngInject
 */
function MdAriaService($$rAF, $log, $window, $interpolate) {

  // Load the showWarnings option from the current context and store it inside of a scope variable,
  // because the context will be probably lost in some function calls.
  var showWarnings = this.showWarnings;

  return {
    expect: expect,
    expectAsync: expectAsync,
    expectWithText: expectWithText,
    expectWithoutText: expectWithoutText,
    getText: getText,
    hasAriaLabel: hasAriaLabel,
    parentHasAriaLabel: parentHasAriaLabel
  };

  /**
   * Check if expected attribute has been specified on the target element or child
   * @param element
   * @param attrName
   * @param {optional} defaultValue What to set the attr to if no value is found
   */
  function expect(element, attrName, defaultValue) {

    var node = angular.element(element)[0] || element;

    // if node exists and neither it nor its children have the attribute
    if (node &&
       ((!node.hasAttribute(attrName) ||
        node.getAttribute(attrName).length === 0) &&
        !childHasAttribute(node, attrName))) {

      defaultValue = angular.isString(defaultValue) ? defaultValue.trim() : '';
      if (defaultValue.length) {
        element.attr(attrName, defaultValue);
      } else if (showWarnings) {
        $log.warn('ARIA: Attribute "', attrName, '", required for accessibility, is missing on node:', node);
      }

    }
  }

  function expectAsync(element, attrName, defaultValueGetter) {
    // Problem: when retrieving the element's contents synchronously to find the label,
    // the text may not be defined yet in the case of a binding.
    // There is a higher chance that a binding will be defined if we wait one frame.
    $$rAF(function() {
        expect(element, attrName, defaultValueGetter());
    });
  }

  function expectWithText(element, attrName) {
    var content = getText(element) || "";
    var hasBinding = content.indexOf($interpolate.startSymbol()) > -1;

    if (hasBinding) {
      expectAsync(element, attrName, function() {
        return getText(element);
      });
    } else {
      expect(element, attrName, content);
    }
  }

  function expectWithoutText(element, attrName) {
    var content = getText(element);
    var hasBinding = content.indexOf($interpolate.startSymbol()) > -1;

    if (!hasBinding && !content) {
      expect(element, attrName, content);
    }
  }

  function getText(element) {
    element = element[0] || element;
    var walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false);
    var text = '';

    var node;
    while (node = walker.nextNode()) {
      if (!isAriaHiddenNode(node)) {
        text += node.textContent;
      }
    }

    return text.trim() || '';

    function isAriaHiddenNode(node) {
      while (node.parentNode && (node = node.parentNode) !== element) {
        if (node.getAttribute && node.getAttribute('aria-hidden') === 'true') {
          return true;
        }
      }
    }
  }

  function childHasAttribute(node, attrName) {
    var hasChildren = node.hasChildNodes(),
        hasAttr = false;

    function isHidden(el) {
      var style = el.currentStyle ? el.currentStyle : $window.getComputedStyle(el);
      return (style.display === 'none');
    }

    if (hasChildren) {
      var children = node.childNodes;
      for (var i=0; i < children.length; i++) {
        var child = children[i];
        if (child.nodeType === 1 && child.hasAttribute(attrName)) {
          if (!isHidden(child)) {
            hasAttr = true;
          }
        }
      }
    }
    return hasAttr;
  }

  /**
   * Check if expected element has aria label attribute
   * @param element
   */
  function hasAriaLabel(element) {
    var node = angular.element(element)[0] || element;

    /* Check if compatible node type (ie: not HTML Document node) */
    if (!node.hasAttribute) {
      return false;
    }

    /* Check label or description attributes */
    return node.hasAttribute('aria-label') || node.hasAttribute('aria-labelledby') || node.hasAttribute('aria-describedby');
  }

  /**
   * Check if expected element's parent has aria label attribute and has valid role and tagName
   * @param element
   * @param {optional} level Number of levels deep search should be performed
   */
  function parentHasAriaLabel(element, level) {
    level = level || 1;
    var node = angular.element(element)[0] || element;
    if (!node.parentNode) {
      return false;
    }
    if (performCheck(node.parentNode)) {
      return true;
    }
    level--;
    if (level) {
      return parentHasAriaLabel(node.parentNode, level);
    }
    return false;

    function performCheck(parentNode) {
      if (!hasAriaLabel(parentNode)) {
        return false;
      }
      /* Perform role blacklist check */
      if (parentNode.hasAttribute('role')) {
        switch (parentNode.getAttribute('role').toLowerCase()) {
          case 'command':
          case 'definition':
          case 'directory':
          case 'grid':
          case 'list':
          case 'listitem':
          case 'log':
          case 'marquee':
          case 'menu':
          case 'menubar':
          case 'note':
          case 'presentation':
          case 'separator':
          case 'scrollbar':
          case 'status':
          case 'tablist':
            return false;
        }
      }
      /* Perform tagName blacklist check */
      switch (parentNode.tagName.toLowerCase()) {
        case 'abbr':
        case 'acronym':
        case 'address':
        case 'applet':
        case 'audio':
        case 'b':
        case 'bdi':
        case 'bdo':
        case 'big':
        case 'blockquote':
        case 'br':
        case 'canvas':
        case 'caption':
        case 'center':
        case 'cite':
        case 'code':
        case 'col':
        case 'data':
        case 'dd':
        case 'del':
        case 'dfn':
        case 'dir':
        case 'div':
        case 'dl':
        case 'em':
        case 'embed':
        case 'fieldset':
        case 'figcaption':
        case 'font':
        case 'h1':
        case 'h2':
        case 'h3':
        case 'h4':
        case 'h5':
        case 'h6':
        case 'hgroup':
        case 'html':
        case 'i':
        case 'ins':
        case 'isindex':
        case 'kbd':
        case 'keygen':
        case 'label':
        case 'legend':
        case 'li':
        case 'map':
        case 'mark':
        case 'menu':
        case 'object':
        case 'ol':
        case 'output':
        case 'pre':
        case 'presentation':
        case 'q':
        case 'rt':
        case 'ruby':
        case 'samp':
        case 'small':
        case 'source':
        case 'span':
        case 'status':
        case 'strike':
        case 'strong':
        case 'sub':
        case 'sup':
        case 'svg':
        case 'tbody':
        case 'td':
        case 'th':
        case 'thead':
        case 'time':
        case 'tr':
        case 'track':
        case 'tt':
        case 'ul':
        case 'var':
          return false;
      }
      return true;
    }
  }
}

/**
 * @ngdoc module
 * @name material.core.compiler
 * @description
 * AngularJS Material template and element compiler.
 */
angular
  .module('material.core')
  .provider('$mdCompiler', MdCompilerProvider);

/**
 * @ngdoc service
 * @name $mdCompilerProvider
 * @module material.core.compiler
 * @description
 * The `$mdCompiler` is able to respect the AngularJS `$compileProvider.preAssignBindingsEnabled`
 * state when using AngularJS versions greater than or equal to 1.5.10 and less than 1.7.0.
 * See the [AngularJS documentation for `$compileProvider.preAssignBindingsEnabled`
 * ](https://code.angularjs.org/1.6.10/docs/api/ng/provider/$compileProvider#preAssignBindingsEnabled)
 * for more information.
 *
 * To enable/disable whether the controllers of dynamic AngularJS Material components
 * (i.e. dialog, panel, toast, bottomsheet) respect the AngularJS
 * `$compileProvider.preAssignBindingsEnabled` flag, call the AngularJS Material method:
 * `$mdCompilerProvider.respectPreAssignBindingsEnabled(boolean)`.
 *
 * This AngularJS Material *flag* doesn't affect directives/components created via regular
 * AngularJS methods. These constitute the majority of AngularJS Material and user-created
 * components. Only dynamic construction of elements such as Dialogs, Panels, Toasts, BottomSheets,
 * etc. may be affected. Invoking `$mdCompilerProvider.respectPreAssignBindingsEnabled(true)`
 * will effect **bindings** in controllers created by AngularJS Material's services like
 * `$mdDialog`, `$mdPanel`, `$mdToast`, or `$mdBottomSheet`.
 *
 * See [$mdCompilerProvider.respectPreAssignBindingsEnabled](#mdcompilerprovider-respectpreassignbindingsenabled-respected)
 * for the details of how the different versions and settings of AngularJS affect this behavior.
 *
 * @usage
 *
 * Respect the AngularJS Compiler Setting
 *
 * <hljs lang="js">
 *   app.config(function($mdCompilerProvider) {
 *     $mdCompilerProvider.respectPreAssignBindingsEnabled(true);
 *   });
 * </hljs>
 *
 * @example
 * Using the default (backwards compatible) values for AngularJS 1.6
 * - AngularJS' `$compileProvider.preAssignBindingsEnabled(false)`
 * - AngularJS Material's `$mdCompilerProvider.respectPreAssignBindingsEnabled(false)`
 * <br><br>
 *
 * <hljs lang="js">
 * $mdDialog.show({
 *   locals: {
 *     myVar: true
 *   },
 *   controller: MyController,
 *   bindToController: true
 * }
 *
 * function MyController() {
 *   // Locals from Angular Material are available. e.g myVar is true.
 * }
 *
 * MyController.prototype.$onInit = function() {
 *   // Bindings are also available in the $onInit lifecycle hook.
 * }
 * </hljs>
 *
 * Recommended Settings for AngularJS 1.6
 * - AngularJS' `$compileProvider.preAssignBindingsEnabled(false)`
 * - AngularJS Material's `$mdCompilerProvider.respectPreAssignBindingsEnabled(true)`
 * <br><br>
 *
 * <hljs lang="js">
 * $mdDialog.show({
 *   locals: {
 *     myVar: true
 *   },
 *   controller: MyController,
 *   bindToController: true
 * }
 *
 * function MyController() {
 *   // No locals from Angular Material are available. e.g myVar is undefined.
 * }
 *
 * MyController.prototype.$onInit = function() {
 *   // Bindings are now available in the $onInit lifecycle hook.
 * }
 * </hljs>
 *
 */
MdCompilerProvider['$inject'] = ['$compileProvider'];
function MdCompilerProvider($compileProvider) {

  var provider = this;

  /**
   * @ngdoc method
   * @name $mdCompilerProvider#respectPreAssignBindingsEnabled
   *
   * @param {boolean=} respected update the `respectPreAssignBindingsEnabled` state if provided,
   *  otherwise just return the current Material `respectPreAssignBindingsEnabled` state.
   * @returns {boolean|MdCompilerProvider} current value, if used as a getter, or itself (chaining)
   *  if used as a setter.
   *
   * @description
   * Call this method to enable/disable whether Material-specific (dialog/panel/toast/bottomsheet)
   * controllers respect the AngularJS `$compileProvider.preAssignBindingsEnabled` flag. Note that
   * this doesn't affect directives/components created via regular AngularJS methods which
   * constitute most Material and user-created components.
   *
   * If disabled (`false`), the compiler assigns the value of each of the bindings to the
   * properties of the controller object before the constructor of this object is called.
   * The ability to disable this settings is **deprecated** and will be removed in
   * AngularJS Material 1.2.0.
   *
   * If enabled (`true`) the behavior depends on the AngularJS version used:
   *
   * - `<1.5.10`
   *  - Bindings are pre-assigned.
   * - `>=1.5.10 <1.7`
   *  - Respects whatever `$compileProvider.preAssignBindingsEnabled()` reports. If the
   *    `preAssignBindingsEnabled` flag wasn't set manually, it defaults to pre-assigning bindings
   *    with AngularJS `1.5` and to calling the constructor first with AngularJS `1.6`.
   * - `>=1.7`
   *  - The compiler calls the constructor first before assigning bindings and
   *    `$compileProvider.preAssignBindingsEnabled()` no longer exists.
   *
   * Defaults
   * - The default value is `false` in AngularJS 1.6 and earlier.
   *  - It is planned to fix this value to `true` and not allow the `false` value in
   *    AngularJS Material 1.2.0.
   *
   * It is recommended to set this flag to `true` when using AngularJS Material 1.1.x with
   * AngularJS versions >= 1.5.10. The only reason it's not set that way by default is backwards
   * compatibility.
   *
   * By not setting the flag to `true` when AngularJS' `$compileProvider.preAssignBindingsEnabled()`
   * is set to `false` (i.e. default behavior in AngularJS 1.6 or newer), unit testing of
   * Material Dialog/Panel/Toast/BottomSheet controllers using the `$controller` helper
   * is problematic as it always follows AngularJS' `$compileProvider.preAssignBindingsEnabled()`
   * value.
   */
  var respectPreAssignBindingsEnabled = false;
  this.respectPreAssignBindingsEnabled = function(respected) {
    if (angular.isDefined(respected)) {
      respectPreAssignBindingsEnabled = respected;
      return this;
    }

    return respectPreAssignBindingsEnabled;
  };

  /**
   * @private
   * @description
   * This function returns `true` if AngularJS Material-specific (dialog/panel/toast/bottomsheet)
   * controllers have bindings pre-assigned in controller constructors and `false` otherwise.
   *
   * Note that this doesn't affect directives/components created via regular AngularJS methods
   * which constitute most Material and user-created components; their behavior can be checked via
   * `$compileProvider.preAssignBindingsEnabled()` in AngularJS `>=1.5.10 <1.7.0`.
   *
   * @returns {*} current preAssignBindingsEnabled state
   */
  function getPreAssignBindingsEnabled() {
    if (!respectPreAssignBindingsEnabled) {
      // respectPreAssignBindingsEnabled === false
      // We're ignoring the AngularJS `$compileProvider.preAssignBindingsEnabled()` value in this case.
      return true;
    }

    // respectPreAssignBindingsEnabled === true

    // This check is needed because $compileProvider.preAssignBindingsEnabled does not exist prior
    // to AngularJS 1.5.10, is deprecated in AngularJS 1.6.x, and removed in AngularJS 1.7.x.
    if (typeof $compileProvider.preAssignBindingsEnabled === 'function') {
      return $compileProvider.preAssignBindingsEnabled();
    }

    // Flag respected but not present => apply logic based on AngularJS version used.
    if (angular.version.major === 1 && angular.version.minor < 6) {
      // AngularJS <1.5.10
      return true;
    }

    // AngularJS >=1.7.0
    return false;
  }

  this.$get = ["$q", "$templateRequest", "$injector", "$compile", "$controller",
    function($q, $templateRequest, $injector, $compile, $controller) {
      return new MdCompilerService($q, $templateRequest, $injector, $compile, $controller);
    }];

  /**
   * @ngdoc service
   * @name $mdCompiler
   * @module material.core.compiler
   * @description
   * The $mdCompiler service is an abstraction of AngularJS's compiler, that allows developers
   * to easily compile an element with options like in a Directive Definition Object.
   *
   * > The compiler powers a lot of components inside of AngularJS Material.
   * > Like the `$mdPanel` or `$mdDialog`.
   *
   * @usage
   *
   * Basic Usage with a template
   *
   * <hljs lang="js">
   *   $mdCompiler.compile({
   *     templateUrl: 'modal.html',
   *     controller: 'ModalCtrl',
   *     locals: {
   *       modal: myModalInstance;
   *     }
   *   }).then(function (compileData) {
   *     compileData.element; // Compiled DOM element
   *     compileData.link(myScope); // Instantiate controller and link element to scope.
   *   });
   * </hljs>
   *
   * Example with a content element
   *
   * <hljs lang="js">
   *
   *   // Create a virtual element and link it manually.
   *   // The compiler doesn't need to recompile the element each time.
   *   var myElement = $compile('<span>Test</span>')(myScope);
   *
   *   $mdCompiler.compile({
   *     contentElement: myElement
   *   }).then(function (compileData) {
   *     compileData.element // Content Element (same as above)
   *     compileData.link // This does nothing when using a contentElement.
   *   });
   * </hljs>
   *
   * > Content Element is a significant performance improvement when the developer already knows that the
   * > compiled element will be always the same and the scope will not change either.
   *
   * The `contentElement` option also supports DOM elements which will be temporary removed and restored
   * at its old position.
   *
   * <hljs lang="js">
   *   var domElement = document.querySelector('#myElement');
   *
   *   $mdCompiler.compile({
   *     contentElement: myElement
   *   }).then(function (compileData) {
   *     compileData.element // Content Element (same as above)
   *     compileData.link // This does nothing when using a contentElement.
   *   });
   * </hljs>
   *
   * The `$mdCompiler` can also query for the element in the DOM itself.
   *
   * <hljs lang="js">
   *   $mdCompiler.compile({
   *     contentElement: '#myElement'
   *   }).then(function (compileData) {
   *     compileData.element // Content Element (same as above)
   *     compileData.link // This does nothing when using a contentElement.
   *   });
   * </hljs>
   *
   */
  function MdCompilerService($q, $templateRequest, $injector, $compile, $controller) {

    /** @private @const {!angular.$q} */
    this.$q = $q;

    /** @private @const {!angular.$templateRequest} */
    this.$templateRequest = $templateRequest;

    /** @private @const {!angular.$injector} */
    this.$injector = $injector;

    /** @private @const {!angular.$compile} */
    this.$compile = $compile;

    /** @private @const {!angular.$controller} */
    this.$controller = $controller;
  }

  /**
   * @ngdoc method
   * @name $mdCompiler#compile
   * @description
   *
   * A method to compile a HTML template with the AngularJS compiler.
   * The `$mdCompiler` is wrapper around the AngularJS compiler and provides extra functionality
   * like controller instantiation or async resolves.
   *
   * @param {!Object} options An options object, with the following properties:
   *
   *    - `controller` - `{string|function}` Controller fn that should be associated with
   *         newly created scope or the name of a registered controller if passed as a string.
   *    - `controllerAs` - `{string=}` A controller alias name. If present the controller will be
   *         published to scope under the `controllerAs` name.
   *    - `contentElement` - `{string|Element}`: Instead of using a template, which will be
   *         compiled each time, you can also use a DOM element.<br/>
   *    - `template` - `{string=}` An html template as a string.
   *    - `templateUrl` - `{string=}` A path to an html template.
   *    - `transformTemplate` - `{function(template)=}` A function which transforms the template after
   *        it is loaded. It will be given the template string as a parameter, and should
   *        return a a new string representing the transformed template.
   *    - `resolve` - `{Object.<string, function>=}` - An optional map of dependencies which should
   *        be injected into the controller. If any of these dependencies are promises, the compiler
   *        will wait for them all to be resolved, or if one is rejected before the controller is
   *        instantiated `compile()` will fail..
   *      * `key` - `{string}`: a name of a dependency to be injected into the controller.
   *      * `factory` - `{string|function}`: If `string` then it is an alias for a service.
   *        Otherwise if function, then it is injected and the return value is treated as the
   *        dependency. If the result is a promise, it is resolved before its value is
   *        injected into the controller.
   *
   * @returns {Object} promise A promise, which will be resolved with a `compileData` object.
   * `compileData` has the following properties:
   *
   *   - `element` - `{Element}`: an uncompiled element matching the provided template.
   *   - `link` - `{function(scope)}`: A link function, which, when called, will compile
   *     the element and instantiate the provided controller (if given).
   *   - `locals` - `{Object}`: The locals which will be passed into the controller once `link` is
   *     called. If `bindToController` is true, they will be copied to the ctrl instead
   */
  MdCompilerService.prototype.compile = function(options) {

    if (options.contentElement) {
      return this._prepareContentElement(options);
    } else {
      return this._compileTemplate(options);
    }

  };

  /**
   * Instead of compiling any template, the compiler just fetches an existing HTML element from the DOM and
   * provides a restore function to put the element back it old DOM position.
   * @param {!Object} options Options to be used for the compiler.
   */
  MdCompilerService.prototype._prepareContentElement = function(options) {

    var contentElement = this._fetchContentElement(options);

    return this.$q.resolve({
      element: contentElement.element,
      cleanup: contentElement.restore,
      locals: {},
      link: function() {
        return contentElement.element;
      }
    });

  };

  /**
   * Compiles a template by considering all options and waiting for all resolves to be ready.
   * @param {!Object} options Compile options
   * @returns {!Object} Compile data with link function.
   */
  MdCompilerService.prototype._compileTemplate = function(options) {

    var self = this;
    var templateUrl = options.templateUrl;
    var template = options.template || '';
    var resolve = angular.extend({}, options.resolve);
    var locals = angular.extend({}, options.locals);
    var transformTemplate = options.transformTemplate || angular.identity;

    // Take resolve values and invoke them.
    // Resolves can either be a string (value: 'MyRegisteredAngularConst'),
    // or an invokable 'factory' of sorts: (value: function ValueGetter($dependency) {})
    angular.forEach(resolve, function(value, key) {
      if (angular.isString(value)) {
        resolve[key] = self.$injector.get(value);
      } else {
        resolve[key] = self.$injector.invoke(value);
      }
    });

    // Add the locals, which are just straight values to inject
    // eg locals: { three: 3 }, will inject three into the controller
    angular.extend(resolve, locals);

    if (templateUrl) {
      resolve.$$ngTemplate = this.$templateRequest(templateUrl);
    } else {
      resolve.$$ngTemplate = this.$q.when(template);
    }


    // Wait for all the resolves to finish if they are promises
    return this.$q.all(resolve).then(function(locals) {

      var template = transformTemplate(locals.$$ngTemplate, options);
      var element = options.element || angular.element('<div>').html(template.trim()).contents();

      return self._compileElement(locals, element, options);
    });

  };

  /**
   * Method to compile an element with the given options.
   * @param {!Object} locals Locals to be injected to the controller if present
   * @param {!JQLite} element Element to be compiled and linked
   * @param {!Object} options Options to be used for linking.
   * @returns {!Object} Compile data with link function.
   */
  MdCompilerService.prototype._compileElement = function(locals, element, options) {
    var self = this;
    var ngLinkFn = this.$compile(element);

    var compileData = {
      element: element,
      cleanup: element.remove.bind(element),
      locals: locals,
      link: linkFn
    };

    function linkFn(scope) {
      locals.$scope = scope;

      // Instantiate controller if the developer provided one.
      if (options.controller) {

        var injectLocals = angular.extend({}, locals, {
          $element: element
        });

        // Create the specified controller instance.
        var ctrl = self._createController(options, injectLocals, locals);

        // Unique identifier for AngularJS Route ngView controllers.
        element.data('$ngControllerController', ctrl);
        element.children().data('$ngControllerController', ctrl);

        // Expose the instantiated controller to the compile data
        compileData.controller = ctrl;
      }

      // Invoke the AngularJS $compile link function.
      return ngLinkFn(scope);
    }

    return compileData;

  };

  /**
   * Creates and instantiates a new controller with the specified options.
   * @param {!Object} options Options that include the controller function or string.
   * @param {!Object} injectLocals Locals to to be provided in the controller DI.
   * @param {!Object} locals Locals to be injected to the controller.
   * @returns {!Object} Created controller instance.
   */
  MdCompilerService.prototype._createController = function(options, injectLocals, locals) {
    var ctrl;
    var preAssignBindingsEnabled = getPreAssignBindingsEnabled();
    // The third argument to $controller is considered private and undocumented:
    // https://github.com/angular/angular.js/blob/v1.6.10/src/ng/controller.js#L102-L109.
    // TODO remove the use of this third argument in AngularJS Material 1.2.0.
    // Passing `true` as the third argument causes `$controller` to return a function that
    // gets the controller instance instead of returning the instance directly. When the
    // controller is defined as a function, `invokeCtrl.instance` is the *same instance* as
    // `invokeCtrl()`. However, when the controller is an ES6 class, `invokeCtrl.instance` is a
    // *different instance* from `invokeCtrl()`.
    if (preAssignBindingsEnabled) {
      var invokeCtrl = this.$controller(options.controller, injectLocals, true);

      if (options.bindToController) {
        angular.extend(invokeCtrl.instance, locals);
      }

      // Use the private API callback to instantiate and initialize the specified controller.
      ctrl = invokeCtrl();
    } else {
      // If we don't need to pre-assign bindings, avoid using the private API third argument and
      // related callback.
      ctrl = this.$controller(options.controller, injectLocals);

      if (options.bindToController) {
        angular.extend(ctrl, locals);
      }
    }

    if (options.controllerAs) {
      injectLocals.$scope[options.controllerAs] = ctrl;
    }

    // Call the $onInit hook if it's present on the controller.
    angular.isFunction(ctrl.$onInit) && ctrl.$onInit();

    return ctrl;
  };

  /**
   * Fetches an element removing it from the DOM and using it temporary for the compiler.
   * Elements which were fetched will be restored after use.
   * @param {!Object} options Options to be used for the compilation.
   * @returns {{element: !JQLite, restore: !function}}
   */
  MdCompilerService.prototype._fetchContentElement = function(options) {

    var contentEl = options.contentElement;
    var restoreFn = null;

    if (angular.isString(contentEl)) {
      contentEl = document.querySelector(contentEl);
      restoreFn = createRestoreFn(contentEl);
    } else {
      contentEl = contentEl[0] || contentEl;

      // When the element is visible in the DOM, then we restore it at close of the dialog.
      // Otherwise it will be removed from the DOM after close.
      if (document.contains(contentEl)) {
        restoreFn = createRestoreFn(contentEl);
      } else {
        restoreFn = function() {
          if (contentEl.parentNode) {
            contentEl.parentNode.removeChild(contentEl);
          }
        };
      }
    }

    return {
      element: angular.element(contentEl),
      restore: restoreFn
    };

    function createRestoreFn(element) {
      var parent = element.parentNode;
      var nextSibling = element.nextElementSibling;

      return function() {
        if (!nextSibling) {
          // When the element didn't had any sibling, then it can be simply appended to the
          // parent, because it plays no role, which index it had before.
          parent.appendChild(element);
        } else {
          // When the element had a sibling, which marks the previous position of the element
          // in the DOM, we insert it correctly before the sibling, to have the same index as
          // before.
          parent.insertBefore(element, nextSibling);
        }
      };
    }
  };
}



MdGesture['$inject'] = ["$$MdGestureHandler", "$$rAF", "$timeout"];
attachToDocument['$inject'] = ["$mdGesture", "$$MdGestureHandler"];var HANDLERS = {};

/**
 * The state of the current 'pointer'. The pointer represents the state of the current touch.
 * It contains normalized x and y coordinates from DOM events,
 * as well as other information abstracted from the DOM.
 */
var pointer, lastPointer, maxClickDistance = 6;
var forceSkipClickHijack = false, disableAllGestures = false;

/**
 * The position of the most recent click if that click was on a label element.
 * @type {{x: number, y: number}|null}
 */
var lastLabelClickPos = null;

// Used to attach event listeners once when multiple ng-apps are running.
var isInitialized = false;

var userAgent = navigator.userAgent || navigator.vendor || window.opera;
var isIos = userAgent.match(/ipad|iphone|ipod/i);
var isAndroid = userAgent.match(/android/i);

/**
 * @ngdoc module
 * @name material.core.gestures
 * @description
 * AngularJS Material Gesture handling for touch devices. This module replaced the usage of the hammerjs library.
 */
angular
  .module('material.core.gestures', [])
  .provider('$mdGesture', MdGestureProvider)
  .factory('$$MdGestureHandler', MdGestureHandler)
  .run(attachToDocument);

/**
 * @ngdoc service
 * @name $mdGestureProvider
 * @module material.core.gestures
 *
 * @description
 * In some scenarios on mobile devices (without jQuery), the click events should NOT be hijacked.
 * `$mdGestureProvider` is used to configure the Gesture module to ignore or skip click hijacking on mobile
 * devices.
 *
 * You can also change the max click distance, `6px` by default, if you have issues on some touch screens.
 *
 * <hljs lang="js">
 *   app.config(function($mdGestureProvider) {
 *
 *     // For mobile devices without jQuery loaded, do not
 *     // intercept click events during the capture phase.
 *     $mdGestureProvider.skipClickHijack();
 *
 *     // If hijacking clicks, you may want to change the default click distance
 *     $mdGestureProvider.setMaxClickDistance(12);
 *   });
 * </hljs>
 *
 */
function MdGestureProvider() { }

MdGestureProvider.prototype = {

  /**
   * @ngdoc method
   * @name $mdGestureProvider#disableAll
   *
   * @description
   * Disable all gesture detection. This can be beneficial to application performance
   * and memory usage.
   */
  disableAll: function () {
    disableAllGestures = true;
  },

  // Publish access to setter to configure a variable BEFORE the
  // $mdGesture service is instantiated...
  /**
   * @ngdoc method
   * @name $mdGestureProvider#skipClickHijack
   *
   * @description
   * Tell the AngularJS Material Gesture module to skip (or ignore) click hijacking on mobile devices.
   */
  skipClickHijack: function() {
    return forceSkipClickHijack = true;
  },

  /**
   * @ngdoc method
   * @name $mdGestureProvider#setMaxClickDistance
   * @param clickDistance {string} Distance in pixels. I.e. `12px`.
   * @description
   * Set the max distance from the origin of the touch event to trigger touch handlers.
   */
  setMaxClickDistance: function(clickDistance) {
    maxClickDistance = parseInt(clickDistance);
  },

  /**
   * $get is used to build an instance of $mdGesture
   * ngInject
   */
  $get : ["$$MdGestureHandler", "$$rAF", "$timeout", function($$MdGestureHandler, $$rAF, $timeout) {
       return new MdGesture($$MdGestureHandler, $$rAF, $timeout);
  }]
};



/**
 * MdGesture factory construction function
 * ngInject
 */
function MdGesture($$MdGestureHandler, $$rAF, $timeout) {
  var touchActionProperty = getTouchAction();
  var hasJQuery =  (typeof window.jQuery !== 'undefined') && (angular.element === window.jQuery);

  var self = {
    handler: addHandler,
    register: register,
    isAndroid: isAndroid,
    isIos: isIos,
    // On mobile w/out jQuery, we normally intercept clicks. Should we skip that?
    isHijackingClicks: (isIos || isAndroid) && !hasJQuery && !forceSkipClickHijack
  };

  if (self.isHijackingClicks) {
    self.handler('click', {
      options: {
        maxDistance: maxClickDistance
      },
      onEnd: checkDistanceAndEmit('click')
    });

    self.handler('focus', {
      options: {
        maxDistance: maxClickDistance
      },
      onEnd: function(ev, pointer) {
        if (pointer.distance < this.state.options.maxDistance && canFocus(ev.target)) {
          this.dispatchEvent(ev, 'focus', pointer);
          ev.target.focus();
        }
      }
    });

    self.handler('mouseup', {
      options: {
        maxDistance: maxClickDistance
      },
      onEnd: checkDistanceAndEmit('mouseup')
    });

    self.handler('mousedown', {
      onStart: function(ev) {
        this.dispatchEvent(ev, 'mousedown');
      }
    });
  }

  function checkDistanceAndEmit(eventName) {
    return function(ev, pointer) {
      if (pointer.distance < this.state.options.maxDistance) {
        this.dispatchEvent(ev, eventName, pointer);
      }
    };
  }

  /**
   * Register an element to listen for a handler.
   * This allows an element to override the default options for a handler.
   * Additionally, some handlers like drag and hold only dispatch events if
   * the domEvent happens inside an element that's registered to listen for these events.
   *
   * @see GestureHandler for how overriding of default options works.
   * @example $mdGesture.register(myElement, 'drag', { minDistance: 20, horizontal: false })
   */
  function register(element, handlerName, options) {
    var handler = HANDLERS[handlerName.replace(/^\$md./, '')];
    if (!handler) {
      throw new Error('Failed to register element with handler ' + handlerName + '. ' +
      'Available handlers: ' + Object.keys(HANDLERS).join(', '));
    }
    return handler.registerElement(element, options);
  }

  /*
   * add a handler to $mdGesture. see below.
   */
  function addHandler(name, definition) {
    var handler = new $$MdGestureHandler(name);
    angular.extend(handler, definition);
    HANDLERS[name] = handler;

    return self;
  }

  /**
   * Register handlers. These listen to touch/start/move events, interpret them,
   * and dispatch gesture events depending on options & conditions. These are all
   * instances of GestureHandler.
   * @see GestureHandler
   */
  return self
    /*
     * The press handler dispatches an event on touchdown/touchend.
     * It's a simple abstraction of touch/mouse/pointer start and end.
     */
    .handler('press', {
      onStart: function (ev, pointer) {
        this.dispatchEvent(ev, '$md.pressdown');
      },
      onEnd: function (ev, pointer) {
        this.dispatchEvent(ev, '$md.pressup');
      }
    })

    /*
     * The hold handler dispatches an event if the user keeps their finger within
     * the same <maxDistance> area for <delay> ms.
     * The hold handler will only run if a parent of the touch target is registered
     * to listen for hold events through $mdGesture.register()
     */
    .handler('hold', {
      options: {
        maxDistance: 6,
        delay: 500
      },
      onCancel: function () {
        $timeout.cancel(this.state.timeout);
      },
      onStart: function (ev, pointer) {
        // For hold, require a parent to be registered with $mdGesture.register()
        // Because we prevent scroll events, this is necessary.
        if (!this.state.registeredParent) return this.cancel();

        this.state.pos = {x: pointer.x, y: pointer.y};
        this.state.timeout = $timeout(angular.bind(this, function holdDelayFn() {
          this.dispatchEvent(ev, '$md.hold');
          this.cancel(); // we're done!
        }), this.state.options.delay, false);
      },
      onMove: function (ev, pointer) {
        // Don't scroll while waiting for hold.
        // If we don't preventDefault touchmove events here, Android will assume we don't
        // want to listen to anymore touch events. It will start scrolling and stop sending
        // touchmove events.
        if (!touchActionProperty && ev.type === 'touchmove') ev.preventDefault();

        // If the user moves greater than <maxDistance> pixels, stop the hold timer
        // set in onStart
        var dx = this.state.pos.x - pointer.x;
        var dy = this.state.pos.y - pointer.y;
        if (Math.sqrt(dx * dx + dy * dy) > this.options.maxDistance) {
          this.cancel();
        }
      },
      onEnd: function () {
        this.onCancel();
      }
    })

    /*
     * The drag handler dispatches a drag event if the user holds and moves his finger greater than
     * <minDistance> px in the x or y direction, depending on options.horizontal.
     * The drag will be cancelled if the user moves his finger greater than <minDistance>*<cancelMultiplier> in
     * the perpendicular direction. Eg if the drag is horizontal and the user moves his finger <minDistance>*<cancelMultiplier>
     * pixels vertically, this handler won't consider the move part of a drag.
     */
    .handler('drag', {
      options: {
        minDistance: 6,
        horizontal: true,
        cancelMultiplier: 1.5
      },
      onSetup: function(element, options) {
        if (touchActionProperty) {
          // We check for horizontal to be false, because otherwise we would overwrite the default opts.
          this.oldTouchAction = element[0].style[touchActionProperty];
          element[0].style[touchActionProperty] = options.horizontal ? 'pan-y' : 'pan-x';
        }
      },
      onCleanup: function(element) {
        if (this.oldTouchAction) {
          element[0].style[touchActionProperty] = this.oldTouchAction;
        }
      },
      onStart: function (ev) {
        // For drag, require a parent to be registered with $mdGesture.register()
        if (!this.state.registeredParent) this.cancel();
      },
      onMove: function (ev, pointer) {
        var shouldStartDrag, shouldCancel;
        // Don't scroll while deciding if this touchmove qualifies as a drag event.
        // If we don't preventDefault touchmove events here, Android will assume we don't
        // want to listen to anymore touch events. It will start scrolling and stop sending
        // touchmove events.
        if (!touchActionProperty && ev.type === 'touchmove') ev.preventDefault();

        if (!this.state.dragPointer) {
          if (this.state.options.horizontal) {
            shouldStartDrag = Math.abs(pointer.distanceX) > this.state.options.minDistance;
            shouldCancel = Math.abs(pointer.distanceY) > this.state.options.minDistance * this.state.options.cancelMultiplier;
          } else {
            shouldStartDrag = Math.abs(pointer.distanceY) > this.state.options.minDistance;
            shouldCancel = Math.abs(pointer.distanceX) > this.state.options.minDistance * this.state.options.cancelMultiplier;
          }

          if (shouldStartDrag) {
            // Create a new pointer representing this drag, starting at this point where the drag started.
            this.state.dragPointer = makeStartPointer(ev);
            updatePointerState(ev, this.state.dragPointer);
            this.dispatchEvent(ev, '$md.dragstart', this.state.dragPointer);

          } else if (shouldCancel) {
            this.cancel();
          }
        } else {
          this.dispatchDragMove(ev);
        }
      },
      // Only dispatch dragmove events every frame; any more is unnecessary
      dispatchDragMove: $$rAF.throttle(function (ev) {
        // Make sure the drag didn't stop while waiting for the next frame
        if (this.state.isRunning) {
          updatePointerState(ev, this.state.dragPointer);
          this.dispatchEvent(ev, '$md.drag', this.state.dragPointer);
        }
      }),
      onEnd: function (ev, pointer) {
        if (this.state.dragPointer) {
          updatePointerState(ev, this.state.dragPointer);
          this.dispatchEvent(ev, '$md.dragend', this.state.dragPointer);
        }
      }
    })

    /*
     * The swipe handler will dispatch a swipe event if, on the end of a touch,
     * the velocity and distance were high enough.
     */
    .handler('swipe', {
      options: {
        minVelocity: 0.65,
        minDistance: 10
      },
      onEnd: function (ev, pointer) {
        var eventType;

        if (Math.abs(pointer.velocityX) > this.state.options.minVelocity &&
          Math.abs(pointer.distanceX) > this.state.options.minDistance) {
          eventType = pointer.directionX == 'left' ? '$md.swipeleft' : '$md.swiperight';
          this.dispatchEvent(ev, eventType);
        }
        else if (Math.abs(pointer.velocityY) > this.state.options.minVelocity &&
          Math.abs(pointer.distanceY) > this.state.options.minDistance) {
          eventType = pointer.directionY == 'up' ? '$md.swipeup' : '$md.swipedown';
          this.dispatchEvent(ev, eventType);
        }
      }
    });

  function getTouchAction() {
    var testEl = document.createElement('div');
    var vendorPrefixes = ['', 'webkit', 'Moz', 'MS', 'ms', 'o'];

    for (var i = 0; i < vendorPrefixes.length; i++) {
      var prefix = vendorPrefixes[i];
      var property = prefix ? prefix + 'TouchAction' : 'touchAction';
      if (angular.isDefined(testEl.style[property])) {
        return property;
      }
    }
  }

}

/**
 * MdGestureHandler
 * A GestureHandler is an object which is able to dispatch custom dom events
 * based on native dom {touch,pointer,mouse}{start,move,end} events.
 *
 * A gesture will manage its lifecycle through the start,move,end, and cancel
 * functions, which are called by native dom events.
 *
 * A gesture has the concept of 'options' (eg a swipe's required velocity), which can be
 * overridden by elements registering through $mdGesture.register()
 */
function GestureHandler (name) {
  this.name = name;
  this.state = {};
}

function MdGestureHandler() {
  var hasJQuery =  (typeof window.jQuery !== 'undefined') && (angular.element === window.jQuery);

  GestureHandler.prototype = {
    options: {},
    // jQuery listeners don't work with custom DOMEvents, so we have to dispatch events
    // differently when jQuery is loaded
    dispatchEvent: hasJQuery ?  jQueryDispatchEvent : nativeDispatchEvent,

    // These are overridden by the registered handler
    onSetup: angular.noop,
    onCleanup: angular.noop,
    onStart: angular.noop,
    onMove: angular.noop,
    onEnd: angular.noop,
    onCancel: angular.noop,

    // onStart sets up a new state for the handler, which includes options from the
    // nearest registered parent element of ev.target.
    start: function (ev, pointer) {
      if (this.state.isRunning) return;
      var parentTarget = this.getNearestParent(ev.target);
      // Get the options from the nearest registered parent
      var parentTargetOptions = parentTarget && parentTarget.$mdGesture[this.name] || {};

      this.state = {
        isRunning: true,
        // Override the default options with the nearest registered parent's options
        options: angular.extend({}, this.options, parentTargetOptions),
        // Pass in the registered parent node to the state so the onStart listener can use
        registeredParent: parentTarget
      };
      this.onStart(ev, pointer);
    },
    move: function (ev, pointer) {
      if (!this.state.isRunning) return;
      this.onMove(ev, pointer);
    },
    end: function (ev, pointer) {
      if (!this.state.isRunning) return;
      this.onEnd(ev, pointer);
      this.state.isRunning = false;
    },
    cancel: function (ev, pointer) {
      this.onCancel(ev, pointer);
      this.state = {};
    },

    // Find and return the nearest parent element that has been registered to
    // listen for this handler via $mdGesture.register(element, 'handlerName').
    getNearestParent: function (node) {
      var current = node;
      while (current) {
        if ((current.$mdGesture || {})[this.name]) {
          return current;
        }
        current = current.parentNode;
      }
      return null;
    },

    // Called from $mdGesture.register when an element registers itself with a handler.
    // Store the options the user gave on the DOMElement itself. These options will
    // be retrieved with getNearestParent when the handler starts.
    registerElement: function (element, options) {
      var self = this;
      element[0].$mdGesture = element[0].$mdGesture || {};
      element[0].$mdGesture[this.name] = options || {};
      element.on('$destroy', onDestroy);

      self.onSetup(element, options || {});

      return onDestroy;

      function onDestroy() {
        delete element[0].$mdGesture[self.name];
        element.off('$destroy', onDestroy);

        self.onCleanup(element, options || {});
      }
    }
  };

  return GestureHandler;

  /**
   * Dispatch an event with jQuery
   * TODO: Make sure this sends bubbling events
   *
   * @param srcEvent the original DOM touch event that started this.
   * @param eventType the name of the custom event to send (eg 'click' or '$md.drag')
   * @param eventPointer the pointer object that matches this event.
   */
  function jQueryDispatchEvent(srcEvent, eventType, eventPointer) {
    eventPointer = eventPointer || pointer;
    var eventObj = new angular.element.Event(eventType);

    eventObj.$material = true;
    eventObj.pointer = eventPointer;
    eventObj.srcEvent = srcEvent;

    angular.extend(eventObj, {
      clientX: eventPointer.x,
      clientY: eventPointer.y,
      screenX: eventPointer.x,
      screenY: eventPointer.y,
      pageX: eventPointer.x,
      pageY: eventPointer.y,
      ctrlKey: srcEvent.ctrlKey,
      altKey: srcEvent.altKey,
      shiftKey: srcEvent.shiftKey,
      metaKey: srcEvent.metaKey
    });
    angular.element(eventPointer.target).trigger(eventObj);
  }

  /**
   * NOTE: nativeDispatchEvent is very performance sensitive.
   * @param srcEvent the original DOM touch event that started this.
   * @param eventType the name of the custom event to send (eg 'click' or '$md.drag')
   * @param eventPointer the pointer object that matches this event.
   */
  function nativeDispatchEvent(srcEvent, eventType, eventPointer) {
    eventPointer = eventPointer || pointer;
    var eventObj;

    if (eventType === 'click' || eventType === 'mouseup' || eventType === 'mousedown') {
      if (typeof window.MouseEvent === "function") {
        eventObj = new MouseEvent(eventType, {
          bubbles: true,
          cancelable: true,
          screenX: Number(srcEvent.screenX),
          screenY: Number(srcEvent.screenY),
          clientX: Number(eventPointer.x),
          clientY: Number(eventPointer.y),
          ctrlKey: srcEvent.ctrlKey,
          altKey: srcEvent.altKey,
          shiftKey: srcEvent.shiftKey,
          metaKey: srcEvent.metaKey,
          button: srcEvent.button,
          buttons: srcEvent.buttons,
          relatedTarget: srcEvent.relatedTarget || null
        });
      } else {
        eventObj = document.createEvent('MouseEvents');
        // This has been deprecated
        // https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/initMouseEvent
        eventObj.initMouseEvent(
          eventType, true, true, window, srcEvent.detail,
          eventPointer.x, eventPointer.y, eventPointer.x, eventPointer.y,
          srcEvent.ctrlKey, srcEvent.altKey, srcEvent.shiftKey, srcEvent.metaKey,
          srcEvent.button, srcEvent.relatedTarget || null
        );
      }
    } else {
      if (typeof window.CustomEvent === "function") {
        eventObj = new CustomEvent(eventType, {
          bubbles: true,
          cancelable: true,
          detail: {}
        });
      } else {
        // This has been deprecated
        // https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/initCustomEvent
        eventObj = document.createEvent('CustomEvent');
        eventObj.initCustomEvent(eventType, true, true, {});
      }
    }
    eventObj.$material = true;
    eventObj.pointer = eventPointer;
    eventObj.srcEvent = srcEvent;
    eventPointer.target.dispatchEvent(eventObj);
  }
}

/**
 * Attach Gestures: hook document and check shouldHijack clicks
 * ngInject
 */
function attachToDocument($mdGesture, $$MdGestureHandler) {
  if (disableAllGestures) {
    return;
  }

  // Polyfill document.contains for IE11.
  // TODO: move to util
  document.contains || (document.contains = function (node) {
    return document.body.contains(node);
  });

  if (!isInitialized && $mdGesture.isHijackingClicks) {
    /*
     * If hijack clicks is true, we preventDefault any click that wasn't
     * sent by AngularJS Material. This is because on older Android & iOS, a false, or 'ghost',
     * click event will be sent ~400ms after a touchend event happens.
     * The only way to know if this click is real is to prevent any normal
     * click events, and add a flag to events sent by material so we know not to prevent those.
     *
     * Two exceptions to click events that should be prevented are:
     *  - click events sent by the keyboard (eg form submit)
     *  - events that originate from an Ionic app
     */
    document.addEventListener('click'    , clickHijacker     , true);
    document.addEventListener('mouseup'  , mouseInputHijacker, true);
    document.addEventListener('mousedown', mouseInputHijacker, true);
    document.addEventListener('focus'    , mouseInputHijacker, true);

    isInitialized = true;
  }

  function mouseInputHijacker(ev) {
    var isKeyClick = !ev.clientX && !ev.clientY;

    if (
      !isKeyClick &&
      !ev.$material &&
      !ev.isIonicTap &&
      !isInputEventFromLabelClick(ev) &&
      (ev.type !== 'mousedown' || (!canFocus(ev.target) && !canFocus(document.activeElement)))
    ) {
      ev.preventDefault();
      ev.stopPropagation();
    }
  }

  /**
   * Ignore click events that don't come from AngularJS Material, Ionic, Input Label clicks,
   * or key presses that generate click events. This helps to ignore the ghost tap events on
   * older mobile browsers that get sent after a 300-400ms delay.
   * @param ev MouseEvent or modified MouseEvent with $material, pointer, and other fields
   */
  function clickHijacker(ev) {
    var isKeyClick;
    if (isIos) {
      isKeyClick = angular.isDefined(ev.webkitForce) && ev.webkitForce === 0;
    } else {
      isKeyClick = ev.clientX === 0 && ev.clientY === 0;
    }
    if (!isKeyClick && !ev.$material && !ev.isIonicTap && !isInputEventFromLabelClick(ev)) {
      ev.preventDefault();
      ev.stopPropagation();
      lastLabelClickPos = null;
    } else {
      lastLabelClickPos = null;
      if (ev.target.tagName.toLowerCase() === 'label') {
        lastLabelClickPos = {x: ev.x, y: ev.y};
      }
    }
  }


  // Listen to all events to cover all platforms.
  var START_EVENTS = 'mousedown touchstart pointerdown';
  var MOVE_EVENTS = 'mousemove touchmove pointermove';
  var END_EVENTS = 'mouseup mouseleave touchend touchcancel pointerup pointercancel';

  angular.element(document)
    .on(START_EVENTS, gestureStart)
    .on(MOVE_EVENTS, gestureMove)
    .on(END_EVENTS, gestureEnd)
    // For testing
    .on('$$mdGestureReset', function gestureClearCache () {
      lastPointer = pointer = null;
    });

  /**
   * When a DOM event happens, run all registered gesture handlers' lifecycle
   * methods which match the DOM event.
   * Eg. when a 'touchstart' event happens, runHandlers('start') will call and
   * run `handler.cancel()` and `handler.start()` on all registered handlers.
   */
  function runHandlers(handlerEvent, event) {
    var handler;
    for (var name in HANDLERS) {
      handler = HANDLERS[name];
      if (handler instanceof $$MdGestureHandler) {

        if (handlerEvent === 'start') {
          // Run cancel to reset any handlers' state
          handler.cancel();
        }
        handler[handlerEvent](event, pointer);
      }
    }
  }

  /*
   * gestureStart vets if a start event is legitimate (and not part of a 'ghost click' from iOS/Android)
   * If it is legitimate, we initiate the pointer state and mark the current pointer's type
   * For example, for a touchstart event, mark the current pointer as a 'touch' pointer, so mouse events
   * won't effect it.
   */
  function gestureStart(ev) {
    // If we're already touched down, abort
    if (pointer) return;

    var now = +Date.now();

    // iOS & old android bug: after a touch event, a click event is sent 350 ms later.
    // If <400ms have passed, don't allow an event of a different type than the previous event
    if (lastPointer && !typesMatch(ev, lastPointer) && (now - lastPointer.endTime < 1500)) {
      return;
    }

    pointer = makeStartPointer(ev);

    runHandlers('start', ev);
  }

  /**
   * If a move event happens of the right type, update the pointer and run all the move handlers.
   * "of the right type": if a mousemove happens but our pointer started with a touch event, do
   * nothing.
   */
  function gestureMove(ev) {
    if (!pointer || !typesMatch(ev, pointer)) return;

    updatePointerState(ev, pointer);
    runHandlers('move', ev);
  }

  /**
   * If an end event happens of the right type, update the pointer, run endHandlers, and save the
   * pointer as 'lastPointer'.
   */
  function gestureEnd(ev) {
    if (!pointer || !typesMatch(ev, pointer)) return;

    updatePointerState(ev, pointer);
    pointer.endTime = +Date.now();

    if (ev.type !== 'pointercancel') {
      runHandlers('end', ev);
    }

    lastPointer = pointer;
    pointer = null;
  }

}

// ********************
// Module Functions
// ********************

/*
 * Initiate the pointer. x, y, and the pointer's type.
 */
function makeStartPointer(ev) {
  var point = getEventPoint(ev);
  var startPointer = {
    startTime: +Date.now(),
    target: ev.target,
    // 'p' for pointer events, 'm' for mouse, 't' for touch
    type: ev.type.charAt(0)
  };
  startPointer.startX = startPointer.x = point.pageX;
  startPointer.startY = startPointer.y = point.pageY;
  return startPointer;
}

/*
 * return whether the pointer's type matches the event's type.
 * Eg if a touch event happens but the pointer has a mouse type, return false.
 */
function typesMatch(ev, pointer) {
  return ev && pointer && ev.type.charAt(0) === pointer.type;
}

/**
 * Gets whether the given event is an input event that was caused by clicking on an
 * associated label element.
 *
 * This is necessary because the browser will, upon clicking on a label element, fire an
 * *extra* click event on its associated input (if any). mdGesture is able to flag the label
 * click as with `$material` correctly, but not the second input click.
 *
 * In order to determine whether an input event is from a label click, we compare the (x, y) for
 * the event to the (x, y) for the most recent label click (which is cleared whenever a non-label
 * click occurs). Unfortunately, there are no event properties that tie the input and the label
 * together (such as relatedTarget).
 *
 * @param {MouseEvent} event
 * @returns {boolean}
 */
function isInputEventFromLabelClick(event) {
  return lastLabelClickPos
      && lastLabelClickPos.x === event.x
      && lastLabelClickPos.y === event.y;
}

/*
 * Update the given pointer based upon the given DOMEvent.
 * Distance, velocity, direction, duration, etc
 */
function updatePointerState(ev, pointer) {
  var point = getEventPoint(ev);
  var x = pointer.x = point.pageX;
  var y = pointer.y = point.pageY;

  pointer.distanceX = x - pointer.startX;
  pointer.distanceY = y - pointer.startY;
  pointer.distance = Math.sqrt(
    pointer.distanceX * pointer.distanceX + pointer.distanceY * pointer.distanceY
  );

  pointer.directionX = pointer.distanceX > 0 ? 'right' : pointer.distanceX < 0 ? 'left' : '';
  pointer.directionY = pointer.distanceY > 0 ? 'down' : pointer.distanceY < 0 ? 'up' : '';

  pointer.duration = +Date.now() - pointer.startTime;
  pointer.velocityX = pointer.distanceX / pointer.duration;
  pointer.velocityY = pointer.distanceY / pointer.duration;
}

/**
 * Normalize the point where the DOM event happened whether it's touch or mouse.
 * @returns point event obj with pageX and pageY on it.
 */
function getEventPoint(ev) {
  ev = ev.originalEvent || ev; // support jQuery events
  return (ev.touches && ev.touches[0]) ||
    (ev.changedTouches && ev.changedTouches[0]) ||
    ev;
}

/** Checks whether an element can be focused. */
function canFocus(element) {
  return (
    !!element &&
    element.getAttribute('tabindex') !== '-1' &&
    !element.hasAttribute('disabled') &&
    (
      element.hasAttribute('tabindex') ||
      element.hasAttribute('href') ||
      element.isContentEditable ||
      ['INPUT', 'SELECT', 'BUTTON', 'TEXTAREA', 'VIDEO', 'AUDIO'].indexOf(element.nodeName) !== -1
    )
  );
}

/**
 * @ngdoc module
 * @name material.core.interaction
 * @description
 * User interaction detection to provide proper accessibility.
 */
MdInteractionService['$inject'] = ["$timeout", "$mdUtil", "$rootScope"];
angular
  .module('material.core.interaction', [])
  .service('$mdInteraction', MdInteractionService);


/**
 * @ngdoc service
 * @name $mdInteraction
 * @module material.core.interaction
 *
 * @description
 *
 * Service which keeps track of the last interaction type and validates them for several browsers.
 * The service hooks into the document's body and listens for touch, mouse and keyboard events.
 *
 * The most recent interaction type can be retrieved by calling the `getLastInteractionType` method.
 *
 * Here is an example markup for using the interaction service.
 *
 * <hljs lang="js">
 *   var lastType = $mdInteraction.getLastInteractionType();
 *
 *   if (lastType === 'keyboard') {
 *     // We only restore the focus for keyboard users.
 *     restoreFocus();
 *   }
 * </hljs>
 *
 */
function MdInteractionService($timeout, $mdUtil, $rootScope) {
  this.$timeout = $timeout;
  this.$mdUtil = $mdUtil;
  this.$rootScope = $rootScope;

  // IE browsers can also trigger pointer events, which also leads to an interaction.
  this.pointerEvent = 'MSPointerEvent' in window ? 'MSPointerDown' : 'PointerEvent' in window ? 'pointerdown' : null;
  this.bodyElement = angular.element(document.body);
  this.isBuffering = false;
  this.bufferTimeout = null;
  this.lastInteractionType = null;
  this.lastInteractionTime = null;
  this.inputHandler = this.onInputEvent.bind(this);
  this.bufferedInputHandler = this.onBufferInputEvent.bind(this);

  // Type Mappings for the different events
  // There will be three three interaction types
  // `keyboard`, `mouse` and `touch`
  // type `pointer` will be evaluated in `pointerMap` for IE Browser events
  this.inputEventMap = {
    'keydown': 'keyboard',
    'mousedown': 'mouse',
    'mouseenter': 'mouse',
    'touchstart': 'touch',
    'pointerdown': 'pointer',
    'MSPointerDown': 'pointer'
  };

  // IE PointerDown events will be validated in `touch` or `mouse`
  // Index numbers referenced here: https://msdn.microsoft.com/library/windows/apps/hh466130.aspx
  this.iePointerMap = {
    2: 'touch',
    3: 'touch',
    4: 'mouse'
  };

  this.initializeEvents();
  this.$rootScope.$on('$destroy', this.deregister.bind(this));
}

/**
 * Removes all event listeners created by $mdInteration on the
 * body element.
 */
MdInteractionService.prototype.deregister = function() {

    this.bodyElement.off('keydown mousedown', this.inputHandler);

    if ('ontouchstart' in document.documentElement) {
      this.bodyElement.off('touchstart', this.bufferedInputHandler);
    }

    if (this.pointerEvent) {
      this.bodyElement.off(this.pointerEvent, this.inputHandler);
    }

};

/**
 * Initializes the interaction service, by registering all interaction events to the
 * body element.
 */
MdInteractionService.prototype.initializeEvents = function() {

  this.bodyElement.on('keydown mousedown', this.inputHandler);

  if ('ontouchstart' in document.documentElement) {
    this.bodyElement.on('touchstart', this.bufferedInputHandler);
  }

  if (this.pointerEvent) {
    this.bodyElement.on(this.pointerEvent, this.inputHandler);
  }

};

/**
 * Event listener for normal interaction events, which should be tracked.
 * @param event {MouseEvent|KeyboardEvent|PointerEvent|TouchEvent}
 */
MdInteractionService.prototype.onInputEvent = function(event) {
  if (this.isBuffering) {
    return;
  }

  var type = this.inputEventMap[event.type];

  if (type === 'pointer') {
    type = this.iePointerMap[event.pointerType] || event.pointerType;
  }

  this.lastInteractionType = type;
  this.lastInteractionTime = this.$mdUtil.now();
};

/**
 * Event listener for interaction events which should be buffered (touch events).
 * @param event {TouchEvent}
 */
MdInteractionService.prototype.onBufferInputEvent = function(event) {
  this.$timeout.cancel(this.bufferTimeout);

  this.onInputEvent(event);
  this.isBuffering = true;

  // The timeout of 650ms is needed to delay the touchstart, because otherwise the touch will call
  // the `onInput` function multiple times.
  this.bufferTimeout = this.$timeout(function() {
    this.isBuffering = false;
  }.bind(this), 650, false);

};

/**
 * @ngdoc method
 * @name $mdInteraction#getLastInteractionType
 * @description Retrieves the last interaction type triggered in body.
 * @returns {string|null} Last interaction type.
 */
MdInteractionService.prototype.getLastInteractionType = function() {
  return this.lastInteractionType;
};

/**
 * @ngdoc method
 * @name $mdInteraction#isUserInvoked
 * @description Method to detect whether any interaction happened recently or not.
 * @param {number=} checkDelay Time to check for any interaction to have been triggered.
 * @returns {boolean} Whether there was any interaction or not.
 */
MdInteractionService.prototype.isUserInvoked = function(checkDelay) {
  var delay = angular.isNumber(checkDelay) ? checkDelay : 15;

  // Check for any interaction to be within the specified check time.
  return this.lastInteractionTime >= this.$mdUtil.now() - delay;
};

angular.module('material.core')
  .provider('$$interimElement', InterimElementProvider);

/*
 * @ngdoc service
 * @name $$interimElement
 * @module material.core
 *
 * @description
 *
 * Factory that constructs `$$interimElement.$service` services.
 * Used internally in material design for elements that appear on screen temporarily.
 * The service provides a promise-like API for interacting with the temporary
 * elements.
 *
 * ```js
 * app.service('$mdToast', function($$interimElement) {
 *   var $mdToast = $$interimElement(toastDefaultOptions);
 *   return $mdToast;
 * });
 * ```
 * @param {object=} defaultOptions Options used by default for the `show` method on the service.
 *
 * @returns {$$interimElement.$service}
 *
 */

function InterimElementProvider() {
  InterimElementFactory['$inject'] = ["$document", "$q", "$rootScope", "$timeout", "$rootElement", "$animate", "$mdUtil", "$mdCompiler", "$mdTheming", "$injector", "$exceptionHandler"];
  createInterimElementProvider.$get = InterimElementFactory;
  return createInterimElementProvider;

  /**
   * Returns a new provider which allows configuration of a new interimElement
   * service. Allows configuration of default options & methods for options,
   * as well as configuration of 'preset' methods (eg dialog.basic(): basic is a preset method)
   */
  function createInterimElementProvider(interimFactoryName) {
    factory['$inject'] = ["$$interimElement", "$injector"];
    var EXPOSED_METHODS = ['onHide', 'onShow', 'onRemove'];

    var customMethods = {};
    var providerConfig = {
      presets: {}
    };

    var provider = {
      setDefaults: setDefaults,
      addPreset: addPreset,
      addMethod: addMethod,
      $get: factory
    };

    /**
     * all interim elements will come with the 'build' preset
     */
    provider.addPreset('build', {
      methods: ['controller', 'controllerAs', 'resolve', 'multiple',
        'template', 'templateUrl', 'themable', 'transformTemplate', 'parent', 'contentElement']
    });

    return provider;

    /**
     * Save the configured defaults to be used when the factory is instantiated
     */
    function setDefaults(definition) {
      providerConfig.optionsFactory = definition.options;
      providerConfig.methods = (definition.methods || []).concat(EXPOSED_METHODS);
      return provider;
    }

    /**
     * Add a method to the factory that isn't specific to any interim element operations
     */

    function addMethod(name, fn) {
      customMethods[name] = fn;
      return provider;
    }

    /**
     * Save the configured preset to be used when the factory is instantiated
     */
    function addPreset(name, definition) {
      definition = definition || {};
      definition.methods = definition.methods || [];
      definition.options = definition.options || function() { return {}; };

      if (/^cancel|hide|show$/.test(name)) {
        throw new Error("Preset '" + name + "' in " + interimFactoryName + " is reserved!");
      }
      if (definition.methods.indexOf('_options') > -1) {
        throw new Error("Method '_options' in " + interimFactoryName + " is reserved!");
      }
      providerConfig.presets[name] = {
        methods: definition.methods.concat(EXPOSED_METHODS),
        optionsFactory: definition.options,
        argOption: definition.argOption
      };
      return provider;
    }

    function addPresetMethod(presetName, methodName, method) {
      providerConfig.presets[presetName][methodName] = method;
    }

    /**
     * Create a factory that has the given methods & defaults implementing interimElement
     */
    /* ngInject */
    function factory($$interimElement, $injector) {
      var defaultMethods;
      var defaultOptions;
      var interimElementService = $$interimElement();

      /*
       * publicService is what the developer will be using.
       * It has methods hide(), cancel(), show(), build(), and any other
       * presets which were set during the config phase.
       */
      var publicService = {
        hide: interimElementService.hide,
        cancel: interimElementService.cancel,
        show: showInterimElement,

        // Special internal method to destroy an interim element without animations
        // used when navigation changes causes a $scope.$destroy() action
        destroy : destroyInterimElement
      };


      defaultMethods = providerConfig.methods || [];
      // This must be invoked after the publicService is initialized
      defaultOptions = invokeFactory(providerConfig.optionsFactory, {});

      // Copy over the simple custom methods
      angular.forEach(customMethods, function(fn, name) {
        publicService[name] = fn;
      });

      angular.forEach(providerConfig.presets, function(definition, name) {
        var presetDefaults = invokeFactory(definition.optionsFactory, {});
        var presetMethods = (definition.methods || []).concat(defaultMethods);

        // Every interimElement built with a preset has a field called `$type`,
        // which matches the name of the preset.
        // Eg in preset 'confirm', options.$type === 'confirm'
        angular.extend(presetDefaults, { $type: name });

        // This creates a preset class which has setter methods for every
        // method given in the `.addPreset()` function, as well as every
        // method given in the `.setDefaults()` function.
        //
        // @example
        // .setDefaults({
        //   methods: ['hasBackdrop', 'clickOutsideToClose', 'escapeToClose', 'targetEvent'],
        //   options: dialogDefaultOptions
        // })
        // .addPreset('alert', {
        //   methods: ['title', 'ok'],
        //   options: alertDialogOptions
        // })
        //
        // Set values will be passed to the options when interimElement.show() is called.
        function Preset(opts) {
          this._options = angular.extend({}, presetDefaults, opts);
        }
        angular.forEach(presetMethods, function(name) {
          Preset.prototype[name] = function(value) {
            this._options[name] = value;
            return this;
          };
        });

        // Create shortcut method for one-linear methods
        if (definition.argOption) {
          var methodName = 'show' + name.charAt(0).toUpperCase() + name.slice(1);
          publicService[methodName] = function(arg) {
            var config = publicService[name](arg);
            return publicService.show(config);
          };
        }

        // eg $mdDialog.alert() will return a new alert preset
        publicService[name] = function(arg) {
          // If argOption is supplied, eg `argOption: 'content'`, then we assume
          // if the argument is not an options object then it is the `argOption` option.
          //
          // @example `$mdToast.simple('hello')` // sets options.content to hello
          //                                     // because argOption === 'content'
          if (arguments.length && definition.argOption &&
              !angular.isObject(arg) && !angular.isArray(arg))  {

            return (new Preset())[definition.argOption](arg);

          } else {
            return new Preset(arg);
          }

        };
      });

      return publicService;

      /**
       *
       */
      function showInterimElement(opts) {
        // opts is either a preset which stores its options on an _options field,
        // or just an object made up of options
        opts = opts || { };
        if (opts._options) opts = opts._options;

        return interimElementService.show(
          angular.extend({}, defaultOptions, opts)
        );
      }

      /**
       *  Special method to hide and destroy an interimElement WITHOUT
       *  any 'leave` or hide animations ( an immediate force hide/remove )
       *
       *  NOTE: This calls the onRemove() subclass method for each component...
       *  which must have code to respond to `options.$destroy == true`
       */
      function destroyInterimElement(opts) {
          return interimElementService.destroy(opts);
      }

      /**
       * Helper to call $injector.invoke with a local of the factory name for
       * this provider.
       * If an $mdDialog is providing options for a dialog and tries to inject
       * $mdDialog, a circular dependency error will happen.
       * We get around that by manually injecting $mdDialog as a local.
       */
      function invokeFactory(factory, defaultVal) {
        var locals = {};
        locals[interimFactoryName] = publicService;
        return $injector.invoke(factory || function() { return defaultVal; }, {}, locals);
      }

    }

  }

  /* ngInject */
  function InterimElementFactory($document, $q, $rootScope, $timeout, $rootElement, $animate,
                                 $mdUtil, $mdCompiler, $mdTheming, $injector, $exceptionHandler) {
    return function createInterimElementService() {
      var SHOW_CANCELLED = false;

      /*
       * @ngdoc service
       * @name $$interimElement.$service
       *
       * @description
       * A service used to control inserting and removing an element into the DOM.
       *
       */

      var service;

      var showPromises = []; // Promises for the interim's which are currently opening.
      var hidePromises = []; // Promises for the interim's which are currently hiding.
      var showingInterims = []; // Interim elements which are currently showing up.

      // Publish instance $$interimElement service;
      // ... used as $mdDialog, $mdToast, $mdMenu, and $mdSelect

      return service = {
        show: show,
        hide: waitForInterim(hide),
        cancel: waitForInterim(cancel),
        destroy : destroy,
        $injector_: $injector
      };

      /*
       * @ngdoc method
       * @name $$interimElement.$service#show
       * @kind function
       *
       * @description
       * Adds the `$interimElement` to the DOM and returns a special promise that will be resolved or rejected
       * with hide or cancel, respectively. To external cancel/hide, developers should use the
       *
       * @param {*} options is hashMap of settings
       * @returns a Promise
       *
       */
      function show(options) {
        options = options || {};
        var interimElement = new InterimElement(options || {});

        // When an interim element is currently showing, we have to cancel it.
        // Just hiding it, will resolve the InterimElement's promise, the promise should be
        // rejected instead.
        var hideAction = options.multiple ? $q.resolve() : $q.all(showPromises);

        if (!options.multiple) {
          // Wait for all opening interim's to finish their transition.
          hideAction = hideAction.then(function() {
            // Wait for all closing and showing interim's to be completely closed.
            var promiseArray = hidePromises.concat(showingInterims.map(service.cancel));
            return $q.all(promiseArray);
          });
        }

        var showAction = hideAction.then(function() {

          return interimElement
            .show()
            .then(function () {
              showingInterims.push(interimElement);
            })
            .catch(function (reason) {
              return reason;
            })
            .finally(function() {
              showPromises.splice(showPromises.indexOf(showAction), 1);
            });

        });

        showPromises.push(showAction);

        // In AngularJS 1.6+, exceptions inside promises will cause a rejection. We need to handle
        // the rejection and only log it if it's an error.
        interimElement.deferred.promise.catch(function(fault) {
          if (fault instanceof Error) {
            $exceptionHandler(fault);
          }

          return fault;
        });

        // Return a promise that will be resolved when the interim
        // element is hidden or cancelled...
        return interimElement.deferred.promise;
      }

      /*
       * @ngdoc method
       * @name $$interimElement.$service#hide
       * @kind function
       *
       * @description
       * Removes the `$interimElement` from the DOM and resolves the promise returned from `show`
       *
       * @param {*} resolveParam Data to resolve the promise with
       * @returns a Promise that will be resolved after the element has been removed.
       *
       */
      function hide(reason, options) {
        options = options || {};

        if (options.closeAll) {
          // We have to make a shallow copy of the array, because otherwise the map will break.
          return $q.all(showingInterims.slice().reverse().map(closeElement));
        } else if (options.closeTo !== undefined) {
          return $q.all(showingInterims.slice(options.closeTo).map(closeElement));
        }

        // Hide the latest showing interim element.
        return closeElement(showingInterims[showingInterims.length - 1]);

        function closeElement(interim) {

          if (!interim) {
            return $q.when(reason);
          }

          var hideAction = interim
            .remove(reason, false, options || { })
            .catch(function(reason) { return reason; })
            .finally(function() {
              hidePromises.splice(hidePromises.indexOf(hideAction), 1);
            });

          showingInterims.splice(showingInterims.indexOf(interim), 1);
          hidePromises.push(hideAction);

          return interim.deferred.promise;
        }
      }

      /*
       * @ngdoc method
       * @name $$interimElement.$service#cancel
       * @kind function
       *
       * @description
       * Removes the `$interimElement` from the DOM and rejects the promise returned from `show`
       *
       * @param {*} reason Data to reject the promise with
       * @returns Promise that will be resolved after the element has been removed.
       *
       */
      function cancel(reason, options) {
        var interim = showingInterims.pop();
        if (!interim) {
          return $q.when(reason);
        }

        var cancelAction = interim
          .remove(reason, true, options || {})
          .catch(function(reason) { return reason; })
          .finally(function() {
            hidePromises.splice(hidePromises.indexOf(cancelAction), 1);
          });

        hidePromises.push(cancelAction);

        // Since AngularJS 1.6.7, promises will be logged to $exceptionHandler when the promise
        // is not handling the rejection. We create a pseudo catch handler, which will prevent the
        // promise from being logged to the $exceptionHandler.
        return interim.deferred.promise.catch(angular.noop);
      }

      /**
       * Creates a function to wait for at least one interim element to be available.
       * @param callbackFn Function to be used as callback
       * @returns {Function}
       */
      function waitForInterim(callbackFn) {
        return function() {
          var fnArguments = arguments;

          if (!showingInterims.length) {
            // When there are still interim's opening, then wait for the first interim element to
            // finish its open animation.
            if (showPromises.length) {
              return showPromises[0].finally(function () {
                return callbackFn.apply(service, fnArguments);
              });
            }

            return $q.when("No interim elements currently showing up.");
          }

          return callbackFn.apply(service, fnArguments);
        };
      }

      /*
       * Special method to quick-remove the interim element without animations
       * Note: interim elements are in "interim containers"
       */
      function destroy(targetEl) {
        var interim = !targetEl ? showingInterims.shift() : null;

        var parentEl = angular.element(targetEl).length && angular.element(targetEl)[0].parentNode;

        if (parentEl) {
          // Try to find the interim in the stack which corresponds to the supplied DOM element.
          var filtered = showingInterims.filter(function(entry) {
            return entry.options.element[0] === parentEl;
          });

          // Note: This function might be called when the element already has been removed,
          // in which case we won't find any matches.
          if (filtered.length) {
            interim = filtered[0];
            showingInterims.splice(showingInterims.indexOf(interim), 1);
          }
        }

        return interim ? interim.remove(SHOW_CANCELLED, false, { '$destroy': true }) :
                         $q.when(SHOW_CANCELLED);
      }

      /*
       * Internal Interim Element Object
       * Used internally to manage the DOM element and related data
       */
      function InterimElement(options) {
        var self, element, showAction = $q.when(true);

        options = configureScopeAndTransitions(options);

        return self = {
          options : options,
          deferred: $q.defer(),
          show    : createAndTransitionIn,
          remove  : transitionOutAndRemove
        };

        /**
         * Compile, link, and show this interim element
         * Use optional autoHided and transition-in effects
         */
        function createAndTransitionIn() {
          return $q(function(resolve, reject) {

            // Trigger onCompiling callback before the compilation starts.
            // This is useful, when modifying options, which can be influenced by developers.
            options.onCompiling && options.onCompiling(options);

            compileElement(options)
              .then(function(compiledData) {
                element = linkElement(compiledData, options);

                // Expose the cleanup function from the compiler.
                options.cleanupElement = compiledData.cleanup;

                showAction = showElement(element, options, compiledData.controller)
                  .then(resolve, rejectAll);
              }).catch(rejectAll);

            function rejectAll(fault) {
              // Force the '$md<xxx>.show()' promise to reject
              self.deferred.reject(fault);

              // Continue rejection propagation
              reject(fault);
            }
          });
        }

        /**
         * After the show process has finished/rejected:
         * - announce 'removing',
         * - perform the transition-out, and
         * - perform optional clean up scope.
         */
        function transitionOutAndRemove(response, isCancelled, opts) {

          // abort if the show() and compile failed
          if (!element) return $q.when(false);

          options = angular.extend(options || {}, opts || {});
          options.cancelAutoHide && options.cancelAutoHide();
          options.element.triggerHandler('$mdInterimElementRemove');

          if (options.$destroy === true) {

            return hideElement(options.element, options).then(function(){
              (isCancelled && rejectAll(response)) || resolveAll(response);
            });

          } else {
            $q.when(showAction).finally(function() {
              hideElement(options.element, options).then(function() {
                isCancelled ? rejectAll(response) : resolveAll(response);
              }, rejectAll);
            });

            return self.deferred.promise;
          }


          /**
           * The `show()` returns a promise that will be resolved when the interim
           * element is hidden or cancelled...
           */
          function resolveAll(response) {
            self.deferred.resolve(response);
          }

          /**
           * Force the '$md<xxx>.show()' promise to reject
           */
          function rejectAll(fault) {
            self.deferred.reject(fault);
          }
        }

        /**
         * Prepare optional isolated scope and prepare $animate with default enter and leave
         * transitions for the new element instance.
         */
        function configureScopeAndTransitions(options) {
          options = options || { };
          if (options.template) {
            options.template = $mdUtil.processTemplate(options.template);
          }

          return angular.extend({
            preserveScope: false,
            cancelAutoHide : angular.noop,
            scope: options.scope || $rootScope.$new(options.isolateScope),

            /**
             * Default usage to enable $animate to transition-in; can be easily overridden via 'options'
             */
            onShow: function transitionIn(scope, element, options) {
              return $animate.enter(element, options.parent);
            },

            /**
             * Default usage to enable $animate to transition-out; can be easily overridden via 'options'
             */
            onRemove: function transitionOut(scope, element) {
              // Element could be undefined if a new element is shown before
              // the old one finishes compiling.
              return element && $animate.leave(element) || $q.when();
            }
          }, options);

        }

        /**
         * Compile an element with a templateUrl, controller, and locals
         */
        function compileElement(options) {

          var compiled = !options.skipCompile ? $mdCompiler.compile(options) : null;

          return compiled || $q(function (resolve) {
              resolve({
                locals: {},
                link: function () {
                  return options.element;
                }
              });
            });
        }

        /**
         *  Link an element with compiled configuration
         */
        function linkElement(compileData, options){
          angular.extend(compileData.locals, options);

          var element = compileData.link(options.scope);

          // Search for parent at insertion time, if not specified
          options.element = element;
          options.parent = findParent(element, options);
          if (options.themable) $mdTheming(element);

          return element;
        }

        /**
         * Search for parent at insertion time, if not specified
         */
        function findParent(element, options) {
          var parent = options.parent;

          // Search for parent at insertion time, if not specified
          if (angular.isFunction(parent)) {
            parent = parent(options.scope, element, options);
          } else if (angular.isString(parent)) {
            parent = angular.element($document[0].querySelector(parent));
          } else {
            parent = angular.element(parent);
          }

          // If parent querySelector/getter function fails, or it's just null,
          // find a default.
          if (!(parent || {}).length) {
            var el;
            if ($rootElement[0] && $rootElement[0].querySelector) {
              el = $rootElement[0].querySelector(':not(svg) > body');
            }
            if (!el) el = $rootElement[0];
            if (el.nodeName == '#comment') {
              el = $document[0].body;
            }
            return angular.element(el);
          }

          return parent;
        }

        /**
         * If auto-hide is enabled, start timer and prepare cancel function
         */
        function startAutoHide() {
          var autoHideTimer, cancelAutoHide = angular.noop;

          if (options.hideDelay) {
            autoHideTimer = $timeout(service.hide, options.hideDelay) ;
            cancelAutoHide = function() {
              $timeout.cancel(autoHideTimer);
            };
          }

          // Cache for subsequent use
          options.cancelAutoHide = function() {
            cancelAutoHide();
            options.cancelAutoHide = undefined;
          };
        }

        /**
         * Show the element ( with transitions), notify complete and start
         * optional auto-Hide
         */
        function showElement(element, options, controller) {
          // Trigger onShowing callback before the `show()` starts
          var notifyShowing = options.onShowing || angular.noop;
          // Trigger onComplete callback when the `show()` finishes
          var notifyComplete = options.onComplete || angular.noop;

          // Necessary for consistency between AngularJS 1.5 and 1.6.
          try {
            notifyShowing(options.scope, element, options, controller);
          } catch (e) {
            return $q.reject(e);
          }

          return $q(function (resolve, reject) {
            try {
              // Start transitionIn
              $q.when(options.onShow(options.scope, element, options, controller))
                .then(function () {
                  notifyComplete(options.scope, element, options);
                  startAutoHide();

                  resolve(element);
                }, reject);

            } catch (e) {
              reject(e.message);
            }
          });
        }

        function hideElement(element, options) {
          var announceRemoving = options.onRemoving || angular.noop;

          return $q(function (resolve, reject) {
            try {
              // Start transitionIn
              var action = $q.when(options.onRemove(options.scope, element, options) || true);

              // Trigger callback *before* the remove operation starts
              announceRemoving(element, action);

              if (options.$destroy) {
                // For $destroy, onRemove should be synchronous
                resolve(element);

                if (!options.preserveScope && options.scope) {
                  // scope destroy should still be be done after the current digest is done
                  action.then(function() { options.scope.$destroy(); });
                }
              } else {
                // Wait until transition-out is done
                action.then(function () {
                  if (!options.preserveScope && options.scope) {
                    options.scope.$destroy();
                  }

                  resolve(element);
                }, reject);
              }
            } catch (e) {
              reject(e.message);
            }
          });
        }

      }
    };

  }

}

(function() {
  'use strict';

  var $mdUtil, $interpolate, $log;

  var SUFFIXES = /(-gt)?-(sm|md|lg|print)/g;
  var WHITESPACE = /\s+/g;

  var FLEX_OPTIONS = ['grow', 'initial', 'auto', 'none', 'noshrink', 'nogrow'];
  var LAYOUT_OPTIONS = ['row', 'column'];
  var ALIGNMENT_MAIN_AXIS= ["", "start", "center", "end", "stretch", "space-around", "space-between"];
  var ALIGNMENT_CROSS_AXIS= ["", "start", "center", "end", "stretch"];

  var config = {
    /**
     * Enable directive attribute-to-class conversions
     * Developers can use `<body md-layout-css />` to quickly
     * disable the Layout directives and prohibit the injection of Layout classNames
     */
    enabled: true,

    /**
     * List of mediaQuery breakpoints and associated suffixes
     *
     *   [
     *    { suffix: "sm", mediaQuery: "screen and (max-width: 599px)" },
     *    { suffix: "md", mediaQuery: "screen and (min-width: 600px) and (max-width: 959px)" }
     *   ]
     */
    breakpoints: []
  };

  registerLayoutAPI(angular.module('material.core.layout', ['ng']));

  /**
   *   registerLayoutAPI()
   *
   *   The original AngularJS Material Layout solution used attribute selectors and CSS.
   *
   *  ```html
   *  <div layout="column"> My Content </div>
   *  ```
   *
   *  ```css
   *  [layout] {
   *    box-sizing: border-box;
   *    display:flex;
   *  }
   *  [layout=column] {
   *    flex-direction : column
   *  }
   *  ```
   *
   *  Use of attribute selectors creates significant performance impacts in some
   *  browsers... mainly IE.
   *
   *  This module registers directives that allow the same layout attributes to be
   *  interpreted and converted to class selectors. The directive will add equivalent classes to each element that
   *  contains a Layout directive.
   *
   * ```html
   *   <div layout="column" class="layout layout-column"> My Content </div>
   *```
   *
   *  ```css
   *  .layout {
   *    box-sizing: border-box;
   *    display:flex;
   *  }
   *  .layout-column {
   *    flex-direction : column
   *  }
   *  ```
   */
  function registerLayoutAPI(module){
    var PREFIX_REGEXP = /^((?:x|data)[:\-_])/i;
    var SPECIAL_CHARS_REGEXP = /([:\-_]+(.))/g;

    // NOTE: these are also defined in constants::MEDIA_PRIORITY and constants::MEDIA
    var BREAKPOINTS     = ["", "xs", "gt-xs", "sm", "gt-sm", "md", "gt-md", "lg", "gt-lg", "xl", "print"];
    var API_WITH_VALUES = ["layout", "flex", "flex-order", "flex-offset", "layout-align"];
    var API_NO_VALUES   = ["show", "hide", "layout-padding", "layout-margin"];


    // Build directive registration functions for the standard Layout API... for all breakpoints.
    angular.forEach(BREAKPOINTS, function(mqb) {

      // Attribute directives with expected, observable value(s)
      angular.forEach(API_WITH_VALUES, function(name){
        var fullName = mqb ? name + "-" + mqb : name;
        module.directive(directiveNormalize(fullName), attributeWithObserve(fullName));
      });

      // Attribute directives with no expected value(s)
      angular.forEach(API_NO_VALUES, function(name){
        var fullName = mqb ? name + "-" + mqb : name;
        module.directive(directiveNormalize(fullName), attributeWithoutValue(fullName));
      });

    });

    // Register other, special directive functions for the Layout features:
    module

      .provider('$$mdLayout'     , function() {
        // Publish internal service for Layouts
        return {
          $get : angular.noop,
          validateAttributeValue : validateAttributeValue,
          validateAttributeUsage : validateAttributeUsage,
          /**
           * Easy way to disable/enable the Layout API.
           * When disabled, this stops all attribute-to-classname generations
           */
          disableLayouts  : function(isDisabled) {
            config.enabled =  (isDisabled !== true);
          }
        };
      })

      .directive('mdLayoutCss'        , disableLayoutDirective)
      .directive('ngCloak'            , buildCloakInterceptor('ng-cloak'))

      .directive('layoutWrap'   , attributeWithoutValue('layout-wrap'))
      .directive('layoutNowrap' , attributeWithoutValue('layout-nowrap'))
      .directive('layoutNoWrap' , attributeWithoutValue('layout-no-wrap'))
      .directive('layoutFill'   , attributeWithoutValue('layout-fill'))

      // !! Deprecated attributes: use the `-lt` (aka less-than) notations

      .directive('layoutLtMd'     , warnAttrNotSupported('layout-lt-md', true))
      .directive('layoutLtLg'     , warnAttrNotSupported('layout-lt-lg', true))
      .directive('flexLtMd'       , warnAttrNotSupported('flex-lt-md', true))
      .directive('flexLtLg'       , warnAttrNotSupported('flex-lt-lg', true))

      .directive('layoutAlignLtMd', warnAttrNotSupported('layout-align-lt-md'))
      .directive('layoutAlignLtLg', warnAttrNotSupported('layout-align-lt-lg'))
      .directive('flexOrderLtMd'  , warnAttrNotSupported('flex-order-lt-md'))
      .directive('flexOrderLtLg'  , warnAttrNotSupported('flex-order-lt-lg'))
      .directive('offsetLtMd'     , warnAttrNotSupported('flex-offset-lt-md'))
      .directive('offsetLtLg'     , warnAttrNotSupported('flex-offset-lt-lg'))

      .directive('hideLtMd'       , warnAttrNotSupported('hide-lt-md'))
      .directive('hideLtLg'       , warnAttrNotSupported('hide-lt-lg'))
      .directive('showLtMd'       , warnAttrNotSupported('show-lt-md'))
      .directive('showLtLg'       , warnAttrNotSupported('show-lt-lg'))

      // Determine if
      .config(detectDisabledLayouts);

    /**
     * Converts snake_case to camelCase.
     * Also there is special case for Moz prefix starting with upper case letter.
     * @param name Name to normalize
     */
    function directiveNormalize(name) {
      return name
        .replace(PREFIX_REGEXP, '')
        .replace(SPECIAL_CHARS_REGEXP, function(_, separator, letter, offset) {
          return offset ? letter.toUpperCase() : letter;
        });
    }

  }


  /**
    * Detect if any of the HTML tags has a [md-layouts-disabled] attribute;
    * If yes, then immediately disable all layout API features
    *
    * Note: this attribute should be specified on either the HTML or BODY tags
    */
   /**
    * ngInject
    */
   function detectDisabledLayouts() {
     var isDisabled = !!document.querySelector('[md-layouts-disabled]');
     config.enabled = !isDisabled;
   }

  /**
   * Special directive that will disable ALL Layout conversions of layout
   * attribute(s) to classname(s).
   *
   * <link rel="stylesheet" href="angular-material.min.css">
   * <link rel="stylesheet" href="angular-material.layout.css">
   *
   * <body md-layout-css>
   *  ...
   * </body>
   *
   * Note: Using md-layout-css directive requires the developer to load the Material
   * Layout Attribute stylesheet (which only uses attribute selectors):
   *
   *       `angular-material.layout.css`
   *
   * Another option is to use the LayoutProvider to configure and disable the attribute
   * conversions; this would obviate the use of the `md-layout-css` directive
   *
   */
  function disableLayoutDirective() {
    // Return a 1x-only, first-match attribute directive
    config.enabled = false;

    return {
      restrict : 'A',
      priority : '900'
    };
  }

  /**
   * Tail-hook ngCloak to delay the uncloaking while Layout transformers
   * finish processing. Eliminates flicker with Material.Layouts
   */
  function buildCloakInterceptor(className) {
    return ['$timeout', function($timeout){
      return {
        restrict : 'A',
        priority : -10,   // run after normal ng-cloak
        compile  : function(element) {
          if (!config.enabled) return angular.noop;

          // Re-add the cloak
          element.addClass(className);

          return function(scope, element) {
            // Wait while layout injectors configure, then uncloak
            // NOTE: $rAF does not delay enough... and this is a 1x-only event,
            //       $timeout is acceptable.
            $timeout(function(){
              element.removeClass(className);
            }, 10, false);
          };
        }
      };
    }];
  }


  // *********************************************************************************
  //
  // These functions create registration functions for AngularJS Material Layout attribute directives
  // This provides easy translation to switch AngularJS Material attribute selectors to
  // CLASS selectors and directives; which has huge performance implications
  // for IE Browsers
  //
  // *********************************************************************************

  /**
   * Creates a directive registration function where a possible dynamic attribute
   * value will be observed/watched.
   * @param {string} className attribute name; eg `layout-gt-md` with value ="row"
   */
  function attributeWithObserve(className) {

    return ['$mdUtil', '$interpolate', "$log", function(_$mdUtil_, _$interpolate_, _$log_) {
      $mdUtil = _$mdUtil_;
      $interpolate = _$interpolate_;
      $log = _$log_;

      return {
        restrict: 'A',
        compile: function(element, attr) {
          var linkFn;
          if (config.enabled) {
            // immediately replace static (non-interpolated) invalid values...

            validateAttributeUsage(className, attr, element, $log);

            validateAttributeValue(className,
              getNormalizedAttrValue(className, attr, ""),
              buildUpdateFn(element, className, attr)
            );

            linkFn = translateWithValueToCssClass;
          }

          // Use for postLink to account for transforms after ng-transclude.
          return linkFn || angular.noop;
        }
      };
    }];

    /**
     * Add as transformed class selector(s), then
     * remove the deprecated attribute selector
     */
    function translateWithValueToCssClass(scope, element, attrs) {
      var updateFn = updateClassWithValue(element, className, attrs);
      var unwatch = attrs.$observe(attrs.$normalize(className), updateFn);

      updateFn(getNormalizedAttrValue(className, attrs, ""));
      scope.$on("$destroy", function() { unwatch(); });
    }
  }

  /**
   * Creates a registration function for AngularJS Material Layout attribute directive.
   * This is a `simple` transpose of attribute usage to class usage; where we ignore
   * any attribute value
   */
  function attributeWithoutValue(className) {
    return ['$mdUtil', '$interpolate', "$log", function(_$mdUtil_, _$interpolate_, _$log_) {
      $mdUtil = _$mdUtil_;
      $interpolate = _$interpolate_;
      $log = _$log_;

      return {
        restrict: 'A',
        compile: function(element, attr) {
          var linkFn;
          if (config.enabled) {
            // immediately replace static (non-interpolated) invalid values...

            validateAttributeValue(className,
              getNormalizedAttrValue(className, attr, ""),
              buildUpdateFn(element, className, attr)
            );

            translateToCssClass(null, element);

            // Use for postLink to account for transforms after ng-transclude.
            linkFn = translateToCssClass;
          }

          return linkFn || angular.noop;
        }
      };
    }];

    /**
     * Add as transformed class selector, then
     * remove the deprecated attribute selector
     */
    function translateToCssClass(scope, element) {
      element.addClass(className);
    }
  }



  /**
   * After link-phase, do NOT remove deprecated layout attribute selector.
   * Instead watch the attribute so interpolated data-bindings to layout
   * selectors will continue to be supported.
   *
   * $observe() the className and update with new class (after removing the last one)
   *
   * e.g. `layout="{{layoutDemo.direction}}"` will update...
   *
   * NOTE: The value must match one of the specified styles in the CSS.
   * For example `flex-gt-md="{{size}}`  where `scope.size == 47` will NOT work since
   * only breakpoints for 0, 5, 10, 15... 100, 33, 34, 66, 67 are defined.
   *
   */
  function updateClassWithValue(element, className) {
    var lastClass;

    return function updateClassFn(newValue) {
      var value = validateAttributeValue(className, newValue || "");
      if (angular.isDefined(value)) {
        if (lastClass) element.removeClass(lastClass);
        lastClass = !value ? className : className + "-" + value.trim().replace(WHITESPACE, "-");
        element.addClass(lastClass);
      }
    };
  }

  /**
   * Provide console warning that this layout attribute has been deprecated
   *
   */
  function warnAttrNotSupported(className) {
    var parts = className.split("-");
    return ["$log", function($log) {
      $log.warn(className + "has been deprecated. Please use a `" + parts[0] + "-gt-<xxx>` variant.");
      return angular.noop;
    }];
  }

  /**
   * Centralize warnings for known flexbox issues (especially IE-related issues)
   */
  function validateAttributeUsage(className, attr, element, $log){
    var message, usage, url;
    var nodeName = element[0].nodeName.toLowerCase();

    switch (className.replace(SUFFIXES,"")) {
      case "flex":
        if ((nodeName == "md-button") || (nodeName == "fieldset")){
          // @see https://github.com/philipwalton/flexbugs#9-some-html-elements-cant-be-flex-containers
          // Use <div flex> wrapper inside (preferred) or outside

          usage = "<" + nodeName + " " + className + "></" + nodeName + ">";
          url = "https://github.com/philipwalton/flexbugs#9-some-html-elements-cant-be-flex-containers";
          message = "Markup '{0}' may not work as expected in IE Browsers. Consult '{1}' for details.";

          $log.warn($mdUtil.supplant(message, [usage, url]));
        }
    }

  }


  /**
   * For the Layout attribute value, validate or replace with default
   * fallback value
   */
  function validateAttributeValue(className, value, updateFn) {
    var origValue;

    if (!needsInterpolation(value)) {
      switch (className.replace(SUFFIXES,"")) {
        case 'layout'        :
          if (!findIn(value, LAYOUT_OPTIONS)) {
            value = LAYOUT_OPTIONS[0];    // 'row';
          }
          break;

        case 'flex'          :
          if (!findIn(value, FLEX_OPTIONS)) {
            if (isNaN(value)) {
              value = '';
            }
          }
          break;

        case 'flex-offset' :
        case 'flex-order'    :
          if (!value || isNaN(+value)) {
            value = '0';
          }
          break;

        case 'layout-align'  :
          var axis = extractAlignAxis(value);
          value = $mdUtil.supplant("{main}-{cross}",axis);
          break;

        case 'layout-padding' :
        case 'layout-margin'  :
        case 'layout-fill'    :
        case 'layout-wrap'    :
        case 'layout-nowrap' :
          value = '';
          break;
      }

      if (value != origValue) {
        (updateFn || angular.noop)(value);
      }
    }

    return value ? value.trim() : "";
  }

  /**
   * Replace current attribute value with fallback value
   */
  function buildUpdateFn(element, className, attrs) {
    return function updateAttrValue(fallback) {
      if (!needsInterpolation(fallback)) {
        // Do not modify the element's attribute value; so
        // uses '<ui-layout layout="/api/sidebar.html" />' will not
        // be affected. Just update the attrs value.
        attrs[attrs.$normalize(className)] = fallback;
      }
    };
  }

  /**
   * See if the original value has interpolation symbols:
   * e.g.  flex-gt-md="{{triggerPoint}}"
   */
  function needsInterpolation(value) {
    return (value || "").indexOf($interpolate.startSymbol()) > -1;
  }

  function getNormalizedAttrValue(className, attrs, defaultVal) {
    var normalizedAttr = attrs.$normalize(className);
    return attrs[normalizedAttr] ? attrs[normalizedAttr].trim().replace(WHITESPACE, "-") : defaultVal || null;
  }

  function findIn(item, list, replaceWith) {
    item = replaceWith && item ? item.replace(WHITESPACE, replaceWith) : item;

    var found = false;
    if (item) {
      list.forEach(function(it) {
        it = replaceWith ? it.replace(WHITESPACE, replaceWith) : it;
        found = found || (it === item);
      });
    }
    return found;
  }

  function extractAlignAxis(attrValue) {
    var axis = {
      main : "start",
      cross: "stretch"
    }, values;

    attrValue = (attrValue || "");

    if (attrValue.indexOf("-") === 0 || attrValue.indexOf(" ") === 0) {
      // For missing main-axis values
      attrValue = "none" + attrValue;
    }

    values = attrValue.toLowerCase().trim().replace(WHITESPACE, "-").split("-");
    if (values.length && (values[0] === "space")) {
      // for main-axis values of "space-around" or "space-between"
      values = [values[0]+"-"+values[1],values[2]];
    }

    if (values.length > 0) axis.main  = values[0] || axis.main;
    if (values.length > 1) axis.cross = values[1] || axis.cross;

    if (ALIGNMENT_MAIN_AXIS.indexOf(axis.main) < 0)   axis.main = "start";
    if (ALIGNMENT_CROSS_AXIS.indexOf(axis.cross) < 0) axis.cross = "stretch";

    return axis;
  }


})();

/**
 * @ngdoc module
 * @name material.core.liveannouncer
 * @description
 * AngularJS Material Live Announcer to provide accessibility for Voice Readers.
 */
MdLiveAnnouncer['$inject'] = ["$timeout"];
angular
  .module('material.core')
  .service('$mdLiveAnnouncer', MdLiveAnnouncer);

/**
 * @ngdoc service
 * @name $mdLiveAnnouncer
 * @module material.core.liveannouncer
 *
 * @description
 *
 * Service to announce messages to supported screenreaders.
 *
 * > The `$mdLiveAnnouncer` service is internally used for components to provide proper accessibility.
 *
 * <hljs lang="js">
 *   module.controller('AppCtrl', function($mdLiveAnnouncer) {
 *     // Basic announcement (Polite Mode)
 *     $mdLiveAnnouncer.announce('Hey Google');
 *
 *     // Custom announcement (Assertive Mode)
 *     $mdLiveAnnouncer.announce('Hey Google', 'assertive');
 *   });
 * </hljs>
 *
 */
function MdLiveAnnouncer($timeout) {
  /** @private @const @type {!angular.$timeout} */
  this._$timeout = $timeout;

  /** @private @const @type {!HTMLElement} */
  this._liveElement = this._createLiveElement();

  /** @private @const @type {!number} */
  this._announceTimeout = 100;
}

/**
 * @ngdoc method
 * @name $mdLiveAnnouncer#announce
 * @description Announces messages to supported screenreaders.
 * @param {string} message Message to be announced to the screenreader
 * @param {'off'|'polite'|'assertive'} politeness The politeness of the announcer element.
 */
MdLiveAnnouncer.prototype.announce = function(message, politeness) {
  if (!politeness) {
    politeness = 'polite';
  }

  var self = this;

  self._liveElement.textContent = '';
  self._liveElement.setAttribute('aria-live', politeness);

  // This 100ms timeout is necessary for some browser + screen-reader combinations:
  // - Both JAWS and NVDA over IE11 will not announce anything without a non-zero timeout.
  // - With Chrome and IE11 with NVDA or JAWS, a repeated (identical) message won't be read a
  //   second time without clearing and then using a non-zero delay.
  // (using JAWS 17 at time of this writing).
  self._$timeout(function() {
    self._liveElement.textContent = message;
  }, self._announceTimeout, false);
};

/**
 * Creates a live announcer element, which listens for DOM changes and announces them
 * to the screenreaders.
 * @returns {!HTMLElement}
 * @private
 */
MdLiveAnnouncer.prototype._createLiveElement = function() {
  var liveEl = document.createElement('div');

  liveEl.classList.add('md-visually-hidden');
  liveEl.setAttribute('role', 'status');
  liveEl.setAttribute('aria-atomic', 'true');
  liveEl.setAttribute('aria-live', 'polite');

  document.body.appendChild(liveEl);

  return liveEl;
};

/**
 * @ngdoc service
 * @name $$mdMeta
 * @module material.core.meta
 *
 * @description
 *
 * A provider and a service that simplifies meta tags access
 *
 * Note: This is intended only for use with dynamic meta tags such as browser color and title.
 * Tags that are only processed when the page is rendered (such as `charset`, and `http-equiv`)
 * will not work since `$$mdMeta` adds the tags after the page has already been loaded.
 *
 * ```js
 * app.config(function($$mdMetaProvider) {
 *   var removeMeta = $$mdMetaProvider.setMeta('meta-name', 'content');
 *   var metaValue  = $$mdMetaProvider.getMeta('meta-name'); // -> 'content'
 *
 *   removeMeta();
 * });
 *
 * app.controller('myController', function($$mdMeta) {
 *   var removeMeta = $$mdMeta.setMeta('meta-name', 'content');
 *   var metaValue  = $$mdMeta.getMeta('meta-name'); // -> 'content'
 *
 *   removeMeta();
 * });
 * ```
 *
 * @returns {$$mdMeta.$service}
 *
 */
angular.module('material.core.meta', [])
  .provider('$$mdMeta', function () {
    var head = angular.element(document.head);
    var metaElements = {};

    /**
     * Checks if the requested element was written manually and maps it
     *
     * @param {string} name meta tag 'name' attribute value
     * @returns {boolean} returns true if there is an element with the requested name
     */
    function mapExistingElement(name) {
      if (metaElements[name]) {
        return true;
      }

      var element = document.getElementsByName(name)[0];

      if (!element) {
        return false;
      }

      metaElements[name] = angular.element(element);

      return true;
    }

    /**
     * @ngdoc method
     * @name $$mdMeta#setMeta
     *
     * @description
     * Creates meta element with the 'name' and 'content' attributes,
     * if the meta tag is already created than we replace the 'content' value
     *
     * @param {string} name meta tag 'name' attribute value
     * @param {string} content meta tag 'content' attribute value
     * @returns {function} remove function
     *
     */
    function setMeta(name, content) {
      mapExistingElement(name);

      if (!metaElements[name]) {
        var newMeta = angular.element('<meta name="' + name + '" content="' + content + '"/>');
        head.append(newMeta);
        metaElements[name] = newMeta;
      }
      else {
        metaElements[name].attr('content', content);
      }

      return function () {
        metaElements[name].attr('content', '');
        metaElements[name].remove();
        delete metaElements[name];
      };
    }

    /**
     * @ngdoc method
     * @name $$mdMeta#getMeta
     *
     * @description
     * Gets the 'content' attribute value of the wanted meta element
     *
     * @param {string} name meta tag 'name' attribute value
     * @returns {string} content attribute value
     */
    function getMeta(name) {
      if (!mapExistingElement(name)) {
        throw Error('$$mdMeta: could not find a meta tag with the name \'' + name + '\'');
      }

      return metaElements[name].attr('content');
    }

    var module = {
      setMeta: setMeta,
      getMeta: getMeta
    };

    return angular.extend({}, module, {
      $get: function () {
        return module;
      }
    });
  });
  /**
   * @ngdoc module
   * @name material.core.componentRegistry
   *
   * @description
   * A component instance registration service.
   * Note: currently this as a private service in the SideNav component.
   */
  ComponentRegistry['$inject'] = ["$log", "$q"];
  angular.module('material.core')
    .factory('$mdComponentRegistry', ComponentRegistry);

  /*
   * @private
   * @ngdoc factory
   * @name ComponentRegistry
   * @module material.core.componentRegistry
   *
   */
  function ComponentRegistry($log, $q) {

    var self;
    var instances = [];
    var pendings = { };

    return self = {
      /**
       * Used to print an error when an instance for a handle isn't found.
       */
      notFoundError: function(handle, msgContext) {
        $log.error((msgContext || "") + 'No instance found for handle', handle);
      },
      /**
       * Return all registered instances as an array.
       */
      getInstances: function() {
        return instances;
      },

      /**
       * Get a registered instance.
       * @param handle the String handle to look up for a registered instance.
       */
      get: function(handle) {
        if (!isValidID(handle)) return null;

        var i, j, instance;
        for (i = 0, j = instances.length; i < j; i++) {
          instance = instances[i];
          if (instance.$$mdHandle === handle) {
            return instance;
          }
        }
        return null;
      },

      /**
       * Register an instance.
       * @param instance the instance to register
       * @param handle the handle to identify the instance under.
       */
      register: function(instance, handle) {
        if (!handle) return angular.noop;

        instance.$$mdHandle = handle;
        instances.push(instance);
        resolveWhen();

        return deregister;

        /**
         * Remove registration for an instance
         */
        function deregister() {
          var index = instances.indexOf(instance);
          if (index !== -1) {
            instances.splice(index, 1);
          }
        }

        /**
         * Resolve any pending promises for this instance
         */
        function resolveWhen() {
          var dfd = pendings[handle];
          if (dfd) {
            dfd.forEach(function (promise) {
              promise.resolve(instance);
            });
            delete pendings[handle];
          }
        }
      },

      /**
       * Async accessor to registered component instance
       * If not available then a promise is created to notify
       * all listeners when the instance is registered.
       */
      when : function(handle) {
        if (isValidID(handle)) {
          var deferred = $q.defer();
          var instance = self.get(handle);

          if (instance)  {
            deferred.resolve(instance);
          } else {
            if (pendings[handle] === undefined) {
              pendings[handle] = [];
            }
            pendings[handle].push(deferred);
          }

          return deferred.promise;
        }
        return $q.reject("Invalid `md-component-id` value.");
      }

    };

    function isValidID(handle){
      return handle && (handle !== "");
    }

  }

angular.module('material.core.theming.palette', [])
.constant('$mdColorPalette', {
  'red': {
    '50': '#ffebee',
    '100': '#ffcdd2',
    '200': '#ef9a9a',
    '300': '#e57373',
    '400': '#ef5350',
    '500': '#f44336',
    '600': '#e53935',
    '700': '#d32f2f',
    '800': '#c62828',
    '900': '#b71c1c',
    'A100': '#ff8a80',
    'A200': '#ff5252',
    'A400': '#ff1744',
    'A700': '#d50000',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 300 A100',
    'contrastStrongLightColors': '400 500 600 700 A200 A400 A700'
  },
  'pink': {
    '50': '#fce4ec',
    '100': '#f8bbd0',
    '200': '#f48fb1',
    '300': '#f06292',
    '400': '#ec407a',
    '500': '#e91e63',
    '600': '#d81b60',
    '700': '#c2185b',
    '800': '#ad1457',
    '900': '#880e4f',
    'A100': '#ff80ab',
    'A200': '#ff4081',
    'A400': '#f50057',
    'A700': '#c51162',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 A100',
    'contrastStrongLightColors': '500 600 A200 A400 A700'
  },
  'purple': {
    '50': '#f3e5f5',
    '100': '#e1bee7',
    '200': '#ce93d8',
    '300': '#ba68c8',
    '400': '#ab47bc',
    '500': '#9c27b0',
    '600': '#8e24aa',
    '700': '#7b1fa2',
    '800': '#6a1b9a',
    '900': '#4a148c',
    'A100': '#ea80fc',
    'A200': '#e040fb',
    'A400': '#d500f9',
    'A700': '#aa00ff',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 A100',
    'contrastStrongLightColors': '300 400 A200 A400 A700'
  },
  'deep-purple': {
    '50': '#ede7f6',
    '100': '#d1c4e9',
    '200': '#b39ddb',
    '300': '#9575cd',
    '400': '#7e57c2',
    '500': '#673ab7',
    '600': '#5e35b1',
    '700': '#512da8',
    '800': '#4527a0',
    '900': '#311b92',
    'A100': '#b388ff',
    'A200': '#7c4dff',
    'A400': '#651fff',
    'A700': '#6200ea',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 A100',
    'contrastStrongLightColors': '300 400 A200'
  },
  'indigo': {
    '50': '#e8eaf6',
    '100': '#c5cae9',
    '200': '#9fa8da',
    '300': '#7986cb',
    '400': '#5c6bc0',
    '500': '#3f51b5',
    '600': '#3949ab',
    '700': '#303f9f',
    '800': '#283593',
    '900': '#1a237e',
    'A100': '#8c9eff',
    'A200': '#536dfe',
    'A400': '#3d5afe',
    'A700': '#304ffe',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 A100',
    'contrastStrongLightColors': '300 400 A200 A400'
  },
  'blue': {
    '50': '#e3f2fd',
    '100': '#bbdefb',
    '200': '#90caf9',
    '300': '#64b5f6',
    '400': '#42a5f5',
    '500': '#2196f3',
    '600': '#1e88e5',
    '700': '#1976d2',
    '800': '#1565c0',
    '900': '#0d47a1',
    'A100': '#82b1ff',
    'A200': '#448aff',
    'A400': '#2979ff',
    'A700': '#2962ff',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 300 400 A100',
    'contrastStrongLightColors': '500 600 700 A200 A400 A700'
  },
  'light-blue': {
    '50': '#e1f5fe',
    '100': '#b3e5fc',
    '200': '#81d4fa',
    '300': '#4fc3f7',
    '400': '#29b6f6',
    '500': '#03a9f4',
    '600': '#039be5',
    '700': '#0288d1',
    '800': '#0277bd',
    '900': '#01579b',
    'A100': '#80d8ff',
    'A200': '#40c4ff',
    'A400': '#00b0ff',
    'A700': '#0091ea',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '600 700 800 900 A700',
    'contrastStrongLightColors': '600 700 800 A700'
  },
  'cyan': {
    '50': '#e0f7fa',
    '100': '#b2ebf2',
    '200': '#80deea',
    '300': '#4dd0e1',
    '400': '#26c6da',
    '500': '#00bcd4',
    '600': '#00acc1',
    '700': '#0097a7',
    '800': '#00838f',
    '900': '#006064',
    'A100': '#84ffff',
    'A200': '#18ffff',
    'A400': '#00e5ff',
    'A700': '#00b8d4',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '700 800 900',
    'contrastStrongLightColors': '700 800 900'
  },
  'teal': {
    '50': '#e0f2f1',
    '100': '#b2dfdb',
    '200': '#80cbc4',
    '300': '#4db6ac',
    '400': '#26a69a',
    '500': '#009688',
    '600': '#00897b',
    '700': '#00796b',
    '800': '#00695c',
    '900': '#004d40',
    'A100': '#a7ffeb',
    'A200': '#64ffda',
    'A400': '#1de9b6',
    'A700': '#00bfa5',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '500 600 700 800 900',
    'contrastStrongLightColors': '500 600 700'
  },
  'green': {
    '50': '#e8f5e9',
    '100': '#c8e6c9',
    '200': '#a5d6a7',
    '300': '#81c784',
    '400': '#66bb6a',
    '500': '#4caf50',
    '600': '#43a047',
    '700': '#388e3c',
    '800': '#2e7d32',
    '900': '#1b5e20',
    'A100': '#b9f6ca',
    'A200': '#69f0ae',
    'A400': '#00e676',
    'A700': '#00c853',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '500 600 700 800 900',
    'contrastStrongLightColors': '500 600 700'
  },
  'light-green': {
    '50': '#f1f8e9',
    '100': '#dcedc8',
    '200': '#c5e1a5',
    '300': '#aed581',
    '400': '#9ccc65',
    '500': '#8bc34a',
    '600': '#7cb342',
    '700': '#689f38',
    '800': '#558b2f',
    '900': '#33691e',
    'A100': '#ccff90',
    'A200': '#b2ff59',
    'A400': '#76ff03',
    'A700': '#64dd17',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '700 800 900',
    'contrastStrongLightColors': '700 800 900'
  },
  'lime': {
    '50': '#f9fbe7',
    '100': '#f0f4c3',
    '200': '#e6ee9c',
    '300': '#dce775',
    '400': '#d4e157',
    '500': '#cddc39',
    '600': '#c0ca33',
    '700': '#afb42b',
    '800': '#9e9d24',
    '900': '#827717',
    'A100': '#f4ff81',
    'A200': '#eeff41',
    'A400': '#c6ff00',
    'A700': '#aeea00',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '900',
    'contrastStrongLightColors': '900'
  },
  'yellow': {
    '50': '#fffde7',
    '100': '#fff9c4',
    '200': '#fff59d',
    '300': '#fff176',
    '400': '#ffee58',
    '500': '#ffeb3b',
    '600': '#fdd835',
    '700': '#fbc02d',
    '800': '#f9a825',
    '900': '#f57f17',
    'A100': '#ffff8d',
    'A200': '#ffff00',
    'A400': '#ffea00',
    'A700': '#ffd600',
    'contrastDefaultColor': 'dark'
  },
  'amber': {
    '50': '#fff8e1',
    '100': '#ffecb3',
    '200': '#ffe082',
    '300': '#ffd54f',
    '400': '#ffca28',
    '500': '#ffc107',
    '600': '#ffb300',
    '700': '#ffa000',
    '800': '#ff8f00',
    '900': '#ff6f00',
    'A100': '#ffe57f',
    'A200': '#ffd740',
    'A400': '#ffc400',
    'A700': '#ffab00',
    'contrastDefaultColor': 'dark'
  },
  'orange': {
    '50': '#fff3e0',
    '100': '#ffe0b2',
    '200': '#ffcc80',
    '300': '#ffb74d',
    '400': '#ffa726',
    '500': '#ff9800',
    '600': '#fb8c00',
    '700': '#f57c00',
    '800': '#ef6c00',
    '900': '#e65100',
    'A100': '#ffd180',
    'A200': '#ffab40',
    'A400': '#ff9100',
    'A700': '#ff6d00',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '800 900',
    'contrastStrongLightColors': '800 900'
  },
  'deep-orange': {
    '50': '#fbe9e7',
    '100': '#ffccbc',
    '200': '#ffab91',
    '300': '#ff8a65',
    '400': '#ff7043',
    '500': '#ff5722',
    '600': '#f4511e',
    '700': '#e64a19',
    '800': '#d84315',
    '900': '#bf360c',
    'A100': '#ff9e80',
    'A200': '#ff6e40',
    'A400': '#ff3d00',
    'A700': '#dd2c00',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 300 400 A100 A200',
    'contrastStrongLightColors': '500 600 700 800 900 A400 A700'
  },
  'brown': {
    '50': '#efebe9',
    '100': '#d7ccc8',
    '200': '#bcaaa4',
    '300': '#a1887f',
    '400': '#8d6e63',
    '500': '#795548',
    '600': '#6d4c41',
    '700': '#5d4037',
    '800': '#4e342e',
    '900': '#3e2723',
    'A100': '#d7ccc8',
    'A200': '#bcaaa4',
    'A400': '#8d6e63',
    'A700': '#5d4037',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 A100 A200',
    'contrastStrongLightColors': '300 400'
  },
  'grey': {
    '50': '#fafafa',
    '100': '#f5f5f5',
    '200': '#eeeeee',
    '300': '#e0e0e0',
    '400': '#bdbdbd',
    '500': '#9e9e9e',
    '600': '#757575',
    '700': '#616161',
    '800': '#424242',
    '900': '#212121',
    'A100': '#ffffff',
    'A200': '#000000',
    'A400': '#303030',
    'A700': '#616161',
    'contrastDefaultColor': 'dark',
    'contrastLightColors': '600 700 800 900 A200 A400 A700'
  },
  'blue-grey': {
    '50': '#eceff1',
    '100': '#cfd8dc',
    '200': '#b0bec5',
    '300': '#90a4ae',
    '400': '#78909c',
    '500': '#607d8b',
    '600': '#546e7a',
    '700': '#455a64',
    '800': '#37474f',
    '900': '#263238',
    'A100': '#cfd8dc',
    'A200': '#b0bec5',
    'A400': '#78909c',
    'A700': '#455a64',
    'contrastDefaultColor': 'light',
    'contrastDarkColors': '50 100 200 300 A100 A200',
    'contrastStrongLightColors': '400 500 700'
  }
});

(function(angular) {
  'use strict';
/**
 * @ngdoc module
 * @name material.core.theming
 * @description
 * Theming
 */
detectDisabledThemes['$inject'] = ["$mdThemingProvider"];
ThemingDirective['$inject'] = ["$mdTheming", "$interpolate", "$parse", "$mdUtil", "$q", "$log"];
ThemableDirective['$inject'] = ["$mdTheming"];
ThemingProvider['$inject'] = ["$mdColorPalette", "$$mdMetaProvider"];
generateAllThemes['$inject'] = ["$injector", "$mdTheming"];
angular.module('material.core.theming', ['material.core.theming.palette', 'material.core.meta'])
  .directive('mdTheme', ThemingDirective)
  .directive('mdThemable', ThemableDirective)
  .directive('mdThemesDisabled', disableThemesDirective)
  .provider('$mdTheming', ThemingProvider)
  .config(detectDisabledThemes)
  .run(generateAllThemes);

/**
 * Detect if the HTML or the BODY tags has a [md-themes-disabled] attribute
 * If yes, then immediately disable all theme stylesheet generation and DOM injection
 */
/**
 * ngInject
 */
function detectDisabledThemes($mdThemingProvider) {
  var isDisabled = !!document.querySelector('[md-themes-disabled]');
  $mdThemingProvider.disableTheming(isDisabled);
}

/**
 * @ngdoc service
 * @name $mdThemingProvider
 * @module material.core.theming
 *
 * @description Provider to configure the `$mdTheming` service.
 *
 * ### Default Theme
 * The `$mdThemingProvider` uses by default the following theme configuration:
 *
 * - Primary Palette: `Blue`
 * - Accent Palette: `Pink`
 * - Warn Palette: `Deep-Orange`
 * - Background Palette: `Grey`
 *
 * If you don't want to use the `md-theme` directive on the elements itself, you may want to overwrite
 * the default theme.<br/>
 * This can be done by using the following markup.
 *
 * <hljs lang="js">
 *   myAppModule.config(function($mdThemingProvider) {
 *     $mdThemingProvider
 *       .theme('default')
 *       .primaryPalette('blue')
 *       .accentPalette('teal')
 *       .warnPalette('red')
 *       .backgroundPalette('grey');
 *   });
 * </hljs>
 *

 * ### Dynamic Themes
 *
 * By default, if you change a theme at runtime, the `$mdTheming` service will not detect those changes.<br/>
 * If you have an application, which changes its theme on runtime, you have to enable theme watching.
 *
 * <hljs lang="js">
 *   myAppModule.config(function($mdThemingProvider) {
 *     // Enable theme watching.
 *     $mdThemingProvider.alwaysWatchTheme(true);
 *   });
 * </hljs>
 *
 * ### Custom Theme Styles
 *
 * Sometimes you may want to use your own theme styles for some custom components.<br/>
 * You are able to register your own styles by using the following markup.
 *
 * <hljs lang="js">
 *   myAppModule.config(function($mdThemingProvider) {
 *     // Register our custom stylesheet into the theming provider.
 *     $mdThemingProvider.registerStyles(STYLESHEET);
 *   });
 * </hljs>
 *
 * The `registerStyles` method only accepts strings as value, so you're actually not able to load an external
 * stylesheet file into the `$mdThemingProvider`.
 *
 * If it's necessary to load an external stylesheet, we suggest using a bundler, which supports including raw content,
 * like [raw-loader](https://github.com/webpack/raw-loader) for `webpack`.
 *
 * <hljs lang="js">
 *   myAppModule.config(function($mdThemingProvider) {
 *     // Register your custom stylesheet into the theming provider.
 *     $mdThemingProvider.registerStyles(require('../styles/my-component.theme.css'));
 *   });
 * </hljs>
 *
 * ### Browser color
 *
 * Enables browser header coloring
 * for more info please visit:
 * https://developers.google.com/web/fundamentals/design-and-ui/browser-customization/theme-color
 *
 * Options parameter: <br/>
 * `theme`   - A defined theme via `$mdThemeProvider` to use the palettes from. Default is `default` theme. <br/>
 * `palette` - Can be any one of the basic material design palettes, extended defined palettes and 'primary',
 *             'accent', 'background' and 'warn'. Default is `primary`. <br/>
 * `hue`     - The hue from the selected palette. Default is `800`<br/>
 *
 * <hljs lang="js">
 *   myAppModule.config(function($mdThemingProvider) {
 *     // Enable browser color
 *     $mdThemingProvider.enableBrowserColor({
 *       theme: 'myTheme', // Default is 'default'
 *       palette: 'accent', // Default is 'primary', any basic material palette and extended palettes are available
 *       hue: '200' // Default is '800'
 *     });
 *   });
 * </hljs>
 */

/**
 * Some Example Valid Theming Expressions
 * =======================================
 *
 * Intention group expansion: (valid for primary, accent, warn, background)
 *
 * {{primary-100}} - grab shade 100 from the primary palette
 * {{primary-100-0.7}} - grab shade 100, apply opacity of 0.7
 * {{primary-100-contrast}} - grab shade 100's contrast color
 * {{primary-hue-1}} - grab the shade assigned to hue-1 from the primary palette
 * {{primary-hue-1-0.7}} - apply 0.7 opacity to primary-hue-1
 * {{primary-color}} - Generates .md-hue-1, .md-hue-2, .md-hue-3 with configured shades set for each hue
 * {{primary-color-0.7}} - Apply 0.7 opacity to each of the above rules
 * {{primary-contrast}} - Generates .md-hue-1, .md-hue-2, .md-hue-3 with configured contrast (ie. text) color shades set for each hue
 * {{primary-contrast-0.7}} - Apply 0.7 opacity to each of the above rules
 *
 * Foreground expansion: Applies rgba to black/white foreground text
 *
 * {{foreground-1}} - used for primary text
 * {{foreground-2}} - used for secondary text/divider
 * {{foreground-3}} - used for disabled text
 * {{foreground-4}} - used for dividers
 */

// In memory generated CSS rules; registered by theme.name
var GENERATED = { };

// In memory storage of defined themes and color palettes (both loaded by CSS, and user specified)
var PALETTES;

// Text Colors on light and dark backgrounds
// @see https://material.io/archive/guidelines/style/color.html#color-usability
var DARK_FOREGROUND = {
  name: 'dark',
  '1': 'rgba(0,0,0,0.87)',
  '2': 'rgba(0,0,0,0.54)',
  '3': 'rgba(0,0,0,0.38)',
  '4': 'rgba(0,0,0,0.12)'
};
var LIGHT_FOREGROUND = {
  name: 'light',
  '1': 'rgba(255,255,255,1.0)',
  '2': 'rgba(255,255,255,0.7)',
  '3': 'rgba(255,255,255,0.5)',
  '4': 'rgba(255,255,255,0.12)'
};

var DARK_SHADOW = '1px 1px 0px rgba(0,0,0,0.4), -1px -1px 0px rgba(0,0,0,0.4)';
var LIGHT_SHADOW = '';

var DARK_CONTRAST_COLOR = colorToRgbaArray('rgba(0,0,0,0.87)');
var LIGHT_CONTRAST_COLOR = colorToRgbaArray('rgba(255,255,255,0.87)');
var STRONG_LIGHT_CONTRAST_COLOR = colorToRgbaArray('rgb(255,255,255)');

var THEME_COLOR_TYPES = ['primary', 'accent', 'warn', 'background'];
var DEFAULT_COLOR_TYPE = 'primary';

// A color in a theme will use these hues by default, if not specified by user.
var LIGHT_DEFAULT_HUES = {
  'accent': {
    'default': 'A200',
    'hue-1': 'A100',
    'hue-2': 'A400',
    'hue-3': 'A700'
  },
  'background': {
    'default': '50',
    'hue-1': 'A100',
    'hue-2': '100',
    'hue-3': '300'
  }
};

var DARK_DEFAULT_HUES = {
  'background': {
    'default': 'A400',
    'hue-1': '800',
    'hue-2': '900',
    'hue-3': 'A200'
  }
};
THEME_COLOR_TYPES.forEach(function(colorType) {
  // Color types with unspecified default hues will use these default hue values
  var defaultDefaultHues = {
    'default': '500',
    'hue-1': '300',
    'hue-2': '800',
    'hue-3': 'A100'
  };
  if (!LIGHT_DEFAULT_HUES[colorType]) LIGHT_DEFAULT_HUES[colorType] = defaultDefaultHues;
  if (!DARK_DEFAULT_HUES[colorType]) DARK_DEFAULT_HUES[colorType] = defaultDefaultHues;
});

var VALID_HUE_VALUES = [
  '50', '100', '200', '300', '400', '500', '600',
  '700', '800', '900', 'A100', 'A200', 'A400', 'A700'
];

var themeConfig = {
  disableTheming : false,   // Generate our themes at run time; also disable stylesheet DOM injection
  generateOnDemand : false, // Whether or not themes are to be generated on-demand (vs. eagerly).
  registeredStyles : [],    // Custom styles registered to be used in the theming of custom components.
  nonce : null              // Nonce to be added as an attribute to the generated themes style tags.
};

/**
 *
 */
function ThemingProvider($mdColorPalette, $$mdMetaProvider) {
  ThemingService['$inject'] = ["$rootScope", "$mdUtil", "$q", "$log"];
  PALETTES = { };
  var THEMES = { };

  var themingProvider;

  var alwaysWatchTheme = false;
  var defaultTheme = 'default';

  // Load JS Defined Palettes
  angular.extend(PALETTES, $mdColorPalette);

  // Default theme defined in core.js

  /**
   * Adds `theme-color` and `msapplication-navbutton-color` meta tags with the color parameter
   * @param {string} color Hex value of the wanted browser color
   * @returns {function} Remove function of the meta tags
   */
  var setBrowserColor = function (color) {
    // Chrome, Firefox OS and Opera
    var removeChrome = $$mdMetaProvider.setMeta('theme-color', color);
    // Windows Phone
    var removeWindows = $$mdMetaProvider.setMeta('msapplication-navbutton-color', color);

    return function () {
      removeChrome();
      removeWindows();
    };
  };

  /**
   * @ngdoc method
   * @name $mdThemingProvider#enableBrowserColor
   * @description
   * Enables browser header coloring. For more info please visit
   * <a href="https://developers.google.com/web/fundamentals/design-and-ui/browser-customization/theme-color">
   *   Web Fundamentals</a>.
   * @param {object=} options Options for the browser color, which include:<br/>
   * - `theme` - `{string}`: A defined theme via `$mdThemeProvider` to use the palettes from. Default is `default` theme. <br/>
   * - `palette` - `{string}`:  Can be any one of the basic material design palettes, extended defined palettes, or `primary`,
   *  `accent`, `background`, and `warn`. Default is `primary`.<br/>
   * - `hue` -  `{string}`: The hue from the selected palette. Default is `800`.<br/>
   * @returns {function} Function that removes the browser coloring when called.
   */
  var enableBrowserColor = function (options) {
    options = angular.isObject(options) ? options : {};

    var theme = options.theme || 'default';
    var hue = options.hue || '800';

    var palette = PALETTES[options.palette] ||
      PALETTES[THEMES[theme].colors[options.palette || 'primary'].name];

    var color = angular.isObject(palette[hue]) ? palette[hue].hex : palette[hue];
    if (color.substr(0, 1) !== '#') color = '#' + color;

    return setBrowserColor(color);
  };

  return themingProvider = {
    definePalette: definePalette,
    extendPalette: extendPalette,
    theme: registerTheme,

    /**
     * return a read-only clone of the current theme configuration
     */
    configuration : function() {
      return angular.extend({ }, themeConfig, {
        defaultTheme : defaultTheme,
        alwaysWatchTheme : alwaysWatchTheme,
        registeredStyles : [].concat(themeConfig.registeredStyles)
      });
    },

    /**
     * @ngdoc method
     * @name $mdThemingProvider#disableTheming
     * @description
     * An easier way to disable theming without having to use `.constant("$MD_THEME_CSS","");`.
     * This disables all dynamic theme style sheet generations and injections.
     * @param {boolean=} isDisabled Disable all dynamic theme style sheet generations and injections
     *  if `true` or `undefined`.
     */
    disableTheming: function(isDisabled) {
      themeConfig.disableTheming = angular.isUndefined(isDisabled) || !!isDisabled;
    },

    /**
     * @ngdoc method
     * @name $mdThemingProvider#registerStyles
     * @param {string} styles The styles to be appended to AngularJS Material's built in theme CSS.
     */
    registerStyles: function(styles) {
      themeConfig.registeredStyles.push(styles);
    },

    /**
     * @ngdoc method
     * @name $mdThemingProvider#setNonce
     * @param {string} nonceValue The nonce to be added as an attribute to the theme style tags.
     * Setting a value allows the use of CSP policy without using the unsafe-inline directive.
     */
    setNonce: function(nonceValue) {
      themeConfig.nonce = nonceValue;
    },

    generateThemesOnDemand: function(onDemand) {
      themeConfig.generateOnDemand = onDemand;
    },

    /**
     * @ngdoc method
     * @name $mdThemingProvider#setDefaultTheme
     * @param {string} theme Default theme name to be applied to elements. Default value is `default`.
     */
    setDefaultTheme: function(theme) {
      defaultTheme = theme;
    },

    /**
     * @ngdoc method
     * @name $mdThemingProvider#alwaysWatchTheme
     * @param {boolean} alwaysWatch Whether or not to always watch themes for changes and re-apply
     * classes when they change. Default is `false`. Enabling can reduce performance.
     */
    alwaysWatchTheme: function(alwaysWatch) {
      alwaysWatchTheme = alwaysWatch;
    },

    enableBrowserColor: enableBrowserColor,

    $get: ThemingService,
    _LIGHT_DEFAULT_HUES: LIGHT_DEFAULT_HUES,
    _DARK_DEFAULT_HUES: DARK_DEFAULT_HUES,
    _PALETTES: PALETTES,
    _THEMES: THEMES,
    _parseRules: parseRules,
    _rgba: rgba
  };

  /**
   * @ngdoc method
   * @name $mdThemingProvider#definePalette
   * @description
   * In the event that you need to define a custom color palette, you can use this function to
   * make it available to your theme for use in its intention groups.<br>
   * Note that you must specify all hues in the definition map.
   * @param {string} name Name of palette being defined
   * @param {object} map Palette definition that includes hue definitions and contrast colors:
   * - `'50'` - `{string}`: HEX color
   * - `'100'` - `{string}`: HEX color
   * - `'200'` - `{string}`: HEX color
   * - `'300'` - `{string}`: HEX color
   * - `'400'` - `{string}`: HEX color
   * - `'500'` - `{string}`: HEX color
   * - `'600'` - `{string}`: HEX color
   * - `'700'` - `{string}`: HEX color
   * - `'800'` - `{string}`: HEX color
   * - `'900'` - `{string}`: HEX color
   * - `'A100'` - `{string}`: HEX color
   * - `'A200'` - `{string}`: HEX color
   * - `'A400'` - `{string}`: HEX color
   * - `'A700'` - `{string}`: HEX color
   * - `'contrastDefaultColor'` - `{string}`: `light` or `dark`
   * - `'contrastDarkColors'` - `{string[]}`: Hues which should use dark contrast colors (i.e. raised button text).
   *  For example: `['50', '100', '200', '300', '400', 'A100']`.
   * - `'contrastLightColors'` - `{string[]}`: Hues which should use light contrast colors (i.e. raised button text).
   *  For example: `['500', '600', '700', '800', '900', 'A200', 'A400', 'A700']`.
   */
  function definePalette(name, map) {
    map = map || {};
    PALETTES[name] = checkPaletteValid(name, map);
    return themingProvider;
  }

  /**
   * @ngdoc method
   * @name $mdThemingProvider#extendPalette
   * @description
   * Sometimes it is easier to extend an existing color palette and then change a few properties,
   * rather than defining a whole new palette.
   * @param {string} name Name of palette being extended
   * @param {object} map Palette definition that includes optional hue definitions and contrast colors:
   * - `'50'` - `{string}`: HEX color
   * - `'100'` - `{string}`: HEX color
   * - `'200'` - `{string}`: HEX color
   * - `'300'` - `{string}`: HEX color
   * - `'400'` - `{string}`: HEX color
   * - `'500'` - `{string}`: HEX color
   * - `'600'` - `{string}`: HEX color
   * - `'700'` - `{string}`: HEX color
   * - `'800'` - `{string}`: HEX color
   * - `'900'` - `{string}`: HEX color
   * - `'A100'` - `{string}`: HEX color
   * - `'A200'` - `{string}`: HEX color
   * - `'A400'` - `{string}`: HEX color
   * - `'A700'` - `{string}`: HEX color
   * - `'contrastDefaultColor'` - `{string}`: `light` or `dark`
   * - `'contrastDarkColors'` - `{string[]}`: Hues which should use dark contrast colors (i.e. raised button text).
   *  For example: `['50', '100', '200', '300', '400', 'A100']`.
   * - `'contrastLightColors'` - `{string[]}`: Hues which should use light contrast colors (i.e. raised button text).
   *  For example: `['500', '600', '700', '800', '900', 'A200', 'A400', 'A700']`.
   *  @returns {object} A new object which is a copy of the given palette, `name`,
   *    with variables from `map` overwritten.
   */
  function extendPalette(name, map) {
    return checkPaletteValid(name,  angular.extend({}, PALETTES[name] || {}, map));
  }

  // Make sure that palette has all required hues
  function checkPaletteValid(name, map) {
    var missingColors = VALID_HUE_VALUES.filter(function(field) {
      return !map[field];
    });
    if (missingColors.length) {
      throw new Error("Missing colors %1 in palette %2!"
                      .replace('%1', missingColors.join(', '))
                      .replace('%2', name));
    }

    return map;
  }

  /**
   * @ngdoc method
   * @name $mdThemingProvider#theme
   * @description
   * Register a theme (which is a collection of color palettes); i.e. `warn`, `accent`,
   * `background`, and `primary`.<br>
   * Optionally inherit from an existing theme.
   * @param {string} name Name of theme being registered
   * @param {string=} inheritFrom Existing theme name to inherit from
   */
  function registerTheme(name, inheritFrom) {
    if (THEMES[name]) return THEMES[name];

    inheritFrom = inheritFrom || 'default';

    var parentTheme = typeof inheritFrom === 'string' ? THEMES[inheritFrom] : inheritFrom;
    var theme = new Theme(name);

    if (parentTheme) {
      angular.forEach(parentTheme.colors, function(color, colorType) {
        theme.colors[colorType] = {
          name: color.name,
          // Make sure a COPY of the hues is given to the child color,
          // not the same reference.
          hues: angular.extend({}, color.hues)
        };
      });
    }
    THEMES[name] = theme;

    return theme;
  }

  function Theme(name) {
    var self = this;
    self.name = name;
    self.colors = {};

    self.dark = setDark;
    setDark(false);

    function setDark(isDark) {
      isDark = arguments.length === 0 ? true : !!isDark;

      // If no change, abort
      if (isDark === self.isDark) return;

      self.isDark = isDark;

      self.foregroundPalette = self.isDark ? LIGHT_FOREGROUND : DARK_FOREGROUND;
      self.foregroundShadow = self.isDark ? DARK_SHADOW : LIGHT_SHADOW;

      // Light and dark themes have different default hues.
      // Go through each existing color type for this theme, and for every
      // hue value that is still the default hue value from the previous light/dark setting,
      // set it to the default hue value from the new light/dark setting.
      var newDefaultHues = self.isDark ? DARK_DEFAULT_HUES : LIGHT_DEFAULT_HUES;
      var oldDefaultHues = self.isDark ? LIGHT_DEFAULT_HUES : DARK_DEFAULT_HUES;
      angular.forEach(newDefaultHues, function(newDefaults, colorType) {
        var color = self.colors[colorType];
        var oldDefaults = oldDefaultHues[colorType];
        if (color) {
          for (var hueName in color.hues) {
            if (color.hues[hueName] === oldDefaults[hueName]) {
              color.hues[hueName] = newDefaults[hueName];
            }
          }
        }
      });

      return self;
    }

    THEME_COLOR_TYPES.forEach(function(colorType) {
      var defaultHues = (self.isDark ? DARK_DEFAULT_HUES : LIGHT_DEFAULT_HUES)[colorType];
      self[colorType + 'Palette'] = function setPaletteType(paletteName, hues) {
        var color = self.colors[colorType] = {
          name: paletteName,
          hues: angular.extend({}, defaultHues, hues)
        };

        Object.keys(color.hues).forEach(function(name) {
          if (!defaultHues[name]) {
            throw new Error("Invalid hue name '%1' in theme %2's %3 color %4. Available hue names: %4"
              .replace('%1', name)
              .replace('%2', self.name)
              .replace('%3', paletteName)
              .replace('%4', Object.keys(defaultHues).join(', '))
            );
          }
        });
        Object.keys(color.hues).map(function(key) {
          return color.hues[key];
        }).forEach(function(hueValue) {
          if (VALID_HUE_VALUES.indexOf(hueValue) == -1) {
            throw new Error("Invalid hue value '%1' in theme %2's %3 color %4. Available hue values: %5"
              .replace('%1', hueValue)
              .replace('%2', self.name)
              .replace('%3', colorType)
              .replace('%4', paletteName)
              .replace('%5', VALID_HUE_VALUES.join(', '))
            );
          }
        });
        return self;
      };

      self[colorType + 'Color'] = function() {
        var args = Array.prototype.slice.call(arguments);
        // eslint-disable-next-line no-console
        console.warn('$mdThemingProviderTheme.' + colorType + 'Color() has been deprecated. ' +
                     'Use $mdThemingProviderTheme.' + colorType + 'Palette() instead.');
        return self[colorType + 'Palette'].apply(self, args);
      };
    });
  }

  /**
   * @ngdoc service
   * @name $mdTheming
   * @module material.core.theming
   * @description
   * Service that makes an element apply theming related <b>classes</b> to itself.
   *
   * For more information on the hue objects, their default values, as well as valid hue values, please visit <a ng-href="Theming/03_configuring_a_theme#specifying-custom-hues-for-color-intentions">the custom hues section of Configuring a Theme</a>.
   *
   * <hljs lang="js">
   * // Example component directive that we want to apply theming classes to.
   * app.directive('myFancyDirective', function($mdTheming) {
   *   return {
   *     restrict: 'AE',
   *     link: function(scope, element, attrs) {
   *       // Initialize the service using our directive's element
   *       $mdTheming(element);
   *
   *       $mdTheming.defineTheme('myTheme', {
   *         primary: 'blue',
   *         primaryHues: {
   *           default: '500',
   *           hue-1: '300',
   *           hue-2: '900',
   *           hue-3: 'A100'
   *         },
   *         accent: 'pink',
   *         accentHues: {
   *           default: '600',
   *           hue-1: '300',
   *           hue-2: '200',
   *           hue-3: 'A500'
   *         },
   *         warn: 'red',
   *         // It's not necessary to specify all hues in the object.
   *         warnHues: {
   *           default: '200',
   *           hue-3: 'A100'
   *         },
   *         // It's not necessary to specify custom hues at all.
   *         background: 'grey',
   *         dark: true
   *       });
   *       // Your directive's custom code here.
   *     }
   *   };
   * });
   * </hljs>
   * @param {element=} element Element that will have theming classes applied to it.
   */

  /**
   * @ngdoc property
   * @name $mdTheming#THEMES
   * @description
   * Property to get all the themes defined
   * @returns {object} All the themes defined with their properties.
   */

  /**
   * @ngdoc property
   * @name $mdTheming#PALETTES
   * @description
   * Property to get all the palettes defined
   * @returns {object} All the palettes defined with their colors.
   */

  /**
   * @ngdoc method
   * @name $mdTheming#registered
   * @description
   * Determine is specified theme name is a valid, registered theme
   * @param {string} themeName the theme to check if registered
   * @returns {boolean} whether the theme is registered or not
   */

  /**
   * @ngdoc method
   * @name $mdTheming#defaultTheme
   * @description
   * Returns the default theme
   * @returns {string} The default theme
   */

  /**
   * @ngdoc method
   * @name $mdTheming#generateTheme
   * @description
   * Lazy generate themes - by default, every theme is generated when defined.
   * You can disable this in the configuration section using the
   * `$mdThemingProvider.generateThemesOnDemand(true);`
   *
   * The theme name that is passed in must match the name of the theme that was defined as part of
   * the configuration block.
   *
   * @param {string} name theme name to generate
   */

  /**
   * @ngdoc method
   * @name $mdTheming#setBrowserColor
   * @description
   * Enables browser header coloring. For more info please visit
   * <a href="https://developers.google.com/web/fundamentals/design-and-ui/browser-customization/theme-color">
   *   Web Fundamentals</a>.
   * @param {object=} options Options for the browser color, which include:<br/>
   * - `theme` - `{string}`: A defined theme via `$mdThemeProvider` to use the palettes from.
   *    Default is `default` theme. <br/>
   * - `palette` - `{string}`:  Can be any one of the basic material design palettes, extended
   *    defined palettes, or `primary`, `accent`, `background`, and `warn`. Default is `primary`.
   * <br/>
   * - `hue` -  `{string}`: The hue from the selected palette. Default is `800`.<br/>
   * @returns {function} Function that removes the browser coloring when called.
   */

  /**
   * @ngdoc method
   * @name $mdTheming#defineTheme
   * @description
   * Dynamically define a theme by using an options object that contains palette names.
   *
   * @param {string} name Theme name to define
   * @param {object} options Theme definition options
   *
   * Options are:<br/>
   * - `primary` - `{string}`: The name of the primary palette to use in the theme.<br/>
   * - `primaryHues` - `{object=}`: Override hues for primary palette.<br/>
   * - `accent` - `{string}`: The name of the accent palette to use in the theme.<br/>
   * - `accentHues` - `{object=}`: Override hues for accent palette.<br/>
   * - `warn` - `{string}`: The name of the warn palette to use in the theme.<br/>
   * - `warnHues` - `{object=}`: Override hues for warn palette.<br/>
   * - `background` - `{string}`: The name of the background palette to use in the theme.<br/>
   * - `backgroundHues` - `{object=}`: Override hues for background palette.<br/>
   * - `dark` - `{boolean}`: Indicates if it's a dark theme.<br/>
   * @returns {Promise<string>} A resolved promise with the new theme name.
   */

  /* ngInject */
  function ThemingService($rootScope, $mdUtil, $q, $log) {
    // Allow us to be invoked via a linking function signature.
    var applyTheme = function (scope, el) {
      if (el === undefined) { el = scope; scope = undefined; }
      if (scope === undefined) { scope = $rootScope; }
      applyTheme.inherit(el, el);
    };

    Object.defineProperty(applyTheme, 'THEMES', {
      get: function () {
        return angular.extend({}, THEMES);
      }
    });
    Object.defineProperty(applyTheme, 'PALETTES', {
      get: function () {
        return angular.extend({}, PALETTES);
      }
    });
    Object.defineProperty(applyTheme, 'ALWAYS_WATCH', {
      get: function () {
        return alwaysWatchTheme;
      }
    });
    applyTheme.inherit = inheritTheme;
    applyTheme.registered = registered;
    applyTheme.defaultTheme = function() { return defaultTheme; };
    applyTheme.generateTheme = function(name) { generateTheme(THEMES[name], name, themeConfig.nonce); };
    applyTheme.defineTheme = function(name, options) {
      options = options || {};

      var theme = registerTheme(name);

      if (options.primary) {
        theme.primaryPalette(options.primary, options.primaryHues);
      }
      if (options.accent) {
        theme.accentPalette(options.accent, options.accentHues);
      }
      if (options.warn) {
        theme.warnPalette(options.warn, options.warnHues);
      }
      if (options.background) {
        theme.backgroundPalette(options.background, options.backgroundHues);
      }
      if (options.dark){
        theme.dark();
      }

      this.generateTheme(name);

      return $q.resolve(name);
    };
    applyTheme.setBrowserColor = enableBrowserColor;

    return applyTheme;

    /**
     * Determine is specified theme name is a valid, registered theme
     */
    function registered(themeName) {
      if (themeName === undefined || themeName === '') return true;
      return applyTheme.THEMES[themeName] !== undefined;
    }

    /**
     * Get theme name for the element, then update with Theme CSS class
     */
    function inheritTheme (el, parent) {
      var ctrl = parent.controller('mdTheme') || el.data('$mdThemeController');
      var scope = el.scope();

      updateThemeClass(lookupThemeName());

      if (ctrl) {
        var watchTheme = alwaysWatchTheme ||
                         ctrl.$shouldWatch ||
                         $mdUtil.parseAttributeBoolean(el.attr('md-theme-watch'));

        if (watchTheme || ctrl.isAsyncTheme) {
          var clearNameWatcher = function () {
            if (unwatch) {
              unwatch();
              unwatch = undefined;
            }
          };

          var unwatch = ctrl.registerChanges(function(name) {
            updateThemeClass(name);

            if (!watchTheme) {
              clearNameWatcher();
            }
          });

          if (scope) {
            scope.$on('$destroy', clearNameWatcher);
          } else {
            el.on('$destroy', clearNameWatcher);
          }
        }
      }

      /**
       * Find the theme name from the parent controller or element data
       */
      function lookupThemeName() {
        // As a few components (dialog) add their controllers later, we should also watch for a controller init.
        return ctrl && ctrl.$mdTheme || (defaultTheme === 'default' ? '' : defaultTheme);
      }

      /**
       * Remove old theme class and apply a new one
       * NOTE: if not a valid theme name, then the current name is not changed
       */
      function updateThemeClass(theme) {
        if (!theme) return;
        if (!registered(theme)) {
          $log.warn('Attempted to use unregistered theme \'' + theme + '\'. ' +
                    'Register it with $mdThemingProvider.theme().');
        }

        var oldTheme = el.data('$mdThemeName');
        if (oldTheme) el.removeClass('md-' + oldTheme +'-theme');
        el.addClass('md-' + theme + '-theme');
        el.data('$mdThemeName', theme);
        if (ctrl) {
          el.data('$mdThemeController', ctrl);
        }
      }
    }

  }
}

function ThemingDirective($mdTheming, $interpolate, $parse, $mdUtil, $q, $log) {
  return {
    priority: 101, // has to be more than 100 to be before interpolation (issue on IE)
    link: {
      pre: function(scope, el, attrs) {
        var registeredCallbacks = [];

        var startSymbol = $interpolate.startSymbol();
        var endSymbol = $interpolate.endSymbol();

        var theme = attrs.mdTheme.trim();

        var hasInterpolation =
          theme.substr(0, startSymbol.length) === startSymbol &&
          theme.lastIndexOf(endSymbol) === theme.length - endSymbol.length;

        var oneTimeOperator = '::';
        var oneTimeBind = attrs.mdTheme
            .split(startSymbol).join('')
            .split(endSymbol).join('')
            .trim()
            .substr(0, oneTimeOperator.length) === oneTimeOperator;

        var getTheme = function () {
          var interpolation = $interpolate(attrs.mdTheme)(scope);
          return $parse(interpolation)(scope) || interpolation;
        };

        var ctrl = {
          isAsyncTheme: angular.isFunction(getTheme()) || angular.isFunction(getTheme().then),
          registerChanges: function (cb, context) {
            if (context) {
              cb = angular.bind(context, cb);
            }

            registeredCallbacks.push(cb);

            return function () {
              var index = registeredCallbacks.indexOf(cb);

              if (index > -1) {
                registeredCallbacks.splice(index, 1);
              }
            };
          },
          $setTheme: function (theme) {
            if (!$mdTheming.registered(theme)) {
              $log.warn('attempted to use unregistered theme \'' + theme + '\'');
            }

            ctrl.$mdTheme = theme;

            // Iterating backwards to support unregistering during iteration
            // http://stackoverflow.com/a/9882349/890293
            // we don't use `reverse()` of array because it mutates the array and we don't want it
            // to get re-indexed
            for (var i = registeredCallbacks.length; i--;) {
              registeredCallbacks[i](theme);
            }
          },
          $shouldWatch: $mdUtil.parseAttributeBoolean(el.attr('md-theme-watch')) ||
                        $mdTheming.ALWAYS_WATCH ||
                        (hasInterpolation && !oneTimeBind)
        };

        el.data('$mdThemeController', ctrl);

        var setParsedTheme = function (theme) {
          if (typeof theme === 'string') {
            return ctrl.$setTheme(theme);
          }

          $q.when(angular.isFunction(theme) ?  theme() : theme)
            .then(function(name) {
              ctrl.$setTheme(name);
            });
        };

        setParsedTheme(getTheme());

        var unwatch = scope.$watch(getTheme, function(theme) {
          if (theme) {
            setParsedTheme(theme);

            if (!ctrl.$shouldWatch) {
              unwatch();
            }
          }
        });
      }
    }
  };
}

/**
 * Special directive that will disable ALL runtime Theme style generation and DOM injection
 *
 * <link rel="stylesheet" href="angular-material.min.css">
 * <link rel="stylesheet" href="angular-material.themes.css">
 *
 * <body md-themes-disabled>
 *  ...
 * </body>
 *
 * Note: Using md-themes-css directive requires the developer to load external
 * theme stylesheets; e.g. custom themes from Material-Tools:
 *
 *       `angular-material.themes.css`
 *
 * Another option is to use the ThemingProvider to configure and disable the attribute
 * conversions; this would obviate the use of the `md-themes-css` directive
 *
 */
function disableThemesDirective() {
  themeConfig.disableTheming = true;

  // Return a 1x-only, first-match attribute directive
  return {
    restrict : 'A',
    priority : '900'
  };
}

function ThemableDirective($mdTheming) {
  return $mdTheming;
}

function parseRules(theme, colorType, rules) {
  checkValidPalette(theme, colorType);

  rules = rules.replace(/THEME_NAME/g, theme.name);
  var themeNameRegex = new RegExp('\\.md-' + theme.name + '-theme', 'g');
  var simpleVariableRegex = /'?"?\{\{\s*([a-zA-Z]+)-(A?\d+|hue-[0-3]|shadow|default)-?(\d\.?\d*)?(contrast)?\s*\}\}'?"?/g;

  // find and replace simple variables where we use a specific hue, not an entire palette
  // eg. "{{primary-100}}"
  // \(' + THEME_COLOR_TYPES.join('\|') + '\)'
  rules = rules.replace(simpleVariableRegex, function(match, colorType, hue, opacity, contrast) {
    if (colorType === 'foreground') {
      if (hue == 'shadow') {
        return theme.foregroundShadow;
      } else {
        return theme.foregroundPalette[hue] || theme.foregroundPalette['1'];
      }
    }

    // `default` is also accepted as a hue-value, because the background palettes are
    // using it as a name for the default hue.
    if (hue.indexOf('hue') === 0 || hue === 'default') {
      hue = theme.colors[colorType].hues[hue];
    }

    return rgba((PALETTES[ theme.colors[colorType].name ][hue] || '')[contrast ? 'contrast' : 'value'], opacity);
  });

  // Matches '{{ primary-color }}', etc
  var hueRegex = new RegExp('(\'|")?{{\\s*([a-zA-Z]+)-(color|contrast)-?(\\d\\.?\\d*)?\\s*}}("|\')?','g');
  var generatedRules = [];

  // For each type, generate rules for each hue (ie. default, md-hue-1, md-hue-2, md-hue-3)
  angular.forEach(['default', 'hue-1', 'hue-2', 'hue-3'], function(hueName) {
    var newRule = rules
      .replace(hueRegex, function(match, _, matchedColorType, hueType, opacity) {
        var color = theme.colors[matchedColorType];
        var palette = PALETTES[color.name];
        var hueValue = color.hues[hueName];
        return rgba(palette[hueValue][hueType === 'color' ? 'value' : 'contrast'], opacity);
      });
    if (hueName !== 'default') {
      newRule = newRule.replace(themeNameRegex, '.md-' + theme.name + '-theme.md-' + hueName);
    }

    // Don't apply a selector rule to the default theme, making it easier to override
    // styles of the base-component
    if (theme.name == 'default') {
      var themeRuleRegex = /((?:\s|>|\.|\w|-|:|\(|\)|\[|\]|"|'|=)*)\.md-default-theme((?:\s|>|\.|\w|-|:|\(|\)|\[|\]|"|'|=)*)/g;

      newRule = newRule.replace(themeRuleRegex, function(match, start, end) {
        return match + ', ' + start + end;
      });
    }
    generatedRules.push(newRule);
  });

  return generatedRules;
}

var rulesByType = {};

// Generate our themes at run time given the state of THEMES and PALETTES
function generateAllThemes($injector, $mdTheming) {
  var head = document.head;
  var firstChild = head ? head.firstElementChild : null;
  var themeCss = !themeConfig.disableTheming && $injector.has('$MD_THEME_CSS') ? $injector.get('$MD_THEME_CSS') : '';

  // Append our custom registered styles to the theme stylesheet.
  themeCss += themeConfig.registeredStyles.join('');

  if (!firstChild) return;
  if (themeCss.length === 0) return; // no rules, so no point in running this expensive task

  // Expose contrast colors for palettes to ensure that text is always readable
  angular.forEach(PALETTES, sanitizePalette);

  // MD_THEME_CSS is a string generated by the build process that includes all the themable
  // components as templates

  // Break the CSS into individual rules
  var rules = themeCss
                  .split(/\}(?!(\}|'|"|;))/)
                  .filter(function(rule) { return rule && rule.trim().length; })
                  .map(function(rule) { return rule.trim() + '}'; });

  THEME_COLOR_TYPES.forEach(function(type) {
    rulesByType[type] = '';
  });

  // Sort the rules based on type, allowing us to do color substitution on a per-type basis
  rules.forEach(function(rule) {
    // First: test that if the rule has '.md-accent', it goes into the accent set of rules
    for (var i = 0, type; type = THEME_COLOR_TYPES[i]; i++) {
      if (rule.indexOf('.md-' + type) > -1) {
        return rulesByType[type] += rule;
      }
    }

    // If no eg 'md-accent' class is found, try to just find 'accent' in the rule and guess from
    // there
    for (i = 0; type = THEME_COLOR_TYPES[i]; i++) {
      if (rule.indexOf(type) > -1) {
        return rulesByType[type] += rule;
      }
    }

    // Default to the primary array
    return rulesByType[DEFAULT_COLOR_TYPE] += rule;
  });

  // If themes are being generated on-demand, quit here. The user will later manually
  // call generateTheme to do this on a theme-by-theme basis.
  if (themeConfig.generateOnDemand) return;

  angular.forEach($mdTheming.THEMES, function(theme) {
    if (!GENERATED[theme.name] && !($mdTheming.defaultTheme() !== 'default' && theme.name === 'default')) {
      generateTheme(theme, theme.name, themeConfig.nonce);
    }
  });


  // *************************
  // Internal functions
  // *************************

  // The user specifies a 'default' contrast color as either light or dark,
  // then explicitly lists which hues are the opposite contrast (eg. A100 has dark, A200 has light)
  function sanitizePalette(palette, name) {
    var defaultContrast = palette.contrastDefaultColor;
    var lightColors = palette.contrastLightColors || [];
    var strongLightColors = palette.contrastStrongLightColors || [];
    var darkColors = palette.contrastDarkColors || [];

    // These colors are provided as space-separated lists
    if (typeof lightColors === 'string') lightColors = lightColors.split(' ');
    if (typeof strongLightColors === 'string') strongLightColors = strongLightColors.split(' ');
    if (typeof darkColors === 'string') darkColors = darkColors.split(' ');

    // Cleanup after ourselves
    delete palette.contrastDefaultColor;
    delete palette.contrastLightColors;
    delete palette.contrastStrongLightColors;
    delete palette.contrastDarkColors;

    // Change { 'A100': '#fffeee' } to { 'A100': { value: '#fffeee', contrast:DARK_CONTRAST_COLOR }
    angular.forEach(palette, function(hueValue, hueName) {
      if (angular.isObject(hueValue)) return; // Already converted
      // Map everything to rgb colors
      var rgbValue = colorToRgbaArray(hueValue);
      if (!rgbValue) {
        throw new Error("Color %1, in palette %2's hue %3, is invalid. Hex or rgb(a) color expected."
                        .replace('%1', hueValue)
                        .replace('%2', palette.name)
                        .replace('%3', hueName));
      }

      palette[hueName] = {
        hex: palette[hueName],
        value: rgbValue,
        contrast: getContrastColor()
      };
      function getContrastColor() {
        if (defaultContrast === 'light') {
          if (darkColors.indexOf(hueName) > -1) {
            return DARK_CONTRAST_COLOR;
          } else {
            return strongLightColors.indexOf(hueName) > -1 ? STRONG_LIGHT_CONTRAST_COLOR
              : LIGHT_CONTRAST_COLOR;
          }
        } else {
          if (lightColors.indexOf(hueName) > -1) {
            return strongLightColors.indexOf(hueName) > -1 ? STRONG_LIGHT_CONTRAST_COLOR
              : LIGHT_CONTRAST_COLOR;
          } else {
            return DARK_CONTRAST_COLOR;
          }
        }
      }
    });
  }
}

function generateTheme(theme, name, nonce) {
  var head = document.head;
  var firstChild = head ? head.firstElementChild : null;

  if (!GENERATED[name]) {
    // For each theme, use the color palettes specified for
    // `primary`, `warn` and `accent` to generate CSS rules.
    THEME_COLOR_TYPES.forEach(function(colorType) {
      var styleStrings = parseRules(theme, colorType, rulesByType[colorType]);
      while (styleStrings.length) {
        var styleContent = styleStrings.shift();
        if (styleContent) {
          var style = document.createElement('style');
          style.setAttribute('md-theme-style', '');
          if (nonce) {
            style.setAttribute('nonce', nonce);
          }
          style.appendChild(document.createTextNode(styleContent));
          head.insertBefore(style, firstChild);
        }
      }
    });

    GENERATED[theme.name] = true;
  }

}


function checkValidPalette(theme, colorType) {
  // If theme attempts to use a palette that doesnt exist, throw error
  if (!PALETTES[ (theme.colors[colorType] || {}).name ]) {
    throw new Error(
      "You supplied an invalid color palette for theme %1's %2 palette. Available palettes: %3"
                    .replace('%1', theme.name)
                    .replace('%2', colorType)
                    .replace('%3', Object.keys(PALETTES).join(', '))
    );
  }
}

function colorToRgbaArray(clr) {
  if (angular.isArray(clr) && clr.length == 3) return clr;
  if (/^rgb/.test(clr)) {
    return clr.replace(/(^\s*rgba?\(|\)\s*$)/g, '').split(',').map(function(value, i) {
      return i == 3 ? parseFloat(value, 10) : parseInt(value, 10);
    });
  }
  if (clr.charAt(0) == '#') clr = clr.substring(1);
  if (!/^([a-fA-F0-9]{3}){1,2}$/g.test(clr)) return;

  var dig = clr.length / 3;
  var red = clr.substr(0, dig);
  var grn = clr.substr(dig, dig);
  var blu = clr.substr(dig * 2);
  if (dig === 1) {
    red += red;
    grn += grn;
    blu += blu;
  }
  return [parseInt(red, 16), parseInt(grn, 16), parseInt(blu, 16)];
}

function rgba(rgbArray, opacity) {
  if (!rgbArray) return "rgb('0,0,0')";

  if (rgbArray.length == 4) {
    rgbArray = angular.copy(rgbArray);
    opacity ? rgbArray.pop() : opacity = rgbArray.pop();
  }
  return opacity && (typeof opacity == 'number' || (typeof opacity == 'string' && opacity.length)) ?
    'rgba(' + rgbArray.join(',') + ',' + opacity + ')' :
    'rgb(' + rgbArray.join(',') + ')';
}


})(window.angular);

(function() {
  'use strict';

  /**
   * @ngdoc service
   * @name $mdButtonInkRipple
   * @module material.core
   *
   * @description
   * Provides ripple effects for md-button.  See $mdInkRipple service for all possible configuration options.
   *
   * @param {object=} scope Scope within the current context
   * @param {object=} element The element the ripple effect should be applied to
   * @param {object=} options (Optional) Configuration options to override the default ripple configuration
   */

  MdButtonInkRipple['$inject'] = ["$mdInkRipple"];
  angular.module('material.core')
    .factory('$mdButtonInkRipple', MdButtonInkRipple);

  function MdButtonInkRipple($mdInkRipple) {
    return {
      attach: function attachRipple(scope, element, options) {
        options = angular.extend(optionsForElement(element), options);

        return $mdInkRipple.attach(scope, element, options);
      }
    };

    function optionsForElement(element) {
      if (element.hasClass('md-icon-button')) {
        return {
          isMenuItem: element.hasClass('md-menu-item'),
          fitRipple: true,
          center: true
        };
      } else {
        return {
          isMenuItem: element.hasClass('md-menu-item'),
          dimBackground: true
        };
      }
    }
  }
})();

(function() {
  'use strict';

    /**
   * @ngdoc service
   * @name $mdCheckboxInkRipple
   * @module material.core
   *
   * @description
   * Provides ripple effects for md-checkbox.  See $mdInkRipple service for all possible configuration options.
   *
   * @param {object=} scope Scope within the current context
   * @param {object=} element The element the ripple effect should be applied to
   * @param {object=} options (Optional) Configuration options to override the defaultripple configuration
   */

  MdCheckboxInkRipple['$inject'] = ["$mdInkRipple"];
  angular.module('material.core')
    .factory('$mdCheckboxInkRipple', MdCheckboxInkRipple);

  function MdCheckboxInkRipple($mdInkRipple) {
    return {
      attach: attach
    };

    function attach(scope, element, options) {
      return $mdInkRipple.attach(scope, element, angular.extend({
        center: true,
        dimBackground: false,
        fitRipple: true
      }, options));
    }
  }
})();

(function() {
  'use strict';

  /**
   * @ngdoc service
   * @name $mdListInkRipple
   * @module material.core
   *
   * @description
   * Provides ripple effects for md-list.  See $mdInkRipple service for all possible configuration options.
   *
   * @param {object=} scope Scope within the current context
   * @param {object=} element The element the ripple effect should be applied to
   * @param {object=} options (Optional) Configuration options to override the defaultripple configuration
   */

  MdListInkRipple['$inject'] = ["$mdInkRipple"];
  angular.module('material.core')
    .factory('$mdListInkRipple', MdListInkRipple);

  function MdListInkRipple($mdInkRipple) {
    return {
      attach: attach
    };

    function attach(scope, element, options) {
      return $mdInkRipple.attach(scope, element, angular.extend({
        center: false,
        dimBackground: true,
        outline: false,
        rippleSize: 'full'
      }, options));
    }
  }
})();

/**
 * @ngdoc module
 * @name material.core.ripple
 * @description
 * Ripple
 */
InkRippleCtrl['$inject'] = ["$scope", "$element", "rippleOptions", "$window", "$timeout", "$mdUtil", "$mdColorUtil"];
InkRippleDirective['$inject'] = ["$mdButtonInkRipple", "$mdCheckboxInkRipple"];
angular.module('material.core')
    .provider('$mdInkRipple', InkRippleProvider)
    .directive('mdInkRipple', InkRippleDirective)
    .directive('mdNoInk', attrNoDirective)
    .directive('mdNoBar', attrNoDirective)
    .directive('mdNoStretch', attrNoDirective);

var DURATION = 450;

/**
 * @ngdoc directive
 * @name mdInkRipple
 * @module material.core.ripple
 *
 * @description
 * The `md-ink-ripple` directive allows you to specify the ripple color or if a ripple is allowed.
 *
 * @param {string|boolean} md-ink-ripple A color string `#FF0000` or boolean (`false` or `0`) for
 *  preventing ripple
 *
 * @usage
 * ### String values
 * <hljs lang="html">
 *   <ANY md-ink-ripple="#FF0000">
 *     Ripples in red
 *   </ANY>
 *
 *   <ANY md-ink-ripple="false">
 *     Not rippling
 *   </ANY>
 * </hljs>
 *
 * ### Interpolated values
 * <hljs lang="html">
 *   <ANY md-ink-ripple="{{ randomColor() }}">
 *     Ripples with the return value of 'randomColor' function
 *   </ANY>
 *
 *   <ANY md-ink-ripple="{{ canRipple() }}">
 *     Ripples if 'canRipple' function return value is not 'false' or '0'
 *   </ANY>
 * </hljs>
 */
function InkRippleDirective ($mdButtonInkRipple, $mdCheckboxInkRipple) {
  return {
    controller: angular.noop,
    link:       function (scope, element, attr) {
      attr.hasOwnProperty('mdInkRippleCheckbox')
          ? $mdCheckboxInkRipple.attach(scope, element)
          : $mdButtonInkRipple.attach(scope, element);
    }
  };
}

/**
 * @ngdoc service
 * @name $mdInkRipple
 * @module material.core.ripple
 *
 * @description
 * `$mdInkRipple` is a service for adding ripples to any element.
 *
 * @usage
 * <hljs lang="js">
 * app.factory('$myElementInkRipple', function($mdInkRipple) {
 *   return {
 *     attach: function (scope, element, options) {
 *       return $mdInkRipple.attach(scope, element, angular.extend({
 *         center: false,
 *         dimBackground: true
 *       }, options));
 *     }
 *   };
 * });
 *
 * app.controller('myController', function ($scope, $element, $myElementInkRipple) {
 *   $scope.onClick = function (ev) {
 *     $myElementInkRipple.attach($scope, angular.element(ev.target), { center: true });
 *   }
 * });
 * </hljs>
 */

/**
 * @ngdoc service
 * @name $mdInkRippleProvider
 * @module material.core.ripple
 *
 * @description
  * If you want to disable ink ripples globally, for all components, you can call the
 * `disableInkRipple` method in your app's config.
 *
 *
 * @usage
 * <hljs lang="js">
 * app.config(function ($mdInkRippleProvider) {
 *   $mdInkRippleProvider.disableInkRipple();
 * });
 * </hljs>
 */

function InkRippleProvider () {
  var isDisabledGlobally = false;

  return {
    disableInkRipple: disableInkRipple,
    $get: ["$injector", function($injector) {
      return { attach: attach };

      /**
       * @ngdoc method
       * @name $mdInkRipple#attach
       *
       * @description
       * Attaching given scope, element and options to inkRipple controller
       *
       * @param {object=} scope Scope within the current context
       * @param {object=} element The element the ripple effect should be applied to
       * @param {object=} options (Optional) Configuration options to override the defaultRipple configuration
       * * `center` -  Whether the ripple should start from the center of the container element
       * * `dimBackground` - Whether the background should be dimmed with the ripple color
       * * `colorElement` - The element the ripple should take its color from, defined by css property `color`
       * * `fitRipple` - Whether the ripple should fill the element
       */
      function attach (scope, element, options) {
        if (isDisabledGlobally || element.controller('mdNoInk')) return angular.noop;
        return $injector.instantiate(InkRippleCtrl, {
          $scope:        scope,
          $element:      element,
          rippleOptions: options
        });
      }
    }]
  };

  /**
   * @ngdoc method
   * @name $mdInkRippleProvider#disableInkRipple
   *
   * @description
   * A config-time method that, when called, disables ripples globally.
   */
  function disableInkRipple () {
    isDisabledGlobally = true;
  }
}

/**
 * Controller used by the ripple service in order to apply ripples
 * ngInject
 */
function InkRippleCtrl ($scope, $element, rippleOptions, $window, $timeout, $mdUtil, $mdColorUtil) {
  this.$window    = $window;
  this.$timeout   = $timeout;
  this.$mdUtil    = $mdUtil;
  this.$mdColorUtil    = $mdColorUtil;
  this.$scope     = $scope;
  this.$element   = $element;
  this.options    = rippleOptions;
  this.mousedown  = false;
  this.ripples    = [];
  this.timeout    = null; // Stores a reference to the most-recent ripple timeout
  this.lastRipple = null;

  $mdUtil.valueOnUse(this, 'container', this.createContainer);

  this.$element.addClass('md-ink-ripple');

  // attach method for unit tests
  ($element.controller('mdInkRipple') || {}).createRipple = angular.bind(this, this.createRipple);
  ($element.controller('mdInkRipple') || {}).setColor = angular.bind(this, this.color);

  this.bindEvents();
}


/**
 * Either remove or unlock any remaining ripples when the user mouses off of the element (either by
 * mouseup or mouseleave event)
 */
function autoCleanup (self, cleanupFn) {
  if (self.mousedown || self.lastRipple) {
    self.mousedown = false;
    self.$mdUtil.nextTick(angular.bind(self, cleanupFn), false);
  }
}


/**
 * Returns the color that the ripple should be (either based on CSS or hard-coded)
 * @returns {string}
 */
InkRippleCtrl.prototype.color = function (value) {
  var self = this;

  // If assigning a color value, apply it to background and the ripple color
  if (angular.isDefined(value)) {
    self._color = self._parseColor(value);
  }

  // If color lookup, use assigned, defined, or inherited
  return self._color || self._parseColor(self.inkRipple()) || self._parseColor(getElementColor());

  /**
   * Finds the color element and returns its text color for use as default ripple color
   * @returns {string}
   */
  function getElementColor () {
    var items = self.options && self.options.colorElement ? self.options.colorElement : [];
    var elem =  items.length ? items[ 0 ] : self.$element[ 0 ];

    return elem ? self.$window.getComputedStyle(elem).color : 'rgb(0,0,0)';
  }
};

/**
 * Updating the ripple colors based on the current inkRipple value
 * or the element's computed style color
 */
InkRippleCtrl.prototype.calculateColor = function () {
  return this.color();
};


/**
 * Takes a string color and converts it to RGBA format
 * @param {string} color
 * @param {number} multiplier
 * @returns {string}
 */
InkRippleCtrl.prototype._parseColor = function parseColor (color, multiplier) {
  multiplier = multiplier || 1;
  var colorUtil = this.$mdColorUtil;

  if (!color) return;
  if (color.indexOf('rgba') === 0) return color.replace(/\d?\.?\d*\s*\)\s*$/, (0.1 * multiplier).toString() + ')');
  if (color.indexOf('rgb') === 0) return colorUtil.rgbToRgba(color);
  if (color.indexOf('#') === 0) return colorUtil.hexToRgba(color);

};

/**
 * Binds events to the root element for
 */
InkRippleCtrl.prototype.bindEvents = function () {
  this.$element.on('mousedown', angular.bind(this, this.handleMousedown));
  this.$element.on('mouseup touchend', angular.bind(this, this.handleMouseup));
  this.$element.on('mouseleave', angular.bind(this, this.handleMouseup));
  this.$element.on('touchmove', angular.bind(this, this.handleTouchmove));
};

/**
 * Create a new ripple on every mousedown event from the root element
 * @param event {MouseEvent}
 */
InkRippleCtrl.prototype.handleMousedown = function (event) {
  if (this.mousedown) return;

  // When jQuery is loaded, we have to get the original event
  if (event.hasOwnProperty('originalEvent')) event = event.originalEvent;
  this.mousedown = true;
  if (this.options.center) {
    this.createRipple(this.container.prop('clientWidth') / 2, this.container.prop('clientWidth') / 2);
  } else {

    // We need to calculate the relative coordinates if the target is a sublayer of the ripple element
    if (event.srcElement !== this.$element[0]) {
      var layerRect = this.$element[0].getBoundingClientRect();
      var layerX = event.clientX - layerRect.left;
      var layerY = event.clientY - layerRect.top;

      this.createRipple(layerX, layerY);
    } else {
      this.createRipple(event.offsetX, event.offsetY);
    }
  }
};

/**
 * Either remove or unlock any remaining ripples when the user mouses off of the element (either by
 * mouseup, touchend or mouseleave event)
 */
InkRippleCtrl.prototype.handleMouseup = function () {
  this.$timeout(function () {
    autoCleanup(this, this.clearRipples);
  }.bind(this));
};

/**
 * Either remove or unlock any remaining ripples when the user mouses off of the element (by
 * touchmove)
 */
InkRippleCtrl.prototype.handleTouchmove = function () {
  autoCleanup(this, this.deleteRipples);
};

/**
 * Cycles through all ripples and attempts to remove them.
 */
InkRippleCtrl.prototype.deleteRipples = function () {
  for (var i = 0; i < this.ripples.length; i++) {
    this.ripples[ i ].remove();
  }
};

/**
 * Cycles through all ripples and attempts to remove them with fade.
 * Depending on logic within `fadeInComplete`, some removals will be postponed.
 */
InkRippleCtrl.prototype.clearRipples = function () {
  for (var i = 0; i < this.ripples.length; i++) {
    this.fadeInComplete(this.ripples[ i ]);
  }
};

/**
 * Creates the ripple container element
 * @returns {*}
 */
InkRippleCtrl.prototype.createContainer = function () {
  var container = angular.element('<div class="md-ripple-container"></div>');
  this.$element.append(container);
  return container;
};

InkRippleCtrl.prototype.clearTimeout = function () {
  if (this.timeout) {
    this.$timeout.cancel(this.timeout);
    this.timeout = null;
  }
};

InkRippleCtrl.prototype.isRippleAllowed = function () {
  var element = this.$element[0];
  do {
    if (!element.tagName || element.tagName === 'BODY') break;

    if (element && angular.isFunction(element.hasAttribute)) {
      if (element.hasAttribute('disabled')) return false;
      if (this.inkRipple() === 'false' || this.inkRipple() === '0') return false;
    }

  } while (element = element.parentNode);
  return true;
};

/**
 * The attribute `md-ink-ripple` may be a static or interpolated
 * color value OR a boolean indicator (used to disable ripples)
 */
InkRippleCtrl.prototype.inkRipple = function () {
  return this.$element.attr('md-ink-ripple');
};

/**
 * Creates a new ripple and adds it to the container.  Also tracks ripple in `this.ripples`.
 * @param left
 * @param top
 */
InkRippleCtrl.prototype.createRipple = function (left, top) {
  if (!this.isRippleAllowed()) return;

  var ctrl        = this;
  var colorUtil   = ctrl.$mdColorUtil;
  var ripple      = angular.element('<div class="md-ripple"></div>');
  var width       = this.$element.prop('clientWidth');
  var height      = this.$element.prop('clientHeight');
  var x           = Math.max(Math.abs(width - left), left) * 2;
  var y           = Math.max(Math.abs(height - top), top) * 2;
  var size        = getSize(this.options.fitRipple, x, y);
  var color       = this.calculateColor();

  ripple.css({
    left:            left + 'px',
    top:             top + 'px',
    background:      'black',
    width:           size + 'px',
    height:          size + 'px',
    backgroundColor: colorUtil.rgbaToRgb(color),
    borderColor:     colorUtil.rgbaToRgb(color)
  });
  this.lastRipple = ripple;

  // we only want one timeout to be running at a time
  this.clearTimeout();
  this.timeout    = this.$timeout(function () {
    ctrl.clearTimeout();
    if (!ctrl.mousedown) ctrl.fadeInComplete(ripple);
  }, DURATION * 0.35, false);

  if (this.options.dimBackground) this.container.css({ backgroundColor: color });
  this.container.append(ripple);
  this.ripples.push(ripple);
  ripple.addClass('md-ripple-placed');

  this.$mdUtil.nextTick(function () {

    ripple.addClass('md-ripple-scaled md-ripple-active');
    ctrl.$timeout(function () {
      ctrl.clearRipples();
    }, DURATION, false);

  }, false);

  function getSize (fit, x, y) {
    return fit
        ? Math.max(x, y)
        : Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
  }
};



/**
 * After fadeIn finishes, either kicks off the fade-out animation or queues the element for removal on mouseup
 * @param ripple
 */
InkRippleCtrl.prototype.fadeInComplete = function (ripple) {
  if (this.lastRipple === ripple) {
    if (!this.timeout && !this.mousedown) {
      this.removeRipple(ripple);
    }
  } else {
    this.removeRipple(ripple);
  }
};

/**
 * Kicks off the animation for removing a ripple
 * @param ripple {Element}
 */
InkRippleCtrl.prototype.removeRipple = function (ripple) {
  var ctrl  = this;
  var index = this.ripples.indexOf(ripple);
  if (index < 0) return;
  this.ripples.splice(this.ripples.indexOf(ripple), 1);
  ripple.removeClass('md-ripple-active');
  ripple.addClass('md-ripple-remove');
  if (this.ripples.length === 0) this.container.css({ backgroundColor: '' });
  // use a 2-second timeout in order to allow for the animation to finish
  // we don't actually care how long the animation takes
  this.$timeout(function () {
    ctrl.fadeOutComplete(ripple);
  }, DURATION, false);
};

/**
 * Removes the provided ripple from the DOM
 * @param ripple
 */
InkRippleCtrl.prototype.fadeOutComplete = function (ripple) {
  ripple.remove();
  this.lastRipple = null;
};

/**
 * Used to create an empty directive.  This is used to track flag-directives whose children may have
 * functionality based on them.
 *
 * Example: `md-no-ink` will potentially be used by all child directives.
 */
function attrNoDirective () {
  return { controller: angular.noop };
}

(function() {
  'use strict';

    /**
   * @ngdoc service
   * @name $mdTabInkRipple
   * @module material.core
   *
   * @description
   * Provides ripple effects for md-tabs.  See $mdInkRipple service for all possible configuration options.
   *
   * @param {object=} scope Scope within the current context
   * @param {object=} element The element the ripple effect should be applied to
   * @param {object=} options (Optional) Configuration options to override the defaultripple configuration
   */

  MdTabInkRipple['$inject'] = ["$mdInkRipple"];
  angular.module('material.core')
    .factory('$mdTabInkRipple', MdTabInkRipple);

  function MdTabInkRipple($mdInkRipple) {
    return {
      attach: attach
    };

    function attach(scope, element, options) {
      return $mdInkRipple.attach(scope, element, angular.extend({
        center: false,
        dimBackground: true,
        outline: false,
        rippleSize: 'full'
      }, options));
    }
  }
})();

// Polyfill angular < 1.4 (provide $animateCss)
angular
  .module('material.core')
  .factory('$$mdAnimate', ["$q", "$timeout", "$mdConstant", "$animateCss", function($q, $timeout, $mdConstant, $animateCss){

     // Since $$mdAnimate is injected into $mdUtil... use a wrapper function
     // to subsequently inject $mdUtil as an argument to the AnimateDomUtils

     return function($mdUtil) {
       return AnimateDomUtils($mdUtil, $q, $timeout, $mdConstant, $animateCss);
     };
   }]);

/**
 * Factory function that requires special injections
 */
function AnimateDomUtils($mdUtil, $q, $timeout, $mdConstant, $animateCss) {
  var self;
  return self = {
    /**
     *
     */
    translate3d : function(target, from, to, options) {
      return $animateCss(target, {
        from: from,
        to: to,
        addClass: options.transitionInClass,
        removeClass: options.transitionOutClass,
        duration: options.duration
      })
      .start()
      .then(function(){
          // Resolve with reverser function...
          return reverseTranslate;
      });

      /**
       * Specific reversal of the request translate animation above...
       */
      function reverseTranslate (newFrom) {
        return $animateCss(target, {
           to: newFrom || from,
           addClass: options.transitionOutClass,
           removeClass: options.transitionInClass,
           duration: options.duration
        }).start();

      }
    },

    /**
     * Listen for transitionEnd event (with optional timeout)
     * Announce completion or failure via promise handlers
     */
    waitTransitionEnd: function (element, opts) {
      var TIMEOUT = 3000; // fallback is 3 secs

      return $q(function(resolve, reject){
        opts = opts || { };

        // If there is no transition is found, resolve immediately
        //
        // NOTE: using $mdUtil.nextTick() causes delays/issues
        if (noTransitionFound(opts.cachedTransitionStyles)) {
          TIMEOUT = 0;
        }

        var timer = $timeout(finished, opts.timeout || TIMEOUT);
        element.on($mdConstant.CSS.TRANSITIONEND, finished);

        /**
         * Upon timeout or transitionEnd, reject or resolve (respectively) this promise.
         * NOTE: Make sure this transitionEnd didn't bubble up from a child
         */
        function finished(ev) {
          if (ev && ev.target !== element[0]) return;

          if (ev) $timeout.cancel(timer);
          element.off($mdConstant.CSS.TRANSITIONEND, finished);

          // Never reject since ngAnimate may cause timeouts due missed transitionEnd events
          resolve();

        }

        /**
         * Checks whether or not there is a transition.
         *
         * @param styles The cached styles to use for the calculation. If null, getComputedStyle()
         * will be used.
         *
         * @returns {boolean} True if there is no transition/duration; false otherwise.
         */
        function noTransitionFound(styles) {
          styles = styles || window.getComputedStyle(element[0]);

          return styles.transitionDuration == '0s' || (!styles.transition && !styles.transitionProperty);
        }

      });
    },

    calculateTransformValues: function (element, originator) {
      var origin = originator.element;
      var bounds = originator.bounds;

      if (origin || bounds) {
        var originBnds = origin ? self.clientRect(origin) || currentBounds() : self.copyRect(bounds);
        var dialogRect = self.copyRect(element[0].getBoundingClientRect());
        var dialogCenterPt = self.centerPointFor(dialogRect);
        var originCenterPt = self.centerPointFor(originBnds);

        return {
          centerX: originCenterPt.x - dialogCenterPt.x,
          centerY: originCenterPt.y - dialogCenterPt.y,
          scaleX: Math.round(100 * Math.min(0.5, originBnds.width / dialogRect.width)) / 100,
          scaleY: Math.round(100 * Math.min(0.5, originBnds.height / dialogRect.height)) / 100
        };
      }
      return {centerX: 0, centerY: 0, scaleX: 0.5, scaleY: 0.5};

      /**
       * This is a fallback if the origin information is no longer valid, then the
       * origin bounds simply becomes the current bounds for the dialogContainer's parent
       */
      function currentBounds() {
        var cntr = element ? element.parent() : null;
        var parent = cntr ? cntr.parent() : null;

        return parent ? self.clientRect(parent) : null;
      }
    },

    /**
     * Calculate the zoom transform from dialog to origin.
     *
     * We use this to set the dialog position immediately;
     * then the md-transition-in actually translates back to
     * `translate3d(0,0,0) scale(1.0)`...
     *
     * NOTE: all values are rounded to the nearest integer
     */
    calculateZoomToOrigin: function (element, originator) {
      var zoomTemplate = "translate3d( {centerX}px, {centerY}px, 0 ) scale( {scaleX}, {scaleY} )";
      var buildZoom = angular.bind(null, $mdUtil.supplant, zoomTemplate);

      return buildZoom(self.calculateTransformValues(element, originator));
    },

    /**
     * Calculate the slide transform from panel to origin.
     * NOTE: all values are rounded to the nearest integer
     */
    calculateSlideToOrigin: function (element, originator) {
      var slideTemplate = "translate3d( {centerX}px, {centerY}px, 0 )";
      var buildSlide = angular.bind(null, $mdUtil.supplant, slideTemplate);

      return buildSlide(self.calculateTransformValues(element, originator));
    },

    /**
     * Enhance raw values to represent valid css stylings...
     */
    toCss : function(raw) {
      var css = { };
      var lookups = 'left top right bottom width height x y min-width min-height max-width max-height';

      angular.forEach(raw, function(value,key) {
        if (angular.isUndefined(value)) return;

        if (lookups.indexOf(key) >= 0) {
          css[key] = value + 'px';
        } else {
          switch (key) {
            case 'transition':
              convertToVendor(key, $mdConstant.CSS.TRANSITION, value);
              break;
            case 'transform':
              convertToVendor(key, $mdConstant.CSS.TRANSFORM, value);
              break;
            case 'transformOrigin':
              convertToVendor(key, $mdConstant.CSS.TRANSFORM_ORIGIN, value);
              break;
            case 'font-size':
              css['font-size'] = value; // font sizes aren't always in px
              break;
          }
        }
      });

      return css;

      function convertToVendor(key, vendor, value) {
        angular.forEach(vendor.split(' '), function (key) {
          css[key] = value;
        });
      }
    },

    /**
     * Convert the translate CSS value to key/value pair(s).
     */
    toTransformCss: function (transform, addTransition, transition) {
      var css = {};
      angular.forEach($mdConstant.CSS.TRANSFORM.split(' '), function (key) {
        css[key] = transform;
      });

      if (addTransition) {
        transition = transition || "all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1) !important";
        css.transition = transition;
      }

      return css;
    },

    /**
     *  Clone the Rect and calculate the height/width if needed
     */
    copyRect: function (source, destination) {
      if (!source) return null;

      destination = destination || {};

      angular.forEach('left top right bottom width height'.split(' '), function (key) {
        destination[key] = Math.round(source[key]);
      });

      destination.width = destination.width || (destination.right - destination.left);
      destination.height = destination.height || (destination.bottom - destination.top);

      return destination;
    },

    /**
     * Calculate ClientRect of element; return null if hidden or zero size
     */
    clientRect: function (element) {
      var bounds = angular.element(element)[0].getBoundingClientRect();
      var isPositiveSizeClientRect = function (rect) {
        return rect && (rect.width > 0) && (rect.height > 0);
      };

      // If the event origin element has zero size, it has probably been hidden.
      return isPositiveSizeClientRect(bounds) ? self.copyRect(bounds) : null;
    },

    /**
     *  Calculate 'rounded' center point of Rect
     */
    centerPointFor: function (targetRect) {
      return targetRect ? {
        x: Math.round(targetRect.left + (targetRect.width / 2)),
        y: Math.round(targetRect.top + (targetRect.height / 2))
      } : { x : 0, y : 0 };
    }

  };
}


if (angular.version.minor >= 4) {
  angular.module('material.core.animate', []);
} else {
(function() {
  "use strict";

  var forEach = angular.forEach;

  var WEBKIT = angular.isDefined(document.documentElement.style.WebkitAppearance);
  var TRANSITION_PROP = WEBKIT ? 'WebkitTransition' : 'transition';
  var ANIMATION_PROP = WEBKIT ? 'WebkitAnimation' : 'animation';
  var PREFIX = WEBKIT ? '-webkit-' : '';

  var TRANSITION_EVENTS = (WEBKIT ? 'webkitTransitionEnd ' : '') + 'transitionend';
  var ANIMATION_EVENTS = (WEBKIT ? 'webkitAnimationEnd ' : '') + 'animationend';

  var $$ForceReflowFactory = ['$document', function($document) {
    return function() {
      return $document[0].body.clientWidth + 1;
    };
  }];

  var $$rAFMutexFactory = ['$$rAF', function($$rAF) {
    return function() {
      var passed = false;
      $$rAF(function() {
        passed = true;
      });
      return function(fn) {
        passed ? fn() : $$rAF(fn);
      };
    };
  }];

  var $$AnimateRunnerFactory = ['$q', '$$rAFMutex', function($q, $$rAFMutex) {
    var INITIAL_STATE = 0;
    var DONE_PENDING_STATE = 1;
    var DONE_COMPLETE_STATE = 2;

    function AnimateRunner(host) {
      this.setHost(host);

      this._doneCallbacks = [];
      this._runInAnimationFrame = $$rAFMutex();
      this._state = 0;
    }

    AnimateRunner.prototype = {
      setHost: function(host) {
        this.host = host || {};
      },

      done: function(fn) {
        if (this._state === DONE_COMPLETE_STATE) {
          fn();
        } else {
          this._doneCallbacks.push(fn);
        }
      },

      progress: angular.noop,

      getPromise: function() {
        if (!this.promise) {
          var self = this;
          this.promise = $q(function(resolve, reject) {
            self.done(function(status) {
              status === false ? reject() : resolve();
            });
          });
        }
        return this.promise;
      },

      then: function(resolveHandler, rejectHandler) {
        return this.getPromise().then(resolveHandler, rejectHandler);
      },

      'catch': function(handler) {
        return this.getPromise()['catch'](handler);
      },

      'finally': function(handler) {
        return this.getPromise()['finally'](handler);
      },

      pause: function() {
        if (this.host.pause) {
          this.host.pause();
        }
      },

      resume: function() {
        if (this.host.resume) {
          this.host.resume();
        }
      },

      end: function() {
        if (this.host.end) {
          this.host.end();
        }
        this._resolve(true);
      },

      cancel: function() {
        if (this.host.cancel) {
          this.host.cancel();
        }
        this._resolve(false);
      },

      complete: function(response) {
        var self = this;
        if (self._state === INITIAL_STATE) {
          self._state = DONE_PENDING_STATE;
          self._runInAnimationFrame(function() {
            self._resolve(response);
          });
        }
      },

      _resolve: function(response) {
        if (this._state !== DONE_COMPLETE_STATE) {
          forEach(this._doneCallbacks, function(fn) {
            fn(response);
          });
          this._doneCallbacks.length = 0;
          this._state = DONE_COMPLETE_STATE;
        }
      }
    };

    // Polyfill AnimateRunner.all which is used by input animations
    AnimateRunner.all = function(runners, callback) {
      var count = 0;
      var status = true;
      forEach(runners, function(runner) {
        runner.done(onProgress);
      });

      function onProgress(response) {
        status = status && response;
        if (++count === runners.length) {
          callback(status);
        }
      }
    };

    return AnimateRunner;
  }];

  angular
    .module('material.core.animate', [])
    .factory('$$forceReflow', $$ForceReflowFactory)
    .factory('$$AnimateRunner', $$AnimateRunnerFactory)
    .factory('$$rAFMutex', $$rAFMutexFactory)
    .factory('$animateCss', ['$window', '$$rAF', '$$AnimateRunner', '$$forceReflow', '$$jqLite', '$timeout', '$animate',
                     function($window,   $$rAF,   $$AnimateRunner,   $$forceReflow,   $$jqLite,   $timeout, $animate) {

      function init(element, options) {

        var temporaryStyles = [];
        var node = getDomNode(element);
        var areAnimationsAllowed = node && $animate.enabled();

        var hasCompleteStyles = false;
        var hasCompleteClasses = false;

        if (areAnimationsAllowed) {
          if (options.transitionStyle) {
            temporaryStyles.push([PREFIX + 'transition', options.transitionStyle]);
          }

          if (options.keyframeStyle) {
            temporaryStyles.push([PREFIX + 'animation', options.keyframeStyle]);
          }

          if (options.delay) {
            temporaryStyles.push([PREFIX + 'transition-delay', options.delay + 's']);
          }

          if (options.duration) {
            temporaryStyles.push([PREFIX + 'transition-duration', options.duration + 's']);
          }

          hasCompleteStyles = options.keyframeStyle ||
              (options.to && (options.duration > 0 || options.transitionStyle));
          hasCompleteClasses = !!options.addClass || !!options.removeClass;

          blockTransition(element, true);
        }

        var hasCompleteAnimation = areAnimationsAllowed && (hasCompleteStyles || hasCompleteClasses);

        applyAnimationFromStyles(element, options);

        var animationClosed = false;
        var events, eventFn;

        return {
          close: $window.close,
          start: function() {
            var runner = new $$AnimateRunner();
            waitUntilQuiet(function() {
              blockTransition(element, false);
              if (!hasCompleteAnimation) {
                return close();
              }

              forEach(temporaryStyles, function(entry) {
                var key = entry[0];
                var value = entry[1];
                node.style[camelCase(key)] = value;
              });

              applyClasses(element, options);

              var timings = computeTimings(element);
              if (timings.duration === 0) {
                return close();
              }

              var moreStyles = [];

              if (options.easing) {
                if (timings.transitionDuration) {
                  moreStyles.push([PREFIX + 'transition-timing-function', options.easing]);
                }
                if (timings.animationDuration) {
                  moreStyles.push([PREFIX + 'animation-timing-function', options.easing]);
                }
              }

              if (options.delay && timings.animationDelay) {
                moreStyles.push([PREFIX + 'animation-delay', options.delay + 's']);
              }

              if (options.duration && timings.animationDuration) {
                moreStyles.push([PREFIX + 'animation-duration', options.duration + 's']);
              }

              forEach(moreStyles, function(entry) {
                var key = entry[0];
                var value = entry[1];
                node.style[camelCase(key)] = value;
                temporaryStyles.push(entry);
              });

              var maxDelay = timings.delay;
              var maxDelayTime = maxDelay * 1000;
              var maxDuration = timings.duration;
              var maxDurationTime = maxDuration * 1000;
              var startTime = Date.now();

              events = [];
              if (timings.transitionDuration) {
                events.push(TRANSITION_EVENTS);
              }
              if (timings.animationDuration) {
                events.push(ANIMATION_EVENTS);
              }
              events = events.join(' ');
              eventFn = function(event) {
                event.stopPropagation();
                var ev = event.originalEvent || event;
                var timeStamp = ev.timeStamp || Date.now();
                var elapsedTime = parseFloat(ev.elapsedTime.toFixed(3));
                if (Math.max(timeStamp - startTime, 0) >= maxDelayTime && elapsedTime >= maxDuration) {
                  close();
                }
              };
              element.on(events, eventFn);

              applyAnimationToStyles(element, options);

              $timeout(close, maxDelayTime + maxDurationTime * 1.5, false);
            });

            return runner;

            function close() {
              if (animationClosed) return;
              animationClosed = true;

              if (events && eventFn) {
                element.off(events, eventFn);
              }
              applyClasses(element, options);
              applyAnimationStyles(element, options);
              forEach(temporaryStyles, function(entry) {
                node.style[camelCase(entry[0])] = '';
              });
              runner.complete(true);
              return runner;
            }
          }
        };
      }

      function applyClasses(element, options) {
        if (options.addClass) {
          $$jqLite.addClass(element, options.addClass);
          options.addClass = null;
        }
        if (options.removeClass) {
          $$jqLite.removeClass(element, options.removeClass);
          options.removeClass = null;
        }
      }

      function computeTimings(element) {
        var node = getDomNode(element);
        var cs = $window.getComputedStyle(node);
        var tdr = parseMaxTime(cs[prop('transitionDuration')]);
        var adr = parseMaxTime(cs[prop('animationDuration')]);
        var tdy = parseMaxTime(cs[prop('transitionDelay')]);
        var ady = parseMaxTime(cs[prop('animationDelay')]);

        adr *= (parseInt(cs[prop('animationIterationCount')], 10) || 1);
        var duration = Math.max(adr, tdr);
        var delay = Math.max(ady, tdy);

        return {
          duration: duration,
          delay: delay,
          animationDuration: adr,
          transitionDuration: tdr,
          animationDelay: ady,
          transitionDelay: tdy
        };

        function prop(key) {
          return WEBKIT ? 'Webkit' + key.charAt(0).toUpperCase() + key.substr(1)
                        : key;
        }
      }

      function parseMaxTime(str) {
        var maxValue = 0;
        var values = (str || "").split(/\s*,\s*/);
        forEach(values, function(value) {
          // it's always safe to consider only second values and omit `ms` values since
          // getComputedStyle will always handle the conversion for us
          if (value.charAt(value.length - 1) == 's') {
            value = value.substring(0, value.length - 1);
          }
          value = parseFloat(value) || 0;
          maxValue = maxValue ? Math.max(value, maxValue) : value;
        });
        return maxValue;
      }

      var cancelLastRAFRequest;
      var rafWaitQueue = [];
      function waitUntilQuiet(callback) {
        if (cancelLastRAFRequest) {
          cancelLastRAFRequest(); // cancels the request
        }
        rafWaitQueue.push(callback);
        cancelLastRAFRequest = $$rAF(function() {
          cancelLastRAFRequest = null;

          // DO NOT REMOVE THIS LINE OR REFACTOR OUT THE `pageWidth` variable.
          // PLEASE EXAMINE THE `$$forceReflow` service to understand why.
          var pageWidth = $$forceReflow();

          // we use a for loop to ensure that if the queue is changed
          // during this looping then it will consider new requests
          for (var i = 0; i < rafWaitQueue.length; i++) {
            rafWaitQueue[i](pageWidth);
          }
          rafWaitQueue.length = 0;
        });
      }

      function applyAnimationStyles(element, options) {
        applyAnimationFromStyles(element, options);
        applyAnimationToStyles(element, options);
      }

      function applyAnimationFromStyles(element, options) {
        if (options.from) {
          element.css(options.from);
          options.from = null;
        }
      }

      function applyAnimationToStyles(element, options) {
        if (options.to) {
          element.css(options.to);
          options.to = null;
        }
      }

      function getDomNode(element) {
        for (var i = 0; i < element.length; i++) {
          if (element[i].nodeType === 1) return element[i];
        }
      }

      function blockTransition(element, bool) {
        var node = getDomNode(element);
        var key = camelCase(PREFIX + 'transition-delay');
        node.style[key] = bool ? '-9999s' : '';
      }

      return init;
    }]);

  /**
   * Older browsers [FF31] expect camelCase
   * property keys.
   * e.g.
   *  animation-duration --> animationDuration
   */
  function camelCase(str) {
    return str.replace(/-[a-z]/g, function(str) {
      return str.charAt(1).toUpperCase();
    });
  }

})();

}

(function(){ 
angular.module("material.core").constant("$MD_THEME_CSS", "md-autocomplete.md-THEME_NAME-theme{background:\"{{background-hue-1}}\"}md-autocomplete.md-THEME_NAME-theme[disabled]:not([md-floating-label]){background:\"{{background-hue-2}}\"}md-autocomplete.md-THEME_NAME-theme button md-icon path{fill:\"{{background-600}}\"}md-autocomplete.md-THEME_NAME-theme button:after{background:\"{{background-600-0.3}}\"}md-autocomplete.md-THEME_NAME-theme input{color:\"{{foreground-1}}\"}md-autocomplete.md-THEME_NAME-theme.md-accent md-input-container.md-input-focused .md-input{border-color:\"{{accent-color}}\"}md-autocomplete.md-THEME_NAME-theme.md-accent md-input-container.md-input-focused label,md-autocomplete.md-THEME_NAME-theme.md-accent md-input-container.md-input-focused md-icon{color:\"{{accent-color}}\"}md-autocomplete.md-THEME_NAME-theme.md-accent md-progress-linear .md-container{background-color:\"{{accent-100}}\"}md-autocomplete.md-THEME_NAME-theme.md-accent md-progress-linear .md-bar{background-color:\"{{accent-color}}\"}md-autocomplete.md-THEME_NAME-theme.md-warn md-input-container.md-input-focused .md-input{border-color:\"{{warn-A700}}\"}md-autocomplete.md-THEME_NAME-theme.md-warn md-input-container.md-input-focused label,md-autocomplete.md-THEME_NAME-theme.md-warn md-input-container.md-input-focused md-icon{color:\"{{warn-A700}}\"}md-autocomplete.md-THEME_NAME-theme.md-warn md-progress-linear .md-container{background-color:\"{{warn-100}}\"}md-autocomplete.md-THEME_NAME-theme.md-warn md-progress-linear .md-bar{background-color:\"{{warn-color}}\"}.md-autocomplete-standard-list-container.md-THEME_NAME-theme,.md-autocomplete-suggestions-container.md-THEME_NAME-theme{background:\"{{background-hue-1}}\"}.md-autocomplete-standard-list-container.md-THEME_NAME-theme li,.md-autocomplete-suggestions-container.md-THEME_NAME-theme li{color:\"{{foreground-1}}\"}.md-autocomplete-standard-list-container.md-THEME_NAME-theme li#selected_option,.md-autocomplete-standard-list-container.md-THEME_NAME-theme li:hover,.md-autocomplete-suggestions-container.md-THEME_NAME-theme li#selected_option,.md-autocomplete-suggestions-container.md-THEME_NAME-theme li:hover{background:\"{{background-500-0.18}}\"}md-backdrop{background-color:\"{{background-900-0.0}}\"}md-backdrop.md-opaque.md-THEME_NAME-theme{background-color:\"{{background-900-1.0}}\"}md-bottom-sheet.md-THEME_NAME-theme{background-color:\"{{background-color}}\";border-top-color:\"{{background-hue-3}}\"}md-bottom-sheet.md-THEME_NAME-theme.md-list md-list-item{color:\"{{foreground-1}}\"}md-bottom-sheet.md-THEME_NAME-theme .md-subheader{background-color:\"{{background-color}}\";color:\"{{foreground-1}}\"}.md-button.md-THEME_NAME-theme:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme:not([disabled]):hover{background-color:\"{{background-500-0.2}}\"}.md-button.md-THEME_NAME-theme:not([disabled]).md-icon-button:hover{background-color:transparent}.md-button.md-THEME_NAME-theme.md-fab md-icon{color:\"{{accent-contrast}}\"}.md-button.md-THEME_NAME-theme.md-primary{color:\"{{primary-color}}\"}.md-button.md-THEME_NAME-theme.md-primary.md-fab,.md-button.md-THEME_NAME-theme.md-primary.md-raised{color:\"{{primary-contrast}}\";background-color:\"{{primary-color}}\"}.md-button.md-THEME_NAME-theme.md-primary.md-fab:not([disabled]) md-icon,.md-button.md-THEME_NAME-theme.md-primary.md-raised:not([disabled]) md-icon{color:\"{{primary-contrast}}\"}.md-button.md-THEME_NAME-theme.md-primary.md-fab:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-primary.md-fab:not([disabled]):hover,.md-button.md-THEME_NAME-theme.md-primary.md-raised:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-primary.md-raised:not([disabled]):hover{background-color:\"{{primary-600}}\"}.md-button.md-THEME_NAME-theme.md-primary:not([disabled]) md-icon{color:\"{{primary-color}}\"}.md-button.md-THEME_NAME-theme.md-fab{background-color:\"{{accent-color}}\";color:\"{{accent-contrast}}\"}.md-button.md-THEME_NAME-theme.md-fab:not([disabled]) .md-icon{color:\"{{accent-contrast}}\"}.md-button.md-THEME_NAME-theme.md-fab:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-fab:not([disabled]):hover{background-color:\"{{accent-A700}}\"}.md-button.md-THEME_NAME-theme.md-raised{color:\"{{background-900}}\";background-color:\"{{background-50}}\"}.md-button.md-THEME_NAME-theme.md-raised:not([disabled]) md-icon{color:\"{{background-900}}\"}.md-button.md-THEME_NAME-theme.md-raised:not([disabled]):hover{background-color:\"{{background-50}}\"}.md-button.md-THEME_NAME-theme.md-raised:not([disabled]).md-focused{background-color:\"{{background-200}}\"}.md-button.md-THEME_NAME-theme.md-warn{color:\"{{warn-color}}\"}.md-button.md-THEME_NAME-theme.md-warn.md-fab,.md-button.md-THEME_NAME-theme.md-warn.md-raised{color:\"{{warn-contrast}}\";background-color:\"{{warn-color}}\"}.md-button.md-THEME_NAME-theme.md-warn.md-fab:not([disabled]) md-icon,.md-button.md-THEME_NAME-theme.md-warn.md-raised:not([disabled]) md-icon{color:\"{{warn-contrast}}\"}.md-button.md-THEME_NAME-theme.md-warn.md-fab:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-warn.md-fab:not([disabled]):hover,.md-button.md-THEME_NAME-theme.md-warn.md-raised:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-warn.md-raised:not([disabled]):hover{background-color:\"{{warn-600}}\"}.md-button.md-THEME_NAME-theme.md-warn:not([disabled]) md-icon{color:\"{{warn-color}}\"}.md-button.md-THEME_NAME-theme.md-accent{color:\"{{accent-color}}\"}.md-button.md-THEME_NAME-theme.md-accent.md-fab,.md-button.md-THEME_NAME-theme.md-accent.md-raised{color:\"{{accent-contrast}}\";background-color:\"{{accent-color}}\"}.md-button.md-THEME_NAME-theme.md-accent.md-fab:not([disabled]) md-icon,.md-button.md-THEME_NAME-theme.md-accent.md-raised:not([disabled]) md-icon{color:\"{{accent-contrast}}\"}.md-button.md-THEME_NAME-theme.md-accent.md-fab:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-accent.md-fab:not([disabled]):hover,.md-button.md-THEME_NAME-theme.md-accent.md-raised:not([disabled]).md-focused,.md-button.md-THEME_NAME-theme.md-accent.md-raised:not([disabled]):hover{background-color:\"{{accent-A700}}\"}.md-button.md-THEME_NAME-theme.md-accent:not([disabled]) md-icon{color:\"{{accent-color}}\"}.md-button.md-THEME_NAME-theme.md-accent[disabled],.md-button.md-THEME_NAME-theme.md-fab[disabled],.md-button.md-THEME_NAME-theme.md-raised[disabled],.md-button.md-THEME_NAME-theme.md-warn[disabled],.md-button.md-THEME_NAME-theme[disabled]{color:\"{{foreground-3}}\";cursor:default}.md-button.md-THEME_NAME-theme.md-accent[disabled] md-icon,.md-button.md-THEME_NAME-theme.md-fab[disabled] md-icon,.md-button.md-THEME_NAME-theme.md-raised[disabled] md-icon,.md-button.md-THEME_NAME-theme.md-warn[disabled] md-icon,.md-button.md-THEME_NAME-theme[disabled] md-icon{color:\"{{foreground-3}}\"}.md-button.md-THEME_NAME-theme.md-fab[disabled],.md-button.md-THEME_NAME-theme.md-raised[disabled]{background-color:\"{{foreground-4}}\"}.md-button.md-THEME_NAME-theme[disabled]{background-color:transparent}._md a.md-THEME_NAME-theme:not(.md-button).md-primary{color:\"{{primary-color}}\"}._md a.md-THEME_NAME-theme:not(.md-button).md-primary:hover{color:\"{{primary-700}}\"}._md a.md-THEME_NAME-theme:not(.md-button).md-accent{color:\"{{accent-color}}\"}._md a.md-THEME_NAME-theme:not(.md-button).md-accent:hover{color:\"{{accent-A700}}\"}._md a.md-THEME_NAME-theme:not(.md-button).md-warn{color:\"{{warn-color}}\"}._md a.md-THEME_NAME-theme:not(.md-button).md-warn:hover{color:\"{{warn-700}}\"}md-card.md-THEME_NAME-theme{color:\"{{foreground-1}}\";background-color:\"{{background-hue-1}}\";border-radius:2px}md-card.md-THEME_NAME-theme .md-card-image{border-radius:2px 2px 0 0}md-card.md-THEME_NAME-theme md-card-header md-card-avatar md-icon{color:\"{{background-color}}\";background-color:\"{{foreground-3}}\"}md-card.md-THEME_NAME-theme md-card-header md-card-header-text .md-subhead,md-card.md-THEME_NAME-theme md-card-title md-card-title-text:not(:only-child) .md-subhead{color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme .md-ripple{color:\"{{accent-A700}}\"}md-checkbox.md-THEME_NAME-theme.md-checked .md-ripple{color:\"{{background-600}}\"}md-checkbox.md-THEME_NAME-theme.md-checked.md-focused .md-container:before{background-color:\"{{accent-color-0.26}}\"}md-checkbox.md-THEME_NAME-theme .md-ink-ripple{color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme.md-checked .md-ink-ripple{color:\"{{accent-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not(.md-checked) .md-icon{border-color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme.md-checked .md-icon{background-color:\"{{accent-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme.md-checked .md-icon:after{border-color:\"{{accent-contrast-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary .md-ripple{color:\"{{primary-600}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-ripple{color:\"{{background-600}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary .md-ink-ripple{color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-ink-ripple{color:\"{{primary-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary:not(.md-checked) .md-icon{border-color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-icon{background-color:\"{{primary-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked.md-focused .md-container:before{background-color:\"{{primary-color-0.26}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-icon:after{border-color:\"{{primary-contrast-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-primary .md-indeterminate[disabled] .md-container{color:\"{{foreground-3}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn .md-ripple{color:\"{{warn-600}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn .md-ink-ripple{color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-ink-ripple{color:\"{{warn-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn:not(.md-checked) .md-icon{border-color:\"{{foreground-2}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-icon{background-color:\"{{warn-color-0.87}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked.md-focused:not([disabled]) .md-container:before{background-color:\"{{warn-color-0.26}}\"}md-checkbox.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-icon:after{border-color:\"{{background-200}}\"}md-checkbox.md-THEME_NAME-theme[disabled]:not(.md-checked) .md-icon{border-color:\"{{foreground-3}}\"}md-checkbox.md-THEME_NAME-theme[disabled].md-checked .md-icon{background-color:\"{{foreground-3}}\"}md-checkbox.md-THEME_NAME-theme[disabled].md-checked .md-icon:after{border-color:\"{{background-200}}\"}md-checkbox.md-THEME_NAME-theme[disabled] .md-icon:after{border-color:\"{{foreground-3}}\"}md-checkbox.md-THEME_NAME-theme[disabled] .md-label{color:\"{{foreground-3}}\"}md-chips.md-THEME_NAME-theme .md-chips{box-shadow:0 1px \"{{foreground-4}}\"}md-chips.md-THEME_NAME-theme .md-chips.md-focused{box-shadow:0 2px \"{{primary-color}}\"}md-chips.md-THEME_NAME-theme .md-chips .md-chip-input-container input{color:\"{{foreground-1}}\"}md-chips.md-THEME_NAME-theme .md-chips .md-chip-input-container input:-moz-placeholder,md-chips.md-THEME_NAME-theme .md-chips .md-chip-input-container input::-moz-placeholder{color:\"{{foreground-3}}\"}md-chips.md-THEME_NAME-theme .md-chips .md-chip-input-container input:-ms-input-placeholder{color:\"{{foreground-3}}\"}md-chips.md-THEME_NAME-theme .md-chips .md-chip-input-container input::-webkit-input-placeholder{color:\"{{foreground-3}}\"}md-chips.md-THEME_NAME-theme md-chip{background:\"{{background-300}}\";color:\"{{background-800}}\"}md-chips.md-THEME_NAME-theme md-chip md-icon{color:\"{{background-700}}\"}md-chips.md-THEME_NAME-theme md-chip.md-focused{background:\"{{primary-color}}\";color:\"{{primary-contrast}}\"}md-chips.md-THEME_NAME-theme md-chip.md-focused md-icon{color:\"{{primary-contrast}}\"}md-chips.md-THEME_NAME-theme md-chip._md-chip-editing{background:transparent;color:\"{{background-800}}\"}md-chips.md-THEME_NAME-theme md-chip-remove .md-button md-icon path{fill:\"{{background-500}}\"}.md-contact-suggestion span.md-contact-email{color:\"{{background-400}}\"}md-content.md-THEME_NAME-theme{color:\"{{foreground-1}}\";background-color:\"{{background-default}}\"}.md-THEME_NAME-theme .md-calendar{background:\"{{background-hue-1}}\";color:\"{{foreground-1-0.87}}\"}.md-THEME_NAME-theme .md-calendar tr:last-child td{border-bottom-color:\"{{background-hue-2}}\"}.md-THEME_NAME-theme .md-calendar-day-header{background:\"{{background-500-0.32}}\";color:\"{{foreground-1-0.87}}\"}.md-THEME_NAME-theme .md-calendar-date.md-calendar-date-today .md-calendar-date-selection-indicator{border:1px solid \"{{primary-500}}\"}.md-THEME_NAME-theme .md-calendar-date.md-calendar-date-today.md-calendar-date-disabled{color:\"{{primary-500-0.6}}\"}.md-calendar-date.md-focus .md-THEME_NAME-theme .md-calendar-date-selection-indicator,.md-THEME_NAME-theme .md-calendar-date-selection-indicator:hover{background:\"{{background-500-0.32}}\"}.md-THEME_NAME-theme .md-calendar-date.md-calendar-selected-date .md-calendar-date-selection-indicator,.md-THEME_NAME-theme .md-calendar-date.md-focus.md-calendar-selected-date .md-calendar-date-selection-indicator{background:\"{{primary-500}}\";color:\"{{primary-500-contrast}}\";border-color:transparent}.md-THEME_NAME-theme .md-calendar-date-disabled,.md-THEME_NAME-theme .md-calendar-month-label-disabled{color:\"{{foreground-3}}\"}.md-THEME_NAME-theme .md-calendar-month-label md-icon,.md-THEME_NAME-theme .md-datepicker-input{color:\"{{foreground-1}}\"}.md-THEME_NAME-theme .md-datepicker-input:-moz-placeholder,.md-THEME_NAME-theme .md-datepicker-input::-moz-placeholder{color:\"{{foreground-3}}\"}.md-THEME_NAME-theme .md-datepicker-input:-ms-input-placeholder{color:\"{{foreground-3}}\"}.md-THEME_NAME-theme .md-datepicker-input::-webkit-input-placeholder{color:\"{{foreground-3}}\"}.md-THEME_NAME-theme .md-datepicker-input-container{border-bottom-color:\"{{foreground-4}}\"}.md-THEME_NAME-theme .md-datepicker-input-container.md-datepicker-focused{border-bottom-color:\"{{primary-color}}\"}.md-accent .md-THEME_NAME-theme .md-datepicker-input-container.md-datepicker-focused{border-bottom-color:\"{{accent-color}}\"}.md-THEME_NAME-theme .md-datepicker-input-container.md-datepicker-invalid,.md-warn .md-THEME_NAME-theme .md-datepicker-input-container.md-datepicker-focused{border-bottom-color:\"{{warn-A700}}\"}.md-THEME_NAME-theme .md-datepicker-calendar-pane{border-color:\"{{background-hue-1}}\"}.md-THEME_NAME-theme .md-datepicker-triangle-button .md-datepicker-expand-triangle{border-top-color:\"{{foreground-2}}\"}.md-THEME_NAME-theme .md-datepicker-open .md-datepicker-calendar-icon{color:\"{{primary-color}}\"}.md-accent .md-THEME_NAME-theme .md-datepicker-open .md-datepicker-calendar-icon,.md-THEME_NAME-theme .md-datepicker-open.md-accent .md-datepicker-calendar-icon{color:\"{{accent-color}}\"}.md-THEME_NAME-theme .md-datepicker-open.md-warn .md-datepicker-calendar-icon,.md-warn .md-THEME_NAME-theme .md-datepicker-open .md-datepicker-calendar-icon{color:\"{{warn-A700}}\"}.md-THEME_NAME-theme .md-datepicker-calendar{background:\"{{background-hue-1}}\"}.md-THEME_NAME-theme .md-datepicker-input-mask-opaque{box-shadow:0 0 0 9999px \"{{background-hue-1}}\"}.md-THEME_NAME-theme .md-datepicker-open .md-datepicker-input-container{background:\"{{background-hue-1}}\"}md-dialog.md-THEME_NAME-theme{border-radius:4px;background-color:\"{{background-hue-1}}\";color:\"{{foreground-1}}\"}md-dialog.md-THEME_NAME-theme.md-content-overflow .md-actions,md-dialog.md-THEME_NAME-theme.md-content-overflow md-dialog-actions,md-divider.md-THEME_NAME-theme{border-top-color:\"{{foreground-4}}\"}.layout-gt-lg-row>md-divider.md-THEME_NAME-theme,.layout-gt-md-row>md-divider.md-THEME_NAME-theme,.layout-gt-sm-row>md-divider.md-THEME_NAME-theme,.layout-gt-xs-row>md-divider.md-THEME_NAME-theme,.layout-lg-row>md-divider.md-THEME_NAME-theme,.layout-md-row>md-divider.md-THEME_NAME-theme,.layout-row>md-divider.md-THEME_NAME-theme,.layout-sm-row>md-divider.md-THEME_NAME-theme,.layout-xl-row>md-divider.md-THEME_NAME-theme,.layout-xs-row>md-divider.md-THEME_NAME-theme{border-right-color:\"{{foreground-4}}\"}md-icon.md-THEME_NAME-theme{color:\"{{foreground-2}}\"}md-icon.md-THEME_NAME-theme.md-primary{color:\"{{primary-color}}\"}md-icon.md-THEME_NAME-theme.md-accent{color:\"{{accent-color}}\"}md-icon.md-THEME_NAME-theme.md-warn{color:\"{{warn-color}}\"}md-input-container.md-THEME_NAME-theme .md-input{color:\"{{foreground-1}}\";border-color:\"{{foreground-4}}\"}md-input-container.md-THEME_NAME-theme .md-input:-moz-placeholder,md-input-container.md-THEME_NAME-theme .md-input::-moz-placeholder{color:\"{{foreground-3}}\"}md-input-container.md-THEME_NAME-theme .md-input:-ms-input-placeholder{color:\"{{foreground-3}}\"}md-input-container.md-THEME_NAME-theme .md-input::-webkit-input-placeholder{color:\"{{foreground-3}}\"}md-input-container.md-THEME_NAME-theme>md-icon{color:\"{{foreground-1}}\"}md-input-container.md-THEME_NAME-theme .md-placeholder,md-input-container.md-THEME_NAME-theme label{color:\"{{foreground-3}}\"}md-input-container.md-THEME_NAME-theme label.md-required:after{color:\"{{warn-A700}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-focused):not(.md-input-invalid) label.md-required:after{color:\"{{foreground-2}}\"}md-input-container.md-THEME_NAME-theme .md-input-message-animation,md-input-container.md-THEME_NAME-theme .md-input-messages-animation{color:\"{{warn-A700}}\"}md-input-container.md-THEME_NAME-theme .md-input-message-animation .md-char-counter,md-input-container.md-THEME_NAME-theme .md-input-messages-animation .md-char-counter{color:\"{{foreground-1}}\"}md-input-container.md-THEME_NAME-theme.md-input-focused .md-input:-moz-placeholder,md-input-container.md-THEME_NAME-theme.md-input-focused .md-input::-moz-placeholder{color:\"{{foreground-2}}\"}md-input-container.md-THEME_NAME-theme.md-input-focused .md-input:-ms-input-placeholder{color:\"{{foreground-2}}\"}md-input-container.md-THEME_NAME-theme.md-input-focused .md-input::-webkit-input-placeholder{color:\"{{foreground-2}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-has-value label{color:\"{{foreground-2}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused .md-input,md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-resized .md-input{border-color:\"{{primary-color}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused label,md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused md-icon{color:\"{{primary-color}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-accent .md-input{border-color:\"{{accent-color}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-accent label,md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-accent md-icon{color:\"{{accent-color}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-warn .md-input{border-color:\"{{warn-A700}}\"}md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-warn label,md-input-container.md-THEME_NAME-theme:not(.md-input-invalid).md-input-focused.md-warn md-icon{color:\"{{warn-A700}}\"}md-input-container.md-THEME_NAME-theme.md-input-invalid .md-input{border-color:\"{{warn-A700}}\"}md-input-container.md-THEME_NAME-theme.md-input-invalid .md-char-counter,md-input-container.md-THEME_NAME-theme.md-input-invalid .md-input-message-animation,md-input-container.md-THEME_NAME-theme.md-input-invalid label{color:\"{{warn-A700}}\"}[disabled] md-input-container.md-THEME_NAME-theme .md-input,md-input-container.md-THEME_NAME-theme .md-input[disabled]{border-bottom-color:transparent;color:\"{{foreground-3}}\";background-image:linear-gradient(90deg,\"{{foreground-3}}\" 0,\"{{foreground-3}}\" 33%,transparent 0);background-image:-ms-linear-gradient(left,transparent 0,\"{{foreground-3}}\" 100%)}md-list.md-THEME_NAME-theme md-list-item.md-2-line .md-list-item-text h3,md-list.md-THEME_NAME-theme md-list-item.md-2-line .md-list-item-text h4,md-list.md-THEME_NAME-theme md-list-item.md-3-line .md-list-item-text h3,md-list.md-THEME_NAME-theme md-list-item.md-3-line .md-list-item-text h4{color:\"{{foreground-1}}\"}md-list.md-THEME_NAME-theme md-list-item.md-2-line .md-list-item-text p,md-list.md-THEME_NAME-theme md-list-item.md-3-line .md-list-item-text p{color:\"{{foreground-2}}\"}md-list.md-THEME_NAME-theme .md-proxy-focus.md-focused div.md-no-style{background-color:\"{{background-100}}\"}md-list.md-THEME_NAME-theme md-list-item .md-avatar-icon{background-color:\"{{foreground-3}}\";color:\"{{background-color}}\"}md-list.md-THEME_NAME-theme md-list-item>md-icon{color:\"{{foreground-2}}\"}md-list.md-THEME_NAME-theme md-list-item>md-icon.md-highlight{color:\"{{primary-color}}\"}md-list.md-THEME_NAME-theme md-list-item>md-icon.md-highlight.md-accent{color:\"{{accent-color}}\"}md-menu-content.md-THEME_NAME-theme{background-color:\"{{background-hue-1}}\"}md-menu-content.md-THEME_NAME-theme md-menu-item{color:\"{{foreground-1}}\"}md-menu-content.md-THEME_NAME-theme md-menu-item md-icon{color:\"{{foreground-2}}\"}md-menu-content.md-THEME_NAME-theme md-menu-item .md-button[disabled],md-menu-content.md-THEME_NAME-theme md-menu-item .md-button[disabled] md-icon{color:\"{{foreground-3}}\"}md-menu-content.md-THEME_NAME-theme md-menu-divider{background-color:\"{{foreground-4}}\"}md-menu-bar.md-THEME_NAME-theme>button.md-button{color:\"{{foreground-1}}\";border-radius:2px}md-menu-bar.md-THEME_NAME-theme md-menu>button{color:\"{{foreground-1}}\"}md-menu-bar.md-THEME_NAME-theme md-menu.md-open>button,md-menu-bar.md-THEME_NAME-theme md-menu>button:focus{outline:none;background-color:\"{{ background-500-0.18}}\"}md-menu-bar.md-THEME_NAME-theme.md-open:not(.md-keyboard-mode) md-menu:hover>button{background-color:\"{{ background-500-0.18}}\"}md-menu-bar.md-THEME_NAME-theme:not(.md-keyboard-mode):not(.md-open) md-menu button:focus,md-menu-bar.md-THEME_NAME-theme:not(.md-keyboard-mode):not(.md-open) md-menu button:hover{background:transparent}md-menu-content.md-THEME_NAME-theme .md-menu>.md-button:after{color:\"{{foreground-2}}\"}md-menu-content.md-THEME_NAME-theme .md-menu.md-open>.md-button{background-color:\"{{ background-500-0.18}}\"}md-toolbar.md-THEME_NAME-theme.md-menu-toolbar{background-color:\"{{background-hue-1}}\";color:\"{{foreground-1}}\"}md-toolbar.md-THEME_NAME-theme.md-menu-toolbar md-toolbar-filler{background-color:\"{{primary-color}}\";color:\"{{primary-contrast}}\"}md-toolbar.md-THEME_NAME-theme.md-menu-toolbar md-toolbar-filler md-icon{color:\"{{primary-contrast}}\"}md-nav-bar.md-THEME_NAME-theme .md-nav-bar{background-color:transparent;border-color:\"{{foreground-4}}\"}md-nav-bar.md-THEME_NAME-theme .md-button._md-nav-button.md-unselected{color:\"{{foreground-2}}\"}md-nav-bar.md-THEME_NAME-theme .md-button._md-nav-button[disabled]{color:\"{{foreground-3}}\"}md-nav-bar.md-THEME_NAME-theme md-nav-ink-bar{color:\"{{accent-color}}\";background:\"{{accent-color}}\"}md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar{background-color:\"{{accent-color}}\"}md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar .md-button._md-nav-button{color:\"{{accent-A100}}\"}md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar .md-button._md-nav-button.md-active,md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{accent-contrast}}\"}md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{accent-contrast-0.1}}\"}md-nav-bar.md-THEME_NAME-theme.md-accent>.md-nav-bar md-nav-ink-bar{color:\"{{primary-600-1}}\";background:\"{{primary-600-1}}\"}md-nav-bar.md-THEME_NAME-theme.md-warn>.md-nav-bar{background-color:\"{{warn-color}}\"}md-nav-bar.md-THEME_NAME-theme.md-warn>.md-nav-bar .md-button._md-nav-button{color:\"{{warn-100}}\"}md-nav-bar.md-THEME_NAME-theme.md-warn>.md-nav-bar .md-button._md-nav-button.md-active,md-nav-bar.md-THEME_NAME-theme.md-warn>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{warn-contrast}}\"}md-nav-bar.md-THEME_NAME-theme.md-warn>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{warn-contrast-0.1}}\"}md-nav-bar.md-THEME_NAME-theme.md-primary>.md-nav-bar{background-color:\"{{primary-color}}\"}md-nav-bar.md-THEME_NAME-theme.md-primary>.md-nav-bar .md-button._md-nav-button{color:\"{{primary-100}}\"}md-nav-bar.md-THEME_NAME-theme.md-primary>.md-nav-bar .md-button._md-nav-button.md-active,md-nav-bar.md-THEME_NAME-theme.md-primary>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{primary-contrast}}\"}md-nav-bar.md-THEME_NAME-theme.md-primary>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{primary-contrast-0.1}}\"}md-toolbar>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar{background-color:\"{{primary-color}}\"}md-toolbar>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button{color:\"{{primary-100}}\"}md-toolbar>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-active,md-toolbar>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{primary-contrast}}\"}md-toolbar>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{primary-contrast-0.1}}\"}md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar{background-color:\"{{accent-color}}\"}md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button{color:\"{{accent-A100}}\"}md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-active,md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{accent-contrast}}\"}md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{accent-contrast-0.1}}\"}md-toolbar.md-accent>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar md-nav-ink-bar{color:\"{{primary-600-1}}\";background:\"{{primary-600-1}}\"}md-toolbar.md-warn>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar{background-color:\"{{warn-color}}\"}md-toolbar.md-warn>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button{color:\"{{warn-100}}\"}md-toolbar.md-warn>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-active,md-toolbar.md-warn>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{color:\"{{warn-contrast}}\"}md-toolbar.md-warn>md-nav-bar.md-THEME_NAME-theme>.md-nav-bar .md-button._md-nav-button.md-focused{background:\"{{warn-contrast-0.1}}\"}._md-panel-backdrop.md-THEME_NAME-theme{background-color:\"{{background-900-1.0}}\"}md-progress-circular.md-THEME_NAME-theme path{stroke:\"{{primary-color}}\"}md-progress-circular.md-THEME_NAME-theme.md-warn path{stroke:\"{{warn-color}}\"}md-progress-circular.md-THEME_NAME-theme.md-accent path{stroke:\"{{accent-color}}\"}md-progress-linear.md-THEME_NAME-theme .md-container{background-color:\"{{primary-100}}\"}md-progress-linear.md-THEME_NAME-theme .md-bar{background-color:\"{{primary-color}}\"}md-progress-linear.md-THEME_NAME-theme.md-warn .md-container{background-color:\"{{warn-100}}\"}md-progress-linear.md-THEME_NAME-theme.md-warn .md-bar{background-color:\"{{warn-color}}\"}md-progress-linear.md-THEME_NAME-theme.md-accent .md-container{background-color:\"{{accent-100}}\"}md-progress-linear.md-THEME_NAME-theme.md-accent .md-bar{background-color:\"{{accent-color}}\"}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-primary .md-bar1{background-color:\"{{primary-100}}\"}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-primary .md-dashed:before{background:radial-gradient(\"{{primary-100}}\" 0,\"{{primary-100}}\" 16%,transparent 42%)}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-warn .md-bar1{background-color:\"{{warn-100}}\"}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-warn .md-dashed:before{background:radial-gradient(\"{{warn-100}}\" 0,\"{{warn-100}}\" 16%,transparent 42%)}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-accent .md-bar1{background-color:\"{{accent-100}}\"}md-progress-linear.md-THEME_NAME-theme[md-mode=buffer].md-accent .md-dashed:before{background:radial-gradient(\"{{accent-100}}\" 0,\"{{accent-100}}\" 16%,transparent 42%)}md-radio-button.md-THEME_NAME-theme .md-off{border-color:\"{{foreground-2}}\"}md-radio-button.md-THEME_NAME-theme .md-on{background-color:\"{{accent-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme.md-checked .md-off{border-color:\"{{accent-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme.md-checked .md-ink-ripple{color:\"{{accent-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme .md-container .md-ripple{color:\"{{accent-A700}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary .md-on,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary .md-on,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary .md-on,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary .md-on{background-color:\"{{primary-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary.md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary .md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary .md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary.md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary .md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary .md-checked .md-off{border-color:\"{{primary-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary.md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary .md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary .md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary.md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary.md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary .md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary .md-checked .md-ink-ripple{color:\"{{primary-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-primary .md-container .md-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-primary .md-container .md-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-primary .md-container .md-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-primary .md-container .md-ripple{color:\"{{primary-600}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn .md-on,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn .md-on,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn .md-on,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn .md-on{background-color:\"{{warn-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn.md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn .md-checked .md-off,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn .md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn.md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn .md-checked .md-off,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn .md-checked .md-off{border-color:\"{{warn-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn.md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn .md-checked .md-ink-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn .md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn.md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn.md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn .md-checked .md-ink-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn .md-checked .md-ink-ripple{color:\"{{warn-color-0.87}}\"}md-radio-button.md-THEME_NAME-theme:not([disabled]).md-warn .md-container .md-ripple,md-radio-button.md-THEME_NAME-theme:not([disabled]) .md-warn .md-container .md-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]).md-warn .md-container .md-ripple,md-radio-group.md-THEME_NAME-theme:not([disabled]) .md-warn .md-container .md-ripple{color:\"{{warn-600}}\"}md-radio-button.md-THEME_NAME-theme[disabled],md-radio-group.md-THEME_NAME-theme[disabled]{color:\"{{foreground-3}}\"}md-radio-button.md-THEME_NAME-theme[disabled] .md-container .md-off,md-radio-button.md-THEME_NAME-theme[disabled] .md-container .md-on,md-radio-group.md-THEME_NAME-theme[disabled] .md-container .md-off,md-radio-group.md-THEME_NAME-theme[disabled] .md-container .md-on{border-color:\"{{foreground-3}}\"}md-radio-group.md-THEME_NAME-theme .md-checked .md-ink-ripple{color:\"{{accent-color-0.26}}\"}md-radio-group.md-THEME_NAME-theme .md-checked:not([disabled]).md-primary .md-ink-ripple,md-radio-group.md-THEME_NAME-theme.md-primary .md-checked:not([disabled]) .md-ink-ripple{color:\"{{primary-color-0.26}}\"}md-radio-group.md-THEME_NAME-theme.md-focused.ng-empty>md-radio-button:first-child .md-container:before{background-color:\"{{foreground-3-0.26}}\"}md-radio-group.md-THEME_NAME-theme.md-focused:not(:empty) .md-checked .md-container:before{background-color:\"{{accent-color-0.26}}\"}md-radio-group.md-THEME_NAME-theme.md-focused:not(:empty) .md-checked.md-primary .md-container:before,md-radio-group.md-THEME_NAME-theme.md-focused:not(:empty).md-primary .md-checked .md-container:before{background-color:\"{{primary-color-0.26}}\"}md-radio-group.md-THEME_NAME-theme.md-focused:not(:empty) .md-checked.md-warn .md-container:before,md-radio-group.md-THEME_NAME-theme.md-focused:not(:empty).md-warn .md-checked .md-container:before{background-color:\"{{warn-color-0.26}}\"}md-input-container md-select.md-THEME_NAME-theme .md-select-value span:first-child:after{color:\"{{warn-A700}}\"}md-input-container:not(.md-input-focused):not(.md-input-invalid) md-select.md-THEME_NAME-theme .md-select-value span:first-child:after{color:\"{{foreground-3}}\"}md-input-container.md-input-focused:not(.md-input-has-value) md-select.md-THEME_NAME-theme .md-select-value,md-input-container.md-input-focused:not(.md-input-has-value) md-select.md-THEME_NAME-theme .md-select-value.md-select-placeholder{color:\"{{primary-color}}\"}md-input-container.md-input-invalid md-select.md-THEME_NAME-theme .md-select-value{color:\"{{warn-A700}}\"!important;border-bottom-color:\"{{warn-A700}}\"!important}md-input-container.md-input-invalid md-select.md-THEME_NAME-theme.md-no-underline .md-select-value{border-bottom-color:transparent!important}md-input-container:not(.md-input-invalid).md-input-focused.md-accent .md-select-value{border-color:\"{{accent-color}}\"}md-input-container:not(.md-input-invalid).md-input-focused.md-accent .md-select-value span{color:\"{{accent-color}}\"}md-input-container:not(.md-input-invalid).md-input-focused.md-warn .md-select-value{border-color:\"{{warn-A700}}\"}md-input-container:not(.md-input-invalid).md-input-focused.md-warn .md-select-value span{color:\"{{warn-A700}}\"}md-select.md-THEME_NAME-theme[disabled] .md-select-value{border-bottom-color:transparent;background-image:linear-gradient(90deg,\"{{foreground-3}}\" 0,\"{{foreground-3}}\" 33%,transparent 0);background-image:-ms-linear-gradient(left,transparent 0,\"{{foreground-3}}\" 100%)}md-select.md-THEME_NAME-theme .md-select-value{border-bottom-color:\"{{foreground-4}}\"}md-select.md-THEME_NAME-theme .md-select-value.md-select-placeholder{color:\"{{foreground-3}}\"}md-select.md-THEME_NAME-theme .md-select-value span:first-child:after{color:\"{{warn-A700}}\"}md-select.md-THEME_NAME-theme.md-no-underline .md-select-value{border-bottom-color:transparent!important}md-select.md-THEME_NAME-theme.ng-invalid.ng-touched .md-select-value{color:\"{{warn-A700}}\"!important;border-bottom-color:\"{{warn-A700}}\"!important}md-select.md-THEME_NAME-theme.ng-invalid.ng-touched.md-no-underline .md-select-value{border-bottom-color:transparent!important}md-select.md-THEME_NAME-theme:not([disabled]):focus .md-select-value{border-bottom-color:\"{{primary-color}}\";color:\"{{ foreground-1 }}\"}md-select.md-THEME_NAME-theme:not([disabled]):focus .md-select-value.md-select-placeholder{color:\"{{ foreground-1 }}\"}md-select.md-THEME_NAME-theme:not([disabled]):focus.md-no-underline .md-select-value{border-bottom-color:transparent!important}md-select.md-THEME_NAME-theme:not([disabled]):focus.md-accent .md-select-value{border-bottom-color:\"{{accent-color}}\"}md-select.md-THEME_NAME-theme:not([disabled]):focus.md-warn .md-select-value{border-bottom-color:\"{{warn-color}}\"}md-select.md-THEME_NAME-theme[disabled] .md-select-icon,md-select.md-THEME_NAME-theme[disabled] .md-select-value,md-select.md-THEME_NAME-theme[disabled] .md-select-value.md-select-placeholder{color:\"{{foreground-3}}\"}md-select.md-THEME_NAME-theme .md-select-icon{color:\"{{foreground-2}}\"}md-select-menu.md-THEME_NAME-theme md-content{background-color:\"{{background-hue-1}}\"}md-select-menu.md-THEME_NAME-theme md-content md-optgroup{color:\"{{foreground-2}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option{color:\"{{foreground-1}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option[disabled] .md-text{color:\"{{foreground-3}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option:not([disabled]):focus,md-select-menu.md-THEME_NAME-theme md-content md-option:not([disabled]):hover{background-color:\"{{background-500-0.18}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option[selected]{color:\"{{primary-500}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option[selected]:focus{color:\"{{primary-600}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option[selected].md-accent{color:\"{{accent-color}}\"}md-select-menu.md-THEME_NAME-theme md-content md-option[selected].md-accent:focus{color:\"{{accent-A700}}\"}.md-checkbox-enabled.md-THEME_NAME-theme .md-ripple{color:\"{{primary-600}}\"}.md-checkbox-enabled.md-THEME_NAME-theme[selected] .md-ripple{color:\"{{background-600}}\"}.md-checkbox-enabled.md-THEME_NAME-theme .md-ink-ripple{color:\"{{foreground-2}}\"}.md-checkbox-enabled.md-THEME_NAME-theme[selected] .md-ink-ripple{color:\"{{primary-color-0.87}}\"}.md-checkbox-enabled.md-THEME_NAME-theme:not(.md-checked) .md-icon{border-color:\"{{foreground-2}}\"}.md-checkbox-enabled.md-THEME_NAME-theme[selected] .md-icon{background-color:\"{{primary-color-0.87}}\"}.md-checkbox-enabled.md-THEME_NAME-theme[selected].md-focused .md-container:before{background-color:\"{{primary-color-0.26}}\"}.md-checkbox-enabled.md-THEME_NAME-theme[selected] .md-icon:after{border-color:\"{{primary-contrast-0.87}}\"}.md-checkbox-enabled.md-THEME_NAME-theme .md-indeterminate[disabled] .md-container{color:\"{{foreground-3}}\"}.md-checkbox-enabled.md-THEME_NAME-theme md-option .md-text{color:\"{{foreground-1}}\"}md-sidenav.md-THEME_NAME-theme,md-sidenav.md-THEME_NAME-theme md-content{background-color:\"{{background-hue-1}}\"}md-slider.md-THEME_NAME-theme .md-track{background-color:\"{{foreground-3}}\"}md-slider.md-THEME_NAME-theme .md-track-ticks{color:\"{{background-contrast}}\"}md-slider.md-THEME_NAME-theme .md-focus-ring{background-color:\"{{accent-A200-0.2}}\"}md-slider.md-THEME_NAME-theme .md-disabled-thumb{border-color:\"{{background-color}}\";background-color:\"{{background-color}}\"}md-slider.md-THEME_NAME-theme.md-min .md-thumb:after{background-color:\"{{background-color}}\";border-color:\"{{foreground-3}}\"}md-slider.md-THEME_NAME-theme.md-min .md-focus-ring{background-color:\"{{foreground-3-0.38}}\"}md-slider.md-THEME_NAME-theme.md-min[md-discrete] .md-thumb:after{background-color:\"{{background-contrast}}\";border-color:transparent}md-slider.md-THEME_NAME-theme.md-min[md-discrete] .md-sign{background-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme.md-min[md-discrete] .md-sign:after{border-top-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme.md-min[md-discrete][md-vertical] .md-sign:after{border-top-color:transparent;border-left-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme .md-track.md-track-fill{background-color:\"{{accent-color}}\"}md-slider.md-THEME_NAME-theme .md-thumb:after{border-color:\"{{accent-color}}\";background-color:\"{{accent-color}}\"}md-slider.md-THEME_NAME-theme .md-sign{background-color:\"{{accent-color}}\"}md-slider.md-THEME_NAME-theme .md-sign:after{border-top-color:\"{{accent-color}}\"}md-slider.md-THEME_NAME-theme[md-vertical] .md-sign:after{border-top-color:transparent;border-left-color:\"{{accent-color}}\"}md-slider.md-THEME_NAME-theme .md-thumb-text{color:\"{{accent-contrast}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-focus-ring{background-color:\"{{warn-200-0.38}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-track.md-track-fill{background-color:\"{{warn-color}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-thumb:after{border-color:\"{{warn-color}}\";background-color:\"{{warn-color}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-sign{background-color:\"{{warn-color}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-sign:after{border-top-color:\"{{warn-color}}\"}md-slider.md-THEME_NAME-theme.md-warn[md-vertical] .md-sign:after{border-top-color:transparent;border-left-color:\"{{warn-color}}\"}md-slider.md-THEME_NAME-theme.md-warn .md-thumb-text{color:\"{{warn-contrast}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-focus-ring{background-color:\"{{primary-200-0.38}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-track.md-track-fill{background-color:\"{{primary-color}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-thumb:after{border-color:\"{{primary-color}}\";background-color:\"{{primary-color}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-sign{background-color:\"{{primary-color}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-sign:after{border-top-color:\"{{primary-color}}\"}md-slider.md-THEME_NAME-theme.md-primary[md-vertical] .md-sign:after{border-top-color:transparent;border-left-color:\"{{primary-color}}\"}md-slider.md-THEME_NAME-theme.md-primary .md-thumb-text{color:\"{{primary-contrast}}\"}md-slider.md-THEME_NAME-theme[disabled] .md-thumb:after{border-color:transparent}md-slider.md-THEME_NAME-theme[disabled]:not(.md-min) .md-thumb:after,md-slider.md-THEME_NAME-theme[disabled][md-discrete] .md-thumb:after{background-color:\"{{foreground-3}}\";border-color:transparent}md-slider.md-THEME_NAME-theme[disabled][readonly] .md-sign{background-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme[disabled][readonly] .md-sign:after{border-top-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme[disabled][readonly][md-vertical] .md-sign:after{border-top-color:transparent;border-left-color:\"{{background-400}}\"}md-slider.md-THEME_NAME-theme[disabled][readonly] .md-disabled-thumb{border-color:transparent;background-color:transparent}md-slider-container[disabled]>:first-child:not(md-slider),md-slider-container[disabled]>:last-child:not(md-slider){color:\"{{foreground-3}}\"}.md-subheader.md-THEME_NAME-theme{color:\"{{ foreground-2-0.23 }}\";background-color:\"{{background-default}}\"}.md-subheader.md-THEME_NAME-theme.md-primary{color:\"{{primary-color}}\"}.md-subheader.md-THEME_NAME-theme.md-accent{color:\"{{accent-color}}\"}.md-subheader.md-THEME_NAME-theme.md-warn{color:\"{{warn-color}}\"}md-switch.md-THEME_NAME-theme .md-ink-ripple{color:\"{{background-500}}\"}md-switch.md-THEME_NAME-theme .md-thumb{background-color:\"{{background-50}}\"}md-switch.md-THEME_NAME-theme .md-bar{background-color:\"{{background-500}}\"}md-switch.md-THEME_NAME-theme.md-focused:not(.md-checked) .md-thumb:before,md-switch.md-THEME_NAME-theme.md-focused[disabled] .md-thumb:before{background-color:\"{{foreground-4}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]) .md-ink-ripple{color:\"{{accent-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]) .md-thumb{background-color:\"{{accent-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]) .md-bar{background-color:\"{{accent-color-0.5}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-focused .md-thumb:before{background-color:\"{{accent-color-0.26}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-primary .md-ink-ripple{color:\"{{primary-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-primary .md-thumb{background-color:\"{{primary-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-primary .md-bar{background-color:\"{{primary-color-0.5}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-primary.md-focused .md-thumb:before{background-color:\"{{primary-color-0.26}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-warn .md-ink-ripple{color:\"{{warn-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-warn .md-thumb{background-color:\"{{warn-color}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-warn .md-bar{background-color:\"{{warn-color-0.5}}\"}md-switch.md-THEME_NAME-theme.md-checked:not([disabled]).md-warn.md-focused .md-thumb:before{background-color:\"{{warn-color-0.26}}\"}md-switch.md-THEME_NAME-theme[disabled] .md-thumb{background-color:\"{{background-400}}\"}md-switch.md-THEME_NAME-theme[disabled] .md-bar{background-color:\"{{foreground-4}}\"}md-tabs.md-THEME_NAME-theme md-tabs-wrapper{background-color:transparent;border-color:\"{{foreground-4}}\"}md-tabs.md-THEME_NAME-theme .md-paginator md-icon{color:\"{{primary-color}}\"}md-tabs.md-THEME_NAME-theme md-ink-bar{color:\"{{accent-color}}\";background:\"{{accent-color}}\"}md-tabs.md-THEME_NAME-theme .md-tab{color:\"{{foreground-2}}\"}md-tabs.md-THEME_NAME-theme .md-tab[disabled],md-tabs.md-THEME_NAME-theme .md-tab[disabled] md-icon{color:\"{{foreground-3}}\"}md-tabs.md-THEME_NAME-theme .md-tab.md-active,md-tabs.md-THEME_NAME-theme .md-tab.md-active md-icon,md-tabs.md-THEME_NAME-theme .md-tab.md-focused,md-tabs.md-THEME_NAME-theme .md-tab.md-focused md-icon{color:\"{{primary-color}}\"}md-tabs.md-THEME_NAME-theme .md-tab.md-focused{background:\"{{primary-color-0.1}}\"}md-tabs.md-THEME_NAME-theme .md-tab .md-ripple-container{color:\"{{accent-A100}}\"}md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper{background-color:\"{{accent-color}}\"}md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{accent-A100}}\"}md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{accent-contrast}}\"}md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{accent-contrast-0.1}}\"}md-tabs.md-THEME_NAME-theme.md-accent>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-ink-bar{color:\"{{primary-600-1}}\";background:\"{{primary-600-1}}\"}md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper{background-color:\"{{primary-color}}\"}md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{primary-100}}\"}md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{primary-contrast}}\"}md-tabs.md-THEME_NAME-theme.md-primary>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{primary-contrast-0.1}}\"}md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper{background-color:\"{{warn-color}}\"}md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{warn-100}}\"}md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{warn-contrast}}\"}md-tabs.md-THEME_NAME-theme.md-warn>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{warn-contrast-0.1}}\"}md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper{background-color:\"{{primary-color}}\"}md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{primary-100}}\"}md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{primary-contrast}}\"}md-toolbar>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{primary-contrast-0.1}}\"}md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper{background-color:\"{{accent-color}}\"}md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{accent-A100}}\"}md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{accent-contrast}}\"}md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{accent-contrast-0.1}}\"}md-toolbar.md-accent>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-ink-bar{color:\"{{primary-600-1}}\";background:\"{{primary-600-1}}\"}md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper{background-color:\"{{warn-color}}\"}md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]),md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]) md-icon{color:\"{{warn-100}}\"}md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active,md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-active md-icon,md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused,md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused md-icon{color:\"{{warn-contrast}}\"}md-toolbar.md-warn>md-tabs.md-THEME_NAME-theme>md-tabs-wrapper>md-tabs-canvas>md-pagination-wrapper>md-tab-item:not([disabled]).md-focused{background:\"{{warn-contrast-0.1}}\"}md-toast.md-THEME_NAME-theme .md-toast-content{background-color:#323232;color:\"{{background-50}}\"}md-toast.md-THEME_NAME-theme .md-toast-content .md-button{color:\"{{background-50}}\"}md-toast.md-THEME_NAME-theme .md-toast-content .md-button.md-highlight{color:\"{{accent-color}}\"}md-toast.md-THEME_NAME-theme .md-toast-content .md-button.md-highlight.md-primary{color:\"{{primary-color}}\"}md-toast.md-THEME_NAME-theme .md-toast-content .md-button.md-highlight.md-warn{color:\"{{warn-color}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar){background-color:\"{{primary-color}}\";color:\"{{primary-contrast}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar) md-icon{color:\"{{primary-contrast}}\";fill:\"{{primary-contrast}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar) .md-button[disabled] md-icon{color:\"{{primary-contrast-0.26}}\";fill:\"{{primary-contrast-0.26}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar).md-accent{background-color:\"{{accent-color}}\";color:\"{{accent-contrast}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar).md-accent .md-ink-ripple{color:\"{{accent-contrast}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar).md-accent md-icon{color:\"{{accent-contrast}}\";fill:\"{{accent-contrast}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar).md-accent .md-button[disabled] md-icon{color:\"{{accent-contrast-0.26}}\";fill:\"{{accent-contrast-0.26}}\"}md-toolbar.md-THEME_NAME-theme:not(.md-menu-toolbar).md-warn{background-color:\"{{warn-color}}\";color:\"{{warn-contrast}}\"}.md-panel.md-tooltip.md-THEME_NAME-theme{color:\"{{background-700-contrast}}\";background-color:\"{{background-700}}\"}body.md-THEME_NAME-theme,html.md-THEME_NAME-theme{color:\"{{foreground-1}}\";background-color:\"{{background-color}}\"}"); 
})();


})(window, window.angular);