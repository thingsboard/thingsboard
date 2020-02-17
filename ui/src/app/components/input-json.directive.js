export default angular.module('tbInputJson', [])
    .directive('tbJsonText', InputJson)
    .name;

function InputJson() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attr, ngModel) {
            function into(input) {
                return JSON.parse(input);//eslint-disable-line
            }
            function out(data) {
                return JSON.stringify(data);//eslint-disable-line
            }
            ngModel.$parsers.push(into);
            ngModel.$formatters.push(out);
        }
    };
}
