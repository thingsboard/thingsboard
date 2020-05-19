/**
 *
 * AngularJS-Material-Mocks
 *
 * Developers interested in running their own custom unit tests WITH angular-material.js loaded...
 * must also include this *mocks* file. Similar to `angular-mocks.js`, `angular-material-mocks.js`
 * will override and disable specific AngularJS Material performance settings:
 *
 *  - Disabled Theme CSS rule generations
 *  - Forces $mdAria.expectWithText() to be synchronous
 *  - Mocks $$rAF.throttle()
 *  - Captures flush exceptions from $$rAF
 *
 */
(function(window, angular, undefined) {

'use strict';

  // Allow our code to know when they are running inside of a test so they can expose extra services
  // that should NOT be exposed to the public but that should be tested.
  //
  // As an example, see input.js which exposes some animation-related methods.
  window._mdMocksIncluded = true;

/**
 * @ngdoc module
 * @name ngMaterial-mock
 * @packageName angular-material-mocks
 *
 * @description
 *
 * The `ngMaterial-mock` module provides support
 *
 */
angular.module('ngMaterial-mock', [
  'ngMock',
  'ngAnimateMock',
  'material.core'
  ])
  .config(['$provide', function($provide) {

    $provide.factory('$material', ['$animate', '$timeout', function($animate, $timeout) {
      return {
        flushOutstandingAnimations: function() {
          // this code is placed in a try-catch statement
          // since 1.3 and 1.4 handle their animations differently
          // and there may be situations where follow-up animations
          // are run in one version and not the other
          try { $animate.flush(); } catch(e) {}
        },
        flushInterimElement: function() {
          this.flushOutstandingAnimations();
          $timeout.flush();
          this.flushOutstandingAnimations();
          $timeout.flush();
          this.flushOutstandingAnimations();
          $timeout.flush();
        }
      };
    }]);

    /**
      * AngularJS Material dynamically generates Style tags
      * based on themes and palletes; for each ng-app.
      *
      * For testing, we want to disable generation and
      * <style> DOM injections. So we clear the huge THEME
      * styles while testing...
      */
     $provide.constant('$MD_THEME_CSS', '/**/');

    /**
     * Add throttle() and wrap .flush() to catch `no callbacks present`
     * errors
     */
    $provide.decorator('$$rAF', function throttleInjector($delegate){

      $delegate.throttle = function(cb) {
        return function() {
          cb.apply(this, arguments);
        };
      };

      var ngFlush = $delegate.flush;
      $delegate.flush = function() {
        try      { ngFlush();  }
        catch(e) { ;           }
      };

      return $delegate;
    });

    /**
     * Capture $timeout.flush() errors: "No deferred tasks to be flushed"
     * errors
     */
    $provide.decorator('$timeout', function throttleInjector($delegate){

      var ngFlush = $delegate.flush;
      $delegate.flush = function() {
          var args = Array.prototype.slice.call(arguments);
          try      { ngFlush.apply($delegate, args);  }
          catch(e) { }
      };

      return $delegate;
    });

  }]);

  /**
   * Stylesheet Mocks used by `animateCss.spec.js`
   */
  window.createMockStyleSheet = function createMockStyleSheet(doc, wind) {
    doc = doc ? doc[0] : window.document;
    wind = wind || window;

    var node = doc.createElement('style');
    var head = doc.getElementsByTagName('head')[0];
    head.appendChild(node);

    var ss = doc.styleSheets[doc.styleSheets.length - 1];

    return {
      addRule: function(selector, styles) {
        styles = addVendorPrefix(styles);

        try {
          ss.insertRule(selector + '{ ' + styles + '}', 0);
        }
        catch (e) {
          try {
            ss.addRule(selector, styles);
          }
          catch (e2) {}
        }
      },

      destroy: function() {
        head.removeChild(node);
      }
    };

    /**
     * Decompose styles, attached specific vendor prefixes
     * and recompose...
     * e.g.
     *    'transition:0.5s linear all; font-size:100px;'
     * becomes
     *    '-webkit-transition:0.5s linear all; transition:0.5s linear all; font-size:100px;'
     */
    function addVendorPrefix(styles) {
      var cache = { };

      // Decompose into cache registry
      styles
        .match(/([\-A-Za-z]*)\w\:\w*([A-Za-z0-9\.\-\s]*)/gi)
        .forEach(function(style){
          var pair = style.split(":");
          var key = pair[0];

          switch(key) {
            case 'transition':
            case 'transform':
            case 'animation':
            case 'transition-duration':
            case 'animation-duration':
              cache[key] = cache['-webkit-' + key] = pair[1];
              break;
            default:
              cache[key] = pair[1];
          }
        });

        // Recompose full style object (as string)
        styles = "";
        angular.forEach(cache, function(value, key) {
          styles = styles + key + ":" + value + "; ";
        });

        return styles;
    }

  };

})(window, window.angular);
