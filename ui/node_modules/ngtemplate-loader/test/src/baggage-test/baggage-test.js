angular.module('testModule')
    .directive('baggage', function() {

        return {
            restrict: 'E',
            templateUrl: require('../../index.js!html-loader!')
        }

    });