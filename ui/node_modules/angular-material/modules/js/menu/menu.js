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
 * @name material.components.menu
 */

angular.module('material.components.menu', [
  'material.core',
  'material.components.backdrop'
]);



MenuController['$inject'] = ["$mdMenu", "$attrs", "$element", "$scope", "$mdUtil", "$timeout", "$rootScope", "$q", "$log"];
angular
    .module('material.components.menu')
    .controller('mdMenuCtrl', MenuController);

/**
 * ngInject
 */
function MenuController($mdMenu, $attrs, $element, $scope, $mdUtil, $timeout, $rootScope, $q, $log) {

  var prefixer = $mdUtil.prefixer();
  var menuContainer;
  var self = this;
  var triggerElement;

  this.nestLevel = parseInt($attrs.mdNestLevel, 10) || 0;

  /**
   * Called by our linking fn to provide access to the menu-content
   * element removed during link
   */
  this.init = function init(setMenuContainer, opts) {
    opts = opts || {};
    menuContainer = setMenuContainer;

    // Default element for ARIA attributes has the ngClick or ngMouseenter expression
    triggerElement = $element[0].querySelector(prefixer.buildSelector(['ng-click', 'ng-mouseenter']));
    triggerElement.setAttribute('aria-expanded', 'false');

    this.isInMenuBar = opts.isInMenuBar;
    this.nestedMenus = $mdUtil.nodesToArray(menuContainer[0].querySelectorAll('.md-nested-menu'));

    menuContainer.on('$mdInterimElementRemove', function() {
      self.isOpen = false;
      $mdUtil.nextTick(function(){ self.onIsOpenChanged(self.isOpen);});
    });
    $mdUtil.nextTick(function(){ self.onIsOpenChanged(self.isOpen);});

    var menuContainerId = 'menu_container_' + $mdUtil.nextUid();
    menuContainer.attr('id', menuContainerId);
    angular.element(triggerElement).attr({
      'aria-owns': menuContainerId,
      'aria-haspopup': 'true'
    });

    $scope.$on('$destroy', angular.bind(this, function() {
      this.disableHoverListener();
      $mdMenu.destroy();
    }));

    menuContainer.on('$destroy', function() {
      $mdMenu.destroy();
    });
  };

  var openMenuTimeout, menuItems, deregisterScopeListeners = [];
  this.enableHoverListener = function() {
    deregisterScopeListeners.push($rootScope.$on('$mdMenuOpen', function(event, el) {
      if (menuContainer[0].contains(el[0])) {
        self.currentlyOpenMenu = el.controller('mdMenu');
        self.isAlreadyOpening = false;
        self.currentlyOpenMenu.registerContainerProxy(self.triggerContainerProxy.bind(self));
      }
    }));
    deregisterScopeListeners.push($rootScope.$on('$mdMenuClose', function(event, el) {
      if (menuContainer[0].contains(el[0])) {
        self.currentlyOpenMenu = undefined;
      }
    }));
    menuItems = angular.element($mdUtil.nodesToArray(menuContainer[0].children[0].children));
    menuItems.on('mouseenter', self.handleMenuItemHover);
    menuItems.on('mouseleave', self.handleMenuItemMouseLeave);
  };

  this.disableHoverListener = function() {
    while (deregisterScopeListeners.length) {
      deregisterScopeListeners.shift()();
    }
    menuItems && menuItems.off('mouseenter', self.handleMenuItemHover);
    menuItems && menuItems.off('mouseleave', self.handleMenuItemMouseLeave);
  };

  this.handleMenuItemHover = function(event) {
    if (self.isAlreadyOpening) return;
    var nestedMenu = (
      event.target.querySelector('md-menu')
        || $mdUtil.getClosest(event.target, 'MD-MENU')
    );
    openMenuTimeout = $timeout(function() {
      if (nestedMenu) {
        nestedMenu = angular.element(nestedMenu).controller('mdMenu');
      }

      if (self.currentlyOpenMenu && self.currentlyOpenMenu != nestedMenu) {
        var closeTo = self.nestLevel + 1;
        self.currentlyOpenMenu.close(true, { closeTo: closeTo });
        self.isAlreadyOpening = !!nestedMenu;
        nestedMenu && nestedMenu.open();
      } else if (nestedMenu && !nestedMenu.isOpen && nestedMenu.open) {
        self.isAlreadyOpening = !!nestedMenu;
        nestedMenu && nestedMenu.open();
      }
    }, nestedMenu ? 100 : 250);
    var focusableTarget = event.currentTarget.querySelector('.md-button:not([disabled])');
    focusableTarget && focusableTarget.focus();
  };

  this.handleMenuItemMouseLeave = function() {
    if (openMenuTimeout) {
      $timeout.cancel(openMenuTimeout);
      openMenuTimeout = undefined;
    }
  };


  /**
   * Uses the $mdMenu interim element service to open the menu contents
   */
  this.open = function openMenu(ev) {
    ev && ev.stopPropagation();
    ev && ev.preventDefault();
    if (self.isOpen) return;
    self.enableHoverListener();
    self.isOpen = true;
    $mdUtil.nextTick(function(){ self.onIsOpenChanged(self.isOpen);});
    triggerElement = triggerElement || (ev ? ev.target : $element[0]);
    triggerElement.setAttribute('aria-expanded', 'true');
    $scope.$emit('$mdMenuOpen', $element);
    $mdMenu.show({
      scope: $scope,
      mdMenuCtrl: self,
      nestLevel: self.nestLevel,
      element: menuContainer,
      target: triggerElement,
      preserveElement: true,
      parent: 'body'
    }).finally(function() {
      triggerElement.setAttribute('aria-expanded', 'false');
      self.disableHoverListener();
    });
  };

  this.onIsOpenChanged = function(isOpen) {
    if (isOpen) {
      menuContainer.attr('aria-hidden', 'false');
      $element[0].classList.add('md-open');
      angular.forEach(self.nestedMenus, function(el) {
        el.classList.remove('md-open');
      });
    } else {
      menuContainer.attr('aria-hidden', 'true');
      $element[0].classList.remove('md-open');
    }
    $scope.$mdMenuIsOpen = self.isOpen;
  };

  this.focusMenuContainer = function focusMenuContainer() {
    var focusTarget = menuContainer[0]
      .querySelector(prefixer.buildSelector(['md-menu-focus-target', 'md-autofocus']));

    if (!focusTarget) focusTarget = menuContainer[0].querySelector('.md-button:not([disabled])');
    focusTarget.focus();
  };

  this.registerContainerProxy = function registerContainerProxy(handler) {
    this.containerProxy = handler;
  };

  this.triggerContainerProxy = function triggerContainerProxy(ev) {
    this.containerProxy && this.containerProxy(ev);
  };

  this.destroy = function() {
    return self.isOpen ? $mdMenu.destroy() : $q.when(false);
  };

  // Use the $mdMenu interim element service to close the menu contents
  this.close = function closeMenu(skipFocus, closeOpts) {
    if (!self.isOpen) return;
    self.isOpen = false;
    $mdUtil.nextTick(function(){ self.onIsOpenChanged(self.isOpen);});

    var eventDetails = angular.extend({}, closeOpts, { skipFocus: skipFocus });
    $scope.$emit('$mdMenuClose', $element, eventDetails);
    $mdMenu.hide(null, closeOpts);

    if (!skipFocus) {
      var el = self.restoreFocusTo || $element.find('button')[0];
      if (el instanceof angular.element) el = el[0];
      if (el) el.focus();
    }
  };

  /**
   * Build a nice object out of our string attribute which specifies the
   * target mode for left and top positioning
   */
  this.positionMode = function positionMode() {
    var attachment = ($attrs.mdPositionMode || 'target').split(' ');

    // If attachment is a single item, duplicate it for our second value.
    // ie. 'target' -> 'target target'
    if (attachment.length === 1) {
      attachment.push(attachment[0]);
    }

    return {
      left: attachment[0],
      top: attachment[1]
    };
  };

  /**
   * Build a nice object out of our string attribute which specifies
   * the offset of top and left in pixels.
   */
  this.offsets = function offsets() {
    var position = ($attrs.mdOffset || '0 0').split(' ').map(parseFloat);
    if (position.length === 2) {
      return {
        left: position[0],
        top: position[1]
      };
    } else if (position.length === 1) {
      return {
        top: position[0],
        left: position[0]
      };
    } else {
      throw Error('Invalid offsets specified. Please follow format <x, y> or <n>');
    }
  };

  // Functionality that is exposed in the view.
  $scope.$mdMenu = {
    open: this.open,
    close: this.close
  };

  // Deprecated APIs
  $scope.$mdOpenMenu = angular.bind(this, function() {
    $log.warn('mdMenu: The $mdOpenMenu method is deprecated. Please use `$mdMenu.open`.');
    return this.open.apply(this, arguments);
  });
}

/**
 * @ngdoc directive
 * @name mdMenu
 * @module material.components.menu
 * @restrict E
 * @description
 *
 * Menus are elements that open when clicked. They are useful for displaying
 * additional options within the context of an action.
 *
 * Every `md-menu` must specify exactly two child elements. The first element is what is
 * left in the DOM and is used to open the menu. This element is called the trigger element.
 * The trigger element's scope has access to `$mdMenu.open($event)`
 * which it may call to open the menu. By passing $event as argument, the
 * corresponding event is stopped from propagating up the DOM-tree. Similarly, `$mdMenu.close()`
 * can be used to close the menu.
 *
 * The second element is the `md-menu-content` element which represents the
 * contents of the menu when it is open. Typically this will contain `md-menu-item`s,
 * but you can do custom content as well.
 *
 * <hljs lang="html">
 * <md-menu>
 *  <!-- Trigger element is a md-button with an icon -->
 *  <md-button ng-click="$mdMenu.open($event)" class="md-icon-button" aria-label="Open sample menu">
 *    <md-icon md-svg-icon="call:phone"></md-icon>
 *  </md-button>
 *  <md-menu-content>
 *    <md-menu-item><md-button ng-click="doSomething()">Do Something</md-button></md-menu-item>
 *  </md-menu-content>
 * </md-menu>
 * </hljs>

 * ## Sizing Menus
 *
 * The width of the menu when it is open may be specified by specifying a `width`
 * attribute on the `md-menu-content` element.
 * See the [Material Design Spec](https://material.io/archive/guidelines/components/menus.html#menus-simple-menus)
 * for more information.
 *
 * ## Menu Density
 *
 * You can use dense menus by adding the `md-dense` class to the `md-menu-content` element.
 * This reduces the height of menu items, their top and bottom padding, and default font size.
 * Without the `md-dense` class, we use the "mobile" height of `48px`. With the `md-dense` class,
 * we use the "desktop" height of `32px`. We do not support the "dense desktop" option in the spec,
 * which uses a height of `24px`, at this time.
 * See the [Menu Specs](https://material.io/archive/guidelines/components/menus.html#menus-specs)
 * section of the Material Design Spec for more information.
 *
 * ## Aligning Menus
 *
 * When a menu opens, it is important that the content aligns with the trigger element.
 * Failure to align menus can result in jarring experiences for users as content
 * suddenly shifts. To help with this, `md-menu` provides several APIs to help
 * with alignment.
 *
 * ### Target Mode
 *
 * By default, `md-menu` will attempt to align the `md-menu-content` by aligning
 * designated child elements in both the trigger and the menu content.
 *
 * To specify the alignment element in the `trigger` you can use the `md-menu-origin`
 * attribute on a child element. If no `md-menu-origin` is specified, the `md-menu`
 * will be used as the origin element.
 *
 * Similarly, the `md-menu-content` may specify a `md-menu-align-target` for a
 * `md-menu-item` to specify the node that it should try and align with.
 *
 * In this example code, we specify an icon to be our origin element, and an
 * icon in our menu content to be our alignment target. This ensures that both
 * icons are aligned when the menu opens.
 *
 * <hljs lang="html">
 * <md-menu>
 *  <md-button ng-click="$mdMenu.open($event)" class="md-icon-button" aria-label="Open some menu">
 *    <md-icon md-menu-origin md-svg-icon="call:phone"></md-icon>
 *  </md-button>
 *  <md-menu-content>
 *    <md-menu-item>
 *      <md-button ng-click="doSomething()" aria-label="Do something">
 *        <md-icon md-menu-align-target md-svg-icon="call:phone"></md-icon>
 *        Do Something
 *      </md-button>
 *    </md-menu-item>
 *  </md-menu-content>
 * </md-menu>
 * </hljs>
 *
 * ### Position Mode
 *
 * We can specify the origin of the menu by using the `md-position-mode` attribute.
 * This attribute allows specifying the positioning by the `x` and `y` axes.
 *
 * The default mode is `target target`. This mode uses the left and top edges of the origin element
 * to position the menu in LTR layouts. The `x` axis modes will adjust when in RTL layouts.
 *
 * Sometimes you want to specify alignment from the right side of a origin element. For example,
 * if we have a menu on the right side a toolbar, we may want to right align our menu content.
 * We can use `target-right target` to specify a right-oriented alignment target.
 * There is a working example of this in the Menu Position Modes demo.
 *
 * #### Horizontal Positioning Options
 * - `target`
 * - `target-left`
 * - `target-right`
 * - `cascade`
 * - `right`
 * - `left`
 *
 * #### Vertical Positioning Options
 * - `target`
 * - `cascade`
 * - `bottom`
 *
 * ### Menu Offsets
 *
 * It is sometimes unavoidable to need to have a deeper level of control for
 * the positioning of a menu to ensure perfect alignment. `md-menu` provides
 * the `md-offset` attribute to allow pixel-level specificity when adjusting
 * menu positioning.
 *
 * This offset is provided in the format of `x y` or `n` where `n` will be used
 * in both the `x` and `y` axis.
 * For example, to move a menu by `2px` down from the top, we can use:
 *
 * <hljs lang="html">
 * <md-menu md-offset="0 2">
 *   <!-- menu-content -->
 * </md-menu>
 * </hljs>
 *
 * Specifying `md-offset="2 2"` would shift the menu two pixels down and two pixels to the right.
 *
 * ### Auto Focus
 * By default, when a menu opens, `md-menu` focuses the first button in the menu content.
 *
 * Sometimes you would like to focus another menu item instead of the first.<br/>
 * This can be done by applying the `md-autofocus` directive on the given element.
 *
 * <hljs lang="html">
 * <md-menu-item>
 *   <md-button md-autofocus ng-click="doSomething()">
 *     Auto Focus
 *   </md-button>
 * </md-menu-item>
 * </hljs>
 *
 *
 * ### Preventing close
 *
 * Sometimes you would like to be able to click on a menu item without having the menu
 * close. To do this, AngularJS Material exposes the `md-prevent-menu-close` attribute which
 * can be added to a button inside a menu to stop the menu from automatically closing.
 * You can then close the menu either by using `$mdMenu.close()` in the template,
 * or programmatically by injecting `$mdMenu` and calling `$mdMenu.hide()`.
 *
 * <hljs lang="html">
 * <md-menu-content ng-mouseleave="$mdMenu.close()">
 *   <md-menu-item>
 *     <md-button ng-click="doSomething()" aria-label="Do something" md-prevent-menu-close="md-prevent-menu-close">
 *       <md-icon md-menu-align-target md-svg-icon="call:phone"></md-icon>
 *       Do Something
 *     </md-button>
 *   </md-menu-item>
 * </md-menu-content>
 * </hljs>
 *
 * @usage
 * <hljs lang="html">
 * <md-menu>
 *  <md-button ng-click="$mdMenu.open($event)" class="md-icon-button">
 *    <md-icon md-svg-icon="call:phone"></md-icon>
 *  </md-button>
 *  <md-menu-content>
 *    <md-menu-item><md-button ng-click="doSomething()">Do Something</md-button></md-menu-item>
 *  </md-menu-content>
 * </md-menu>
 * </hljs>
 *
 * @param {string=} md-position-mode Specify pre-defined position modes for the `x` and `y` axes.
 *  The default modes are `target target`. This positions the origin of the menu using the left and top edges
 *  of the origin element in LTR layouts.<br>
 *  #### Valid modes for horizontal positioning
 * - `target`
 * - `target-left`
 * - `target-right`
 * - `cascade`
 * - `right`
 * - `left`<br>
 *  #### Valid modes for vertical positioning
 * - `target`
 * - `cascade`
 * - `bottom`
 * @param {string=} md-offset An offset to apply to the dropdown on opening, after positioning.
 *  Defined as `x` and `y` pixel offset values in the form of `x y`.<br>
 *  The default value is `0 0`.
 */
MenuDirective['$inject'] = ["$mdUtil"];
angular
    .module('material.components.menu')
    .directive('mdMenu', MenuDirective);

/**
 * ngInject
 */
function MenuDirective($mdUtil) {
  var INVALID_PREFIX = 'Invalid HTML for md-menu: ';
  return {
    restrict: 'E',
    require: ['mdMenu', '?^mdMenuBar'],
    controller: 'mdMenuCtrl', // empty function to be built by link
    scope: true,
    compile: compile
  };

  function compile(templateElement) {
    templateElement.addClass('md-menu');

    var triggerEl = templateElement.children()[0];
    var prefixer = $mdUtil.prefixer();

    if (!prefixer.hasAttribute(triggerEl, 'ng-click')) {
      triggerEl = triggerEl
          .querySelector(prefixer.buildSelector(['ng-click', 'ng-mouseenter'])) || triggerEl;
    }

    var isButtonTrigger = triggerEl.nodeName === 'MD-BUTTON' || triggerEl.nodeName === 'BUTTON';

    if (triggerEl && isButtonTrigger && !triggerEl.hasAttribute('type')) {
      triggerEl.setAttribute('type', 'button');
    }

    if (!triggerEl) {
      throw Error(INVALID_PREFIX + 'Expected the menu to have a trigger element.');
    }

    if (templateElement.children().length !== 2) {
      throw Error(INVALID_PREFIX + 'Expected two children elements. The second element must have a `md-menu-content` element.');
    }

    // Default element for ARIA attributes has the ngClick or ngMouseenter expression
    triggerEl && triggerEl.setAttribute('aria-haspopup', 'true');

    var nestedMenus = templateElement[0].querySelectorAll('md-menu');
    var nestingDepth = parseInt(templateElement[0].getAttribute('md-nest-level'), 10) || 0;
    if (nestedMenus) {
      angular.forEach($mdUtil.nodesToArray(nestedMenus), function(menuEl) {
        if (!menuEl.hasAttribute('md-position-mode')) {
          menuEl.setAttribute('md-position-mode', 'cascade');
        }
        menuEl.classList.add('_md-nested-menu');
        menuEl.setAttribute('md-nest-level', nestingDepth + 1);
      });
    }
    return link;
  }

  function link(scope, element, attr, ctrls) {
    var mdMenuCtrl = ctrls[0];
    var isInMenuBar = !!ctrls[1];
    // Move everything into a md-menu-container and pass it to the controller
    var menuContainer = angular.element('<div class="_md md-open-menu-container md-whiteframe-z2"></div>');
    var menuContents = element.children()[1];

    element.addClass('_md');     // private md component indicator for styling

    if (!menuContents.hasAttribute('role')) {
      menuContents.setAttribute('role', 'menu');
    }
    menuContainer.append(menuContents);

    element.on('$destroy', function() {
      menuContainer.remove();
    });

    element.append(menuContainer);
    menuContainer[0].style.display = 'none';
    mdMenuCtrl.init(menuContainer, { isInMenuBar: isInMenuBar });

  }
}


MenuProvider['$inject'] = ["$$interimElementProvider"];angular
  .module('material.components.menu')
  .provider('$mdMenu', MenuProvider);

/**
 * Interim element provider for the menu.
 * Handles behavior for a menu while it is open, including:
 *    - handling animating the menu opening/closing
 *    - handling key/mouse events on the menu element
 *    - handling enabling/disabling scroll while the menu is open
 *    - handling redrawing during resizes and orientation changes
 *
 */

function MenuProvider($$interimElementProvider) {
  menuDefaultOptions['$inject'] = ["$mdUtil", "$mdTheming", "$mdConstant", "$document", "$window", "$q", "$$rAF", "$animateCss", "$animate", "$log"];
  var MENU_EDGE_MARGIN = 8;

  return $$interimElementProvider('$mdMenu')
    .setDefaults({
      methods: ['target'],
      options: menuDefaultOptions
    });

  /* ngInject */
  function menuDefaultOptions($mdUtil, $mdTheming, $mdConstant, $document, $window, $q, $$rAF,
                              $animateCss, $animate, $log) {

    var prefixer = $mdUtil.prefixer();
    var animator = $mdUtil.dom.animator;

    return {
      parent: 'body',
      onShow: onShow,
      onRemove: onRemove,
      hasBackdrop: true,
      disableParentScroll: true,
      skipCompile: true,
      preserveScope: true,
      multiple: true,
      themable: true
    };

    /**
     * Show modal backdrop element...
     * @returns {function(): void} A function that removes this backdrop
     */
    function showBackdrop(scope, element, options) {
      if (options.nestLevel) return angular.noop;

      // If we are not within a dialog...
      if (options.disableParentScroll && !$mdUtil.getClosest(options.target, 'MD-DIALOG')) {
        // !! DO this before creating the backdrop; since disableScrollAround()
        //    configures the scroll offset; which is used by mdBackDrop postLink()
        options.restoreScroll = $mdUtil.disableScrollAround(options.element, options.parent);
      } else {
        options.disableParentScroll = false;
      }

      if (options.hasBackdrop) {
        options.backdrop = $mdUtil.createBackdrop(scope, "md-menu-backdrop md-click-catcher");

        $animate.enter(options.backdrop, $document[0].body);
      }

      /**
       * Hide and destroys the backdrop created by showBackdrop()
       */
      return function hideBackdrop() {
        if (options.backdrop) options.backdrop.remove();
        if (options.disableParentScroll) options.restoreScroll();
      };
    }

    /**
     * Removing the menu element from the DOM and remove all associated event listeners
     * and backdrop
     */
    function onRemove(scope, element, opts) {
      opts.cleanupInteraction();
      opts.cleanupBackdrop();
      opts.cleanupResizing();
      opts.hideBackdrop();

      // Before the menu is closing remove the clickable class.
      element.removeClass('md-clickable');

      // For navigation $destroy events, do a quick, non-animated removal,
      // but for normal closes (from clicks, etc) animate the removal

      return (opts.$destroy === true) ? detachAndClean() : animateRemoval().then(detachAndClean);

      /**
       * For normal closes, animate the removal.
       * For forced closes (like $destroy events), skip the animations
       */
      function animateRemoval() {
        return $animateCss(element, {addClass: 'md-leave'}).start();
      }

      /**
       * Detach the element
       */
      function detachAndClean() {
        element.removeClass('md-active');
        detachElement(element, opts);
        opts.alreadyOpen = false;
      }

    }

    /**
     * Inserts and configures the staged Menu element into the DOM, positioning it,
     * and wiring up various interaction events
     */
    function onShow(scope, element, opts) {
      sanitizeAndConfigure(opts);

      if (opts.menuContentEl[0]) {
        // Inherit the theme from the target element.
        $mdTheming.inherit(opts.menuContentEl, opts.target);
      } else {
        $log.warn(
          '$mdMenu: Menu elements should always contain a `md-menu-content` element,' +
          'otherwise interactivity features will not work properly.',
          element
        );
      }

      // Register various listeners to move menu on resize/orientation change
      opts.cleanupResizing = startRepositioningOnResize();
      opts.hideBackdrop = showBackdrop(scope, element, opts);

      // Return the promise for when our menu is done animating in
      return showMenu()
        .then(function(response) {
          opts.alreadyOpen = true;
          opts.cleanupInteraction = activateInteraction();
          opts.cleanupBackdrop = setupBackdrop();

          // Since the menu finished its animation, mark the menu as clickable.
          element.addClass('md-clickable');

          return response;
        });

      /**
       * Place the menu into the DOM and call positioning related functions
       */
      function showMenu() {
        opts.parent.append(element);
        element[0].style.display = '';

        return $q(function(resolve) {
          var position = calculateMenuPosition(element, opts);

          element.removeClass('md-leave');

          // Animate the menu scaling, and opacity [from its position origin (default == top-left)]
          // to normal scale.
          $animateCss(element, {
            addClass: 'md-active',
            from: animator.toCss(position),
            to: animator.toCss({transform: ''})
          })
          .start()
          .then(resolve);

        });
      }

      /**
       * Check for valid opts and set some sane defaults
       */
      function sanitizeAndConfigure() {
        if (!opts.target) {
          throw Error(
            '$mdMenu.show() expected a target to animate from in options.target'
          );
        }
        angular.extend(opts, {
          alreadyOpen: false,
          isRemoved: false,
          target: angular.element(opts.target), // make sure it's not a naked DOM node
          parent: angular.element(opts.parent),
          menuContentEl: angular.element(element[0].querySelector('md-menu-content'))
        });
      }

      /**
       * Configure various resize listeners for screen changes
       */
      function startRepositioningOnResize() {

        var repositionMenu = (function(target, options) {
          return $$rAF.throttle(function() {
            if (opts.isRemoved) return;
            var position = calculateMenuPosition(target, options);

            target.css(animator.toCss(position));
          });
        })(element, opts);

        $window.addEventListener('resize', repositionMenu);
        $window.addEventListener('orientationchange', repositionMenu);

        return function stopRepositioningOnResize() {

          // Disable resizing handlers
          $window.removeEventListener('resize', repositionMenu);
          $window.removeEventListener('orientationchange', repositionMenu);

        };
      }

      /**
       * Sets up the backdrop and listens for click elements.
       * Once the backdrop will be clicked, the menu will automatically close.
       * @returns {!Function} Function to remove the backdrop.
       */
      function setupBackdrop() {
        if (!opts.backdrop) return angular.noop;

        opts.backdrop.on('click', onBackdropClick);

        return function() {
          opts.backdrop.off('click', onBackdropClick);
        };
      }

      /**
       * Function to be called whenever the backdrop is clicked.
       * @param {!MouseEvent} event
       */
      function onBackdropClick(event) {
        event.preventDefault();
        event.stopPropagation();

        scope.$apply(function() {
          opts.mdMenuCtrl.close(true, { closeAll: true });
        });
      }

      /**
       * Activate interaction on the menu. Resolves the focus target and closes the menu on
       * escape or option click.
       * @returns {!Function} Function to deactivate the interaction listeners.
       */
      function activateInteraction() {
        if (!opts.menuContentEl[0]) return angular.noop;

        // Wire up keyboard listeners.
        // - Close on escape,
        // - focus next item on down arrow,
        // - focus prev item on up
        opts.menuContentEl.on('keydown', onMenuKeyDown);
        opts.menuContentEl[0].addEventListener('click', captureClickListener, true);

        // kick off initial focus in the menu on the first enabled element
        var focusTarget = opts.menuContentEl[0]
          .querySelector(prefixer.buildSelector(['md-menu-focus-target', 'md-autofocus']));

        if (!focusTarget) {
          var childrenLen = opts.menuContentEl[0].children.length;
          for (var childIndex = 0; childIndex < childrenLen; childIndex++) {
            var child = opts.menuContentEl[0].children[childIndex];
            focusTarget = child.querySelector('.md-button:not([disabled])');
            if (focusTarget) {
              break;
            }
            if (child.firstElementChild && !child.firstElementChild.disabled) {
              focusTarget = child.firstElementChild;
              break;
            }
          }
        }

        focusTarget && focusTarget.focus();

        return function cleanupInteraction() {
          opts.menuContentEl.off('keydown', onMenuKeyDown);
          opts.menuContentEl[0].removeEventListener('click', captureClickListener, true);
        };

        // ************************************
        // internal functions
        // ************************************

        function onMenuKeyDown(ev) {
          var handled;
          switch (ev.keyCode) {
            case $mdConstant.KEY_CODE.ESCAPE:
              opts.mdMenuCtrl.close(false, { closeAll: true });
              handled = true;
              break;
            case $mdConstant.KEY_CODE.TAB:
              opts.mdMenuCtrl.close(false, { closeAll: true });
              // Don't prevent default or stop propagation on this event as we want tab
              // to move the focus to the next focusable element on the page.
              handled = false;
              break;
            case $mdConstant.KEY_CODE.UP_ARROW:
              if (!focusMenuItem(ev, opts.menuContentEl, opts, -1) && !opts.nestLevel) {
                opts.mdMenuCtrl.triggerContainerProxy(ev);
              }
              handled = true;
              break;
            case $mdConstant.KEY_CODE.DOWN_ARROW:
              if (!focusMenuItem(ev, opts.menuContentEl, opts, 1) && !opts.nestLevel) {
                opts.mdMenuCtrl.triggerContainerProxy(ev);
              }
              handled = true;
              break;
            case $mdConstant.KEY_CODE.LEFT_ARROW:
              if (opts.nestLevel) {
                opts.mdMenuCtrl.close();
              } else {
                opts.mdMenuCtrl.triggerContainerProxy(ev);
              }
              handled = true;
              break;
            case $mdConstant.KEY_CODE.RIGHT_ARROW:
              var parentMenu = $mdUtil.getClosest(ev.target, 'MD-MENU');
              if (parentMenu && parentMenu != opts.parent[0]) {
                ev.target.click();
              } else {
                opts.mdMenuCtrl.triggerContainerProxy(ev);
              }
              handled = true;
              break;
          }
          if (handled) {
            ev.preventDefault();
            ev.stopImmediatePropagation();
          }
        }

        function onBackdropClick(e) {
          e.preventDefault();
          e.stopPropagation();
          scope.$apply(function() {
            opts.mdMenuCtrl.close(true, { closeAll: true });
          });
        }

        // Close menu on menu item click, if said menu-item is not disabled
        function captureClickListener(e) {
          var target = e.target;
          // Traverse up the event until we get to the menuContentEl to see if
          // there is an ng-click and that the ng-click is not disabled
          do {
            if (target == opts.menuContentEl[0]) return;
            if ((hasAnyAttribute(target, ['ng-click', 'ng-href', 'ui-sref']) ||
                target.nodeName == 'BUTTON' || target.nodeName == 'MD-BUTTON') && !hasAnyAttribute(target, ['md-prevent-menu-close'])) {
              var closestMenu = $mdUtil.getClosest(target, 'MD-MENU');
              if (!target.hasAttribute('disabled') && (!closestMenu || closestMenu == opts.parent[0])) {
                close();
              }
              break;
            }
          } while (target = target.parentNode);

          function close() {
            scope.$apply(function() {
              opts.mdMenuCtrl.close(true, { closeAll: true });
            });
          }

          function hasAnyAttribute(target, attrs) {
            if (!target) return false;

            for (var i = 0, attr; attr = attrs[i]; ++i) {
              if (prefixer.hasAttribute(target, attr)) {
                return true;
              }
            }

            return false;
          }
        }

      }
    }

    /**
     * Takes a keypress event and focuses the next/previous menu
     * item from the emitting element
     * @param {event} e - The origin keypress event
     * @param {angular.element} menuEl - The menu element
     * @param {object} opts - The interim element options for the mdMenu
     * @param {number} direction - The direction to move in (+1 = next, -1 = prev)
     */
    function focusMenuItem(e, menuEl, opts, direction) {
      var currentItem = $mdUtil.getClosest(e.target, 'MD-MENU-ITEM');

      var items = $mdUtil.nodesToArray(menuEl[0].children);
      var currentIndex = items.indexOf(currentItem);

      // Traverse through our elements in the specified direction (+/-1) and try to
      // focus them until we find one that accepts focus
      var didFocus;
      for (var i = currentIndex + direction; i >= 0 && i < items.length; i = i + direction) {
        var focusTarget = items[i].querySelector('.md-button');
        didFocus = attemptFocus(focusTarget);
        if (didFocus) {
          break;
        }
      }
      return didFocus;
    }

    /**
     * Attempts to focus an element. Checks whether that element is the currently
     * focused element after attempting.
     * @param {HTMLElement} el - the element to attempt focus on
     * @returns {boolean} - whether the element was successfully focused
     */
    function attemptFocus(el) {
      if (el && el.getAttribute('tabindex') != -1) {
        el.focus();
        return ($document[0].activeElement == el);
      }
    }

    /**
     * Use browser to remove this element without triggering a $destroy event
     */
    function detachElement(element, opts) {
      if (!opts.preserveElement) {
        if (toNode(element).parentNode === toNode(opts.parent)) {
          toNode(opts.parent).removeChild(toNode(element));
        }
      } else {
        toNode(element).style.display = 'none';
      }
    }

    /**
     * Computes menu position and sets the style on the menu container
     * @param {HTMLElement} el - the menu container element
     * @param {object} opts - the interim element options object
     */
    function calculateMenuPosition(el, opts) {

      var containerNode = el[0],
        openMenuNode = el[0].firstElementChild,
        openMenuNodeRect = openMenuNode.getBoundingClientRect(),
        boundryNode = $document[0].body,
        boundryNodeRect = boundryNode.getBoundingClientRect();

      var menuStyle = $window.getComputedStyle(openMenuNode);

      var originNode = opts.target[0].querySelector(prefixer.buildSelector('md-menu-origin')) || opts.target[0],
        originNodeRect = originNode.getBoundingClientRect();

      var bounds = {
        left: boundryNodeRect.left + MENU_EDGE_MARGIN,
        top: Math.max(boundryNodeRect.top, 0) + MENU_EDGE_MARGIN,
        bottom: Math.max(boundryNodeRect.bottom, Math.max(boundryNodeRect.top, 0) + boundryNodeRect.height) - MENU_EDGE_MARGIN,
        right: boundryNodeRect.right - MENU_EDGE_MARGIN
      };

      var alignTarget, alignTargetRect = { top:0, left : 0, right:0, bottom:0 }, existingOffsets  = { top:0, left : 0, right:0, bottom:0  };
      var positionMode = opts.mdMenuCtrl.positionMode();

      if (positionMode.top === 'target' || positionMode.left === 'target' || positionMode.left === 'target-right') {
        alignTarget = firstVisibleChild();
        if (alignTarget) {
          // TODO: Allow centering on an arbitrary node, for now center on first menu-item's child
          alignTarget = alignTarget.firstElementChild || alignTarget;
          alignTarget = alignTarget.querySelector(prefixer.buildSelector('md-menu-align-target')) || alignTarget;
          alignTargetRect = alignTarget.getBoundingClientRect();

          existingOffsets = {
            top: parseFloat(containerNode.style.top || 0),
            left: parseFloat(containerNode.style.left || 0)
          };
        }
      }

      var position = {};
      var transformOrigin = 'top ';

      switch (positionMode.top) {
        case 'target':
          position.top = existingOffsets.top + originNodeRect.top - alignTargetRect.top;
          break;
        case 'cascade':
          position.top = originNodeRect.top - parseFloat(menuStyle.paddingTop) - originNode.style.top;
          break;
        case 'bottom':
          position.top = originNodeRect.top + originNodeRect.height;
          break;
        default:
          throw new Error('Invalid target mode "' + positionMode.top + '" specified for md-menu on Y axis.');
      }

      var rtl = ($mdUtil.bidi() === 'rtl');

      switch (positionMode.left) {
        case 'target':
          position.left = existingOffsets.left + originNodeRect.left - alignTargetRect.left;
          transformOrigin += rtl ? 'right'  : 'left';
          break;
        case 'target-left':
          position.left = originNodeRect.left;
          transformOrigin += 'left';
          break;
        case 'target-right':
          position.left = originNodeRect.right - openMenuNodeRect.width + (openMenuNodeRect.right - alignTargetRect.right);
          transformOrigin += 'right';
          break;
        case 'cascade':
          var willFitRight = rtl ? (originNodeRect.left - openMenuNodeRect.width) < bounds.left : (originNodeRect.right + openMenuNodeRect.width) < bounds.right;
          position.left = willFitRight ? originNodeRect.right - originNode.style.left : originNodeRect.left - originNode.style.left - openMenuNodeRect.width;
          transformOrigin += willFitRight ? 'left' : 'right';
          break;
        case 'right':
          if (rtl) {
            position.left = originNodeRect.right - originNodeRect.width;
            transformOrigin += 'left';
          } else {
            position.left = originNodeRect.right - openMenuNodeRect.width;
            transformOrigin += 'right';
          }
          break;
        case 'left':
          if (rtl) {
            position.left = originNodeRect.right - openMenuNodeRect.width;
            transformOrigin += 'right';
          } else {
            position.left = originNodeRect.left;
            transformOrigin += 'left';
          }
          break;
        default:
          throw new Error('Invalid target mode "' + positionMode.left + '" specified for md-menu on X axis.');
      }

      var offsets = opts.mdMenuCtrl.offsets();
      position.top += offsets.top;
      position.left += offsets.left;

      clamp(position);

      var scaleX = Math.round(100 * Math.min(originNodeRect.width / containerNode.offsetWidth, 1.0)) / 100;
      var scaleY = Math.round(100 * Math.min(originNodeRect.height / containerNode.offsetHeight, 1.0)) / 100;

      return {
        top: Math.round(position.top),
        left: Math.round(position.left),
        // Animate a scale out if we aren't just repositioning
        transform: !opts.alreadyOpen ? $mdUtil.supplant('scale({0},{1})', [scaleX, scaleY]) : undefined,
        transformOrigin: transformOrigin
      };

      /**
       * Clamps the repositioning of the menu within the confines of
       * bounding element (often the screen/body)
       */
      function clamp(pos) {
        pos.top = Math.max(Math.min(pos.top, bounds.bottom - containerNode.offsetHeight), bounds.top);
        pos.left = Math.max(Math.min(pos.left, bounds.right - containerNode.offsetWidth), bounds.left);
      }

      /**
       * Gets the first visible child in the openMenuNode
       * Necessary incase menu nodes are being dynamically hidden
       */
      function firstVisibleChild() {
        for (var i = 0; i < openMenuNode.children.length; ++i) {
          if ($window.getComputedStyle(openMenuNode.children[i]).display != 'none') {
            return openMenuNode.children[i];
          }
        }
      }
    }
  }
  function toNode(el) {
    if (el instanceof angular.element) {
      el = el[0];
    }
    return el;
  }
}

})(window, window.angular);