/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.progressCircular');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.progressCircular
 * @description Module for a circular progressbar
 */

angular.module('material.components.progressCircular', ['material.core']);

/**
 * @ngdoc directive
 * @name mdProgressCircular
 * @module material.components.progressCircular
 * @restrict E
 *
 * @description
 * The circular progress directive is used to make loading content in your app as delightful and
 * painless as possible by minimizing the amount of visual change a user sees before they can view
 * and interact with content.
 *
 * For operations where the percentage of the operation completed can be determined, use a
 * determinate indicator. They give users a quick sense of how long an operation will take.
 *
 * For operations where the user is asked to wait a moment while something finishes up, and it’s
 * not necessary to expose what's happening behind the scenes and how long it will take, use an
 * indeterminate indicator.
 *
 * @param {string} md-mode Select from one of two modes: **'determinate'** and **'indeterminate'**.
 *
 * Note: if the `md-mode` value is set as undefined or specified as not 1 of the two (2) valid modes, then **'indeterminate'**
 * will be auto-applied as the mode.
 *
 * Note: if not configured, the `md-mode="indeterminate"` will be auto injected as an attribute.
 * If `value=""` is also specified, however, then `md-mode="determinate"` would be auto-injected instead.
 * @param {number=} value In determinate mode, this number represents the percentage of the
 *     circular progress. Default: 0
 * @param {number=} md-diameter This specifies the diameter of the circular progress. The value
 * should be a pixel-size value (eg '100'). If this attribute is
 * not present then a default value of '50px' is assumed.
 *
 * @param {boolean=} ng-disabled Determines whether to disable the progress element.
 *
 * @usage
 * <hljs lang="html">
 * <md-progress-circular md-mode="determinate" value="..."></md-progress-circular>
 *
 * <md-progress-circular md-mode="determinate" ng-value="..."></md-progress-circular>
 *
 * <md-progress-circular md-mode="determinate" value="..." md-diameter="100"></md-progress-circular>
 *
 * <md-progress-circular md-mode="indeterminate"></md-progress-circular>
 * </hljs>
 */

MdProgressCircularDirective['$inject'] = ["$window", "$mdProgressCircular", "$mdTheming", "$mdUtil", "$interval", "$log"];
angular
  .module('material.components.progressCircular')
  .directive('mdProgressCircular', MdProgressCircularDirective);

/* ngInject */
function MdProgressCircularDirective($window, $mdProgressCircular, $mdTheming,
                                     $mdUtil, $interval, $log) {

  // Note that this shouldn't use use $$rAF, because it can cause an infinite loop
  // in any tests that call $animate.flush.
  var rAF = $window.requestAnimationFrame ||
            $window.webkitRequestAnimationFrame ||
            angular.noop;

  var cAF = $window.cancelAnimationFrame ||
            $window.webkitCancelAnimationFrame ||
            $window.webkitCancelRequestAnimationFrame ||
            angular.noop;

  var MODE_DETERMINATE = 'determinate';
  var MODE_INDETERMINATE = 'indeterminate';
  var DISABLED_CLASS = '_md-progress-circular-disabled';
  var INDETERMINATE_CLASS = 'md-mode-indeterminate';

  return {
    restrict: 'E',
    scope: {
      value: '@',
      mdDiameter: '@',
      mdMode: '@'
    },
    template:
      '<svg xmlns="http://www.w3.org/2000/svg">' +
        '<path fill="none"/>' +
      '</svg>',
    compile: function(element, attrs) {
      element.attr({
        'aria-valuemin': 0,
        'aria-valuemax': 100,
        'role': 'progressbar'
      });

      if (angular.isUndefined(attrs.mdMode)) {
        var mode = attrs.hasOwnProperty('value') ? MODE_DETERMINATE : MODE_INDETERMINATE;
        attrs.$set('mdMode', mode);
      } else {
        attrs.$set('mdMode', attrs.mdMode.trim());
      }

      return MdProgressCircularLink;
    }
  };

  function MdProgressCircularLink(scope, element, attrs) {
    var node = element[0];
    var svg = angular.element(node.querySelector('svg'));
    var path = angular.element(node.querySelector('path'));
    var startIndeterminate = $mdProgressCircular.startIndeterminate;
    var endIndeterminate = $mdProgressCircular.endIndeterminate;
    var iterationCount = 0;
    var lastAnimationId = 0;
    var lastDrawFrame;
    var interval;

    $mdTheming(element);
    element.toggleClass(DISABLED_CLASS, attrs.hasOwnProperty('disabled'));

    // If the mode is indeterminate, it doesn't need to
    // wait for the next digest. It can start right away.
    if (scope.mdMode === MODE_INDETERMINATE){
      startIndeterminateAnimation();
    }

    scope.$on('$destroy', function(){
      cleanupIndeterminateAnimation();

      if (lastDrawFrame) {
        cAF(lastDrawFrame);
      }
    });

    scope.$watchGroup(['value', 'mdMode', function() {
      var isDisabled = node.disabled;

      // Sometimes the browser doesn't return a boolean, in
      // which case we should check whether the attribute is
      // present.
      if (isDisabled === true || isDisabled === false){
        return isDisabled;
      }

      return angular.isDefined(element.attr('disabled'));
    }], function(newValues, oldValues) {
      var mode = newValues[1];
      var isDisabled = newValues[2];
      var wasDisabled = oldValues[2];
      var diameter = 0;
      var strokeWidth = 0;

      if (isDisabled !== wasDisabled) {
        element.toggleClass(DISABLED_CLASS, !!isDisabled);
      }

      if (isDisabled) {
        cleanupIndeterminateAnimation();
      } else {
        if (mode !== MODE_DETERMINATE && mode !== MODE_INDETERMINATE) {
          mode = MODE_INDETERMINATE;
          attrs.$set('mdMode', mode);
        }

        if (mode === MODE_INDETERMINATE) {
          if (oldValues[1] === MODE_DETERMINATE) {
            diameter = getSize(scope.mdDiameter);
            strokeWidth = getStroke(diameter);
            path.attr('d', getSvgArc(diameter, strokeWidth, true));
            path.attr('stroke-dasharray', (diameter - strokeWidth) * $window.Math.PI * 0.75);
          }
          startIndeterminateAnimation();
        } else {
          var newValue = clamp(newValues[0]);
          var oldValue = clamp(oldValues[0]);

          cleanupIndeterminateAnimation();

          if (oldValues[1] === MODE_INDETERMINATE) {
            diameter = getSize(scope.mdDiameter);
            strokeWidth = getStroke(diameter);
            path.attr('d', getSvgArc(diameter, strokeWidth, false));
            path.attr('stroke-dasharray', (diameter - strokeWidth) * $window.Math.PI);
          }

          element.attr('aria-valuenow', newValue);
          renderCircle(oldValue, newValue);
        }
      }

    });

    // This is in a separate watch in order to avoid layout, unless
    // the value has actually changed.
    scope.$watch('mdDiameter', function(newValue) {
      var diameter = getSize(newValue);
      var strokeWidth = getStroke(diameter);
      var value = clamp(scope.value);
      var transformOrigin = (diameter / 2) + 'px';
      var dimensions = {
        width: diameter + 'px',
        height: diameter + 'px'
      };

      // The viewBox has to be applied via setAttribute, because it is
      // case-sensitive. If jQuery is included in the page, `.attr` lowercases
      // all attribute names.
      svg[0].setAttribute('viewBox', '0 0 ' + diameter + ' ' + diameter);

      // Usually viewBox sets the dimensions for the SVG, however that doesn't
      // seem to be the case on IE10.
      // Important! The transform origin has to be set from here and it has to
      // be in the format of "Ypx Ypx Ypx", otherwise the rotation wobbles in
      // IE and Edge, because they don't account for the stroke width when
      // rotating. Also "center" doesn't help in this case, it has to be a
      // precise value.
      svg
        .css(dimensions)
        .css('transform-origin', transformOrigin + ' ' + transformOrigin + ' ' + transformOrigin);

      element.css(dimensions);

      path.attr('stroke-width', strokeWidth);
      path.attr('stroke-linecap', 'square');
      if (scope.mdMode == MODE_INDETERMINATE) {
        path.attr('d', getSvgArc(diameter, strokeWidth, true));
        path.attr('stroke-dasharray', (diameter - strokeWidth) * $window.Math.PI * 0.75);
        path.attr('stroke-dashoffset', getDashLength(diameter, strokeWidth, 1, 75));
      } else {
        path.attr('d', getSvgArc(diameter, strokeWidth, false));
        path.attr('stroke-dasharray', (diameter - strokeWidth) * $window.Math.PI);
        path.attr('stroke-dashoffset', getDashLength(diameter, strokeWidth, 0, 100));
        renderCircle(value, value);
      }

    });

    function renderCircle(animateFrom, animateTo, easing, duration, iterationCount, maxValue) {
      var id = ++lastAnimationId;
      var startTime = $mdUtil.now();
      var changeInValue = animateTo - animateFrom;
      var diameter = getSize(scope.mdDiameter);
      var strokeWidth = getStroke(diameter);
      var ease = easing || $mdProgressCircular.easeFn;
      var animationDuration = duration || $mdProgressCircular.duration;
      var rotation = -90 * (iterationCount || 0);
      var dashLimit = maxValue || 100;

      // No need to animate it if the values are the same
      if (animateTo === animateFrom) {
        renderFrame(animateTo);
      } else {
        lastDrawFrame = rAF(function animation() {
          var currentTime = $window.Math.max(0, $window.Math.min($mdUtil.now() - startTime, animationDuration));

          renderFrame(ease(currentTime, animateFrom, changeInValue, animationDuration));

          // Do not allow overlapping animations
          if (id === lastAnimationId && currentTime < animationDuration) {
            lastDrawFrame = rAF(animation);
          }
        });
      }

      function renderFrame(value) {
        path.attr('stroke-dashoffset', getDashLength(diameter, strokeWidth, value, dashLimit));
        path.attr('transform','rotate(' + (rotation) + ' ' + diameter/2 + ' ' + diameter/2 + ')');
      }
    }

    function animateIndeterminate() {
      renderCircle(
        startIndeterminate,
        endIndeterminate,
        $mdProgressCircular.easeFnIndeterminate,
        $mdProgressCircular.durationIndeterminate,
        iterationCount,
        75
      );

      // The %4 technically isn't necessary, but it keeps the rotation
      // under 360, instead of becoming a crazy large number.
      iterationCount = ++iterationCount % 4;

    }

    function startIndeterminateAnimation() {
      if (!interval) {
        // Note that this interval isn't supposed to trigger a digest.
        interval = $interval(
          animateIndeterminate,
          $mdProgressCircular.durationIndeterminate,
          0,
          false
        );

        animateIndeterminate();

        element
          .addClass(INDETERMINATE_CLASS)
          .removeAttr('aria-valuenow');
      }
    }

    function cleanupIndeterminateAnimation() {
      if (interval) {
        $interval.cancel(interval);
        interval = null;
        element.removeClass(INDETERMINATE_CLASS);
      }
    }
  }

  /**
   * Returns SVG path data for progress circle
   * Syntax spec: https://www.w3.org/TR/SVG/paths.html#PathDataEllipticalArcCommands
   *
   * @param {number} diameter Diameter of the container.
   * @param {number} strokeWidth Stroke width to be used when drawing circle
   * @param {boolean} indeterminate Use if progress circle will be used for indeterminate
   *
   * @returns {string} String representation of an SVG arc.
   */
  function getSvgArc(diameter, strokeWidth, indeterminate) {
    var radius = diameter / 2;
    var offset = strokeWidth / 2;
    var start = radius + ',' + offset; // ie: (25, 2.5) or 12 o'clock
    var end = offset + ',' + radius;   // ie: (2.5, 25) or  9 o'clock
    var arcRadius = radius - offset;
    return 'M' + start
         + 'A' + arcRadius + ',' + arcRadius + ' 0 1 1 ' + end // 75% circle
         + (indeterminate ? '' : 'A' + arcRadius + ',' + arcRadius + ' 0 0 1 ' + start); // loop to start
  }

  /**
   * Return stroke length for progress circle
   *
   * @param {number} diameter Diameter of the container.
   * @param {number} strokeWidth Stroke width to be used when drawing circle
   * @param {number} value Percentage of circle (between 0 and 100)
   * @param {number} limit Max percentage for circle
   *
   * @returns {number} Stroke length for progres circle
   */
  function getDashLength(diameter, strokeWidth, value, limit) {
    return (diameter - strokeWidth) * $window.Math.PI * ((3 * (limit || 100) / 100) - (value/100));
  }

  /**
   * Limits a value between 0 and 100.
   */
  function clamp(value) {
    return $window.Math.max(0, $window.Math.min(value || 0, 100));
  }

  /**
   * Determines the size of a progress circle, based on the provided
   * value in the following formats: `X`, `Ypx`, `Z%`.
   */
  function getSize(value) {
    var defaultValue = $mdProgressCircular.progressSize;

    if (value) {
      var parsed = parseFloat(value);

      if (value.lastIndexOf('%') === value.length - 1) {
        parsed = (parsed / 100) * defaultValue;
      }

      return parsed;
    }

    return defaultValue;
  }

  /**
   * Determines the circle's stroke width, based on
   * the provided diameter.
   */
  function getStroke(diameter) {
    return $mdProgressCircular.strokeWidth / 100 * diameter;
  }

}

/**
 * @ngdoc service
 * @name $mdProgressCircular
 * @module material.components.progressCircular
 *
 * @description
 * Allows the user to specify the default options for the `progressCircular` directive.
 *
 * @property {number} progressSize Diameter of the progress circle in pixels.
 * @property {number} strokeWidth Width of the circle's stroke as a percentage of the circle's size.
 * @property {number} duration Length of the circle animation in milliseconds.
 * @property {function} easeFn Default easing animation function.
 * @property {object} easingPresets Collection of pre-defined easing functions.
 *
 * @property {number} durationIndeterminate Duration of the indeterminate animation.
 * @property {number} startIndeterminate Indeterminate animation start point.
 * @property {number} endIndeterminate Indeterminate animation end point.
 * @property {function} easeFnIndeterminate Easing function to be used when animating
 * between the indeterminate values.
 *
 * @property {(function(object): object)} configure Used to modify the default options.
 *
 * @usage
 * <hljs lang="js">
 *   myAppModule.config(function($mdProgressCircularProvider) {
 *
 *     // Example of changing the default progress options.
 *     $mdProgressCircularProvider.configure({
 *       progressSize: 100,
 *       strokeWidth: 20,
 *       duration: 800
 *     });
 * });
 * </hljs>
 *
 */

angular
  .module('material.components.progressCircular')
  .provider("$mdProgressCircular", MdProgressCircularProvider);

function MdProgressCircularProvider() {
  var progressConfig = {
    progressSize: 50,
    strokeWidth: 10,
    duration: 100,
    easeFn: linearEase,

    durationIndeterminate: 1333,
    startIndeterminate: 1,
    endIndeterminate: 149,
    easeFnIndeterminate: materialEase,

    easingPresets: {
      linearEase: linearEase,
      materialEase: materialEase
    }
  };

  return {
    configure: function(options) {
      progressConfig = angular.extend(progressConfig, options || {});
      return progressConfig;
    },
    $get: function() { return progressConfig; }
  };

  function linearEase(t, b, c, d) {
    return c * t / d + b;
  }

  function materialEase(t, b, c, d) {
    // via http://www.timotheegroleau.com/Flash/experiments/easing_function_generator.htm
    // with settings of [0, 0, 1, 1]
    var ts = (t /= d) * t;
    var tc = ts * t;
    return b + c * (6 * tc * ts + -15 * ts * ts + 10 * tc);
  }
}

ngmaterial.components.progressCircular = angular.module("material.components.progressCircular");