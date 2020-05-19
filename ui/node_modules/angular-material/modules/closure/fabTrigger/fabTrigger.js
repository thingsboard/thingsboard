/*!
 * Angular Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.0-rc.5-master-26a5fb8
 */
goog.provide('ng.material.components.fabTrigger');
goog.require('ng.material.core');
(function() {
  'use strict';

  /**
   * @ngdoc module
   * @name material.components.fabTrigger
   */
  angular
    .module('material.components.fabTrigger', ['material.core'])
    .directive('mdFabTrigger', MdFabTriggerDirective);

  /**
   * @ngdoc directive
   * @name mdFabTrigger
   * @module material.components.fabSpeedDial
   *
   * @restrict E
   *
   * @description
   * The `<md-fab-trigger>` directive is used inside of a `<md-fab-speed-dial>` or
   * `<md-fab-toolbar>` directive to mark an element (or elements) as the trigger and setup the
   * proper event listeners.
   *
   * @usage
   * See the `<md-fab-speed-dial>` or `<md-fab-toolbar>` directives for example usage.
   */
  function MdFabTriggerDirective() {
    // TODO: Remove this completely?
    return {
      restrict: 'E',

      require: ['^?mdFabSpeedDial', '^?mdFabToolbar']
    };
  }
})();


ng.material.components.fabTrigger = angular.module("material.components.fabTrigger");