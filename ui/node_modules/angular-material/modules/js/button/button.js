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
 * @name material.components.button
 * @description
 *
 * Button
 */
MdButtonDirective['$inject'] = ["$mdButtonInkRipple", "$mdTheming", "$mdAria", "$mdInteraction"];
MdAnchorDirective['$inject'] = ["$mdTheming"];
angular
    .module('material.components.button', ['material.core'])
    .directive('mdButton', MdButtonDirective)
    .directive('a', MdAnchorDirective);


/**
 * @private
 * @restrict E
 *
 * @description
 * `a` is an anchor directive used to inherit theme colors for md-primary, md-accent, etc.
 *
 * @usage
 *
 * <hljs lang="html">
 *  <md-content md-theme="myTheme">
 *    <a href="#chapter1" class="md-accent"></a>
 *  </md-content>
 * </hljs>
 */
function MdAnchorDirective($mdTheming) {
  return {
    restrict : 'E',
    link : function postLink(scope, element) {
      // Make sure to inherit theme so stand-alone anchors
      // support theme colors for md-primary, md-accent, etc.
      $mdTheming(element);
    }
  };
}


/**
 * @ngdoc directive
 * @name mdButton
 * @module material.components.button
 *
 * @restrict E
 *
 * @description
 * `<md-button>` is a button directive with optional ink ripples (default enabled).
 *
 * If you supply a `href` or `ng-href` attribute, it will become an `<a>` element. Otherwise, it
 * will become a `<button>` element. As per the
 * [Material Design specifications](https://material.google.com/style/color.html#color-color-palette)
 * the FAB button background is filled with the accent color [by default]. The primary color palette
 * may be used with the `md-primary` class.
 *
 * Developers can also change the color palette of the button, by using the following classes
 * - `md-primary`
 * - `md-accent`
 * - `md-warn`
 *
 * See for example
 *
 * <hljs lang="html">
 *   <md-button class="md-primary">Primary Button</md-button>
 * </hljs>
 *
 * Button can be also raised, which means that they will use the current color palette to fill the button.
 *
 * <hljs lang="html">
 *   <md-button class="md-accent md-raised">Raised and Accent Button</md-button>
 * </hljs>
 *
 * It is also possible to disable the focus effect on the button, by using the following markup.
 *
 * <hljs lang="html">
 *   <md-button class="md-no-focus">No Focus Style</md-button>
 * </hljs>
 *
 * @param {string=} aria-label Adds alternative text to button for accessibility, useful for icon buttons.
 * If no default text is found, a warning will be logged.
 * @param {boolean=} md-no-ink If present, disable ink ripple effects.
 * @param {string=} md-ripple-size Overrides the default ripple size logic. Options: `full`, `partial`, `auto`.
 * @param {expression=} ng-disabled Disable the button when the expression is truthy.
 * @param {expression=} ng-blur Expression evaluated when focus is removed from the button.
 *
 * @usage
 *
 * Regular buttons:
 *
 * <hljs lang="html">
 *  <md-button> Flat Button </md-button>
 *  <md-button href="http://google.com"> Flat link </md-button>
 *  <md-button class="md-raised"> Raised Button </md-button>
 *  <md-button ng-disabled="true"> Disabled Button </md-button>
 *  <md-button>
 *    <md-icon md-svg-src="your/icon.svg"></md-icon>
 *    Register Now
 *  </md-button>
 * </hljs>
 *
 * FAB buttons:
 *
 * <hljs lang="html">
 *  <md-button class="md-fab" aria-label="FAB">
 *    <md-icon md-svg-src="your/icon.svg"></md-icon>
 *  </md-button>
 *  <!-- mini-FAB -->
 *  <md-button class="md-fab md-mini" aria-label="Mini FAB">
 *    <md-icon md-svg-src="your/icon.svg"></md-icon>
 *  </md-button>
 *  <!-- Button with SVG Icon -->
 *  <md-button class="md-icon-button" aria-label="Custom Icon Button">
 *    <md-icon md-svg-icon="path/to/your.svg"></md-icon>
 *  </md-button>
 * </hljs>
 */
function MdButtonDirective($mdButtonInkRipple, $mdTheming, $mdAria, $mdInteraction) {

  return {
    restrict: 'EA',
    replace: true,
    transclude: true,
    template: getTemplate,
    link: postLink
  };

  function isAnchor(attr) {
    return angular.isDefined(attr.href) || angular.isDefined(attr.ngHref) || angular.isDefined(attr.ngLink) || angular.isDefined(attr.uiSref);
  }

  function getTemplate(element, attr) {
    if (isAnchor(attr)) {
      return '<a class="md-button" ng-transclude></a>';
    } else {
      // If buttons don't have type="button", they will submit forms automatically.
      var btnType = (typeof attr.type === 'undefined') ? 'button' : attr.type;
      return '<button class="md-button" type="' + btnType + '" ng-transclude></button>';
    }
  }

  function postLink(scope, element, attr) {
    $mdTheming(element);
    $mdButtonInkRipple.attach(scope, element);

    // Use async expect to support possible bindings in the button label
    $mdAria.expectWithoutText(element, 'aria-label');

    // For anchor elements, we have to set tabindex manually when the element is disabled.
    // We don't do this for md-nav-bar anchors as the component manages its own tabindex values.
    if (isAnchor(attr) && angular.isDefined(attr.ngDisabled) &&
        !element.hasClass('_md-nav-button')) {
      scope.$watch(attr.ngDisabled, function(isDisabled) {
        element.attr('tabindex', isDisabled ? -1 : 0);
      });
    }

    // disabling click event when disabled is true
    element.on('click', function(e){
      if (attr.disabled === true) {
        e.preventDefault();
        e.stopImmediatePropagation();
      }
    });

    if (!element.hasClass('md-no-focus')) {

      element.on('focus', function() {

        // Only show the focus effect when being focused through keyboard interaction or programmatically
        if (!$mdInteraction.isUserInvoked() || $mdInteraction.getLastInteractionType() === 'keyboard') {
          element.addClass('md-focused');
        }

      });

      element.on('blur', function() {
        element.removeClass('md-focused');
      });
    }

  }

}

})(window, window.angular);