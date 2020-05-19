(function(){"use strict";angular
  .module('angularMaterialExpansionPanel')
  .controller('NavController', NavController);



NavController.$inject = ['$scope', '$rootScope'];
function NavController($scope, $rootScope) {
  $rootScope.$on('$routeChangeSuccess', function(event, current) {
    $scope.currentNavItem = current.$$route.originalPath || '/';
  });
}
}());