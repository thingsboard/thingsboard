/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.navBar');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.navBar
 */


MdNavBar['$inject'] = ["$mdAria", "$mdTheming"];
MdNavBarController['$inject'] = ["$element", "$scope", "$timeout", "$mdConstant"];
MdNavItem['$inject'] = ["$mdAria", "$$rAF", "$mdUtil", "$window"];
MdNavItemController['$inject'] = ["$element"];
angular.module('material.components.navBar', ['material.core'])
    .controller('MdNavBarController', MdNavBarController)
    .directive('mdNavBar', MdNavBar)
    .controller('MdNavItemController', MdNavItemController)
    .directive('mdNavItem', MdNavItem);

/**
 * @ngdoc directive
 * @name mdNavBar
 * @module material.components.navBar
 *
 * @restrict E
 *
 * @description
 * The `<md-nav-bar>` directive renders a list of material tabs that can be used
 * for top-level page navigation. Unlike `<md-tabs>`, it has no concept of a tab
 * body and no bar pagination.
 *
 * Because it deals with page navigation, certain routing concepts are built-in.
 * Route changes via `ng-href`, `ui-sref`, or `ng-click` events are supported.
 * Alternatively, the user could simply watch the value of `md-selected-nav-item`
 * (`currentNavItem` in the below example) for changes.
 *
 * Accessibility functionality is implemented as a
 * <a href="https://www.w3.org/TR/wai-aria-1.0/complete#tablist">
 *   tablist</a> with
 * <a href="https://www.w3.org/TR/wai-aria-1.0/complete#tab">tabs</a>.
 * We've kept the `role="navigation"` on the `<nav>`, for backwards compatibility, even though
 *  it is not required in the
 * <a href="https://www.w3.org/TR/wai-aria-practices/#aria_lh_navigation">
 *   latest Working Group Note from December 2017</a>.
 *
 * <h3>Keyboard Navigation</h3>
 * - `Tab`/`Shift+Tab` moves the focus to the next/previous interactive element on the page
 * - `Enter`/`Space` selects the focused nav item and navigates to display the item's contents
 * - `Right`/`Down` moves focus to the next nav item, wrapping at the end
 * - `Left`/`Up` moves focus to the previous nav item, wrapping at the end
 * - `Home`/`End` moves the focus to the first/last nav item
 *
 * @param {string=} md-selected-nav-item The name of the current tab; this must
 *     match the `name` attribute of `<md-nav-item>`.
 * @param {boolean=} md-no-ink-bar If set to true, the ink bar will be hidden.
 * @param {string=} nav-bar-aria-label An `aria-label` applied to the `md-nav-bar`'s tablist
 * for accessibility.
 *
 * @usage
 * <hljs lang="html">
 *  <md-nav-bar md-selected-nav-item="currentNavItem">
 *    <md-nav-item md-nav-click="goto('page1')" name="page1">
 *      Page One
 *    </md-nav-item>
 *    <md-nav-item md-nav-href="#page2" name="page3">Page Two</md-nav-item>
 *    <md-nav-item md-nav-sref="page3" name="page2">Page Three</md-nav-item>
 *    <md-nav-item
 *      md-nav-sref="app.page4"
 *      sref-opts="{reload: true, notify: true}"
 *      name="page4">
 *      Page Four
 *    </md-nav-item>
 *  </md-nav-bar>
 *</hljs>
 * <hljs lang="js">
 * (function() {
 *   'use strict';
 *
 *    $rootScope.$on('$routeChangeSuccess', function(event, current) {
 *      $scope.currentLink = getCurrentLinkFromRoute(current);
 *    });
 * });
 * </hljs>
 */
/**
 * @param $mdAria
 * @param $mdTheming
 * @constructor
 * ngInject
 */
function MdNavBar($mdAria, $mdTheming) {
  return {
    restrict: 'E',
    transclude: true,
    controller: MdNavBarController,
    controllerAs: 'ctrl',
    bindToController: true,
    scope: {
      'mdSelectedNavItem': '=?',
      'mdNoInkBar': '=?',
      'navBarAriaLabel': '@?',
    },
    template:
      '<div class="md-nav-bar">' +
        '<nav role="navigation">' +
          '<ul class="_md-nav-bar-list" ng-transclude role="tablist" ' +
            'ng-focus="ctrl.onFocus()" ' + // Deprecated but kept for now in order to not break tests
            'aria-label="{{ctrl.navBarAriaLabel}}">' +
          '</ul>' +
        '</nav>' +
        '<md-nav-ink-bar ng-hide="ctrl.mdNoInkBar"></md-nav-ink-bar>' +
      '</div>',
    link: function(scope, element, attrs, ctrl) {
      $mdTheming(element);
      if (!ctrl.navBarAriaLabel) {
        $mdAria.expectAsync(element, 'aria-label', angular.noop);
      }
    },
  };
}

/**
 * Controller for the nav-bar component.
 * Accessibility functionality is implemented as a tablist
 * (https://www.w3.org/TR/wai-aria-1.0/complete#tablist) and
 * tabs (https://www.w3.org/TR/wai-aria-1.0/complete#tab).
 *
 * @param {!angular.JQLite} $element
 * @param {!angular.Scope} $scope
 * @param {!angular.Timeout} $timeout
 * @param {!Object} $mdConstant
 * @constructor
 * @final
 * ngInject
 */
function MdNavBarController($element, $scope, $timeout, $mdConstant) {
  // Injected variables
  /** @private @const {!angular.Timeout} */
  this._$timeout = $timeout;

  /** @private @const {!angular.Scope} */
  this._$scope = $scope;

  /** @private @const {!Object} */
  this._$mdConstant = $mdConstant;

  // Data-bound variables.
  /** @type {string} */
  this.mdSelectedNavItem;

  /** @type {string} */
  this.navBarAriaLabel;

  // State variables.

  /** @type {?angular.JQLite} */
  this._navBarEl = $element[0];

  /** @type {?angular.JQLite} */
  this._inkbar;

  var self = this;
  // need to wait for transcluded content to be available
  var deregisterTabWatch = this._$scope.$watch(function() {
    return self._navBarEl.querySelectorAll('._md-nav-button').length;
  },
  function(newLength) {
    if (newLength > 0) {
      self._initTabs();
      deregisterTabWatch();
    }
  });
}

/**
 * Initializes the tab components once they exist.
 * @private
 */
MdNavBarController.prototype._initTabs = function() {
  this._inkbar = angular.element(this._navBarEl.querySelector('md-nav-ink-bar'));

  var self = this;
  this._$timeout(function() {
    self._updateTabs(self.mdSelectedNavItem, undefined);
  });

  this._$scope.$watch('ctrl.mdSelectedNavItem', function(newValue, oldValue) {
    // Wait a digest before update tabs for products doing
    // anything dynamic in the template.
    self._$timeout(function() {
      self._updateTabs(newValue, oldValue);
    });
  });
};

/**
 * Set the current tab to be selected.
 * @param {string|undefined} newValue New current tab name.
 * @param {string|undefined} oldValue Previous tab name.
 * @private
 */
MdNavBarController.prototype._updateTabs = function(newValue, oldValue) {
  var self = this;
  var tabs = this._getTabs();
  var sameTab = newValue === oldValue;

  // this._getTabs can return null if nav-bar has not yet been initialized
  if (!tabs) return;

  var oldIndex = -1;
  var newIndex = -1;
  var newTab = this._getTabByName(newValue);
  var oldTab = this._getTabByName(oldValue);

  if (oldTab) {
    oldTab.setSelected(false);
    oldIndex = tabs.indexOf(oldTab);
  }

  if (newTab) {
    newTab.setSelected(true);
    newIndex = tabs.indexOf(newTab);
  }

  this._$timeout(function() {
    self._updateInkBarStyles(newTab, newIndex, oldIndex);
    // Don't change focus when there is no newTab, the new and old tabs are the same, or when
    // called from MdNavBarController._initTabs() which would have no oldTab defined.
    if (newTab && oldTab && !sameTab) {
      self._moveFocus(oldTab, newTab);
    }
  });
};

/**
 * Repositions the ink bar to the selected tab.
 * @private
 */
MdNavBarController.prototype._updateInkBarStyles = function(tab, newIndex, oldIndex) {
  this._inkbar.toggleClass('_md-left', newIndex < oldIndex)
      .toggleClass('_md-right', newIndex > oldIndex);

  this._inkbar.css({display: newIndex < 0 ? 'none' : ''});

  if (tab) {
    var tabEl = tab.getButtonEl();
    var left = tabEl.offsetLeft;

    this._inkbar.css({left: left + 'px', width: tabEl.offsetWidth + 'px'});
  }
};

/**
 * Returns an array of the current tabs.
 * @return {Array<!MdNavItemController>}
 * @private
 */
MdNavBarController.prototype._getTabs = function() {
  var controllers = Array.prototype.slice.call(
    this._navBarEl.querySelectorAll('.md-nav-item'))
    .map(function(el) {
      return angular.element(el).controller('mdNavItem');
    });
  return controllers.indexOf(undefined) ? controllers : null;
};

/**
 * Returns the tab with the specified name.
 * @param {string} name The name of the tab, found in its name attribute.
 * @return {MdNavItemController}
 * @private
 */
MdNavBarController.prototype._getTabByName = function(name) {
  return this._findTab(function(tab) {
    return tab.getName() === name;
  });
};

/**
 * Returns the selected tab.
 * @return {MdNavItemController}
 * @private
 */
MdNavBarController.prototype._getSelectedTab = function() {
  return this._findTab(function(tab) {
    return tab.isSelected();
  });
};

/**
 * Returns the focused tab.
 * @return {MdNavItemController}
 */
MdNavBarController.prototype.getFocusedTab = function() {
  return this._findTab(function(tab) {
    return tab.hasFocus();
  });
};

/**
 * Find a tab that matches the specified function, starting from the first tab.
 * @param {Function} fn
 * @param {number=} startIndex index to start at. Defaults to 0.
 * @returns {MdNavItemController}
 * @private
 */
MdNavBarController.prototype._findTab = function(fn, startIndex) {
  var tabs = this._getTabs();
  if (startIndex === undefined || startIndex === null) {
    startIndex = 0;
  }
  for (var i = startIndex; i < tabs.length; i++) {
    if (fn(tabs[i])) {
      return tabs[i];
    }
  }
  return null;
};

/**
 * Find a tab that matches the specified function, going backwards from the end of the list.
 * @param {Function} fn
 * @param {number=} startIndex index to start at. Defaults to tabs.length - 1.
 * @returns {MdNavItemController}
 * @private
 */
MdNavBarController.prototype._findTabReverse = function(fn, startIndex) {
  var tabs = this._getTabs();
  if (startIndex === undefined || startIndex === null) {
    startIndex = tabs.length - 1;
  }
  for (var i = startIndex; i >= 0 ; i--) {
    if (fn(tabs[i])) {
      return tabs[i];
    }
  }
  return null;
};

/**
 * Direct focus to the selected tab when focus enters the nav bar.
 */
MdNavBarController.prototype.onFocus = function() {
  var tab = this._getSelectedTab();
  if (tab) {
    tab.setFocused(true);
  }
};

/**
 * Move focus from oldTab to newTab.
 * @param {!MdNavItemController} oldTab
 * @param {!MdNavItemController} newTab
 * @private
 */
MdNavBarController.prototype._moveFocus = function(oldTab, newTab) {
  oldTab.setFocused(false);
  newTab.setFocused(true);
};

/**
 * Focus the first tab.
 * @private
 */
MdNavBarController.prototype._focusFirstTab = function() {
  var tabs = this._getTabs();
  if (!tabs) return;
  var tabToFocus = this._findTab(function(tab) {
    return tab._isEnabled();
  });
  if (tabToFocus) {
    this._moveFocus(this.getFocusedTab(), tabToFocus);
  }
};

/**
 * Focus the last tab.
 * @private
 */
MdNavBarController.prototype._focusLastTab = function() {
  var tabs = this._getTabs();
  if (!tabs) return;
  var tabToFocus = this._findTabReverse(function(tab) {
    return tab._isEnabled();
  });
  if (tabToFocus) {
    this._moveFocus(this.getFocusedTab(), tabToFocus);
  }
};

/**
 * Focus the next non-disabled tab.
 * @param {number} focusedTabIndex the index of the currently focused tab
 * @private
 */
MdNavBarController.prototype._focusNextTab = function(focusedTabIndex) {
  var tabs = this._getTabs();
  if (!tabs) return;
  var tabToFocus = this._findTab(function(tab) {
    return tab._isEnabled();
  }, focusedTabIndex + 1);
  if (tabToFocus) {
    this._moveFocus(this.getFocusedTab(), tabToFocus);
  } else {
    this._focusFirstTab();
  }
};

/**
 * Focus the previous non-disabled tab.
 * @param {number} focusedTabIndex the index of the currently focused tab
 * @private
 */
MdNavBarController.prototype._focusPreviousTab = function(focusedTabIndex) {
  var tabs = this._getTabs();
  if (!tabs) return;
  var tabToFocus = this._findTabReverse(function(tab) {
    return tab._isEnabled();
  }, focusedTabIndex - 1);
  if (tabToFocus) {
    this._moveFocus(this.getFocusedTab(), tabToFocus);
  } else {
    this._focusLastTab();
  }
};

/**
 * Responds to keydown events.
 * Calls to preventDefault() stop the page from scrolling when changing focus in the nav-bar.
 * @param {!KeyboardEvent} e
 */
MdNavBarController.prototype.onKeydown = function(e) {
  var keyCodes = this._$mdConstant.KEY_CODE;
  var tabs = this._getTabs();
  var focusedTab = this.getFocusedTab();
  if (!focusedTab || !tabs) return;

  var focusedTabIndex = tabs.indexOf(focusedTab);

  // use arrow keys to navigate between tabs
  switch (e.keyCode) {
    case keyCodes.UP_ARROW:
    case keyCodes.LEFT_ARROW:
      e.preventDefault();
      this._focusPreviousTab(focusedTabIndex);
      break;
    case keyCodes.DOWN_ARROW:
    case keyCodes.RIGHT_ARROW:
      e.preventDefault();
      this._focusNextTab(focusedTabIndex);
      break;
    case keyCodes.SPACE:
    case keyCodes.ENTER:
      // timeout to avoid a "digest already in progress" console error
      this._$timeout(function() {
        focusedTab.getButtonEl().click();
      });
      break;
    case keyCodes.HOME:
      e.preventDefault();
      this._focusFirstTab();
      break;
    case keyCodes.END:
      e.preventDefault();
      this._focusLastTab();
      break;
  }
};

/**
 * @ngdoc directive
 * @name mdNavItem
 * @module material.components.navBar
 *
 * @restrict E
 *
 * @description
 * `<md-nav-item>` describes a page navigation link within the `<md-nav-bar>` component.
 * It renders an `<md-button>` as the actual link.
 *
 * Exactly one of the `md-nav-click`, `md-nav-href`, or `md-nav-sref` attributes are required
 * to be specified.
 *
 * @param {string=} nav-item-aria-label Allows setting or overriding the label that is announced by
 *     a screen reader for the nav item's button. If this is not set, the nav item's transcluded
 *     content will be announced. Make sure to set this if the nav item's transcluded content does
 *     not include descriptive text, for example only an icon.
 * @param {expression=} md-nav-click Expression which will be evaluated when the
 *     link is clicked to change the page. Renders as an `ng-click`.
 * @param {string=} md-nav-href url to transition to when this link is clicked.
 *     Renders as an `ng-href`.
 * @param {string=} md-nav-sref UI-Router state to transition to when this link is
 *     clicked. Renders as a `ui-sref`.
 * @param {string} name The name of this link. Used by the nav bar to know
 *     which link is currently selected.
 * @param {!object=} sref-opts UI-Router options that are passed to the `$state.go()` function. See
 *     the <a ng-href="https://ui-router.github.io/docs/latest/interfaces/transition.transitionoptions.html"
 *     target="_blank">UI-Router documentation for details</a>.
 *
 * @usage
 * See <a ng-href="api/directive/mdNavBar">md-nav-bar</a> for usage.
 */
/**
 * @param $mdAria
 * @param $$rAF
 * @param $mdUtil
 * @param $window
 * @constructor
 * ngInject
 */
function MdNavItem($mdAria, $$rAF, $mdUtil, $window) {
  return {
    restrict: 'E',
    require: ['mdNavItem', '^mdNavBar'],
    controller: MdNavItemController,
    bindToController: true,
    controllerAs: 'ctrl',
    replace: true,
    transclude: true,
    template: function(tElement, tAttrs) {
      var hasNavClick = tAttrs.mdNavClick;
      var hasNavHref = tAttrs.mdNavHref;
      var hasNavSref = tAttrs.mdNavSref;
      var hasSrefOpts = tAttrs.srefOpts;
      var navigationAttribute;
      var navigationOptions;
      var buttonTemplate;

      // Cannot specify more than one nav attribute
      if ((hasNavClick ? 1 : 0) + (hasNavHref ? 1 : 0) + (hasNavSref ? 1 : 0) > 1) {
        throw Error(
          'Please do not specify more than one of the md-nav-click, md-nav-href, ' +
          'or md-nav-sref attributes per nav-item directive.'
        );
      }

      if (hasNavClick !== undefined && hasNavClick !== null) {
        navigationAttribute = 'ng-click="ctrl.mdNavClick()"';
      } else if (hasNavHref !== undefined && hasNavHref !== null) {
        navigationAttribute = 'ng-href="{{ctrl.mdNavHref}}"';
      } else if (hasNavSref !== undefined && hasNavSref !== null) {
        navigationAttribute = 'ui-sref="{{ctrl.mdNavSref}}"';
      } else {
        throw Error(
          'Please specify at least one of the md-nav-click, md-nav-href, or md-nav-sref ' +
          'attributes per nav-item directive.');
      }

      navigationOptions = hasSrefOpts ? 'ui-sref-opts="{{ctrl.srefOpts}}" ' : '';

      if (navigationAttribute) {
        buttonTemplate = '' +
          '<md-button class="_md-nav-button md-accent" ' +
            'ng-class="ctrl.getNgClassMap()" ' +
            'ng-blur="ctrl.setFocused(false)" ' +
            'ng-disabled="ctrl.disabled" ' +
            'tabindex="-1" ' +
            'role="tab" ' +
            'ng-attr-aria-label="{{ctrl.navItemAriaLabel ? ctrl.navItemAriaLabel : undefined}}" ' +
            'aria-selected="{{ctrl.isSelected()}}" ' +
            navigationOptions +
            navigationAttribute + '>' +
            '<span ng-transclude class="_md-nav-button-text"></span>' +
          '</md-button>';
      }

      return '' +
        '<li class="md-nav-item" ' +
          'role="presentation">' +
          (buttonTemplate || '') +
        '</li>';
    },
    scope: {
      'mdNavClick': '&?',
      'mdNavHref': '@?',
      'mdNavSref': '@?',
      'srefOpts': '=?',
      'name': '@',
      'navItemAriaLabel': '@?',
    },
    link: function(scope, element, attrs, controllers) {
      var disconnect;
      var mdNavItem;
      var mdNavBar;
      var navButton;

      // When accessing the element's contents synchronously, they
      // may not be defined yet because of transclusion. There is a higher
      // chance that it will be accessible if we wait one frame.
      $$rAF(function() {
        mdNavItem = controllers[0];
        mdNavBar = controllers[1];
        navButton = angular.element(element[0].querySelector('._md-nav-button'));

        if (!mdNavItem.name) {
          mdNavItem.name = angular.element(element[0]
              .querySelector('._md-nav-button-text')).text().trim();
        }

        navButton.on('keydown', function($event) {
          mdNavBar.onKeydown($event);
        });

        navButton.on('focus', function() {
          if (!mdNavBar.getFocusedTab()) {
            mdNavBar.onFocus();
          }
        });

        navButton.on('click', function() {
          // This triggers a watcher on mdNavBar.mdSelectedNavItem which calls
          // MdNavBarController._updateTabs() after a $timeout. That function calls
          // MdNavItemController.setSelected() for the old tab with false and the new tab with true.
          mdNavBar.mdSelectedNavItem = mdNavItem.name;
          scope.$apply();
        });

        // Get the disabled attribute value first, then setup observing of value changes
        mdNavItem.disabled = $mdUtil.parseAttributeBoolean(attrs['disabled'], false);
        if ('MutationObserver' in $window) {
          var config = {attributes: true, attributeFilter: ['disabled']};
          var targetNode = element[0];
          var mutationCallback = function(mutationList) {
            $mdUtil.nextTick(function() {
              mdNavItem.disabled = $mdUtil.parseAttributeBoolean(attrs[mutationList[0].attributeName], false);
            });
          };
          var observer = new MutationObserver(mutationCallback);
          observer.observe(targetNode, config);
          disconnect = observer.disconnect.bind(observer);
        } else {
          attrs.$observe('disabled', function (value) {
            mdNavItem.disabled = $mdUtil.parseAttributeBoolean(value, false);
          });
        }

        if (!mdNavItem.navItemAriaLabel) {
          $mdAria.expectWithText(navButton, 'aria-label');
        }
      });

      scope.$on('destroy', function() {
        navButton.off('keydown');
        navButton.off('focus');
        navButton.off('click');
        disconnect();
      });
    }
  };
}

/**
 * Controller for the nav-item component.
 * @param {!angular.JQLite} $element
 * @constructor
 * @final
 * ngInject
 */
function MdNavItemController($element) {

  /** @private @const {!angular.JQLite} */
  this._$element = $element;

  // Data-bound variables

  /** @const {?Function} */
  this.mdNavClick;

  /** @const {?string} */
  this.mdNavHref;

  /** @const {?string} */
  this.mdNavSref;
  /** @const {?Object} */
  this.srefOpts;
  /** @const {?string} */
  this.name;

  /** @type {string} */
  this.navItemAriaLabel;

  // State variables
  /** @private {boolean} */
  this._selected = false;

  /** @private {boolean} */
  this._focused = false;
}

/**
 * Returns a map of class names and values for use by ng-class.
 * @return {!Object<string,boolean>}
 */
MdNavItemController.prototype.getNgClassMap = function() {
  return {
    'md-active': this._selected,
    'md-primary': this._selected,
    'md-unselected': !this._selected,
    'md-focused': this._focused,
  };
};

/**
 * Get the name attribute of the tab.
 * @return {string}
 */
MdNavItemController.prototype.getName = function() {
  return this.name;
};

/**
 * Get the button element associated with the tab.
 * @return {!Element}
 */
MdNavItemController.prototype.getButtonEl = function() {
  return this._$element[0].querySelector('._md-nav-button');
};

/**
 * Set the selected state of the tab and updates the tabindex.
 * This function is called for the oldTab and newTab when selection changes.
 * @param {boolean} isSelected true to select the tab, false to deselect the tab
 */
MdNavItemController.prototype.setSelected = function(isSelected) {
  this._selected = isSelected;
  if (isSelected) {
    // https://www.w3.org/TR/wai-aria-practices/examples/tabs/tabs-2/tabs.html suggests that we call
    // removeAttribute('tabindex') here, but that causes our unit tests to fail due to
    // document.activeElement staying set to the body instead of the focused nav button.
    this.getButtonEl().setAttribute('tabindex', '0');
  } else {
    this.getButtonEl().setAttribute('tabindex', '-1');
  }
};

/**
 * @return {boolean}
 */
MdNavItemController.prototype.isSelected = function() {
  return this._selected;
};

/**
 * Set the focused state of the tab.
 * @param {boolean} isFocused
 */
MdNavItemController.prototype.setFocused = function(isFocused) {
  this._focused = isFocused;

  if (isFocused) {
    this.getButtonEl().focus();
  }
};

/**
 * @return {boolean} true if the tab has focus, false if not.
 */
MdNavItemController.prototype.hasFocus = function() {
  return this._focused;
};

/**
 * @return {boolean} true if the tab is enabled, false if disabled.
 * @private
 */
MdNavItemController.prototype._isEnabled = function() {
  return !this._$element.attr('disabled');
};

ngmaterial.components.navBar = angular.module("material.components.navBar");