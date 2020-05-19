/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
(function( window, angular, undefined ){
"use strict";

(function() {
  'use strict';

  /**
   * @ngdoc module
   * @name material.components.fabToolbar
   */
  angular
    // Declare our module
    .module('material.components.fabToolbar', [
      'material.core',
      'material.components.fabShared',
      'material.components.fabActions'
    ])

    // Register our directive
    .directive('mdFabToolbar', MdFabToolbarDirective)

    // Register our custom animations
    .animation('.md-fab-toolbar', MdFabToolbarAnimation)

    // Register a service for the animation so that we can easily inject it into unit tests
    .service('mdFabToolbarAnimation', MdFabToolbarAnimation);

  /**
   * @ngdoc directive
   * @name mdFabToolbar
   * @module material.components.fabToolbar
   *
   * @restrict E
   *
   * @description
   *
   * The `<md-fab-toolbar>` directive is used to present a toolbar of elements (usually `<md-button>`s)
   * for quick access to common actions when a floating action button is activated (via click or
   * keyboard navigation).
   *
   * You may also easily position the trigger by applying one one of the following classes to the
   * `<md-fab-toolbar>` element:
   *  - `md-fab-top-left`
   *  - `md-fab-top-right`
   *  - `md-fab-bottom-left`
   *  - `md-fab-bottom-right`
   *
   * These CSS classes use `position: absolute`, so you need to ensure that the container element
   * also uses `position: absolute` or `position: relative` in order for them to work.
   *
   * @usage
   *
   * <hljs lang="html">
   * <md-fab-toolbar md-direction='left'>
   *   <md-fab-trigger>
   *     <md-button aria-label="Add..."><md-icon md-svg-src="/img/icons/plus.svg"></md-icon></md-button>
   *   </md-fab-trigger>
   *
   *   <md-toolbar>
   *    <md-fab-actions>
   *      <md-button aria-label="Add User">
   *        <md-icon md-svg-src="/img/icons/user.svg"></md-icon>
   *      </md-button>
   *
   *      <md-button aria-label="Add Group">
   *        <md-icon md-svg-src="/img/icons/group.svg"></md-icon>
   *      </md-button>
   *    </md-fab-actions>
   *   </md-toolbar>
   * </md-fab-toolbar>
   * </hljs>
   *
   * @param {string} md-direction From which direction you would like the toolbar items to appear
   * relative to the trigger element. Supports `left` and `right` directions.
   * @param {expression=} md-open Programmatically control whether or not the toolbar is visible.
   */
  function MdFabToolbarDirective() {
    return {
      restrict: 'E',
      transclude: true,
      template: '<div class="md-fab-toolbar-wrapper">' +
      '  <div class="md-fab-toolbar-content" ng-transclude></div>' +
      '</div>',

      scope: {
        direction: '@?mdDirection',
        isOpen: '=?mdOpen'
      },

      bindToController: true,
      controller: 'MdFabController',
      controllerAs: 'vm',

      link: link
    };

    function link(scope, element, attributes) {
      // Add the base class for animations
      element.addClass('md-fab-toolbar');

      // Prepend the background element to the trigger's button
      element.find('md-fab-trigger').find('button')
        .prepend('<div class="md-fab-toolbar-background"></div>');
    }
  }

  function MdFabToolbarAnimation() {

    function runAnimation(element, className, done) {
      // If no className was specified, don't do anything
      if (!className) {
        return;
      }

      var el = element[0];
      var ctrl = element.controller('mdFabToolbar');

      // Grab the relevant child elements
      var backgroundElement = el.querySelector('.md-fab-toolbar-background');
      var triggerElement = el.querySelector('md-fab-trigger button');
      var toolbarElement = el.querySelector('md-toolbar');
      var iconElement = el.querySelector('md-fab-trigger button md-icon');
      var actions = element.find('md-fab-actions').children();

      // If we have both elements, use them to position the new background
      if (triggerElement && backgroundElement) {
        // Get our variables
        var color = window.getComputedStyle(triggerElement).getPropertyValue('background-color');
        var width = el.offsetWidth;
        var height = el.offsetHeight;

        // Make it twice as big as it should be since we scale from the center
        var scale = 2 * (width / triggerElement.offsetWidth);

        // Set some basic styles no matter what animation we're doing
        backgroundElement.style.backgroundColor = color;
        backgroundElement.style.borderRadius = width + 'px';

        // If we're open
        if (ctrl.isOpen) {
          // Turn on toolbar pointer events when closed
          toolbarElement.style.pointerEvents = 'inherit';

          backgroundElement.style.width = triggerElement.offsetWidth + 'px';
          backgroundElement.style.height = triggerElement.offsetHeight + 'px';
          backgroundElement.style.transform = 'scale(' + scale + ')';

          // Set the next close animation to have the proper delays
          backgroundElement.style.transitionDelay = '0ms';
          iconElement && (iconElement.style.transitionDelay = '.3s');

          // Apply a transition delay to actions
          angular.forEach(actions, function(action, index) {
            action.style.transitionDelay = (actions.length - index) * 25 + 'ms';
          });
        } else {
          // Turn off toolbar pointer events when closed
          toolbarElement.style.pointerEvents = 'none';

          // Scale it back down to the trigger's size
          backgroundElement.style.transform = 'scale(1)';

          // Reset the position
          backgroundElement.style.top = '0';

          if (element.hasClass('md-right')) {
            backgroundElement.style.left = '0';
            backgroundElement.style.right = null;
          }

          if (element.hasClass('md-left')) {
            backgroundElement.style.right = '0';
            backgroundElement.style.left = null;
          }

          // Set the next open animation to have the proper delays
          backgroundElement.style.transitionDelay = '200ms';
          iconElement && (iconElement.style.transitionDelay = '0ms');

          // Apply a transition delay to actions
          angular.forEach(actions, function(action, index) {
            action.style.transitionDelay = 200 + (index * 25) + 'ms';
          });
        }
      }
    }

    return {
      addClass: function(element, className, done) {
        runAnimation(element, className, done);
        done();
      },

      removeClass: function(element, className, done) {
        runAnimation(element, className, done);
        done();
      }
    };
  }
})();

})(window, window.angular);