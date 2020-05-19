angular.module('nutritionApp', ['md.data.table', 'ngMaterial'])

  .config(['$mdThemingProvider', function ($mdThemingProvider) {
    'use strict';
    
    $mdThemingProvider.theme('default')
      .primaryPalette('blue')
      .accentPalette('pink');
  }]);