/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.19
 */
goog.provide('ngmaterial.components.truncate');
goog.require('ngmaterial.core');
/**
 * @ngdoc module
 * @name material.components.truncate
 */
MdTruncateController['$inject'] = ["$element"];
angular.module('material.components.truncate', ['material.core'])
  .directive('mdTruncate', MdTruncateDirective);

/**
 * @ngdoc directive
 * @name mdTruncate
 * @module material.components.truncate
 * @restrict AE
 * @description
 *
 * The `md-truncate` component displays a label that will automatically clip text which is wider
 * than the component. By default, it displays an ellipsis, but you may apply the `md-clip` CSS
 * class to override this default and use a standard "clipping" approach.
 *
 * <i><b>Note:</b> The `md-truncate` component does not automatically adjust it's width. You must
 * provide the `flex` attribute, or some other CSS-based width management. See the
 * <a ng-href="./demo/truncate">demos</a> for examples.</i>
 *
 * @usage
 *
 * ### As an Element
 *
 * <hljs lang="html">
 *   <div layout="row">
 *     <md-button>Back</md-button>
 *
 *     <md-truncate flex>Chapter 1 - The Way of the Old West</md-truncate>
 *
 *     <md-button>Forward</md-button>
 *   </div>
 * </hljs>
 *
 * ### As an Attribute
 *
 * <hljs lang="html">
 *   <h2 md-truncate style="max-width: 100px;">Some Title With a Lot of Text</h2>
 * </hljs>
 *
 * ## CSS & Styles
 *
 * `<md-truncate>` provides two CSS classes that you may use to control the type of clipping.
 *
 * <i><b>Note:</b> The `md-truncate` also applies a setting of `width: 0;` when used with the `flex`
 * attribute to fix an issue with the flex element not shrinking properly.</i>
 *
 * <div>
 * <docs-css-api-table>
 *
 *   <docs-css-selector code=".md-ellipsis">
 *     Assigns the "ellipsis" behavior (default) which will cut off mid-word and append an ellipsis
 *     (&hellip;) to the end of the text.
 *   </docs-css-selector>
 *
 *   <docs-css-selector code=".md-clip">
 *     Assigns the "clipping" behavior which will simply chop off the text. This may happen
 *     mid-word or even mid-character.
 *   </docs-css-selector>
 *
 * </docs-css-api-table>
 * </div>
 */
function MdTruncateDirective() {
  return {
    restrict: 'AE',

    controller: MdTruncateController
  };
}

/**
 * Controller for the <md-truncate> component.
 *
 * @param $element The md-truncate element.
 *
 * @constructor
 * ngInject
 */
function MdTruncateController($element) {
  $element.addClass('md-truncate');
}

ngmaterial.components.truncate = angular.module("material.components.truncate");