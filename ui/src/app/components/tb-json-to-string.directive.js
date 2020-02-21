export default angular.module('tbJsonToString', [])
    .directive('tbJsonToString', InputJson)
    .name;

function InputJson() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attr, ngModelCtrl) {
            function into(input) {
                try {
                    ngModelCtrl.$setValidity('invalidJSON', true);
                    return angular.fromJson(input);
                } catch (e) {
                    ngModelCtrl.$setValidity('invalidJSON', false);
                }
            }
            function out(data) {
                try {
                    ngModelCtrl.$setValidity('invalidJSON', true);
                    return angular.toJson(data);
                } catch (e) {
                    ngModelCtrl.$setValidity('invalidJSON', false);
                }
            }
            ngModelCtrl.$parsers.push(into);
            ngModelCtrl.$formatters.push(out);
        }
    };
}
