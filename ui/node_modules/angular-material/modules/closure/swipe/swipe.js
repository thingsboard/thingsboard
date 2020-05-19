/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.swipe');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.swipe
 * @description Swipe module!
 */
/**
 * @ngdoc directive
 * @module material.components.swipe
 * @name mdSwipeLeft
 *
 * @restrict A
 *
 * @description
 * The md-swipe-left directive allows you to specify custom behavior when an element is swiped
 * left.
 *
 * ### Notes
 * - The `$event.currentTarget` of the swiped element will be `null`, but you can get a
 * reference to the element that actually holds the `md-swipe-left` directive by using
 * `$target.current`
 *
 * > You can see this in action on the <a ng-href="demo/swipe">demo page</a> (Look at the Developer
 * Tools console while swiping).
 *
 * @usage
 * <hljs lang="html">
 * <div md-swipe-left="onSwipeLeft($event, $target)">Swipe me left!</div>
 * </hljs>
 */
/**
 * @ngdoc directive
 * @module material.components.swipe
 * @name mdSwipeRight
 *
 * @restrict A
 *
 * @description
 * The md-swipe-right directive allows you to specify custom behavior when an element is swiped
 * right.
 *
 * ### Notes
 * - The `$event.currentTarget` of the swiped element will be `null`, but you can get a
 * reference to the element that actually holds the `md-swipe-right` directive by using
 * `$target.current`
 *
 * > You can see this in action on the <a ng-href="demo/swipe">demo page</a> (Look at the Developer
 * Tools console while swiping).
 *
 * @usage
 * <hljs lang="html">
 * <div md-swipe-right="onSwipeRight($event, $target)">Swipe me right!</div>
 * </hljs>
 */
/**
 * @ngdoc directive
 * @module material.components.swipe
 * @name mdSwipeUp
 *
 * @restrict A
 *
 * @description
 * The md-swipe-up directive allows you to specify custom behavior when an element is swiped
 * up.
 *
 * ### Notes
 * - The `$event.currentTarget` of the swiped element will be `null`, but you can get a
 * reference to the element that actually holds the `md-swipe-up` directive by using
 * `$target.current`
 *
 * > You can see this in action on the <a ng-href="demo/swipe">demo page</a> (Look at the Developer
 * Tools console while swiping).
 *
 * @usage
 * <hljs lang="html">
 * <div md-swipe-up="onSwipeUp($event, $target)">Swipe me up!</div>
 * </hljs>
 */
/**
 * @ngdoc directive
 * @module material.components.swipe
 * @name mdSwipeDown
 *
 * @restrict A
 *
 * @description
 * The md-swipe-down directive allows you to specify custom behavior when an element is swiped
 * down.
 *
 * ### Notes
 * - The `$event.currentTarget` of the swiped element will be `null`, but you can get a
 * reference to the element that actually holds the `md-swipe-down` directive by using
 * `$target.current`
 *
 * > You can see this in action on the <a ng-href="demo/swipe">demo page</a> (Look at the Developer
 * Tools console while swiping).
 *
 * @usage
 * <hljs lang="html">
 * <div md-swipe-down="onSwipeDown($event, $target)">Swipe me down!</div>
 * </hljs>
 */

angular.module('material.components.swipe', ['material.core'])
    .directive('mdSwipeLeft', getDirective('SwipeLeft'))
    .directive('mdSwipeRight', getDirective('SwipeRight'))
    .directive('mdSwipeUp', getDirective('SwipeUp'))
    .directive('mdSwipeDown', getDirective('SwipeDown'));

function getDirective(name) {
    DirectiveFactory['$inject'] = ["$parse"];
  var directiveName = 'md' + name;
  var eventName = '$md.' + name.toLowerCase();

  return DirectiveFactory;

  /* ngInject */
  function DirectiveFactory($parse) {
      return { restrict: 'A', link: postLink };
      function postLink(scope, element, attr) {
        var fn = $parse(attr[directiveName]);
        element.on(eventName, function(ev) {
          var currentTarget = ev.currentTarget;
          scope.$applyAsync(function() { fn(scope, { $event: ev, $target: { current: currentTarget } }); });
        });
      }
    }
}



ngmaterial.components.swipe = angular.module("material.components.swipe");