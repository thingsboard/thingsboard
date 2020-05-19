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
 * @name material.components.bottomSheet
 * @description
 * BottomSheet
 */
MdBottomSheetDirective['$inject'] = ["$mdBottomSheet"];
MdBottomSheetProvider['$inject'] = ["$$interimElementProvider"];
angular
  .module('material.components.bottomSheet', [
    'material.core',
    'material.components.backdrop'
  ])
  .directive('mdBottomSheet', MdBottomSheetDirective)
  .provider('$mdBottomSheet', MdBottomSheetProvider);

/* ngInject */
function MdBottomSheetDirective($mdBottomSheet) {
  return {
    restrict: 'E',
    link : function postLink(scope, element) {
      element.addClass('_md');     // private md component indicator for styling

      // When navigation force destroys an interimElement, then
      // listen and $destroy() that interim instance...
      scope.$on('$destroy', function() {
        $mdBottomSheet.destroy();
      });
    }
  };
}


/**
 * @ngdoc service
 * @name $mdBottomSheet
 * @module material.components.bottomSheet
 *
 * @description
 * `$mdBottomSheet` opens a bottom sheet over the app and provides a simple promise API.
 *
 * ## Restrictions
 *
 * - The bottom sheet's template must have an outer `<md-bottom-sheet>` element.
 * - Add the `md-grid` class to the bottom sheet for a grid layout.
 * - Add the `md-list` class to the bottom sheet for a list layout.
 *
 * @usage
 * <hljs lang="html">
 * <div ng-controller="MyController">
 *   <md-button ng-click="openBottomSheet()">
 *     Open a Bottom Sheet!
 *   </md-button>
 * </div>
 * </hljs>
 * <hljs lang="js">
 * var app = angular.module('app', ['ngMaterial']);
 * app.controller('MyController', function($scope, $mdBottomSheet) {
 *   $scope.openBottomSheet = function() {
 *     $mdBottomSheet.show({
 *       template: '<md-bottom-sheet>' +
 *       'Hello! <md-button ng-click="closeBottomSheet()">Close</md-button>' +
 *       '</md-bottom-sheet>'
 *     })
 *
 *     // Fires when the hide() method is used
 *     .then(function() {
 *       console.log('You clicked the button to close the bottom sheet!');
 *     })
 *
 *     // Fires when the cancel() method is used
 *     .catch(function() {
 *       console.log('You hit escape or clicked the backdrop to close.');
 *     });
 *   };
 *
 *   $scope.closeBottomSheet = function($scope, $mdBottomSheet) {
 *     $mdBottomSheet.hide();
 *   }
 *
 * });
 * </hljs>
 *
 * ### Custom Presets
 * Developers are also able to create their own preset, which can be easily used without repeating
 * their options each time.
 *
 * <hljs lang="js">
 *   $mdBottomSheetProvider.addPreset('testPreset', {
 *     options: function() {
 *       return {
 *         template:
 *           '<md-bottom-sheet>' +
 *             'This is a custom preset' +
 *           '</md-bottom-sheet>',
 *         controllerAs: 'bottomSheet',
 *         bindToController: true,
 *         clickOutsideToClose: true,
 *         escapeToClose: true
 *       };
 *     }
 *   });
 * </hljs>
 *
 * After you create your preset during the config phase, you can easily access it.
 *
 * <hljs lang="js">
 *   $mdBottomSheet.show(
 *     $mdBottomSheet.testPreset()
 *   );
 * </hljs>
 */

 /**
 * @ngdoc method
 * @name $mdBottomSheet#show
 *
 * @description
 * Show a bottom sheet with the specified options.
 *
 * <em><b>Note:</b> You should <b>always</b> provide a `.catch()` method in case the user hits the
 * `esc` key or clicks the background to close. In this case, the `cancel()` method will
 * automatically be called on the bottom sheet which will `reject()` the promise. See the @usage
 * section above for an example.
 *
 * Newer versions of Angular will throw a `Possibly unhandled rejection` exception if you forget
 * this.</em>
 *
 * @param {object} optionsOrPreset Either provide an `$mdBottomSheetPreset` defined during the config phase or
 * an options object, with the following properties:
 *
 *   - `templateUrl` - `{string=}`: The url of an html template file that will
 *   be used as the content of the bottom sheet. Restrictions: the template must
 *   have an outer `md-bottom-sheet` element.
 *   - `template` - `{string=}`: Same as templateUrl, except this is an actual
 *   template string.
 *   - `scope` - `{object=}`: the scope to link the template / controller to. If none is specified, it will create a new child scope.
 *     This scope will be destroyed when the bottom sheet is removed unless `preserveScope` is set to true.
 *   - `preserveScope` - `{boolean=}`: whether to preserve the scope when the element is removed. Default is false
 *   - `controller` - `{string=}`: The controller to associate with this bottom sheet.
 *   - `locals` - `{string=}`: An object containing key/value pairs. The keys will
 *   be used as names of values to inject into the controller. For example,
 *   `locals: {three: 3}` would inject `three` into the controller with the value
 *   of 3.
 *   - `clickOutsideToClose` - `{boolean=}`: Whether the user can click outside the bottom sheet to
 *     close it. Default true.
 *   - `bindToController` - `{boolean=}`: When set to true, the locals will be bound to the controller instance.
 *   - `disableBackdrop` - `{boolean=}`: When set to true, the bottomsheet will not show a backdrop.
 *   - `escapeToClose` - `{boolean=}`: Whether the user can press escape to close the bottom sheet.
 *     Default true.
 *   - `isLockedOpen` - `{boolean=}`: Disables all default ways of closing the bottom sheet. **Note:** this will override
 *     the `clickOutsideToClose` and `escapeToClose` options, leaving only the `hide` and `cancel`
 *     methods as ways of closing the bottom sheet. Defaults to false.
 *   - `resolve` - `{object=}`: Similar to locals, except it takes promises as values
 *   and the bottom sheet will not open until the promises resolve.
 *   - `controllerAs` - `{string=}`: An alias to assign the controller to on the scope.
 *   - `parent` - `{element=}`: The element to append the bottom sheet to. The `parent` may be a `function`, `string`,
 *   `object`, or null. Defaults to appending to the body of the root element (or the root element) of the application.
 *   e.g. angular.element(document.getElementById('content')) or "#content"
 *   - `disableParentScroll` - `{boolean=}`: Whether to disable scrolling while the bottom sheet is open.
 *     Default true.
 *
 * @returns {promise} A promise that can be resolved with `$mdBottomSheet.hide()` or
 * rejected with `$mdBottomSheet.cancel()`.
 */

/**
 * @ngdoc method
 * @name $mdBottomSheet#hide
 *
 * @description
 * Hide the existing bottom sheet and resolve the promise returned from
 * `$mdBottomSheet.show()`. This call will close the most recently opened/current bottomsheet (if
 * any).
 *
 * <em><b>Note:</b> Use a `.then()` on your `.show()` to handle this callback.</em>
 *
 * @param {*=} response An argument for the resolved promise.
 *
 */

/**
 * @ngdoc method
 * @name $mdBottomSheet#cancel
 *
 * @description
 * Hide the existing bottom sheet and reject the promise returned from
 * `$mdBottomSheet.show()`.
 *
 * <em><b>Note:</b> Use a `.catch()` on your `.show()` to handle this callback.</em>
 *
 * @param {*=} response An argument for the rejected promise.
 *
 */

function MdBottomSheetProvider($$interimElementProvider) {
  // how fast we need to flick down to close the sheet, pixels/ms
  bottomSheetDefaults['$inject'] = ["$animate", "$mdConstant", "$mdUtil", "$mdTheming", "$mdBottomSheet", "$rootElement", "$mdGesture", "$log"];
  var CLOSING_VELOCITY = 0.5;
  var PADDING = 80; // same as css

  return $$interimElementProvider('$mdBottomSheet')
    .setDefaults({
      methods: ['disableParentScroll', 'escapeToClose', 'clickOutsideToClose'],
      options: bottomSheetDefaults
    });

  /* ngInject */
  function bottomSheetDefaults($animate, $mdConstant, $mdUtil, $mdTheming, $mdBottomSheet, $rootElement,
                               $mdGesture, $log) {
    var backdrop;

    return {
      themable: true,
      onShow: onShow,
      onRemove: onRemove,
      disableBackdrop: false,
      escapeToClose: true,
      clickOutsideToClose: true,
      disableParentScroll: true,
      isLockedOpen: false
    };


    function onShow(scope, element, options, controller) {

      element = $mdUtil.extractElementByName(element, 'md-bottom-sheet');

      // prevent tab focus or click focus on the bottom-sheet container
      element.attr('tabindex', '-1');

      // Once the md-bottom-sheet has `ng-cloak` applied on his template the opening animation will not work properly.
      // This is a very common problem, so we have to notify the developer about this.
      if (element.hasClass('ng-cloak')) {
        var message = '$mdBottomSheet: using `<md-bottom-sheet ng-cloak>` will affect the bottom-sheet opening animations.';
        $log.warn(message, element[0]);
      }

      if (options.isLockedOpen) {
        options.clickOutsideToClose = false;
        options.escapeToClose = false;
      } else {
        options.cleanupGestures = registerGestures(element, options.parent);
      }

      if (!options.disableBackdrop) {
        // Add a backdrop that will close on click
        backdrop = $mdUtil.createBackdrop(scope, "md-bottom-sheet-backdrop md-opaque");

        // Prevent mouse focus on backdrop; ONLY programatic focus allowed.
        // This allows clicks on backdrop to propogate to the $rootElement and
        // ESC key events to be detected properly.
        backdrop[0].tabIndex = -1;

        if (options.clickOutsideToClose) {
          backdrop.on('click', function() {
            $mdUtil.nextTick($mdBottomSheet.cancel,true);
          });
        }

        $mdTheming.inherit(backdrop, options.parent);

        $animate.enter(backdrop, options.parent, null);
      }

      $mdTheming.inherit(element, options.parent);

      if (options.disableParentScroll) {
        options.restoreScroll = $mdUtil.disableScrollAround(element, options.parent);
      }

      return $animate.enter(element, options.parent, backdrop)
        .then(function() {
          var focusable = $mdUtil.findFocusTarget(element) || angular.element(
            element[0].querySelector('button') ||
            element[0].querySelector('a') ||
            element[0].querySelector($mdUtil.prefixer('ng-click', true))
          ) || backdrop;

          if (options.escapeToClose) {
            options.rootElementKeyupCallback = function(e) {
              if (e.keyCode === $mdConstant.KEY_CODE.ESCAPE) {
                $mdUtil.nextTick($mdBottomSheet.cancel,true);
              }
            };

            $rootElement.on('keyup', options.rootElementKeyupCallback);
            focusable && focusable.focus();
          }
        });

    }

    function onRemove(scope, element, options) {
      if (!options.disableBackdrop) $animate.leave(backdrop);

      return $animate.leave(element).then(function() {
        if (options.disableParentScroll) {
          options.restoreScroll();
          delete options.restoreScroll;
        }

        options.cleanupGestures && options.cleanupGestures();
      });
    }

    /**
     * Adds the drag gestures to the bottom sheet.
     */
    function registerGestures(element, parent) {
      var deregister = $mdGesture.register(parent, 'drag', { horizontal: false });
      parent.on('$md.dragstart', onDragStart)
        .on('$md.drag', onDrag)
        .on('$md.dragend', onDragEnd);

      return function cleanupGestures() {
        deregister();
        parent.off('$md.dragstart', onDragStart);
        parent.off('$md.drag', onDrag);
        parent.off('$md.dragend', onDragEnd);
      };

      function onDragStart() {
        // Disable transitions on transform so that it feels fast
        element.css($mdConstant.CSS.TRANSITION_DURATION, '0ms');
      }

      function onDrag(ev) {
        var transform = ev.pointer.distanceY;
        if (transform < 5) {
          // Slow down drag when trying to drag up, and stop after PADDING
          transform = Math.max(-PADDING, transform / 2);
        }
        element.css($mdConstant.CSS.TRANSFORM, 'translate3d(0,' + (PADDING + transform) + 'px,0)');
      }

      function onDragEnd(ev) {
        if (ev.pointer.distanceY > 0 &&
            (ev.pointer.distanceY > 20 || Math.abs(ev.pointer.velocityY) > CLOSING_VELOCITY)) {
          var distanceRemaining = element.prop('offsetHeight') - ev.pointer.distanceY;
          var transitionDuration = Math.min(distanceRemaining / ev.pointer.velocityY * 0.75, 500);
          element.css($mdConstant.CSS.TRANSITION_DURATION, transitionDuration + 'ms');
          $mdUtil.nextTick($mdBottomSheet.cancel,true);
        } else {
          element.css($mdConstant.CSS.TRANSITION_DURATION, '');
          element.css($mdConstant.CSS.TRANSFORM, '');
        }
      }
    }

  }

}

})(window, window.angular);