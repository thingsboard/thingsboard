/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.tabs');
goog.require('ngmaterial.components.icon');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.tabs
 * @description
 *
 *  Tabs, created with the `<md-tabs>` directive provide *tabbed* navigation with different styles.
 *  The Tabs component consists of clickable tabs that are aligned horizontally side-by-side.
 *
 *  Features include support for:
 *
 *  - static or dynamic tabs,
 *  - responsive designs,
 *  - accessibility support (ARIA),
 *  - tab pagination,
 *  - external or internal tab content,
 *  - focus indicators and arrow-key navigations,
 *  - programmatic lookup and access to tab controllers, and
 *  - dynamic transitions through different tab contents.
 *
 */
/*
 * @see js folder for tabs implementation
 */
angular.module('material.components.tabs', [
  'material.core',
  'material.components.icon'
]);

angular
.module('material.components.tabs')
.service('MdTabsPaginationService', MdTabsPaginationService);

/**
 * @private
 * @module material.components.tabs
 * @name MdTabsPaginationService
 * @description Provides many standalone functions to ease in pagination calculations.
 *
 * Most functions accept the elements and the current offset.
 *
 * The `elements` parameter is typically the value returned from the `getElements()` function of the
 * tabsController.
 *
 * The `offset` parameter is always positive regardless of LTR or RTL (we simply make the LTR one
 * negative when we apply our transform). This is typically the `ctrl.leftOffset` variable in the
 * tabsController.
 *
 * @returns MdTabsPaginationService
 * @constructor
 */
function MdTabsPaginationService() {
  return {
    decreasePageOffset: decreasePageOffset,
    increasePageOffset: increasePageOffset,
    getTabOffsets: getTabOffsets,
    getTotalTabsWidth: getTotalTabsWidth
  };

  /**
   * Returns the offset for the next decreasing page.
   *
   * @param elements
   * @param currentOffset
   * @returns {number}
   */
  function decreasePageOffset(elements, currentOffset) {
    var canvas       = elements.canvas,
        tabOffsets   = getTabOffsets(elements),
        i, firstVisibleTabOffset;

    // Find the first fully visible tab in offset range
    for (i = 0; i < tabOffsets.length; i++) {
      if (tabOffsets[i] >= currentOffset) {
        firstVisibleTabOffset = tabOffsets[i];
        break;
      }
    }

    // Return (the first visible tab offset - the tabs container width) without going negative
    return Math.max(0, firstVisibleTabOffset - canvas.clientWidth);
  }

  /**
   * Returns the offset for the next increasing page.
   *
   * @param elements
   * @param currentOffset
   * @returns {number}
   */
  function increasePageOffset(elements, currentOffset) {
    var canvas       = elements.canvas,
        maxOffset    = getTotalTabsWidth(elements) - canvas.clientWidth,
        tabOffsets   = getTabOffsets(elements),
        i, firstHiddenTabOffset;

    // Find the first partially (or fully) invisible tab
    for (i = 0; i < tabOffsets.length, tabOffsets[i] <= currentOffset + canvas.clientWidth; i++) {
      firstHiddenTabOffset = tabOffsets[i];
    }

    // Return the offset of the first hidden tab, or the maximum offset (whichever is smaller)
    return Math.min(maxOffset, firstHiddenTabOffset);
  }

  /**
   * Returns the offsets of all of the tabs based on their widths.
   *
   * @param elements
   * @returns {number[]}
   */
  function getTabOffsets(elements) {
    var i, tab, currentOffset = 0, offsets = [];

    for (i = 0; i < elements.tabs.length; i++) {
      tab = elements.tabs[i];
      offsets.push(currentOffset);
      currentOffset += tab.offsetWidth;
    }

    return offsets;
  }

  /**
   * Sum the width of all tabs.
   *
   * @param elements
   * @returns {number}
   */
  function getTotalTabsWidth(elements) {
    var sum = 0, i, tab;

    for (i = 0; i < elements.tabs.length; i++) {
      tab = elements.tabs[i];
      sum += tab.offsetWidth;
    }

    return sum;
  }

}

/**
 * @ngdoc directive
 * @name mdTab
 * @module material.components.tabs
 *
 * @restrict E
 *
 * @description
 * The `<md-tab>` is a nested directive used within `<md-tabs>` to specify a tab with a **label**
 * and optional *view content*.
 *
 * If the `label` attribute is not specified, then an optional `<md-tab-label>` tag can be used to
 * specify more complex tab header markup. If neither the **label** nor the **md-tab-label** are
 * specified, then the nested markup of the `<md-tab>` is used as the tab header markup.
 *
 * Please note that if you use `<md-tab-label>`, your content **MUST** be wrapped in the
 * `<md-tab-body>` tag.  This is to define a clear separation between the tab content and the tab
 * label.
 *
 * This container is used by the TabsController to show/hide the active tab's content view. This
 * synchronization is automatically managed by the internal TabsController whenever the tab
 * selection changes. Selection changes can be initiated via data binding changes, programmatic
 * invocation, or user gestures.
 *
 * @param {string=} label Optional attribute to specify a simple string as the tab label
 * @param {boolean=} ng-disabled If present and expression evaluates to truthy, disabled tab
 *  selection.
 * @param {string=} md-tab-class Optional attribute to specify a class that will be applied to the
 *  tab's button
 * @param {expression=} md-on-deselect Expression to be evaluated after the tab has been
 *  de-selected.
 * @param {expression=} md-on-select Expression to be evaluated after the tab has been selected.
 * @param {boolean=} md-active When true, sets the active tab.  Note: There can only be one active
 *  tab at a time.
 *
 *
 * @usage
 *
 * <hljs lang="html">
 * <md-tab label="My Tab" md-tab-class="my-content-tab" ng-disabled md-on-select="onSelect()"
 *         md-on-deselect="onDeselect()">
 *   <h3>My Tab content</h3>
 * </md-tab>
 *
 * <md-tab>
 *   <md-tab-label>
 *     <h3>My Tab</h3>
 *   </md-tab-label>
 *   <md-tab-body>
 *     <p>
 *       Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque
 *       laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi
 *       architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit
 *       aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione
 *       voluptatem sequi nesciunt.
 *     </p>
 *   </md-tab-body>
 * </md-tab>
 * </hljs>
 *
 */
angular
    .module('material.components.tabs')
    .directive('mdTab', MdTab);

function MdTab () {
  return {
    require:  '^?mdTabs',
    terminal: true,
    compile:  function (element, attr) {
      var label = firstChild(element, 'md-tab-label'),
          body  = firstChild(element, 'md-tab-body');

      if (label.length === 0) {
        label = angular.element('<md-tab-label></md-tab-label>');
        if (attr.label) label.text(attr.label);
        else label.append(element.contents());

        if (body.length === 0) {
          var contents = element.contents().detach();
          body         = angular.element('<md-tab-body></md-tab-body>');
          body.append(contents);
        }
      }

      element.append(label);
      if (body.html()) element.append(body);

      return postLink;
    },
    scope:    {
      active:   '=?mdActive',
      disabled: '=?ngDisabled',
      select:   '&?mdOnSelect',
      deselect: '&?mdOnDeselect',
      tabClass: '@mdTabClass'
    }
  };

  function postLink (scope, element, attr, ctrl) {
    if (!ctrl) return;
    var index = ctrl.getTabElementIndex(element),
        body  = firstChild(element, 'md-tab-body').remove(),
        label = firstChild(element, 'md-tab-label').remove(),
        data  = ctrl.insertTab({
          scope:    scope,
          parent:   scope.$parent,
          index:    index,
          element:  element,
          template: body.html(),
          label:    label.html()
        }, index);

    scope.select   = scope.select || angular.noop;
    scope.deselect = scope.deselect || angular.noop;

    scope.$watch('active', function (active) { if (active) ctrl.select(data.getIndex(), true); });
    scope.$watch('disabled', function () { ctrl.refreshIndex(); });
    scope.$watch(
        function () {
          return ctrl.getTabElementIndex(element);
        },
        function (newIndex) {
          data.index = newIndex;
          ctrl.updateTabOrder();
        }
    );
    scope.$on('$destroy', function () { ctrl.removeTab(data); });
  }

  function firstChild (element, tagName) {
    var children = element[0].children;
    for (var i = 0, len = children.length; i < len; i++) {
      var child = children[i];
      if (child.tagName === tagName.toUpperCase()) return angular.element(child);
    }
    return angular.element();
  }
}

angular
    .module('material.components.tabs')
    .directive('mdTabItem', MdTabItem);

function MdTabItem () {
  return {
    require: '^?mdTabs',
    link:    function link (scope, element, attr, ctrl) {
      if (!ctrl) return;
      ctrl.attachRipple(scope, element);
    }
  };
}

angular
    .module('material.components.tabs')
    .directive('mdTabLabel', MdTabLabel);

function MdTabLabel () {
  return { terminal: true };
}



MdTabScroll['$inject'] = ["$parse"];angular.module('material.components.tabs')
    .directive('mdTabScroll', MdTabScroll);

function MdTabScroll ($parse) {
  return {
    restrict: 'A',
    compile: function ($element, attr) {
      var fn = $parse(attr.mdTabScroll, null, true);
      return function ngEventHandler (scope, element) {
        element.on('wheel', function (event) {
          scope.$apply(function () { fn(scope, { $event: event }); });
        });
      };
    }
  };
}


MdTabsController['$inject'] = ["$scope", "$element", "$window", "$mdConstant", "$mdTabInkRipple", "$mdUtil", "$animateCss", "$attrs", "$compile", "$mdTheming", "$mdInteraction", "$timeout", "MdTabsPaginationService"];angular
    .module('material.components.tabs')
    .controller('MdTabsController', MdTabsController);

/**
 * ngInject
 */
function MdTabsController ($scope, $element, $window, $mdConstant, $mdTabInkRipple, $mdUtil,
                           $animateCss, $attrs, $compile, $mdTheming, $mdInteraction, $timeout,
                           MdTabsPaginationService) {
  // define private properties
  var ctrl      = this,
      locked    = false,
      queue     = [],
      destroyed = false,
      loaded    = false;

  // Define public methods
  ctrl.$onInit            = $onInit;
  ctrl.updatePagination   = $mdUtil.debounce(updatePagination, 100);
  ctrl.redirectFocus      = redirectFocus;
  ctrl.attachRipple       = attachRipple;
  ctrl.insertTab          = insertTab;
  ctrl.removeTab          = removeTab;
  ctrl.select             = select;
  ctrl.scroll             = scroll;
  ctrl.nextPage           = nextPage;
  ctrl.previousPage       = previousPage;
  ctrl.keydown            = keydown;
  ctrl.canPageForward     = canPageForward;
  ctrl.canPageBack        = canPageBack;
  ctrl.refreshIndex       = refreshIndex;
  ctrl.incrementIndex     = incrementIndex;
  ctrl.getTabElementIndex = getTabElementIndex;
  ctrl.updateInkBarStyles = $mdUtil.debounce(updateInkBarStyles, 100);
  ctrl.updateTabOrder     = $mdUtil.debounce(updateTabOrder, 100);
  ctrl.getFocusedTabId    = getFocusedTabId;

  // For AngularJS 1.4 and older, where there are no lifecycle hooks but bindings are pre-assigned,
  // manually call the $onInit hook.
  if (angular.version.major === 1 && angular.version.minor <= 4) {
    this.$onInit();
  }

  /**
   * AngularJS Lifecycle hook for newer AngularJS versions.
   * Bindings are not guaranteed to have been assigned in the controller, but they are in the
   * $onInit hook.
   */
  function $onInit() {
    // Define one-way bindings
    defineOneWayBinding('stretchTabs', handleStretchTabs);

    // Define public properties with change handlers
    defineProperty('focusIndex', handleFocusIndexChange, ctrl.selectedIndex || 0);
    defineProperty('offsetLeft', handleOffsetChange, 0);
    defineProperty('hasContent', handleHasContent, false);
    defineProperty('maxTabWidth', handleMaxTabWidth, getMaxTabWidth());
    defineProperty('shouldPaginate', handleShouldPaginate, false);

    // Define boolean attributes
    defineBooleanAttribute('noInkBar', handleInkBar);
    defineBooleanAttribute('dynamicHeight', handleDynamicHeight);
    defineBooleanAttribute('noPagination');
    defineBooleanAttribute('swipeContent');
    defineBooleanAttribute('noDisconnect');
    defineBooleanAttribute('autoselect');
    defineBooleanAttribute('noSelectClick');
    defineBooleanAttribute('centerTabs', handleCenterTabs, false);
    defineBooleanAttribute('enableDisconnect');

    // Define public properties
    ctrl.scope             = $scope;
    ctrl.parent            = $scope.$parent;
    ctrl.tabs              = [];
    ctrl.lastSelectedIndex = null;
    ctrl.hasFocus          = false;
    ctrl.styleTabItemFocus = false;
    ctrl.shouldCenterTabs  = shouldCenterTabs();
    ctrl.tabContentPrefix  = 'tab-content-';
    ctrl.navigationHint = 'Use the left and right arrow keys to navigate between tabs';

    // Setup the tabs controller after all bindings are available.
    setupTabsController();
  }

  /**
   * Perform setup for the controller, setup events and watcher(s)
   */
  function setupTabsController () {
    ctrl.selectedIndex = ctrl.selectedIndex || 0;
    compileTemplate();
    configureWatchers();
    bindEvents();
    $mdTheming($element);
    $mdUtil.nextTick(function () {
      updateHeightFromContent();
      adjustOffset();
      updateInkBarStyles();
      ctrl.tabs[ ctrl.selectedIndex ] && ctrl.tabs[ ctrl.selectedIndex ].scope.select();
      loaded = true;
      updatePagination();
    });
  }

  /**
   * Compiles the template provided by the user.  This is passed as an attribute from the tabs
   * directive's template function.
   */
  function compileTemplate () {
    var template = $attrs.$mdTabsTemplate,
        element  = angular.element($element[0].querySelector('md-tab-data'));

    element.html(template);
    $compile(element.contents())(ctrl.parent);
    delete $attrs.$mdTabsTemplate;
  }

  /**
   * Binds events used by the tabs component.
   */
  function bindEvents () {
    angular.element($window).on('resize', handleWindowResize);
    $scope.$on('$destroy', cleanup);
  }

  /**
   * Configure watcher(s) used by Tabs
   */
  function configureWatchers () {
    $scope.$watch('$mdTabsCtrl.selectedIndex', handleSelectedIndexChange);
  }

  /**
   * Creates a one-way binding manually rather than relying on AngularJS's isolated scope
   * @param key
   * @param handler
   */
  function defineOneWayBinding (key, handler) {
    var attr = $attrs.$normalize('md-' + key);
    if (handler) defineProperty(key, handler);
    $attrs.$observe(attr, function (newValue) { ctrl[ key ] = newValue; });
  }

  /**
   * Defines boolean attributes with default value set to true. I.e. md-stretch-tabs with no value
   * will be treated as being truthy.
   * @param {string} key
   * @param {Function} handler
   */
  function defineBooleanAttribute (key, handler) {
    var attr = $attrs.$normalize('md-' + key);
    if (handler) defineProperty(key, handler);
    if ($attrs.hasOwnProperty(attr)) updateValue($attrs[attr]);
    $attrs.$observe(attr, updateValue);
    function updateValue (newValue) {
      ctrl[ key ] = newValue !== 'false';
    }
  }

  /**
   * Remove any events defined by this controller
   */
  function cleanup () {
    destroyed = true;
    angular.element($window).off('resize', handleWindowResize);
  }

  // Change handlers

  /**
   * Toggles stretch tabs class and updates inkbar when tab stretching changes.
   */
  function handleStretchTabs () {
    var elements = getElements();
    angular.element(elements.wrapper).toggleClass('md-stretch-tabs', shouldStretchTabs());
    updateInkBarStyles();
  }

  /**
   * Update the value of ctrl.shouldCenterTabs.
   */
  function handleCenterTabs () {
    ctrl.shouldCenterTabs = shouldCenterTabs();
  }

  /**
   * @param {number} newWidth new max tab width in pixels
   * @param {number} oldWidth previous max tab width in pixels
   */
  function handleMaxTabWidth (newWidth, oldWidth) {
    if (newWidth !== oldWidth) {
      var elements = getElements();

      // Set the max width for the real tabs
      angular.forEach(elements.tabs, function(tab) {
        tab.style.maxWidth = newWidth + 'px';
      });

      // Set the max width for the dummy tabs too
      angular.forEach(elements.dummies, function(tab) {
        tab.style.maxWidth = newWidth + 'px';
      });

      $mdUtil.nextTick(ctrl.updateInkBarStyles);
    }
  }

  function handleShouldPaginate (newValue, oldValue) {
    if (newValue !== oldValue) {
      ctrl.maxTabWidth      = getMaxTabWidth();
      ctrl.shouldCenterTabs = shouldCenterTabs();
      $mdUtil.nextTick(function () {
        ctrl.maxTabWidth = getMaxTabWidth();
        adjustOffset(ctrl.selectedIndex);
      });
    }
  }

  /**
   * Add/remove the `md-no-tab-content` class depending on `ctrl.hasContent`
   * @param {boolean} hasContent
   */
  function handleHasContent (hasContent) {
    $element[ hasContent ? 'removeClass' : 'addClass' ]('md-no-tab-content');
  }

  /**
   * Apply ctrl.offsetLeft to the paging element when it changes
   * @param {string|number} left
   */
  function handleOffsetChange (left) {
    var newValue = ((ctrl.shouldCenterTabs || isRtl() ? '' : '-') + left + 'px');

    // Fix double-negative which can happen with RTL support
    newValue = newValue.replace('--', '');

    angular.element(getElements().paging).css($mdConstant.CSS.TRANSFORM,
                                              'translate(' + newValue + ', 0)');
    $scope.$broadcast('$mdTabsPaginationChanged');
  }

  /**
   * Update the UI whenever `ctrl.focusIndex` is updated
   * @param {number} newIndex
   * @param {number} oldIndex
   */
  function handleFocusIndexChange (newIndex, oldIndex) {
    if (newIndex === oldIndex) return;
    if (!getElements().tabs[ newIndex ]) return;
    adjustOffset();
    redirectFocus();
  }

  /**
   * Update the UI whenever the selected index changes. Calls user-defined select/deselect methods.
   * @param {number} newValue selected index's new value
   * @param {number} oldValue selected index's previous value
   */
  function handleSelectedIndexChange (newValue, oldValue) {
    if (newValue === oldValue) return;

    ctrl.selectedIndex     = getNearestSafeIndex(newValue);
    ctrl.lastSelectedIndex = oldValue;
    ctrl.updateInkBarStyles();
    updateHeightFromContent();
    adjustOffset(newValue);
    $scope.$broadcast('$mdTabsChanged');
    ctrl.tabs[ oldValue ] && ctrl.tabs[ oldValue ].scope.deselect();
    ctrl.tabs[ newValue ] && ctrl.tabs[ newValue ].scope.select();
  }

  function getTabElementIndex(tabEl){
    var tabs = $element[0].getElementsByTagName('md-tab');
    return Array.prototype.indexOf.call(tabs, tabEl[0]);
  }

  /**
   * Queues up a call to `handleWindowResize` when a resize occurs while the tabs component is
   * hidden.
   */
  function handleResizeWhenVisible () {
    // if there is already a watcher waiting for resize, do nothing
    if (handleResizeWhenVisible.watcher) return;
    // otherwise, we will abuse the $watch function to check for visible
    handleResizeWhenVisible.watcher = $scope.$watch(function () {
      // since we are checking for DOM size, we use $mdUtil.nextTick() to wait for after the DOM updates
      $mdUtil.nextTick(function () {
        // if the watcher has already run (ie. multiple digests in one cycle), do nothing
        if (!handleResizeWhenVisible.watcher) return;

        if ($element.prop('offsetParent')) {
          handleResizeWhenVisible.watcher();
          handleResizeWhenVisible.watcher = null;

          handleWindowResize();
        }
      }, false);
    });
  }

  // Event handlers / actions

  /**
   * Handle user keyboard interactions
   * @param {KeyboardEvent} event keydown event
   */
  function keydown (event) {
    switch (event.keyCode) {
      case $mdConstant.KEY_CODE.LEFT_ARROW:
        event.preventDefault();
        incrementIndex(-1, true);
        break;
      case $mdConstant.KEY_CODE.RIGHT_ARROW:
        event.preventDefault();
        incrementIndex(1, true);
        break;
      case $mdConstant.KEY_CODE.SPACE:
      case $mdConstant.KEY_CODE.ENTER:
        event.preventDefault();
        if (!locked) select(ctrl.focusIndex);
        break;
      case $mdConstant.KEY_CODE.TAB:
        // On tabbing out of the tablist, reset hasFocus to reset ng-focused and
        // its md-focused class if the focused tab is not the active tab.
        if (ctrl.focusIndex !== ctrl.selectedIndex) {
          ctrl.focusIndex = ctrl.selectedIndex;
        }
        break;
    }
  }

  /**
   * Update the selected index. Triggers a click event on the original `md-tab` element in order
   * to fire user-added click events if canSkipClick or `md-no-select-click` are false.
   * @param index
   * @param canSkipClick Optionally allow not firing the click event if `md-no-select-click` is also true.
   */
  function select (index, canSkipClick) {
    if (!locked) ctrl.focusIndex = ctrl.selectedIndex = index;
    // skip the click event if noSelectClick is enabled
    if (canSkipClick && ctrl.noSelectClick) return;
    // nextTick is required to prevent errors in user-defined click events
    $mdUtil.nextTick(function () {
      ctrl.tabs[ index ].element.triggerHandler('click');
    }, false);
  }

  /**
   * When pagination is on, this makes sure the selected index is in view.
   * @param {WheelEvent} event
   */
  function scroll (event) {
    if (!ctrl.shouldPaginate) return;
    event.preventDefault();
    if (event.deltaY) {
      ctrl.offsetLeft = fixOffset(ctrl.offsetLeft + event.deltaY);
    } else if (event.deltaX) {
      ctrl.offsetLeft = fixOffset(ctrl.offsetLeft + event.deltaX);
    }
  }

  /**
   * Slides the tabs over approximately one page forward.
   */
  function nextPage () {
    if (!ctrl.canPageForward()) { return; }

    var newOffset = MdTabsPaginationService.increasePageOffset(getElements(), ctrl.offsetLeft);

    ctrl.offsetLeft = fixOffset(newOffset);
  }

  /**
   * Slides the tabs over approximately one page backward.
   */
  function previousPage () {
    if (!ctrl.canPageBack()) { return; }

    var newOffset = MdTabsPaginationService.decreasePageOffset(getElements(), ctrl.offsetLeft);

    // Set the new offset
    ctrl.offsetLeft = fixOffset(newOffset);
  }

  /**
   * Update size calculations when the window is resized.
   */
  function handleWindowResize () {
    ctrl.lastSelectedIndex = ctrl.selectedIndex;
    ctrl.offsetLeft        = fixOffset(ctrl.offsetLeft);

    $mdUtil.nextTick(function () {
      ctrl.updateInkBarStyles();
      updatePagination();
    });
  }

  /**
   * Hides or shows the tabs ink bar.
   * @param {boolean} hide A Boolean (not just truthy/falsy) value to determine whether the class
   * should be added or removed.
   */
  function handleInkBar (hide) {
    angular.element(getElements().inkBar).toggleClass('ng-hide', hide);
  }

  /**
   * Enables or disables tabs dynamic height.
   * @param {boolean} value A Boolean (not just truthy/falsy) value to determine whether the class
   * should be added or removed.
   */
  function handleDynamicHeight (value) {
    $element.toggleClass('md-dynamic-height', value);
  }

  /**
   * Remove a tab from the data and select the nearest valid tab.
   * @param {Object} tabData tab to remove
   */
  function removeTab (tabData) {
    if (destroyed) return;
    var selectedIndex = ctrl.selectedIndex,
        tab           = ctrl.tabs.splice(tabData.getIndex(), 1)[ 0 ];
    refreshIndex();
    // when removing a tab, if the selected index did not change, we have to manually trigger the
    //   tab select/deselect events
    if (ctrl.selectedIndex === selectedIndex) {
      tab.scope.deselect();
      ctrl.tabs[ ctrl.selectedIndex ] && ctrl.tabs[ ctrl.selectedIndex ].scope.select();
    }
    $mdUtil.nextTick(function () {
      updatePagination();
      ctrl.offsetLeft = fixOffset(ctrl.offsetLeft);
    });
  }

  /**
   * Create an entry in the tabs array for a new tab at the specified index.
   * @param {Object} tabData tab to insert
   * @param {number} index location to insert the new tab
   * @returns {Object} the inserted tab
   */
  function insertTab (tabData, index) {
    var hasLoaded = loaded;
    var proto = {
          getIndex:     function () { return ctrl.tabs.indexOf(tab); },
          isActive:     function () { return this.getIndex() === ctrl.selectedIndex; },
          isLeft:       function () { return this.getIndex() < ctrl.selectedIndex; },
          isRight:      function () { return this.getIndex() > ctrl.selectedIndex; },
          shouldRender: function () { return !ctrl.noDisconnect || this.isActive(); },
          hasFocus:     function () {
            return ctrl.styleTabItemFocus
                && ctrl.hasFocus && this.getIndex() === ctrl.focusIndex;
          },
          id:           $mdUtil.nextUid(),
          hasContent: !!(tabData.template && tabData.template.trim())
    };
    var tab = angular.extend(proto, tabData);

    if (angular.isDefined(index)) {
      ctrl.tabs.splice(index, 0, tab);
    } else {
      ctrl.tabs.push(tab);
    }
    processQueue();
    updateHasContent();

    $mdUtil.nextTick(function () {
      updatePagination();
      setAriaControls(tab);

      // if autoselect is enabled, select the newly added tab
      if (hasLoaded && ctrl.autoselect) {
        $mdUtil.nextTick(function () {
          $mdUtil.nextTick(function () { select(ctrl.tabs.indexOf(tab)); });
        });
      }
    });
    return tab;
  }

  // Getter methods

  /**
   * Gathers references to all of the DOM elements used by this controller.
   * @returns {Object}
   */
  function getElements () {
    var elements = {};
    var node = $element[0];

    // gather tab bar elements
    elements.wrapper = node.querySelector('md-tabs-wrapper');
    elements.canvas  = elements.wrapper.querySelector('md-tabs-canvas');
    elements.paging  = elements.canvas.querySelector('md-pagination-wrapper');
    elements.inkBar  = elements.paging.querySelector('md-ink-bar');
    elements.nextButton = node.querySelector('md-next-button');
    elements.prevButton = node.querySelector('md-prev-button');

    elements.contents = node.querySelectorAll('md-tabs-content-wrapper > md-tab-content');
    elements.tabs    = elements.paging.querySelectorAll('md-tab-item');
    elements.dummies = elements.canvas.querySelectorAll('md-dummy-tab');

    return elements;
  }

  /**
   * Determines whether or not the left pagination arrow should be enabled.
   * @returns {boolean}
   */
  function canPageBack () {
    // This works for both LTR and RTL
    return ctrl.offsetLeft > 0;
  }

  /**
   * Determines whether or not the right pagination arrow should be enabled.
   * @returns {*|boolean}
   */
  function canPageForward () {
    var elements = getElements();
    var lastTab = elements.tabs[ elements.tabs.length - 1 ];

    if (isRtl()) {
      return ctrl.offsetLeft < elements.paging.offsetWidth - elements.canvas.offsetWidth;
    }

    return lastTab && lastTab.offsetLeft + lastTab.offsetWidth > elements.canvas.clientWidth +
        ctrl.offsetLeft;
  }

  /**
   * Returns currently focused tab item's element ID
   */
  function getFocusedTabId() {
    var focusedTab = ctrl.tabs[ctrl.focusIndex];
    if (!focusedTab || !focusedTab.id) {
      return null;
    }
    return 'tab-item-' + focusedTab.id;
  }

  /**
   * Determines if the UI should stretch the tabs to fill the available space.
   * @returns {*}
   */
  function shouldStretchTabs () {
    switch (ctrl.stretchTabs) {
      case 'always':
        return true;
      case 'never':
        return false;
      default:
        return !ctrl.shouldPaginate
            && $window.matchMedia('(max-width: 600px)').matches;
    }
  }

  /**
   * Determines if the tabs should appear centered.
   * @returns {boolean}
   */
  function shouldCenterTabs () {
    return ctrl.centerTabs && !ctrl.shouldPaginate;
  }

  /**
   * Determines if pagination is necessary to display the tabs within the available space.
   * @returns {boolean} true if pagination is necessary, false otherwise
   */
  function shouldPaginate () {
    var shouldPaginate;
    if (ctrl.noPagination || !loaded) return false;
    var canvasWidth = $element.prop('clientWidth');

    angular.forEach(getElements().tabs, function (tab) {
      canvasWidth -= tab.offsetWidth;
    });

    shouldPaginate = canvasWidth < 0;
    // Work around width calculation issues on IE11 when pagination is enabled.
    // Don't do this on other browsers because it breaks scroll to new tab animation.
    if ($mdUtil.msie) {
      if (shouldPaginate) {
        getElements().paging.style.width = '999999px';
      } else {
        getElements().paging.style.width = undefined;
      }
    }
    return shouldPaginate;
  }

  /**
   * Finds the nearest tab index that is available. This is primarily used for when the active
   * tab is removed.
   * @param newIndex
   * @returns {*}
   */
  function getNearestSafeIndex (newIndex) {
    if (newIndex === -1) return -1;
    var maxOffset = Math.max(ctrl.tabs.length - newIndex, newIndex),
        i, tab;
    for (i = 0; i <= maxOffset; i++) {
      tab = ctrl.tabs[ newIndex + i ];
      if (tab && (tab.scope.disabled !== true)) return tab.getIndex();
      tab = ctrl.tabs[ newIndex - i ];
      if (tab && (tab.scope.disabled !== true)) return tab.getIndex();
    }
    return newIndex;
  }

  // Utility methods

  /**
   * Defines a property using a getter and setter in order to trigger a change handler without
   * using `$watch` to observe changes.
   * @param {PropertyKey} key
   * @param {Function} handler
   * @param {any} value
   */
  function defineProperty (key, handler, value) {
    Object.defineProperty(ctrl, key, {
      get: function () { return value; },
      set: function (newValue) {
        var oldValue = value;
        value        = newValue;
        handler && handler(newValue, oldValue);
      }
    });
  }

  /**
   * Updates whether or not pagination should be displayed.
   */
  function updatePagination () {
    ctrl.maxTabWidth = getMaxTabWidth();
    ctrl.shouldPaginate = shouldPaginate();
  }

  /**
   * @param {Array<HTMLElement>} tabs tab item elements for use in computing total width
   * @returns {number} the width of the tabs in the specified array in pixels
   */
  function calcTabsWidth(tabs) {
    var width = 0;

    angular.forEach(tabs, function (tab) {
      // Uses the larger value between `getBoundingClientRect().width` and `offsetWidth`.  This
      // prevents `offsetWidth` value from being rounded down and causing wrapping issues, but
      // also handles scenarios where `getBoundingClientRect()` is inaccurate (ie. tabs inside
      // of a dialog).
      width += Math.max(tab.offsetWidth, tab.getBoundingClientRect().width);
    });

    return Math.ceil(width);
  }

  /**
   * @returns {number} either the max width as constrained by the container or the max width from
   * the 2017 version of the Material Design spec.
   */
  function getMaxTabWidth() {
    var elements = getElements(),
      containerWidth = elements.canvas.clientWidth,

      // See https://material.io/archive/guidelines/components/tabs.html#tabs-specs
      specMax = 264;

    // Do the spec maximum, or the canvas width; whichever is *smaller* (tabs larger than the canvas
    // width can break the pagination) but not less than 0
    return Math.max(0, Math.min(containerWidth - 1, specMax));
  }

  /**
   * Re-orders the tabs and updates the selected and focus indexes to their new positions.
   * This is triggered by `tabDirective.js` when the user's tabs have been re-ordered.
   */
  function updateTabOrder () {
    var selectedItem   = ctrl.tabs[ ctrl.selectedIndex ],
        focusItem      = ctrl.tabs[ ctrl.focusIndex ];
    ctrl.tabs          = ctrl.tabs.sort(function (a, b) {
      return a.index - b.index;
    });
    ctrl.selectedIndex = ctrl.tabs.indexOf(selectedItem);
    ctrl.focusIndex    = ctrl.tabs.indexOf(focusItem);
  }

  /**
   * This moves the selected or focus index left or right. This is used by the keydown handler.
   * @param {number} inc amount to increment
   * @param {boolean} focus true to increment the focus index, false to increment the selected index
   */
  function incrementIndex (inc, focus) {
    var newIndex,
        key   = focus ? 'focusIndex' : 'selectedIndex',
        index = ctrl[ key ];
    for (newIndex = index + inc;
         ctrl.tabs[ newIndex ] && ctrl.tabs[ newIndex ].scope.disabled;
         newIndex += inc) { /* do nothing */ }

    newIndex = (index + inc + ctrl.tabs.length) % ctrl.tabs.length;

    if (ctrl.tabs[ newIndex ]) {
      ctrl[ key ] = newIndex;
    }
  }

  /**
   * This is used to forward focus to tab container elements. This method is necessary to avoid
   * animation issues when attempting to focus an item that is out of view.
   */
  function redirectFocus () {
    ctrl.styleTabItemFocus = ($mdInteraction.getLastInteractionType() === 'keyboard');
    var tabToFocus = getElements().tabs[ctrl.focusIndex];
    if (tabToFocus) {
      tabToFocus.focus();
    }
  }

  /**
   * Forces the pagination to move the focused tab into view.
   * @param {number=} index of tab to have its offset adjusted
   */
  function adjustOffset (index) {
    var elements = getElements();

    if (!angular.isNumber(index)) index = ctrl.focusIndex;
    if (!elements.tabs[ index ]) return;
    if (ctrl.shouldCenterTabs) return;
    var tab         = elements.tabs[ index ],
        left        = tab.offsetLeft,
        right       = tab.offsetWidth + left,
        extraOffset = 32;

    // If we are selecting the first tab (in LTR and RTL), always set the offset to 0
    if (index === 0) {
      ctrl.offsetLeft = 0;
      return;
    }

    if (isRtl()) {
      var tabWidthsBefore = calcTabsWidth(Array.prototype.slice.call(elements.tabs, 0, index));
      var tabWidthsIncluding = calcTabsWidth(Array.prototype.slice.call(elements.tabs, 0, index + 1));

      ctrl.offsetLeft = Math.min(ctrl.offsetLeft, fixOffset(tabWidthsBefore));
      ctrl.offsetLeft = Math.max(ctrl.offsetLeft, fixOffset(tabWidthsIncluding - elements.canvas.clientWidth));
    } else {
      ctrl.offsetLeft = Math.max(ctrl.offsetLeft, fixOffset(right - elements.canvas.clientWidth + extraOffset));
      ctrl.offsetLeft = Math.min(ctrl.offsetLeft, fixOffset(left));
    }
  }

  /**
   * Iterates through all queued functions and clears the queue. This is used for functions that
   * are called before the UI is ready, such as size calculations.
   */
  function processQueue () {
    queue.forEach(function (func) { $mdUtil.nextTick(func); });
    queue = [];
  }

  /**
   * Determines if the tab content area is needed.
   */
  function updateHasContent () {
    var hasContent = false;
    var i;

    for (i = 0; i < ctrl.tabs.length; i++) {
      if (ctrl.tabs[i].hasContent) {
        hasContent = true;
        break;
      }
    }

    ctrl.hasContent = hasContent;
  }

  /**
   * Moves the indexes to their nearest valid values.
   */
  function refreshIndex () {
    ctrl.selectedIndex = getNearestSafeIndex(ctrl.selectedIndex);
    ctrl.focusIndex    = getNearestSafeIndex(ctrl.focusIndex);
  }

  /**
   * Calculates the content height of the current tab.
   * @returns {*}
   */
  function updateHeightFromContent () {
    if (!ctrl.dynamicHeight) return $element.css('height', '');
    if (!ctrl.tabs.length) return queue.push(updateHeightFromContent);

    var elements = getElements();

    var tabContent    = elements.contents[ ctrl.selectedIndex ],
        contentHeight = tabContent ? tabContent.offsetHeight : 0,
        tabsHeight    = elements.wrapper.offsetHeight,
        newHeight     = contentHeight + tabsHeight,
        currentHeight = $element.prop('clientHeight');

    if (currentHeight === newHeight) return;

    // Adjusts calculations for when the buttons are bottom-aligned since this relies on absolute
    // positioning.  This should probably be cleaned up if a cleaner solution is possible.
    if ($element.attr('md-align-tabs') === 'bottom') {
      currentHeight -= tabsHeight;
      newHeight -= tabsHeight;
      // Need to include bottom border in these calculations
      if ($element.attr('md-border-bottom') !== undefined) {
        ++currentHeight;
      }
    }

    // Lock during animation so the user can't change tabs
    locked = true;

    var fromHeight = { height: currentHeight + 'px' },
        toHeight = { height: newHeight + 'px' };

    // Set the height to the current, specific pixel height to fix a bug on iOS where the height
    // first animates to 0, then back to the proper height causing a visual glitch
    $element.css(fromHeight);

    // Animate the height from the old to the new
    $animateCss($element, {
      from: fromHeight,
      to: toHeight,
      easing: 'cubic-bezier(0.35, 0, 0.25, 1)',
      duration: 0.5
    }).start().done(function () {
      // Then (to fix the same iOS issue as above), disable transitions and remove the specific
      // pixel height so the height can size with browser width/content changes, etc.
      $element.css({
        transition: 'none',
        height: ''
      });

      // In the next tick, re-allow transitions (if we do it all at once, $element.css is "smart"
      // enough to batch it for us instead of doing it immediately, which undoes the original
      // transition: none)
      $mdUtil.nextTick(function() {
        $element.css('transition', '');
      });

      // And unlock so tab changes can occur
      locked = false;
    });
  }

  /**
   * Repositions the ink bar to the selected tab.
   * Parameters are used when calling itself recursively when md-center-tabs is used as we need to
   * run two passes to properly center the tabs. These parameters ensure that we only run two passes
   * and that we don't run indefinitely.
   * @param {number=} previousTotalWidth previous width of pagination wrapper
   * @param {number=} previousWidthOfTabItems previous width of all tab items
   */
  function updateInkBarStyles (previousTotalWidth, previousWidthOfTabItems) {
    if (ctrl.noInkBar) {
      return;
    }
    var elements = getElements();

    if (!elements.tabs[ ctrl.selectedIndex ]) {
      angular.element(elements.inkBar).css({ left: 'auto', right: 'auto' });
      return;
    }

    if (!ctrl.tabs.length) {
      queue.push(ctrl.updateInkBarStyles);
      return;
    }
    // If the element is not visible, we will not be able to calculate sizes until it becomes
    // visible. We should treat that as a resize event rather than just updating the ink bar.
    if (!$element.prop('offsetParent')) {
      handleResizeWhenVisible();
      return;
    }

    var index      = ctrl.selectedIndex,
        totalWidth = elements.paging.offsetWidth,
        tab        = elements.tabs[ index ],
        left       = tab.offsetLeft,
        right      = totalWidth - left - tab.offsetWidth;

    if (ctrl.shouldCenterTabs) {
      // We need to use the same calculate process as in the pagination wrapper, to avoid rounding
      // deviations.
      var totalWidthOfTabItems = calcTabsWidth(elements.tabs);

      if (totalWidth > totalWidthOfTabItems &&
          previousTotalWidth !== totalWidth &&
          previousWidthOfTabItems !== totalWidthOfTabItems) {
        $timeout(updateInkBarStyles, 0, true, totalWidth, totalWidthOfTabItems);
      }
    }
    updateInkBarClassName();
    angular.element(elements.inkBar).css({ left: left + 'px', right: right + 'px' });
  }

  /**
   * Adds left/right classes so that the ink bar will animate properly.
   */
  function updateInkBarClassName () {
    var elements = getElements();
    var newIndex = ctrl.selectedIndex,
        oldIndex = ctrl.lastSelectedIndex,
        ink      = angular.element(elements.inkBar);
    if (!angular.isNumber(oldIndex)) return;
    ink
        .toggleClass('md-left', newIndex < oldIndex)
        .toggleClass('md-right', newIndex > oldIndex);
  }

  /**
   * Takes an offset value and makes sure that it is within the min/max allowed values.
   * @param {number} value
   * @returns {number}
   */
  function fixOffset (value) {
    var elements = getElements();

    if (!elements.tabs.length || !ctrl.shouldPaginate) return 0;

    var lastTab    = elements.tabs[ elements.tabs.length - 1 ],
        totalWidth = lastTab.offsetLeft + lastTab.offsetWidth;

    if (isRtl()) {
      value = Math.min(elements.paging.offsetWidth - elements.canvas.clientWidth, value);
      value = Math.max(0, value);
    } else {
      value = Math.max(0, value);
      value = Math.min(totalWidth - elements.canvas.clientWidth, value);
    }

    return value;
  }

  /**
   * Attaches a ripple to the tab item element.
   * @param scope
   * @param element
   */
  function attachRipple (scope, element) {
    var elements = getElements();
    var options = { colorElement: angular.element(elements.inkBar) };
    $mdTabInkRipple.attach(scope, element, options);
  }

  /**
   * Sets the `aria-controls` attribute to the elements that correspond to the passed-in tab.
   * @param tab
   */
  function setAriaControls (tab) {
    if (tab.hasContent) {
      var nodes = $element[0].querySelectorAll('[md-tab-id="' + tab.id + '"]');
      angular.element(nodes).attr('aria-controls', ctrl.tabContentPrefix + tab.id);
    }
  }

  function isRtl() {
    return ($mdUtil.bidi() === 'rtl');
  }
}

/**
 * @ngdoc directive
 * @name mdTabs
 * @module material.components.tabs
 *
 * @restrict E
 *
 * @description
 * The `<md-tabs>` directive serves as the container for 1..n
 * <a ng-href="api/directive/mdTab">`<md-tab>`</a> child directives.
 * In turn, the nested `<md-tab>` directive is used to specify a tab label for the
 * **header button** and <i>optional</i> tab view content that will be associated with each tab
 * button.
 *
 * Below is the markup for its simplest usage:
 *
 *  <hljs lang="html">
 *  <md-tabs>
 *    <md-tab label="Tab #1"></md-tab>
 *    <md-tab label="Tab #2"></md-tab>
 *    <md-tab label="Tab #3"></md-tab>
 *  </md-tabs>
 *  </hljs>
 *
 * Tabs support three (3) usage scenarios:
 *
 *  1. Tabs (buttons only)
 *  2. Tabs with internal view content
 *  3. Tabs with external view content
 *
 * **Tabs-only** support is useful when tab buttons are used for custom navigation regardless of any
 * other components, content, or views.
 *
 * <i><b>Note:</b> If you are using the Tabs component for page-level navigation, please use
 * the <a ng-href="./api/directive/mdNavBar">NavBar component</a> instead. It handles this
 * case a more natively and more performantly.</i>
 *
 * **Tabs with internal views** are the traditional usage where each tab has associated view
 * content and the view switching is managed internally by the Tabs component.
 *
 * **Tabs with external view content** is often useful when content associated with each tab is
 * independently managed and data-binding notifications announce tab selection changes.
 *
 * Additional features also include:
 *
 * *  Content can include any markup.
 * *  If a tab is disabled while active/selected, then the next tab will be auto-selected.
 *
 * ### Explanation of tab stretching
 *
 * Initially, tabs will have an inherent size.  This size will either be defined by how much space
 * is needed to accommodate their text or set by the user through CSS.
 * Calculations will be based on this size.
 *
 * On mobile devices, tabs will be expanded to fill the available horizontal space.
 * When this happens, all tabs will become the same size.
 *
 * On desktops, by default, stretching will never occur.
 *
 * This default behavior can be overridden through the `md-stretch-tabs` attribute.
 * Here is a table showing when stretching will occur:
 *
 * `md-stretch-tabs` | mobile    | desktop
 * ------------------|-----------|--------
 * `auto`            | stretched | ---
 * `always`          | stretched | stretched
 * `never`           | ---       | ---
 *
 * @param {integer=} md-selected Index of the active/selected tab.
 * @param {boolean=} md-no-ink-bar If present, disables the selection ink bar.
 * @param {string=}  md-align-tabs Attribute to indicate position of tab buttons: `bottom` or `top`;
 *  Default is `top`.
 * @param {string=} md-stretch-tabs Attribute to indicate whether or not to stretch tabs: `auto`,
 *  `always`, or `never`; Default is `auto`.
 * @param {boolean=} md-dynamic-height When enabled, the tab wrapper will resize based on the
 *  contents of the selected tab.
 * @param {boolean=} md-border-bottom If present, shows a solid `1px` border between the tabs and
 *  their content.
 * @param {boolean=} md-center-tabs If defined, tabs will be centered provided there is no need
 *  for pagination.
 * @param {boolean=} md-no-pagination When enabled, pagination will remain off.
 * @param {boolean=} md-swipe-content When enabled, swipe gestures will be enabled for the content
 *  area to allow swiping between tabs.
 * @param {boolean=} md-enable-disconnect When enabled, scopes will be disconnected for tabs that
 *  are not being displayed. This provides a performance boost, but may also cause unexpected
 *  issues. It is not recommended for most users.
 * @param {boolean=} md-autoselect When present, any tabs added after the initial load will be
 *  automatically selected.
 * @param {boolean=} md-no-select-click When true, click events will not be fired when the value of
 *  `md-active` on an `md-tab` changes. This is useful when using tabs with UI-Router's child
 *  states, as triggering a click event in that case can cause an extra tab change to occur.
 * @param {string=} md-navigation-hint Attribute to override the default `tablist` navigation hint
 *  that screen readers will announce to provide instructions for navigating between tabs. This is
 *  desirable when you want the hint to be in a different language. Default is "Use the left and
 *  right arrow keys to navigate between tabs".
 *
 * @usage
 * <hljs lang="html">
 * <md-tabs md-selected="selectedIndex" >
 *   <img ng-src="img/angular.png" class="centered">
 *   <md-tab
 *       ng-repeat="tab in tabs | orderBy:predicate:reversed"
 *       md-on-select="onTabSelected(tab)"
 *       md-on-deselect="announceDeselected(tab)"
 *       ng-disabled="tab.disabled">
 *     <md-tab-label>
 *       {{tab.title}}
 *       <img src="img/removeTab.png" ng-click="removeTab(tab)" class="delete">
 *     </md-tab-label>
 *     <md-tab-body>
 *       {{tab.content}}
 *     </md-tab-body>
 *   </md-tab>
 * </md-tabs>
 * </hljs>
 *
 */
MdTabs['$inject'] = ["$$mdSvgRegistry"];
angular
    .module('material.components.tabs')
    .directive('mdTabs', MdTabs);

function MdTabs ($$mdSvgRegistry) {
  return {
    scope:            {
      navigationHint: '@?mdNavigationHint',
      selectedIndex: '=?mdSelected'
    },
    template:         function (element, attr) {
      attr.$mdTabsTemplate = element.html();
      return '' +
        '<md-tabs-wrapper> ' +
          '<md-tab-data></md-tab-data> ' +
          '<md-prev-button ' +
              'tabindex="-1" ' +
              'role="button" ' +
              'aria-label="Previous Page" ' +
              'aria-disabled="{{!$mdTabsCtrl.canPageBack()}}" ' +
              'ng-class="{ \'md-disabled\': !$mdTabsCtrl.canPageBack() }" ' +
              'ng-if="$mdTabsCtrl.shouldPaginate" ' +
              'ng-click="$mdTabsCtrl.previousPage()"> ' +
            '<md-icon md-svg-src="'+ $$mdSvgRegistry.mdTabsArrow +'"></md-icon> ' +
          '</md-prev-button> ' +
          '<md-next-button ' +
              'tabindex="-1" ' +
              'role="button" ' +
              'aria-label="Next Page" ' +
              'aria-disabled="{{!$mdTabsCtrl.canPageForward()}}" ' +
              'ng-class="{ \'md-disabled\': !$mdTabsCtrl.canPageForward() }" ' +
              'ng-if="$mdTabsCtrl.shouldPaginate" ' +
              'ng-click="$mdTabsCtrl.nextPage()"> ' +
            '<md-icon md-svg-src="'+ $$mdSvgRegistry.mdTabsArrow +'"></md-icon> ' +
          '</md-next-button> ' +
          '<md-tabs-canvas ' +
              'tabindex="{{ $mdTabsCtrl.hasFocus ? -1 : 0 }}" ' +
              'ng-focus="$mdTabsCtrl.redirectFocus()" ' +
              'ng-class="{ ' +
                  '\'md-paginated\': $mdTabsCtrl.shouldPaginate, ' +
                  '\'md-center-tabs\': $mdTabsCtrl.shouldCenterTabs ' +
              '}" ' +
              'ng-keydown="$mdTabsCtrl.keydown($event)"> ' +
            '<md-pagination-wrapper ' +
                'ng-class="{ \'md-center-tabs\': $mdTabsCtrl.shouldCenterTabs }" ' +
                'md-tab-scroll="$mdTabsCtrl.scroll($event)" ' +
                'role="tablist" ' +
                'aria-label="{{::$mdTabsCtrl.navigationHint}}">' +
              '<md-tab-item ' +
                  'tabindex="{{ tab.isActive() ? 0 : -1 }}" ' +
                  'class="md-tab {{::tab.scope.tabClass}}" ' +
                  'ng-repeat="tab in $mdTabsCtrl.tabs" ' +
                  'role="tab" ' +
                  'id="tab-item-{{::tab.id}}" ' +
                  'md-tab-id="{{::tab.id}}" ' +
                  'aria-selected="{{tab.isActive()}}" ' +
                  'aria-disabled="{{tab.scope.disabled || \'false\'}}" ' +
                  'ng-click="$mdTabsCtrl.select(tab.getIndex())" ' +
                  'ng-focus="$mdTabsCtrl.hasFocus = true" ' +
                  'ng-blur="$mdTabsCtrl.hasFocus = false" ' +
                  'ng-class="{ ' +
                      '\'md-active\':    tab.isActive(), ' +
                      '\'md-focused\':   tab.hasFocus(), ' +
                      '\'md-disabled\':  tab.scope.disabled ' +
                  '}" ' +
                  'ng-disabled="tab.scope.disabled" ' +
                  'md-swipe-left="$mdTabsCtrl.nextPage()" ' +
                  'md-swipe-right="$mdTabsCtrl.previousPage()" ' +
                  'md-tabs-template="::tab.label" ' +
                  'md-scope="::tab.parent"></md-tab-item> ' +
              '<md-ink-bar></md-ink-bar> ' +
            '</md-pagination-wrapper> ' +
            '<md-tabs-dummy-wrapper aria-hidden="true" class="md-visually-hidden md-dummy-wrapper"> ' +
              '<md-dummy-tab ' +
                  'class="md-tab" ' +
                  'tabindex="-1" ' +
                  'ng-focus="$mdTabsCtrl.hasFocus = true" ' +
                  'ng-blur="$mdTabsCtrl.hasFocus = false" ' +
                  'ng-repeat="tab in $mdTabsCtrl.tabs" ' +
                  'md-tabs-template="::tab.label" ' +
                  'md-scope="::tab.parent"></md-dummy-tab> ' +
            '</md-tabs-dummy-wrapper> ' +
          '</md-tabs-canvas> ' +
        '</md-tabs-wrapper> ' +
        '<md-tabs-content-wrapper ng-show="$mdTabsCtrl.hasContent && $mdTabsCtrl.selectedIndex >= 0" class="_md"> ' +
          '<md-tab-content ' +
              'id="{{:: $mdTabsCtrl.tabContentPrefix + tab.id}}" ' +
              'class="_md" ' +
              'role="tabpanel" ' +
              'aria-labelledby="tab-item-{{::tab.id}}" ' +
              'md-swipe-left="$mdTabsCtrl.swipeContent && $mdTabsCtrl.incrementIndex(1)" ' +
              'md-swipe-right="$mdTabsCtrl.swipeContent && $mdTabsCtrl.incrementIndex(-1)" ' +
              'ng-if="tab.hasContent" ' +
              'ng-repeat="(index, tab) in $mdTabsCtrl.tabs" ' +
              'ng-class="{ ' +
                '\'md-no-transition\': $mdTabsCtrl.lastSelectedIndex == null, ' +
                '\'md-active\':        tab.isActive(), ' +
                '\'md-left\':          tab.isLeft(), ' +
                '\'md-right\':         tab.isRight(), ' +
                '\'md-no-scroll\':     $mdTabsCtrl.dynamicHeight ' +
              '}"> ' +
            '<div ' +
                'md-tabs-template="::tab.template" ' +
                'md-connected-if="tab.isActive()" ' +
                'md-scope="::tab.parent" ' +
                'ng-if="$mdTabsCtrl.enableDisconnect || tab.shouldRender()"></div> ' +
          '</md-tab-content> ' +
        '</md-tabs-content-wrapper>';
    },
    controller:       'MdTabsController',
    controllerAs:     '$mdTabsCtrl',
    bindToController: true
  };
}


MdTabsDummyWrapper['$inject'] = ["$mdUtil", "$window"];angular
  .module('material.components.tabs')
  .directive('mdTabsDummyWrapper', MdTabsDummyWrapper);

/**
 * @private
 *
 * @param $mdUtil
 * @param $window
 * @returns {{require: string, link: link}}
 * @constructor
 *
 * ngInject
 */
function MdTabsDummyWrapper ($mdUtil, $window) {
  return {
    require: '^?mdTabs',
    link:    function link (scope, element, attr, ctrl) {
      if (!ctrl) return;

      var observer;
      var disconnect;

      var mutationCallback = function() {
        ctrl.updatePagination();
        ctrl.updateInkBarStyles();
      };

      if ('MutationObserver' in $window) {
        var config = {
          childList: true,
          subtree: true,
          // Per https://bugzilla.mozilla.org/show_bug.cgi?id=1138368, browsers will not fire
          // the childList mutation, once a <span> element's innerText changes.
          // The characterData of the <span> element will change.
          characterData: true
        };

        observer = new MutationObserver(mutationCallback);
        observer.observe(element[0], config);
        disconnect = observer.disconnect.bind(observer);
      } else {
        var debounced = $mdUtil.debounce(mutationCallback, 15, null, false);

        element.on('DOMSubtreeModified', debounced);
        disconnect = element.off.bind(element, 'DOMSubtreeModified', debounced);
      }

      // Disconnect the observer
      scope.$on('$destroy', function() {
        disconnect();
      });
    }
  };
}


MdTabsTemplate['$inject'] = ["$compile", "$mdUtil"];angular
    .module('material.components.tabs')
    .directive('mdTabsTemplate', MdTabsTemplate);

function MdTabsTemplate ($compile, $mdUtil) {
  return {
    restrict: 'A',
    link:     link,
    scope:    {
      template:     '=mdTabsTemplate',
      connected:    '=?mdConnectedIf',
      compileScope: '=mdScope'
    },
    require:  '^?mdTabs'
  };
  function link (scope, element, attr, ctrl) {
    if (!ctrl) return;

    var compileScope = ctrl.enableDisconnect ? scope.compileScope.$new() : scope.compileScope;

    element.html(scope.template);
    $compile(element.contents())(compileScope);

    return $mdUtil.nextTick(handleScope);

    function handleScope () {
      scope.$watch('connected', function (value) { value === false ? disconnect() : reconnect(); });
      scope.$on('$destroy', reconnect);
    }

    function disconnect () {
      if (ctrl.enableDisconnect) $mdUtil.disconnectScope(compileScope);
    }

    function reconnect () {
      if (ctrl.enableDisconnect) $mdUtil.reconnectScope(compileScope);
    }
  }
}

ngmaterial.components.tabs = angular.module("material.components.tabs");