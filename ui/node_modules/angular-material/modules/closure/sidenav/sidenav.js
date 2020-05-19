/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.sidenav');
goog.require('ngmaterial.components.backdrop');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.sidenav
 *
 * @description
 * A Sidenav component.
 */
SidenavService['$inject'] = ["$mdComponentRegistry", "$mdUtil", "$q", "$log"];
SidenavDirective['$inject'] = ["$mdMedia", "$mdUtil", "$mdConstant", "$mdTheming", "$mdInteraction", "$animate", "$compile", "$parse", "$log", "$q", "$document", "$window", "$$rAF"];
SidenavController['$inject'] = ["$scope", "$attrs", "$mdComponentRegistry", "$q", "$interpolate"];
angular
  .module('material.components.sidenav', [
    'material.core',
    'material.components.backdrop'
  ])
  .factory('$mdSidenav', SidenavService)
  .directive('mdSidenav', SidenavDirective)
  .directive('mdSidenavFocus', SidenavFocusDirective)
  .controller('$mdSidenavController', SidenavController);


/**
 * @ngdoc service
 * @name $mdSidenav
 * @module material.components.sidenav
 *
 * @description
 * `$mdSidenav` makes it easy to interact with multiple sidenavs in an app. When looking up a
 * sidenav instance, you can either look it up synchronously or wait for it to be initialized
 * asynchronously. This is done by passing the second argument to `$mdSidenav`.
 *
 * @usage
 * <hljs lang="js">
 * // Async lookup for sidenav instance; will resolve when the instance is available
 * $mdSidenav(componentId, true).then(function(instance) {
 *   $log.debug( componentId + "is now ready" );
 * });
 * // Sync lookup for sidenav instance; this will resolve immediately.
 * $mdSidenav(componentId).then(function(instance) {
 *   $log.debug( componentId + "is now ready" );
 * });
 * // Async toggle the given sidenav;
 * // when instance is known ready and lazy lookup is not needed.
 * $mdSidenav(componentId)
 *    .toggle()
 *    .then(function(){
 *      $log.debug('toggled');
 *    });
 * // Async open the given sidenav
 * $mdSidenav(componentId)
 *    .open()
 *    .then(function(){
 *      $log.debug('opened');
 *    });
 * // Async close the given sidenav
 * $mdSidenav(componentId)
 *    .close()
 *    .then(function(){
 *      $log.debug('closed');
 *    });
 * // Async lookup for sidenav instance
 * $mdSidenav(componentId, true).then(function(instance) {
 *   // On close callback to handle close, backdrop click, or escape key pressed.
 *   // Callback happens BEFORE the close action occurs.
 *   instance.onClose(function() {
 *     $log.debug('closing');
 *   });
 * });
 * // Sync check to see if the specified sidenav is set to be open
 * $mdSidenav(componentId).isOpen();
 * // Sync check to whether given sidenav is locked open
 * // If this is true, the sidenav will be open regardless of close()
 * $mdSidenav(componentId).isLockedOpen();
 * </hljs>
 */
function SidenavService($mdComponentRegistry, $mdUtil, $q, $log) {
  var errorMsg = "SideNav '{0}' is not available! Did you use md-component-id='{0}'?";
  var service = {
    find: findInstance,      //  sync  - returns proxy API
    waitFor: waitForInstance //  async - returns promise
  };

  /**
   * Service API that supports three (3) usages:
   * $mdSidenav().find("left")               // sync (must already exist) or returns undefined
   * $mdSidenav("left").toggle();            // sync (must already exist) or returns reject promise;
   * $mdSidenav("left",true).then(function(left) { // async returns instance when available
   *  left.toggle();
   * });
   */
  return function(handle, enableWait) {
    if (angular.isUndefined(handle)) {
      return service;
    }

    var shouldWait = enableWait === true;
    var instance = service.find(handle, shouldWait);
    return !instance && shouldWait ? service.waitFor(handle) :
           !instance && angular.isUndefined(enableWait) ? addLegacyAPI(service, handle) : instance;
  };

  /**
   * For failed instance/handle lookups, older-clients expect an response object with noops
   * that include `rejected promise APIs`
   * @param service
   * @param handle
   * @returns {Object}
   */
  function addLegacyAPI(service, handle) {
    var falseFn = function() {
      return false;
    };
    var rejectFn = function() {
      return $q.when($mdUtil.supplant(errorMsg, [handle || ""]));
    };

    return angular.extend({
      isLockedOpen: falseFn,
      isOpen: falseFn,
      toggle: rejectFn,
      open: rejectFn,
      close: rejectFn,
      onClose: angular.noop,
      then: function(callback) {
        return waitForInstance(handle).then(callback || angular.noop);
      }
    }, service);
  }

  /**
   * Synchronously lookup the controller instance for the specified sidNav instance which has been
   * registered with the markup `md-component-id`
   */
  function findInstance(handle, shouldWait) {
    var instance = $mdComponentRegistry.get(handle);

    if (!instance && !shouldWait) {
      // Report missing instance
      $log.error($mdUtil.supplant(errorMsg, [handle || ""]));

      // The component has not registered itself... most like NOT yet created
      // return null to indicate that the Sidenav is not in the DOM
      return undefined;
    }
    return instance;
  }

  /**
   * Asynchronously wait for the component instantiation,
   * Deferred lookup of component instance using $component registry
   */
  function waitForInstance(handle) {
    return $mdComponentRegistry.when(handle).catch($log.error);
  }
}

/**
 * @ngdoc directive
 * @name mdSidenavFocus
 * @module material.components.sidenav
 *
 * @restrict A
 *
 * @description
 * `mdSidenavFocus` provides a way to specify the focused element when a sidenav opens.
 * This is completely optional, as the sidenav itself is focused by default.
 *
 * @usage
 * <hljs lang="html">
 * <md-sidenav>
 *   <form>
 *     <md-input-container>
 *       <label for="testInput">Label</label>
 *       <input id="testInput" type="text" md-sidenav-focus>
 *     </md-input-container>
 *   </form>
 * </md-sidenav>
 * </hljs>
 **/
function SidenavFocusDirective() {
  return {
    restrict: 'A',
    require: '^mdSidenav',
    link: function(scope, element, attr, sidenavCtrl) {
      // @see $mdUtil.findFocusTarget(...)
    }
  };
}

/**
 * @ngdoc directive
 * @name mdSidenav
 * @module material.components.sidenav
 * @restrict E
 *
 * @description
 * A Sidenav component that can be opened and closed programmatically.
 *
 * By default, upon opening it will slide out on top of the main content area.
 *
 * For keyboard and screen reader accessibility, focus is sent to the sidenav wrapper by default.
 * It can be overridden with the `md-autofocus` directive on the child element you want focused.
 *
 * @usage
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
 *
 * <hljs lang="js">
 * var app = angular.module('myApp', ['ngMaterial']);
 * app.controller('MyController', function($scope, $mdSidenav) {
 *   $scope.openLeftMenu = function() {
 *     $mdSidenav('left').toggle();
 *   };
 * });
 * </hljs>
 *
 * @param {expression=} md-is-open A model bound to whether the sidenav is opened.
 * @param {boolean=} md-disable-backdrop When present in the markup, the sidenav will not show a
 *  backdrop.
 * @param {boolean=} md-disable-close-events When present in the markup, clicking the backdrop or
 *  pressing the 'Escape' key will not close the sidenav.
 * @param {string=} md-component-id componentId to use with $mdSidenav service.
 * @param {expression=} md-is-locked-open When this expression evaluates to true,
 * the sidenav 'locks open': it falls into the content's flow instead
 * of appearing over it. This overrides the `md-is-open` attribute.
 * @param {string=} md-disable-scroll-target Selector, pointing to an element, whose scrolling will
 * be disabled when the sidenav is opened. By default this is the sidenav's direct parent.
 *
* The $mdMedia() service is exposed to the is-locked-open attribute, which
 * can be given a media query or one of the `sm`, `gt-sm`, `md`, `gt-md`, `lg` or `gt-lg` presets.
 * Examples:
 *
 *   - `<md-sidenav md-is-locked-open="shouldLockOpen"></md-sidenav>`
 *   - `<md-sidenav md-is-locked-open="$mdMedia('min-width: 1000px')"></md-sidenav>`
 *   - `<md-sidenav md-is-locked-open="$mdMedia('sm')"></md-sidenav>` (locks open on small screens)
 */
function SidenavDirective($mdMedia, $mdUtil, $mdConstant, $mdTheming, $mdInteraction, $animate,
                          $compile, $parse, $log, $q, $document, $window, $$rAF) {
  return {
    restrict: 'E',
    scope: {
      isOpen: '=?mdIsOpen'
    },
    controller: '$mdSidenavController',
    compile: function(element) {
      element.addClass('md-closed').attr('tabIndex', '-1');
      return postLink;
    }
  };

  /**
   * Directive Post Link function...
   */
  function postLink(scope, element, attr, sidenavCtrl) {
    var lastParentOverFlow;
    var backdrop;
    var disableScrollTarget = null;
    var disableCloseEvents;
    var triggeringInteractionType;
    var triggeringElement = null;
    var previousContainerStyles;
    var promise = $q.when(true);
    var isLockedOpenParsed = $parse(attr.mdIsLockedOpen);
    var ngWindow = angular.element($window);
    var isLocked = function() {
      return isLockedOpenParsed(scope.$parent, {
        $media: function(arg) {
          $log.warn("$media is deprecated for is-locked-open. Use $mdMedia instead.");
          return $mdMedia(arg);
        },
        $mdMedia: $mdMedia
      });
    };

    if (attr.mdDisableScrollTarget) {
      disableScrollTarget = $document[0].querySelector(attr.mdDisableScrollTarget);

      if (disableScrollTarget) {
        disableScrollTarget = angular.element(disableScrollTarget);
      } else {
        $log.warn($mdUtil.supplant('mdSidenav: couldn\'t find element matching ' +
          'selector "{selector}". Falling back to parent.',
          { selector: attr.mdDisableScrollTarget }));
      }
    }

    if (!disableScrollTarget) {
      disableScrollTarget = element.parent();
    }

    // Only create the backdrop if the backdrop isn't disabled.
    if (!attr.hasOwnProperty('mdDisableBackdrop')) {
      backdrop = $mdUtil.createBackdrop(scope, "md-sidenav-backdrop md-opaque ng-enter");
    }

    // If md-disable-close-events is set on the sidenav we will disable
    // backdrop click and Escape key events
    if (attr.hasOwnProperty('mdDisableCloseEvents')) {
      disableCloseEvents = true;
    }

    element.addClass('_md');     // private md component indicator for styling
    $mdTheming(element);

    // The backdrop should inherit the sidenavs theme,
    // because the backdrop will take its parent theme by default.
    if (backdrop) $mdTheming.inherit(backdrop, element);

    element.on('$destroy', function() {
      backdrop && backdrop.remove();
      sidenavCtrl.destroy();
    });

    scope.$on('$destroy', function(){
      backdrop && backdrop.remove();
    });

    scope.$watch(isLocked, updateIsLocked);
    scope.$watch('isOpen', updateIsOpen);


    // Publish special accessor for the Controller instance
    sidenavCtrl.$toggleOpen = toggleOpen;

    /**
     * Toggle the DOM classes to indicate `locked`
     * @param isLocked
     * @param oldValue
     */
    function updateIsLocked(isLocked, oldValue) {
      scope.isLockedOpen = isLocked;
      if (isLocked === oldValue) {
        element.toggleClass('md-locked-open', !!isLocked);
      } else {
        $animate[isLocked ? 'addClass' : 'removeClass'](element, 'md-locked-open');
      }
      if (backdrop) {
        backdrop.toggleClass('md-locked-open', !!isLocked);
      }
    }

    /**
     * Toggle the SideNav view and attach/detach listeners
     * @param isOpen
     */
    function updateIsOpen(isOpen) {
      // Support deprecated md-sidenav-focus attribute as fallback
      var focusEl = $mdUtil.findFocusTarget(element) ||
        $mdUtil.findFocusTarget(element,'[md-sidenav-focus]') || element;
      var parent = element.parent();
      var restorePositioning;

      // If the user hasn't set the disable close events property we are adding
      // click and escape events to close the sidenav
      if (!disableCloseEvents) {
        parent[isOpen ? 'on' : 'off']('keydown', onKeyDown);
        if (backdrop) backdrop[isOpen ? 'on' : 'off']('click', close);
      }

      restorePositioning = updateContainerPositions(parent, isOpen);

      if (isOpen) {
        // Capture upon opening..
        triggeringElement = $document[0].activeElement;
        triggeringInteractionType = $mdInteraction.getLastInteractionType();
      }

      disableParentScroll(isOpen);

      return promise = $q.all([
        isOpen && backdrop ? $animate.enter(backdrop, parent) : backdrop ?
                             $animate.leave(backdrop) : $q.when(true),
        $animate[isOpen ? 'removeClass' : 'addClass'](element, 'md-closed')
      ]).then(function() {
        // Perform focus when animations are ALL done...
        if (scope.isOpen) {
          $$rAF(function() {
            // Notifies child components that the sidenav was opened. Should wait
            // a frame in order to allow for the element height to be computed.
            ngWindow.triggerHandler('resize');
          });

          focusEl && focusEl.focus();
        }

        // Restores the positioning on the sidenav and backdrop.
        restorePositioning && restorePositioning();
      });
    }

    function updateContainerPositions(parent, willOpen) {
      var drawerEl = element[0];
      var scrollTop = parent[0].scrollTop;

      if (willOpen && scrollTop) {
        previousContainerStyles = {
          top: drawerEl.style.top,
          bottom: drawerEl.style.bottom,
          height: drawerEl.style.height
        };

        // When the parent is scrolled down, then we want to be able to show the sidenav at the
        // current scroll position. We're moving the sidenav down to the correct scroll position
        // and apply the height of the parent, to increase the performance. Using 100% as height,
        // will impact the performance heavily.
        var positionStyle = {
          top: scrollTop + 'px',
          bottom: 'auto',
          height: parent[0].clientHeight + 'px'
        };

        // Apply the new position styles to the sidenav and backdrop.
        element.css(positionStyle);
        backdrop.css(positionStyle);
      }

      // When the sidenav is closing and we have previous defined container styles,
      // then we return a restore function, which resets the sidenav and backdrop.
      if (!willOpen && previousContainerStyles) {
        return function() {
          drawerEl.style.top = previousContainerStyles.top;
          drawerEl.style.bottom = previousContainerStyles.bottom;
          drawerEl.style.height = previousContainerStyles.height;

          backdrop[0].style.top = null;
          backdrop[0].style.bottom = null;
          backdrop[0].style.height = null;

          previousContainerStyles = null;
        };
      }
    }

    /**
     * Prevent parent scrolling (when the SideNav is open)
     */
    function disableParentScroll(disabled) {
      if (disabled && !lastParentOverFlow) {
        lastParentOverFlow = disableScrollTarget.css('overflow');
        disableScrollTarget.css('overflow', 'hidden');
      } else if (angular.isDefined(lastParentOverFlow)) {
        disableScrollTarget.css('overflow', lastParentOverFlow);
        lastParentOverFlow = undefined;
      }
    }

    /**
     * Toggle the sideNav view and publish a promise to be resolved when
     * the view animation finishes.
     * @param {boolean} isOpen true to open the sidenav, false to close it
     * @returns {*} promise to be resolved when the view animation finishes
     */
    function toggleOpen(isOpen) {
      if (scope.isOpen === isOpen) {
        return $q.when(true);
      } else {
        if (scope.isOpen && sidenavCtrl.onCloseCb) sidenavCtrl.onCloseCb();

        return $q(function(resolve) {
          // Toggle value to force an async `updateIsOpen()` to run
          scope.isOpen = isOpen;

          $mdUtil.nextTick(function() {
            // When the current `updateIsOpen()` animation finishes
            promise.then(function(result) {

              if (!scope.isOpen && triggeringElement && triggeringInteractionType === 'keyboard') {
                // reset focus to originating element (if available) upon close
                triggeringElement.focus();
                triggeringElement = null;
              }

              resolve(result);
            });
          });
        });
      }
    }

    /**
     * Auto-close sideNav when the `escape` key is pressed.
     * @param {KeyboardEvent} ev keydown event
     */
    function onKeyDown(ev) {
      var isEscape = (ev.keyCode === $mdConstant.KEY_CODE.ESCAPE);
      return isEscape ? close(ev) : $q.when(true);
    }

    /**
     * With backdrop `clicks` or `escape` key-press, immediately apply the CSS close transition...
     * Then notify the controller to close() and perform its own actions.
     * @param {Event} ev
     * @returns {*}
     */
    function close(ev) {
      ev.preventDefault();

      return sidenavCtrl.close();
    }
  }
}

/*
 * @private
 * @ngdoc controller
 * @name SidenavController
 * @module material.components.sidenav
 */
function SidenavController($scope, $attrs, $mdComponentRegistry, $q, $interpolate) {
  var self = this;

  // Use Default internal method until overridden by directive postLink

  // Synchronous getters
  self.isOpen = function() { return !!$scope.isOpen; };
  self.isLockedOpen = function() { return !!$scope.isLockedOpen; };

  // Synchronous setters
  self.onClose = function (callback) {
    self.onCloseCb = callback;
    return self;
  };

  // Async actions
  self.open   = function() { return self.$toggleOpen(true);  };
  self.close  = function() { return self.$toggleOpen(false); };
  self.toggle = function() { return self.$toggleOpen(!$scope.isOpen);  };
  self.$toggleOpen = function(value) { return $q.when($scope.isOpen = value); };

  // Evaluate the component id.
  var rawId = $attrs.mdComponentId;
  var hasDataBinding = rawId && rawId.indexOf($interpolate.startSymbol()) > -1;
  var componentId = hasDataBinding ? $interpolate(rawId)($scope.$parent) : rawId;

  // Register the component.
  self.destroy = $mdComponentRegistry.register(self, componentId);

  // Watch and update the component, if the id has changed.
  if (hasDataBinding) {
    $attrs.$observe('mdComponentId', function(id) {
      if (id && id !== self.$$mdHandle) {
        // `destroy` only deregisters the old component id so we can add the new one.
        self.destroy();
        self.destroy = $mdComponentRegistry.register(self, id);
      }
    });
  }
}

ngmaterial.components.sidenav = angular.module("material.components.sidenav");