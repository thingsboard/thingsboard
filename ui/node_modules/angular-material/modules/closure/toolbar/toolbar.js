/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.toolbar');
goog.require('ngmaterial.components.content');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.toolbar
 */
mdToolbarDirective['$inject'] = ["$$rAF", "$mdConstant", "$mdUtil", "$mdTheming", "$animate"];
angular.module('material.components.toolbar', [
  'material.core',
  'material.components.content'
])
  .directive('mdToolbar', mdToolbarDirective);

/**
 * @ngdoc directive
 * @name mdToolbar
 * @module material.components.toolbar
 * @restrict E
 * @description
 * `md-toolbar` is used to place a toolbar in your app.
 *
 * Toolbars are usually used above a content area to display the title of the
 * current page, and show relevant action buttons for that page.
 *
 * You can change the height of the toolbar by adding either the
 * `md-medium-tall` or `md-tall` class to the toolbar.
 *
 * @usage
 * <hljs lang="html">
 * <div layout="column" layout-fill>
 *   <md-toolbar>
 *
 *     <div class="md-toolbar-tools">
 *       <h2 md-truncate flex>My App's Title</h2>
 *
 *       <md-button>
 *         Right Bar Button
 *       </md-button>
 *     </div>
 *
 *   </md-toolbar>
 *   <md-content>
 *     Hello!
 *   </md-content>
 * </div>
 * </hljs>
 *
 * <i><b>Note:</b> The code above shows usage with the `md-truncate` component which provides an
 * ellipsis if the title is longer than the width of the Toolbar.</i>
 *
 * ## CSS & Styles
 *
 * The `<md-toolbar>` provides a few custom CSS classes that you may use to enhance the
 * functionality of your toolbar.
 *
 * <div>
 * <docs-css-api-table>
 *
 *   <docs-css-selector code="md-toolbar .md-toolbar-tools">
 *     The `md-toolbar-tools` class provides quite a bit of automatic styling for your toolbar
 *     buttons and text. When applied, it will center the buttons and text vertically for you.
 *   </docs-css-selector>
 *
 * </docs-css-api-table>
 * </div>
 *
 * ### Private Classes
 *
 * Currently, the only private class is the `md-toolbar-transitions` class. All other classes are
 * considered public.
 *
 * @param {boolean=} md-scroll-shrink Whether the header should shrink away as
 * the user scrolls down, and reveal itself as the user scrolls up.
 *
 * _**Note (1):** for scrollShrink to work, the toolbar must be a sibling of a
 * `md-content` element, placed before it. See the scroll shrink demo._
 *
 * _**Note (2):** The `md-scroll-shrink` attribute is only parsed on component
 * initialization, it does not watch for scope changes._
 *
 *
 * @param {number=} md-shrink-speed-factor How much to change the speed of the toolbar's
 * shrinking by. For example, if 0.25 is given then the toolbar will shrink
 * at one fourth the rate at which the user scrolls down. Default 0.5.
 *
 */

function mdToolbarDirective($$rAF, $mdConstant, $mdUtil, $mdTheming, $animate) {
  var translateY = angular.bind(null, $mdUtil.supplant, 'translate3d(0,{0}px,0)');

  return {
    template: '',
    restrict: 'E',

    link: function(scope, element, attr) {

      element.addClass('_md');     // private md component indicator for styling
      $mdTheming(element);

      $mdUtil.nextTick(function () {
        element.addClass('_md-toolbar-transitions');     // adding toolbar transitions after digest
      }, false);

      if (angular.isDefined(attr.mdScrollShrink)) {
        setupScrollShrink();
      }

      function setupScrollShrink() {

        var toolbarHeight;
        var contentElement;
        var disableScrollShrink = angular.noop;

        // Current "y" position of scroll
        // Store the last scroll top position
        var y = 0;
        var prevScrollTop = 0;
        var shrinkSpeedFactor = attr.mdShrinkSpeedFactor || 0.5;

        var debouncedContentScroll = $$rAF.throttle(onContentScroll);
        var debouncedUpdateHeight = $mdUtil.debounce(updateToolbarHeight, 5 * 1000);

        // Wait for $mdContentLoaded event from mdContent directive.
        // If the mdContent element is a sibling of our toolbar, hook it up
        // to scroll events.

        scope.$on('$mdContentLoaded', onMdContentLoad);

        // If the toolbar is used inside an ng-if statement, we may miss the
        // $mdContentLoaded event, so we attempt to fake it if we have a
        // md-content close enough.

        attr.$observe('mdScrollShrink', onChangeScrollShrink);

        // If the toolbar has ngShow or ngHide we need to update height immediately as it changed
        // and not wait for $mdUtil.debounce to happen

        if (attr.ngShow) { scope.$watch(attr.ngShow, updateToolbarHeight); }
        if (attr.ngHide) { scope.$watch(attr.ngHide, updateToolbarHeight); }

        // If the scope is destroyed (which could happen with ng-if), make sure
        // to disable scroll shrinking again

        scope.$on('$destroy', disableScrollShrink);

        /**
         *
         */
        function onChangeScrollShrink(shrinkWithScroll) {
          var closestContent = element.parent().find('md-content');

          // If we have a content element, fake the call; this might still fail
          // if the content element isn't a sibling of the toolbar

          if (!contentElement && closestContent.length) {
            onMdContentLoad(null, closestContent);
          }

          // Evaluate the expression
          shrinkWithScroll = scope.$eval(shrinkWithScroll);

          // Disable only if the attribute's expression evaluates to false
          if (shrinkWithScroll === false) {
            disableScrollShrink();
          } else {
            disableScrollShrink = enableScrollShrink();
          }
        }

        /**
         *
         */
        function onMdContentLoad($event, newContentEl) {
          // Toolbar and content must be siblings
          if (newContentEl && element.parent()[0] === newContentEl.parent()[0]) {
            // unhook old content event listener if exists
            if (contentElement) {
              contentElement.off('scroll', debouncedContentScroll);
            }

            contentElement = newContentEl;
            disableScrollShrink = enableScrollShrink();
          }
        }

        /**
         *
         */
        function onContentScroll(e) {
          var scrollTop = e ? e.target.scrollTop : prevScrollTop;

          debouncedUpdateHeight();

          y = Math.min(
            toolbarHeight / shrinkSpeedFactor,
            Math.max(0, y + scrollTop - prevScrollTop)
          );

          element.css($mdConstant.CSS.TRANSFORM, translateY([-y * shrinkSpeedFactor]));
          contentElement.css($mdConstant.CSS.TRANSFORM, translateY([(toolbarHeight - y) * shrinkSpeedFactor]));

          prevScrollTop = scrollTop;

          $mdUtil.nextTick(function() {
            var hasWhiteFrame = element.hasClass('md-whiteframe-z1');

            if (hasWhiteFrame && !y) {
              $animate.removeClass(element, 'md-whiteframe-z1');
            } else if (!hasWhiteFrame && y) {
              $animate.addClass(element, 'md-whiteframe-z1');
            }
          });

        }

        /**
         *
         */
        function enableScrollShrink() {
          if (!contentElement)     return angular.noop;           // no md-content

          contentElement.on('scroll', debouncedContentScroll);
          contentElement.attr('scroll-shrink', 'true');

          $mdUtil.nextTick(updateToolbarHeight, false);

          return function disableScrollShrink() {
            contentElement.off('scroll', debouncedContentScroll);
            contentElement.attr('scroll-shrink', 'false');

            updateToolbarHeight();
          };
        }

        /**
         *
         */
        function updateToolbarHeight() {
          toolbarHeight = element.prop('offsetHeight');
          // Add a negative margin-top the size of the toolbar to the content el.
          // The content will start transformed down the toolbarHeight amount,
          // so everything looks normal.
          //
          // As the user scrolls down, the content will be transformed up slowly
          // to put the content underneath where the toolbar was.
          var margin = (-toolbarHeight * shrinkSpeedFactor) + 'px';

          contentElement.css({
            "margin-top": margin,
            "margin-bottom": margin
          });

          onContentScroll();
        }

      }

    }
  };

}

ngmaterial.components.toolbar = angular.module("material.components.toolbar");