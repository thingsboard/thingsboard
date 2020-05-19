/*!
 * AngularJS Material Design
 * https://github.com/angular/material
 * @license MIT
 * v1.1.18-master-97a1616
 */
!function(i,e){"use strict";function t(a){return{restrict:"E",require:["^?mdFabSpeedDial","^?mdFabToolbar"],compile:function(i,e){var t=i.children();a.prefixer().hasAttribute(t,"ng-repeat")?t.addClass("md-fab-action-item"):t.wrap('<div class="md-fab-action-item">')}}}t.$inject=["$mdUtil"],e.module("material.components.fabActions",["material.core"]).directive("mdFabActions",t)}(window,window.angular);