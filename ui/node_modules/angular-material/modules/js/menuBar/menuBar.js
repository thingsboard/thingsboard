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
 * @name material.components.menuBar
 */

angular.module('material.components.menuBar', [
  'material.core',
  'material.components.icon',
  'material.components.menu'
]);


MenuBarController['$inject'] = ["$scope", "$rootScope", "$element", "$attrs", "$mdConstant", "$document", "$mdUtil", "$timeout"];
angular
  .module('material.components.menuBar')
  .controller('MenuBarController', MenuBarController);

var BOUND_MENU_METHODS = ['handleKeyDown', 'handleMenuHover', 'scheduleOpenHoveredMenu', 'cancelScheduledOpen'];

/**
 * ngInject
 */
function MenuBarController($scope, $rootScope, $element, $attrs, $mdConstant, $document, $mdUtil, $timeout) {
  this.$element = $element;
  this.$attrs = $attrs;
  this.$mdConstant = $mdConstant;
  this.$mdUtil = $mdUtil;
  this.$document = $document;
  this.$scope = $scope;
  this.$rootScope = $rootScope;
  this.$timeout = $timeout;

  var self = this;
  angular.forEach(BOUND_MENU_METHODS, function(methodName) {
    self[methodName] = angular.bind(self, self[methodName]);
  });
}

MenuBarController.prototype.init = function() {
  var $element = this.$element;
  var $mdUtil = this.$mdUtil;
  var $scope = this.$scope;

  var self = this;
  var deregisterFns = [];
  $element.on('keydown', this.handleKeyDown);
  this.parentToolbar = $mdUtil.getClosest($element, 'MD-TOOLBAR');

  deregisterFns.push(this.$rootScope.$on('$mdMenuOpen', function(event, el) {
    if (self.getMenus().indexOf(el[0]) != -1) {
      $element[0].classList.add('md-open');
      el[0].classList.add('md-open');
      self.currentlyOpenMenu = el.controller('mdMenu');
      self.currentlyOpenMenu.registerContainerProxy(self.handleKeyDown);
      self.enableOpenOnHover();
    }
  }));

  deregisterFns.push(this.$rootScope.$on('$mdMenuClose', function(event, el, opts) {
    var rootMenus = self.getMenus();
    if (rootMenus.indexOf(el[0]) != -1) {
      $element[0].classList.remove('md-open');
      el[0].classList.remove('md-open');
    }

    if ($element[0].contains(el[0])) {
      var parentMenu = el[0];
      while (parentMenu && rootMenus.indexOf(parentMenu) == -1) {
        parentMenu = $mdUtil.getClosest(parentMenu, 'MD-MENU', true);
      }
      if (parentMenu) {
        if (!opts.skipFocus) parentMenu.querySelector('button:not([disabled])').focus();
        self.currentlyOpenMenu = undefined;
        self.disableOpenOnHover();
        self.setKeyboardMode(true);
      }
    }
  }));

  $scope.$on('$destroy', function() {
    self.disableOpenOnHover();
    while (deregisterFns.length) {
      deregisterFns.shift()();
    }
  });


  this.setKeyboardMode(true);
};

MenuBarController.prototype.setKeyboardMode = function(enabled) {
  if (enabled) this.$element[0].classList.add('md-keyboard-mode');
  else this.$element[0].classList.remove('md-keyboard-mode');
};

MenuBarController.prototype.enableOpenOnHover = function() {
  if (this.openOnHoverEnabled) return;

  var self = this;

  self.openOnHoverEnabled = true;

  if (self.parentToolbar) {
    self.parentToolbar.classList.add('md-has-open-menu');

    // Needs to be on the next tick so it doesn't close immediately.
    self.$mdUtil.nextTick(function() {
      angular.element(self.parentToolbar).on('click', self.handleParentClick);
    }, false);
  }

  angular
    .element(self.getMenus())
    .on('mouseenter', self.handleMenuHover);
};

MenuBarController.prototype.handleMenuHover = function(e) {
  this.setKeyboardMode(false);
  if (this.openOnHoverEnabled) {
    this.scheduleOpenHoveredMenu(e);
  }
};

MenuBarController.prototype.disableOpenOnHover = function() {
  if (!this.openOnHoverEnabled) return;

  this.openOnHoverEnabled = false;

  if (this.parentToolbar) {
    this.parentToolbar.classList.remove('md-has-open-menu');
    angular.element(this.parentToolbar).off('click', this.handleParentClick);
  }

  angular
    .element(this.getMenus())
    .off('mouseenter', this.handleMenuHover);
};

MenuBarController.prototype.scheduleOpenHoveredMenu = function(e) {
  var menuEl = angular.element(e.currentTarget);
  var menuCtrl = menuEl.controller('mdMenu');
  this.setKeyboardMode(false);
  this.scheduleOpenMenu(menuCtrl);
};

MenuBarController.prototype.scheduleOpenMenu = function(menuCtrl) {
  var self = this;
  var $timeout = this.$timeout;
  if (menuCtrl != self.currentlyOpenMenu) {
    $timeout.cancel(self.pendingMenuOpen);
    self.pendingMenuOpen = $timeout(function() {
      self.pendingMenuOpen = undefined;
      if (self.currentlyOpenMenu) {
        self.currentlyOpenMenu.close(true, { closeAll: true });
      }
      menuCtrl.open();
    }, 200, false);
  }
};

MenuBarController.prototype.handleKeyDown = function(e) {
  var keyCodes = this.$mdConstant.KEY_CODE;
  var currentMenu = this.currentlyOpenMenu;
  var wasOpen = currentMenu && currentMenu.isOpen;
  this.setKeyboardMode(true);
  var handled, newMenu, newMenuCtrl;
  switch (e.keyCode) {
    case keyCodes.DOWN_ARROW:
      if (currentMenu) {
        currentMenu.focusMenuContainer();
      } else {
        this.openFocusedMenu();
      }
      handled = true;
      break;
    case keyCodes.UP_ARROW:
      currentMenu && currentMenu.close();
      handled = true;
      break;
    case keyCodes.LEFT_ARROW:
      newMenu = this.focusMenu(-1);
      if (wasOpen) {
        newMenuCtrl = angular.element(newMenu).controller('mdMenu');
        this.scheduleOpenMenu(newMenuCtrl);
      }
      handled = true;
      break;
    case keyCodes.RIGHT_ARROW:
      newMenu = this.focusMenu(+1);
      if (wasOpen) {
        newMenuCtrl = angular.element(newMenu).controller('mdMenu');
        this.scheduleOpenMenu(newMenuCtrl);
      }
      handled = true;
      break;
  }
  if (handled) {
    e && e.preventDefault && e.preventDefault();
    e && e.stopImmediatePropagation && e.stopImmediatePropagation();
  }
};

MenuBarController.prototype.focusMenu = function(direction) {
  var menus = this.getMenus();
  var focusedIndex = this.getFocusedMenuIndex();

  if (focusedIndex == -1) { focusedIndex = this.getOpenMenuIndex(); }

  var changed = false;

  if (focusedIndex == -1) { focusedIndex = 0; changed = true; }
  else if (
    direction < 0 && focusedIndex > 0 ||
    direction > 0 && focusedIndex < menus.length - direction
  ) {
    focusedIndex += direction;
    changed = true;
  }
  if (changed) {
    menus[focusedIndex].querySelector('button').focus();
    return menus[focusedIndex];
  }
};

MenuBarController.prototype.openFocusedMenu = function() {
  var menu = this.getFocusedMenu();
  menu && angular.element(menu).controller('mdMenu').open();
};

MenuBarController.prototype.getMenus = function() {
  var $element = this.$element;
  return this.$mdUtil.nodesToArray($element[0].children)
    .filter(function(el) { return el.nodeName == 'MD-MENU'; });
};

MenuBarController.prototype.getFocusedMenu = function() {
  return this.getMenus()[this.getFocusedMenuIndex()];
};

MenuBarController.prototype.getFocusedMenuIndex = function() {
  var $mdUtil = this.$mdUtil;
  var focusedEl = $mdUtil.getClosest(
    this.$document[0].activeElement,
    'MD-MENU'
  );
  if (!focusedEl) return -1;

  var focusedIndex = this.getMenus().indexOf(focusedEl);
  return focusedIndex;
};

MenuBarController.prototype.getOpenMenuIndex = function() {
  var menus = this.getMenus();
  for (var i = 0; i < menus.length; ++i) {
    if (menus[i].classList.contains('md-open')) return i;
  }
  return -1;
};

MenuBarController.prototype.handleParentClick = function(event) {
  var openMenu = this.querySelector('md-menu.md-open');

  if (openMenu && !openMenu.contains(event.target)) {
    angular.element(openMenu).controller('mdMenu').close(true, {
      closeAll: true
    });
  }
};

/**
 * @ngdoc directive
 * @name mdMenuBar
 * @module material.components.menuBar
 * @restrict E
 * @description
 *
 * Menu bars are containers that hold multiple menus. They change the behavior and appearance
 * of the `md-menu` directive to behave similar to an operating system provided menu.
 *
 * @usage
 * <hljs lang="html">
 * <md-menu-bar>
 *   <md-menu>
 *     <button ng-click="$mdMenu.open()">
 *       File
 *     </button>
 *     <md-menu-content>
 *       <md-menu-item>
 *         <md-button ng-click="ctrl.sampleAction('share', $event)">
 *           Share...
 *         </md-button>
 *       </md-menu-item>
 *       <md-menu-divider></md-menu-divider>
 *       <md-menu-item>
 *       <md-menu-item>
 *         <md-menu>
 *           <md-button ng-click="$mdMenu.open()">New</md-button>
 *           <md-menu-content>
 *             <md-menu-item><md-button ng-click="ctrl.sampleAction('New Document', $event)">Document</md-button></md-menu-item>
 *             <md-menu-item><md-button ng-click="ctrl.sampleAction('New Spreadsheet', $event)">Spreadsheet</md-button></md-menu-item>
 *             <md-menu-item><md-button ng-click="ctrl.sampleAction('New Presentation', $event)">Presentation</md-button></md-menu-item>
 *             <md-menu-item><md-button ng-click="ctrl.sampleAction('New Form', $event)">Form</md-button></md-menu-item>
 *             <md-menu-item><md-button ng-click="ctrl.sampleAction('New Drawing', $event)">Drawing</md-button></md-menu-item>
 *           </md-menu-content>
 *         </md-menu>
 *       </md-menu-item>
 *     </md-menu-content>
 *   </md-menu>
 * </md-menu-bar>
 * </hljs>
 *
 * ## Menu Bar Controls
 *
 * You may place `md-menu-item`s that function as controls within menu bars.
 * There are two modes that are exposed via the `type` attribute of the `md-menu-item`.
 * `type="checkbox"` will function as a boolean control for the `ng-model` attribute of the
 * `md-menu-item`. `type="radio"` will function like a radio button, setting the `ngModel`
 * to the `string` value of the `value` attribute. If you need non-string values, you can use
 * `ng-value` to provide an expression (this is similar to how angular's native `input[type=radio]`
 * works.
 *
 * If you want either to disable closing the opened menu when clicked, you can add the
 * `md-prevent-menu-close` attribute to the `md-menu-item`. The attribute will be forwarded to the
 * `button` element that is generated.
 *
 * <hljs lang="html">
 * <md-menu-bar>
 *  <md-menu>
 *    <button ng-click="$mdMenu.open()">
 *      Sample Menu
 *    </button>
 *    <md-menu-content>
 *      <md-menu-item type="checkbox" ng-model="settings.allowChanges" md-prevent-menu-close>
 *        Allow changes
 *      </md-menu-item>
 *      <md-menu-divider></md-menu-divider>
 *      <md-menu-item type="radio" ng-model="settings.mode" ng-value="1">Mode 1</md-menu-item>
 *      <md-menu-item type="radio" ng-model="settings.mode" ng-value="2">Mode 2</md-menu-item>
 *      <md-menu-item type="radio" ng-model="settings.mode" ng-value="3">Mode 3</md-menu-item>
 *    </md-menu-content>
 *  </md-menu>
 * </md-menu-bar>
 * </hljs>
 *
 *
 * ### Nesting Menus
 *
 * Menus may be nested within menu bars. This is commonly called cascading menus.
 * To nest a menu place the nested menu inside the content of the `md-menu-item`.
 * <hljs lang="html">
 * <md-menu-item>
 *   <md-menu>
 *     <button ng-click="$mdMenu.open()">New</md-button>
 *     <md-menu-content>
 *       <md-menu-item><md-button ng-click="ctrl.sampleAction('New Document', $event)">Document</md-button></md-menu-item>
 *       <md-menu-item><md-button ng-click="ctrl.sampleAction('New Spreadsheet', $event)">Spreadsheet</md-button></md-menu-item>
 *       <md-menu-item><md-button ng-click="ctrl.sampleAction('New Presentation', $event)">Presentation</md-button></md-menu-item>
 *       <md-menu-item><md-button ng-click="ctrl.sampleAction('New Form', $event)">Form</md-button></md-menu-item>
 *       <md-menu-item><md-button ng-click="ctrl.sampleAction('New Drawing', $event)">Drawing</md-button></md-menu-item>
 *     </md-menu-content>
 *   </md-menu>
 * </md-menu-item>
 * </hljs>
 *
 */

MenuBarDirective['$inject'] = ["$mdUtil", "$mdTheming"];
angular
  .module('material.components.menuBar')
  .directive('mdMenuBar', MenuBarDirective);

/* ngInject */
function MenuBarDirective($mdUtil, $mdTheming) {
  return {
    restrict: 'E',
    require: 'mdMenuBar',
    controller: 'MenuBarController',

    compile: function compile(templateEl, templateAttrs) {
      if (!templateAttrs.ariaRole) {
        templateEl[0].setAttribute('role', 'menubar');
      }
      angular.forEach(templateEl[0].children, function(menuEl) {
        if (menuEl.nodeName == 'MD-MENU') {
          if (!menuEl.hasAttribute('md-position-mode')) {
            menuEl.setAttribute('md-position-mode', 'left bottom');

            // Since we're in the compile function and actual `md-buttons` are not compiled yet,
            // we need to query for possible `md-buttons` as well.
            menuEl.querySelector('button, a, md-button').setAttribute('role', 'menuitem');
          }
          var contentEls = $mdUtil.nodesToArray(menuEl.querySelectorAll('md-menu-content'));
          angular.forEach(contentEls, function(contentEl) {
            contentEl.classList.add('md-menu-bar-menu');
            contentEl.classList.add('md-dense');
            if (!contentEl.hasAttribute('width')) {
              contentEl.setAttribute('width', 5);
            }
          });
        }
      });

      // Mark the child menu items that they're inside a menu bar. This is necessary,
      // because mnMenuItem has special behaviour during compilation, depending on
      // whether it is inside a mdMenuBar. We can usually figure this out via the DOM,
      // however if a directive that uses documentFragment is applied to the child (e.g. ngRepeat),
      // the element won't have a parent and won't compile properly.
      templateEl.find('md-menu-item').addClass('md-in-menu-bar');

      return function postLink(scope, el, attr, ctrl) {
        el.addClass('_md');     // private md component indicator for styling
        $mdTheming(scope, el);
        ctrl.init();
      };
    }
  };

}


angular
  .module('material.components.menuBar')
  .directive('mdMenuDivider', MenuDividerDirective);


function MenuDividerDirective() {
  return {
    restrict: 'E',
    compile: function(templateEl, templateAttrs) {
      if (!templateAttrs.role) {
        templateEl[0].setAttribute('role', 'separator');
      }
    }
  };
}


MenuItemController['$inject'] = ["$scope", "$element", "$attrs"];
angular
  .module('material.components.menuBar')
  .controller('MenuItemController', MenuItemController);


/**
 * ngInject
 */
function MenuItemController($scope, $element, $attrs) {
  this.$element = $element;
  this.$attrs = $attrs;
  this.$scope = $scope;
}

MenuItemController.prototype.init = function(ngModel) {
  var $element = this.$element;
  var $attrs = this.$attrs;

  this.ngModel = ngModel;
  if ($attrs.type == 'checkbox' || $attrs.type == 'radio') {
    this.mode  = $attrs.type;
    this.iconEl = $element[0].children[0];
    this.buttonEl = $element[0].children[1];
    if (ngModel) {
      // Clear ngAria set attributes
      this.initClickListeners();
    }
  }
};

// ngAria auto sets attributes on a menu-item with a ngModel.
// We don't want this because our content (buttons) get the focus
// and set their own aria attributes appropritately. Having both
// breaks NVDA / JAWS. This undeoes ngAria's attrs.
MenuItemController.prototype.clearNgAria = function() {
  var el = this.$element[0];
  var clearAttrs = ['role', 'tabindex', 'aria-invalid', 'aria-checked'];
  angular.forEach(clearAttrs, function(attr) {
    el.removeAttribute(attr);
  });
};

MenuItemController.prototype.initClickListeners = function() {
  var self = this;
  var ngModel = this.ngModel;
  var $scope = this.$scope;
  var $attrs = this.$attrs;
  var $element = this.$element;
  var mode = this.mode;

  this.handleClick = angular.bind(this, this.handleClick);

  var icon = this.iconEl;
  var button = angular.element(this.buttonEl);
  var handleClick = this.handleClick;

  $attrs.$observe('disabled', setDisabled);
  setDisabled($attrs.disabled);

  ngModel.$render = function render() {
    self.clearNgAria();
    if (isSelected()) {
      icon.style.display = '';
      button.attr('aria-checked', 'true');
    } else {
      icon.style.display = 'none';
      button.attr('aria-checked', 'false');
    }
  };

  $scope.$$postDigest(ngModel.$render);

  function isSelected() {
    if (mode == 'radio') {
      var val = $attrs.ngValue ? $scope.$eval($attrs.ngValue) : $attrs.value;
      return ngModel.$modelValue == val;
    } else {
      return ngModel.$modelValue;
    }
  }

  function setDisabled(disabled) {
    if (disabled) {
      button.off('click', handleClick);
    } else {
      button.on('click', handleClick);
    }
  }
};

MenuItemController.prototype.handleClick = function(e) {
  var mode = this.mode;
  var ngModel = this.ngModel;
  var $attrs = this.$attrs;
  var newVal;
  if (mode == 'checkbox') {
    newVal = !ngModel.$modelValue;
  } else if (mode == 'radio') {
    newVal = $attrs.ngValue ? this.$scope.$eval($attrs.ngValue) : $attrs.value;
  }
  ngModel.$setViewValue(newVal);
  ngModel.$render();
};


MenuItemDirective['$inject'] = ["$mdUtil", "$mdConstant", "$$mdSvgRegistry"];
angular
  .module('material.components.menuBar')
  .directive('mdMenuItem', MenuItemDirective);

 /* ngInject */
function MenuItemDirective($mdUtil, $mdConstant, $$mdSvgRegistry) {
  return {
    controller: 'MenuItemController',
    require: ['mdMenuItem', '?ngModel'],
    priority: $mdConstant.BEFORE_NG_ARIA,
    compile: function(templateEl, templateAttrs) {
      var type = templateAttrs.type;
      var inMenuBarClass = 'md-in-menu-bar';

      // Note: This allows us to show the `check` icon for the md-menu-bar items.
      // The `md-in-menu-bar` class is set by the mdMenuBar directive.
      if ((type === 'checkbox' || type === 'radio') && templateEl.hasClass(inMenuBarClass)) {
        var text = templateEl[0].textContent;
        var buttonEl = angular.element('<md-button type="button"></md-button>');
        var iconTemplate = '<md-icon md-svg-src="' + $$mdSvgRegistry.mdChecked + '"></md-icon>';

        buttonEl.html(text);
        buttonEl.attr('tabindex', '0');

        if (angular.isDefined(templateAttrs.mdPreventMenuClose)) {
          buttonEl.attr('md-prevent-menu-close', templateAttrs.mdPreventMenuClose);
        }

        templateEl.html('');
        templateEl.append(angular.element(iconTemplate));
        templateEl.append(buttonEl);
        templateEl.addClass('md-indent').removeClass(inMenuBarClass);

        setDefault('role', type === 'checkbox' ? 'menuitemcheckbox' : 'menuitemradio', buttonEl);
        moveAttrToButton('ng-disabled');

      } else {
        setDefault('role', 'menuitem', templateEl[0].querySelector('md-button, button, a'));
      }


      return function(scope, el, attrs, ctrls) {
        var ctrl = ctrls[0];
        var ngModel = ctrls[1];
        ctrl.init(ngModel);
      };

      function setDefault(attr, val, el) {
        el = el || templateEl;
        if (el instanceof angular.element) {
          el = el[0];
        }
        if (!el.hasAttribute(attr)) {
          el.setAttribute(attr, val);
        }
      }

      function moveAttrToButton(attribute) {
        var attributes = $mdUtil.prefixer(attribute);

        angular.forEach(attributes, function(attr) {
          if (templateEl[0].hasAttribute(attr)) {
            var val = templateEl[0].getAttribute(attr);
            buttonEl[0].setAttribute(attr, val);
            templateEl[0].removeAttribute(attr);
          }
        });
      }
    }
  };
}

})(window, window.angular);