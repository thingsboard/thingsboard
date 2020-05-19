angular.module('baggageExample', [])
    .directive('myDirective', function() {
        return {
            restrict: 'E',
            templateUrl: require('./my-directive.html'),
            link: function(scope) {
                scope.foo = 'world';
            }
        }
    });
