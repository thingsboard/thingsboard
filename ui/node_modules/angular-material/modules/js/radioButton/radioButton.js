/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
(function( window, angular, undefined ){
"use strict";

/**
 * @ngdoc module
 * @name material.components.radioButton
 * @description radioButton module!
 */
mdRadioGroupDirective['$inject'] = ["$mdUtil", "$mdConstant", "$mdTheming", "$timeout"];
mdRadioButtonDirective['$inject'] = ["$mdAria", "$mdUtil", "$mdTheming"];
angular.module('material.components.radioButton', [
  'material.core'
])
  .directive('mdRadioGroup', mdRadioGroupDirective)
  .directive('mdRadioButton', mdRadioButtonDirective);

/**
 * @ngdoc directive
 * @module material.components.radioButton
 * @name mdRadioGroup
 *
 * @restrict E
 *
 * @description
 * The `<md-radio-group>` directive identifies a grouping
 * container for the 1..n grouped radio buttons; specified using nested
 * `<md-radio-button>` elements.
 *
 * The radio button uses the accent color by default. The primary color palette may be used with
 * the `md-primary` class.
 *
 * Note: `<md-radio-group>` and `<md-radio-button>` handle `tabindex` differently
 * than the native `<input type="radio">` controls. Whereas the native controls
 * force the user to tab through all the radio buttons, `<md-radio-group>`
 * is focusable and by default the `<md-radio-button>`s are not.
 *
 * @param {string} ng-model Assignable angular expression to data-bind to.
 * @param {string=} ng-change AngularJS expression to be executed when input changes due to user
 *    interaction.
 * @param {boolean=} md-no-ink If present, disables ink ripple effects.
 *
 * @usage
 * <hljs lang="html">
 * <md-radio-group ng-model="selected">
 *   <md-radio-button ng-repeat="item in items"
 *                    ng-value="item.value" aria-label="{{item.label}}">
 *     {{ item.label }}
 *   </md-radio-button>
 * </md-radio-group>
 * </hljs>
 */
function mdRadioGroupDirective($mdUtil, $mdConstant, $mdTheming, $timeout) {
  RadioGroupController.prototype = createRadioGroupControllerProto();

  return {
    restrict: 'E',
    controller: ['$element', RadioGroupController],
    require: ['mdRadioGroup', '?ngModel'],
    link: { pre: linkRadioGroup }
  };

  function linkRadioGroup(scope, element, attr, ctrls) {
    element.addClass('_md');     // private md component indicator for styling
    $mdTheming(element);

    var rgCtrl = ctrls[0];
    var ngModelCtrl = ctrls[1] || $mdUtil.fakeNgModel();

    rgCtrl.init(ngModelCtrl);

    scope.mouseActive = false;

    element
      .attr({
        'role': 'radiogroup',
        'tabIndex': element.attr('tabindex') || '0'
      })
      .on('keydown', keydownListener)
      .on('mousedown', function(event) {
        scope.mouseActive = true;
        $timeout(function() {
          scope.mouseActive = false;
        }, 100);
      })
      .on('focus', function() {
        if (scope.mouseActive === false) {
          rgCtrl.$element.addClass('md-focused');
        }
      })
      .on('blur', function() {
        rgCtrl.$element.removeClass('md-focused');
      });

    /**
     *
     */
    function setFocus() {
      if (!element.hasClass('md-focused')) { element.addClass('md-focused'); }
    }

    /**
     *
     */
    function keydownListener(ev) {
      var keyCode = ev.which || ev.keyCode;

      // Only listen to events that we originated ourselves
      // so that we don't trigger on things like arrow keys in
      // inputs.

      if (keyCode != $mdConstant.KEY_CODE.ENTER &&
          ev.currentTarget != ev.target) {
        return;
      }

      switch (keyCode) {
        case $mdConstant.KEY_CODE.LEFT_ARROW:
        case $mdConstant.KEY_CODE.UP_ARROW:
          ev.preventDefault();
          rgCtrl.selectPrevious();
          setFocus();
          break;

        case $mdConstant.KEY_CODE.RIGHT_ARROW:
        case $mdConstant.KEY_CODE.DOWN_ARROW:
          ev.preventDefault();
          rgCtrl.selectNext();
          setFocus();
          break;

        case $mdConstant.KEY_CODE.ENTER:
          var form = angular.element($mdUtil.getClosest(element[0], 'form'));
          if (form.length > 0) {
            form.triggerHandler('submit');
          }
          break;
      }

    }
  }

  function RadioGroupController($element) {
    this._radioButtonRenderFns = [];
    this.$element = $element;
  }

  function createRadioGroupControllerProto() {
    return {
      init: function(ngModelCtrl) {
        this._ngModelCtrl = ngModelCtrl;
        this._ngModelCtrl.$render = angular.bind(this, this.render);
      },
      add: function(rbRender) {
        this._radioButtonRenderFns.push(rbRender);
      },
      remove: function(rbRender) {
        var index = this._radioButtonRenderFns.indexOf(rbRender);
        if (index !== -1) {
          this._radioButtonRenderFns.splice(index, 1);
        }
      },
      render: function() {
        this._radioButtonRenderFns.forEach(function(rbRender) {
          rbRender();
        });
      },
      setViewValue: function(value, eventType) {
        this._ngModelCtrl.$setViewValue(value, eventType);
        // update the other radio buttons as well
        this.render();
      },
      getViewValue: function() {
        return this._ngModelCtrl.$viewValue;
      },
      selectNext: function() {
        return changeSelectedButton(this.$element, 1);
      },
      selectPrevious: function() {
        return changeSelectedButton(this.$element, -1);
      },
      setActiveDescendant: function (radioId) {
        this.$element.attr('aria-activedescendant', radioId);
      },
      isDisabled: function() {
        return this.$element[0].hasAttribute('disabled');
      }
    };
  }
  /**
   * Change the radio group's selected button by a given increment.
   * If no button is selected, select the first button.
   */
  function changeSelectedButton(parent, increment) {
    // Coerce all child radio buttons into an array, then wrap then in an iterator
    var buttons = $mdUtil.iterator(parent[0].querySelectorAll('md-radio-button'), true);

    if (buttons.count()) {
      var validate = function (button) {
        // If disabled, then NOT valid
        return !angular.element(button).attr("disabled");
      };

      var selected = parent[0].querySelector('md-radio-button.md-checked');
      var target = buttons[increment < 0 ? 'previous' : 'next'](selected, validate) || buttons.first();

      // Activate radioButton's click listener (triggerHandler won't create a real click event)
      angular.element(target).triggerHandler('click');
    }
  }

}

/**
 * @ngdoc directive
 * @module material.components.radioButton
 * @name mdRadioButton
 *
 * @restrict E
 *
 * @description
 * The `<md-radio-button>`directive is the child directive required to be used within `<md-radio-group>` elements.
 *
 * While similar to the `<input type="radio" ng-model="" value="">` directive,
 * the `<md-radio-button>` directive provides ink effects, ARIA support, and
 * supports use within named radio groups.
 *
 * One of `value` or `ng-value` must be set so that the `md-radio-group`'s model is set properly when the
 * `md-radio-button` is selected.
 *
 * @param {string} value The value to which the model should be set when selected.
 * @param {string} ng-value AngularJS expression which sets the value to which the model should
 *    be set when selected.
 * @param {string=} name Property name of the form under which the control is published.
 * @param {string=} aria-label Adds label to radio button for accessibility.
 *    Defaults to radio button's text. If no text content is available, a warning will be logged.
 *
 * @usage
 * <hljs lang="html">
 *
 * <md-radio-button value="1" aria-label="Label 1">
 *   Label 1
 * </md-radio-button>
 *
 * <md-radio-button ng-value="specialValue" aria-label="Green">
 *   Green
 * </md-radio-button>
 *
 * </hljs>
 *
 */
function mdRadioButtonDirective($mdAria, $mdUtil, $mdTheming) {

  var CHECKED_CSS = 'md-checked';

  return {
    restrict: 'E',
    require: '^mdRadioGroup',
    transclude: true,
    template: '<div class="md-container" md-ink-ripple md-ink-ripple-checkbox>' +
                '<div class="md-off"></div>' +
                '<div class="md-on"></div>' +
              '</div>' +
              '<div ng-transclude class="md-label"></div>',
    link: link
  };

  function link(scope, element, attr, rgCtrl) {
    var lastChecked;

    $mdTheming(element);
    configureAria(element, scope);

    // ngAria overwrites the aria-checked inside a $watch for ngValue.
    // We should defer the initialization until all the watches have fired.
    // This can also be fixed by removing the `lastChecked` check, but that'll
    // cause more DOM manipulation on each digest.
    if (attr.ngValue) {
      $mdUtil.nextTick(initialize, false);
    } else {
      initialize();
    }

    /**
     * Initializes the component.
     */
    function initialize() {
      if (!rgCtrl) {
        throw 'RadioButton: No RadioGroupController could be found.';
      }

      rgCtrl.add(render);
      attr.$observe('value', render);

      element
        .on('click', listener)
        .on('$destroy', function() {
          rgCtrl.remove(render);
        });
    }

    /**
     * On click functionality.
     */
    function listener(ev) {
      if (element[0].hasAttribute('disabled') || rgCtrl.isDisabled()) return;

      scope.$apply(function() {
        rgCtrl.setViewValue(attr.value, ev && ev.type);
      });
    }

    /**
     *  Add or remove the `.md-checked` class from the RadioButton (and conditionally its parent).
     *  Update the `aria-activedescendant` attribute.
     */
    function render() {
      var checked = rgCtrl.getViewValue() == attr.value;

      if (checked === lastChecked) return;

      if (element[0].parentNode.nodeName.toLowerCase() !== 'md-radio-group') {
        // If the radioButton is inside a div, then add class so highlighting will work
        element.parent().toggleClass(CHECKED_CSS, checked);
      }

      if (checked) {
        rgCtrl.setActiveDescendant(element.attr('id'));
      }

      lastChecked = checked;

      element
        .attr('aria-checked', checked)
        .toggleClass(CHECKED_CSS, checked);
    }

    /**
     * Inject ARIA-specific attributes appropriate for each radio button
     */
    function configureAria(element, scope){
      element.attr({
        id: attr.id || 'radio_' + $mdUtil.nextUid(),
        role: 'radio',
        'aria-checked': 'false'
      });

      $mdAria.expectWithText(element, 'aria-label');
    }
  }
}

})(window, window.angular);