/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.slider');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.slider
 */
SliderDirective['$inject'] = ["$$rAF", "$window", "$mdAria", "$mdUtil", "$mdConstant", "$mdTheming", "$mdGesture", "$parse", "$log", "$timeout"];
angular.module('material.components.slider', [
  'material.core'
])
.directive('mdSlider', SliderDirective)
.directive('mdSliderContainer', SliderContainerDirective);

/**
 * @type {number} the page size used for stepping when page up/down keys are pressed.
 */
var stepPageSize = 10;
/**
 * @type {number} the multiplier applied to a step when the arrow key is pressed along with
 *  alt, meta, or ctrl.
 */
var modifierMultiplier = 4;

/**
 * @ngdoc directive
 * @name mdSliderContainer
 * @module material.components.slider
 * @restrict E
 * @description
 * The `<md-slider-container>` can hold the slider with two other elements.
 * In this case, the other elements are a `span` for the label and an `input` for displaying
 * the model value.
 *
 * @usage
 * <hljs lang="html">
 *  <md-slider-container>
 *    <span>Red</span>
 *    <md-slider min="0" max="255" ng-model="color.red" aria-label="red" id="red-slider">
 *    </md-slider>
 *    <md-input-container>
 *      <input type="number" ng-model="color.red" aria-label="Red" aria-controls="red-slider">
 *    </md-input-container>
 *  </md-slider-container>
 * </hljs>
 */
function SliderContainerDirective() {
  return {
    controller: function () {},
    compile: function (elem) {
      var slider = elem.find('md-slider');

      if (!slider) {
        return;
      }

      var vertical = slider.attr('md-vertical');

      if (vertical !== undefined) {
        elem.attr('md-vertical', '');
      }

      if (!slider.attr('flex')) {
        slider.attr('flex', '');
      }

      return function postLink(scope, element, attr, ctrl) {
        element.addClass('_md');     // private md component indicator for styling

        // We have to manually stop the $watch on ngDisabled because it exists
        // on the parent scope, and won't be automatically destroyed when
        // the component is destroyed.
        function setDisable(value) {
          element.children().attr('disabled', value);
          element.find('input').attr('disabled', value);
        }

        var stopDisabledWatch = angular.noop;

        if (attr.disabled) {
          setDisable(true);
        }
        else if (attr.ngDisabled) {
          stopDisabledWatch = scope.$watch(attr.ngDisabled, function (value) {
            setDisable(value);
          });
        }

        scope.$on('$destroy', function () {
          stopDisabledWatch();
        });

        var initialMaxWidth;

        /**
         * @param {number} length of the input's string value
         */
        ctrl.fitInputWidthToTextLength = function (length) {
          var input = element[0].querySelector('md-input-container');

          if (input) {
            var computedStyle = getComputedStyle(input);
            var minWidth = parseInt(computedStyle.minWidth);
            var padding = parseInt(computedStyle.paddingLeft) + parseInt(computedStyle.paddingRight);

            initialMaxWidth = initialMaxWidth || parseInt(computedStyle.maxWidth);
            var newMaxWidth = Math.max(initialMaxWidth, minWidth + padding + (minWidth / 2 * length));

            input.style.maxWidth = newMaxWidth + 'px';
          }
        };
      };
    }
  };
}

/**
 * @ngdoc directive
 * @name mdSlider
 * @module material.components.slider
 * @restrict E
 * @description
 * The `<md-slider>` component allows the user to choose from a range of values.
 *
 * As per the [Material Design spec](https://material.io/archive/guidelines/style/color.html#color-color-system)
 * the slider is in the accent color by default. The primary color palette may be used with
 * the `md-primary` class.
 *
 * The slider has two modes:
 * - "normal" mode where the user slides between a wide range of values
 * - "discrete" mode where the user slides between only a few select values
 *
 * To enable discrete mode, add the `md-discrete` attribute to a slider
 * and use the `step` attribute to change the distance between
 * values the user is allowed to pick.
 *
 * When using the keyboard:
 * - pressing the arrow keys will increase or decrease the slider's value by one step
 * - holding the Meta, Control, or Alt key while pressing the arrow keys will
 *   move the slider four steps at a time
 * - pressing the Home key will move the slider to the first allowed value
 * - pressing the End key will move the slider to the last allowed value
 * - pressing the Page Up key will increase the slider value by ten
 * - pressing the Page Down key will decrease the slider value by ten
 *
 * @usage
 * <h4>Normal Mode</h4>
 * <hljs lang="html">
 * <md-slider ng-model="myValue" min="5" max="500">
 * </md-slider>
 * </hljs>
 * <h4>Discrete Mode</h4>
 * <hljs lang="html">
 * <md-slider md-discrete ng-model="myDiscreteValue" step="10" min="10" max="130">
 * </md-slider>
 * </hljs>
 * <h4>Invert Mode</h4>
 * <hljs lang="html">
 * <md-slider md-invert ng-model="myValue" step="10" min="10" max="130">
 * </md-slider>
 * </hljs>
 *
 * @param {expression} ng-model Assignable angular expression to be data-bound.
 *  The expression should evaluate to a `number`.
 * @param {expression=} ng-disabled If this expression evaluates as truthy, the slider will be
 *  disabled.
 * @param {expression=} ng-readonly If this expression evaluates as truthy, the slider will be in
 *  read only mode.
 * @param {boolean=} md-discrete If this attribute exists during initialization, enable discrete
 *  mode. Defaults to `false`.
 * @param {boolean=} md-vertical If this attribute exists during initialization, enable vertical
 *  orientation mode. Defaults to `false`.
 * @param {boolean=} md-invert If this attribute exists during initialization, enable inverted mode.
 *  Defaults to `false`.
 * @param {number=} step The distance between values the user is allowed to pick. Defaults to `1`.
 * @param {number=} min The minimum value the user is allowed to pick. Defaults to `0`.
 * @param {number=} max The maximum value the user is allowed to pick. Defaults to `100`.
 * @param {number=} round The amount of numbers after the decimal point. The maximum is 6 to
 *  prevent scientific notation. Defaults to `3`.
 */
function SliderDirective($$rAF, $window, $mdAria, $mdUtil, $mdConstant, $mdTheming, $mdGesture,
                         $parse, $log, $timeout) {
  return {
    scope: {},
    require: ['?ngModel', '?^mdSliderContainer'],
    template:
      '<div class="md-slider-wrapper">' +
        '<div class="md-slider-content">' +
          '<div class="md-track-container">' +
            '<div class="md-track"></div>' +
            '<div class="md-track md-track-fill"></div>' +
            '<div class="md-track-ticks"></div>' +
          '</div>' +
          '<div class="md-thumb-container">' +
            '<div class="md-thumb"></div>' +
            '<div class="md-focus-thumb"></div>' +
            '<div class="md-focus-ring"></div>' +
            '<div class="md-sign">' +
              '<span class="md-thumb-text"></span>' +
            '</div>' +
            '<div class="md-disabled-thumb"></div>' +
          '</div>' +
        '</div>' +
      '</div>',
    compile: compile
  };

  // **********************************************************
  // Private Methods
  // **********************************************************

  function compile (tElement, tAttrs) {
    var wrapper = angular.element(tElement[0].getElementsByClassName('md-slider-wrapper'));

    var tabIndex = tAttrs.tabindex || 0;
    wrapper.attr('tabindex', tabIndex);

    if (tAttrs.disabled || tAttrs.ngDisabled) wrapper.attr('tabindex', -1);

    wrapper.attr('role', 'slider');

    $mdAria.expect(tElement, 'aria-label');

    return postLink;
  }

  function postLink(scope, element, attr, ctrls) {
    $mdTheming(element);
    var ngModelCtrl = ctrls[0] || {
      // Mock ngModelController if it doesn't exist to give us
      // the minimum functionality needed
      $setViewValue: function(val) {
        this.$viewValue = val;
        this.$viewChangeListeners.forEach(function(cb) { cb(); });
      },
      $parsers: [],
      $formatters: [],
      $viewChangeListeners: []
    };

    var containerCtrl = ctrls[1];
    var container = angular.element($mdUtil.getClosest(element, '_md-slider-container', true));
    var isDisabled = attr.ngDisabled ? angular.bind(null, $parse(attr.ngDisabled), scope.$parent) : function () {
          return element[0].hasAttribute('disabled');
        };

    var thumb = angular.element(element[0].querySelector('.md-thumb'));
    var thumbText = angular.element(element[0].querySelector('.md-thumb-text'));
    var thumbContainer = thumb.parent();
    var trackContainer = angular.element(element[0].querySelector('.md-track-container'));
    var activeTrack = angular.element(element[0].querySelector('.md-track-fill'));
    var tickContainer = angular.element(element[0].querySelector('.md-track-ticks'));
    var wrapper = angular.element(element[0].getElementsByClassName('md-slider-wrapper'));
    var content = angular.element(element[0].getElementsByClassName('md-slider-content'));
    var throttledRefreshDimensions = $mdUtil.throttle(refreshSliderDimensions, 5000);

    // Default values, overridable by attrs
    var DEFAULT_ROUND = 3;
    var vertical = angular.isDefined(attr.mdVertical);
    var discrete = angular.isDefined(attr.mdDiscrete);
    var invert = angular.isDefined(attr.mdInvert);
    angular.isDefined(attr.min) ? attr.$observe('min', updateMin) : updateMin(0);
    angular.isDefined(attr.max) ? attr.$observe('max', updateMax) : updateMax(100);
    angular.isDefined(attr.step)? attr.$observe('step', updateStep) : updateStep(1);
    angular.isDefined(attr.round)? attr.$observe('round', updateRound) : updateRound(DEFAULT_ROUND);

    // We have to manually stop the $watch on ngDisabled because it exists
    // on the parent scope, and won't be automatically destroyed when
    // the component is destroyed.
    var stopDisabledWatch = angular.noop;
    if (attr.ngDisabled) {
      stopDisabledWatch = scope.$parent.$watch(attr.ngDisabled, updateAriaDisabled);
    }

    $mdGesture.register(wrapper, 'drag', { horizontal: !vertical });

    scope.mouseActive = false;

    wrapper
      .on('keydown', keydownListener)
      .on('mousedown', mouseDownListener)
      .on('focus', focusListener)
      .on('blur', blurListener)
      .on('$md.pressdown', onPressDown)
      .on('$md.pressup', onPressUp)
      .on('$md.dragstart', onDragStart)
      .on('$md.drag', onDrag)
      .on('$md.dragend', onDragEnd);

    // On resize, recalculate the slider's dimensions and re-render
    function updateAll() {
      refreshSliderDimensions();
      ngModelRender();
    }
    setTimeout(updateAll, 0);

    var debouncedUpdateAll = $$rAF.throttle(updateAll);
    angular.element($window).on('resize', debouncedUpdateAll);

    scope.$on('$destroy', function() {
      angular.element($window).off('resize', debouncedUpdateAll);
    });

    ngModelCtrl.$render = ngModelRender;
    ngModelCtrl.$viewChangeListeners.push(ngModelRender);
    ngModelCtrl.$formatters.push(minMaxValidator);
    ngModelCtrl.$formatters.push(stepValidator);

    /**
     * Attributes
     */
    var min;
    var max;
    var step;
    var round;
    function updateMin(value) {
      min = parseFloat(value);
      ngModelCtrl.$viewValue = minMaxValidator(ngModelCtrl.$modelValue, min, max);
      wrapper.attr('aria-valuemin', value);
      updateAll();
    }
    function updateMax(value) {
      max = parseFloat(value);
      ngModelCtrl.$viewValue = minMaxValidator(ngModelCtrl.$modelValue, min, max);
      wrapper.attr('aria-valuemax', value);
      updateAll();
    }
    function updateStep(value) {
      step = parseFloat(value);
    }
    function updateRound(value) {
      // Set max round digits to 6, after 6 the input uses scientific notation
      round = minMaxValidator(parseInt(value), 0, 6);
    }
    function updateAriaDisabled() {
      element.attr('aria-disabled', !!isDisabled());
    }

    // Draw the ticks with canvas.
    // The alternative to drawing ticks with canvas is to draw one element for each tick,
    // which could quickly become a performance bottleneck.
    var tickCanvas, tickCtx;
    function redrawTicks() {
      if (!discrete || isDisabled()) return;
      if (angular.isUndefined(step))         return;

      if (step <= 0) {
        var msg = 'Slider step value must be greater than zero when in discrete mode';
        $log.error(msg);
        throw new Error(msg);
      }

      var numSteps = Math.floor((max - min) / step);
      if (!tickCanvas) {
        tickCanvas = angular.element('<canvas>').css('position', 'absolute');
        tickContainer.append(tickCanvas);

        tickCtx = tickCanvas[0].getContext('2d');
      }

      var dimensions = getSliderDimensions();

      // If `dimensions` doesn't have height and width it might be the first attempt so we will refresh dimensions
      if (dimensions && !dimensions.height && !dimensions.width) {
        refreshSliderDimensions();
        dimensions = sliderDimensions;
      }

      tickCanvas[0].width = dimensions.width;
      tickCanvas[0].height = dimensions.height;

      var distance;
      for (var i = 0; i <= numSteps; i++) {
        var trackTicksStyle = $window.getComputedStyle(tickContainer[0]);
        tickCtx.fillStyle = trackTicksStyle.color || 'black';

        distance = Math.floor((vertical ? dimensions.height : dimensions.width) * (i / numSteps));

        tickCtx.fillRect(vertical ? 0 : distance - 1,
          vertical ? distance - 1 : 0,
          vertical ? dimensions.width : 2,
          vertical ? 2 : dimensions.height);
      }
    }

    function clearTicks() {
      if (tickCanvas && tickCtx) {
        var dimensions = getSliderDimensions();
        tickCtx.clearRect(0, 0, dimensions.width, dimensions.height);
      }
    }

    /**
     * Refreshing Dimensions
     */
    var sliderDimensions = {};
    refreshSliderDimensions();
    function refreshSliderDimensions() {
      sliderDimensions = trackContainer[0].getBoundingClientRect();
    }
    function getSliderDimensions() {
      throttledRefreshDimensions();
      return sliderDimensions;
    }

    /**
     * left/right/up/down arrow listener
     * @param {!KeyboardEvent} ev
     */
    function keydownListener(ev) {
      if (isDisabled()) return;
      var keyCodes = $mdConstant.KEY_CODE;

      var changeAmount;
      switch (ev.keyCode) {
        case keyCodes.DOWN_ARROW:
        case keyCodes.LEFT_ARROW:
          ev.preventDefault();
          changeAmount = -step;
          break;
        case keyCodes.UP_ARROW:
        case keyCodes.RIGHT_ARROW:
          ev.preventDefault();
          changeAmount = step;
          break;
        case keyCodes.PAGE_DOWN:
          ev.preventDefault();
          changeAmount = -step * stepPageSize;
          break;
        case keyCodes.PAGE_UP:
          ev.preventDefault();
          changeAmount = step * stepPageSize;
          break;
        case keyCodes.HOME:
          ev.preventDefault();
          ev.stopPropagation();
          updateValue(min);
          break;
        case keyCodes.END:
          ev.preventDefault();
          ev.stopPropagation();
          updateValue(max);
          break;
      }
      if (changeAmount) {
        changeAmount = invert ? -changeAmount : changeAmount;
        if (ev.metaKey || ev.ctrlKey || ev.altKey) {
          changeAmount *= modifierMultiplier;
        }
        ev.preventDefault();
        ev.stopPropagation();
        updateValue(ngModelCtrl.$viewValue + changeAmount);
      }
    }

    /**
     * @param value new slider value used for setting the model value
     */
    function updateValue(value) {
      scope.$evalAsync(function() {
        setModelValue(value);
      });
    }

    function mouseDownListener() {
      redrawTicks();

      scope.mouseActive = true;
      wrapper.removeClass('md-focused');

      $timeout(function() {
        scope.mouseActive = false;
      }, 100);
    }

    function focusListener() {
      if (scope.mouseActive === false) {
        wrapper.addClass('md-focused');
      }
    }

    function blurListener() {
      wrapper.removeClass('md-focused');
      element.removeClass('md-active');
      clearTicks();
    }

    /**
     * ngModel setters and validators
     */
    function setModelValue(value) {
      ngModelCtrl.$setViewValue(minMaxValidator(stepValidator(value)));
    }
    function ngModelRender() {
      if (isNaN(ngModelCtrl.$viewValue)) {
        ngModelCtrl.$viewValue = ngModelCtrl.$modelValue;
      }

      ngModelCtrl.$viewValue = minMaxValidator(ngModelCtrl.$viewValue);

      var percent = valueToPercent(ngModelCtrl.$viewValue);
      scope.modelValue = ngModelCtrl.$viewValue;
      wrapper.attr('aria-valuenow', ngModelCtrl.$viewValue);
      setSliderPercent(percent);
      thumbText.text(ngModelCtrl.$viewValue);
    }

    function minMaxValidator(value, minValue, maxValue) {
      if (angular.isNumber(value)) {
        minValue = angular.isNumber(minValue) ? minValue : min;
        maxValue = angular.isNumber(maxValue) ? maxValue : max;

        return Math.max(minValue, Math.min(maxValue, value));
      }
    }

    function stepValidator(value) {
      if (angular.isNumber(value)) {
        var formattedValue = (Math.round((value - min) / step) * step + min);
        formattedValue = (Math.round(formattedValue * Math.pow(10, round)) / Math.pow(10, round));

        if (containerCtrl && containerCtrl.fitInputWidthToTextLength) {
          $mdUtil.debounce(function () {
            containerCtrl.fitInputWidthToTextLength(formattedValue.toString().length);
          }, 100)();
        }

        return formattedValue;
      }
    }

    /**
     * @param {number} percent 0-1
     */
    function setSliderPercent(percent) {

      percent = clamp(percent);

      var thumbPosition = (percent * 100) + '%';
      var activeTrackPercent = invert ? (1 - percent) * 100 + '%' : thumbPosition;

      if (vertical) {
        thumbContainer.css('bottom', thumbPosition);
      }
      else {
        $mdUtil.bidiProperty(thumbContainer, 'left', 'right', thumbPosition);
      }


      activeTrack.css(vertical ? 'height' : 'width', activeTrackPercent);

      element.toggleClass((invert ? 'md-max' : 'md-min'), percent === 0);
      element.toggleClass((invert ? 'md-min' : 'md-max'), percent === 1);
    }

    /**
     * Slide listeners
     */
    var isDragging = false;

    function onPressDown(ev) {
      if (isDisabled()) return;

      element.addClass('md-active');
      element[0].focus();
      refreshSliderDimensions();

      var exactVal = percentToValue(positionToPercent(vertical ? ev.pointer.y : ev.pointer.x));
      var closestVal = minMaxValidator(stepValidator(exactVal));
      scope.$apply(function() {
        setModelValue(closestVal);
        setSliderPercent(valueToPercent(closestVal));
      });
    }
    function onPressUp(ev) {
      if (isDisabled()) return;

      element.removeClass('md-dragging');

      var exactVal = percentToValue(positionToPercent(vertical ? ev.pointer.y : ev.pointer.x));
      var closestVal = minMaxValidator(stepValidator(exactVal));
      scope.$apply(function() {
        setModelValue(closestVal);
        ngModelRender();
      });
    }
    function onDragStart(ev) {
      if (isDisabled()) return;
      isDragging = true;

      ev.stopPropagation();

      element.addClass('md-dragging');
      setSliderFromEvent(ev);
    }
    function onDrag(ev) {
      if (!isDragging) return;
      ev.stopPropagation();
      setSliderFromEvent(ev);
    }
    function onDragEnd(ev) {
      if (!isDragging) return;
      ev.stopPropagation();
      isDragging = false;
    }

    function setSliderFromEvent(ev) {
      // While panning discrete, update only the
      // visual positioning but not the model value.
      if (discrete) adjustThumbPosition(vertical ? ev.pointer.y : ev.pointer.x);
      else            doSlide(vertical ? ev.pointer.y : ev.pointer.x);
    }

    /**
     * Slide the UI by changing the model value
     * @param x
     */
    function doSlide(x) {
      scope.$evalAsync(function() {
        setModelValue(percentToValue(positionToPercent(x)));
      });
    }

    /**
     * Slide the UI without changing the model (while dragging/panning)
     * @param x
     */
    function adjustThumbPosition(x) {
      var exactVal = percentToValue(positionToPercent(x));
      var closestVal = minMaxValidator(stepValidator(exactVal));
      setSliderPercent(positionToPercent(x));
      thumbText.text(closestVal);
    }

    /**
    * Clamps the value to be between 0 and 1.
    * @param {number} value The value to clamp.
    * @returns {number}
    */
    function clamp(value) {
      return Math.max(0, Math.min(value || 0, 1));
    }

    /**
     * Convert position on slider to percentage value of offset from beginning...
     * @param position
     * @returns {number}
     */
    function positionToPercent(position) {
      var offset = vertical ? sliderDimensions.top : sliderDimensions.left;
      var size = vertical ? sliderDimensions.height : sliderDimensions.width;
      var calc = (position - offset) / size;

      if (!vertical && $mdUtil.bidi() === 'rtl') {
        calc = 1 - calc;
      }

      return Math.max(0, Math.min(1, vertical ? 1 - calc : calc));
    }

    /**
     * Convert percentage offset on slide to equivalent model value
     * @param percent
     * @returns {*}
     */
    function percentToValue(percent) {
      var adjustedPercent = invert ? (1 - percent) : percent;
      return (min + adjustedPercent * (max - min));
    }

    function valueToPercent(val) {
      var percent = (val - min) / (max - min);
      return invert ? (1 - percent) : percent;
    }
  }
}

ngmaterial.components.slider = angular.module("material.components.slider");