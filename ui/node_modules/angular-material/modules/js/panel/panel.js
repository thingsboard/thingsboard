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
 * @name material.components.panel
 */
MdPanelService['$inject'] = ["presets", "$rootElement", "$rootScope", "$injector", "$window"];
angular
  .module('material.components.panel', [
    'material.core',
    'material.components.backdrop'
  ])
  .provider('$mdPanel', MdPanelProvider);


/*****************************************************************************
 *                            PUBLIC DOCUMENTATION                           *
 *****************************************************************************/


/**
 * @ngdoc service
 * @name $mdPanelProvider
 * @module material.components.panel
 *
 * @description
 * `$mdPanelProvider` allows users to create configuration presets that will be
 * stored within a cached presets object. When the configuration is needed, the
 * user can request the preset by passing it as the first parameter in the
 * `$mdPanel.create` or `$mdPanel.open` methods.
 *
 * @usage
 * <hljs lang="js">
 * (function(angular, undefined) {
 *   'use strict';
 *
 *   angular
 *       .module('demoApp', ['ngMaterial'])
 *       .config(DemoConfig)
 *       .controller('DemoCtrl', DemoCtrl)
 *       .controller('DemoMenuCtrl', DemoMenuCtrl);
 *
 *   function DemoConfig($mdPanelProvider) {
 *     $mdPanelProvider.definePreset('demoPreset', {
 *       attachTo: angular.element(document.body),
 *       controller: DemoMenuCtrl,
 *       controllerAs: 'ctrl',
 *       template: '' +
 *           '<div class="menu-panel" md-whiteframe="4">' +
 *           '  <div class="menu-content">' +
 *           '    <div class="menu-item" ng-repeat="item in ctrl.items">' +
 *           '      <button class="md-button">' +
 *           '        <span>{{item}}</span>' +
 *           '      </button>' +
 *           '    </div>' +
 *           '    <md-divider></md-divider>' +
 *           '    <div class="menu-item">' +
 *           '      <button class="md-button" ng-click="ctrl.closeMenu()">' +
 *           '        <span>Close Menu</span>' +
 *           '      </button>' +
 *           '    </div>' +
 *           '  </div>' +
 *           '</div>',
 *       panelClass: 'menu-panel-container',
 *       focusOnOpen: false,
 *       zIndex: 100,
 *       propagateContainerEvents: true,
 *       groupName: 'menus'
 *     });
 *   }
 *
 *   function PanelProviderCtrl($mdPanel) {
 *     this.navigation = {
 *       name: 'navigation',
 *       items: [
 *         'Home',
 *         'About',
 *         'Contact'
 *       ]
 *     };
 *     this.favorites = {
 *       name: 'favorites',
 *       items: [
 *         'Add to Favorites'
 *       ]
 *     };
 *     this.more = {
 *       name: 'more',
 *       items: [
 *         'Account',
 *         'Sign Out'
 *       ]
 *     };
 *
 *     $mdPanel.newPanelGroup('menus', {
 *       maxOpen: 2
 *     });
 *
 *     this.showMenu = function($event, menu) {
 *       $mdPanel.open('demoPreset', {
 *         id: 'menu_' + menu.name,
 *         position: $mdPanel.newPanelPosition()
 *             .relativeTo($event.target)
 *             .addPanelPosition(
 *               $mdPanel.xPosition.ALIGN_START,
 *               $mdPanel.yPosition.BELOW
 *             ),
 *         locals: {
 *           items: menu.items
 *         },
 *         openFrom: $event
 *       });
 *     };
 *   }
 *
 *   function PanelMenuCtrl(mdPanelRef) {
 *     // 'mdPanelRef' is injected in the controller.
 *     this.closeMenu = function() {
 *       if (mdPanelRef) {
 *         mdPanelRef.close();
 *       }
 *     };
 *   }
 * })(angular);
 * </hljs>
 */

/**
 * @ngdoc method
 * @name $mdPanelProvider#definePreset
 * @description
 * Takes the passed in preset name and preset configuration object and adds it
 * to the `_presets` object of the provider. This `_presets` object is then
 * passed along to the `$mdPanel` service.
 *
 * @param {string} name Preset name.
 * @param {!Object} preset Specific configuration object that can contain any
 *     and all of the parameters avaialble within the `$mdPanel.create` method.
 *     However, parameters that pertain to id, position, animation, and user
 *     interaction are not allowed and will be removed from the preset
 *     configuration.
 */


/*****************************************************************************
 *                               MdPanel Service                             *
 *****************************************************************************/


/**
 * @ngdoc service
 * @name $mdPanel
 * @module material.components.panel
 *
 * @description
 * `$mdPanel` is a robust, low-level service for creating floating panels on
 * the screen. It can be used to implement tooltips, dialogs, pop-ups, etc.
 *
 * The following types, referenced below, have separate documentation:
 * - <a href="api/type/MdPanelAnimation">MdPanelAnimation</a> from `$mdPanel.newPanelAnimation()`
 * - <a href="api/type/MdPanelPosition">MdPanelPosition</a> from `$mdPanel.newPanelPosition()`
 * - <a href="api/type/MdPanelRef">MdPanelRef</a> from the `$mdPanel.open()` Promise or
 * injected in the panel's controller
 *
 * @usage
 * <hljs lang="js">
 * (function(angular, undefined) {
 *   'use strict';
 *
 *   angular
 *       .module('demoApp', ['ngMaterial'])
 *       .controller('DemoDialogController', DialogController)
 *       .controller('DemoCtrl', function($mdPanel) {
 *
 *     var panelRef;
 *
 *     function showPanel($event) {
 *       var panelPosition = $mdPanel.newPanelPosition()
 *           .absolute()
 *           .top('50%')
 *           .left('50%');
 *
 *       var panelAnimation = $mdPanel.newPanelAnimation()
 *           .openFrom($event)
 *           .duration(200)
 *           .closeTo('.show-button')
 *           .withAnimation($mdPanel.animation.SCALE);
 *
 *       var config = {
 *         attachTo: angular.element(document.body),
 *         controller: DialogController,
 *         controllerAs: 'ctrl',
 *         position: panelPosition,
 *         animation: panelAnimation,
 *         targetEvent: $event,
 *         templateUrl: 'dialog-template.html',
 *         clickOutsideToClose: true,
 *         escapeToClose: true,
 *         focusOnOpen: true
 *       };
 *
 *       $mdPanel.open(config)
 *           .then(function(result) {
 *             panelRef = result;
 *           });
 *     }
 *   }
 *
 *   function DialogController(MdPanelRef) {
 *     function closeDialog() {
 *       if (MdPanelRef) MdPanelRef.close();
 *     }
 *   }
 * })(angular);
 * </hljs>
 */

/**
 * @ngdoc method
 * @name $mdPanel#create
 * @description
 * Creates a panel with the specified options.
 *
 * @param config {!Object=} Specific configuration object that may contain the
 *     following properties:
 *
 *   - `id` - `{string=}`: An ID to track the panel by. When an ID is provided,
 *     the created panel is added to a tracked panels object. Any subsequent
 *     requests made to create a panel with that ID are ignored. This is useful
 *     in having the panel service not open multiple panels from the same user
 *     interaction when there is no backdrop and events are propagated. Defaults
 *     to an arbitrary string that is not tracked.
 *   - `template` - `{string=}`: HTML template to show in the panel. This
 *     **must** be trusted HTML with respect to AngularJS’s
 *     [$sce service](https://docs.angularjs.org/api/ng/service/$sce).
 *   - `templateUrl` - `{string=}`: The URL that will be used as the content of
 *     the panel.
 *   - `contentElement` - `{(string|!angular.JQLite|!Element)=}`: Pre-compiled
 *     element to be used as the panel's content.
 *   - `controller` - `{(function|string)=}`: The controller to associate with
 *     the panel. The controller can inject a reference to the returned
 *     panelRef, which allows the panel to be closed, hidden, and shown. Any
 *     fields passed in through locals or resolve will be bound to the
 *     controller.
 *   - `controllerAs` - `{string=}`: An alias to assign the controller to on
 *     the scope.
 *   - `bindToController` - `{boolean=}`: Binds locals to the controller
 *     instead of passing them in. Defaults to true, as this is a best
 *     practice.
 *   - `locals` - `{Object=}`: An object containing key/value pairs. The keys
 *     will be used as names of values to inject into the controller. For
 *     example, `locals: {three: 3}` would inject `three` into the controller,
 *     with the value 3. 'mdPanelRef' is a reserved key, and will always
 *     be set to the created MdPanelRef instance.
 *   - `resolve` - `{Object=}`: Similar to locals, except it takes promises as
 *     values. The panel will not open until all of the promises resolve.
 *   - `attachTo` - `{(string|!angular.JQLite|!Element)=}`: The element to
 *     attach the panel to. Defaults to appending to the root element of the
 *     application.
 *   - `propagateContainerEvents` - `{boolean=}`: Whether pointer or touch
 *     events should be allowed to propagate 'go through' the container, aka the
 *     wrapper, of the panel. Defaults to false.
 *   - `panelClass` - `{string=}`: A css class to apply to the panel element.
 *     This class should define any borders, box-shadow, etc. for the panel.
 *   - `zIndex` - `{number=}`: The z-index to place the panel at.
 *     Defaults to 80.
 *   - `position` - `{MdPanelPosition=}`: An MdPanelPosition object that
 *     specifies the alignment of the panel. For more information, see
 *     `MdPanelPosition`.
 *   - `clickOutsideToClose` - `{boolean=}`: Whether the user can click
 *     outside the panel to close it. Defaults to false.
 *   - `escapeToClose` - `{boolean=}`: Whether the user can press escape to
 *     close the panel. Defaults to false.
 *   - `onCloseSuccess` - `{function(!panelRef, string)=}`: Function that is
 *     called after the close successfully finishes. The first parameter passed
 *     into this function is the current panelRef and the 2nd is an optional
 *     string explaining the close reason. The currently supported closeReasons
 *     can be found in the MdPanelRef.closeReasons enum. These are by default
 *     passed along by the panel.
 *   - `trapFocus` - `{boolean=}`: Whether focus should be trapped within the
 *     panel. If `trapFocus` is true, the user will not be able to interact
 *     with the rest of the page until the panel is dismissed. Defaults to
 *     false.
 *   - `focusOnOpen` - `{boolean=}`: An option to override focus behavior on
 *     open. Only disable if focusing some other way, as focus management is
 *     required for panels to be accessible. Defaults to true.
 *   - `fullscreen` - `{boolean=}`: Whether the panel should be full screen.
 *     Applies the class `._md-panel-fullscreen` to the panel on open. Defaults
 *     to false.
 *   - `animation` - `{MdPanelAnimation=}`: An MdPanelAnimation object that
 *     specifies the animation of the panel. For more information, see
 *     `MdPanelAnimation`.
 *   - `hasBackdrop` - `{boolean=}`: Whether there should be an opaque backdrop
 *     behind the panel. Defaults to false.
 *   - `disableParentScroll` - `{boolean=}`: Whether the user can scroll the
 *     page behind the panel. Defaults to false.
 *   - `onDomAdded` - `{function=}`: Callback function used to announce when
 *     the panel is added to the DOM.
 *   - `onOpenComplete` - `{function=}`: Callback function used to announce
 *     when the open() action is finished.
 *   - `onRemoving` - `{function=}`: Callback function used to announce the
 *     close/hide() action is starting.
 *   - `onDomRemoved` - `{function=}`: Callback function used to announce when
 *     the panel is removed from the DOM.
 *   - `origin` - `{(string|!angular.JQLite|!Element)=}`: The element to focus
 *     on when the panel closes. This is commonly the element which triggered
 *     the opening of the panel. If you do not use `origin`, you need to control
 *     the focus manually.
 *   - `groupName` - `{(string|!Array<string>)=}`: A group name or an array of
 *     group names. The group name is used for creating a group of panels. The
 *     group is used for configuring the number of open panels and identifying
 *     specific behaviors for groups. For instance, all tooltips could be
 *     identified using the same groupName.
 *
 * @returns {!MdPanelRef} panelRef
 */

/**
 * @ngdoc method
 * @name $mdPanel#open
 * @description
 * Calls the create method above, then opens the panel. This is a shortcut for
 * creating and then calling open manually. If custom methods need to be
 * called when the panel is added to the DOM or opened, do not use this method.
 * Instead create the panel, chain promises on the domAdded and openComplete
 * methods, and call open from the returned panelRef.
 *
 * @param {!Object=} config Specific configuration object that may contain
 *     the properties defined in `$mdPanel.create`.
 * @returns {!angular.$q.Promise<!MdPanelRef>} panelRef A promise that resolves
 *     to an instance of the panel.
 */

/**
 * @ngdoc method
 * @name $mdPanel#newPanelPosition
 * @description
 * Returns a new instance of the MdPanelPosition object. Use this to create
 * the position config object.
 *
 * @returns {!MdPanelPosition} panelPosition
 */

/**
 * @ngdoc method
 * @name $mdPanel#newPanelAnimation
 * @description
 * Returns a new instance of the MdPanelAnimation object. Use this to create
 * the animation config object.
 *
 * @returns {!MdPanelAnimation} panelAnimation
 */

/**
 * @ngdoc method
 * @name $mdPanel#newPanelGroup
 * @description
 * Creates a panel group and adds it to a tracked list of panel groups.
 *
 * @param {string} groupName Name of the group to create.
 * @param {!Object=} config Specific configuration object that may contain the
 *     following properties:
 *
 *   - `maxOpen` - `{number=}`: The maximum number of panels that are allowed to
 *     be open within a defined panel group.
 *
 * @returns {!Object<string,
 *     {panels: !Array<!MdPanelRef>,
 *     openPanels: !Array<!MdPanelRef>,
 *     maxOpen: number}>} panelGroup
 */

/**
 * @ngdoc method
 * @name $mdPanel#setGroupMaxOpen
 * @description
 * Sets the maximum number of panels in a group that can be opened at a given
 * time.
 *
 * @param {string} groupName The name of the group to configure.
 * @param {number} maxOpen The maximum number of panels that can be
 *     opened. Infinity can be passed in to remove the maxOpen limit.
 */


/*****************************************************************************
 *                                 MdPanelRef                                *
 *****************************************************************************/


/**
 * @ngdoc type
 * @name MdPanelRef
 * @module material.components.panel
 * @description
 * A reference to a created panel. This reference contains a unique id for the
 * panel, along with the following properties:
 *
 *   - `id` - `{string}`: The unique id for the panel. This id is used to track
 *     when a panel was interacted with.
 *   - `config` - `{!Object=}`: The entire config object that was used in
 *     create.
 *   - `isAttached` - `{boolean}`: Whether the panel is attached to the DOM.
 *     Visibility to the user does not factor into isAttached.
 *   - `panelContainer` - `{angular.JQLite}`: The wrapper element containing the
 *     panel. This property is added in order to have access to the `addClass`,
 *     `removeClass`, `toggleClass`, etc methods.
 *   - `panelEl` - `{angular.JQLite}`: The panel element. This property is added
 *     in order to have access to the `addClass`, `removeClass`, `toggleClass`,
 *     etc methods.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#open
 * @description
 * Attaches and shows the panel.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel is
 *     opened.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#close
 * @description
 * Hides and detaches the panel. Note that this will **not** destroy the panel.
 * If you don't intend on using the panel again, call the {@link #destroy
 * destroy} method afterwards.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel is
 *     closed.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#attach
 * @description
 * Create the panel elements and attach them to the DOM. The panel will be
 * hidden by default.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel is
 *     attached.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#detach
 * @description
 * Removes the panel from the DOM. This will NOT hide the panel before removing
 * it.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel is
 *     detached.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#show
 * @description
 * Shows the panel.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel has
 *     shown and animations are completed.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#hide
 * @description
 * Hides the panel.
 *
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel has
 *     hidden and animations are completed.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#destroy
 * @description
 * Destroys the panel. The panel cannot be opened again after this is called.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#addClass
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 * @description
 * Adds a class to the panel. DO NOT use this hide/show the panel.
 *
 * @param {string} newClass class to be added.
 * @param {boolean} toElement Whether or not to add the class to the panel
 *     element instead of the container.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#removeClass
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 * @description
 * Removes a class from the panel. DO NOT use this to hide/show the panel.
 *
 * @param {string} oldClass Class to be removed.
 * @param {boolean} fromElement Whether or not to remove the class from the
 *     panel element instead of the container.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#toggleClass
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 * @description
 * Toggles a class on the panel. DO NOT use this to hide/show the panel.
 *
 * @param {string} toggleClass Class to be toggled.
 * @param {boolean} onElement Whether or not to remove the class from the panel
 *     element instead of the container.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#updatePosition
 * @description
 * Updates the position configuration of a panel. Use this to update the
 * position of a panel that is open, without having to close and re-open the
 * panel.
 *
 * @param {!MdPanelPosition} position
 */

/**
 * @ngdoc method
 * @name MdPanelRef#addToGroup
 * @description
 * Adds a panel to a group if the panel does not exist within the group already.
 * A panel can only exist within a single group.
 *
 * @param {string} groupName The name of the group to add the panel to.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#removeFromGroup
 * @description
 * Removes a panel from a group if the panel exists within that group. The group
 * must be created ahead of time.
 *
 * @param {string} groupName The name of the group.
 */

/**
 * @ngdoc method
 * @name MdPanelRef#registerInterceptor
 * @description
 * Registers an interceptor with the panel. The callback should return a promise,
 * which will allow the action to continue when it gets resolved, or will
 * prevent an action if it is rejected. The interceptors are called sequentially
 * and it reverse order. `type` must be one of the following
 * values available on `$mdPanel.interceptorTypes`:
 * * `CLOSE` - Gets called before the panel begins closing.
 *
 * @param {string} type Type of interceptor.
 * @param {!angular.$q.Promise<any>} callback Callback to be registered.
 * @returns {!MdPanelRef}
 */

/**
 * @ngdoc method
 * @name MdPanelRef#removeInterceptor
 * @description
 * Removes a registered interceptor.
 *
 * @param {string} type Type of interceptor to be removed.
 * @param {function(): !angular.$q.Promise<any>} callback Interceptor to be removed.
 * @returns {!MdPanelRef}
 */

/**
 * @ngdoc method
 * @name MdPanelRef#removeAllInterceptors
 * @description
 * Removes all interceptors. If a type is supplied, only the
 * interceptors of that type will be cleared.
 *
 * @param {string=} type Type of interceptors to be removed.
 * @returns {!MdPanelRef}
 */

/**
 * @ngdoc method
 * @name MdPanelRef#updateAnimation
 * @description
 * Updates the animation configuration for a panel. You can use this to change
 * the panel's animation without having to re-create it.
 *
 * @param {!MdPanelAnimation} animation
 */


/*****************************************************************************
 *                               MdPanelPosition                            *
 *****************************************************************************/


/**
 * @ngdoc type
 * @name MdPanelPosition
 * @module material.components.panel
 * @description
 *
 * Object for configuring the position of the panel.
 *
 * @usage
 *
 * #### Centering the panel
 *
 * <hljs lang="js">
 * new MdPanelPosition().absolute().center();
 * </hljs>
 *
 * #### Overlapping the panel with an element
 *
 * <hljs lang="js">
 * new MdPanelPosition()
 *     .relativeTo(someElement)
 *     .addPanelPosition(
 *       $mdPanel.xPosition.ALIGN_START,
 *       $mdPanel.yPosition.ALIGN_TOPS
 *     );
 * </hljs>
 *
 * #### Aligning the panel with the bottom of an element
 *
 * <hljs lang="js">
 * new MdPanelPosition()
 *     .relativeTo(someElement)
 *     .addPanelPosition($mdPanel.xPosition.CENTER, $mdPanel.yPosition.BELOW);
 * </hljs>
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#absolute
 * @description
 * Positions the panel absolutely relative to the parent element. If the parent
 * is document.body, this is equivalent to positioning the panel absolutely
 * within the viewport.
 *
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#relativeTo
 * @description
 * Positions the panel relative to a specific element.
 *
 * @param {string|!Element|!angular.JQLite} element Query selector, DOM element,
 *     or angular element to position the panel with respect to.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#top
 * @description
 * Sets the value of `top` for the panel. Clears any previously set vertical
 * position.
 *
 * @param {string=} top Value of `top`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#bottom
 * @description
 * Sets the value of `bottom` for the panel. Clears any previously set vertical
 * position.
 *
 * @param {string=} bottom Value of `bottom`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#start
 * @description
 * Sets the panel to the start of the page - `left` if `ltr` or `right` for
 * `rtl`. Clears any previously set horizontal position.
 *
 * @param {string=} start Value of position. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#end
 * @description
 * Sets the panel to the end of the page - `right` if `ltr` or `left` for `rtl`.
 * Clears any previously set horizontal position.
 *
 * @param {string=} end Value of position. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#left
 * @description
 * Sets the value of `left` for the panel. Clears any previously set
 * horizontal position.
 *
 * @param {string=} left Value of `left`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#right
 * @description
 * Sets the value of `right` for the panel. Clears any previously set
 * horizontal position.
 *
 * @param {string=} right Value of `right`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#centerHorizontally
 * @description
 * Centers the panel horizontally in the viewport. Clears any previously set
 * horizontal position.
 *
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#centerVertically
 * @description
 * Centers the panel vertically in the viewport. Clears any previously set
 * vertical position.
 *
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#center
 * @description
 * Centers the panel horizontally and vertically in the viewport. This is
 * equivalent to calling both `centerHorizontally` and `centerVertically`.
 * Clears any previously set horizontal and vertical positions.
 *
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#addPanelPosition
 * @description
 * Sets the x and y position for the panel relative to another element. Can be
 * called multiple times to specify an ordered list of panel positions. The
 * first position which allows the panel to be completely on-screen will be
 * chosen; the last position will be chose whether it is on-screen or not.
 *
 * xPosition must be one of the following values available on
 * $mdPanel.xPosition:
 *
 *
 * CENTER | ALIGN_START | ALIGN_END | OFFSET_START | OFFSET_END
 *
 * <pre>
 *    *************
 *    *           *
 *    *   PANEL   *
 *    *           *
 *    *************
 *   A B    C    D E
 *
 * A: OFFSET_START (for LTR displays)
 * B: ALIGN_START (for LTR displays)
 * C: CENTER
 * D: ALIGN_END (for LTR displays)
 * E: OFFSET_END (for LTR displays)
 * </pre>
 *
 * yPosition must be one of the following values available on
 * $mdPanel.yPosition:
 *
 * CENTER | ALIGN_TOPS | ALIGN_BOTTOMS | ABOVE | BELOW
 *
 * <pre>
 *   F
 *   G *************
 *     *           *
 *   H *   PANEL   *
 *     *           *
 *   I *************
 *   J
 *
 * F: BELOW
 * G: ALIGN_TOPS
 * H: CENTER
 * I: ALIGN_BOTTOMS
 * J: ABOVE
 * </pre>
 *
 * @param {string} xPosition
 * @param {string} yPosition
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#withOffsetX
 * @description
 * Sets the value of the offset in the x-direction.
 *
 * @param {string|number} offsetX
 * @returns {!MdPanelPosition}
 */

/**
 * @ngdoc method
 * @name MdPanelPosition#withOffsetY
 * @description
 * Sets the value of the offset in the y-direction.
 *
 * @param {string|number} offsetY
 * @returns {!MdPanelPosition}
 */


/*****************************************************************************
 *                               MdPanelAnimation                            *
 *****************************************************************************/


/**
 * @ngdoc type
 * @name MdPanelAnimation
 * @module material.components.panel
 * @description
 * Animation configuration object. To use, create an MdPanelAnimation with the
 * desired properties, then pass the object as part of $mdPanel creation.
 *
 * @usage
 *
 * <hljs lang="js">
 * var panelAnimation = new MdPanelAnimation()
 *     .openFrom(myButtonEl)
 *     .duration(1337)
 *     .closeTo('.my-button')
 *     .withAnimation($mdPanel.animation.SCALE);
 *
 * $mdPanel.create({
 *   animation: panelAnimation
 * });
 * </hljs>
 */

/**
 * @ngdoc method
 * @name MdPanelAnimation#openFrom
 * @description
 * Specifies where to start the open animation. `openFrom` accepts a
 * click event object, query selector, DOM element, or a Rect object that
 * is used to determine the bounds. When passed a click event, the location
 * of the click will be used as the position to start the animation.
 *
 * @param {string|!Element|!Event|{top: number, left: number}}
 * @returns {!MdPanelAnimation}
 */

/**
 * @ngdoc method
 * @name MdPanelAnimation#closeTo
 * @description
 * Specifies where to animate the panel close. `closeTo` accepts a
 * query selector, DOM element, or a Rect object that is used to determine
 * the bounds.
 *
 * @param {string|!Element|{top: number, left: number}}
 * @returns {!MdPanelAnimation}
 */

/**
 * @ngdoc method
 * @name MdPanelAnimation#withAnimation
 * @description
 * Specifies the animation class.
 *
 * There are several default animations that can be used: `$mdPanel.animation.`
 *  - `SLIDE`: The panel slides in and out from the specified
 *       elements. It will not fade in or out.
 *  - `SCALE`: The panel scales in and out. Slide and fade are
 *       included in this animation.
 *  - `FADE`: The panel fades in and out.
 *
 * Custom classes will by default fade in and out unless
 * `transition: opacity 1ms` is added to the to custom class.
 *
 * @param {string|{open: string, close: string}} cssClass
 * @returns {!MdPanelAnimation}
 */

/**
 * @ngdoc method
 * @name MdPanelAnimation#duration
 * @description
 * Specifies the duration of the animation in milliseconds. The `duration`
 * method accepts either a number or an object with separate open and close
 * durations.
 *
 * @param {number|{open: number, close: number}} duration
 * @returns {!MdPanelAnimation}
 */


/*****************************************************************************
 *                            PUBLIC DOCUMENTATION                           *
 *****************************************************************************/


var MD_PANEL_Z_INDEX = 80;
var MD_PANEL_HIDDEN = '_md-panel-hidden';
var FOCUS_TRAP_TEMPLATE = angular.element(
    '<div class="_md-panel-focus-trap" tabindex="0"></div>');

var _presets = {};


/**
 * A provider that is used for creating presets for the panel API.
 * @final @constructor ngInject
 */
function MdPanelProvider() {
  return {
    'definePreset': definePreset,
    'getAllPresets': getAllPresets,
    'clearPresets': clearPresets,
    '$get': $getProvider()
  };
}


/**
 * Takes the passed in panel configuration object and adds it to the `_presets`
 * object at the specified name.
 * @param {string} name Name of the preset to set.
 * @param {!Object} preset Specific configuration object that can contain any
 *     and all of the parameters available within the `$mdPanel.create` method.
 *     However, parameters that pertain to id, position, animation, and user
 *     interaction are not allowed and will be removed from the preset
 *     configuration.
 */
function definePreset(name, preset) {
  if (!name || !preset) {
    throw new Error('mdPanelProvider: The panel preset definition is ' +
        'malformed. The name and preset object are required.');
  } else if (_presets.hasOwnProperty(name)) {
    throw new Error('mdPanelProvider: The panel preset you have requested ' +
        'has already been defined.');
  }

  // Delete any property on the preset that is not allowed.
  delete preset.id;
  delete preset.position;
  delete preset.animation;

  _presets[name] = preset;
}


/**
 * Gets a clone of the `_presets`.
 * @return {!Object}
 */
function getAllPresets() {
  return angular.copy(_presets);
}


/**
 * Clears all of the stored presets.
 */
function clearPresets() {
  _presets = {};
}


/**
 * Represents the `$get` method of the AngularJS provider. From here, a new
 * reference to the MdPanelService is returned where the needed arguments are
 * passed in including the MdPanelProvider `_presets`.
 * @param {!Object} _presets
 * @param {!angular.JQLite} $rootElement
 * @param {!angular.Scope} $rootScope
 * @param {!angular.$injector} $injector
 * @param {!angular.$window} $window
 */
function $getProvider() {
  return [
    '$rootElement', '$rootScope', '$injector', '$window',
    function($rootElement, $rootScope, $injector, $window) {
      return new MdPanelService(_presets, $rootElement, $rootScope,
          $injector, $window);
    }
  ];
}


/*****************************************************************************
 *                               MdPanel Service                             *
 *****************************************************************************/


/**
 * A service that is used for controlling/displaying panels on the screen.
 * @param {!Object} presets
 * @param {!angular.JQLite} $rootElement
 * @param {!angular.Scope} $rootScope
 * @param {!angular.$injector} $injector
 * @param {!angular.$window} $window
 * @final @constructor ngInject
 */
function MdPanelService(presets, $rootElement, $rootScope, $injector, $window) {
  /**
   * Default config options for the panel.
   * Anything angular related needs to be done later. Therefore
   *     scope: $rootScope.$new(true),
   *     attachTo: $rootElement,
   * are added later.
   * @private {!Object}
   */
  this._defaultConfigOptions = {
    bindToController: true,
    clickOutsideToClose: false,
    disableParentScroll: false,
    escapeToClose: false,
    focusOnOpen: true,
    fullscreen: false,
    hasBackdrop: false,
    propagateContainerEvents: false,
    transformTemplate: angular.bind(this, this._wrapTemplate),
    trapFocus: false,
    zIndex: MD_PANEL_Z_INDEX
  };

  /** @private {!Object} */
  this._config = {};

  /** @private {!Object} */
  this._presets = presets;

  /** @private @const */
  this._$rootElement = $rootElement;

  /** @private @const */
  this._$rootScope = $rootScope;

  /** @private @const */
  this._$injector = $injector;

  /** @private @const */
  this._$window = $window;

  /** @private @const */
  this._$mdUtil = this._$injector.get('$mdUtil');

  /** @private {!Object<string, !MdPanelRef>} */
  this._trackedPanels = {};

  /**
   * @private {!Object<string,
   *     {panels: !Array<!MdPanelRef>,
   *     openPanels: !Array<!MdPanelRef>,
   *     maxOpen: number}>}
   */
  this._groups = Object.create(null);

  /**
   * Default animations that can be used within the panel.
   * @type {enum}
   */
  this.animation = MdPanelAnimation.animation;

  /**
   * Possible values of xPosition for positioning the panel relative to
   * another element.
   * @type {enum}
   */
  this.xPosition = MdPanelPosition.xPosition;

  /**
   * Possible values of yPosition for positioning the panel relative to
   * another element.
   * @type {enum}
   */
  this.yPosition = MdPanelPosition.yPosition;

  /**
   * Possible values for the interceptors that can be registered on a panel.
   * @type {enum}
   */
  this.interceptorTypes = MdPanelRef.interceptorTypes;

  /**
   * Possible values for closing of a panel.
   * @type {enum}
   */
  this.closeReasons = MdPanelRef.closeReasons;

  /**
   * Possible values of absolute position.
   * @type {enum}
   */
  this.absPosition = MdPanelPosition.absPosition;
}


/**
 * Creates a panel with the specified options.
 * @param {string=} preset Name of a preset configuration that can be used to
 *     extend the panel configuration.
 * @param {!Object=} config Configuration object for the panel.
 * @returns {!MdPanelRef}
 */
MdPanelService.prototype.create = function(preset, config) {
  if (typeof preset === 'string') {
    preset = this._getPresetByName(preset);
  } else if (typeof preset === 'object' &&
      (angular.isUndefined(config) || !config)) {
    config = preset;
    preset = {};
  }

  preset = preset || {};
  config = config || {};

  // If the passed-in config contains an ID and the ID is within _trackedPanels,
  // return the tracked panel after updating its config with the passed-in
  // config.
  if (angular.isDefined(config.id) && this._trackedPanels[config.id]) {
    var trackedPanel = this._trackedPanels[config.id];
    angular.extend(trackedPanel.config, config);
    return trackedPanel;
  }

  // Combine the passed-in config, the _defaultConfigOptions, and the preset
  // configuration into the `_config`.
  this._config = angular.extend({
    // If no ID is set within the passed-in config, then create an arbitrary ID.
    id: config.id || 'panel_' + this._$mdUtil.nextUid(),
    scope: this._$rootScope.$new(true),
    attachTo: this._$rootElement
  }, this._defaultConfigOptions, config, preset);

  // Create the panelRef and add it to the `_trackedPanels` object.
  var panelRef = new MdPanelRef(this._config, this._$injector);
  this._trackedPanels[this._config.id] = panelRef;

  // Add the panel to each of its requested groups.
  if (this._config.groupName) {
    if (angular.isString(this._config.groupName)) {
      this._config.groupName = [this._config.groupName];
    }
    angular.forEach(this._config.groupName, function(group) {
      panelRef.addToGroup(group);
    });
  }

  this._config.scope.$on('$destroy', angular.bind(panelRef, panelRef.detach));

  return panelRef;
};


/**
 * Creates and opens a panel with the specified options.
 * @param {string=} preset Name of a preset configuration that can be used to
 *     extend the panel configuration.
 * @param {!Object=} config Configuration object for the panel.
 * @returns {!angular.$q.Promise<!MdPanelRef>} The panel created from create.
 */
MdPanelService.prototype.open = function(preset, config) {
  var panelRef = this.create(preset, config);
  return panelRef.open().then(function() {
    return panelRef;
  });
};


/**
 * Gets a specific preset configuration object saved within `_presets`.
 * @param {string} preset Name of the preset to search for.
 * @returns {!Object} The preset configuration object.
 */
MdPanelService.prototype._getPresetByName = function(preset) {
  if (!this._presets[preset]) {
    throw new Error('mdPanel: The panel preset configuration that you ' +
        'requested does not exist. Use the $mdPanelProvider to create a ' +
        'preset before requesting one.');
  }
  return this._presets[preset];
};


/**
 * Returns a new instance of the MdPanelPosition. Use this to create the
 * positioning object.
 * @returns {!MdPanelPosition}
 */
MdPanelService.prototype.newPanelPosition = function() {
  return new MdPanelPosition(this._$injector);
};


/**
 * Returns a new instance of the MdPanelAnimation. Use this to create the
 * animation object.
 * @returns {!MdPanelAnimation}
 */
MdPanelService.prototype.newPanelAnimation = function() {
  return new MdPanelAnimation(this._$injector);
};


/**
 * Creates a panel group and adds it to a tracked list of panel groups.
 * @param groupName {string} Name of the group to create.
 * @param config {!Object=} Specific configuration object that may contain the
 *     following properties:
 *
 *   - `maxOpen` - `{number=}`: The maximum number of panels that are allowed
 *     open within a defined panel group.
 *
 * @returns {!Object<string,
 *     {panels: !Array<!MdPanelRef>,
 *     openPanels: !Array<!MdPanelRef>,
 *     maxOpen: number}>} panelGroup
 */
MdPanelService.prototype.newPanelGroup = function(groupName, config) {
  if (!this._groups[groupName]) {
    config = config || {};
    var group = {
      panels: [],
      openPanels: [],
      maxOpen: config.maxOpen > 0 ? config.maxOpen : Infinity
    };
    this._groups[groupName] = group;
  }
  return this._groups[groupName];
};


/**
 * Sets the maximum number of panels in a group that can be opened at a given
 * time.
 * @param {string} groupName The name of the group to configure.
 * @param {number} maxOpen The maximum number of panels that can be
 *     opened. Infinity can be passed in to remove the maxOpen limit.
 */
MdPanelService.prototype.setGroupMaxOpen = function(groupName, maxOpen) {
  if (this._groups[groupName]) {
    this._groups[groupName].maxOpen = maxOpen;
  } else {
    throw new Error('mdPanel: Group does not exist yet. Call newPanelGroup().');
  }
};


/**
 * Determines if the current number of open panels within a group exceeds the
 * limit of allowed open panels.
 * @param {string} groupName The name of the group to check.
 * @returns {boolean} true if open count does exceed maxOpen and false if not.
 * @private
 */
MdPanelService.prototype._openCountExceedsMaxOpen = function(groupName) {
  if (this._groups[groupName]) {
    var group = this._groups[groupName];
    return group.maxOpen > 0 && group.openPanels.length > group.maxOpen;
  }
  return false;
};


/**
 * Closes the first open panel within a specific group.
 * @param {string} groupName The name of the group.
 * @private
 */
MdPanelService.prototype._closeFirstOpenedPanel = function(groupName) {
  this._groups[groupName].openPanels[0].close();
};


/**
 * Wraps the users template in two elements, md-panel-outer-wrapper, which
 * covers the entire attachTo element, and md-panel, which contains only the
 * template. This allows the panel control over positioning, animations,
 * and similar properties.
 * @param {string} origTemplate The original template.
 * @returns {string} The wrapped template.
 * @private
 */
MdPanelService.prototype._wrapTemplate = function(origTemplate) {
  var template = origTemplate || '';

  // The panel should be initially rendered offscreen so we can calculate
  // height and width for positioning.
  return '' +
      '<div class="md-panel-outer-wrapper">' +
      '  <div class="md-panel _md-panel-offscreen">' + template + '</div>' +
      '</div>';
};


/**
 * Wraps a content element in a md-panel-outer wrapper and
 * positions it off-screen. Allows for proper control over positoning
 * and animations.
 * @param {!angular.JQLite} contentElement Element to be wrapped.
 * @return {!angular.JQLite} Wrapper element.
 * @private
 */
MdPanelService.prototype._wrapContentElement = function(contentElement) {
  var wrapper = angular.element('<div class="md-panel-outer-wrapper">');

  contentElement.addClass('md-panel _md-panel-offscreen');
  wrapper.append(contentElement);

  return wrapper;
};


/*****************************************************************************
 *                                 MdPanelRef                                *
 *****************************************************************************/


/**
 * A reference to a created panel. This reference contains a unique id for the
 * panel, along with properties/functions used to control the panel.
 * @param {!Object} config
 * @param {!angular.$injector} $injector
 * @final @constructor
 */
function MdPanelRef(config, $injector) {
  // Injected variables.
  /** @private @const {!angular.$q} */
  this._$q = $injector.get('$q');

  /** @private @const {!angular.$mdCompiler} */
  this._$mdCompiler = $injector.get('$mdCompiler');

  /** @private @const {!angular.$mdConstant} */
  this._$mdConstant = $injector.get('$mdConstant');

  /** @private @const {!angular.$mdUtil} */
  this._$mdUtil = $injector.get('$mdUtil');

  /** @private @const {!angular.$mdTheming} */
  this._$mdTheming = $injector.get('$mdTheming');

  /** @private @const {!angular.Scope} */
  this._$rootScope = $injector.get('$rootScope');

  /** @private @const {!angular.$animate} */
  this._$animate = $injector.get('$animate');

  /** @private @const {!MdPanelRef} */
  this._$mdPanel = $injector.get('$mdPanel');

  /** @private @const {!angular.$log} */
  this._$log = $injector.get('$log');

  /** @private @const {!angular.$window} */
  this._$window = $injector.get('$window');

  /** @private @const {!Function} */
  this._$$rAF = $injector.get('$$rAF');

  // Public variables.
  /**
   * Unique id for the panelRef.
   * @type {string}
   */
  this.id = config.id;

  /** @type {!Object} */
  this.config = config;

  /** @type {!angular.JQLite|undefined} */
  this.panelContainer;

  /** @type {!angular.JQLite|undefined} */
  this.panelEl;

  /**
   * Whether the panel is attached. This is synchronous. When attach is called,
   * isAttached is set to true. When detach is called, isAttached is set to
   * false.
   * @type {boolean}
   */
  this.isAttached = false;

  // Private variables.
  /** @private {Array<function()>} */
  this._removeListeners = [];

  /** @private {!angular.JQLite|undefined} */
  this._topFocusTrap;

  /** @private {!angular.JQLite|undefined} */
  this._bottomFocusTrap;

  /** @private {!$mdPanel|undefined} */
  this._backdropRef;

  /** @private {Function?} */
  this._restoreScroll = null;

  /**
   * Keeps track of all the panel interceptors.
   * @private {!Object}
   */
  this._interceptors = Object.create(null);

  /**
   * Cleanup function, provided by `$mdCompiler` and assigned after the element
   * has been compiled. When `contentElement` is used, the function is used to
   * restore the element to it's proper place in the DOM.
   * @private {!Function}
   */
  this._compilerCleanup = null;

  /**
   * Cache for saving and restoring element inline styles, CSS classes etc.
   * @type {{styles: string, classes: string}}
   */
  this._restoreCache = {
    styles: '',
    classes: ''
  };
}


MdPanelRef.interceptorTypes = {
  CLOSE: 'onClose'
};


/**
 * Opens an already created and configured panel. If the panel is already
 * visible, does nothing.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel is opened and animations finish.
 */
MdPanelRef.prototype.open = function() {
  var self = this;
  return this._$q(function(resolve, reject) {
    var done = self._done(resolve, self);
    var show = self._simpleBind(self.show, self);
    var checkGroupMaxOpen = function() {
      if (self.config.groupName) {
        angular.forEach(self.config.groupName, function(group) {
          if (self._$mdPanel._openCountExceedsMaxOpen(group)) {
            self._$mdPanel._closeFirstOpenedPanel(group);
          }
        });
      }
    };

    self.attach()
        .then(show)
        .then(checkGroupMaxOpen)
        .then(done)
        .catch(reject);
  });
};


/**
 * Closes the panel.
 * @param {string} closeReason The event type that triggered the close.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel is closed and animations finish.
 */
MdPanelRef.prototype.close = function(closeReason) {
  var self = this;

  return this._$q(function(resolve, reject) {
    self._callInterceptors(MdPanelRef.interceptorTypes.CLOSE).then(function() {
      var done = self._done(resolve, self);
      var detach = self._simpleBind(self.detach, self);
      var onCloseSuccess = self.config['onCloseSuccess'] || angular.noop;
      onCloseSuccess = angular.bind(self, onCloseSuccess, self, closeReason);

      self.hide()
          .then(detach)
          .then(done)
          .then(onCloseSuccess)
          .catch(reject);
    }, reject);
  });
};


/**
 * Attaches the panel. The panel will be hidden afterwards.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel is attached.
 */
MdPanelRef.prototype.attach = function() {
  if (this.isAttached && this.panelEl) {
    return this._$q.when(this);
  }

  var self = this;
  return this._$q(function(resolve, reject) {
    var done = self._done(resolve, self);
    var onDomAdded = self.config['onDomAdded'] || angular.noop;
    var addListeners = function(response) {
      self.isAttached = true;
      self._addEventListeners();
      return response;
    };

    self._$q.all([
        self._createBackdrop(),
        self._createPanel()
            .then(addListeners)
            .catch(reject)
    ]).then(onDomAdded)
      .then(done)
      .catch(reject);
  });
};


/**
 * Only detaches the panel. Will NOT hide the panel first.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel is detached.
 */
MdPanelRef.prototype.detach = function() {
  if (!this.isAttached) {
    return this._$q.when(this);
  }

  var self = this;
  var onDomRemoved = self.config['onDomRemoved'] || angular.noop;

  var detachFn = function() {
    self._removeEventListeners();

    // Remove the focus traps that we added earlier for keeping focus within
    // the panel.
    if (self._topFocusTrap && self._topFocusTrap.parentNode) {
      self._topFocusTrap.parentNode.removeChild(self._topFocusTrap);
    }

    if (self._bottomFocusTrap && self._bottomFocusTrap.parentNode) {
      self._bottomFocusTrap.parentNode.removeChild(self._bottomFocusTrap);
    }

    if (self._restoreCache.classes) {
      self.panelEl[0].className = self._restoreCache.classes;
    }

    // Either restore the saved styles or clear the ones set by mdPanel.
    self.panelEl[0].style.cssText = self._restoreCache.styles || '';

    self._compilerCleanup();
    self.panelContainer.remove();
    self.isAttached = false;
    return self._$q.when(self);
  };

  if (this._restoreScroll) {
    this._restoreScroll();
    this._restoreScroll = null;
  }

  return this._$q(function(resolve, reject) {
    var done = self._done(resolve, self);

    self._$q.all([
      detachFn(),
      self._backdropRef ? self._backdropRef.detach() : true
    ]).then(onDomRemoved)
      .then(done)
      .catch(reject);
  });
};


/**
 * Destroys the panel. The Panel cannot be opened again after this.
 */
MdPanelRef.prototype.destroy = function() {
  var self = this;
  if (this.config.groupName) {
    angular.forEach(this.config.groupName, function(group) {
      self.removeFromGroup(group);
    });
  }
  this.config.scope.$destroy();
  this.config.locals = null;
  this.config.onDomAdded = null;
  this.config.onDomRemoved = null;
  this.config.onRemoving = null;
  this.config.onOpenComplete = null;
  this._interceptors = null;
};


/**
 * Shows the panel.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel has shown and animations finish.
 */
MdPanelRef.prototype.show = function() {
  if (!this.panelContainer) {
    return this._$q(function(resolve, reject) {
      reject('mdPanel: Panel does not exist yet. Call open() or attach().');
    });
  }

  if (!this.panelContainer.hasClass(MD_PANEL_HIDDEN)) {
    return this._$q.when(this);
  }

  var self = this;
  var animatePromise = function() {
    self.panelContainer.removeClass(MD_PANEL_HIDDEN);
    return self._animateOpen();
  };

  return this._$q(function(resolve, reject) {
    var done = self._done(resolve, self);
    var onOpenComplete = self.config['onOpenComplete'] || angular.noop;
    var addToGroupOpen = function() {
      if (self.config.groupName) {
        angular.forEach(self.config.groupName, function(group) {
          self._$mdPanel._groups[group].openPanels.push(self);
        });
      }
    };

    self._$q.all([
      self._backdropRef ? self._backdropRef.show() : self,
      animatePromise().then(function() { self._focusOnOpen(); }, reject)
    ]).then(onOpenComplete)
      .then(addToGroupOpen)
      .then(done)
      .catch(reject);
  });
};


/**
 * Hides the panel.
 * @returns {!angular.$q.Promise<!MdPanelRef>} A promise that is resolved when
 *     the panel has hidden and animations finish.
 */
MdPanelRef.prototype.hide = function() {
  if (!this.panelContainer) {
    return this._$q(function(resolve, reject) {
      reject('mdPanel: Panel does not exist yet. Call open() or attach().');
    });
  }

  if (this.panelContainer.hasClass(MD_PANEL_HIDDEN)) {
    return this._$q.when(this);
  }

  var self = this;

  return this._$q(function(resolve, reject) {
    var done = self._done(resolve, self);
    var onRemoving = self.config['onRemoving'] || angular.noop;
    var hidePanel = function() {
      self.panelContainer.addClass(MD_PANEL_HIDDEN);
    };
    var removeFromGroupOpen = function() {
      if (self.config.groupName) {
        var group, index;
        angular.forEach(self.config.groupName, function(group) {
          group = self._$mdPanel._groups[group];
          index = group.openPanels.indexOf(self);
          if (index > -1) {
            group.openPanels.splice(index, 1);
          }
        });
      }
    };
    var focusOnOrigin = function() {
      var origin = self.config['origin'];
      if (origin) {
        getElement(origin).focus();
      }
    };

    self._$q.all([
      self._backdropRef ? self._backdropRef.hide() : self,
      self._animateClose()
          .then(onRemoving)
          .then(hidePanel)
          .then(removeFromGroupOpen)
          .then(focusOnOrigin)
          .catch(reject)
    ]).then(done, reject);
  });
};


/**
 * Add a class to the panel. DO NOT use this to hide/show the panel.
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 *
 * @param {string} newClass Class to be added.
 * @param {boolean} toElement Whether or not to add the class to the panel
 *     element instead of the container.
 */
MdPanelRef.prototype.addClass = function(newClass, toElement) {
  this._$log.warn(
      'mdPanel: The addClass method is in the process of being deprecated. ' +
      'Full deprecation is scheduled for the AngularJS Material 1.2 release. ' +
      'To achieve the same results, use the panelContainer or panelEl ' +
      'JQLite elements that are referenced in MdPanelRef.');

  if (!this.panelContainer) {
    throw new Error(
        'mdPanel: Panel does not exist yet. Call open() or attach().');
  }

  if (!toElement && !this.panelContainer.hasClass(newClass)) {
    this.panelContainer.addClass(newClass);
  } else if (toElement && !this.panelEl.hasClass(newClass)) {
    this.panelEl.addClass(newClass);
  }
};


/**
 * Remove a class from the panel. DO NOT use this to hide/show the panel.
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 *
 * @param {string} oldClass Class to be removed.
 * @param {boolean} fromElement Whether or not to remove the class from the
 *     panel element instead of the container.
 */
MdPanelRef.prototype.removeClass = function(oldClass, fromElement) {
  this._$log.warn(
      'mdPanel: The removeClass method is in the process of being deprecated. ' +
      'Full deprecation is scheduled for the AngularJS Material 1.2 release. ' +
      'To achieve the same results, use the panelContainer or panelEl ' +
      'JQLite elements that are referenced in MdPanelRef.');

  if (!this.panelContainer) {
    throw new Error(
        'mdPanel: Panel does not exist yet. Call open() or attach().');
  }

  if (!fromElement && this.panelContainer.hasClass(oldClass)) {
    this.panelContainer.removeClass(oldClass);
  } else if (fromElement && this.panelEl.hasClass(oldClass)) {
    this.panelEl.removeClass(oldClass);
  }
};


/**
 * Toggle a class on the panel. DO NOT use this to hide/show the panel.
 * @deprecated
 * This method is in the process of being deprecated in favor of using the panel
 * and container JQLite elements that are referenced in the MdPanelRef object.
 * Full deprecation is scheduled for material 1.2.
 *
 * @param {string} toggleClass The class to toggle.
 * @param {boolean} onElement Whether or not to toggle the class on the panel
 *     element instead of the container.
 */
MdPanelRef.prototype.toggleClass = function(toggleClass, onElement) {
  this._$log.warn(
      'mdPanel: The toggleClass method is in the process of being deprecated. ' +
      'Full deprecation is scheduled for the AngularJS Material 1.2 release. ' +
      'To achieve the same results, use the panelContainer or panelEl ' +
      'JQLite elements that are referenced in MdPanelRef.');

  if (!this.panelContainer) {
    throw new Error(
        'mdPanel: Panel does not exist yet. Call open() or attach().');
  }

  if (!onElement) {
    this.panelContainer.toggleClass(toggleClass);
  } else {
    this.panelEl.toggleClass(toggleClass);
  }
};


/**
 * Compiles the panel, according to the passed in config and appends it to
 * the DOM. Helps normalize differences in the compilation process between
 * using a string template and a content element.
 * @returns {!angular.$q.Promise<!MdPanelRef>} Promise that is resolved when
 *     the element has been compiled and added to the DOM.
 * @private
 */
MdPanelRef.prototype._compile = function() {
  var self = this;

  // Compile the element via $mdCompiler. Note that when using a
  // contentElement, the element isn't actually being compiled, rather the
  // compiler saves it's place in the DOM and provides a way of restoring it.
  return self._$mdCompiler.compile(self.config).then(function(compileData) {
    var config = self.config;

    if (config.contentElement) {
      var panelEl = compileData.element;

      // Since mdPanel modifies the inline styles and CSS classes, we need
      // to save them in order to be able to restore on close.
      self._restoreCache.styles = panelEl[0].style.cssText;
      self._restoreCache.classes = panelEl[0].className;

      self.panelContainer = self._$mdPanel._wrapContentElement(panelEl);
      self.panelEl = panelEl;
    } else {
      self.panelContainer = compileData.link(config['scope']);
      self.panelEl = angular.element(
        self.panelContainer[0].querySelector('.md-panel')
      );
    }

    // Save a reference to the cleanup function from the compiler.
    self._compilerCleanup = compileData.cleanup;

    // Attach the panel to the proper place in the DOM.
    getElement(self.config['attachTo']).append(self.panelContainer);

    return self;
  });
};


/**
 * Creates a panel and adds it to the dom.
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel is
 *     created.
 * @private
 */
MdPanelRef.prototype._createPanel = function() {
  var self = this;

  return this._$q(function(resolve, reject) {
    if (!self.config.locals) {
      self.config.locals = {};
    }

    self.config.locals.mdPanelRef = self;

    self._compile().then(function() {
      if (self.config['disableParentScroll']) {
        self._restoreScroll = self._$mdUtil.disableScrollAround(
          null,
          self.panelContainer,
          { disableScrollMask: true }
        );
      }

      // Add a custom CSS class to the panel element.
      if (self.config['panelClass']) {
        self.panelEl.addClass(self.config['panelClass']);
      }

      // Handle click and touch events for the panel container.
      if (self.config['propagateContainerEvents']) {
        self.panelContainer.css('pointer-events', 'none');
        self.panelEl.css('pointer-events', 'all');
      }

      // Panel may be outside the $rootElement, tell ngAnimate to animate
      // regardless.
      if (self._$animate.pin) {
        self._$animate.pin(
          self.panelContainer,
          getElement(self.config['attachTo'])
        );
      }

      self._configureTrapFocus();
      self._addStyles().then(function() {
        resolve(self);
      }, reject);
    }, reject);

  });
};


/**
 * Adds the styles for the panel, such as positioning and z-index. Also,
 * themes the panel element and panel container using `$mdTheming`.
 * @returns {!angular.$q.Promise<!MdPanelRef>}
 * @private
 */
MdPanelRef.prototype._addStyles = function() {
  var self = this;
  return this._$q(function(resolve) {
    self.panelContainer.css('z-index', self.config['zIndex']);
    self.panelEl.css('z-index', self.config['zIndex'] + 1);

    var hideAndResolve = function() {
      // Theme the element and container.
      self._setTheming();

      // Remove offscreen class and add hidden class.
      self.panelEl.removeClass('_md-panel-offscreen');
      self.panelContainer.addClass(MD_PANEL_HIDDEN);

      resolve(self);
    };

    if (self.config['fullscreen']) {
      self.panelEl.addClass('_md-panel-fullscreen');
      hideAndResolve();
      return; // Don't setup positioning.
    }

    var positionConfig = self.config['position'];
    if (!positionConfig) {
      hideAndResolve();
      return; // Don't setup positioning.
    }

    // Wait for angular to finish processing the template
    self._$rootScope['$$postDigest'](function() {
      // Position it correctly. This is necessary so that the panel will have a
      // defined height and width.
      self._updatePosition(true);

      // Theme the element and container.
      self._setTheming();

      resolve(self);
    });
  });
};


/**
 * Sets the `$mdTheming` classes on the `panelContainer` and `panelEl`.
 * @private
 */
MdPanelRef.prototype._setTheming = function() {
  this._$mdTheming(this.panelEl);
  this._$mdTheming(this.panelContainer);
};


/**
 * Updates the position configuration of a panel
 * @param {!MdPanelPosition} position
 */
MdPanelRef.prototype.updatePosition = function(position) {
  if (!this.panelContainer) {
    throw new Error(
        'mdPanel: Panel does not exist yet. Call open() or attach().');
  }

  this.config['position'] = position;
  this._updatePosition();
};


/**
 * Calculates and updates the position of the panel.
 * @param {boolean=} init
 * @private
 */
MdPanelRef.prototype._updatePosition = function(init) {
  var positionConfig = this.config['position'];

  if (positionConfig) {
    positionConfig._setPanelPosition(this.panelEl);

    // Hide the panel now that position is known.
    if (init) {
      this.panelEl.removeClass('_md-panel-offscreen');
      this.panelContainer.addClass(MD_PANEL_HIDDEN);
    }

    this.panelEl.css(
      MdPanelPosition.absPosition.TOP,
      positionConfig.getTop()
    );
    this.panelEl.css(
      MdPanelPosition.absPosition.BOTTOM,
      positionConfig.getBottom()
    );
    this.panelEl.css(
      MdPanelPosition.absPosition.LEFT,
      positionConfig.getLeft()
    );
    this.panelEl.css(
      MdPanelPosition.absPosition.RIGHT,
      positionConfig.getRight()
    );
  }
};


/**
 * Focuses on the panel or the first focus target.
 * @private
 */
MdPanelRef.prototype._focusOnOpen = function() {
  if (this.config['focusOnOpen']) {
    // Wait for the template to finish rendering to guarantee md-autofocus has
    // finished adding the class md-autofocus, otherwise the focusable element
    // isn't available to focus.
    var self = this;
    this._$rootScope['$$postDigest'](function() {
      var target = self._$mdUtil.findFocusTarget(self.panelEl) ||
          self.panelEl;
      target.focus();
    });
  }
};


/**
 * Shows the backdrop.
 * @returns {!angular.$q.Promise} A promise that is resolved when the backdrop
 *     is created and attached.
 * @private
 */
MdPanelRef.prototype._createBackdrop = function() {
  if (this.config.hasBackdrop) {
    if (!this._backdropRef) {
      var backdropAnimation = this._$mdPanel.newPanelAnimation()
          .openFrom(this.config.attachTo)
          .withAnimation({
            open: '_md-opaque-enter',
            close: '_md-opaque-leave'
          });

      if (this.config.animation) {
        backdropAnimation.duration(this.config.animation._rawDuration);
      }

      var backdropConfig = {
        animation: backdropAnimation,
        attachTo: this.config.attachTo,
        focusOnOpen: false,
        panelClass: '_md-panel-backdrop',
        zIndex: this.config.zIndex - 1
      };

      this._backdropRef = this._$mdPanel.create(backdropConfig);
    }
    if (!this._backdropRef.isAttached) {
      return this._backdropRef.attach();
    }
  }
};


/**
 * Listen for escape keys and outside clicks to auto close.
 * @private
 */
MdPanelRef.prototype._addEventListeners = function() {
  this._configureEscapeToClose();
  this._configureClickOutsideToClose();
  this._configureScrollListener();
};


/**
 * Remove event listeners added in _addEventListeners.
 * @private
 */
MdPanelRef.prototype._removeEventListeners = function() {
  this._removeListeners && this._removeListeners.forEach(function(removeFn) {
    removeFn();
  });
  this._removeListeners = [];
};


/**
 * Setup the escapeToClose event listeners.
 * @private
 */
MdPanelRef.prototype._configureEscapeToClose = function() {
  if (this.config['escapeToClose']) {
    var parentTarget = getElement(this.config['attachTo']);
    var self = this;

    var keyHandlerFn = function(ev) {
      if (ev.keyCode === self._$mdConstant.KEY_CODE.ESCAPE) {
        ev.stopPropagation();
        ev.preventDefault();

        self.close(MdPanelRef.closeReasons.ESCAPE);
      }
    };

    // Add keydown listeners
    this.panelContainer.on('keydown', keyHandlerFn);
    parentTarget.on('keydown', keyHandlerFn);

    // Queue remove listeners function
    this._removeListeners.push(function() {
      self.panelContainer.off('keydown', keyHandlerFn);
      parentTarget.off('keydown', keyHandlerFn);
    });
  }
};


/**
 * Setup the clickOutsideToClose event listeners.
 * @private
 */
MdPanelRef.prototype._configureClickOutsideToClose = function() {
  if (this.config['clickOutsideToClose']) {
    var target = this.config['propagateContainerEvents'] ?
        angular.element(document.body) :
        this.panelContainer;
    var sourceEl;

    // Keep track of the element on which the mouse originally went down
    // so that we can only close the backdrop when the 'click' started on it.
    // A simple 'click' handler does not work, it sets the target object as the
    // element the mouse went down on.
    var mousedownHandler = function(ev) {
      sourceEl = ev.target;
    };

    // We check if our original element and the target is the backdrop
    // because if the original was the backdrop and the target was inside the
    // panel we don't want to panel to close.
    var self = this;
    var mouseupHandler = function(ev) {
      if (self.config['propagateContainerEvents']) {

        // We check if the sourceEl of the event is the panel element or one
        // of it's children. If it is not, then close the panel.
        if (sourceEl !== self.panelEl[0] && !self.panelEl[0].contains(sourceEl)) {
          self.close();
        }

      } else if (sourceEl === target[0] && ev.target === target[0]) {
        ev.stopPropagation();
        ev.preventDefault();

        self.close(MdPanelRef.closeReasons.CLICK_OUTSIDE);
      }
    };

    // Add listeners
    target.on('mousedown', mousedownHandler);
    target.on('mouseup', mouseupHandler);

    // Queue remove listeners function
    this._removeListeners.push(function() {
      target.off('mousedown', mousedownHandler);
      target.off('mouseup', mouseupHandler);
    });
  }
};


/**
 * Configures the listeners for updating the panel position on scroll.
 * @private
*/
MdPanelRef.prototype._configureScrollListener = function() {
  // No need to bind the event if scrolling is disabled.
  if (!this.config['disableParentScroll']) {
    var updatePosition = angular.bind(this, this._updatePosition);
    var debouncedUpdatePosition = this._$$rAF.throttle(updatePosition);
    var self = this;

    var onScroll = function() {
      debouncedUpdatePosition();
    };

    // Add listeners.
    this._$window.addEventListener('scroll', onScroll, true);

    // Queue remove listeners function.
    this._removeListeners.push(function() {
      self._$window.removeEventListener('scroll', onScroll, true);
    });
  }
};


/**
 * Setup the focus traps. These traps will wrap focus when tabbing past the
 * panel. When shift-tabbing, the focus will stick in place.
 * @private
 */
MdPanelRef.prototype._configureTrapFocus = function() {
  // Focus doesn't remain inside of the panel without this.
  this.panelEl.attr('tabIndex', '-1');
  if (this.config['trapFocus']) {
    var element = this.panelEl;
    // Set up elements before and after the panel to capture focus and
    // redirect back into the panel.
    this._topFocusTrap = FOCUS_TRAP_TEMPLATE.clone()[0];
    this._bottomFocusTrap = FOCUS_TRAP_TEMPLATE.clone()[0];

    // When focus is about to move out of the panel, we want to intercept it
    // and redirect it back to the panel element.
    var focusHandler = function() {
      element.focus();
    };
    this._topFocusTrap.addEventListener('focus', focusHandler);
    this._bottomFocusTrap.addEventListener('focus', focusHandler);

    // Queue remove listeners function
    this._removeListeners.push(this._simpleBind(function() {
      this._topFocusTrap.removeEventListener('focus', focusHandler);
      this._bottomFocusTrap.removeEventListener('focus', focusHandler);
    }, this));

    // The top focus trap inserted immediately before the md-panel element (as
    // a sibling). The bottom focus trap inserted immediately after the
    // md-panel element (as a sibling).
    element[0].parentNode.insertBefore(this._topFocusTrap, element[0]);
    element.after(this._bottomFocusTrap);
  }
};


/**
 * Updates the animation of a panel.
 * @param {!MdPanelAnimation} animation
 */
MdPanelRef.prototype.updateAnimation = function(animation) {
  this.config['animation'] = animation;

  if (this._backdropRef) {
    this._backdropRef.config.animation.duration(animation._rawDuration);
  }
};


/**
 * Animate the panel opening.
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel has
 *     animated open.
 * @private
 */
MdPanelRef.prototype._animateOpen = function() {
  this.panelContainer.addClass('md-panel-is-showing');
  var animationConfig = this.config['animation'];
  if (!animationConfig) {
    // Promise is in progress, return it.
    this.panelContainer.addClass('_md-panel-shown');
    return this._$q.when(this);
  }

  var self = this;
  return this._$q(function(resolve) {
    var done = self._done(resolve, self);
    var warnAndOpen = function() {
      self._$log.warn(
          'mdPanel: MdPanel Animations failed. ' +
          'Showing panel without animating.');
      done();
    };

    animationConfig.animateOpen(self.panelEl)
        .then(done, warnAndOpen);
  });
};


/**
 * Animate the panel closing.
 * @returns {!angular.$q.Promise} A promise that is resolved when the panel has
 *     animated closed.
 * @private
 */
MdPanelRef.prototype._animateClose = function() {
  var animationConfig = this.config['animation'];
  if (!animationConfig) {
    this.panelContainer.removeClass('md-panel-is-showing');
    this.panelContainer.removeClass('_md-panel-shown');
    return this._$q.when(this);
  }

  var self = this;
  return this._$q(function(resolve) {
    var done = function() {
      self.panelContainer.removeClass('md-panel-is-showing');
      resolve(self);
    };
    var warnAndClose = function() {
      self._$log.warn(
          'mdPanel: MdPanel Animations failed. ' +
          'Hiding panel without animating.');
      done();
    };

    animationConfig.animateClose(self.panelEl)
        .then(done, warnAndClose);
  });
};


/**
 * Registers a interceptor with the panel. The callback should return a promise,
 * which will allow the action to continue when it gets resolved, or will
 * prevent an action if it is rejected.
 * @param {string} type Type of interceptor.
 * @param {!angular.$q.Promise<!any>} callback Callback to be registered.
 * @returns {!MdPanelRef}
 */
MdPanelRef.prototype.registerInterceptor = function(type, callback) {
  var error = null;

  if (!angular.isString(type)) {
    error = 'Interceptor type must be a string, instead got ' + typeof type;
  } else if (!angular.isFunction(callback)) {
    error = 'Interceptor callback must be a function, instead got ' + typeof callback;
  }

  if (error) {
    throw new Error('MdPanel: ' + error);
  }

  var interceptors = this._interceptors[type] = this._interceptors[type] || [];

  if (interceptors.indexOf(callback) === -1) {
    interceptors.push(callback);
  }

  return this;
};


/**
 * Removes a registered interceptor.
 * @param {string} type Type of interceptor to be removed.
 * @param {Function} callback Interceptor to be removed.
 * @returns {!MdPanelRef}
 */
MdPanelRef.prototype.removeInterceptor = function(type, callback) {
  var index = this._interceptors[type] ?
    this._interceptors[type].indexOf(callback) : -1;

  if (index > -1) {
    this._interceptors[type].splice(index, 1);
  }

  return this;
};


/**
 * Removes all interceptors.
 * @param {string=} type Type of interceptors to be removed.
 *     If ommited, all interceptors types will be removed.
 * @returns {!MdPanelRef}
 */
MdPanelRef.prototype.removeAllInterceptors = function(type) {
  if (type) {
    this._interceptors[type] = [];
  } else {
    this._interceptors = Object.create(null);
  }

  return this;
};


/**
 * Invokes all the interceptors of a certain type sequantially in
 *     reverse order. Works in a similar way to `$q.all`, except it
 *     respects the order of the functions.
 * @param {string} type Type of interceptors to be invoked.
 * @returns {!angular.$q.Promise<!MdPanelRef>}
 * @private
 */
MdPanelRef.prototype._callInterceptors = function(type) {
  var self = this;
  var $q = self._$q;
  var interceptors = self._interceptors && self._interceptors[type] || [];

  return interceptors.reduceRight(function(promise, interceptor) {
    var isPromiseLike = interceptor && angular.isFunction(interceptor.then);
    var response = isPromiseLike ? interceptor : null;

    /**
    * For interceptors to reject/cancel subsequent portions of the chain, simply
    * return a `$q.reject(<value>)`
    */
    return promise.then(function() {
      if (!response) {
        try {
          response = interceptor(self);
        } catch (e) {
          response = $q.reject(e);
        }
      }

     return response;
    });
  }, $q.resolve(self));
};


/**
 * Faster, more basic than angular.bind
 * http://jsperf.com/angular-bind-vs-custom-vs-native
 * @param {function} callback
 * @param {!Object} self
 * @return {function} Callback function with a bound self.
 */
MdPanelRef.prototype._simpleBind = function(callback, self) {
  return function(value) {
    return callback.apply(self, value);
  };
};


/**
 * @param {function} callback
 * @param {!Object} self
 * @return {function} Callback function with a self param.
 */
MdPanelRef.prototype._done = function(callback, self) {
  return function() {
    callback(self);
  };
};


/**
 * Adds a panel to a group if the panel does not exist within the group already.
 * A panel can only exist within a single group.
 * @param {string} groupName The name of the group.
 */
MdPanelRef.prototype.addToGroup = function(groupName) {
  if (!this._$mdPanel._groups[groupName]) {
    this._$mdPanel.newPanelGroup(groupName);
  }

  var group = this._$mdPanel._groups[groupName];
  var index = group.panels.indexOf(this);

  if (index < 0) {
    group.panels.push(this);
  }
};


/**
 * Removes a panel from a group if the panel exists within that group. The group
 * must be created ahead of time.
 * @param {string} groupName The name of the group.
 */
MdPanelRef.prototype.removeFromGroup = function(groupName) {
  if (!this._$mdPanel._groups[groupName]) {
    throw new Error('mdPanel: The group ' + groupName + ' does not exist.');
  }

  var group = this._$mdPanel._groups[groupName];
  var index = group.panels.indexOf(this);

  if (index > -1) {
    group.panels.splice(index, 1);
  }
};


/**
 * Possible default closeReasons for the close function.
 * @enum {string}
 */
MdPanelRef.closeReasons = {
  CLICK_OUTSIDE: 'clickOutsideToClose',
  ESCAPE: 'escapeToClose',
};


/*****************************************************************************
 *                               MdPanelPosition                             *
 *****************************************************************************/


/**
 * Position configuration object. To use, create an MdPanelPosition with the
 * desired properties, then pass the object as part of $mdPanel creation.
 *
 * Example:
 *
 * var panelPosition = new MdPanelPosition()
 *     .relativeTo(myButtonEl)
 *     .addPanelPosition(
 *       $mdPanel.xPosition.CENTER,
 *       $mdPanel.yPosition.ALIGN_TOPS
 *     );
 *
 * $mdPanel.create({
 *   position: panelPosition
 * });
 *
 * @param {!angular.$injector} $injector
 * @final @constructor
 */
function MdPanelPosition($injector) {
  /** @private @const {!angular.$window} */
  this._$window = $injector.get('$window');

  /** @private {boolean} */
  this._isRTL = $injector.get('$mdUtil').bidi() === 'rtl';

  /** @private @const {!angular.$mdConstant} */
  this._$mdConstant = $injector.get('$mdConstant');

  /** @private {boolean} */
  this._absolute = false;

  /** @private {!angular.JQLite} */
  this._relativeToEl;

  /** @private {string} */
  this._top = '';

  /** @private {string} */
  this._bottom = '';

  /** @private {string} */
  this._left = '';

  /** @private {string} */
  this._right = '';

  /** @private {!Array<string>} */
  this._translateX = [];

  /** @private {!Array<string>} */
  this._translateY = [];

  /** @private {!Array<{x:string, y:string}>} */
  this._positions = [];

  /** @private {?{x:string, y:string}} */
  this._actualPosition;
}


/**
 * Possible values of xPosition.
 * @enum {string}
 */
MdPanelPosition.xPosition = {
  CENTER: 'center',
  ALIGN_START: 'align-start',
  ALIGN_END: 'align-end',
  OFFSET_START: 'offset-start',
  OFFSET_END: 'offset-end'
};


/**
 * Possible values of yPosition.
 * @enum {string}
 */
MdPanelPosition.yPosition = {
  CENTER: 'center',
  ALIGN_TOPS: 'align-tops',
  ALIGN_BOTTOMS: 'align-bottoms',
  ABOVE: 'above',
  BELOW: 'below'
};


/**
 * Possible values of absolute position.
 * @enum {string}
 */
MdPanelPosition.absPosition = {
  TOP: 'top',
  RIGHT: 'right',
  BOTTOM: 'bottom',
  LEFT: 'left'
};

/**
 * Margin between the edges of a panel and the viewport.
 * @const {number}
 */
MdPanelPosition.viewportMargin = 8;


/**
 * Sets absolute positioning for the panel.
 * @return {!MdPanelPosition}
 */
MdPanelPosition.prototype.absolute = function() {
  this._absolute = true;
  return this;
};


/**
 * Sets the value of a position for the panel. Clears any previously set
 * position.
 * @param {string} position Position to set
 * @param {string=} value Value of the position. Defaults to '0'.
 * @returns {!MdPanelPosition}
 * @private
 */
MdPanelPosition.prototype._setPosition = function(position, value) {
  if (position === MdPanelPosition.absPosition.RIGHT ||
      position === MdPanelPosition.absPosition.LEFT) {
    this._left = this._right = '';
  } else if (
      position === MdPanelPosition.absPosition.BOTTOM ||
      position === MdPanelPosition.absPosition.TOP) {
    this._top = this._bottom = '';
  } else {
    var positions = Object.keys(MdPanelPosition.absPosition).join()
        .toLowerCase();

    throw new Error('mdPanel: Position must be one of ' + positions + '.');
  }

  this['_' +  position] = angular.isString(value) ? value : '0';

  return this;
};


/**
 * Sets the value of `top` for the panel. Clears any previously set vertical
 * position.
 * @param {string=} top Value of `top`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.top = function(top) {
  return this._setPosition(MdPanelPosition.absPosition.TOP, top);
};


/**
 * Sets the value of `bottom` for the panel. Clears any previously set vertical
 * position.
 * @param {string=} bottom Value of `bottom`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.bottom = function(bottom) {
  return this._setPosition(MdPanelPosition.absPosition.BOTTOM, bottom);
};


/**
 * Sets the panel to the start of the page - `left` if `ltr` or `right` for
 * `rtl`. Clears any previously set horizontal position.
 * @param {string=} start Value of position. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.start = function(start) {
  var position = this._isRTL ? MdPanelPosition.absPosition.RIGHT : MdPanelPosition.absPosition.LEFT;
  return this._setPosition(position, start);
};


/**
 * Sets the panel to the end of the page - `right` if `ltr` or `left` for `rtl`.
 * Clears any previously set horizontal position.
 * @param {string=} end Value of position. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.end = function(end) {
  var position = this._isRTL ? MdPanelPosition.absPosition.LEFT : MdPanelPosition.absPosition.RIGHT;
  return this._setPosition(position, end);
};


/**
 * Sets the value of `left` for the panel. Clears any previously set
 * horizontal position.
 * @param {string=} left Value of `left`. Defaults to '0'.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.left = function(left) {
  return this._setPosition(MdPanelPosition.absPosition.LEFT, left);
};


/**
 * Sets the value of `right` for the panel. Clears any previously set
 * horizontal position.
 * @param {string=} right Value of `right`. Defaults to '0'.
 * @returns {!MdPanelPosition}
*/
MdPanelPosition.prototype.right = function(right) {
  return this._setPosition(MdPanelPosition.absPosition.RIGHT, right);
};


/**
 * Centers the panel horizontally in the viewport. Clears any previously set
 * horizontal position.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.centerHorizontally = function() {
  this._left = '50%';
  this._right = '';
  this._translateX = ['-50%'];
  return this;
};


/**
 * Centers the panel vertically in the viewport. Clears any previously set
 * vertical position.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.centerVertically = function() {
  this._top = '50%';
  this._bottom = '';
  this._translateY = ['-50%'];
  return this;
};


/**
 * Centers the panel horizontally and vertically in the viewport. This is
 * equivalent to calling both `centerHorizontally` and `centerVertically`.
 * Clears any previously set horizontal and vertical positions.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.center = function() {
  return this.centerHorizontally().centerVertically();
};


/**
 * Sets element for relative positioning.
 * @param {string|!Element|!angular.JQLite} element Query selector, DOM element,
 *     or angular element to set the panel relative to.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.relativeTo = function(element) {
  this._absolute = false;
  this._relativeToEl = getElement(element);
  return this;
};


/**
 * Sets the x and y positions for the panel relative to another element.
 * @param {string} xPosition must be one of the MdPanelPosition.xPosition
 *     values.
 * @param {string} yPosition must be one of the MdPanelPosition.yPosition
 *     values.
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.addPanelPosition = function(xPosition, yPosition) {
  if (!this._relativeToEl) {
    throw new Error('mdPanel: addPanelPosition can only be used with ' +
        'relative positioning. Set relativeTo first.');
  }

  this._validateXPosition(xPosition);
  this._validateYPosition(yPosition);

  this._positions.push({
      x: xPosition,
      y: yPosition,
  });
  return this;
};


/**
 * Ensures that yPosition is a valid position name. Throw an exception if not.
 * @param {string} yPosition
 */
MdPanelPosition.prototype._validateYPosition = function(yPosition) {
  // empty is ok
  if (yPosition == null) {
      return;
  }

  var positionKeys = Object.keys(MdPanelPosition.yPosition);
  var positionValues = [];
  for (var key, i = 0; key = positionKeys[i]; i++) {
    var position = MdPanelPosition.yPosition[key];
    positionValues.push(position);

    if (position === yPosition) {
      return;
    }
  }

  throw new Error('mdPanel: Panel y position only accepts the following ' +
      'values:\n' + positionValues.join(' | '));
};


/**
 * Ensures that xPosition is a valid position name. Throw an exception if not.
 * @param {string} xPosition
 */
MdPanelPosition.prototype._validateXPosition = function(xPosition) {
  // empty is ok
  if (xPosition == null) {
      return;
  }

  var positionKeys = Object.keys(MdPanelPosition.xPosition);
  var positionValues = [];
  for (var key, i = 0; key = positionKeys[i]; i++) {
    var position = MdPanelPosition.xPosition[key];
    positionValues.push(position);
    if (position === xPosition) {
      return;
    }
  }

  throw new Error('mdPanel: Panel x Position only accepts the following ' +
      'values:\n' + positionValues.join(' | '));
};


/**
 * Sets the value of the offset in the x-direction. This will add to any
 * previously set offsets.
 * @param {string|number|function(MdPanelPosition): string} offsetX
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.withOffsetX = function(offsetX) {
  this._translateX.push(addUnits(offsetX));
  return this;
};


/**
 * Sets the value of the offset in the y-direction. This will add to any
 * previously set offsets.
 * @param {string|number|function(MdPanelPosition): string} offsetY
 * @returns {!MdPanelPosition}
 */
MdPanelPosition.prototype.withOffsetY = function(offsetY) {
  this._translateY.push(addUnits(offsetY));
  return this;
};


/**
 * Gets the value of `top` for the panel.
 * @returns {string}
 */
MdPanelPosition.prototype.getTop = function() {
  return this._top;
};


/**
 * Gets the value of `bottom` for the panel.
 * @returns {string}
 */
MdPanelPosition.prototype.getBottom = function() {
  return this._bottom;
};


/**
 * Gets the value of `left` for the panel.
 * @returns {string}
 */
MdPanelPosition.prototype.getLeft = function() {
  return this._left;
};


/**
 * Gets the value of `right` for the panel.
 * @returns {string}
 */
MdPanelPosition.prototype.getRight = function() {
  return this._right;
};


/**
 * Gets the value of `transform` for the panel.
 * @returns {string}
 */
MdPanelPosition.prototype.getTransform = function() {
  var translateX = this._reduceTranslateValues('translateX', this._translateX);
  var translateY = this._reduceTranslateValues('translateY', this._translateY);

  // It's important to trim the result, because the browser will ignore the set
  // operation if the string contains only whitespace.
  return (translateX + ' ' + translateY).trim();
};


/**
 * Sets the `transform` value for a panel element.
 * @param {!angular.JQLite} panelEl
 * @returns {!angular.JQLite}
 * @private
 */
MdPanelPosition.prototype._setTransform = function(panelEl) {
  return panelEl.css(this._$mdConstant.CSS.TRANSFORM, this.getTransform());
};


/**
 * True if the panel is completely on-screen with this positioning; false
 * otherwise.
 * @param {!angular.JQLite} panelEl
 * @return {boolean}
 * @private
 */
MdPanelPosition.prototype._isOnscreen = function(panelEl) {
  // this works because we always use fixed positioning for the panel,
  // which is relative to the viewport.
  var left = parseInt(this.getLeft());
  var top = parseInt(this.getTop());

  if (this._translateX.length || this._translateY.length) {
    var prefixedTransform = this._$mdConstant.CSS.TRANSFORM;
    var offsets = getComputedTranslations(panelEl, prefixedTransform);
    left += offsets.x;
    top += offsets.y;
  }

  var right = left + panelEl[0].offsetWidth;
  var bottom = top + panelEl[0].offsetHeight;

  return (left >= 0) &&
    (top >= 0) &&
    (bottom <= this._$window.innerHeight) &&
    (right <= this._$window.innerWidth);
};


/**
 * Gets the first x/y position that can fit on-screen.
 * @returns {{x: string, y: string}}
 */
MdPanelPosition.prototype.getActualPosition = function() {
  return this._actualPosition;
};


/**
 * Reduces a list of translate values to a string that can be used within
 * transform.
 * @param {string} translateFn
 * @param {!Array<string>} values
 * @returns {string}
 * @private
 */
MdPanelPosition.prototype._reduceTranslateValues =
    function(translateFn, values) {
      return values.map(function(translation) {
        var translationValue = angular.isFunction(translation) ?
            addUnits(translation(this)) : translation;
        return translateFn + '(' + translationValue + ')';
      }, this).join(' ');
    };


/**
 * Sets the panel position based on the created panel element and best x/y
 * positioning.
 * @param {!angular.JQLite} panelEl
 * @private
 */
MdPanelPosition.prototype._setPanelPosition = function(panelEl) {
  // Remove the "position adjusted" class in case it has been added before.
  panelEl.removeClass('_md-panel-position-adjusted');

  // Only calculate the position if necessary.
  if (this._absolute) {
    this._setTransform(panelEl);
    return;
  }

  if (this._actualPosition) {
    this._calculatePanelPosition(panelEl, this._actualPosition);
    this._setTransform(panelEl);
    this._constrainToViewport(panelEl);
    return;
  }

  for (var i = 0; i < this._positions.length; i++) {
    this._actualPosition = this._positions[i];
    this._calculatePanelPosition(panelEl, this._actualPosition);
    this._setTransform(panelEl);

    if (this._isOnscreen(panelEl)) {
      return;
    }
  }

  this._constrainToViewport(panelEl);
};


/**
 * Constrains a panel's position to the viewport.
 * @param {!angular.JQLite} panelEl
 * @private
 */
MdPanelPosition.prototype._constrainToViewport = function(panelEl) {
  var margin = MdPanelPosition.viewportMargin;
  var initialTop = this._top;
  var initialLeft = this._left;

  if (this.getTop()) {
    var top = parseInt(this.getTop());
    var bottom = panelEl[0].offsetHeight + top;
    var viewportHeight = this._$window.innerHeight;

    if (top < margin) {
      this._top = margin + 'px';
    } else if (bottom > viewportHeight) {
      this._top = top - (bottom - viewportHeight + margin) + 'px';
    }
  }

  if (this.getLeft()) {
    var left = parseInt(this.getLeft());
    var right = panelEl[0].offsetWidth + left;
    var viewportWidth = this._$window.innerWidth;

    if (left < margin) {
      this._left = margin + 'px';
    } else if (right > viewportWidth) {
      this._left = left - (right - viewportWidth + margin) + 'px';
    }
  }

  // Class that can be used to re-style the panel if it was repositioned.
  panelEl.toggleClass(
    '_md-panel-position-adjusted',
    this._top !== initialTop || this._left !== initialLeft
  );
};


/**
 * Switches between 'start' and 'end'.
 * @param {string} position Horizontal position of the panel
 * @returns {string} Reversed position
 * @private
 */
MdPanelPosition.prototype._reverseXPosition = function(position) {
  if (position === MdPanelPosition.xPosition.CENTER) {
    return position;
  }

  var start = 'start';
  var end = 'end';

  return position.indexOf(start) > -1 ? position.replace(start, end) : position.replace(end, start);
};


/**
 * Handles horizontal positioning in rtl or ltr environments.
 * @param {string} position Horizontal position of the panel
 * @returns {string} The correct position according the page direction
 * @private
 */
MdPanelPosition.prototype._bidi = function(position) {
  return this._isRTL ? this._reverseXPosition(position) : position;
};


/**
 * Calculates the panel position based on the created panel element and the
 * provided positioning.
 * @param {!angular.JQLite} panelEl
 * @param {!{x:string, y:string}} position
 * @private
 */
MdPanelPosition.prototype._calculatePanelPosition = function(panelEl, position) {

  var panelBounds = panelEl[0].getBoundingClientRect();
  var panelWidth = Math.max(panelBounds.width, panelEl[0].clientWidth);
  var panelHeight = Math.max(panelBounds.height, panelEl[0].clientHeight);

  var targetBounds = this._relativeToEl[0].getBoundingClientRect();

  var targetLeft = targetBounds.left;
  var targetRight = targetBounds.right;
  var targetWidth = targetBounds.width;

  switch (this._bidi(position.x)) {
    case MdPanelPosition.xPosition.OFFSET_START:
      this._left = targetLeft - panelWidth + 'px';
      break;
    case MdPanelPosition.xPosition.ALIGN_END:
      this._left = targetRight - panelWidth + 'px';
      break;
    case MdPanelPosition.xPosition.CENTER:
      var left = targetLeft + (0.5 * targetWidth) - (0.5 * panelWidth);
      this._left = left + 'px';
      break;
    case MdPanelPosition.xPosition.ALIGN_START:
      this._left = targetLeft + 'px';
      break;
    case MdPanelPosition.xPosition.OFFSET_END:
      this._left = targetRight + 'px';
      break;
  }

  var targetTop = targetBounds.top;
  var targetBottom = targetBounds.bottom;
  var targetHeight = targetBounds.height;

  switch (position.y) {
    case MdPanelPosition.yPosition.ABOVE:
      this._top = targetTop - panelHeight + 'px';
      break;
    case MdPanelPosition.yPosition.ALIGN_BOTTOMS:
      this._top = targetBottom - panelHeight + 'px';
      break;
    case MdPanelPosition.yPosition.CENTER:
      var top = targetTop + (0.5 * targetHeight) - (0.5 * panelHeight);
      this._top = top + 'px';
      break;
    case MdPanelPosition.yPosition.ALIGN_TOPS:
      this._top = targetTop + 'px';
      break;
    case MdPanelPosition.yPosition.BELOW:
      this._top = targetBottom + 'px';
      break;
  }
};


/*****************************************************************************
 *                               MdPanelAnimation                            *
 *****************************************************************************/


/**
 * Animation configuration object. To use, create an MdPanelAnimation with the
 * desired properties, then pass the object as part of $mdPanel creation.
 *
 * Example:
 *
 * var panelAnimation = new MdPanelAnimation()
 *     .openFrom(myButtonEl)
 *     .closeTo('.my-button')
 *     .withAnimation($mdPanel.animation.SCALE);
 *
 * $mdPanel.create({
 *   animation: panelAnimation
 * });
 *
 * @param {!angular.$injector} $injector
 * @final @constructor
 */
function MdPanelAnimation($injector) {
  /** @private @const {!angular.$mdUtil} */
  this._$mdUtil = $injector.get('$mdUtil');

  /**
   * @private {{element: !angular.JQLite|undefined, bounds: !DOMRect}|
   *     undefined}
   */
  this._openFrom;

  /**
   * @private {{element: !angular.JQLite|undefined, bounds: !DOMRect}|
   *     undefined}
   */
  this._closeTo;

  /** @private {string|{open: string, close: string}} */
  this._animationClass = '';

  /** @private {number} */
  this._openDuration;

  /** @private {number} */
  this._closeDuration;

  /** @private {number|{open: number, close: number}} */
  this._rawDuration;
}


/**
 * Possible default animations.
 * @enum {string}
 */
MdPanelAnimation.animation = {
  SLIDE: 'md-panel-animate-slide',
  SCALE: 'md-panel-animate-scale',
  FADE: 'md-panel-animate-fade'
};


/**
 * Specifies where to start the open animation. `openFrom` accepts a
 * click event object, query selector, DOM element, or a Rect object that
 * is used to determine the bounds. When passed a click event, the location
 * of the click will be used as the position to start the animation.
 * @param {string|!Element|!Event|{top: number, left: number}} openFrom
 * @returns {!MdPanelAnimation}
 */
MdPanelAnimation.prototype.openFrom = function(openFrom) {
  // Check if 'openFrom' is an Event.
  openFrom = openFrom.target ? openFrom.target : openFrom;

  this._openFrom = this._getPanelAnimationTarget(openFrom);

  if (!this._closeTo) {
    this._closeTo = this._openFrom;
  }
  return this;
};


/**
 * Specifies where to animate the panel close. `closeTo` accepts a
 * query selector, DOM element, or a Rect object that is used to determine
 * the bounds.
 * @param {string|!Element|{top: number, left: number}} closeTo
 * @returns {!MdPanelAnimation}
 */
MdPanelAnimation.prototype.closeTo = function(closeTo) {
  this._closeTo = this._getPanelAnimationTarget(closeTo);
  return this;
};


/**
 * Specifies the duration of the animation in milliseconds.
 * @param {number|{open: number, close: number}} duration
 * @returns {!MdPanelAnimation}
 */
MdPanelAnimation.prototype.duration = function(duration) {
  if (duration) {
    if (angular.isNumber(duration)) {
      this._openDuration = this._closeDuration = toSeconds(duration);
    } else if (angular.isObject(duration)) {
      this._openDuration = toSeconds(duration.open);
      this._closeDuration = toSeconds(duration.close);
    }
  }

  // Save the original value so it can be passed to the backdrop.
  this._rawDuration = duration;

  return this;

  function toSeconds(value) {
    if (angular.isNumber(value)) return value / 1000;
  }
};


/**
 * Returns the element and bounds for the animation target.
 * @param {string|!Element|{top: number, left: number}} location
 * @returns {{element: !angular.JQLite|undefined, bounds: !DOMRect}}
 * @private
 */
MdPanelAnimation.prototype._getPanelAnimationTarget = function(location) {
  if (angular.isDefined(location.top) || angular.isDefined(location.left)) {
    return {
      element: undefined,
      bounds: {
        top: location.top || 0,
        left: location.left || 0
      }
    };
  } else {
    return this._getBoundingClientRect(getElement(location));
  }
};


/**
 * Specifies the animation class.
 *
 * There are several default animations that can be used:
 * (MdPanelAnimation.animation)
 *   SLIDE: The panel slides in and out from the specified
 *        elements.
 *   SCALE: The panel scales in and out.
 *   FADE: The panel fades in and out.
 *
 * @param {string|{open: string, close: string}} cssClass
 * @returns {!MdPanelAnimation}
 */
MdPanelAnimation.prototype.withAnimation = function(cssClass) {
  this._animationClass = cssClass;
  return this;
};


/**
 * Animate the panel open.
 * @param {!angular.JQLite} panelEl
 * @returns {!angular.$q.Promise} A promise that is resolved when the open
 *     animation is complete.
 */
MdPanelAnimation.prototype.animateOpen = function(panelEl) {
  var animator = this._$mdUtil.dom.animator;

  this._fixBounds(panelEl);
  var animationOptions = {};

  // Include the panel transformations when calculating the animations.
  var panelTransform = panelEl[0].style.transform || '';

  var openFrom = animator.toTransformCss(panelTransform);
  var openTo = animator.toTransformCss(panelTransform);

  switch (this._animationClass) {
    case MdPanelAnimation.animation.SLIDE:
      // Slide should start with opacity: 1.
      panelEl.css('opacity', '1');

      animationOptions = {
        transitionInClass: '_md-panel-animate-enter'
      };

      var openSlide = animator.calculateSlideToOrigin(
              panelEl, this._openFrom) || '';
      openFrom = animator.toTransformCss(openSlide + ' ' + panelTransform);
      break;

    case MdPanelAnimation.animation.SCALE:
      animationOptions = {
        transitionInClass: '_md-panel-animate-enter'
      };

      var openScale = animator.calculateZoomToOrigin(
              panelEl, this._openFrom) || '';
      openFrom = animator.toTransformCss(openScale + ' ' + panelTransform);
      break;

    case MdPanelAnimation.animation.FADE:
      animationOptions = {
        transitionInClass: '_md-panel-animate-enter'
      };
      break;

    default:
      if (angular.isString(this._animationClass)) {
        animationOptions = {
          transitionInClass: this._animationClass
        };
      } else {
        animationOptions = {
          transitionInClass: this._animationClass['open'],
          transitionOutClass: this._animationClass['close'],
        };
      }
  }

  animationOptions.duration = this._openDuration;

  return animator
      .translate3d(panelEl, openFrom, openTo, animationOptions);
};


/**
 * Animate the panel close.
 * @param {!angular.JQLite} panelEl
 * @returns {!angular.$q.Promise} A promise that resolves when the close
 *     animation is complete.
 */
MdPanelAnimation.prototype.animateClose = function(panelEl) {
  var animator = this._$mdUtil.dom.animator;
  var reverseAnimationOptions = {};

  // Include the panel transformations when calculating the animations.
  var panelTransform = panelEl[0].style.transform || '';

  var closeFrom = animator.toTransformCss(panelTransform);
  var closeTo = animator.toTransformCss(panelTransform);

  switch (this._animationClass) {
    case MdPanelAnimation.animation.SLIDE:
      // Slide should start with opacity: 1.
      panelEl.css('opacity', '1');
      reverseAnimationOptions = {
        transitionInClass: '_md-panel-animate-leave'
      };

      var closeSlide = animator.calculateSlideToOrigin(
              panelEl, this._closeTo) || '';
      closeTo = animator.toTransformCss(closeSlide + ' ' + panelTransform);
      break;

    case MdPanelAnimation.animation.SCALE:
      reverseAnimationOptions = {
        transitionInClass: '_md-panel-animate-scale-out _md-panel-animate-leave'
      };

      var closeScale = animator.calculateZoomToOrigin(
              panelEl, this._closeTo) || '';
      closeTo = animator.toTransformCss(closeScale + ' ' + panelTransform);
      break;

    case MdPanelAnimation.animation.FADE:
      reverseAnimationOptions = {
        transitionInClass: '_md-panel-animate-fade-out _md-panel-animate-leave'
      };
      break;

    default:
      if (angular.isString(this._animationClass)) {
        reverseAnimationOptions = {
          transitionOutClass: this._animationClass
        };
      } else {
        reverseAnimationOptions = {
          transitionInClass: this._animationClass['close'],
          transitionOutClass: this._animationClass['open']
        };
      }
  }

  reverseAnimationOptions.duration = this._closeDuration;

  return animator
      .translate3d(panelEl, closeFrom, closeTo, reverseAnimationOptions);
};


/**
 * Set the height and width to match the panel if not provided.
 * @param {!angular.JQLite} panelEl
 * @private
 */
MdPanelAnimation.prototype._fixBounds = function(panelEl) {
  var panelWidth = panelEl[0].offsetWidth;
  var panelHeight = panelEl[0].offsetHeight;

  if (this._openFrom && this._openFrom.bounds.height == null) {
    this._openFrom.bounds.height = panelHeight;
  }
  if (this._openFrom && this._openFrom.bounds.width == null) {
    this._openFrom.bounds.width = panelWidth;
  }
  if (this._closeTo && this._closeTo.bounds.height == null) {
    this._closeTo.bounds.height = panelHeight;
  }
  if (this._closeTo && this._closeTo.bounds.width == null) {
    this._closeTo.bounds.width = panelWidth;
  }
};


/**
 * Identify the bounding RECT for the target element.
 * @param {!angular.JQLite} element
 * @returns {{element: !angular.JQLite|undefined, bounds: !DOMRect}}
 * @private
 */
MdPanelAnimation.prototype._getBoundingClientRect = function(element) {
  if (element instanceof angular.element) {
    return {
      element: element,
      bounds: element[0].getBoundingClientRect()
    };
  }
};


/*****************************************************************************
 *                                Util Methods                               *
 *****************************************************************************/


/**
 * Returns the angular element associated with a css selector or element.
 * @param el {string|!angular.JQLite|!Element}
 * @returns {!angular.JQLite}
 */
function getElement(el) {
  var queryResult = angular.isString(el) ?
      document.querySelector(el) : el;
  return angular.element(queryResult);
}

/**
 * Gets the computed values for an element's translateX and translateY in px.
 * @param {!angular.JQLite|!Element} el
 * @param {string} property
 * @return {{x: number, y: number}}
 */
function getComputedTranslations(el, property) {
  // The transform being returned by `getComputedStyle` is in the format:
  // `matrix(a, b, c, d, translateX, translateY)` if defined and `none`
  // if the element doesn't have a transform.
  var transform = getComputedStyle(el[0] || el)[property];
  var openIndex = transform.indexOf('(');
  var closeIndex = transform.lastIndexOf(')');
  var output = { x: 0, y: 0 };

  if (openIndex > -1 && closeIndex > -1) {
    var parsedValues = transform
      .substring(openIndex + 1, closeIndex)
      .split(', ')
      .slice(-2);

    output.x = parseInt(parsedValues[0]);
    output.y = parseInt(parsedValues[1]);
  }

  return output;
}

/**
 * Adds units to a number value.
 * @param {string|number} value
 * @return {string}
 */
function addUnits(value) {
  return angular.isNumber(value) ? value + 'px' : value;
}

})(window, window.angular);