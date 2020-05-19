angular.module('flow.btn', ['flow.init'])
.directive('flowBtn', [function() {
  return {
    'restrict': 'EA',
    'scope': false,
    'require': '^flowInit',
    'link': function(scope, element, attrs) {
      var isDirectory = attrs.hasOwnProperty('flowDirectory');
      var isSingleFile = attrs.hasOwnProperty('flowSingleFile');
      var inputAttrs = attrs.hasOwnProperty('flowAttrs') && scope.$eval(attrs.flowAttrs);
      scope.$flow.assignBrowse(element, isDirectory, isSingleFile, inputAttrs);
    }
  };
}]);