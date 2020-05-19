/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
(function( window, angular, undefined ){
"use strict";

(function () {
  "use strict";

  /**
   *  Use a RegExp to check if the `md-colors="<expression>"` is static string
   *  or one that should be observed and dynamically interpolated.
   */
  MdColorsDirective['$inject'] = ["$mdColors", "$mdUtil", "$log", "$parse"];
  MdColorsService['$inject'] = ["$mdTheming", "$mdUtil", "$log"];
  var STATIC_COLOR_EXPRESSION = /^{((\s|,)*?["'a-zA-Z-]+?\s*?:\s*?('|")[a-zA-Z0-9-.]*('|"))+\s*}$/;
  var colorPalettes = null;

  /**
   * @ngdoc module
   * @name material.components.colors
   *
   * @description
   * Define $mdColors service and a `md-colors=""` attribute directive
   */
  angular
    .module('material.components.colors', ['material.core'])
    .directive('mdColors', MdColorsDirective)
    .service('$mdColors', MdColorsService);

  /**
   * @ngdoc service
   * @name $mdColors
   * @module material.components.colors
   *
   * @description
   * By default, defining a theme does not make its colors available for applying to non AngularJS
   * Material elements. The `$mdColors` service is used by the `md-color` directive to convert a
   * set of color expressions to RGBA values and then apply those values to the element as CSS
   * property values.
   *
   * @usage
   * Getting a color based on a theme
   *
   *  <hljs lang="js">
   *    angular.controller('myCtrl', function ($mdColors) {
   *      var color = $mdColors.getThemeColor('myTheme-primary-900-0.5');
   *      ...
   *    });
   *  </hljs>
   *
   * Applying a color from a palette to an element
   * <hljs lang="js">
   *   app.directive('myDirective', function($mdColors) {
   *     return {
   *       ...
   *       link: function (scope, elem) {
   *         $mdColors.applyThemeColors(elem, {color: 'red-A200-0.2'});
   *       }
   *    }
   *   });
   * </hljs>
   */
  function MdColorsService($mdTheming, $mdUtil, $log) {
    colorPalettes = colorPalettes || Object.keys($mdTheming.PALETTES);

    // Publish service instance
    return {
      applyThemeColors: applyThemeColors,
      getThemeColor: getThemeColor,
      hasTheme: hasTheme
    };

    // ********************************************
    // Internal Methods
    // ********************************************

    /**
     * @ngdoc method
     * @name $mdColors#applyThemeColors
     *
     * @description
     * Lookup a set of colors by hue, theme, and palette, then apply those colors
     * with the provided opacity (via `rgba()`) to the specified CSS property.
     *
     * @param {angular.element} element the element to apply the styles to
     * @param {Object} colorExpression Keys are CSS properties and values are strings representing
     * the `theme-palette-hue-opacity` of the desired color. For example:
     * `{'color': 'red-A200-0.3', 'background-color': 'myTheme-primary-700-0.8'}`. Theme, hue, and
     * opacity are optional.
     */
    function applyThemeColors(element, colorExpression) {
      try {
        if (colorExpression) {
          // Assign the calculate RGBA color values directly as inline CSS
          element.css(interpolateColors(colorExpression));
        }
      } catch (e) {
        $log.error(e.message);
      }
    }

    /**
     * @ngdoc method
     * @name $mdColors#getThemeColor
     *
     * @description
     * Get a parsed RGBA color using a string representing the `theme-palette-hue-opacity` of the
     * desired color.
     *
     * @param {string} expression color expression like `'red-A200-0.3'` or
     *  `'myTheme-primary-700-0.8'`. Theme, hue, and opacity are optional.
     * @returns {string} a CSS color value like `rgba(211, 47, 47, 0.8)`
     */
    function getThemeColor(expression) {
      var color = extractColorOptions(expression);

      return parseColor(color);
    }

    /**
     * Return the parsed color
     * @param {{hue: *, theme: any, palette: *, opacity: (*|string|number)}} color hash map of color
     *  definitions
     * @param {boolean=} contrast whether use contrast color for foreground. Defaults to false.
     * @returns {string} rgba color string
     */
    function parseColor(color, contrast) {
      contrast = contrast || false;
      var rgbValues = $mdTheming.PALETTES[color.palette][color.hue];

      rgbValues = contrast ? rgbValues.contrast : rgbValues.value;

      return $mdUtil.supplant('rgba({0}, {1}, {2}, {3})',
        [rgbValues[0], rgbValues[1], rgbValues[2], rgbValues[3] || color.opacity]
      );
    }

    /**
     * Convert the color expression into an object with scope-interpolated values
     * Then calculate the rgba() values based on the theme color parts
     * @param {Object} themeColors json object, keys are css properties and values are string of
     * the wanted color, for example: `{color: 'red-A200-0.3'}`.
     * @return {Object} Hashmap of CSS properties with associated `rgba()` string values
     */
    function interpolateColors(themeColors) {
      var rgbColors = {};

      var hasColorProperty = themeColors.hasOwnProperty('color');

      angular.forEach(themeColors, function (value, key) {
        var color = extractColorOptions(value);
        var hasBackground = key.indexOf('background') > -1;

        rgbColors[key] = parseColor(color);
        if (hasBackground && !hasColorProperty) {
          rgbColors.color = parseColor(color, true);
        }
      });

      return rgbColors;
    }

    /**
     * Check if expression has defined theme
     * For instance:
     *   'myTheme-primary' => true
     *   'red-800' => false
     * @param {string} expression color expression like 'red-800', 'red-A200-0.3',
     *   'myTheme-primary', or 'myTheme-primary-400'
     * @return {boolean} true if the expression has a theme part, false otherwise.
     */
    function hasTheme(expression) {
      return angular.isDefined($mdTheming.THEMES[expression.split('-')[0]]);
    }

    /**
     * For the evaluated expression, extract the color parts into a hash map
     * @param {string} expression color expression like 'red-800', 'red-A200-0.3',
     *   'myTheme-primary', or 'myTheme-primary-400'
     * @returns {{hue: *, theme: any, palette: *, opacity: (*|string|number)}}
     */
    function extractColorOptions(expression) {
      var parts = expression.split('-');
      var hasTheme = angular.isDefined($mdTheming.THEMES[parts[0]]);
      var theme = hasTheme ? parts.splice(0, 1)[0] : $mdTheming.defaultTheme();

      return {
        theme: theme,
        palette: extractPalette(parts, theme),
        hue: extractHue(parts, theme),
        opacity: parts[2] || 1
      };
    }

    /**
     * Calculate the theme palette name
     * @param {Array} parts
     * @param {string} theme name
     * @return {string}
     */
    function extractPalette(parts, theme) {
      // If the next section is one of the palettes we assume it's a two word palette
      // Two word palette can be also written in camelCase, forming camelCase to dash-case

      var isTwoWord = parts.length > 1 && colorPalettes.indexOf(parts[1]) !== -1;
      var palette = parts[0].replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();

      if (isTwoWord)  palette = parts[0] + '-' + parts.splice(1, 1);

      if (colorPalettes.indexOf(palette) === -1) {
        // If the palette is not in the palette list it's one of primary/accent/warn/background
        var scheme = $mdTheming.THEMES[theme].colors[palette];
        if (!scheme) {
          throw new Error($mdUtil.supplant(
            'mdColors: couldn\'t find \'{palette}\' in the palettes.',
            {palette: palette}));
        }
        palette = scheme.name;
      }

      return palette;
    }

    /**
     * @param {Array} parts
     * @param {string} theme name
     * @return {*}
     */
    function extractHue(parts, theme) {
      var themeColors = $mdTheming.THEMES[theme].colors;

      if (parts[1] === 'hue') {
        var hueNumber = parseInt(parts.splice(2, 1)[0], 10);

        if (hueNumber < 1 || hueNumber > 3) {
          throw new Error($mdUtil.supplant(
            'mdColors: \'hue-{hueNumber}\' is not a valid hue, can be only \'hue-1\', \'hue-2\' and \'hue-3\'',
            {hueNumber: hueNumber}));
        }
        parts[1] = 'hue-' + hueNumber;

        if (!(parts[0] in themeColors)) {
          throw new Error($mdUtil.supplant(
            'mdColors: \'hue-x\' can only be used with [{availableThemes}], but was used with \'{usedTheme}\'',
            {
            availableThemes: Object.keys(themeColors).join(', '),
            usedTheme: parts[0]
          }));
        }

        return themeColors[parts[0]].hues[parts[1]];
      }

      return parts[1] || themeColors[parts[0] in themeColors ? parts[0] : 'primary'].hues['default'];
    }
  }

  /**
   * @ngdoc directive
   * @name mdColors
   * @module material.components.colors
   *
   * @restrict A
   *
   * @description
   * `mdColors` directive will apply the theme-based color expression as RGBA CSS style values.
   *
   *   The format will be similar to the colors defined in the Sass files:
   *
   *   ## `[?theme]-[palette]-[?hue]-[?opacity]`
   *   - [theme]    - default value is the default theme
   *   - [palette]  - can be either palette name or primary/accent/warn/background
   *   - [hue]      - default is 500 (hue-x can be used with primary/accent/warn/background)
   *   - [opacity]  - default is 1
   *
   *
   *   > `?` indicates optional parameter
   *
   * @usage
   * <hljs lang="html">
   *   <div md-colors="{background: 'myTheme-accent-900-0.43'}">
   *     <div md-colors="{color: 'red-A100', 'border-color': 'primary-600'}">
   *       <span>Color demo</span>
   *     </div>
   *   </div>
   * </hljs>
   *
   * The `mdColors` directive will automatically watch for changes in the expression if it recognizes
   * an interpolation expression or a function. For performance options, you can use `::` prefix to
   * the `md-colors` expression to indicate a one-time data binding.
   *
   * <hljs lang="html">
   *   <md-card md-colors="::{background: '{{theme}}-primary-700'}">
   *   </md-card>
   * </hljs>
   */
  function MdColorsDirective($mdColors, $mdUtil, $log, $parse) {
    return {
      restrict: 'A',
      require: ['^?mdTheme'],
      compile: function (tElem, tAttrs) {
        var shouldWatch = shouldColorsWatch();

        return function (scope, element, attrs, ctrl) {
          var mdThemeController = ctrl[0];

          var lastColors = {};

          /**
           * @param {string=} theme
           * @return {Object} colors found in the specified theme
           */
          var parseColors = function (theme) {
            if (typeof theme !== 'string') {
              theme = '';
            }

            if (!attrs.mdColors) {
              attrs.mdColors = '{}';
            }

            /**
             * Json.parse() does not work because the keys are not quoted;
             * use $parse to convert to a hash map
             */
            var colors = $parse(attrs.mdColors)(scope);

            /**
             * If mdTheme is defined higher up the DOM tree,
             * we add mdTheme's theme to the colors which don't specify a theme.
             *
             * @example
             * <hljs lang="html">
             *   <div md-theme="myTheme">
             *     <div md-colors="{background: 'primary-600'}">
             *       <span md-colors="{background: 'mySecondTheme-accent-200'}">Color demo</span>
             *     </div>
             *   </div>
             * </hljs>
             *
             * 'primary-600' will be changed to 'myTheme-primary-600',
             * but 'mySecondTheme-accent-200' will not be changed since it has a theme defined.
             */
            if (mdThemeController) {
              Object.keys(colors).forEach(function (prop) {
                var color = colors[prop];
                if (!$mdColors.hasTheme(color)) {
                  colors[prop] = (theme || mdThemeController.$mdTheme) + '-' + color;
                }
              });
            }

            cleanElement(colors);

            return colors;
          };

          /**
           * @param {Object} colors
           */
          var cleanElement = function (colors) {
            if (!angular.equals(colors, lastColors)) {
              var keys = Object.keys(lastColors);

              if (lastColors.background && !keys.color) {
                keys.push('color');
              }

              keys.forEach(function (key) {
                element.css(key, '');
              });
            }

            lastColors = colors;
          };

          /**
           * Registering for mgTheme changes and asking mdTheme controller run our callback whenever
           * a theme changes.
           */
          var unregisterChanges = angular.noop;

          if (mdThemeController) {
            unregisterChanges = mdThemeController.registerChanges(function (theme) {
              $mdColors.applyThemeColors(element, parseColors(theme));
            });
          }

          scope.$on('$destroy', function () {
            unregisterChanges();
          });

          try {
            if (shouldWatch) {
              scope.$watch(parseColors, angular.bind(this,
                $mdColors.applyThemeColors, element
              ), true);
            }
            else {
              $mdColors.applyThemeColors(element, parseColors());
            }

          }
          catch (e) {
            $log.error(e.message);
          }

        };

        /**
         * @return {boolean}
         */
        function shouldColorsWatch() {
          // Simulate 1x binding and mark mdColorsWatch == false
          var rawColorExpression = tAttrs.mdColors;
          var bindOnce = rawColorExpression.indexOf('::') > -1;
          var isStatic = bindOnce ? true : STATIC_COLOR_EXPRESSION.test(tAttrs.mdColors);

          // Remove it for the postLink...
          tAttrs.mdColors = rawColorExpression.replace('::', '');

          var hasWatchAttr = angular.isDefined(tAttrs.mdColorsWatch);

          return (bindOnce || isStatic) ? false :
            hasWatchAttr ? $mdUtil.parseAttributeBoolean(tAttrs.mdColorsWatch) : true;
        }
      }
    };
  }
})();

})(window, window.angular);