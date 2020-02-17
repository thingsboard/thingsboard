export default angular.module('tbJsonText', [])
    .directive('tbJsonText', InputJson)
    .name;

function InputJson() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attr, ngModel) {
            function into(input) {
                return angular.fromJson(input);
            }
            function out(data) {
                return angular.toJson(data);
            }
            ngModel.$parsers.push(into);
            ngModel.$formatters.push(out);
        }
    };
}
