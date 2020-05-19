angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelIcon', mdExpansionPanelIconDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelIcon
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelIcon` can be used in both `md-expansion-panel-collapsed` and `md-expansion-panel-header` as the first or last element.
 * Adding this will provide a animated arrow for expanded and collapsed states
 **/
function mdExpansionPanelIconDirective() {
  var directive = {
    restrict: 'E',
    template: '<md-icon class="md-expansion-panel-icon" md-svg-icon="icons/ic_keyboard_arrow_right_black_24px.svg"></md-icon>',
    replace: true
  };
  return directive;
}
