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

    $attrs.$observe('disabled', function(disabled) {
      isDisabled = (typeof disabled === 'string' && disabled !== 'false') ? true : false;

      if (isDisabled === true) {
        $element.attr('tabindex', '-1');
      } else {
        $element.attr('tabindex', '0');
      }
    });

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