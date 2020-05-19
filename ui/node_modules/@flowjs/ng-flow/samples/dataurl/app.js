/*global angular */
'use strict';

/**
 * The main app module
 * @name app
 * @type {angular.Module}
 */
var app = angular.module('app', ['flow'])
.config(['flowFactoryProvider', function (flowFactoryProvider) {
  flowFactoryProvider.defaults = {
    target: 'upload.php',
    permanentErrors: [404, 500, 501],
    maxChunkRetries: 1,
    chunkRetryInterval: 5000,
    simultaneousUploads: 4
  };
  flowFactoryProvider.on('catchAll', function (event) {
    console.log('catchAll', arguments);
  });
  // Can be used with different implementations of Flow.js
  // flowFactoryProvider.factory = fustyFlowFactory;
}]).directive('appDownloadUrl', [function () {
  return {
    restrict: 'A',
    link: function(scope, element, attrs) {
      element.bind('dragstart', function (event) {
        var config = scope.$eval(attrs.appDownloadUrl);
        if (!config.disabled) {
          var data = config.mime + ':' + config.name + ':' + window.location.href + config.url;
          event.dataTransfer.setData('DownloadURL', data);
        }
      });
    }
  };
}]).directive("appDragstart", [function () {
  return function(scope, element, attrs) {
    element.bind('dragstart', function (event) {
      scope.$eval(attrs.appDragstart);
    });
  }
}]).directive("appDragend", [function () {
  return function(scope, element, attrs) {
    element.bind('dragend', function (event) {
      scope.$eval(attrs.appDragend);
    });
  }
}]).run(function ($rootScope) {
  $rootScope.dropEnabled = true;
});