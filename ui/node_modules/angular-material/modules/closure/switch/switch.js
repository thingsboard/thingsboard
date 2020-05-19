/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.switch');
goog.require('ngmaterial.components.checkbox');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.switch
 */

MdSwitch['$inject'] = ["mdCheckboxDirective", "$mdUtil", "$mdConstant", "$parse", "$$rAF", "$mdGesture", "$timeout"];
angular.module('material.components.switch', [
  'material.core',
  'material.components.checkbox'
])
  .directive('mdSwitch', MdSwitch);

/**
 * @ngdoc directive
 * @module material.components.switch
 * @name mdSwitch
 * @restrict E
 *
 * The switch directive is used very much like the normal [angular checkbox](https://docs.angularjs.org/api/ng/input/input%5Bcheckbox%5D).
 *
 * As per the [Material Design spec](https://material.io/archive/guidelines/style/color.html#color-color-system)
 * the switch is in the accent color by default. The primary color palette may be used with
 * the `md-primary` class.
 *
 * @param {expression} ng-model Assignable angular expression to data-bind to.
 * @param {string=} name Property name of the form under which the control is published.
 * @param {expression=} ng-true-value The value to which the expression should be set when selected.
 * @param {expression=} ng-false-value The value to which the expression should be set when not selected.
 * @param {expression=} ng-change Expression to be executed when the model value changes.
 * @param {expression=} ng-disabled En/Disable based on the expression.
 * @param {boolean=} md-no-ink Use of attribute indicates use of ripple ink effects.
 * @param {string=} aria-label Publish the button label used by screen-readers for accessibility. Defaults to the switch's text.
 * @param {boolean=} md-invert When set to true, the switch will be inverted.
 *
 * @usage
 * <hljs lang="html">
 * <md-switch ng-model="isActive" aria-label="Finished?">
 *   Finished ?
 * </md-switch>
 *
 * <md-switch md-no-ink ng-model="hasInk" aria-label="No Ink Effects">
 *   No Ink Effects
 * </md-switch>
 *
 * <md-switch ng-disabled="true" ng-model="isDisabled" aria-label="Disabled">
 *   Disabled
 * </md-switch>
 *
 * </hljs>
 */
function MdSwitch(mdCheckboxDirective, $mdUtil, $mdConstant, $parse, $$rAF, $mdGesture, $timeout) {
  var checkboxDirective = mdCheckboxDirective[0];

  return {
    restrict: 'E',
    priority: $mdConstant.BEFORE_NG_ARIA,
    transclude: true,
    template:
      '<div class="md-container">' +
        '<div class="md-bar"></div>' +
        '<div class="md-thumb-container">' +
          '<div class="md-thumb" md-ink-ripple md-ink-ripple-checkbox></div>' +
        '</div>'+
      '</div>' +
      '<div ng-transclude class="md-label"></div>',
    require: ['^?mdInputContainer', '?ngModel', '?^form'],
    compile: mdSwitchCompile
  };

  function mdSwitchCompile(element, attr) {
    var checkboxLink = checkboxDirective.compile(element, attr).post;
    // No transition on initial load.
    element.addClass('md-dragging');

    return function (scope, element, attr, ctrls) {
      var containerCtrl = ctrls[0];
      var ngModel = ctrls[1] || $mdUtil.fakeNgModel();
      var formCtrl = ctrls[2];

      var disabledGetter = null;
      if (attr.disabled != null) {
        disabledGetter = function() { return true; };
      } else if (attr.ngDisabled) {
        disabledGetter = $parse(attr.ngDisabled);
      }

      var thumbContainer = angular.element(element[0].querySelector('.md-thumb-container'));
      var switchContainer = angular.element(element[0].querySelector('.md-container'));
      var labelContainer = angular.element(element[0].querySelector('.md-label'));

      // no transition on initial load
      $$rAF(function() {
        element.removeClass('md-dragging');
      });

      checkboxLink(scope, element, attr, ctrls);

      if (disabledGetter) {
        scope.$watch(disabledGetter, function(isDisabled) {
          element.attr('tabindex', isDisabled ? -1 : 0);
        });
      }

      attr.$observe('mdInvert', function(newValue) {
        var isInverted = $mdUtil.parseAttributeBoolean(newValue);

        isInverted ? element.prepend(labelContainer) : element.prepend(switchContainer);

        // Toggle a CSS class to update the margin.
        element.toggleClass('md-inverted', isInverted);
      });

      // These events are triggered by setup drag
      $mdGesture.register(switchContainer, 'drag');
      switchContainer
        .on('$md.dragstart', onDragStart)
        .on('$md.drag', onDrag)
        .on('$md.dragend', onDragEnd);

      var drag;
      function onDragStart(ev) {
        // Don't go if the switch is disabled.
        if (disabledGetter && disabledGetter(scope)) return;
        ev.stopPropagation();

        element.addClass('md-dragging');
        drag = {width: thumbContainer.prop('offsetWidth')};
      }

      function onDrag(ev) {
        if (!drag) return;
        ev.stopPropagation();
        ev.srcEvent && ev.srcEvent.preventDefault();

        var percent = ev.pointer.distanceX / drag.width;

        // if checked, start from right. else, start from left
        var translate = ngModel.$viewValue ?  1 + percent : percent;
        // Make sure the switch stays inside its bounds, 0-1%
        translate = Math.max(0, Math.min(1, translate));

        thumbContainer.css($mdConstant.CSS.TRANSFORM, 'translate3d(' + (100*translate) + '%,0,0)');
        drag.translate = translate;
      }

      function onDragEnd(ev) {
        if (!drag) return;
        ev.stopPropagation();

        element.removeClass('md-dragging');
        thumbContainer.css($mdConstant.CSS.TRANSFORM, '');

        // We changed if there is no distance (this is a click a click),
        // or if the drag distance is >50% of the total.
        var isChanged = ngModel.$viewValue ? drag.translate < 0.5 : drag.translate > 0.5;
        if (isChanged) {
          applyModelValue(!ngModel.$viewValue);
        }
        drag = null;

        // Wait for incoming mouse click
        scope.skipToggle = true;
        $timeout(function() {
          scope.skipToggle = false;
        }, 1);
      }

      function applyModelValue(newValue) {
        scope.$apply(function() {
          ngModel.$setViewValue(newValue);
          ngModel.$render();
        });
      }

    };
  }


}

ngmaterial.components.switch = angular.module("material.components.switch");