(function(){"use strict";/**
 * @ngdoc module
 * @name material.components.expansionPanels
 *
 * @description
 * Expansion panel component
 */
angular
  .module('material.components.expansionPanels', [
    'material.core'
  ]);
}());
(function(){"use strict";angular.module("material.components.expansionPanels").run(["$templateCache", function($templateCache) {$templateCache.put("icons/ic_keyboard_arrow_right_black_24px.svg","<svg fill=\"#000000\" height=\"24\" viewBox=\"0 0 24 24\" width=\"24\" xmlns=\"http://www.w3.org/2000/svg\">\n    <path d=\"M8.59 16.34l4.58-4.59-4.58-4.59L10 5.75l6 6-6 6z\"/>\n    <path d=\"M0-.25h24v24H0z\" fill=\"none\"/>\n</svg>");}]);}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanel', expansionPanelDirective);


var ANIMATION_TIME = 180; //ms


/**
 * @ngdoc directive
 * @name mdExpansionPanel
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanel` is the main container for panels
 *
 * @param {string=} md-component-id - add an id if you want to acces the panel via the `$mdExpansionPanel` service
 **/
function expansionPanelDirective() {
  var directive = {
    restrict: 'E',
    require: ['mdExpansionPanel', '?^^mdExpansionPanelGroup'],
    scope: true,
    compile: compile,
    controller: ['$scope', '$element', '$attrs', '$window', '$$rAF', '$mdConstant', '$mdUtil', '$mdComponentRegistry', '$timeout', '$q', '$animate', '$parse', controller]
  };
  return directive;




  function compile(tElement, tAttrs) {
    var INVALID_PREFIX = 'Invalid HTML for md-expansion-panel: ';

    tElement.attr('tabindex', tAttrs.tabindex || '0');

    if (tElement[0].querySelector('md-expansion-panel-collapsed') === null) {
      throw Error(INVALID_PREFIX + 'Expected a child element of `md-epxansion-panel-collapsed`');
    }
    if (tElement[0].querySelector('md-expansion-panel-expanded') === null) {
      throw Error(INVALID_PREFIX + 'Expected a child element of `md-epxansion-panel-expanded`');
    }

    return function postLink(scope, element, attrs, ctrls) {
      var epxansionPanelCtrl = ctrls[0];
      var epxansionPanelGroupCtrl = ctrls[1];

      epxansionPanelCtrl.epxansionPanelGroupCtrl = epxansionPanelGroupCtrl || undefined;
      epxansionPanelCtrl.init();
    };
  }




  function controller($scope, $element, $attrs, $window, $$rAF, $mdConstant, $mdUtil, $mdComponentRegistry, $timeout, $q, $animate, $parse) {
    /* jshint validthis: true */
    var vm = this;

    var collapsedCtrl;
    var expandedCtrl;
    var headerCtrl;
    var footerCtrl;
    var deregister;
    var scrollContainer;
    var stickyContainer;
    var topKiller;
    var resizeKiller;
    var onRemoveCallback;
    var transformParent;
    var backdrop;
    var inited = false;
    var registerOnInit = false;
    var _isOpen = false;
    var isDisabled = false;
    var debouncedUpdateScroll = $$rAF.throttle(updateScroll);
    var debouncedUpdateResize = $$rAF.throttle(updateResize);

    vm.registerCollapsed = function (ctrl) { collapsedCtrl = ctrl; };
    vm.registerExpanded = function (ctrl) { expandedCtrl = ctrl; };
    vm.registerHeader = function (ctrl) { headerCtrl = ctrl; };
    vm.registerFooter = function (ctrl) { footerCtrl = ctrl; };



    if ($attrs.mdComponentId === undefined) {
      $attrs.$set('mdComponentId', '_expansion_panel_id_' + $mdUtil.nextUid());
      registerPanel();
    } else {
      $attrs.$observe('mdComponentId', function() {
        registerPanel();
      });
    }

    vm.$element = $element;
    vm.expand = expand;
    vm.collapse = collapse;
    vm.remove = remove;
    vm.destroy = destroy;
    vm.onRemove = onRemove;
    vm.init = init;

    if ($attrs.ngDisabled !== undefined) {
      $scope.$watch($attrs.ngDisabled, function(value) {
        isDisabled = value;
        $element.attr('tabindex', isDisabled ? -1 : 0);
      });
    } else if ($attrs.disabled !== undefined) {
      isDisabled = ($attrs.disabled !== undefined && $attrs.disabled !== 'false' && $attrs.disabled !== false);
      $element.attr('tabindex', isDisabled ? -1 : 0);
    }

    $element
      .on('focus', function (ev) {
        $element.on('keydown', handleKeypress);
      })
      .on('blur', function (ev) {
        $element.off('keydown', handleKeypress);
      });

    function handleKeypress(ev) {
      var keyCodes = $mdConstant.KEY_CODE;
      switch (ev.keyCode) {
        case keyCodes.ENTER:
          expand();
          break;
        case keyCodes.ESCAPE:
          collapse();
          break;
      }
    }


    $scope.$panel = {
      collapse: collapse,
      expand: expand,
      remove: remove,
      isOpen: isOpen
    };

    $scope.$on('$destroy', function () {
      removeClickCatcher();

      // remove component from registry
      if (typeof deregister === 'function') {
        deregister();
        deregister = undefined;
      }
      killEvents();
    });





    function init() {
      inited = true;
      if (registerOnInit === true) {
        registerPanel();
      }
    }


    function registerPanel() {
      if (inited === false) {
        registerOnInit = true;
        return;
      }

      // deregister if component was already registered
      if (typeof deregister === 'function') {
        deregister();
        deregister = undefined;
      }
      // remove component from group ctrl if component was already added
      if (vm.componentId && vm.epxansionPanelGroupCtrl) {
        vm.epxansionPanelGroupCtrl.removePanel(vm.componentId);
      }

      // if componentId was removed then set one
      if ($attrs.mdComponentId === undefined) {
        $attrs.$set('mdComponentId', '_expansion_panel_id_' + $mdUtil.nextUid());
      }

      vm.componentId = $attrs.mdComponentId;
      deregister = $mdComponentRegistry.register({
        expand: expand,
        collapse: collapse,
        remove: remove,
        onRemove: onRemove,
        isOpen: isOpen,
        addClickCatcher: addClickCatcher,
        removeClickCatcher: removeClickCatcher,
        componentId: $attrs.mdComponentId
      }, $attrs.mdComponentId);

      if (vm.epxansionPanelGroupCtrl) {
        vm.epxansionPanelGroupCtrl.addPanel(vm.componentId, {
          expand: expand,
          collapse: collapse,
          remove: remove,
          onRemove: onRemove,
          destroy: destroy,
          isOpen: isOpen
        });
      }
    }


    function isOpen() {
      return _isOpen;
    }

    function expand(options) {
      if (_isOpen === true || isDisabled === true) { return; }
      _isOpen = true;
      options = options || {};

      var deferred = $q.defer();

      if (vm.epxansionPanelGroupCtrl) {
        vm.epxansionPanelGroupCtrl.expandPanel(vm.componentId);
      }

      $element.removeClass('md-close');
      $element.addClass('md-open');
      if (options.animation === false) {
        $element.addClass('md-no-animation');
      } else {
        $element.removeClass('md-no-animation');
      }

      initEvents();
      collapsedCtrl.hide(options);
      expandedCtrl.show(options);

      if (headerCtrl) { headerCtrl.show(options); }
      if (footerCtrl) { footerCtrl.show(options); }

      $timeout(function () {
        deferred.resolve();
      }, options.animation === false ? 0 : ANIMATION_TIME);
      return deferred.promise;
    }


    function collapse(options) {
      if (_isOpen === false) { return; }
      _isOpen = false;
      options = options || {};

      var deferred = $q.defer();

      $element.addClass('md-close');
      $element.removeClass('md-open');
      if (options.animation === false) {
        $element.addClass('md-no-animation');
      } else {
        $element.removeClass('md-no-animation');
      }

      killEvents();
      collapsedCtrl.show(options);
      expandedCtrl.hide(options);

      if (headerCtrl) { headerCtrl.hide(options); }
      if (footerCtrl) { footerCtrl.hide(options); }

      $timeout(function () {
        deferred.resolve();
      }, options.animation === false ? 0 : ANIMATION_TIME);
      return deferred.promise;
    }


    function remove(options) {
      options = options || {};
      var deferred = $q.defer();

      if (vm.epxansionPanelGroupCtrl) {
        vm.epxansionPanelGroupCtrl.removePanel(vm.componentId);
      }

      if (typeof deregister === 'function') {
        deregister();
        deregister = undefined;
      }

      if (options.animation === false || _isOpen === false) {
        $scope.$destroy();
        $element.remove();
        deferred.resolve();
        callbackRemove();
      } else {
        collapse();
        $timeout(function () {
          $scope.$destroy();
          $element.remove();
          deferred.resolve();
          callbackRemove();
        }, ANIMATION_TIME);
      }

      return deferred.promise;
    }

    function onRemove(callback) {
      onRemoveCallback = callback;
    }

    function callbackRemove() {
      if (typeof onRemoveCallback === 'function') {
        onRemoveCallback();
        onRemoveCallback = undefined;
      }
    }

    function destroy() {
      $scope.$destroy();
    }



    function initEvents() {
      if ((!footerCtrl || footerCtrl.noSticky === true) && (!headerCtrl || headerCtrl.noSticky === true)) {
        return;
      }

      // watch for panel position changes
      topKiller = $scope.$watch(function () { return $element[0].offsetTop; }, debouncedUpdateScroll, true);

      // watch for panel position changes
      resizeKiller = $scope.$watch(function () { return $element[0].offsetWidth; }, debouncedUpdateResize, true);

      // listen to md-content scroll events id we are nested in one
      scrollContainer = $mdUtil.getNearestContentElement($element);
      if (scrollContainer.nodeName === 'MD-CONTENT') {
        transformParent = getTransformParent(scrollContainer);
        angular.element(scrollContainer).on('scroll', debouncedUpdateScroll);
      } else {
        transformParent = undefined;
      }

      // listen to expanded content scroll if height is set
      if (expandedCtrl.setHeight === true) {
        expandedCtrl.$element.on('scroll', debouncedUpdateScroll);
      }

      // listen to window scroll events
      angular.element($window)
        .on('scroll', debouncedUpdateScroll)
        .on('resize', debouncedUpdateScroll)
        .on('resize', debouncedUpdateResize);
    }


    function killEvents() {
      if (typeof topKiller === 'function') {
        topKiller();
        topKiller = undefined;
      }

      if (typeof resizeKiller === 'function') {
        resizeKiller();
        resizeKiller = undefined;
      }

      if (scrollContainer && scrollContainer.nodeName === 'MD-CONTENT') {
        angular.element(scrollContainer).off('scroll', debouncedUpdateScroll);
      }

      if (expandedCtrl.setHeight === true) {
        expandedCtrl.$element.off('scroll', debouncedUpdateScroll);
      }

      angular.element($window)
        .off('scroll', debouncedUpdateScroll)
        .off('resize', debouncedUpdateScroll)
        .off('resize', debouncedUpdateResize);
    }



    function getTransformParent(el) {
      var parent = el.parentNode;

      while (parent && parent !== document) {
        if (hasComputedStyle(parent, 'transform')) {
          return parent;
        }
        parent = parent.parentNode;
      }

      return undefined;
    }

    function hasComputedStyle(target, key) {
      var hasValue = false;

      if (target) {
        var computedStyles = $window.getComputedStyle(target);
        hasValue = computedStyles[key] !== undefined && computedStyles[key] !== 'none';
      }

      return hasValue;
    }


    function updateScroll(e) {
      var top;
      var bottom;
      var bounds;
      if (expandedCtrl.setHeight === true) {
        bounds = expandedCtrl.$element[0].getBoundingClientRect();
      } else {
        bounds = scrollContainer.getBoundingClientRect();
      }
      var transformTop = transformParent ? transformParent.getBoundingClientRect().top : 0;

      // we never want the header going post the top of the page. to prevent this don't allow top to go below 0
      top = Math.max(bounds.top, 0);
      bottom = top + bounds.height;

      if (footerCtrl && footerCtrl.noSticky === false) { footerCtrl.onScroll(top, bottom, transformTop); }
      if (headerCtrl && headerCtrl.noSticky === false) { headerCtrl.onScroll(top, bottom, transformTop); }
    }


    function updateResize() {
      var value = $element[0].offsetWidth;
      if (footerCtrl && footerCtrl.noSticky === false) { footerCtrl.onResize(value); }
      if (headerCtrl && headerCtrl.noSticky === false) { headerCtrl.onResize(value); }
    }




    function addClickCatcher(clickCallback) {
      backdrop = $mdUtil.createBackdrop($scope);
      backdrop[0].tabIndex = -1;

      if (typeof clickCallback === 'function') {
        backdrop.on('click', clickCallback);
      }

      $animate.enter(backdrop, $element.parent(), null, {duration: 0});
      $element.css('z-index', 60);
    }

    function removeClickCatcher() {
      if (backdrop) {
        backdrop.remove();
        backdrop.off('click');
        backdrop = undefined;
        $element.css('z-index', '');
      }
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .factory('$mdExpansionPanel', expansionPanelService);


/**
 * @ngdoc service
 * @name $mdExpansionPanel
 * @module material.components.expansionPanels
 *
 * @description
 * Expand and collapse Expansion Panel using its `md-component-id`
 *
 * @example
 * $mdExpansionPanel('comonentId').then(function (instance) {
 *  instance.exapand();
 *  instance.collapse({animation: false});
 *  instance.remove({animation: false});
 *  instance.onRemove(function () {});
 * });
 */
expansionPanelService.$inject = ['$mdComponentRegistry', '$mdUtil', '$log'];
function expansionPanelService($mdComponentRegistry, $mdUtil, $log) {
  var errorMsg = "ExpansionPanel '{0}' is not available! Did you use md-component-id='{0}'?";
  var service = {
    find: findInstance,
    waitFor: waitForInstance
  };

  return function (handle) {
    if (handle === undefined) { return service; }
    return findInstance(handle);
  };



  function findInstance(handle) {
    var instance = $mdComponentRegistry.get(handle);

    if (!instance) {
      // Report missing instance
      $log.error( $mdUtil.supplant(errorMsg, [handle || ""]) );
      return undefined;
    }

    return instance;
  }

  function waitForInstance(handle) {
    return $mdComponentRegistry.when(handle).catch($log.error);
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelCollapsed', expansionPanelCollapsedDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelCollapsed
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelCollapsed` is used to contain content when the panel is collapsed
 **/
expansionPanelCollapsedDirective.$inject = ['$animateCss', '$timeout'];
function expansionPanelCollapsedDirective($animateCss, $timeout) {
  var directive = {
    restrict: 'E',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;


  function link(scope, element, attrs, expansionPanelCtrl) {
    expansionPanelCtrl.registerCollapsed({
      show: show,
      hide: hide
    });


    element.on('click', function () {
      expansionPanelCtrl.expand();
    });


    function hide(options) {
      // set width to maintian demensions when element is set to postion: absolute
      element.css('width', element[0].offsetWidth + 'px');
      // set min height so the expansion panel does not shrink when collapsed element is set to position: absolute
      expansionPanelCtrl.$element.css('min-height', element[0].offsetHeight + 'px');

      var animationParams = {
        addClass: 'md-absolute md-hide',
        from: {opacity: 1},
        to: {opacity: 0}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        element.removeClass('md-hide');
        element.css('display', 'none');
      });
    }


    function show(options) {
      element.css('display', '');
      // set width to maintian demensions when element is set to postion: absolute
      element.css('width', element[0].parentNode.offsetWidth + 'px');

      var animationParams = {
        addClass: 'md-show',
        from: {opacity: 0},
        to: {opacity: 1}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        // safari will animate the min-height if transition is not set to 0
        expansionPanelCtrl.$element.css('transition', 'none');
        element.removeClass('md-absolute md-show');

        // remove width when element is no longer position: absolute
        element.css('width', '');


        // remove min height when element is no longer position: absolute
        expansionPanelCtrl.$element.css('min-height', '');
        // remove transition block on next digest
        $timeout(function () {
          expansionPanelCtrl.$element.css('transition', '');
        }, 0);
      });
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelExpanded', expansionPanelExpandedDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelExpanded
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelExpanded` is used to contain content when the panel is expanded
 *
 * @param {number=} height - add this aatribute set the max height of the expanded content. The container will be set to scroll
 **/
expansionPanelExpandedDirective.$inject = ['$animateCss', '$timeout'];
function expansionPanelExpandedDirective($animateCss, $timeout) {
  var directive = {
    restrict: 'E',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;


  function link(scope, element, attrs, expansionPanelCtrl) {
    var setHeight = attrs.height || undefined;
    if (setHeight !== undefined) { setHeight = setHeight.replace('px', '') + 'px'; }

    expansionPanelCtrl.registerExpanded({
      show: show,
      hide: hide,
      setHeight: setHeight !== undefined,
      $element: element
    });




    function hide(options) {
      var height = setHeight ? setHeight : element[0].scrollHeight + 'px';
      element.addClass('md-hide md-overflow');
      element.removeClass('md-show md-scroll-y');

      var animationParams = {
        from: {'max-height': height, opacity: 1},
        to: {'max-height': '48px', opacity: 0}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        element.css('display', 'none');
        element.removeClass('md-hide');
      });
    }


    function show(options) {
      element.css('display', '');
      element.addClass('md-show md-overflow');
      // use passed in height or the contents height
      var height = setHeight ? setHeight : element[0].scrollHeight + 'px';

      var animationParams = {
        from: {'max-height': '48px', opacity: 0},
        to: {'max-height': height, opacity: 1}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {

        // if height was passed in then set div to scroll
        if (setHeight !== undefined) {
          element.addClass('md-scroll-y');
        } else {
          // safari will animate the max-height if transition is not set to 0
          element.css('transition', 'none');
          element.css('max-height', 'none');
          // remove transition block on next digest
          $timeout(function () {
            element.css('transition', '');
          }, 0);
        }

        element.removeClass('md-overflow');
      });
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelFooter', expansionPanelFooterDirective);




/**
 * @ngdoc directive
 * @name mdExpansionPanelFooter
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelFooter` is nested inside of `mdExpansionPanelExpanded` and contains content you want at the bottom.
 * By default the Footer will stick to the bottom of the page if the panel expands past
 * this is optional
 *
 * @param {boolean=} md-no-sticky - add this aatribute to disable sticky
 **/
function expansionPanelFooterDirective() {
  var directive = {
    restrict: 'E',
    transclude: true,
    template: '<div class="md-expansion-panel-footer-container" ng-transclude></div>',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;



  function link(scope, element, attrs, expansionPanelCtrl) {
    var isStuck = false;
    var noSticky = attrs.mdNoSticky !== undefined;
    var container = angular.element(element[0].querySelector('.md-expansion-panel-footer-container'));

    expansionPanelCtrl.registerFooter({
      show: show,
      hide: hide,
      onScroll: onScroll,
      onResize: onResize,
      noSticky: noSticky
    });



    function show() {

    }
    function hide() {
      unstick();
    }

    function onScroll(top, bottom, transformTop) {
      var height;
      var footerBounds = element[0].getBoundingClientRect();
      var offset;

      if (footerBounds.bottom > bottom) {
        height = container[0].offsetHeight;
        offset = bottom - height - transformTop;
        if (offset < element[0].parentNode.getBoundingClientRect().top) {
          offset = element[0].parentNode.getBoundingClientRect().top;
        }

        // set container width because element becomes postion fixed
        container.css('width', expansionPanelCtrl.$element[0].offsetWidth + 'px');

        // set element height so it does not loose its height when container is position fixed
        element.css('height', height + 'px');
        container.css('top', offset + 'px');

        element.addClass('md-stick');
        isStuck = true;
      } else if (isStuck === true) {
        unstick();
      }
    }

    function onResize(width) {
      if (isStuck === false) { return; }
      container.css('width', width + 'px');
    }


    function unstick() {
      isStuck = false;
      container.css('width', '');
      container.css('top', '');
      element.css('height', '');
      element.removeClass('md-stick');
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelGroup', expansionPanelGroupDirective);

/**
 * @ngdoc directive
 * @name mdExpansionPanelGroup
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelGroup` is a container used to manage multiple expansion panels
 *
 * @param {string=} md-component-id - add an id if you want to acces the panel via the `$mdExpansionPanelGroup` service
 * @param {string=} auto-expand - panels expand when added to `<md-expansion-panel-group>`
 * @param {string=} multiple - allows for more than one panel to be expanded at a time
 **/
function expansionPanelGroupDirective() {
  var directive = {
    restrict: 'E',
    controller: ['$scope', '$attrs', '$element', '$mdComponentRegistry', controller]
  };
  return directive;


  function controller($scope, $attrs, $element, $mdComponentRegistry) {
    /* jshint validthis: true */
    var vm = this;

    var deregister;
    var registered = {};
    var panels = {};
    var onChangeFuncs = [];
    var multipleExpand = $attrs.mdMultiple !== undefined || $attrs.multiple !== undefined;
    var autoExpand = $attrs.mdAutoExpand !== undefined || $attrs.autoExpand !== undefined;


    deregister = $mdComponentRegistry.register({
      $element: $element,
      register: register,
      getRegistered: getRegistered,
      getAll: getAll,
      getOpen: getOpen,
      remove: remove,
      removeAll: removeAll,
      collapseAll: collapseAll,
      onChange: onChange,
      count: panelCount
    }, $attrs.mdComponentId);

    vm.addPanel = addPanel;
    vm.expandPanel = expandPanel;
    vm.removePanel = removePanel;


    $scope.$on('$destroy', function () {
      if (typeof deregister === 'function') {
        deregister();
        deregister = undefined;
      }

      // destroy all panels
      // for some reason the child panels scopes are not getting destroyed
      Object.keys(panels).forEach(function (key) {
        panels[key].destroy();
      });
    });



    function onChange(callback) {
      onChangeFuncs.push(callback);

      return function () {
        onChangeFuncs.splice(onChangeFuncs.indexOf(callback), 1);
      };
    }

    function callOnChange() {
      var count = panelCount();
      onChangeFuncs.forEach(function (func) {
        func(count);
      });
    }


    function addPanel(componentId, panelCtrl) {
      panels[componentId] = panelCtrl;
      if (autoExpand === true) {
        panelCtrl.expand();
        closeOthers(componentId);
      }
      callOnChange();
    }

    function expandPanel(componentId) {
      closeOthers(componentId);
    }

    function remove(componentId, options) {
      return panels[componentId].remove(options);
    }

    function removeAll(options) {
      Object.keys(panels).forEach(function (panelId) {
        panels[panelId].remove(options);
      });
    }

    function removePanel(componentId) {
      delete panels[componentId];
      callOnChange();
    }

    function panelCount() {
      return Object.keys(panels).length;
    }

    function closeOthers(id) {
      if (multipleExpand === false) {
        Object.keys(panels).forEach(function (panelId) {
          if (panelId !== id) { panels[panelId].collapse(); }
        });
      }
    }


    function register(name, options) {
      if (registered[name] !== undefined) {
        throw Error('$mdExpansionPanelGroup.register() The name "' + name + '" has already been registered');
      }
      registered[name] = options;
    }


    function getRegistered(name) {
      if (registered[name] === undefined) {
        throw Error('$mdExpansionPanelGroup.addPanel() Cannot find Panel with name of "' + name + '"');
      }
      return registered[name];
    }


    function getAll() {
      return Object.keys(panels).map(function (panelId) {
        return panels[panelId];
      });
    }

    function getOpen() {
      return Object.keys(panels).map(function (panelId) {
        return panels[panelId];
      }).filter(function (instance) {
        return instance.isOpen();
      });
    }

    function collapseAll(noAnimation) {
      var animation = noAnimation === true ? false : true;
      Object.keys(panels).forEach(function (panelId) {
        panels[panelId].collapse({animation: animation});
      });
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .factory('$mdExpansionPanelGroup', expansionPanelGroupService);


/**
 * @ngdoc service
 * @name $mdExpansionPanelGroup
 * @module material.components.expansionPanels
 *
 * @description
 * Expand and collapse Expansion Panel using its `md-component-id`
 *
 * @example
 * $mdExpansionPanelGroup('comonentId').then(function (instance) {
 *  instance.register({
 *    componentId: 'cardComponentId',
 *    templateUrl: 'template.html',
 *    controller: 'Controller'
 *  });
 *  instance.add('cardComponentId', {local: localData});
 *  instance.remove('cardComponentId', {animation: false});
 *  instance.removeAll({animation: false});
 * });
 */
expansionPanelGroupService.$inject = ['$mdComponentRegistry', '$mdUtil', '$mdExpansionPanel', '$templateRequest', '$rootScope', '$compile', '$controller', '$q', '$log'];
function expansionPanelGroupService($mdComponentRegistry, $mdUtil, $mdExpansionPanel, $templateRequest, $rootScope, $compile, $controller, $q, $log) {
  var errorMsg = "ExpansionPanelGroup '{0}' is not available! Did you use md-component-id='{0}'?";
  var service = {
    find: findInstance,
    waitFor: waitForInstance
  };

  return function (handle) {
    if (handle === undefined) { return service; }
    return findInstance(handle);
  };



  function findInstance(handle) {
    var instance = $mdComponentRegistry.get(handle);

    if (!instance) {
      // Report missing instance
      $log.error( $mdUtil.supplant(errorMsg, [handle || ""]) );
      return undefined;
    }

    return createGroupInstance(instance);
  }

  function waitForInstance(handle) {
    var deffered = $q.defer();

    $mdComponentRegistry.when(handle).then(function (instance) {
      deffered.resolve(createGroupInstance(instance));
    }).catch(function (error) {
      deffered.reject();
      $log.error(error);
    });

    return deffered.promise;
  }





  // --- returned service for group instance ---

  function createGroupInstance(instance) {
    var service = {
      add: add,
      register: register,
      getAll: getAll,
      getOpen: getOpen,
      remove: remove,
      removeAll: removeAll,
      collapseAll: collapseAll,
      onChange: onChange,
      count: count
    };

    return service;


    function register(name, options) {
      if (typeof name !== 'string') {
        throw Error('$mdExpansionPanelGroup.register() Expects name to be a string');
      }

      validateOptions(options);
      instance.register(name, options);
    }

    function remove(componentId, options) {
      return instance.remove(componentId, options);
    }

    function removeAll(options) {
      instance.removeAll(options);
    }

    function onChange(callback) {
      return instance.onChange(callback);
    }

    function count() {
      return instance.count();
    }

    function getAll() {
      return instance.getAll();
    }

    function getOpen() {
      return instance.getOpen();
    }

    function collapseAll(noAnimation) {
      instance.collapseAll(noAnimation);
    }


    function add(options, locals) {
      locals = locals || {};
      // assume if options is a string then they are calling a registered card by its component id
      if (typeof options === 'string') {
        // call add panel with the stored options
        return add(instance.getRegistered(options), locals);
      }

      validateOptions(options);
      if (options.componentId && instance.isPanelActive(options.componentId)) {
        return $q.reject('panel with componentId "' + options.componentId + '" is currently active');
      }


      var deffered = $q.defer();
      var scope = $rootScope.$new();
      angular.extend(scope, options.scope);

      getTemplate(options, function (template) {
        var element = angular.element(template);
        var componentId = options.componentId || element.attr('md-component-id') || '_panelComponentId_' + $mdUtil.nextUid();
        var panelPromise = $mdExpansionPanel().waitFor(componentId);
        element.attr('md-component-id', componentId);

        var linkFunc = $compile(element);
        if (options.controller) {
          angular.extend(locals, options.locals || {});
          locals.$scope = scope;
          locals.$panel = panelPromise;
          var invokeCtrl = $controller(options.controller, locals, true);
          var ctrl = invokeCtrl();
          element.data('$ngControllerController', ctrl);
          element.children().data('$ngControllerController', ctrl);
          if (options.controllerAs) {
            scope[options.controllerAs] = ctrl;
          }
        }

        // link after the element is added so we can find card manager directive
        instance.$element.append(element);
        linkFunc(scope);

        panelPromise.then(function (instance) {
          deffered.resolve(instance);
        });
      });

      return deffered.promise;
    }


    function validateOptions(options) {
      if (typeof options !== 'object' || options === null) {
        throw Error('$mdExapnsionPanelGroup.add()/.register() : Requires an options object to be passed in');
      }

      // if none of these exist then a dialog box cannot be created
      if (!options.template && !options.templateUrl) {
        throw Error('$mdExapnsionPanelGroup.add()/.register() : Is missing required paramters to create. Required One of the following: template, templateUrl');
      }
    }



    function getTemplate(options, callback) {
      var template;

      if (options.templateUrl !== undefined) {
        $templateRequest(options.templateUrl)
          .then(function(response) {
            callback(response);
          });
      } else {
        callback(options.template);
      }
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelHeader', expansionPanelHeaderDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelHeader
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelHeader` is nested inside of `mdExpansionPanelExpanded` and contains content you want in place of the collapsed content
 * this is optional
 *
 * @param {boolean=} md-no-sticky - add this aatribute to disable sticky
 **/
expansionPanelHeaderDirective.$inject = [];
function expansionPanelHeaderDirective() {
  var directive = {
    restrict: 'E',
    transclude: true,
    template: '<div class="md-expansion-panel-header-container" ng-transclude></div>',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;



  function link(scope, element, attrs, expansionPanelCtrl) {
    var isStuck = false;
    var noSticky = attrs.mdNoSticky !== undefined;
    var container = angular.element(element[0].querySelector('.md-expansion-panel-header-container'));

    expansionPanelCtrl.registerHeader({
      show: show,
      hide: hide,
      noSticky: noSticky,
      onScroll: onScroll,
      onResize: onResize
    });


    function show() {

    }
    function hide() {
      unstick();
    }


    function onScroll(top, bottom, transformTop) {
      var offset;
      var panelbottom;
      var bounds = element[0].getBoundingClientRect();


      if (bounds.top < top) {
        offset = top - transformTop;
        panelbottom = element[0].parentNode.getBoundingClientRect().bottom - top - bounds.height;
        if (panelbottom < 0) {
          offset += panelbottom;
        }

        // set container width because element becomes postion fixed
        container.css('width', element[0].offsetWidth + 'px');
        container.css('top', offset + 'px');

        // set element height so it does not shink when container is position fixed
        element.css('height', container[0].offsetHeight + 'px');

        element.removeClass('md-no-stick');
        element.addClass('md-stick');
        isStuck = true;
      } else if (isStuck === true) {
        unstick();
      }
    }

    function onResize(width) {
      if (isStuck === false) { return; }
      container.css('width', width + 'px');
    }


    function unstick() {
      isStuck = false;
      container.css('width', '');
      element.css('height', '');
      element.css('top', '');
      element.removeClass('md-stick');
      element.addClass('md-no-stick');
    }
  }
}
}());
(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelIcon', mdExpansionPanelIconDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelIcon
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelIcon` can be used in both `md-expansion-panel-collapsed` and `md-expansion-panel-header` as the first or last element.
 * Adding this will provide a animated arrow for expanded and collapsed states
 **/
function mdExpansionPanelIconDirective() {
  var directive = {
    restrict: 'E',
    template: '<md-icon class="md-expansion-panel-icon" md-svg-icon="icons/ic_keyboard_arrow_right_black_24px.svg"></md-icon>',
    replace: true
  };
  return directive;
}
}());