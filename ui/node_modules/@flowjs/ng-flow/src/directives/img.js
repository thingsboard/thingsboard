angular.module('flow.img', ['flow.init'])
.directive('flowImg', [function() {
  return {
    'scope': false,
    'require': '^flowInit',
    'link': function(scope, element, attrs) {
      var file = attrs.flowImg;
      scope.$watch(file, function (file) {
        if (!file) {
          return ;
        }
        var fileReader = new FileReader();
        fileReader.readAsDataURL(file.file);
        fileReader.onload = function (event) {
          scope.$apply(function () {
            attrs.$set('src', event.target.result);
          });
        };
      });
    }
  };
}]);