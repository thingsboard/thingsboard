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
 * @name material.components.autocomplete
 */
/*
 * @see js folder for autocomplete implementation
 */
angular.module('material.components.autocomplete', [
  'material.core',
  'material.components.icon',
  'material.components.virtualRepeat'
]);


MdAutocompleteCtrl['$inject'] = ["$scope", "$element", "$mdUtil", "$mdConstant", "$mdTheming", "$window", "$animate", "$rootElement", "$attrs", "$q", "$log", "$mdLiveAnnouncer"];angular
    .module('material.components.autocomplete')
    .controller('MdAutocompleteCtrl', MdAutocompleteCtrl);

var ITEM_HEIGHT   = 48,
    MAX_ITEMS     = 5,
    MENU_PADDING  = 8,
    INPUT_PADDING = 2, // Padding provided by `md-input-container`
    MODE_STANDARD = 'standard',
    MODE_VIRTUAL = 'virtual';

function MdAutocompleteCtrl ($scope, $element, $mdUtil, $mdConstant, $mdTheming, $window,
                             $animate, $rootElement, $attrs, $q, $log, $mdLiveAnnouncer) {

  // Internal Variables.
  var ctrl                 = this,
      itemParts            = $scope.itemsExpr.split(/ in /i),
      itemExpr             = itemParts[ 1 ],
      elements             = null,
      cache                = {},
      noBlur               = false,
      selectedItemWatchers = [],
      hasFocus             = false,
      fetchesInProgress    = 0,
      enableWrapScroll     = null,
      inputModelCtrl       = null,
      debouncedOnResize    = $mdUtil.debounce(onWindowResize),
      mode                 = MODE_VIRTUAL; // default

  // Public Exported Variables with handlers
  defineProperty('hidden', handleHiddenChange, true);

  // Public Exported Variables
  ctrl.scope      = $scope;
  ctrl.parent     = $scope.$parent;
  ctrl.itemName   = itemParts[ 0 ];
  ctrl.matches    = [];
  ctrl.loading    = false;
  ctrl.hidden     = true;
  ctrl.index      = null;
  ctrl.id         = $mdUtil.nextUid();
  ctrl.isDisabled = null;
  ctrl.isRequired = null;
  ctrl.isReadonly = null;
  ctrl.hasNotFound = false;
  ctrl.selectedMessage = $scope.selectedMessage || 'selected';

  // Public Exported Methods
  ctrl.keydown                       = keydown;
  ctrl.blur                          = blur;
  ctrl.focus                         = focus;
  ctrl.clear                         = clearValue;
  ctrl.select                        = select;
  ctrl.listEnter                     = onListEnter;
  ctrl.listLeave                     = onListLeave;
  ctrl.mouseUp                       = onMouseup;
  ctrl.getCurrentDisplayValue        = getCurrentDisplayValue;
  ctrl.registerSelectedItemWatcher   = registerSelectedItemWatcher;
  ctrl.unregisterSelectedItemWatcher = unregisterSelectedItemWatcher;
  ctrl.notFoundVisible               = notFoundVisible;
  ctrl.loadingIsVisible              = loadingIsVisible;
  ctrl.positionDropdown              = positionDropdown;

  /**
   * Report types to be used for the $mdLiveAnnouncer
   * @enum {number} Unique flag id.
   */
  var ReportType = {
    Count: 1,
    Selected: 2
  };

  return init();

  // initialization methods

  /**
   * Initialize the controller, setup watchers, gather elements
   */
  function init () {

    $mdUtil.initOptionalProperties($scope, $attrs, {
      searchText: '',
      selectedItem: null,
      clearButton: false,
      disableVirtualRepeat: false,
    });

    $mdTheming($element);
    configureWatchers();
    $mdUtil.nextTick(function () {

      gatherElements();
      moveDropdown();

      // Forward all focus events to the input element when autofocus is enabled
      if ($scope.autofocus) {
        $element.on('focus', focusInputElement);
      }
      if ($scope.inputAriaDescribedBy) {
        elements.input.setAttribute('aria-describedby', $scope.inputAriaDescribedBy);
      }
      if (!$scope.floatingLabel) {
        if ($scope.inputAriaLabel) {
          elements.input.setAttribute('aria-label', $scope.inputAriaLabel);
        } else if ($scope.inputAriaLabelledBy) {
          elements.input.setAttribute('aria-labelledby', $scope.inputAriaLabelledBy);
        } else if ($scope.placeholder) {
          // If no aria-label or aria-labelledby references are defined, then just label using the
          // placeholder.
          elements.input.setAttribute('aria-label', $scope.placeholder);
        }
      }
    });
  }

  function updateModelValidators() {
    if (!$scope.requireMatch || !inputModelCtrl) return;

    inputModelCtrl.$setValidity('md-require-match', !!$scope.selectedItem || !$scope.searchText);
  }

  /**
   * Calculates the dropdown's position and applies the new styles to the menu element
   * @returns {*}
   */
  function positionDropdown () {
    if (!elements) {
      return $mdUtil.nextTick(positionDropdown, false, $scope);
    }

    var dropdownHeight = ($scope.dropdownItems || MAX_ITEMS) * ITEM_HEIGHT;
    var hrect  = elements.wrap.getBoundingClientRect(),
        vrect  = elements.snap.getBoundingClientRect(),
        root   = elements.root.getBoundingClientRect(),
        top    = vrect.bottom - root.top,
        bot    = root.bottom - vrect.top,
        left   = hrect.left - root.left,
        width  = hrect.width,
        offset = getVerticalOffset(),
        position = $scope.dropdownPosition,
        styles, enoughBottomSpace, enoughTopSpace;
    var bottomSpace = root.bottom - vrect.bottom - MENU_PADDING + $mdUtil.getViewportTop();
    var topSpace = vrect.top - MENU_PADDING;

    // Automatically determine dropdown placement based on available space in viewport.
    if (!position) {
      enoughTopSpace = topSpace > dropdownHeight;
      enoughBottomSpace = bottomSpace > dropdownHeight;
      if (enoughBottomSpace) {
        position = 'bottom';
      } else if (enoughTopSpace) {
        position = 'top';
      } else {
        position = topSpace > bottomSpace ? 'top' : 'bottom';
      }
    }
    // Adjust the width to account for the padding provided by `md-input-container`
    if ($attrs.mdFloatingLabel) {
      left += INPUT_PADDING;
      width -= INPUT_PADDING * 2;
    }
    styles = {
      left:     left + 'px',
      minWidth: width + 'px',
      maxWidth: Math.max(hrect.right - root.left, root.right - hrect.left) - MENU_PADDING + 'px'
    };

    if (position === 'top') {
      styles.top       = 'auto';
      styles.bottom    = bot + 'px';
      styles.maxHeight = Math.min(dropdownHeight, topSpace) + 'px';
    } else {
      bottomSpace = root.bottom - hrect.bottom - MENU_PADDING + $mdUtil.getViewportTop();

      styles.top       = (top - offset) + 'px';
      styles.bottom    = 'auto';
      styles.maxHeight = Math.min(dropdownHeight, bottomSpace) + 'px';
    }

    elements.$.scrollContainer.css(styles);
    $mdUtil.nextTick(correctHorizontalAlignment, false, $scope);

    /**
     * Calculates the vertical offset for floating label examples to account for ngMessages
     * @returns {number}
     */
    function getVerticalOffset () {
      var offset = 0;
      var inputContainer = $element.find('md-input-container');
      if (inputContainer.length) {
        var input = inputContainer.find('input');
        offset = inputContainer.prop('offsetHeight');
        offset -= input.prop('offsetTop');
        offset -= input.prop('offsetHeight');
        // add in the height left up top for the floating label text
        offset += inputContainer.prop('offsetTop');
      }
      return offset;
    }

    /**
     * Makes sure that the menu doesn't go off of the screen on either side.
     */
    function correctHorizontalAlignment () {
      var dropdown = elements.scrollContainer.getBoundingClientRect(),
          styles   = {};
      if (dropdown.right > root.right) {
        styles.left = (hrect.right - dropdown.width) + 'px';
      }
      elements.$.scrollContainer.css(styles);
    }
  }

  /**
   * Moves the dropdown menu to the body tag in order to avoid z-index and overflow issues.
   */
  function moveDropdown () {
    if (!elements.$.root.length) return;
    $mdTheming(elements.$.scrollContainer);
    elements.$.scrollContainer.detach();
    elements.$.root.append(elements.$.scrollContainer);
    if ($animate.pin) $animate.pin(elements.$.scrollContainer, $rootElement);
  }

  /**
   * Sends focus to the input element.
   */
  function focusInputElement () {
    elements.input.focus();
  }

  /**
   * Sets up any watchers used by autocomplete
   */
  function configureWatchers () {
    var wait = parseInt($scope.delay, 10) || 0;

    $attrs.$observe('disabled', function (value) { ctrl.isDisabled = $mdUtil.parseAttributeBoolean(value, false); });
    $attrs.$observe('required', function (value) { ctrl.isRequired = $mdUtil.parseAttributeBoolean(value, false); });
    $attrs.$observe('readonly', function (value) { ctrl.isReadonly = $mdUtil.parseAttributeBoolean(value, false); });

    $scope.$watch('searchText', wait ? $mdUtil.debounce(handleSearchText, wait) : handleSearchText);
    $scope.$watch('selectedItem', selectedItemChange);

    angular.element($window).on('resize', debouncedOnResize);

    $scope.$on('$destroy', cleanup);
  }

  /**
   * Removes any events or leftover elements created by this controller
   */
  function cleanup () {
    if (!ctrl.hidden) {
      $mdUtil.enableScrolling();
    }

    angular.element($window).off('resize', debouncedOnResize);

    if (elements){
      var items = ['ul', 'scroller', 'scrollContainer', 'input'];
      angular.forEach(items, function(key){
        elements.$[key].remove();
      });
    }
  }

  /**
   * Event handler to be called whenever the window resizes.
   */
  function onWindowResize() {
    if (!ctrl.hidden) {
      positionDropdown();
    }
  }

  /**
   * Gathers all of the elements needed for this controller
   */
  function gatherElements () {

    var snapWrap = gatherSnapWrap();

    elements = {
      main:  $element[0],
      scrollContainer: $element[0].querySelector('.md-virtual-repeat-container, .md-standard-list-container'),
      scroller: $element[0].querySelector('.md-virtual-repeat-scroller, .md-standard-list-scroller'),
      ul:    $element.find('ul')[0],
      input: $element.find('input')[0],
      wrap:  snapWrap.wrap,
      snap:  snapWrap.snap,
      root:  document.body,
    };

    elements.li   = elements.ul.getElementsByTagName('li');
    elements.$    = getAngularElements(elements);
    mode = elements.scrollContainer.classList.contains('md-standard-list-container') ? MODE_STANDARD : MODE_VIRTUAL;
    inputModelCtrl = elements.$.input.controller('ngModel');
  }

  /**
   * Gathers the snap and wrap elements
   *
   */
  function gatherSnapWrap() {
    var element;
    var value;
    for (element = $element; element.length; element = element.parent()) {
      value = element.attr('md-autocomplete-snap');
      if (angular.isDefined(value)) break;
    }

    if (element.length) {
      return {
        snap: element[0],
        wrap: (value.toLowerCase() === 'width') ? element[0] : $element.find('md-autocomplete-wrap')[0]
      };
    }

    var wrap = $element.find('md-autocomplete-wrap')[0];
    return {
      snap: wrap,
      wrap: wrap
    };
  }

  /**
   * Gathers angular-wrapped versions of each element
   * @param elements
   * @returns {{}}
   */
  function getAngularElements (elements) {
    var obj = {};
    for (var key in elements) {
      if (elements.hasOwnProperty(key)) obj[ key ] = angular.element(elements[ key ]);
    }
    return obj;
  }

  // event/change handlers

  /**
   * Handles changes to the `hidden` property.
   * @param hidden
   * @param oldHidden
   */
  function handleHiddenChange (hidden, oldHidden) {
    if (!hidden && oldHidden) {
      positionDropdown();

      // Report in polite mode, because the screen reader should finish the default description of
      // the input element.
      reportMessages(true, ReportType.Count | ReportType.Selected);

      if (elements) {
        $mdUtil.disableScrollAround(elements.ul);
        enableWrapScroll = disableElementScrollEvents(angular.element(elements.wrap));
      }
    } else if (hidden && !oldHidden) {
      $mdUtil.enableScrolling();

      if (enableWrapScroll) {
        enableWrapScroll();
        enableWrapScroll = null;
      }
    }
  }

  /**
   * Disables scrolling for a specific element
   */
  function disableElementScrollEvents(element) {

    function preventDefault(e) {
      e.preventDefault();
    }

    element.on('wheel', preventDefault);
    element.on('touchmove', preventDefault);

    return function() {
      element.off('wheel', preventDefault);
      element.off('touchmove', preventDefault);
    };
  }

  /**
   * When the user mouses over the dropdown menu, ignore blur events.
   */
  function onListEnter () {
    noBlur = true;
  }

  /**
   * When the user's mouse leaves the menu, blur events may hide the menu again.
   */
  function onListLeave () {
    if (!hasFocus && !ctrl.hidden) elements.input.focus();
    noBlur = false;
    ctrl.hidden = shouldHide();
  }

  /**
   * When the mouse button is released, send focus back to the input field.
   */
  function onMouseup () {
    elements.input.focus();
  }

  /**
   * Handles changes to the selected item.
   * @param selectedItem
   * @param previousSelectedItem
   */
  function selectedItemChange (selectedItem, previousSelectedItem) {

    updateModelValidators();

    if (selectedItem) {
      getDisplayValue(selectedItem).then(function (val) {
        $scope.searchText = val;
        handleSelectedItemChange(selectedItem, previousSelectedItem);
      });
    } else if (previousSelectedItem && $scope.searchText) {
      getDisplayValue(previousSelectedItem).then(function(displayValue) {
        // Clear the searchText, when the selectedItem is set to null.
        // Do not clear the searchText, when the searchText isn't matching with the previous
        // selected item.
        if (angular.isString($scope.searchText)
          && displayValue.toString().toLowerCase() === $scope.searchText.toLowerCase()) {
          $scope.searchText = '';
        }
      });
    }

    if (selectedItem !== previousSelectedItem) {
      announceItemChange();
    }
  }

  /**
   * Use the user-defined expression to announce changes each time a new item is selected
   */
  function announceItemChange () {
    angular.isFunction($scope.itemChange) &&
      $scope.itemChange(getItemAsNameVal($scope.selectedItem));
  }

  /**
   * Use the user-defined expression to announce changes each time the search text is changed
   */
  function announceTextChange () {
    angular.isFunction($scope.textChange) && $scope.textChange();
  }

  /**
   * Calls any external watchers listening for the selected item.  Used in conjunction with
   * `registerSelectedItemWatcher`.
   * @param selectedItem
   * @param previousSelectedItem
   */
  function handleSelectedItemChange (selectedItem, previousSelectedItem) {
    selectedItemWatchers.forEach(function (watcher) {
      watcher(selectedItem, previousSelectedItem);
    });
  }

  /**
   * Register a function to be called when the selected item changes.
   * @param cb
   */
  function registerSelectedItemWatcher (cb) {
    if (selectedItemWatchers.indexOf(cb) === -1) {
      selectedItemWatchers.push(cb);
    }
  }

  /**
   * Unregister a function previously registered for selected item changes.
   * @param cb
   */
  function unregisterSelectedItemWatcher (cb) {
    var i = selectedItemWatchers.indexOf(cb);
    if (i !== -1) {
      selectedItemWatchers.splice(i, 1);
    }
  }

  /**
   * Handles changes to the searchText property.
   * @param searchText
   * @param previousSearchText
   */
  function handleSearchText (searchText, previousSearchText) {
    ctrl.index = getDefaultIndex();

    // do nothing on init
    if (searchText === previousSearchText) return;

    updateModelValidators();

    getDisplayValue($scope.selectedItem).then(function (val) {
      // clear selected item if search text no longer matches it
      if (searchText !== val) {
        $scope.selectedItem = null;

        // trigger change event if available
        if (searchText !== previousSearchText) {
          announceTextChange();
        }

        // cancel results if search text is not long enough
        if (!isMinLengthMet()) {
          ctrl.matches = [];

          setLoading(false);
          reportMessages(true, ReportType.Count);

        } else {
          handleQuery();
        }
      }
    });

  }

  /**
   * Handles input blur event, determines if the dropdown should hide.
   */
  function blur($event) {
    hasFocus = false;

    if (!noBlur) {
      ctrl.hidden = shouldHide();
      evalAttr('ngBlur', { $event: $event });
    }
  }

  /**
   * Force blur on input element
   * @param forceBlur
   */
  function doBlur(forceBlur) {
    if (forceBlur) {
      noBlur = false;
      hasFocus = false;
    }
    elements.input.blur();
  }

  /**
   * Handles input focus event, determines if the dropdown should show.
   */
  function focus($event) {
    hasFocus = true;

    if (isSearchable() && isMinLengthMet()) {
      handleQuery();
    }

    ctrl.hidden = shouldHide();

    evalAttr('ngFocus', { $event: $event });
  }

  /**
   * Handles keyboard input.
   * @param event
   */
  function keydown (event) {
    switch (event.keyCode) {
      case $mdConstant.KEY_CODE.DOWN_ARROW:
        if (ctrl.loading || hasSelection()) return;
        event.stopPropagation();
        event.preventDefault();
        ctrl.index   = Math.min(ctrl.index + 1, ctrl.matches.length - 1);
        updateScroll();
        reportMessages(false, ReportType.Selected);
        break;
      case $mdConstant.KEY_CODE.UP_ARROW:
        if (ctrl.loading || hasSelection()) return;
        event.stopPropagation();
        event.preventDefault();
        ctrl.index   = ctrl.index < 0 ? ctrl.matches.length - 1 : Math.max(0, ctrl.index - 1);
        updateScroll();
        reportMessages(false, ReportType.Selected);
        break;
      case $mdConstant.KEY_CODE.TAB:
        // If we hit tab, assume that we've left the list so it will close
        onListLeave();

        if (ctrl.hidden || ctrl.loading || ctrl.index < 0 || ctrl.matches.length < 1) return;
        select(ctrl.index);
        break;
      case $mdConstant.KEY_CODE.ENTER:
        if (ctrl.hidden || ctrl.loading || ctrl.index < 0 || ctrl.matches.length < 1) return;
        if (hasSelection()) return;
        event.stopImmediatePropagation();
        event.preventDefault();
        select(ctrl.index);
        break;
      case $mdConstant.KEY_CODE.ESCAPE:
        event.preventDefault(); // Prevent browser from always clearing input
        if (!shouldProcessEscape()) return;
        event.stopPropagation();

        clearSelectedItem();
        if ($scope.searchText && hasEscapeOption('clear')) {
          clearSearchText();
        }

        // Manually hide (needed for mdNotFound support)
        ctrl.hidden = true;

        if (hasEscapeOption('blur')) {
          // Force the component to blur if they hit escape
          doBlur(true);
        }

        break;
      default:
    }
  }

  // getters

  /**
   * Returns the minimum length needed to display the dropdown.
   * @returns {*}
   */
  function getMinLength () {
    return angular.isNumber($scope.minLength) ? $scope.minLength : 1;
  }

  /**
   * Returns the display value for an item.
   * @param item
   * @returns {*}
   */
  function getDisplayValue (item) {
    return $q.when(getItemText(item) || item).then(function(itemText) {
      if (itemText && !angular.isString(itemText)) {
        $log.warn('md-autocomplete: Could not resolve display value to a string. ' +
          'Please check the `md-item-text` attribute.');
      }

      return itemText;
    });

    /**
     * Getter function to invoke user-defined expression (in the directive)
     * to convert your object to a single string.
     * @param item
     * @returns {string|null}
     */
    function getItemText (item) {
      return (item && $scope.itemText) ? $scope.itemText(getItemAsNameVal(item)) : null;
    }
  }

  /**
   * Returns the locals object for compiling item templates.
   * @param item
   * @returns {Object|undefined}
   */
  function getItemAsNameVal (item) {
    if (!item) {
      return undefined;
    }

    var locals = {};
    if (ctrl.itemName) {
      locals[ ctrl.itemName ] = item;
    }

    return locals;
  }

  /**
   * Returns the default index based on whether or not autoselect is enabled.
   * @returns {number} 0 if autoselect is enabled, -1 if not.
   */
  function getDefaultIndex () {
    return $scope.autoselect ? 0 : -1;
  }

  /**
   * Sets the loading parameter and updates the hidden state.
   * @param value {boolean} Whether or not the component is currently loading.
   */
  function setLoading(value) {
    if (ctrl.loading !== value) {
      ctrl.loading = value;
    }

    // Always refresh the hidden variable as something else might have changed
    ctrl.hidden = shouldHide();
  }

  /**
   * Determines if the menu should be hidden.
   * @returns {boolean} true if the menu should be hidden
   */
  function shouldHide () {
    return !shouldShow();
  }

  /**
   * Determines whether the autocomplete is able to query within the current state.
   * @returns {boolean} true if the query can be run
   */
  function isSearchable() {
    if (ctrl.loading && !hasMatches()) {
      // No query when query is in progress.
      return false;
    } else if (hasSelection()) {
      // No query if there is already a selection
      return false;
    }
    else if (!hasFocus) {
      // No query if the input does not have focus
      return false;
    }
    return true;
  }

  /**
   * @returns {boolean} if the escape keydown should be processed, return true.
   *  Otherwise return false.
   */
  function shouldProcessEscape() {
    return hasEscapeOption('blur') || !ctrl.hidden || ctrl.loading || hasEscapeOption('clear') && $scope.searchText;
  }

  /**
   * @param {string} option check if this option is set
   * @returns {boolean} if the specified escape option is set, return true. Return false otherwise.
   */
  function hasEscapeOption(option) {
    return !$scope.escapeOptions || $scope.escapeOptions.toLowerCase().indexOf(option) !== -1;
  }

  /**
   * Determines if the menu should be shown.
   * @returns {boolean} true if the menu should be shown
   */
  function shouldShow() {
    if (ctrl.isReadonly) {
      // Don't show if read only is set
      return false;
    } else if (!isSearchable()) {
      // Don't show if a query is in progress, there is already a selection,
      // or the input is not focused.
      return false;
    }
    return (isMinLengthMet() && hasMatches()) || notFoundVisible();
  }

  /**
   * @returns {boolean} true if the search text has matches.
   */
  function hasMatches() {
    return ctrl.matches.length ? true : false;
  }

  /**
   * @returns {boolean} true if the autocomplete has a valid selection.
   */
  function hasSelection() {
    return ctrl.scope.selectedItem ? true : false;
  }

  /**
   * @returns {boolean} true if the loading indicator is, or should be, visible.
   */
  function loadingIsVisible() {
    return ctrl.loading && !hasSelection();
  }

  /**
   * @returns {*} the display value of the current item.
   */
  function getCurrentDisplayValue () {
    return getDisplayValue(ctrl.matches[ ctrl.index ]);
  }

  /**
   * Determines if the minimum length is met by the search text.
   * @returns {*} true if the minimum length is met by the search text
   */
  function isMinLengthMet () {
    return ($scope.searchText || '').length >= getMinLength();
  }

  // actions

  /**
   * Defines a public property with a handler and a default value.
   * @param {string} key
   * @param {Function} handler function
   * @param {*} value default value
   */
  function defineProperty (key, handler, value) {
    Object.defineProperty(ctrl, key, {
      get: function () { return value; },
      set: function (newValue) {
        var oldValue = value;
        value        = newValue;
        handler(newValue, oldValue);
      }
    });
  }

  /**
   * Selects the item at the given index.
   * @param {number} index to select
   */
  function select (index) {
    // force form to update state for validation
    $mdUtil.nextTick(function () {
      getDisplayValue(ctrl.matches[ index ]).then(function (val) {
        var ngModel = elements.$.input.controller('ngModel');
        $mdLiveAnnouncer.announce(val + ' ' + ctrl.selectedMessage, 'assertive');
        ngModel.$setViewValue(val);
        ngModel.$render();
      }).finally(function () {
        $scope.selectedItem = ctrl.matches[ index ];
        setLoading(false);
      });
    }, false);
  }

  /**
   * Clears the searchText value and selected item.
   */
  function clearValue () {
    clearSelectedItem();
    clearSearchText();
  }

  /**
   * Clears the selected item
   */
  function clearSelectedItem () {
    // Reset our variables
    ctrl.index = 0;
    ctrl.matches = [];
  }

  /**
   * Clears the searchText value
   */
  function clearSearchText () {
    // Set the loading to true so we don't see flashes of content.
    // The flashing will only occur when an async request is running.
    // So the loading process will stop when the results had been retrieved.
    setLoading(true);

    $scope.searchText = '';

    // Normally, triggering the change / input event is unnecessary, because the browser detects it properly.
    // But some browsers are not detecting it properly, which means that we have to trigger the event.
    // Using the `input` is not working properly, because for example IE11 is not supporting the `input` event.
    // The `change` event is a good alternative and is supported by all supported browsers.
    var eventObj = document.createEvent('CustomEvent');
    eventObj.initCustomEvent('change', true, true, { value: '' });
    elements.input.dispatchEvent(eventObj);

    // For some reason, firing the above event resets the value of $scope.searchText if
    // $scope.searchText has a space character at the end, so we blank it one more time and then
    // focus.
    elements.input.blur();
    $scope.searchText = '';
    elements.input.focus();
  }

  /**
   * Fetches the results for the provided search text.
   * @param searchText
   */
  function fetchResults (searchText) {
    var items = $scope.$parent.$eval(itemExpr),
        term  = searchText.toLowerCase(),
        isList = angular.isArray(items),
        isPromise = !!items.then; // Every promise should contain a `then` property

    if (isList) onResultsRetrieved(items);
    else if (isPromise) handleAsyncResults(items);

    function handleAsyncResults(items) {
      if (!items) return;

      items = $q.when(items);
      fetchesInProgress++;
      setLoading(true);

      $mdUtil.nextTick(function () {
          items
            .then(onResultsRetrieved)
            .finally(function(){
              if (--fetchesInProgress === 0) {
                setLoading(false);
              }
            });
      },true, $scope);
    }

    function onResultsRetrieved(matches) {
      cache[term] = matches;

      // Just cache the results if the request is now outdated.
      // The request becomes outdated, when the new searchText has changed during the result fetching.
      if ((searchText || '') !== ($scope.searchText || '')) {
        return;
      }

      handleResults(matches);
    }
  }


  /**
   * Reports given message types to supported screen readers.
   * @param {boolean} isPolite Whether the announcement should be polite.
   * @param {!number} types Message flags to be reported to the screen reader.
   */
  function reportMessages(isPolite, types) {
    var politeness = isPolite ? 'polite' : 'assertive';
    var messages = [];

    if (types & ReportType.Selected && ctrl.index !== -1) {
      messages.push(getCurrentDisplayValue());
    }

    if (types & ReportType.Count) {
      messages.push($q.resolve(getCountMessage()));
    }

    $q.all(messages).then(function(data) {
      $mdLiveAnnouncer.announce(data.join(' '), politeness);
    });
  }

  /**
   * @returns {string} the ARIA message for how many results match the current query.
   */
  function getCountMessage () {
    switch (ctrl.matches.length) {
      case 0:
        return 'There are no matches available.';
      case 1:
        return 'There is 1 match available.';
      default:
        return 'There are ' + ctrl.matches.length + ' matches available.';
    }
  }

  /**
   * Makes sure that the focused element is within view.
   */
  function updateScroll () {
    if (!elements.li[0]) return;
    if (mode === MODE_STANDARD) {
      updateStandardScroll();
    } else {
      updateVirtualScroll();
    }
  }

  function updateVirtualScroll() {
    // elements in virtual scroll have consistent heights
    var optionHeight = elements.li[0].offsetHeight,
        top = optionHeight * ctrl.index,
        bottom = top + optionHeight,
        containerHeight = elements.scroller.clientHeight,
        scrollTop = elements.scroller.scrollTop;

    if (top < scrollTop) {
      scrollTo(top);
    } else if (bottom > scrollTop + containerHeight) {
      scrollTo(bottom - containerHeight);
    }
  }

  function updateStandardScroll() {
    // elements in standard scroll have variable heights
    var selected =  elements.li[ctrl.index] || elements.li[0];
    var containerHeight = elements.scrollContainer.offsetHeight,
        top = selected && selected.offsetTop || 0,
        bottom = top + selected.clientHeight,
        scrollTop = elements.scrollContainer.scrollTop;

    if (top < scrollTop) {
      scrollTo(top);
    } else if (bottom > scrollTop + containerHeight) {
      scrollTo(bottom - containerHeight);
    }
  }

  function isPromiseFetching() {
    return fetchesInProgress !== 0;
  }

  function scrollTo (offset) {
    if (mode === MODE_STANDARD) {
      elements.scrollContainer.scrollTop = offset;
    } else {
      elements.$.scrollContainer.controller('mdVirtualRepeatContainer').scrollTo(offset);
    }
  }

  function notFoundVisible () {
    var textLength = (ctrl.scope.searchText || '').length;

    return ctrl.hasNotFound && !hasMatches() && (!ctrl.loading || isPromiseFetching()) && textLength >= getMinLength() && (hasFocus || noBlur) && !hasSelection();
  }

  /**
   * Starts the query to gather the results for the current searchText.  Attempts to return cached
   * results first, then forwards the process to `fetchResults` if necessary.
   */
  function handleQuery () {
    var searchText = $scope.searchText || '';
    var term = searchText.toLowerCase();

    // If caching is enabled and the current searchText is stored in the cache
    if (!$scope.noCache && cache[term]) {
      // The results should be handled as same as a normal un-cached request does.
      handleResults(cache[term]);
    } else {
      fetchResults(searchText);
    }

    ctrl.hidden = shouldHide();
  }

  /**
   * Handles the retrieved results by showing them in the autocompletes dropdown.
   * @param results Retrieved results
   */
  function handleResults(results) {
    ctrl.matches = results;
    ctrl.hidden  = shouldHide();

    // If loading is in progress, then we'll end the progress. This is needed for example,
    // when the `clear` button was clicked, because there we always show the loading process, to prevent flashing.
    if (ctrl.loading) setLoading(false);

    if ($scope.selectOnMatch) selectItemOnMatch();

    positionDropdown();
    reportMessages(true, ReportType.Count);
  }

  /**
   * If there is only one matching item and the search text matches its display value exactly,
   * automatically select that item.  Note: This function is only called if the user uses the
   * `md-select-on-match` flag.
   */
  function selectItemOnMatch () {
    var searchText = $scope.searchText,
        matches    = ctrl.matches,
        item       = matches[ 0 ];
    if (matches.length === 1) getDisplayValue(item).then(function (displayValue) {
      var isMatching = searchText === displayValue;
      if ($scope.matchInsensitive && !isMatching) {
        isMatching = searchText.toLowerCase() === displayValue.toLowerCase();
      }

      if (isMatching) {
        select(0);
      }
    });
  }

  /**
   * Evaluates an attribute expression against the parent scope.
   * @param {String} attr Name of the attribute to be evaluated.
   * @param {Object?} locals Properties to be injected into the evaluation context.
   */
 function evalAttr(attr, locals) {
    if ($attrs[attr]) {
      $scope.$parent.$eval($attrs[attr], locals || {});
    }
  }

}


MdAutocomplete['$inject'] = ["$$mdSvgRegistry"];angular
    .module('material.components.autocomplete')
    .directive('mdAutocomplete', MdAutocomplete);

/**
 * @ngdoc directive
 * @name mdAutocomplete
 * @module material.components.autocomplete
 *
 * @description
 * `<md-autocomplete>` is a special input component with a drop-down of all possible matches to a
 *     custom query. This component allows you to provide real-time suggestions as the user types
 *     in the input area.
 *
 * To start, you will need to specify the required parameters and provide a template for your
 *     results. The content inside `md-autocomplete` will be treated as a template.
 *
 * In more complex cases, you may want to include other content such as a message to display when
 *     no matches were found.  You can do this by wrapping your template in `md-item-template` and
 *     adding a tag for `md-not-found`.  An example of this is shown below.
 *
 * To reset the displayed value you must clear both values for `md-search-text` and
 * `md-selected-item`.
 *
 * ### Validation
 *
 * You can use `ng-messages` to include validation the same way that you would normally validate;
 *     however, if you want to replicate a standard input with a floating label, you will have to
 *     do the following:
 *
 * - Make sure that your template is wrapped in `md-item-template`
 * - Add your `ng-messages` code inside of `md-autocomplete`
 * - Add your validation properties to `md-autocomplete` (ie. `required`)
 * - Add a `name` to `md-autocomplete` (to be used on the generated `input`)
 *
 * There is an example below of how this should look.
 *
 * ### Snapping Drop-Down
 *
 * You can cause the autocomplete drop-down to snap to an ancestor element by applying the
 *     `md-autocomplete-snap` attribute to that element. You can also snap to the width of
 *     the `md-autocomplete-snap` element by setting the attribute's value to `width`
 *     (ie. `md-autocomplete-snap="width"`).
 *
 * ### Notes
 *
 * **Autocomplete Dropdown Items Rendering**
 *
 * The `md-autocomplete` uses the the <a ng-href="api/directive/mdVirtualRepeat">
 *   mdVirtualRepeat</a> directive for displaying the results inside of the dropdown.<br/>
 *
 * > When encountering issues regarding the item template please take a look at the
 *   <a ng-href="api/directive/mdVirtualRepeatContainer">VirtualRepeatContainer</a> documentation.
 *
 * **Autocomplete inside of a Virtual Repeat**
 *
 * When using the `md-autocomplete` directive inside of a
 * <a ng-href="api/directive/mdVirtualRepeatContainer">VirtualRepeatContainer</a> the dropdown items
 * might not update properly, because caching of the results is enabled by default.
 *
 * The autocomplete will then show invalid dropdown items, because the Virtual Repeat only updates
 * the scope bindings rather than re-creating the `md-autocomplete`. This means that the previous
 * cached results will be used.
 *
 * > To avoid such problems, ensure that the autocomplete does not cache any results via
 * `md-no-cache="true"`:
 *
 * <hljs lang="html">
 *   <md-autocomplete
 *       md-no-cache="true"
 *       md-selected-item="selectedItem"
 *       md-items="item in items"
 *       md-search-text="searchText"
 *       md-item-text="item.display">
 *     <span>{{ item.display }}</span>
 *   </md-autocomplete>
 * </hljs>
 *
 *
 * @param {expression} md-items An expression in the format of `item in results` to iterate over
 *     matches for your search.<br/><br/>
 *     The `results` expression can be also a function, which returns the results synchronously
 *     or asynchronously (per Promise).
 * @param {expression=} md-selected-item-change An expression to be run each time a new item is
 *     selected.
 * @param {expression=} md-search-text-change An expression to be run each time the search text
 *     updates.
 * @param {expression=} md-search-text A model to bind the search query text to.
 * @param {object=} md-selected-item A model to bind the selected item to.
 * @param {expression=} md-item-text An expression that will convert your object to a single string.
 * @param {string=} placeholder Placeholder text that will be forwarded to the input.
 * @param {boolean=} md-no-cache Disables the internal caching that happens in autocomplete.
 * @param {boolean=} ng-disabled Determines whether or not to disable the input field.
 * @param {boolean=} md-require-match When set to true, the autocomplete will add a validator,
 *     which will evaluate to false, when no item is currently selected.
 * @param {number=} md-min-length Specifies the minimum length of text before autocomplete will
 *     make suggestions.
 * @param {number=} md-delay Specifies the amount of time (in milliseconds) to wait before looking
 *     for results.
 * @param {boolean=} md-clear-button Whether the clear button for the autocomplete input should show
 *     up or not.
 * @param {boolean=} md-autofocus If true, the autocomplete will be automatically focused when a
 *     `$mdDialog`, `$mdBottomsheet` or `$mdSidenav`, which contains the autocomplete, is opening.
 *     <br/><br/>
 *     Also the autocomplete will immediately focus the input element.
 * @param {boolean=} md-no-asterisk When present, asterisk will not be appended to the floating
 *     label.
 * @param {boolean=} md-autoselect If set to true, the first item will be automatically selected
 *     in the dropdown upon open.
 * @param {string=} md-input-name The name attribute given to the input element to be used with
 *     FormController.
 * @param {string=} md-menu-class This class will be applied to the dropdown menu for styling.
 * @param {string=} md-menu-container-class This class will be applied to the parent container
 *     of the dropdown panel.
 * @param {string=} md-input-class This will be applied to the input for styling. This attribute
 *     is only valid when a `md-floating-label` is defined.
 * @param {string=} md-floating-label This will add a floating label to autocomplete and wrap it in
 *     `md-input-container`.
 * @param {string=} md-select-on-focus When present the input's text will be automatically selected
 *     on focus.
 * @param {string=} md-input-id An ID to be added to the input element.
 * @param {number=} md-input-minlength The minimum length for the input's value for validation.
 * @param {number=} md-input-maxlength The maximum length for the input's value for validation.
 * @param {boolean=} md-select-on-match When set, autocomplete will automatically select
 *     the item if the search text is an exact match. <br/><br/>
 *     An exact match is when only one match is displayed.
 * @param {boolean=} md-match-case-insensitive When set and using `md-select-on-match`, autocomplete
 *     will select on case-insensitive match.
 * @param {string=} md-escape-options Override escape key logic. Default is `blur clear`.<br/>
 *     Options: `blur`, `clear`, `none`.
 * @param {string=} md-dropdown-items Specifies the maximum amount of items to be shown in
 *     the dropdown.<br/><br/>
 *     When the dropdown doesn't fit into the viewport, the dropdown will shrink
 *     as much as possible.
 * @param {string=} md-dropdown-position Overrides the default dropdown position. Options: `top`,
 *    `bottom`.
 * @param {string=} input-aria-describedby A space-separated list of element IDs. This should
 *     contain the IDs of any elements that describe this autocomplete. Screen readers will read the
 *     content of these elements at the end of announcing that the autocomplete has been selected
 *     and describing its current state. The descriptive elements do not need to be visible on the
 *     page.
 * @param {string=} input-aria-labelledby A space-separated list of element IDs. The ideal use case
 *     is that this would contain the ID of a `<label>` element that is associated with this
 *     autocomplete. This will only have affect when `md-floating-label` is not defined.<br><br>
 *     For `<label id="state">US State</label>`, you would set this to
 *     `input-aria-labelledby="state"`.
 * @param {string=} input-aria-label A label that will be applied to the autocomplete's input.
 *    This will be announced by screen readers before the placeholder.
 *    This will only have affect when `md-floating-label` is not defined. If you define both
 *    `input-aria-label` and `input-aria-labelledby`, then `input-aria-label` will take precedence.
 * @param {string=} md-selected-message Attribute to specify the text that the screen reader will
 *    announce after a value is selected. Default is: "selected". If `Alaska` is selected in the
 *    options panel, it will read "Alaska selected". You will want to override this when your app
 *    is running in a non-English locale.
 * @param {boolean=} ng-trim If set to false, the search text will be not trimmed automatically.
 *     Defaults to true.
 * @param {string=} ng-pattern Adds the pattern validator to the ngModel of the search text.
 *     See the [ngPattern Directive](https://docs.angularjs.org/api/ng/directive/ngPattern)
 *     for more details.
 * @param {string=} md-mode Specify the repeat mode for suggestion lists. Acceptable values include
 *     `virtual` (md-virtual-repeat) and `standard` (ng-repeat). See the
 *     `Specifying Repeat Mode` example for mode details. Default is `virtual`.
 *
 * @usage
 * ### Basic Example
 * <hljs lang="html">
 *   <md-autocomplete
 *       md-selected-item="selectedItem"
 *       md-search-text="searchText"
 *       md-items="item in getMatches(searchText)"
 *       md-item-text="item.display">
 *     <span md-highlight-text="searchText">{{item.display}}</span>
 *   </md-autocomplete>
 * </hljs>
 *
 * ### Example with "not found" message
 * <hljs lang="html">
 * <md-autocomplete
 *     md-selected-item="selectedItem"
 *     md-search-text="searchText"
 *     md-items="item in getMatches(searchText)"
 *     md-item-text="item.display">
 *   <md-item-template>
 *     <span md-highlight-text="searchText">{{item.display}}</span>
 *   </md-item-template>
 *   <md-not-found>
 *     No matches found.
 *   </md-not-found>
 * </md-autocomplete>
 * </hljs>
 *
 * In this example, our code utilizes `md-item-template` and `md-not-found` to specify the
 *     different parts that make up our component.
 *
 * ### Clear button for the input
 * By default, the clear button is displayed when there is input. This aligns with the spec's
 * [Search Pattern](https://material.io/archive/guidelines/patterns/search.html#search-in-app-search).
 * In floating label mode, when `md-floating-label="My Label"` is applied, the clear button is not
 * displayed by default (see the spec's
 * [Autocomplete Text Field](https://material.io/archive/guidelines/components/text-fields.html#text-fields-layout)).
 *
 * Nevertheless, developers are able to explicitly toggle the clear button for all autocomplete
 * components with `md-clear-button`.
 *
 * <hljs lang="html">
 *   <md-autocomplete ... md-clear-button="true"></md-autocomplete>
 *   <md-autocomplete ... md-clear-button="false"></md-autocomplete>
 * </hljs>
 *
 * In previous versions, the clear button was always hidden when the component was disabled.
 * This changed in `1.1.5` to give the developer control of this behavior. This example
 * will hide the clear button only when the component is disabled.
 *
 * <hljs lang="html">
 *   <md-autocomplete ... ng-disabled="disabled" md-clear-button="!disabled"></md-autocomplete>
 * </hljs>
 *
 * ### Example with validation
 * <hljs lang="html">
 * <form name="autocompleteForm">
 *   <md-autocomplete
 *       required
 *       md-input-name="autocomplete"
 *       md-selected-item="selectedItem"
 *       md-search-text="searchText"
 *       md-items="item in getMatches(searchText)"
 *       md-item-text="item.display">
 *     <md-item-template>
 *       <span md-highlight-text="searchText">{{item.display}}</span>
 *     </md-item-template>
 *     <div ng-messages="autocompleteForm.autocomplete.$error">
 *       <div ng-message="required">This field is required</div>
 *     </div>
 *   </md-autocomplete>
 * </form>
 * </hljs>
 *
 * In this example, our code utilizes `md-item-template` and `ng-messages` to specify
 *     input validation for the field.
 *
 * ### Asynchronous Results
 * The autocomplete items expression also supports promises, which will resolve with the query
 * results.
 *
 * <hljs lang="js">
 *   function AppController($scope, $http) {
 *     $scope.query = function(searchText) {
 *       return $http
 *         .get(BACKEND_URL + '/items/' + searchText)
 *         .then(function(data) {
 *           // Map the response object to the data object.
 *           return data;
 *         });
 *     };
 *   }
 * </hljs>
 *
 * <hljs lang="html">
 *   <md-autocomplete
 *       md-selected-item="selectedItem"
 *       md-search-text="searchText"
 *       md-items="item in query(searchText)">
 *     <md-item-template>
 *       <span md-highlight-text="searchText">{{item}}</span>
 *     </md-item-template>
 * </md-autocomplete>
 * </hljs>
 *
 * ### Specifying Repeat Mode
 * You can use `md-mode` to specify whether to use standard or virtual lists for
 * rendering autocomplete options.
 * The `md-mode` accepts two values:
 * - `virtual` (default) Uses `md-virtual-repeat` to render list items. Virtual
 *    mode requires you to have consistent heights for all suggestions.
 * - `standard` uses `ng-repeat` to render list items. This allows you to have
 *    options of varying heights.
 *
 * Note that using 'standard' mode will require you to address any list
 * performance issues (e.g. pagination) separately within your application.
 *
 * <hljs lang="html">
 *   <md-autocomplete
 *       md-selected-item="selectedItem"
 *       md-search-text="searchText"
 *       md-items="item in getMatches(searchText)"
 *       md-item-text="item.display"
 *       md-mode="standard">
 *     <span md-highlight-text="searchText">{{item.display}}</span>
 *   </md-autocomplete>
 * </hljs>
 */
function MdAutocomplete ($$mdSvgRegistry) {
  var REPEAT_STANDARD = 'standard';
  var REPEAT_VIRTUAL = 'virtual';
  var REPEAT_MODES = [REPEAT_STANDARD, REPEAT_VIRTUAL];

  /** get a valid repeat mode from an md-mode attribute string. */
  function getRepeatMode(modeStr) {
    if (!modeStr) { return REPEAT_VIRTUAL; }
    modeStr = modeStr.toLowerCase();
    return  REPEAT_MODES.indexOf(modeStr) > -1 ? modeStr : REPEAT_VIRTUAL;
  }

  return {
    controller:   'MdAutocompleteCtrl',
    controllerAs: '$mdAutocompleteCtrl',
    scope:        {
      inputName:          '@mdInputName',
      inputMinlength:     '@mdInputMinlength',
      inputMaxlength:     '@mdInputMaxlength',
      searchText:         '=?mdSearchText',
      selectedItem:       '=?mdSelectedItem',
      itemsExpr:          '@mdItems',
      itemText:           '&mdItemText',
      placeholder:        '@placeholder',
      inputAriaDescribedBy: '@?inputAriaDescribedby',
      inputAriaLabelledBy: '@?inputAriaLabelledby',
      inputAriaLabel:     '@?inputAriaLabel',
      noCache:            '=?mdNoCache',
      requireMatch:       '=?mdRequireMatch',
      selectOnMatch:      '=?mdSelectOnMatch',
      matchInsensitive:   '=?mdMatchCaseInsensitive',
      itemChange:         '&?mdSelectedItemChange',
      textChange:         '&?mdSearchTextChange',
      minLength:          '=?mdMinLength',
      delay:              '=?mdDelay',
      autofocus:          '=?mdAutofocus',
      floatingLabel:      '@?mdFloatingLabel',
      autoselect:         '=?mdAutoselect',
      menuClass:          '@?mdMenuClass',
      menuContainerClass: '@?mdMenuContainerClass',
      inputClass:         '@?mdInputClass',
      inputId:            '@?mdInputId',
      escapeOptions:      '@?mdEscapeOptions',
      dropdownItems:      '=?mdDropdownItems',
      dropdownPosition:   '@?mdDropdownPosition',
      clearButton:        '=?mdClearButton',
      selectedMessage:    '@?mdSelectedMessage',
      mdMode: '=?mdMode'
    },
    compile: function(tElement, tAttrs) {
      var attributes = ['md-select-on-focus', 'md-no-asterisk', 'ng-trim', 'ng-pattern'];
      var input = tElement.find('input');

      attributes.forEach(function(attribute) {
        var attrValue = tAttrs[tAttrs.$normalize(attribute)];

        if (attrValue !== null) {
          input.attr(attribute, attrValue);
        }
      });

      return function(scope, element, attrs, ctrl) {
        // Retrieve the state of using a md-not-found template by using our attribute, which will
        // be added to the element in the template function.
        ctrl.hasNotFound = !!element.attr('md-has-not-found');

        // By default the inset autocomplete should show the clear button when not explicitly
        // overwritten or in floating label mode.
        if (!angular.isDefined(attrs.mdClearButton) && !scope.floatingLabel) {
          scope.clearButton = true;
        }

        scope.mdMode = getRepeatMode(attrs.mdMode);
      };
    },
    template:     function (element, attr) {
      var noItemsTemplate = getNoItemsTemplate(),
          itemTemplate    = getItemTemplate(),
          leftover        = element.html(),
          tabindex        = attr.tabindex;

      // Set our attribute for the link function above which runs later.
      // We will set an attribute, because otherwise the stored variables will be trashed when
      // removing the element is hidden while retrieving the template. For example when using ngIf.
      if (noItemsTemplate) element.attr('md-has-not-found', true);

      // Always set our tabindex of the autocomplete directive to -1, because our input
      // will hold the actual tabindex.
      element.attr('tabindex', '-1');

      return '\
        <md-autocomplete-wrap\
            ng-class="{ \'md-whiteframe-z1\': !floatingLabel, \
                        \'md-menu-showing\': !$mdAutocompleteCtrl.hidden, \
                        \'md-show-clear-button\': !!clearButton }">\
          ' + getInputElement() + '\
          ' + getClearButton() + '\
          <md-progress-linear\
              class="' + (attr.mdFloatingLabel ? 'md-inline' : '') + '"\
              ng-if="$mdAutocompleteCtrl.loadingIsVisible()"\
              md-mode="indeterminate"></md-progress-linear>\
          ' + getContainer(attr.mdMenuContainerClass, attr.mdMode) + '\
            <ul class="md-autocomplete-suggestions"\
                ng-class="::menuClass"\
                id="ul-{{$mdAutocompleteCtrl.id}}"\
                ng-mouseenter="$mdAutocompleteCtrl.listEnter()"\
                ng-mouseleave="$mdAutocompleteCtrl.listLeave()"\
                ng-mouseup="$mdAutocompleteCtrl.mouseUp()"\
                role="listbox">\
              <li ' + getRepeatType(attr.mdMode) + ' ="item in $mdAutocompleteCtrl.matches"\
                  ng-class="{ selected: $index === $mdAutocompleteCtrl.index }"\
                  ng-attr-id="{{$index === $mdAutocompleteCtrl.index ? \'selected_option\' : undefined}}"\
                  ng-click="$mdAutocompleteCtrl.select($index)"\
                  role="option"\
                  aria-setsize="{{$mdAutocompleteCtrl.matches.length}}"\
                  aria-posinset="{{$index+1}}"\
                  aria-selected="{{$index === $mdAutocompleteCtrl.index ? true : false}}" \
                  md-extra-name="$mdAutocompleteCtrl.itemName">\
                  ' + itemTemplate + '\
                  </li>' + noItemsTemplate + '\
            </ul>\
          '  + getContainerClosingTags(attr.mdMode) + '\
        </md-autocomplete-wrap>';

      function getItemTemplate() {
        var templateTag = element.find('md-item-template').detach(),
            html = templateTag.length ? templateTag.html() : element.html();
        if (!templateTag.length) element.empty();
        return '<md-autocomplete-parent-scope md-autocomplete-replace>' + html +
               '</md-autocomplete-parent-scope>';
      }

      function getNoItemsTemplate() {
        var templateTag = element.find('md-not-found').detach(),
            template = templateTag.length ? templateTag.html() : '';
        return template
            ? '<li ng-if="$mdAutocompleteCtrl.notFoundVisible()"\
                         md-autocomplete-parent-scope>' + template + '</li>'
            : '';
      }

      function getContainer(menuContainerClass, repeatMode) {
        // prepend a space if needed
        menuContainerClass = menuContainerClass ? ' ' + menuContainerClass : '';

        if (isVirtualRepeatDisabled(repeatMode)) {
          return '\
            <div \
                ng-hide="$mdAutocompleteCtrl.hidden"\
                class="md-standard-list-container md-autocomplete-suggestions-container md-whiteframe-z1' + menuContainerClass + '"\
                ng-class="{ \'md-not-found\': $mdAutocompleteCtrl.notFoundVisible() }"\
                role="presentation">\
              <div class="md-standard-list-scroller" role="presentation">';
        }

        return '\
          <md-virtual-repeat-container\
              md-auto-shrink\
              md-auto-shrink-min="1"\
              ng-hide="$mdAutocompleteCtrl.hidden"\
              class="md-virtual-repeat-container md-autocomplete-suggestions-container md-whiteframe-z1' + menuContainerClass + '"\
              ng-class="{ \'md-not-found\': $mdAutocompleteCtrl.notFoundVisible() }"\
              role="presentation">';
      }

      function getContainerClosingTags(repeatMode) {
        return isVirtualRepeatDisabled(repeatMode) ?
            '   </div>\
              </div>\
            </div>' : '</md-virtual-repeat-container>';
      }

      function getRepeatType(repeatMode) {
        return isVirtualRepeatDisabled(repeatMode)  ?
          'ng-repeat' : 'md-virtual-repeat';
      }

      function isVirtualRepeatDisabled(repeatMode) {
        // ensure we have a valid repeat mode
        var correctedRepeatMode = getRepeatMode(repeatMode);
        return correctedRepeatMode !== REPEAT_VIRTUAL;
      }

      function getInputElement () {
        if (attr.mdFloatingLabel) {
          return '\
            <md-input-container ng-if="floatingLabel">\
              <label>{{floatingLabel}}</label>\
              <input type="search"\
                ' + (tabindex != null ? 'tabindex="' + tabindex + '"' : '') + '\
                id="{{ inputId || \'fl-input-\' + $mdAutocompleteCtrl.id }}"\
                name="{{inputName}}"\
                ng-class="::inputClass"\
                autocomplete="off"\
                ng-required="$mdAutocompleteCtrl.isRequired"\
                ng-readonly="$mdAutocompleteCtrl.isReadonly"\
                ng-minlength="inputMinlength"\
                ng-maxlength="inputMaxlength"\
                ng-disabled="$mdAutocompleteCtrl.isDisabled"\
                ng-model="$mdAutocompleteCtrl.scope.searchText"\
                ng-model-options="{ allowInvalid: true }"\
                ng-keydown="$mdAutocompleteCtrl.keydown($event)"\
                ng-blur="$mdAutocompleteCtrl.blur($event)"\
                ng-focus="$mdAutocompleteCtrl.focus($event)"\
                aria-label="{{floatingLabel}}"\
                aria-autocomplete="list"\
                role="combobox"\
                aria-haspopup="true"\
                aria-expanded="{{!$mdAutocompleteCtrl.hidden}}"\
                aria-owns="ul-{{$mdAutocompleteCtrl.id}}"\
                ng-attr-aria-activedescendant="{{$mdAutocompleteCtrl.index >= 0 ? \'selected_option\' : undefined}}">\
              <div md-autocomplete-parent-scope md-autocomplete-replace>' + leftover + '</div>\
            </md-input-container>';
        } else {
          return '\
            <input type="search"\
              ' + (tabindex != null ? 'tabindex="' + tabindex + '"' : '') + '\
              id="{{ inputId || \'input-\' + $mdAutocompleteCtrl.id }}"\
              name="{{inputName}}"\
              ng-class="::inputClass"\
              ng-if="!floatingLabel"\
              autocomplete="off"\
              ng-required="$mdAutocompleteCtrl.isRequired"\
              ng-disabled="$mdAutocompleteCtrl.isDisabled"\
              ng-readonly="$mdAutocompleteCtrl.isReadonly"\
              ng-minlength="inputMinlength"\
              ng-maxlength="inputMaxlength"\
              ng-model="$mdAutocompleteCtrl.scope.searchText"\
              ng-keydown="$mdAutocompleteCtrl.keydown($event)"\
              ng-blur="$mdAutocompleteCtrl.blur($event)"\
              ng-focus="$mdAutocompleteCtrl.focus($event)"\
              placeholder="{{placeholder}}"\
              aria-label="{{placeholder}}"\
              aria-autocomplete="list"\
              role="combobox"\
              aria-haspopup="true"\
              aria-expanded="{{!$mdAutocompleteCtrl.hidden}}"\
              aria-owns="ul-{{$mdAutocompleteCtrl.id}}"\
              ng-attr-aria-activedescendant="{{$mdAutocompleteCtrl.index >= 0 ? \'selected_option\' : undefined}}">';
        }
      }

      function getClearButton() {
        return '' +
          '<button ' +
              'type="button" ' +
              'aria-label="Clear Input" ' +
              'tabindex="-1" ' +
              'ng-if="clearButton && $mdAutocompleteCtrl.scope.searchText" ' +
              'ng-click="$mdAutocompleteCtrl.clear($event)">' +
            '<md-icon md-svg-src="' + $$mdSvgRegistry.mdClose + '"></md-icon>' +
          '</button>';
        }
    }
  };
}


MdAutocompleteItemScopeDirective['$inject'] = ["$compile", "$mdUtil"];angular
  .module('material.components.autocomplete')
  .directive('mdAutocompleteParentScope', MdAutocompleteItemScopeDirective);

function MdAutocompleteItemScopeDirective($compile, $mdUtil) {
  return {
    restrict: 'AE',
    compile: compile,
    terminal: true,
    transclude: 'element'
  };

  function compile(tElement, tAttr, transclude) {
    return function postLink(scope, element, attr) {
      var ctrl = scope.$mdAutocompleteCtrl;
      var newScope = ctrl.parent.$new();
      var itemName = ctrl.itemName;

      // Watch for changes to our scope's variables and copy them to the new scope
      watchVariable('$index', '$index');
      watchVariable('item', itemName);

      // Ensure that $digest calls on our scope trigger $digest on newScope.
      connectScopes();

      // Link the element against newScope.
      transclude(newScope, function(clone) {
        element.after(clone);
      });

      /**
       * Creates a watcher for variables that are copied from the parent scope
       * @param variable
       * @param alias
       */
      function watchVariable(variable, alias) {
        newScope[alias] = scope[variable];

        scope.$watch(variable, function(value) {
          $mdUtil.nextTick(function() {
            newScope[alias] = value;
          });
        });
      }

      /**
       * Creates watchers on scope and newScope that ensure that for any
       * $digest of scope, newScope is also $digested.
       */
      function connectScopes() {
        var scopeDigesting = false;
        var newScopeDigesting = false;

        scope.$watch(function() {
          if (newScopeDigesting || scopeDigesting) {
            return;
          }

          scopeDigesting = true;
          scope.$$postDigest(function() {
            if (!newScopeDigesting) {
              newScope.$digest();
            }

            scopeDigesting = newScopeDigesting = false;
          });
        });

        newScope.$watch(function() {
          newScopeDigesting = true;
        });
      }
    };
  }
}

MdHighlightCtrl['$inject'] = ["$scope", "$element", "$attrs"];angular
    .module('material.components.autocomplete')
    .controller('MdHighlightCtrl', MdHighlightCtrl);

function MdHighlightCtrl ($scope, $element, $attrs) {
  this.$scope = $scope;
  this.$element = $element;
  this.$attrs = $attrs;

  // Cache the Regex to avoid rebuilding each time.
  this.regex = null;
}

MdHighlightCtrl.prototype.init = function(unsafeTermFn, unsafeContentFn) {

  this.flags = this.$attrs.mdHighlightFlags || '';

  this.unregisterFn = this.$scope.$watch(function($scope) {
    return {
      term: unsafeTermFn($scope),
      contentText: unsafeContentFn($scope)
    };
  }.bind(this), this.onRender.bind(this), true);

  this.$element.on('$destroy', this.unregisterFn);
};

/**
 * Triggered once a new change has been recognized and the highlighted
 * text needs to be updated.
 */
MdHighlightCtrl.prototype.onRender = function(state, prevState) {

  var contentText = state.contentText;

  /* Update the regex if it's outdated, because we don't want to rebuilt it constantly. */
  if (this.regex === null || state.term !== prevState.term) {
    this.regex = this.createRegex(state.term, this.flags);
  }

  /* If a term is available apply the regex to the content */
  if (state.term) {
    this.applyRegex(contentText);
  } else {
    this.$element.text(contentText);
  }

};

/**
 * Decomposes the specified text into different tokens (whether match or not).
 * Breaking down the string guarantees proper XSS protection due to the native browser
 * escaping of unsafe text.
 */
MdHighlightCtrl.prototype.applyRegex = function(text) {
  var tokens = this.resolveTokens(text);

  this.$element.empty();

  tokens.forEach(function (token) {

    if (token.isMatch) {
      var tokenEl = angular.element('<span class="highlight">').text(token.text);

      this.$element.append(tokenEl);
    } else {
      this.$element.append(document.createTextNode(token));
    }

  }.bind(this));

};

  /**
 * Decomposes the specified text into different tokens by running the regex against the text.
 */
MdHighlightCtrl.prototype.resolveTokens = function(string) {
  var tokens = [];
  var lastIndex = 0;

  // Use replace here, because it supports global and single regular expressions at same time.
  string.replace(this.regex, function(match, index) {
    appendToken(lastIndex, index);

    tokens.push({
      text: match,
      isMatch: true
    });

    lastIndex = index + match.length;
  });

  // Append the missing text as a token.
  appendToken(lastIndex);

  return tokens;

  function appendToken(from, to) {
    var targetText = string.slice(from, to);
    targetText && tokens.push(targetText);
  }
};

/** Creates a regex for the specified text with the given flags. */
MdHighlightCtrl.prototype.createRegex = function(term, flags) {
  var startFlag = '', endFlag = '';
  var regexTerm = this.sanitizeRegex(term);

  if (flags.indexOf('^') >= 0) startFlag = '^';
  if (flags.indexOf('$') >= 0) endFlag = '$';

  return new RegExp(startFlag + regexTerm + endFlag, flags.replace(/[$^]/g, ''));
};

/** Sanitizes a regex by removing all common RegExp identifiers */
MdHighlightCtrl.prototype.sanitizeRegex = function(term) {
  return term && term.toString().replace(/[\\^$*+?.()|{}[\]]/g, '\\$&');
};


MdHighlight['$inject'] = ["$interpolate", "$parse"];angular
    .module('material.components.autocomplete')
    .directive('mdHighlightText', MdHighlight);

/**
 * @ngdoc directive
 * @name mdHighlightText
 * @module material.components.autocomplete
 *
 * @description
 * The `md-highlight-text` directive allows you to specify text that should be highlighted within
 *     an element.  Highlighted text will be wrapped in `<span class="highlight"></span>` which can
 *     be styled through CSS.  Please note that child elements may not be used with this directive.
 *
 * @param {string} md-highlight-text A model to be searched for
 * @param {string=} md-highlight-flags A list of flags (loosely based on JavaScript RexExp flags).
 * #### **Supported flags**:
 * - `g`: Find all matches within the provided text
 * - `i`: Ignore case when searching for matches
 * - `$`: Only match if the text ends with the search term
 * - `^`: Only match if the text begins with the search term
 *
 * @usage
 * <hljs lang="html">
 * <input placeholder="Enter a search term..." ng-model="searchTerm" type="text" />
 * <ul>
 *   <li ng-repeat="result in results" md-highlight-text="searchTerm" md-highlight-flags="i">
 *     {{result.text}}
 *   </li>
 * </ul>
 * </hljs>
 */

function MdHighlight ($interpolate, $parse) {
  return {
    terminal: true,
    controller: 'MdHighlightCtrl',
    compile: function mdHighlightCompile(tElement, tAttr) {
      var termExpr = $parse(tAttr.mdHighlightText);
      var unsafeContentExpr = $interpolate(tElement.html());

      return function mdHighlightLink(scope, element, attr, ctrl) {
        ctrl.init(termExpr, unsafeContentExpr);
      };
    }
  };
}

})(window, window.angular);