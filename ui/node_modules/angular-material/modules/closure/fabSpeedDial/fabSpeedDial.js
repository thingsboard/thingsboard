/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.fabShared');
goog.require('ngmaterial.core');
(function() {
  'use strict';

  MdFabController['$inject'] = ["$scope", "$element", "$animate", "$mdUtil", "$mdConstant", "$timeout"];
  angular.module('material.components.fabShared', ['material.core'])
    .controller('MdFabController', MdFabController);

  function MdFabController($scope, $element, $animate, $mdUtil, $mdConstant, $timeout) {
    var vm = this;
    var initialAnimationAttempts = 0;

    // NOTE: We use async eval(s) below to avoid conflicts with any existing digest loops

    vm.open = function() {
      $scope.$evalAsync("vm.isOpen = true");
    };

    vm.close = function() {
      // Async eval to avoid conflicts with existing digest loops
      $scope.$evalAsync("vm.isOpen = false");

      // Focus the trigger when the element closes so users can still tab to the next item
      $element.find('md-fab-trigger')[0].focus();
    };

    // Toggle the open/close state when the trigger is clicked
    vm.toggle = function() {
      $scope.$evalAsync("vm.isOpen = !vm.isOpen");
    };

    /*
     * AngularJS Lifecycle hook for newer AngularJS versions.
     * Bindings are not guaranteed to have been assigned in the controller, but they are in the $onInit hook.
     */
    vm.$onInit = function() {
      setupDefaults();
      setupListeners();
      setupWatchers();

      fireInitialAnimations();
    };

    // For AngularJS 1.4 and older, where there are no lifecycle hooks but bindings are pre-assigned,
    // manually call the $onInit hook.
    if (angular.version.major === 1 && angular.version.minor <= 4) {
      this.$onInit();
    }

    function setupDefaults() {
      // Set the default direction to 'down' if none is specified
      vm.direction = vm.direction || 'down';

      // Set the default to be closed
      vm.isOpen = vm.isOpen || false;

      // Start the keyboard interaction at the first action
      resetActionIndex();

      // Add an animations waiting class so we know not to run
      $element.addClass('md-animations-waiting');
    }

    function setupListeners() {
      var eventTypes = [
        'click', 'focusin', 'focusout'
      ];

      // Add our listeners
      angular.forEach(eventTypes, function(eventType) {
        $element.on(eventType, parseEvents);
      });

      // Remove our listeners when destroyed
      $scope.$on('$destroy', function() {
        angular.forEach(eventTypes, function(eventType) {
          $element.off(eventType, parseEvents);
        });

        // remove any attached keyboard handlers in case element is removed while
        // speed dial is open
        disableKeyboard();
      });
    }

    var closeTimeout;
    function parseEvents(event) {
      // If the event is a click, just handle it
      if (event.type == 'click') {
        handleItemClick(event);
      }

      // If we focusout, set a timeout to close the element
      if (event.type == 'focusout' && !closeTimeout) {
        closeTimeout = $timeout(function() {
          vm.close();
        }, 100, false);
      }

      // If we see a focusin and there is a timeout about to run, cancel it so we stay open
      if (event.type == 'focusin' && closeTimeout) {
        $timeout.cancel(closeTimeout);
        closeTimeout = null;
      }
    }

    function resetActionIndex() {
      vm.currentActionIndex = -1;
    }

    function setupWatchers() {
      // Watch for changes to the direction and update classes/attributes
      $scope.$watch('vm.direction', function(newDir, oldDir) {
        // Add the appropriate classes so we can target the direction in the CSS
        $animate.removeClass($element, 'md-' + oldDir);
        $animate.addClass($element, 'md-' + newDir);

        // Reset the action index since it may have changed
        resetActionIndex();
      });

      var trigger, actions;

      // Watch for changes to md-open
      $scope.$watch('vm.isOpen', function(isOpen) {
        // Reset the action index since it may have changed
        resetActionIndex();

        // We can't get the trigger/actions outside of the watch because the component hasn't been
        // linked yet, so we wait until the first watch fires to cache them.
        if (!trigger || !actions) {
          trigger = getTriggerElement();
          actions = getActionsElement();
        }

        if (isOpen) {
          enableKeyboard();
        } else {
          disableKeyboard();
        }

        var toAdd = isOpen ? 'md-is-open' : '';
        var toRemove = isOpen ? '' : 'md-is-open';

        // Set the proper ARIA attributes
        trigger.attr('aria-haspopup', true);
        trigger.attr('aria-expanded', isOpen);
        actions.attr('aria-hidden', !isOpen);

        // Animate the CSS classes
        $animate.setClass($element, toAdd, toRemove);
      });
    }

    function fireInitialAnimations() {
      // If the element is actually visible on the screen
      if ($element[0].scrollHeight > 0) {
        // Fire our animation
        $animate.addClass($element, '_md-animations-ready').then(function() {
          // Remove the waiting class
          $element.removeClass('md-animations-waiting');
        });
      }

      // Otherwise, try for up to 1 second before giving up
      else if (initialAnimationAttempts < 10) {
        $timeout(fireInitialAnimations, 100);

        // Increment our counter
        initialAnimationAttempts = initialAnimationAttempts + 1;
      }
    }

    function enableKeyboard() {
      $element.on('keydown', keyPressed);

      // On the next tick, setup a check for outside clicks; we do this on the next tick to avoid
      // clicks/touches that result in the isOpen attribute changing (e.g. a bound radio button)
      $mdUtil.nextTick(function() {
        angular.element(document).on('click touchend', checkForOutsideClick);
      });

      // TODO: On desktop, we should be able to reset the indexes so you cannot tab through, but
      // this breaks accessibility, especially on mobile, since you have no arrow keys to press
      // resetActionTabIndexes();
    }

    function disableKeyboard() {
      $element.off('keydown', keyPressed);
      angular.element(document).off('click touchend', checkForOutsideClick);
    }

    function checkForOutsideClick(event) {
      if (event.target) {
        var closestTrigger = $mdUtil.getClosest(event.target, 'md-fab-trigger');
        var closestActions = $mdUtil.getClosest(event.target, 'md-fab-actions');

        if (!closestTrigger && !closestActions) {
          vm.close();
        }
      }
    }

    function keyPressed(event) {
      switch (event.which) {
        case $mdConstant.KEY_CODE.ESCAPE: vm.close(); event.preventDefault(); return false;
        case $mdConstant.KEY_CODE.LEFT_ARROW: doKeyLeft(event); return false;
        case $mdConstant.KEY_CODE.UP_ARROW: doKeyUp(event); return false;
        case $mdConstant.KEY_CODE.RIGHT_ARROW: doKeyRight(event); return false;
        case $mdConstant.KEY_CODE.DOWN_ARROW: doKeyDown(event); return false;
      }
    }

    function doActionPrev(event) {
      focusAction(event, -1);
    }

    function doActionNext(event) {
      focusAction(event, 1);
    }

    function focusAction(event, direction) {
      var actions = resetActionTabIndexes();

      // Increment/decrement the counter with restrictions
      vm.currentActionIndex = vm.currentActionIndex + direction;
      vm.currentActionIndex = Math.min(actions.length - 1, vm.currentActionIndex);
      vm.currentActionIndex = Math.max(0, vm.currentActionIndex);

      // Focus the element
      var focusElement =  angular.element(actions[vm.currentActionIndex]).children()[0];
      angular.element(focusElement).attr('tabindex', 0);
      focusElement.focus();

      // Make sure the event doesn't bubble and cause something else
      event.preventDefault();
      event.stopImmediatePropagation();
    }

    function resetActionTabIndexes() {
      // Grab all of the actions
      var actions = getActionsElement()[0].querySelectorAll('.md-fab-action-item');

      // Disable all other actions for tabbing
      angular.forEach(actions, function(action) {
        angular.element(angular.element(action).children()[0]).attr('tabindex', -1);
      });

      return actions;
    }

    function doKeyLeft(event) {
      if (vm.direction === 'left') {
        doActionNext(event);
      } else {
        doActionPrev(event);
      }
    }

    function doKeyUp(event) {
      if (vm.direction === 'down') {
        doActionPrev(event);
      } else {
        doActionNext(event);
      }
    }

    function doKeyRight(event) {
      if (vm.direction === 'left') {
        doActionPrev(event);
      } else {
        doActionNext(event);
      }
    }

    function doKeyDown(event) {
      if (vm.direction === 'up') {
        doActionPrev(event);
      } else {
        doActionNext(event);
      }
    }

    function isTrigger(element) {
      return $mdUtil.getClosest(element, 'md-fab-trigger');
    }

    function isAction(element) {
      return $mdUtil.getClosest(element, 'md-fab-actions');
    }

    function handleItemClick(event) {
      if (isTrigger(event.target)) {
        vm.toggle();
      }

      if (isAction(event.target)) {
        vm.close();
      }
    }

    function getTriggerElement() {
      return $element.find('md-fab-trigger');
    }

    function getActionsElement() {
      return $element.find('md-fab-actions');
    }
  }
})();

(function() {
  'use strict';

  /**
   * The duration of the CSS animation in milliseconds.
   *
   * @type {number}
   */
  MdFabSpeedDialFlingAnimation['$inject'] = ["$timeout"];
  MdFabSpeedDialScaleAnimation['$inject'] = ["$timeout"];
  var cssAnimationDuration = 300;

  /**
   * @ngdoc module
   * @name material.components.fabSpeedDial
   */
  angular
    // Declare our module
    .module('material.components.fabSpeedDial', [
      'material.core',
      'material.components.fabShared',
      'material.components.fabActions'
    ])

    // Register our directive
    .directive('mdFabSpeedDial', MdFabSpeedDialDirective)

    // Register our custom animations
    .animation('.md-fling', MdFabSpeedDialFlingAnimation)
    .animation('.md-scale', MdFabSpeedDialScaleAnimation)

    // Register a service for each animation so that we can easily inject them into unit tests
    .service('mdFabSpeedDialFlingAnimation', MdFabSpeedDialFlingAnimation)
    .service('mdFabSpeedDialScaleAnimation', MdFabSpeedDialScaleAnimation);

  /**
   * @ngdoc directive
   * @name mdFabSpeedDial
   * @module material.components.fabSpeedDial
   *
   * @restrict E
   *
   * @description
   * The `<md-fab-speed-dial>` directive is used to present a series of popup elements (usually
   * `<md-button>`s) for quick access to common actions.
   *
   * There are currently two animations available by applying one of the following classes to
   * the component:
   *
   *  - `md-fling` - The speed dial items appear from underneath the trigger and move into their
   *    appropriate positions.
   *  - `md-scale` - The speed dial items appear in their proper places by scaling from 0% to 100%.
   *
   * You may also easily position the trigger by applying one one of the following classes to the
   * `<md-fab-speed-dial>` element:
   *  - `md-fab-top-left`
   *  - `md-fab-top-right`
   *  - `md-fab-bottom-left`
   *  - `md-fab-bottom-right`
   *
   * These CSS classes use `position: absolute`, so you need to ensure that the container element
   * also uses `position: absolute` or `position: relative` in order for them to work.
   *
   * Additionally, you may use the standard `ng-mouseenter` and `ng-mouseleave` directives to
   * open or close the speed dial. However, if you wish to allow users to hover over the empty
   * space where the actions will appear, you must also add the `md-hover-full` class to the speed
   * dial element. Without this, the hover effect will only occur on top of the trigger.
   *
   * See the demos for more information.
   *
   * ## Troubleshooting
   *
   * If your speed dial shows the closing animation upon launch, you may need to use `ng-cloak` on
   * the parent container to ensure that it is only visible once ready. We have plans to remove this
   * necessity in the future.
   *
   * @usage
   * <hljs lang="html">
   * <md-fab-speed-dial md-direction="up" class="md-fling">
   *   <md-fab-trigger>
   *     <md-button aria-label="Add..."><md-icon md-svg-src="/img/icons/plus.svg"></md-icon></md-button>
   *   </md-fab-trigger>
   *
   *   <md-fab-actions>
   *     <md-button aria-label="Add User">
   *       <md-icon md-svg-src="/img/icons/user.svg"></md-icon>
   *     </md-button>
   *
   *     <md-button aria-label="Add Group">
   *       <md-icon md-svg-src="/img/icons/group.svg"></md-icon>
   *     </md-button>
   *   </md-fab-actions>
   * </md-fab-speed-dial>
   * </hljs>
   *
   * @param {string} md-direction From which direction you would like the speed dial to appear
   * relative to the trigger element.
   * @param {expression=} md-open Programmatically control whether or not the speed-dial is visible.
   */
  function MdFabSpeedDialDirective() {
    return {
      restrict: 'E',

      scope: {
        direction: '@?mdDirection',
        isOpen: '=?mdOpen'
      },

      bindToController: true,
      controller: 'MdFabController',
      controllerAs: 'vm',

      link: FabSpeedDialLink
    };

    function FabSpeedDialLink(scope, element) {
      // Prepend an element to hold our CSS variables so we can use them in the animations below
      element.prepend('<div class="_md-css-variables"></div>');
    }
  }

  function MdFabSpeedDialFlingAnimation($timeout) {
    function delayDone(done) { $timeout(done, cssAnimationDuration, false); }

    function runAnimation(element) {
      // Don't run if we are still waiting and we are not ready
      if (element.hasClass('md-animations-waiting') && !element.hasClass('_md-animations-ready')) {
        return;
      }

      var el = element[0];
      var ctrl = element.controller('mdFabSpeedDial');
      var items = el.querySelectorAll('.md-fab-action-item');

      // Grab our trigger element
      var triggerElement = el.querySelector('md-fab-trigger');

      // Grab our element which stores CSS variables
      var variablesElement = el.querySelector('._md-css-variables');

      // Setup JS variables based on our CSS variables
      var startZIndex = parseInt(window.getComputedStyle(variablesElement).zIndex);

      // Always reset the items to their natural position/state
      angular.forEach(items, function(item, index) {
        var styles = item.style;

        styles.transform = styles.webkitTransform = '';
        styles.transitionDelay = '';
        styles.opacity = 1;

        // Make the items closest to the trigger have the highest z-index
        styles.zIndex = (items.length - index) + startZIndex;
      });

      // Set the trigger to be above all of the actions so they disappear behind it.
      triggerElement.style.zIndex = startZIndex + items.length + 1;

      // If the control is closed, hide the items behind the trigger
      if (!ctrl.isOpen) {
        angular.forEach(items, function(item, index) {
          var newPosition, axis;
          var styles = item.style;

          // Make sure to account for differences in the dimensions of the trigger verses the items
          // so that we can properly center everything; this helps hide the item's shadows behind
          // the trigger.
          var triggerItemHeightOffset = (triggerElement.clientHeight - item.clientHeight) / 2;
          var triggerItemWidthOffset = (triggerElement.clientWidth - item.clientWidth) / 2;

          switch (ctrl.direction) {
            case 'up':
              newPosition = (item.scrollHeight * (index + 1) + triggerItemHeightOffset);
              axis = 'Y';
              break;
            case 'down':
              newPosition = -(item.scrollHeight * (index + 1) + triggerItemHeightOffset);
              axis = 'Y';
              break;
            case 'left':
              newPosition = (item.scrollWidth * (index + 1) + triggerItemWidthOffset);
              axis = 'X';
              break;
            case 'right':
              newPosition = -(item.scrollWidth * (index + 1) + triggerItemWidthOffset);
              axis = 'X';
              break;
          }

          var newTranslate = 'translate' + axis + '(' + newPosition + 'px)';

          styles.transform = styles.webkitTransform = newTranslate;
        });
      }
    }

    return {
      addClass: function(element, className, done) {
        if (element.hasClass('md-fling')) {
          runAnimation(element);
          delayDone(done);
        } else {
          done();
        }
      },
      removeClass: function(element, className, done) {
        runAnimation(element);
        delayDone(done);
      }
    };
  }

  function MdFabSpeedDialScaleAnimation($timeout) {
    function delayDone(done) { $timeout(done, cssAnimationDuration, false); }

    var delay = 65;

    function runAnimation(element) {
      var el = element[0];
      var ctrl = element.controller('mdFabSpeedDial');
      var items = el.querySelectorAll('.md-fab-action-item');

      // Grab our element which stores CSS variables
      var variablesElement = el.querySelector('._md-css-variables');

      // Setup JS variables based on our CSS variables
      var startZIndex = parseInt(window.getComputedStyle(variablesElement).zIndex);

      // Always reset the items to their natural position/state
      angular.forEach(items, function(item, index) {
        var styles = item.style,
          offsetDelay = index * delay;

        styles.opacity = ctrl.isOpen ? 1 : 0;
        styles.transform = styles.webkitTransform = ctrl.isOpen ? 'scale(1)' : 'scale(0)';
        styles.transitionDelay = (ctrl.isOpen ? offsetDelay : (items.length - offsetDelay)) + 'ms';

        // Make the items closest to the trigger have the highest z-index
        styles.zIndex = (items.length - index) + startZIndex;
      });
    }

    return {
      addClass: function(element, className, done) {
        runAnimation(element);
        delayDone(done);
      },

      removeClass: function(element, className, done) {
        runAnimation(element);
        delayDone(done);
      }
    };
  }
})();

ngmaterial.components.fabShared = angular.module("material.components.fabShared");