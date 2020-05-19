angular.module("demo", ["ngRoute", "dndLists"])
    .config(function($routeProvider) {
        $routeProvider
            .when('/simple', {
                templateUrl: 'simple/simple-frame.html',
                controller: 'SimpleDemoController'
            })
            .when('/nested', {
                templateUrl: 'nested/nested-frame.html',
                controller: 'NestedListsDemoController'
            })
            .when('/types', {
                templateUrl: 'types/types-frame.html',
                controller: 'TypesDemoController'
            })
            .when('/advanced', {
                templateUrl: 'advanced/advanced-frame.html',
                controller: 'AdvancedDemoController'
            })
            .when('/multi', {
                templateUrl: 'multi/multi-frame.html',
                controller: 'MultiDemoController'
            })
            .otherwise({redirectTo: '/nested'});
    })

    .directive('navigation', function($rootScope, $location) {
        return {
            template: '<li ng-repeat="option in options" ng-class="{active: isActive(option)}">' +
                      '    <a ng-href="{{option.href}}">{{option.label}}</a>' +
                      '</li>',
            link: function (scope, element, attr) {
                scope.options = [
                    {label: "Nested Containers", href: "#/nested"},
                    {label: "Simple Demo", href: "#/simple"},
                    {label: "Item Types", href: "#/types"},
                    {label: "Advanced Demo", href: "#/advanced"},
                    {label: "Multiselection", href: "#/multi"},
                    {label: "Github", href: "https://github.com/marceljuenemann/angular-drag-and-drop-lists"}
                ];

                scope.isActive = function(option) {
                    return option.href.indexOf(scope.location) === 1;
                };

                $rootScope.$on("$locationChangeSuccess", function(event, next, current) {
                    scope.location = $location.path();
                });
            }
        };
    });
