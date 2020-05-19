/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// identity function for calling harmony imports with the correct context
/******/ 	__webpack_require__.i = function(value) { return value; };
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 1);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
exports.default = 'babel-test';


/***/ }),
/* 1 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";

toAnnotate.$inject = ["$scope"];
Object.defineProperty(exports, "__esModule", { value: true });
var to_import_1 = __webpack_require__(0);
console.log(to_import_1.default);
var someCtrl = (function () {
    someCtrl.$inject = ["$scope"];
    function someCtrl($scope) {
        this.doSomething();
    }
    someCtrl.prototype.doSomething = function () {
    };
    return someCtrl;
}());
angular.module('test', [])
    .controller('testCtrl', ["$scope", function ($scope) {
}])
    .factory('testFactory', ["$cacheFactory", function ($cacheFactory) {
    return {};
}])
    .service('testNotAnnotated', function () {
    return {};
})
    .directive('testDirective', ["$timeout", function ($timeout) {
    return {
        restrict: 'E',
        controller: ["$scope", function ($scope) {
        }],
    };
}])
    .controller('someCtrl', someCtrl);
function toAnnotate($scope) {
    'ngInject';
    console.log('hi'); // should be function body, otherwise babel remove directive prologue
}
console.log('after annotated function');


/***/ })
/******/ ]);
//# sourceMappingURL=build.js.map