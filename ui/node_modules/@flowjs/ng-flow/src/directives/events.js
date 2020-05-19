!function (angular) {'use strict';
  var module = angular.module('flow.events', ['flow.init']);
  var events = {
    fileSuccess: ['$file', '$message'],
    fileProgress: ['$file'],
    fileAdded: ['$file', '$event'],
    filesAdded: ['$files', '$event'],
    filesSubmitted: ['$files', '$event'],
    fileRetry: ['$file'],
    fileRemoved: ['$file'],
    fileError: ['$file', '$message'],
    uploadStart: [],
    complete: [],
    progress: [],
    error: ['$message', '$file']
  };

  angular.forEach(events, function (eventArgs, eventName) {
    var name = 'flow' + capitaliseFirstLetter(eventName);
    if (name == 'flowUploadStart') {
      name = 'flowUploadStarted';// event alias
    }
    module.directive(name, [function() {
      return {
        require: '^flowInit',
        controller: ['$scope', '$attrs', function ($scope, $attrs) {
          $scope.$on('flow::' + eventName, function () {
            var funcArgs = Array.prototype.slice.call(arguments);
            var event = funcArgs.shift();// remove angular event
            // remove flow object and ignore event if it is from parent directive
            if ($scope.$flow !== funcArgs.shift()) {
              return ;
            }
            var args = {};
            angular.forEach(eventArgs, function(value, key) {
              args[value] = funcArgs[key];
            });
            if ($scope.$eval($attrs[name], args) === false) {
              event.preventDefault();
            }
          });
        }]
      };
    }]);
  });

  function capitaliseFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
  }
}(angular);
